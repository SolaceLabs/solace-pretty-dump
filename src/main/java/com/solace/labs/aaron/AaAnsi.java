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

import org.fusesource.jansi.Ansi;
import org.fusesource.jansi.Ansi.Attribute;
import org.fusesource.jansi.AnsiConsole;

/**
 * Kind of a wrapper around the JAnsi library.  Modified for my own uses.
 * @author Aaron Lee
 */
public class AaAnsi {

	enum ColorMode {
		OFF,
		MINIMAL,
		STANDARD,
		VIVID,
		LIGHT,
		;
	}

	private static ColorMode MODE = ColorMode.STANDARD;
	static {
//		if (System.getProperty("PRETTY_COLORS") != null) {
//			try {
//				MODE = ColorMode.valueOf(System.getProperty("PRETTY_COLORS").toUpperCase());
//				Elem.updateColors(MODE);
//			} catch (IllegalArgumentException e) { }
//		}
		if (System.getenv("PRETTY_COLORS") != null) {
			try {
				MODE = ColorMode.valueOf(System.getenv("PRETTY_COLORS").toUpperCase());
				Elem.updateColors(MODE);
			} catch (IllegalArgumentException e) { }
		}
	}
	
	static ColorMode getColorMode() {
		return MODE;
	}
	
	private Ansi ansi = new Ansi();
	private Elem curElem = null;  // not yet inside anything
	
	private boolean isOn() {
		return (MODE != ColorMode.OFF);
	}

	public AaAnsi() {
		reset();
	}
	
	public AaAnsi colorizeTopic(String topic) {
		if (MODE == ColorMode.VIVID) return colorizeTopicRainbow(topic);
		else return colorizeTopicPlain(topic);
	}
	
	private AaAnsi colorizeTopicPlain(String topic) {
//		fg(Elem.DESTINATION).a(topic);
		String[] levels = topic.split("/");
		for (int i=0; i<levels.length; i++) {
			fg(Elem.DESTINATION).a(levels[i]);
			if (i < levels.length-1) {
				fg(Elem.TOPIC_SEPARATOR).a('/');//.reset();
			}
		}
		return this;
	}
	
//	private static int maxLevels = 1;
	static LinkedListOfIntegers maxLengthTopicLevels = new LinkedListOfIntegers();
	private AaAnsi colorizeTopicRainbow(String topic) {
		String[] levels = topic.split("/");
//		maxLevels = Math.max(maxLevels, levels.length);
		maxLengthTopicLevels.insert(levels.length);
		// https://github.com/topics/256-colors
		int[] colorTable = new int[] { 82, 83, 84, 85, 86, 87,
				                       81, 75, 69, 63, /*57,*/
				                       93, 129, 165, 201,
				                       200, 199, 198, 197,
				                       203, 209, 215, 221, 227,
				                       191, 155, 119
		};
		int startingColIndex = 5;  // cyan 86
//		int startingColIndex = 7;  // temp 75
		double step = Math.max(1, Math.min(3, colorTable.length * 1.0 / Math.max(1, maxLengthTopicLevels.getMax())));
		
		for (int i=0; i<levels.length; i++) {
			ansi.fg(colorTable[(startingColIndex + (int)(step * i)) % colorTable.length]).a(levels[i]);
			if (i < levels.length-1) {
//				fg(15).a('/');
//				fg(Elem.DESTINATION).faint(true).a('/').faint(false);
				fg(Elem.TOPIC_SEPARATOR).a('/').reset();
			}
		}
		return this;
	}
	
	
	public AaAnsi invalid(String s) {
		if (isOn()) {
//			ansi.reset().bg(196).fg(231).a(s).reset();
//			ansi.reset().bg(196).fg(16).a(s).reset();
			fg(Elem.ERROR).a(s).reset();
//			ansi.fgRed().a(s);
//			restore();
		} else {
			ansi.a(s);
		}
		return this;
	}
	
	public AaAnsi ex(Exception e) {
		String exception = e.getClass().getSimpleName() + " - " + e.getMessage();
		return invalid(exception);
//		if (isOn()) {
//			fg(Elem.ERROR).a(exception);
//			ansi.fgBlack().bgRed().a(exception);
//			restore();
//		} else {
//			ansi.a(exception);
//			ansi.a(e.getClass().getSimpleName()).a(" - ").a(e.getMessage());
//		}
//		return this;
	}
	
	
	public AaAnsi fg(Elem elem) {
		curElem = elem;
		if (isOn()) {
			Col c = elem.getCurrentColor();
			if (c.faint) {
//				ansi.a(Attribute.INTENSITY_FAINT).fg(c.value);
				makeFaint().fg(c.value);
			} else {
//				ansi.a(Attribute.RESET).fg(c.value);
				fg(c.value);
			}
		}
		return this;
	}
	
	public AaAnsi makeFaint() {
		if (isOn()) {
			/* if (faint)*/ ansi.a(Attribute.INTENSITY_FAINT);
//			else ansi.a(Attribute.INTENSITY_BOLD_OFF);  // doesn't work.  For some reason there is no "faint off" ..!?
		}
		return this;
	}
	
	public AaAnsi fg(int colorIndex) {
		if (isOn()) {
			if (colorIndex == -1) ansi.fgDefault();
			else ansi.fg(colorIndex);
		}
		return this;
	}

