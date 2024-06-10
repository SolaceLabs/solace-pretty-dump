package com.solace.labs.aaron;

import com.solacesystems.jcsmp.JCSMPException;
import com.solacesystems.jcsmp.JCSMPFactory;
import com.solacesystems.jcsmp.JCSMPProperties;
import com.solacesystems.jcsmp.JCSMPSession;
import com.solacesystems.jcsmp.Queue;
import com.solacesystems.jcsmp.SessionEventArgs;
import com.solacesystems.jcsmp.SessionEventHandler;
import com.solacesystems.jcsmp.Topic;

public class LotsOfSubTopics {

	
	public static void main(String... args) throws JCSMPException {
		JCSMPProperties properties = new JCSMPProperties();
	    properties.setProperty(JCSMPProperties.HOST, args[0]);
	    properties.setProperty(JCSMPProperties.VPN_NAME,  args[1]);     // message-vpn
	    properties.setProperty(JCSMPProperties.USERNAME, args[2]);      // client-username
        properties.setProperty(JCSMPProperties.PASSWORD, args[3]);  // client-password
        final String queueName = args[4];
        
        JCSMPSession session = JCSMPFactory.onlyInstance().createSession(properties, null, new SessionEventHandler() {
			
			@Override
			public void handleEvent(SessionEventArgs arg0) {
				if (arg0.getException() != null) {
					System.err.println("Received this: " + arg0);
				} else {
					System.out.println("Received this: " + arg0);
				}
			}
		});
        session.connect();
        
        // Create a Flow be able to bind to and consume messages from the Queue.
//        final ConsumerFlowProperties flow_prop = new ConsumerFlowProperties();
//        flow_prop.setEndpoint(queue);
//        flow_prop.setAckMode(JCSMPProperties.SUPPORTED_MESSAGE_ACK_CLIENT);  // best practice
//        flow_prop.setActiveFlowIndication(true);  // Flow events will advise when 
        // https://docs.solace.com/API-Developer-Online-Ref-Documentation/java/com/solacesystems/jcsmp/JCSMPSession.html#addSubscription-com.solacesystems.jcsmp.Endpoint-com.solacesystems.jcsmp.Subscription-int-
        
//        final XMLMessageConsumer consumer = session.getMessageConsumer((XMLMessageListener)null);  // blocking non-async
        final int TOTAL = 100;
        final Queue queue = JCSMPFactory.onlyInstance().createQueue(queueName);
        long start = System.currentTimeMillis();
        System.out.print("Starting... ");
        for (int i=0;i<TOTAL-1;i++) {
        	Topic topic = JCSMPFactory.onlyInstance().createTopic("abc/"+(i+1));
        	session.addSubscription(queue, topic, 0);
        }
        Topic topic = JCSMPFactory.onlyInstance().createTopic("abc/"+TOTAL);
    	session.addSubscription(queue, topic, JCSMPSession.WAIT_FOR_CONFIRM);
    	long end = System.currentTimeMillis();
        System.out.println("Done in " + (end-start) + "ms.");
        
        
        
        

        
        
        
	}
}
