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

	private AaAnsi ansi = new AaAnsi();
	private final int indent;
	private int level = 0;
    private StringBuilder characterDataSb = new StringBuilder();   // need StringBuilder for the 'characters' method since SAX can call it multiple times
	private Tag previous = null;
	private AaAnsi startTagForLater = null;
	
	enum Tag {
		START,
		END,
		;
	}
	
	public SaxHandler(int indent) {
		this.indent = indent;
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
			ansi.a(startTagForLater).reset().a('>');
			if (indent > 0) ansi.a('\n');
		} else if (previous == Tag.END) {  // aa debug june 25
			if (indent > 0) ansi.a('\n');
		}
		startTagForLater = new AaAnsi();  // reset for new start tag
		if (indent > 0 && level > 0) {
			startTagForLater.a(UsefulUtils.indent(indent * level));
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
	static AaAnsi guessAndFormatChars(String val, String key) {
		try {
			Double.parseDouble(val);  // is it a number?
			try {
//				long l = Long.parseLong(val);  // is it specifically a long or int or something
				BigInteger bi = new BigInteger(val);
				
				String ts = UsefulUtils.guessIfTimestamp(key, bi.longValue());
				if (ts != null) {
//					ansi.fg(Elem.CHAR).a(ts);
					return new AaAnsi().fg(Elem.NUMBER).a(val).makeFaint().a(ts);
				} else {
					return new AaAnsi().fg(Elem.NUMBER).a(val);  // yup!
				}
//				return new AaAnsi().fg(Elem.NUMBER).a(val);  // yup!
			} catch (NumberFormatException e) {
				return new AaAnsi().fg(Elem.FLOAT).a(val);  // nope!
			}
		} catch (NumberFormatException e) {  // nope, not a number
			if (val.equalsIgnoreCase("true") || val.equalsIgnoreCase("false")) {  // is it a Boolean?
				return new AaAnsi().fg(Elem.BOOLEAN).a(val);
			}
			return new AaAnsi().fg(Elem.STRING).a(val);  // assume it's a string
		}
	}

	@Override
	public final void endElement(String namespaceURI, String localName, String qName) throws SAXException {
		level--;
		String chars = characterDataSb.toString().trim();
		if (previous == Tag.START) {
			if (startTagForLater != null) {  // the previous tag is a start tag
				if (chars.length() > 0) {
					ansi.a(startTagForLater).reset().a('>').a(guessAndFormatChars(chars, qName)).reset();
					ansi.a("</").fg(Elem.KEY).a(qName).reset().a('>');
				} else {  // closing a startTagForLater tag right away, and no chars, so make it a singleton
					ansi.a(startTagForLater).reset().a("/>");
				}
				startTagForLater = null;
			} else {  // already been blanked (maybe by a comment?)
				if (chars.length() > 0) {
					ansi.a(guessAndFormatChars(chars, qName)).reset();
				}
				ansi.a("</").fg(Elem.KEY).a(qName).reset().a('>');
			}
		} else {  // previous tag was another END tag
			if (indent > 0) ansi.a('\n');  // aaron debug june 25
			if (indent > 0 && level > 0) {
				ansi.a(UsefulUtils.indent(indent * level));
			}
			if (chars.length() > 0) ansi.a(guessAndFormatChars(chars, qName)).reset();
			ansi.a("</").fg(Elem.KEY).a(qName).reset().a('>');
		}
//		if (indent > 0) ansi.a('\n');  // aaron debug june 25
		characterDataSb.setLength(0);
		previous = Tag.END;
	}

	@Override
	public void comment(char[] ch, int start, int length) throws SAXException {
		if (indent > 0) {  // show if not compact
			if (startTagForLater != null) {
				ansi.a(startTagForLater).reset().a('>');
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
