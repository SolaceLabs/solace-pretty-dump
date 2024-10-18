package com.solace.labs.aaron;

import java.io.IOException;
import java.io.StringReader;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.fusesource.jansi.AnsiConsole;
import org.htmlunit.cyberneko.parsers.SAXParser;
import org.htmlunit.cyberneko.xerces.xni.XNIException;
import org.htmlunit.cyberneko.xerces.xni.parser.XMLInputSource;

import com.solace.labs.aaron.ConfigState.DisplayType;
import com.solace.labs.aaron.MessageHelper.PrettyMsgType;
import com.solace.labs.aaron.utils.DecoderUtils;
import com.solacesystems.jcsmp.impl.sdt.MapImpl;
import com.solacesystems.jcsmp.impl.sdt.MapTLVBuffer;
import com.solacesystems.jcsmp.impl.sdt.StreamImpl;
import com.solacesystems.jcsmp.impl.sdt.StreamTLVBuffer;

class PayloadSection {  // like, the XML payload and the binary payload; but also the user props (map) and user data (binary) could use this

    private static final Logger logger = LogManager.getLogger(PayloadSection.class);
	

	final ConfigState config;
	int size = 0;
	String type = null;  // might initialize later if JSON or XML
	AaAnsi formatted = AaAnsi.n().fg(Elem.NULL).a("<UNINITIALIZED>");  // to ensure gets overwritten
	int numElements = 0;  // primarily for User Properties SDTMap element count
	
	PayloadSection(ConfigState config) {
		this.config = config;
	}
	
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
	void formatString(final String text, final byte[] bytes, boolean decodedFromBytes) {
		formatString(text, bytes, null, decodedFromBytes);
	}
	
