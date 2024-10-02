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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.fusesource.jansi.Ansi;
import org.fusesource.jansi.Ansi.Attribute;
import org.fusesource.jansi.AnsiConsole;

import com.solace.labs.aaron.AaAnsi.ColorMode;
import com.solace.labs.aaron.Banner.Which;
import com.solacesystems.jcsmp.AccessDeniedException;
import com.solacesystems.jcsmp.Browser;
import com.solacesystems.jcsmp.BrowserProperties;
import com.solacesystems.jcsmp.BytesXMLMessage;
import com.solacesystems.jcsmp.CapabilityType;
import com.solacesystems.jcsmp.ConsumerFlowProperties;
import com.solacesystems.jcsmp.DeliveryMode;
import com.solacesystems.jcsmp.EndpointProperties;
import com.solacesystems.jcsmp.FlowEvent;
import com.solacesystems.jcsmp.FlowEventArgs;
import com.solacesystems.jcsmp.FlowEventHandler;
import com.solacesystems.jcsmp.FlowReceiver;
import com.solacesystems.jcsmp.InvalidPropertiesException;
import com.solacesystems.jcsmp.JCSMPChannelProperties;
import com.solacesystems.jcsmp.JCSMPErrorResponseException;
import com.solacesystems.jcsmp.JCSMPErrorResponseSubcodeEx;
import com.solacesystems.jcsmp.JCSMPException;
import com.solacesystems.jcsmp.JCSMPFactory;
import com.solacesystems.jcsmp.JCSMPGlobalProperties;
import com.solacesystems.jcsmp.JCSMPNotComparableException;
import com.solacesystems.jcsmp.JCSMPProperties;
import com.solacesystems.jcsmp.JCSMPReconnectEventHandler;
import com.solacesystems.jcsmp.JCSMPSession;
import com.solacesystems.jcsmp.JCSMPTransportException;
import com.solacesystems.jcsmp.Queue;
import com.solacesystems.jcsmp.ReplicationGroupMessageId;
import com.solacesystems.jcsmp.SessionEvent;
import com.solacesystems.jcsmp.SessionEventArgs;
import com.solacesystems.jcsmp.SessionEventHandler;
import com.solacesystems.jcsmp.Topic;
import com.solacesystems.jcsmp.TopicProperties;
import com.solacesystems.jcsmp.XMLMessageConsumer;
import com.solacesystems.jcsmp.XMLMessageListener;

/** Based on DirectSubscriber sample from https://github.com/SolaceSamples/solace-samples-java-jcsmp */
public class PrettyDump {

	private static final String APP_NAME = PrettyDump.class.getSimpleName();
	private static final Logger logger = LogManager.getLogger(PrettyDump.class);
	static {
		logger.info("### Starting PrettyDump!");
	}
	private static final String DEFAULT_TOPIC = "#noexport/>";

	private static final PrintStream o = System.out;
	private static final JCSMPFactory f = JCSMPFactory.onlyInstance();
	private final JCSMPProperties properties = new JCSMPProperties();
	private JCSMPSession session;
	//	private static PayloadHelper payloadHelper;

	private String[] topics = new String[] { DEFAULT_TOPIC };  // default starting topic
	private Queue queue = null;  // might be temp/non-durable, or regular
	private Browser browser = null;  // in case we need it, can't do async, is a blocking/looping pull
	private FlowReceiver flowQueueReceiver = null;  // for queues and tempQueue
	private XMLMessageConsumer directConsumer = null;  // for Direct

	final private MessageHelper ph;
	final private ConfigState config = new ConfigState();
	private long origMsgCount = Long.MAX_VALUE;
	private long msgCountRemaining = Long.MAX_VALUE;
//	private long skipMsgCount = 0;
//	private long skipMsgCountOrig = 0;
	private String selector = null;
	private String contentFilter = null;
	private long browseFrom = -1;
	private long browseTo = Long.MAX_VALUE;
	private ReplicationGroupMessageId browseToRGMID = null;
	private ReplicationGroupMessageId browseFromRGMID = null;

	private PrettyDump() {
		this.ph = new MessageHelper(config);
	}

	

	private void printParamsInfo(String indentStr, String countStr) {
		//    	if (indentStr.isEmpty()) indentStr = "2";
		//    	String countStr = Long.toString(count);
		//    	o.println(countStr);
		int pad = Math.max(indentStr.length(), countStr.length());
		AaAnsi ansi = AaAnsi.n();
		if (!indentStr.isEmpty()) {
			ansi.a("Indent=").fg(Elem.NUMBER).a(String.format("%"+pad+"s",indentStr)).reset().a(" (");
			if (config.isOneLineMode()) {
				if (config.isNoPayload()) {
					ansi.a("topic-only mode");
				} else if (config.getCurrentIndent() == 2) {
					ansi.a("two-line mode");
				} else {
					ansi.a("one-line mode");
					if (config.isAutoResizeIndent()) {
						ansi.a(", auto-indent");
					} else {
						ansi.a(", topic width=").a(config.getCurrentIndent()-2);
					}
				}
				if (config.isAutoSpaceTopicLevelsEnabled()) {
					ansi.a(", aligned topic levels");
				} else {
				}
				if (!config.isNoPayload()) {
					if (config.isAutoTrimPayload()) {
						ansi.a(", auto-trim payload");
					} else {
						ansi.a(", full payload");
					}
				}
			} else {
				if (config.isNoPayload()) {
					ansi.a("no-payload mode");//normal mode, indent=").a(config.getCurrentIndent());
				} else {
					ansi.a("normal mode");
					if (config.getCurrentIndent() > 0) ansi.a(", indent=").a(config.getCurrentIndent());
				}
				if (config.getCurrentIndent() == 0) {
					ansi.a(", compressed");
					if (config.isAutoTrimPayload()) ansi.a(", hide User Props");
				}
			}
			ansi.a(')');
		}
		if (!countStr.isEmpty()) {  // normal mode
			if (ansi.length() > 0) ansi.a("\n Count=");
			else ansi.a("Count=");
			ansi.fg(Elem.NUMBER).a(String.format("%"+pad+"s", countStr)).reset();
			if (origMsgCount == Long.MAX_VALUE) {
				assert config.isLastNMessagesEnabled();
				ansi.a(" (only dump last " + config.getLastNMessagesCapacity());
			} else {
				ansi.a(" (stop after " + countStr);
			}
			ansi.a(" messages)");
		}
		//    	}
		o.println(ansi);
	}
	
	public static void main(String... args) throws JCSMPException, IOException, InterruptedException {
		PrettyDump dump = new PrettyDump();
		dump.run(args);
	}
	
	
	
	
	// MAIN!

