package com.armedia.caliente.engine.ucm.model;

import java.io.Serializable;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang3.StringUtils;

import com.armedia.commons.utilities.Tools;

public final class UcmGUID implements Comparable<UcmGUID>, Serializable {
	private static final long serialVersionUID = 1L;

	private final String string;

	public UcmGUID(String guid) {
		if (guid == null) {
			guid = "";
		}
		try {
			Hex.decodeHex(guid.toCharArray());
			if (guid.length() < 32) {
				// Ensure the BY_GUID is 32 characters (16 bytes) long
				guid = StringUtils.leftPad(guid, 32, '0');
			}
		} catch (DecoderException e) {
			// Do nothing - this is a special BY_GUID...just don't pad it
		}
		this.string = guid.toUpperCase();
	}

	public String getString() {
		return this.string;
	}

	@Override
	public int hashCode() {
		return Tools.hashTool(this, null, this.string);
	}

	@Override
	public boolean equals(Object obj) {
		if (!Tools.baseEquals(this, obj)) { return false; }
		UcmGUID other = UcmGUID.class.cast(obj);
		if (!Tools.equals(this.string, other.string)) { return false; }
		return true;
	}

	@Override
	public int compareTo(UcmGUID o) {
		if (o == null) { return 1; }
		return Tools.compare(this.string, o.string);
	}

	@Override
	public String toString() {
		return String.format("UcmGUID[%s]", this.string);
	}
}