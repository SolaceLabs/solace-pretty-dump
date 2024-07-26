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

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Pattern;

import com.solacesystems.common.util.ByteArray;

public class UsefulUtils {

//	private static Charset DEFAULT_CHARSET = StandardCharsets.ISO_8859_1;
//	private static CharsetDecoder DECODER = DEFAULT_CHARSET.newDecoder();

	public static final char[] HARDCODED = new char[] {
//			'€','·','‚','ƒ','„','…','†','‡','ˆ','‰','Š','‹','Œ','·','Ž','·',  // from win-1252
//			'·','‘','’','“','”','•','–','—','˜','™','š','›','œ','·','ž','Ÿ',  // from win-1252
//			'·','·','·','·','·','·','·','·','·','·','·','·','·','·','·','·',  // unused in iso-8859-1
//			'·','·','·','·','·','·','·','·','·','·','·','·','·','·','·','·',  // unused in iso-8859-1
//			' ','¡','¢','£','¤','¥','¦','§','¨','©','ª','«','¬','­','®','¯',
//			'°','±','²','³','´','µ','¶','·','¸','¹','º','»','¼','½','¾','¿',
//			'À','Á','Â','Ã','Ä','Å','Æ','Ç','È','É','Ê','Ë','Ì','Í','Î','Ï',
//			'Ð','Ñ','Ò','Ó','Ô','Õ','Ö','×','Ø','Ù','Ú','Û','Ü','Ý','Þ','ß',
//			'à','á','â','ã','ä','å','æ','ç','è','é','ê','ë','ì','í','î','ï',
//			'ð','ñ','ò','ó','ô','õ','ö','÷','ø','ù','ú','û','ü','ý','þ','ÿ',

			'·','·','·','·','·','·','·','·','·','·','·','·','·','·','·','·',
			'·','·','·','·','·','·','·','·','·','·','·','·','·','·','·','·',
			'·','·','·','·','·','·','·','·','·','·','·','·','·','·','·','·',
//			'Ç','ü','é','â','ä','à','å','ç','ê','ë','è','ï','î','ì','Ä','Å',  // DOS/IBM437 yeah!
//			'É','æ','Æ','ô','ö','ò','û','ù','ÿ','Ö','Ü','¢','£','¥','₧','ƒ',
//			'á','í','ó','ú','ñ','Ñ','ª','º','¿','⌐','¬','½','¼','¡','«','»',
			'·','·','·','·','·','·','·','·','·','·','·','·','·','·','·','·',
			'·','·','·','·','·','·','·','·','·','·','·','·','·','·','·','·',
			'·','·','·','·','·','·','·','·','·','·','·','·','·','·','·','·',
//			'░','▒','▓','│','┤','╡','╢','╖','╕','╣','║','╗','╝','╜','╛','┐',
//			'└','┴','┬','├','─','┼','╞','╟','╚','╔','╩','╦','╠','═','╬','╧',
//			'╨','╤','╥','╙','╘','╒','╓','╫','╪','┘','┌','█','▄','▌','▐','▀',
			'·','·','·','·','·','·','·','·','·','·','·','·','·','·','·','·',
			'·','·','·','·','·','·','·','·','·','·','·','·','·','·','·','·',
//			'α','ß','Γ','π','Σ','σ','µ','τ','Φ','Θ','Ω','δ','∞','φ','ε','∩',
//			'≡','±','≥','≤','⌠','⌡','÷','≈','°','∙','·','√','ⁿ','²','■','·',
			'·','·','·','·','·','·','·','·','·','·','·','·','·','·','·','·',
			'·','·','·','·','·','·','·','·','·','·','·','·','·','·','·','·',
//			/* '╳'*/'∅'/*'Ø'*/,'☺','☻','♥','♦','♣','♠','•','◘','○','◙','♂','♀','♪','♫','☼',  // how to represent NULL?
//			'►','◄','↕','‼','¶','§','▬','↨','↑','↓','→','←','∟','↔','▲','▼',
			/* '␣', */' ','!','"','#','$','%','&','\'','(',')','*','+',',','-','.','/',
			'0','1','2','3','4','5','6','7','8','9',':',';','<','=','>','?',
			'@','A','B','C','D','E','F','G','H','I','J','K','L','M','N','O',
			'P','Q','R','S','T','U','V','W','X','Y','Z','[','\\',']','^','_',
			'`','a','b','c','d','e','f','g','h','i','j','k','l','m','n','o',
			'p','q','r','s','t','u','v','w','x','y','z','{','¦','}','~','⌂'
		};
	
/*	private static char[] getCharArray(byte[] orig) {
		ByteBuffer buffer = ByteBuffer.wrap(orig);
		String decoded;
		try {
//			decoded = DECODER.decode(buffer).toString();  // won't throw b/c it can map every single byte successfully
//			DECODER.decode(buffer).
			char[] arr2 = DECODER.decode(buffer).array();
			for (int i=0; i<arr2.length; i++) {
//				char c = decoded.charAt(i);
				char c = arr2[i];
				if ((c >= 0 && c < 32) || c >= 127 && c < 160) {
					arr2[i] = '·';
//				} else {
//					arr2[i] = c;
				}
			}
			return arr2;
		} catch (CharacterCodingException e) {  // won't happen with ISO 8859-1
			e.printStackTrace();
			System.exit(-1);
			return null;
		}
	}*/
	
