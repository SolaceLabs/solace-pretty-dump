package com.solace.labs.aaron;

public class PrettyException extends Exception {

	public PrettyException(String s, Exception e) {
		super(s, e);
	}
	
	public PrettyException(Exception e) {
		super(e);
	}

	private static final long serialVersionUID = 1L;

	
}
