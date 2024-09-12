package com.solace.labs.aaron;

import java.io.IOException;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
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
import com.solacesystems.jcsmp.XMLMessage;
import com.solacesystems.jcsmp.XMLMessageProducer;

public class AllMsgGenerator {

	// payload helper methods
	
	static String getPayloadJsonObjectSmall() {
		JsonObject jo = new JsonObject();
		jo.addProperty("name", "aaron");
		jo.addProperty("age", 47);
		jo.addProperty("height", 1.82);
		jo.addProperty("nice", true);
		jo.add("null", null);
		jo.addProperty("emoji", "😅");
		return jo.toString();
	}

	static String getPayloadJsonArraySmall() {
		JsonArray ja = new JsonArray();
		ja.add("aaron");
		ja.add(47);
		ja.add(1.82);
		ja.add(true);
		ja.add((JsonElement)null);
		ja.add("😅");
		return ja.toString();
	}

	static SDTMap getPayloadSDTMapSmall() throws SDTException {
		SDTMap map = f.createMap();
		map.putString("name", "aaron");
		map.putInteger("age", 47);
		map.putDouble("height", 1.82);
		map.putBoolean("nice", true);
		map.putObject("null", null);
		map.putString("emoji", "😅");
		return map;
	}

	static String getPayloadSDTStreamSmall() {
		SDTStream stream = f.createStream();
		stream.writeString("aaron");
		stream.writeInteger(47);
		stream.writeDouble(1.82);
		stream.writeBoolean(true);
		stream.writeObject(null);
		stream.writeString("😅");
		return stream.toString();
	}

	static String getPayloadXmlSmall() {
		String s = "<payload><name>aaron</name><age>47</age><height>1.82</height><nice>true</nice><null></null><emoji>😅</emoji></payload>";
		return s;
	}
	
	static String ENGLISH_STRING = "Hello, this is a regular string. It is written in English.";
	static String SPANISH_STRING = "Hola, esta es una cadena normal. Está escrita en español.";
	static String FRENCH_STRING = "Bonjour, ceci est une chaîne régulière. Elle est écrite en français.";
	static String CANTONESE_STRING = "您好，這是一個常規字串。是用粵語寫的。";
	static String RUSSIAN_STRING = "Здравствуйте, это обычная строка. Она написана на русском языке.";
	static String LOG_STRING = "2024-09-11T01:33:17.110+00:00 <local3.notice> solace1081 event: CLIENT: CLIENT_CLIENT_NAME_CHANGE: default PrettyDump_AaronsThinkPad3/992/00110001/EzN57TqULU Client (20) PrettyDump_AaronsThinkPad3/992/00110001/EzN57TqULU username foo changed name from AaronsThinkPad3/992/00110001/EzN57TqULU\u0000";
	static String CURRENCY_STRING = "The currency of UK is £.  The curency of Japan is ¥.  1/100th of $1.00 is 1¢.";

	static Map<String,String> strings = new HashMap<>();
	static {
//		strings.put("json-simple", getPayloadJsonObjectSmall());
//		strings.put("xml-simple", getPayloadXmlSmall());
//		strings.put("english", ENGLISH_STRING);
//		strings.put("french", FRENCH_STRING);
//		strings.put("spanish", SPANISH_STRING);
//		strings.put("russian", RUSSIAN_STRING);
//		strings.put("arabic", "مرحبًا، هذه سلسلة عادية، وهي مكتوبة باللغة العربية.");
//		strings.put("cantonese", CANTONESE_STRING);
//		strings.put("log", LOG_STRING);
		strings.put("null", "  \u0000  ");
//		strings.put("currency", CURRENCY_STRING);
	}
	
	static byte[] allVals = new byte[256];
	static {
		for (int i=0; i<256; i++) {
			allVals[i] = (byte)(i-128);
		}
	}
	
	enum Cs {
		UTF_8,
		ASCII,
		LATIN1,
		DOS,
		WIN_1252,
		UTF_16,
		;
	}
	
	static Map<Cs, CharsetEncoder> encoders = new HashMap<>();
	static {
		encoders.put(Cs.UTF_8, StandardCharsets.UTF_8.newEncoder().onMalformedInput(CodingErrorAction.REPLACE).onUnmappableCharacter(CodingErrorAction.REPLACE));
		encoders.put(Cs.ASCII, StandardCharsets.US_ASCII.newEncoder().onMalformedInput(CodingErrorAction.REPLACE).onUnmappableCharacter(CodingErrorAction.REPLACE).replaceWith(new byte[] { (byte)0x1a }));  // 0x1a is the "substitute" control char
		encoders.put(Cs.LATIN1, StandardCharsets.ISO_8859_1.newEncoder().onMalformedInput(CodingErrorAction.REPLACE).onUnmappableCharacter(CodingErrorAction.REPLACE).replaceWith(new byte[] { (byte)0x1a }));
		encoders.put(Cs.WIN_1252, Charset.forName("windows-1252").newEncoder().onMalformedInput(CodingErrorAction.REPLACE).onUnmappableCharacter(CodingErrorAction.REPLACE).replaceWith(new byte[] { (byte)0x1a }));
		encoders.put(Cs.DOS, Charset.forName("ibm-437").newEncoder().onMalformedInput(CodingErrorAction.REPLACE).onUnmappableCharacter(CodingErrorAction.REPLACE).replaceWith(new byte[] { (byte)0x1a }));
		encoders.put(Cs.UTF_16, StandardCharsets.UTF_16.newEncoder().onMalformedInput(CodingErrorAction.REPLACE).onUnmappableCharacter(CodingErrorAction.REPLACE));
	}
	
