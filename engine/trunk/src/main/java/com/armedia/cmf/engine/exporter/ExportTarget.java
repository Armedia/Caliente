package com.armedia.cmf.engine.exporter;

import com.armedia.cmf.storage.StoredObjectType;
import com.armedia.commons.utilities.Tools;

public final class ExportTarget implements Comparable<ExportTarget> {
	private final StoredObjectType type;
	private final String id;
	private final String searchKey;
	private final Long number;

	ExportTarget() {
		this.type = null;
		this.id = null;
		this.searchKey = null;
		this.number = null;
	}

	public ExportTarget(StoredObjectType type, String id, String searchKey) {
		this(type, id, searchKey, null);
	}

	public ExportTarget(StoredObjectType type, String id, String searchKey, Long number) {
		if (id == null) { throw new IllegalArgumentException("Must provide an object id"); }
		this.type = type;
		this.id = id;
		this.searchKey = Tools.coalesce(searchKey, id);
		this.number = number;
	}

	public StoredObjectType getType() {
		return this.type;
	}

	public String getId() {
		return this.id;
	}

	public String getSearchKey() {
		return this.searchKey;
	}

	public Long getNumber() {
		return this.number;
	}

	@Override
	public int hashCode() {
		return Tools.hashTool(this, null, this.type, this.id, this.searchKey);
	}

	@Override
	public boolean equals(Object obj) {
		if (!Tools.baseEquals(this, obj)) { return false; }
		ExportTarget other = ExportTarget.class.cast(obj);
		if (!Tools.equals(this.type, other.type)) { return false; }
		if (!Tools.equals(this.id, other.id)) { return false; }
		if (!Tools.equals(this.searchKey, other.searchKey)) { return false; }
		return true;
	}

	@Override
	public String toString() {
		if (this.number != null) { return String.format("ExportTarget [type=%s, id=%s, searchKey=%s, number=%s]",
			this.type, this.id, this.searchKey, this.number); }
		return String.format("ExportTarget [type=%s, id=%s, searchKey=%s]", this.type, this.id, this.searchKey);
	}

	@Override
	public int compareTo(ExportTarget other) {
		if (other == null) { return 1; }
		int c = Tools.compare(this.type, other.type);
		if (c != 0) { return c; }
		c = Tools.compare(this.id, other.id);
		if (c != 0) { return c; }
		c = Tools.compare(this.searchKey, other.searchKey);
		if (c != 0) { return c; }
		// We are the same...
		return 0;
	}
}