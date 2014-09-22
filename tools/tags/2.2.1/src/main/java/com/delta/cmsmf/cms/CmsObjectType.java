package com.delta.cmsmf.cms;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

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

public enum CmsObjectType {

	// IMPORTANT: The object types must be declared in the proper import order
	// otherwise that operation will fail.

	USER(CmsUser.class, IDfUser.class),
	GROUP(CmsGroup.class, IDfGroup.class, CmsDependencyType.PEER),
	ACL(CmsACL.class, IDfACL.class),
	TYPE(CmsType.class, IDfType.class, CmsDependencyType.HIERARCHY, null, true),
	FORMAT(CmsFormat.class, IDfFormat.class),
	FOLDER(CmsFolder.class, IDfFolder.class, CmsDependencyType.HIERARCHY, null, true),
	DOCUMENT(CmsDocument.class, IDfDocument.class, CmsDependencyType.PEER, null, true),
	CONTENT(CmsContent.class, IDfContent.class, "dmr_content", DOCUMENT);

	private final String dmType;
	private final Class<? extends IDfPersistentObject> dfClass;
	private final Class<? extends CmsObject<?>> objectClass;
	private final CmsDependencyType peerDependencyType;
	private final boolean supportsBatching;
	private final Set<Object> surrogateOf;

	private <T extends IDfPersistentObject, C extends CmsObject<T>> CmsObjectType(Class<C> objectClass,
		Class<T> dfClass, CmsObjectType... surrogateOf) {
		this(objectClass, dfClass, null, null, surrogateOf);
	}

	private <T extends IDfPersistentObject, C extends CmsObject<T>> CmsObjectType(Class<C> objectClass,
		Class<T> dfClass, String dmType, CmsObjectType... surrogateOf) {
		this(objectClass, dfClass, null, dmType, surrogateOf);
	}

	private <T extends IDfPersistentObject, C extends CmsObject<T>> CmsObjectType(Class<C> objectClass,
		Class<T> dfClass, CmsDependencyType peerDependencyType, CmsObjectType... surrogateOf) {
		this(objectClass, dfClass, peerDependencyType, null, surrogateOf);
	}

	private <T extends IDfPersistentObject, C extends CmsObject<T>> CmsObjectType(Class<C> objectClass,
		Class<T> dfClass, CmsDependencyType peerDependencyType, String dmType, CmsObjectType... surrogateOf) {
		this(objectClass, dfClass, peerDependencyType, dmType, false, surrogateOf);
	}

	private <T extends IDfPersistentObject, C extends CmsObject<T>> CmsObjectType(Class<C> objectClass,
		Class<T> dfClass, CmsDependencyType peerDependencyType, String dmType, boolean supportsBatching,
		CmsObjectType... surrogateOf) {
		if (dmType == null) {
			this.dmType = String.format("dm_%s", name().toLowerCase());
		} else {
			this.dmType = dmType;
		}
		this.dfClass = dfClass;
		this.objectClass = objectClass;
		this.peerDependencyType = Tools.coalesce(peerDependencyType, CmsDependencyType.NONE);
		this.supportsBatching = supportsBatching;
		Set<Object> s = new TreeSet<Object>();
		if (surrogateOf != null) {
			for (CmsObjectType t : surrogateOf) {
				s.add(t);
			}
		}
		this.surrogateOf = Collections.unmodifiableSet(s);
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

	public final Class<? extends CmsObject<?>> getCmsObjectClass() {
		return this.objectClass;
	}

	public final CmsObject<?> newInstance() throws CMSMFException {
		try {
			return this.objectClass.newInstance();
		} catch (Throwable t) {
			throw new CMSMFException(String.format("Failed to instantiate a new object of class [%s] for [%s]",
				this.objectClass.getCanonicalName(), name()), t);
		}
	}

	public final CmsDependencyType getPeerDependencyType() {
		return this.peerDependencyType;
	}

	public final boolean isBatchingSupported() {
		return this.supportsBatching;
	}

	private static Map<String, CmsObjectType> DM_TYPE_DECODER = null;
	private static Map<String, CmsObjectType> CLASS_DECODER = null;

	public static CmsObjectType decodeType(IDfPersistentObject object) throws DfException,
		UnsupportedObjectTypeException {
		if (object == null) { throw new IllegalArgumentException("Must provide an object to decode the type from"); }
		IDfType type = object.getType();
		while (type != null) {
			try {
				return CmsObjectType.decodeType(type.getName());
			} catch (UnsupportedObjectTypeException e) {
				// This type isn't supported...try its parent
				type = type.getSuperType();
				continue;
			}
		}
		// The only way we get here is if we can't decode into a supported type
		throw new UnsupportedObjectTypeException(object.getType().getName());
	}

	public static CmsObjectType decodeType(String type) throws UnsupportedObjectTypeException {
		synchronized (CmsObjectType.class) {
			if (CmsObjectType.DM_TYPE_DECODER == null) {
				Map<String, CmsObjectType> m = new HashMap<String, CmsObjectType>();
				for (CmsObjectType t : CmsObjectType.values()) {
					m.put(t.dmType, t);
				}
				CmsObjectType.DM_TYPE_DECODER = Collections.unmodifiableMap(m);
			}
		}
		if (type == null) { throw new IllegalArgumentException("Must provide a type to decode"); }
		CmsObjectType ret = CmsObjectType.DM_TYPE_DECODER.get(type);
		if (ret == null) { throw new UnsupportedObjectTypeException(type); }
		return ret;
	}

	public static CmsObjectType decodeFromClass(Class<?> klass) {
		synchronized (CmsObjectType.class) {
			if (CmsObjectType.CLASS_DECODER == null) {
				Map<String, CmsObjectType> m = new HashMap<String, CmsObjectType>();
				for (CmsObjectType t : CmsObjectType.values()) {
					m.put(t.objectClass.getCanonicalName(), t);
				}
				CmsObjectType.CLASS_DECODER = Collections.unmodifiableMap(m);
			}
		}
		if (klass == null) { throw new IllegalArgumentException("Must provide a class to decode"); }
		CmsObjectType ret = CmsObjectType.CLASS_DECODER.get(klass.getCanonicalName());
		if (ret == null) { throw new UnsupportedObjectClassException(klass); }
		return ret;
	}
}