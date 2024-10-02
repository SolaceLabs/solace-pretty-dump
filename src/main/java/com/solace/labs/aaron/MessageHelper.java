package com.solace.labs.aaron;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.zip.DataFormatException;
import java.util.zip.GZIPInputStream;
import java.util.zip.Inflater;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.fusesource.jansi.AnsiConsole;

import com.google.protobuf.MessageOrBuilder;
import com.solace.labs.topic.Sub;
import com.solacesystems.jcsmp.BytesMessage;
import com.solacesystems.jcsmp.BytesXMLMessage;
import com.solacesystems.jcsmp.Destination;
import com.solacesystems.jcsmp.MapMessage;
import com.solacesystems.jcsmp.StreamMessage;
import com.solacesystems.jcsmp.TextMessage;
import com.solacesystems.jcsmp.Topic;
import com.solacesystems.jcsmp.XMLContentMessage;

/**
 * A PayloadHelper contains a charset and an indent amount, and is used for 
 */
public class MessageHelper {
	
	
    private static final Logger logger = LogManager.getLogger(MessageHelper.class);
	
    private final ConfigState config;
    
	MessageHelper(ConfigState config) {
		this.config = config;
	}
	
	ConfigState getConfigState() {
		return config;
	}

	/** Only used by PrettyWrap */
	public PayloadSection buildPayloadSection(ByteBuffer payloadContents) {
    	int currentScreenWidth = AnsiConsole.getTerminalWidth();
    	if (currentScreenWidth == 0) currentScreenWidth = 80;
		PayloadSection payload = new PayloadSection(config);
		payload.formatByteBufferFromWrapMode(payloadContents);
		return payload;
	}
	
	


	// Destination:                            Topic 'solace/samples/jcsmp/hello/aaron'
	public static AaAnsi colorizeDestination(Destination jcsmpDestination) {
		AaAnsi ansi = AaAnsi.n().fg(Elem.PAYLOAD_TYPE).a("Destination:                            ").fg(Elem.DESTINATION);
		if (jcsmpDestination instanceof Topic) {
			ansi.a("Topic '").colorizeTopic(jcsmpDestination.getName(), -1).fg(Elem.DESTINATION).a("'");
		} else {  // queue
			ansi.a("Queue '").a(jcsmpDestination.getName()).a("'");
		}
		return ansi.reset();
	}
	
	// JMSDestination:                         Topic 'solace/json/test'
	/** Needs to bee the whole line!  E.g. "<code>JMSDestination:                         Topic 'solace/json/test'</code>" */
	public static AaAnsi colorizeDestinationString(String destinationLine) {
		AaAnsi ansi = AaAnsi.n().fg(Elem.PAYLOAD_TYPE).a(destinationLine.substring(0,40)).fg(Elem.DESTINATION);
		String rest = destinationLine.substring(40);
		if (rest.startsWith("Topic")) {
			ansi.a("Topic '").colorizeTopic(rest.substring(7, rest.length()-1), -1).fg(Elem.DESTINATION).a("'");
		} else {
			ansi.a("Queue '").a(rest.substring(7, rest.length()-1)).a("'");
		}
		return ansi.reset();
	}
	
	public enum PrettyMsgType {
        MAP("SDT MapMessage"),
        STREAM("SDT StreamMessage"),
        TEXT("SDT TextMessage"),
        XML("XML Content Message"),
        BYTES("Raw BytesMessage"),
        ;
		
		final String desc;
		
		private PrettyMsgType(String desc) {
			this.desc = desc;
		}
		
