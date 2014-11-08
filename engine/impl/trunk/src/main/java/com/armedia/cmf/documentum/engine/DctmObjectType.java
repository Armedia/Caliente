package com.armedia.cmf.documentum.engine;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import com.armedia.cmf.storage.StoredObjectType;
import com.armedia.commons.utilities.Tools;
import com.documentum.fc.client.IDfACL;
import com.documentum.fc.client.IDfDocument;
import com.documentum.fc.client.IDfFolder;
import com.documentum.fc.client.IDfFormat;
import com.documentum.fc.client.IDfGroup;
import com.documentum.fc.client.IDfPersistentObject;
import com.documentum.fc.client.IDfType;
import com.documentum.fc.client.IDfUser;
import com.documentum.fc.client.content.IDfContent;
import com.documentum.fc.common.DfException;

public enum DctmObjectType {

	// IMPORTANT: The object types must be declared in the proper import order
	// otherwise that operation will fail.

	USER(StoredObjectType.USER, IDfUser.class),
	GROUP(StoredObjectType.GROUP, IDfGroup.class, DctmDependencyType.PEER),
	ACL(StoredObjectType.ACL, IDfACL.class),
	TYPE(StoredObjectType.TYPE, IDfType.class, DctmDependencyType.HIERARCHY, null, true, false),
	FORMAT(StoredObjectType.FORMAT, IDfFormat.class),
	FOLDER(StoredObjectType.FOLDER, IDfFolder.class, DctmDependencyType.HIERARCHY, null, true, false),
	DOCUMENT(StoredObjectType.DOCUMENT, IDfDocument.class, DctmDependencyType.PEER, null, true, true),
	CONTENT(StoredObjectType.CONTENT_STREAM, IDfContent.class, "dmr_content", DOCUMENT);

	private final StoredObjectType cmsType;
	private final String dmType;
	private final Class<? extends IDfPersistentObject> dfClass;
	private final DctmDependencyType peerDependencyType;
	private final boolean supportsBatching;
	private final boolean failureInterruptsBatch;
	private final Set<Object> surrogateOf;

	private <T extends IDfPersistentObject> DctmObjectType(StoredObjectType cmsType, Class<T> dfClass,
		DctmObjectType... surrogateOf) {
		this(cmsType, dfClass, null, null, surrogateOf);
	}

	private <T extends IDfPersistentObject> DctmObjectType(StoredObjectType cmsType, Class<T> dfClass, String dmType,
		DctmObjectType... surrogateOf) {
		this(cmsType, dfClass, null, dmType, surrogateOf);
	}

	private <T extends IDfPersistentObject> DctmObjectType(StoredObjectType cmsType, Class<T> dfClass,
		DctmDependencyType peerDependencyType, DctmObjectType... surrogateOf) {
		this(cmsType, dfClass, peerDependencyType, null, surrogateOf);
	}

	private <T extends IDfPersistentObject> DctmObjectType(StoredObjectType cmsType, Class<T> dfClass,
		DctmDependencyType peerDependencyType, String dmType, DctmObjectType... surrogateOf) {
		this(cmsType, dfClass, peerDependencyType, dmType, false, false, surrogateOf);
	}

	private <T extends IDfPersistentObject> DctmObjectType(StoredObjectType cmsType, Class<T> dfClass,
		DctmDependencyType peerDependencyType, String dmType, boolean supportsBatching, boolean failureInterruptsBatch,
		DctmObjectType... surrogateOf) {
		this.cmsType = cmsType;
		if (dmType == null) {
			this.dmType = String.format("dm_%s", name().toLowerCase());
		} else {
			this.dmType = dmType;
		}
		this.dfClass = dfClass;
		this.peerDependencyType = Tools.coalesce(peerDependencyType, DctmDependencyType.NONE);
		this.supportsBatching = supportsBatching;
		this.failureInterruptsBatch = failureInterruptsBatch;
		Set<Object> s = new TreeSet<Object>();
		if (surrogateOf != null) {
			for (DctmObjectType t : surrogateOf) {
				s.add(t);
			}
		}
		this.surrogateOf = Collections.unmodifiableSet(s);
	}

	public final StoredObjectType getStoredObjectType() {
		return this.cmsType;
	}

	public final boolean isSurrogate() {
		return !this.surrogateOf.isEmpty();
	}

	public final Set<?> getSurrogateOf() {
		return this.surrogateOf;
	}

	public final boolean isProperClass(IDfPersistentObject o) {
		if (o == null) { return true; }
		return this.dfClass.isAssignableFrom(o.getClass());
	}

	public final Class<? extends IDfPersistentObject> getDfClass() {
		return this.dfClass;
	}

	public final DctmDependencyType getPeerDependencyType() {
		return this.peerDependencyType;
	}

	public final boolean isBatchingSupported() {
		return this.supportsBatching;
	}

	public final boolean isFailureInterruptsBatch() {
		return this.failureInterruptsBatch;
	}

	private static Map<String, DctmObjectType> DM_TYPE_DECODER = null;

	public static DctmObjectType decodeType(IDfPersistentObject object) throws DfException,
		UnsupportedDctmObjectTypeException {
		if (object == null) { throw new IllegalArgumentException("Must provide an object to decode the type from"); }
		IDfType type = object.getType();
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
		throw new UnsupportedDctmObjectTypeException(object.getType().getName());
	}

	public static DctmObjectType decodeType(String type) throws UnsupportedDctmObjectTypeException {
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