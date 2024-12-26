/*
 * Copyright 2023-2024 Solace Corporation. All rights reserved.
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

import java.io.PrintStream;

/**
 * Utility class just to move all the text help out of the PrettyDump class
 */
public class HelperText {
	
	private static final PrintStream o = System.out;
	
	
	static void printHelpExamples() {
		o.println("Command line examples:");
		o.println("‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾");
		o.println("prettydump 'solace/>'");
		o.println("  Localhost broker using default credentials, subscribe to \"#noexport/solace/>\", regular indent formatting");
		o.println();
		o.println("prettydump b:q1 0");
		o.println("  Localhost broker using default credentials, browse queue \"q1\", compress payload display");
		o.println();
		o.println("prettydump ws://localhost:8008 testvpn test pa55word '>' -1");
		o.println("  Localhost broker using WebSockets, \"testvpn\" VPN, user \"test\", subscribe to \"#noexport/>\", and enable auto-indent one-line mode (OLM)");
		o.println();
		o.println("prettydump tcps://demo.messaging.solace.cloud:55443 demo solace-cloud-client pw123 --export 'logs/>' -2");
		o.println("  Solace Cloud broker, subscribe to \"logs/>\", and enable two-line mode");
		o.println();
		o.println("prettydump 10.0.7.49 uat browser pw b:q.stg.logging 00 --count=-100");
		o.println("  Network broker, \"uat\" VPN, user \"browser\", browse queue \"q.stg.logging\" but only dump last 100 messages, and hide payload");
		o.println();
		o.println("prettydump 'tq:>,!bus/>,!stats/>' -50");
		o.println("  Localhost broker, subscribe to everything BUT \"bus\" and \"stats\" topics (need a tempQ for NOT subscriptions), OLM, trim topic to 50 chars");
		o.println();
		o.println("prettydump 10.0.7.49 uat app1 secretPw q:dmq.dev.app1 --count=1 --SUB_ACK_WINDOW_SIZE=1 ");
		o.println("  Network broker, consume/ACK & dump one message off queue \"dmq.dev.app1\", and set the AD window size to 1");
		o.println();
		o.println("prettydump tcps://stage1:55443 uat1 user1 pw b:massive.q --selector='dmq.dev.app1 --count=1 --SUB_ACK_WINDOW_SIZE=1 ");
		o.println("  Network broker, consume/ACK & dump one message off queue \"dmq.dev.app1\", and set the AD window size to 1");
		o.println();
		// TODO what is the meaning of exporting vs. not exporting a not subscription
		
		
		o.println("Runtime examples: type these and press [ENTER]");
		o.println("‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾");
		o.println("cm : switch to \"minimal\" colour mode (only topic coloured)");
		o.println("3  : highlight the 3rd topic level, dim the others");
		o.println("t  : in one-line/compressed modes: trim the payload to the terminal width");
//		o.println("     in regular mode, trime user props and payload to 30 lines");
		o.println("+  : in one-line mode, enable topic level spacing");
		o.println("f=abc : client-side filter for 'abc', only show messages that contain that string");
		o.println("ts : toggle printing of message received timestamp");
	}
	
	
	
	static void printHelpIndent() {

		o.println("    • 1..8      normal mode, pretty-printed and indented n spaces");
		o.println("    • 0         normal mode, payload and user properties compressed to one line");
		o.println("    • 00        no payload mode, user properties still pretty-printed");
		o.println("    • 000       no payload mode, user properties each compressed to one line");
		o.println("    • 0000      no payload mode, no user properties, headers only");
		o.println("    • -250..-3  one-line mode, topic and payload only, compressed, abs(n) fixed indent");
		o.println("    • -2        two-line mode, topic and payload on two lines (try 'minimal' color mode)");
		o.println("    • -1        one-line mode, automatic variable payload indentation");
		o.println("    • -0        one-line mode, topic only");
        o.println("    • For one-line modes, change '-' to '+' to enable topic level alignment");
//		o.println("    - ±0        one-line mode, topic only, with/without topic spacing");
		//		o.println("Runtime: press 't'[ENTER] (or argument '--trim') to auto-trim payload to screen width");
		//		o.println("Runtime: press '+' or '-'[ENTER] to toggle topic level spacing during runtime");
		////		o.println("Runtime: press \"t[ENTER]\" to toggle payload trim to terminal width (or argument --trim)");
		////		o.println("Runtime: press \"+ or -[ENTER]\" to toggle topic level spacing/alignment (or argument \"+indent\")");
		//		o.println("NOTE: optional content Filter searches entire message body, regardless of indent");
		////		printUsageText();
		//		o.println("See README.md for more detailed help with indent.");

	}


