package com.solace.labs.aaron.utils;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetDecoder;

public class DecoderUtils {

	public static String decodeToString(CharsetDecoder decoder, byte[] bytes) { 
        CharBuffer cb;
		try {
			// won't throw since the decoder is "REPLACE" and not "REPORT"
			cb = decoder.decode(ByteBuffer.wrap(bytes));
			return cb.toString();
		} catch (CharacterCodingException e) {
			// can't get here now
			throw new AssertionError("Could not decode bytes to charset",e);
		}
	}

	public static String decodeToString(CharsetDecoder decoder, ByteBuffer buffer) { 
		try {
			// won't throw since the decoder is "REPLACE" and not "REPORT"
			CharBuffer cb = decoder.decode(buffer);
			return cb.toString();
		} catch (CharacterCodingException e) {
			// can't get here now
			throw new AssertionError("Could not decode bytes to charset",e);
		}
	}


}
