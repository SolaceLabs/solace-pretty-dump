package com.solace.labs.aaron;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
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
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

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
    static long msgCount = 0;
    Pattern filterRegexPattern = null;
//	boolean filteringOn = false;
    static int currentScreenWidth = 80;  // assume for now
    ThinkingAnsiHelper thinking = new ThinkingAnsiHelper();

	
    private int INDENT = 2;  // default starting value
    private boolean oneLineMode = false;
    private boolean noPayload = false;
    private boolean autoResizeIndent = false;  // specify -1 as indent for this MODE
    boolean autoSpaceTopicLevels = false;  // specify +something to space out the levels
    boolean autoTrimPayload = false;
    private LinkedListOfIntegers maxLengthTopicsList = new LinkedListOfIntegers();
    
    private List<LinkedListOfIntegers> levelLengths = new ArrayList<>();
    
	public PayloadHelper(Charset charset) {
		this.charset = charset;
		decoder = this.charset.newDecoder().onMalformedInput(CodingErrorAction.REPLACE).onUnmappableCharacter(CodingErrorAction.REPLACE);
		
		
	}

	public boolean isFilteringOn() {
		return thinking.isFilteringOn();
	}
    
	
    /** this method tracks what the longest topic string has been for the last 50 messages, so things line up nicely with indent mode "-1"
	 *  only used in "-1" auto-indent mode
     */
    private void updateTopicIndentValue(int maxTopicLength) {
    	maxLengthTopicsList.insert(maxTopicLength);
    	if (maxLengthTopicsList.getMax() + 2 != INDENT) {  // changed our current max
//    		int from = Math.abs(INDENT);
    		INDENT = maxLengthTopicsList.getMax() + 1;  // so INDENT will always be at least 3 (even MQTT spec states topic must be length > 1)
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
	    		if (INDENT == Integer.MIN_VALUE) sb.append("⋅/");  // always space out topic-only mode
	    		else sb.append('/');
	    	}
    	}
    	return sb.toString();
    }
    
    
    
    public int getCurrentIndent() {
    	return INDENT;
    }
    
    /** for auto-indent one-line "-1" mode */
    int getEffectiveIndent() {
    	if (oneLineMode) return 0;
    	return INDENT;
//    	return Math.min(INDENT, currentScreenWidth - 15);
    }
    
    void dealWithIndentParam(String indentStr) throws NumberFormatException {
    	if (indentStr.startsWith("+") && indentStr.length() >= 2) {
    		autoSpaceTopicLevels = true;
    		indentStr = "-" + indentStr.substring(1);
    	}
		int indent = Integer.parseInt(indentStr);
		if (indent < -250 || indent > 8) throw new NumberFormatException();
		INDENT = indent;
		if (INDENT < 0) {
			oneLineMode = true;
			if (INDENT == -1) {
				autoResizeIndent = true;  // use auto-resizing based on max topic length
				INDENT = 3;  // starting value (1 + 2 for padding)
				updateTopicIndentValue(1);  // now update it
			} else if (INDENT == -2) {  // two line mode
				INDENT = Math.abs(INDENT);
			} else {
				INDENT = Math.abs(INDENT) + 2;
//				updateTopicIndentValue(INDENT);  // now update it  TODO why do we need to update if not auto-indenting?
			}
		} else if (INDENT == 0) {
			if (indentStr.equals("-0")) {  // special case, print topic only
				INDENT = Integer.MIN_VALUE;  // not necessary anymore
				oneLineMode = true;
				noPayload = true;
			} else if (indentStr.equals("00")) {
				noPayload = true;
				INDENT = 2;
			} else if (indentStr.equals("000")) {
				noPayload = true;
				INDENT = 0;
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

	/** Only used by PrettyWrap */
	public PayloadSection buildPayloadSection(ByteBuffer payloadContents) {
    	currentScreenWidth = AnsiConsole.getTerminalWidth();
    	if (currentScreenWidth == 0) currentScreenWidth = 80;
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
		
		@Override
		public String toString() {
			if (type == null) return formatted.toRawString();
			else return new StringBuilder().append(type).append('\n').append(formatted.toRawString()).toString();
		}
		
		public String getType() {
			return type;
		}
		
		public String getFormattedPayload() {
			return formatted.toString();
		}
		
		/** sets `formatted` to be the nice String representation */
		void formatString(final String text, final byte[] bytes) {
			formatString(text, bytes, null);
		}
		
		void formatString(final String text, final byte[] bytes, String contentType) {
			if (contentType == null) contentType = "";  // empty string, for easier matching later
			if (text == null) {
				formatted = AaAnsi.n();
				type = "<NULL>";
				return;
			} else if (text.isEmpty()) {
				formatted = AaAnsi.n();
				type = "<EMPTY>";
				return;
			}
			String trimmed = text.trim();
        	if ((trimmed.startsWith("{") && trimmed.endsWith("}")) || contentType.contains("application/json")) {  // try JSON object
        		try {
            		formatted = GsonUtils.parseJsonObject(trimmed, getEffectiveIndent());
        			type = charset.displayName() + " charset, JSON Object";
				} catch (IOException e) {
        			type = charset.displayName() + " charset, INVALID JSON payload";
//        			formatted = new AaAnsi().setError().a("ERROR: ").a(e.getMessage()).reset().a('\n').a(text).reset().toString();
        			formatted = new AaAnsi().ex(e).a('\n').a(trimmed);
				}
        	} else if ((trimmed.startsWith("[") && trimmed.endsWith("]")) || contentType.contains("application/json")) {  // try JSON array
        		try {
            		formatted = GsonUtils.parseJsonArray(trimmed, getEffectiveIndent());
        			type = charset.displayName() + " charset, JSON Array";
        		} catch (IOException e) {
        			type = charset.displayName() + " charset, INVALID JSON payload";
//        			formatted = new AaAnsi().setError().a("ERROR: ").a(e.getMessage()).reset().a('\n').a(text).reset().toString();
        			formatted = new AaAnsi().ex(e).a('\n').a(trimmed);
				}
        	} else if ((trimmed.startsWith("<") && trimmed.endsWith(">")) ||
        			"application/xml".equals(contentType)  || contentType.contains("text/xml")) {  // try XML
    			try {
    				SaxHandler handler = new SaxHandler(getEffectiveIndent());
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
        		formatted = new AaAnsi().aStyledString(text).reset();
//        		formatted = text;
        	}
			boolean malformed = text.contains("\ufffd");
//			System.out.println("MALFORMED: " + malformed);
        	if (malformed) {
				type = "non " + type;
        	}
        	double ratio = (1.0 * formatted.getControlCharsCount() + formatted.getReplacementCharsCount()) / formatted.getTotalCharCount();
			if (malformed && ratio >= 0.25 /* && !oneLineMode */) {  // 25%, very likely a binary file
				formatted = UsefulUtils.printBinaryBytesSdkPerfStyle(bytes, getEffectiveIndent(), currentScreenWidth);
        	} else if (malformed || formatted.getControlCharsCount() > 0) {  // any unusual control chars (not tab, LF, CR, FF, or Esc, or NUL at string end
				if (!oneLineMode && getEffectiveIndent() > 0) {  // only if not in one-line mode!
					formatted.a('\n').a(UsefulUtils.printBinaryBytesSdkPerfStyle(bytes, getEffectiveIndent(), currentScreenWidth));
				}
        	}
		}

		void formatBytes(byte[] bytes, String contentType) {
			String parsed = decodeToString(bytes);
//			boolean malformed = parsed.contains("\ufffd");
			formatString(parsed, bytes, contentType);  // call the String version
			if (!type.startsWith("non")) {
				type = "valid " + type;
			}
/*        	if (malformed) {
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
*/		}	

    	void formatByteBufferFromWrapMode(ByteBuffer buffer) {
    		byte[] copy = null;
    		String tempType = null;
			byte first = buffer.get();  // check out the first byte
			if (first == 0x1c) {  // text message, one byte of size
				int size = Byte.toUnsignedInt(buffer.get());
				if (buffer.limit() != size) throw new IllegalArgumentException("size byte ("+size+") did not match buffer limit ("+buffer.limit()+")!");
				buffer.limit(buffer.limit()-1);  // text messages are null terminated, so don't read the trailing null
				tempType = PrettyMsgType.TEXT.toString();
//				System.out.println("0x1c SDT TextMessage detected, byte lenght: " + size);
			} else if (first == 0x1d) {  // text message, 2 bytes of size
				int size = Short.toUnsignedInt(buffer.getShort());
				if (buffer.limit() != size) throw new IllegalArgumentException("size bytes ("+size+") did not match buffer limit ("+buffer.limit()+")!");
//				System.out.println("0x1d SDT TextMessage detected, byte lenght: " + size);
				buffer.limit(buffer.limit()-1);  // text messages are null terminated, so don't read the trailing null
				tempType = PrettyMsgType.TEXT.toString();
			} else if (first == 0x1e) {  // text message, 3 bytes of size
				int size = Byte.toUnsignedInt(buffer.get()) << 16;
				size |= buffer.getShort();
				if (buffer.limit() != size) throw new IllegalArgumentException("size bytes ("+size+") did not match buffer limit ("+buffer.limit()+")!");
				buffer.limit(buffer.limit()-1);  // text messages are null terminated, so don't read the trailing null
				tempType = PrettyMsgType.TEXT.toString();
//				System.out.println("0x1e SDT TextMessage detected, byte lenght: " + size);
			} else if (first == 0x1f) {  // text message, 4 bytes of size
				int size = buffer.getInt();
				if (buffer.limit() != size) throw new IllegalArgumentException("size bytes ("+size+") did not match buffer limit ("+buffer.limit()+")!");
				buffer.limit(buffer.limit()-1);  // text messages are null terminated, so don't read the trailing null
//				System.out.println("0x1f SDT TextMessage detected, byte lenght: " + size);
				tempType = PrettyMsgType.TEXT.toString();
			} else if (first == 0x2b) {  // SDT Map?
				// check the size matches correctly:
				int size = buffer.getInt();  // next 4 bytes
//				System.out.println("size bytes ("+size+") and buffer limit ("+buffer.limit()+")!");
				if (buffer.limit() == size) {  // looks correct!  otherwise maybe just a regular binary msg
					copy = Arrays.copyOfRange(buffer.array(), 0, buffer.limit());
					MapTLVBuffer buf = new MapTLVBuffer(copy);  // sneaky hidden but public methods
					MapImpl map = new MapImpl(buf);
//					String test = SdtUtils.printMap(map, 3).toString();
					formatted = SdtUtils.printMap(map, getEffectiveIndent());
	            	type = PrettyMsgType.MAP.toString();  // hack, should be in the msg helper object
					return;
    			} else {
    				buffer.rewind();  // put back to beginning
    			}
			} else if (first == 0x2f) {  // SDT Stream?
				// check the size matches correctly:
				int size = buffer.getInt();  // next 4 bytes
//				System.out.println("size bytes ("+size+") and buffer limit ("+buffer.limit()+")!");
				if (buffer.limit() == size) {  // looks correct!  otherwise maybe just a regular binary msg
					copy = Arrays.copyOfRange(buffer.array(), 0, buffer.limit());
					StreamTLVBuffer buf = new StreamTLVBuffer(copy);  // sneaky hidden but public methods
					StreamImpl map = new StreamImpl(buf);
//					String test = SdtUtils.printMap(map, 3).toString();
					formatted = SdtUtils.printStream(map, getEffectiveIndent());
	            	type = PrettyMsgType.STREAM.toString();
					return;
    			} else {
    				buffer.rewind();  // put back to beginning
    			}
			} else {
				buffer.rewind();  // put back to beginning
//				tempType = PrettyMsgType.BYTES.toString();
			}
			// if we're here, then either a TextMessage that we've shifted around or binary message ready to be read
//			pos = buffer.position();
//			limit = buffer.limit();
//			capacity = buffer.capacity();
//			System.out.printf("pos: %d, lim: %d, cap: %d%n", pos, limit, capacity);
			copy = Arrays.copyOfRange(buffer.array(), 0, buffer.limit());
			String parsed = decodeToString(copy);
//			boolean malformed = parsed.contains("\ufffd");
			formatString(parsed, copy);  // call the String version
			if (tempType != null) type = tempType + ", " + type;
//        	if (malformed) {
//				type = "Non " + type;
//				if (INDENT > 0) {
//					formatted.a('\n').a(UsefulUtils.printBinaryBytesSdkPerfStyle(Arrays.copyOfRange(buffer.array(), 0, buffer.limit()), INDENT, currentScreenWidth));
//				}
//        	}
		}	

	}
	
	private class MessageHelperObject {
		
		final BytesXMLMessage orig;
		String[] headerLines;
    	String msgDestName;  // this would only be used in the 1-line version
    	String msgDestNameFormatted;
        String msgType;
        boolean hasPrintedMsgTypeYet = false;  // have we printed out the message type?

        PayloadSection binary;
        PayloadSection xml = null;
        PayloadSection userProps = null;
        PayloadSection userData = null;
                
        private MessageHelperObject(BytesXMLMessage message) {
        	orig = message;
        	msgType = orig.getClass().getSimpleName();  // will be "Impl" unless overridden later
        }
        
        private void prepMessageForPrinting() {
        	headerLines = orig.dump(XMLMessage.MSGDUMP_BRIEF).split("\n");
        }
        
        public String buildFullStringObject() {
        	StringBuilder sb = new StringBuilder();
        	for (String line : headerLines) {
        		if (line.startsWith("User Data") && userData != null) {
        			sb.append(line).append('\n').append(userData).append('\n');
        		} else if (line.startsWith("User Property Map") && userProps != null) {
        			sb.append(line).append('\n').append(userProps).append('\n');
        		} else if (line.startsWith("Binary Attachment") && binary != null) {
        			sb.append(getMessageTypeLine().reset()).append('\n').append(line).append('\n').append(binary).append('\n');
        		} else if (line.startsWith("XML") && xml != null) {
        			sb.append(getMessageTypeLine().reset()).append('\n').append(line).append('\n').append(xml).append('\n');
        		} else {
        			sb.append(line).append('\n');
        		}
        	}
        	return sb.toString();
        }
        
        /** Only call this once all the filtering is done and time to space out the topic */
        void updateTopicSpacing() {
        	AaAnsi aaDest = new AaAnsi().fg(Elem.DESTINATION);
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
            	else if (oneLineMode && msgDestName.length() > getEffectiveIndent() - 1) {  // too long, need to trim it
//    				msgDestName = 
            		// let's not trim the queue name... leave it
    			}
        		aaDest.a(msgDestName).reset();
        	} else {  // a Topic
        		msgDestName = orig.getDestination().getName();
        		if (autoSpaceTopicLevels) {
        			msgDestName = updateTopicSpaceOutLevels(msgDestName);
        		}
            	if (autoResizeIndent) {
            		updateTopicIndentValue(msgDestName.length());
            	} else if (oneLineMode && getCurrentIndent() > 2 && msgDestName.length() > getCurrentIndent() - 1) {  // too long, need to trim it
            		msgDestName = msgDestName.substring(0, getCurrentIndent()-2) + "…";
            		// huh we actually trim it here?  Ok
    			}
            	if (!oneLineMode) {
            		aaDest.a("Topic '").colorizeTopic(msgDestName, highlightTopicLevel).fg(Elem.DESTINATION).a('\'').reset();
            	} else {
            		aaDest.colorizeTopic(msgDestName, highlightTopicLevel);
            	}
        	}
        	msgDestNameFormatted = aaDest.toString();
//        	if (autoResizeIndent) updateTopicIndentValue(msgDestName.length());        	
        }
        
        AaAnsi getMessageTypeLine() {
        	AaAnsi aa = AaAnsi.n().a("Message Type:                           ");
			aa.fg(Elem.PAYLOAD_TYPE).a(msgType).reset();
			return aa;
        }

        
        void printMsgTypeIfRequired(SystemOutHelper systemOut) {
    		if (!hasPrintedMsgTypeYet) {
    			systemOut.println(getMessageTypeLine());
    			hasPrintedMsgTypeYet = true;
    		}
        }
	}
	
	static int highlightTopicLevel = -1;
	
    
    // Helper class, for printing message to the console ///////////////////////////////////////

	private static final int DIVIDER_LENGTH = 60;  // same as SdkPerf JCSMP

	public AaAnsi printMessageStart() {
        String head = " Start Message #" + msgCount + " ";
        String headPre = UsefulUtils.pad((int)Math.ceil((DIVIDER_LENGTH - head.length()) / 2.0) , '^');
        String headPost = UsefulUtils.pad((int)Math.floor((DIVIDER_LENGTH - head.length()) / 2.0) , '^');
        return AaAnsi.n().fg(Elem.MSG_BREAK).a(headPre).a(head).a(headPost).reset();
	}
	
	public AaAnsi printMessageEnd() {
        String end = " End Message #" + msgCount + " ";
        String end2 = UsefulUtils.pad((int)Math.ceil((DIVIDER_LENGTH - end.length()) / 2.0) , '^');
        String end3 = UsefulUtils.pad((int)Math.floor((DIVIDER_LENGTH - end.length()) / 2.0) , '^');
        return new AaAnsi().fg(Elem.MSG_BREAK).a(end2).a(end).a(end3).reset();
	}

	// Destination:                            Topic 'solace/samples/jcsmp/hello/aaron'
	public static AaAnsi colorizeDestination(Destination jcsmpDestination) {
		AaAnsi aa = new AaAnsi().fg(Elem.PAYLOAD_TYPE).a("Destination:                            ").fg(Elem.DESTINATION);
		if (jcsmpDestination instanceof Topic) {
			aa.a("Topic '").colorizeTopic(jcsmpDestination.getName(), highlightTopicLevel).fg(Elem.DESTINATION).a("'");
		} else {  // queue
			aa.a("Queue '").a(jcsmpDestination.getName()).a("'");
		}
		return aa.reset();
	}
	
	// JMSDestination:                         Topic 'solace/json/test'
	/** Needs to bee the whole line!  E.g. "<code>JMSDestination:                         Topic 'solace/json/test'</code>" */
	public static AaAnsi colorizeDestination(String destinationLine) {
		AaAnsi aa = new AaAnsi().fg(Elem.PAYLOAD_TYPE).a(destinationLine.substring(0,40)).fg(Elem.DESTINATION);
		String rest = destinationLine.substring(40);
		if (rest.startsWith("Topic")) {
			aa.a("Topic '").colorizeTopic(rest.substring(7, rest.length()-1), highlightTopicLevel).fg(Elem.DESTINATION).a("'");
		} else {
			aa.a("Queue '").a(rest.substring(7, rest.length()-1)).a("'");
		}
		return aa.reset();
	}
	
	public enum PrettyMsgType {
        MAP("SDT MapMessage"),
        STREAM("SDT StreamMessage"),
        TEXT("SDT TextMessage"),
        XML("XML Content Message"),
        BYTES("Raw BytesMessage"),
        ;
		
		final String description;
		
		private PrettyMsgType(String desc) {
			this.description = desc;
		}
		
		@Override
		public String toString() {
			return description;
		}
	}

	
	private void handlePayloadSection(String line, PayloadSection ps, AaAnsi aa) {
		boolean invalid = ps.type != null && (ps.type.contains("non") || ps.type.contains("INVALID"));
		if (getEffectiveIndent() == 0 || noPayload) {  // so compressed and/or no payload
    		if (ps.type != null) {
    			aa.a(',').a(' ');
    			if (invalid) aa.invalid(ps.type);
    			else aa.fg(Elem.PAYLOAD_TYPE).a(ps.type).reset();
    		}
//    		if (!noPayload) aa.a(": ").a(ps.formatted);
    		if (!noPayload) aa.a('\n').a(ps.formatted);
		} else {
			aa.a('\n');
    		if (ps.type != null) {
    			if (invalid) aa.invalid(UsefulUtils.capitalizeFirst(ps.type));
    			else aa.fg(Elem.PAYLOAD_TYPE).a(UsefulUtils.capitalizeFirst(ps.type));//.reset();
        		if (!ps.type.contains("EMPTY")) aa.a(':');
        		aa.a('\n');
    		}
        	if (!noPayload && (ps.type == null || !ps.type.contains("EMPTY"))) {
        		aa.a(ps.formatted).a('\n');
        	}
		}
	}
	

    public void dealWithMessage(BytesXMLMessage message) {
    	msgCount++;
    	currentScreenWidth = AnsiConsole.getTerminalWidth();
    	if (currentScreenWidth == 0) currentScreenWidth = 80;  // for running in eclipse or others that don't return a good value
    	MessageHelperObject ms = new MessageHelperObject(message);
        // if doing topic only, or if there's no payload in compressed (<0) MODE, then just print the topic
    	// can't do this anymore here b/c we have filtering now
//    	if (INDENT == Integer.MIN_VALUE || (INDENT < 0 && !message.hasContent() && !message.hasAttachment())) {
//    		System.out.println(ms.msgDestNameFormatted);
//            return;
//    	}
//    	if (noPayload && oneLineMode && filterRegexPattern == null) {  // just dumping topic, let's optimize here
//            ms.updateTopicSpacing();  // now that we've determined if we're gonna filter this message, do the topic stuff
//            System.out.println(ms.msgDestNameFormatted);
//            return;
//    	}
    	
    	if (message instanceof XMLContentMessage) {
        	ms.msgType = PrettyMsgType.XML.toString();
        } else {
	        if (message instanceof MapMessage) {
	        	ms.msgType = PrettyMsgType.MAP.toString();
	        } else if (message instanceof StreamMessage) {
	        	ms.msgType = PrettyMsgType.STREAM.toString();
	        } else if (message instanceof TextMessage) {
				ms.msgType = /* "<EMPTY> " + */ PrettyMsgType.TEXT.toString();
	        } else if (message instanceof BytesMessage) {
//	        	if (message.hasAttachment() && message.getAttachmentContentLength() > 0) ms.msgType = PrettyMsgType.BYTES.toString();
//	        	else ms.msgType = "<EMPTY> " + PrettyMsgType.BYTES.toString();
	        	ms.msgType = PrettyMsgType.BYTES.toString();
	        } else {  // shouldn't be anything else..?
	        	// leave as Impl class
	        }
        }
    	
    	
    	// so at this point we know we know we will need the payload, so might as well try to parse it now
        try {  // want to catch SDT exceptions from the map and stream; payload string encoding issues now caught in format()
        	if (!noPayload || filterRegexPattern != null) {
	        	if (message.hasAttachment()) { // getAttachmentContentLength() > 0) {
	        		ms.binary = new PayloadSection();
		            if (message instanceof MapMessage) {
	//	            	ms.msgType = "SDT MapMessage";
		            	ms.binary.formatted = SdtUtils.printMap(((MapMessage)message).getMap(), getEffectiveIndent());
		            } else if (message instanceof StreamMessage) {
	//	            	ms.msgType = "SDT StreamMessage";
		            	// set directly
		            	ms.binary.formatted = SdtUtils.printStream(((StreamMessage)message).getStream(), getEffectiveIndent());
		            } else {  // either text or binary, try/hope that the payload is a string, and then we can try to format it
			            if (message instanceof TextMessage) {
//			            	String pay = ((TextMessage)message).getText();
//			            	ByteBuffer bb = message.getAttachmentByteBuffer();
			            	byte[] bytes = message.getAttachmentByteBuffer().array();
			            	ms.binary.formatString(((TextMessage)message).getText(), bytes, message.getHTTPContentType());
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
			            			ms.binary.formatted = UsefulUtils.printBinaryBytesSdkPerfStyle(bytes, getEffectiveIndent(), currentScreenWidth);
			            		}
			            	} else {  // should be valid! maybe check if it's properly formatted, UTF-8
			    	        	ms.msgType = PrettyMsgType.TEXT.toString();
			            	}
			            } else {  // bytes message
	//		            	ms.msgType = "Raw BytesMessage";
			            	if (message.getAttachmentByteBuffer() != null) {  // should be impossible since content length > 0
			            		byte[] bytes = message.getAttachmentByteBuffer().array();
			            		if ("gzip".equals(message.getHTTPContentEncoding())) {
			            			try {
										GZIPInputStream gzip = new GZIPInputStream(new ByteArrayInputStream(bytes));
										ByteArrayOutputStream os = new ByteArrayOutputStream();
										gzip.transferTo(os);
										bytes = os.toByteArray();
										ms.msgType = "GZIPed " + ms.msgType;
									} catch (IOException e) {
										logger.warn("Had a message marked as 'gzip' but wasn't", e);
										logger.warn(message.dump());
										System.out.println(AaAnsi.n().ex("Had a message marked as 'gzip' but wasn't", e).toString());
									}
			            		}
	
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
												ms.binary.formatted = ProtoBufUtils.decode(protoMsg, getEffectiveIndent());
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
	        	}
	            if (message.hasContent()) {  // try the XML portion of the payload (OLD SCHOOL!!!)
	            	ms.xml = new PayloadSection();   // could there be Protobuf stuff in this section??
	            	ms.xml.formatBytes(message.getBytes(), message.getHTTPContentType());
	            }
        	}
            if (message.getProperties() != null && !message.getProperties().isEmpty()) {
            	ms.userProps = new PayloadSection();
            	ms.userProps.formatted = SdtUtils.printMap(message.getProperties(), getEffectiveIndent());
            }
            if (message.getUserData() != null && message.getUserData().length > 0) {
            	ms.userData = new PayloadSection();
            	ms.userData.formatBytes(message.getUserData(), null);
            }
            
            // done preparing the message.  Now we might have to filter it?
            ms.prepMessageForPrinting();
            if (filterRegexPattern != null) {
            	if (!filterRegexPattern.matcher(ms.buildFullStringObject()).find()) {
                	thinking.tick();
                	return;
            	} else {
            		thinking.filteringOff();
            	}
            }
            ms.updateTopicSpacing();  // now that we've determined if we're gonna filter this message, do the topic stuff
            
            // now it's time to try printing it!
            SystemOutHelper systemOut = new SystemOutHelper();
            if (!oneLineMode) {
            	String[] headerLines = ms.headerLines;
                for (String line : headerLines) {
                	if (line.isEmpty() || line.matches("\\s*")) continue;  // testing 
					if (line.startsWith("User Property Map:") && ms.userProps != null) {
                		if (getEffectiveIndent() == 0) {
	                		systemOut.println("User Property Map:                      " + ms.userProps.formatted);
                		} else {
                    		systemOut.println(new AaAnsi().a(line));
                    		systemOut.println(ms.userProps.formatted);
                		}
                		if (getEffectiveIndent() > 0 && !noPayload) systemOut.println();
                	} else if (line.startsWith("User Data:") && ms.userData != null) {
                		if (getEffectiveIndent() == 0) {
	                		systemOut.println("User Data:                              " + ms.userData.formatted);
                		} else {
	                		systemOut.println(new AaAnsi().a(line).a(" bytes"));
	                		systemOut.println(ms.userData.formatted);
                		}
                		if (getEffectiveIndent() > 0 && !noPayload) systemOut.println();
                	} else if (line.startsWith("SDT Map:") || line.startsWith("SDT Stream:")) {
                		// skip (handled as part of the binary attachment)
                	} else if (line.startsWith("Binary Attachment:")) {
//                		assert ms.binary != null;
                    	ms.printMsgTypeIfRequired(systemOut);
                    	AaAnsi payloadText = AaAnsi.n().a(line).a(" bytes");
                    	if (ms.binary != null) handlePayloadSection(line, ms.binary, payloadText);
                    	systemOut.println(payloadText);
                	} else if (line.startsWith("XML:")) {
//                		assert ms.xml != null;
                    	ms.printMsgTypeIfRequired(systemOut);
                    	AaAnsi payloadText = AaAnsi.n().fg(Elem.WARN).a("XML Payload section:           ").reset();
                    	payloadText.a("         ").a(line.substring(40)).a(" bytes").reset();
                    	if (ms.xml != null) handlePayloadSection(line, ms.xml, payloadText);
                    	systemOut.println(payloadText);
                	} else if (line.startsWith("Destination:           ")) {  // contains, not startsWith, due to ANSI codes
                		systemOut.print("Destination:                            ");
                		systemOut.println(ms.msgDestNameFormatted);  // just print out since it's already formatted
                	} else if (line.startsWith("Message Id:") && message.getDeliveryMode() == DeliveryMode.DIRECT) {
                		// skip it, hide the auto-generated message ID on Direct messages
                	} else {  // everything else
//                		System.out.println(new AaAnsi().a(line));
                		systemOut.println(UsefulUtils.guessIfMapLookingThing(line));
                	}
                }
                if (!message.hasContent() && !message.hasAttachment()) {
                	ms.printMsgTypeIfRequired(systemOut);
//                	systemOut.println(new AaAnsi().fg(Elem.PAYLOAD_TYPE).a(UsefulUtils.capitalizeFirst(ms.msgType)).a(", <EMPTY PAYLOAD>").reset().toString());
                	systemOut.println(new AaAnsi().fg(Elem.PAYLOAD_TYPE).a("<NO PAYLOAD>").reset().toString());
                } else if (message.hasAttachment() && message.getAttachmentContentLength() == 0) {
                	ms.printMsgTypeIfRequired(systemOut);
//                	systemOut.println(new AaAnsi().fg(Elem.PAYLOAD_TYPE).a(UsefulUtils.capitalizeFirst(ms.msgType)).a(", <EMPTY PAYLOAD>").reset().toString());
                	systemOut.println(new AaAnsi().fg(Elem.PAYLOAD_TYPE).a("<EMPTY PAYLOAD>").reset().toString());
                	
                }
        	} else {  // one-line mode!
//        		System.out.println(INDENT + ", " + currentScreenWidth + ", " + message.getDestination().getName());
        		if (ms.binary != null && ms.xml != null) {
        			// that's not great for one-line printing!  but probably pretty rare!!!!
        			systemOut.println("Message contains both binary and XML payloads:");
        			systemOut.println(message.dump().trim());  // raw JCSMP full dump
        		} else if (noPayload) {  // "-0" mode
    				systemOut.println(ms.msgDestNameFormatted);
        		} else {  // one payload section defined, or empty
    				AaAnsi payload = null;
    				if (ms.binary != null) payload = ms.binary.formatted;
    				else if (ms.xml != null) payload = ms.xml.formatted;
    				else payload = AaAnsi.n().faintOn().a("<EMPTY> ").a(ms.msgType).reset();  // hopefully an EMPTY message
        			
    				systemOut.print(ms.msgDestNameFormatted);
    				if (getCurrentIndent() == 2) {  // two-line mode
    					systemOut.println();
    					systemOut.print("  ");
    				} else {
    					int spaceToAdd = getCurrentIndent() - ms.msgDestName.length();
    					systemOut.print(UsefulUtils.pad(spaceToAdd, ' '));
    				}
    				if (autoTrimPayload) {
//    					System.out.printf("width=%d, indent=%d%n", currentScreenWidth, getCurrentIndent());
    					systemOut.println(payload.trim(currentScreenWidth - getCurrentIndent() - 0));
    				}
    				else {
    					systemOut.println(payload.reset());  // need the reset b/c a (fancy) string payload won't have the reset() at the end
//    					System.out.printf("charCount=%d, calcLength=%d, width=%d, size=%d%n", ms.binary.formatted.getCharCount(), AaAnsi.length(ms.binary.formatted.toString()),currentScreenWidth,Math.abs(INDENT)+ms.binary.formatted.getCharCount());
    					if (getCurrentIndent() + payload.getTotalCharCount() > currentScreenWidth) systemOut.println();
    				}
        		}
        	}
            if (!oneLineMode) System.out.println(printMessageStart());
            System.out.print(systemOut);//.raw.toString());
			if (!oneLineMode && getEffectiveIndent() > 0 /* && !noPayload */) System.out.println(printMessageEnd());
			
        } catch (RuntimeException e) {  // really shouldn't happen!!
        	System.out.println(printMessageStart());
        	System.out.println(AaAnsi.n().ex("Exception occured, check ~/.pretty/pretty.log for details. ", e));
        	logger.warn("Had issue parsing a message.  Message follows after exception.",e);
        	logger.warn(message.dump());
        	System.out.println(message.dump());
        	System.out.println(printMessageEnd());
        }
    }
}
