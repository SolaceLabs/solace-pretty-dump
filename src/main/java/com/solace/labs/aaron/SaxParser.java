/*
 * Copyright 2016-2018 Solace Corporation. All rights reserved.
 *
 * http://www.solace.com
 *
 * This source is distributed under the terms and conditions of any contract or
 * contracts between Solace Corporation ("Solace") and you or your company. If
 * there are no contracts in place use of this source is not authorized. No
 * support is provided and no distribution, sharing with others or re-use of 
 * this source is authorized unless specifically stated in the contracts 
 * referred to above.
 *
 * This software is custom built to specifications provided by you, and is 
 * provided under a paid service engagement or statement of work signed between
 * you and Solace. This product is provided as is and is not supported by 
 * Solace unless such support is provided for under an agreement signed between
 * you and Solace.
 */

package com.solace.labs.aaron;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;

import javax.xml.XMLConstants;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

/**
 * This class could be used to SAX parse any XML with the passed-in Handler.
 */
public class SaxParser {

    private static final String XML_FEATURE_DISABLE_DTD        = "http://apache.org/xml/features/disallow-doctype-decl";
    private static final String XML_FEATURE_GENERAL_ENTITIES   = "http://xml.org/sax/features/external-general-entities";
    private static final String XML_FEATURE_PARAMETER_ENTITIES = "http://xml.org/sax/features/external-parameter-entities";
    private static final String XML_FEATURE_LOAD_EXTERNAL_DTD  = "http://apache.org/xml/features/nonvalidating/load-external-dtd";
    private static final String XML_PARAMETER_LEXICAL_HANDLER  = "http://xml.org/sax/properties/lexical-handler";

    protected static final SAXParserFactory saxFactory;

//    private static final Logger logger = LoggerFactory.getLogger(SaxParser.class);

    private static void setFactoryFeature(String feature, boolean value) {
    	try {
    		saxFactory.setFeature(feature,value);
//        	logger.info("SAXParserFactory now enabled for: "+feature);
    	} catch (SAXNotSupportedException | SAXNotRecognizedException | ParserConfigurationException e) {
//        	logger.info("Not supported with this implementation of SAXParserFactory: "+e.toString());
    	}
    }
    
    static {
        saxFactory = SAXParserFactory.newInstance();
        saxFactory.setNamespaceAware(true);
        saxFactory.setValidating(true);
    	// Since an Unmarshaller parses XML and does not support any 
    	// flags for disabling XXE, it's imperative to parse the untrusted 
    	// XML through a configurable secure parser first, generate a 
    	// Source object as a result, and pass the source object to the 
    	// Unmarshaller. - https://www.owasp.org/index.php/XML_External_Entity_(XXE)_Prevention_Cheat_Sheet
        setFactoryFeature(XMLConstants.FEATURE_SECURE_PROCESSING,true);
        setFactoryFeature(XML_FEATURE_DISABLE_DTD,true);
        setFactoryFeature(XML_FEATURE_GENERAL_ENTITIES,false);
        setFactoryFeature(XML_FEATURE_PARAMETER_ENTITIES,false);
        setFactoryFeature(XML_FEATURE_LOAD_EXTERNAL_DTD,false);

    }
    
    private static void setFeatureSilent(XMLReader xmlReader, String feature, boolean value) {
    	try {
    		xmlReader.setFeature(feature,value);
    	} catch (SAXNotSupportedException | SAXNotRecognizedException e) {
    		// fail silently at this point
    	}
    }

    private static void makeReaderSecure(XMLReader xmlReader) {
    	// these first 5 work.  But they shouldn't be needed since the factory is set with these already!
    	setFeatureSilent(xmlReader,XMLConstants.FEATURE_SECURE_PROCESSING,true);
   		setFeatureSilent(xmlReader,XML_FEATURE_DISABLE_DTD,true);
   		setFeatureSilent(xmlReader,XML_FEATURE_GENERAL_ENTITIES,false);
   		setFeatureSilent(xmlReader,XML_FEATURE_PARAMETER_ENTITIES,false);
   		setFeatureSilent(xmlReader,XML_FEATURE_LOAD_EXTERNAL_DTD,false);
   		// these were added for WF, but don't work on my dev machine.  Maybe not for SAX parsers?
   		setFeatureSilent(xmlReader,XMLConstants.ACCESS_EXTERNAL_DTD,false);
   		setFeatureSilent(xmlReader,XMLConstants.ACCESS_EXTERNAL_SCHEMA,false);
   		setFeatureSilent(xmlReader,XMLConstants.ACCESS_EXTERNAL_STYLESHEET,false);
    }
    
    public static void parseString(String xml, DefaultHandler handler) throws SaxParserException {
        parseReader(new BufferedReader(new StringReader(xml)), handler);
    }

    private static void parseSource(InputSource inputSource, DefaultHandler handler) throws SaxParserException {
        if (handler == null)
            throw new NullPointerException("DefaultHandler passed to parse() is null");
        try {
            XMLReader xmlReader = saxFactory.newSAXParser().getXMLReader();
            xmlReader.setContentHandler(handler);
            makeReaderSecure(xmlReader);
            xmlReader.setErrorHandler(handler);
            try {
                xmlReader.setProperty(XML_PARAMETER_LEXICAL_HANDLER, handler);
            } catch (SAXNotSupportedException | SAXNotRecognizedException e) {
            	// ignore this... during schema loading at program startup, will throw an exception since SempReplySchemaLoader uses a DefaultHandler
            	// during runtime, the GenericSempSaxHandler implements the required methods
            }
            inputSource.setEncoding("UTF-8");  // only parsing Java Strings, and payload already converted if here
            xmlReader.parse(inputSource);
        } catch (ParserConfigurationException e) {
            throw new SaxParserException(e);
        } catch (SAXException e) {
            throw new SaxParserException(e);
        } catch (IOException e) {
            throw new SaxParserException(e);
        }
    }
    
    public static void parseStream(InputStream inputStream, DefaultHandler handler) throws SaxParserException {
        try {
        	parseSource(new InputSource(inputStream),handler);
        } finally {
            try {
                inputStream.close();
            } catch (IOException e) {
            }
        }
    }

    public static void parseReader(Reader reader, DefaultHandler handler) throws SaxParserException {
        try {
            parseSource(new InputSource(reader),handler);
        } finally {
            try {
                reader.close();
            } catch (IOException e) {
            }
        }
    }

    private SaxParser() {
        // can't instantiate
        throw new AssertionError("Not allowed to instantiate this class");
    }
}
