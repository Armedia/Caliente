package com.armedia.caliente.engine.ucm.model;

import java.util.EnumMap;
import java.util.Map;

import com.armedia.caliente.store.CmfType;
import com.armedia.commons.utilities.Tools;

public enum UcmObjectType {
	//
	FILE(CmfType.DOCUMENT), //
	FOLDER(CmfType.FOLDER), //
	//
	;

	private static final Map<CmfType, UcmObjectType> REVERSE;

	static {
		Map<CmfType, UcmObjectType> reverse = new EnumMap<>(CmfType.class);
		for (UcmObjectType t : UcmObjectType.values()) {
			UcmObjectType old = reverse.put(t.cmfType, t);
			if (old != null) { throw new RuntimeException(
				String.format("UcmTypes %s and %s have identical CMF mappings to %s", t, old, t.cmfType)); }
		}
		REVERSE = Tools.freezeMap(reverse);
	}

	public final CmfType cmfType;

	private UcmObjectType(CmfType cmfType) {
		this.cmfType = cmfType;
	}

	public static UcmObjectType resolve(CmfType type) {
		return UcmObjectType.REVERSE.get(type);
	}
}