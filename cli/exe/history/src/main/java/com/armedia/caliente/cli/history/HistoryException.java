package com.armedia.caliente.cli.history;

import com.documentum.fc.common.IDfId;

class HistoryException extends Exception {
	private static final long serialVersionUID = 1L;

	private final IDfId chronicleId;

	public HistoryException(IDfId chronicleId) {
		this.chronicleId = chronicleId;
	}

	public HistoryException(IDfId chronicleId, String message) {
		super(message);
		this.chronicleId = chronicleId;
	}

	public HistoryException(IDfId chronicleId, Throwable cause) {
		super(cause);
		this.chronicleId = chronicleId;
	}

	public HistoryException(IDfId chronicleId, String message, Throwable cause) {
		super(message, cause);
		this.chronicleId = chronicleId;
	}

	public HistoryException(IDfId chronicleId, String message, Throwable cause, boolean enableSuppression,
		boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
		this.chronicleId = chronicleId;
	}

	public IDfId getChronicleId() {
		return this.chronicleId;
	}
}