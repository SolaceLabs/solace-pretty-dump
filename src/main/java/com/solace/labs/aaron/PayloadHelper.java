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
import com.solace.labs.aaron.utils.BoundedLinkedList;
import com.solace.labs.topic.Sub;
import com.solacesystems.jcsmp.BytesMessage;
import com.solacesystems.jcsmp.BytesXMLMessage;
import com.solacesystems.jcsmp.Destination;
import com.solacesystems.jcsmp.MapMessage;
import com.solacesystems.jcsmp.StreamMessage;
import com.solacesystems.jcsmp.TextMessage;
import com.solacesystems.jcsmp.Topic;
import com.solacesystems.jcsmp.XMLContentMessage;
import com.solacesystems.jcsmp.impl.sdt.MapImpl;
import com.solacesystems.jcsmp.impl.sdt.MapTLVBuffer;
import com.solacesystems.jcsmp.impl.sdt.StreamImpl;
import com.solacesystems.jcsmp.impl.sdt.StreamTLVBuffer;

/**
 * A PayloadHelper contains a charset and an indent amount, and is used for 
 */
public enum PayloadHelper {
	
	Helper,
	;
	
	private PayloadHelper() {
		
	}

//	private static PayloadHelper this_;
	
//	private static Map<Charset, CharsetDecoder> mapOfDecoders = new ConcurrentHashMap<>();  // why do this?  I only need one..?
	
    private static final Logger logger = LogManager.getLogger(PayloadHelper.class);
	
    private volatile boolean isStopped = false;
    private Charset charset;
	private CharsetDecoder decoder;
    private Map<Sub, Method> protobufCallbacks = new HashMap<>();
    private long msgCount = 0;
    private long filteredCount = 0;
    private Pattern filterRegexPattern = null;
//	boolean filteringOn = false;
//    static int currentScreenWidth = 80;  // assume for now
//    private ThinkingAnsiHelper thinking = new ThinkingAnsiHelper();
	private int highlightTopicLevel = -1;

	
    private int INDENT = 2;  // default starting value
    private boolean oneLineMode = false;
    private boolean noPayload = false;
    private boolean autoResizeIndent = false;  // specify -1 as indent for this MODE
    private boolean autoSpaceTopicLevels = false;  // specify +something to space out the levels
    private boolean autoTrimPayload = false;
    private BoundedLinkedList.Comparable<Integer> maxLengthTopicsList = new BoundedLinkedList.Comparable<>(50);
    private List<BoundedLinkedList.Comparable<Integer>> levelLengths = new ArrayList<>();
    private BoundedLinkedList<MessageHelper> lastNMessages = null;
    
	private void payloadHelperInit(Charset charset) {
		this.charset = charset;
		decoder = this.charset.newDecoder().onMalformedInput(CodingErrorAction.REPLACE).onUnmappableCharacter(CodingErrorAction.REPLACE);
	}
	
	public static void init(Charset charset) {
//		this_ = new PayloadHelper(charset);
		Helper.payloadHelperInit(charset);
	}
	
	public void setAutoTrimPayload(boolean trim) {
		autoTrimPayload = trim;
	}

	public void toggleAutoTrimPayload() {
		autoTrimPayload = !autoTrimPayload;
	}

	public void setRegexFilterPattern(Pattern regex) {
		filterRegexPattern = regex;
	}
	
	public void enableLastNMessage(int amount) {
		lastNMessages = new BoundedLinkedList<>(amount);
	}
	
	public void setProtobufCallbacks(Map<Sub, Method> map) {
		protobufCallbacks = map;
	}
	
	
	public boolean isOneLineMode() {
		return oneLineMode;
	}
	
	public boolean isNoPayload() {
		return noPayload;
	}
	
	public boolean isAutoResizeIndent() {
		return autoResizeIndent;
	}
	
	public boolean isAutoTrimPayload() {
		return autoTrimPayload;
	}
	
	public void setAutoSpaceTopicLevels(boolean enabled) {
		autoSpaceTopicLevels = enabled;
	}
	
	public boolean isAutoSpaceTopicLevelsEnabled() {
		return autoSpaceTopicLevels;
	}
    
