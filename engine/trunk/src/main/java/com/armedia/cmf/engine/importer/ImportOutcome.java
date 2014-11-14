package com.armedia.cmf.engine.importer;

public class ImportOutcome {

	private final ImportResult result;
	private final String newId;
	private final String newLabel;

	public ImportOutcome(ImportResult result) {
		this(result, null, null);
	}

	/**
	 * @param result
	 * @param newId
	 * @param newLabel
	 */
	public ImportOutcome(ImportResult result, String newId, String newLabel) {
		this.result = result;
		this.newId = newId;
		this.newLabel = newLabel;
	}

	public final ImportResult getResult() {
		return this.result;
	}

	public final String getNewId() {
		return this.newId;
	}

	public final String getNewLabel() {
		return this.newLabel;
	}
}