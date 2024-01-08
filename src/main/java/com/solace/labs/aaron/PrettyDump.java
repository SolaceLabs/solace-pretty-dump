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
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;

import org.fusesource.jansi.AnsiConsole;

import com.solacesystems.jcsmp.Browser;
import com.solacesystems.jcsmp.BrowserProperties;
import com.solacesystems.jcsmp.BytesXMLMessage;
import com.solacesystems.jcsmp.ConsumerFlowProperties;
import com.solacesystems.jcsmp.DeliveryMode;
import com.solacesystems.jcsmp.Destination;
import com.solacesystems.jcsmp.FlowEventArgs;
import com.solacesystems.jcsmp.FlowEventHandler;
import com.solacesystems.jcsmp.FlowReceiver;
import com.solacesystems.jcsmp.JCSMPChannelProperties;
import com.solacesystems.jcsmp.JCSMPErrorResponseException;
import com.solacesystems.jcsmp.JCSMPException;
import com.solacesystems.jcsmp.JCSMPFactory;
import com.solacesystems.jcsmp.JCSMPGlobalProperties;
import com.solacesystems.jcsmp.JCSMPProperties;
import com.solacesystems.jcsmp.JCSMPReconnectEventHandler;
import com.solacesystems.jcsmp.JCSMPSession;
import com.solacesystems.jcsmp.JCSMPTransportException;
import com.solacesystems.jcsmp.MapMessage;
import com.solacesystems.jcsmp.OperationNotSupportedException;
import com.solacesystems.jcsmp.Queue;
import com.solacesystems.jcsmp.SessionEventArgs;
import com.solacesystems.jcsmp.SessionEventHandler;
import com.solacesystems.jcsmp.StreamMessage;
import com.solacesystems.jcsmp.TextMessage;
import com.solacesystems.jcsmp.Topic;
import com.solacesystems.jcsmp.TopicProperties;
import com.solacesystems.jcsmp.XMLMessage;
import com.solacesystems.jcsmp.XMLMessageConsumer;
import com.solacesystems.jcsmp.XMLMessageListener;

/** Based on DirectSubscriber sample from https://github.com/SolaceSamples/solace-samples-java-jcsmp */
public class PrettyDump {

    private static final String APP_NAME = PrettyDump.class.getSimpleName();

    private static int INDENT = 4;
    private static boolean autoResizeIndent = false;  // specify -1 as indent for this MODE
    private static LinkedListOfIntegers maxLengthTopics = new LinkedListOfIntegers();
//    private static String COMPACT_STRING_FORMAT;

    private static volatile boolean isShutdown = false;          // are we done yet?
    private static String[] topics = new String[] { "#noexport/>" };  // default starting topic
    private static Browser browser = null;  // in case we need it, can't do async, is a blocking/looping pull
    private static long browseFrom = -1;
    private static long browseTo = Long.MAX_VALUE;
    private static long msgCount = 0;
    
    
    private static void updateCompactStringFormat(int maxTopicLength) {
    	maxLengthTopics.insert(maxTopicLength);
    	if (maxLengthTopics.getMax() + 2 != Math.abs(INDENT)) {  // changed our current max
    		int from = Math.abs(INDENT);
    		INDENT = -1 * (maxTopicLength + 2);
//    		COMPACT_STRING_FORMAT = "%s%-" + Math.max(1, Math.abs(INDENT) - 2) + "s%s  %s%n";  // minus 2 because we have two spaces between topic & payload
    		// add coloring here
//    		AaAnsi ansi = new AaAnsi();
//    		ansi.fg(Elem.DESTINATION).a("%-" + Math.max(1, Math.abs(INDENT) - 2) + "s  ").reset().a("%s%n");
//    		COMPACT_STRING_FORMAT = ansi.toString();
    		// no need for coloring, done below in MessageHelper
//    		COMPACT_STRING_FORMAT = "%-" + Math.max(1, Math.abs(INDENT) - 2) + "s  %s%n";  // minus 2 because we have two spaces between topic & payload
    		System.out.println("** changing INDENT from " + from + " to " + Math.abs(INDENT) + "**");
//    		System.out.println(COMPACT_STRING_FORMAT);
    	}
    }

