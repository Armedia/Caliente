package com.delta.cmsmf.cmsobjects;

import java.util.HashMap;
import java.util.Map;

/**
 * The DctmObjectTypesEnum class holds enumerations for various documentum object types that are
 * handled by cmsmf application.
 *
 * @author Shridev Makim 6/15/2010
 */
public enum DctmObjectTypesEnum {

	/** The enum for dm_user type. */
	DCTM_USER,
	/** The enum for dm_group type. */
	DCTM_GROUP,
	/** The enum for dm_acl type. */
	DCTM_ACL,
	/** The enum for dm_type type. */
	DCTM_TYPE,
	/** The enum for dm_format type. */
	DCTM_FORMAT,
	/** The enum for dm_folder type. */
	DCTM_FOLDER,
	/** The enum for dm_document type. */
	DCTM_DOCUMENT,
	/** The enum for dmr_content type. */
	DCTM_CONTENT;

	private final String dmType;
	private final String name;

	private DctmObjectTypesEnum() {
		this.dmType = name().toLowerCase().replaceAll("^dctm_", "dm_");
		this.name = name().toLowerCase().replaceAll("^dctm_", "");
	}

	public String getName() {
		return this.name;
	}

	public String getDocumentumType() {
		return this.dmType;
	}

	private static Map<String, DctmObjectTypesEnum> DECODER = null;

	public static DctmObjectTypesEnum decode(String str) {
		synchronized (DctmObjectTypesEnum.class) {
			if (DctmObjectTypesEnum.DECODER == null) {
				DctmObjectTypesEnum.DECODER = new HashMap<String, DctmObjectTypesEnum>();
				for (DctmObjectTypesEnum t : DctmObjectTypesEnum.values()) {
					DctmObjectTypesEnum.DECODER.put(t.dmType, t);
				}
			}
		}
		return DctmObjectTypesEnum.DECODER.get(str);
	}
}