	void formatString(final String text, final byte[] bytes, String contentType, boolean decodedFromBytes) {
		if (contentType == null) contentType = "";  // empty string, for easier matching later
		size = bytes.length;
		if (text == null) {  // that shouldn't happen?
			formatted = AaAnsi.n();
			type = "<NULL>";
			return;
		} else if (text.isEmpty()) {
			formatted = AaAnsi.n();
			type = "<EMPTY>";
			if (size != 0 && size != 3 && size != 6) {
				logger.warn("Empty string passed, but bytes has length " + size + ". This is unexpected.");
			}
			return;
		}
		if (config.payloadDisplay == DisplayType.RAW) {  // leave completely alone
			formatted = AaAnsi.n().a(text);  // nice default colour
			if (decodedFromBytes) type = config.charset.displayName() + " encoded string";
		} else {
			String trimmed = text.trim();
	    	if ("application/json".equals(contentType)) {  // guess it's JSON?
	    		try {
	        		formatted = GsonUtils.parseJsonDunnoWhich(trimmed, config.getFormattingIndent());
	    			type = (decodedFromBytes ? config.charset.displayName() + " charset, " : "") + "JSON " + (trimmed.charAt(0) == '{' ? "OBJECT":"ARRAY");
				} catch (IOException e) {
					type = (decodedFromBytes ? config.charset.displayName() + " charset, " : "") + "INVALID JSON payload";
	//    			formatted = new AaAnsi().setError().a("ERROR: ").a(e.getMessage()).reset().a('\n').a(text).reset().toString();
	    			formatted = AaAnsi.n().ex(e).a('\n').a(trimmed);
				}
	    	} else if (trimmed.startsWith("{") && trimmed.endsWith("}")) {  // try JSON object
	    		try {
	        		formatted = GsonUtils.parseJsonObject(trimmed, config.getFormattingIndent());
	    			type = (decodedFromBytes ? config.charset.displayName() + " charset, " : "") + "JSON Object";
				} catch (IOException e) {
					type = (decodedFromBytes ? config.charset.displayName() + " charset, " : "") + "INVALID JSON payload";
	//    			formatted = new AaAnsi().setError().a("ERROR: ").a(e.getMessage()).reset().a('\n').a(text).reset().toString();
	    			formatted = AaAnsi.n().ex(e).a('\n').a(trimmed);
				}
	    	} else if (trimmed.startsWith("[") && trimmed.endsWith("]")) {  // try JSON array
	    		try {
	        		formatted = GsonUtils.parseJsonArray(trimmed, config.getFormattingIndent());
	        		type = (decodedFromBytes ? config.charset.displayName() + " charset, " : "") + "JSON Array";
	    		} catch (IOException e) {
	    			type = (decodedFromBytes ? config.charset.displayName() + " charset, " : "") + "INVALID JSON payload";
	//    			formatted = new AaAnsi().setError().a("ERROR: ").a(e.getMessage()).reset().a('\n').a(text).reset().toString();
	    			formatted = AaAnsi.n().ex(e).a('\n').a(trimmed);
				}
	    	} else if ((trimmed.startsWith("<") && trimmed.endsWith(">")) ||
	    			"application/xml".equals(contentType)  || contentType.contains("text/xml") ||
	    			"text/html".equals(contentType)  || contentType.contains("html")) {  // try XML
	    		// I'm looking for the ASCII substitution char, and replacing it with � what is that?  FFFD?
	//    		String substitutionReplacedTrimmed = trimmed.replaceAll("\\Q\u001a\\E", "� ");
	    		String substitutionReplacedTrimmed = trimmed.replaceAll("\\Q\u001a\\E", "\ufffd");
				try {
					if ("application/xml".equals(contentType)  || contentType.contains("text/xml")) throw new SaxParserException("");  // throw to the catch for HTML processing
					SaxHandler handler = new SaxHandler(config.getFormattingIndent());
					SaxParser.parseString(substitutionReplacedTrimmed, handler);
	                formatted = handler.getResult();  // overwrite
	                type = (decodedFromBytes ? config.charset.displayName() + " charset, " : "") + "XML document";
				} catch (SaxParserException e) {
					// maybe it's HTML?
					final StringReader sr = new StringReader(substitutionReplacedTrimmed);
					final XMLInputSource htmlInputSource = new XMLInputSource(null, "foo", null, sr, config.charset.name());
					final SAXParser htmlParser = new SAXParser();
					SaxHandler handler = new SaxHandler(config.getFormattingIndent(), false);
					htmlParser.setContentHandler(handler);
					// this doesn't work that well... first<p>second<p>third<p> become <p>second</p><p>third</p>
					// probably works well on modern html that have proper open/close tags
					try {
						htmlParser.parse(htmlInputSource);
		                formatted = handler.getResult();  // overwrite
		                type = (decodedFromBytes ? config.charset.displayName() + " charset, " : "") + "HTML document";
					} catch (XNIException | IOException e1) {
						logger.warn("Couldn't parse xml or html", e);
						type = (decodedFromBytes ? config.charset.displayName() + " charset, " : "") + "INVALID XML payload";
		    			formatted = AaAnsi.n().ex(e).a('\n').a(trimmed);
					}
				}
	    	} else {  // it's neither JSON or XML, but has text content
	//    		type = charset.displayName() + " String";
	    		if (decodedFromBytes) type = config.charset.displayName() + " encoded string";
	    		formatted = AaAnsi.n().aStyledString(text).reset();
	//    		formatted = text;
	    	}
		}
		boolean malformed = text.contains("\ufffd");
//		System.out.println("MALFORMED: " + malformed);
    	if (malformed) {
    		// should be impossible for type == null, but just in case.  means that TextMessage, UTF-8, but contains replacement char
			type = "non " + (type == null ? StandardCharsets.UTF_8.displayName() + " encoded string" : type);
    	}
    	if (config.payloadDisplay != DisplayType.RAW) {
	    	double ratio = (1.0 * formatted.getControlCharsCount() + formatted.getReplacementCharsCount()) / formatted.getTotalCharCount();
			if ((malformed && ratio >= 0.25) || config.payloadDisplay == DisplayType.DUMP) {  // 25%, very likely a binary file
				formatted = UsefulUtils.printBinaryBytesSdkPerfStyle(bytes, config.getFormattingIndent(), AnsiConsole.getTerminalWidth());
			} else if (malformed || formatted.getControlCharsCount() > 0 || formatted.getReplacementCharsCount() > 0) {  // any unusual control chars (not tab, LF, CR, FF, or Esc, or NUL at string end
				if (!config.oneLineMode && config.getFormattingIndent() > 0) {  // only if not in one-line mode!
					formatted.a('\n').aa(UsefulUtils.printBinaryBytesSdkPerfStyle(bytes, config.getFormattingIndent(), AnsiConsole.getTerminalWidth()));
				}
	    	}
    	}
	}

