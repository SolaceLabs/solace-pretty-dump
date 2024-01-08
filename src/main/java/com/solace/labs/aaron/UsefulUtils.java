package com.solace.labs.aaron;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.StandardCharsets;

import com.solacesystems.common.util.ByteArray;

public class UsefulUtils {

	
//	private static String getSimpleString(byte[] orig) {
//		return getSimpleString(orig, StandardCharsets.UTF_8);
//	}
	
	private static Charset DEFAULT_CHARSET = StandardCharsets.ISO_8859_1;
	private static CharsetDecoder DECODER = DEFAULT_CHARSET.newDecoder();
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

			'Ç','ü','é','â','ä','à','å','ç','ê','ë','è','ï','î','ì','Ä','Å',  // DOS/IBM437 yeah!
			'É','æ','Æ','ô','ö','ò','û','ù','ÿ','Ö','Ü','¢','£','¥','₧','ƒ',
			'á','í','ó','ú','ñ','Ñ','ª','º','¿','⌐','¬','½','¼','¡','«','»',
			'░','▒','▓','│','┤','╡','╢','╖','╕','╣','║','╗','╝','╜','╛','┐',
			'└','┴','┬','├','─','┼','╞','╟','╚','╔','╩','╦','╠','═','╬','╧',
			'╨','╤','╥','╙','╘','╒','╓','╫','╪','┘','┌','█','▄','▌','▐','▀',
			'α','ß','Γ','π','Σ','σ','µ','τ','Φ','Θ','Ω','δ','∞','φ','ε','∩',
			'≡','±','≥','≤','⌠','⌡','÷','≈','°','∙','·','√','ⁿ','²','■',' ',
//			'·','·','·','·','·','·','·','·','·','·','·','·','·','·','·','·',
//			'·','·','·','·','·','·','·','·','·','·','·','·','·','·','·','·',
			'╳','☺','☻','♥','♦','♣','♠','•','◘','○','◙','♂','♀','♪','♫','☼',
			'►','◄','↕','‼','¶','§','▬','↨','↑','↓','→','←','∟','↔','▲','▼',
			' ','!','"','#','$','%','&','\'','(',')','*','+',',','-','.','/',
			'0','1','2','3','4','5','6','7','8','9',':',';','<','=','>','?',
			'@','A','B','C','D','E','F','G','H','I','J','K','L','M','N','O',
			'P','Q','R','S','T','U','V','W','X','Y','Z','[','\\',']','^','_',
			'`','a','b','c','d','e','f','g','h','i','j','k','l','m','n','o',
			'p','q','r','s','t','u','v','w','x','y','z','{','¦','}','~','⌂'
		};
	
	private static char[] getCharArray(byte[] orig) {
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
	}
	
	public static char getSimpleChar2(byte orig) {
		return HARDCODED[orig + 128];
	}
	
	private static char getSimpleChar(byte orig) {
		
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
	}
	
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
	static String chop(String s) {
		if (s.endsWith("\n")) {
			return chop(s.substring(0, s.length()-1));  // just in case there's 2 trailing carriage returns?
		}
		return s;
	}
	
	private static final byte[] HEX_ARRAY = "0123456789abcdef".getBytes(StandardCharsets.US_ASCII);
	
	private static String bytesToHex(byte[] bytes) {
	    byte[] hexChars = new byte[bytes.length * 2];
	    for (int j = 0; j < bytes.length; j++) {
	        int v = bytes[j] & 0xFF;
	        hexChars[j * 2] = HEX_ARRAY[v >>> 4];
	        hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
	    }
	    return new String(hexChars, StandardCharsets.US_ASCII);
	}

	public static String[] bytesToHexStrings(byte[] bytes) {
	    byte[] hexChars = new byte[2];
	    String[] blah = new String[bytes.length];
	    for (int j = 0; j < bytes.length; j++) {
	        int v = bytes[j] & 0xFF;
	        hexChars[0] = HEX_ARRAY[v >>> 4];
	        hexChars[1] = HEX_ARRAY[v & 0x0F];
	        blah[j] = new String(hexChars, StandardCharsets.US_ASCII);
	    }
	    return blah;
	}


	/*
  1d 00 a3 5b 7b 22 6e 61    6d 65 22 3a 22 74 65 73    ...[{"name":"tes
  74 20 70 72 6f 64 75 63    74 22 2c 22 71 75 61 6e    t.product","quan
  74 69 74 79 22 3a 35 2c    22 70 72 69 63 65 22 3a    tity":5,"price":
  31 30 2e 39 39 2c 22 74    6f 74 61 6c 22 3a 35 30    10.99,"total":50
  7d 2c 7b 22 6e 61 6d 65    22 3a 22 43 72 65 61 74    },{"name":"Creat
	 */

	private static String printBinaryBytesSdkPerfStyle2(byte[] bytes) {
		return printBinaryBytesSdkPerfStyle2(bytes, 0);
	}

	private static String printBinaryBytesSdkPerfStyle2(byte[] bytes, int indent) {
		ByteArray ba = new ByteArray(bytes);
		return indent(indent) + ba.toString();
	}
	
	static String printBinaryBytesSdkPerfStyle(byte[] bytes, int indent) {
		return printBytes(bytes, indent, 16);  // default
	}

	static String printBinaryBytesSdkPerfStyle(byte[] bytes) {
		return printBytes(bytes, 4, 16);  // default
	}
	
	static String printBinaryBytesSdkPerfStyle(byte[] bytes, int indent, int terminalWidth) {
		if (terminalWidth > 142 + indent) return printBytes(bytes, indent, 32);
		else return printBytes(bytes, indent, 16);
	}
	
