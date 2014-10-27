package com.delta.cmsmf.cms;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import com.armedia.cmf.storage.StoredObjectType;
import com.armedia.commons.utilities.Tools;
import com.delta.cmsmf.exception.CMSMFException;
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

	USER(StoredObjectType.USER, DctmUser.class, IDfUser.class),
	GROUP(StoredObjectType.GROUP, DctmGroup.class, IDfGroup.class, DctmDependencyType.PEER),
	ACL(StoredObjectType.ACL, DctmACL.class, IDfACL.class),
	TYPE(StoredObjectType.TYPE, DctmType.class, IDfType.class, DctmDependencyType.HIERARCHY, null, true, false),
	FORMAT(StoredObjectType.FORMAT, DctmFormat.class, IDfFormat.class),
	FOLDER(StoredObjectType.FOLDER, DctmFolder.class, IDfFolder.class, DctmDependencyType.HIERARCHY, null, true, false),
	DOCUMENT(StoredObjectType.DOCUMENT, DctmDocument.class, IDfDocument.class, DctmDependencyType.PEER, null, true, true),
	CONTENT(StoredObjectType.CONTENT_STREAM, DctmContentStream.class, IDfContent.class, "dmr_content", DOCUMENT);

	private final StoredObjectType cmsType;
	private final String dmType;
	private final Class<? extends IDfPersistentObject> dfClass;
	private final Class<? extends DctmPersistentObject<?>> objectClass;
	private final DctmDependencyType peerDependencyType;
	private final boolean supportsBatching;
	private final boolean failureInterruptsBatch;
	private final Set<Object> surrogateOf;

	private <T extends IDfPersistentObject, C extends DctmPersistentObject<T>> DctmObjectType(StoredObjectType cmsType,
		Class<C> objectClass, Class<T> dfClass, DctmObjectType... surrogateOf) {
		this(cmsType, objectClass, dfClass, null, null, surrogateOf);
	}

	private <T extends IDfPersistentObject, C extends DctmPersistentObject<T>> DctmObjectType(StoredObjectType cmsType,
		Class<C> objectClass, Class<T> dfClass, String dmType, DctmObjectType... surrogateOf) {
		this(cmsType, objectClass, dfClass, null, dmType, surrogateOf);
	}

	private <T extends IDfPersistentObject, C extends DctmPersistentObject<T>> DctmObjectType(StoredObjectType cmsType,
		Class<C> objectClass, Class<T> dfClass, DctmDependencyType peerDependencyType, DctmObjectType... surrogateOf) {
		this(cmsType, objectClass, dfClass, peerDependencyType, null, surrogateOf);
	}

	private <T extends IDfPersistentObject, C extends DctmPersistentObject<T>> DctmObjectType(StoredObjectType cmsType,
		Class<C> objectClass, Class<T> dfClass, DctmDependencyType peerDependencyType, String dmType,
		DctmObjectType... surrogateOf) {
		this(cmsType, objectClass, dfClass, peerDependencyType, dmType, false, false, surrogateOf);
	}

	private <T extends IDfPersistentObject, C extends DctmPersistentObject<T>> DctmObjectType(StoredObjectType cmsType,
		Class<C> objectClass, Class<T> dfClass, DctmDependencyType peerDependencyType, String dmType,
		boolean supportsBatching, boolean failureInterruptsBatch, DctmObjectType... surrogateOf) {
		this.cmsType = cmsType;
		if (dmType == null) {
			this.dmType = String.format("dm_%s", name().toLowerCase());
		} else {
			this.dmType = dmType;
		}
		this.dfClass = dfClass;
		this.objectClass = objectClass;
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

	public final StoredObjectType getCmsType() {
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

	public final Class<? extends DctmPersistentObject<?>> getCmsObjectClass() {
		return this.objectClass;
	}

	public final DctmPersistentObject<?> newInstance() throws CMSMFException {
		try {
			return this.objectClass.newInstance();
		} catch (Throwable t) {
			throw new CMSMFException(String.format("Failed to instantiate a new object of class [%s] for [%s]",
				this.objectClass.getCanonicalName(), name()), t);
		}
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
	private static Map<String, DctmObjectType> CLASS_DECODER = null;

	public static DctmObjectType decodeType(IDfPersistentObject object) throws DfException,
	UnsupportedObjectTypeException {
		if (object == null) { throw new IllegalArgumentException("Must provide an object to decode the type from"); }
		IDfType type = object.getType();
		while (type != null) {
			try {
				return DctmObjectType.decodeType(type.getName());
			} catch (UnsupportedObjectTypeException e) {
				// This type isn't supported...try its parent
				type = type.getSuperType();
				continue;
			}
		}
		// The only way we get here is if we can't decode into a supported type
		throw new UnsupportedObjectTypeException(object.getType().getName());
	}

	public static DctmObjectType decodeType(String type) throws UnsupportedObjectTypeException {
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
		if (ret == null) { throw new UnsupportedObjectTypeException(type); }
		return ret;
	}

	public static DctmObjectType decodeFromClass(Class<?> klass) {
		synchronized (DctmObjectType.class) {
			if (DctmObjectType.CLASS_DECODER == null) {
				Map<String, DctmObjectType> m = new HashMap<String, DctmObjectType>();
				for (DctmObjectType t : DctmObjectType.values()) {
					m.put(t.objectClass.getCanonicalName(), t);
				}
				DctmObjectType.CLASS_DECODER = Collections.unmodifiableMap(m);
			}
		}
		if (klass == null) { throw new IllegalArgumentException("Must provide a class to decode"); }
		DctmObjectType ret = DctmObjectType.CLASS_DECODER.get(klass.getCanonicalName());
		if (ret == null) { throw new UnsupportedObjectClassException(klass); }
		return ret;
	}
}