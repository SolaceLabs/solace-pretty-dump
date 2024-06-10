package com.solace.labs.aaron;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CoderResult;

public class AaronCharsetDecoder extends CharsetDecoder {

	public AaronCharsetDecoder(Charset cs, float averageCharsPerByte, float maxCharsPerByte) {
		super(cs, averageCharsPerByte, maxCharsPerByte);
		// TODO Auto-generated constructor stub
	}

	@Override
	protected CoderResult decodeLoop(ByteBuffer in, CharBuffer out) {
		// TODO Auto-generated method stub
		return null;
	}

}
