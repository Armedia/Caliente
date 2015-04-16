package com.armedia.cmf.storage;

public enum StoredObjectType {
	//
	DATASTORE,
	USER,
	GROUP,
	ACL,
	TYPE,
	FORMAT,
	FOLDER,
	DOCUMENT,
	CONTENT, // Only exists for backward compatibility, and should not be used for anything else
	WORKFLOW;

	public static StoredObjectType decodeString(String str) {
		if (str == null) { throw new NullPointerException("Must provide a valid string to decode"); }
		try {
			return StoredObjectType.valueOf(str);
		} catch (IllegalArgumentException e) {
			for (TypeDecoder d : TypeDecoder.DECODERS) {
				StoredObjectType ret = d.translateObjectType(str);
				if (ret != null) { return ret; }
			}
			throw new IllegalArgumentException(String.format(
				"The string [%s] could not be translated to a StoredObjectType", str));
		}
	}
}