package com.armedia.cmf.storage.jdbc;

import java.io.Serializable;

import com.armedia.commons.utilities.Tools;

public class JdbcContentLocator implements Serializable, Comparable<JdbcContentLocator> {
	private static final long serialVersionUID = 1L;

	private final String objectId;
	private final String qualifier;

	JdbcContentLocator(String objectId, String qualifier) {
		if (objectId == null) { throw new IllegalArgumentException("Must provide a non-null object ID"); }
		this.objectId = objectId;
		this.qualifier = Tools.coalesce(qualifier, "");
	}

	public String getObjectId() {
		return this.objectId;
	}

	public String getQualifier() {
		return this.qualifier;
	}

	@Override
	public int hashCode() {
		return Tools.hashTool(this, null, this.objectId, this.qualifier);
	}

	@Override
	public boolean equals(Object obj) {
		if (!Tools.baseEquals(this, obj)) { return false; }
		JdbcContentLocator other = JdbcContentLocator.class.cast(obj);
		if (!Tools.equals(this.objectId, other.objectId)) { return false; }
		if (!Tools.equals(this.qualifier, other.qualifier)) { return false; }
		return true;
	}

	@Override
	public String toString() {
		return String.format("JdbcContentLocator [objectId=%s, qualifier=%s]", this.objectId, this.qualifier);
	}

	@Override
	public int compareTo(JdbcContentLocator o) {
		if (o == null) { return 1; }
		if (o == this) { return 0; }
		int r = Tools.compare(this.objectId, o.objectId);
		if (r != 0) { return r; }
		r = Tools.compare(this.qualifier, o.qualifier);
		if (r != 0) { return r; }
		return r;
	}
}