	void formatBytes(byte[] bytes, String contentType) {
		String parsed = DecoderUtils.decodeToString(config.decoder, bytes);
//		boolean malformed = parsed.contains("\ufffd");
		formatString(parsed, bytes, contentType, true);  // call the String version
		if (!type.startsWith("non") && size > 0) {
			if (formatted.getControlCharsCount() > 0 || formatted.getReplacementCharsCount() > 0) {
				type = "technically valid " + type;
				if (formatted.getReplacementCharsCount() > 0) type += " (contains replacement chars)";
			}
			else type = "valid " + type;
		}
//		if (bytes[0] == 0x1c || bytes[0] == 0x1c || bytes[0] == 0x1c || bytes[0] == 0x1c || bytes[0] == 0x1c || bytes[0] == 0x1c || )
//		formatByteBufferFromWrapMode(ByteBuffer.wrap(bytes));
	}
	
/*	private boolean checkIfSdtMap(byte[] bytes) {
		// check the size matches correctly:
		int size = buffer.getInt();  // next 4 bytes
//		System.out.println("size bytes ("+size+") and buffer limit ("+buffer.limit()+")!");
		if (buffer.limit() == size) {  // looks correct!  otherwise maybe just a regular binary msg
			byte[] copy = null;
			copy = Arrays.copyOfRange(buffer.array(), 0, buffer.limit());
			MapTLVBuffer buf = new MapTLVBuffer(copy);  // sneaky hidden but public methods
			MapImpl map = new MapImpl(buf);
//			String test = SdtUtils.printMap(map, 3).toString();
			formatted = SdtUtils.printMap(map, config.getFormattingIndent());
        	type = PrettyMsgType.MAP.toString();  // hack, should be in the msg helper object
			return;
		} else {
			buffer.rewind();  // put back to beginning
		}
	}
*/
	
