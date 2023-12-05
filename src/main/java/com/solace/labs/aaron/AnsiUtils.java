package com.solace.labs.aaron;

import java.io.IOException;
import java.io.StringWriter;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.io.XMLWriter;

public class AnsiUtils {

	
	public static String tryFormat(String trimmedPayload, int indent) throws PrettyException {
		try {
			return tryFormat(trimmedPayload, indent, false);
		} catch (IOException | DocumentException e) {
			throw new PrettyException(e);
		}
	}
	
//	public static String optimisticFormat(String str, int indent) throws PrettyException {
//		try {
//			return tryFormat(str.trim(), indent, true);
//		} catch (IOException | DocumentException e) {
//			throw new PrettyException(e);
//		}
//	}
	
	public static String tryFormat(String trimmedPayload, int indent, boolean lenient) throws IOException, DocumentException {
    	if (trimmedPayload.startsWith("{") && trimmedPayload.endsWith("}")) {  // try JSON object
    		return GsonUtils.parseJsonObject(trimmedPayload, Math.max(indent, 0), lenient);
    	} else if (trimmedPayload.startsWith("[") && trimmedPayload.endsWith("]")) {  // try JSON array
    		return GsonUtils.parseJsonArray(trimmedPayload, Math.max(indent, 0), lenient);
    	} else if (trimmedPayload.startsWith("<") && trimmedPayload.endsWith(">")) {  // try XML
            Document document = DocumentHelper.parseText(trimmedPayload);
            StringWriter stringWriter = new StringWriter();
            XMLWriter xmlWriter = new XMLWriter(stringWriter, PrettyDump.XML_FORMAT);
            xmlWriter.write(document);
            xmlWriter.flush();
            // TBD to add some XML color formatting here
            return stringWriter.toString();
    	} else {
    		throw new IOException("Can't convert");
    	}
	}
	
	
	
	
}
