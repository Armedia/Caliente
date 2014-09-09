package com.delta.cmsmf.cms;

import java.util.HashMap;
import java.util.Map;

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
	TYPE(CmsType.class, IDfType.class, CmsDependencyType.HIERARCHY),
	FORMAT(CmsFormat.class, IDfFormat.class),
	FOLDER(CmsFolder.class, IDfFolder.class),
	DOCUMENT(CmsDocument.class, IDfDocument.class) {
		@Override
		protected CmsObjectType getActualType(IDfPersistentObject obj) throws DfException {
			if (obj instanceof IDfDocument) {
				IDfDocument doc = IDfDocument.class.cast(obj);
				if (doc.isReference()) { return CmsObjectType.DOCUMENT_REFERENCE; }
			}
			return super.getActualType(obj);
		}
	},
	CONTENT(CmsContent.class, IDfContent.class, "dmr_content"),
	DOCUMENT_REFERENCE(CmsDocumentReference.class, IDfDocument.class, CmsDependencyType.PEER);

	private final String dmType;
	private final Class<? extends IDfPersistentObject> dfClass;
	private final Class<? extends CmsObject<?>> objectClass;
	private final CmsDependencyType peerDependencyType;

	private <T extends IDfPersistentObject, C extends CmsObject<T>> CmsObjectType(Class<C> objectClass, Class<T> dfClass) {
		this(objectClass, dfClass, null, null);
	}

	private <T extends IDfPersistentObject, C extends CmsObject<T>> CmsObjectType(Class<C> objectClass,
		Class<T> dfClass, String dmType) {
		this(objectClass, dfClass, null, dmType);
	}

	private <T extends IDfPersistentObject, C extends CmsObject<T>> CmsObjectType(Class<C> objectClass,
		Class<T> dfClass, CmsDependencyType peerDependencyType) {
		this(objectClass, dfClass, peerDependencyType, null);
	}

	private <T extends IDfPersistentObject, C extends CmsObject<T>> CmsObjectType(Class<C> objectClass,
		Class<T> dfClass, CmsDependencyType peerDependencyType, String dmType) {
		if (dmType == null) {
			this.dmType = String.format("dm_%s", name().toLowerCase());
		} else {
			this.dmType = dmType;
		}
		this.dfClass = dfClass;
		this.objectClass = objectClass;
		this.peerDependencyType = Tools.coalesce(peerDependencyType, CmsDependencyType.NONE);
	}

	/**
	 * <p>
	 * This method permits individual instances of types to determine alternative object types given
	 * special circumstances (for instance, reference documents vs. documents).
	 * </p>
	 *
	 * @param obj
	 * @return the actual type
	 */
	protected CmsObjectType getActualType(IDfPersistentObject obj) throws DfException {
		return this;
	}

	public final boolean isProperClass(IDfPersistentObject o) {
		if (o == null) { return true; }
		return this.dfClass.isAssignableFrom(o.getClass());
	}

	public final Class<? extends IDfPersistentObject> getDfClass() {
		return this.dfClass;
	}

	public final Class<? extends CmsObject<?>> getObjectClass() {
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

	public final String getDocumentumType() {
		return this.dmType;
	}

	public final CmsDependencyType getPeerDependencyType() {
		return this.peerDependencyType;
	}

	private static Map<String, CmsObjectType> DECODER = null;

	public static CmsObjectType decodeType(IDfPersistentObject object) throws DfException {
		synchronized (CmsObjectType.class) {
			if (CmsObjectType.DECODER == null) {
				CmsObjectType.DECODER = new HashMap<String, CmsObjectType>();
				for (CmsObjectType t : CmsObjectType.values()) {
					CmsObjectType.DECODER.put(t.dmType, t);
				}
			}
		}
		final String type = object.getType().getName();
		CmsObjectType ret = CmsObjectType.DECODER.get(type);
		if (ret == null) { throw new IllegalArgumentException(String.format("Unsupported object type [%s]", type)); }
		return ret.getActualType(object);
	}
}