/*
 * Copyright 2023-2024 Solace Corporation. All rights reserved.
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

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.protobuf.ByteString;
import com.solace.labs.aaron.AaAnsi.ColorMode;
import com.solacesystems.common.util.ByteArray;
import com.solacesystems.jcsmp.Destination;
import com.solacesystems.jcsmp.SDTMap;
import com.solacesystems.jcsmp.SDTStream;

/**
 * Used by AaAnsi, my wrapper around the JAnsi library to give me better controls over colours.
 * @author Aaron Lee
 */
public enum Elem {
	
	KEY,
	DATA_TYPE,
	PAYLOAD_TYPE,
	NULL,
	STRING,
	CHAR,
	NUMBER,
	FLOAT,
	BOOLEAN,
	BYTES,
	BYTES_CHARS,
	BRACE,
	DESTINATION,
	TOPIC_SEPARATOR,
	MSG_BREAK,
	WARN,
	ERROR,
	UNKNOWN,
	DEFAULT,
	;

	private static final Logger logger = LogManager.getLogger(Elem.class);
	static Map<AaAnsi.ColorMode, Map<Elem, Col>> colorMap = new HashMap<>();
	static {
		colorMap.put(AaAnsi.ColorMode.STANDARD, new HashMap<>());
		Map<Elem, Col> map = colorMap.get(AaAnsi.ColorMode.STANDARD);
		map.put(KEY, new Col(12));
		map.put(DATA_TYPE, new Col(4));
		map.put(PAYLOAD_TYPE, new Col(15));
		map.put(NULL, new Col(1, false, true));
		map.put(STRING, new Col(2));
		map.put(CHAR, new Col(10));
		map.put(NUMBER, new Col(3));
		map.put(FLOAT, new Col(3));
		map.put(BOOLEAN, new Col(13));
		map.put(BYTES, new Col(5));
		map.put(BYTES_CHARS, new Col(6));
		map.put(BRACE, new Col(-1));
		map.put(DESTINATION, new Col(14));
		map.put(TOPIC_SEPARATOR, new Col(6));
		map.put(MSG_BREAK, new Col(-1,true));
		map.put(WARN, new Col(208));
		map.put(ERROR, new Col(9));
		map.put(UNKNOWN, new Col(198, false, true));
		map.put(DEFAULT, new Col(-1));

		colorMap.put(AaAnsi.ColorMode.MINIMAL, new HashMap<>());
		map = colorMap.get(AaAnsi.ColorMode.MINIMAL);
//		map.put(KEY, new Col(12));
//		map.put(DATA_TYPE, new Col(-1));
//		map.put(PAYLOAD_TYPE, new Col(-1));
//		map.put(NULL, new Col(2, false, true));
//		map.put(STRING, new Col(2));
//		map.put(CHAR, new Col(2));
//		map.put(NUMBER, new Col(-1));
//		map.put(FLOAT, new Col(-1));
//		map.put(BOOLEAN, new Col(2));
//		map.put(BYTES, new Col(5));
//		map.put(BYTES_CHARS, new Col(2));
//		map.put(BRACE, new Col(-1));
//		map.put(DESTINATION, new Col(14));
//		map.put(TOPIC_SEPARATOR, new Col(6));
//		map.put(MSG_BREAK, new Col(-1));
//		map.put(WARN, new Col(11));
//		map.put(ERROR, new Col(9));
//		map.put(UNKNOWN, new Col(13, false, true));
//		map.put(DEFAULT, new Col(-1));
		map.put(KEY, new Col(-1));
		map.put(DATA_TYPE, new Col(-1));
		map.put(PAYLOAD_TYPE, new Col(15));
		map.put(NULL, new Col(2, false, true));
		map.put(STRING, new Col(-1));
		map.put(CHAR, new Col(-1));
		map.put(NUMBER, new Col(-1));
		map.put(FLOAT, new Col(-1));
		map.put(BOOLEAN, new Col(-1));
		map.put(BYTES, new Col(5));
		map.put(BYTES_CHARS, new Col(-1));
		map.put(BRACE, new Col(-1));
//		map.put(DESTINATION, new Col(14));
//		map.put(TOPIC_SEPARATOR, new Col(6));
		map.put(DESTINATION, new Col(42));
		map.put(TOPIC_SEPARATOR, new Col(36));
		map.put(MSG_BREAK, new Col(-1, true));
		map.put(WARN, new Col(11));
		map.put(ERROR, new Col(9));
		map.put(UNKNOWN, new Col(13, false, true));
		map.put(DEFAULT, new Col(-1));
		
		colorMap.put(AaAnsi.ColorMode.VIVID, new HashMap<>());
		map = colorMap.get(AaAnsi.ColorMode.VIVID);
		map.put(KEY, new Col(75));
		map.put(DATA_TYPE, new Col(33));
		map.put(PAYLOAD_TYPE, new Col(195));
		map.put(NULL, new Col(198, false, true));
		map.put(STRING, new Col(47));
		map.put(CHAR, new Col(119));
//		defaults.put(NUMBER, new Col(226));
		map.put(NUMBER, new Col(214));
//		defaults.put(FLOAT, new Col(214));
		map.put(FLOAT, new Col(226));
		map.put(BOOLEAN, new Col(207));
		map.put(BYTES, new Col(99));
		map.put(BYTES_CHARS, new Col(87));
		map.put(BRACE, new Col(230));
		map.put(DESTINATION, new Col(49));
//		map.put(TOPIC_SEPARATOR, new Col(43));
		map.put(TOPIC_SEPARATOR, new Col(36));
		map.put(MSG_BREAK, new Col(-1, true));
		map.put(WARN, new Col(208));
		map.put(ERROR, new Col(9));
		map.put(UNKNOWN, new Col(196, false, true));
//		defaults.put(DEFAULT, new Col(66));
		map.put(DEFAULT, new Col(245));

		colorMap.put(AaAnsi.ColorMode.LIGHT, new HashMap<>());
		map = colorMap.get(AaAnsi.ColorMode.LIGHT);
//		map.put(KEY, new Col(26));
		map.put(KEY, new Col(21));
//		map.put(DATA_TYPE, new Col(33));
		map.put(DATA_TYPE, new Col(26));
		map.put(PAYLOAD_TYPE, new Col(17));
		map.put(NULL, new Col(198, false, true));
		map.put(STRING, new Col(28));
//		map.put(CHAR, new Col(40));
		map.put(CHAR, new Col(34));
//		map.put(NUMBER, new Col(136));
		map.put(NUMBER, new Col(130));
//		map.put(FLOAT, new Col(142));
		map.put(FLOAT, new Col(136));
		map.put(BOOLEAN, new Col(57));
		map.put(BYTES, new Col(92));
		map.put(BYTES_CHARS, new Col(33));
		map.put(BRACE, new Col(52));
//		map.put(DESTINATION, new Col(30));
		map.put(DESTINATION, new Col(23));
//		map.put(TOPIC_SEPARATOR, new Col(42));
		map.put(TOPIC_SEPARATOR, new Col(30));
		map.put(MSG_BREAK, new Col(247));
		map.put(WARN, new Col(172));
		map.put(ERROR, new Col(160));
		map.put(UNKNOWN, new Col(196, false, true));
		map.put(DEFAULT, new Col(241));
	
		// 22, 28, 34, 40, 46, 83, 120, 157, 194, 231
		colorMap.put(AaAnsi.ColorMode.MATRIX, new HashMap<>());
		map = colorMap.get(AaAnsi.ColorMode.MATRIX);
		map.put(KEY, new Col(40));
		map.put(DATA_TYPE, new Col(34));
		map.put(PAYLOAD_TYPE, new Col(34));
		map.put(NULL, new Col(47, false, true));
		map.put(STRING, new Col(46));
		map.put(CHAR, new Col(82));
		map.put(NUMBER, new Col(83));
		map.put(FLOAT, new Col(83));
		map.put(BOOLEAN, new Col(120));
		map.put(BYTES, new Col(28));
		map.put(BYTES_CHARS, new Col(34));
		map.put(BRACE, new Col(157));
		map.put(DESTINATION, new Col(47));
		map.put(TOPIC_SEPARATOR, new Col(34));
		map.put(MSG_BREAK, new Col(35, true));
		map.put(WARN, new Col(154));
		map.put(ERROR, new Col(157));
		map.put(UNKNOWN, new Col(157, false, true));
		map.put(DEFAULT, new Col(28));

		colorMap.put(AaAnsi.ColorMode.OFF, new HashMap<>());
		map = colorMap.get(AaAnsi.ColorMode.OFF);
		map.put(WARN, new Col(11));
		map.put(ERROR, new Col(9));
		map.put(DEFAULT, new Col(-1));
	}
	
