package com.armedia.caliente.engine.ucm.model;

import java.util.EnumMap;
import java.util.Map;

import com.armedia.caliente.store.CmfObject;
import com.armedia.commons.utilities.Tools;

public enum UcmObjectType {
	//
	FILE(CmfObject.Archetype.DOCUMENT), //
	FOLDER(CmfObject.Archetype.FOLDER), //
	//
	;

	private static final Map<CmfObject.Archetype, UcmObjectType> REVERSE;

	static {
		Map<CmfObject.Archetype, UcmObjectType> reverse = new EnumMap<>(CmfObject.Archetype.class);
		for (UcmObjectType t : UcmObjectType.values()) {
			UcmObjectType old = reverse.put(t.archetype, t);
			if (old != null) { throw new RuntimeException(
				String.format("UcmTypes %s and %s have identical CMF mappings to %s", t, old, t.archetype)); }
		}
		REVERSE = Tools.freezeMap(reverse);
	}

	public final CmfObject.Archetype archetype;

	private UcmObjectType(CmfObject.Archetype archetype) {
		this.archetype = archetype;
	}

	public static UcmObjectType resolve(CmfObject.Archetype type) {
		return UcmObjectType.REVERSE.get(type);
	}
}