package com.solace.labs.aaron;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.fusesource.jansi.AnsiConsole;

import com.google.protobuf.MessageOrBuilder;
import com.solace.labs.topic.Sub;
import com.solacesystems.jcsmp.BytesMessage;
import com.solacesystems.jcsmp.BytesXMLMessage;
import com.solacesystems.jcsmp.DeliveryMode;
import com.solacesystems.jcsmp.Destination;
import com.solacesystems.jcsmp.MapMessage;
import com.solacesystems.jcsmp.Queue;
import com.solacesystems.jcsmp.StreamMessage;
import com.solacesystems.jcsmp.TextMessage;
import com.solacesystems.jcsmp.Topic;
import com.solacesystems.jcsmp.XMLContentMessage;
import com.solacesystems.jcsmp.XMLMessage;
import com.solacesystems.jcsmp.impl.sdt.MapImpl;
import com.solacesystems.jcsmp.impl.sdt.MapTLVBuffer;
import com.solacesystems.jcsmp.impl.sdt.StreamImpl;
import com.solacesystems.jcsmp.impl.sdt.StreamTLVBuffer;

/**
 * A PayloadHelper contains a charset and an indent amount, and is used for 
 */
public class PayloadHelper {

	
	
//	private static Map<Charset, CharsetDecoder> mapOfDecoders = new ConcurrentHashMap<>();  // why do this?  I only need one..?
	
    private static final Logger logger = LogManager.getLogger(PayloadHelper.class);
	
    
    private final Charset charset;
	private final CharsetDecoder decoder;
    Map<Sub, Method> protobufCallbacks = new HashMap<>();
    long msgCount = 0;
    static int currentScreenWidth = 80;  // assume for now

	
    int INDENT = 4;  // default starting value
    boolean noPayload = false;
    private boolean autoResizeIndent = false;  // specify -1 as indent for this MODE
    boolean autoSpaceTopicLevels = false;  // specify +something to space out the levels
    boolean autoTrimPayload = false;
    private LinkedListOfIntegers maxLengthTopicsList = new LinkedListOfIntegers();
    
    private List<LinkedListOfIntegers> levelLengths = new ArrayList<>();
    
	public PayloadHelper(Charset charset) {
		this.charset = charset;
		decoder = this.charset.newDecoder().onMalformedInput(CodingErrorAction.REPLACE).onUnmappableCharacter(CodingErrorAction.REPLACE);
		
		
	}
	
    
	