	static Elem guessByType(Object value) {
		if (value == null) return Elem.NULL;
		if (value instanceof Number) {
			if (value instanceof Double || value instanceof Float || value instanceof BigDecimal) return Elem.FLOAT;
			return Elem.NUMBER;
		}
		if (value instanceof String) {
			return ((String)value).isEmpty() ? Elem.NULL : Elem.STRING;
		}
		if (value instanceof Character) return Elem.CHAR;
		if (value instanceof ByteArray || value instanceof ByteString) return Elem.BYTES;
		if (value instanceof Boolean) return Elem.BOOLEAN;
		if (value instanceof Destination) return Elem.DESTINATION;
		if (value instanceof SDTMap || value instanceof SDTStream) return Elem.UNKNOWN;  // doesn't matter, this will get formatted anyway
		logger.warn("Found a value that couldn't be guessed! " + value.toString() + ", " + value.getClass().getName());
		return Elem.UNKNOWN;
	}
	
	@Override
	public String toString() {
		return this.name() + ": " + lookup.get(this);
	}
	
	private static Map<Elem,Col> lookup = new HashMap<>();
	static {  // defaults
		for (Elem elem : Elem.values()) {
			lookup.put(elem, colorMap.get(AaAnsi.ColorMode.STANDARD).get(elem));
		}
	}
	
	static void updateColors(ColorMode mode) {
		Map<Elem,Col> whichCols;
		if (colorMap.containsKey(mode)) {
			whichCols = colorMap.get(mode);
			for (Elem elem : whichCols.keySet()) {
				elem.updateColor(whichCols.get(elem));
			}
		}
	}
	
	private void updateColor(Col newColor) {
		lookup.put(this, newColor);
	}

//	static final int[] MATRIX_COLORS = new int[] { /* 22, */ 28, 34, 40, 46, /* 83, 120, */ 157 }; 
	static final int[] MATRIX_COLORS = new int[] { /* 22, */ 28, 34, 40, 46, 120, /* 157 */ }; 

	Col getCurrentColor() {
		if (AaAnsi.getColorMode() == ColorMode.MATRIX) {
			return new Col(MATRIX_COLORS[(int)(Math.random() * MATRIX_COLORS.length)]);
			// if you comment this out, check: public AaAnsi aa(AaAnsi aa)
		}
		// else...
		return lookup.get(this);
	}
	
}