	public boolean isLastNMessagesEnabled() {
		return lastNMessages != null;
	}
	
	public int getHighlightedTopicLevel() {
		return highlightTopicLevel;
	}

	public void setHighlightedTopicLevel(int level) {
		highlightTopicLevel = level;
	}

	public int getLastNMessagesSize() {
		if (lastNMessages == null) return 0;
		else return lastNMessages.size();
	}

	public int getLastNMessagesCapacity() {
		if (lastNMessages == null) return 0;
		else return lastNMessages.capacity();
	}

	public void incMessageCount() {
		msgCount++;
	}
	
	public void incFilteredCount() {
		filteredCount++;
	}
	
	public long getMessageCount() {
		return msgCount;
    }
	
	public long getFilteredCount() {
		return filteredCount;
	}
	
	public BoundedLinkedList<MessageHelper> getLastNMessages() {
		return lastNMessages;
	}

    /** this method tracks what the longest topic string has been for the last 50 messages, so things line up nicely with indent mode "-1"
	 *  only used in "-1" auto-indent mode
     */
    public void updateTopicIndentValue(int maxTopicLength) {
    	if (autoResizeIndent) {
	    	maxLengthTopicsList.add(maxTopicLength);
	    	if (maxLengthTopicsList.getMax() + 1 != INDENT) {  // changed our current max
	//    		int from = Math.abs(INDENT);
	    		INDENT = maxLengthTopicsList.getMax() + 1;  // so INDENT will always be at least 3 (even MQTT spec states topic must be length > 1)
	//    		System.out.println(new org.fusesource.jansi.Ansi().reset().a(org.fusesource.jansi.Ansi.Attribute.INTENSITY_FAINT).a("** changing INDENT from " + from + " to " + Math.abs(INDENT) + "**").reset().toString());
	//    	} else {
	//    		System.out.println(new org.fusesource.jansi.Ansi().reset().a(org.fusesource.jansi.Ansi.Attribute.INTENSITY_FAINT).a("** keeping INDENT = " + Math.abs(INDENT) + "**").reset().toString());
	    	}
    	}
    }
    
    
    public String updateTopicSpaceOutLevels(String topic) {
    	if (autoSpaceTopicLevels) {
	    	String[] levels = topic.split("/");
	    	if (levelLengths.size() < levels.length) {
	    		for (int i=levelLengths.size(); i < levels.length; i++) {
	    			levelLengths.add(new BoundedLinkedList.Comparable<Integer>(200));  // Keep column sizes for 200 msgs
	    		}
	    	}
	    	for (int i=0; i < levelLengths.size(); i++) {
	    		if (i < levels.length) {
	    			levelLengths.get(i).add(levels[i].length());
	    		} else {
	    			levelLengths.get(i).add(0);
	    		}
	    	}
	    	StringBuilder sb = new StringBuilder();
	    	for (int i=0; i < levels.length; i++) {
	    		sb.append(levels[i]);
	    		int max = levelLengths.get(i).getMax();
	    		if (i < levels.length-1) {
		    		if (max > levels[i].length()) {
						sb.append(UsefulUtils.pad(max - levels[i].length(), /*'⋅'*/ '·' ));
		    		}
		    		if (INDENT == Integer.MIN_VALUE) sb.append("·/");// ("⋅/");  // always space out topic-only mode
		    		else sb.append('/');
		    	}
	    	}
	    	return sb.toString();
    	} else {
    		return topic;
    	}
    }
    
    
    
    public int getCurrentIndent() {
    	return INDENT;
    }
    
    /** for auto-indent one-line "-1" mode */
    int getFormattingIndent() {
    	if (oneLineMode) return 0;
    	return INDENT;
//    	return Math.min(INDENT, currentScreenWidth - 15);
    }
    
