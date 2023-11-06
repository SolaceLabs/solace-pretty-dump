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
import java.io.StringWriter;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.StandardCharsets;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.solacesystems.jcsmp.Browser;
import com.solacesystems.jcsmp.BrowserProperties;
import com.solacesystems.jcsmp.BytesXMLMessage;
import com.solacesystems.jcsmp.ConsumerFlowProperties;
import com.solacesystems.jcsmp.DeliveryMode;
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
    private static String COMPACT_STRING_FORMAT;

    private static volatile boolean isShutdown = false;          // are we done yet?
    private static String[] topics = new String[] { "#noexport/>" };  // default starting topic
    private static Browser browser = null;  // in case we need it, can't do async, is a blocking/looping pull
    private static long browseFrom = -1;
    private static long browseTo = Long.MAX_VALUE;

    @SuppressWarnings("deprecation")  // this is for our use of Message ID for the browser
	public static void main(String... args) throws JCSMPException, IOException, InterruptedException {
    	for (String arg : args) {
    		if (arg.equals("-h") || arg.startsWith("--h") || args.length > 6) {
                System.out.printf("Usage: %s [host:port] [message-vpn] [username] [password] [topics|q:queue|b:queue] [indent]%n%n", APP_NAME);
                System.out.println(" - If using TLS, remember \"tcps://\" before host");
                System.out.println(" - Default parameters will be: localhost default aaron pw \"#noexport/>\" 4");
                System.out.println("    - If client-username 'default' is enabled in VPN, you can use any username");
                System.out.println(" - Subscribing options, one of:");
                System.out.println("    - comma-separated list of Direct topic subscriptions");
                System.out.println("    - q:queueName to consume from queue");
                System.out.println("    - b:queueName to browse a queue");
                System.out.println("       - Can browse all messages, or specific messages by ID");
                System.out.println(" - Optional indent: integer, default = 4 spaces; specifying 0 compresses payload formatting");
                System.out.println("    - Use negative indent value (column width) for one-line topic & payload only");
                System.out.println("       - Use negative zero (\"-0\") for only topic, no payload");
                System.out.println(" - Shortcut mode: if the first argument contains '>', assume topics and localhost default broker");
                System.out.println("    - e.g. ./bin/PrettyDump \"test/>\" -30");
                System.out.println("    - If zero parameters, assume localhost default broker and subscribe to \"#noexport/>\"");
                System.out.println(" - Default charset is UTF-8. Override by setting: export PRETTY_DUMP_OPTS=-Dcharset=whatever");
                System.out.println("    - e.g. export PRETTY_DUMP_OPTS=-Dcharset=Shift_JIS  (or \"set\" on Windows)");
                System.out.println();
                System.exit(0);
    		}
    	}
    	String host = "localhost";
    	boolean shortcut = false;
    	// new shortcut mode... if first arg has a > in it, assume topic wildcard, and assume localhost default connectivity for rest
    	if (args.length > 0) {
    		if (args[0].contains(">")) {  // shortcut mode
    			shortcut = true;
    			topics = args[0].split("\\s*,\\s*");  // split on commas, remove any whitespace around them
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
        			COMPACT_STRING_FORMAT = "%-" + Math.max(1, Math.abs(INDENT) - 2) + "s  %s%n";  // minus 2 because we have two spaces between topic & payload
        		} else if (INDENT == 0) {
        			if (indentStr.equals("-0")) {  // special case, topic only
        				INDENT = Integer.MIN_VALUE;
        			}
        		}
        	} catch (NumberFormatException e) {
        		System.out.printf("Invalid value for indent: '%s', using default %d instead.%n", indentStr, INDENT);
        	}
        }
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
        channelProps.setReconnectRetries(5);
        channelProps.setConnectRetriesPerHost(1);
        properties.setProperty(JCSMPProperties.CLIENT_CHANNEL_PROPERTIES, channelProps);
        final JCSMPSession session;
        session = JCSMPFactory.onlyInstance().createSession(properties, null, new SessionEventHandler() {
            @Override
            public void handleEvent(SessionEventArgs event) {  // could be reconnecting, connection lost, etc.
                System.out.printf(" ### Received a Session event: %s%n", event);
            }
        });
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
        } else if (topics.length == 1 && topics[0].startsWith("b:") && topics[0].length() > 2) {  // BROWSING!
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
                // double-check
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
                	Thread.sleep(100);
                	session.closeSession();  // will also close consumer object
                    Thread.sleep(400);
                } catch (InterruptedException e) { }  // ignore, we're quitting anyway
                System.out.println();
            }
        }));  	

        if (browser != null) {  // ok, so we're browsing... can't use async msg receive callback, have to poll the queue
            PrinterHelper printer = new PrinterHelper();
            // hasMore() is useless! from JavaDocs: Returns true if there is at least one message available in the Browser's local message buffer. Note: If this method returns false, it does not mean that the queue is empty; subsequent calls to Browser.hasMore() or Browser.getNext() might return true and a message respectively.
//        	while (browser.hasMore()) {
//        		BytesXMLMessage nextMsg = browser.getNext();
//        		printer.onReceive(nextMsg);
//        	}
            BytesXMLMessage nextMsg;
            try {
	        	while (!isShutdown && (nextMsg = browser.getNext()) != null) {
	        		if (browseFrom == -1) {
	        			printer.onReceive(nextMsg);  // print all messages
	        		} else {
	        			try {
		        			long msgId = nextMsg.getMessageIdLong();  // deprecated, shouldn't be using this, but oh well!
		        			if (msgId <= 0) {
		        				System.out.println("Message received with no Message ID set!");
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
        System.out.println("Main thread exiting.");
    }


    
    
    // Helper class, for printing message to the console ///////////////////////////////////////

    private static class PrinterHelper implements XMLMessageListener {
    	
//    	private static final CharsetDecoder DECODER = Charset.forName("UTF-8").newDecoder();
    	private static final CharsetDecoder DECODER;
    	private static final OutputFormat XML_FORMAT = INDENT < 0 ? OutputFormat.createCompactFormat() : OutputFormat.createPrettyPrint();
    	static {
    		XML_FORMAT.setIndentSize(Math.max(INDENT, 0));
    		XML_FORMAT.setSuppressDeclaration(true);  // hides <?xml version="1.0"?>
    		if (System.getProperty("charset") != null) {
    			try {
    				DECODER = Charset.forName(System.getProperty("charset")).newDecoder();
    				XML_FORMAT.setEncoding(System.getProperty("charset"));
    			} catch (Exception e) {
    				System.err.println("Invalid charset specified!");
    				e.printStackTrace();
    				System.exit(1);
    				throw e;  // will never get here, but stops Java from complaining about not initializing final DECODER variable
    			}
    		} else {
    			DECODER = StandardCharsets.UTF_8.newDecoder();
    			XML_FORMAT.setEncoding("UTF-8");
    		}
    	}
    	
        @Override
        public void onReceive(BytesXMLMessage message) {
        	String msgDestName;
        	if (message.getDestination() instanceof Queue) {
        		msgDestName = "Queue '" + message.getDestination().getName() + "'";
        	} else {
        		msgDestName = message.getDestination().getName();
        	}
            // if doing topic only, or if there's no payload in compressed (<0) mode, then just print the topic
        	if (INDENT == Integer.MIN_VALUE || (INDENT < 1 && !message.hasContent() && !message.hasAttachment())) {
        		System.out.println(msgDestName);
                if (browser == null && message.getDeliveryMode() != DeliveryMode.DIRECT) message.ackMessage();  // if it's a queue
                return;
        	}
            if (INDENT >= 0) System.out.println("^^^^^^^^^^^^^^^^^ Start Message ^^^^^^^^^^^^^^^^^^^^^^^^^^");
            String payload = null;
            String trimmedPayload = null;
            String msgType = message.getClass().getSimpleName();  // will be "Impl" unless overridden below
            String payloadType = "";  // TBD later
            ByteBuffer buffer = null;  // if it's a binary message
            try {
	            if (message instanceof TextMessage) {
	                payload = ((TextMessage)message).getText();
	                msgType = "TextMessage";
	            } else {  // bytes, stream, map
                    buffer = message.getAttachmentByteBuffer();
                    if (buffer != null) {
	                    CharBuffer cb = DECODER.decode(buffer);  // could throw off a bunch of exceptions if it's binary or a different charset
	                    payload = cb.toString();
                    }
		            if (message instanceof MapMessage) {
		            	msgType = "MapMessage";
		            } else if (message instanceof StreamMessage) {
		            	msgType = "StreamMessage";
		            } else {
		            	msgType = "BytesMessage";
		            }
	            }
                if (payload == null || payload.isEmpty() && message.hasContent()) {  // try the XML portion of the payload (OLD SCHOOL!!!)
                	byte[] attachment = message.getBytes();  // XML?
                	buffer = ByteBuffer.wrap(attachment);
                    CharBuffer cb = DECODER.decode(buffer);  // could throw off a bunch of exceptions
                    payload = cb.toString();
                    msgType = "XML Payload (should really be using Binary attachment)";
                }
                if (payload != null && !payload.isEmpty()) {  // means a detected String payload
                	trimmedPayload = payload.trim();
                	if (trimmedPayload.startsWith("{") && trimmedPayload.endsWith("}")) {  // try JSON
                		try {
	                        JSONObject jo = new JSONObject(trimmedPayload);
	                        // success in parsing JSON!
	                        payloadType = "JSON Object";
	                        payload = jo.toString(Math.max(INDENT, 0)).trim();  // overwrite
                		} catch (JSONException e) {  // parsing error
                			payloadType = "INVALID JSON";
                		}
                	} else if (trimmedPayload.startsWith("[") && trimmedPayload.endsWith("]")) {  // try JSON array
                		try {
	                        JSONArray ja = new JSONArray(trimmedPayload);
	                        payloadType = "JSON Array";
	                        payload = ja.toString(Math.max(INDENT, 0)).trim();  // overwrite
                		} catch (JSONException e) {  // parsing error
                			payloadType = "INVALID JSON";
                		}
                	} else if (trimmedPayload.startsWith("<") && trimmedPayload.endsWith(">")) {  // try XML
                		try {
	                        Document document = DocumentHelper.parseText(trimmedPayload);
	                        StringWriter stringWriter = new StringWriter();
	                        XMLWriter xmlWriter = new XMLWriter(stringWriter, XML_FORMAT);
	                        xmlWriter.write(document);
	                        xmlWriter.flush();
	                        payloadType = "XML";
	                        payload = stringWriter.toString().trim();  // overwrite
                		} catch (DocumentException | IOException e) {  // parsing error
                			payloadType = "INVALID XML";
                		}
                	} else {  // it's neither JSON or XML, but has text content
                		payloadType = DECODER.charset().displayName() + " String";
                	}
	                // all done parsing what we can and initializing the vars, so print it out!
                	if (INDENT >= 0) {  // postive indent, so print out the standard SdkPerf -md type header stuff first
		                System.out.println(message.dump(XMLMessage.MSGDUMP_BRIEF).trim());
		                // now the payload part
//		                if (INDENT == 0 || (!payloadType.startsWith("XML") && (!payloadType.startsWith("JSON")))) {  // i.e. if it's invalid or just a string, then save the carriage return
		                if (INDENT == 0) {
		                	String combined = msgType + ", " + payloadType + ":";
		                	System.out.printf("%-40s%s%n", combined, payload);
		                } else {
		                	System.out.printf("%s, %s:%n%s%n", msgType, payloadType, payload);
		                }
                	} else {
                		System.out.printf(COMPACT_STRING_FORMAT, msgDestName, payload);
                	}
                } else {  // empty string?  or Map or Stream
                	if (INDENT >= 0) {
                		System.out.println(message.dump(XMLMessage.MSGDUMP_FULL).trim());
                	} else {  // compact form
                		if (payload == null) payload = "";
                		System.out.printf(COMPACT_STRING_FORMAT, msgDestName, payload);
                	}
                }
            } catch (CharacterCodingException e) {  // parsing error, or means it's probably an actual binary message
            	if (INDENT >= 0) {
            		System.out.println(message.dump(XMLMessage.MSGDUMP_FULL).trim());
            	} else {
            		byte[] bytes = buffer.array();
            		for (int i=0; i < bytes.length; i++) {
            			if ((bytes[i] >= 0 && bytes[i] < 32) || bytes[i] == 127) {  // control char
            				bytes[i] = 46;
            			}
            		}
            		payload = new String(buffer.array(), StandardCharsets.UTF_8);  // this doesn't seem to throw off errors for unprintable chars
            		System.out.printf(COMPACT_STRING_FORMAT, msgDestName, payload);
            	}
            }
            if (INDENT > 0) System.out.println("^^^^^^^^^^^^^^^^^^ End Message ^^^^^^^^^^^^^^^^^^^^^^^^^^^");  // if == 0, then skip b/c we're compact!
            // if we're not browsing, and it's not a Direct message (doesn't matter if we ACK a Direct message anyhow)
            if (browser == null && message.getDeliveryMode() != DeliveryMode.DIRECT) message.ackMessage();  // if it's a queue
            
            // new test
            BytesXMLMessage copy;
			try {
				copy = SolaceMessageCopy.copy(message, true, true, true);
				System.out.println("COPY:");
	            System.out.println(copy.dump());
			} catch (JCSMPException e) {
				e.printStackTrace();
			}
			System.out.println("ORIG:");
            System.out.println(message.dump());
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
