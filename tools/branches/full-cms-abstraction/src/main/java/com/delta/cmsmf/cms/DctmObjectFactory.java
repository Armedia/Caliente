package com.delta.cmsmf.cms;

import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

import com.armedia.cmf.documentum.engine.DctmObjectType;
import com.armedia.cmf.documentum.engine.UnsupportedDctmObjectTypeException;
import com.delta.cmsmf.exception.CMSMFException;

public class DctmObjectFactory {

	private static final Map<DctmObjectType, Class<? extends DctmPersistentObject<?>>> TYPE_MAP;
	private static final Map<String, DctmObjectType> REVERSE_MAP;

	static {
		Map<DctmObjectType, Class<? extends DctmPersistentObject<?>>> m = new EnumMap<DctmObjectType, Class<? extends DctmPersistentObject<?>>>(
			DctmObjectType.class);
		m.put(DctmObjectType.ACL, DctmACL.class);
		m.put(DctmObjectType.CONTENT, DctmContentStream.class);
		m.put(DctmObjectType.DOCUMENT, DctmDocument.class);
		m.put(DctmObjectType.FOLDER, DctmFolder.class);
		m.put(DctmObjectType.FORMAT, DctmFormat.class);
		m.put(DctmObjectType.GROUP, DctmGroup.class);
		m.put(DctmObjectType.TYPE, DctmType.class);
		m.put(DctmObjectType.USER, DctmUser.class);
		TYPE_MAP = Collections.unmodifiableMap(m);

		Map<String, DctmObjectType> r = new HashMap<String, DctmObjectType>();
		r.put(DctmACL.class.getCanonicalName(), DctmObjectType.ACL);
		r.put(DctmContentStream.class.getCanonicalName(), DctmObjectType.CONTENT);
		r.put(DctmDocument.class.getCanonicalName(), DctmObjectType.DOCUMENT);
		r.put(DctmFolder.class.getCanonicalName(), DctmObjectType.FOLDER);
		r.put(DctmFormat.class.getCanonicalName(), DctmObjectType.FORMAT);
		r.put(DctmGroup.class.getCanonicalName(), DctmObjectType.GROUP);
		r.put(DctmType.class.getCanonicalName(), DctmObjectType.TYPE);
		r.put(DctmUser.class.getCanonicalName(), DctmObjectType.USER);
		REVERSE_MAP = Collections.unmodifiableMap(r);
	}

	public static Class<? extends DctmPersistentObject<?>> decodeType(DctmObjectType t) {
		if (t == null) { throw new IllegalArgumentException("Must provide a type to decode"); }
		return DctmObjectFactory.TYPE_MAP.get(t);
	}

	public static DctmObjectType decodeClass(Class<?> klass) {
		if (klass == null) { throw new IllegalArgumentException("Must provide a class to decode"); }
		return DctmObjectFactory.REVERSE_MAP.get(klass.getCanonicalName());
	}

	public static DctmPersistentObject<?> newInstance(DctmObjectType t) throws CMSMFException,
	UnsupportedDctmObjectTypeException {
		if (t == null) { throw new IllegalArgumentException("Must provide a type to decode"); }
		Class<? extends DctmPersistentObject<?>> klass = DctmObjectFactory.decodeType(t);
		try {
			return klass.newInstance();
		} catch (Exception e) {
			throw new CMSMFException(String.format("Exception caught while instantiating a new instance of %s",
				klass.getSimpleName()), e);
		}
	}
}