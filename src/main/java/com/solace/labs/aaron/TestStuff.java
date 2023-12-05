package com.solace.labs.aaron;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class TestStuff {

	
	public static void main(String... args) throws CharacterCodingException {
		
		byte[] arr = new byte[] { 0, 1, 2, 3, 4, 5, 6, 7, 66, 9, 65, 10, 11, 12, 65, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37 };
		
		
		CharsetDecoder DECODER = StandardCharsets.UTF_8.newDecoder();
		DECODER.replaceWith(".");
		System.out.println(DECODER.unmappableCharacterAction());
		System.out.println(DECODER.malformedInputAction());

		ByteBuffer buffer = ByteBuffer.wrap(arr);

		String decoded = DECODER.decode(buffer).toString();
		
		System.out.println(decoded);
		for (int i=0; i<decoded.length(); i++) {
			System.out.print((int)decoded.charAt(i) + ", ");
		}
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
        System.out.println(Arrays.toString(UsefulUtils.bytesToHexStrings(bytes)));
        
        System.out.println(UsefulUtils.printBinarySdkPerf(bytes, 2));
        
//        System.out.println(Byte.toString(bytes[0]));
        
		
		
		
		
	}
}
