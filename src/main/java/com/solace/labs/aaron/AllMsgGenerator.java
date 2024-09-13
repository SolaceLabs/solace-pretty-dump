package com.solace.labs.aaron;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.solacesystems.jcsmp.BytesMessage;
import com.solacesystems.jcsmp.BytesXMLMessage;
import com.solacesystems.jcsmp.InvalidPropertiesException;
import com.solacesystems.jcsmp.JCSMPException;
import com.solacesystems.jcsmp.JCSMPFactory;
import com.solacesystems.jcsmp.JCSMPProperties;
import com.solacesystems.jcsmp.JCSMPSession;
import com.solacesystems.jcsmp.JCSMPStreamingPublishCorrelatingEventHandler;
import com.solacesystems.jcsmp.SDTException;
import com.solacesystems.jcsmp.SDTMap;
import com.solacesystems.jcsmp.SDTStream;
import com.solacesystems.jcsmp.TextMessage;
import com.solacesystems.jcsmp.XMLContentMessage;
import com.solacesystems.jcsmp.XMLMessageProducer;

public class AllMsgGenerator {

	// payload helper methods
	
	private static final String NAME = "Aárön";
	private static final int AGE = 47;
	private static final double HEIGHT = 1.82;
	
	static String getPayloadJsonObjectSmall() {
		JsonObject jo = new JsonObject();
		jo.addProperty("name", NAME);
		jo.addProperty("age", AGE);
		jo.addProperty("height", HEIGHT);
		jo.addProperty("nice", true);
		jo.add("null", null);
		jo.addProperty("emoji", "😅");
		return jo.toString();
	}

	static String getPayloadJsonArraySmall() {
		JsonArray ja = new JsonArray();
		ja.add(NAME);
		ja.add(AGE);
		ja.add(HEIGHT);
		ja.add(true);
		ja.add((JsonElement)null);
		ja.add("😅");
		return ja.toString();
	}

	static String getPayloadXmlSmall() {
		String s = "<payload><name>"+NAME+"</name><age>"+AGE+"</age><height>"+HEIGHT+"</height><nice>true</nice><null></null><emoji>😅</emoji></payload>";
		return s;
	}
	
	
	enum TextStrings {
		JSON_SIMPLE(getPayloadJsonObjectSmall()),
		XML_SIMPLE(getPayloadXmlSmall()),
		ENGLISH("Hello, this sentence is written in English."),
		FRENCH("Bonjour, cette phrase est écrite en français."),
		SPANISH("Hola, esta frase está escrita en español."),
		RUSSIAN("Здравствуйте, это предложение написано на русском языке."),
		ARABIC("مرحباً، هذه الجملة مكتوبة باللغة العربية."),
		CANTONESE("你好，這句話是用粵語寫的。"),
		LOG_ENTRY("2024-09-11T01:33:17.110+00:00 <local3.notice> solace1081 event: CLIENT: CLIENT_CLIENT_NAME_CHANGE: default PrettyDump_AaronsThinkPad3/992/00110001/EzN57TqULU Client (20) PrettyDump_AaronsThinkPad3/992/00110001/EzN57TqULU username foo changed name from AaronsThinkPad3/992/00110001/EzN57TqULU\u0000"),
		ASCII_REPLACE_CHAR("This char \u001a is SUB."),
		UTF8_REPLACE_CHAR("This has the replacement � char."),
		CURRENCY("The currency of UK is £.  The curency of Japan is ¥, and € in Europe.  1/100th of $1.00 is 1¢."),
		NULL_CHAR("A \u0000 A"),
		;
		final String payload;
		TextStrings(String payload) {
			this.payload = payload;
		}
		@Override public String toString() {
			return name().replace('_', '-').toLowerCase();
		}
	}

	static EnumSet<TextStrings> textPayloads2 = EnumSet.of(
			TextStrings.ASCII_REPLACE_CHAR,
			TextStrings.ENGLISH,
			TextStrings.JSON_SIMPLE,
			TextStrings.XML_SIMPLE
			);
//	EnumSet<Texts> textPayloads2 = EnumSet.allOf(Texts.class);
	
