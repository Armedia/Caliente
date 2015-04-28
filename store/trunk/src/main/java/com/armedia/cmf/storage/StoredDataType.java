package com.armedia.cmf.storage;

public enum StoredDataType {
	//
	BOOLEAN,
	INTEGER,
	DOUBLE,
	STRING,
	ID,
	DATETIME,
	OTHER;

	public static StoredDataType decodeString(String str) {
		if (str == null) { throw new NullPointerException("Must provide a valid string to decode"); }
		try {
			return StoredDataType.valueOf(str);
		} catch (IllegalArgumentException e) {
			for (TypeDecoder d : TypeDecoder.DECODERS) {
				StoredDataType ret = d.translateDataType(str);
				if (ret != null) { return ret; }
			}
			throw new IllegalArgumentException(String.format(
				"The string [%s] could not be translated to a StoredDataType", str));
		}
	}
}