	public static char getSimpleChar2(byte orig) {
		return HARDCODED[orig + 128];
	}
	
/*	private static char getSimpleChar(byte orig) {
		
//		if ((orig >= 0 && orig < 32) || orig == 127) {  // control char
		if ((orig >= 0 && orig < 32) || orig == 127 || orig <= -96) {  // control char
			return '·';
			// return '�';
			// ·
			// •
			// ·
			// '�';
		}
		if (orig >= 32) {  // normal ASCII
			return (char)orig;
		}
		// else, the byte is < 0, and so we're in "extended ascii"
		ByteBuffer bb = ByteBuffer.wrap(new byte[] { orig });
		return DEFAULT_CHARSET.decode(bb).charAt(0);
		
		
//		if ((orig >= 0 && orig < 32) || orig == 127 || orig < 0) {  // control char
//			orig = 46;  // a period .
//		}
//		return '�';
//		return (char)orig;
	}*/
	
/*	private static String getSimpleString(byte[] orig) {
		byte[] bytes = Arrays.copyOf(orig, orig.length);
		for (int i=0; i < bytes.length; i++) {
			if (bytes[i] <= 32 || bytes[i] == 127) {  // negative, control char or space
				bytes[i] = 46;  // a period .
			}
		}
		return new String(bytes, StandardCharsets.US_ASCII);
	}
*/	
	/**
	 * Removes only trailing '\n' chars (unlike String.trim())
	 * @param s
	 * @return
	 */
	static String chop(String s) {
		if (s.endsWith("\n") || s.endsWith("\r")) {
			// recursive call!
			return chop(s.substring(0, s.length()-1));  // just in case there's 2 trailing carriage returns?
		}
		return s;
	}
	
//	private static final byte[] HEX_ARRAY_BYTES = "0123456789abcdef".getBytes(StandardCharsets.US_ASCII);
//	private static final char[] HEX_ARRAY_CHARS = "0123456789abcdef".getBytes(StandardCharsets.US_ASCII);
	private static final String[] BYTE_REPS = new String[256];  // static config, only 256 possible Strings
	static {
		/* private static final */char[] TEST = { '0','1','2','3','4','5','6','7','8','9','a','b','c','d','e','f' };
		for (int i=0; i<16; i++) {
			for (int j=0; j<16; j++) {
				BYTE_REPS[(i * 16) + j] = new StringBuilder().append(TEST[i]).append(TEST[j]).toString();
			}
		}
	}
	
	public static String getByteRepresentation(byte b) {
		if (b < 0) return BYTE_REPS[b + 256];
		return BYTE_REPS[b];
	}

