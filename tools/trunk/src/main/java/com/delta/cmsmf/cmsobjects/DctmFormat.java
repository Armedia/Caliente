package com.delta.cmsmf.cmsobjects;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import com.delta.cmsmf.constants.DctmAttrNameConstants;
import com.delta.cmsmf.constants.DctmTypeConstants;
import com.delta.cmsmf.exception.CMSMFException;
import com.delta.cmsmf.runtime.DuplicateChecker;
import com.documentum.fc.client.IDfFormat;
import com.documentum.fc.client.IDfPersistentObject;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.common.DfException;

/**
 * The DctmFormat class contains methods to export/import dm_format type of objects from/to
 * Documentum CMS. It also contains methods to export any supporting objects that
 * are needed to replicate a dm_format object in target repository.
 *
 * @author Shridev Makim 6/15/2010
 */
public class DctmFormat extends DctmObject {

	/** The Constant serialVersionUID. */
	private static final long serialVersionUID = 1L;

	// Static variables used to see how many formats were created, skipped, updated
	/** Keeps track of nbr of format objects read from file during import process. */
	private static AtomicInteger formats_read = new AtomicInteger(0);
	/** Keeps track of nbr of format objects skipped due to duplicates during import process. */
	private static AtomicInteger formats_skipped = new AtomicInteger(0);
	/** Keeps track of nbr of format objects updated in CMS during import process. */
	private static AtomicInteger formats_updated = new AtomicInteger(0);
	/** Keeps track of nbr of format objects created in CMS during import process. */
	private static AtomicInteger formats_created = new AtomicInteger(0);

	/** The logger object used for logging. */
	private static Logger logger = Logger.getLogger(DctmFormat.class);

	/**
	 * Instantiates a new DctmFormat object.
	 */
	public DctmFormat() {
		super();
		// set dctmObjectType to dctm_format
		this.dctmObjectType = DctmObjectType.DCTM_FORMAT;
	}

