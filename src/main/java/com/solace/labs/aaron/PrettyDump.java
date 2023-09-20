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
import com.solacesystems.jcsmp.JCSMPProperties;
import com.solacesystems.jcsmp.JCSMPSession;
import com.solacesystems.jcsmp.JCSMPTransportException;
import com.solacesystems.jcsmp.OperationNotSupportedException;
import com.solacesystems.jcsmp.Queue;
import com.solacesystems.jcsmp.SessionEventArgs;
import com.solacesystems.jcsmp.SessionEventHandler;
import com.solacesystems.jcsmp.TextMessage;
import com.solacesystems.jcsmp.XMLMessage;
import com.solacesystems.jcsmp.XMLMessageConsumer;
import com.solacesystems.jcsmp.XMLMessageListener;

/** Based on DirectSubscriber sample from https://github.com/SolaceSamples/solace-samples-java-jcsmp */
public class PrettyDump {

    private static final String APP_NAME = PrettyDump.class.getSimpleName();

    private static volatile boolean isShutdown = false;          // are we done yet?
    private static String[] topics = null;
    private static Browser browser = null;  // in case we need it, can't do async, is a blocking/looping pull

    /** the main method. */
    public static void main(String... args) throws JCSMPException, IOException, InterruptedException {
        if (args.length < 5) {  // Check command line arguments
            System.out.printf("Usage: %s <host:port> <message-vpn> <client-username> <password> <topics | q:queue | b:queue> [indent]%n%n", APP_NAME);
            System.out.println(" - If using TLS, remember \"tcps://\" before host");
            System.out.println(" - One of:");
            System.out.println("    - comma-separated list of Direct topic subscriptions");
            System.out.println("    - \"q:queueName\" to consume from queue");
            System.out.println("    - \"b:queueName\" to browse a queue");
            System.out.println(" - Optional indent: integer, default==4; specifying 0 compresses payload formatting");
            System.out.println("    - Use negative indent value (column width) for ultra-compact topic & payload only");
            System.out.println(" - Default charset is UTF-8. Override by setting: export PRETTY_DUMP_OPTS=-Dcharset=whatever");
            System.out.println("    - e.g. export PRETTY_DUMP_OPTS=-Dcharset=Shift_JIS  (or \"set\" on Windows)");
            System.exit(0);
        }
        System.out.println(APP_NAME + " initializing...");

        final JCSMPProperties properties = new JCSMPProperties();
        properties.setProperty(JCSMPProperties.HOST, args[0]);          // host:port
        properties.setProperty(JCSMPProperties.VPN_NAME,  args[1]);     // message-vpn
        properties.setProperty(JCSMPProperties.USERNAME, args[2]);      // client-username
        properties.setProperty(JCSMPProperties.PASSWORD, args[3]);  // client-password
        topics = args[4].split(",");
        if (args.length > 5) {
        	try {
        		int indent = Integer.parseInt(args[5]);
        		if (indent < -80 || indent > 20) throw new NumberFormatException();
        		INDENT = indent;
        		if (INDENT < 0) {
        			COMPACT_STRING_FORMAT = "%-" + Math.abs(INDENT) + "s  %s%n";
        		}
        	} catch (NumberFormatException e) {
        		System.out.printf("Invalid value for indent: '%s', using default %d instead.%n", args[5], INDENT);
        	}
        }
        properties.setProperty(JCSMPProperties.REAPPLY_SUBSCRIPTIONS, true);  // subscribe Direct subs after reconnect
        JCSMPChannelProperties channelProps = new JCSMPChannelProperties();
        channelProps.setReconnectRetries(20);      // recommended settings
        channelProps.setConnectRetriesPerHost(5);  // recommended settings
        // https://docs.solace.com/Solace-PubSub-Messaging-APIs/API-Developer-Guide/Configuring-Connection-T.htm
        properties.setProperty(JCSMPProperties.CLIENT_CHANNEL_PROPERTIES, channelProps);
        final JCSMPSession session;
        session = JCSMPFactory.onlyInstance().createSession(properties, null, new SessionEventHandler() {
            @Override
            public void handleEvent(SessionEventArgs event) {  // could be reconnecting, connection lost, etc.
                System.out.printf("### Received a Session event: %s%n", event);
            }
        });
        session.connect();  // connect to the broker

        // is it a queue?
        if (topics.length == 1 && topics[0].startsWith("q:") && topics[0].length() > 2) {
            // configure the queue API object locally
            String queueName = topics[0].substring(2);
            final Queue queue = JCSMPFactory.onlyInstance().createQueue(queueName);
            // double-check
            System.out.printf("%nWill consume/ACK all messages on queue '" + queueName + "'. Use browse 'b:' otherwise.%nAre you sure? [y|yes]: ");
            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
            String answer = reader.readLine().trim().toLowerCase();
            if (!"y".equals(answer) && !"yes".equals(answer)) {
            	System.out.println("Exiting.");
            	System.exit(0);
            }
            reader.close();
            
            // Create a Flow be able to bind to and consume messages from the Queue.
            final ConsumerFlowProperties flow_prop = new ConsumerFlowProperties();
            flow_prop.setEndpoint(queue);
            flow_prop.setAckMode(JCSMPProperties.SUPPORTED_MESSAGE_ACK_CLIENT);  // best practice
            flow_prop.setActiveFlowIndication(true);  // Flow events will advise when

            System.out.printf("Attempting to bind to queue '%s' on the broker.%n", queueName);
            FlowReceiver flowQueueReceiver = null;
            try {
                // see bottom of file for QueueFlowListener class, which receives the messages from the queue
                flowQueueReceiver = session.createFlow(new PrinterHelper(), flow_prop, null, new FlowEventHandler() {
                    @Override
                    public void handleEvent(Object source, FlowEventArgs event) {
                        // Flow events are usually: active, reconnecting (i.e. unbound), reconnected, active
                        // logger.info("### Received a Flow event: " + event);
                        // try disabling and re-enabling the queue to see in action
                    }
                });
                // tell the broker to start sending messages on this queue receiver
                flowQueueReceiver.start();
                System.out.println("Success!");
            } catch (OperationNotSupportedException e) {  // not allowed to do this
                throw e;
            } catch (JCSMPErrorResponseException e) {  // something else went wrong: queue not exist, queue shutdown, etc.
                throw e;
            }
        } else if (topics.length == 1 && topics[0].startsWith("b:") && topics[0].length() > 2) {
            String queueName = topics[0].substring(2);
            final Queue queue = JCSMPFactory.onlyInstance().createQueue(queueName);

            final BrowserProperties bp = new BrowserProperties();
	        bp.setEndpoint(queue);
            bp.setTransportWindowSize(255);
            bp.setWaitTimeout(1000);
            System.out.printf("Attempting to browse queue '%s' on the broker.%n", queueName);
	        try {
	        	browser = session.createBrowser(bp);
                System.out.println("Success!");
	        } catch (JCSMPErrorResponseException e) {  // something else went wrong: queue not exist, queue shutdown, etc.
	        	throw e;
	        }
        } else {
            // Anonymous inner-class for MessageListener, this demonstrates the async threaded message callback
            final XMLMessageConsumer consumer = session.getMessageConsumer(new PrinterHelper());
    
            for (String topic : topics) {
                session.addSubscription(JCSMPFactory.onlyInstance().createTopic(topic));
                System.out.println("Subscribed to Direct topic: " + topic);
            }
            // add more subscriptions here if you want
            consumer.start();
        }

        System.out.println();
        System.out.println(APP_NAME + " connected, and running. Press Ctrl-C to quit.");

        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            public void run() {
                System.out.println("Shutdown detected, quitting...");
                isShutdown = true;
                session.closeSession();  // will also close consumer object
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) { }
                System.out.println();
            }
        }));  	

        if (browser != null) {
            PrinterHelper printer = new PrinterHelper();
            // hasMore() is useless! from JavaDocs: Returns true if there is at least one message available in the Browser's local message buffer. Note: If this method returns false, it does not mean that the queue is empty; subsequent calls to Browser.hasMore() or Browser.getNext() might return true and a message respectively.
//        	while (browser.hasMore()) {
//        		BytesXMLMessage nextMsg = browser.getNext();
//        		printer.onReceive(nextMsg);
//        	}
            BytesXMLMessage nextMsg;
        	while ((nextMsg = browser.getNext()) != null) {
        		printer.onReceive(nextMsg);
        	}
        	System.out.println("Browsing finished!");
        	browser.close();
        } else {
        	while (!isShutdown) {
        		Thread.sleep(50);
        	}
        }
        isShutdown = true;
        System.out.println("Main thread exiting.");
    }

    
    private static int INDENT = 4;
    private static String COMPACT_STRING_FORMAT;

    private static class PrinterHelper implements XMLMessageListener {
    	
//    	private static final CharsetDecoder DECODER = Charset.forName("UTF-8").newDecoder();
    	private static final CharsetDecoder DECODER;
    	private static final OutputFormat XML_FORMAT = OutputFormat.createPrettyPrint();
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
//                    buffer = ByteBuffer.wrap(((BytesMessage)message).getData());  // save this for later (might be a map or stream)
                    buffer = message.getAttachmentByteBuffer();
//	                if (message instanceof BytesMessage) {
                    if (buffer != null) {
	                    CharBuffer cb = DECODER.decode(buffer);  // could throw off a bunch of exceptions
	                    payload = cb.toString();
                    }
                    msgType = "BytesMessage";
//		            } else if (message instanceof MapMessage) {
//		            	payload = "SDTMap";
//		            	msgType = "MapMessage";
//		            } else if (message instanceof StreamMessage) {
//		            	payload = "SDTStream";
//		            	msgType = "StreamMessage";
//		            } else {  // what else could it be?
//		            	// the "else" block below will print this out in full
//		            }
	            }
                if (payload == null || payload.isEmpty() && message.hasContent()) {  // try the XML portion of the payload (OLD SCHOOL!!!)
                	byte[] attachment = new byte[message.getContentLength()];
                	message.readContentBytes(attachment);
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
                	if (INDENT >= 0) {
		                System.out.println(message.dump(XMLMessage.MSGDUMP_BRIEF).trim());
		                System.out.printf("%s, %s:%n%s%n", payloadType, msgType, payload);
                	} else {
                		System.out.printf(COMPACT_STRING_FORMAT, message.getDestination().getName(), payload);
                	}
                } else {  // empty string?  or Map or Stream
                	if (INDENT >= 0) {
                		System.out.println(message.dump(XMLMessage.MSGDUMP_FULL).trim());
                	} else {  // compact form
                		if (payload == null) payload = "";
                		System.out.printf(COMPACT_STRING_FORMAT, message.getDestination().getName(), payload);
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
            		System.out.printf(COMPACT_STRING_FORMAT, message.getDestination().getName(), payload);
            	}
            }
            if (INDENT >= 0) System.out.println("^^^^^^^^^^^^^^^^^^ End Message ^^^^^^^^^^^^^^^^^^^^^^^^^^^");
            // if we're not browsing, and it's not a Direct message (doesn't matter if we ACK a Direct message anyhow)
            if (browser == null && message.getDeliveryMode() != DeliveryMode.DIRECT) message.ackMessage();  // if it's a queue
        }

        @Override
        public void onException(JCSMPException e) {  // uh oh!
            System.out.printf("### MessageListener's onException(): %s%n",e);
            if (e instanceof JCSMPTransportException) {  // all reconnect attempts failed
                isShutdown = true;  // let's quit; or, could initiate a new connection attempt
            }
        }
    }
}
