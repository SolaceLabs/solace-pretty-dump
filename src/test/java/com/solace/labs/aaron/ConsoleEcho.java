package com.solace.labs.aaron;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class ConsoleEcho {

	
	public static void main(String... args) throws IOException, InterruptedException {
		
		
//		System.out.println("starting...");
////		BufferedInputStream bim = new BufferedInputStream(System.in);
//		InputStreamReader reader = new InputStreamReader(System.in);
//		BufferedReader in = new BufferedReader(reader);
//		
//		while (true) {
//			
//			String input = in.readLine();
//			if (input == null) {  // nothing to do
//				Thread.sleep(50);
//			} else {
//				System.out.println("con: " + input);
//			}
//		}
		
		InputStreamReader reader = new InputStreamReader(System.in);
		

		while (true) {
			
			int a = reader.read();
			System.out.println(a);
			
//			if (System.in.available() > 0) {
//				int c = System.in.read();
//				System.out.println(c);
//			}
			
			
			
		}
		
		
		
		
	}
}
