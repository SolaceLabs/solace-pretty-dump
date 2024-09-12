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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.fusesource.jansi.Ansi;
import org.fusesource.jansi.Ansi.Attribute;
import org.fusesource.jansi.AnsiConsole;

import com.solace.labs.aaron.utils.BoundedLinkedList;

/**
 * Kind of a wrapper around the JAnsi library.  Modified for my own uses.
 * @author Aaron Lee
 */
public class AaAnsi /* implements CharSequence */ {

	enum ColorMode {
		OFF,
		MINIMAL,
		STANDARD,
		VIVID,
		LIGHT,
		MATRIX,
		;
	}

	static ColorMode MODE = ColorMode.STANDARD;
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
	
	private static final Logger logger = LogManager.getLogger(AaAnsi.class);
	private Ansi jansi = new Ansi();
	private StringBuilder rawSb = new StringBuilder();
//	private StringBuilder rawCompressedSb = new StringBuilder();
//	private char lastRawCompressedChar = '-';  // used to strip multiple spaces
	private boolean insideEscapeCode = false;  // needed at the class level since we add chars in different methods
//	private int charCount = 0;
	private Elem curElem = null;  // used to track which element type we're inside (useful for switching colours back in styleString append)
	private int controlChars = 0;  // we'll turn this to true if we get too many control chars
	private int replacementChars = 0;

	
	// needed for CharSequence apparently..?  Dunno if I ever need these
/*	@Override
	public char charAt(int index) {
//		assert false;
		return rawSb.charAt(index);
	}

	@Override
	public CharSequence subSequence(int start, int end) {
		assert false;
		return rawSb.subSequence(start, end);
	}
	///////////////////
	*/
	private AaAnsi() {
		reset();
	}

	public AaAnsi(boolean reset) {
		if (reset) reset();
	}

//	private void incChar(int amount) {
////		charCount += amount;
//	}
//	
//	private void incChar() {
////		charCount ++;
//	}
//	
	private boolean isOn() {
		return (MODE != ColorMode.OFF);
	}

	public int getTotalCharCount() {
//		return charCount;
		return rawSb.length();
	}
	
	public int getControlCharsCount() {
		return controlChars;
	}
	