	/**
	 * Returns a String that's just a long representation of bytes: "517f0c20b3524d"
	 * @param bytes
	 * @return a String that's just a long representation of bytes: "517f0c20b3524d"
	 */
	@SuppressWarnings("unused")
	private static String bytesToLongHexString(byte[] bytes) {
	    byte[] hexChars = new byte[bytes.length * 2];  // twice as many, two chars for each byte (e.g. 7F, 0C)
	    StringBuilder sb = new StringBuilder(bytes.length * 2);
	    for (int i = 0; i < bytes.length; i++) {
//	        int v = bytes[j] & 0xFF;
//	        hexChars[j * 2] = HEX_ARRAY_BYTES[v >>> 4];
//	        hexChars[j * 2 + 1] = HEX_ARRAY_BYTES[v & 0x0F];
	    	sb.append(getByteRepresentation(bytes[i]));
	    }
//	    return new String(hexChars, StandardCharsets.US_ASCII);
	    return sb.toString();
	}

	/**
	 * Returns a nicer-looking String: "[51,7f,0c,20,b3,52,4d] (len=7)"
	 * @param bytes
	 * @return
	 */
	// now it's comma separated..!
	public static String bytesToSpacedHexString(byte[] bytes) {
		if (bytes.length == 0) return "[]";
		StringBuilder sb = new StringBuilder("[");
//        sb.append((char)HEX_ARRAY_BYTES[((int)bytes[0] & 0xFF) >>> 4]).append((char)HEX_ARRAY_BYTES[bytes[0] & 0x0F]);
//        sb.append(TEST[((int)bytes[0] & 0xFF) >>> 4]).append(TEST[bytes[0] & 0x0F]);
        sb.append(getByteRepresentation(bytes[0]));
        if (bytes.length > 1) {
//	    byte[] hexChars = new byte[bytes.length * 2];
        	for (int i = 1; i < bytes.length; i++) {
//        		int v = bytes[i] & 0xFF;
//        		sb.append(' ').append(TEST[v >>> 4]).append(TEST[v & 0x0F]);
                sb.append(',').append(getByteRepresentation(bytes[i]));
        	}
        }
        sb.append(']');
        if (bytes.length > 8) sb.append(" (len=").append(bytes.length).append(')');
        return sb.toString();
//	    return new String(hexChars, StandardCharsets.US_ASCII);
	}

	
	/** for IP addresses */
	public static String ipAddressBytesToIpV4String(byte[] bytes) {
		assert bytes.length == 4;
		StringBuilder sb = new StringBuilder(" (");
        sb.append(Byte.toUnsignedInt(bytes[0]));
    	for (int i = 1; i < 4; i++) {
    		sb.append('.').append(Byte.toUnsignedInt(bytes[i]));
    	}
        return sb.append(')').toString();
	}

	public static String[] bytesToHexStringArray(byte[] bytes) {
//	    byte[] hexChars = new byte[2];
	    String[] blah = new String[bytes.length];
	    for (int j = 0; j < bytes.length; j++) {
//	        int v = bytes[j] & 0xFF;
//	        hexChars[0] = HEX_ARRAY_BYTES[v >>> 4];
//	        hexChars[1] = HEX_ARRAY_BYTES[v & 0x0F];
//	        blah[j] = new String(hexChars, StandardCharsets.US_ASCII);
	    	blah[j] = getByteRepresentation(bytes[j]);
	    }
	    return blah;
	}

	@SuppressWarnings("unused")
	private static String printBinaryBytesSdkPerfStyle2(byte[] bytes) {
		return printBinaryBytesSdkPerfStyle2(bytes, 0);
	}

	private static String printBinaryBytesSdkPerfStyle2(byte[] bytes, int indent) {
		ByteArray ba = new ByteArray(bytes);
//		byte[] ba2 = ba.asBytes();
		if (ba.getLength() > 100) {
			String ts = ba.toString();  // this toString() only returns the first 100 bytes of the array
			ts = ts.replace("]", ",...]");  // to indicate there is more
			return indent(indent) + ts;
		}
		return indent(indent) + ba.toString();
	}
	
//	static AaAnsi printBinaryBytesSdkPerfStyle(byte[] bytes, int indent) {
//		return printBytes(bytes, indent, 16);  // default
//	}
//
//	static AaAnsi printBinaryBytesSdkPerfStyle(byte[] bytes) {
//		return printBytes(bytes, 4, 16);  // default
//	}
	