		@Override
		public String toString() {
			return desc;
		}
	}

	
	public boolean isStopped() {
		return config.isShutdown;
	}
	
	
	private void doBinaryPayload(BytesXMLMessage message, MessageObject ms, byte[] bytes) {
//		byte[] bytes = message.getAttachmentByteBuffer().array();
		if ("gzip".equals(message.getHTTPContentEncoding())) {
			GZIPInputStream gzip = null;
			ByteArrayOutputStream os = new ByteArrayOutputStream();
			try {
				gzip = new GZIPInputStream(new ByteArrayInputStream(bytes));
				int len = 0;
				byte[] buffer = new byte[1024];
				while ((len = gzip.read(buffer)) > 0) {
				    os.write(buffer, 0, len);
				}
				bytes = os.toByteArray();
				ms.msgType = "GZIPed " + ms.msgType;
			} catch (IOException e) {
				logger.warn("Had a message marked as 'gzip' but wasn't");
				logger.warn(message.dump());
				System.out.println(AaAnsi.n().ex("Had a message marked as 'gzip' but wasn't", e).toString());
			} finally {
				try {
					if (gzip != null) gzip.close();
					os.close();
				} catch (IOException e2) {
				}
			}
		} else if ("deflate".equals(message.getHTTPContentEncoding())) {
			Inflater inflater = new Inflater();
			ByteArrayOutputStream os = new ByteArrayOutputStream();
			try {
				inflater.setInput(bytes);
				byte[] buffer = new byte[1024];
				while (!inflater.finished()) {
					int decompressedSize = inflater.inflate(buffer);
					os.write(buffer, 0, decompressedSize);
				}
				bytes = os.toByteArray();
				ms.msgType = "DEFLATEd " + ms.msgType;
				String s = UsefulUtils.textMessageBytesToString(bytes);
				if (s != null) {
					ms.msgType = "DEFLATEd " + PrettyMsgType.TEXT.desc;
	            	ms.binary.formatString(s, bytes, message.getHTTPContentType(), false);
	            	return;  // all done, it's a deflated TextMessage
				}
			} catch (DataFormatException e) {
				logger.warn("Had a message marked as 'deflate' but wasn't");
				logger.warn(message.dump());
				System.out.println(AaAnsi.n().ex("Had a message marked as 'deflate' but wasn't", e).toString());
			} finally {
				try {
					os.close();
				} catch (IOException e2) {
				}
			}
		}

		boolean topicMatch = false;
		if (!config.rawPayload) {
			// Protobuf stuff...
			for (Entry<Sub,Method> entry : config.protobufCallbacks.entrySet()) {
    			Sub sub = entry.getKey();
    			String topic = message.getDestination().getName();
//			            			System.out.printf("Sub: %s, topic: %s%n", sub, topic);
//			            			System.out.println("Matches?  " + sub.matches(topic));
//			            			System.out.println("regex?  " + sub.pattern.matcher(topic).matches());
    			if (sub.matches(topic)) {
    				if (topicMatch == true) {  // we already found a match!
    					logger.error("Found multiple Protobuf subscriptions that matched " + topic);
    					return;  // done here, just log but don't overwrite
    				}
    				topicMatch = true;
    				Object o;
					try {
						o = entry.getValue().invoke(null, bytes);
						MessageOrBuilder protoMsg = (MessageOrBuilder)o;
						ms.binary.formatted = ProtoBufUtils.decode(protoMsg, config.getFormattingIndent());
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
		}
		if (!topicMatch) ms.binary.formatBytes(bytes, message.getHTTPContentType());  // didn't match anything
	}
	

    public void dealWithMessage(BytesXMLMessage message) {
    	if (isStopped()) return;
    	config.currentMsgCount++;
    	MessageObject ms = new MessageObject(this.config, message, config.currentMsgCount);
    	if (message instanceof XMLContentMessage) {
        	ms.msgType = PrettyMsgType.XML.toString();
        } else if (message instanceof MapMessage) {
        	ms.msgType = PrettyMsgType.MAP.toString();
        } else if (message instanceof StreamMessage) {
        	ms.msgType = PrettyMsgType.STREAM.toString();
        } else if (message instanceof TextMessage) {
			ms.msgType = PrettyMsgType.TEXT.toString();
        } else if (message instanceof BytesMessage) {
        	ms.msgType = PrettyMsgType.BYTES.toString();
        } else {  // shouldn't be anything else..?   Even JMS ObjectMessage arrives to JCSMP as BytesMessage & no way to check that payload bit/type
        	// leave as Impl class
        }
    	
    	// so at this point we know we know we will need the payload, so might as well try to parse it now
        try {  // want to catch SDT exceptions from the map and stream; payload string encoding issues now caught in format()
        	if (!config.noPayload || config.filterRegexPattern != null) {
	        	if (message.hasAttachment()) { // getAttachmentContentLength() > 0) {
	        		ms.binary = new PayloadSection(config);
	        		ms.binary.size = message.getAttachmentContentLength();
		            if (message instanceof MapMessage) {
	//	            	ms.msgType = "SDT MapMessage";
		            	ms.binary.formatted = SdtUtils.printMap(((MapMessage)message).getMap(), config.getFormattingIndent());
		            } else if (message instanceof StreamMessage) {
	//	            	ms.msgType = "SDT StreamMessage";
		            	// set directly
		            	ms.binary.formatted = SdtUtils.printStream(((StreamMessage)message).getStream(), config.getFormattingIndent());
		            } else {  // either text or binary, try/hope that the payload is a string, and then we can try to format it
			            if (message instanceof TextMessage) {
//			            	String pay = ((TextMessage)message).getText();
//			            	ByteBuffer bb = message.getAttachmentByteBuffer();
			            	byte[] bytes = message.getAttachmentByteBuffer().array();
			            	ms.binary.formatString(((TextMessage)message).getText(), bytes, message.getHTTPContentType(), false);
	//		            	ms.msgType = ms.binary.formatted.getCharCount() == 0 ? "<EMPTY> SDT TextMessage" : "SDT TextMessage";
			            	if (ms.binary.formatted.getTotalCharCount() == 0) {  // looks like an empty text message, but could be malformed
			            		int len = message.getAttachmentContentLength();
			            		// let's validate it (knowing a bit about how SDT TextMessages are formatted on the wire)
			            		if ((len == 3 && bytes[0] == 0x1c && bytes[1] == 0x03 && bytes[2] == 0x00) ||   // JCSMP empty String
			            				(len == 6 && bytes[0] == 0x1f && bytes[1] == 0x00 && bytes[2] == 0x00 && bytes[3] == 0x00 && bytes[4] == 0x06 && bytes[5] == 0x00)) {  // CCSMP empty String
//			            			ms.msgType = "<EMPTY> SDT TextMessage";
			            			ms.binary.type = "<EMPTY STRING> SDT TextMessage";
//			            			ms.binary = null;  // blank it out
			            		} else {  // invalid!!
//			            			ms.msgType = "*malformed* SDT TextMessage";
			            			ms.binary.type = "INVALID *malformed* SDT TextMessage";
			            			int currentScreenWidth = AnsiConsole.getTerminalWidth();
			            			if (currentScreenWidth == 0) currentScreenWidth = 80;
			            			ms.binary.formatted = UsefulUtils.printBinaryBytesSdkPerfStyle(bytes, config.getFormattingIndent(), currentScreenWidth);
			            		}
			            	} else {  // should be valid! maybe check if it's properly formatted, UTF-8
//			    	        	ms.msgType = PrettyMsgType.TEXT.toString();  // shouldn't need to do this?
			            	}
			            } else {  // bytes message
	//		            	ms.msgType = "Raw BytesMessage";
			            	if (message.getAttachmentByteBuffer() != null) {
			            		doBinaryPayload(message, ms, message.getAttachmentByteBuffer().array());
		                    }
			            }
		            }
	        	}
	            if (message.hasContent()) {  // try the XML portion of the payload (OLD SCHOOL!!!)
	            	ms.xml = new PayloadSection(config);   // TODO could there be Protobuf stuff in this section??
//	            	if (message instanceof XMLContentMessage) {
//	            		ms.xml.formatString(((XMLContentMessage)message).getXMLContent(), message.getBytes(), message.getHTTPContentType(), false);
//	            	} else {
	            		// don't try to use getXMLContent() in case it's binary
	            		ms.xml.formatBytes(message.getBytes(), message.getHTTPContentType());
//	            	}
	            }
        	}
            if (message.getProperties() != null && !message.getProperties().isEmpty()) {
            	ms.userProps = new PayloadSection(config);
				ms.userProps.formatted = SdtUtils.printMap(message.getProperties(), config.getFormattingIndent());
            	ms.userProps.numElements = SdtUtils.countElements(message.getProperties());
            }
            if (message.getUserData() != null && message.getUserData().length > 0) {
            	ms.userData = new PayloadSection(config);
            	if (!config.isOneLineMode()) ms.userData.formatBytes(message.getUserData(), null);
            	ms.userData.type = null;
            }
            
            // done preparing the message.  Now we might have to filter it?
            if (config.filterRegexPattern != null) {
            	String rawDump = ms.buildFullStringObject();
            	if (!config.filterRegexPattern.matcher(rawDump).find()) {  // no match
            		config.incFilteredCount();
					if (config.isLastNMessagesEnabled()) {  // gathering
						ThinkingAnsiHelper.tick2(ThinkingAnsiHelper.makeStringGathered("Msg filtered.",
								config.getMessageCount(), config.getFilteredCount(), config.getMessageCount()-config.getFilteredCount(), config.getLastNMessagesCapacity()));
					} else {
						ThinkingAnsiHelper.tick2(ThinkingAnsiHelper.makeStringPrinted("Msg filtered.",
								config.getMessageCount(), config.getFilteredCount(), config.getMessageCount()-config.getFilteredCount()));
					}
//            		if (lastNMessages == null) ThinkingAnsiHelper.tick();
//            		else ThinkingAnsiHelper.tick(String.format("%d messages gathered, skipping filtered msg #", lastNMessages.size()));
                	return;
            	} else {
//            		System.out.println(rawDump);
            		ThinkingAnsiHelper.filteringOff();
            	}
            }
            
            // TODO only update the spacing when we're actually printing it out?
//            ms.updateTopicSpacing();  // now that we've determined if we're gonna filter this message, do the topic stuff

            // now it's time to try printing it!
//            if (lastNMessages != null) {
			if (config.isLastNMessagesEnabled()) {  // gathering
				config.lastNMessagesList.add(ms);
				ThinkingAnsiHelper.tick2(ThinkingAnsiHelper.makeStringGathered("",
						config.getMessageCount(), config.getFilteredCount(), config.getMessageCount()-config.getFilteredCount(), config.getLastNMessagesCapacity()));
//            	ThinkingAnsiHelper.tick("Gathering last " + lastNMessages.capacity() + " messages, received ");
            } else {
            	if (isStopped()) return;  // stop if we're stopped!
            	if (ThinkingAnsiHelper.isFilteringOn()) ThinkingAnsiHelper.filteringOff();
                SystemOutHelper systemOut = ms.printMessage();
            	System.out.print(systemOut);
            	if (config.filterRegexPattern != null) {  // there is some filtering
					ThinkingAnsiHelper.tick2(ThinkingAnsiHelper.makeStringPrinted("",
							config.getMessageCount(), config.getFilteredCount(), config.getMessageCount()-config.getFilteredCount()));
            	}
            }
        } catch (RuntimeException e) {  // really shouldn't happen!!
        	System.out.println(ms.printMessageStart());
        	System.out.println(AaAnsi.n().ex("Exception occured, check ~/.pretty/pretty.log for details. ", e));
        	logger.warn("Had issue parsing a message.  Message follows after exception.",e);
        	logger.warn(message.dump());
        	System.out.println(message.dump());
        	System.out.println(ms.printMessageEnd());
        }
    }
    
    

    
}
