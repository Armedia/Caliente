package com.armedia.caliente.engine.exporter;

import com.armedia.caliente.store.CmfObjectStore.LockStatus;

public enum ExportSkipReason {
	//
	UNSUPPORTED("Object archetype is disabled or unsupported"),
	ALREADY_LOCKED("Already locked", LockStatus.ALREADY_LOCKED),
	ALREADY_STORED("Already stored", LockStatus.ALREADY_STORED),
	ALREADY_FAILED("Already failed", LockStatus.ALREADY_FAILED),
	SKIPPED("Explicitly skipped"),
	DEPENDENCY_FAILED("A dependency failed to export"),
	//
	;

	public final String message;
	public final LockStatus lockStatus;

	private ExportSkipReason(String message) {
		this(message, null);
	}

	private ExportSkipReason(String message, LockStatus lockStatus) {
		this.message = message;
		this.lockStatus = lockStatus;
	}

	@Override
	public final String toString() {
		return this.message;
	}
}