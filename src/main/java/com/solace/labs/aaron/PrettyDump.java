/*
 * Copyright 2021-2022 Solace Corporation. All rights reserved.
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
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.UnmappableCharacterException;

import org.json.JSONException;
import org.json.JSONObject;

import com.solacesystems.jcsmp.BytesMessage;
import com.solacesystems.jcsmp.BytesXMLMessage;
import com.solacesystems.jcsmp.JCSMPChannelProperties;
import com.solacesystems.jcsmp.JCSMPException;
import com.solacesystems.jcsmp.JCSMPFactory;
import com.solacesystems.jcsmp.JCSMPProperties;
import com.solacesystems.jcsmp.JCSMPSession;
import com.solacesystems.jcsmp.JCSMPTransportException;
import com.solacesystems.jcsmp.SessionEventArgs;
import com.solacesystems.jcsmp.SessionEventHandler;
import com.solacesystems.jcsmp.TextMessage;
import com.solacesystems.jcsmp.XMLMessage;
import com.solacesystems.jcsmp.XMLMessageConsumer;
import com.solacesystems.jcsmp.XMLMessageListener;

/** This is a more detailed subscriber sample. */
public class PrettyDump {

    private static final String SAMPLE_NAME = PrettyDump.class.getSimpleName();

    private static volatile boolean isShutdown = false;          // are we done yet?
    private static volatile String topic = null;

    /** the main method. */
    public static void main(String... args) throws JCSMPException, IOException, InterruptedException {
        if (args.length != 5) {  // Check command line arguments
            System.out.printf("Usage: %s <host:port> <message-vpn> <client-username> <password> <topic>%n%n", SAMPLE_NAME);
            System.exit(-1);
        }
        System.out.println(SAMPLE_NAME + " initializing...");

        final JCSMPProperties properties = new JCSMPProperties();
        properties.setProperty(JCSMPProperties.HOST, args[0]);          // host:port
        properties.setProperty(JCSMPProperties.VPN_NAME,  args[1]);     // message-vpn
        properties.setProperty(JCSMPProperties.USERNAME, args[2]);      // client-username
        properties.setProperty(JCSMPProperties.PASSWORD, args[3]);  // client-password
        topic = args[4];
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

        // Anonymous inner-class for MessageListener, this demonstrates the async threaded message callback
        final XMLMessageConsumer consumer = session.getMessageConsumer(new XMLMessageListener() {
            @Override
            public void onReceive(BytesXMLMessage message) {
            	System.out.println("^^^^^^^^^^^^^^^^^^ Start Message ^^^^^^^^^^^^^^^^^^^^^^^^^^^");
            	if (message instanceof TextMessage) {
            		String payload = ((TextMessage)message).getText().trim();
            		if (payload.startsWith("{")) {
            			try {
            				JSONObject jo = new JSONObject(payload);
                        	System.out.println(message.dump(XMLMessage.MSGDUMP_BRIEF));
            				System.out.println("TextMessage JSON: " + jo.toString(4));
            			} catch (JSONException e) {
            				System.err.println("Couldn't parse JSON");
                        	System.out.println(message.dump(XMLMessage.MSGDUMP_FULL));
            			}
            		} else {
//            			System.out.println("Text message, not {");
                    	System.out.println(message.dump(XMLMessage.MSGDUMP_FULL));
            		}
            	} else if (message instanceof BytesMessage) {
//            		String payload = new String(((BytesMessage)message).getData(), Charset.forName("UTF-8"));
            		CharsetDecoder decoder = Charset.forName("UTF-8").newDecoder();
            		try {
            			ByteBuffer buffer = ByteBuffer.wrap(((BytesMessage)message).getData());
            			CharBuffer cb = decoder.decode(buffer);
            			String payload = cb.toString().trim();
	            		if (payload.startsWith("{")) {
	            			try {
	            				JSONObject jo = new JSONObject(payload);
	                        	System.out.println(message.dump(XMLMessage.MSGDUMP_BRIEF));
	            				System.out.println("BytesMessage JSON: " + jo.toString(4));
	            			} catch (JSONException e) {
	            				System.err.println("Couldn't parse JSON");
	                        	System.out.println(message.dump(XMLMessage.MSGDUMP_FULL));
	            			}
	            		} else {
	            			System.out.println("BytesMessage with text content:");
	                    	System.out.println(message.dump(XMLMessage.MSGDUMP_FULL));
	            		}
            		} catch (Exception e) {  // means it's probably an actual binary message
//            			System.out.println("Binary");
                    	System.out.println(message.dump(XMLMessage.MSGDUMP_FULL));
            		}
            		
            	} else {
            		System.out.println("Got a message not text or binary");
                	System.out.println(message.dump(XMLMessage.MSGDUMP_FULL));
            	}
            	System.out.println("^^^^^^^^^^^^^^^^^^ End Message ^^^^^^^^^^^^^^^^^^^^^^^^^^^");
            }

            @Override
            public void onException(JCSMPException e) {  // uh oh!
                System.out.printf("### MessageListener's onException(): %s%n",e);
                if (e instanceof JCSMPTransportException) {  // all reconnect attempts failed
                    isShutdown = true;  // let's quit; or, could initiate a new connection attempt
                }
            }
        });

        session.addSubscription(JCSMPFactory.onlyInstance().createTopic(topic));
        // add more subscriptions here if you want
        consumer.start();
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
}
