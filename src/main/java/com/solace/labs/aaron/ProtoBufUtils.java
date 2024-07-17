package com.solace.labs.aaron;

import static com.solace.labs.aaron.UsefulUtils.indent;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.protobuf.AbstractMessage;
import com.google.protobuf.ByteString;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.Descriptors.FieldDescriptor.Type;
import com.google.protobuf.MessageOrBuilder;
import com.solace.labs.topic.Sub;

public class ProtoBufUtils {

    private static final Logger logger = LogManager.getLogger(ProtoBufUtils.class);
    
    static String pretty(Type dataType) {
    	switch (dataType) {
		case BOOL: return "Bool";
		case BYTES:
		case DOUBLE:
		case ENUM:
		case FIXED32:
		case FIXED64:
		case FLOAT:
		case GROUP:
		case INT32:
		case INT64:
		case MESSAGE:
		case SFIXED32:
		case SFIXED64:
		case SINT32:
		case SINT64:
		case STRING:
		case UINT32:
		case UINT64:
		default:
			return dataType.toString();
    	}
    }
	
	static Map<Sub, Method> loadProtobufDefinitions() {
	    Map<Sub, Method> protobufCallbacks = new HashMap<>();

    	// first thing, test if any protobuf definitions are included...
		final String appConfigPath = "protobuf.properties";
		URL url = Thread.currentThread().getContextClassLoader().getResource(appConfigPath);
		if (url == null) {
			System.out.println(new AaAnsi().invalid("WARN: could not locate " + appConfigPath + " on classpath").toString());
			logger.warn("Could not find " + appConfigPath + " on classpath.  Normally inside ./lib/classes/");
		} else {
	    	try {
//				System.out.println("Loading topic subscriptions and protobuf mappings");
				boolean issues = false;
				logger.info("Trying to load {} from path: {}", appConfigPath, url);
				Properties protobufProps = new Properties();
				protobufProps.load(url.openStream());
				logger.info("This is what I loaded: {}", protobufProps);
		    	for (Entry<Object,Object> entry : protobufProps.entrySet()) {
		    		try {
		    			logger.debug("Verifying topic subscription {}", entry.getKey().toString());
		    			Sub sub = new Sub(entry.getKey().toString());
		    			logger.debug("Attempting to load class {}", entry.getValue().toString());
		    			Class<?> clazz = Class.forName(entry.getValue().toString());
		    			boolean foundMethod = false;
		    			for (Method method : clazz.getMethods()) {  // loop through all the methods
		    				if (method.getName().startsWith("parseFrom")) {
		    					if (method.getParameterCount() == 1  && method.getParameterTypes()[0] == byte[].class) {  // the one I want!
			    					// let's double-check that we can call this method with no issues...
									try {
										// try to parse an empty payload
										method.invoke(null, new byte[] { });
	//									com.google.protobuf.GeneratedMessageV3 spanData = (com.google.protobuf.GeneratedMessageV3)method.invoke(null, new byte[] { });
										// looks good!?
			    						foundMethod = true;
				    					protobufCallbacks.put(sub, method);
				    					logger.info("Success! Loaded {} for subscription {}", method.toString(), sub);
									} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
										logger.warn("Could not instantiate parseFrom(byte[]) method!", e);
										issues = true;
									}	
		    					}
		    				}
		    			}
		    			if (!foundMethod) {  // couldn't find the parseFrom(byte[]) method I need
		    				logger.warn("Could not find appropriate parseFrom(byte[]) method in class {}", clazz);
		    				issues = true;
		    			}
		    		} catch (ClassNotFoundException | SecurityException e) {
	    				logger.warn("Could not find class {}", entry.getValue().toString(), e);
	    				issues = true;
		    		}
		    	}
		    	if (issues) {
		    		System.out.println(new AaAnsi().invalid("WARN: had issues loading Protobuf definitions, check log file").toString());
		    	} else {
		    		logger.info("Successfully loaded all Protobuf definitions");
		    	}
	    	} catch (Exception e) {
	    		logger.warn("Caught while trying to load protobuf definitions", e);
	    		System.out.println(new AaAnsi().invalid("WARN: had issues loading Protobuf definitions, check log file").toString());
	    	}
		}
		return protobufCallbacks;
	}
	
