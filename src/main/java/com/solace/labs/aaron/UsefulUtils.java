package com.solace.labs.aaron;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import org.fusesource.jansi.Ansi;

public class UsefulUtils {

	
//	private static String getSimpleString(byte[] orig) {
//		return getSimpleString(orig, StandardCharsets.UTF_8);
//	}
	
	private static char getSimpleChar(byte orig) {
		if ((orig >= 0 && orig <= 32) || orig == 127 || orig < 0) {  // control char
			orig = 46;  // a period .
		}
		return (char)orig;
	}
	
	private static String getSimpleString(byte[] orig) {
		byte[] bytes = Arrays.copyOf(orig, orig.length);
		for (int i=0; i < bytes.length; i++) {
			if (bytes[i] <= 32 || bytes[i] == 127) {  // negative, control char or space
				bytes[i] = 46;  // a period .
			}
		}
		return new String(bytes, StandardCharsets.US_ASCII);
	}
	
	static String chop(String s) {
		if (s.endsWith("\n")) {
			return chop(s.substring(0, s.length()-1));  // just in case there's 2 trailing carriage returns?
		}
		return s;
	}
	
	private static final byte[] HEX_ARRAY = "0123456789abcdef".getBytes(StandardCharsets.US_ASCII);
	
	static String bytesToHex(byte[] bytes) {
	    byte[] hexChars = new byte[bytes.length * 2];
	    for (int j = 0; j < bytes.length; j++) {
	        int v = bytes[j] & 0xFF;
	        hexChars[j * 2] = HEX_ARRAY[v >>> 4];
	        hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
	    }
	    return new String(hexChars, StandardCharsets.US_ASCII);
	}

	static String[] bytesToHexStrings(byte[] bytes) {
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
	
	static final int WIDTH = 32;
	static final int COLS = 8;
	
	/** this should only be called if we know it's not a UTF-8 (or whatever) string */
	static String printBinarySdkPerf(byte[] bytes, int indent) {
		if (indent <= 0) {
			return new Ansi().reset().a("[").fgBlue().a(getSimpleString(bytes)).reset().a("]").toString();  // just a long string of chars
		}
		String[] hex = bytesToHexStrings(bytes);
		Ansi ansi = new Ansi().reset();
		for (int i=0; i < hex.length; i++) {
			if (i % WIDTH == 0) {
				ansi.a(indent(indent)).fgMagenta();
			}
			ansi.a(hex[i]).a(" ");
			if (i % COLS == COLS-1) {
				ansi.a("   ");
			}
			if (i % WIDTH == WIDTH-1) {
				ansi.fgBlue();
				for (int j=i-(WIDTH-1); j<i; j++) {
					ansi.a(getSimpleChar(bytes[j]));
				}
				ansi.reset().a('\n');
//				if (i < hex.length-1 || indent > 0) ansi.a('\n');
			}
		}
		// last trailing bit, if not evenly divisible by WIDTH
		if (hex.length % WIDTH != 0) {
			ansi.reset();
			int leftover = hex.length % WIDTH;
			for (int i=0; i < WIDTH - leftover; i++) {
				ansi.a("   ");
			}
			int extraGaps = (WIDTH - leftover) / COLS;
			for (int i=0; i <= extraGaps; i++) {
				ansi.a("   ");
			}
			ansi.fgBlue();
			for (int i= hex.length - leftover; i<hex.length; i++) {
				ansi.a(getSimpleChar(bytes[i]));
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
