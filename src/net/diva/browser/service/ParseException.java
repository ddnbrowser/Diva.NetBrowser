package net.diva.browser.service;

@SuppressWarnings("serial")
public class ParseException extends Exception {
	public ParseException() {
	}

	public ParseException(String detailMessage) {
		super(detailMessage);
	}

	public ParseException(Throwable throwable) {
		super(throwable);
	}

	public ParseException(String detailMessage, Throwable throwable) {
		super(detailMessage, throwable);
	}
}
