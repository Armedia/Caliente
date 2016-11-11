package com.armedia.caliente.store;

public enum CmfType {
	//
	DATASTORE, USER, GROUP, ACL, TYPE, FORMAT, FOLDER, DOCUMENT, WORKFLOW;

	public static CmfType decodeString(String str) {
		if (str == null) { throw new NullPointerException("Must provide a valid string to decode"); }
		try {
			return CmfType.valueOf(str);
		} catch (IllegalArgumentException e) {
			for (CmfTypeDecoder d : CmfTypeDecoder.DECODERS) {
				CmfType ret = d.translateObjectType(str);
				if (ret != null) { return ret; }
			}
			throw new IllegalArgumentException(
				String.format("The string [%s] could not be translated to a CmfType", str));
		}
	}
}