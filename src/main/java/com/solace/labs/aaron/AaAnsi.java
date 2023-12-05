package com.solace.labs.aaron;

import org.fusesource.jansi.Ansi;

public class AaAnsi extends Ansi {
	
	private final boolean withColor = true;
	
	public AaAnsi(boolean withColor) {
		super();
//		this.withColor = withColor;
		if (!this.withColor) super.reset();
	}

	@Override
	public Ansi fgBlue() {
		if (withColor) return super.fgBlue();
		return this;
	}

	@Override
	public Ansi fgCyan() {
		if (withColor) return super.fgCyan();
		return this;
	}

	@Override
	public Ansi fgGreen() {
		if (withColor) return super.fgGreen();
		return this;
	}

	@Override
	public Ansi fgMagenta() {
		if (withColor) return super.fgMagenta();
		return this;
	}

	@Override
	public Ansi fgRed() {
		if (withColor) return super.fgRed();
		return this;
	}

	@Override
	public Ansi fgYellow() {
		if (withColor) return super.fgYellow();
		return this;
	}

	@Override
	public Ansi fgBrightBlack() {
		if (withColor) return super.fgBrightBlack();
		return this;
	}

	@Override
	public Ansi reset() {
		if (withColor) return super.reset();
		return this;
	}
	
}
