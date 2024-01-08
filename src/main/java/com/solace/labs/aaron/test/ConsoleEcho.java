package com.solace.labs.aaron.test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class ConsoleEcho {

	
	public static void main(String... args) throws IOException, InterruptedException {
		
		
		System.out.println("starting...");
//		BufferedInputStream bim = new BufferedInputStream(System.in);
		InputStreamReader reader = new InputStreamReader(System.in);
		BufferedReader in = new BufferedReader(reader);
		
		while (true) {
			
			String input = in.readLine();
			if (input == null) {  // nothing to do
				Thread.sleep(50);
			} else {
				System.out.println("con: " + input);
			}
		}
		
		
		
		
		
		
	}
}