	static AaAnsi printBinaryBytesSdkPerfStyle(byte[] bytes, int indent, int terminalWidth) {
		if (indent <= 0) {
//			return new AaAnsi().reset().a("[").fg(Elem.BYTES_CHARS).a(getSimpleString(bytes)).reset().a("]").toString();  // just a long string of chars
//			return new AaAnsi().reset().fg(Elem.BYTES).a(printBinaryBytesSdkPerfStyle2(bytes)).reset();  // byte values
			return new AaAnsi().reset().fg(Elem.BYTES).a(bytesToSpacedHexString(bytes)).reset();  // byte values
		}
		if (terminalWidth > 151) return printBytes2(bytes, indent, 32);  // widescreen
		else return printBytes2(bytes, indent, 16);
	}
	
//	static final int WIDTH = 32;
	static final int COLS = 8;
	
	/*  Orig SdkPerf dump format:
	  1d 00 a3 5b 7b 22 6e 61    6d 65 22 3a 22 74 65 73    ...[{"name":"tes
	  74 20 70 72 6f 64 75 63    74 22 2c 22 71 75 61 6e    t.product","quan
	  74 69 74 79 22 3a 35 2c    22 70 72 69 63 65 22 3a    tity":5,"price":
	  31 30 2e 39 39 2c 22 74    6f 74 61 6c 22 3a 35 30    10.99,"total":50
	  7d 2c 7b 22 6e 61 6d 65    22 3a 22 43 72 65 61 74    },{"name":"Creat
		 */
	
	/** this should only be called if we know it's not a UTF-8 (or whatever) string */
	private static AaAnsi printBytes(byte[] bytes, int indent, int width) {
		indent = 2;  // force override, 2 is what SdkPerf does too
//		String[] hex = bytesToHexStringArray(bytes);
		String hex2 = bytesToLongHexString(bytes);
		AaAnsi aa = new AaAnsi();
		for (int i=0; i < bytes.length; i++) {
			if (i % width == 0) {
				aa.a(indent(indent)).fg(Elem.BYTES);
			}
//			ansi.a(hex[i]).a(" ");
			aa.a(hex2.substring(i*2, (i*2)+2)).a(" ");
			if (i % COLS == COLS-1) {
				aa.a("   ");
			}
			if (i % width == width-1) {
				aa.fg(Elem.BYTES_CHARS);
				for (int j=i-(width-1); j<=i; j++) {
					aa.a(getSimpleChar2(bytes[j]));
					if (j % 8 == 7 && j != i) aa.a("--");
//					if (j % 16 == 15) ansi.a(" ");
				}
				aa.reset().a('\n');
//				if (i < bytes.length-1 || indent > 0) ansi.a('\n');
			}
		}
		// last trailing bit, if not evenly divisible by WIDTH
		// works for everthing except 24
		if (bytes.length % width != 0) {
			aa.reset();
			int leftover = bytes.length % width;
			for (int i=0; i < width - leftover; i++) {
				aa.a("   ");
			}
			int extraGaps = (width - leftover - 1) / COLS;
			for (int i=0; i <= extraGaps; i++) {
				aa.a("   ");
			}
			aa.fg(Elem.BYTES_CHARS);
			for (int i= bytes.length - leftover; i<bytes.length; i++) {
				aa.a(getSimpleChar2(bytes[i]));
				if (i % 8 == 7) aa.a("  ");
//				if (i % 16 == 15) ansi.a(" ");
			}
			aa.reset();
//			if (indent > 0) ansi.a('\n');
		}
		return aa;
	}

