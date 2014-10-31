package com.armedia.cmf.engine.exporter;

import com.armedia.cmf.storage.StoredObjectType;
import com.armedia.commons.utilities.Tools;

public final class ExportTarget implements Comparable<ExportTarget> {
	private final StoredObjectType type;
	private final String id;
	private final Long number;

	ExportTarget() {
		this.type = null;
		this.id = null;
		this.number = null;
	}

	public ExportTarget(StoredObjectType type, String id) {
		this(type, id, null);
	}

	public ExportTarget(StoredObjectType type, String id, Long number) {
		if (type == null) { throw new IllegalArgumentException("Must provide an object type"); }
		if (id == null) { throw new IllegalArgumentException("Must provide an object id"); }
		this.type = type;
		this.id = id;
		this.number = number;
	}

	public StoredObjectType getType() {
		return this.type;
	}

	public String getId() {
		return this.id;
	}

	public Long getNumber() {
		return this.number;
	}

	@Override
	public int hashCode() {
		return Tools.hashTool(this, null, this.type, this.id);
	}

	@Override
	public boolean equals(Object obj) {
		if (!Tools.baseEquals(this, obj)) { return false; }
		ExportTarget other = ExportTarget.class.cast(obj);
		if (!Tools.equals(this.type, other.type)) { return false; }
		if (!Tools.equals(this.id, other.id)) { return false; }
		return true;
	}

	@Override
	public String toString() {
		return String.format("Target [type=%s, id=%s, number=%s]", this.type, this.id, this.number);
	}

	@Override
	public int compareTo(ExportTarget other) {
		if (other == null) { return 1; }
		int c = Tools.compare(this.type, other.type);
		if (c != 0) { return c; }
		c = Tools.compare(this.id, other.id);
		if (c != 0) { return c; }
		// We are the same...
		return 0;
	}
}