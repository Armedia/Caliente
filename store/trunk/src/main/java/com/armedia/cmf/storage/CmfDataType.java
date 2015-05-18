package com.armedia.cmf.storage;

public enum CmfDataType {
	//
	BOOLEAN,
	INTEGER,
	DOUBLE,
	STRING,
	ID,
	DATETIME,
	URI,
	HTML,
	OTHER,
	//
	;

	public static CmfDataType decodeString(String str) {
		if (str == null) { throw new NullPointerException("Must provide a valid string to decode"); }
		try {
			return CmfDataType.valueOf(str);
		} catch (IllegalArgumentException e) {
			for (CmfTypeDecoder d : CmfTypeDecoder.DECODERS) {
				CmfDataType ret = d.translateDataType(str);
				if (ret != null) { return ret; }
			}
			throw new IllegalArgumentException(String.format(
				"The string [%s] could not be translated to a CmfDataType", str));
		}
	}
}