    // this method tracks what the longest topic string has been for the last 50 messages, so things line up nicely with indent mode "-1"
    private void updateTopicIndentValue(int maxTopicLength) {
    	maxLengthTopicsList.insert(maxTopicLength);
    	if (maxLengthTopicsList.getMax() + 2 != Math.abs(INDENT)) {  // changed our current max
//    		int from = Math.abs(INDENT);
    		INDENT = -1 * (maxLengthTopicsList.getMax() + 2);
//    		System.out.println(new Ansi().reset().a(Attribute.INTENSITY_FAINT).a("** changing INDENT from " + from + " to " + Math.abs(INDENT) + "**").reset().toString());
    	}
    }
    
    
    private String updateTopicSpaceOutLevels(String topic) {
    	String[] levels = topic.split("/");
    	if (levelLengths.size() < levels.length) {
    		for (int i=levelLengths.size(); i < levels.length; i++) {
    			levelLengths.add(new LinkedListOfIntegers(500));
    		}
    	}
    	for (int i=0; i < levelLengths.size(); i++) {
    		if (i < levels.length) {
    			levelLengths.get(i).insert(levels[i].length());
    		} else {
    			levelLengths.get(i).insert(0);
    		}
    	}
    	StringBuilder sb = new StringBuilder();
    	for (int i=0; i < levels.length; i++) {
    		sb.append(levels[i]);
    		int max = levelLengths.get(i).getMax();
    		if (i < levels.length-1) {
	    		if (max > levels[i].length()) {
					sb.append(UsefulUtils.pad(max - levels[i].length(), '⋅' /* '·' */));
	    		}
	    		if (INDENT == Integer.MIN_VALUE) sb.append("⋅/");
	    		else sb.append('/');
	    	}
    	}
    	return sb.toString();
    }
    
    
    

    
    void dealWithIndentParam(String indentStr) throws NumberFormatException {
    	if (indentStr.startsWith("+") && indentStr.length() >= 2) {
    		autoSpaceTopicLevels = true;
    		indentStr = "-" + indentStr.substring(1);
    	}
		int indent = Integer.parseInt(indentStr);
		if (indent < -250 || indent > 20) throw new NumberFormatException();
		INDENT = indent;
		if (INDENT < 0) {
			if (INDENT == -1) {
				autoResizeIndent = true;  // use auto-resizing based on max topic length
				INDENT = -3;  // starting value (1 + 2 for padding)
				updateTopicIndentValue(1);  // now update it
			} else {
				updateTopicIndentValue(Math.abs(INDENT));  // now update it
			}
		} else if (INDENT == 0) {
			if (indentStr.equals("-0")) {  // special case, print topic only
				INDENT = Integer.MIN_VALUE;
			} else if (indentStr.equals("00")) {
				noPayload = true;
				INDENT = 2;
			}
		}
    }
	
	
	
//	private PayloadHelper(Charset charset, int indent) {
//		this.charset = charset;
//		this.decoder = charset.newDecoder().onMalformedInput(CodingErrorAction.REPLACE).onUnmappableCharacter(CodingErrorAction.REPLACE);
//		if (!mapOfDecoders.containsKey(charset)) {
//			mapOfDecoders.put(charset, charset.newDecoder().onMalformedInput(CodingErrorAction.REPLACE).onUnmappableCharacter(CodingErrorAction.REPLACE));
//		}
//	}
	
	
	
	public String decodeToString(byte[] bytes) { 
        CharBuffer cb;
		try {
			// won't throw since the decoder is "REPLACE" and not "REPORT"
//			cb = mapOfDecoders.get(charset).decode(ByteBuffer.wrap(bytes));
			cb = decoder.decode(ByteBuffer.wrap(bytes));
			return cb.toString();
		} catch (CharacterCodingException e) {
			// can't get here now
			throw new AssertionError("Could not decode bytes to charset",e);
		}
	}

	public String decodeToString(ByteBuffer buffer) { 
		try {
			// won't throw since the decoder is "REPLACE" and not "REPORT"
//			CharBuffer cb = mapOfDecoders.get(charset).decode(buffer);
			CharBuffer cb = decoder.decode(buffer);
			return cb.toString();
		} catch (CharacterCodingException e) {
			// can't get here now
			throw new AssertionError("Could not decode bytes to charset",e);
		}
	}


//	public String parseAsJson(String s) {
//		return "";
//	}
	
	
	
/*
	public class PayloadSection2 {
		
		private String type = null;  // might initialize later if JSON or XML
		private String formatted = "<UNINITIALIZED>";  // to ensure gets overwritten
		
		private String rawString = null;
		private byte[] rawBytes = null;
		
	}
	*/

	public PayloadSection buildPayloadSection(ByteBuffer payloadContents) {
    	currentScreenWidth = AnsiConsole.getTerminalWidth();
		PayloadSection payload = new PayloadSection();
		payload.formatByteBufferFromWrapMode(payloadContents);
		return payload;
	}
	
//	public PayloadSection buildPayloadSection(byte[] bytes) {
//		PayloadSection payload = new PayloadSection();
//		payload.formatBytes(bytes);
//		return payload;
//	}
	
	public class PayloadSection {  // like, the XML payload and the binary payload; but also the user props (map) and user data (binary) could use this

		String type = null;  // might initialize later if JSON or XML
		AaAnsi formatted = AaAnsi.n().fg(Elem.NULL).a("<UNINITIALIZED>");  // to ensure gets overwritten
		
		public String getType() {
			return type;
		}
		
		public String getFormattedPayload() {
			return formatted.toString();
		}
		
		void formatString(final String text) {
			formatString(text, null);
		}
		