	/** this should only be called if we know it's not a UTF-8 (or whatever) string */
	private static AaAnsi printBytes2(byte[] bytes, int indent, int width) {
		// width must be either 16 or 32 for wide-screen
		indent = 2;  // force override, 2 is what SdkPerf does too
//		String[] hex = bytesToHexStringArray(bytes);
		String hex2 = bytesToLongHexString(bytes);
		AaAnsi aa = new AaAnsi();
		int roundedLenghth = (int)(Math.ceil(bytes.length * 1.0 / width) * width);  
		for (int i=0; i < roundedLenghth; i++) {
			if (i % width == 0) {
				// some extra row values to show the complete hex code here
				aa.fg(Elem.DATA_TYPE).a(String.format("%04x",(i / 16) % (4096))).a('0').a(' ').a(' ').a(' ').fg(Elem.BYTES);
			}
//			ansi.a(hex[i]).a(" ");
			if (i < bytes.length) aa.a(hex2.substring(i*2, (i*2)+2)).a(' ');
			else aa.a('⋅').a('⋅').a(' ');
			if (i % COLS == COLS-1) {
				aa.a(' ').a(' ');
			}
			if (i % width == width-1) {
				aa.a(' ').fg(Elem.BYTES_CHARS);
				for (int j=i-(width-1); j<=i; j++) {
					if (j < bytes.length) {
						aa.a(getSimpleChar2(bytes[j]));
						if (j % 8 == 7 && j != i) aa.a(' ').a(' ');
					}
//					if (j % 16 == 15) ansi.a(" ");
				}
				aa.reset();
				if (i < bytes.length-1) aa.a('\n');
//				if (i < bytes.length-1 || indent > 0) ansi.a('\n');
			}
		}
		if ("1".equals("1")) return aa;
		// last trailing bit, if not evenly divisible by WIDTH
		// works for everthing except 24
		if (bytes.length % width != 0) {
			aa.reset();
			int leftover = bytes.length % width;
			for (int i=0; i < width - leftover; i++) {
				aa.a(' ').a(' ').a(' ');
			}
			int extraGaps = (width - leftover - 1) / COLS;
			for (int i=0; i <= extraGaps; i++) {
				aa.a(' ').a(' ');
			}
			aa.a(' ');
			aa.fg(Elem.BYTES_CHARS);
			for (int i= bytes.length - leftover; i<bytes.length; i++) {
				aa.a(getSimpleChar2(bytes[i]));
				if (i % 8 == 7) aa.a(' ').a(' ');
//				if (i % 16 == 15) ansi.a(" ");
			}
			aa.reset();
//			if (indent > 0) ansi.a('\n');
		}
		return aa;
	}

	
	static String indent(int amount) {
		return pad(amount, ' ');
	}

	static String pad(int amount, char c) {
		if (Math.abs(amount) > 500) {
			return "  <CODING PROBLEM! Tell Aaron>  ";
		}
		StringBuilder sb = new StringBuilder();
		for (int i=0; i<amount; i++) {
			sb.append(c);
		}
		return sb.toString();
	}
	
	
	
	
	
/*	static void reflectionUtils(String className) throws IOException {
		try {
			Class<?> clazz = Class.forName(className);
			reflectionUtils(clazz);
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} finally {
			
		}
	}
	

	static void reflectionUtils(Class<?> clazz) throws IOException {
		
		try {
			for (Method m : clazz.getMethods()) {
				System.out.println(m);
			}
			Class<?>[] inners = clazz.getClasses();
			for (Class<?> inner : inners) {
				System.out.printf("###################%n%s%n",inner.getName());
//				System.in.read();
//				reflectionUtils(inner);
			}
			reflectionListInners(clazz);
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		}
	}

	
	static void reflectionListInners(Class<?> clazz) throws IOException {
		BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
		try {
			Class<?>[] inners = clazz.getClasses();
			for (Class<?> inner : inners) {
				System.out.println(inner.getName());
//				reader.readLine();
				if (inner.getName().startsWith(clazz.getName())) reflectionListInners(inner);
			}
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		}
	}*/


