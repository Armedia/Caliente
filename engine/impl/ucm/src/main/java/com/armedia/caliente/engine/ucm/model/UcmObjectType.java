package com.armedia.caliente.engine.ucm.model;

import java.util.EnumMap;
import java.util.Map;

import com.armedia.caliente.store.CmfArchetype;
import com.armedia.commons.utilities.Tools;

public enum UcmObjectType {
	//
	FILE(CmfArchetype.DOCUMENT), //
	FOLDER(CmfArchetype.FOLDER), //
	//
	;

	private static final Map<CmfArchetype, UcmObjectType> REVERSE;

	static {
		Map<CmfArchetype, UcmObjectType> reverse = new EnumMap<>(CmfArchetype.class);
		for (UcmObjectType t : UcmObjectType.values()) {
			UcmObjectType old = reverse.put(t.cmfArchetype, t);
			if (old != null) { throw new RuntimeException(
				String.format("UcmTypes %s and %s have identical CMF mappings to %s", t, old, t.cmfArchetype)); }
		}
		REVERSE = Tools.freezeMap(reverse);
	}

	public final CmfArchetype cmfArchetype;

	private UcmObjectType(CmfArchetype cmfArchetype) {
		this.cmfArchetype = cmfArchetype;
	}

	public static UcmObjectType resolve(CmfArchetype type) {
		return UcmObjectType.REVERSE.get(type);
	}
}