		void formatString(final String text, final String contentType) {
			if (text == null || text.isEmpty()) {
				formatted = AaAnsi.n();
				return;
			}
			String trimmed = text.trim();
        	if ((trimmed.startsWith("{") && trimmed.endsWith("}")) || "application/json".equals(contentType)) {  // try JSON object
        		try {
            		formatted = GsonUtils.parseJsonObject(trimmed, Math.max(INDENT, 0));
        			type = charset.displayName() + " charset, JSON Object";
				} catch (IOException e) {
        			type = charset.displayName() + " charset, INVALID JSON payload";
//        			formatted = new AaAnsi().setError().a("ERROR: ").a(e.getMessage()).reset().a('\n').a(text).reset().toString();
        			formatted = new AaAnsi().ex(e).a('\n').a(trimmed);
				}
        	} else if ((trimmed.startsWith("[") && trimmed.endsWith("]")) || "application/json".equals(contentType)) {  // try JSON array
        		try {
            		formatted = GsonUtils.parseJsonArray(trimmed, Math.max(INDENT, 0));
        			type = charset.displayName() + " charset, JSON Array";
        		} catch (IOException e) {
        			type = charset.displayName() + " charset, INVALID JSON payload";
//        			formatted = new AaAnsi().setError().a("ERROR: ").a(e.getMessage()).reset().a('\n').a(text).reset().toString();
        			formatted = new AaAnsi().ex(e).a('\n').a(trimmed);
				}
        	} else if ((trimmed.startsWith("<") && trimmed.endsWith(">")) ||
        			"application/xml".equals(contentType)  || "text/xml".equals(contentType)) {  // try XML
    			try {
    				SaxHandler handler = new SaxHandler(INDENT);
					SaxParser.parseString(trimmed, handler);
                    formatted = handler.getResult();  // overwrite
                    type = charset.displayName() + " charset, XML document";
				} catch (SaxParserException e) {
					logger.error("Couldn't parse xml", e);
        			type = charset.displayName() + " charset, INVALID XML payload";
        			formatted = new AaAnsi().ex(e).a('\n').a(trimmed);
				}
        	} else {  // it's neither JSON or XML, but has text content
//        		type = charset.displayName() + " String";
        		type = charset.displayName() + " encoded string";
        		formatted = new AaAnsi().aStyledString(trimmed).reset();
//        		formatted = text;
        	}
		}

		void formatBytes(byte[] bytes, String contentType) {
			String parsed = decodeToString(bytes);
			boolean malformed = parsed.contains("\ufffd");
			formatString(parsed, contentType);  // call the String version
        	if (malformed) {
				type = "Non " + type;
        	} else {
        		type = "valid " + type;
        	}
        	double ratio = (1.0 * formatted.controlChars + formatted.replacementChars) / formatted.getCharCount();
//        	System.out.printf("Ratio for (controlChars %d + replaceChars %d) / charCount %d == %f%n",
//        			formatted.controlChars, formatted.replacementChars, formatted.getCharCount(), ratio);
        	if (malformed && ratio > 0.2) {  // 20%, very likely a binary file
				formatted = UsefulUtils.printBinaryBytesSdkPerfStyle(bytes, INDENT, currentScreenWidth);
        	} else if (malformed || formatted.controlChars > 0) {  // any unusual control chars (not tab, LF, CR, FF, or Esc, or NUL at string end
				if (INDENT > 0) {
					formatted.a('\n').a(UsefulUtils.printBinaryBytesSdkPerfStyle(bytes, INDENT, currentScreenWidth));
				}
        	}
		}	

