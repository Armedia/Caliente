package com.armedia.caliente.engine.alfresco.bi.importer.model;

import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.armedia.caliente.store.CmfDataType;
import com.armedia.commons.utilities.Tools;

public enum AlfrescoDataType {
	//
	TEXT(CmfDataType.STRING, CmfDataType.ID, CmfDataType.HTML, CmfDataType.URI),
	INT(CmfDataType.INTEGER),
	DOUBLE(CmfDataType.DOUBLE),
	DATETIME(CmfDataType.DATETIME),
	BOOLEAN(CmfDataType.BOOLEAN),
	ANY(CmfDataType.OTHER),
	//
	// Now, the other Alfresco types that have no mapping in CMIS
	//
	ASSOCREF(),
	CATEGORY(),
	CHILDASSOCREF(),
	CONTENT(),
	DATE(),
	FLOAT(),
	LOCALE(),
	LONG(),
	MLTEXT(),
	NODEREF(),
	QNAME(),
	CMIS_ID(),
	//
	;

	private final Set<CmfDataType> mappedTypes;
	public final String nameString;

	private AlfrescoDataType(CmfDataType... types) {
		if (types != null) {
			EnumSet<CmfDataType> s = EnumSet.noneOf(CmfDataType.class);
			for (CmfDataType t : types) {
				if (t == null) {
					continue;
				}
				s.add(t);
			}
			this.mappedTypes = Tools.freezeSet(s);
		} else {
			this.mappedTypes = Collections.emptySet();
		}
		String name = name();
		if (name.indexOf('_') < 0) {
			this.nameString = String.format("d:%s", name).toLowerCase();
		} else {
			this.nameString = name.replace('_', ':').toLowerCase();
		}
	}

	public final boolean matches(CmfDataType t) {
		if (t == null) { throw new IllegalArgumentException("Must provide a valid CmfDataType instance"); }
		return this.mappedTypes.contains(t);
	}

	private static Map<CmfDataType, AlfrescoDataType> CMF_MAP = null;

	private static Map<String, AlfrescoDataType> STR_MAP = null;

	private static synchronized Map<CmfDataType, AlfrescoDataType> getCmfMappings() {
		if (AlfrescoDataType.CMF_MAP != null) { return AlfrescoDataType.CMF_MAP; }
		Map<CmfDataType, AlfrescoDataType> m = new EnumMap<>(CmfDataType.class);
		Map<String, AlfrescoDataType> m2 = new HashMap<>();
		for (AlfrescoDataType p : AlfrescoDataType.values()) {

			AlfrescoDataType p2 = m2.put(p.nameString, p);
			if (p2 != null) { throw new IllegalStateException(
				String.format("Two property types mapped as [%s] - %s and %s", p.nameString, p.name(), p2.name())); }

			for (CmfDataType t : p.mappedTypes) {
				p2 = m.put(t, p);
				if (p2 != null) { throw new IllegalStateException(
					String.format("Mapping error: CmfDataType.%s is mapped to by both %s and %s", t, p, p2)); }
			}
		}
		AlfrescoDataType.CMF_MAP = Tools.freezeMap(m);
		AlfrescoDataType.STR_MAP = Tools.freezeMap(m2);
		return AlfrescoDataType.CMF_MAP;
	}

	private static synchronized Map<String, AlfrescoDataType> getStrMappings() {
		AlfrescoDataType.getCmfMappings();
		return AlfrescoDataType.STR_MAP;
	}

	public static AlfrescoDataType decode(CmfDataType t) {
		AlfrescoDataType pt = AlfrescoDataType.getCmfMappings().get(t);
		if (pt == null) { throw new IllegalArgumentException(String.format("Unsupported property type %s", t)); }
		return pt;
	}

	public static AlfrescoDataType decode(String s) {
		AlfrescoDataType pt = AlfrescoDataType.getStrMappings().get(s);
		if (pt == null) { throw new IllegalArgumentException(String.format("Unsupported property type name [%s]", s)); }
		return pt;
	}
}