package com.delta.cmsmf.cmsobjects;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

import com.delta.cmsmf.datastore.DataObject;

/**
 * The DctmObjectType class holds enumerations for various documentum object types that are
 * handled by cmsmf application.
 *
 * @author Shridev Makim 6/15/2010
 */
public enum DctmObjectType {

	/** The enum for dm_user type. */
	DCTM_USER(DctmUser.class),
	/** The enum for dm_group type. */
	DCTM_GROUP(DctmGroup.class),
	/** The enum for dm_acl type. */
	DCTM_ACL(DctmACL.class),
	/** The enum for dm_type type. */
	DCTM_TYPE(DctmType.class),
	/** The enum for dm_format type. */
	DCTM_FORMAT(DctmFormat.class),
	/** The enum for dm_folder type. */
	DCTM_FOLDER(DctmFolder.class),
	/** The enum for dm_document type. */
	DCTM_DOCUMENT(DctmDocument.class),
	/** The enum for dmr_content type. */
	DCTM_CONTENT(DctmContent.class);

	private final String dmType;
	private final String name;
	private final Class<? extends DctmObject> objectClass;
	private final Constructor<? extends DctmObject> constructor;

	private DctmObjectType(Class<? extends DctmObject> objectClass) {
		this.dmType = name().toLowerCase().replaceAll("^dctm_", "dm_");
		this.name = name().toLowerCase().replaceAll("^dctm_", "");
		this.objectClass = objectClass;
		try {
			this.constructor = objectClass.getConstructor(DataObject.class);
		} catch (SecurityException e) {
			throw new RuntimeException(String.format("Failed to locate the required constructor for %s", name()), e);
		} catch (NoSuchMethodException e) {
			throw new RuntimeException(String.format("Failed to locate the required constructor for %s", name()), e);
		}
	}

	public final String getName() {
		return this.name;
	}

	public final Class<? extends DctmObject> getObjectClass() {
		return this.objectClass;
	}

	public final DctmObject newInstance(DataObject dataObject) throws InstantiationException, IllegalAccessException,
		InvocationTargetException {
		return this.constructor.newInstance(dataObject);
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
