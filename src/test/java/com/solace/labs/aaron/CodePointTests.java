package com.solace.labs.aaron;

public class CodePointTests {


	
	public static void main(String... args) {
		
		
		for (int i=0; i< 267; i++) {
			System.out.println(i + ":"
				+ ", control=" + Character.isISOControl(i)
				+ ", defined=" + Character.isDefined(i)
				+ ", ignoreIdent=" + Character.isIdentifierIgnorable(i)
				+ ", whitespace=" + Character.isWhitespace(i)
//				+ ", DISPLAY=" + ()
				);
		}
		
		
		
		
		
	}
	
}
