package com.solace.labs.aaron;

import com.solacesystems.jcsmp.BytesMessage;
import com.solacesystems.jcsmp.BytesXMLMessage;
import com.solacesystems.jcsmp.JCSMPException;
import com.solacesystems.jcsmp.JCSMPFactory;
import com.solacesystems.jcsmp.JCSMPProperties;
import com.solacesystems.jcsmp.JCSMPSession;
import com.solacesystems.jcsmp.JCSMPStreamingPublishCorrelatingEventHandler;
import com.solacesystems.jcsmp.MapMessage;
import com.solacesystems.jcsmp.StreamMessage;
import com.solacesystems.jcsmp.TextMessage;
import com.solacesystems.jcsmp.XMLMessageProducer;

public final class SolaceMessageCopy {

	private static final JCSMPFactory f = JCSMPFactory.onlyInstance();
	
	

	/** Might want to use a Builder pattern here?   */
	public static BytesXMLMessage copy(BytesXMLMessage msg, boolean includeTtl, boolean includeExpiration, boolean includeElidingEligible) throws JCSMPException {
		BytesXMLMessage dstMsg;
		// need to create specific message type... can't just use generic message and set payload as bytes
		if (msg instanceof TextMessage) {
			System.out.println("TextMessage");
			dstMsg = f.createMessage(TextMessage.class);
		}
		else if (msg instanceof MapMessage) {
			System.out.println("MapMessage");
			dstMsg = f.createMessage(MapMessage.class);
		}
		else if (msg instanceof StreamMessage) {
			System.out.println("StreamMessage");
			dstMsg = f.createMessage(StreamMessage.class);
		}
		else {
			System.out.println("BytesMessage");
			dstMsg = f.createMessage(BytesMessage.class);
		}
		
        // metadata / headers
        if (msg.getApplicationMessageId() != null)			dstMsg.setApplicationMessageId(msg.getApplicationMessageId());
        if (msg.getApplicationMessageType() != null)		dstMsg.setApplicationMessageType(msg.getApplicationMessageType());
        // no set on this...
//        if (msg.getConsumerIdList() != null && !msg.getConsumerIdList().isEmpty()) {
//            JsonArrayBuilder jab = Json.createArrayBuilder();
//            for (Long l : msg.getConsumerIdList()) {
//                jab.add(l);
//            }
//            job.add("consumerIdList", jab.build());
//        }
        // this will get set automatically by populating 
//        if (msg.getContentLength() > 0) job.add("contentLength", msg.getContentLength());
        if (msg.getCorrelationId() != null)					dstMsg.setCorrelationId(msg.getCorrelationId());
        dstMsg.setCos(msg.getCos());
//        try {
//            job.add("deliveryCount", msg.getDeliveryCount());
//        } catch (UnsupportedOperationException e) {
//            // ignore
//        }
        dstMsg.setDeliveryMode(msg.getDeliveryMode());
        if (msg.isDMQEligible() && includeElidingEligible)	dstMsg.setDMQEligible(true);
        if (msg.getExpiration() > 0 && includeExpiration)	dstMsg.setExpiration(msg.getExpiration());  // obviously this might not be great if cloning messages elsewhere later
        if (msg.getHTTPContentEncoding() != null)			dstMsg.setHTTPContentEncoding(msg.getHTTPContentEncoding());
        if (msg.getHTTPContentType() != null)				dstMsg.setHTTPContentType(msg.getHTTPContentType());
        // this is automatically set by the broker, as the message spool ID
//        if (msg.getMessageId() != null) job.add("mesageId", msg.getMessageId());  // deprecated, but still dump it out
        dstMsg.setPriority(msg.getPriority());
        // can't set this...
//        if (msg.getRedelivered()) job.add("redelivered", msg.getRedelivered());
        // can't set the RGMID..!
//        if (msg.getReplicationGroupMessageId() != null) job.add("replicationGroupMessageId", msg.getReplicationGroupMessageId().toString());
        if (msg.isReplyMessage())							dstMsg.setAsReplyMessage(true);
        if (msg.getReplyTo() != null)						dstMsg.setReplyTo(msg.getReplyTo());
        if (msg.getSenderId() != null)						dstMsg.setSenderId(msg.getSenderId());
        if (msg.getSenderTimestamp() != null)				dstMsg.setSenderTimestamp(msg.getSenderTimestamp());
        if (msg.getSequenceNumber() != null)				dstMsg.setSequenceNumber(msg.getSequenceNumber());
        if (msg.getTimeToLive() > 0 && includeTtl)			dstMsg.setTimeToLive(msg.getTimeToLive());

        
        
        // properties
        if (msg.getProperties() != null)					dstMsg.setProperties(msg.getProperties());

        // binary payload portion
        if (msg.hasAttachment()) {
        	// need to use specific message type
        	if (msg instanceof TextMessage) ((TextMessage)dstMsg).setText(((TextMessage) msg).getText());
        	else if (msg instanceof MapMessage) ((MapMessage)dstMsg).setMap(((MapMessage) msg).getMap());
        	else if (msg instanceof StreamMessage) ((StreamMessage)dstMsg).setStream(((StreamMessage) msg).getStream());
        	else ((BytesMessage)dstMsg).setData(((BytesMessage) msg).getData());
        }
        	
        // XML payload
        if (msg.hasContent()) {  // try the XML portion of the payload (OLD SCHOOL!!!)
        	// one way
//        	byte[] attachment = new byte[msg.getContentLength()];
//        	msg.readContentBytes(attachment);
//        	dstMsg.writeAttachment(attachment);
        	// another way
        	dstMsg.writeBytes(msg.getBytes());  // this is apparently the XML portion
        	// switch from XML!
//        	dstMsg.setData(msg.getBytes());  // switch from XML portion to binary portion
        }
        
        // live testing...
        if ("republish".equals("republish")) {
	        JCSMPProperties props = new JCSMPProperties();
	        props.setProperty(JCSMPProperties.HOST, "localhost");
	        props.setProperty(JCSMPProperties.VPN_NAME, "default");
	        props.setProperty(JCSMPProperties.USERNAME, "aaron");
	        JCSMPSession session = f.createSession(props);
	        session.connect();
	        XMLMessageProducer producer = session.getMessageProducer(new JCSMPStreamingPublishCorrelatingEventHandler() {
				
				@Override
				public void responseReceivedEx(Object arg0) {
					// TODO Auto-generated method stub
				}
				
				@Override
				public void handleErrorEx(Object arg0, JCSMPException arg1, long arg2) {
					// TODO Auto-generated method stub
				}
			});
	        // can't republish received message!!!!
	        // 09:29:15.639 [Context_1_ConsumerDispatcher] WARN  com.solac.jcsmp.impl.flow.FlowHandleImpl - Exception occurred in async delivery for flow 0
	        // java.lang.IllegalAccessError: May not modify read-only message.
	        //         at com.solacesystems.jcsmp.impl.JCSMPXMLMessage.checkReadOnlyBeforeModify(JCSMPXMLMessage.java:1330) ~[sol-jcsmp-10.21.0.jar:?]
//	        producer.send(msg, f.createTopic("copy/orig"));
	        producer.send(dstMsg, f.createTopic("copy/copy"));
	        session.closeSession();
        }        
		return dstMsg;
	}

/*	
	private static SDTMap shallowCopyMap(SDTMap map) {
		SDTMap dstMap = f.createMap();
        try {
            for (String key : map.keySet()) {
                Object o = map.get(key);
                if (o instanceof String) {
                	dstMap.put
                    job.add(key, (String)o);
                } else if (o instanceof SDTMap) {
                    job.add(key, sdtMapToJson((SDTMap)o));
                } else if (o instanceof SDTStream) {
                    job.add(key, sdtStreamToJson((SDTStream)o));
                } else if (o instanceof Double) {
                    job.add(key, (Double)o);
                } else if (o instanceof Float) {
                    job.add(key, (Float)o);
                } else if (o instanceof Integer) {
                    job.add(key, (Integer)o);
                } else if (o instanceof Long) {
                    job.add(key, (Long)o);
                } else if (o instanceof Boolean) {
                    job.add(key, (Boolean)o);
                } else if (o instanceof Short) {
                    job.add(key, (Short)o);
                } else if (o instanceof Byte) {
                    job.add(key, (Byte)o);
                } else if (o instanceof ByteArray) {
                    job.add(key, new String(Base64.getEncoder().encode(((ByteArray)o).asBytes())));
                } else if (o instanceof Character) {
                    job.add(key, (Character)o);
                } else if (o instanceof Destination) {
                    job.add(key, ((Destination)o).getName());
                } else {
                    System.err.println("Unhandled type "+o.getClass().getName()+"!!  "+key+", "+o);
                }
            }
            
        } catch (SDTException e) {
            e.printStackTrace();
        }
		
		
		
		
		
		return dstMap;
	}
	
	
	private static SDTStream deepCopyStream(SDTStream srcStream) {
		SDTStream dstStream = f.createStream();
		
		
		
		
		
		return dstStream;
	}
*/	

	
	private SolaceMessageCopy() {
		throw new AssertionError("Utility class, don't instantiate");
	}
}
