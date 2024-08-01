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

import static com.solace.labs.aaron.UsefulUtils.indent;

import java.util.Iterator;

import com.solacesystems.common.util.ByteArray;
import com.solacesystems.jcsmp.SDTException;
import com.solacesystems.jcsmp.SDTMap;
import com.solacesystems.jcsmp.SDTStream;
import com.solacesystems.jcsmp.Topic;

public class SdtUtils {
	
	static int countElements(SDTMap map) {
		int count = 0;
		try {
			Iterator<String> it = map.keySet().iterator();
			while (it.hasNext()) {
				Object value = map.get(it.next());
				if (value instanceof SDTMap) {
					count += countElements((SDTMap)value);
				} else if (value instanceof SDTStream) {
					count += countElements((SDTStream)value);
				} else count++;  // just a regular element
			}
		} catch (SDTException e) {  // shouldn't happen, we know it's well-defined since we received it
			return -1;
		}
		return count;
	}
	
	static int countElements(SDTStream stream) {
		int count = 0;
		try {
			while (stream.hasRemaining()) {
				Object value = stream.read();
				if (value instanceof SDTMap) {
					count += countElements((SDTMap)value);
				} else if (value instanceof SDTStream) {
					count += countElements((SDTStream)value);
				} else count++;  // just a regular element
			}
		} catch (SDTException e) {  // shouldn't happen, we know it's well-defined since we received it
			return -1;
		}
		return count;
	}

	static AaAnsi printMap(SDTMap map, final int indentFactor) {
		try {
			return privPrintMap(map, indentFactor, indentFactor).reset();
		} catch (SDTException e) {
			throw new IllegalArgumentException("Could not parse SDT", e);
		}
	}

	private static AaAnsi privPrintMap(SDTMap map, final int curIndent, final int indentFactor) throws SDTException {
		AaAnsi aa = new AaAnsi();
		privPrintMap(map, curIndent, indentFactor, aa);
//		if (indentFactor <= 0) return new AaAnsi().fg(Elem.BRACE).a("{ ") + ansi.toString() + new AaAnsi().fg(Elem.BRACE).a(" }");
//		if (indentFactor <= 0) return new AaAnsi().fg(Elem.BRACE).a('{') + aa.toString() + new AaAnsi().fg(Elem.BRACE).a('}').reset();
		if (indentFactor <= 0) return new AaAnsi().fg(Elem.BRACE).a('{').a(aa).fg(Elem.BRACE).a('}');
		else return aa;
//		else return ansi.toString() + AaAnsi.n().fg(Elem.BRACE).a(" }").reset();
	}
	
	private static void privPrintMap(SDTMap map, final int curIndent, final int indentFactor, AaAnsi ansi) throws SDTException {
		if (map == null) {
			return;
		} else if (map.isEmpty()) {
			ansi.a(indent(curIndent)).fg(Elem.NULL).a("<EMPTY>").reset();
			return;
		}
//		String strIndent = indent(indent);
		Iterator<String> it = map.keySet().iterator();
		while (it.hasNext()) {
			String key = it.next();
			Object value = map.get(key);
			String strValue = String.valueOf(value);
			AaAnsi ansiValue = null; // AaAnsi.n().fg(Elem.guessByType(value)).a(strValue);
			String type = "NULL";  // default value of the type for now, will get overwritten with actual object type
			// that's how SdkPerf JCSMP does it:
			if (value != null) {
				Class<?> valuClass = value.getClass();
				type = valuClass.getSimpleName();
				if (type.endsWith("Impl")) {
					type = type.substring(0, type.length()-4);
				} else if (value instanceof CharSequence) {
					strValue = '"' + strValue + '"';  // double quotes for strings
//					strValue = '“' + strValue + '”';  // double quotes for strings
				} else if (value instanceof Character && indentFactor > 0) {
					strValue = "'" + strValue + "'";  // single quotes for chars
				} else if (value instanceof ByteArray) {
					if (((ByteArray)value).getLength() > 16 && indentFactor > 0) {
						
					} else {
						strValue = UsefulUtils.bytesToSpacedHexString(((ByteArray)value).asBytes());
					}
				}
				if (value instanceof SDTMap) {
					AaAnsi inner = new AaAnsi();
					if (indentFactor > 0) inner.a("\n");
//					else inner.a(" ");
					inner.a(privPrintMap((SDTMap) value, curIndent + indentFactor, indentFactor));
					ansiValue = inner;
//					strValue = inner.toString();  // overwrite
				} else if (value instanceof SDTStream) {
					AaAnsi inner = new AaAnsi();
					if (indentFactor > 0) inner.a("\n");
//					else inner.a(" ");
					inner.a(privPrintStream((SDTStream) value, curIndent + indentFactor, indentFactor));
					ansiValue = inner;
//					strValue = inner.toString();  // overwrite
				} else {  // everything else
					ansiValue = AaAnsi.n().fg(Elem.guessByType(value)).a(strValue); // update
				}
			} else {  // value is null, but there is no way to query the map for the data type
				ansiValue = AaAnsi.n().fg(Elem.NULL).a(strValue); // update
			}
			ansi.reset().a(indent(curIndent));
//			if (indentFactor > 0) ansi.a("Key ");
//			ansi.fg(Elem.KEY).a("'").a(key).a("'").reset();
//			if (indentFactor > 0) ansi.a(' ');
			if (indentFactor > 0) ansi.a("Key ").fg(Elem.KEY).a("'").a(key).a("' ");//.reset();
			else ansi.fg(Elem.KEY).a(key);  // no more single quotes on key name for compressed view
			
			//			ansi.reset().a(indent(indent));
//			if (indentFactor > 0) {
//				ansi.a("Key ").fg(Elem.KEY).a("'").a(key).a("' ").reset();
//			} else {
//				ansi.fg(Elem.KEY).a(key).reset();
//			}
			ansi.fg(Elem.DATA_TYPE);
			ansi.a('(').a(type).a(')').reset();
			if (indentFactor > 0) ansi.a(": ");
//			else ansi.a(":");
			if (value instanceof Topic) ansi.colorizeTopic(strValue, -1).reset();
			else {
//				Elem guess = Elem.guessByType(value);
				ansi./* fg(guess). */a(ansiValue).reset();
				if (value instanceof Long) {
					String ts = UsefulUtils.guessIfTimestampLong(key, (long)value);
					if (ts != null) {
//						ansi.fg(Elem.CHAR).a(ts);
						ansi.fg(Elem.NUMBER).faintOn().a(ts).reset();
					}
				} else if (value instanceof String) {
					String ts = UsefulUtils.guessIfTimestampString(key, (String)value);
					if (ts != null) {
						ansi.fg(Elem.STRING).faintOn().a(ts).reset();
					}
				}
			}
			if (it.hasNext()) {
				if (indentFactor > 0) ansi.a("\n");
				else ansi.a(",");
			}
		}
	}

	
	
	
	static AaAnsi printStream(SDTStream stream, final int indentFactor) {
		try {
			return privPrintStream(stream, indentFactor, indentFactor).reset();
		} catch (SDTException e) {
			throw new IllegalArgumentException("Could not parse SDT", e);
		}
	}
	