	@SuppressWarnings("deprecation")  // this is for our use of Message ID for the browser
	public void run(String... args) throws JCSMPException, IOException, InterruptedException {
		
		//		
		//		ReplicationGroupMessageId one = f.createReplicationGroupMessageId("rmid1:3477f-a5ce520f5ec-00000000-000f89e2");
		//		ReplicationGroupMessageId two = f.createReplicationGroupMessageId("rmid1:3477f-a5ce520f5ec-00000000-000f89e2");
		//		o.println(one.equals(two));
		//		System.exit(0);
		//		
		for (String arg : args) {
			if (arg.equals("-h") || arg.equals("--h") || arg.equals("-?") || arg.startsWith("--?") || arg.equals("-help") || arg.equals("--help")) {
				HelperText.printHelpText(true);
//				o.println("Use -hm  for more help");
				System.exit(0);
			} else if (arg.equals("-hm") || arg.equals("--hm") || arg.equals("-??")) {
				HelperText.printHelpMoreText();
				System.exit(0);
			} else if (arg.equals("-he") || arg.equals("--he")) {
				HelperText.printHelpExamples();
				System.exit(0);
			}
		}
		if (args.length == 1 && args[0].toLowerCase().equals("wrap")) {
			PrettyWrap.main(new String[0]);
			System.exit(0);
		}
		config.setCharset(CHARSET);
		if (System.getenv("PRETTY_SELECTOR") != null && !System.getenv("PRETTY_SELECTOR").isEmpty()) {
			o.println(AaAnsi.n().invalid(String.format("Env var PRETTY_SELECTOR deprecated, use argument --selector=\"%s\" instead.%n", System.getenv("PRETTY_SELECTOR"))));
			System.exit(1);
			//			selector = System.getenv("PRETTY_SELECTOR");
		}
		if (System.getenv("PRETTY_FILTER") != null && !System.getenv("PRETTY_FILTER").isEmpty()) {
			o.println(AaAnsi.n().invalid(String.format("Env var PRETTY_FILTER deprecated, use argument --filter=\"%s\" instead.%n", System.getenv("PRETTY_FILTER"))));
			System.exit(1);
			//			contentFilter = System.getenv("PRETTY_FILTER");
		}

		// special command-line argument handling
		ArrayList<String> regArgsList = new ArrayList<>();
		ArrayList<String> specialArgsList = new ArrayList<>();
		for (String arg : args) {
			if (arg.startsWith("--") || (arg.equals("-defaults"))) specialArgsList.add(arg);
			else regArgsList.add(arg);
		}

		// let's do the regular arguments now
		String host = "localhost";
		String vpn = "default";
		String username = "foo";
		String password = "bar";
		// new shortcut MODE... if first arg looks like topics, assume topic wildcard, and assume localhost default connectivity for rest
		if (regArgsList.size() > 0 && regArgsList.size() <= 2) {  // can only have topic+indent in shortcut mode
			String arg0 = regArgsList.get(0);
			boolean shortcut = false;
			if ((arg0.contains("/") && !arg0.contains("//"))  // hosts can't have any of these "topic-looking" chars
					|| arg0.contains(">")
					|| arg0.contains("*")
					|| arg0.contains("#")
					|| arg0.startsWith("tq:")) {  // shortcut MODE
				shortcut = true;
				//				topics = args[0].split("\\s*,\\s*");  // split on commas, remove any whitespace around them
			} else if (arg0.matches("^[qbf]:.+")) {  // either browse, queue consume, or browse first to localhost
				shortcut = true;
				//				topics = new String[] { args[0] };  // just the one, queue name will get parsed out later
			} else if (regArgsList.size() == 1) {  // just one param, maybe its indent?
				// see if it's an integer, we'll use for indent
				try {
					config.dealWithIndentParam(arg0);
					// if nothing thrown, then it's a valid indent, so assume shortcut mode
					shortcut = true;
					regArgsList.add(0, DEFAULT_TOPIC);  // stick the default topic in front of this arg
				} catch (NumberFormatException e) {  // not a number
					// do nothing, host will get set below because !shortcut
				} catch (IllegalArgumentException e) {  // a number, but not valid... let the check code later deal with it
					shortcut = true;
					regArgsList.add(0, DEFAULT_TOPIC);  // stick the default topic in front of this arg
				}
			}
			if (shortcut) {  // add the default params
				regArgsList.add(0, host);
				regArgsList.add(1, vpn);
				regArgsList.add(2, username);
				regArgsList.add(3, password);
			} else {
				host = regArgsList.get(0);
			}
		} else if (regArgsList.size() > 0) {
			host = regArgsList.get(0);
		}
//		o.println(argsList);
		if (regArgsList.size() > 1) vpn = regArgsList.get(1);
		if (regArgsList.size() > 2) username = regArgsList.get(2);
		if (regArgsList.size() > 3) password = regArgsList.get(3);
		if (regArgsList.size() > 4) {
			String arg4 = regArgsList.get(4);
			if (arg4.matches("^[qbf]:.+")) {
				topics = new String[] { arg4 };  // just the one, queue name will get parsed out later
			} else {
				topics = arg4.split("\\s*,\\s*");  // split on commas, remove any whitespace around them, might start with tq:
			}
		}
		if (regArgsList.size() > 5) {
			String indentStr = regArgsList.get(5);  // grab the correct command-line argument
			try {
				config.dealWithIndentParam(indentStr);
			} catch (IllegalArgumentException e) {
				o.println(AaAnsi.n().invalid(String.format("Invalid value for indent: '%s'.  ", indentStr)));
				o.println();
				HelperText.printHelpIndent();
				o.println();
				o.println("prettydump -h  or  -hm for more help details, or see README.md for more details");
				System.exit(1);
			}
		}
		if (regArgsList.size() > 6) {
			HelperText.printHelpMoreText();
			throw new IllegalArgumentException("Too many arguments");
		}
		// we'll handle the special -- args down below
		
		AnsiConsole.systemInstall();
		if (AnsiConsole.getTerminalWidth() >= 80) o.print(Banner.printBanner(Which.DUMP));
		else o.println();
		o.println(APP_NAME + " initializing...");
		config.setProtobufCallbacks(ProtoBufUtils.loadProtobufDefinitions());
		BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
		// now let's get on with it!
//		final JCSMPProperties properties = new JCSMPProperties();
		properties.setProperty(JCSMPProperties.HOST, host);          // host:port
		properties.setProperty(JCSMPProperties.VPN_NAME, vpn);     // message-vpn
		properties.setProperty(JCSMPProperties.USERNAME, username);      // client-username
		properties.setProperty(JCSMPProperties.PASSWORD, password);  // client-password
		properties.setProperty(JCSMPProperties.REAPPLY_SUBSCRIPTIONS, true);  // subscribe Direct subs after reconnect
		properties.setProperty(JCSMPProperties.SUB_ACK_WINDOW_SIZE, 20);  // moderate performance
		JCSMPChannelProperties channelProps = new JCSMPChannelProperties();
		channelProps.setConnectRetries(0);
		channelProps.setReconnectRetries(-1);
		channelProps.setConnectRetriesPerHost(1);
		//		channelProps.setCompressionLevel(9);
		properties.setProperty(JCSMPProperties.CLIENT_CHANNEL_PROPERTIES, channelProps);
		properties.setProperty(JCSMPProperties.IGNORE_DUPLICATE_SUBSCRIPTION_ERROR, false);  // we need these exceptions to know if our tempQ is new or same
		JCSMPGlobalProperties.setShouldDropInternalReplyMessages(false);  // neat trick to hear all other req/rep messages
		// if we're going to override any special settings, do it here
		
		if (specialArgsList.contains("--defaults") || specialArgsList.contains("-defaults")) {
			o.println("These are the default JCSMPProperties that PrettyDump uses.");
			o.println("Most are defaults in JCSMP, some have been overriden.");
			o.println("Not all of these are settable, see Javadocs for more info.");
			List<String> upperProps = new ArrayList<>();
			Map<String, String> propsMap = new HashMap<>();
			for (String key : properties.propertyNames()) {
				upperProps.add(key.toUpperCase());
				propsMap.put(key.toUpperCase(), key);
			}
			upperProps.sort(new Comparator<String>() {
				@Override
				public int compare(String o1, String o2) {
					return o1.compareTo(o2);
				}
			});
			AaAnsi ansi = AaAnsi.n();
			for (String key : upperProps) {
				Object o = properties.getProperty(propsMap.get(key));
//				aa.fg(Elem.KEY).a(key).reset().a('=');
				if (o instanceof Integer) {
					ansi.fg(Elem.KEY).a('-').a('-').a(key).reset().a('=');
					ansi.fg(Elem.NUMBER).a(o.toString()).a('\n');
				} else if (o instanceof Boolean) {
					ansi.fg(Elem.KEY).a('-').a('-').a(key).reset().a('=');
					ansi.fg(Elem.BOOLEAN).a(o.toString()).a('\n');
				} else if (o instanceof String) {
					ansi.fg(Elem.KEY).a('-').a('-').a(key).reset().a('=');
					ansi.fg(Elem.STRING).a(o.toString()).a('\n');
				} else {  //skip this one
//					aa.fg(Elem.KEY).a(key).reset().a('=');
//					aa.fg(Elem.DATA_TYPE).a(o.getClass().getSimpleName()).a(' ').a(o.toString());
				}
			}
			o.println(ansi.reset());
			o.print("For JCSMPProperties descriptions, see Javadocs here: ");
			o.println(new Ansi().fg(4).a(Attribute.UNDERLINE).a("https://docs.solace.com/API-Developer-Online-Ref-Documentation/java/com/solacesystems/jcsmp/JCSMPProperties.html").reset());
			System.exit(0);
		}

		int jcscmpPropCount = 0;
		for (String arg : specialArgsList) {
			if (arg.startsWith("--selector")) {
				try {
					selector = arg.substring("--selector=".length());
					if (selector != null && !selector.isEmpty() && selector.length() > 2000) {
						o.println(AaAnsi.n().invalid("Selector length greater than 2000 character limit!"));
						o.println("Quitting! 💀");
						System.exit(1);
					}
				} catch (StringIndexOutOfBoundsException e) {
					o.println(AaAnsi.n().invalid(String.format("Must specify a value for Selector.")));
					HelperText.printHelpMoreText();
					o.println("See README.md for more detailed help.");
					System.exit(1);
				}
			} else if (arg.startsWith("--filter")) {
				try {
					contentFilter = arg.substring("--filter=".length());
					if (contentFilter != null && !contentFilter.isEmpty()) {
						// compiling might throw an exception
						Pattern p = Pattern.compile(contentFilter, Pattern.MULTILINE | Pattern.DOTALL);//| Pattern.CASE_INSENSITIVE);
						config.setRegexFilterPattern(p);
					}
				} catch (StringIndexOutOfBoundsException e) {
					o.println(AaAnsi.n().invalid(String.format("Must specify a regex for Filter.")));
					HelperText.printHelpMoreText();
					o.println("See README.md for more detailed help.");
				}
			} else if (arg.equals("--trim")) {
				config.setAutoTrimPayload(true);
			} else if (arg.equals("--ts")) {
				config.includeTimestamp = true;
			} else if (arg.equals("--export")) {
				config.noExport = false;
			} else if (arg.equals("--compressed")) {
				config.isCompressed = true;  // doesn't really matter, we never use this again
				properties.setProperty(JCSMPProperties.CLIENT_CHANNEL_PROPERTIES_COMPRESSION_LEVEL, 9);
			} else if (arg.equals("--raw")) {
				config.rawPayload = true;
			} else if (arg.startsWith("--count")) {
				String argVal = "?";
				try {
					argVal = arg.substring("--count=".length());
					//				o.println(argVal);
					long count = Long.parseLong(argVal);
					if (count > 0) {
						msgCountRemaining = count;
						origMsgCount = count;
					} else if (count < 0) {  // keep the last N messages
						config.enableLastNMessage(Math.abs((int)count));
					} else {
						throw new NumberFormatException();
					}
				} catch (NumberFormatException | StringIndexOutOfBoundsException e) {
					o.println(AaAnsi.n().invalid(String.format("Invalid value for count: '%s'. n > 0 to stop after n msgs; n < 0 to display last n msgs.", argVal)));
					HelperText.printHelpMoreText();
					System.exit(1);
				}
/*			} else if (arg.startsWith("--skip")) {
				String argVal = "?";
				try {
					argVal = arg.substring("--skip=".length());
					//				o.println(argVal);
					long skip = Long.parseLong(argVal);
					if (skip > 0) {
						skipMsgCount = skip;
						skipMsgCountOrig = skip;
//					} else if (count < 0) {  // keep the last N messages
//						config.enableLastNMessage(Math.abs((int)count));
					} else {
						throw new NumberFormatException();
					}
				} catch (NumberFormatException | StringIndexOutOfBoundsException e) {
					o.println(AaAnsi.n().invalid(String.format("Invalid value for skip: '%s'. n > 0 to skip first n msgs.", argVal)));
					printHelpMoreText();
					System.exit(1);
				}*/
			} else if (arg.startsWith("--") && arg.length() > 2 && arg.contains("=") && UsefulUtils.setContainsIgnoreCase(properties.propertyNames(), arg.substring(2, arg.indexOf('=')))) {
				String key = arg.substring(2, arg.indexOf('='));
				String propName = UsefulUtils.setGetIgnoreCase(properties.propertyNames(), key);
				String val = arg.substring(arg.indexOf('=')+1);
				if (val.isEmpty()) {
					throw new IllegalArgumentException("Must provide JCSMPProperty name = value");
				}
				Object obj = properties.getProperty(propName);
				AaAnsi ansi = AaAnsi.n().a(jcscmpPropCount == 0 ? "Overriding " : "           ").fg(Elem.KEY).a("JCSMPProperties.").a(propName.toUpperCase()).reset().a(": ");
				if (obj instanceof Integer) {
					int oldVal = (int)obj;
					properties.setProperty(propName, Integer.parseInt(val));
					ansi.fg(Elem.NUMBER).a(oldVal).reset().a(" → ").fg(Elem.NUMBER).a(properties.getProperty(propName).toString());
				} else if (obj instanceof Boolean) {
					boolean oldVal = (boolean)obj;
					properties.setProperty(propName, Boolean.parseBoolean(val));
					ansi.fg(Elem.BOOLEAN).a(oldVal).reset().a(" → ").fg(Elem.BOOLEAN).a(properties.getProperty(propName).toString());
				} else {  // assume String!
					String oldVal = (String)obj;
//					if (oldVal.isEmpty()) oldVal = "\"\"";
					properties.setProperty(propName, val);
					if (oldVal.isEmpty()) {
						ansi.fg(Elem.STRING).faintOn().a("<EMPTY>").reset();
					} else {
						ansi.fg(Elem.STRING).a(oldVal).reset();
					}
					ansi.a(" → ").fg(Elem.STRING).a(properties.getProperty(propName).toString());
				}
//				o.println("Overriding JCSMPProperties." + propName + "=" + properties.getProperty(propName));
				o.println(ansi.reset());
				jcscmpPropCount++;
			} else {
				o.println(AaAnsi.n().invalid("Don't recognize this argument '" + arg + "'!"));
				o.println("Quitting! 💀");
				System.exit(1);
			}
		}
		
		session = f.createSession(properties, null, new SessionEventHandler() {
			@Override
			public void handleEvent(SessionEventArgs event) {  // could be reconnecting, connection lost, etc.
				//        		o.println(" > " + event.getEvent());
				if (event.getEvent() == SessionEvent.RECONNECTING) {
					if (config.isConnected) {  // first time
						config.isConnected = false;
						//						o.print(AaAnsi.n().invalid("Connection lost!") + "\n > RECONNECTING...");
						o.println(AaAnsi.n().warn("TCP Connection lost!"));
						o.print(" > SESSION RECONNECTING...");
					} else {
						o.print(".");
						//            			o.print(". " + Thread.currentThread().getName() + " (" + Thread.currentThread().isDaemon() + ") ");
						//            			Set<Thread> threadSet = Thread.getAllStackTraces().keySet();
						//            			for (Thread t : threadSet) {
						//            				if (!t.isDaemon()) {
						//            					o.printf("%s %s%n", t.getName(), (t.getStackTrace().length > 0 ? Arrays.toString(t.getStackTrace()) : "n/a"));
						//            					o.printf("%s %s%n", t.getName(), t.getStackTrace()[0].toString());
						//            				}
						//            			}
					}
				} else if (event.getEvent() == SessionEvent.RECONNECTED) {
					o.println("\n > SESSION RECONNECTED!");
					config.isConnected = true;
				} else {
					if (!config.isConnected) o.println();  // add a blank line
					o.println(" > " + event.getEvent() + ": " + event);
				}
			}
		});
		session.connect();  // connect to the broker... could throw JCSMPException, so best practice would be to try-catch here..!
		config.isConnected = true;
		session.setProperty(JCSMPProperties.CLIENT_NAME, "PrettyDump_" + session.getProperty(JCSMPProperties.CLIENT_NAME));
		o.printf("%s connected to VPN '%s' on broker '%s' v%s.%n%n",
				APP_NAME, session.getProperty(JCSMPProperties.VPN_NAME_IN_USE),
				session.getCapability(CapabilityType.PEER_ROUTER_NAME),
				session.getCapability(CapabilityType.PEER_SOFTWARE_VERSION));

		{  // print out some nice param info
			String indentStr = "";  // default
			String countStr = "";
			if (regArgsList.size() > 5) indentStr = regArgsList.get(5);  // grab the correct command-line argument
			if (origMsgCount != Long.MAX_VALUE) countStr = Long.toString(origMsgCount);
			else if (config.isLastNMessagesEnabled()) countStr = Integer.toString(-config.getLastNMessagesCapacity());
			if (regArgsList.size() > 6) countStr = regArgsList.get(6);
			if (!indentStr.isEmpty() || !countStr.isEmpty()) printParamsInfo(indentStr, countStr);
		}		

//				for (CapabilityType cap : CapabilityType.values()) {
//					o.println(cap + ": " + session.getCapability(cap));
//				}

		// is it a queue?
		if (topics.length == 1 && topics[0].startsWith("q:") && topics[0].length() > 2) {  // QUEUE CONSUME!
			// configure the queue API object locally
			String queueName = topics[0].substring(2);
			queue = f.createQueue(queueName);
			// Create a Flow be able to bind to and consume messages from the Queue.
			final ConsumerFlowProperties flowProps = new ConsumerFlowProperties();
			flowProps.setEndpoint(queue);
			flowProps.setAckMode(JCSMPProperties.SUPPORTED_MESSAGE_ACK_CLIENT);  // not best practice, but good enough for this app
			flowProps.setActiveFlowIndication(true);
			if (selector != null) {
				//            	flow_prop.setSelector("pasta = 'rotini' OR pasta = 'farfalle'");
				flowProps.setSelector(selector);
			}
			o.printf("Attempting to bind to queue '%s' on the broker... ", queueName);
			flowQueueReceiver = null;
			final CountDownLatch latch = new CountDownLatch(1);
			try {
				flowQueueReceiver = session.createFlow(new PrinterHelper(), flowProps, null, new FlowEventHandler() {
					@Override
					public void handleEvent(Object source, FlowEventArgs event) {
						FlowEvent fe = event.getEvent();
						// Flow events are usually: active, reconnecting (i.e. unbound), reconnected, active
						if (fe == FlowEvent.FLOW_RECONNECTING && config.isConnected) {
							// flow active here?
							o.println(AaAnsi.n().warn("'"+queueName+"' flow closed! Queue egress probably shutdown at the broker."));
							o.println(" > FLOW RECONNECTING...");
						} else if (fe == FlowEvent.FLOW_RECONNECTED) {
							// flow inactive here?
							o.println(" > FLOW RECONNECTED!");
						} else if (fe == FlowEvent.FLOW_ACTIVE) {
							config.isFlowActive = true;
							if (latch.getCount() == 1) {  // first time here, so skip this notification
								if (config.isFlowActive) {
									// TODO just to hide the warning, but need to probably do something with this?
								}
							} else {
								if (!config.isConnected) o.println();  // connection coming back up
								o.println(" > " + fe);
							}
						} else if (fe == FlowEvent.FLOW_INACTIVE) {
							config.isFlowActive = false;
						} else if (!config.isConnected) {
							// ignore
						} else {
							o.println(" > " + fe);
						}
						//						o.printf(" ### Received a Flow event: %s%n", event);  // hide this, don't really need to show in this app
					}
				});
				o.println("success!");
				o.println();
				// double-check
				if (contentFilter != null) {
					o.println(AaAnsi.n().a("🔎 Client-side regex Filter detected: ").fg(Elem.STRING).a(contentFilter).reset());
					o.println(AaAnsi.n().warn("Filtered messages that are not displayed will still be ACKed!"));
				}
				if (config.isLastNMessagesEnabled()) {
					o.println(AaAnsi.n().warn(String.format("Only last %d will be displayed, but all received messages will still be ACKed!", config.getLastNMessagesCapacity())));
				}
				if (selector != null) {
					o.println(AaAnsi.n().a("🔎 Selector detected: ").fg(Elem.STRING).a(selector).reset());
					o.print(AaAnsi.n().fg(Elem.PAYLOAD_TYPE).a(String.format("Will consume/ACK %s messages on queue '%s' that match Selector.%nUse browse 'b:' command-line option otherwise.%nAre you sure? [y|yes]: ", msgCountRemaining == Long.MAX_VALUE ? "all" : msgCountRemaining, queueName)));
				} else {  // no selectors, consume all
					o.print(AaAnsi.n().fg(Elem.PAYLOAD_TYPE).a(String.format("Will consume/ACK %s messages on queue '%s'.%nUse browse 'b:' command-line option otherwise.%nAre you sure? [y|yes]: ", msgCountRemaining == Long.MAX_VALUE ? "all" : msgCountRemaining, queueName)));
				}
				o.print(AaAnsi.n().fg(Elem.WARN));  // turn the console yellow
				String answer = reader.readLine().trim().toLowerCase();
				o.print(AaAnsi.n());  // to reset() the ANSI
				if (!"y".equals(answer) && !"yes".equals(answer)) {
					o.println("\nExiting. 👎🏼");
					System.exit(0);
				}
				latch.countDown();  // this hides the FLOW_ACTIVE until after all this stuff
				//                reader.close();
				// tell the broker to start sending messages on this queue receiver
				flowQueueReceiver.start();
				//            } catch (OperationNotSupportedException e) {  // not allowed to do this
				//            	o.println(AaAnsi.n().invalid(String.format("%nUh-oh!  There was a problem: %s%n%s%nQuitting!", e.getMessage(), e.toString())));
				//    			System.exit(1);
				//            } catch (JCSMPErrorResponseException e) {  // something else went wrong: queue not exist, queue shutdown, etc.
				//            	o.println(AaAnsi.n().invalid(String.format("%nUh-oh!  There was a problem: %s: %s", e.getClass().getSimpleName(), e.getMessage())));
				////            	o.println(AaAnsi.n().invalid(String.format("%nUh-oh!  There was a problem: %s%n%s%nQuitting!", e.getResponsePhrase(), e.toString())));
				//    			System.exit(1);
			} catch (JCSMPException | AccessDeniedException e) {  // something else went wrong: queue not exist, queue shutdown, etc.
				o.println(AaAnsi.n().invalid(String.format("%nUh-oh!  There was a problem: %s: %s", e.getClass().getSimpleName(), e.getMessage())));
				//            	if (e.getCause() instanceof JCSMPErrorResponseException) {
				//                	o.println(AaAnsi.n().invalid(((JCSMPErrorResponseException)e.getCause()).getMessage()));
				//            	}
				o.println("Quitting! 💀");
				logger.error("Caught this connecting to a queue",e);
				if (flowQueueReceiver != null) flowQueueReceiver.close();
				session.closeSession();
				System.exit(1);
			}
		} else if (topics.length == 1 && (topics[0].startsWith("b:") || topics[0].startsWith("f:")) && topics[0].length() > 2) {  // BROWSING!
			String queueName = topics[0].substring(2);
			final Queue queue = f.createQueue(queueName);
			final BrowserProperties bp = new BrowserProperties();
			bp.setEndpoint(queue);
			bp.setTransportWindowSize(255);
			bp.setWaitTimeout(1000);
			if (selector != null) {
				bp.setSelector(selector);
			}
			o.printf("Attempting to browse queue '%s' on the broker... ", queueName);
			try {
				browser = session.createBrowser(bp, new FlowEventHandler() {
					@Override
					public void handleEvent(Object source, FlowEventArgs event) {
						// Flow events are usually: active, reconnecting (i.e. unbound), reconnected, active
						if (event.getEvent() == FlowEvent.FLOW_RECONNECTING && config.isConnected) {
							o.println(AaAnsi.n().warn("'"+queueName+"' flow closed! Queue egress probably shutdown at the broker."));
							o.print(" > FLOW RECONNECTING...");
						} else if (event.getEvent() == FlowEvent.FLOW_RECONNECTED) {
							o.println("\n > FLOW RECONNECTED!");
						} // else ignore!
						//						o.printf(" ### Received a Flow event: %s%n", event);  // hide this, don't really need to show in this app
					}
				});
				o.println("success!");
				o.println();
				if (selector != null) {
					//					o.println(AaAnsi.n().fg(Elem.PAYLOAD_TYPE).a(String.format("🔎 Selector detected: \"%s\"", selector)).reset());
					o.println(AaAnsi.n().a("🔎 Selector detected: ").fg(Elem.STRING).a(selector).reset());
				}
				if (contentFilter != null) {
					o.println(AaAnsi.n().a("🔎 Client-side regex Filter detected: ").fg(Elem.STRING).a(contentFilter).reset());
				}
				if (topics[0].startsWith("b:")) {  // regular browse, prompt for msg IDs
					o.print(AaAnsi.n().fg(Elem.PAYLOAD_TYPE).a(String.format("Browse %s messages -> press [ENTER],%n or to/from or range of MsgSpoolIDs (e.g. \"10659-11061\" or \"9817-\" or \"-10845\"),%n or to/from RGMID (e.g. \"-rmid1:3477f-a5ce52...\"): ", msgCountRemaining == Long.MAX_VALUE ? "all" : msgCountRemaining)));
					o.print(AaAnsi.n().fg(Elem.KEY));  // turn the console blue
					String answer = reader.readLine().trim().toLowerCase();
					o.print(AaAnsi.n());  // to reset() the ANSI
					if (answer.isEmpty()) {
						// all messages
					} else {
						if (answer.contains("rmid")) try {  // lol not best practices, should be two blocks
							if (answer.startsWith("rmid")) {
								if (!answer.endsWith("-")) throw new IllegalArgumentException();
								answer = answer.substring(0, answer.length()-1);
								// let's look for this particular RGMID and then start browsing after that
								browseFromRGMID = f.createReplicationGroupMessageId(answer);  // will throw if malformed
							} else if (answer.startsWith("-rmid")) {
								answer = answer.substring(1);  // crop off the leading "-"
								browseToRGMID = f.createReplicationGroupMessageId(answer);  // will throw if malformed
							}
						} catch (IllegalArgumentException | InvalidPropertiesException e) {
							o.println(AaAnsi.n().invalid("Invalid format, must be: rmid1:xxxxxx- (starting from), or -rmid1:xxxxx (until)"));
							System.exit(1);
						} else try {  // assume this is a Message Spool ID
							browseTo = Long.parseLong(answer);  // could throw if not a number
							if (browseTo < 0) {  // negative, means range: everything up to
								browseTo = Math.abs(browseTo);
							} else {
								throw new NumberFormatException();
							}
						} catch (NumberFormatException e) {
							if (answer.matches("\\d+-\\d*")) {  // either 1234-5678  or  1234-
								String[] numbers = answer.split("-");
								browseFrom = Long.parseLong(numbers[0]);
								if (numbers.length > 1) browseTo = Long.parseLong(numbers[1]);
							} else {
								o.println(AaAnsi.n().invalid("Invalid format, must be: xxxxx- (starting from), or -xxxxx (until), or xxxxx-yyyyy range"));
								System.exit(1);
							}
						}
					}
				} else {  // f:queueName, only dump first message on queue
					msgCountRemaining = 1;
					origMsgCount = 1;
				}
				//	        } catch (JCSMPErrorResponseException e) {  // something else went wrong: queue not exist, queue shutdown, etc.
				//            	o.println(AaAnsi.n().invalid(String.format("%nUh-oh!  There was a problem: %s%n%s%nQuitting!", e.getResponsePhrase(), e.toString())));
				////            	System.err.printf("%nUh-oh!  There was a problem: %s%n%s%nQuitting!", e.getResponsePhrase(), e.toString());
				//    			System.exit(1);
			} catch (JCSMPException | AccessDeniedException e) {  // something else went wrong: queue not exist, queue shutdown, etc.
				logger.error("Caught this connecting to a queue",e);
				o.println(AaAnsi.n().invalid(String.format("%nUh-oh!  There was a problem: %s: %s", e.getClass().getSimpleName(), e.getMessage())));
				o.println("Quitting! 💀");
				System.exit(1);
			}
		} else {  // either direct or temporaryQ with subs
			if (contentFilter != null) {
				o.println(AaAnsi.n().a("🔎 Client-side regex Filter detected: ").fg(Elem.STRING).a(contentFilter).reset());
			}
			// now 
			if (topics[0].startsWith("tq:")) {  // gonna use a temporary queue for Guaranteed delivery
				topics[0] = topics[0].substring(3);
				if (topics.length == 1 && topics[0].equals("")) topics = new String[0];
				else {
					for (int i=0; i<topics.length; i++) {
						if (config.noExport && !topics[i].startsWith("#noexport/")) {
							topics[i] = "#noexport/" + topics[i];
						}
					}
				}
				Arrays.sort(topics);  // sort the list b/c we need to any not subscriptions first, and "!" is 0x21, so only space would be before this
				queue = session.createTemporaryQueue();
				// Provision the temporary Queue and create a receiver
				ConsumerFlowProperties flowProps = new ConsumerFlowProperties();
				flowProps.setActiveFlowIndication(true);
				flowProps.setEndpoint(queue);
				flowProps.setAckMode(JCSMPProperties.SUPPORTED_MESSAGE_ACK_CLIENT);
				if (selector != null) {
					flowProps.setSelector(selector);
				}
				//				o.println("max msg queue: " + (Integer)session.getCapability(CapabilityType.MAX_GUARANTEED_MSG_SIZE));
				//				EndpointProperties endpointProps = new EndpointProperties(EndpointProperties.ACCESSTYPE_NONEXCLUSIVE, (Integer)session.getCapability(CapabilityType.MAX_GUARANTEED_MSG_SIZE), EndpointProperties.PERMISSION_NONE, 100);
				EndpointProperties endpointProps = new EndpointProperties(EndpointProperties.ACCESSTYPE_NONEXCLUSIVE, 9_999_999, EndpointProperties.PERMISSION_NONE, 100);
				endpointProps.setMaxMsgRedelivery(15);
				endpointProps.setDiscardBehavior(EndpointProperties.DISCARD_NOTIFY_SENDER_OFF);  // we don't want this temp queue filling up and forcing a NACK!
				if (selector != null) {
					//                	flow_prop.setSelector("pasta = 'rotini' OR pasta = 'farfalle'");
					flowProps.setSelector(selector);
				}
				o.print(AaAnsi.n().fg(Elem.PAYLOAD_TYPE).a(String.format("Creating temporary Queue.")).reset());
				if (selector != null) {
					o.println(AaAnsi.n().a(" 🔎 Selector detected: ").fg(Elem.STRING).a(selector).reset());
				} else {
					o.println();
				}
				final CountDownLatch latch = new CountDownLatch(1);
				flowQueueReceiver = session.createFlow(new PrinterHelper(), flowProps, endpointProps, new FlowEventHandler() {
					@Override
					public void handleEvent(Object source, FlowEventArgs event) {
						//						o.printf(" ### Received a Flow event: %s on thread %s %n", event, Thread.currentThread().getName());  // hide this, don't really need to show in this app
						//						o.println(queue.getName());
						//						if (event.getEvent() == FlowEvent.FLOW_ACTIVE && queue != null && !queue.isDurable()) {  // don't need to check this stuff here b/c this handler would only exist if we come down this path of the code
						if (event.getEvent() == FlowEvent.FLOW_ACTIVE) {
							boolean isNewTempQueue = false;
							//							o.println("temp queue deteted!  gonna re-add subs");
							for (String topic : topics) {
								Topic t = f.createTopic(topic);
								try {
									try {
										session.addSubscription(queue, t, JCSMPSession.WAIT_FOR_CONFIRM);  // will throw exception if already there
										if (latch.getCount() == 1) {  // first time doing this
											//											AaAnsi aa = AaAnsi.n().fg(Elem.PAYLOAD_TYPE).a(String.format("Subscribed tempQ to %stopic: '%s'", (topic.startsWith("!") ? "*NOT* " : ""), AaAnsi.n().colorizeTopic(topic)));
											AaAnsi ansi = AaAnsi.n().fg(Elem.PAYLOAD_TYPE).a(String.format("Subscribed tempQ to %stopic: '", (topic.startsWith("!") || topic.startsWith("#noexport/!") ? "*NOT* " : "")));
//											ansi.aa(AaAnsi.colorizeTopic(topic));
											ansi.fg(Elem.DESTINATION).a(topic).fg(Elem.PAYLOAD_TYPE);
											ansi.a('\'').reset();
											o.println(ansi);
										} else {
											// means that we've successfully added a sub to the tempQ on reconnect, which means new tempQ
											isNewTempQueue = true;
										}
									} catch (JCSMPErrorResponseException e) {
										if (e.getSubcodeEx() == JCSMPErrorResponseSubcodeEx.SUBSCRIPTION_ALREADY_PRESENT) {
											// this means we're reconnecting to the same tempQ that still exists
										} else {  // not what I was expecting, toss it up to the outer try
											throw e;
										}
									}
								} catch (JCSMPException | RuntimeException e) {
									o.println(AaAnsi.n().ex(e));
									System.exit(5);
									//									e.printStackTrace();
									//									isShutdown = true;
								}
							}
							latch.countDown();  // subs added, let's continue
							if (isNewTempQueue) {
								o.println(AaAnsi.n().invalid("!!! New temporary queue created: possible message loss during disconnect"));
							}
						} else {
							o.println("OTHER FLOW THING: " + event);
						}
					}
				});
				o.println(AaAnsi.n().a("Queue name: ").fg(Elem.KEY).a(flowQueueReceiver.getDestination().getName()).reset());
				if (topics.length == 0) o.println(AaAnsi.n().warn("No subscriptions added, I hope you're copying messages into this queue!").toString());
				latch.await();  // block here until the subs are added
				flowQueueReceiver.start();
			} else {
				// Regular Direct topic consumer, using async / callback to receive
				for (int i=0; i<topics.length; i++) {
					if (topics[i].startsWith("!")) {
						AaAnsi ansi = AaAnsi.n().invalid("\nNOT subscriptions are not supported for Direct messaging, use a tempQ: ");
						ansi.fg(Elem.DESTINATION).a("tq:" + topics[i]).reset();
						o.println(ansi);
						o.println("Quitting! 💀");
						System.exit(1);
					}
					if (config.noExport && !topics[i].startsWith("#noexport/")) {
						topics[i] = "#noexport/" + topics[i];
					}
				}
				directConsumer = session.getMessageConsumer((JCSMPReconnectEventHandler)null, new PrinterHelper());
				for (String topic : topics) {
					TopicProperties tp = new TopicProperties();
					if (topic.equals("#") || topic.endsWith("/#")) {  // special MQTT wildcard, zero-or-more
						//						topic = topic.substring(0, topic.length()-1) + (char)3;
						tp.setName(topic.substring(0, topic.length()-1) + (char)3);
					} else {
						tp.setName(topic);
					}
					tp.setRxAllDeliverToOne(true);  // ensure DTO-override / DA is enabled for this sub
					Topic t = f.createTopic(tp);
					session.addSubscription(t, true);  // true == wait for confirm
//					o.println(AaAnsi.n().fg(Elem.PAYLOAD_TYPE).a(String.format("Subscribed to Direct topic: '%s'", topic)).reset());
					AaAnsi ansi = AaAnsi.n().fg(Elem.PAYLOAD_TYPE).a("Subscribed to Direct topic: '");
//					ansi.aa(AaAnsi.colorizeTopic(topic));
					ansi.fg(Elem.DESTINATION).a(topic).fg(Elem.PAYLOAD_TYPE);
					ansi.a('\'').reset();
					o.println(ansi);
				}
				directConsumer.start();
			}
		}
		// DONE!!!!   READY TO ROCK!
		o.println();
		o.println("Starting. Press Ctrl-C to quit.");
		if (config.isLastNMessagesEnabled()) {
			ThinkingAnsiHelper.tick2(ThinkingAnsiHelper.makeStringGathered(null, 0, 0, 0, config.getLastNMessagesCapacity()));
			//			ThinkingAnsiHelper.tick(String.format("%d messages gathered, # messages received = ", config.getLastNMessagesSize()));
//		} else if (config.) {
		}
		final Thread shutdownThread = new Thread(() -> {
//			config.stop();
			ThinkingAnsiHelper.filteringOff();
			o.print(AaAnsi.n());
			o.println("\nShutdown hook triggered, quitting...");
			config.isShutdown = true;
			if (config.isConnected) {  // if we're disconnected, skip this because these will block/lock waiting on the reconnect to happen
				if (flowQueueReceiver != null) flowQueueReceiver.close();  // will remove the temp queue if required
				if (directConsumer != null) directConsumer.close();
			}
			try {
				Thread.sleep(200);
				session.closeSession();
				Thread.sleep(300);
			} catch (InterruptedException e) {  // ignore, we're quitting anyway
			}
			if (config.isLastNMessagesEnabled()) {  // got some messages to dump!
				for (MessageObject msg : config.getLastNMessages()) {
					o.print(msg.printMessage());
				}
				o.println();
			}
			logger.info("### PrettyDump finishing!");
			o.println("Goodbye! 👋🏼");
			AnsiConsole.systemUninstall();
			})
		;
		shutdownThread.setName("Shutdown Hook thread");
		//        shutdownThread.setDaemon(true);  // doesn't work, still gets deadlocked if disconnected and trying to reconnect
		// https://stackoverflow.com/questions/58972594/difference-in-adding-a-daemon-vs-non-daemon-thread-in-a-java-shutdown-hook
		Runtime.getRuntime().addShutdownHook(shutdownThread);

		if (browser != null) {  // ok, so we're browsing... can't use async msg receive callback, have to poll the queue
			PrinterHelper printer = new PrinterHelper();
			// hasMore() is useless! from JavaDocs: Returns true if there is at least one message available in the Browser's local message buffer. Note: If this method returns false, it does not mean that the queue is empty; subsequent calls to Browser.hasMore() or Browser.getNext() might return true and a message respectively.
			BytesXMLMessage nextMsg;
			try {
				while (!config.isShutdown && msgCountRemaining > 0) {
					handleKeyboardInput(reader);
					nextMsg = browser.getNext(-1);  // don't wait, return immediately
					if (nextMsg == null) {
						Thread.sleep(50);
						continue;
					}
					if (browseFromRGMID != null || browseToRGMID != null) {  // looking for a RGMID
						ReplicationGroupMessageId rgmid = nextMsg.getReplicationGroupMessageId();
						assert rgmid != null;
						if (browseFromRGMID == null) {  // everything up to a message
							assert browseToRGMID != null;
							printer.onReceive(nextMsg);  // always print it
							try {
								if (rgmid.compare(browseToRGMID) == 0) {  // match!  time to stop
									o.println(AaAnsi.n().fg(Elem.PAYLOAD_TYPE).a(String.format("%nMessage with RGMID '%s' received. Done.", rgmid)).reset());
									break;  // done!
								} else if (rgmid.compare(browseToRGMID) > 0) {  // too big!  time to stop
									o.println(AaAnsi.n().fg(Elem.PAYLOAD_TYPE).a(String.format("%nMessage with RGMID '%d' received, greater than than browse range '%d'. Done.", rgmid, browseToRGMID)).reset());
									break;  // done!
								}
							} catch (JCSMPNotComparableException e) {  // can't compare, so ignore and keep going
							}
						} else {
							// still looking for a specific RGMID
							try {
								if (rgmid.compare(browseFromRGMID) == 0) {  // match!  time to start
									printer.onReceive(nextMsg);
									browseFromRGMID = null;  // blank it out and just print everything from now on
								} else {
									config.incMessageCount();
									config.incFilteredCount();
									//									ThinkingAnsiHelper.tick("RGMID outside of range, skipping msg #");
									if (config.isLastNMessagesEnabled()) {  // gathering
										ThinkingAnsiHelper.tick2(ThinkingAnsiHelper.makeStringGathered("RGMID outside of range.",
												config.getMessageCount(), config.getFilteredCount(), 0, config.getLastNMessagesCapacity()));
									} else {
										ThinkingAnsiHelper.tick2(ThinkingAnsiHelper.makeStringPrinted("RGMID outside of range.",
												config.getMessageCount(), config.getFilteredCount(), 0));
									}
								}
							} catch (JCSMPNotComparableException e) {  // can't compare, so ignore and keep going
								config.incMessageCount();
								config.incFilteredCount();
								//								ThinkingAnsiHelper.tick("RGMID outside of range, skipping msg #");
								if (config.isLastNMessagesEnabled()) {  // gathering
									ThinkingAnsiHelper.tick2(ThinkingAnsiHelper.makeStringGathered("RGMID outside of range.",
											config.getMessageCount(), config.getFilteredCount(), 0, config.getLastNMessagesCapacity()));
								} else {
									ThinkingAnsiHelper.tick2(ThinkingAnsiHelper.makeStringPrinted("RGMID outside of range.",
											config.getMessageCount(), config.getFilteredCount(), 0));
								}
							}
						}
					} else {  // looking for a Message Spool ID, or no looking
						try {
							long msgId = nextMsg.getMessageIdLong();  // deprecated, shouldn't be using this, but oh well!
							if (msgId <= 0) {  // should be impossible??
								o.println(AaAnsi.n().invalid("Message received with no Message ID set!"));
								printer.onReceive(nextMsg);
								break;
							}
							if (msgId == browseTo) {  // match!  time to stop
								printer.onReceive(nextMsg);
								break;
							} else if (msgId > browseTo) {  // too far, time to stop
								printer.onReceive(nextMsg);
								o.println(AaAnsi.n().fg(Elem.PAYLOAD_TYPE).a(String.format("%nMessage with ID '%d' received, greater than than browse range '%d'. Done.", msgId, browseTo)).reset());
								break;  // done!
							} else if (msgId < browseFrom) {  // haven't got there yet
								config.incMessageCount();
								config.incFilteredCount();
								//								ThinkingAnsiHelper.tick("MsgSpoolId outside of range, skipping msg #");
								if (config.isLastNMessagesEnabled()) {  // gathering
									ThinkingAnsiHelper.tick2(ThinkingAnsiHelper.makeStringGathered("MsgSpoolId outside of range.",
											config.getMessageCount(), config.getFilteredCount(), 0, config.getLastNMessagesCapacity()));
								} else {
									ThinkingAnsiHelper.tick2(ThinkingAnsiHelper.makeStringPrinted("MsgSpoolId outside of range.",
											config.getMessageCount(), config.getFilteredCount(), 0));
								}
							} else {  // normal mode
								printer.onReceive(nextMsg);
							}
						} catch (Exception e) {
							o.println(AaAnsi.n().invalid("Exception on message trying to get Message ID!").reset());
							printer.onReceive(nextMsg);
							break;
						}
					}
				}  // end of while loop
			} catch (JCSMPException | AccessDeniedException e) {  // something else went wrong: queue permissions changed? , etc.
				o.println();
				o.println("I'm here inside catch block");
				if (!config.isShutdown) {
					o.println(AaAnsi.n().invalid(String.format("%nUh-oh!  There was a problem: %s: %s", e.getClass().getSimpleName(), e.getMessage())));
					o.println("Quitting! 💀");
					System.exit(1);
				}
			} finally {
				o.println(AaAnsi.n());
				o.println("Browsing finished!");
				browser.close();
			}
		} else {  // async receive, either Direct sub or from a queue, so just wait here until Ctrl+C pressed
			//        	BufferedReader r = new BufferedReader(new InputStreamReader(System.in));
			while (!config.isShutdown) {
				Thread.sleep(50);
				// blocking receive test code
				//				BytesXMLMessage msg;
				//				if ((msg = directConsumer.receive(0)) != null) {
				//					payloadHelper.dealWithMessage(msg);
				//				}
				handleKeyboardInput(reader);
			}
		}
		config.isShutdown = true;
		o.print(AaAnsi.n());
		o.println("Main thread exiting.");
	}  // end of main()

