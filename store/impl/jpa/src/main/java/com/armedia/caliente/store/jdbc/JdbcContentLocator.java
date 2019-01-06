package com.armedia.caliente.store.jdbc;

import java.io.Serializable;

import com.armedia.caliente.store.CmfContentStream;
import com.armedia.commons.utilities.Tools;

public class JdbcContentLocator implements Serializable, Comparable<JdbcContentLocator> {
	private static final long serialVersionUID = 1L;

	private final String objectId;
	private final CmfContentStream info;

	JdbcContentLocator(String objectId, CmfContentStream info) {
		if (objectId == null) { throw new IllegalArgumentException("Must provide a non-null object ID"); }
		if (info == null) { throw new IllegalArgumentException("Must provide a non-null content info object"); }
		this.objectId = objectId;
		this.info = info;
	}

	public String getObjectId() {
		return this.objectId;
	}

	public CmfContentStream getInfo() {
		return this.info;
	}

	@Override
	public int hashCode() {
		return Tools.hashTool(this, null, this.objectId, this.info);
	}

	@Override
	public boolean equals(Object obj) {
		if (!Tools.baseEquals(this, obj)) { return false; }
		JdbcContentLocator other = JdbcContentLocator.class.cast(obj);
		if (!Tools.equals(this.objectId, other.objectId)) { return false; }
		if (!Tools.equals(this.info, other.info)) { return false; }
		return true;
	}

	@Override
	public String toString() {
		return String.format("JdbcContentLocator [objectId=%s, qualifier=%s]", this.objectId, this.info);
	}

	@Override
	public int compareTo(JdbcContentLocator o) {
		if (o == null) { return 1; }
		if (o == this) { return 0; }
		int r = Tools.compare(this.objectId, o.objectId);
		if (r != 0) { return r; }
		r = Tools.compare(this.info, o.info);
		if (r != 0) { return r; }
		return r;
	}
}