//	static final int WIDTH = 32;
	static final int COLS = 8;
	
	/** this should only be called if we know it's not a UTF-8 (or whatever) string */
	private static String printBytes(byte[] bytes, int indent, int width) {
		if (indent <= 0) {
//			return new AaAnsi().reset().a("[").fg(Elem.BYTES_CHARS).a(getSimpleString(bytes)).reset().a("]").toString();  // just a long string of chars
			return new AaAnsi().reset().fg(Elem.BYTES).a(printBinaryBytesSdkPerfStyle2(bytes)).reset().toString();  // byte values
		}
		String[] hex = bytesToHexStrings(bytes);
		AaAnsi ansi = new AaAnsi();
		for (int i=0; i < hex.length; i++) {
			if (i % width == 0) {
				ansi.a(indent(indent)).fg(Elem.BYTES);
			}
			ansi.a(hex[i]).a(" ");
			if (i % COLS == COLS-1) {
				ansi.a("   ");
			}
			if (i % width == width-1) {
				ansi.fg(Elem.BYTES_CHARS);
				for (int j=i-(width-1); j<=i; j++) {
					ansi.a(getSimpleChar2(bytes[j]));
					if (j % 8 == 7) ansi.a("  ");
//					if (j % 16 == 15) ansi.a(" ");
				}
				ansi.reset().a('\n');
//				if (i < hex.length-1 || indent > 0) ansi.a('\n');
			}
		}
		// last trailing bit, if not evenly divisible by WIDTH
		// works for everthing except 24
		if (hex.length % width != 0) {
			ansi.reset();
			int leftover = hex.length % width;
			for (int i=0; i < width - leftover; i++) {
				ansi.a("   ");
			}
			int extraGaps = (width - leftover - 1) / COLS;
			for (int i=0; i <= extraGaps; i++) {
				ansi.a("   ");
			}
			ansi.fg(Elem.BYTES_CHARS);
			for (int i= hex.length - leftover; i<hex.length; i++) {
				ansi.a(getSimpleChar2(bytes[i]));
				if (i % 8 == 7) ansi.a("  ");
//				if (i % 16 == 15) ansi.a(" ");
			}
			ansi.reset();
//			if (indent > 0) ansi.a('\n');
		}
		return ansi.toString();
	}
	
	
	static String indent(int amount) {
		StringBuilder sb = new StringBuilder();
		for (int i=0; i<amount; i++) {
			sb.append(' ');
		}
		return sb.toString();
	}

	static String pad(int amount, char c) {
		StringBuilder sb = new StringBuilder();
		for (int i=0; i<amount; i++) {
			sb.append(c);
		}
		return sb.toString();
	}
	


}
