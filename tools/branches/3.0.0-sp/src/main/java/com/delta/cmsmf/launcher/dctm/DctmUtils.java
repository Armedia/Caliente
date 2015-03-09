package com.delta.cmsmf.launcher.dctm;

import java.util.Date;

import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.delta.cmsmf.cfg.Setting;
import com.documentum.fc.client.IDfDocument;
import com.documentum.fc.client.IDfFolder;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.client.IDfSysObject;
import com.documentum.fc.common.DfException;

class DctmUtils {

	// CMSMF Last Export Related constants
	private static final String LAST_EXPORT_OBJ_NAME = "cmsmf_last_export";

	/** The log object used for logging. */
	static Logger log = LoggerFactory.getLogger(DctmUtils.class);

	private static IDfSysObject getCmsmfStateObject(IDfSession dctmSession, boolean createIfMissing) throws DfException {
		final String targetDocbaseName = dctmSession.getDocbaseName();
		final String cabinetName = Setting.STATE_CABINET.getString();
		final String objectName = DctmUtils.LAST_EXPORT_OBJ_NAME;
		final String cabinetPath = String.format("/%s", cabinetName);
		final String folderPath = String.format("%s/%s", cabinetPath, targetDocbaseName);
		final String documentPath = String.format("%s/%s", folderPath, objectName);
		IDfSysObject lstExportObj = (IDfSysObject) dctmSession.getObjectByPath(documentPath);
		if ((lstExportObj == null) && createIfMissing) {
			// Object does not exist, create one.
			// try to locate a folder for a target repository and create one if it doesn't exist
			IDfFolder trgtDocbaseFolder = dctmSession.getFolderByPath(folderPath);
			if (trgtDocbaseFolder == null) {
				// target folder does not exist, create one.
				// try to locate the cmsmf_sync cabinet and create one if it doesn't exist
				IDfFolder cmsmfSyncCabinet = dctmSession.getFolderByPath(cabinetPath);
				if (cmsmfSyncCabinet == null) {
					DctmUtils.log.info(String.format("Creating cabinet [%s] in source repository", cabinetName));
					// create the cabinet and folder underneath
					cmsmfSyncCabinet = IDfFolder.class.cast(dctmSession.newObject("dm_cabinet"));
					cmsmfSyncCabinet.setObjectName(cabinetName);
					cmsmfSyncCabinet.setHidden(true);
					cmsmfSyncCabinet.save();
				}

				// create a folder for a target repository in this cabinet.
				trgtDocbaseFolder = IDfFolder.class.cast(dctmSession.newObject("dm_folder"));
				trgtDocbaseFolder.setObjectName(targetDocbaseName);
				trgtDocbaseFolder.link(cmsmfSyncCabinet.getObjectId().getId());
				trgtDocbaseFolder.save();
			}
			// Create the object
			lstExportObj = IDfDocument.class.cast(dctmSession.newObject("dm_document"));
			lstExportObj.setObjectName(objectName);
			lstExportObj.link(trgtDocbaseFolder.getObjectId().getId());
			lstExportObj.save();
		}
		return lstExportObj;
	}

	/**
	 * Gets the last export date.
	 *
	 * @param dctmSession
	 *            the dctm session
	 * @return the last export date
	 */
	public static Date getLastExportDate(IDfSession dctmSession) {
		try {
			// Try to locate the last export object to read the date from subject attribute
			IDfSysObject lstExportObj = DctmUtils.getCmsmfStateObject(dctmSession, false);
			final String message;
			String lastExportDate = null;
			if (lstExportObj != null) {
				lastExportDate = lstExportObj.getSubject();
				message = String.format("The last export date was [%s]", lastExportDate);
			} else {
				message = "No previous export date";
			}
			if (DctmUtils.log.isInfoEnabled()) {
				DctmUtils.log.info(message);
			}
			return (lastExportDate != null ? DateUtils.parseDate(lastExportDate,
				DateFormatUtils.ISO_DATETIME_TIME_ZONE_FORMAT.getPattern()) : null);
		} catch (Exception e) {
			DctmUtils.log.error("Error occured while retrieving last export run date", e);
		}
		return null;
	}

	/**
	 * Sets the last export date.
	 *
	 * @param dctmSession
	 *            the dctm session
	 * @param exportDate
	 *            the export date
	 */
	public static void setLastExportDate(IDfSession dctmSession, Date exportDate) {
		try {
			// Try to locate the last export object to read the date from subject attribute
			IDfSysObject lstExportObj = DctmUtils.getCmsmfStateObject(dctmSession, true);
			String lastExport = DateFormatUtils.ISO_DATETIME_TIME_ZONE_FORMAT.format(exportDate);
			lstExportObj.setSubject(lastExport);
			lstExportObj.save();
			if (DctmUtils.log.isInfoEnabled()) {
				DctmUtils.log.info(String.format("Last export date set to [%s]", lastExport));
			}
		} catch (DfException e) {
			DctmUtils.log.error("Error occured while setting last export run date", e);
		}
	}
}