	private void handleKeyboardInput(BufferedReader reader) throws IOException {
		String userInput = null;
		if (System.in.available() > 0) {
			userInput = reader.readLine();
		}
		if (userInput != null && !userInput.isEmpty()) {
			try {
				userInput = userInput.toLowerCase();
				int highlight = Integer.parseInt(userInput);
				if (highlight >= 0 && highlight < 125) {
					config.setHighlightedTopicLevel(highlight - 1);  // so 0 -> -1 (highlight off), 1 -> 0 (level 1), etc.
				}
			} catch (NumberFormatException e) {
				if ("+".equals(userInput)) {
					config.setAutoSpaceTopicLevels(true);
				} else if ("-".equals(userInput)) {
					config.setAutoSpaceTopicLevels(false);
				} else if ("t".equals(userInput)) {
					config.toggleAutoTrimPayload();
				} else if ("q".equals(userInput)) {
					config.isShutdown = true;  // quit
				} else if (userInput.charAt(0) == 'c' && userInput.length() == 2) {
					// assume we're trying to switch colours
					ColorMode temp = null;
					switch (userInput.charAt(1)) {
					case 'v': temp = ColorMode.VIVID; break;
					case 's': temp = ColorMode.STANDARD; break;
					case 'm': temp = ColorMode.MINIMAL; break;
					case 'l': temp = ColorMode.LIGHT; break;
					case 'o': temp = ColorMode.OFF; break;
					case 'x': temp = ColorMode.MATRIX; break;
					}
					if (temp != null) {
						Elem.updateColors(temp);
						AaAnsi.MODE = temp;
					}
				} else if ("ts".equals(userInput)) {
					config.includeTimestamp = !config.includeTimestamp;
				}
			}
		}
	}