//	static ExtensionRegistry registry = ExtensionRegistry.newInstance();
//	
//	public static void add(Object o) {
//		EgressV1.registerAllExtensions(registry);
//	}
	
	public static AaAnsi decode(MessageOrBuilder msg, int indentFactor) {
		Map<FieldDescriptor, Object> map = msg.getAllFields();
		return handleMessage(map, indentFactor, indentFactor);
	}
	
	private static AaAnsi handleMessage(Map<FieldDescriptor, Object> map, final int indent, final int indentFactor) {
		AaAnsi ansi = new AaAnsi();
		handleMessage(map, ansi, indent, indentFactor);
//		if (indentFactor <= 0) return new AaAnsi().fg(Elem.BRACE).a('{') + ansi.toString() + new AaAnsi().fg(Elem.BRACE).a('}').reset();
		if (indentFactor <= 0) return new AaAnsi().fg(Elem.BRACE).a('{').a(ansi).fg(Elem.BRACE).a('}');//.reset();
		else return ansi;//.reset().toString();
	}
	
	private static void handleMessage(Map<FieldDescriptor, Object> map, AaAnsi ansi, final int indent, final int indentFactor) {
		if (map == null) return;
		if (map.isEmpty()) {
			ansi.a(indent(indent)).fg(Elem.NULL).a("<EMPTY>").reset();
			return;
		}
		try {
			Iterator<FieldDescriptor> it = map.keySet().iterator();
			while (it.hasNext()) {
				FieldDescriptor fd = it.next();
				Object val = map.get(fd);  // can't be null, by protobuf definition
				ansi.reset().a(indent(indent));
				// key
//				if (indentFactor > 0) ansi.a("Key ");
				ansi.fg(Elem.KEY).a("'").a(fd.getName()).a("'");
				if (indentFactor > 0) ansi.a(' ');
				ansi.fg(Elem.DATA_TYPE).a('(').a(fd.getType().name());
				if (val instanceof List) ansi.a("[]");
				ansi.a(')').reset();
				if (indentFactor > 0) ansi.a(": ");
				// value
				// https://protobuf.dev/programming-guides/proto3/#scalar
				switch (fd.getType()) {
				case DOUBLE:
				case FLOAT:
					if (val instanceof List) {
						List<?> list = (List<?>)val;
						ansi.fg(Elem.BRACE).a("[ ");
						Iterator<?> listIt = list.iterator();
						if (!listIt.hasNext()) {  // empty!
							ansi.fg(Elem.NULL).a("<EMPTY>").fg(Elem.BRACE).a(" ]");  // empty 
						} else {
							while (listIt.hasNext()) {
//								String str = val.toString();
								Object o = listIt.next();
								ansi.fg(Elem.FLOAT).a(o.toString()).reset();
								if (list.iterator().hasNext()) {
									ansi.a(',');
									if (indentFactor > 0) ansi.a(' ');
								}
							}
							ansi.fg(Elem.BRACE).a(" ]");
						}
					} else {
						ansi.fg(Elem.FLOAT).a(val.toString());
					}
					break;
				case INT32:
				case INT64:
				case SINT32:
				case SINT64:
				case SFIXED32:
				case SFIXED64:
					if (val instanceof List) {
						List<?> list = (List<?>)val;
						ansi.fg(Elem.BRACE).a("[ ");
						Iterator<?> listIt = list.iterator();
						if (!listIt.hasNext()) {  // empty!
							ansi.fg(Elem.NULL).a("<EMPTY>").fg(Elem.BRACE).a(" ]");  // empty 
						} else {
							while (listIt.hasNext()) {
								Object o = listIt.next();
								ansi.fg(Elem.NUMBER).a(o.toString());
								if (indent > 0) {
									String ts = UsefulUtils.guessIfTimestampLong(fd.getName(), (long)val);
									if (ts != null) {
										ansi.makeFaint().a(ts);
									}
								}
								if (list.iterator().hasNext()) {
									ansi.reset().a(',');
									if (indentFactor > 0) ansi.a(' ');
								}
							}
							ansi.fg(Elem.BRACE).a(" ]");
						}
					} else {
						ansi.fg(Elem.NUMBER).a(val.toString());
						if (indent > 0) {
							String ts = UsefulUtils.guessIfTimestampLong(fd.getName(), (long)val);
							if (ts != null) {
								ansi.makeFaint().a(ts);
							}
						}
					}
					break;
				case UINT32:
				case FIXED32:
		//				long l = (int)val & 0x00000000ffffffffL;  // convert to long
		//				ansi.fg(Elem.NUMBER).aRaw(val.toString());
					/*
					if (val instanceof List) {
						List<?> list = (List<?>)val;
						ansi.fg(Elem.BRACE).a("[ ");
						Iterator<?> listIt = list.iterator();
						if (!listIt.hasNext()) {  // empty!
							ansi.fg(Elem.NULL).a("<EMPTY>").fg(Elem.BRACE).a(" ]");  // empty 
						} else {
							while (listIt.hasNext()) {
								Object o = listIt.next();
								ansi.fg(Elem.NUMBER).a(Integer.toUnsignedString((int)o));
								String ts = UsefulUtils.guessIfTimestamp(fd.getName(), (long)val);
								if (ts != null) {
									ansi.makeFaint().a(ts);
								}
								if (list.iterator().hasNext()) {
									ansi.reset().a(',');
									if (indentFactor > 0) ansi.a(' ');
								}
							}
							ansi.fg(Elem.BRACE).a(" ]");
						}
					} else {
						ansi.fg(Elem.NUMBER).a(Integer.toUnsignedString((int)val));
						String ts = UsefulUtils.guessIfTimestamp(fd.getName(), (long)val);
						if (ts != null) {
							ansi.makeFaint().a(ts);
						}
					}*/
					
					
					
					ansi.fg(Elem.NUMBER).a(Integer.toUnsignedString((int)val));
					break;
				case UINT64:
				case FIXED64:
					ansi.fg(Elem.NUMBER).a(Long.toUnsignedString((long)val));
					break;
				case BOOL:
					ansi.fg(Elem.BOOLEAN).a(val.toString());
					break;
				case ENUM:
				case STRING:
					ansi.fg(Elem.STRING).a('"').a(val.toString()).a('"');
					break;
				case BYTES:
					if (val instanceof List) {
						List<?> list = (List<?>)val;
						ansi.fg(Elem.BRACE).a("[ ");
						Iterator<?> listIt = list.iterator();
						if (!listIt.hasNext()) {  // empty!
							ansi.fg(Elem.NULL).a("<EMPTY>").fg(Elem.BRACE).a(" ]");  // empty 
						} else {
							while (listIt.hasNext()) {
								ByteString bs = (ByteString)listIt.next();
								addByteString(bs, ansi, fd);
								if (list.iterator().hasNext()) {
									ansi.reset().a(',');
									if (indentFactor > 0) ansi.a(' ');
								}
							}
							ansi.fg(Elem.BRACE).a(" ]");
						}
					} else {
						addByteString((ByteString)val, ansi, fd);
					}
					break;
				case MESSAGE:
//					AaAnsi inner = new AaAnsi();
//					if (indentFactor > 0) inner.a('\n');
					if (val instanceof List) {  // repeated    TODO can anything be repeated??  I think so..!
						List<?> list = (List<?>)val;
						ansi.fg(Elem.BRACE).a("[");
						Iterator<?> listIt = list.iterator();
						if (!listIt.hasNext()) {  // empty!
							ansi.fg(Elem.NULL).a("<EMPTY>").fg(Elem.BRACE).a("]");  // empty 
						} else {
							if (indentFactor > 0) ansi.a('\n');
							while (listIt.hasNext()) {
								AbstractMessage obj = (AbstractMessage)listIt.next();
								if (indentFactor > 0) ansi.a(indent(indent + (indent/2)));
								ansi.fg(Elem.DATA_TYPE).a("(MESSAGE)").reset();
								if (indentFactor > 0) ansi.a(":\n");
								ansi.a(handleMessage(obj.getAllFields(), indent + indentFactor, indentFactor));
								if (listIt.hasNext()) {
									if (indentFactor > 0) ansi.a('\n');
									else ansi.reset().a(',');
								}
							}
							ansi.fg(Elem.BRACE).a(" ]");
						}						
					} else {  // not repeated, just a message
						if (indentFactor > 0) ansi.a('\n');
						ansi.a(handleMessage(((AbstractMessage)val).getAllFields(), indent+indentFactor, indentFactor));
					}
					break;
//				case GROUP:
//					break;
				default:
					ansi.fg(Elem.DEFAULT).a(val.toString()).a(" (").a(val.getClass().getName()).a(")");//.a(ansi);
		//				sb.append(o.toString()).append(" (").append(o.getClass().getName()).append(")\n");
					break;
				
				}
				ansi.reset();
				if (it.hasNext()) {
					if (indentFactor > 0) ansi.a("\n");
					else ansi.a(",");
				}
//				if (fd.getType() != Type.MESSAGE) {
//					if (indentFactor > 0) ansi.a('\n');
//					else ansi.a(',');
//				}
			}
		} catch (RuntimeException e) {
			System.out.println("This is as far as we got:\n" + ansi.toString());
			throw e;
		}
	}
	
	
	
	private static void addByteString(ByteString bs, AaAnsi ansi, FieldDescriptor fd) {
		if (bs.size() == 4 && fd.getName().toLowerCase().contains("ip")) {  // assume IP address
			ansi.fg(Elem.BYTES).a(UsefulUtils.bytesToSpacedHexString(bs.toByteArray()));
			ansi.makeFaint().a(UsefulUtils.ipAddressBytesToIpV4String(bs.toByteArray()));
		} else {
			ansi.fg(Elem.BYTES).a(UsefulUtils.bytesToSpacedHexString(bs.toByteArray()));
		}
	}
	
}
