package com.armedia.cmf.engine.exporter;

public enum ExportSkipReason {
	//
	UNSUPPORTED("Object archetype is disabled or unsupported"),
	ALREADY_LOCKED("Already locked"),
	ALREADY_STORED("Already stored"),
	SKIPPED("Explicitly skipped"),
	//
	;

	public final String message;

	private ExportSkipReason(String message) {
		this.message = message;
	}

	@Override
	public final String toString() {
		return this.message;
	}
}