	static Charset CHARSET;
	static CharsetDecoder DECODER;
	static {
		if (System.getenv("PRETTY_CHARSET") != null) {
			logger.info("Detected environment variable PRETTY_CHARSET: " + System.getenv("PRETTY_CHARSET"));
			try {
				CHARSET = Charset.forName(System.getenv("PRETTY_CHARSET"));  // this should throw if it doesn't find it
				DECODER = CHARSET.newDecoder().onMalformedInput(CodingErrorAction.REPLACE).onUnmappableCharacter(CodingErrorAction.REPLACE);
			} catch (Exception e) {
				o.println(AaAnsi.n().invalid("Invalid charset specified! PRETTY_CHARSET=" + System.getenv("PRETTY_CHARSET")).reset());
				o.println("Use one of: " + Charset.availableCharsets().keySet());
				o.println("Or clear environment variable 'PRETTY_CHARSET' to use default UTF-8");
				o.println("Quitting! 💀");
				System.exit(1);
			}
		} else {
			logger.info("Environment variable PRETTY_CHARSET not detected, using default UTF-8");
			CHARSET = StandardCharsets.UTF_8;
			DECODER = StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPLACE).onUnmappableCharacter(CodingErrorAction.REPLACE);
		}
	}

	// Helper class, for printing message to the console ///////////////////////////////////////

	private class PrinterHelper implements XMLMessageListener {

		@Override
		public void onReceive(BytesXMLMessage message) {
			if (config.isShutdown) return;  // we're done, don't do anything with this
			ph.dealWithMessage(message);
			if (!ThinkingAnsiHelper.isFilteringOn()) msgCountRemaining--;  // payload helper would turn it off
			// if we're not browsing, and it's not a Direct message (doesn't matter if we ACK a Direct message anyhow)
			// NOTE!  You can still ACK a browsed message!!
			if (browser == null && message.getDeliveryMode() != DeliveryMode.DIRECT) message.ackMessage();  // if it's a queue
			if (msgCountRemaining == 0) {
				o.println("\n" + AaAnsi.n().fg(Elem.PAYLOAD_TYPE).a(origMsgCount + " messages received. Quitting.").reset());
//				config.stop();
				config.isShutdown = true;
//				if (flowQueueReceiver != null) flowQueueReceiver.close();
//				if (directConsumer != null) directConsumer.close();
			}
		}

		@Override
		public void onException(JCSMPException e) {  // uh oh!
			o.println(AaAnsi.n().ex(" ### MessageListener's onException()", e).a(" - check ~/.pretty/pretty.log for details. "));
			logger.warn("Caught in my onExecption() callback", e);
			if (e instanceof JCSMPTransportException) {  // all reconnect attempts failed (impossible now since we're at -1)
				config.isShutdown = true;  // let's quit
			}
		}
	}
}
