package com.armedia.cmf.engine.documentum;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

import com.armedia.cmf.engine.importer.ImportStrategy;
import com.armedia.cmf.engine.importer.ImportStrategy.BatchItemStrategy;
import com.armedia.cmf.storage.CmfType;
import com.armedia.commons.utilities.Tools;
import com.documentum.fc.client.IDfACL;
import com.documentum.fc.client.IDfDocument;
import com.documentum.fc.client.IDfFolder;
import com.documentum.fc.client.IDfFormat;
import com.documentum.fc.client.IDfGroup;
import com.documentum.fc.client.IDfPersistentObject;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.client.IDfType;
import com.documentum.fc.client.IDfUser;
import com.documentum.fc.client.content.IDfStore;
import com.documentum.fc.common.DfException;
import com.documentum.fc.common.IDfId;

public enum DctmObjectType {

	// IMPORTANT: The object types must be declared in the proper import order
	// otherwise that operation will fail.

	STORE(CmfType.DATASTORE, IDfStore.class),
	USER(CmfType.USER, IDfUser.class),
	GROUP(CmfType.GROUP, IDfGroup.class, BatchItemStrategy.ITEMS_SERIALIZED, null, true, false, true, false),
	ACL(CmfType.ACL, IDfACL.class),
	TYPE(CmfType.TYPE, IDfType.class, BatchItemStrategy.ITEMS_CONCURRENT, null, true, false, false),
	FORMAT(CmfType.FORMAT, IDfFormat.class),
	FOLDER(CmfType.FOLDER, IDfFolder.class, BatchItemStrategy.ITEMS_CONCURRENT, null, true, false),
	DOCUMENT(CmfType.DOCUMENT, IDfDocument.class, BatchItemStrategy.ITEMS_SERIALIZED, null, true, true),
	//
	;

	private final CmfType cmsType;
	private final String dmType;
	private final Class<? extends IDfPersistentObject> dfClass;
	private final BatchItemStrategy batchingStrategy;
	private final boolean supportsBatching;
	private final boolean failureInterruptsBatch;
	private final boolean supportsTransactions;
	private final boolean parallelCapable;
	public final ImportStrategy importStrategy = new ImportStrategy() {

		@Override
		public boolean isIgnored() {
			return false;
		}

		@Override
		public BatchItemStrategy getBatchItemStrategy() {
			if (!DctmObjectType.this.supportsBatching) { return null; }
			return DctmObjectType.this.batchingStrategy;
		}

		@Override
		public boolean isParallelCapable() {
			return DctmObjectType.this.parallelCapable;
		}

		@Override
		public boolean isBatchFailRemainder() {
			return DctmObjectType.this.failureInterruptsBatch;
		}

		@Override
		public boolean isBatchingSupported() {
			return DctmObjectType.this.supportsBatching;
		}

		@Override
		public boolean isBatchIndependent() {
			// For now, eventually we'll do something different
			return true;
		}

		@Override
		public boolean isSupportsTransactions() {
			return DctmObjectType.this.supportsTransactions;
		}
	};

	private <T extends IDfPersistentObject> DctmObjectType(CmfType cmsType, Class<T> dfClass) {
		this(cmsType, dfClass, null, null);
	}

	private <T extends IDfPersistentObject> DctmObjectType(CmfType cmsType, Class<T> dfClass, String dmType) {
		this(cmsType, dfClass, null, dmType);
	}

	private <T extends IDfPersistentObject> DctmObjectType(CmfType cmsType, Class<T> dfClass,
		BatchItemStrategy batchingStrategy) {
		this(cmsType, dfClass, batchingStrategy, null);
	}

	private <T extends IDfPersistentObject> DctmObjectType(CmfType cmsType, Class<T> dfClass,
		BatchItemStrategy batchingStrategy, String dmType) {
		this(cmsType, dfClass, batchingStrategy, dmType, false, false);
	}

	private <T extends IDfPersistentObject> DctmObjectType(CmfType cmsType, Class<T> dfClass,
		BatchItemStrategy batchingStrategy, String dmType, boolean supportsBatching, boolean failureInterruptsBatch) {
		this(cmsType, dfClass, batchingStrategy, dmType, supportsBatching, failureInterruptsBatch, true, true);
	}

