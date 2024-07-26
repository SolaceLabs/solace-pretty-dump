package com.solace.labs.aaron;

public class ThinkingAnsiHelper {

	private boolean isFilteringOn = false;
	
	private static final char[] LOOPS = new char[] { '━', '\\', '|', '/' };

	private int screenPosX = 0;
	private int charIndex = 0;
	private int rainbowIndex = 0;
	private static final String[] BACKSPACES = new String[64];
	static {
//		Ansi ansi = new Ansi();
		for (int i=0; i<BACKSPACES.length; i++) {
//			BACKSPACES[i] = UsefulUtils.pad(i, (char)8);
			BACKSPACES[i] = (char)27 + "[30D"; 
//			BACKSPACES[i] = ansi.cursorLeft(30).toString();
		}
//		System.out.println(Arrays.toString(BACKSPACES[1].getBytes()));
	}
	
	private void clearReset() {
//		if (screenPosX < BACKSPACES.length) System.out.print(BACKSPACES[screenPosX]);  // hardcoded
//		else System.out.print(UsefulUtils.pad(screenPosX, (char)8));
		screenPosX = 0;
		if (rainbowIndex == 0) charIndex = (charIndex + 1) % 4;
		rainbowIndex = (rainbowIndex + 1) % AaAnsi.rainbowTable.length;  // pretty colours
	}
	
	public boolean isFilteringOn() {
		return isFilteringOn;
	}
	
	public void filteringOff() {
		if (isFilteringOn) {
			isFilteringOn = false;
			clearReset();
		} // else nothing to do
	}
	
	public void tick2() {
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
	}
	
	
	public void tick() {
		if (!isFilteringOn) {
			isFilteringOn = true;
			AaAnsi.resetAnsi(System.out);
//			System.out.print(new AaAnsi(false).fg(AaAnsi.rainbowTable[(rainbowIndex++) % AaAnsi.rainbowTable.length]));
		}
		clearReset();
		String m = new StringBuilder("🔎 Skipping filtered msg #").append(PayloadHelper.msgCount).toString();
		screenPosX = m.length();
//		System.out.print(new AaAnsi(false).fg(AaAnsi.rainbowTable[rainbowIndex]).a(m));
		System.out.print(m);
		System.out.print(BACKSPACES[1]);
//		
//		
//		if (screenPosX >= 60 || screenPosX >= PayloadHelper.currentScreenWidth * 0.8 - 1) {  // time to rewind
//			clearReset();
//			System.out.print(new AaAnsi(false).fg(AaAnsi.rainbowTable[(rainbowIndex++) % AaAnsi.rainbowTable.length]));
//		} else {  // normal
//			System.out.print(LOOPS[charIndex]);
//			screenPosX++;
//		}
		
		
		
		
		
	}
	
}
