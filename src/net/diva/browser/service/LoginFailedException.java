package net.diva.browser.service;

@SuppressWarnings("serial")
public class LoginFailedException extends Exception {
	public LoginFailedException() {
	}

	public LoginFailedException(String detailMessage) {
		super(detailMessage);
	}

	public LoginFailedException(Throwable throwable) {
		super(throwable);
	}

	public LoginFailedException(String detailMessage, Throwable throwable) {
		super(detailMessage, throwable);
	}
}