	@Override
	public String toString() {
//		return UsefulUtils.chop(ansi.toString()) + (isOn() ? new Ansi().reset().toString() : "");
		return UsefulUtils.chop(ansi.toString()) + new Ansi().reset().toString();
	}
	
	public AaAnsi aRaw(String s) {
		ansi.a(s);
		return this;
	}

	public AaAnsi a(String s) {
		return a(s, false);
	}
	
	public AaAnsi a(String s, boolean compact) {
		StringBuilder sb = new StringBuilder();
		String replacement = null;  // needed if we have to substitute any invalid chars
		for (int i=0; i<s.length(); i++) {
			char c = s.charAt(i);
			if (c < 0x20) {  // special handling of control characters
				if (compact || (!compact && (c < 0x09 || c == 0x0B || c == 0x0C || c > 0x0D))) {
					sb.append("·");  // control chars
				} else sb.append(c);
			} else if (c == 0x7f) {
				sb.append("·");
			} else if (c == '\ufffd') {
				if (replacement == null) {
					if (isOn()) {
	//					Ansi a = new Ansi().reset().bg(196).fg(231).a('\ufffd').bgDefault().fg(curElem.getCurrentColor().value);
						Ansi a = new Ansi().reset().bg(Elem.ERROR.getCurrentColor().value).fg(231).a('¿').bgDefault().fg(curElem.getCurrentColor().value);
						replacement = a.toString();
					} else {
						replacement = "¿";
					}
				}
				sb.append(replacement);
			} else {
				sb.append(c);
			}
		}
		ansi.a(sb.toString());
		return this;
	}
	
	public AaAnsi a(AaAnsi ansi) {
		this.ansi.a(ansi.toString());
		return this;
	}
	
	
	public AaAnsi a2(String s) {
		if (s.contains("\ufffd")) {
//			String replacement = new AaAnsi().invalid("¿", curElem).toString();
//			System.out.println("REPLACEMTN: '" + replacement + "'");
			String replacement;
			if (isOn()) {
				Ansi a = new Ansi().reset().bg(196).fg(231).a("?").bgDefault().fg(curElem.getCurrentColor().value);
				replacement = a.toString();
			} else {
				replacement = "?";
			}
			s = s.replaceAll("\ufffd", replacement);
		}
		ansi.a(s);
		return this;
	}
	
	public AaAnsi a(char c) {
		ansi.a(c);
		return this;
	}

	public AaAnsi a(boolean b) {
		ansi.a(Boolean.toString(b));
		return this;
	}
	
	/**
	 * I think this was supposed to be for quotes and stuff
	 */
//	public AaAnsi bookend(String s) {
//		if (s != null && s.length() > 2 && s.charAt(0) == s.charAt(s.length()-1)) {
//			makeFaintfaint(true);
//			ansi.a(s.charAt(0));
//			makeFaintfaint(false);
//			ansi.a(s.substring(1, s.length()-1));
//			makeFaintfaint(true);
//			ansi.a(s.charAt(s.length()-1));
//			makeFaintfaint(false);
//		} else {
//			ansi.a(s);
//		}
//		return this;
//	}

	private void restore() {
		if (isOn() && curElem != null) {
			ansi.bgDefault();
			fg(curElem);
		}
	}

	public AaAnsi reset() {
		if (isOn()) {
			ansi.reset();
			if (Elem.DEFAULT.getCurrentColor().value != -1) ansi.fg(Elem.DEFAULT.getCurrentColor().value);
		}
		return this;
	}
	
//	public AaAnsi fgRgb(int r, int g, int b) {
//		if (isOn()) ansi.fgRgb(r, g, b);
//		return this;
//	}
	
	

	
	public static void main(String... args) {
//		test();
	}
	
	static void test() {
		
		System.out.println("width: " + AnsiConsole.out().getTerminalWidth());

		System.out.println("colors: " + AnsiConsole.out().getColors());
		System.out.println("MODE: " + AnsiConsole.out().getMode());
		System.out.println("type: " + AnsiConsole.out().getType());

		Ansi ansi = new Ansi();
		for (int i=0;i<16;i++) ansi.fg(i).a("█████████");
		System.out.println(ansi.toString());
		
		ansi = new Ansi();
		for (int i=0;i<256;i++) ansi.fgRgb(i, 0, 0).a("█");
		ansi.a('\n');
		for (int i=0;i<256;i++) ansi.fgRgb(i, i, 0).a("█");
		ansi.a('\n');
		for (int i=0;i<256;i++) ansi.fgRgb(0, i, 0).a("█");
		ansi.a('\n');
		for (int i=0;i<256;i++) ansi.fgRgb(0, i, i).a("█");
		ansi.a('\n');
		for (int i=0;i<256;i++) ansi.fgRgb(0, 0, i).a("█");
		ansi.a('\n');
		for (int i=0;i<256;i++) ansi.fgRgb(i, 0, i).a("█");
		ansi.a('\n');
		for (int i=0;i<256;i++) ansi.fg(i).a("█");
		ansi.a('\n');
		ansi.fgRgb(0, 200, 149).a("solace███ int 43 closest 00d7af   ");
		ansi.fgRgb(0, 255, 190).a("bright solace███ int 49 00ffaf");
		ansi.a('\n');
		ansi.reset();
//		AnsiConsole.out().println(ansi);
		System.out.println(ansi.toString());
//		System.out.println(new AaAnsi().setSolaceGreen().a("AARONSOLACEMANUAL").reset());
		
	}

	

}
