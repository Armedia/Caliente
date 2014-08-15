package com.delta.cmsmf.runtime;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import com.delta.cmsmf.cmsobjects.DctmObjectTypesEnum;

/**
 * The Class AppCounter keeps running tab of number of various types of objects processed during
 * export or
 * import step. This class implements singleton design pattern to maintain single set of properties
 * through
 * out the execution.
 * <p>
 * This class contains a counter for each type of object that cmsmf application is able to handle.
 * 
 * @author Shridev Makim 6/15/2010
 */
public class AppCounter {

	/** The logger object used for logging. */
	static Logger logger = Logger.getLogger(AppCounter.class);

	/**
	 * Instantiates a new App counter. Private constructor to prevent
	 * new instances being created.
	 */
	private AppCounter() {
		// no code here; this is a singleton class so private constructor
	}

	/**
	 * Gets the singleton instance of App counter class.
	 * 
	 * @return the object counter
	 */
	public static synchronized AppCounter getObjectCounter() {
		if (AppCounter.singletonInstance == null) {
			// we can call this private constructor
			AppCounter.singletonInstance = new AppCounter();
		}
		return AppCounter.singletonInstance;
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
	private static AppCounter singletonInstance;

	/** The document counter. */
	private int documentCounter = 0;

	/** The folder counter. */
	private int folderCounter = 0;

	/** The user counter. */
	private int userCounter = 0;

	/** The group counter. */
	private int groupCounter = 0;

	/** The acl counter. */
	private int aclCounter = 0;

	/** The type counter. */
	private int typeCounter = 0;

	/** The format counter. */
	private int formatCounter = 0;

	/**
	 * Gets the document counter.
	 * 
	 * @return the document counter
	 */
	public int getDocumentCounter() {
		return this.documentCounter;
	}

	/**
	 * Gets the folder counter.
	 * 
	 * @return the folder counter
	 */
	public int getFolderCounter() {
		return this.folderCounter;
	}

	/**
	 * Gets the user counter.
	 * 
	 * @return the user counter
	 */
	public int getUserCounter() {
		return this.userCounter;
	}

	/**
	 * Gets the group counter.
	 * 
	 * @return the group counter
	 */
	public int getGroupCounter() {
		return this.groupCounter;
	}

	/**
	 * Gets the acl counter.
	 * 
	 * @return the acl counter
	 */
	public int getAclCounter() {
		return this.aclCounter;
	}

	/**
	 * Gets the type counter.
	 * 
	 * @return the type counter
	 */
	public int getTypeCounter() {
		return this.typeCounter;
	}

	/**
	 * Gets the format counter.
	 * 
	 * @return the format counter
	 */
	public int getFormatCounter() {
		return this.formatCounter;
	}

	/**
	 * Prints the counters to a log file.
	 */
	public void printCounters() {
		AppCounter.logger.info("Total nbr of documents processed: " + this.documentCounter);
		AppCounter.logger.info("Total nbr of folders processed: " + this.folderCounter);
		AppCounter.logger.info("Total nbr of users processed: " + this.userCounter);
		AppCounter.logger.info("Total nbr of groups processed: " + this.groupCounter);
		AppCounter.logger.info("Total nbr of acls processed: " + this.aclCounter);
		AppCounter.logger.info("Total nbr of types processed: " + this.typeCounter);
		AppCounter.logger.info("Total nbr of formats processed: " + this.formatCounter);
	}

	/**
	 * Increments a counter for a gievn object type.
	 * 
	 * @param dctmObjectType
	 *            the dctm object type
	 */
	public void incrementCounter(DctmObjectTypesEnum dctmObjectType) {
		switch (dctmObjectType) {
			case DCTM_DOCUMENT:
				this.documentCounter++;
				if ((this.documentCounter % 100) == 0) {
					if (AppCounter.logger.isEnabledFor(Level.INFO)) {
						AppCounter.logger.info("INFO:: Processed " + this.documentCounter + " documents so far.");
					}
				}
				break;
			case DCTM_FOLDER:
				this.folderCounter++;
				break;
			case DCTM_USER:
				this.userCounter++;
				break;
			case DCTM_GROUP:
				this.groupCounter++;
				break;
			case DCTM_ACL:
				this.aclCounter++;
				break;
			case DCTM_TYPE:
				this.typeCounter++;
				break;
			case DCTM_FORMAT:
				this.formatCounter++;
				break;
			default:
				AppCounter.logger.warn("Trying to increment invalid dctm type counter!");
		}

	}

	/**
	 * Resets all of the counters to 0.
	 */
	public void resetCounters() {
		this.documentCounter = 0;
		this.folderCounter = 0;
		this.userCounter = 0;
		this.groupCounter = 0;
		this.aclCounter = 0;
		this.typeCounter = 0;
		this.formatCounter = 0;
	}
}