	/**
	 * Instantiates a DctmFormat object with new CMS session.
	 *
	 * @param dctmSession
	 *            the existing documentum CMS session
	 */
	public DctmFormat(IDfSession dctmSession) {
		super(dctmSession);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.delta.cmsmf.cmsobjects.DctmObject#createInCMS()
	 */
	@Override
	public void createInCMS() throws DfException, IOException {
		DctmFormat.formats_read.incrementAndGet();

		if (DctmFormat.logger.isEnabledFor(Level.INFO)) {
			DctmFormat.logger.info("Started creating dctm dm_format in repository");
		}

		// Begin transaction
		this.dctmSession.beginTrans();

		try {
			boolean doesFormatNeedUpdate = false;
			IDfPersistentObject prsstntObj = null;
			// First check to see if the Format already exist; if it does, check to see if we need
// to update it
			String formatName = getStrSingleAttrValue(DctmAttrNameConstants.NAME);

			IDfFormat format = this.dctmSession.getFormat(formatName);
			if (format != null) { // we found existing format
				int versionStamp = format.getVStamp();
				if (versionStamp != getIntSingleAttrValue(DctmAttrNameConstants.I_VSTAMP)) {
					// We need to update the format
					if (DctmFormat.logger.isEnabledFor(Level.DEBUG)) {
						DctmFormat.logger.debug("Format by name " + formatName
							+ " already exist in target repository but needs to be updated.");
					}

					// NOTE Remove the name attribute from attribute map to avoid following error
					// [DM_FORMAT_E_DUPLICATE_NAME] error:
					// "Failed to save format object -- format with name %s already exists"
					removeAttribute(DctmAttrNameConstants.NAME);

					prsstntObj = format;
					doesFormatNeedUpdate = true;
				} else { // Identical format exists in the target repository, abort the transaction
// and quit
					if (DctmFormat.logger.isEnabledFor(Level.DEBUG)) {
						DctmFormat.logger.debug("Identical format " + formatName
							+ " already exists in target repository.");
					}
					this.dctmSession.abortTrans();
					DctmFormat.formats_skipped.incrementAndGet();
					return;
				}
			} else { // format doesn't exist in repo, create one
				if (DctmFormat.logger.isEnabledFor(Level.DEBUG)) {
					DctmFormat.logger.debug("Creating format " + formatName + " in target repository.");
				}
				prsstntObj = this.dctmSession.newObject(DctmTypeConstants.DM_FORMAT);
			}

			// set various attributes
			setAllAttributesInCMS(prsstntObj, this, false, doesFormatNeedUpdate);

			// save the format object
			prsstntObj.save();
			if (doesFormatNeedUpdate) {
				DctmFormat.formats_updated.incrementAndGet();
			} else {
				DctmFormat.formats_created.incrementAndGet();
			}

			// update vStamp of the format object
			updateVStamp(prsstntObj, this);

			if (DctmFormat.logger.isEnabledFor(Level.INFO)) {
				DctmFormat.logger.info("Finished creating dctm dm_format in repository with name: " + formatName);
			}
		} catch (DfException e) {
			// Abort the transaction in case of DfException
			this.dctmSession.abortTrans();
			throw (e);
		}

		// Commit the transaction
		this.dctmSession.commitTrans();
	}

	/**
	 * Prints the import report detailing how many format objects were read, updated, created,
	 * skipped
	 * during the import process.
	 */
	public static void printImportReport() {
		DctmFormat.logger.info("No. of format objects read from file: " + DctmFormat.formats_read);
		DctmFormat.logger.info("No. of format objects skipped due to duplicates: " + DctmFormat.formats_skipped);
		DctmFormat.logger.info("No. of format objects updated: " + DctmFormat.formats_updated);
		DctmFormat.logger.info("No. of format objects created: " + DctmFormat.formats_created);
	}

	/**
	 * Gets the detailed format import report.
	 *
	 * @return the detailed format import report
	 */
	public static String getDetailedFormatImportReport() {
		StringBuffer importReport = new StringBuffer();
		importReport.append("\nNo. of format objects read from file: " + DctmFormat.formats_read + ".");
		importReport.append("\nNo. of format objects skipped due to duplicates: " + DctmFormat.formats_skipped + ".");
		importReport.append("\nNo. of format objects updated: " + DctmFormat.formats_updated + ".");
		importReport.append("\nNo. of format objects created: " + DctmFormat.formats_created + ".");

		return importReport.toString();
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.delta.cmsmf.cmsobjects.DctmObject#getFromCMS(com.documentum.fc.client.IDfPersistentObject)
	 */
	@Override
	protected DctmObject doGetFromCMS(IDfPersistentObject prsstntObj) throws CMSMFException {
		if (DctmFormat.logger.isEnabledFor(Level.INFO)) {
			DctmFormat.logger.info("Started getting dctm dm_format from repository");
		}
		String formatID = "";
		try {
			formatID = prsstntObj.getObjectId().getId();
			String formatName = prsstntObj.getString(DctmAttrNameConstants.NAME);
			// Check if this format has already been exported, if not, add to processed list
			if (!DuplicateChecker.getDuplicateChecker().isFormatProcessed(formatID)) {

				DctmFormat dctmFormat = new DctmFormat();
				getAllAttributesFromCMS(dctmFormat, prsstntObj, formatID);

				return dctmFormat;
			} else {
				if (DctmFormat.logger.isEnabledFor(Level.INFO)) {
					DctmFormat.logger.info("Format " + formatName + " already has been or is being exported!");
				}
			}
		} catch (DfException e) {
			throw (new CMSMFException("Error retrieving format in repository with id: " + formatID, e));
		}
		if (DctmFormat.logger.isEnabledFor(Level.INFO)) {
			DctmFormat.logger.info("Finished getting dctm dm_format from repository with id: " + formatID);
		}

		return null;
	}
}
