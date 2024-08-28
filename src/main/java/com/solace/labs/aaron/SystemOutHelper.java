package com.solace.labs.aaron;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SystemOutHelper {

	StringBuilder col = new StringBuilder();
	StringBuilder bw = new StringBuilder();
	
	public SystemOutHelper print(String s) {
		col.append(s);
		bw.append(s);
		return this;
	}
	
	public SystemOutHelper print(AaAnsi ansi) {
		col.append(ansi.toString());
		bw.append(ansi.toRawString());
		return this;
	}
	
	public SystemOutHelper println(String s) {
		col.append(s).append('\n');
		bw.append(s).append('\n');
		return this;
	}
	
	public SystemOutHelper println() {
		col.append('\n');
		bw.append('\n');
		return this;
	}
	
	public SystemOutHelper println(AaAnsi ansi) {
		col.append(ansi.toString()).append('\n');
		bw.append(ansi.toRawString()).append('\n');
		return this;
	}
	
	public boolean containsRegex(Pattern p) {
		Matcher m = p.matcher(bw.toString());
		return m.find();
	}
	
	@Override
	public String toString() {
		return col.toString();
	}
}
