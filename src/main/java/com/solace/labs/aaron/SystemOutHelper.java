package com.solace.labs.aaron;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SystemOutHelper {

	StringBuilder sb = new StringBuilder();
	StringBuilder raw = new StringBuilder();
	
	public SystemOutHelper print(String s) {
		sb.append(s);
		raw.append(s);
		return this;
	}
	
	public SystemOutHelper print(AaAnsi aa) {
		sb.append(aa.toString());
		raw.append(aa.toRawString());
		return this;
	}
	
	public SystemOutHelper println(String s) {
		sb.append(s).append('\n');
		raw.append(s).append('\n');
		return this;
	}
	
	public SystemOutHelper println() {
		sb.append('\n');
		raw.append('\n');
		return this;
	}
	
	public SystemOutHelper println(AaAnsi aa) {
		sb.append(aa.toString()).append('\n');
		raw.append(aa.toRawString()).append('\n');
		return this;
	}
	
	public boolean containsRegex(Pattern p) {
//		Pattern p = Pattern.compile(regex, Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
		Matcher m = p.matcher(raw.toString());
		
//		Matcher m = p.matcher("hello aaron world");
		return m.find();
	}
	
	@Override
	public String toString() {
		return sb.toString();
	}
}