	static Map<String,byte[]> binaryPayloads = new HashMap<>();
	static {
		byte[] allByteValsArray = new byte[256];
		for (int i=0; i<128; i++) {
			allByteValsArray[i] = (byte)(i);
		}
		for (int i=128; i<256; i++) {
			allByteValsArray[i] = (byte)(i-256);
		}
		binaryPayloads.put("all-byte-values", allByteValsArray);
		
	}
	
	static SDTMap getPayloadSDTMapSmall() throws SDTException {
		SDTMap map = f.createMap();
		map.putString("name", NAME);
		map.putInteger("age", AGE);
		map.putDouble("height", HEIGHT);
		map.putBoolean("nice", true);
		map.putObject("null", null);
		map.putString("emoji", "😅");
		return map;
	}

	static Map<String,Object> mapPayloads = new HashMap<>();
	static {
		
	}

	static String getPayloadSDTStreamSmall() {
		SDTStream stream = f.createStream();
		stream.writeString(NAME);
		stream.writeInteger(AGE);
		stream.writeDouble(HEIGHT);
		stream.writeBoolean(true);
		stream.writeObject(null);
		stream.writeString("😅");
		return stream.toString();
	}


	static Map<String,Object> streamPayloads = new HashMap<>();
	static {
		
	}

	
	
	enum CharsetType {
		UTF_8,
		ASCII,
		LATIN1,
		DOS,
		WIN_1252,
		UTF_16,
		;
		
		String getDisplayName() {
			return name().replace('_', '-').toLowerCase();
//			return encoders.get(this).charset().displayName().toLowerCase();
		}
	}
	
	static Map<CharsetType, CharsetEncoder> encoders = new HashMap<>();
	static {
		encoders.put(CharsetType.UTF_8, StandardCharsets.UTF_8.newEncoder()
				.onMalformedInput(CodingErrorAction.REPLACE)
				.onUnmappableCharacter(CodingErrorAction.REPLACE));
		encoders.put(CharsetType.ASCII, StandardCharsets.US_ASCII.newEncoder()
				.onMalformedInput(CodingErrorAction.REPLACE)
				.onUnmappableCharacter(CodingErrorAction.REPLACE)
				.replaceWith(new byte[] { (byte)0x1a }));  // 0x1a is the "substitute" control char
		encoders.put(CharsetType.LATIN1, StandardCharsets.ISO_8859_1.newEncoder()
				.onMalformedInput(CodingErrorAction.REPLACE)
				.onUnmappableCharacter(CodingErrorAction.REPLACE)
				.replaceWith(new byte[] { (byte)0x1a }));  // replace will never fire b/c latin1 works for every byte value
		encoders.put(CharsetType.WIN_1252, Charset.forName("windows-1252").newEncoder()
				.onMalformedInput(CodingErrorAction.REPLACE)
				.onUnmappableCharacter(CodingErrorAction.REPLACE)
				.replaceWith(new byte[] { (byte)0x1a }));
		encoders.put(CharsetType.DOS, Charset.forName("IBM437").newEncoder()
				.onMalformedInput(CodingErrorAction.REPLACE)
				.onUnmappableCharacter(CodingErrorAction.REPLACE)
				.replaceWith(new byte[] { (byte)0x1a }));
		encoders.put(CharsetType.UTF_16, StandardCharsets.UTF_16.newEncoder()
				.onMalformedInput(CodingErrorAction.REPORT)
				.onUnmappableCharacter(CodingErrorAction.REPORT));
	}
	
	private static byte[] encode(String s, CharsetType charset) {
		try {
			ByteBuffer bb = encoders.get(charset).encode(CharBuffer.wrap(s));
			byte[] bytes = new byte[bb.limit()];
			bb.get(bytes);
			return bytes;
		} catch (CharacterCodingException e) {  // shouldn't happen!
			e.printStackTrace();
			throw new IllegalStateException("Shouldn't be able to get here!", e);
		}
	}
	
	
	// Sender object helper methods