	private <T extends IDfPersistentObject> DctmObjectType(CmfType cmsType, Class<T> dfClass,
		BatchItemStrategy batchingStrategy, String dmType, boolean supportsBatching, boolean failureInterruptsBatch,
		boolean supportsTransactions) {
		this(cmsType, dfClass, batchingStrategy, dmType, supportsBatching, failureInterruptsBatch, supportsTransactions,
			true);
	}

	private <T extends IDfPersistentObject> DctmObjectType(CmfType cmsType, Class<T> dfClass,
		BatchItemStrategy batchingStrategy, String dmType, boolean supportsBatching, boolean failureInterruptsBatch,
		boolean supportsTransactions, boolean parallelCapable) {
		this.cmsType = cmsType;
		if (dmType == null) {
			this.dmType = String.format("dm_%s", name().toLowerCase());
		} else {
			this.dmType = dmType;
		}
		this.dfClass = dfClass;
		this.batchingStrategy = batchingStrategy;
		this.supportsBatching = supportsBatching;
		this.failureInterruptsBatch = failureInterruptsBatch;
		this.supportsTransactions = supportsTransactions;
		this.parallelCapable = parallelCapable;
	}

	public final CmfType getStoredObjectType() {
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
	private static Map<CmfType, DctmObjectType> OBJECT_TYPE_TRANSLATOR = null;

	public static DctmObjectType decodeType(IDfPersistentObject object)
		throws DfException, UnsupportedDctmObjectTypeException {
		if (object == null) { throw new IllegalArgumentException("Must provide an object to decode the type from"); }
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
				type = type.getSuperType();
				continue;
			}
		}
		// The only way we get here is if we can't decode into a supported type
		throw new UnsupportedDctmObjectTypeException(typeName);
	}

	private static DctmObjectType decodeType(String type) throws UnsupportedDctmObjectTypeException {
		synchronized (DctmObjectType.class) {
			if (DctmObjectType.DM_TYPE_DECODER == null) {
				Map<String, DctmObjectType> m = new HashMap<String, DctmObjectType>();
				for (DctmObjectType t : DctmObjectType.values()) {
					m.put(t.dmType, t);
				}
				DctmObjectType.DM_TYPE_DECODER = Tools.freezeMap(m);
			}
		}
		if (type == null) { throw new IllegalArgumentException("Must provide a type to decode"); }
		DctmObjectType ret = DctmObjectType.DM_TYPE_DECODER.get(type);
		if (ret == null) { throw new UnsupportedDctmObjectTypeException(type); }
		return ret;
	}

	public static DctmObjectType decodeType(CmfType type) {
		synchronized (DctmObjectType.class) {
			if (DctmObjectType.OBJECT_TYPE_TRANSLATOR == null) {
				Map<CmfType, DctmObjectType> m = new EnumMap<CmfType, DctmObjectType>(CmfType.class);
				for (DctmObjectType t : DctmObjectType.values()) {
					CmfType c = t.getStoredObjectType();
					if (c != null) {
						m.put(c, t);
					}
				}
				DctmObjectType.OBJECT_TYPE_TRANSLATOR = Tools.freezeMap(m);
			}
		}
		return DctmObjectType.OBJECT_TYPE_TRANSLATOR.get(type);
	}

	public static DctmObjectType decodeType(IDfId id) {
		if (id == null) { throw new IllegalArgumentException("Must provide an ID to decode the information from"); }
		// TODO: Not happy with this hardcoded table - maybe find a way to dynamically populate this
		// from the installation?
		switch (id.getTypePart()) {
			case 0x28:
				return DctmObjectType.STORE;

			case 0x11:
				return DctmObjectType.USER;

			case 0x12:
				return DctmObjectType.GROUP;

			case 0x45:
				return DctmObjectType.ACL;

			case 0x03:
				return DctmObjectType.TYPE;

			case 0x27:
				return DctmObjectType.FORMAT;

			case 0x0b: // fall-through, both folders and cabinets map as folders
			case 0x0c:
				return DctmObjectType.FOLDER;

			// case 0x08: // fall-through - dm_sysobject types will be folded into dm_document
			case 0x09:
				return DctmObjectType.DOCUMENT;

			default:
				return null;
		}
	}
}