    /** Throws NumberFormat if it can't be parsed, or IllegalArgument if it is a number, but invalid */
    public void dealWithIndentParam(String indentStr) throws NumberFormatException, IllegalArgumentException {
    	// first, switch any pluses to minuses
    	if (indentStr.startsWith("+") && indentStr.length() >= 2) {
    		autoSpaceTopicLevels = true;
    		indentStr = "-" + indentStr.substring(1);
    	}
		int indent = Integer.parseInt(indentStr);  // might throw
		if (indent < -250 || indent > 8) throw new IllegalArgumentException();
		INDENT = indent;
		if (INDENT < 0) {
			oneLineMode = true;
			if (INDENT == -1) {
				autoResizeIndent = true;  // use auto-resizing based on max topic length
				INDENT = 3;  // starting value (1 + 2 for padding)
				updateTopicIndentValue(2);  // now update it
			} else if (INDENT == -2) {  // two line mode
				INDENT = Math.abs(INDENT);
			} else {
				INDENT = Math.abs(INDENT) + 2; // TODO why is this 2?  I think 1 for ... and 1 for space
//				updateTopicIndentValue(INDENT);  // now update it  TODO why do we need to update if not auto-indenting?
			}
		} else if (INDENT == 0) {
			if (indentStr.equals("-0")) {  // special case, print topic only
				INDENT = Integer.MIN_VALUE;  // not necessary anymore
				oneLineMode = true;
				noPayload = true;
			} else if (indentStr.equals("00")) {
				INDENT = 2;
				noPayload = true;
			} else if (indentStr.equals("000")) {
				noPayload = true;
//				INDENT = 0;  // that's already done above!
			} else if (indentStr.equals("0")) {
				// nothing, normal
			} else if (indentStr.equals("0000")) {  // something new, no user proper or data
				INDENT = 0;
				noPayload = true;
				autoTrimPayload = true;
			} else {  // shouldn't be anything else (e.g. "0000")
				throw new IllegalArgumentException();
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
    	int currentScreenWidth = AnsiConsole.getTerminalWidth();
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

		int size = 0;
		String type = null;  // might initialize later if JSON or XML
		AaAnsi formatted = AaAnsi.n().fg(Elem.NULL).a("<UNINITIALIZED>");  // to ensure gets overwritten
		int numElements = 0;  // primarily for User Properties SDTMap element count
		
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
		
		public int getSize() {
			return size;
		}
		
		public String getSizeString() {
			return new StringBuilder().append("(len=").append(size).append(')').toString();
		}
		
		/** sets `formatted` to be the nice String representation */
		void formatString(final String text, final byte[] bytes) {
			formatString(text, bytes, null);
		}
		
		void formatString(final String text, final byte[] bytes, String contentType) {
			if (contentType == null) contentType = "";  // empty string, for easier matching later
			size = bytes.length;
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
            		formatted = GsonUtils.parseJsonObject(trimmed, getFormattingIndent());
        			type = charset.displayName() + " charset, JSON Object";
				} catch (IOException e) {
        			type = charset.displayName() + " charset, INVALID JSON payload";
//        			formatted = new AaAnsi().setError().a("ERROR: ").a(e.getMessage()).reset().a('\n').a(text).reset().toString();
        			formatted = new AaAnsi().ex(e).a('\n').a(trimmed);
				}
        	} else if ((trimmed.startsWith("[") && trimmed.endsWith("]")) || contentType.contains("application/json")) {  // try JSON array
        		try {
            		formatted = GsonUtils.parseJsonArray(trimmed, getFormattingIndent());
        			type = charset.displayName() + " charset, JSON Array";
        		} catch (IOException e) {
        			type = charset.displayName() + " charset, INVALID JSON payload";
//        			formatted = new AaAnsi().setError().a("ERROR: ").a(e.getMessage()).reset().a('\n').a(text).reset().toString();
        			formatted = new AaAnsi().ex(e).a('\n').a(trimmed);
				}
        	} else if ((trimmed.startsWith("<") && trimmed.endsWith(">")) ||
        			"application/xml".equals(contentType)  || contentType.contains("text/xml")) {  // try XML
    			try {
    				SaxHandler handler = new SaxHandler(getFormattingIndent());
					SaxParser.parseString(trimmed, handler);
                    formatted = handler.getResult();  // overwrite
                    type = charset.displayName() + " charset, XML document";
				} catch (SaxParserException e) {
					logger.warn("Couldn't parse xml", e);
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
				formatted = UsefulUtils.printBinaryBytesSdkPerfStyle(bytes, getFormattingIndent(), AnsiConsole.getTerminalWidth());
        	} else if (malformed || formatted.getControlCharsCount() > 0) {  // any unusual control chars (not tab, LF, CR, FF, or Esc, or NUL at string end
				if (!oneLineMode && getFormattingIndent() > 0) {  // only if not in one-line mode!
					formatted.a('\n').a(UsefulUtils.printBinaryBytesSdkPerfStyle(bytes, getFormattingIndent(), AnsiConsole.getTerminalWidth()));
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
					formatted = SdtUtils.printMap(map, getFormattingIndent());
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
					formatted = SdtUtils.printStream(map, getFormattingIndent());
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
/*	
	private class MessageHelperObject {
		
		final BytesXMLMessage orig;
		final long msgCountNumber;
		final String[] headerLines;
    	final String msgDestName;  // this would only be used in the 1-line version
    	AaAnsi msgDestNameFormatted;
        String msgType;  // could change
        boolean hasPrintedMsgTypeYet = false;  // have we printed out the message type?

        PayloadSection binary;
        PayloadSection xml = null;
        PayloadSection userProps = null;
        PayloadSection userData = null;
                
        private MessageHelperObject(BytesXMLMessage message, long msgCountNumber) {
        	orig = message;
        	this.msgCountNumber = msgCountNumber;
        	this.msgDestName = message.getDestination().getName();
        	headerLines = orig.dump(XMLMessage.MSGDUMP_BRIEF).split("\n");
        	msgType = orig.getClass().getSimpleName();  // will be "Impl" unless overridden later
        }

        /** this only gets called in non-one-line mode, otherwise we might have to do some trimming first * /
        private void colorizeDestForRegularMode() {
        	String msgDestName = orig.getDestination().getName();
        	if (orig.getDestination() instanceof Queue) {
        		msgDestNameFormatted = AaAnsi.n().fg(Elem.DESTINATION).a("Queue '").a(msgDestName).a('\'');
        	} else {  // a Topic
        		if (oneLineMode) {  // shouldn't be calling this yet!!?!?!!
            		msgDestNameFormatted = AaAnsi.n().fg(Elem.DESTINATION).a(msgDestName);
        		} else {
            		msgDestNameFormatted = AaAnsi.n().fg(Elem.DESTINATION).a("Topic '").colorizeTopic(msgDestName, highlightTopicLevel).a('\'');
        		}
        	}
        	msgDestNameFormatted.reset();
        }
        
        // make sure this is called before printMessage()
//        private void prepMessageForPrinting() {
//        	headerLines = orig.dump(XMLMessage.MSGDUMP_BRIEF).split("\n");
//        }
        
        public String buildFullStringObject() {
        	StringBuilder sb = new StringBuilder();
        	for (String line : headerLines) {
        		if (line.startsWith("User Data") && userData != null) {
        			sb.append(line).append('\n').append(userData).append('\n');
        		} else if (line.startsWith("User Property Map") && userProps != null) {
        			sb.append(line).append('\n').append(userProps).append('\n');
        		} else if (line.startsWith("Binary Attachment") && binary != null) {
        			sb.append(getMessageTypeLine().toRawString()).append('\n').append(line).append('\n').append(binary).append('\n');
        		} else if (line.startsWith("XML") && xml != null) {
        			sb.append(getMessageTypeLine().toRawString()).append('\n').append(line).append('\n').append(xml).append('\n');
        		} else {
        			sb.append(line).append('\n');
        		}
        	}
        	return sb.toString();
        }
        
        /** Only call this once all the filtering is done and time to space out the topic * /
        void updateTopicSpacing() {
        	String msgDestName = orig.getDestination().getName();
        	AaAnsi aaDest = new AaAnsi().fg(Elem.DESTINATION);
        	if (orig.getDestination() instanceof Queue) {
        		msgDestName = "Queue '" + orig.getDestination().getName() + "'";
            	if (autoResizeIndent) updateTopicIndentValue(msgDestName.length());
        		aaDest.a(msgDestName);
        	} else {  // a Topic
        		msgDestName = orig.getDestination().getName();
        		if (autoSpaceTopicLevels) {
        			msgDestName = updateTopicSpaceOutLevels(msgDestName);
        		}
            	if (autoResizeIndent) {
            		updateTopicIndentValue(msgDestName.length());
    			}
            	if (!oneLineMode) {
            		aaDest.a("Topic '").colorizeTopic(msgDestName, highlightTopicLevel).fg(Elem.DESTINATION).a('\'');
            	} else {
            		aaDest.colorizeTopic(msgDestName, highlightTopicLevel);
            	}
        	}
        	msgDestNameFormatted = aaDest.reset();
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
        
        
        SystemOutHelper printMessage() {
            // now it's time to try printing it!
            SystemOutHelper systemOut = new SystemOutHelper();
            if (!oneLineMode) {
                systemOut.println(printMessageStart(msgCountNumber));
                for (String line : headerLines) {
                	if (line.isEmpty() || line.matches("\\s*")) continue;  // testing 
					if (line.startsWith("User Property Map:") && userProps != null) {
                		if (getEffectiveIndent() == 0) {
	                		systemOut.println("User Property Map:                      " + userProps.formatted);
                		} else {
//                    		systemOut.println(new AaAnsi().a(line));
	                		systemOut.println("User Property Map:                      " + userProps.numElements + " elements");
                    		systemOut.println(userProps.formatted);
                		}
                		if (getEffectiveIndent() > 0 && !noPayload) systemOut.println();
                	} else if (line.startsWith("User Data:") && userData != null) {
                		if (getEffectiveIndent() == 0) {
	                		systemOut.println("User Data:                              " + userData.formatted);
                		} else {
	                		systemOut.println(new AaAnsi().a(line).a(" bytes"));
	                		systemOut.println(userData.formatted);
                		}
                		if (getEffectiveIndent() > 0 && !noPayload) systemOut.println();
                	} else if (line.startsWith("SDT Map:") || line.startsWith("SDT Stream:")) {
                		// skip (handled as part of the binary attachment)
                	} else if (line.startsWith("Binary Attachment:")) {
//                		assert binary != null;
                    	printMsgTypeIfRequired(systemOut);
                    	AaAnsi payloadText = AaAnsi.n().a(line).a(" bytes");
                    	if (binary != null) handlePayloadSection(line, binary, payloadText);
                    	systemOut.println(payloadText);
                	} else if (line.startsWith("XML:")) {
//                		assert xml != null;
                    	printMsgTypeIfRequired(systemOut);
                    	AaAnsi payloadText = AaAnsi.n().fg(Elem.WARN).a("XML Payload section:           ").reset();
                    	payloadText.a("         ").a(line.substring(40)).a(" bytes").reset();
                    	if (xml != null) handlePayloadSection(line, xml, payloadText);
                    	systemOut.println(payloadText);
                	} else if (line.startsWith("Destination:           ")) {
                		systemOut.print("Destination:                            ");
                		colorizeDestForRegularMode();
                		systemOut.println(msgDestNameFormatted);  // just print out since it's already formatted
                	} else if (line.startsWith("Message Id:") && orig.getDeliveryMode() == DeliveryMode.DIRECT) {
                		// skip it, hide the auto-generated message ID on Direct messages
                	} else {  // everything else
//                		System.out.println(new AaAnsi().a(line));
                		systemOut.println(UsefulUtils.guessIfMapLookingThing(line));
                	}
                }
                if (!orig.hasContent() && !orig.hasAttachment()) {
                	printMsgTypeIfRequired(systemOut);
//                	systemOut.println(new AaAnsi().fg(Elem.PAYLOAD_TYPE).a(UsefulUtils.capitalizeFirst(msgType)).a(", <EMPTY PAYLOAD>").reset().toString());
                	systemOut.println(new AaAnsi().fg(Elem.PAYLOAD_TYPE).a("<NO PAYLOAD>").reset().toString());
                } else if (orig.hasAttachment() && orig.getAttachmentContentLength() == 0) {
                	printMsgTypeIfRequired(systemOut);
//                	systemOut.println(new AaAnsi().fg(Elem.PAYLOAD_TYPE).a(UsefulUtils.capitalizeFirst(msgType)).a(", <EMPTY PAYLOAD>").reset().toString());
                	systemOut.println(new AaAnsi().fg(Elem.PAYLOAD_TYPE).a("<EMPTY PAYLOAD>").reset().toString());
                	
                }
    			if (getEffectiveIndent() > 0) systemOut.println(printMessageEnd(msgCountNumber));
        	} else {  // one-line mode!
                updateTopicSpacing();  // now that we've determined if we're gonna filter this message, do the topic stuff
        		String payloadSizeString;
        		if (binary != null && xml != null) {
        			// that's not great for one-line printing!  but probably pretty rare!!!!
        			systemOut.println("Message contains both binary and XML payloads:");
        			systemOut.println(orig.dump().trim());  // raw JCSMP full dump
        		} else if (noPayload) {  // "-0" mode
    				systemOut.println(msgDestNameFormatted);
        		} else {  // one payload section defined, or empty
    				AaAnsi payload = null;
    				if (binary != null) {
    					payload = binary.formatted;
    					payloadSizeString = binary.getSizeString();
    				}
    				else if (xml != null) {
    					payload = xml.formatted;
    					payloadSizeString = xml.getSizeString();
    				}
    				else {
    					payload = AaAnsi.n().faintOn().a("<EMPTY> ").a(msgType).reset();  // hopefully an EMPTY message
    					payloadSizeString = "";  // empty payload is already the payload
    				}
//					int minPayloadStringSizeLength = payloadSizeString.length();
    				minLengthPayloadLength.add(payloadSizeString.length());
    				if (userProps != null) {
    					if (userProps.numElements == 0) maxLengthUserPropsList.add(0);
    					if (userProps.numElements < 10) maxLengthUserPropsList.add(1);
    					else if (userProps.numElements < 100) maxLengthUserPropsList.add(2);
    					else if (userProps.numElements < 100) maxLengthUserPropsList.add(3);  // assume 999 is the most!
    				}
    				else  maxLengthUserPropsList.add(0);
    				// we need all that in case we're in -1 mode and we need to trim
    				
    				if (getCurrentIndent() == 2) {  // two-line mode
						AaAnsi props = AaAnsi.n().fg(Elem.PAYLOAD_TYPE);
    					if (userProps != null) {
//    						props.a(userProps.numElements + (userProps.numElements == 1 ? " UserProp" : " UserProps")).reset();  // wiggles to much back and for with 1 / 2
    						props.a(userProps.numElements + " UserProps").reset();
    					} else {
    						props.faintOn().a("- UserProps").reset();
    					}
    					int spaceToAdd = currentScreenWidth - msgDestNameFormatted.length() - props.length();
    					if (spaceToAdd < 0) {
    	    				systemOut.print(msgDestNameFormatted.trim(msgDestNameFormatted.length() + spaceToAdd - 1)).print(" ");
    					} else {
    	    				systemOut.print(msgDestNameFormatted);
    	    				systemOut.print(UsefulUtils.pad(spaceToAdd, ' '));
    					}
    					systemOut.println(props);
    					systemOut.print("  ");
        				if (autoTrimPayload) {
        					if (payload.length() > currentScreenWidth - getCurrentIndent()) {
	        					systemOut.print(payload.trim(currentScreenWidth - getCurrentIndent() - payloadSizeString.length()));
	        					systemOut.println(payloadSizeString);
        					} else {  // enough space
        						systemOut.println(payload);
        					}
        				}
        				else {
        					systemOut.println(payload.reset());  // need the reset b/c a (fancy) string payload won't have the reset() at the end
        					if (getCurrentIndent() + payload.getTotalCharCount() > currentScreenWidth) systemOut.println();
        				}
    				} else {  // proper one-line mode!
    					final int PADDING_CORRECTION = 1;
        				int minPayloadLength = minLengthPayloadLength.getMax();
        				if (maxLengthUserPropsList.getMax() > 0) minPayloadLength += maxLengthUserPropsList.getMax() + 2 - PADDING_CORRECTION;  // user prop spacing + 1 + 1 for blackspace
        				int effectiveIndent = getCurrentIndent();
//        				System.out.printf("minPayloadLength=%d, maxLengthUserProps=%d%n", minLengthPayloadLength.getMax(), maxLengthUserPropsList.getMax());
        				if (autoResizeIndent) {  // mode -1
        					if (getCurrentIndent() > currentScreenWidth - minPayloadLength - 1) {  // need to trim the topic!   minPL = 11
        						effectiveIndent = currentScreenWidth - minPayloadLength - 1;
        						systemOut.print(msgDestNameFormatted.trim(effectiveIndent-1));
        						int spaceToAdd = effectiveIndent - Math.min(msgDestName.length(), effectiveIndent-1);
        						systemOut.print(UsefulUtils.pad(spaceToAdd + PADDING_CORRECTION, ' '));
        					} else {
        						systemOut.print(msgDestNameFormatted);
        						int spaceToAdd = getCurrentIndent() - msgDestName.length();
        						systemOut.print(UsefulUtils.pad(spaceToAdd + PADDING_CORRECTION, ' '));
        					}
        				} else {
        					systemOut.print(msgDestNameFormatted.trim(getCurrentIndent()-1));
        					int spaceToAdd = getCurrentIndent() - Math.min(msgDestName.length(), getCurrentIndent()-1);
        					systemOut.print(UsefulUtils.pad(spaceToAdd + PADDING_CORRECTION, ' '));
        				}
        				
    					AaAnsi props = AaAnsi.n().fg(Elem.PAYLOAD_TYPE);
//    					AaAnsi props = AaAnsi.n().insertBlackSpace().a(' ').fg(Elem.BYTES);
    					if (maxLengthUserPropsList.getMax() > 0) {
//    						props.blackOn();
    						if (userProps == null || userProps.numElements == 0) props.faintOn().a('-');
    						else props.a(userProps.numElements);
//    						props.blackOff();
    						props.a(' ');
    					}
    					systemOut.print(props.reset());
    					if (autoTrimPayload) {
    						int trimLength = currentScreenWidth - effectiveIndent - PADDING_CORRECTION - props.length();// - payloadSizeString.length();
    						if (payload.length() <= trimLength) {
    							systemOut.println(payload);
    						} else {
//    							System.out.printf("w=%d,in=%d,efIn=%d,MXpay=%d,len=%d,trim=%d%n", currentScreenWidth, getCurrentIndent(), effectiveIndent, minLengthPayloadLength.getMax(), payloadSizeString.length(), trimLength);
    							systemOut.print(payload.trim(trimLength - payloadSizeString.length())).println(payloadSizeString);
    						}
    					}
    					else {
    						systemOut.println(payload.reset());  // need the reset b/c a (fancy) string payload won't have the reset() at the end
//    					System.out.printf("charCount=%d, calcLength=%d, width=%d, size=%d%n", binary.formatted.getCharCount(), AaAnsi.length(binary.formatted.toString()),currentScreenWidth,Math.abs(INDENT)+binary.formatted.getCharCount());
    						if (getCurrentIndent() + payload.getTotalCharCount() > currentScreenWidth) systemOut.println();
    					}
    				}
        		}
        	}
			return systemOut;
        }
	}
	*/
	
    
    // Helper class, for printing message to the console ///////////////////////////////////////


	// Destination:                            Topic 'solace/samples/jcsmp/hello/aaron'
	public static AaAnsi colorizeDestination(Destination jcsmpDestination) {
		AaAnsi aa = new AaAnsi().fg(Elem.PAYLOAD_TYPE).a("Destination:                            ").fg(Elem.DESTINATION);
		if (jcsmpDestination instanceof Topic) {
			aa.a("Topic '").colorizeTopic(jcsmpDestination.getName(), -1).fg(Elem.DESTINATION).a("'");
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
			aa.a("Topic '").colorizeTopic(rest.substring(7, rest.length()-1), -1).fg(Elem.DESTINATION).a("'");
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

	
	public void handlePayloadSection(String line, PayloadSection ps, AaAnsi aa) {
		boolean invalid = ps.type != null && (ps.type.contains("non") || ps.type.contains("INVALID"));
		if (getFormattingIndent() == 0 || noPayload) {  // so compressed and/or no payload
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

	public void stop() {
		isStopped = true;
	}

    public void dealWithMessage(BytesXMLMessage message) {
    	if (isStopped) return;
    	msgCount++;
//    	currentScreenWidth = AnsiConsole.getTerminalWidth();
//    	if (currentScreenWidth == 0) currentScreenWidth = 80;  // for running in eclipse or others that don't return a good value
    	MessageHelper ms = new MessageHelper(message, msgCount);
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
	        		ms.binary.size = message.getAttachmentContentLength();
		            if (message instanceof MapMessage) {
	//	            	ms.msgType = "SDT MapMessage";
		            	ms.binary.formatted = SdtUtils.printMap(((MapMessage)message).getMap(), getFormattingIndent());
		            } else if (message instanceof StreamMessage) {
	//	            	ms.msgType = "SDT StreamMessage";
		            	// set directly
		            	ms.binary.formatted = SdtUtils.printStream(((StreamMessage)message).getStream(), getFormattingIndent());
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
			            			int currentScreenWidth = AnsiConsole.getTerminalWidth();
			            			if (currentScreenWidth == 0) currentScreenWidth = 80;
			            			ms.binary.formatted = UsefulUtils.printBinaryBytesSdkPerfStyle(bytes, getFormattingIndent(), currentScreenWidth);
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
										logger.warn("Had a message marked as 'gzip' but wasn't");
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
												ms.binary.formatted = ProtoBufUtils.decode(protoMsg, getFormattingIndent());
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
            	ms.userProps.formatted = SdtUtils.printMap(message.getProperties(), getFormattingIndent());
            	ms.userProps.numElements = SdtUtils.countElements(message.getProperties());
            }
            if (message.getUserData() != null && message.getUserData().length > 0) {
            	ms.userData = new PayloadSection();
            	ms.userData.formatBytes(message.getUserData(), null);
            	ms.userData.type = null;
            }
            
            // done preparing the message.  Now we might have to filter it?
//            ms.prepMessageForPrinting();
            if (filterRegexPattern != null) {
            	String rawDump = ms.buildFullStringObject();
            	if (!filterRegexPattern.matcher(rawDump).find()) {
            		incFilteredCount();
					if (isLastNMessagesEnabled()) {  // gathering
						ThinkingAnsiHelper.tick2(ThinkingAnsiHelper.makeStringGathered("Msg filtered.",
								getMessageCount(), getFilteredCount(), getMessageCount()-getFilteredCount(), getLastNMessagesCapacity()));
					} else {
						ThinkingAnsiHelper.tick2(ThinkingAnsiHelper.makeStringPrinted("Msg filtered.",
								getMessageCount(), getFilteredCount(), getMessageCount()-getFilteredCount()));
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
			if (isLastNMessagesEnabled()) {  // gathering
				lastNMessages.add(ms);
				ThinkingAnsiHelper.tick2(ThinkingAnsiHelper.makeStringGathered("",
						getMessageCount(), getFilteredCount(), getMessageCount()-getFilteredCount(), getLastNMessagesCapacity()));
//            	ThinkingAnsiHelper.tick("Gathering last " + lastNMessages.capacity() + " messages, received ");
            } else {
            	if (isStopped) return;  // stop if we're stopped!
            	if (ThinkingAnsiHelper.isFilteringOn()) ThinkingAnsiHelper.filteringOff();
                SystemOutHelper systemOut = ms.printMessage();
            	System.out.print(systemOut);
            }
        } catch (RuntimeException e) {  // really shouldn't happen!!
        	System.out.println(MessageHelper.printMessageStart());
        	System.out.println(AaAnsi.n().ex("Exception occured, check ~/.pretty/pretty.log for details. ", e));
        	logger.warn("Had issue parsing a message.  Message follows after exception.",e);
        	logger.warn(message.dump());
        	System.out.println(message.dump());
        	System.out.println(MessageHelper.printMessageEnd());
        }
    }
    
    

    
}
