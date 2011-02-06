package net.diva.browser.service;

@SuppressWarnings("serial")
public class OperationFailedException extends Exception {
	public OperationFailedException() {
		super();
	}

	public OperationFailedException(String detailMessage, Throwable throwable) {
		super(detailMessage, throwable);
	}

	public OperationFailedException(String detailMessage) {
		super(detailMessage);
	}

	public OperationFailedException(Throwable throwable) {
		super(throwable);
	}
}
