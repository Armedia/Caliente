package com.armedia.caliente.engine.dfc;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.armedia.caliente.engine.importer.ImportStrategy;
import com.armedia.caliente.store.CmfObject;
import com.armedia.commons.utilities.Tools;
import com.documentum.fc.client.IDfACL;
import com.documentum.fc.client.IDfFolder;
import com.documentum.fc.client.IDfFormat;
import com.documentum.fc.client.IDfGroup;
import com.documentum.fc.client.IDfPersistentObject;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.client.IDfSysObject;
import com.documentum.fc.client.IDfType;
import com.documentum.fc.client.IDfUser;
import com.documentum.fc.client.content.IDfStore;
import com.documentum.fc.common.DfException;
import com.documentum.fc.common.IDfId;

enum DctmObjectTypeFlag {
	//
	PARALLEL_CAPABLE, FAILURE_INTERRUPTS_BATCH, SUPPORTS_TRANSACTIONS,
	//
	;
}

public enum DctmObjectType {

	// IMPORTANT: The object types must be declared in the proper import order
	// otherwise that operation will fail.

	STORE(
		CmfObject.Archetype.DATASTORE,
		IDfStore.class,
		null,
		Flag.FAILURE_INTERRUPTS_BATCH,
		Flag.SUPPORTS_TRANSACTIONS,
		Flag.PARALLEL_CAPABLE),
	USER(
		CmfObject.Archetype.USER,
		IDfUser.class,
		null,
		Flag.FAILURE_INTERRUPTS_BATCH,
		Flag.SUPPORTS_TRANSACTIONS,
		Flag.PARALLEL_CAPABLE),
	GROUP(
		CmfObject.Archetype.GROUP,
		IDfGroup.class,
		null,
		Flag.FAILURE_INTERRUPTS_BATCH,
		Flag.SUPPORTS_TRANSACTIONS,
		Flag.PARALLEL_CAPABLE),
	ACL(
		CmfObject.Archetype.ACL,
		IDfACL.class,
		null,
		Flag.FAILURE_INTERRUPTS_BATCH,
		Flag.SUPPORTS_TRANSACTIONS,
		Flag.PARALLEL_CAPABLE),
	TYPE(
		CmfObject.Archetype.TYPE,
		IDfType.class,
		null,
		Flag.FAILURE_INTERRUPTS_BATCH,
		// Flag.SUPPORTS_TRANSACTIONS,
		Flag.PARALLEL_CAPABLE),
	FORMAT(
		CmfObject.Archetype.FORMAT,
		IDfFormat.class,
		null,
		Flag.FAILURE_INTERRUPTS_BATCH,
		Flag.SUPPORTS_TRANSACTIONS,
		Flag.PARALLEL_CAPABLE),
	FOLDER(
		CmfObject.Archetype.FOLDER,
		IDfFolder.class,
		null,
		Flag.FAILURE_INTERRUPTS_BATCH,
		Flag.SUPPORTS_TRANSACTIONS,
		Flag.PARALLEL_CAPABLE),
	DOCUMENT(
		CmfObject.Archetype.DOCUMENT,
		IDfSysObject.class,
		null,
		Flag.FAILURE_INTERRUPTS_BATCH,
		Flag.SUPPORTS_TRANSACTIONS,
		Flag.PARALLEL_CAPABLE),
	//
	;

	private static enum Flag {
		//
		PARALLEL_CAPABLE, FAILURE_INTERRUPTS_BATCH, SUPPORTS_TRANSACTIONS,
		//
		;
	}

	private final CmfObject.Archetype cmsType;
	private final String dmType;
	private final Class<? extends IDfPersistentObject> dfClass;
	private final boolean failureInterruptsBatch;
	private final boolean supportsTransactions;
	private final boolean parallelCapable;
	public final ImportStrategy importStrategy = new ImportStrategy() {

		@Override
		public boolean isIgnored() {
			return false;
		}

		@Override
		public boolean isParallelCapable() {
			return DctmObjectType.this.parallelCapable;
		}

		@Override
		public boolean isFailBatchOnError() {
			return DctmObjectType.this.failureInterruptsBatch;
		}

		@Override
		public boolean isSupportsTransactions() {
			return DctmObjectType.this.supportsTransactions;
		}
	};

	private <T extends IDfPersistentObject> DctmObjectType(CmfObject.Archetype cmsType, Class<T> dfClass) {
		this(cmsType, dfClass, null);
	}

	private <T extends IDfPersistentObject> DctmObjectType(CmfObject.Archetype cmsType, Class<T> dfClass, String dmType,
		Flag... flags) {
		this.cmsType = cmsType;
		if (dmType == null) {
			this.dmType = String.format("dm_%s", name().toLowerCase());
		} else {
			this.dmType = dmType;
		}
		this.dfClass = dfClass;
		Set<Flag> s = EnumSet.noneOf(Flag.class);
		if (flags != null) {
			for (Flag f : flags) {
				s.add(f);
			}
		}
		this.failureInterruptsBatch = s.contains(Flag.FAILURE_INTERRUPTS_BATCH);
		this.supportsTransactions = s.contains(Flag.SUPPORTS_TRANSACTIONS);
		this.parallelCapable = s.contains(Flag.PARALLEL_CAPABLE);
	}