	interface Sender {
		void send() throws JCSMPException, InterruptedException;
	}
	
	abstract class Sender2 implements Sender {
		final String topic;
		final Object payload;
		Sender2(String topic, Object payload) {
			this.topic = topic;
			this.payload = payload;
		}
		void send(BytesXMLMessage msg) throws JCSMPException {
			producer.send(msg, f.createTopic(topic));
		}
	}
	
	
	class TextMessageSender2 extends Sender2 {
		public TextMessageSender2(String topic, String payload) {
			super(topic, payload);
		}
		public void send() throws JCSMPException {
			TextMessage msg = f.createMessage(TextMessage.class);
			msg.setText((String)payload);
			System.out.println(topic + ": " + (String)payload);
			super.send(msg);
		}
	}

	class BytesMessageSender2 extends Sender2 {
		public BytesMessageSender2(String topic, byte[] payload) {
			super(topic, payload);
		}
		public BytesMessageSender2(String topic, String payload) {
			super(topic, payload.getBytes(StandardCharsets.UTF_8));
		}
		public void send() throws JCSMPException {
			BytesMessage msg = f.createMessage(BytesMessage.class);
			msg.setData((byte[])payload);
			System.out.println(topic + ": " + Arrays.toString((byte[])payload));
			super.send(msg);
		}
	}

	class XmlContentMessageSender2 extends Sender2 {
		public XmlContentMessageSender2(String topic, String payload) {
			super(topic, payload);
		}
		public void send() throws JCSMPException {
			XMLContentMessage msg = f.createMessage(XMLContentMessage.class);
			msg.setXMLContent((String)payload);
			System.out.println(topic + ": " + (String)payload);
			super.send(msg);
		}
	}

	class XmlBinaryMessageSender2 extends Sender2 {
		public XmlBinaryMessageSender2(String topic, byte[] payload) {
			super(topic, payload);
		}
		public void send() throws JCSMPException {
			BytesXMLMessage msg = f.createBytesXMLMessage();
			msg.writeBytes((byte[])payload);
			System.out.println(topic + ": " + Arrays.toString((byte[])payload));
			super.send(msg);
		}
	}

	class DoubleBinaryMessageSender2 extends Sender2 {
		public DoubleBinaryMessageSender2(String topic, byte[] payload) {
			super(topic, payload);
		}
		public void send() throws JCSMPException {
			BytesXMLMessage msg = f.createBytesXMLMessage();
			msg.writeBytes((byte[])payload);
			msg.writeAttachment((byte[])payload);
			System.out.println(topic + ": " + Arrays.toString((byte[])payload));
			super.send(msg);
		}
	}

	// generic ones
	
/*	class TextMessageSender implements Sender {
		final String topic;
		final String payload;
		public TextMessageSender(String topic, String payload) {
			this.topic = topic;
			this.payload = payload;
		}
		public void send() throws JCSMPException {
			TextMessage msg = f.createMessage(TextMessage.class);
			msg.setText(payload);
			producer.send(msg, f.createTopic(topic));
		}
	}

	class BytesMessageSender implements Sender {
		final String topic;
		final byte[] payload;
		public BytesMessageSender(String topic, byte[] payload) {
			this.topic = topic;
			this.payload = payload;
		}
		public void send() throws JCSMPException {
			BytesMessage msg = f.createMessage(BytesMessage.class);
			msg.setData(payload);
			producer.send(msg, f.createTopic(topic));
		}
	}

	class XmlMessageSender implements Sender {
		final String topic;
		final byte[] payload;
		public XmlMessageSender(String topic, byte[] payload) {
			this.topic = topic;
			this.payload = payload;
		}
		public void send() throws JCSMPException {
			XMLMessage msg = f.createMessage(XMLMessage.class);
			msg.writeAttachment(payload);
			producer.send(msg, f.createTopic(topic));
		}
	}
*/
	// specific ones
	
	
	class InvalidTextMessage implements Sender {
		public void send() throws JCSMPException {
			TextMessage msg = f.createMessage(TextMessage.class);
			msg.writeAttachment("This is overwriting using writeAttachment()".getBytes());
			producer.send(msg, f.createTopic("gen/text/invalid"));
		}
	}
	
	
	
