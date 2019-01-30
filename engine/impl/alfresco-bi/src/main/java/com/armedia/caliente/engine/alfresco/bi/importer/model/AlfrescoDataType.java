package com.armedia.caliente.engine.alfresco.bi.importer.model;

import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.armedia.caliente.store.CmfValueType;
import com.armedia.commons.utilities.Tools;

public enum AlfrescoDataType {
	//
	TEXT(CmfValueType.STRING, CmfValueType.ID, CmfValueType.HTML, CmfValueType.URI),
	INT(CmfValueType.INTEGER),
	DOUBLE(CmfValueType.DOUBLE),
	DATETIME(CmfValueType.DATETIME),
	BOOLEAN(CmfValueType.BOOLEAN),
	ANY(CmfValueType.OTHER),
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

	public final CmfValueType cmfValueType;
	private final Set<CmfValueType> mappedTypes;
	public final String nameString;

	private AlfrescoDataType(CmfValueType... types) {
		CmfValueType cmfValueType = null;
		if ((types != null) && (types.length > 0)) {
			EnumSet<CmfValueType> s = EnumSet.noneOf(CmfValueType.class);
			for (CmfValueType t : types) {
				if (t == null) {
					continue;
				}
				if (cmfValueType == null) {
					cmfValueType = t;
				}
				s.add(t);
			}
			this.mappedTypes = Tools.freezeSet(s);
		} else {
			this.mappedTypes = Collections.emptySet();
		}
		this.cmfValueType = cmfValueType;
		String name = name();
		if (name.indexOf('_') < 0) {
			this.nameString = String.format("d:%s", name).toLowerCase();
		} else {
			this.nameString = name.replace('_', ':').toLowerCase();
		}
	}

	public final boolean matches(CmfValueType t) {
		if (t == null) { throw new IllegalArgumentException("Must provide a valid CmfDataType instance"); }
		return this.mappedTypes.contains(t);
	}

	private static Map<CmfValueType, AlfrescoDataType> CMF_MAP = null;

	private static Map<String, AlfrescoDataType> STR_MAP = null;

	private static synchronized Map<CmfValueType, AlfrescoDataType> getCmfMappings() {
		if (AlfrescoDataType.CMF_MAP != null) { return AlfrescoDataType.CMF_MAP; }
		Map<CmfValueType, AlfrescoDataType> m = new EnumMap<>(CmfValueType.class);
		Map<String, AlfrescoDataType> m2 = new HashMap<>();
		for (AlfrescoDataType p : AlfrescoDataType.values()) {

			AlfrescoDataType p2 = m2.put(p.nameString, p);
			if (p2 != null) { throw new IllegalStateException(
				String.format("Two property types mapped as [%s] - %s and %s", p.nameString, p.name(), p2.name())); }

			for (CmfValueType t : p.mappedTypes) {
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

	public static AlfrescoDataType decode(CmfValueType t) {
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