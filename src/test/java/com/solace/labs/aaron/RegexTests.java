package com.solace.labs.aaron;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RegexTests {

	
	
	public static void main(String... args) {
		
		String
		key = "blahMs";
		
		String regex = "M[sS]";
		
		System.out.println(key.matches(regex));
		System.out.println(key.contains(regex));
		
		Pattern p = Pattern.compile(regex);
		Matcher m = p.matcher(key);
		System.out.println(m.find());
	
		key = "msDuration";
//		regex = "[^\\{Alpha}]ms";
		regex = "(?:^|[^a-zA-Z])ms(?:$|ec|[^a-zA-Z])";  // non-capturing group(either start or non-alpha) + ms + non-cg(either end or non-alpha)
		
		p = Pattern.compile(regex);
		m = p.matcher(key);
		System.out.println(m.find());
		
		String ansi = "\u001b[2;30m] color ";
		Pattern p2 = Pattern.compile("\u001b\\[[0-9; ]*m");
//		String ansi = "\u001b[2;30m] color ";
//		Pattern p2 = Pattern.compile("\u001b");
		System.out.println(p2);
		System.out.println(p2.matcher(ansi).find());
	
	}
}
