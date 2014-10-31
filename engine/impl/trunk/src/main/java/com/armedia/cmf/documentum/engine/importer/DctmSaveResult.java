package com.armedia.cmf.documentum.engine.importer;

import com.armedia.cmf.engine.importer.ImportResult;
import com.armedia.commons.utilities.Tools;

public final class DctmSaveResult {
	private final ImportResult cmsImportResult;
	private final String objectLabel;
	private final String objectId;

	private DctmSaveResult(ImportResult cmsImportResult, String objectLabel, String objectId) {
		this.cmsImportResult = cmsImportResult;
		this.objectLabel = objectLabel;
		this.objectId = objectId;
	}

	public ImportResult getResult() {
		return this.cmsImportResult;
	}

	public String getObjectLabel() {
		return this.objectLabel;
	}

	public String getObjectId() {
		return this.objectId;
	}

	@Override
	public int hashCode() {
		return Tools.hashTool(this, null, this.cmsImportResult, this.objectLabel, this.objectId);
	}

	@Override
	public boolean equals(Object obj) {
		if (!Tools.baseEquals(this, obj)) { return false; }
		DctmSaveResult other = DctmSaveResult.class.cast(obj);
		if (!Tools.equals(this.cmsImportResult, other.cmsImportResult)) { return false; }
		if (!Tools.equals(this.objectLabel, other.objectLabel)) { return false; }
		if (!Tools.equals(this.objectId, other.objectId)) { return false; }
		return true;
	}

	@Override
	public String toString() {
		return String.format("SaveResult [cmsImportResult=%s, objectLabel=%s, objectId=%s]", this.cmsImportResult,
			this.objectLabel, this.objectId);
	}
}