	private static final JCSMPFactory f = JCSMPFactory.onlyInstance();
	private final JCSMPProperties props;
	private final JCSMPSession session;
	
	private XMLMessageProducer producer;
	private List<Sender> types = new ArrayList<>();
	private static final String ROOT_TOPIC = "solace";
	
	public AllMsgGenerator(String... args) throws InvalidPropertiesException  {
		props = new JCSMPProperties();
		props.setProperty(JCSMPProperties.HOST, args[0]);
		props.setProperty(JCSMPProperties.VPN_NAME, args[1]);
		props.setProperty(JCSMPProperties.USERNAME, args[2]);
		if (args.length > 3) props.setProperty(JCSMPProperties.PASSWORD, args[3]);
		session = f.createSession(props);
		
		types.add(new BytesMessageSender2(ROOT_TOPIC + "/bytes/array/raw", binaryPayloads.get("all-byte-values")));
		types.add(new BytesMessageSender2(ROOT_TOPIC + "/bytes/array/utf-8", encode(StandardCharsets.UTF_8.decode(ByteBuffer.wrap(binaryPayloads.get("all-byte-values"))).toString(), CharsetType.UTF_8)));
		types.add(new TextMessageSender2(ROOT_TOPIC + "/text/array/ascii", StandardCharsets.US_ASCII.decode(ByteBuffer.wrap(binaryPayloads.get("all-byte-values"))).toString()));
		types.add(new BytesMessageSender2(ROOT_TOPIC + "/bytes/array/ascii", encode(StandardCharsets.US_ASCII.decode(ByteBuffer.wrap(binaryPayloads.get("all-byte-values"))).toString(), CharsetType.ASCII)));
//		types.add(new XmlBinaryMessageSender2(ROOT_TOPIC + "/bytes/array/raw", binaryPayloads.get("all-byte-values")));

/*		for (String type : textPayloads.keySet()) {
			types.add(makeSender(TextMessageSender2.class, type, null));
			types.add(makeSender(BytesMessageSender2.class, type, CharsetType.UTF_8));
//			types.add(makeSender(BytesMessageSender2.class, type, CharsetType.ASCII));
			types.add(makeSender(BytesMessageSender2.class, type, CharsetType.LATIN1));
//			types.add(makeSender(BytesMessageSender2.class, type, CharsetType.WIN_1252));
//			types.add(makeSender(BytesMessageSender2.class, type, CharsetType.DOS));
//			types.add(makeSender(BytesMessageSender2.class, type, CharsetType.UTF_16));
			types.add(makeSender(XmlContentMessageSender2.class, type, null));
			types.add(makeSender(XmlBinaryMessageSender2.class, type, CharsetType.UTF_8));
//			types.add(makeSender(XmlBinaryMessageSender2.class, type, CharsetType.ASCII));
			types.add(makeSender(XmlBinaryMessageSender2.class, type, CharsetType.LATIN1));
//			types.add(makeSender(XmlBinaryMessageSender2.class, type, CharsetType.WIN_1252));
//			types.add(makeSender(XmlBinaryMessageSender2.class, type, CharsetType.DOS));
//			types.add(makeSender(XmlBinaryMessageSender2.class, type, CharsetType.UTF_16));
			types.add(makeSender(DoubleBinaryMessageSender2.class, type, CharsetType.UTF_8));
			types.add(makeSender(DoubleBinaryMessageSender2.class, type, CharsetType.LATIN1));
		}
		*/
		
		for (TextStrings type : textPayloads2) {
			types.add(makeSender(TextMessageSender2.class, type, null));
		}
	}
	
/*	private Sender makeSender(Class clazz, String type, CharsetType charset) {
		if (clazz == TextMessageSender2.class) {
			return new TextMessageSender2(String.format("%s/text/%s", ROOT_TOPIC, type), textPayloads.get(type));
		} else if (clazz == BytesMessageSender2.class) {
			return new BytesMessageSender2(String.format("%s/bytes/%s/%s", ROOT_TOPIC, charset.getDisplayName(), type), encode(textPayloads.get(type), charset));
		} else if (clazz == XmlContentMessageSender2.class) {
			return new XmlContentMessageSender2(String.format("%s/xml-content/%s", ROOT_TOPIC, type), textPayloads.get(type));
		} else if (clazz == XmlBinaryMessageSender2.class) {
			return new XmlBinaryMessageSender2(String.format("%s/xml-binary/%s/%s", ROOT_TOPIC, charset.getDisplayName(), type), encode(textPayloads.get(type), charset));
		} else if (clazz == DoubleBinaryMessageSender2.class) {
			return new DoubleBinaryMessageSender2(String.format("%s/double-binary/%s/%s", ROOT_TOPIC, charset.getDisplayName(), type), encode(textPayloads.get(type), charset));
		} else {
			throw new AssertionError();
		}
	}*/

