package com.delta.cmsmf.cmsobjects;

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

/**
 * The CmsObjectType class holds enumerations for various documentum object types that are
 * handled by cmsmf application.
 *
 * @author Shridev Makim 6/15/2010
 */
public enum DctmObjectType {

	// IMPORTANT: The object types must be declared in the proper import order
	// otherwise that operation will fail.

	/** The enum for dm_user type. */
	DCTM_USER(DctmUser.class, IDfUser.class),
	/** The enum for dm_group type. */
	DCTM_GROUP(DctmGroup.class, IDfGroup.class),
	/** The enum for dm_acl type. */
	DCTM_ACL(DctmACL.class, IDfACL.class),
	/** The enum for dm_type type. */
	DCTM_TYPE(DctmType.class, IDfType.class),
	/** The enum for dm_format type. */
	DCTM_FORMAT(DctmFormat.class, IDfFormat.class),
	/** The enum for dm_folder type. */
	DCTM_FOLDER(DctmFolder.class, IDfFolder.class),
	/** The enum for dm_document type. */
	DCTM_DOCUMENT(DctmDocument.class, IDfDocument.class),
	/** The enum for dmr_content type. */
	DCTM_CONTENT(DctmContent.class, IDfContent.class),
	/** The enum for dm_document type. */
	DCTM_REFERENCE_DOCUMENT(DctmReferenceDocument.class, IDfDocument.class);

	private final String dmType;
	private final String name;
	private final Class<? extends IDfPersistentObject> dfClass;
	private final Class<? extends DctmObject<?>> objectClass;

	private DctmObjectType(Class<? extends DctmObject<?>> objectClass, Class<? extends IDfPersistentObject> dfClass) {
		this.dmType = name().toLowerCase().replaceAll("^dctm_", "dm_");
		this.name = name().toLowerCase().replaceAll("^dctm_", "");
		this.dfClass = dfClass;
		this.objectClass = objectClass;
	}

	public final String getName() {
		return this.name;
	}

	public final boolean isProperClass(IDfPersistentObject o) {
		if (o == null) { return true; }
		return this.dfClass.isAssignableFrom(o.getClass());
	}

	public final Class<? extends DctmObject<?>> getObjectClass() {
		return this.objectClass;
	}

	public final DctmObject<?> newInstance() throws InstantiationException, IllegalAccessException,
		InvocationTargetException {
		return this.objectClass.newInstance();
	}

	public final String getDocumentumType() {
		return this.dmType;
	}

	private static Map<String, DctmObjectType> DECODER = null;

	public static DctmObjectType decode(String str) {
		synchronized (DctmObjectType.class) {
			if (DctmObjectType.DECODER == null) {
				DctmObjectType.DECODER = new HashMap<String, DctmObjectType>();
				for (DctmObjectType t : DctmObjectType.values()) {
					DctmObjectType.DECODER.put(t.dmType, t);
				}
			}
		}
		return DctmObjectType.DECODER.get(str);
	}
}
