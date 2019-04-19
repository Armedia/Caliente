package com.armedia.caliente.engine.alfresco.bi.importer.model;

import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.armedia.caliente.store.CmfValue;
import com.armedia.commons.utilities.Tools;

public enum AlfrescoDataType {
	//
	TEXT(CmfValue.Type.STRING, CmfValue.Type.ID, CmfValue.Type.HTML, CmfValue.Type.URI),
	INT(CmfValue.Type.INTEGER),
	DOUBLE(CmfValue.Type.DOUBLE),
	DATETIME(CmfValue.Type.DATETIME),
	BOOLEAN(CmfValue.Type.BOOLEAN),
	ANY(CmfValue.Type.OTHER),
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

	public final CmfValue.Type cmfValueType;
	private final Set<CmfValue.Type> mappedTypes;
	public final String nameString;

	private AlfrescoDataType(CmfValue.Type... types) {
		CmfValue.Type cmfValueType = null;
		if ((types != null) && (types.length > 0)) {
			EnumSet<CmfValue.Type> s = EnumSet.noneOf(CmfValue.Type.class);
			for (CmfValue.Type t : types) {
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

	public final boolean matches(CmfValue.Type t) {
		if (t == null) { throw new IllegalArgumentException("Must provide a valid CmfValue.Type instance"); }
		return this.mappedTypes.contains(t);
	}

	private static Map<CmfValue.Type, AlfrescoDataType> CMF_MAP = null;
	private static Map<String, AlfrescoDataType> STR_MAP = null;
	static {
		Map<CmfValue.Type, AlfrescoDataType> m = new EnumMap<>(CmfValue.Type.class);
		Map<String, AlfrescoDataType> m2 = new HashMap<>();
		for (AlfrescoDataType p : AlfrescoDataType.values()) {

			AlfrescoDataType p2 = m2.put(p.nameString, p);
			if (p2 != null) {
				throw new IllegalStateException(
					String.format("Two property types mapped as [%s] - %s and %s", p.nameString, p.name(), p2.name()));
			}

			for (CmfValue.Type t : p.mappedTypes) {
				p2 = m.put(t, p);
				if (p2 != null) {
					throw new IllegalStateException(
						String.format("Mapping error: CmfValue.Type.%s is mapped to by both %s and %s", t, p, p2));
				}
			}
		}
		AlfrescoDataType.CMF_MAP = Tools.freezeMap(m);
		AlfrescoDataType.STR_MAP = Tools.freezeMap(m2);
	}

	public static AlfrescoDataType decode(CmfValue.Type t) {
		AlfrescoDataType pt = AlfrescoDataType.CMF_MAP.get(t);
		if (pt == null) { throw new IllegalArgumentException(String.format("Unsupported property type %s", t)); }
		return pt;
	}

	public static AlfrescoDataType decode(String s) {
		AlfrescoDataType pt = AlfrescoDataType.STR_MAP.get(s);
		if (pt == null) { throw new IllegalArgumentException(String.format("Unsupported property type name [%s]", s)); }
		return pt;
	}
}