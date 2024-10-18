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

import java.math.BigInteger;

import org.xml.sax.Attributes;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.ext.LexicalHandler;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Taken/inspired by my StatsPump code from a long time ago.
 * Pretty basic pretty-print formatter for XML docs.
 * @author Aaron Lee
 */
public class SaxHandler extends DefaultHandler implements LexicalHandler, ErrorHandler {

	private AaAnsi ansi = AaAnsi.n();
	private final int indentFactor;
	private final boolean doTrimOnCharacters;
	private int level = 0;
    private StringBuilder characterDataSb = new StringBuilder();   // need StringBuilder for the 'characters' method since SAX can call it multiple times
	private Tag previous = null;
	private AaAnsi startTagForLater = null;
	
	enum Tag {
		START,
		END,
		;
	}
	
	public SaxHandler(int indentFactor) {
		this(indentFactor, true);
	}

	public SaxHandler(int indentFactor, boolean doTrimOnCharacters) {
		this.indentFactor = indentFactor;
		this.doTrimOnCharacters = doTrimOnCharacters;
	}

	@Override
	public void startDocument() throws SAXException { }

	@Override
	public void endDocument() throws SAXException { }

	public AaAnsi getResult() {
		return ansi;
	}

	@Override
	public final void characters(char[] ch, int start, int length) {
        characterDataSb.append(String.valueOf(ch,start,length));
	}

	@Override
	public final void startElement(String namespaceURI, String localName, String qName, Attributes atts) throws SAXException {
        characterDataSb.setLength(0);  // start capturing a new bunch of characters after this startTagForLater... re-use to prevent object creation
		if (previous == Tag.START && startTagForLater != null) {  // if the tag before me was also a start, then need to dump out my saved tag
//			assert startTagForLater != null;
			ansi.aa(startTagForLater).reset().a('>');
			if (indentFactor > 0) ansi.a('\n');
		} else if (previous == Tag.END) {  // aa debug june 25
			if (indentFactor > 0) ansi.a('\n');
		}
		startTagForLater = AaAnsi.n();  // reset for new start tag
		if (indentFactor > 0 && level > 0) {
			startTagForLater.a(UsefulUtils.indent(indentFactor * level));
		}
		startTagForLater.a('<').fg(Elem.KEY).a(qName);
		for (int i=0; i<atts.getLength(); i++) {
			startTagForLater.a(' ').fg(Elem.DATA_TYPE).a(atts.getQName(i)).fg(Elem.BRACE).a("=").fg(Elem.BYTES_CHARS).a('"').a(atts.getValue(i)).a('"');
    	}
		previous = Tag.START;
		level++;
	}

	/**
	 * Cheeky method to try to guess what a datatype is, and add some colour-coding.
	 */
	static AaAnsi guessAndFormatChars(String val, String key, int indentFactor) {
		try {
			Double.parseDouble(val);  // is it a number?
			try {
				// yup!
				BigInteger bi = new BigInteger(val);  // is it a (really long?) integer
				String ts = null;
				if (indentFactor > 0) ts = UsefulUtils.guessIfTimestampLong(key, bi.longValue());
				if (ts != null) {
					return AaAnsi.n().fg(Elem.NUMBER).a(val).faintOn().a(ts);
				} else {
					return AaAnsi.n().fg(Elem.NUMBER).a(val);  // yup!
				}
			} catch (NumberFormatException e) {
				return AaAnsi.n().fg(Elem.FLOAT).a(val);  // nope! not an int, but a float
			}
		} catch (NumberFormatException e) {  // nope, not a number at all
			if (val.equalsIgnoreCase("true") || val.equalsIgnoreCase("false")) {  // is it a Boolean?
				return AaAnsi.n().fg(Elem.BOOLEAN).a(val);
			}
			return AaAnsi.n().fg(Elem.STRING).a(val);  // assume it's a string
		}
	}

	@Override
	public final void endElement(String namespaceURI, String localName, String qName) throws SAXException {
		level--;
		String chars = doTrimOnCharacters ? characterDataSb.toString().trim() : characterDataSb.toString();
		if (previous == Tag.START) {
			if (startTagForLater != null) {  // the previous tag is a start tag
				if (chars.length() > 0) {
					ansi.aa(startTagForLater).reset().a('>').aa(guessAndFormatChars(chars, qName, indentFactor)).reset();
					ansi.a("</").fg(Elem.KEY).a(qName).reset().a('>');
				} else {  // closing a startTagForLater tag right away, and no chars, so make it a singleton
					ansi.aa(startTagForLater).reset().a("/>");
				}
				startTagForLater = null;
			} else {  // already been blanked (maybe by a comment?)
				if (chars.length() > 0) {
					ansi.aa(guessAndFormatChars(chars, qName, indentFactor)).reset();
				}
				ansi.a("</").fg(Elem.KEY).a(qName).reset().a('>');
			}
		} else {  // previous tag was another END tag
			if (indentFactor > 0) ansi.a('\n');  // aaron debug june 25
			if (indentFactor > 0 && level > 0) {
				ansi.a(UsefulUtils.indent(indentFactor * level));
			}
			if (chars.length() > 0) ansi.aa(guessAndFormatChars(chars, qName, indentFactor)).reset();
			ansi.a("</").fg(Elem.KEY).a(qName).reset().a('>');
		}
//		if (indent > 0) ansi.a('\n');  // aaron debug june 25
		characterDataSb.setLength(0);
		previous = Tag.END;
	}

	@Override
	public void comment(char[] ch, int start, int length) throws SAXException {
		if (indentFactor > 0) {  // show if not compact
			if (startTagForLater != null) {
				ansi.aa(startTagForLater).reset().a('>');
//				if (indent > 0) ansi.a('\n');
				startTagForLater = null;
			}
			ansi.fg(Elem.MSG_BREAK).a("<!--").a(String.copyValueOf(ch, start, length)).a("-->");
//			ansi.fg(Elem.MSG_BREAK).a("<!--").a(String.copyValueOf(ch, start, length)).a("-->\n");
		}
	}
	
	@Override
	public void startDTD(String name, String publicId, String systemId) throws SAXException { }

	@Override
	public void endCDATA() throws SAXException { }

	@Override
	public void startCDATA() throws SAXException { }

	@Override
	public void endDTD() throws SAXException { }

	@Override
	public void startEntity(String name) throws SAXException { }

	@Override
	public void endEntity(String name) throws SAXException { }
}
