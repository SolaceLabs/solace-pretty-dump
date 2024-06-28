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
		MATRIX,
		;
	}

	private static ColorMode MODE = ColorMode.STANDARD;
	static {
		if (System.getenv("PRETTY_COLORS") != null) {
			try {
				MODE = ColorMode.valueOf(System.getenv("PRETTY_COLORS").toUpperCase());
				Elem.updateColors(MODE);
			} catch (IllegalArgumentException e) {
				System.err.println(AaAnsi.n().invalid(String.format("Invalid value for environment variable PRETTY_COLORS \"%s\"", System.getenv("PRETTY_COLORS"))));
//				System.out.println(AaAnsi.n().invalid("asdlfkjalsdkfj"));
				System.err.println("Valid values are: standard, vivd, light, minimal, matrix, off");
			}
		}
	}
		
	static ColorMode getColorMode() {
		return MODE;
	}
	
	private Ansi jansi = new Ansi();
	private int charCount = 0;
	private Elem curElem = null;  // used to track which element type we're inside (useful for switching colours back in styleString append)
	int controlChars = 0;  // we'll turn this to true if we get too many control chars
	int replacementChars = 0;
	
	private void incChar(int amount) {
		charCount += amount;
	}
	
	private void incChar() {
		charCount ++;
	}
	
	private boolean isOn() {
		return (MODE != ColorMode.OFF);
	}

	public AaAnsi() {
		reset();
	}

	public AaAnsi(boolean reset) {
		if (reset) reset();
	}

	public int getCharCount() {
		return charCount;
	}
	
	/** Convenience static builder */
	public static AaAnsi n() {
		return new AaAnsi();
	}
	
	/** Convenience, creates a new AaAnsi and adds the string (performing formatting) */
//	public static AaAnsi s(String s) {
//		AaAnsi aa = AaAnsi.n().a(s);
//		return aa;
//	}

	/** Convenience, creates a new AaAnsi and adds the string raw */
//	public static AaAnsi r(String s) {
//		AaAnsi aa = AaAnsi.n().aRaw(s);
//		return aa;
//	}

	public AaAnsi colorizeTopic(String topic, int highlight) {
		/*
		 * if (MODE == ColorMode.VIVID) return colorizeTopicRainbow(topic,highlight);
		 * else
		 */ if (MODE == ColorMode.OFF) return a(topic);
		else return colorizeTopicPlain(topic, highlight);
	}
	
	private AaAnsi colorizeTopicPlain(String topic, int highlight) {
		String[] levels = topic.split("/");
		maxLengthTopicLevels.insert(levels.length);
		if (highlight >= 0) makeFaint();
		for (int i=0; i<levels.length; i++) {
			incChar(levels[i].length());   // need to update manually since adjusting the jansi directly
			if (i == highlight) {
				jansi.reset();  // back to bright!
			}
			fg(figureColor(i, highlight));
			if (i == highlight) {
				int firstDot = levels[i].indexOf('⋅');
				if (firstDot == -1) {
					jansi.a(levels[i]);
					makeFaint();
				} else {
					jansi.a(levels[i].substring(0,firstDot));
					makeFaint();
					jansi.a(levels[i].substring(firstDot));
				}
			} else {
				int firstDot = levels[i].indexOf('⋅');
				if (firstDot == -1) {
					jansi.a(levels[i]);
				} else {
					jansi.a(levels[i].substring(0,firstDot));
					makeFaint();
					jansi.a(levels[i].substring(firstDot));
				}
			}
			if (i < levels.length-1) {
				if (highlight == -1) reset();
				fg(Elem.TOPIC_SEPARATOR).a('/');  // this does the charCount!
			}
		}
		return reset();
	}
	
	final static int[] topicColorTable = new int[] {
			82, 83, 84, 85, 86,
			87, 81, 75, 69, 63, /*57,*/
            93, 129, 165, 201,
            200, 199, 198, 197,
            203, 209, 215, 221,
            227, 191, 155, 119
	};

	// this is for the scrolling rainbow line during Wrap mode
	final static int[] rainbowTable = new int[] {
			46, 47, 48, 49, 50,
			51, 45, 39, 33, 27,
			21, 57, 93, 129, 165,
			201, 200, 199, 198, 197,
			196, 202, 208, 214, 220,
			226, 190, 154, 118, 82
	};
	
	private static LinkedListOfIntegers maxLengthTopicLevels = new LinkedListOfIntegers();
	
	private int figureColor(int i, int highlight) {
		if (MODE == ColorMode.VIVID) {
			// https://github.com/topics/256-colors
			int startingColIndex = 5;  // cyan 86
	//		int startingColIndex = 7;  // temp 75
			double step = Math.max(1, Math.min(3, topicColorTable.length * 1.0 / Math.max(1, maxLengthTopicLevels.getMax())));
			if (i == highlight) {
				return 49;  // solace green ish
			} else return topicColorTable[(startingColIndex + (int)(step * i)) % topicColorTable.length];
		} else {
			return Elem.DESTINATION.getCurrentColor().value;
		}
	}

