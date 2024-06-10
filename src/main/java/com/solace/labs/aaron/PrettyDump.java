/*
 * Copyright 2023 Solace Corporation. All rights reserved.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.solace.labs.aaron;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.fusesource.jansi.AnsiConsole;

import com.solace.labs.aaron.Banner.Which;
import com.solacesystems.jcsmp.AccessDeniedException;
import com.solacesystems.jcsmp.Browser;
import com.solacesystems.jcsmp.BrowserProperties;
import com.solacesystems.jcsmp.BytesXMLMessage;
import com.solacesystems.jcsmp.ConsumerFlowProperties;
import com.solacesystems.jcsmp.DeliveryMode;
import com.solacesystems.jcsmp.FlowEventArgs;
import com.solacesystems.jcsmp.FlowEventHandler;
import com.solacesystems.jcsmp.FlowReceiver;
import com.solacesystems.jcsmp.JCSMPChannelProperties;
import com.solacesystems.jcsmp.JCSMPException;
import com.solacesystems.jcsmp.JCSMPFactory;
import com.solacesystems.jcsmp.JCSMPGlobalProperties;
import com.solacesystems.jcsmp.JCSMPProperties;
import com.solacesystems.jcsmp.JCSMPReconnectEventHandler;
import com.solacesystems.jcsmp.JCSMPSession;
import com.solacesystems.jcsmp.JCSMPTransportException;
import com.solacesystems.jcsmp.Queue;
import com.solacesystems.jcsmp.SessionEventArgs;
import com.solacesystems.jcsmp.SessionEventHandler;
import com.solacesystems.jcsmp.Topic;
import com.solacesystems.jcsmp.TopicProperties;
import com.solacesystems.jcsmp.XMLMessageConsumer;
import com.solacesystems.jcsmp.XMLMessageListener;

/** Based on DirectSubscriber sample from https://github.com/SolaceSamples/solace-samples-java-jcsmp */
public class PrettyDump {

    private static final String APP_NAME = PrettyDump.class.getSimpleName();
    private static final Logger logger = LogManager.getLogger(PrettyDump.class);

    private static PayloadHelper payloadHelper;

    private static volatile boolean isShutdown = false;          // are we done yet?
    private static String[] topics = new String[] { "#noexport/>" };  // default starting topic
    private static Browser browser = null;  // in case we need it, can't do async, is a blocking/looping pull
    private static long browseFrom = -1;
    private static long browseTo = Long.MAX_VALUE;

