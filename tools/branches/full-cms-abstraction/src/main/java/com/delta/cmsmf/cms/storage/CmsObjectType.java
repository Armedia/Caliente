package com.delta.cmsmf.cms.storage;

public enum CmsObjectType {
	//
	USER,
	GROUP,
	ACL,
	TYPE,
	FORMAT,
	FOLDER,
	DOCUMENT,
	CONTENT_STREAM;

	public static CmsObjectType decode(String str) throws UnsupportedObjectTypeException {
		if (str == null) { throw new IllegalArgumentException("Must provide a valid string to decode"); }
		try {
			return CmsObjectType.valueOf(str);
		} catch (IllegalArgumentException e) {
			throw new UnsupportedObjectTypeException(str);
		}
	}
}