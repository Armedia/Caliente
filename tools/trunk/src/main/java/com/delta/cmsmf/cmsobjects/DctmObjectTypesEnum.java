package com.delta.cmsmf.cmsobjects;

import java.util.HashMap;
import java.util.Map;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import com.delta.cmsmf.mainEngine.CMSMFMain;

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

	private final String targetName;

	private final AtomicInteger counter = new AtomicInteger(0);

	private DctmObjectTypesEnum() {
		this.dmType = name().toLowerCase().replaceAll("^dctm_", "dm_");
		String rest = null;
		int idx = name().indexOf('_');
		if (idx < 0) {
			rest = name();
		} else {
			rest = name().substring(idx + 1);
			if (rest.length() < 1) {
				rest = name();
			}
		}
		this.targetName = rest.toLowerCase();
	}

	public String getDocumentumType() {
		return this.dmType;
	}

	public String getNextId() {
		return String.format("%08x", this.counter.getAndIncrement());
	}

	public File getBaseFolder() throws IOException {
		File baseFolder = CMSMFMain.getInstance().getStreamFilesDirectory();
		baseFolder = new File(baseFolder, this.targetName);
		baseFolder = baseFolder.getCanonicalFile();
		if (!baseFolder.exists() && !baseFolder.mkdirs()) {
			throw new IOException(String.format("Failed to create the directory [%s]", baseFolder));
		} else if (!baseFolder.isDirectory()) { throw new IOException(String.format("The path [%s] is not a directory",
			baseFolder)); }
		return baseFolder;
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
