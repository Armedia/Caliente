package com.armedia.caliente.cli.history;

class HistoryException extends Exception {
	private static final long serialVersionUID = 1L;

	private final String identifier;

	public HistoryException(String identifier) {
		this.identifier = identifier;
	}

	public HistoryException(String identifier, String message) {
		super(message);
		this.identifier = identifier;
	}

	public HistoryException(String identifier, Throwable cause) {
		super(cause);
		this.identifier = identifier;
	}

	public HistoryException(String identifier, String message, Throwable cause) {
		super(message, cause);
		this.identifier = identifier;
	}

	public HistoryException(String identifier, String message, Throwable cause, boolean enableSuppression,
		boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
		this.identifier = identifier;
	}

	public String getIdentifier() {
		return this.identifier;
	}
}