	// 5 year range around whatever today is
	static final int YEAR_RANGE = 5;
	static final long LOWER_MS = System.currentTimeMillis() - (1000L * Math.round(60 * 60 * 24 * 365.25 * YEAR_RANGE));
	static final long UPPER_MS = System.currentTimeMillis() + (1000L * Math.round(60 * 60 * 24 * 365.25 * YEAR_RANGE));
	// regex: either(start of string, or non-alpha) + ms + either(end of string, ec, or non-alpha)
	// e.g. elapsed-ms, elapsed-msec, ms, msec, ms.duration;   msDuration won't work
	static final Pattern PATTERN_MS = Pattern.compile("(?:^|[^a-zA-Z])ms(?:$|ec|[^a-zA-Z])");
	static final Pattern PATTERN_US = Pattern.compile("(?:^|[^a-zA-Z])us(?:$|ec|[^a-zA-Z])");
	static final Pattern PATTERN_NS = Pattern.compile("(?:^|[^a-zA-Z])ns(?:$|ec|[^a-zA-Z])");
	static final Set<String> TIME_SEC_KEYWORDS = new LinkedHashSet<>();
	static final Set<String> TIME_MS_KEYWORDS = new LinkedHashSet<>();
	static final Set<String> TIME_US_KEYWORDS = new LinkedHashSet<>();  // micros
	static final Set<String> TIME_NS_KEYWORDS = new LinkedHashSet<>();
	static {
		TIME_SEC_KEYWORDS.add("time");
		TIME_SEC_KEYWORDS.add("sec");
		TIME_SEC_KEYWORDS.add("epoch");
		TIME_SEC_KEYWORDS.add("stamp");
		
		TIME_MS_KEYWORDS.add("time");
		TIME_MS_KEYWORDS.add("milli");
		TIME_MS_KEYWORDS.add("epoch");
		TIME_MS_KEYWORDS.add("stamp");
		
		TIME_US_KEYWORDS.add("time");
		TIME_US_KEYWORDS.add("micro");
		TIME_US_KEYWORDS.add("epoch");
		TIME_US_KEYWORDS.add("stamp");
		
		TIME_NS_KEYWORDS.add("time");
		TIME_NS_KEYWORDS.add("nano");
		TIME_NS_KEYWORDS.add("epoch");
		TIME_NS_KEYWORDS.add("stamp");
	}
	
	private static boolean timeNameContains(Set<String> setToCheck, String keyName) {
		for (String keyWord : setToCheck) {
			if (keyName.contains(keyWord)) return true;
		}
		return false;
	}
	
//	private static final String PATTERN = " '('yyyy-MM-dd'T'HH:mm:ss.SSS z')'";
	private static final String PATTERN = " '('EEE yyyy-MM-dd HH:mm:ss.SSS z')'";
//	private static final String PATTERN = ", HH:mm:ss.SSS X')'";
//	static final Locale locale = Locale.CANADA;
//	private static final DateFormat DATE_FORMAT = DateFormat.getDateInstance(DateFormat.SHORT);
//	private static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat(PATTERN);
	private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat(PATTERN);
	
	static String guessIfTimestampLong(String keyName, long l) {
		keyName = keyName.toLowerCase();
		if (l > LOWER_MS && l < UPPER_MS) {  // milliseconds
			if (timeNameContains(TIME_MS_KEYWORDS, keyName) || (keyName.contains("ms") && PATTERN_MS.matcher(keyName).find())) {
				return DATE_FORMAT.format(new Date(l));
			}
		} else if (l > LOWER_MS*1_000_000 && l < UPPER_MS*1_000_000) {  // nanos
			if (timeNameContains(TIME_NS_KEYWORDS, keyName) || (keyName.contains("ns") && PATTERN_NS.matcher(keyName).find())) {
				return DATE_FORMAT.format(new Date(l / 1_000_000));
			}
		} else if (l > LOWER_MS*1_000 && l < UPPER_MS*1_000) {  // micros
			if (timeNameContains(TIME_US_KEYWORDS, keyName) || (keyName.contains("us") && PATTERN_US.matcher(keyName).find())) {
				return DATE_FORMAT.format(new Date(l / 1_000));
			}
		} else if (l > LOWER_MS/1_000 && l < UPPER_MS/1_000 && timeNameContains(TIME_SEC_KEYWORDS, keyName)) {
			return DATE_FORMAT.format(new Date(l * 1_000));
		}
		return null;
	}
	