    @SuppressWarnings("deprecation")  // this is for our use of Message ID for the browser
	public static void main(String... args) throws JCSMPException, IOException, InterruptedException {
    	// test code goes here
    	
    	for (String arg : args) {
    		if (arg.equals("-h") || arg.startsWith("--h") || arg.equals("-?") || arg.startsWith("--?") || args.length > 6) {
                System.out.printf("Usage: %s [host:port] [msg-vpn] [username] [password] [topics|q:queue|b:queue|f:queue] [indent]%n", APP_NAME);
                System.out.printf("   or: %s <topics|q:queue|b:queue|f:queue> [indent]    for \"shortcut\" mode%n%n", APP_NAME);
                System.out.println(" - If using TLS, remember \"tcps://\" before host");
                System.out.println(" - Default parameters will be: localhost default foo bar \"#noexport/>\" 4");
//                System.out.println("     (FYI: if client-username 'default' is enabled in VPN, you can use any username)");
                System.out.println(" - Subscribing options (param 5, or shortcut param 1), one of:");
                System.out.println("    - comma-separated list of Direct topic subscriptions");
                System.out.println("       - strongly consider prefixing with '#noexport/' if using DMR or MNR");
                System.out.println("    - q:queueName to consume from queue");
                System.out.println("    - b:queueName to browse a queue (all messages, or range of messages by ID)");
//                System.out.println("       - Can browse all messages, or specific messages by ID");
                System.out.println("    - f:queueName to browse/dump only first oldest message on a queue");
                System.out.println(" - Optional indent: integer, default = 4 spaces; specifying 0 compresses payload formatting");
                System.out.println("    - Use negative indent value (column width) for one-line topic & payload only");
                System.out.println("       - Or use -1 for auto column width adjustment");
                System.out.println("       - Use negative zero \"-0\" for only topic, no payload");
                System.out.println(" - Shortcut mode: first argument contains '>' or starts '[qbf]:', assume localhost default broker");
                System.out.println("    - e.g. bin/PrettyDump \"logs/>\" -1   ~or~   bin/PrettyDump q:q1");
                System.out.println("    - Or queues as well: e.g. ./bin/PrettyDump q:q1   ~or~   ./bin/PrettyDump b:dmq -1");
//                System.out.println("    - If zero parameters, assume localhost default broker and subscribe to \"#noexport/>\"");
                System.out.println("Environment variable options:");
                System.out.println(" - Multiple colour schemes supported. Override by setting: export PRETTY_COLORS=whatever");
                System.out.println("    - Choose: \"standard\" (default), \"minimal\", \"vivid\", \"light\", \"off\"");
                System.out.println(" - Default charset is UTF-8. Override by setting: export PRETTY_CHARSET=whatever");
                System.out.println("    - e.g. export PRETTY_CHARSET=ISO-8859-1  (or \"set\" on Windows)");
                System.out.println();
//                System.out.println("v0.1.0, 2024/01/09");
//                System.out.println();
                System.exit(0);
    		}
    	}
    	if (args.length == 1 && args[0].toLowerCase().equals("wrap")) {
    		PrettyWrap.main(new String[0]);
    		System.exit(0);
    	}
    	logger.info("### Starting PrettyDump!");
    	payloadHelper = new PayloadHelper(CHARSET);
    	String host = "localhost";
    	boolean shortcut = false;
    	// new shortcut MODE... if first arg looks like topics, assume topic wildcard, and assume localhost default connectivity for rest
    	if (args.length > 0) {
    		if (args[0].contains(">") || args[0].contains("*/")) {  // shortcut MODE
    			shortcut = true;
    			topics = args[0].split("\\s*,\\s*");  // split on commas, remove any whitespace around them
    		} else if (args[0].matches("^[qbf]:.*")) {  // either browse, queue consume, or browse first to localhost
    			shortcut = true;
    			topics = new String[] { args[0] };  // just the one, queue name will get parsed out later
    		} else if (args.length == 1) {  // exactly one arg
    			// see if it's an integer, we'll use for indent
    			try {
            		payloadHelper.dealWithIndentParam(args[0]);
            		// if nothing thrown, then it's a valid indent, so assume shortcut mode
            		shortcut = true;
    			} catch (NumberFormatException e) {
    				host = args[0];
    			}
    		} else {
				host = args[0];
    		}
    	}
    	String vpn = "default";
    	if (args.length > 1 && !shortcut) vpn = args[1];
    	String username = "foo";
    	if (args.length > 2 && !shortcut) username = args[2];
    	String password = "bar";
    	if (args.length > 3 && !shortcut) password = args[3];
    	if (args.length > 4 && !shortcut) topics = args[4].split("\\s*,\\s*");  // split on commas, remove any whitespace around them 
        if ((args.length > 5 && !shortcut) || (shortcut && args.length > 1)) {
        	String indentStr = args[shortcut ? 1 : 5];  // grab the correct command-line argument
        	try {
        		payloadHelper.dealWithIndentParam(indentStr);
        	} catch (NumberFormatException e) {
        		System.out.println(AaAnsi.n().invalid(String.format("Invalid value for indent: '%s', using default %d instead.%n", indentStr, payloadHelper.INDENT)));
        	}
        }
		AnsiConsole.systemInstall();
//		AaAnsi a = AaAnsi.n().a("hello there ").faintOn().a(" now this is faint").faintOff().a(" and now not.");
//		System.out.println(a);
		if (AnsiConsole.getTerminalWidth() >= 80) System.out.print(Banner.printBanner(Which.DUMP));
		else System.out.println();
        System.out.println(APP_NAME + " initializing...");
        payloadHelper.protobufCallbacks = ProtoBufUtils.loadProtobufDefinitions();
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
		// now let's get on with it!
        final JCSMPProperties properties = new JCSMPProperties();
        properties.setProperty(JCSMPProperties.HOST, host);          // host:port
        properties.setProperty(JCSMPProperties.VPN_NAME, vpn);     // message-vpn
        properties.setProperty(JCSMPProperties.USERNAME, username);      // client-username
        properties.setProperty(JCSMPProperties.PASSWORD, password);  // client-password
        properties.setProperty(JCSMPProperties.REAPPLY_SUBSCRIPTIONS, true);  // subscribe Direct subs after reconnect
        JCSMPChannelProperties channelProps = new JCSMPChannelProperties();
        channelProps.setConnectRetries(0);
        channelProps.setReconnectRetries(-1);
        channelProps.setConnectRetriesPerHost(1);
        properties.setProperty(JCSMPProperties.CLIENT_CHANNEL_PROPERTIES, channelProps);
        final JCSMPSession session;
        session = JCSMPFactory.onlyInstance().createSession(properties, null, new SessionEventHandler() {
            @Override
            public void handleEvent(SessionEventArgs event) {  // could be reconnecting, connection lost, etc.
//                System.out.printf(" ### Received a Session event: %s%n", event);
                System.out.println(" > " + event.getEvent().toString());
            }
        });
        session.connect();  // connect to the broker... could throw JCSMPException, so best practice would be to try-catch here..!
        session.setProperty(JCSMPProperties.CLIENT_NAME, "PrettyDump_" + session.getProperty(JCSMPProperties.CLIENT_NAME));
        
        System.out.printf("%s connected to '%s' VPN on broker '%s'.%n%n", APP_NAME, session.getProperty(JCSMPProperties.VPN_NAME_IN_USE), session.getProperty(JCSMPProperties.HOST));
        
        // is it a queue?
        if (topics.length == 1 && topics[0].startsWith("q:") && topics[0].length() > 2) {  // QUEUE CONSUME!
            // configure the queue API object locally
            String queueName = topics[0].substring(2);
            final Queue queue = JCSMPFactory.onlyInstance().createQueue(queueName);
            // Create a Flow be able to bind to and consume messages from the Queue.
            final ConsumerFlowProperties flow_prop = new ConsumerFlowProperties();
            flow_prop.setEndpoint(queue);
            flow_prop.setAckMode(JCSMPProperties.SUPPORTED_MESSAGE_ACK_CLIENT);  // best practice (but probably not necessary for this app!)
            flow_prop.setActiveFlowIndication(true);
            System.out.printf("Attempting to bind to queue '%s' on the broker... ", queueName);
            FlowReceiver flowQueueReceiver = null;
            try {
//            	if ("1".equals("1")) throw new JCSMPException("blajsdflklskfjd");
                // see bottom of file for QueueFlowListener class, which receives the messages from the queue
                flowQueueReceiver = session.createFlow(new PrinterHelper(), flow_prop, null, new FlowEventHandler() {
                    @Override
                    public void handleEvent(Object source, FlowEventArgs event) {
                        // Flow events are usually: active, reconnecting (i.e. unbound), reconnected, active
//                        System.out.printf(" ### Received a Flow event: %s%n", event);  // hide this, don't really need to show in this app
                    }
                });
                System.out.println("success!");
                // double-check
                System.out.print(AaAnsi.n().fg(Elem.PAYLOAD_TYPE).a(String.format("%nWill consume/ACK all messages on queue '%s'. Use browse 'b:' command-line option otherwise.%nAre you sure? [y|yes]: ", queueName)));
                String answer = reader.readLine().trim().toLowerCase();
                System.out.print(AaAnsi.n());  // to reset() the ANSI
                if (!"y".equals(answer) && !"yes".equals(answer)) {
                	System.out.println("\nExiting. 👎🏼");
                	System.exit(0);
                }
//                reader.close();
                // tell the broker to start sending messages on this queue receiver
                flowQueueReceiver.start();
//            } catch (OperationNotSupportedException e) {  // not allowed to do this
//            	System.out.println(AaAnsi.n().invalid(String.format("%nUh-oh!  There was a problem: %s%n%s%nQuitting!", e.getMessage(), e.toString())));
//    			System.exit(1);
//            } catch (JCSMPErrorResponseException e) {  // something else went wrong: queue not exist, queue shutdown, etc.
//            	System.out.println(AaAnsi.n().invalid(String.format("%nUh-oh!  There was a problem: %s: %s", e.getClass().getSimpleName(), e.getMessage())));
////            	System.out.println(AaAnsi.n().invalid(String.format("%nUh-oh!  There was a problem: %s%n%s%nQuitting!", e.getResponsePhrase(), e.toString())));
//    			System.exit(1);
            } catch (JCSMPException | AccessDeniedException e) {  // something else went wrong: queue not exist, queue shutdown, etc.
            	System.out.println(AaAnsi.n().invalid(String.format("%nUh-oh!  There was a problem: %s: %s", e.getClass().getSimpleName(), e.getMessage())));
//            	if (e.getCause() instanceof JCSMPErrorResponseException) {
//                	System.out.println(AaAnsi.n().invalid(((JCSMPErrorResponseException)e.getCause()).getMessage()));
//            	}
            	System.out.println("Quitting! 💀");
    			System.exit(1);
            }
        } else if (topics.length == 1 && (topics[0].startsWith("b:") || topics[0].startsWith("f:")) && topics[0].length() > 2) {  // BROWSING!
            String queueName = topics[0].substring(2);
            final Queue queue = JCSMPFactory.onlyInstance().createQueue(queueName);
            final BrowserProperties bp = new BrowserProperties();
	        bp.setEndpoint(queue);
            bp.setTransportWindowSize(255);
            bp.setWaitTimeout(1000);
            System.out.printf("Attempting to browse queue '%s' on the broker... ", queueName);
	        try {
	        	browser = session.createBrowser(bp);
                System.out.println("success!");
                if (topics[0].startsWith("b:")) {  // regular browse, prompt for msg IDs
                	System.out.print(AaAnsi.n().fg(Elem.PAYLOAD_TYPE).a(String.format("%nBrowse all messages -> press [ENTER],%n or enter specific Message ID,%n or range of IDs (e.g. \"25909-26183\" or \"9517-\"): ")));
	                String answer = reader.readLine().trim().toLowerCase();
	                System.out.print(AaAnsi.n());  // to reset() the ANSI
	                if (answer.isEmpty()) {
	                	// all messages
	                } else {
	                	try {  // assume only one integer (message ID) inputed
	                		browseFrom = Integer.parseInt(answer);
	                		browseTo = Integer.parseInt(answer);
	                	} catch (NumberFormatException e) {
	                		if (answer.matches("\\d+-\\d*")) {  // either 1234-5678  or  1234-
	                			String[] numbers = answer.split("-");
	                			browseFrom = Integer.parseInt(numbers[0]);
	                			if (numbers.length > 1) browseTo = Integer.parseInt(numbers[1]);
	                		} else {
	                			System.out.println(AaAnsi.n().invalid("Invalid format, must be either integer, or range xxxxx-yyyyyy"));
	                			System.exit(1);
	                		}
	                	}
	                }
                } else {  // f:queueName, only dump first message on queue
                	browseFrom = 0;
                	browseTo = 0;
                }
//	        } catch (JCSMPErrorResponseException e) {  // something else went wrong: queue not exist, queue shutdown, etc.
//            	System.out.println(AaAnsi.n().invalid(String.format("%nUh-oh!  There was a problem: %s%n%s%nQuitting!", e.getResponsePhrase(), e.toString())));
////            	System.err.printf("%nUh-oh!  There was a problem: %s%n%s%nQuitting!", e.getResponsePhrase(), e.toString());
//    			System.exit(1);
            } catch (JCSMPException | AccessDeniedException e) {  // something else went wrong: queue not exist, queue shutdown, etc.
            	System.out.println(AaAnsi.n().invalid(String.format("%nUh-oh!  There was a problem: %s: %s", e.getClass().getSimpleName(), e.getMessage())));
            	System.out.println("Quitting! 💀");
    			System.exit(1);
	        }
        } else {
        	JCSMPGlobalProperties.setShouldDropInternalReplyMessages(false);  // neat trick to hear/echo all req/rep messages
            // Regular Direct topic consumer, using async / callback to receive
            final XMLMessageConsumer consumer = session.getMessageConsumer((JCSMPReconnectEventHandler)null, new PrinterHelper());
            for (String topic : topics) {
                TopicProperties tp = new TopicProperties();
                tp.setName(topic);
                tp.setRxAllDeliverToOne(true);  // ensure DTO-override / DA is enabled for this sub
                Topic t = JCSMPFactory.onlyInstance().createTopic(tp);
                session.addSubscription(t, true);  // true == wait for confirm
                System.out.println(AaAnsi.n().fg(Elem.PAYLOAD_TYPE).a(String.format("Subscribed to Direct topic: '%s'", topic)).reset());
            }
//            System.out.print(AaAnsi.n());  // to reset() the ANSI
            consumer.start();
        }
        System.out.println();
        System.out.println("Starting. Press Ctrl-C to quit.");

        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            public void run() {
                System.out.println("Shutdown detected, quitting...");
                isShutdown = true;
                try {
                	Thread.sleep(200);
                	session.closeSession();  // will also close consumer object
                    Thread.sleep(300);
                } catch (InterruptedException e) {  // ignore, we're quitting anyway
                } finally {
                	System.out.println("Goodbye! 👋🏼");
                    AnsiConsole.systemUninstall();
                }
            }
        }));  	

        if (browser != null) {  // ok, so we're browsing... can't use async msg receive callback, have to poll the queue
            PrinterHelper printer = new PrinterHelper();
            // hasMore() is useless! from JavaDocs: Returns true if there is at least one message available in the Browser's local message buffer. Note: If this method returns false, it does not mean that the queue is empty; subsequent calls to Browser.hasMore() or Browser.getNext() might return true and a message respectively.
            BytesXMLMessage nextMsg;
            try {
//	        	while (!isShutdown && (nextMsg = browser.getNext()) != null) {
	        	while (!isShutdown) {  // change in behaviour... continue browsing until told to quit
	        		nextMsg = browser.getNext(-1);  // don't wait, return immediately
	        		if (nextMsg == null) {
	        			Thread.sleep(50);
	        			continue;
	        		}
	        		if (browseFrom == -1) {
	        			printer.onReceive(nextMsg);  // print all messages
	        		} else {
	        			try {
		        			long msgId = nextMsg.getMessageIdLong();  // deprecated, shouldn't be using this, but oh well!
		        			if (msgId <= 0) {  // should be impossible??
		        				System.err.println("Message received with no Message ID set!");
		        				printer.onReceive(nextMsg);
		        				break;
		        			}
		        			if (msgId >= browseFrom && msgId <= browseTo) {
		        				printer.onReceive(nextMsg);
		        				if (browseFrom == browseTo) break;  // just looking for one specific message, so done!
		        			} else if (msgId > browseTo) {
		        				if (browseFrom == browseTo) printer.onReceive(nextMsg);  // just looking for one specific message, so print this one to show the last
		        				System.out.println(AaAnsi.n().fg(Elem.PAYLOAD_TYPE).a(String.format("Message with ID '%d' received, greater than than browse range '%d'. Done.", msgId, browseTo)).reset());
		        				break;  // done!
		        			} else {
		        				payloadHelper.msgCount++;
//		        				msgCount++;  // else we're just skipping this message, but still count it anyway
		        			}
	        			} catch (Exception e) {
	        				System.out.println(AaAnsi.n().invalid("Exception on message trying to get Message ID!").reset());
	        				printer.onReceive(nextMsg);
	        				break;
	        			}
	        		}
	        	}
            } catch (JCSMPException | AccessDeniedException e) {  // something else went wrong: queue not exist, queue shutdown, etc.
            	System.out.println(AaAnsi.n().invalid(String.format("%nUh-oh!  There was a problem: %s: %s", e.getClass().getSimpleName(), e.getMessage())));
            	System.out.println("Quitting! 💀");
    			System.exit(1);
            } finally {
            	System.out.println("Browsing finished!");
            	browser.close();
            }
        } else {  // async receive, either Direct sub or from a queue, so just wait here until Ctrl+C pressed
//        	BufferedReader r = new BufferedReader(new InputStreamReader(System.in));
        	while (!isShutdown) {
        		Thread.sleep(50);
        		String blah = reader.readLine();
        		if (blah != null) {
        			try {
        				int highlight = Integer.parseInt(blah);
        				if (highlight >= 0 && highlight < 125) {
        					PayloadHelper.highlightTopicLevel = highlight - 1;  // so 0 -> -1 (highlight off), 1 -> 0 (level 1), etc.
        				}
        			} catch (NumberFormatException e) {
        				if ("+".equals(blah)) {
        					payloadHelper.autoSpaceTopicLevels = true;
        				} else if ("-".equals(blah)) {
        					payloadHelper.autoSpaceTopicLevels = false;
        				} else if ("t".equals(blah)) {
        					payloadHelper.autoTrimPayload = !payloadHelper.autoTrimPayload;
        				}
        			}
        		}
        	}
        }
        isShutdown = true;
        System.out.println("Main thread exiting.");
    }  // end of main()


    static Charset CHARSET;
	static CharsetDecoder DECODER;
	static {
		if (System.getenv("PRETTY_CHARSET") != null) {
			logger.info("Detected environment variable PRETTY_CHARSET: " + System.getenv("PRETTY_CHARSET"));
			try {
				CHARSET = Charset.forName(System.getenv("PRETTY_CHARSET"));  // this should throw if it doesn't find it
				DECODER = CHARSET.newDecoder().onMalformedInput(CodingErrorAction.REPLACE).onUnmappableCharacter(CodingErrorAction.REPLACE);
			} catch (Exception e) {
				System.out.println(AaAnsi.n().invalid("Invalid charset specified! PRETTY_CHARSET=" + System.getenv("PRETTY_CHARSET")).reset());
				System.out.println("Use one of: " + Charset.availableCharsets().keySet());
				System.out.println("Or clear environment variable 'PRETTY_CHARSET' to use default UTF-8");
            	System.out.println("Quitting! 💀");
				System.exit(1);
			}
		} else {
			logger.info("Environment variable PRETTY_CHARSET not detected, using default UTF-8");
			CHARSET = StandardCharsets.UTF_8;
			DECODER = StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPLACE).onUnmappableCharacter(CodingErrorAction.REPLACE);
		}
	}
	
