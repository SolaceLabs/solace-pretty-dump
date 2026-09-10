/*
 * Copyright 2026 Solace Corporation. All rights reserved.
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

final class CliOptions {

	static final String DEFAULT_HOST = "localhost";
	static final String DEFAULT_VPN = "default";
	static final String DEFAULT_USERNAME = "foo";
	static final String DEFAULT_PASSWORD = "bar";
	static final String DEFAULT_TOPIC = "#noexport/>";

	private final List<String> regularArgs;
	private final List<String> specialArgs;
	private final String host;
	private final String vpn;
	private final String username;
	private final String password;
	private final String[] topics;
	private final String indentArg;
	private final boolean shortcutMode;
	private final boolean help;
	private final boolean helpMore;
	private final boolean helpExamples;
	private final boolean wrapMode;
	private final String outputMode;
	private final boolean noAnsi;
	private final boolean noBanner;
	private final boolean quiet;

	private CliOptions(List<String> regularArgs, List<String> specialArgs, String host, String vpn,
			String username, String password, String[] topics, String indentArg, boolean shortcutMode,
			boolean help, boolean helpMore, boolean helpExamples, boolean wrapMode, String outputMode,
			boolean noAnsi, boolean noBanner, boolean quiet) {
		this.regularArgs = Collections.unmodifiableList(regularArgs);
		this.specialArgs = Collections.unmodifiableList(specialArgs);
		this.host = host;
		this.vpn = vpn;
		this.username = username;
		this.password = password;
		this.topics = topics.clone();
		this.indentArg = indentArg;
		this.shortcutMode = shortcutMode;
		this.help = help;
		this.helpMore = helpMore;
		this.helpExamples = helpExamples;
		this.wrapMode = wrapMode;
		this.outputMode = outputMode;
		this.noAnsi = noAnsi;
		this.noBanner = noBanner;
		this.quiet = quiet;
	}

	static CliOptions parse(String... args) {
		boolean help = false;
		boolean helpMore = false;
		boolean helpExamples = false;
		boolean wrapMode = args.length == 1 && "wrap".equalsIgnoreCase(args[0]);
		String outputMode = "text";
		boolean noAnsi = false;
		boolean noBanner = false;
		boolean quiet = false;

		ArrayList<String> regularArgs = new ArrayList<>();
		ArrayList<String> specialArgs = new ArrayList<>();
		for (String arg : args) {
			if (isHelpArg(arg)) help = true;
			else if (isHelpMoreArg(arg)) helpMore = true;
			else if (isHelpExamplesArg(arg)) helpExamples = true;
			else if (arg.startsWith("--output=")) outputMode = arg.substring("--output=".length()).toLowerCase();
			else if ("--no-ansi".equals(arg)) noAnsi = true;
			else if ("--no-banner".equals(arg)) noBanner = true;
			else if ("--quiet".equals(arg)) quiet = true;

			if (arg.startsWith("--") || "-defaults".equals(arg)) specialArgs.add(arg);
			else regularArgs.add(arg);
		}

		boolean shortcutMode = false;
		String host = DEFAULT_HOST;
		String vpn = DEFAULT_VPN;
		String username = DEFAULT_USERNAME;
		String password = DEFAULT_PASSWORD;
		String[] topics = new String[] { DEFAULT_TOPIC };
		String indentArg = null;

		if (regularArgs.size() > 0 && regularArgs.size() <= 2) {
			String arg0 = regularArgs.get(0);
			boolean shortcut = false;
			if (looksLikeTopicArg(arg0) || arg0.matches("^[qbf]:.+")) {
				shortcut = true;
			} else if (regularArgs.size() == 1) {
				try {
					ConfigState indentProbe = new ConfigState();
					indentProbe.dealWithIndentParam(arg0);
					shortcut = true;
					regularArgs.add(0, DEFAULT_TOPIC);
				} catch (NumberFormatException e) {
					// Not an indent, so treat it as host below.
				} catch (IllegalArgumentException e) {
					// Same as legacy parsing: a numeric but invalid indent still enters shortcut mode,
					// so the later validation path can print the indent-specific help.
					shortcut = true;
					regularArgs.add(0, DEFAULT_TOPIC);
				}
			}
			if (shortcut) {
				shortcutMode = true;
				regularArgs.add(0, host);
				regularArgs.add(1, vpn);
				regularArgs.add(2, username);
				regularArgs.add(3, password);
			} else {
				host = regularArgs.get(0);
			}
		} else if (regularArgs.size() > 0) {
			host = regularArgs.get(0);
		}

		if (regularArgs.size() > 1) vpn = regularArgs.get(1);
		if (regularArgs.size() > 2) username = regularArgs.get(2);
		if (regularArgs.size() > 3) password = regularArgs.get(3);
		if (regularArgs.size() > 4) {
			String arg4 = regularArgs.get(4);
			if (arg4.matches("^[qbf]:.+")) {
				topics = new String[] { arg4 };
			} else {
				topics = arg4.split("\\s*,\\s*");
			}
		}
		if (regularArgs.size() > 5) indentArg = regularArgs.get(5);

		return new CliOptions(regularArgs, specialArgs, host, vpn, username, password, topics,
				indentArg, shortcutMode, help, helpMore, helpExamples, wrapMode, outputMode,
				noAnsi, noBanner, quiet);
	}

	private static boolean isHelpArg(String arg) {
		return "-h".equals(arg) || "--h".equals(arg) || "-?".equals(arg) || arg.startsWith("--?")
				|| "-help".equals(arg) || "--help".equals(arg);
	}

	private static boolean isHelpMoreArg(String arg) {
		return "-hm".equals(arg) || "--hm".equals(arg) || "-??".equals(arg);
	}

	private static boolean isHelpExamplesArg(String arg) {
		return "-he".equals(arg) || "--he".equals(arg);
	}

	private static boolean looksLikeTopicArg(String arg) {
		return (arg.contains("/") && !arg.contains("//"))
				|| arg.contains(">")
				|| arg.contains("*")
				|| arg.contains("#")
				|| arg.startsWith("tq:");
	}

	List<String> getRegularArgs() {
		return regularArgs;
	}

	List<String> getSpecialArgs() {
		return specialArgs;
	}

	String getHost() {
		return host;
	}

	String getVpn() {
		return vpn;
	}

	String getUsername() {
		return username;
	}

	String getPassword() {
		return password;
	}

	String[] getTopics() {
		return topics.clone();
	}

	String getIndentArg() {
		return indentArg;
	}

	boolean isShortcutMode() {
		return shortcutMode;
	}

	boolean isHelp() {
		return help;
	}

	boolean isHelpMore() {
		return helpMore;
	}

	boolean isHelpExamples() {
		return helpExamples;
	}

	boolean isWrapMode() {
		return wrapMode;
	}

	String getOutputMode() {
		return outputMode;
	}

	boolean isNoAnsi() {
		return noAnsi;
	}

	boolean isNoBanner() {
		return noBanner;
	}

	boolean isQuiet() {
		return quiet;
	}
}