	public final CmfObject.Archetype getStoredObjectType() {
		return this.cmsType;
	}

	public final String getDmType() {
		return this.dmType;
	}

	public final boolean isProperClass(IDfPersistentObject o) {
		if (o == null) { return true; }
		return this.dfClass.isInstance(o);
	}

	public final Class<? extends IDfPersistentObject> getDfClass() {
		return this.dfClass;
	}

	private static Map<String, DctmObjectType> DM_TYPE_DECODER = null;
	private static Map<CmfObject.Archetype, DctmObjectType> OBJECT_TYPE_TRANSLATOR = null;

	static {
		Map<String, DctmObjectType> m = new HashMap<>();
		for (DctmObjectType t : DctmObjectType.values()) {
			m.put(t.dmType, t);
		}
		DctmObjectType.DM_TYPE_DECODER = Tools.freezeMap(m);
	}

	static {
		Map<CmfObject.Archetype, DctmObjectType> m = new EnumMap<>(CmfObject.Archetype.class);
		for (DctmObjectType t : DctmObjectType.values()) {
			CmfObject.Archetype c = t.getStoredObjectType();
			if (c != null) {
				m.put(c, t);
			}
		}
		DctmObjectType.OBJECT_TYPE_TRANSLATOR = Tools.freezeMap(m);
	}

	public static DctmObjectType decodeType(IDfPersistentObject object)
		throws DfException, UnsupportedDctmObjectTypeException {
		if (object == null) { throw new IllegalArgumentException("Must provide an object to decode the type from"); }
		IDfId id = object.getObjectId();
		DctmObjectType type = DctmObjectType.decodeType(id);
		if (type != null) { return type; }
		return DctmObjectType.decodeType(object.getType());
	}

	public static DctmObjectType decodeType(IDfSession session, String typeName)
		throws DfException, UnsupportedDctmObjectTypeException {
		if (session == null) { throw new IllegalArgumentException("Must provide a session to find the type in"); }
		if (typeName == null) { throw new IllegalArgumentException("Must provide a type to find"); }
		IDfType type = session.getType(typeName);
		if (type == null) { throw new UnsupportedDctmObjectTypeException(typeName); }
		return DctmObjectType.decodeType(type);
	}

	public static DctmObjectType decodeType(IDfType type) throws DfException, UnsupportedDctmObjectTypeException {
		if (type == null) { throw new IllegalArgumentException("Must provide a type to decode"); }
		final String typeName = type.getName();
		while (type != null) {
			try {
				return DctmObjectType.decodeType(type.getName());
			} catch (UnsupportedDctmObjectTypeException e) {
				// This type isn't supported...try its parent
				IDfType parent = type.getSuperType();
				if ((parent == null) && "dm_sysobject".equalsIgnoreCase(type.getName())) {
					// If we're about to fail, take one last look... if the supertype is EXACTLY
					// dm_sysobject, then we return DOCUMENT
					return DctmObjectType.DOCUMENT;
				}
				type = parent;
				continue;
			}
		}
		// The only way we get here is if we can't decode into a supported type
		throw new UnsupportedDctmObjectTypeException(typeName);
	}

	private static DctmObjectType decodeType(String type) throws UnsupportedDctmObjectTypeException {
		if (type == null) { throw new IllegalArgumentException("Must provide a type to decode"); }
		DctmObjectType ret = DctmObjectType.DM_TYPE_DECODER.get(type);
		if (ret == null) { throw new UnsupportedDctmObjectTypeException(type); }
		return ret;
	}

	public static DctmObjectType decodeType(CmfObject.Archetype type) {
		return DctmObjectType.OBJECT_TYPE_TRANSLATOR.get(type);
	}

	public static DctmObjectType decodeType(IDfId id) {
		if (id == null) { throw new IllegalArgumentException("Must provide an ID to decode the information from"); }
		// TODO: Not happy with this hardcoded table - maybe find a way to dynamically populate this
		// from the installation?
		switch (id.getTypePart()) {
			case IDfId.DM_STORE:
				return DctmObjectType.STORE;

			case IDfId.DM_USER:
				return DctmObjectType.USER;

			case IDfId.DM_GROUP:
				return DctmObjectType.GROUP;

			case IDfId.DM_ACL:
				return DctmObjectType.ACL;

			case IDfId.DM_TYPE:
				return DctmObjectType.TYPE;

			case IDfId.DM_FORMAT:
				return DctmObjectType.FORMAT;

			case IDfId.DM_CABINET:
			case IDfId.DM_FOLDER:
				return DctmObjectType.FOLDER;

			case IDfId.DM_SYSOBJECT:
			case IDfId.DM_DOCUMENT:
				return DctmObjectType.DOCUMENT;

			default:
				return null;
		}
	}
}