	private static byte[] encode(String s, Cs charset) throws CharacterCodingException {
		return encoders.get(charset).encode(CharBuffer.wrap(s)).array();
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
			super.send(msg);
		}
	}

	class TextMessageSender2 extends Sender2 {
		public TextMessageSender2(String topic, String payload) {
			super(topic, payload);
		}
		public void send() throws JCSMPException {
			TextMessage msg = f.createMessage(TextMessage.class);
			msg.setText((String)payload);
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
			super.send(msg);
		}
	}

	// generic ones
	
	class TextMessageSender implements Sender {
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

	// specific ones
	
	
	class InvalidTextMessage implements Sender {
		public void send() throws JCSMPException {
			TextMessage msg = f.createMessage(TextMessage.class);
			msg.writeAttachment("This is overwriting using writeAttachment()".getBytes());
			producer.send(msg, f.createTopic("gen/text/invalid"));
		}
	}

//	class NonUtf8StringMessages implements Sender {
//		public void send() throws JCSMPException, InterruptedException {
//			BytesMessage msg = f.createMessage(BytesMessage.class);
//			msg.setData(getNonUtf8AsciiString());
//			producer.send(msg, f.createTopic("gen/bytes/non-utf8/ascii"));
//			Thread.sleep(1000);
//			msg.setData(getNonUtf8Latin1String());
//			producer.send(msg, f.createTopic("gen/bytes/non-utf8/latin1"));
//			Thread.sleep(1000);
//			msg.setData(getNonUtf8Utf16String());
//			producer.send(msg, f.createTopic("gen/bytes/non-utf8/utf-16"));
//		}
//	}
	
	
	
	private static final JCSMPFactory f = JCSMPFactory.onlyInstance();
	private final JCSMPProperties props;
	private final JCSMPSession session;
	
	private XMLMessageProducer producer;
	private List<Sender> types = new ArrayList<>();
	private static final String ROOT_TOPIC = "solace";
	
	public AllMsgGenerator(String... args) throws InvalidPropertiesException, CharacterCodingException  {
		props = new JCSMPProperties();
		props.setProperty(JCSMPProperties.HOST, args[0]);
		props.setProperty(JCSMPProperties.VPN_NAME, args[1]);
		props.setProperty(JCSMPProperties.USERNAME, args[2]);
		if (args.length > 3) props.setProperty(JCSMPProperties.PASSWORD, args[3]);
		session = f.createSession(props);
		
//		types.add(new BytesMessageSender2("gen/bytes/array", allVals));
//		types.add(new XmlBinaryMessageSender2("gen/bytes/array", allVals));

		for (String type : strings.keySet()) {
			types.add(new TextMessageSender2(String.format("%s/text/%s", ROOT_TOPIC, type), strings.get(type)));
			types.add(new BytesMessageSender2(String.format("%s/bytes/%s/%s", ROOT_TOPIC, encoders.get(Cs.UTF_8).charset().displayName().toLowerCase(), type), encode(strings.get(type), Cs.UTF_8)));
			types.add(new BytesMessageSender2(String.format("%s/bytes/%s/%s", ROOT_TOPIC, encoders.get(Cs.ASCII).charset().displayName().toLowerCase(), type), encode(strings.get(type), Cs.ASCII)));
			types.add(new BytesMessageSender2(String.format("%s/bytes/%s/%s", ROOT_TOPIC, encoders.get(Cs.LATIN1).charset().displayName().toLowerCase(), type), encode(strings.get(type), Cs.LATIN1)));
			types.add(new BytesMessageSender2(String.format("%s/bytes/%s/%s", ROOT_TOPIC, encoders.get(Cs.WIN_1252).charset().displayName().toLowerCase(), type), encode(strings.get(type), Cs.WIN_1252)));
			types.add(new BytesMessageSender2(String.format("%s/bytes/%s/%s", ROOT_TOPIC, encoders.get(Cs.DOS).charset().displayName().toLowerCase(), type), encode(strings.get(type), Cs.DOS)));
			types.add(new BytesMessageSender2(String.format("%s/bytes/%s/%s", ROOT_TOPIC, encoders.get(Cs.UTF_16).charset().displayName().toLowerCase(), type), encode(strings.get(type), Cs.UTF_16)));
			types.add(new XmlContentMessageSender2(String.format("%s/xml-content/%s", ROOT_TOPIC, type), strings.get(type)));
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
		int count = 10000;
		while (System.in.available() == 0 && count > 0) {
			try {
//				types.stream().
				Sender s = types.get((int)(Math.random() * types.size()));
				s.send();
				System.out.print(".");
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
