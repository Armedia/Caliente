package com.armedia.caliente.engine.importer;

public class ImportOutcome {

	public static final ImportOutcome SKIPPED = new ImportOutcome(ImportResult.SKIPPED);
	public static final ImportOutcome FAILED = new ImportOutcome(ImportResult.FAILED);
	public static final ImportOutcome IGNORED = new ImportOutcome(ImportResult.IGNORED);
	public static final ImportOutcome DUPLICATE = new ImportOutcome(ImportResult.DUPLICATE);

	private final ImportResult result;
	private final String newId;
	private final String newLabel;

	private ImportOutcome(ImportResult result) {
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

	@Override
	public String toString() {
		return String.format("ImportOutcome [result=%s, newId=%s, newLabel=%s]", this.result, this.newId,
			this.newLabel);
	}
}