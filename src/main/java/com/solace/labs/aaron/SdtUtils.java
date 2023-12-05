package com.solace.labs.aaron;

import java.util.Iterator;

import org.fusesource.jansi.Ansi;

import com.solacesystems.jcsmp.SDTException;
import com.solacesystems.jcsmp.SDTMap;
import com.solacesystems.jcsmp.SDTStream;
import static com.solace.labs.aaron.UsefulUtils.indent;

public class SdtUtils {

//	static String printMap(SDTMap map) throws SDTException {
//		return printMap(map, 4);
//	}

	static String printMap(SDTMap map, final int indentFactor) throws SDTException {
		return privPrintMap(map, indentFactor, indentFactor);
	}

	private static String privPrintMap(SDTMap map, final int indent, final int indentFactor) throws SDTException {
		Ansi ansi = new AaAnsi(false);
		privPrintMap(map, indent, indentFactor, ansi);
		return ansi.toString();
	}
	
	private static void privPrintMap(SDTMap map, final int indent, final int indentFactor, Ansi ansi) throws SDTException {
		if (map == null) {
			return;
		}
		String strIndent = indent(indent);
		Iterator<String> it = map.keySet().iterator();
		while (it.hasNext()) {
			boolean isPrimitive = true;
			String key = it.next();
			Object value = map.get(key);
			String strValue = String.valueOf(value);
			String type = "NULL";
			if (value != null) {
				Class<?> valuClass = value.getClass();
				type = valuClass.getSimpleName();
				if (type.endsWith("Impl")) {
					type = type.substring(0, type.length()-4);
					isPrimitive = false;
				} else if (type.equals("String")) {
					strValue = '"' + strValue + '"';
				}
				if (value instanceof SDTMap) {
					Ansi a = new AaAnsi(false);
					if (indentFactor > 0)
						a.a("\n").a(privPrintMap((SDTMap) value, indent + indentFactor, indentFactor));
					else
						a.fgMagenta().a("{ ").reset().a(privPrintMap((SDTMap) value, indent + indentFactor, indentFactor)).fgMagenta().a(" }").reset();
					strValue = a.toString();  // overwrite
				} else if (value instanceof SDTStream) {
//					if (indentFactor > 0) strValue = "\n" + privPrintStream((SDTStream)value, indent + indentFactor, indentFactor);
//					else strValue = "[ " + privPrintStream((SDTStream) value, indent + indentFactor, indentFactor) + " ]";
					Ansi a = new AaAnsi(false);
					if (indentFactor > 0)
						a.a("\n").a(privPrintStream((SDTStream) value, indent + indentFactor, indentFactor));
					else
						a.fgMagenta().a("[ ").reset().a(privPrintStream((SDTStream)value, indent + indentFactor, indentFactor)).fgMagenta().a(" ]").reset();
					strValue = a.toString();  // overwrite
				}
			}
			ansi.reset().a(strIndent);
			if (indentFactor > 0) ansi.a("Key '");
			else ansi.a("'");
			ansi.fgBlue().a(key).reset();
			if (indentFactor > 0) ansi.a("' (");
			else ansi.a("'(");
//			if (isPrimitive) ansi.fgYellow();
//			else ansi.fgMagenta();
			ansi.a(type).reset();
			if (indentFactor > 0) ansi.a("): ");
			else ansi.a("):");
			ansi.fgGreen().a(strValue).reset();
			if (it.hasNext()) {
				if (indentFactor > 0) ansi.a("\n");
				else ansi.a("; ");
			}
		}
	}

	static String printStream(SDTStream stream, final int indentFactor) throws SDTException {
		return privPrintStream(stream, indentFactor, indentFactor);
	}
	
	private static String privPrintStream(SDTStream stream, final int indent, final int indentFactor) throws SDTException {
		Ansi ansi = new AaAnsi(false);
		privPrintStream(stream, indent, indentFactor, ansi);
		return ansi.toString();
	}

	
	
	private static void privPrintStream(SDTStream stream, final int indent, final int indentFactor, Ansi ansi) throws SDTException {
		if (stream == null) {
			return;
		}
		String strIndent = indent(indent);
		while (stream.hasRemaining()) {
			boolean isPrimitive = true;
			Object value = stream.read();
			String strValue = String.valueOf(value);
			String type = "NULL";
			if (value != null) {
				Class<?> valuClass = value.getClass();
				type = valuClass.getSimpleName();
				if (type.endsWith("Impl")) {
					type = type.substring(0, type.length()-4);
					isPrimitive = false;
				} else if (type.equals("String")) {
					strValue = '"' + strValue + '"';
				}
				if (value instanceof SDTMap) {
					Ansi a = new AaAnsi(false);
					if (indentFactor > 0)
						a.a("\n").a(privPrintMap((SDTMap) value, indent + indentFactor, indentFactor));
					else
						a.fgMagenta().a("{ ").reset().a(privPrintMap((SDTMap) value, indent + indentFactor, indentFactor)).fgMagenta().a(" }").reset();
					strValue = a.toString();  // overwrite
				} else if (value instanceof SDTStream) {
					Ansi a = new AaAnsi(false);
					if (indentFactor > 0)
						a.a("\n").a(privPrintStream((SDTStream) value, indent + indentFactor, indentFactor));
					else
						a.fgMagenta().a("[ ").reset().a(privPrintStream((SDTStream)value, indent + indentFactor, indentFactor)).fgMagenta().a(" ]").reset();
					strValue = a.toString();  // overwrite
				}
			}
			ansi.reset().a(strIndent).a("(");
//			if (isPrimitive) ansi.fgYellow();
//			else ansi.fgMagenta();
			ansi.a(type).reset();
			if (indentFactor > 0) ansi.a("): ");
			else ansi.a("):");
			ansi.fgGreen().a(strValue).reset();
//			ansi.a(String.format("(%s): %s", type, strValue));
			if (stream.hasRemaining() ) {
				if (indentFactor > 0) ansi.a("\n");
				else ansi.a("; ");
			}
		}
		stream.rewind();
	}
}
