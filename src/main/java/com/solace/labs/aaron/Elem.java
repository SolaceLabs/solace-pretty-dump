/*
 * Copyright 2023 Solace Corporation. All rights reserved.
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

import com.google.protobuf.ByteString;
import com.solace.labs.aaron.AaAnsi.ColorMode;
import com.solacesystems.common.util.ByteArray;
import com.solacesystems.jcsmp.Destination;

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
	ERROR,
	UNKNOWN,
	DEFAULT,
	;

	static Map<AaAnsi.ColorMode, Map<Elem, Col>> colorMap = new HashMap<>();
	static {
		colorMap.put(AaAnsi.ColorMode.STANDARD, new HashMap<>());
		Map<Elem, Col> map = colorMap.get(AaAnsi.ColorMode.STANDARD);
		map.put(KEY, new Col(12));
		map.put(DATA_TYPE, new Col(4));
		map.put(PAYLOAD_TYPE, new Col(-1));
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
		map.put(TOPIC_SEPARATOR, new Col(-1));
		map.put(MSG_BREAK, new Col(-1,true));
		map.put(ERROR, new Col(196));
		map.put(UNKNOWN, new Col(198, false, true));
		map.put(DEFAULT, new Col(-1));

		colorMap.put(AaAnsi.ColorMode.MINIMAL, new HashMap<>());
		map = colorMap.get(AaAnsi.ColorMode.MINIMAL);
		map.put(KEY, new Col(4));
		map.put(DATA_TYPE, new Col(-1));
		map.put(PAYLOAD_TYPE, new Col(-1));
		map.put(NULL, new Col(2, false, true));
		map.put(STRING, new Col(2));
		map.put(CHAR, new Col(2));
		map.put(NUMBER, new Col(2));
		map.put(FLOAT, new Col(2));
		map.put(BOOLEAN, new Col(2));
		map.put(BYTES, new Col(2));
		map.put(BYTES_CHARS, new Col(2));
		map.put(BRACE, new Col(-1));
		map.put(DESTINATION, new Col(14));
		map.put(TOPIC_SEPARATOR, new Col(6));
		map.put(MSG_BREAK, new Col(-1,true));
		map.put(ERROR, new Col(1));
		map.put(UNKNOWN, new Col(1, false, true));
		map.put(DEFAULT, new Col(-1));
		
		colorMap.put(AaAnsi.ColorMode.VIVID, new HashMap<>());
		map = colorMap.get(AaAnsi.ColorMode.VIVID);
		map.put(KEY, new Col(75));
		map.put(DATA_TYPE, new Col(33));
		map.put(PAYLOAD_TYPE, new Col(195));
		map.put(NULL, new Col(198, false, true));
		map.put(STRING, new Col(47));
		map.put(CHAR, new Col(82));
//		defaults.put(NUMBER, new Col(226));
		map.put(NUMBER, new Col(214));
//		defaults.put(FLOAT, new Col(214));
		map.put(FLOAT, new Col(226));
		map.put(BOOLEAN, new Col(207));
		map.put(BYTES, new Col(99));
		map.put(BYTES_CHARS, new Col(87));
		map.put(BRACE, new Col(230));
		map.put(DESTINATION, new Col(49));
		map.put(TOPIC_SEPARATOR, new Col(43));
		map.put(MSG_BREAK, new Col(-1,true));
		map.put(ERROR, new Col(196));
		map.put(UNKNOWN, new Col(196, false, true));
//		defaults.put(DEFAULT, new Col(66));
		map.put(DEFAULT, new Col(102));

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
		map.put(ERROR, new Col(196));
		map.put(UNKNOWN, new Col(196, false, true));
		map.put(DEFAULT, new Col(241));
	
	}
	
	static Elem guessByType(Object value) {
		if (value == null) return Elem.NULL;
		if (value instanceof Number) {
			if (value instanceof Double || value instanceof Float || value instanceof BigDecimal) return Elem.FLOAT;
			return Elem.NUMBER;
		}
		if (value instanceof CharSequence) return Elem.STRING;
		if (value instanceof Character) return Elem.CHAR;
		if (value instanceof ByteArray || value instanceof ByteString) return Elem.BYTES;
		if (value instanceof Boolean) return Elem.BOOLEAN;
		if (value instanceof Destination) return Elem.DESTINATION;
		return Elem.UNKNOWN;
	}

	
	
	
	@Override
	public String toString() {
		return this.name() + ": " + l.get(this);
	}
	
	static Map<Elem,Col> l = new HashMap<>();
	
	static {
		for (Elem elem : Elem.values()) {
			l.put(elem, colorMap.get(AaAnsi.ColorMode.STANDARD).get(elem));
		}
	}
	
	static void updateColors(ColorMode mode) {
		Map<Elem,Col> whichCols;
		switch (mode) {
		case MINIMAL:
		case VIVID:
		case LIGHT:
			whichCols = colorMap.get(mode);
			break;
		default:
			whichCols = colorMap.get(ColorMode.STANDARD);
		}
		for (Elem elem : whichCols.keySet()) {
			elem.updateColor(whichCols.get(elem));
		}
	}
	
	private void updateColor(Col newColor) {
		l.put(this, newColor);
	}

	Col getCurrentColor() {
		return l.get(this);
	}
	
}
