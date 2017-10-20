package com.armedia.calienteng;

import java.io.Serializable;
import java.net.URI;
import java.util.Date;

import com.armedia.commons.utilities.Tools;

public final class FileSpec implements Cloneable, Comparable<FileSpec>, Serializable {
	private static final long serialVersionUID = 1L;

	public static enum Type {
		//
		FOLDER, //
		FILE, //
		//
		;
	}

	private URI uri;
	private Date timestamp;
	private Long changeMarker;
	private String version;
	private Type type;

	public FileSpec(URI uri, Date timestamp, Long changeMarker, String version, Type type) {
		this.uri = uri;
		this.timestamp = timestamp;
		this.changeMarker = changeMarker;
		this.version = version;
		this.type = type;
	}

	public URI getUri() {
		return this.uri;
	}

	public Date getTimestamp() {
		return this.timestamp;
	}

	public Long getChangeMarker() {
		return this.changeMarker;
	}

	public String getVersion() {
		return this.version;
	}

	public Type getType() {
		return this.type;
	}

	@Override
	public int compareTo(FileSpec o) {
		if (o == null) { return 1; }
		int r = Tools.compare(this.uri, o.uri);
		if (r != 0) { return r; }
		r = Tools.compare(this.type, o.type);
		if (r != 0) { return r; }
		r = Tools.compare(this.timestamp, o.timestamp);
		if (r != 0) { return r; }
		r = Tools.compare(this.changeMarker, o.changeMarker);
		if (r != 0) { return r; }
		r = Tools.compare(this.version, o.version);
		if (r != 0) { return r; }
		return 0;
	}

	@Override
	public int hashCode() {
		return Tools.hashTool(this, null, this.uri, this.type, this.timestamp, this.changeMarker, this.version);
	}

	public boolean isSameFile(FileSpec other) {
		if (other == null) { return false; }
		if (other == this) { return true; }
		return Tools.equals(this.uri.getPath(), other.uri.getPath()) && Tools.equals(this.type, other.type);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) { return true; }
		if (!Tools.baseEquals(this, obj)) { return false; }
		FileSpec other = FileSpec.class.cast(obj);
		if (!Tools.equals(this.uri, other.uri)) { return false; }
		if (!Tools.equals(this.type, other.type)) { return false; }
		if (!Tools.equals(this.timestamp, other.timestamp)) { return false; }
		if (!Tools.equals(this.changeMarker, other.changeMarker)) { return false; }
		if (!Tools.equals(this.version, other.version)) { return false; }
		return true;
	}

	@Override
	public String toString() {
		return String.format("FileSpec [uri=%s, type=%s, timestamp=%s, changeMarker=%s, version=%s]", this.uri,
			this.type, this.timestamp, this.changeMarker, this.version);
	}

	@Override
	public FileSpec clone() {
		try {
			return FileSpec.class.cast(super.clone());
		} catch (CloneNotSupportedException e) {
			throw new RuntimeException("Base class failed to clone properly", e);
		}
	}
}