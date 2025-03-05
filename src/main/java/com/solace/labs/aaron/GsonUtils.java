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

import java.io.IOException;
import java.io.StringReader;
import java.math.BigInteger;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;

import dev.solace.aaron.useful.WordUtils;

public class GsonUtils {
	
	private static final Logger logger = LogManager.getLogger(GsonUtils.class);
	private static String lastKeyName = "";
	
	static AaAnsi parseJsonDunnoWhich(String trimmedJson, int indentFactor) throws IOException {
		if (trimmedJson.charAt(0) == '{') return parseJsonObject(trimmedJson, indentFactor);
		else return parseJsonArray(trimmedJson, indentFactor);
	}

	static AaAnsi parseJsonObject(String json, int indentFactor) throws IOException {
		return parseJsonObject(json, indentFactor, false);
	}
	
	private static AaAnsi parseJsonObject(String json, int indentFactor, boolean isLenient) throws IOException {
		JsonReader reader = new JsonReader(new StringReader(json));
		reader.setLenient(false);
		AaAnsi ansi = AaAnsi.n();
		handleObject(reader, ansi, indentFactor, 0);
//		if (indentFactor > 0) ansi.a('\n');
		return ansi;
	}
	
	static AaAnsi parseJsonArray(String json, int indentFactor) throws IOException {
		return parseJsonArray(json, indentFactor, false);
	}
	
	private static AaAnsi parseJsonArray(String json, int indentFactor, boolean isLenient) throws IOException {
		JsonReader reader = new JsonReader(new StringReader(json));
		reader.setLenient(isLenient);
		AaAnsi ansi = AaAnsi.n();
		handleArray(reader, ansi, indentFactor, 0);
		return ansi;
	}
	
	/**
	 * Handle an Object. Consume the first token which is BEGIN_OBJECT. Within
	 * the Object there could be array or non array tokens. We write handler
	 * methods for both. Note the peek() method. It is used to find out the type
	 * of the next token without actually consuming it.
	 * 
	 * @param reader
	 * @throws IOException
	 */
	private static void handleObject(JsonReader reader, AaAnsi ansi, int indentFactor, int curIndent) throws IOException {
		reader.beginObject();
		boolean empty = !reader.hasNext();
		ansi.reset().fg(empty ? Elem.NULL : Elem.BRACE).a('{').reset();
		if (indentFactor > 0) {
			ansi.a(empty ? ' ' : '\n');
		}
		while (reader.hasNext()) {
			JsonToken token = reader.peek();
			if (token.equals(JsonToken.BEGIN_ARRAY)) {
				handleArray(reader, ansi, indentFactor, curIndent + indentFactor);
			} else if (token.equals(JsonToken.BEGIN_OBJECT)) {
//				ansi.a(indent(indent));
				handleObject(reader, ansi, indentFactor, curIndent + indentFactor);
				reader.endObject();
				if (reader.hasNext()) ansi.a(",");
				if (reader.peek().equals(JsonToken.END_OBJECT) || reader.peek().equals(JsonToken.END_ARRAY)) {
					if (indentFactor > 0) ansi.a(' ');
				} else {
					if (indentFactor > 0) ansi.a('\n');
				}
			} else if (token.equals(JsonToken.END_OBJECT)) {  // shouldn't come here b/c we end it above after handling the object
				System.out.println("********************************* JSON parser found END_OEJCT token, and it shouldn't!!!!!");
				reader.endObject();
				return;
			} else if (token.equals(JsonToken.NAME)) {
				handleRegularToken(reader, token, ansi, indentFactor, curIndent + indentFactor);
			} else {
				handleRegularToken(reader, token, ansi, indentFactor, 0);
				// orig
//				if (reader.hasNext()) ansi.a(",");
//				if (indentFactor > 0) ansi.a('\n');
				if (reader.hasNext()) {
					ansi.a(",");
					if (indentFactor > 0) ansi.a('\n');
				} else {  // no more
					if (indentFactor > 0) ansi.a(' ');
				}
			}
		}
		// orig
//		ansi.a(indent(indent)).a("}");
		ansi.fg(empty ? Elem.NULL : Elem.BRACE).a("}").reset();
	}