/*	private AaAnsi colorizeTopicRainbow(String topic, int highlight) {
		String[] levels = topic.split("/");
//		maxLevels = Math.max(maxLevels, levels.length);
		maxLengthTopicLevels.insert(levels.length);
		// https://github.com/topics/256-colors
		int startingColIndex = 5;  // cyan 86
//		int startingColIndex = 7;  // temp 75
		double step = Math.max(1, Math.min(3, topicColorTable.length * 1.0 / Math.max(1, maxLengthTopicLevels.getMax())));
		if (highlight >= 0) makeFaint();
		for (int i=0; i<levels.length; i++) {
//			if (i != 4) makeFaint();
//			else jansi.reset();
			if (i == highlight) {
				jansi.reset();  // back to bright!
				jansi.fg(49);
			} else jansi.fg(topicColorTable[(startingColIndex + (int)(step * i)) % topicColorTable.length]);
			if (i == highlight) {
				int firstDot = levels[i].indexOf('⋅');
				if (firstDot == -1) {
					jansi.a(levels[i]);
					makeFaint();
				} else {
					jansi.a(levels[i].substring(0,firstDot));
					makeFaint();
					jansi.a(levels[i].substring(firstDot));
				}
			} else {
//				jansi.a(levels[i]);  // the whole thing.  maybe we should turn it back to faint after dots start
				int firstDot = levels[i].indexOf('⋅');
				if (firstDot == -1) {
					jansi.a(levels[i]);
				} else {
					jansi.a(levels[i].substring(0,firstDot));
					makeFaint();
					jansi.a(levels[i].substring(firstDot));
				}
			}
			if (i < levels.length-1) {
				if (highlight == -1) reset();
				fg(Elem.TOPIC_SEPARATOR).a('/');
			}
		}
		return reset();
	}*/
	
	public AaAnsi invalid(String s) {
		if (isOn()) {
			fg(Elem.ERROR).a(s).reset();
		} else {
			jansi.a(s);
		}
		incChar(s.length());
		return this;
	}
	
	public AaAnsi ex(Exception e) {
		String exception = e.getClass().getSimpleName() + " - " + e.getMessage();
		return invalid(exception);
	}
	
	public AaAnsi fg(Elem elem) {
		curElem = elem;
		if (isOn()) {
			Col c = elem.getCurrentColor();
			if (c.faint) {
				makeFaint().fg(c.value);
			} else if (c.italics) {
				makeItalics().fg(c.value);
			} else {
				fg(c.value);
			}
		}
		return this;
	}
	
	public AaAnsi fg(int colorIndex) {
		if (isOn()) {
			if (colorIndex == -1) jansi.fgDefault();
			else jansi.fg(colorIndex);
		}
		return this;
	}
	
	AaAnsi makeFaint() {
		if (isOn()) {
			/* if (faint)*/ jansi.a(Attribute.INTENSITY_FAINT);
//			else jansi.a(Attribute.INTENSITY_BOLD_OFF);  // doesn't work.  For some reason there is no "faint off" ..!?
		}
		return this;
	}

	public AaAnsi makeItalics() {
		if (isOn()) {
			jansi.a(Attribute.ITALIC);
		}
		return this;
	}
	
	public AaAnsi italicsOn() {
		if (isOn()) {
			jansi.a(Attribute.ITALIC);
		}
		return this;
	}
	
	public AaAnsi italicsOff() {
		if (isOn()) {
			jansi.a(Attribute.ITALIC_OFF);
		}
		return this;
	}

	
	public AaAnsi faintOn() {
		if (isOn()) {
			jansi.a(Attribute.INTENSITY_FAINT);
		}
		return this;
	}
	
	public AaAnsi faintOff() {
		if (isOn()) {
			jansi.a(Attribute.INTENSITY_BOLD_OFF);  // doesn't work.  For some reason there is no "faint off" ..!?
		}
		return this;
	}

	public int length() {
		return calcLength(this.toString());
	}
	
	static int calcLength(String ansiString) {
		int count = 0;
		int pos = 0;
		boolean insideEscape = false;
		while (pos < ansiString.length()) {
			if (ansiString.charAt(pos) == 27) {
				insideEscape = true;
				pos++;
				continue;
			}
			if (insideEscape) {
				if (ansiString.charAt(pos) == 'm') {
					insideEscape = false;
				}
				pos++;
				continue;
			}
			count++;
			pos++;
		}
		return count;
	}
	
