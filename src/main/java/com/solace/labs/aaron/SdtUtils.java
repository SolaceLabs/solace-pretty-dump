package com.solace.labs.aaron;

import static com.solace.labs.aaron.UsefulUtils.indent;

import java.math.BigDecimal;
import java.util.Iterator;

import com.solacesystems.common.util.ByteArray;
import com.solacesystems.jcsmp.Destination;
import com.solacesystems.jcsmp.SDTException;
import com.solacesystems.jcsmp.SDTMap;
import com.solacesystems.jcsmp.SDTStream;
import com.solacesystems.jcsmp.Topic;

public class SdtUtils {

	static String printMap(SDTMap map, final int indentFactor) throws SDTException {
		return privPrintMap(map, indentFactor, indentFactor);
	}

	private static String privPrintMap(SDTMap map, final int indent, final int indentFactor) throws SDTException {
		AaAnsi ansi = new AaAnsi();
		privPrintMap(map, indent, indentFactor, ansi);
		if (indentFactor <= 0) return new AaAnsi().fg(Elem.BRACE).a('{') + ansi.toString() + new AaAnsi().fg(Elem.BRACE).a('}');
		else return ansi.toString();
	}
	
	private static void privPrintMap(SDTMap map, final int indent, final int indentFactor, AaAnsi ansi) throws SDTException {
		if (map == null) {
			return;
		}
		String strIndent = indent(indent);
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
					strValue = '"' + strValue + '"';
				} else if (value instanceof Character) {
					strValue = "'" + strValue + "'";
				}
				if (value instanceof SDTMap) {
					AaAnsi inner = new AaAnsi();
					if (indentFactor > 0)
						inner.a("\n");
					inner.aRaw(privPrintMap((SDTMap) value, indent + indentFactor, indentFactor));
//					else
//						inner.a("{ ").a(privPrintMap((SDTMap) value, indent + indentFactor, indentFactor)).a(" }");
					strValue = inner.toString();  // overwrite
				} else if (value instanceof SDTStream) {
//					if (indentFactor > 0) strValue = "\n" + privPrintStream((SDTStream)value, indent + indentFactor, indentFactor);
//					else strValue = "[ " + privPrintStream((SDTStream) value, indent + indentFactor, indentFactor) + " ]";
					AaAnsi inner = new AaAnsi();
					if (indentFactor > 0)
						inner.a("\n");
					inner.aRaw(privPrintStream((SDTStream) value, indent + indentFactor, indentFactor));
//					else
//						inner.a("[ ").a(privPrintStream((SDTStream)value, indent + indentFactor, indentFactor)).a(" ]");
					strValue = inner.toString();  // overwrite
				}
			}
			ansi.reset().a(strIndent);
			if (indentFactor > 0) ansi.a("Key ");
//			else ansi.a("'");
			ansi.fg(Elem.KEY).a("'").a(key).a("'").reset();
			if (indentFactor > 0) ansi.a(" ");
//			else ansi.a("'(");
			ansi.fg(Elem.DATA_TYPE);
//			if (isPrimitive) ansi.fg(Elem.NUMBER);
//			else ansi.fg(Elem.NULL);
			ansi.a('(').a(type).a(')').reset();
			if (indentFactor > 0) ansi.a(": ");
			else ansi.a(":");
			if (value instanceof Topic) ansi.colorizeTopic(strValue).reset();
			else ansi.fg(getElem(value)).aRaw(strValue).reset();
			if (it.hasNext()) {
				if (indentFactor > 0) ansi.a("\n");
				else ansi.a(";");
			}
		}
	}

	
	
	
	static String printStream(SDTStream stream, final int indentFactor) throws SDTException {
		return privPrintStream(stream, indentFactor, indentFactor);
	}
	
	private static String privPrintStream(SDTStream stream, final int indent, final int indentFactor) throws SDTException {
		AaAnsi ansi = new AaAnsi();
		privPrintStream(stream, indent, indentFactor, ansi);
		if (indentFactor <= 0) return new AaAnsi().fg(Elem.BRACE).a('[') + ansi.toString() + new AaAnsi().fg(Elem.BRACE).a(']');
		else return ansi.toString();
	}

	private static Elem getElem(Object value) {
		if (value == null) return Elem.NULL;
		if (value instanceof Number) {
			if (value instanceof Double || value instanceof Float || value instanceof BigDecimal) return Elem.FLOAT;
			return Elem.NUMBER;
		}
		if (value instanceof CharSequence) return Elem.STRING;
		if (value instanceof Character) return Elem.CHAR;
		if (value instanceof ByteArray) return Elem.BYTES;
		if (value instanceof Boolean) return Elem.BOOLEAN;
		if (value instanceof Destination) return Elem.DESTINATION;
		return Elem.UNKNOWN;
	}
	
	
	private static void privPrintStream(SDTStream stream, final int indent, final int indentFactor, AaAnsi ansi) throws SDTException {
		if (stream == null) {
			return;
		}
		String strIndent = indent(indent);
		while (stream.hasRemaining()) {
			Object value = stream.read();
			String strValue = String.valueOf(value);
			String type = "NULL";
			if (value != null) {
				Class<?> valuClass = value.getClass();
				type = valuClass.getSimpleName();
				if (type.endsWith("Impl")) {
					type = type.substring(0, type.length()-4);
				} else if (type.equals("String")) {
					strValue = '"' + strValue + '"';
				} else if (value instanceof Character) {
					strValue = "'" + strValue + "'";
				}
				if (value instanceof SDTMap) {
					AaAnsi inner = new AaAnsi();
					if (indentFactor > 0)
						inner.a("\n");
					inner.aRaw(privPrintMap((SDTMap) value, indent + indentFactor, indentFactor));
//					else
//						a.a("{ ").reset().a(privPrintMap((SDTMap) value, indent + indentFactor, indentFactor)).a(" }").reset();
					strValue = inner.toString();  // overwrite
				} else if (value instanceof SDTStream) {
					AaAnsi inner = new AaAnsi();
					if (indentFactor > 0)
						inner.a("\n");
					inner.aRaw(privPrintStream((SDTStream) value, indent + indentFactor, indentFactor));
//					else
//						inner.fg(Elem.BYTES).a("[ ").reset().a(privPrintStream((SDTStream)value, indent + indentFactor, indentFactor)).reset().a(" ]").reset();
					strValue = inner.toString();  // overwrite
				}
			}
//			ansi.reset().a(strIndent).a("(");
//			if (isPrimitive) ansi.fgYellow();
//			else ansi.fgMagenta();
//			if (isPrimitive) ansi.a(Attribute.INTENSITY_FAINT);
//			else ansi.fgMagenta();
//			ansi.a(type).reset();
//			if (indentFactor > 0) ansi.a("): ");
//			else ansi.a("):");
			ansi.reset().a(strIndent);
			ansi.fg(Elem.DATA_TYPE).a('(').a(type).a("):").reset();
			if (indentFactor > 0) ansi.a(' ');
			if (value instanceof Topic) ansi.colorizeTopic(strValue).reset();
			else ansi.fg(getElem(value)).aRaw(strValue).reset();
//			ansi.a(String.format("(%s): %s", type, strValue));
			if (stream.hasRemaining() ) {
				if (indentFactor > 0) ansi.a("\n");
				else ansi.a(",");
			}
		}
		stream.rewind();
	}
}
