package com.delta.cmsmf.datastore.cms;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

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

/**
 * The CmsObjectType class holds enumerations for various documentum object types that are
 * handled by cmsmf application.
 *
 * @author Shridev Makim 6/15/2010
 */
public enum CmsObjectType {

	// IMPORTANT: The object types must be declared in the proper import order
	// otherwise that operation will fail.

	USER(CmsUser.class, IDfUser.class, false, true),
	GROUP(CmsGroup.class, IDfGroup.class, true, true),
	ACL(CmsACL.class, IDfACL.class, false, true),
	TYPE(CmsType.class, IDfType.class, false, false),
	FORMAT(CmsFormat.class, IDfFormat.class, false, true),
	FOLDER(CmsFolder.class, IDfFolder.class, false, true),
	DOCUMENT(CmsDocument.class, IDfDocument.class, true, true) {
	/*
	@Override
	protected CmsObjectType getActualType(IDfPersistentObject obj) {
	if (obj instanceof IDfDocument) {
	IDfDocument doc = IDfDocument.class.cast(obj);
	if (doc.isReference()) { return CmsObjectType.REFERENCE_DOCUMENT; }
	}
	return super.getActualType(obj);
	}
	 */
	},
	// REFERENCE_DOCUMENT(CmsReferenceDocument.class, IDfDocument.class),
	CONTENT(CmsContent.class, IDfContent.class, false, true);

	private final String dmType;
	private final Class<? extends IDfPersistentObject> dfClass;
	private final Class<? extends CmsObject<?>> objectClass;
	private final boolean horizontalDependencies;
	private final boolean supportsParallelImport;

	private CmsObjectType(Class<? extends CmsObject<?>> objectClass, Class<? extends IDfPersistentObject> dfClass,
		boolean horizontalDependencies, boolean supportsParallelImport) {
		this.dmType = String.format("dm_%s", name().toLowerCase());
		this.dfClass = dfClass;
		this.objectClass = objectClass;
		this.horizontalDependencies = horizontalDependencies;
		this.supportsParallelImport = supportsParallelImport;
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
	protected CmsObjectType getActualType(IDfPersistentObject obj) {
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

	public final CmsObject<?> newInstance() throws InstantiationException, IllegalAccessException,
		InvocationTargetException {
		return this.objectClass.newInstance();
	}

	public final String getDocumentumType() {
		return this.dmType;
	}

	public final boolean isHorizontalDependencies() {
		return this.horizontalDependencies;
	}

	public final boolean isSupportsParallelImport() {
		return this.supportsParallelImport;
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