    	void formatByteBufferFromWrapMode(ByteBuffer buffer) {
			byte first = buffer.get();  // check out the first byte
			if (first == 0x1c) {  // text message, one byte of size
				int size = Byte.toUnsignedInt(buffer.get());
				if (buffer.limit() != size) throw new IllegalArgumentException("size byte ("+size+") did not match buffer limit ("+buffer.limit()+")!");
				buffer.limit(buffer.limit()-1);  // text messages are null terminated, so don't read the trailing null
//				System.out.println("0x1c SDT TextMessage detected, byte lenght: " + size);
			} else if (first == 0x1d) {  // text message, 2 bytes of size
				int size = Short.toUnsignedInt(buffer.getShort());
				if (buffer.limit() != size) throw new IllegalArgumentException("size bytes ("+size+") did not match buffer limit ("+buffer.limit()+")!");
//				System.out.println("0x1d SDT TextMessage detected, byte lenght: " + size);
				buffer.limit(buffer.limit()-1);  // text messages are null terminated, so don't read the trailing null
			} else if (first == 0x1e) {  // text message, 3 bytes of size
				int size = Byte.toUnsignedInt(buffer.get()) << 16;
				size |= buffer.getShort();
				if (buffer.limit() != size) throw new IllegalArgumentException("size bytes ("+size+") did not match buffer limit ("+buffer.limit()+")!");
				buffer.limit(buffer.limit()-1);  // text messages are null terminated, so don't read the trailing null
//				System.out.println("0x1e SDT TextMessage detected, byte lenght: " + size);
			} else if (first == 0x1f) {  // text message, 4 bytes of size
				int size = buffer.getInt();
				if (buffer.limit() != size) throw new IllegalArgumentException("size bytes ("+size+") did not match buffer limit ("+buffer.limit()+")!");
				buffer.limit(buffer.limit()-1);  // text messages are null terminated, so don't read the trailing null
//				System.out.println("0x1f SDT TextMessage detected, byte lenght: " + size);
			} else if (first == 0x2b) {  // SDT Map?
				// check the size matches correctly:
				int size = buffer.getInt();  // next 4 bytes
//				System.out.println("size bytes ("+size+") and buffer limit ("+buffer.limit()+")!");
				if (buffer.limit() == size) {  // looks correct!  otherwise maybe just a regular binary msg
					byte[] copy = Arrays.copyOfRange(buffer.array(), 0, buffer.limit());
					MapTLVBuffer buf = new MapTLVBuffer(copy);  // sneaky hidden but public methods
					MapImpl map = new MapImpl(buf);
//					String test = SdtUtils.printMap(map, 3).toString();
					formatted = SdtUtils.printMap(map, INDENT);
	            	type = "SDT MapMessage";
					return;
    			} else {
    				buffer.rewind();  // put back to beginning
    			}
			} else if (first == 0x2f) {  // SDT Stream?
				// check the size matches correctly:
				int size = buffer.getInt();  // next 4 bytes
//				System.out.println("size bytes ("+size+") and buffer limit ("+buffer.limit()+")!");
				if (buffer.limit() == size) {  // looks correct!  otherwise maybe just a regular binary msg
					byte[] copy = Arrays.copyOfRange(buffer.array(), 0, buffer.limit());
					StreamTLVBuffer buf = new StreamTLVBuffer(copy);  // sneaky hidden but public methods
					StreamImpl map = new StreamImpl(buf);
//					String test = SdtUtils.printMap(map, 3).toString();
					formatted = SdtUtils.printStream(map, INDENT);
	            	type = "SDT StreamMessage";
					return;
    			} else {
    				buffer.rewind();  // put back to beginning
    			}
			} else {
				buffer.rewind();  // put back to beginning
			}
			// if we're here, then either a TextMessage that we've shifted around or binary message ready to be read
//			pos = buffer.position();
//			limit = buffer.limit();
//			capacity = buffer.capacity();
//			System.out.printf("pos: %d, lim: %d, cap: %d%n", pos, limit, capacity);
			String parsed = decodeToString(buffer);
			boolean malformed = parsed.contains("\ufffd");
			formatString(parsed);  // call the String version
        	if (malformed) {
				type = "Non " + type;
				if (INDENT > 0) {
					formatted.a('\n').a(UsefulUtils.printBinaryBytesSdkPerfStyle(Arrays.copyOfRange(buffer.array(), 0, buffer.limit()), INDENT, currentScreenWidth));
				}
        	}
		}	

	}
	
	private class MessageHelperObject {
		