    @SuppressWarnings("deprecation")  // this is for our use of Message ID for the browser
	public static void main(String... args) throws JCSMPException, IOException, InterruptedException {
    	
//    	AnsiConsole.systemInstall();
//    	Ansi a1 = new Ansi().fgBlack().bgRed().a("bad").bgDefault().fgGreen();
//    	Ansi a2 = new Ansi().fgGreen();
//    	String s1 = "this is some x words.";
//    	s1 = s1.replaceAll("x", a1.toString());
//    	a2.a(s1);
//    	System.out.println(a2.toString());
//    	System.exit(0);
    	
    	
    	
    	for (String arg : args) {
    		if (arg.equals("-h") || arg.startsWith("--h") || args.length > 6) {
                System.out.printf("Usage: %s [host:port] [msg-vpn] [username] [password] [topics|q:queue|b:queue|f:queue] [indent]%n%n", APP_NAME);
                System.out.println(" - If using TLS, remember \"tcps://\" before host");
                System.out.println(" - Default parameters will be: localhost default aaron pw \"#noexport/>\" 4");
                System.out.println("     (FYI: if client-username 'default' is enabled in VPN, you can use any username)");
                System.out.println(" - Subscribing options, one of:");
                System.out.println("    - comma-separated list of Direct topic subscriptions");
                System.out.println("       - strongly consider prefixing with '#noexport/' if using DMR or MNR");
                System.out.println("    - q:queueName to consume from queue");
                System.out.println("    - b:queueName to browse a queue");
                System.out.println("       - Can browse all messages, or specific messages by ID");
                System.out.println("    - f:queueName to browse/dump only first oldest message on a queue");
                System.out.println(" - Optional indent: integer, default = 4 spaces; specifying 0 compresses payload formatting");
                System.out.println("    - Use negative indent value (column width) for one-line topic & payload only");
                System.out.println("       - Or use -1 for auto column width adjustment");
                System.out.println("       - Use negative zero \"-0\" for only topic, no payload");
                System.out.println(" - Shortcut mode: if the first argument contains '>', assume topics and localhost default broker");
                System.out.println("    - e.g. ./bin/PrettyDump \"test/>\" -30");
                System.out.println("    - If zero parameters, assume localhost default broker and subscribe to \"#noexport/>\"");
                System.out.println("Environment variable options:");
                System.out.println(" - Multiple colour schemes supported. Override by setting: export PRETTY_COLORS=whatever");
                System.out.println("    - Choose: \"standard\" (default), \"minimal\", \"vivid\", \"light\" (for white backgrounds), \"off\"");
                System.out.println(" - Default charset is UTF-8. Override by setting: export PRETTY_CHARSET=whatever");
                System.out.println("    - e.g. export PRETTY_CHARSET=ISO-8859-1  (or \"set\" on Windows)");
                System.out.println("v0.1.0, 2024/01/09");
                System.out.println();
                System.exit(0);
    		}
    	}
    	String host = "localhost";
    	boolean shortcut = false;
    	// new shortcut MODE... if first arg has a > in it, assume topic wildcard, and assume localhost default connectivity for rest
    	if (args.length > 0) {
    		if (args[0].contains(">")) {  // shortcut MODE
    			shortcut = true;
    			topics = args[0].split("\\s*,\\s*");  // split on commas, remove any whitespace around them
    		} else if (args[0].matches("^[qbf]:.*")) {  // either browse, queue consume, or browse first to localhost
    			shortcut = true;
    			topics = new String[] { args[0] };  // just the one, queue name will get parsed out later
    		} else {
				host = args[0];
    		}
    	}
    	String vpn = "default";
    	if (args.length > 1 && !shortcut) vpn = args[1];
    	String username = "aaron";
    	if (args.length > 2 && !shortcut) username = args[2];
    	String password = "pw";
    	if (args.length > 3 && !shortcut) password = args[3];
    	if (args.length > 4 && !shortcut) topics = args[4].split("\\s*,\\s*");  // split on commas, remove any whitespace around them 
        if ((args.length > 5 && !shortcut) || (shortcut && args.length > 1)) {
        	String indentStr = args[shortcut ? 1 : 5];  // grab the correct command-line argument
        	try {
        		int indent = Integer.parseInt(indentStr);
        		if (indent < -250 || indent > 20) throw new NumberFormatException();  // use -200 and then use Linux util 'cut -c204-' for payload only (there are some spaces after topic)
        		INDENT = indent;
        		if (INDENT < 0) {
        			if (INDENT == -1) autoResizeIndent = true;
        			updateCompactStringFormat(Math.abs(INDENT));  // now update it
//        			COMPACT_STRING_FORMAT = "%-" + Math.max(1, Math.abs(INDENT) - 2) + "s  %s%n";  // minus 2 because we have two spaces between topic & payload
        		} else if (INDENT == 0) {
        			if (indentStr.equals("-0")) {  // special case, print topic only
        				INDENT = Integer.MIN_VALUE;
        			}
        		}
        	} catch (NumberFormatException e) {
        		System.out.printf("Invalid value for indent: '%s', using default %d instead.%n", indentStr, INDENT);
        	}
        }
		AnsiConsole.systemInstall();
        System.out.println(Banner.printBanner());
//        System.out.println("Terminal width reported as " + AnsiConsole.getTerminalWidth());
//        TestStuff.main(new String[] { });
        
        System.out.println(APP_NAME + " initializing...");
        final JCSMPProperties properties = new JCSMPProperties();
        properties.setProperty(JCSMPProperties.HOST, host);          // host:port
        properties.setProperty(JCSMPProperties.VPN_NAME, vpn);     // message-vpn
        properties.setProperty(JCSMPProperties.USERNAME, username);      // client-username
        properties.setProperty(JCSMPProperties.PASSWORD, password);  // client-password
        properties.setProperty(JCSMPProperties.REAPPLY_SUBSCRIPTIONS, true);  // subscribe Direct subs after reconnect
        JCSMPChannelProperties channelProps = new JCSMPChannelProperties();
        // die quickly
        channelProps.setConnectRetries(0);
        channelProps.setReconnectRetries(-1);
        channelProps.setConnectRetriesPerHost(1);
        properties.setProperty(JCSMPProperties.CLIENT_CHANNEL_PROPERTIES, channelProps);
        final JCSMPSession session;
        session = JCSMPFactory.onlyInstance().createSession(properties, null, new SessionEventHandler() {
            @Override
            public void handleEvent(SessionEventArgs event) {  // could be reconnecting, connection lost, etc.
                System.out.printf(" ### Received a Session event: %s%n", event);
            }
        });
//		AaAnsi.test();
//		{
//			System.out.print("Aaron RED: ");
//			for (int i=0;i<256;i++) System.out.print("\033[38;2;" + i + ";0;0m█");
//			System.out.println("\033[m");
//		}
//		JColorTest.test();
//		
		
        session.connect();  // connect to the broker... could throw JCSMPException, so best practice would be to try-catch here..!
        System.out.printf("%s connected to VPN '%s' on broker '%s'.%n%n", APP_NAME, session.getProperty(JCSMPProperties.VPN_NAME_IN_USE), session.getProperty(JCSMPProperties.HOST));

        // is it a queue?
        if (topics.length == 1 && topics[0].startsWith("q:") && topics[0].length() > 2) {  // QUEUE CONSUME!
            // configure the queue API object locally
            String queueName = topics[0].substring(2);
            final Queue queue = JCSMPFactory.onlyInstance().createQueue(queueName);
            // Create a Flow be able to bind to and consume messages from the Queue.
            final ConsumerFlowProperties flow_prop = new ConsumerFlowProperties();
            flow_prop.setEndpoint(queue);
            flow_prop.setAckMode(JCSMPProperties.SUPPORTED_MESSAGE_ACK_CLIENT);  // best practice
            flow_prop.setActiveFlowIndication(true);
            System.out.printf("Attempting to bind to queue '%s' on the broker... ", queueName);
            FlowReceiver flowQueueReceiver = null;
            try {
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
                System.out.printf("%nWill consume/ACK all messages on queue '" + queueName + "'. Use browse 'b:' command-line option otherwise.%nAre you sure? [y|yes]: ");
                BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
                String answer = reader.readLine().trim().toLowerCase();
                if (!"y".equals(answer) && !"yes".equals(answer)) {
                	System.out.println("Exiting.");
                	System.exit(0);
                }
                reader.close();
                // tell the broker to start sending messages on this queue receiver
                flowQueueReceiver.start();
            } catch (OperationNotSupportedException e) {  // not allowed to do this
            	System.err.printf("%nUh-oh!  There was a problem: %s%n%s%nQuitting!", e.getMessage(), e.toString());
    			System.exit(1);
            } catch (JCSMPErrorResponseException e) {  // something else went wrong: queue not exist, queue shutdown, etc.
            	System.err.printf("%nUh-oh!  There was a problem: %s%n%s%nQuitting!", e.getResponsePhrase(), e.toString());
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
	                System.out.printf("%nBrowse all messages -> press [ENTER],%n or enter specific Message ID,%n or range of IDs (e.g. \"25909-26183\" or \"9517-\"): ");
	                BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
	                String answer = reader.readLine().trim().toLowerCase();
	                reader.close();
	                if (answer.isEmpty()) {  // all messages
	                	
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
	                			System.out.println("Invalid format, must be either integer, or range xxxxx-yyyyyy");
	                			System.exit(0);
	                		}
	                	}
	                }
                } else {  // f:queueName, only dump first message on queue
                	browseFrom = 0;
                	browseTo = 0;
                }
	        } catch (JCSMPErrorResponseException e) {  // something else went wrong: queue not exist, queue shutdown, etc.
            	System.err.printf("%nUh-oh!  There was a problem: %s%n%s%nQuitting!", e.getResponsePhrase(), e.toString());
    			System.exit(1);
	        }
        } else {
        	JCSMPGlobalProperties.setShouldDropInternalReplyMessages(false);  // neat trick 
//        	JCSMPGlobalProperties gp = new JCSMPGlobalProperties();
//
//        	JCSMPFactory.onlyInstance().setGlobalProperties(gp);
            // Regular Direct topic consumer, using async / callback to receive
            final XMLMessageConsumer consumer = session.getMessageConsumer((JCSMPReconnectEventHandler)null, new PrinterHelper());
            for (String topic : topics) {
                TopicProperties tp = new TopicProperties();
                tp.setName(topic);
                tp.setRxAllDeliverToOne(true);  // ensure DTO-override / DA is enabled for this sub
                Topic t = JCSMPFactory.onlyInstance().createTopic(tp);
                session.addSubscription(t, true);  // true == wait for confirm
                System.out.printf("Subscribed to Direct topic: '%s'%n", topic);
            }
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
                } catch (InterruptedException e) { }  // ignore, we're quitting anyway
                System.out.println("Goodbye!");
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
		        				System.out.printf("Message with ID '%d' received, greater than than browse range '%d'. Done.%n", msgId, browseTo);
		        				break;  // done!
		        			} else {
		        				msgCount++;  // else we're just skipping this message, but still count it anyway
		        			}
	        			} catch (Exception e) {
	        				System.out.println("Exception on message trying to get Message ID!");
	        				printer.onReceive(nextMsg);
	        				break;
	        			}
	        		}
	        	}
	        // getNext() can throw a JCSMPException, but we'll just throw from main() if that happens...
            } finally {
            	System.out.println("Browsing finished!");
            	browser.close();
            }
        } else {  // async receive, either Direct sub or from a queue, so just wait here until Ctrl+C pressed
        	while (!isShutdown) {
        		Thread.sleep(50);
        	}
        }
        isShutdown = true;
        System.out.println("Main thread exiting. 👋🏼");
        AnsiConsole.systemUninstall();
    }


    static Charset CHARSET;
	static CharsetDecoder DECODER;
	static {
		if (System.getenv("PRETTY_CHARSET") != null) {
			try {
				CHARSET = Charset.forName(System.getenv("PRETTY_CHARSET"));  // this should throw if it doesn't find it
				DECODER = CHARSET.newDecoder().onMalformedInput(CodingErrorAction.REPLACE).onUnmappableCharacter(CodingErrorAction.REPLACE);
			} catch (Exception e) {
				System.err.println("Invalid charset specified! '" + System.getenv("PRETTY_CHARSET") + "'");
				e.printStackTrace();
				System.exit(1);
				throw e;  // will never get here, but stops Java from complaining about not initializing final DECODER variable
			}
		} else {
			CHARSET = StandardCharsets.UTF_8;
			DECODER = StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPLACE).onUnmappableCharacter(CodingErrorAction.REPLACE);
		}
