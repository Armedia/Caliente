package com.armedia.caliente.engine.ucm.model;

import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;

import com.armedia.commons.utilities.Tools;

public final class UcmGUID implements Comparable<UcmGUID>, Serializable {
	private static final long serialVersionUID = 1L;

	public static final UcmGUID NULL_GUID;

	static {
		try {
			NULL_GUID = new UcmGUID(new URI("null", "null", null));
		} catch (URISyntaxException e) {
			throw new RuntimeException("Failed to initialize the NULL_GUID GUID", e);
		}
	}

	private final URI uri;

	public UcmGUID(URI uri) {
		this.uri = uri;
	}

	public URI getURI() {
		return this.uri;
	}

	@Override
	public int hashCode() {
		return Tools.hashTool(this, null, this.uri);
	}

	@Override
	public boolean equals(Object obj) {
		if (!Tools.baseEquals(this, obj)) { return false; }
		UcmGUID other = UcmGUID.class.cast(obj);
		if (!Tools.equals(this.uri, other.uri)) { return false; }
		return true;
	}

	@Override
	public int compareTo(UcmGUID o) {
		if (o == null) { return 1; }
		return Tools.compare(this.uri, o.uri);
	}

	@Override
	public String toString() {
		return String.format("UcmGUID[%s]", this.uri);
	}
}