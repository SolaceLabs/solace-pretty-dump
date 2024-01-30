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

	static String printMap(SDTMap map, final int indentFactor) throws SDTException {
		return privPrintMap(map, indentFactor, indentFactor == 4 ? 2 : indentFactor);
	}

	private static String privPrintMap(SDTMap map, final int indent, final int indentFactor) throws SDTException {
		AaAnsi ansi = new AaAnsi();
		privPrintMap(map, indent, indentFactor, ansi);
//		if (indentFactor <= 0) return new AaAnsi().fg(Elem.BRACE).a("{ ") + ansi.toString() + new AaAnsi().fg(Elem.BRACE).a(" }");
		if (indentFactor <= 0) return new AaAnsi().fg(Elem.BRACE).a('{') + ansi.toString() + new AaAnsi().fg(Elem.BRACE).a('}');
		else return ansi.reset().toString();
	}
	
	private static void privPrintMap(SDTMap map, final int indent, final int indentFactor, AaAnsi ansi) throws SDTException {
		if (map == null) {
			return;
		} else if (map.isEmpty()) {
			ansi.a(indent(indent)).fg(Elem.NULL).a("<EMPTY>").reset();
		}
//		String strIndent = indent(indent);
		Iterator<String> it = map.keySet().iterator();
		while (it.hasNext()) {
			String key = it.next();
			Object value = map.get(key);
			String strValue = String.valueOf(value);
			String type = "NULL";
			if (value != null) {
				Class<?> valuClass = value.getClass();
				type = valuClass.getSimpleName();
				if (type.endsWith("Impl")) {
					type = type.substring(0, type.length()-4);
				} else if (value instanceof String) {
					strValue = '"' + strValue + '"';  // double quotes for strings
//					strValue = '“' + strValue + '”';  // double quotes for strings
				} else if (value instanceof Character && indentFactor > 0) {
					strValue = "'" + strValue + "'";  // single quotes for chars
				} else if (value instanceof ByteArray) {
					strValue = UsefulUtils.bytesToSpacedHexString(((ByteArray)value).asBytes());
				}
				if (value instanceof SDTMap) {
					AaAnsi inner = new AaAnsi();
					if (indentFactor > 0) inner.a("\n");
//					else inner.a(" ");
					inner.aRaw(privPrintMap((SDTMap) value, indent + indentFactor, indentFactor));
					strValue = inner.toString();  // overwrite
				} else if (value instanceof SDTStream) {
					AaAnsi inner = new AaAnsi();
					if (indentFactor > 0) inner.a("\n");
//					else inner.a(" ");
					inner.aRaw(privPrintStream((SDTStream) value, indent + indentFactor, indentFactor));
					strValue = inner.toString();  // overwrite
				}
			}
			ansi.reset().a(indent(indent));
			if (indentFactor > 0) ansi.a("Key ");
//			else ansi.a("'");
			if (indentFactor > 0) {
				ansi.fg(Elem.KEY).a("'").a(key).a("' ").reset();
//				ansi.fg(Elem.KEY).a("‘").a(key).a("’ ").reset();
//				ansi.fg(Elem.KEY).a("‹").a(key).a("› ").reset();
			} else {
				ansi.fg(Elem.KEY).a(key).reset();
			}
//			ansi.fg(Elem.KEY).a("'").a(key).a("'").reset();
//			if (indentFactor > 0) ansi.a(" ");
//			else ansi.a("'(");
			ansi.fg(Elem.DATA_TYPE);
//			if (isPrimitive) ansi.fg(Elem.NUMBER);
//			else ansi.fg(Elem.NULL);
			ansi.a('(').a(type).a(')').reset();
			if (indentFactor > 0) ansi.a(": ");
//			else ansi.a(":");
			if (value instanceof Topic) ansi.colorizeTopic(strValue).reset();
			else ansi.fg(Elem.guessByType(value)).aRaw(strValue).reset();
			if (it.hasNext()) {
				if (indentFactor > 0) ansi.a("\n");
//				else ansi.a(";");
				else ansi.a(",");
			}
		}
	}

	
	
	
	static String printStream(SDTStream stream, final int indentFactor) throws SDTException {
		return privPrintStream(stream, indentFactor, indentFactor == 4 ? 2 : indentFactor);
	}
	
	private static String privPrintStream(SDTStream stream, final int indent, final int indentFactor) throws SDTException {
		AaAnsi ansi = new AaAnsi();
		privPrintStream(stream, indent, indentFactor, ansi);
//		if (indentFactor <= 0) return new AaAnsi().fg(Elem.BRACE).a("[ ") + ansi.toString() + new AaAnsi().fg(Elem.BRACE).a(" ]");
		if (indentFactor <= 0) return new AaAnsi().fg(Elem.BRACE).a('[') + ansi.toString() + new AaAnsi().fg(Elem.BRACE).a(']');
		else return ansi.reset().toString();
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
			String type = "NULL";
			if (value != null) {
				Class<?> valuClass = value.getClass();
				type = valuClass.getSimpleName();
				if (type.endsWith("Impl")) {
					type = type.substring(0, type.length()-4);
				} else if (value instanceof CharSequence) {
					strValue = '"' + strValue + '"';
				} else if (value instanceof Character && indentFactor > 0) {
					strValue = "'" + strValue + "'";
				} else if (value instanceof ByteArray) {
					strValue = UsefulUtils.bytesToSpacedHexString(((ByteArray)value).asBytes());
				}
				if (value instanceof SDTMap) {
					AaAnsi inner = new AaAnsi();
					if (indentFactor > 0) inner.a("\n");
//					else inner.a(" ");
					inner.aRaw(privPrintMap((SDTMap) value, indent + indentFactor, indentFactor));
//					else
//						a.a("{ ").reset().a(privPrintMap((SDTMap) value, indent + indentFactor, indentFactor)).a(" }").reset();
					strValue = inner.toString();  // overwrite
				} else if (value instanceof SDTStream) {
					AaAnsi inner = new AaAnsi();
					if (indentFactor > 0) inner.a("\n");
//					else inner.a(" ");
					inner.aRaw(privPrintStream((SDTStream) value, indent + indentFactor, indentFactor));
//					else
//						inner.fg(Elem.BYTES).a("[ ").reset().a(privPrintStream((SDTStream)value, indent + indentFactor, indentFactor)).reset().a(" ]").reset();
					strValue = inner.toString();  // overwrite
				}
			}
			ansi.reset().a(indent(indent));
			if (indentFactor > 0) {
				ansi.fg(Elem.DATA_TYPE).a('(').a(type).a("): ").reset();
			} else {
				ansi.fg(Elem.DATA_TYPE).a('(').a(type).a(")").reset();
			}
			if (value instanceof Topic) ansi.colorizeTopic(strValue).reset();
			else ansi.fg(Elem.guessByType(value)).aRaw(strValue).reset();
			if (stream.hasRemaining() ) {
				if (indentFactor > 0) ansi.a("\n");
				else ansi.a(",");
			}
		}
		stream.rewind();
	}
}