		final BytesXMLMessage orig;
    	String msgDestName;  // this would only be used in the 1-line version
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
//        	if (orig.getDestination() instanceof Queue) {
//        		msgDestName = "Queue '" + orig.getDestination().getName() + "'";
//        		ansi.a("Queue '" + orig.getDestination().getName() + "'");
//        	} else {  // a Topic
//        		msgDestName = orig.getDestination().getName();
//        		ansi.colorizeTopic(orig.getDestination().getName());
//        	}
        	if (orig.getDestination() instanceof Queue) {
        		msgDestName = "Queue '" + orig.getDestination().getName() + "'";
            	if (autoResizeIndent) updateTopicIndentValue(msgDestName.length());
            	else if (INDENT < 0 && msgDestName.length() > Math.abs(INDENT) - 1) {  // too long, need to trim it
//    				msgDestName = 
            		// let's not trim the queue name... leave it
    			}
        		ansi.a(msgDestName);
        	} else {  // a Topic
        		msgDestName = orig.getDestination().getName();
        		if (autoSpaceTopicLevels) {
        			msgDestName = updateTopicSpaceOutLevels(msgDestName);
        		}
            	if (autoResizeIndent) {
            		updateTopicIndentValue(msgDestName.length());
            	} else if (INDENT < 0 && msgDestName.length() > Math.abs(INDENT) - 1) {  // too long, need to trim it
            		msgDestName = msgDestName.substring(0, Math.abs(INDENT)-2) + "…";
    			}
        		ansi.colorizeTopic(msgDestName, highlightTopicLevel);
        	}
        	msgDestNameFormatted = ansi.toString();
//        	if (autoResizeIndent) updateTopicIndentValue(msgDestName.length());
        }
	}
	
	static int highlightTopicLevel = -1;
	
    
    // Helper class, for printing message to the console ///////////////////////////////////////

	private static final int DIVIDER_LENGTH = 60;  // same as SdkPerf JCSMP

	public String printMessageStart() {
        String head = "^^^^^ Start Message #" + ++msgCount + " ^^^^^";
        String headPre = UsefulUtils.pad((int)Math.ceil((DIVIDER_LENGTH - head.length()) / 2.0) , '^');
        String headPost = UsefulUtils.pad((int)Math.floor((DIVIDER_LENGTH - head.length()) / 2.0) , '^');
        return new AaAnsi().fg(Elem.MSG_BREAK).a(headPre).a(head).a(headPost).reset().toString();
	}
	
	public String printMessageEnd() {
        String end = " End Message #" + msgCount + " ";
        String end2 = UsefulUtils.pad((int)Math.ceil((DIVIDER_LENGTH - end.length()) / 2.0) , '^');
        String end3 = UsefulUtils.pad((int)Math.floor((DIVIDER_LENGTH - end.length()) / 2.0) , '^');
        return new AaAnsi().fg(Elem.MSG_BREAK).a(end2).a(end).a(end3).reset().toString();
	}

	// Destination:                            Topic 'solace/samples/jcsmp/hello/aaron'
	public static String colorizeDestination(Destination jcsmpDestination) {
		AaAnsi aa = new AaAnsi().fg(Elem.PAYLOAD_TYPE).a("Destination:                            ").fg(Elem.DESTINATION);
		if (jcsmpDestination instanceof Topic) {
			aa.a("Topic '").colorizeTopic(jcsmpDestination.getName(), highlightTopicLevel).fg(Elem.DESTINATION).a("'");
		} else {  // queue
			aa.a("Queue '").a(jcsmpDestination.getName()).a("'");
		}
		return aa.reset().toString();
	}
	
	// JMSDestination:                         Topic 'solace/json/test'
	/** Needs to bee the whole line!  E.g. "<code>JMSDestination:                         Topic 'solace/json/test'</code>" */
	public static String colorizeDestination(String destinationLine) {
		AaAnsi aa = new AaAnsi().fg(Elem.PAYLOAD_TYPE).a(destinationLine.substring(0,40)).fg(Elem.DESTINATION);
		String rest = destinationLine.substring(40);
		if (rest.startsWith("Topic")) {
			aa.a("Topic '").colorizeTopic(rest.substring(7, rest.length()-1), highlightTopicLevel).fg(Elem.DESTINATION).a("'");
		} else {
			aa.a("Queue '").a(rest.substring(7, rest.length()-1)).a("'");
		}
		return aa.reset().toString();
	}


    public void dealWithMessage(BytesXMLMessage message) {
    	currentScreenWidth = AnsiConsole.getTerminalWidth();
    	MessageHelperObject ms = new MessageHelperObject(message);
        // if doing topic only, or if there's no payload in compressed (<0) MODE, then just print the topic
    	if (INDENT == Integer.MIN_VALUE || (INDENT < 0 && !message.hasContent() && !message.hasAttachment())) {
//    		System.out.println(new AaAnsi().fg(Elem.DESTINATION).aRaw(ms.msgDestNameFormatted));
    		System.out.println(ms.msgDestNameFormatted);
//                if (browser == null && message.getDeliveryMode() != DeliveryMode.DIRECT) message.ackMessage();  // if it's a queue
            return;
    	}
        if (message instanceof MapMessage) {
        	ms.msgType = "SDT MapMessage";
        } else if (message instanceof StreamMessage) {
        	ms.msgType = "SDT StreamMessage";
        } else if (message instanceof TextMessage) {
        	ms.msgType = "SDT TextMessage";
        } else if (message instanceof XMLContentMessage) {
        	ms.msgType = "XML Content Message";
        } else if (message instanceof BytesMessage) {
        	ms.msgType = "Raw BytesMessage";
        } else {  // shouldn't be anything else..?
        	// leave as Impl class
        }

    	
    	
    	// so at this point we know we know we will need the payload, so might as well try to parse it now
        try {  // want to catch SDT exceptions from the map and stream; payload string encoding issues now caught in format()
        	if (message.getAttachmentContentLength() > 0) {
        		ms.binary = new PayloadSection();
	            if (message instanceof MapMessage) {
//	            	ms.msgType = "SDT MapMessage";
	            	ms.binary.formatted = SdtUtils.printMap(((MapMessage)message).getMap(), Math.max(INDENT, 0));
	            } else if (message instanceof StreamMessage) {
//	            	ms.msgType = "SDT StreamMessage";
	            	// set directly
	            	ms.binary.formatted = SdtUtils.printStream(((StreamMessage)message).getStream(), Math.max(INDENT, 0));
	            } else {  // either text or binary, try/hope that the payload is a string, and then we can try to format it
		            if (message instanceof TextMessage) {
		            	ms.binary.formatString(((TextMessage)message).getText(), message.getHTTPContentType());
		            	ms.msgType = ms.binary.formatted.getCharCount() == 0 ? "Empty SDT TextMessage" : "SDT TextMessage";
		            } else {  // bytes message
//		            	ms.msgType = "Raw BytesMessage";
		            	if (message.getAttachmentByteBuffer() != null) {  // should be impossible since content length > 0
		            		byte[] bytes = message.getAttachmentByteBuffer().array();

		            		// Protobuf stuff...
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
		            		if (!topicMatch) ms.binary.formatBytes(bytes, message.getHTTPContentType());
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
            	ms.xml.formatBytes(message.getBytes(), message.getHTTPContentType());
            }
            if (message.getProperties() != null && !message.getProperties().isEmpty()) {
            	ms.userProps = new PayloadSection();
            	ms.userProps.formatted = SdtUtils.printMap(message.getProperties(), INDENT);
            }
            if (message.getUserData() != null && message.getUserData().length > 0) {
            	ms.userData = new PayloadSection();
        		String simple = decodeToString(message.getUserData());
        		AaAnsi ansi = new AaAnsi().fg(Elem.STRING).a(simple).reset();
            	if (simple.contains("\ufffd")) {
            		ansi.a('\n').a(UsefulUtils.printBinaryBytesSdkPerfStyle(message.getUserData(), INDENT, currentScreenWidth));
            	}
        		ms.userData.formatted = ansi;
            }
            
            // now it's time to try printing it!
            if (INDENT >= 0) {
            	System.out.println(printMessageStart());
	            String[] headerLines = message.dump(XMLMessage.MSGDUMP_BRIEF).split("\n");
	            headerLines[0] = colorizeDestination(message.getDestination());
                for (String line : headerLines) {
                	if (line.isEmpty() || line.matches("\\s*")) continue;  // testing 
					if (line.startsWith("User Property Map:") && ms.userProps != null) {
                		System.out.println(new AaAnsi().a(line));
                		System.out.println(ms.userProps.formatted);
                		if (INDENT > 0 && !noPayload) System.out.println();
                	} else if (line.startsWith("User Data:") && ms.userData != null) {
                		System.out.println(new AaAnsi().a(line));
                		System.out.println(ms.userData.formatted);
                		if (INDENT > 0 && !noPayload) System.out.println();
                	} else if (line.startsWith("SDT Map:") || line.startsWith("SDT Stream:")) {
                		// skip (handled as part of the binary attachment)
                	} else if (line.startsWith("Binary Attachment:")) {
//                		String combined = ms.msgType + (ms.binary.type == null ? "" : ", " + ms.binary.type) + ":";
                		StringBuilder sb = new StringBuilder(ms.msgType).append((ms.binary.type == null ? "" : ", " + ms.binary.type)).append(':');
//                		AaAnsi payloadType = AaAnsi.n();
//                		if (combined.contains("Non ") || combined.contains("INVALID")) payloadType.invalid(combined);
//						else payloadType.fg(Elem.PAYLOAD_TYPE).a(combined);
                		if (noPayload) {
//                			combined = "Binary Attachment: " + combined + " " + ms.orig.getAttachmentContentLength() + " bytes";
                			sb.insert(0, "Binary Attachment: ").append(' ').append(ms.orig.getAttachmentContentLength()).append(" bytes");
//                			payloadType.a(" " + ms.orig.getAttachmentContentLength() + " bytes");
//                			System.out.println("Binary Attachment: " + payloadType);
	                		if (sb.toString().contains("Non ") || sb.toString().contains("INVALID")) System.out.println(new AaAnsi().invalid(sb.toString()).reset());
							else System.out.println(new AaAnsi().fg(Elem.PAYLOAD_TYPE).a(sb.toString()).reset());
                		} else {
	                		System.out.println(new AaAnsi().a(line));
//	                		String combined = ms.msgType + (ms.binary.type == null ? "" : ", " + ms.binary.type) + ":";
//	                		if (combined.contains("Non ")) System.out.println(new AaAnsi().invalid(combined));
//							else System.out.println(new AaAnsi().fg(Elem.PAYLOAD_TYPE).a(combined));
	                		if (sb.toString().contains("Non ") || sb.toString().contains("INVALID")) System.out.println(new AaAnsi().invalid(sb.toString()).reset());
							else System.out.println(new AaAnsi().fg(Elem.PAYLOAD_TYPE).a(sb.toString()).reset());
	                		System.out.println(ms.binary.formatted);
	                		if (INDENT > 0) System.out.println();
                		}
                	} else if (line.startsWith("XML:")) {
//                		if (noPayload) {
//                			
//                		} else {
//	                		line = line.replace("XML:                                   ", "XML Payload section being used:        ");
//	                		System.out.println(new AaAnsi().a(line));
//	                		if (ms.xml.type != null) {
//		                		System.out.println(new AaAnsi().fg(Elem.PAYLOAD_TYPE).a(ms.xml.type + ":"));
//	                		}
//	                		System.out.println(ms.xml.formatted);
//	                		if (INDENT > 0) System.out.println();
//                		}
                		
                		
                		if (noPayload) {
                			StringBuilder sb = new StringBuilder(ms.xml.type).append(':');
                			sb.insert(0, "XML Payload section: ").append(' ').append(ms.orig.getContentLength()).append(" bytes");
	                		if (sb.toString().contains("Non ") || sb.toString().contains("INVALID")) System.out.println(new AaAnsi().invalid(sb.toString()).reset());
							else System.out.println(new AaAnsi().fg(Elem.PAYLOAD_TYPE).a(sb.toString()).reset());
                		} else {
	                		line = line.replace("XML:                                   ", "XML Payload section:                   ");
	                		System.out.println(new AaAnsi().a(line));
                			StringBuilder sb = new StringBuilder(UsefulUtils.capitalizeFirst(ms.xml.type)).append(':');
	                		if (sb.toString().contains("Non ") || sb.toString().contains("INVALID")) System.out.println(new AaAnsi().invalid(sb.toString()).reset());
							else System.out.println(new AaAnsi().fg(Elem.PAYLOAD_TYPE).a(sb.toString()).reset());
	                		System.out.println(ms.xml.formatted);
	                		if (INDENT > 0) System.out.println();
                		}
                		
                	} else if (line.contains("Destination:           ")) {  // contains, not startsWith, due to ANSI codes
                		System.out.println(line);  // just print out since it's already formatted
                	} else if (line.startsWith("Message Id:") && message.getDeliveryMode() == DeliveryMode.DIRECT) {
                		// skip it, hide the auto-generated message ID on Direct messages
                	} else {  // everything else
//                		System.out.println(new AaAnsi().a(line));
//                		System.out.println(AaAnsi.n().a(UsefulUtils.guessIfMapLookingThing(line)));
                		System.out.println(UsefulUtils.guessIfMapLookingThing(line));
                	}
                }
                if (!message.hasContent() && !message.hasAttachment()) {
                	System.out.println(new AaAnsi().fg(Elem.PAYLOAD_TYPE).a(ms.msgType).a(", <EMPTY PAYLOAD>").reset().toString());
                }
                if (INDENT > 0 && !noPayload) {  // don't print closing bookend if indent==0
                	System.out.println(printMessageEnd());
                }
        	} else {  // INDENT < 0, one-line mode!
        		if (ms.binary != null && ms.xml != null) {
        			// that's not great for one-line printing!  but probably pretty rare!!!!
        			System.out.println("Message contains both binary and XML payloads:");
        			System.out.println(message.dump().trim());  // raw JCSMP full dump
        		} else if (ms.binary != null) {
    				System.out.print(ms.msgDestNameFormatted);
    				int spaceToAdd = Math.abs(INDENT) - ms.msgDestName.length();
    				System.out.print(UsefulUtils.pad(spaceToAdd, ' '));
    				if (autoTrimPayload) System.out.println(ms.binary.formatted.trim(currentScreenWidth - Math.abs(INDENT) - 1));
    				else {
    					System.out.println(ms.binary.formatted);
//    					System.out.printf("charCount=%d, calcLength=%d, width=%d, size=%d%n", ms.binary.formatted.getCharCount(), AaAnsi.length(ms.binary.formatted.toString()),currentScreenWidth,Math.abs(INDENT)+ms.binary.formatted.getCharCount());
    					if (Math.abs(INDENT) + ms.binary.formatted.getCharCount() > currentScreenWidth) System.out.println();
    				}
//    				System.out.printf("%d %d %d%n", Math.abs(INDENT), ms.binary.formatted.getCharCount(), currentScreenWidth);
        		} else if (ms.xml != null) {
    				System.out.print(ms.msgDestNameFormatted);
    				int spaceToAdd = Math.abs(INDENT) - ms.msgDestName.length();
    				System.out.print(UsefulUtils.pad(spaceToAdd, ' '));
    				if (autoTrimPayload) System.out.println(ms.xml.formatted.trim(currentScreenWidth - Math.abs(INDENT) - 1));
    				else {
    					System.out.println(ms.xml.formatted);
    					if (Math.abs(INDENT) + ms.xml.formatted.getCharCount() > currentScreenWidth) System.out.println();
    				}
        		}  // else both payload sections are empty, but that is handled separately at the very top of this method
        	}
        } catch (RuntimeException e) {  // really shouldn't happen!!
        	System.out.println(printMessageStart());
        	System.out.println(new AaAnsi().ex(e));
        	logger.warn("Had issue parsing a message.  Message follows after exception.",e);
        	logger.warn(message.dump());
        	System.out.println(message.dump());
        	System.out.println(printMessageEnd());
        }
    }
	
	
	
}
