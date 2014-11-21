package com.armedia.cmf.documentum.engine;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import com.armedia.cmf.engine.importer.ImportStrategy;
import com.armedia.cmf.engine.importer.ImportStrategy.BatchingStrategy;
import com.armedia.cmf.storage.StoredObjectType;
import com.documentum.fc.client.IDfACL;
import com.documentum.fc.client.IDfDocument;
import com.documentum.fc.client.IDfFolder;
import com.documentum.fc.client.IDfFormat;
import com.documentum.fc.client.IDfGroup;
import com.documentum.fc.client.IDfPersistentObject;
import com.documentum.fc.client.IDfType;
import com.documentum.fc.client.IDfUser;
import com.documentum.fc.client.content.IDfContent;
import com.documentum.fc.client.content.IDfStore;
import com.documentum.fc.common.DfException;

public enum DctmObjectType {

	// IMPORTANT: The object types must be declared in the proper import order
	// otherwise that operation will fail.

	STORE(StoredObjectType.DATASTORE, IDfStore.class),
	USER(StoredObjectType.USER, IDfUser.class),
	GROUP(StoredObjectType.GROUP, IDfGroup.class, BatchingStrategy.ITEMS_SERIALIZED),
	ACL(StoredObjectType.ACL, IDfACL.class),
	TYPE(StoredObjectType.TYPE, IDfType.class, BatchingStrategy.ITEMS_CONCURRENT, null, true, false),
	FORMAT(StoredObjectType.FORMAT, IDfFormat.class),
	FOLDER(StoredObjectType.FOLDER, IDfFolder.class, BatchingStrategy.ITEMS_CONCURRENT, null, true, false),
	DOCUMENT(StoredObjectType.DOCUMENT, IDfDocument.class, BatchingStrategy.ITEMS_SERIALIZED, null, true, true),
	CONTENT(StoredObjectType.CONTENT_STREAM, IDfContent.class, "dmr_content", DOCUMENT);

	private final StoredObjectType cmsType;
	private final String dmType;
	private final Class<? extends IDfPersistentObject> dfClass;
	private final BatchingStrategy batchingStrategy;
	private final boolean supportsBatching;
	private final boolean failureInterruptsBatch;
	private final Set<Object> surrogateOf;
	public final ImportStrategy importStrategy = new ImportStrategy() {

		@Override
		public boolean isIgnored() {
			return !DctmObjectType.this.surrogateOf.isEmpty();
		}

		@Override
		public BatchingStrategy getBatchingStrategy() {
			if (!DctmObjectType.this.supportsBatching) { return null; }
			return DctmObjectType.this.batchingStrategy;
		}

		@Override
		public boolean isParallelCapable() {
			// All are parallel capable one way or another
			return true;
		}

		@Override
		public boolean isBatchFailRemainder() {
			return DctmObjectType.this.failureInterruptsBatch;
		}
	};

	private <T extends IDfPersistentObject> DctmObjectType(StoredObjectType cmsType, Class<T> dfClass,
		DctmObjectType... surrogateOf) {
		this(cmsType, dfClass, null, null, surrogateOf);
	}

	private <T extends IDfPersistentObject> DctmObjectType(StoredObjectType cmsType, Class<T> dfClass, String dmType,
		DctmObjectType... surrogateOf) {
		this(cmsType, dfClass, null, dmType, surrogateOf);
	}

	private <T extends IDfPersistentObject> DctmObjectType(StoredObjectType cmsType, Class<T> dfClass,
		BatchingStrategy batchingStrategy, DctmObjectType... surrogateOf) {
		this(cmsType, dfClass, batchingStrategy, null, surrogateOf);
	}

	private <T extends IDfPersistentObject> DctmObjectType(StoredObjectType cmsType, Class<T> dfClass,
		BatchingStrategy batchingStrategy, String dmType, DctmObjectType... surrogateOf) {
		this(cmsType, dfClass, batchingStrategy, dmType, false, false, surrogateOf);
	}

	private <T extends IDfPersistentObject> DctmObjectType(StoredObjectType cmsType, Class<T> dfClass,
		BatchingStrategy batchingStrategy, String dmType, boolean supportsBatching, boolean failureInterruptsBatch,
		DctmObjectType... surrogateOf) {
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
		Set<Object> s = null;
		if (surrogateOf != null) {
			s = new TreeSet<Object>();
			for (DctmObjectType t : surrogateOf) {
				if (t != null) {
					s.add(t);
				}
			}
		}
		if ((s == null) || s.isEmpty()) {
			this.surrogateOf = Collections.emptySet();
		} else {
			this.surrogateOf = Collections.unmodifiableSet(s);
		}
	}

	public final StoredObjectType getStoredObjectType() {
		return this.cmsType;
	}

	public final boolean isSurrogate() {
		return !this.surrogateOf.isEmpty();
	}

	public final Set<Object> getSurrogateOf() {
		return this.surrogateOf;
	}

	public final boolean isProperClass(IDfPersistentObject o) {
		if (o == null) { return true; }
		return this.dfClass.isAssignableFrom(o.getClass());
	}

	public final Class<? extends IDfPersistentObject> getDfClass() {
		return this.dfClass;
	}

	private static Map<String, DctmObjectType> DM_TYPE_DECODER = null;

	public static DctmObjectType decodeType(IDfPersistentObject object) throws DfException,
		UnsupportedDctmObjectTypeException {
		if (object == null) { throw new IllegalArgumentException("Must provide an object to decode the type from"); }
		return DctmObjectType.decodeType(object.getType());
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
				DctmObjectType.DM_TYPE_DECODER = Collections.unmodifiableMap(m);
			}
		}
		if (type == null) { throw new IllegalArgumentException("Must provide a type to decode"); }
		DctmObjectType ret = DctmObjectType.DM_TYPE_DECODER.get(type);
		if (ret == null) { throw new UnsupportedDctmObjectTypeException(type); }
		return ret;
	}
}