//	String trim() {
//		if (jansi.)
//	}
	
	
	String trim(int len) {
		assert len > 0;
		String s = toString();
		if (getCharCount() <= len) return toString();
//		if (s == null && s.isEmpty()) return s;  // so now we know there's at least one char
		StringBuilder sb = new StringBuilder();
		int count = 0;
		int pos = 0;
		boolean insideEscape = false;
		do {
			sb.append(s.charAt(pos));
			if (s.charAt(pos) == 27) {
				insideEscape = true;
			} else if (insideEscape) {
				if (s.charAt(pos) == 'm') {
					insideEscape = false;
				}
			} else {
				count++;
			}
			pos++;
		} while (pos < s.length() && (count < len-1 || insideEscape));
		if (pos < s.length()) return sb.append("…").append(AaAnsi.n()).toString();
		else return sb.toString();  // the whole thing   ... this should be impossible now due to implementing charCount
	}

	@Override
	public String toString() {
//		reset();
		return jansi.toString();
	}

	/** Just jam is straight in, don't parse at all! */
	@Deprecated
	private AaAnsi aRaw(String s, int numChars) {
		jansi.a(s);
		return this;
	}

	/** Just copy the whole AaAnsi into this one. */
	public AaAnsi a(AaAnsi ansi) {
		jansi.a(ansi.toString());
		charCount += ansi.charCount;
		controlChars += ansi.controlChars;
		replacementChars += ansi.replacementChars;
		return this;
	}
	
	public AaAnsi a(char c) {
		jansi.a(c);
		incChar();
		return this;
	}

	public AaAnsi a(boolean b) {
		String bs = Boolean.toString(b);
		jansi.a(bs);
		incChar(bs.length());
		return this;
	}

	public AaAnsi a(String s) {
		return a(s, false);
	}
	
	public AaAnsi aStyledString(String s) {
		return fg(Elem.STRING).a(s, true);
	}

	/** Consider each char individually, and if replacement \ufffd char then add some red colour.
	 */
	private AaAnsi a(String s, boolean styled) {
		if (s == null) return this;
//		StringBuilder sb = new StringBuilder();
		AaAnsi aa = new AaAnsi(false);
//		if (styled) aa.fg(Elem.STRING);
		boolean insideNumStyle = false;  // these two vars are for my "styled string" code below
		boolean insideWordStyle = false;
		
		for (int i=0; i<s.length(); i++) {
			char c = s.charAt(i);
//			incChar();
			if (c < 0x20 || c == 0x7f) {  // special handling of control characters, make them visible
				if (c == 0x09 || c == 0x0a || c == 0x0d || c == 0x1b) {  // tab, line feed, carriage return, escape... leave alone
//					sb.append(c);
					aa.a(c);
				} else if (c == 0) {
//					sb.append('∅');  // make NULL more visible
					aa.a('∅');
					if (i != s.length()-1) controlChars++;  // if not at very end of string, then count it (shouldn't have null mid-string)
				} else {
//					sb.append("·");  // all other control chars
					aa.a('·');  // all other control chars
					controlChars++;
				}
			} else if (c == '\ufffd') {  // the replacement char introduced when trying to parse the bytes with the specified charset
				replacementChars++;
				if (isOn()) {  // bright red background, upsidedown ?
					Ansi a = new Ansi().reset().bg(Elem.ERROR.getCurrentColor().value).fg(231).a('¿').bgDefault().fg(curElem.getCurrentColor().value);
//					sb.append(a.toString());
					aa.aRaw(a.toString(), 1);
				} else {
//					sb.append('¿');
					aa.a('¿');
				}
			} else {  // all good, normal char
				if (styled && isOn()) {  // styled is for normal strings, we'll do some colour coding to make it look cooler
					if (Character.isMirrored(c) || c == '\'' || c == '"') {  // things like () {} [] 
						AaAnsi a = new AaAnsi().fg(Elem.BRACE).a(c).fg(Elem.STRING);
//						sb.append(a.toString());
//						aa.a(a);
						aa.fg(Elem.BRACE).a(c).fg(Elem.STRING);
						insideNumStyle = false;
					} else if ((c == ',' || c == ';' || c == '.' || c == ':' || c == '-') && i < s.length()-1) {  // punctuation, and not at the very last char
						if (insideNumStyle && Character.isDigit(s.charAt(i+1))) {  // if we're in a number, and the next char is a number, keep the orange colour
//							sb.append(c);
							aa.a(c);
						} else if (Character.isWhitespace(s.charAt(i+1)) || c == ',' || c == ';') {
							// let's change , ; . : - to default colour as long as the next char is whitespace
							AaAnsi a = new AaAnsi().reset().a(c).fg(Elem.STRING);
//							sb.append(a.toString());
//							aa.a(a);
							aa.reset().a(c).fg(Elem.STRING);
							insideNumStyle = false;
						} else {
//							sb.append(c);
							aa.a(c);
						}
					} else if (Character.isDigit(c)) {  // digits 0-9 in ASCII
						if (!insideNumStyle) {  // if we're not inside a number, then change colour
//							sb.append(new AaAnsi().fg(Elem.NUMBER));
							aa.fg(Elem.NUMBER);
							insideNumStyle = true;
						}
//						sb.append(c);
						aa.a(c);
					} else {
 						if (insideNumStyle) {
//							sb.append(new AaAnsi().fg(Elem.STRING));
							aa.fg(Elem.STRING);
							insideNumStyle = false;
						}
//						sb.append(c);
						aa.a(c);
					}
				} else {  // not styled text, just normal append
//					sb.append(c);
					aa.a(c);
				}
			}
		}
		a(aa);
		return this;
	}

	public AaAnsi reset() {
		if (isOn()) {
			jansi.reset();
			if (Elem.DEFAULT.getCurrentColor().value != -1) jansi.fg(Elem.DEFAULT.getCurrentColor().value);
		}
		return this;
	}

	/** This one actually resets to the ANSI default, rather than (perhaps) my own custom default colour */
	public static void resetAnsi(PrintStream out) {
		out.print(new Ansi().reset().toString());
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
//		AnsiConsole.out().println(jansi);
		System.out.println(ansi.toString());
//		System.out.println(new AaAnsi().setSolaceGreen().a("AARONSOLACEMANUAL").reset());
		
	}

}