	static String guessIfTimestampDouble(String keyName, double d) {
		keyName = keyName.toLowerCase();
		if (d > LOWER_MS/1_000 && d < UPPER_MS/1_000 && timeNameContains(TIME_SEC_KEYWORDS, keyName)) {  // seconds
			return DATE_FORMAT.format(new Date(Math.round(d * 1_000)));
		} else if (d > LOWER_MS && d < UPPER_MS) {  // milliseconds
			if (timeNameContains(TIME_MS_KEYWORDS, keyName) || (keyName.contains("ms") && PATTERN_MS.matcher(keyName).find())) {
				return DATE_FORMAT.format(new Date(Math.round(d)));
			}
		} else if (d > LOWER_MS*1_000_000 && d < UPPER_MS*1_000_000) {  // nanos
			if (timeNameContains(TIME_NS_KEYWORDS, keyName) || (keyName.contains("ns") && PATTERN_NS.matcher(keyName).find())) {
				return DATE_FORMAT.format(new Date((Math.round(d / 1_000_000))));
			}
		} else if (d > LOWER_MS*1_000 && d < UPPER_MS*1_000) {  // micros
			if (timeNameContains(TIME_US_KEYWORDS, keyName) || (keyName.contains("us") && PATTERN_US.matcher(keyName).find())) {
				return DATE_FORMAT.format(new Date(Math.round(d / 1_000)));
			}
		}
		return null;
	}

	static String guessIfTimestampString(String keyName, String val) {
		keyName = keyName.toLowerCase();
		if (val.length() > 5 && Character.isDigit(val.charAt(0)) && Character.isDigit(val.charAt(4))) {  // 5th char is a digit, so doesn't match timestamps: 2024-01-23
			try {
				double d = Double.parseDouble(val);
				try {
					long l = Long.parseLong(val);
					return guessIfTimestampLong(keyName, l);
				} catch (NumberFormatException e) {  // a double, but not a long
					return guessIfTimestampDouble(keyName, d);
				}
			} catch (NumberFormatException e) {  // not a number
				return null;
			}
		}
		return null;
	}

	
	public static String guessIfMapLookingThing(String headerLine) {
		if (headerLine.length() > 42 && headerLine.charAt(40) == '{' && headerLine.endsWith("}")) {
			String sub = formatMapLookingThing(headerLine.substring(40));
			return new StringBuilder().append(headerLine.substring(0,40)).append(sub).toString();
		} else return headerLine;
	}
	
    private static final String SPLIT_ON_COMMAS = ", *(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)";
    private static final String SPLIT_ON_COLONS = ":(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)";
    private static final String SPLIT_ON_EQUALS = "=(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)";
    
    private static String formatMapLookingThing(String s) {
    	s = s.trim();
    	assert s.startsWith("{");
    	assert s.endsWith("}");
    	s = s.substring(1, s.length()-1);
    	AaAnsi aa = AaAnsi.n().fg(Elem.BRACE).a('{');
    	String[] tokens = s.split(SPLIT_ON_COMMAS);  //,-1);
    	boolean validForColons = true;
    	boolean validForEquals = true;
    	for (String token : tokens) {
        	String[] splitOnColons = token.split(SPLIT_ON_COLONS, -1);  // -1 means if trailing : then have empty string
        	String[] splitOnEquals = token.split(SPLIT_ON_EQUALS, -1);
        	if (splitOnColons.length != 2) validForColons = false;
        	if (splitOnEquals.length != 2) validForEquals = false;
    	}
    	if (!(validForColons ^ validForEquals)) {  // either both true, or both false
    		return aa.reset().a(s).fg(Elem.BRACE).a('}').toString();  // don't know which to split on, so bail out
    	}
    	final String whichSplit = validForColons ? SPLIT_ON_COLONS : SPLIT_ON_EQUALS;
    	final char separator = validForColons ? ':' : '=';
    	Iterator<String> it = Arrays.stream(tokens).iterator();
    	while (it.hasNext()) {
    		String[] keyValPair = it.next().split(whichSplit, -1);
//    		aa.fg(Elem.KEY).a('\'').a(keyValPair[0]).a('\'').reset().a(separator);
    		aa.fg(Elem.KEY).a(keyValPair[0]).reset().a(separator);
    		aa.a(SaxHandler.guessAndFormatChars(keyValPair[1], keyValPair[0], 0));
    		if (it.hasNext()) aa.reset().a(',');
    	}
    	return aa.fg(Elem.BRACE).a('}').toString();
    }
    
    public static String capitalizeFirst(String s) {
    	if (s == null) return null;
    	if (s.isEmpty()) return null;
    	StringBuilder sb = new StringBuilder();
    	sb.append(s.substring(0, 1).toUpperCase()).append(s.substring(1));
    	return sb.toString();
    }

}
