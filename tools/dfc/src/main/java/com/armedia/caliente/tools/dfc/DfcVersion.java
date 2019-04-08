package com.armedia.caliente.tools.dfc;

import org.apache.commons.lang3.time.DateFormatUtils;

import com.armedia.commons.utilities.Tools;
import com.documentum.fc.client.IDfSysObject;
import com.documentum.fc.common.DfException;
import com.documentum.fc.common.DfId;
import com.documentum.fc.common.DfTime;
import com.documentum.fc.common.IDfId;
import com.documentum.fc.common.IDfTime;

public final class DfcVersion<T extends IDfSysObject> implements Comparable<DfcVersion<T>> {
	private final DfcVersionHistory<T> history;
	private final IDfId id;
	private final IDfId antecedentId;
	private final IDfTime creationDate;
	private final DfcVersionNumber versionNumber;
	private final T object;

	DfcVersion(DfcVersionHistory<T> history, DfcVersionNumber versionNumber, T object) throws DfException {
		this.history = history;
		this.versionNumber = versionNumber;
		this.object = object;
		this.id = object.getObjectId();
		this.creationDate = Tools.coalesce(object.getCreationDate(), DfTime.DF_NULLDATE);
		this.antecedentId = object.getAntecedentId();
	}

	DfcVersion(DfcVersionHistory<T> history, DfcVersionNumber versionNumber, IDfId id, IDfTime creationDate,
		IDfId antecedentId) throws DfException {
		this.history = history;
		this.versionNumber = versionNumber;
		this.object = null;
		this.id = Tools.coalesce(id, DfId.DF_NULLID);
		this.creationDate = Tools.coalesce(creationDate, DfTime.DF_NULLDATE);
		this.antecedentId = antecedentId;
	}

	public DfcVersionHistory<T> getHistory() {
		return this.history;
	}

	public boolean hasObject() {
		return (this.object != null);
	}

	public T getObject() {
		return this.object;
	}

	public IDfId getId() {
		return this.id;
	}

	public IDfId getAntecedentId() {
		return this.antecedentId;
	}

	public IDfTime getCreationDate() {
		return this.creationDate;
	}

	public DfcVersionNumber getVersionNumber() {
		return this.versionNumber;
	}

	@Override
	public int compareTo(DfcVersion<T> o) {
		if (o == null) { return 1; }
		if (equals(o)) { return 0; }

		// If they're siblings, resort to version numbering
		if (this.versionNumber.isSibling(o.versionNumber)) { return this.versionNumber.compareTo(o.versionNumber); }

		// First, check hierarchy
		if (this.versionNumber.isAntecedentOf(o.versionNumber)) { return -1; }
		if (this.versionNumber.isSuccessorOf(o.versionNumber)) { return 1; }
		if (this.versionNumber.isAncestorOf(o.versionNumber)) { return -1; }
		if (this.versionNumber.isDescendantOf(o.versionNumber)) { return 1; }

		// if there is no familial (hierarchy or peer) relationship, so fall back to chronology
		final int dateResult = this.creationDate.compareTo(o.creationDate);
		if (dateResult != 0) { return dateResult; }

		// No familial or temporal relationship...so can't establish an order between them...
		// sort by whomever's version number is "earliest"
		return this.versionNumber.compareTo(o.versionNumber);
	}

	@Override
	public int hashCode() {
		return Tools.hashTool(this, null, this.versionNumber);
	}

	@Override
	public boolean equals(Object obj) {
		if (!Tools.baseEquals(this, obj)) { return false; }
		@SuppressWarnings("unchecked")
		DfcVersion<T> other = (DfcVersion<T>) obj;
		return (Tools.compare(this.versionNumber, other.versionNumber) == 0);
	}

	@Override
	public String toString() {
		return String.format("DctmVersion [object=%s, versionNumber=%s, creationDate=%s]", this.id, this.versionNumber,
			DateFormatUtils.ISO_8601_EXTENDED_DATETIME_TIME_ZONE_FORMAT.format(this.creationDate.getDate()));
	}
}