	public int getReplacementCharsCount() {
		return replacementChars;
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

	public static AaAnsi colorizeTopic(String topic) {
		return AaAnsi.n().colorizeTopic(topic, -1);
	}
		
	public AaAnsi colorizeTopic(String topic, int highlight) {
		/*
		 * if (MODE == ColorMode.VIVID) return colorizeTopicRainbow(topic,highlight);
		 * else
		 */ if (MODE == ColorMode.OFF) return a(topic);
		else return colorizeTopicPlain(topic, highlight).reset();
	}
	
	private AaAnsi colorizeTopicPlain(String topic, int highlight) {
		String[] levels = topic.split("/", -1);
		maxLengthTopicLevels.add(levels.length);
		if (highlight > 0) faintOn();
		for (int i=0; i<levels.length; i++) {
//			incChar(levels[i].length());   // need to update manually since adjusting the jansi directly
			if (i == highlight) {
				jansi.reset();  // back to bright!
			}
			fg(figureColor(i, highlight));
			if (i == highlight) {
				int firstDot = levels[i].indexOf('·');// ('⋅');
				if (firstDot == -1) {
					jansi.a(levels[i]);
					rawSb.append(levels[i]);
//					rawCompressedSb.append(levels[i]);
					faintOn();
				} else {
					jansi.a(levels[i].substring(0,firstDot));
					rawSb.append(levels[i].substring(0,firstDot));
//					rawCompressedSb.append(levels[i].substring(0,firstDot));
					faintOn();
					jansi.a(levels[i].substring(firstDot));
					rawSb.append(levels[i].substring(firstDot));
//					rawCompressedSb.append(levels[i].substring(firstDot));
				}
			} else {
				int firstDot = levels[i].indexOf('·');//('⋅');
				if (firstDot == -1) {
					jansi.a(levels[i]);
					rawSb.append(levels[i]);
//					rawCompressedSb.append(levels[i]);
				} else {
					jansi.a(levels[i].substring(0,firstDot));
					rawSb.append(levels[i].substring(0,firstDot));
//					rawCompressedSb.append(levels[i].substring(0,firstDot));
					faintOn();
					jansi.a(levels[i].substring(firstDot));
					rawSb.append(levels[i].substring(firstDot));
//					rawCompressedSb.append(levels[i].substring(firstDot));
				}
			}
			if (i < levels.length-1) {
				if (highlight == -1) faintOff();// reset();
				fg(Elem.TOPIC_SEPARATOR, true).a('/');  // this does the charCount!  and the rawSb
			}
		}
		return this;
	}
	
	// https://en.wikipedia.org/wiki/ANSI_escape_code
	final static int[] topicColorTable = new int[] {
			82, 83, 84, 85, 86,
//			87, 81, 75, 69, 63, /*57,*/
			87, 81, 75, 69, 63, 99, 135, 171, 207, 206, 205, 204,
//			/* 93, */ 129, 165, 201,
			
//            200, 199, 198, 197,
			203, 202, 208, 214, 220, 226,
//            203, 209, 215, 221,
			/* 227, */ 191, 155, 119
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
	
	// this is used for vivid colouring, to spread the spectrum out more when few topic levels
	private static BoundedLinkedList.ComparableList<Integer> maxLengthTopicLevels = new BoundedLinkedList.ComparableList<>(200);;
	
	private int figureColor(int i, int highlight) {
		if (MODE == ColorMode.VIVID) {
			// https://github.com/topics/256-colors
			int startingColIndex = 5;  // cyan 86
	//		int startingColIndex = 7;  // temp 75
//			double step = Math.max(1, Math.min(3, topicColorTable.length * 1.0 / Math.max(1, maxLengthTopicLevels.getMax())));
			double step = Math.max(1, Math.min(4, topicColorTable.length * 1.0 / Math.max(1, maxLengthTopicLevels.max())));
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
//		if (isOn()) {
			fg(Elem.ERROR, true).a(s).forceReset();
//		} else {
//			jansi.a(s);
//			incChar(s.length());
//		}
		return this;
	}

	public AaAnsi warn(String s) {
//		if (isOn()) {
			fg(Elem.WARN, true).a(s).forceReset();
//		} else {
//			jansi.a(s);
//			incChar(s.length());
//		}
		return this;
	}

	public AaAnsi ex(Exception e) {
		String exception = e.getClass().getSimpleName() + " - " + e.getMessage();
		return invalid(exception);
	}

	public AaAnsi ex(String s, Exception e) {
		String exception = String.format("%s: %s - %s", s, e.getClass().getSimpleName(), e.getMessage());
		return invalid(exception);
	}

	public AaAnsi fg(Elem elem) {
		return fg(elem, false);
	}
	
	private AaAnsi fg(Elem elem, boolean force) {
		if (isOn() || force) {
			Col newCol = elem.getCurrentColor();
//			if (c.faint) {
//				faintOn().fg(c.value, force);
//			} else if (c.italics) {
//				makeItalics().fg(c.value, force);
//			} else {
//				fg(c.value, force);
//			}
//			if ()
			if (force || curElem == null || curElem.getCurrentColor().value != newCol.value) fg(newCol.value, force);
			if (newCol.faint) {
				faintOn();
//			} else if (curElem == null || curElem.getCurrentColor().faint) {
			} else if (curElem != null && curElem.getCurrentColor().faint) {
				faintOff();
			}
			if (newCol.italics) {
				italicsOn();
//			} else if (curElem == null || curElem.getCurrentColor().italics) {
			} else if (curElem != null && curElem.getCurrentColor().italics) {
				italicsOff();
			}
		}
		curElem = elem;
		return this;
	}

	public AaAnsi fg(int colorIndex) {
		return fg(colorIndex, false);
	}
	
	public AaAnsi fg(int colorIndex, boolean force) {
		if (isOn() || force) {
			if (colorIndex == -1) jansi.fgDefault();
			else jansi.fg(colorIndex);
		}
		return this;
	}
	
//	AaAnsi makeFaint() {
//		if (isOn()) {
//			/* if (faint)*/ jansi.a(Attribute.INTENSITY_FAINT);
////			else jansi.a(Attribute.INTENSITY_BOLD_OFF);  // doesn't work.  For some reason there is no "faint off" ..!?
//		}
//		return this;
//	}

//	public AaAnsi makeItalics() {
//		if (isOn()) {
//			jansi.a(Attribute.ITALIC);
//		}
//		return this;
//	}
	
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
			jansi.a(Attribute.INTENSITY_BOLD_OFF);
		}
		return this;
	}

	// TODO fix this!!!!
	public int length() {
		int len = calcLength(this.toString());
		if (len != rawSb.length()) {
			logger.error(String.format("AaAnsi len and rawSb are not the same: %d vs %d", len, rawSb.length()));
		}
		assert len == rawSb.length();
		return rawSb.length();
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
			} else if (insideEscape) {
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
	
	/** Need to reset() after this call, returned string is not reset */
	String chop(int len) {
//		if (len < 0 ) {
//			return "";
//		}
		if (getTotalCharCount() <= len) return toString();
//		if (s == null && s.isEmpty()) return s;  // so now we know there's at least one char
		final String s = toString();
		StringBuilder sb = new StringBuilder();
		int count = 0;
		int pos = 0;
		boolean insideEscape = false;
		do {
			char c = s.charAt(pos);
			if (c == 27 || insideEscape) {  // we know we can add this char without changing the length
				sb.append(c);
				if (c == 'm') insideEscape = false;
				else insideEscape = true;
			} else {
				if (count < len) {  // still have room
					sb.append(c);
				}
				count++;
			}
//			sb.append(s.charAt(pos));
//			if (s.charAt(pos) == 27) {
//				insideEscape = true;
//			} else if (insideEscape) {
//				if (s.charAt(pos) == 'm') {
//					insideEscape = false;
//				}
//			} else {
//				count++;
//			}
			pos++;
		} while (pos < s.length() && (count <= len || insideEscape));
		if (pos < s.length()) return sb.toString();//.append(AaAnsi.n()).toString();  // append a reset() to my sb
		else {
			logger.warn("FYI, AaAnsi just ended up in the final else block, and shouldn't have.  Len == "+len+" and this is "+this.toString());
			return sb.append(AaAnsi.n()).toString();  // the whole thing   ... this should be impossible now due to implementing charCount
		}
		
	}
	
	String trim(int len) {
		if (len <= 0 ) return "";
//		if (len == 1 ) return "…";
		if (getTotalCharCount() <= len) return toString();
		StringBuilder sb = new StringBuilder();
		return sb.append(chop(len-1)).append('…').append(AaAnsi.n()).toString();
//		final String s = toString();
//		int count = 0;
//		int pos = 0;
//		boolean insideEscape = false;
//		do {
//			sb.append(s.charAt(pos));
//			if (s.charAt(pos) == 27) {
//				insideEscape = true;
//			} else if (insideEscape) {
//				if (s.charAt(pos) == 'm') {
//					insideEscape = false;
//				}
//			} else {
//				count++;
//			}
//			pos++;
//		} while (pos < s.length() && (count < len-1 || insideEscape));
//		if (pos < s.length()) return sb.append("…").append(AaAnsi.n()).toString();  // append a reset() to my sb
//		else {
//			logger.warn("FYI, AaAnsi just ended up in the final else block, and shouldn't have.  Len == "+len+" and this is "+this.toString());
//			return sb.append(AaAnsi.n()).toString();  // the whole thing   ... this should be impossible now due to implementing charCount
//		}
	}

	@Override
	public String toString() {
//		if (rawSb.length() != charCount) {
//			logger.warn(String.format("Had a mismatched charCount rawSb.length=%d, charCount=%d, ansi=%s", rawSb.length(), charCount, jansi));
//			assert rawSb.length() == charCount;
//		}
//		return rawSb.toString();  // plain mode
//		return rawCompressedSb.toString();  // plain mode
		return jansi.toString();
	}
	
	public String toRawString() {
		return rawSb.toString();
	}

//	public String toCompressedRawString() {
//		return rawCompressedSb.toString();
//	}

	/** Just jam is straight in, don't parse at all! */
	private AaAnsi aRaw(String s, String raw) {
		jansi.a(s);
		rawSb.append(raw);
//		rawCompressedSb.append(raw);
//		lastRawCompressedChar = raw.equals("") ? lastRawCompressedChar : raw.charAt(raw.length()-1);
//		charCount += raw.length();
		return this;
	}

	/** Just copy the whole AaAnsi into this one. */
	public AaAnsi aa(AaAnsi ansi) {
		jansi.a(ansi.toString());
		if (curElem != null && (ansi.curElem == null || (ansi.curElem != null && curElem != ansi.curElem))) {
			fg(curElem.getCurrentColor().value);  // put it back to whatever color it was before
		} else if (MODE == ColorMode.MATRIX) {  // gonna make some wacky colours!
			fg(Elem.STRING.getCurrentColor().value);  // doesn't matter what type
			// comment this out if we're using the real palette and not random colors
		}
		controlChars += ansi.controlChars;
		replacementChars += ansi.replacementChars;
		rawSb.append(ansi.rawSb);
//		if (ansi.rawCompressedSb.length() > 0 && ansi.rawCompressedSb.charAt(0) == ' ' && lastRawCompressedChar == ' ') {
//			rawCompressedSb.append(ansi.rawCompressedSb.substring(1));
//		} else {
//			rawCompressedSb.append(ansi.rawCompressedSb);
//		}
//		lastRawCompressedChar = ansi.lastRawCompressedChar;
		insideEscapeCode = ansi.insideEscapeCode;  // possibly inside an escape code, but I doubt it!
		return this;
	}
	
	public AaAnsi a(char c) {
		jansi.a(c);
		rawSb.append(c);
//		if (!(c == 0x09 || c == 0x0a || c == 0x0c || c == 0x0d || c == 0x1b || c == '·')) {
//			if (c == ' ') {
//				if (lastRawCompressedChar == ' ') { // do nothing
//				} else rawCompressedSb.append(c);
//			} else {
//				rawCompressedSb.append(c);
//			}
//			lastRawCompressedChar = c;
//		}
//		incChar();
		return this;
	}

	public AaAnsi a(boolean b) {
		String bs = Boolean.toString(b);
		jansi.a(bs);
		rawSb.append(bs);
//		rawCompressedSb.append(bs);
//		lastRawCompressedChar = 'e';  // doesn't matter, as long as not space
//		incChar(bs.length());
		return this;
	}

	public AaAnsi a(int i) {
		String is = Integer.toString(i);
		jansi.a(is);
		rawSb.append(is);
//		rawCompressedSb.append(bs);
//		lastRawCompressedChar = 'e';  // doesn't matter, as long as not space
//		incChar(is.length());
		return this;
	}

	public AaAnsi a(String s) {
		return a(s, false);
	}
	
	public AaAnsi aStyledString(String s) {
		return fg(Elem.STRING).a(s, true);
	}
	
//	private static final String BLACK_SPACE = new Ansi().reset().bg(0).fg(Elem.DESTINATION.getCurrentColor().value).fg(7).a('│').bgDefault().toString();
	private static final String BLACK_SPACE = new Ansi().reset().bg(0).a(' ').bgDefault().toString();
	public AaAnsi insertBlackSpace() {
		this.aRaw(BLACK_SPACE, " ");
		return this;
	}
	
	public AaAnsi blackOn() {
		this.aRaw(new Ansi().bg(0).toString(), "");
		return this;
	}
	
	public AaAnsi blackOff() {
		this.aRaw(new Ansi().bgDefault().toString(), "");
		return this;
	}
	

	/** Consider each char individually, and if replacement \ufffd char then add some red colour.
	 */
	private AaAnsi a(String s, boolean styled) {
		if (s == null) return this;
		AaAnsi aa = new AaAnsi(false);
		boolean insideNumStyle = false;  // these two vars are for my "styled string" code below
		boolean insideSymbolStyle = false;
//		boolean insideWordStyle = false;
		for (int i=0; i<s.length(); i++) {
			char c = s.charAt(i);
			if (c < 0x20 || c == 0x7f) {  // special handling of control characters, make them visible
				if (insideNumStyle || insideSymbolStyle) {  // this cannot be true if styled == false
					insideNumStyle = false;
					insideSymbolStyle = false;
					aa.fg(Elem.STRING);
				}
				if (c == 0x09 || c == 0x0a || c == 0x0c || c == 0x0d /* || c == 0x1b */) {  // tab, line feed, form feed, carriage return, escape... leave alone
//					assert c != 0x1b;  // could be binary!?
					aa.a(c);
				} else if (c == 0x1b) {
					aa.a("^[");
				} else if (c == 0) {
//					aa.a('␀');  // null char
					aa.a('∅');  // empty set
					if (i != s.length()-1) controlChars++;  // if not at very end of string, then count it (shouldn't have null mid-string)
				} else if (c == 26) {   // aka 0x01  https://en.wikipedia.org/wiki/C0_and_C1_control_codes
//					aa.a('☐');  // used in some testing character encoding replacements to make more obvious
					aa.a('¿');
					controlChars++;
				} else {
					aa.a('·');  // all other control chars
					controlChars++;
				}
			} else if (c == '\ufffd') {  // the replacement char introduced when trying to parse the bytes with the specified charset
				replacementChars++;
				if (isOn()) {  // bright red background, upsidedown ?
					if (curElem == null) {
//						System.out.println("curElem is null.  This is what I have so far:");
//						System.out.println(jansi.toString());
					}
//					Ansi a = new Ansi().reset().bg(Elem.ERROR.getCurrentColor().value).fg(231).a('¿').bgDefault();
					Ansi a = new Ansi().reset().bg(160).fg(231).a('¿').bgDefault();
					if (curElem != null) {
						a.fg(curElem.getCurrentColor().value);
					} else {
						a.fg(Elem.DEFAULT.getCurrentColor().value);
					}
					aa.aRaw(a.toString(), "¿");
				} else {
					aa.a('¿');
				}
			} else {  // all good, normal char
				if (styled && isOn()) {  // styled is for normal strings, we'll do some colour coding to make it look cooler
					if (Character.isMirrored(c) || c == '\'' || c == '"') {  // things like () {} [] 
						aa.fg(Elem.BRACE).a(c).fg(Elem.STRING);
						insideNumStyle = false;
						insideSymbolStyle = false;
					} else if (c == ',' || c == ';' || c == '.' || c == ':' || c == '-' || c == '?' || c == '!') {  // punctuation, and not at the very last char
						if (insideNumStyle && i < s.length()-1 && Character.isDigit(s.charAt(i+1))) {  // if we're in a number, and the next char is a number, keep the orange colour
							aa.a(c);
						} else if ((i < s.length()-1 && Character.isWhitespace(s.charAt(i+1))) || i == s.length()-1) { // || c == ',' || c == ';') {
							// let's change , ; . : - to default colour as long as the next char is whitespace or end
							aa.reset().a(c).fg(Elem.STRING);
							insideNumStyle = false;
							insideSymbolStyle = false;
						} else {
							aa.a(c);
						}
					} else if (Character.isDigit(c)) {  // digits 0-9 in ASCII
						if (!insideNumStyle) {  // if we're not inside a number, then change colour
							aa.fg(Elem.NUMBER);
							insideNumStyle = true;
							insideSymbolStyle = false;
						}
						aa.a(c);
					} else if (c == '/' || c == '*' || c == '+') {
						if (!insideSymbolStyle) {  // if we're not inside a symbol, then change colour
							aa.fg(Elem.KEY);
							insideNumStyle = false;
							insideSymbolStyle = true;
						}
						aa.a(c);
					} else {
 						if (insideNumStyle || insideSymbolStyle) {
							aa.fg(Elem.STRING);
							insideNumStyle = false;
							insideSymbolStyle = false;
						}
						aa.a(c);
					}
				} else {  // not styled text, just normal append
					aa.a(c);
				}
			}
		}
		aa(aa);
		return this;
	}

	public AaAnsi reset() {
		if (isOn()) {
			jansi.reset();
			if (Elem.DEFAULT.getCurrentColor().value != -1) jansi.fg(Elem.DEFAULT.getCurrentColor().value);
			curElem = null;
		}
		return this;
	}
	
	/** Even if not "on", used by warn() and invalid() b/c they force colour anyway */
	private AaAnsi forceReset() {
		jansi.reset();
		if (Elem.DEFAULT.getCurrentColor().value != -1) jansi.fg(Elem.DEFAULT.getCurrentColor().value);
		curElem = null;
		return this;
	}

	/** This one actually resets to the ANSI default, rather than (perhaps) my own custom default colour */
	public static void resetAnsi(PrintStream out) {
		out.print(new Ansi().reset().toString());
	}
	
	
	// used to be main
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
