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
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.fusesource.jansi.AnsiConsole;

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
import com.solacesystems.jcsmp.JCSMPChannelProperties;
import com.solacesystems.jcsmp.JCSMPErrorResponseException;
import com.solacesystems.jcsmp.JCSMPErrorResponseSubcodeEx;
import com.solacesystems.jcsmp.JCSMPException;
import com.solacesystems.jcsmp.JCSMPFactory;
import com.solacesystems.jcsmp.JCSMPGlobalProperties;
import com.solacesystems.jcsmp.JCSMPProperties;
import com.solacesystems.jcsmp.JCSMPReconnectEventHandler;
import com.solacesystems.jcsmp.JCSMPSession;
import com.solacesystems.jcsmp.JCSMPTransportException;
import com.solacesystems.jcsmp.Queue;
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
	private static final JCSMPFactory f = JCSMPFactory.onlyInstance();

	private static PayloadHelper payloadHelper;

	private static volatile boolean isShutdown = false;          // are we done yet?
	private static volatile boolean isConnected = false;
	private static volatile boolean isFlowActive = false;
	private static String[] topics = new String[] { "#noexport/>" };  // default starting topic
	private static Queue queue = null;  // might be temp/non-durable, or regular
	private static Browser browser = null;  // in case we need it, can't do async, is a blocking/looping pull
	private static long origMsgCount = Long.MAX_VALUE;
	private static long msgCountRemaining = Long.MAX_VALUE;
	private static String selector = null;
	private static String contentFilter = null;
	//    private static Queue tempQueue = null;
	private static long browseFrom = -1;
	private static String browseFromRGMID = null;
	private static long browseTo = Long.MAX_VALUE;

	private static FlowReceiver flowQueueReceiver = null;  // for queues and tempQueue
    private static XMLMessageConsumer directConsumer = null;  // for Direct

	@SuppressWarnings("deprecation")  // this is for our use of Message ID for the browser
	public static void main(String... args) throws JCSMPException, IOException, InterruptedException {
		for (String arg : args) {
			if (arg.equals("-h") || arg.startsWith("--h") || arg.equals("-?") || arg.startsWith("--?") || arg.contains("-help")) {
				System.out.printf("Usage: %s [host] [vpn] [user] [pw] [topics|[qbf]:queueName|tq:topics] [indent] [count]%n", APP_NAME.toLowerCase());
				System.out.printf("   or: %s <topics|[qbf]:queueName|tq:topics> [indent] [count]  for \"shortcut\" mode%n%n", APP_NAME.toLowerCase());
				//                System.out.println(" - If using TLS, remember \"tcps://\" before host; or \"ws://\" or \"wss://\" for WebSocket");
				System.out.println(" - Default protocol \"tcp://\"; for TLS use \"tcps://\"; or \"ws://\" or \"wss://\" for WebSocket");
				System.out.println(" - Default parameters will be: localhost:55555 default foo bar \"#noexport/>\" 2");
				//                System.out.println("     (FYI: if client-username 'default' is enabled in VPN, you can use any username)");
				System.out.println(" - Subscribing options (param 5, or shortcut mode param 1), one of:");
				System.out.println("    - Comma-separated list of Direct topic subscriptions");
				System.out.println("       - Strongly consider prefixing with '#noexport/' if using DMR or MNR");
				System.out.println("    - q:queueName to consume from queue");
				System.out.println("    - b:queueName to browse a queue (all messages, or range of messages by ID)");
				//                System.out.println("       - Can browse all messages, or specific messages by ID");
				System.out.println("    - f:queueName to browse/dump only first oldest message on a queue");
				System.out.println("    - tq:topics   to provision a tempQ with topics subscribed (can use NOT '!' topics)");
				System.out.println(" - Optional indent: integer, default==2 spaces; specifying 0 compresses payload formatting");
				System.out.println("    - No payload mode: use indent '00' to only show headers and props, or '000' for compressed");
				System.out.println("    - One-line mode: use negative indent value (trim topic length) for topic & payload only");
				System.out.println("       - Or use -1 for auto column width adjustment, or -2 for two-line mode");
				System.out.println("       - Use negative zero -0 for topic only, no payload");
				System.out.println(" - Optional count: stop after receiving 'count' number of messages");
				System.out.println(" - Shortcut mode: first argument contains '>', '*', or starts '[qbf]:', assume default broker");
				System.out.println("    - e.g. prettydump 'logs/>' -1  ~or~  prettydump q:q1  ~or~  prettydump b:dmq -0");
				System.out.println("    - Or if first argument parses as integer, select as indent, rest default options");

				//                System.out.println("    - e.g. bin/prettydump \"logs/>\" -1   ~or~   bin/prettydump q:q1");
				//                System.out.println("    - Or queues as well: e.g. ./bin/prettydump q:q1   ~or~   ./bin/prettydump b:dmq -1");
				//                System.out.println("    - If zero parameters, assume localhost default broker and subscribe to \"#noexport/>\"");
				System.out.println(" - Runtime options:");
				System.out.println("    - Press \"t[ENTER]\" to toggle payload trim to terminal width in one-line mode");
				System.out.println("    - Press \"+[ENTER]\" to enable topic level spacing/alignment (\"-[ENTER]\" to revert)");
				System.out.println("    - Press \"[1-9][ENTER]\" to highlight a particular topic level (\"0[ENTER]\" to revert)");
				System.out.println("Environment variable options:");
				System.out.println(" - Default charset is UTF-8. Override by setting: export PRETTY_CHARSET=ISO-8859-1");
//				System.out.println("    - e.g. export PRETTY_CHARSET=ISO-8859-1  (or \"set\" on Windows)");
				System.out.println(" - Multiple colour schemes supported. Override by setting: export PRETTY_COLORS=whatever");
				System.out.println("    - Choose: \"standard\" (default), \"vivid\", \"light\", \"minimal\", \"matrix\", \"off\"");
				System.out.println(" - Selector for Queue consume and browse: export PRETTY_SELECTOR=\"what like 'ever%'\"");
				System.out.println(" - Client-side filtering on any received message: export PRETTY_FILTER=\"ID:123abc\"");
				System.out.println("    - Or use --selector=\"what like 'ever%'\" and --filter=\"ID:123abc\" in command line args");
				System.out.println("SdkPerf Wrap mode: use any SdkPerf as usual, pipe command to \" | prettydump wrap\" to prettify");
//				System.out.println(" - Note: add the 'bin' directory to your path to make it easier");
				System.out.println();
				//                System.out.println("v0.1.0, 2024/01/09");
				//                System.out.println();
				System.exit(0);
			}
		}
		if (args.length == 1 && args[0].toLowerCase().equals("wrap")) {
			PrettyWrap.main(new String[0]);
			System.exit(0);
		}
		logger.info("### Starting PrettyDump!");
		payloadHelper = new PayloadHelper(CHARSET);
		if (System.getenv("PRETTY_SELECTOR") != null && !System.getenv("PRETTY_SELECTOR").isEmpty()) {
			selector = System.getenv("PRETTY_SELECTOR");
		}
		if (System.getenv("PRETTY_FILTER") != null && !System.getenv("PRETTY_FILTER").isEmpty()) {
			contentFilter = System.getenv("PRETTY_FILTER");
		}

		// special command-line argument handling
		List<String> args2 = new ArrayList<>();
		for (String arg : args) {
			if (arg.startsWith("--selector=")) {
				selector = arg.substring("--selector=".length());
			} else if (arg.startsWith("--filter=")) {
				contentFilter = arg.substring("--filter=".length());
			} else {
				args2.add(arg);  // add argument normally
			}
		}
		args = args2.toArray(new String[0]);
		if (selector != null && !selector.isEmpty()) {
			if (selector.length() > 2000) {
				System.out.println(AaAnsi.n().invalid("Selector length greater than 2000 character limit!"));
				System.out.println("Quitting! 💀");
				System.exit(1);
			}
		}
		if (contentFilter != null && !contentFilter.isEmpty()) {
			// compiling might throw an exception
			Pattern p = Pattern.compile(contentFilter, Pattern.MULTILINE | Pattern.DOTALL);//| Pattern.CASE_INSENSITIVE);
			payloadHelper.filterRegexPattern = p;
//			System.out.println(AaAnsi.n().fg(Elem.PAYLOAD_TYPE).a(String.format("🔎 Filter detected: \"%s\"", contentFilter)));
		}
//		System.out.println(Arrays.toString(args));
//		System.out.printf("selector='%s'%n", selector);
//		System.out.printf("filter='%s'%n", contentFilter);
		
		String host = "localhost";
		boolean shortcut = false;
		// new shortcut MODE... if first arg looks like topics, assume topic wildcard, and assume localhost default connectivity for rest
		if (args.length > 0) {
			if (args[0].contains(">") || args[0].contains("*") || args[0].startsWith("tq:")) {  // shortcut MODE
				shortcut = true;
				topics = args[0].split("\\s*,\\s*");  // split on commas, remove any whitespace around them
			} else if (args[0].matches("^[qbf]:.+")) {  // either browse, queue consume, or browse first to localhost
				shortcut = true;
				topics = new String[] { args[0] };  // just the one, queue name will get parsed out later
			} else if (args.length == 1) {  // exactly one arg
				// see if it's an integer, we'll use for indent
				try {
					payloadHelper.dealWithIndentParam(args[0]);
					// if nothing thrown, then it's a valid indent, so assume shortcut mode
					shortcut = true;
				} catch (NumberFormatException e) {
					host = args[0];
				}
			} else {
				host = args[0];
			}
		}
		String vpn = "default";
		if (args.length > 1 && !shortcut) vpn = args[1];
		String username = "foo";
		if (args.length > 2 && !shortcut) username = args[2];
		String password = "bar";
		if (args.length > 3 && !shortcut) password = args[3];
		if (args.length > 4 && !shortcut) {
			if (args[4].matches("^[qbf]:.+")) {
				topics = new String[] { args[4] };  // just the one, queue name will get parsed out later
			} else {
				topics = args[4].split("\\s*,\\s*");  // split on commas, remove any whitespace around them
			}
		}
		if ((args.length > 5 && !shortcut) || (shortcut && args.length > 1)) {
			String indentStr = args[shortcut ? 1 : 5];  // grab the correct command-line argument
			try {
				payloadHelper.dealWithIndentParam(indentStr);
			} catch (NumberFormatException e) {
				System.out.println(AaAnsi.n().invalid(String.format("Invalid value for indent: '%s', using default %d instead.%n", indentStr, payloadHelper.getCurrentIndent())));
			}
		}
		if ((args.length > 6 && !shortcut) || (shortcut && args.length > 2)) {
			try {
				long count = Long.parseLong(args[shortcut ? 2 : 6]);
				if (count > 0) {
					msgCountRemaining = count;
					origMsgCount = count;
				}
			} catch (NumberFormatException e) {
				// nothing changes, keep the same. Ignore?  Or throw issue?
			}
		}
		AnsiConsole.systemInstall();
		//		AaAnsi a = AaAnsi.n().a("hello there ").faintOn().a(" now this is faint").faintOff().a(" and now not.");
		//		System.out.println(a);
		if (AnsiConsole.getTerminalWidth() >= 80) System.out.print(Banner.printBanner(Which.DUMP));
		else System.out.println();
		System.out.println(APP_NAME + " initializing...");
		payloadHelper.protobufCallbacks = ProtoBufUtils.loadProtobufDefinitions();
		BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
		// now let's get on with it!
		final JCSMPProperties properties = new JCSMPProperties();
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
		final JCSMPSession session;
		session = f.createSession(properties, null, new SessionEventHandler() {
			@Override
			public void handleEvent(SessionEventArgs event) {  // could be reconnecting, connection lost, etc.
				//        		System.out.println(" > " + event.getEvent());
				if (event.getEvent() == SessionEvent.RECONNECTING) {
					if (isConnected) {  // first time
						isConnected = false;
//						System.out.print(AaAnsi.n().invalid("Connection lost!") + "\n > RECONNECTING...");
						System.out.println(AaAnsi.n().warn("TCP Connection lost!"));
						System.out.print(" > SESSION RECONNECTING...");
					} else {
						System.out.print(".");
						//            			System.out.print(". " + Thread.currentThread().getName() + " (" + Thread.currentThread().isDaemon() + ") ");
						//            			Set<Thread> threadSet = Thread.getAllStackTraces().keySet();
						//            			for (Thread t : threadSet) {
						//            				if (!t.isDaemon()) {
						//            					System.out.printf("%s %s%n", t.getName(), (t.getStackTrace().length > 0 ? Arrays.toString(t.getStackTrace()) : "n/a"));
						//            					System.out.printf("%s %s%n", t.getName(), t.getStackTrace()[0].toString());
						//            				}
						//            			}
					}
				} else if (event.getEvent() == SessionEvent.RECONNECTED) {
					System.out.println("\n > SESSION RECONNECTED!");
					isConnected = true;
				} else {
					if (!isConnected) System.out.println();  // add a blank line
					System.out.println(" > " + event.getEvent() + ": " + event);
				}
			}
		});
		session.connect();  // connect to the broker... could throw JCSMPException, so best practice would be to try-catch here..!
		isConnected = true;
		session.setProperty(JCSMPProperties.CLIENT_NAME, "PrettyDump_" + session.getProperty(JCSMPProperties.CLIENT_NAME));
		System.out.printf("%s connected to '%s' VPN on broker '%s'.%n%n", APP_NAME, session.getProperty(JCSMPProperties.VPN_NAME_IN_USE), session.getProperty(JCSMPProperties.HOST));
		
//		for (CapabilityType cap : CapabilityType.values()) {
//			System.out.println(cap + ": " + session.getCapability(cap));
//		}
		
		if (contentFilter != null) {
//			System.out.println(AaAnsi.n().fg(Elem.PAYLOAD_TYPE).a(String.format("🔎 Client-side Filter detected: \"%s\"", contentFilter)).reset());
			System.out.println(AaAnsi.n().a("🔎 Client-side Filter detected: ").aStyledString(contentFilter).reset());
		}

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
			System.out.printf("Attempting to bind to queue '%s' on the broker... ", queueName);
			flowQueueReceiver = null;
			final CountDownLatch latch = new CountDownLatch(1);
			try {
				//            	if ("1".equals("1")) throw new JCSMPException("blajsdflklskfjd");
				// see bottom of file for QueueFlowListener class, which receives the messages from the queue
				flowQueueReceiver = session.createFlow(new PrinterHelper(), flowProps, null, new FlowEventHandler() {
					@Override
					public void handleEvent(Object source, FlowEventArgs event) {
						FlowEvent fe = event.getEvent();
						// Flow events are usually: active, reconnecting (i.e. unbound), reconnected, active
						if (fe == FlowEvent.FLOW_RECONNECTING && isConnected) {
							// flow active here?
							System.out.println(AaAnsi.n().warn("'"+queueName+"' flow closed! Queue egress probably shutdown at the broker."));
							System.out.println(" > FLOW RECONNECTING...");
						} else if (fe == FlowEvent.FLOW_RECONNECTED) {
							// flow inactive here?
							System.out.println(" > FLOW RECONNECTED!");
						} else if (fe == FlowEvent.FLOW_ACTIVE) {
							isFlowActive = true;
							if (latch.getCount() == 1) {  // first time here, so skip this notification
								
							} else {
								if (!isConnected) System.out.println();  // connection coming back up
								System.out.println(" > " + fe);
							}
						} else if (fe == FlowEvent.FLOW_INACTIVE) {
							isFlowActive = false;
						} else if (!isConnected) {
							// ignore
						} else {
							System.out.println(" > " + fe);
						}
//						System.out.printf(" ### Received a Flow event: %s%n", event);  // hide this, don't really need to show in this app
					}
				});
				System.out.println("success!");
				latch.countDown();
				System.out.println();
				// double-check
				if (selector != null) {
					System.out.println(AaAnsi.n().a("🔎 Selector detected: ").aStyledString(selector).reset());
					System.out.print(AaAnsi.n().fg(Elem.PAYLOAD_TYPE).a(String.format("Will consume/ACK %s messages on queue '%s' that match Selector. Use browse 'b:' command-line option otherwise.%nAre you sure? [y|yes]: ", msgCountRemaining == Long.MAX_VALUE ? "all" : msgCountRemaining, queueName)));
				} else {  // no selectors, consume all
					System.out.print(AaAnsi.n().fg(Elem.PAYLOAD_TYPE).a(String.format("Will consume/ACK %s messages on queue '%s'. Use browse 'b:' command-line option otherwise.%nAre you sure? [y|yes]: ", msgCountRemaining == Long.MAX_VALUE ? "all" : msgCountRemaining, queueName)));
				}
				String answer = reader.readLine().trim().toLowerCase();
				System.out.print(AaAnsi.n());  // to reset() the ANSI
				if (!"y".equals(answer) && !"yes".equals(answer)) {
					System.out.println("\nExiting. 👎🏼");
					System.exit(0);
				}
				//                reader.close();
				// tell the broker to start sending messages on this queue receiver
				flowQueueReceiver.start();
				//            } catch (OperationNotSupportedException e) {  // not allowed to do this
				//            	System.out.println(AaAnsi.n().invalid(String.format("%nUh-oh!  There was a problem: %s%n%s%nQuitting!", e.getMessage(), e.toString())));
				//    			System.exit(1);
				//            } catch (JCSMPErrorResponseException e) {  // something else went wrong: queue not exist, queue shutdown, etc.
				//            	System.out.println(AaAnsi.n().invalid(String.format("%nUh-oh!  There was a problem: %s: %s", e.getClass().getSimpleName(), e.getMessage())));
				////            	System.out.println(AaAnsi.n().invalid(String.format("%nUh-oh!  There was a problem: %s%n%s%nQuitting!", e.getResponsePhrase(), e.toString())));
				//    			System.exit(1);
			} catch (JCSMPException | AccessDeniedException e) {  // something else went wrong: queue not exist, queue shutdown, etc.
				System.out.println(AaAnsi.n().invalid(String.format("%nUh-oh!  There was a problem: %s: %s", e.getClass().getSimpleName(), e.getMessage())));
				//            	if (e.getCause() instanceof JCSMPErrorResponseException) {
				//                	System.out.println(AaAnsi.n().invalid(((JCSMPErrorResponseException)e.getCause()).getMessage()));
				//            	}
				System.out.println("Quitting! 💀");
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
			System.out.printf("Attempting to browse queue '%s' on the broker... ", queueName);
			try {
				browser = session.createBrowser(bp, new FlowEventHandler() {
					@Override
					public void handleEvent(Object source, FlowEventArgs event) {
						// Flow events are usually: active, reconnecting (i.e. unbound), reconnected, active
						if (event.getEvent() == FlowEvent.FLOW_RECONNECTING && isConnected) {
							System.out.println(AaAnsi.n().warn("'"+queueName+"' flow closed! Queue egress probably shutdown at the broker."));
							System.out.print(" > FLOW RECONNECTING...");
						} else if (event.getEvent() == FlowEvent.FLOW_RECONNECTED) {
							System.out.println("\n > FLOW RECONNECTED!");
						} // else ignore!
//						System.out.printf(" ### Received a Flow event: %s%n", event);  // hide this, don't really need to show in this app
					}
				});
				System.out.println("success!");
				System.out.println();
				if (selector != null) {
//					System.out.println(AaAnsi.n().fg(Elem.PAYLOAD_TYPE).a(String.format("🔎 Selector detected: \"%s\"", selector)).reset());
					System.out.println(AaAnsi.n().a("🔎 Selector detected: ").aStyledString(selector).reset());
				}
				if (topics[0].startsWith("b:")) {  // regular browse, prompt for msg IDs
					if (contentFilter == null) {
						System.out.print(AaAnsi.n().fg(Elem.PAYLOAD_TYPE).a(String.format("Browse %s messages -> press [ENTER],%n or enter specific Message Spool ID, or to/from range of IDs%n (e.g. \"106259-110261\" or \"98517-\" or \"-103345\"),%n or start at RGMID (e.g. \"rmid1:3477f-a5ce52...\"): ", msgCountRemaining == Long.MAX_VALUE ? "all" : msgCountRemaining)));
					} else {  // don't allow single MsgID
						System.out.print(AaAnsi.n().fg(Elem.PAYLOAD_TYPE).a(String.format("Browse %s messages -> press [ENTER],%n or range of Spool IDs (e.g. \"259-261\" or \"9517-\" or \"-345\"),%n or start at RGMID (e.g. \"rmid1:3477f-a5ce52...\": ", msgCountRemaining == Long.MAX_VALUE ? "all" : msgCountRemaining)));
					}
					String answer = reader.readLine().trim().toLowerCase();
					System.out.print(AaAnsi.n());  // to reset() the ANSI
					if (answer.isEmpty()) {
						// all messages
					} else {
						if (answer.startsWith("rmid")) {
							// let's look for this particular RGMID and then start browsing after that
//							JCSMPFactory.onlyInstance().createReplicationGroupMessageId(answer);
							browseFromRGMID = answer;
//							browseTo =  // leave alone, maximum browse
						} else try {
							// assume only one integer (message ID) inputed
							browseFrom = Long.parseLong(answer);
							browseTo = Long.parseLong(answer);
							if (browseFrom < 0) {  // negative, means range: everything up to
								browseFrom = 0;
								browseTo = Math.abs(browseTo);
							} else if (contentFilter != null) {  // assume a single value is "browse until"
								browseFrom = 0;
							}
						} catch (NumberFormatException e) {
							if (answer.matches("\\d+-\\d*")) {  // either 1234-5678  or  1234-
								String[] numbers = answer.split("-");
								browseFrom = Long.parseLong(numbers[0]);
								if (numbers.length > 1) browseTo = Long.parseLong(numbers[1]);
							} else {
								System.out.println(AaAnsi.n().invalid("Invalid format, must be either integer, or range xxxxx-yyyyyy"));
								System.exit(1);
							}
						}
					}
				} else {  // f:queueName, only dump first message on queue
					msgCountRemaining = 1;
					origMsgCount = 1;
				}
				//	        } catch (JCSMPErrorResponseException e) {  // something else went wrong: queue not exist, queue shutdown, etc.
				//            	System.out.println(AaAnsi.n().invalid(String.format("%nUh-oh!  There was a problem: %s%n%s%nQuitting!", e.getResponsePhrase(), e.toString())));
				////            	System.err.printf("%nUh-oh!  There was a problem: %s%n%s%nQuitting!", e.getResponsePhrase(), e.toString());
				//    			System.exit(1);
			} catch (JCSMPException | AccessDeniedException e) {  // something else went wrong: queue not exist, queue shutdown, etc.
				logger.error("Caught this connecting to a queue",e);
				System.out.println(AaAnsi.n().invalid(String.format("%nUh-oh!  There was a problem: %s: %s", e.getClass().getSimpleName(), e.getMessage())));
				System.out.println("Quitting! 💀");
				System.exit(1);
			}
		} else {  // either direct or temporaryQ with subs
			// now 
			if (topics[0].startsWith("tq:")) {  // gonna use a temporary queue for Guaranteed delivery
				topics[0] = topics[0].substring(3);
				queue = session.createTemporaryQueue();
				// Provision the temporary Queue and create a receiver
				ConsumerFlowProperties flowProps = new ConsumerFlowProperties();
				flowProps.setActiveFlowIndication(true);
				flowProps.setEndpoint(queue);
				flowProps.setAckMode(JCSMPProperties.SUPPORTED_MESSAGE_ACK_AUTO);  // not best practice, but good enough for this app
				if (selector != null) {
					flowProps.setSelector(selector);
				}
//				System.out.println("max msg queue: " + (Integer)session.getCapability(CapabilityType.MAX_GUARANTEED_MSG_SIZE));
				EndpointProperties endpointProps = new EndpointProperties(EndpointProperties.ACCESSTYPE_NONEXCLUSIVE, (Integer)session.getCapability(CapabilityType.MAX_GUARANTEED_MSG_SIZE), EndpointProperties.PERMISSION_NONE, 100);
				endpointProps.setMaxMsgRedelivery(15);
				endpointProps.setDiscardBehavior(EndpointProperties.DISCARD_NOTIFY_SENDER_OFF);  // we don't want this temp queue filling up and forcing a NACK!
				if (selector != null) {
					//                	flow_prop.setSelector("pasta = 'rotini' OR pasta = 'farfalle'");
					flowProps.setSelector(selector);
				}
				System.out.print(AaAnsi.n().fg(Elem.PAYLOAD_TYPE).a(String.format("Creating temporary Queue.")).reset());
				if (selector != null) {
					System.out.println(AaAnsi.n().a(" 🔎 Selector detected: ").aStyledString(selector).reset());
				} else {
					System.out.println();
				}
				final CountDownLatch latch = new CountDownLatch(1);
				flowQueueReceiver = session.createFlow(new PrinterHelper(), flowProps, endpointProps, new FlowEventHandler() {
					@Override
					public void handleEvent(Object source, FlowEventArgs event) {
//						System.out.printf(" ### Received a Flow event: %s on thread %s %n", event, Thread.currentThread().getName());  // hide this, don't really need to show in this app
//						System.out.println(queue.getName());
//						if (event.getEvent() == FlowEvent.FLOW_ACTIVE && queue != null && !queue.isDurable()) {  // don't need to check this stuff here b/c this handler would only exist if we come down this path of the code
						if (event.getEvent() == FlowEvent.FLOW_ACTIVE) {
							boolean isNewTempQueue = false;
//							System.out.println("temp queue deteted!  gonna re-add subs");
							for (String topic : topics) {
								Topic t = f.createTopic(topic);
								try {
									try {
										session.addSubscription(queue, t, JCSMPSession.WAIT_FOR_CONFIRM);  // will throw exception if already there
										if (latch.getCount() == 1) {  // first time doing this
//											AaAnsi aa = AaAnsi.n().fg(Elem.PAYLOAD_TYPE).a(String.format("Subscribed tempQ to %stopic: '%s'", (topic.startsWith("!") ? "*NOT* " : ""), AaAnsi.n().colorizeTopic(topic)));
											AaAnsi aa = AaAnsi.n().fg(Elem.PAYLOAD_TYPE).a(String.format("Subscribed tempQ to %stopic: '", (topic.startsWith("!") ? "*NOT* " : "")));
											aa.a(AaAnsi.colorizeTopic(topic));
											aa.a('\'').reset();
											System.out.println(aa);
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
									System.out.println(AaAnsi.n().ex(e));
									System.exit(5);
//									e.printStackTrace();
//									isShutdown = true;
								}
							}
							latch.countDown();  // subs added, let's continue
							if (isNewTempQueue) {
								System.out.println(AaAnsi.n().invalid("!!! New temporary queue created: possible message loss during disconnect"));
							}
						} else {
							System.out.println("OTHER FLOW THING: " + event);
						}
					}
				});
				System.out.println(AaAnsi.n().a("Queue name: ").fg(Elem.KEY).a(flowQueueReceiver.getDestination().getName()));
				latch.await();  // block here until the subs are added
//				for (String topic : topics) {
//					Topic t = f.createTopic(topic);
//					session.addSubscription(queue, t, JCSMPSession.WAIT_FOR_CONFIRM);
//					System.out.println(AaAnsi.n().fg(Elem.PAYLOAD_TYPE).a(String.format("Subscribed tempQ to %stopic: '%s'", (topic.startsWith("!") ? "*NOT* " : ""), topic)).reset());
//				}
				flowQueueReceiver.start();
			} else {
				// Regular Direct topic consumer, using async / callback to receive
				directConsumer = session.getMessageConsumer((JCSMPReconnectEventHandler)null, new PrinterHelper());
				for (String topic : topics) {
					TopicProperties tp = new TopicProperties();
					tp.setName(topic);
					tp.setRxAllDeliverToOne(true);  // ensure DTO-override / DA is enabled for this sub
					Topic t = f.createTopic(tp);
					session.addSubscription(t, true);  // true == wait for confirm
					System.out.println(AaAnsi.n().fg(Elem.PAYLOAD_TYPE).a(String.format("Subscribed to Direct topic: '%s'", topic)).reset());
				}
				directConsumer.start();
			}
		}
		// DONE!!!!   READY TO ROCK!
		System.out.println();
		System.out.println("Starting. Press Ctrl-C to quit.");

		final Thread shutdownThread = new Thread(new Runnable() {
			public void run() {
				System.out.print(AaAnsi.n());
				System.out.println("\nShutdown hook triggered, quitting...");
				isShutdown = true;
				if (isConnected) {  // if we're disconnected, skip this because these will block/lock waiting on the reconnect to happen
					if (flowQueueReceiver != null) flowQueueReceiver.close();  // will remove the temp queue if required
					if (directConsumer != null) directConsumer.close();
				}
				try {
					Thread.sleep(200);
					session.closeSession();
					Thread.sleep(300);
				} catch (InterruptedException e) {  // ignore, we're quitting anyway
				} finally {
					System.out.println("Goodbye! 👋🏼");
					AnsiConsole.systemUninstall();
				}
			}
		});
		shutdownThread.setName("Shutdown Hook thread");
		//        shutdownThread.setDaemon(true);  // doesn't work, still gets deadlocked if disconnected and trying to reconnect
		// https://stackoverflow.com/questions/58972594/difference-in-adding-a-daemon-vs-non-daemon-thread-in-a-java-shutdown-hook
		Runtime.getRuntime().addShutdownHook(shutdownThread);

		if (browser != null) {  // ok, so we're browsing... can't use async msg receive callback, have to poll the queue
			PrinterHelper printer = new PrinterHelper();
			// hasMore() is useless! from JavaDocs: Returns true if there is at least one message available in the Browser's local message buffer. Note: If this method returns false, it does not mean that the queue is empty; subsequent calls to Browser.hasMore() or Browser.getNext() might return true and a message respectively.
			BytesXMLMessage nextMsg;
			try {
				//	        	while (!isShutdown && (nextMsg = browser.getNext()) != null) {
				while (!isShutdown && msgCountRemaining > 0) {  // change in behaviour... continue browsing until told to quit
					nextMsg = browser.getNext(-1);  // don't wait, return immediately
					if (nextMsg == null) {
						Thread.sleep(50);
						continue;
					}
					//	        		msgCount--;  // decrement 1 from total number of messages to browse
					if (browseFrom == -1) {
						if (browseFromRGMID != null && nextMsg.getReplicationGroupMessageId().toString().equals(browseFromRGMID)) {
							// match!
							browseFromRGMID = null;  // turn off "filtering"
						}
						if (browseFromRGMID == null) printer.onReceive(nextMsg);  // print all messages
						else {  // else we're just skipping this message, but still count it anyway
							PayloadHelper.msgCount++;
							payloadHelper.thinking.tick();
						}
					} else {
						try {
							long msgId = nextMsg.getMessageIdLong();  // deprecated, shouldn't be using this, but oh well!
							if (msgId <= 0) {  // should be impossible??
								System.out.println(AaAnsi.n().invalid("Message received with no Message ID set!"));
								printer.onReceive(nextMsg);
								break;
							}
							if (msgId >= browseFrom && msgId <= browseTo) {
								printer.onReceive(nextMsg);
								if (browseFrom == browseTo) break;  // just looking for one specific message, so done!
							} else if (msgId > browseTo) {
								if (browseFrom == browseTo) printer.onReceive(nextMsg);  // just looking for one specific message, so print this one to show the last
								System.out.println(AaAnsi.n().fg(Elem.PAYLOAD_TYPE).a(String.format("Message with ID '%d' received, greater than than browse range '%d'. Done.", msgId, browseTo)).reset());
								break;  // done!
							} else {  // else we're just skipping this message, but still count it anyway
								PayloadHelper.msgCount++;
								payloadHelper.thinking.tick();
							}
						} catch (Exception e) {
							System.out.println(AaAnsi.n().invalid("Exception on message trying to get Message ID!").reset());
							printer.onReceive(nextMsg);
							break;
						}
					}
				}
			} catch (JCSMPException | AccessDeniedException e) {  // something else went wrong: queue not exist, queue shutdown, etc.
				System.out.println("I'm here inside catch block");
				if (!isShutdown) {
					System.out.println(AaAnsi.n().invalid(String.format("%nUh-oh!  There was a problem: %s: %s", e.getClass().getSimpleName(), e.getMessage())));
					System.out.println("Quitting! 💀");
					System.exit(1);
				}
			} finally {
				System.out.print(AaAnsi.n());
				System.out.println("Browsing finished!");
				browser.close();
			}
		} else {  // async receive, either Direct sub or from a queue, so just wait here until Ctrl+C pressed
			//        	BufferedReader r = new BufferedReader(new InputStreamReader(System.in));
			while (!isShutdown) {
				Thread.sleep(50);
				// blocking receive mode
//				BytesXMLMessage msg;
//				if ((msg = directConsumer.receive(0)) != null) {
//					payloadHelper.dealWithMessage(msg);
//				}
				String userInput = null;
				if (System.in.available() > 0) {
					userInput = reader.readLine();
				}
				if (userInput != null) {
					try {
						int highlight = Integer.parseInt(userInput);
						if (highlight >= 0 && highlight < 125) {
							PayloadHelper.highlightTopicLevel = highlight - 1;  // so 0 -> -1 (highlight off), 1 -> 0 (level 1), etc.
						}
					} catch (NumberFormatException e) {
						if ("+".equals(userInput)) {
							payloadHelper.autoSpaceTopicLevels = true;
						} else if ("-".equals(userInput)) {
							payloadHelper.autoSpaceTopicLevels = false;
						} else if ("t".equals(userInput)) {
							payloadHelper.autoTrimPayload = !payloadHelper.autoTrimPayload;
						} else if ("q".equals(userInput)) {
							isShutdown = true;  // quit
						}
					}
				}
			}
		}
		isShutdown = true;
		System.out.print(AaAnsi.n());
		System.out.println("Main thread exiting.");
	}  // end of main()


	static Charset CHARSET;
	static CharsetDecoder DECODER;
	static {
		if (System.getenv("PRETTY_CHARSET") != null) {
			logger.info("Detected environment variable PRETTY_CHARSET: " + System.getenv("PRETTY_CHARSET"));
			try {
				CHARSET = Charset.forName(System.getenv("PRETTY_CHARSET"));  // this should throw if it doesn't find it
				DECODER = CHARSET.newDecoder().onMalformedInput(CodingErrorAction.REPLACE).onUnmappableCharacter(CodingErrorAction.REPLACE);
			} catch (Exception e) {
				System.out.println(AaAnsi.n().invalid("Invalid charset specified! PRETTY_CHARSET=" + System.getenv("PRETTY_CHARSET")).reset());
				System.out.println("Use one of: " + Charset.availableCharsets().keySet());
				System.out.println("Or clear environment variable 'PRETTY_CHARSET' to use default UTF-8");
				System.out.println("Quitting! 💀");
				System.exit(1);
			}
		} else {
			logger.info("Environment variable PRETTY_CHARSET not detected, using default UTF-8");
			CHARSET = StandardCharsets.UTF_8;
			DECODER = StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPLACE).onUnmappableCharacter(CodingErrorAction.REPLACE);
		}
	}

	// Helper class, for printing message to the console ///////////////////////////////////////

	private static class PrinterHelper implements XMLMessageListener {

		@Override
		public void onReceive(BytesXMLMessage message) {
			payloadHelper.dealWithMessage(message);
			if (!payloadHelper.isFilteringOn()) msgCountRemaining--;
			// if we're not browsing, and it's not a Direct message (doesn't matter if we ACK a Direct message anyhow)
            if (browser == null && message.getDeliveryMode() != DeliveryMode.DIRECT) message.ackMessage();  // if it's a queue
//			message.ackMessage();
			if (msgCountRemaining == 0) {
				System.out.println("\n" + AaAnsi.n().fg(Elem.PAYLOAD_TYPE).a(origMsgCount + " messages received. Quitting.").reset());
				isShutdown = true;
				if (flowQueueReceiver != null) flowQueueReceiver.close();
				if (directConsumer != null) directConsumer.close();
			}
		}

		@Override
		public void onException(JCSMPException e) {  // uh oh!
			System.out.println(AaAnsi.n().ex(" ### MessageListener's onException(): ", e).a(" - check ~/.pretty/pretty.log for details. "));
			logger.error("Caught in my onExecption() callback", e);
			if (e instanceof JCSMPTransportException) {  // all reconnect attempts failed (impossible now since we're at -1)
				isShutdown = true;  // let's quit
			}
		}
	}
}
