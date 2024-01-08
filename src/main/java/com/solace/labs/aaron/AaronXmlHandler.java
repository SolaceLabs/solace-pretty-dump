package com.solace.labs.aaron;

import org.xml.sax.Attributes;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.ext.LexicalHandler;
import org.xml.sax.helpers.DefaultHandler;

public class AaronXmlHandler extends DefaultHandler implements LexicalHandler, ErrorHandler {


	private AaAnsi ansi = new AaAnsi();
	private final int indent;
	private int level = 0;
    private StringBuilder characterDataSb = new StringBuilder();   // need StringBuilder for the 'characters' method since SAX can call it multiple times
	private Tag previous = null;
	private AaAnsi start = null;
	
	enum Tag {
		START,
		END,
		;
	}
	
	public AaronXmlHandler(int indent) {
		this.indent = indent;
	}
	

	@Override
	public void startDocument() throws SAXException { }

	@Override
	public void endDocument() throws SAXException { }

	public String getResult() {
		return ansi.toString();
	}

	@Override
	public final void characters(char[] ch, int start, int length) {
        characterDataSb.append(String.valueOf(ch,start,length));
//		ansi.a(String.valueOf(ch, start, length));
	}

	@Override
	public final void startElement(String namespaceURI, String localName, String qName, Attributes atts) throws SAXException {
        characterDataSb.setLength(0);  // start capturing a new bunch of characters after this start... re-use to prevent object creation
		if (previous == Tag.START) {
//			assert start != null;
			ansi.a(start).reset().a('>');
			if (indent > 0) ansi.a('\n');
		}
		start = new AaAnsi();
		if (indent > 0 && level > 0) {
			start.a(UsefulUtils.indent(indent * level));
		}
		start.a('<').fg(Elem.KEY).a(qName);
		for (int i=0; i<atts.getLength(); i++) {
			start.a(' ').fg(Elem.DATA_TYPE).a(atts.getQName(i)).fg(Elem.BRACE).a("=").fg(Elem.BYTES_CHARS).a('"').a(atts.getValue(i)).a('"');
    	}
//		ansi.a('>');
		previous = Tag.START;
		level++;
	}

	private AaAnsi formatChars(String s) {
		try {
			Double.parseDouble(s);
			try {
				Integer.parseInt(s);
				return new AaAnsi().fg(Elem.NUMBER).a(s);
			} catch (NumberFormatException e) {
				return new AaAnsi().fg(Elem.FLOAT).a(s);
			}
		} catch (NumberFormatException e) {
			if (s.equalsIgnoreCase("true") || s.equalsIgnoreCase("false")) {
				return new AaAnsi().fg(Elem.BOOLEAN).a(s);
			}
			return new AaAnsi().fg(Elem.STRING).a(s);
		}
	}

	@Override
	public final void endElement(String namespaceURI, String localName, String qName) throws SAXException {
		level--;
		String chars = characterDataSb.toString().trim();
		if (previous == Tag.START) {
			if (chars.length() > 0) {
				ansi.a(start).reset().a('>').a(formatChars(chars)).reset();
				ansi.a("</").fg(Elem.KEY).a(qName).reset().a('>');
			} else {  // closing a start tag right away, and no chars, so make it a singleton
				ansi.a(start).reset().a("/>");
			}
			start = null;// new AaAnsi();
		} else {  // previous tag was another END tag
			if (indent > 0 && level > 0) {
				ansi.a(UsefulUtils.indent(indent * level));
			}
			if (chars.length() > 0) ansi.a(formatChars(chars)).reset();
			ansi.a("</").fg(Elem.KEY).a(qName).reset().a('>');
		}
		if (indent > 0) ansi.a('\n');
		characterDataSb.setLength(0);
		previous = Tag.END;
	}

	@Override
	public void comment(char[] ch, int start, int length) throws SAXException {
		// ignore comments
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