//		System.out.println("Using charset '"+CHARSET.displayName() + "'");
	}
	
	private static String parse(byte[] bytes) throws CharacterCodingException {
        CharBuffer cb = DECODER.decode(ByteBuffer.wrap(bytes));  // could throw off a bunch of unchecked exceptions if it's binary or a different charset
        return cb.toString();
	}

	static class PayloadSection {
		
		String type = null;  // might initialize later if JSON or XML
		String formatted = "<UNINITIALIZED>";  // to ensure gets overwritten
//		boolean malformed = false;  // TBD
		
		void format(final String text) {
			if (text == null || text.isEmpty()) {
				formatted = "";
				return;
			}
			String trimmed = text.trim();
        	if (trimmed.startsWith("{") && trimmed.endsWith("}")) {  // try JSON object
        		try {
            		formatted = GsonUtils.parseJsonObject(trimmed, Math.max(INDENT, 0), false);
        			type = CHARSET.displayName() + " String, JSON Object";
				} catch (IOException e) {
        			type = CHARSET.displayName() + " String, INVALID JSON";
//        			formatted = new AaAnsi().setError().a("ERROR: ").a(e.getMessage()).reset().a('\n').a(text).reset().toString();
        			formatted = new AaAnsi().ex(e).reset().a('\n').a(text, INDENT <= 0).toString();
				}
        	} else if (trimmed.startsWith("[") && trimmed.endsWith("]")) {  // try JSON array
        		try {
            		formatted = GsonUtils.parseJsonArray(trimmed, Math.max(INDENT, 0), false);
        			type = CHARSET.displayName() + " String, JSON Array";
        		} catch (IOException e) {
        			type = CHARSET.displayName() + " String, INVALID JSON";
//        			formatted = new AaAnsi().setError().a("ERROR: ").a(e.getMessage()).reset().a('\n').a(text).reset().toString();
        			formatted = new AaAnsi().ex(e).reset().a('\n').a(text, INDENT <= 0).toString();
				}
        	} else if (trimmed.startsWith("<") && trimmed.endsWith(">")) {  // try XML
    			try {
    				AaronXmlHandler handler = new AaronXmlHandler(INDENT);
					SaxParser.parseString(trimmed, handler);
                    formatted = handler.getResult();  // overwrite
                    type = CHARSET.displayName() + " String, XML";
				} catch (SaxParserException e) {
        			type = CHARSET.displayName() + " String, INVALID XML";
        			formatted = new AaAnsi().ex(e).reset().a('\n').a(text, INDENT <= 0).toString();
				}
//        		try {
//                    Document document = DocumentHelper.parseText(trimmed);
//                    StringWriter stringWriter = new StringWriter();
//                    XMLWriter xmlWriter = new XMLWriter(stringWriter, XML_FORMAT);
//                    xmlWriter.write(document);
//                    xmlWriter.flush();
//                    formatted = stringWriter.toString().trim();  // overwrite
//                    type = CHARSET.displayName() + " String, XML";
//        		} catch (DocumentException | IOException e) {  // parsing error
//        			type = CHARSET.displayName() + " String, INVALID XML";
//        			formatted = new AaAnsi().ex(e).reset().a('\n').a(text, INDENT <= 0).toString();
////        			formatted = new AaAnsi().setError().a("ERROR: ").a(e.getMessage()).reset().a('\n').a(text).reset().toString();
//        		}
        	} else {  // it's neither JSON or XML, but has text content
        		type = CHARSET.displayName() + " String";
        		formatted = new AaAnsi().fg(Elem.STRING).a(text, INDENT <= 0).toString();
        	}
		}

		void format(byte[] bytes) {
			try {
				String parsed = parse(bytes);
				boolean malformed = parsed.contains("\ufffd");
				format(parsed);  // call the String version
	        	if (malformed) {
					type = "Non " + type;
//	        		formatted = formatted.replaceAll("\uFFFD", new AaAnsi().invalid("\ufffd").toString()) +
					if (INDENT > 0) {
						formatted += '\n' + UsefulUtils.printBinaryBytesSdkPerfStyle(bytes, INDENT, AnsiConsole.getTerminalWidth());
					}
	        	}
			} catch (CharacterCodingException e) {  // shouldn't happen since we're REPLACing not REPORTing
				throw new AssertionError("Shouldn't be here, can't throw CharCodingExcption anymore");
//				formatted = UsefulUtils.printBinaryBytesSdkPerfStyle(bytes, INDENT, AnsiConsole.getTerminalWidth());
//				type = "Non " + CHARSET.displayName() + " String, binary data";
			}
		}	
	}
	
	private static class MessageHelper {
		
		final BytesXMLMessage orig;
    	final String msgDestName;
    	final String msgDestNameFormatted;
        String msgType;

        PayloadSection binary;
        PayloadSection xml = null;
        PayloadSection userProps = null;
        PayloadSection userData = null;
                
        private MessageHelper(BytesXMLMessage message) {
        	orig = message;
        	msgType = orig.getClass().getSimpleName();  // will be "Impl" unless overridden later
        	AaAnsi ansi = new AaAnsi().fg(Elem.DESTINATION);
        	if (orig.getDestination() instanceof Queue) {
        		msgDestName = "Queue '" + orig.getDestination().getName() + "'";
        		ansi.a("Queue '" + orig.getDestination().getName() + "'");
        	} else {  // a Topic
        		msgDestName = orig.getDestination().getName();
        		ansi.colorizeTopic(orig.getDestination().getName());
        	}
        	msgDestNameFormatted = ansi.toString();
        	if (autoResizeIndent) updateCompactStringFormat(msgDestName.length());
        }
	}
	
	
	// Destination:                            Topic 'solace/samples/jcsmp/hello/aaron'
	private static void colorizeDestination(String[] dumpLines, Destination destination) {
		AaAnsi ansi = new AaAnsi().fg(Elem.PAYLOAD_TYPE).a("Destination:                            ").fg(Elem.DESTINATION);
		if (destination instanceof Topic) {
			ansi.a("Topic '").colorizeTopic(destination.getName()).fg(Elem.DESTINATION).a("'");
		} else {  // queue
			ansi.a("Queue '").a(destination.getName()).a("'");
		}
		dumpLines[0] = ansi.toString();
	}
	
    
    // Helper class, for printing message to the console ///////////////////////////////////////

    private static class PrinterHelper implements XMLMessageListener {
    	
    	private static final int DIVIDER_LENGTH = 58;  // same as SdkPerf JCSMP

    	private static void printMessageStart() {
            String head = "^^^^^ Start Message #" + ++msgCount + " ^^^^^";
            String headPre = UsefulUtils.pad((int)Math.ceil((DIVIDER_LENGTH - head.length()) / 2.0) , '^');
            String headPost = UsefulUtils.pad((int)Math.floor((DIVIDER_LENGTH - head.length()) / 2.0) , '^');
            System.out.println(new AaAnsi().fg(Elem.MSG_BREAK).a(headPre).a(head).a(headPost));
    	}
    	
    	private static void printMessageEnd() {
            String end = " End Message #" + msgCount + " ";
            String end2 = UsefulUtils.pad((int)Math.ceil((DIVIDER_LENGTH - end.length()) / 2.0) , '^');
            String end3 = UsefulUtils.pad((int)Math.floor((DIVIDER_LENGTH - end.length()) / 2.0) , '^');
            System.out.println(new AaAnsi().fg(Elem.MSG_BREAK).a(end2).a(end).a(end3));
    	}
    	
        @Override
        public void onReceive(BytesXMLMessage message) {
        	MessageHelper ms = new MessageHelper(message);
            // if doing topic only, or if there's no payload in compressed (<0) MODE, then just print the topic
        	if (INDENT == Integer.MIN_VALUE || (INDENT < 0 && !message.hasContent() && !message.hasAttachment())) {
        		System.out.println(new AaAnsi().fg(Elem.DESTINATION).a(ms.msgDestNameFormatted));
                if (browser == null && message.getDeliveryMode() != DeliveryMode.DIRECT) message.ackMessage();  // if it's a queue
                return;
        	}
        	// so at this point we know we know we will need the payload, so might as well try to parse it now
            try {  // want to catch SDT exceptions from the map and stream; payload string encoding issues now caught in format()
            	if (message.getAttachmentContentLength() > 0) {
            		ms.binary = new PayloadSection();
		            if (message instanceof MapMessage) {
		            	ms.msgType = "SDT MapMessage";
		            	ms.binary.formatted = SdtUtils.printMap(((MapMessage)message).getMap(), INDENT);
		            } else if (message instanceof StreamMessage) {
		            	ms.msgType = "SDT StreamMessage";
		            	// set directly
		            	ms.binary.formatted = SdtUtils.printStream(((StreamMessage)message).getStream(), INDENT);
		            } else {  // either text or binary, try/hope that the payload is a string, and then we can try to format it
			            if (message instanceof TextMessage) {
			            	ms.binary.format(((TextMessage)message).getText());
			            	ms.msgType = ms.binary.formatted.isEmpty() ? "Empty SDT TextMessage" : "SDT TextMessage";
			            } else {  // bytes message
			            	ms.msgType = "Raw BytesMessage";
			            	if (message.getAttachmentByteBuffer() != null) {  // should be impossible since content length > 0
			            		ms.binary.format(message.getAttachmentByteBuffer().array());
		                    }
			            }
		            }
//            	} else {
//            		System.out.println("EMPTY " + ms.msgType + " Message!");
            	}
            	// ok we now have the binary payload from the message
	            // what if there is XML content??
                if (message.hasContent()) {  // try the XML portion of the payload (OLD SCHOOL!!!)
                	ms.xml = new PayloadSection();
                	ms.xml.format(message.getBytes());
                }
                if (message.getProperties() != null && !message.getProperties().isEmpty()) {
                	ms.userProps = new PayloadSection();
                	ms.userProps.formatted = SdtUtils.printMap(message.getProperties(), INDENT);
                }
                if (message.getUserData() != null && message.getUserData().length > 0) {
                	ms.userData = new PayloadSection();
            		String simple = parse(message.getUserData());
            		AaAnsi ansi = new AaAnsi().fg(Elem.STRING).a(simple).reset();
                	if (simple.contains("\ufffd")) {
                		ansi.a('\n').aRaw(UsefulUtils.printBinaryBytesSdkPerfStyle(message.getUserData(), INDENT, AnsiConsole.getTerminalWidth()));
//                		ms.userData.formatted = UsefulUtils.printBinaryBytesSdkPerfStyle2(message.getUserData(), INDENT);
                	}
            		ms.userData.formatted = ansi.toString();
                }
                
                // now it's time to try printing it!
	            if (INDENT >= 0) {
	            	printMessageStart();
	            
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
//	                		System.out.prnitln(UsefulUtils.sd)
	                		if (INDENT > 0) System.out.println();
	                	} else if (line.startsWith("SDT Map:")) {
	                		// skip (handled as part of the binary attachment)
	                	} else if (line.startsWith("SDT Stream:")) {
	                		// skip (handled as part of the binary attachment)
	                	} else if (line.startsWith("Binary Attachment:")) {
	                		System.out.println(new AaAnsi().a(line));
	                		String combined = ms.msgType + (ms.binary.type == null ? "" : ", " + ms.binary.type) + ":";
	                		if (combined.contains("Non ")) System.out.println(new AaAnsi().invalid(combined));
							else System.out.println(new AaAnsi().fg(Elem.PAYLOAD_TYPE).a(combined));
//	                		System.out.println(UsefulUtils.chop(ms.binary.formatted));
	                		System.out.println(ms.binary.formatted);
	                		if (INDENT > 0) System.out.println();
	                	} else if (line.startsWith("XML:")) {
	                		line = line.replace("XML:                                   ", "XML Payload: (should use binary payload!)");
	                		System.out.println(new AaAnsi().a(line));
	                		if (ms.xml.type != null) {
		                		/* if (ms.xml.type.contains("INVALID")) System.out.println(new AaAnsi().invalid(ms.xml.type + ":"));
		                		else */ System.out.println(new AaAnsi().fg(Elem.PAYLOAD_TYPE).a(ms.xml.type + ":"));
	                		}
//	                			System.out.println(new AaAnsi().fg(Elem.PAYLOAD_TYPE).a(ms.xml.type).a(':'));
//	                		System.out.println(UsefulUtils.chop(ms.xml.formatted));
	                		System.out.println(ms.xml.formatted);
	                		if (INDENT > 0) System.out.println();
	                	} else if (line.contains("Destination:           ")) {
	                		System.out.println(line);  // just print out since it's already formatted
	                	} else {  // everything else
	                		System.out.println(new AaAnsi().a(line));
	                	}
	                }
	                if (INDENT > 0) {  // don't print closing bookend if indent==0
	                	printMessageEnd();
	                }
            	} else {  // INDENT < 0
            		if (ms.binary != null && ms.xml != null) {
            			// that's not great for one-line printing!
            			System.out.println("Message contains both binary and XML payloads:");
            			System.out.println(message.dump().trim());
            		} else if (ms.binary != null) {
//            			Ansi topic = new Ansi().fgCyan().a(ms.msgDestName).reset();
//            			System.out.println(topic.si
            			
            			
//            			System.out.printf(COMPACT_STRING_FORMAT,
//            					new Ansi().fgCyan().a(ms.msgDestName).reset().toString(),
//            					ms.binary.formatted);
            			try {
            				
            				
            				System.out.print(ms.msgDestNameFormatted);
            				int spaceToAdd = maxLengthTopics.getMax() + 2 - ms.msgDestName.length();
            				System.out.print(UsefulUtils.pad(spaceToAdd, ' '));
            				System.out.println(ms.binary.formatted);
            			} catch (Exception e) {
            				e.printStackTrace();
            			}
            		} else if (ms.xml != null) {
        				System.out.print(ms.msgDestNameFormatted);
        				int spaceToAdd = maxLengthTopics.getMax() + 2 - ms.msgDestName.length();
        				System.out.print(UsefulUtils.pad(spaceToAdd, ' '));
        				System.out.println(ms.xml.formatted);
            		}
            	}
            } catch (Exception e) {  // SDT parsing error, really shouldn't happen!!  or any exception!
//            	System.out.println("EXCEPTION THROWN, HERE WE ARE");
        		String[] lines = message.dump(XMLMessage.MSGDUMP_FULL).trim().split("\n");
        		colorizeDestination(lines, message.getDestination());
        		printMessageStart();
        		System.out.println(new AaAnsi().ex(e));
        		for (String line : lines) {
        			System.out.println(line);
        		}
        		printMessageEnd();
            }
            // if we're not browsing, and it's not a Direct message (doesn't matter if we ACK a Direct message anyhow)
            if (browser == null && message.getDeliveryMode() != DeliveryMode.DIRECT) message.ackMessage();  // if it's a queue
            
            // new test
/*            BytesXMLMessage copy;
			try {
				copy = SolaceMessageCopy.copy(message, true, true, true);
				System.out.println("COPY:");
	            System.out.println(copy.dump());
			} catch (JCSMPException e) {
				e.printStackTrace();
			}
			System.out.println("ORIG:");
            System.out.println(message.dump());
*/
        }

        @Override
        public void onException(JCSMPException e) {  // uh oh!
            System.out.printf(" ### MessageListener's onException(): %s%n",e);
            if (e instanceof JCSMPTransportException) {  // all reconnect attempts failed
                isShutdown = true;  // let's quit; or, could initiate a new connection attempt
            }
        }
    }
}