	private Sender makeSender(Class clazz, TextStrings type, CharsetType charset) {
		if (clazz == TextMessageSender2.class) {
			return new TextMessageSender2(String.format("%s/text/%s", ROOT_TOPIC, type), type.payload);
		} else if (clazz == BytesMessageSender2.class) {
			return new BytesMessageSender2(String.format("%s/bytes/%s/%s", ROOT_TOPIC, charset.getDisplayName(), type), encode(type.payload, charset));
		} else if (clazz == XmlContentMessageSender2.class) {
			return new XmlContentMessageSender2(String.format("%s/xml-content/%s", ROOT_TOPIC, type), type.payload);
		} else if (clazz == XmlBinaryMessageSender2.class) {
			return new XmlBinaryMessageSender2(String.format("%s/xml-binary/%s/%s", ROOT_TOPIC, charset.getDisplayName(), type), encode(type.payload, charset));
		} else if (clazz == DoubleBinaryMessageSender2.class) {
			return new DoubleBinaryMessageSender2(String.format("%s/double-binary/%s/%s", ROOT_TOPIC, charset.getDisplayName(), type), encode(type.payload, charset));
		} else {
			throw new AssertionError();
		}
	}

	private void run() throws JCSMPException, IOException {
//		
		String blah = "  \u0000  ";
		byte[] blah2 = blah.getBytes(StandardCharsets.UTF_8);
		TextMessage msg = f.createMessage(TextMessage.class);
		msg.setText(blah);
		byte[] blah3 = new byte[msg.getAttachmentContentLength()];
		msg.readAttachmentBytes(blah3);
		System.out.println(Arrays.toString(blah2));
		System.out.println(Arrays.toString(blah3));
		String blah4 = msg.getText();
		System.out.println(Arrays.toString(blah4.getBytes(StandardCharsets.UTF_8)));
		
		
		session.connect();
		producer = session.getMessageProducer(new JCSMPStreamingPublishCorrelatingEventHandler() {
			@Override
			public void responseReceivedEx(Object key) {
			}
			
			@Override
			public void handleErrorEx(Object key, JCSMPException cause, long timestamp) {
			}
		});
		
		System.out.println("Connected.  Press [ENTER] to quit.");
		int count = 100000;
		while (System.in.available() == 0 && count > 0) {
			try {
//				types.stream().
				Sender s = types.get((int)(Math.random() * types.size()));
				s.send();
//				System.out.print(".");
				Thread.sleep(3000);
				count--;
			} catch (InterruptedException e) {
				break;
			}
		}
		System.out.println("Quitting.");
		
		
		session.closeSession();
	}
	
	
	

	public static void main(String... args) throws JCSMPException, IOException {
		AllMsgGenerator gen = new AllMsgGenerator(args);
		gen.run();
		
	}
	
	
	
	
	
}