	static void printHelpMoreText() {
		printHelpText(false);
		o.println("    • e.g. prettydump 'logs/>' -1  ~or~  prettydump q:q1  ~or~  prettydump b:dmq -0");
		o.println(" - Optional indent: integer, valid values:");
		printHelpIndent();
		//		o.println(" - Optional indent: integer, default==2 spaces; specifying 0 compresses payload formatting");
		//		o.println("    - No payload mode: use indent '00' to only show headers and props, or '000' for compressed");
		//		o.println("    - One-line mode: use negative indent value (trim topic length) for topic & payload only");
		//		o.println("       - Or use -1 for auto column width adjustment, or -2 for two-line mode");
		//		o.println("       - Use negative zero -0 for topic only, no payload");
		//		o.println(" - Optional count: stop after receiving n number of msgs; or if < 0, only show last n msgs");

		o.println(" - Additional non-ordered arguments: for more advanced capabilities");
		o.println("    • --selector=\"mi like 'hello%world'\"  Selector for Queue consume and browse");
		o.println("    • --filter=\"ABC123\"  client-side regex content filter on any received message dump");
		o.println("    • --count=n     stop after receiving n number of msgs; or if < 0, only show last n msgs");
//		o.println("    • --skip=n      skip the first n messages received");
		
		o.println("    • --raw         show original message payload, not pretty-printed; text, JSON, XML only");
		o.println("    • --dump        enable binary dump for every message (useful for charset encoding issues)");
		o.println("    • --trim        enable paylaod trim for one-line (and two-line) modes");
		o.println("    • --ts          print time when PrettyDump received the message (not messages' timestamp)");
		o.println("    • --export      disables the automatic prefixing of \"#noexport/\" to the start of all topics");
		o.println("    • --compressed  tells JCSMP API to use streaming compression (TCP only, not WebSockets)");
		o.println("    • --defaults    show all possible JCSMP Session properties to set/override");
		o.println(" - Runtime options: type the following into the console while the app is running");
		o.println("    • Press \"t\" ENTER to toggle payload trim to terminal width (or argument --trim) in OLM");
		o.println("    • Press \"+\" or \"-\" ENTER to toggle topic level alignment (or argument \"+indent\") in OLM");
		o.println("    • Press \"[1-n]\" ENTER to highlight a particular topic level (\"0\" ENTER to revert)");
		o.println("    • Type \"c[svlmxo]\" ENTER\" to set colour mode: standard, vivid, light, minimal, matrix, off");
		o.println("    • Type \"f=<PATTERN>\" ENTER\" to start filtering for messages containing PATTERN");
		o.println("Environment variable options:");
		o.println(" - Default charset is UTF-8. Override by setting: export PRETTY_CHARSET=ISO-8859-1");
		//		o.println("    - e.g. export PRETTY_CHARSET=ISO-8859-1  (or \"set\" on Windows)");
		o.println(" - Multiple colour schemes supported. Override by setting: export PRETTY_COLORS=whatever");
		o.println("    • Choose: \"standard\" (default), \"vivid\", \"light\", \"minimal\", \"matrix\", \"off\"");
		o.println();
//		o.println("Runtime commands, type these + [ENTER] while running:");
//		o.println(" - Default charset is UTF-8. Override by setting: export PRETTY_CHARSET=ISO-8859-1");
//		o.println();
		o.println("SdkPerf Wrap mode: use any SdkPerf as usual, pipe command to \" | prettydump wrap\" to prettify");
		o.println();
		o.println("prettydump -he  for examples");
		o.println("See the README.md for more explanations of every feature and capability");
		o.println("https://github.com/SolaceLabs/solace-pretty-dump");
		o.println("https://solace.community/discussion/3238/sdkperf-but-pretty-for-json-and-xml");
		o.println();
	}

	static void printHelpText(boolean full) {
		printUsageText(false);
		o.println(" - Default protocol TCP; for TLS use \"tcps://\"; or \"ws://\" or \"wss://\" for WebSocket");
		o.println(" - Default parameters will be: localhost:55555 default foo bar '#noexport/>' 2");
		o.println(" - Subscribing options (arg 5, or shortcut mode arg 1), one of:");
		o.println("    • Comma-separated list of Direct topic subscriptions");
		o.println("       - Automatic \"#noexport/\" prefixes added for DMR/MNR; disable with --export");
		o.println("    • q:queueName to consume from queue");
		o.println("    • b:queueName to browse a queue (all messages, or range by MsgSpoolID or RGMID)");
		o.println("    • f:queueName to browse/dump only first oldest message on a queue");
		o.println("    • tq:topics   provision a tempQ with optional topics  (can use NOT '!' topics)");
		if (full) o.println(" - Indent: integer, default==2; ≥ 0 normal, = 00 no payload, ≤ -0 one-line mode (OLM)");
		//		o.println(" - Optional count: stop after receiving n number of msgs; or if < 0, only show last n msgs");
		o.println(" - Shortcut mode: first arg looks like a topic, or starts '[qbf]:', assume defaults");
		o.println("    • Or if first arg parses as integer, select as indent, rest default options");
		if (full) o.println(" - Additional args: --count, --filter, --selector, --trim, --ts, --raw, --compressed");
		if (full) o.println(" - Any JCSMP Session property (use --defaults to see all)");
		if (full) o.println(" - Environment variables for decoding charset and colour mode");
		if (full) o.println();
		if (full) o.println("prettydump -hm  for more help on indent, additional parameters, charsets, and colours");
		if (full) o.println("prettydump -he  for examples");
		if (full) o.println();
	}

	static void printUsageText(boolean full) {
		o.println("Usage: prettydump [host] [vpn] [user] [pw] [topics|[qbf]:queueName|tq:topics] [indent]");
		o.println("   or: prettydump <topics|[qbf]:queueName|tq:topics> [indent]  for \"shortcut\" mode");
		o.println();
		if (full) o.println("prettydump -h or -hm  for help on available arguments and environment variables.");
		if (full) o.println("prettydump -he  for examples");
		if (full) o.println();
	}

}
