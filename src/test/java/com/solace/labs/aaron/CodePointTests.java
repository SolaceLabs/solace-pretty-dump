package com.solace.labs.aaron;

import java.nio.charset.StandardCharsets;

public class CodePointTests {


	
	public static void main(String... args) {
		
		
		for (int i=0; i< 128; i++) {
//			System.out.println(i + ":"
//				+ ", control=" + Character.isISOControl(i)
//				+ ", defined=" + Character.isDefined(i)
//				+ ", ignoreIdent=" + Character.isIdentifierIgnorable(i)
//				+ ", whitespace=" + Character.isWhitespace(i)
//				);
			
			
			String s = new String(new byte[] { (byte)(i) }, StandardCharsets.UTF_8);
			char c = s.charAt(0);
//			System.out.println(c);
			if (c == '') {
				System.out.print("* ");
			}
			System.out.println((int)c + ": '" + c + "':"
					+ ", control=" + Character.isISOControl(i)
					+ ", defined=" + Character.isDefined(i)
					+ ", ignoreIdent=" + Character.isIdentifierIgnorable(i)
					+ ", whitespace=" + Character.isWhitespace(i)
					);
		}
		for (int i=0; i< 128; i++) {
//			System.out.println(i + ":"
//				+ ", control=" + Character.isISOControl(i)
//				+ ", defined=" + Character.isDefined(i)
//				+ ", ignoreIdent=" + Character.isIdentifierIgnorable(i)
//				+ ", whitespace=" + Character.isWhitespace(i)
//				);
			
			
			String s = new String(new byte[] { (byte)(i-128) }, StandardCharsets.ISO_8859_1);
			char c = s.charAt(0);
			if (c == '�') {
				System.out.print("* ");
			}
//			System.out.println(c);
//			System.out.println((i+128) + ": " + c + ":"
			System.out.println((int)c + ": " + c + ":"
					+ ", control=" + Character.isISOControl((int)c)
					+ ", defined=" + Character.isDefined((int)c)
					+ ", ignoreIdent=" + Character.isIdentifierIgnorable((int)c)
					+ ", whitespace=" + Character.isWhitespace((int)c)
					);
		}
		
		
		
		
		
	}
	
}