	/**
	 * Handle a json array. The first token would be JsonToken.BEGIN_ARRAY.
	 * Arrays may contain objects or primitives.
	 * 
	 * @param reader
	 * @throws IOException
	 */
	private static void handleArray(JsonReader reader, AaAnsi ansi, int indentFactor, int curIndent) throws IOException {
		reader.beginArray();
		boolean empty = !reader.hasNext();
		ansi.reset().fg(empty ? Elem.NULL : Elem.BRACE).a("[").reset();
		if (indentFactor > 0) ansi.a(empty ? ' ' : '\n');
		while (true) {
			JsonToken token = reader.peek();
			if (token.equals(JsonToken.END_ARRAY)) {
				reader.endArray();
				break;
			} else if (token.equals(JsonToken.BEGIN_OBJECT)) {
				ansi.a(WordUtils.indent(curIndent + indentFactor));
				handleObject(reader, ansi, indentFactor, curIndent + indentFactor);
			} else if (token.equals(JsonToken.BEGIN_ARRAY)) {
				ansi.a(WordUtils.indent(curIndent + indentFactor));
				handleArray(reader, ansi, indentFactor, curIndent + indentFactor);
			} else if (token.equals(JsonToken.END_OBJECT)) {
				reader.endObject();
				if (reader.hasNext()) ansi.a(",");
//				if (indentFactor > 0) ansi.a('\n');
				if (reader.peek().equals(JsonToken.END_OBJECT) || reader.peek().equals(JsonToken.END_ARRAY)) {
					if (indentFactor > 0) ansi.a(' ');
				} else {
					if (indentFactor > 0) ansi.a('\n');
				}
			} else if (token.equals(JsonToken.NAME)) {
				throw new AssertionError();
			} else {
				handleRegularToken(reader, token, ansi, indentFactor, curIndent + indentFactor);
				// orig
//				if (reader.hasNext()) ansi.a(",");
//				if (indentFactor > 0) ansi.a('\n');
				if (reader.hasNext()) {
					ansi.a(",");
					if (indentFactor > 0) ansi.a('\n');
				} else {  // no more
					if (indentFactor > 0) ansi.a(' ');
				}
			}
		}
		// orig
//		ansi.a(indent(indent)).a("]");
//		if (reader.hasNext()) ansi.a(",");
//		if (indentFactor > 0) ansi.a('\n');
		ansi.fg(empty ? Elem.NULL : Elem.BRACE).a("]").reset();
		if (reader.hasNext()) {
			ansi.a(",");
			if (indentFactor > 0) ansi.a('\n');
		} else {  // no more
			if (indentFactor > 0) ansi.a(' ');
		}
	}

	
//	private static final boolean DARK_MODE = false;
	
	
	/**
	 * Handle non array non object tokens
	 * 
	 * @param reader
	 * @param token
	 * @throws IOException
	 */
	private static void handleRegularToken(JsonReader reader, JsonToken token, AaAnsi ansi, int indentFactor, int curIndent) throws IOException {
		ansi.a(WordUtils.indent(curIndent));
		if (token.equals(JsonToken.NAME)) {
//			ansi.fg(Color.BLUE);
			lastKeyName = reader.nextName();
			ansi.fg(Elem.KEY).a("\"" + lastKeyName + "\"").reset().a(":");
			if (curIndent > 0) ansi.a(" ");
		} else if (token.equals(JsonToken.STRING)) {
			ansi.fg(Elem.STRING).a("\"" + reader.nextString() + "\"").reset();
		} else if (token.equals(JsonToken.NUMBER)) {
			String num = reader.nextString();  // we use string to preserve any formatting (e.g. "25.00" stays that way, not 25
//			ansi.fg(Elem.NUMBER).a(num).reset();
			try {
				BigInteger bi = new BigInteger(num);
				ansi.fg(Elem.NUMBER).a(num);
				if (indentFactor > 0) {
					String ts = UsefulUtils.guessIfTimestampLong(lastKeyName, bi.longValue());
					if (ts != null) {
						ansi.faintOn().a(ts);
					}
				}
				ansi.reset();
			} catch (NumberFormatException e) {
				ansi.fg(Elem.FLOAT).a(num).reset();
			}
			
		} else if (token.equals(JsonToken.BOOLEAN)) {
//			ansi.fgMagenta().a(reader.nextString()).reset();
			ansi.fg(Elem.BOOLEAN).a(reader.nextBoolean()).reset();
		} else if (token.equals(JsonToken.NULL)) {
//			ansi.fgRed().a(reader.nextString()).reset();
			ansi.fg(Elem.NULL).a("null").reset();
			reader.nextNull();
		} else {  // nothing else it could be?  https://javadoc.io/doc/com.google.code.gson/gson/2.6.2/com/google/gson/stream/JsonToken.html
			ansi.fg(Elem.UNKNOWN).a("<SKIPPING VALUE>").reset();
			logger.warn("Discovered an \"unknown\" value:\n" + ansi.toString());
			reader.skipValue();
		}
	}

}

