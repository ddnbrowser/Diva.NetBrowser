package net.diva.browser.service;

@SuppressWarnings("serial")
public class NoLoginException extends Exception {
	public NoLoginException() {
	}

	public NoLoginException(String detailMessage) {
		super(detailMessage);
	}

	public NoLoginException(Throwable throwable) {
		super(throwable);
	}

	public NoLoginException(String detailMessage, Throwable throwable) {
		super(detailMessage, throwable);
	}
}
