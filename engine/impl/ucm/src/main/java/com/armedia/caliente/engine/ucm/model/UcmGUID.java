package com.armedia.caliente.engine.ucm.model;

import java.io.Serializable;
import java.util.Arrays;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang3.StringUtils;

import com.armedia.commons.utilities.Tools;

public final class UcmGUID implements Comparable<UcmGUID>, Serializable {
	private static final long serialVersionUID = 1L;

	private final byte[] data;
	private final String string;

	public UcmGUID(String guid) {
		if (guid == null) {
			guid = "";
		}
		if (guid.length() < 32) {
			// Ensure the GUID is 32 characters (16 bytes) long
			guid = StringUtils.leftPad(guid, 32, '0');
		}
		try {
			this.data = Hex.decodeHex(guid.toCharArray());
		} catch (DecoderException e) {
			throw new UcmRuntimeException(String.format("Failed to decode the UcmGUID [%s]", guid), e);
		}
		this.string = guid.toUpperCase();
	}

	public byte[] getData() {
		return this.data.clone();
	}

	public String getString() {
		return this.string;
	}

	@Override
	public int hashCode() {
		return Tools.hashTool(this, null, this.data);
	}

	@Override
	public boolean equals(Object obj) {
		if (!Tools.baseEquals(this, obj)) { return false; }
		UcmGUID other = UcmGUID.class.cast(obj);
		if (!Arrays.equals(this.data, other.data)) { return false; }
		return true;
	}

	@Override
	public int compareTo(UcmGUID o) {
		if (o == null) { return 1; }
		// Compare the bytes...
		for (int i = 0; i < 16; i++) {
			int r = Tools.compare(this.data[i], o.data[i]);
			if (r != 0) { return r; }
		}
		return 0;
	}

	@Override
	public String toString() {
		return String.format("UcmGUID[%s]", this.string);
	}
}