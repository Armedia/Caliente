package com.delta.cmsmf.runtime;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

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
	 * Instantiates a new duplicate checker. Private constructor to prevent
	 * new instances being created.
	 */
	private DuplicateChecker() {
		// no code here; this is a singleton class so private constructor
	}

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

	/*
	 * (non-Javadoc)
	 *
	 * @see java.lang.Object#clone()
	 */
	@Override
	public Object clone() throws CloneNotSupportedException {
		throw new CloneNotSupportedException();
		// prevent generation of a clone
	}

	/** The singleton instance. */
	private static DuplicateChecker singletonInstance;

	/** The map of unique folder IDs. */
	private Map<String, Byte> uniqueFolderIDs = new HashMap<String, Byte>();

	/**
	 * Checks if the folder is already processed. Returns True if it is, otherwise it adds it to
	 * the already processed map and returns False.
	 *
	 * @param folderID
	 *            the folder id
	 * @param addFlag
	 *            the add flag
	 * @return true, if the folder is already processed
	 */
	public boolean isFolderProcessed(String folderID, boolean addFlag) {
		boolean isFolderProcessed = false;
		if (this.uniqueFolderIDs.containsKey(folderID)) {
			isFolderProcessed = true;
		} else if (addFlag) {
			this.uniqueFolderIDs.put(folderID, new Byte((byte) 0));
		}

		return isFolderProcessed;
	}

	/** The map of unique user IDs. */
	private Map<String, Byte> uniqueUserIDs = new HashMap<String, Byte>();

	/**
	 * Checks if the user is already processed. Returns True if it is, otherwise it adds it to
	 * the already processed map and returns False.
	 *
	 * @param userID
	 *            the user id
	 * @return true, if the user is already processed
	 */
	public boolean isUserProcessed(String userID) {
		boolean isUserProcessed = false;
		if (this.uniqueUserIDs.containsKey(userID)) {
			isUserProcessed = true;
		} else {
			this.uniqueUserIDs.put(userID, new Byte((byte) 0));
		}

		return isUserProcessed;
	}

	/** The map of unique group IDs. */
	private Map<String, Byte> uniqueGroupIDs = new HashMap<String, Byte>();

	/**
	 * Checks if the group is already processed. Returns True if it is, otherwise it adds it to
	 * the already processed map and returns False.
	 *
	 * @param groupID
	 *            the group id
	 * @param addFlag
	 *            the add flag
	 * @return true, if the group is already processed
	 */
	public boolean isGroupProcessed(String groupID, boolean addFlag) {
		boolean isGroupProcessed = false;
		if (this.uniqueGroupIDs.containsKey(groupID)) {
			isGroupProcessed = true;
		} else if (addFlag) {
			this.uniqueGroupIDs.put(groupID, new Byte((byte) 0));
		}

		return isGroupProcessed;
	}

	/** The map of unique acl ids. */
	private Map<String, Byte> uniqueACLIDs = new HashMap<String, Byte>();

	/**
	 * Checks if the ACL is already processed. Returns True if it is, otherwise it adds it to
	 * the already processed map and returns False.
	 *
	 * @param aclID
	 *            the acl id
	 * @return true, if the ACL is already processed
	 */
	public boolean isACLProcessed(String aclID) {
		boolean isACLProcessed = false;
		if (this.uniqueACLIDs.containsKey(aclID)) {
			isACLProcessed = true;
		} else {
			this.uniqueACLIDs.put(aclID, new Byte((byte) 0));
		}

		return isACLProcessed;
	}

	/** The map of unique format ids. */
	private Map<String, Byte> uniqueFormatIDs = new HashMap<String, Byte>();

	/**
	 * Checks if the format is already processed. Returns True if it is, otherwise it adds it to
	 * the already processed map and returns False.
	 *
	 * @param formatID
	 *            the format id
	 * @return true, if the format is already processed
	 */
	public boolean isFormatProcessed(String formatID) {
		boolean isFormatProcessed = false;
		if (this.uniqueFormatIDs.containsKey(formatID)) {
			isFormatProcessed = true;
		} else {
			this.uniqueFormatIDs.put(formatID, new Byte((byte) 0));
		}

		return isFormatProcessed;
	}

	/** The map of unique type ids. */
	private Map<String, Byte> uniqueTypeIDs = new HashMap<String, Byte>();

	/**
	 * Checks if the type is already processed. Returns True if it is, otherwise it adds it to
	 * the already processed map and returns False.
	 *
	 * @param typeID
	 *            the type id
	 * @return true, if the type is already processed
	 */
	public boolean isTypeProcessed(String typeID) {
		boolean isTypeProcessed = false;
		if (this.uniqueTypeIDs.containsKey(typeID)) {
			isTypeProcessed = true;
		} else {
			this.uniqueTypeIDs.put(typeID, new Byte((byte) 0));
			if (DuplicateChecker.logger.isEnabledFor(Level.INFO)) {
				DuplicateChecker.logger.info("TypeID " + typeID + " was now processed and added to the processed list");
			}
		}

		return isTypeProcessed;
	}

}
