package com.solace.labs.aaron;

import org.fusesource.jansi.Ansi;
import org.fusesource.jansi.AnsiConsole;

public class ConsoleSize {

	
	
	public static void main(String... args) throws InterruptedException {

		System.out.println(Ansi.isDetected());
		System.out.println(Ansi.isEnabled());

		AnsiConsole.systemInstall();
		
		
		Ansi ansi = new Ansi();
		
		System.out.println(Ansi.isDetected());
		System.out.println(Ansi.isEnabled());
		// cursor stuff doesn't work!  either linux or Cmd prompt
		ansi.saveCursorPosition();
		Thread.sleep(1000);
		ansi.cursorRight(10);
		AnsiConsole.out().print("1");
		Thread.sleep(1000);
		ansi.cursorLeft(2);
		AnsiConsole.out().print("2");
		Thread.sleep(1000);
		ansi.cursorLeft(2);
		AnsiConsole.out().print("3");
		Thread.sleep(1000);
		ansi.cursorLeft(2);
		AnsiConsole.out().print("4");
		Thread.sleep(1000);
		ansi.cursorLeft(2);
		AnsiConsole.out().print("5");
		Thread.sleep(1000);
		
		
		ansi.restoreCursorPosition();
		
		ansi.a("back at the beginning");
		
		System.out.println(ansi.toString());
		
		
		
	}
}