	void formatByteBufferFromWrapMode(ByteBuffer buffer) {
		byte[] copy = null;
		String tempType = null;
		byte first = buffer.get();  // check out the first byte
		if (first == 0x1c) {  // text message, one byte of size
			int size = Byte.toUnsignedInt(buffer.get());
			if (buffer.limit() != size) throw new IllegalArgumentException("size byte ("+size+") did not match buffer limit ("+buffer.limit()+")!");
			buffer.limit(buffer.limit()-1);  // text messages are null terminated, so don't read the trailing null
			tempType = PrettyMsgType.TEXT.toString();
//			System.out.println("0x1c SDT TextMessage detected, byte lenght: " + size);
		} else if (first == 0x1d) {  // text message, 2 bytes of size
			int size = Short.toUnsignedInt(buffer.getShort());
			if (buffer.limit() != size) throw new IllegalArgumentException("size bytes ("+size+") did not match buffer limit ("+buffer.limit()+")!");
//			System.out.println("0x1d SDT TextMessage detected, byte lenght: " + size);
			buffer.limit(buffer.limit()-1);  // text messages are null terminated, so don't read the trailing null
			tempType = PrettyMsgType.TEXT.toString();
		} else if (first == 0x1e) {  // text message, 3 bytes of size
			int size = Byte.toUnsignedInt(buffer.get()) << 16;
			size |= buffer.getShort();
			if (buffer.limit() != size) throw new IllegalArgumentException("size bytes ("+size+") did not match buffer limit ("+buffer.limit()+")!");
			buffer.limit(buffer.limit()-1);  // text messages are null terminated, so don't read the trailing null
			tempType = PrettyMsgType.TEXT.toString();
//			System.out.println("0x1e SDT TextMessage detected, byte lenght: " + size);
		} else if (first == 0x1f) {  // text message, 4 bytes of size
			int size = buffer.getInt();
			if (buffer.limit() != size) throw new IllegalArgumentException("size bytes ("+size+") did not match buffer limit ("+buffer.limit()+")!");
			buffer.limit(buffer.limit()-1);  // text messages are null terminated, so don't read the trailing null
//			System.out.println("0x1f SDT TextMessage detected, byte lenght: " + size);
			tempType = PrettyMsgType.TEXT.toString();
		} else if (first == 0x2b) {  // SDT Map?
			// check the size matches correctly:
			int size = buffer.getInt();  // next 4 bytes
//			System.out.println("size bytes ("+size+") and buffer limit ("+buffer.limit()+")!");
			if (buffer.limit() == size) {  // looks correct!  otherwise maybe just a regular binary msg
//				copy = Arrays.copyOfRange(buffer.array(), 0, buffer.limit());  // need to do this to copy out the whole bytes!
//				// this should work too, ah but might not work if we've advanced some position in the buffer
				// nope, should be fine: https://docs.oracle.com/javase/8/docs/api/java/nio/ByteBuffer.html#get-byte:A-
				copy = new byte[buffer.limit()];
				buffer.get(copy);
				MapTLVBuffer buf = new MapTLVBuffer(copy);  // sneaky hidden but public methods
				MapImpl map = new MapImpl(buf);
//				String test = SdtUtils.printMap(map, 3).toString();
				formatted = SdtUtils.printMap(map, config.getFormattingIndent());
            	type = PrettyMsgType.MAP.toString();  // hack, should be in the msg helper object
				return;
			} else {
				buffer.rewind();  // put back to beginning
			}
		} else if (first == 0x2f) {  // SDT Stream?
			// check the size matches correctly:
			int size = buffer.getInt();  // next 4 bytes
//			System.out.println("size bytes ("+size+") and buffer limit ("+buffer.limit()+")!");
			if (buffer.limit() == size) {  // looks correct!  otherwise maybe just a regular binary msg
				copy = Arrays.copyOfRange(buffer.array(), 0, buffer.limit());
				StreamTLVBuffer buf = new StreamTLVBuffer(copy);  // sneaky hidden but public methods
				StreamImpl map = new StreamImpl(buf);
				formatted = SdtUtils.printStream(map, config.getFormattingIndent());
            	type = PrettyMsgType.STREAM.toString();
				return;
			} else {
				buffer.rewind();  // put back to beginning
			}
		} else {
			buffer.rewind();  // put back to beginning
//			tempType = PrettyMsgType.BYTES.toString();
		}
		// if we're here, then either a TextMessage that we've shifted around or binary message ready to be read
//		pos = buffer.position();
//		limit = buffer.limit();
//		capacity = buffer.capacity();
//		System.out.printf("pos: %d, lim: %d, cap: %d%n", pos, limit, capacity);
		copy = Arrays.copyOfRange(buffer.array(), 0, buffer.limit());
		String parsed = DecoderUtils.decodeToString(config.decoder, copy);
//		boolean malformed = parsed.contains("\ufffd");
		formatString(parsed, copy, true);  // call the String version
		if (tempType != null) type = tempType + ", " + type;
//    	if (malformed) {
//			type = "Non " + type;
//			if (INDENT > 0) {
//				formatted.a('\n').a(UsefulUtils.printBinaryBytesSdkPerfStyle(Arrays.copyOfRange(buffer.array(), 0, buffer.limit()), INDENT, currentScreenWidth));
//			}
//    	}
	}	

}
