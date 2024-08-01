package com.solace.labs.aaron;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import com.solace.labs.aaron.UsefulUtils;


public class TestStuff {

	
	public static void main(String... args) throws CharacterCodingException {
		
//		byte[] arr = new byte[] { 0, 1, 2, 3, 4, 5, 6, 7, 66, 9, 65, 10, 11, -12, 65, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37 };
		byte[] arr = new byte[256];
		for (int i=-128; i<128; i++) {
			arr[i+128] = (byte)i;
		}
//		System.out.println(Arrays.toString(arr));
		String s = new String(arr,Charset.forName("ibm437"));
		System.out.println(Arrays.toString(s.getBytes(StandardCharsets.UTF_8)));
		System.out.println(Arrays.toString(StandardCharsets.UTF_16.newEncoder().encode(CharBuffer.wrap(UsefulUtils.HARDCODED  )).array()));

		for (int i=0; i<256; i++) {
//			System.out.print("'" + s.charAt(i) + "',");
			System.out.print("'" + s.charAt(i) + "' ");
			System.out.print(Arrays.toString(StandardCharsets.UTF_8.newEncoder().encode(CharBuffer.wrap(new char[] { s.charAt(i) })).array()));
			System.out.print(",");
			if (i % 16 == 15) System.out.println();
		}
		System.out.println();
//		CharsetDecoder DECODER = StandardCharsets.ISO_8859_1.newDecoder();
		System.out.println(Charset.availableCharsets().keySet());
//		CharsetDecoder DECODER = Charset.forName("Big5").newDecoder();
//		CharsetDecoder DECODER = Charset.forName("GB18030").newDecoder();
		CharsetDecoder DECODER = Charset.forName("windows-1252").newDecoder();
		DECODER.replaceWith("*");
//		System.out.println(DECODER.unmappableCharacterAction());
//		System.out.println(DECODER.malformedInputAction());
		DECODER.onUnmappableCharacter(CodingErrorAction.REPLACE);
//		DECODER.onMalformedInput(CodingErrorAction.REPLACE);

		ByteBuffer buffer = ByteBuffer.wrap(arr);

//		String decoded = DECODER.decode(buffer).toString();
//		System.out.println(decoded);
//		for (int i=0; i<decoded.length(); i++) {
//			System.out.print((int)decoded.charAt(i) + ", ");
//		}
		System.out.println();
		char[] arr2 = DECODER.decode(buffer).array();
		for (int i=0; i<arr2.length; i++) {
			char c = arr2[i];
			if ((c >= 0 && c < 32) || c >= 127 && c < 160) {
				arr2[i] = '·';
//			} else {
//				arr2[i] = c;
			}
//			System.out.print("'" + arr2[i] + "',");
			System.out.print(arr2[i]);
		}
		System.out.println();
		System.out.println(arr2.length);
		System.out.println();
		for (int i=0; i<arr.length; i++) {
			System.out.print(UsefulUtils.getSimpleChar2(arr[i]));
		}
		System.out.println();

		System.out.println();
		String emoji = "😬";
		System.out.println(emoji);
		System.out.println(emoji.length());
		System.out.println(emoji.charAt(0));
		System.out.println((int)emoji.charAt(0));
		System.out.println((byte)emoji.charAt(0));
//		System.out.println(UsefulUtils.getSimpleString(emoji.getBytes()));
		emoji = "👋🏼";
		System.out.println(emoji);
		System.out.println(emoji.length());
		System.out.println(emoji.charAt(0));
		System.out.println((int)emoji.charAt(0));
		System.out.println((byte)emoji.charAt(0));
		System.out.println((int)emoji.charAt(0) >> 8);
		System.out.println((int)emoji.charAt(0) & 0x00ff);
		for (int i=0; i<emoji.length(); i++) {
			System.out.println(Integer.toHexString((byte)emoji.charAt(i)));
		}
//		System.out.println(UsefulUtils.getSimpleString(emoji.getBytes()));
		
		System.out.println();

		
		System.out.println("new String()");
		System.out.println(new String(arr, StandardCharsets.UTF_8));
		
//		System.out.println("getSimpleString");
//		System.out.println(UsefulUtils.getSimpleString(arr));
		
		System.out.println("DECODER.decode");
		System.out.println(DECODER.decode(ByteBuffer.wrap(arr)));
		
		

		
		
		
        byte[] bytes = new byte[] { -3, -10, 56, 85, -94, 125, -34, 123, -1, 0, -77, 77, 66, 68, 111, 35, 39, 32, 83 };
        bytes = new byte[265];
        for (int i=-128; i<128; i++) {
        	bytes[i+128] = (byte)i;
        }
        bytes[256] = 'a';
        bytes[257] = 'b';
        bytes[258] = 'c';
        bytes[259] = 'd';
        bytes[260] = 'e';
        bytes[261] = 'f';
        bytes[262] = 'g';
        bytes[263] = 'h';
        bytes[264] = 'i';
        System.out.println(Byte.toString(bytes[0]));
        System.out.println(Integer.toHexString(bytes[0]));
        System.out.println(Integer.toHexString(bytes[0]));
        System.out.println(Arrays.toString(UsefulUtils.bytesToHexStringArray(bytes)));
        
//        System.out.println(UsefulUtils.printBinaryBytesSdkPerfStyle(bytes, 2));
        
//        System.out.println(Byte.toString(bytes[0]));
        
		
		
		
		
	}
}