/*	private static String decodeToString(byte[] bytes) {
        CharBuffer cb;
		try {
			cb = DECODER.decode(ByteBuffer.wrap(bytes));  // usually could throw off a bunch of unchecked exceptions if it's binary or a different charset, but now doing "replace" so it shouldn't
			return cb.toString();
		} catch (CharacterCodingException e) {
			throw new IllegalArgumentException("Could not decode bytes to charset",e);
		}
	}
*/
/*	private static class PayloadSection {
		
		String type = null;  // might initialize later if JSON or XML
		String formatted = "<UNINITIALIZED>";  // to ensure gets overwritten
		
		void formatString(final String text) {
			if (text == null || text.isEmpty()) {
				formatted = "";
				return;
			}
			String trimmed = text.trim();
        	if (trimmed.startsWith("{") && trimmed.endsWith("}")) {  // try JSON object
        		try {
            		formatted = GsonUtils.parseJsonObject(trimmed, Math.max(INDENT, 0));
        			type = CHARSET.displayName() + " charset, JSON Object";
				} catch (IOException e) {
        			type = CHARSET.displayName() + " charset, INVALID JSON payload";
//        			formatted = new AaAnsi().setError().a("ERROR: ").a(e.getMessage()).reset().a('\n').a(text).reset().toString();
        			formatted = new AaAnsi().ex(e).a('\n').a(text).toString();
				}
        	} else if (trimmed.startsWith("[") && trimmed.endsWith("]")) {  // try JSON array
        		try {
            		formatted = GsonUtils.parseJsonArray(trimmed, Math.max(INDENT, 0));
        			type = CHARSET.displayName() + " charset, JSON Array";
        		} catch (IOException e) {
        			type = CHARSET.displayName() + " charset, INVALID JSON payload";
//        			formatted = new AaAnsi().setError().a("ERROR: ").a(e.getMessage()).reset().a('\n').a(text).reset().toString();
        			formatted = new AaAnsi().ex(e).a('\n').a(text).toString();
				}
        	} else if (trimmed.startsWith("<") && trimmed.endsWith(">")) {  // try XML
    			try {
    				SaxHandler handler = new SaxHandler(INDENT);
					SaxParser.parseString(trimmed, handler);
                    formatted = handler.getResult();  // overwrite
                    type = CHARSET.displayName() + " charset, XML document";
				} catch (SaxParserException e) {
        			type = CHARSET.displayName() + " charset, INVALID XML payload";
        			formatted = new AaAnsi().ex(e).a('\n').a(text).toString();
				}
        	} else {  // it's neither JSON or XML, but has text content
        		type = CHARSET.displayName() + " String";
        		formatted = new AaAnsi().aStyledString(text).reset().toString();
        	}
		}

		void formatBytes(byte[] bytes) {
			String parsed = decodeToString(bytes);
			boolean malformed = parsed.contains("\ufffd");
			formatString(parsed);  // call the String version
        	if (malformed) {
				type = "Non " + type;
				if (INDENT > 0) {
					formatted += '\n' + UsefulUtils.printBinaryBytesSdkPerfStyle(bytes, INDENT, currentScreenWidth);
				}
        	}
		}
	}
*/	

	
/*	private static class MessageHelperObject {
		
		final BytesXMLMessage orig;
    	final String msgDestName;
    	final String msgDestNameFormatted;
        String msgType;

        PayloadSection binary;
        PayloadSection xml = null;
        PayloadSection userProps = null;
        PayloadSection userData = null;
                
        private MessageHelperObject(BytesXMLMessage message) {
        	orig = message;
        	msgType = orig.getClass().getSimpleName();  // will be "Impl" unless overridden later
        	AaAnsi ansi = new AaAnsi().fg(Elem.DESTINATION);
        	if (orig.getDestination() instanceof Queue) {
        		msgDestName = "Queue '" + orig.getDestination().getName() + "'";
            	if (autoResizeIndent) updateTopicIndentValue(msgDestName.length());
    			if (msgDestName.length() > Math.abs(INDENT) - 1) {  // too long, need to trim it
    				
    			}
        		ansi.a(msgDestName);
        	} else {  // a Topic
        		msgDestName = orig.getDestination().getName();
            	if (autoResizeIndent) updateTopicIndentValue(msgDestName.length());
    			if (msgDestName.length() > Math.abs(INDENT) - 1) {  // too long, need to trim it
    				// TODO add elipsis to trim topic name to indent
    			}
        		ansi.colorizeTopic(msgDestName);
        	}
        	msgDestNameFormatted = ansi.toString();
        }
	}
	*/
    
    // Helper class, for printing message to the console ///////////////////////////////////////

    private static class PrinterHelper implements XMLMessageListener {
    	
//    	private static final int DIVIDER_LENGTH = 58;  // same as SdkPerf JCSMP

//    	private static void printMessageStart() {
//            String head = "^^^^^ Start Message #" + ++msgCount + " ^^^^^";
//            String headPre = UsefulUtils.pad((int)Math.ceil((DIVIDER_LENGTH - head.length()) / 2.0) , '^');
//            String headPost = UsefulUtils.pad((int)Math.floor((DIVIDER_LENGTH - head.length()) / 2.0) , '^');
//            System.out.println(new AaAnsi().fg(Elem.MSG_BREAK).a(headPre).a(head).a(headPost).reset());
//    	}
//    	
//    	private static void printMessageEnd() {
//            String end = " End Message #" + msgCount + " ";
//            String end2 = UsefulUtils.pad((int)Math.ceil((DIVIDER_LENGTH - end.length()) / 2.0) , '^');
//            String end3 = UsefulUtils.pad((int)Math.floor((DIVIDER_LENGTH - end.length()) / 2.0) , '^');
//            System.out.println(new AaAnsi().fg(Elem.MSG_BREAK).a(end2).a(end).a(end3).reset());
//    	}
//
//    	// Destination:                            Topic 'solace/samples/jcsmp/hello/aaron'
//    	private static void colorizeDestination(String[] dumpLines, Destination destination) {
//    		AaAnsi ansi = new AaAnsi().fg(Elem.PAYLOAD_TYPE).a("Destination:                            ").fg(Elem.DESTINATION);
//    		if (destination instanceof Topic) {
//    			ansi.a("Topic '").colorizeTopic(destination.getName()).fg(Elem.DESTINATION).a("'");
//    		} else {  // queue
//    			ansi.a("Queue '").a(destination.getName()).a("'");
//    		}
//    		dumpLines[0] = ansi.toString();
//    	}

        @Override
        public void onReceive(BytesXMLMessage message) {
        	payloadHelper.dealWithMessage(message);
//        	System.out.println("System.out.println(message.dump(XMLMessage.MSGDUMP_FULL)):");
//        	System.out.println(message.dump(XMLMessage.MSGDUMP_FULL));
//        	System.out.println("System.out.println(message.dump(XMLMessage.MSGDUMP_BRIEF)):");
//        	System.out.println(message.dump(XMLMessage.MSGDUMP_BRIEF));
//        	System.out.println("System.out.println(message.dump()):");
//        	System.out.println(message.dump());
//        	if ("1".equals("1")) return;
/*        	MessageHelperObject ms = new MessageHelperObject(message);
            // if doing topic only, or if there's no payload in compressed (<0) MODE, then just print the topic
        	if (INDENT == Integer.MIN_VALUE || (INDENT < 0 && !message.hasContent() && !message.hasAttachment())) {
        		System.out.println(new AaAnsi().fg(Elem.DESTINATION).aRaw(ms.msgDestNameFormatted));
                if (browser == null && message.getDeliveryMode() != DeliveryMode.DIRECT) message.ackMessage();  // if it's a queue
                return;
        	}
        	// so at this point we know we know we will need the payload, so might as well try to parse it now
            try {  // want to catch SDT exceptions from the map and stream; payload string encoding issues now caught in format()
            	if (message.getAttachmentContentLength() > 0) {
                	PayloadHelper ph = new PayloadHelper(CHARSET, INDENT);
            		ms.binary = ph.new PayloadSection();
		            if (message instanceof MapMessage) {
		            	ms.msgType = "SDT MapMessage";
		            	ms.binary.formatted = SdtUtils.printMap(((MapMessage)message).getMap(), Math.max(INDENT, 0));
		            } else if (message instanceof StreamMessage) {
		            	ms.msgType = "SDT StreamMessage";
		            	// set directly
		            	ms.binary.formatted = SdtUtils.printStream(((StreamMessage)message).getStream(), Math.max(INDENT, 0));
		            } else {  // either text or binary, try/hope that the payload is a string, and then we can try to format it
			            if (message instanceof TextMessage) {
			            	ms.binary.formatString(((TextMessage)message).getText());
			            	ms.msgType = ms.binary.formatted.isEmpty() ? "Empty SDT TextMessage" : "SDT TextMessage";
			            } else {  // bytes message
			            	ms.msgType = "Raw BytesMessage";
			            	if (message.getAttachmentByteBuffer() != null) {  // should be impossible since content length > 0
			            		byte[] bytes = message.getAttachmentByteBuffer().array();
			            		
			            		boolean topicMatch = false;
			            		for (Entry<Sub,Method> entry : protobufCallbacks.entrySet()) {
			            			Sub sub = entry.getKey();
			            			String topic = message.getDestination().getName();
//			            			System.out.printf("Sub: %s, topic: %s%n", sub, topic);
//			            			System.out.println("Matches?  " + sub.matches(topic));
//			            			System.out.println("regex?  " + sub.pattern.matcher(topic).matches());
			            			if (sub.matches(topic)) {
			            				topicMatch = true;
			            				Object o;
										try {
											o = entry.getValue().invoke(null, bytes);
											MessageOrBuilder protoMsg = (MessageOrBuilder)o;
//				            			ms.binary.formatted = ProtoBufUtils.decodeReceiveSpan(message.getAttachmentByteBuffer().array());
											ms.binary.formatted = ProtoBufUtils.decode(protoMsg, Math.max(INDENT, 0));
											ms.binary.type = protoMsg.getClass().getSimpleName() + " ProtoBuf";
										} catch (IllegalAccessException e) {
											// TODO Auto-generated catch block
											e.printStackTrace();
										} catch (InvocationTargetException e) {
											// TODO Auto-generated catch block
											e.printStackTrace();
										}
			            			}
			            		}
			            		if (!topicMatch) ms.binary.formatBytes(bytes);  // no match, so treat as regular binary payload
		                    }
			            }
		            }
//            	} else {
//            		System.out.println("EMPTY " + ms.msgType + " Message!");
            	}
            	// ok we now have the binary payload from the message
	            // what if there is XML content??
                if (message.hasContent()) {  // try the XML portion of the payload (OLD SCHOOL!!!)
                	PayloadHelper ph = new PayloadHelper(CHARSET, INDENT);
                	ms.xml = ph.new PayloadSection();
                	ms.xml.formatBytes(message.getBytes());
                }
                if (message.getProperties() != null && !message.getProperties().isEmpty()) {
                	PayloadHelper ph = new PayloadHelper(CHARSET, INDENT);
                	ms.userProps = ph.new PayloadSection();
                	ms.userProps.formatted = SdtUtils.printMap(message.getProperties(), INDENT);
                }
                if (message.getUserData() != null && message.getUserData().length > 0) {
                	PayloadHelper ph = new PayloadHelper(CHARSET, INDENT);
                	ms.userData = ph.new PayloadSection();
            		String simple = decodeToString(message.getUserData());
            		AaAnsi ansi = new AaAnsi().fg(Elem.STRING).a(simple).reset();
                	if (simple.contains("\ufffd")) {
                		ansi.a('\n').aRaw(UsefulUtils.printBinaryBytesSdkPerfStyle(message.getUserData(), INDENT, currentScreenWidth));
                	}
            		ms.userData.formatted = ansi.toString();
                }
                
                // now it's time to try printing it!
	            if (INDENT >= 0) {
	            	PayloadHelper.printMessageStart();
		            String[] headerLines = message.dump(XMLMessage.MSGDUMP_BRIEF).split("\n");
		            colorizeDestination(headerLines, message.getDestination());
	                for (String line : headerLines) {
						if (line.startsWith("User Property Map:") && ms.userProps != null) {
	                		System.out.println(new AaAnsi().a(line));
	                		System.out.println(ms.userProps.formatted);
	                		if (INDENT > 0) System.out.println();
	                	} else if (line.startsWith("User Data:") && ms.userData != null) {
	                		System.out.println(new AaAnsi().a(line));
	                		System.out.println(ms.userData.formatted);
	                		if (INDENT > 0) System.out.println();
	                	} else if (line.startsWith("SDT Map:") || line.startsWith("SDT Stream:")) {
	                		// skip (handled as part of the binary attachment)
	                	} else if (line.startsWith("Binary Attachment:")) {
	                		System.out.println(new AaAnsi().a(line));
	                		String combined = ms.msgType + (ms.binary.type == null ? "" : ", " + ms.binary.type) + ":";
	                		if (combined.contains("Non ")) System.out.println(new AaAnsi().invalid(combined));
							else System.out.println(new AaAnsi().fg(Elem.PAYLOAD_TYPE).a(combined));
	                		System.out.println(ms.binary.formatted);
	                		if (INDENT > 0) System.out.println();
	                	} else if (line.startsWith("XML:")) {
	                		AaAnsi aa = AaAnsi.n().a("XML Payload section (").fg(Elem.NUMBER).a("LEGACY!").reset().a(")          ");
//	                		line = line.replace("XML:                                   ", "XML Payload section (LEGACY!)          ");
	                		line = line.replace("XML:                                   ", aa.toString());
	                		System.out.println(AaAnsi.r(line));
	                		if (ms.xml.type != null) {
		                		System.out.println(new AaAnsi().fg(Elem.PAYLOAD_TYPE).a(ms.xml.type + ":"));
	                		}
	                		System.out.println(ms.xml.formatted);
	                		if (INDENT > 0) System.out.println();
	                	} else if (line.contains("Destination:           ")) {  // contains, not startsWith, due to ANSI codes
	                		System.out.println(line);  // just print out since it's already formatted
	                	} else if (line.startsWith("Message Id:") && message.getDeliveryMode() == DeliveryMode.DIRECT) {
	                		// skip it, hide the auto-generated message ID on Direct messages
	                	} else {  // everything else
	                		System.out.println(AaAnsi.s(line));
	                	}
	                }
	                if (!message.hasContent() && !message.hasAttachment()) {
	                	System.out.println(new AaAnsi().fg(Elem.PAYLOAD_TYPE).a("<EMPTY PAYLOAD>").reset().toString());
	                }
	                if (INDENT > 0) {  // don't print closing bookend if indent==0
	                	printMessageEnd();
	                }
            	} else {  // INDENT < 0, one-line mode!
            		if (ms.binary != null && ms.xml != null) {
            			// that's not great for one-line printing!
            			System.out.println("Message contains both binary and XML payloads:");
            			System.out.println(message.dump().trim());
            		} else if (ms.binary != null) {
        				System.out.print(ms.msgDestNameFormatted);
        				int spaceToAdd = Math.abs(INDENT) - ms.msgDestName.length();
        				System.out.print(UsefulUtils.pad(spaceToAdd, ' '));
        				System.out.println(ms.binary.formatted);
            		} else if (ms.xml != null) {
        				System.out.print(ms.msgDestNameFormatted);
        				int spaceToAdd = Math.abs(INDENT) - ms.msgDestName.length();
        				System.out.print(UsefulUtils.pad(spaceToAdd, ' '));
        				System.out.println(ms.xml.formatted);
            		}  // else both payload sections are empty, but that is handled separately at try very top of this method
            	}
            } catch (RuntimeException e) {  // really shouldn't happen!!
            	printMessageStart();
            	System.out.println(new AaAnsi().ex(e));
            	logger.warn("Had issue parsing a message.  Message follows after exception.",e);
            	logger.warn(message.dump());
            	System.out.println(message.dump());
        		printMessageEnd();
            } */
            // if we're not browsing, and it's not a Direct message (doesn't matter if we ACK a Direct message anyhow)
            if (browser == null && message.getDeliveryMode() != DeliveryMode.DIRECT) message.ackMessage();  // if it's a queue
        }

        @Override
        public void onException(JCSMPException e) {  // uh oh!
            System.out.printf(" ### MessageListener's onException(): %s%n",e);
            if (e instanceof JCSMPTransportException) {  // all reconnect attempts failed
                isShutdown = true;  // let's quit
            }
        }
    }
}
