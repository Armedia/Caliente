package com.delta.cmsmf.runtime;

import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import com.delta.cmsmf.cmsobjects.DctmObjectType;

/**
 * The Class DuplicateChecker. This class implements singleton design pattern and maintains exactly
 * one
 * instance of this class during execution. This class is used through out the application to
 * prevent
 * serializing duplicate folders/groups/users/types/acls/formats objects.
 *
 * The class maintains a Map for already processed objects for each type. Map is used instead of
 * list to
 * facilitate faster lookup. The object id of already processed object is stored as the key in the
 * map and to
 * conserve space, a simple byte is stored in a value in {@code <key, value>} pair of the map.
 *
 * @author Shridev Makim 6/15/2010
 */
public class DuplicateChecker {

	/** The logger object used for logging. */
	static Logger logger = Logger.getLogger(DuplicateChecker.class);

	/**
	 * Gets the singleton instance of the duplicate checker class.
	 *
	 * @return the duplicate checker singleton instance
	 */
	public static synchronized DuplicateChecker getDuplicateChecker() {
		if (DuplicateChecker.singletonInstance == null) {
			// we can call this private constructor
			DuplicateChecker.singletonInstance = new DuplicateChecker();
		}
		return DuplicateChecker.singletonInstance;
	}

	/** The singleton instance. */
	private static DuplicateChecker singletonInstance;

	private final Map<DctmObjectType, Set<String>> uniqueIds;

	/**
	 * Instantiates a new duplicate checker. Private constructor to prevent
	 * new instances being created.
	 */
	private DuplicateChecker() {
		Map<DctmObjectType, Set<String>> uniqueIds = new EnumMap<DctmObjectType, Set<String>>(
			DctmObjectType.class);
		for (DctmObjectType v : DctmObjectType.values()) {
			uniqueIds.put(v, Collections.synchronizedSet(new HashSet<String>()));
		}
		this.uniqueIds = Collections.unmodifiableMap(uniqueIds);
	}

	private boolean isProcessed(DctmObjectType type, String id) {
		return !this.uniqueIds.get(type).add(id);
	}

	/**
	 * Checks if the folder is already processed. Returns True if it is, otherwise it adds it to
	 * the already processed map and returns False.
	 *
	 * @param id
	 *            the folder id
	 * @param addFlag
	 *            the add flag
	 * @return true, if the folder is already processed
	 */
	public boolean isFolderProcessed(String id, boolean addFlag) {
		return isProcessed(DctmObjectType.DCTM_FOLDER, id);
	}

	/**
	 * Checks if the user is already processed. Returns True if it is, otherwise it adds it to
	 * the already processed map and returns False.
	 *
	 * @param id
	 *            the user id
	 * @return true, if the user is already processed
	 */
	public boolean isUserProcessed(String id) {
		return isProcessed(DctmObjectType.DCTM_USER, id);
	}

	/**
	 * Checks if the group is already processed. Returns True if it is, otherwise it adds it to
	 * the already processed map and returns False.
	 *
	 * @param id
	 *            the group id
	 * @param addFlag
	 *            the add flag
	 * @return true, if the group is already processed
	 */
	public boolean isGroupProcessed(String id, boolean addFlag) {
		return isProcessed(DctmObjectType.DCTM_GROUP, id);
	}

	/**
	 * Checks if the ACL is already processed. Returns True if it is, otherwise it adds it to
	 * the already processed map and returns False.
	 *
	 * @param id
	 *            the acl id
	 * @return true, if the ACL is already processed
	 */
	public synchronized boolean isACLProcessed(String id) {
		return isProcessed(DctmObjectType.DCTM_ACL, id);
	}

	/**
	 * Checks if the format is already processed. Returns True if it is, otherwise it adds it to
	 * the already processed map and returns False.
	 *
	 * @param id
	 *            the format id
	 * @return true, if the format is already processed
	 */
	public synchronized boolean isFormatProcessed(String id) {
		return isProcessed(DctmObjectType.DCTM_FORMAT, id);
	}

	/**
	 * Checks if the type is already processed. Returns True if it is, otherwise it adds it to
	 * the already processed map and returns False.
	 *
	 * @param id
	 *            the type id
	 * @return true, if the type is already processed
	 */
	public boolean isTypeProcessed(String id) {
		return isProcessed(DctmObjectType.DCTM_TYPE, id);
	}

}
