/*
 * Copyright 2024 Solace Corporation. All rights reserved.
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