	private static AaAnsi privPrintStream(SDTStream stream, final int curIndent, final int indentFactor) throws SDTException {
		AaAnsi aa = new AaAnsi();
		privPrintStream(stream, curIndent, indentFactor, aa);
//		if (indentFactor <= 0) return new AaAnsi().fg(Elem.BRACE).a('[') + ansi.toString() + new AaAnsi().fg(Elem.BRACE).a(']').reset();
		if (indentFactor <= 0) return new AaAnsi().fg(Elem.BRACE).a('[').a(aa).fg(Elem.BRACE).a(']');
		else return aa;
	}

	
	
	private static void privPrintStream(SDTStream stream, final int indent, final int indentFactor, AaAnsi ansi) throws SDTException {
		if (stream == null) {
			return;
		} else if (!stream.hasRemaining()) {
			ansi.a(indent(indent)).fg(Elem.NULL).a("<EMPTY>").reset();
		}
//		String strIndent = ;
		while (stream.hasRemaining()) {
			Object value = stream.read();
			String strValue = String.valueOf(value);
			AaAnsi strValue2 = null;// AaAnsi.n().fg(Elem.guessByType(value)).a(String.valueOf(strValue));
			String type = "NULL";
			if (value != null) {
				Class<?> valuClass = value.getClass();
				type = valuClass.getSimpleName();
				if (type.endsWith("Impl")) {  // maps and streams
					type = type.substring(0, type.length()-4);
				} else if (value instanceof CharSequence) {
					strValue = '"' + strValue + '"';
				} else if (value instanceof Character && indentFactor > 0) {
					strValue = "'" + strValue + "'";
				} else if (value instanceof ByteArray) {
					strValue = UsefulUtils.bytesToSpacedHexString(((ByteArray)value).asBytes());
				}
//				strValue2 = AaAnsi.n().fg(Elem.guessByType(value)).a(String.valueOf(strValue));  // update
				if (value instanceof SDTMap) {
					AaAnsi inner = new AaAnsi();
					if (indentFactor > 0) inner.a("\n");
//					else inner.a(" ");
					inner.a(privPrintMap((SDTMap) value, indent + indentFactor, indentFactor));
//					else
//						a.a("{ ").reset().a(privPrintMap((SDTMap) value, indent + indentFactor, indentFactor)).a(" }").reset();
//					strValue = inner.toString();  // overwrite
					strValue2 = inner;
				} else if (value instanceof SDTStream) {
					AaAnsi inner = new AaAnsi();
					if (indentFactor > 0) inner.a("\n");
//					else inner.a(" ");
					inner.a(privPrintStream((SDTStream) value, indent + indentFactor, indentFactor));
//					else
//						inner.fg(Elem.BYTES).a("[ ").reset().a(privPrintStream((SDTStream)value, indent + indentFactor, indentFactor)).reset().a(" ]").reset();
//					strValue = inner.toString();  // overwrite
					strValue2 = inner;
				} else {
					strValue2 = AaAnsi.n().fg(Elem.guessByType(value)).a(strValue);  // update
				}
			} else {  // value must be null
				strValue2 = AaAnsi.n().fg(Elem.NULL).a(strValue);  // update
			}
			ansi.reset().a(indent(indent));
			if (indentFactor > 0) {
				ansi.fg(Elem.DATA_TYPE).a('(').a(type).a(")").reset().a(": ");
			} else {
				ansi.fg(Elem.DATA_TYPE).a('(').a(type).a(")").reset();
			}
			if (value instanceof Topic) ansi.colorizeTopic(strValue, -1).reset();
			else ansi.a(strValue2).reset();
			if (stream.hasRemaining() ) {
				if (indentFactor > 0) ansi.a("\n");
				else ansi.a(",");
			}
		}
		stream.rewind();
	}
}
