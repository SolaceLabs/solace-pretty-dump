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

import java.io.IOException;
import java.io.StringWriter;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.solacesystems.jcsmp.BytesMessage;
import com.solacesystems.jcsmp.BytesXMLMessage;
import com.solacesystems.jcsmp.ConsumerFlowProperties;
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

    private static final String SAMPLE_NAME = PrettyDump.class.getSimpleName();

    private static volatile boolean isShutdown = false;          // are we done yet?
    private static volatile String[] topics = null;

    /** the main method. */
    public static void main(String... args) throws JCSMPException, IOException, InterruptedException {
        if (args.length < 5) {  // Check command line arguments
            System.out.printf("Usage: %s <host:port> <message-vpn> <client-username> <password> <topics | q:queue> [indent]%n", SAMPLE_NAME);
            System.out.println("  If using TLS, remember \"tcps://\" before host");
            System.out.println("  Either: comma separated list of topics, or \"q:queueName\" for a queue");
            System.out.println("  Optional indent: integer, default==4");
            System.exit(0);
        }
        System.out.println(SAMPLE_NAME + " initializing...");

        final JCSMPProperties properties = new JCSMPProperties();
        properties.setProperty(JCSMPProperties.HOST, args[0]);          // host:port
        properties.setProperty(JCSMPProperties.VPN_NAME,  args[1]);     // message-vpn
        properties.setProperty(JCSMPProperties.USERNAME, args[2]);      // client-username
        properties.setProperty(JCSMPProperties.PASSWORD, args[3]);  // client-password
        topics = args[4].split(",");
        if (args.length > 5) {
        	try {
        		int indent = Integer.parseInt(args[5]);
        		if (indent < 0 || indent > 20) throw new NumberFormatException();
        		INDENT = indent;
        	} catch (NumberFormatException e) {
        		System.out.printf("Invalid value for indent: '%s', using default %d instead.", args[5], INDENT);
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
                // logger.error(e);
                return;
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
        System.out.println(SAMPLE_NAME + " connected, and running. Press Ctrl-C to quit.");

        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            public void run() {
                System.out.println(" Shutdown detected, quitting...");
                isShutdown = true;
                session.closeSession();  // will also close consumer object
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) { }
                System.out.println();
            }
        }));  	

        while (!isShutdown) {
            Thread.sleep(500);
        }
        isShutdown = true;
        session.closeSession();  // will also close consumer object
        System.out.println("Main thread quitting.");
    }

    
    private static int INDENT = 4;

    private static class PrinterHelper implements XMLMessageListener {
    	
    	private static final CharsetDecoder DECODER = Charset.forName("UTF-8").newDecoder();
    	private static final OutputFormat XML_FORMAT = OutputFormat.createPrettyPrint();
    	static {
    		XML_FORMAT.setIndentSize(INDENT);
    		XML_FORMAT.setSuppressDeclaration(true);  // hides <?xml version="1.0"?>
    		XML_FORMAT.setEncoding("UTF-8");
    	}
    	
        @Override
        public void onReceive(BytesXMLMessage message) {
            System.out.println("^^^^^^^^^^^^^^^^^^ Start Message ^^^^^^^^^^^^^^^^^^^^^^^^^^^");
            String payload = null;
            String type = message.getClass().getSimpleName();
            try {
	            if (message instanceof TextMessage) {
	                payload = ((TextMessage)message).getText().trim();
	                type = "TextMessage";
	            } else if (message instanceof BytesMessage) {
	//        		String payload = new String(((BytesMessage)message).getData(), Charset.forName("UTF-8"));
                    ByteBuffer buffer = ByteBuffer.wrap(((BytesMessage)message).getData());
                    CharBuffer cb = DECODER.decode(buffer);  // could throw off a bunch of exceptions
                    payload = cb.toString().trim();
                    type = "BytesMessage";
	            } else {  // Map or Stream message
	            	// the "else" block below will print this out in full
	            }
                if (payload != null && !payload.isEmpty()) {  // means we've been initialized
                	if (payload.startsWith("{") && payload.endsWith("}")) {  // try JSON
                		try {
	                        JSONObject jo = new JSONObject(payload);
	                        System.out.println(message.dump(XMLMessage.MSGDUMP_BRIEF));
	                        System.out.printf("JSON %s: %s%n", type, jo.toString(INDENT));
                		} catch (JSONException e) {  // parsing error
	                        System.out.println(message.dump(XMLMessage.MSGDUMP_BRIEF));
	                        System.out.printf("INVALID JSON %s:%n%s%n", type, payload);
                		}
                	} else if (payload.startsWith("[") && payload.endsWith("]")) {  // try JSON array
                		try {
	                        JSONArray ja = new JSONArray(payload);
	                        System.out.println(message.dump(XMLMessage.MSGDUMP_BRIEF));
	                        System.out.printf("JSON %s: %s%n", type, ja.toString(INDENT));
                		} catch (JSONException e) {  // parsing error
	                        System.out.println(message.dump(XMLMessage.MSGDUMP_BRIEF));
	                        System.out.printf("INVALID JSON %s:%n%s%n", type, payload);
                		}
                	} else if (payload.startsWith("<") && payload.endsWith(">")) {  // try XML

                	} else if (payload.startsWith("<") && payload.endsWith(">")) {  // try XML
                		try {
	                        Document document = DocumentHelper.parseText(payload);
	                        StringWriter sw = new StringWriter();
	                        XMLWriter writer = new XMLWriter(sw, XML_FORMAT);
	                        writer.write(document);
	                        writer.flush();
	                        System.out.println(message.dump(XMLMessage.MSGDUMP_BRIEF));
	                        System.out.printf("XML %s:%n%s%n", type, sw.toString());
                		} catch (DocumentException | IOException e) {  // parsing error
	                        System.out.println(message.dump(XMLMessage.MSGDUMP_BRIEF));
	                        System.out.printf("INVALID XML %s:%n%s%n", type, payload);
                		}
                	} else {  // it's neither JSON or XML, but has text content
                        System.out.println(message.dump(XMLMessage.MSGDUMP_BRIEF));
                        System.out.printf("UTF-8 String %s:%n%s%n%n", type, payload);
                	}
                } else {  // empty string?  or Map or Stream
                    System.out.println(message.dump(XMLMessage.MSGDUMP_FULL));
                }
            } catch (Exception e) {  // parsing error, or means it's probably an actual binary message
                System.out.println(message.dump(XMLMessage.MSGDUMP_FULL));
            }
            System.out.println("^^^^^^^^^^^^^^^^^^ End Message ^^^^^^^^^^^^^^^^^^^^^^^^^^^");
            message.ackMessage();  // if required, if it's a queue
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
