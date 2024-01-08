package com.solace.labs.aaron;

public class Col {

	
	final int value;
	final boolean faint;
	
	public Col(int value) {
		this.value = value;
		faint = false;
	}
	
	public Col(int value, boolean faint) {
		this.value = value;
		this.faint = faint;
	}
	
	@Override
	public String toString() {
		return (faint ? "Faint " : "") + value;
	}
}
