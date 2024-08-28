package com.solace.labs.aaron;

import org.fusesource.jansi.AnsiConsole;

public class ThinkingAnsiHelper {

	private static boolean isFilteringOn = false;
	
//	private static final char[] LOOPS = new char[] { '━', '\\', '|', '/' };

	private static int lastStringsWidth = 0;
	private static final boolean isEnabled;
//	private static int charIndex = 0;
//	private static int rainbowIndex = 0;
	private static final String BACKSPACES;
	static {
		if (AnsiConsole.getTerminalWidth() == 0) {  // happens when running inside Eclipse
			isEnabled = false;
//			StringBuilder sb = new StringBuilder();
//			for (int i=0; i<200; i++) {
//				sb.append((char)8);  // a backspace char
//			}
//			BACKSPACES = sb.toString();
			BACKSPACES = "\n";
		} else {
			BACKSPACES = (char)27 + "[200D";  // 200 backspace chars
			isEnabled = true;
		}
	}
	static {
//		Ansi ansi = new Ansi();
//		for (int i=0; i<BACKSPACES.length; i++) {
//			BACKSPACES[i] = UsefulUtils.pad(i, (char)8);
//			BACKSPACES[i] = (char)27 + "[30D"; 
//			BACKSPACES[i] = ansi.cursorLeft(30).toString();
//		}
//		System.out.println(Arrays.toString(BACKSPACES[1].getBytes()));
	}
	
	private ThinkingAnsiHelper() {
		
	}
	
	private static void clearReset() {
		if (!isEnabled) return;
//		if (screenPosX < BACKSPACES.length) System.out.print(BACKSPACES[screenPosX]);  // hardcoded
//		else System.out.print(UsefulUtils.pad(screenPosX, (char)8));
//		if (rainbowIndex == 0) charIndex = (charIndex + 1) % 4;
//		rainbowIndex = (rainbowIndex + 1) % AaAnsi.rainbowTable.length;  // pretty colours
		System.out.print(UsefulUtils.indent(lastStringsWidth + 20));  // some buffer
		System.out.print(BACKSPACES);
		lastStringsWidth = 0;
	}
	
	public static boolean isFilteringOn() {
		return isFilteringOn;
	}
	
	public static void filteringOff() {
		if (isFilteringOn) {
			isFilteringOn = false;
			clearReset();
		} // else nothing to do
	}
	
/*	private void tick2() {
		if (!isFilteringOn) {
			isFilteringOn = true;
			System.out.print(new AaAnsi(false).fg(AaAnsi.rainbowTable[(rainbowIndex++) % AaAnsi.rainbowTable.length]));
		}
		if (screenPosX >= 60 || screenPosX >= PayloadHelper.currentScreenWidth * 0.8 - 1) {  // time to rewind
			clearReset();
			System.out.print(new AaAnsi(false).fg(AaAnsi.rainbowTable[(rainbowIndex++) % AaAnsi.rainbowTable.length]));
		} else {  // normal
			System.out.print(LOOPS[charIndex]);
			screenPosX++;
		}
	}*/
	
	private static final String THINKING_PATTERN_PRINT = "🔎 %sRecv'd=%d, Filtered=%d, Printed=%d" + BACKSPACES;
	private static final String THINKING_PATTERN_GATHER = "🔎 %sRecv'd=%d, Filtered=%d, Gathered=%d (%d max)" + BACKSPACES;
	
	public static String makeStringGathered(String optionalPreString, long numReceived, long numFiltered, long numGathered, int max) {
		if (optionalPreString == null || optionalPreString.isEmpty()) {
			return String.format(THINKING_PATTERN_GATHER, "", numReceived, numFiltered, numGathered, max);
		}
		return String.format(THINKING_PATTERN_GATHER, optionalPreString + " ", numReceived, numFiltered, numGathered, max);
	}

	public static String makeStringPrinted(String optionalPreString, long numReceived, long numFiltered, long numPrinted) {
		if (optionalPreString == null || optionalPreString.isEmpty()) {
			return String.format(THINKING_PATTERN_PRINT, "", numReceived, numFiltered, numPrinted);
		}
		return String.format(THINKING_PATTERN_PRINT, optionalPreString + " ", numReceived, numFiltered, numPrinted);
	}

	public static void tick2(String filterString) {
		if (!isFilteringOn) {
			isFilteringOn = true;
			AaAnsi.resetAnsi(System.out);
		}
		if (!isEnabled) return;  // don't print anything
		System.out.print(filterString);
		lastStringsWidth = filterString.length();  // so we can blank out the line when resetting
	}
}
