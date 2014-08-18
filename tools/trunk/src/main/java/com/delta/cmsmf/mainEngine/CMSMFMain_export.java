package com.delta.cmsmf.mainEngine;

import java.io.File;
import java.io.IOException;
import java.util.Date;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateFormatUtils;
import org.apache.log4j.Level;

import com.delta.cmsmf.cmsobjects.DctmObject;
import com.delta.cmsmf.constants.CMSMFAppConstants;
import com.delta.cmsmf.constants.CMSMFProperties;
import com.delta.cmsmf.exception.CMSMFException;
import com.delta.cmsmf.exception.CMSMFFatalException;
import com.delta.cmsmf.exception.CMSMFIOException;
import com.delta.cmsmf.filestreams.FileStreamsManager;
import com.delta.cmsmf.properties.PropertiesManager;
import com.delta.cmsmf.runtime.AppCounter;
import com.delta.cmsmf.serialization.DctmObjectWriter;
import com.delta.cmsmf.utils.CMSMFUtils;
import com.documentum.com.DfClientX;
import com.documentum.fc.client.IDfCollection;
import com.documentum.fc.client.IDfPersistentObject;
import com.documentum.fc.client.IDfQuery;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.common.DfException;
import com.documentum.fc.common.DfId;

/**
 * The main method of this class is an entry point for the cmsmf application.
 *
 * @author Shridev Makim 6/15/2010
 */
public class CMSMFMain_export extends CMSMFMain {

	private class Worker implements Runnable {

		private final String objectId;
		private final FileStreamsManager fsm;

		private Worker(FileStreamsManager fsm, String objectId) {
			this.objectId = objectId;
			this.fsm = fsm;
		}

		@Override
		public void run() {
			// get the object
			IDfSession session = null;
			try {
				session = getSession();
			} catch (Throwable t) {
				throw new RuntimeException("Failed to obtain a working session");
			}
			IDfPersistentObject prsstntObj = null;
			try {
				prsstntObj = session.getObject(new DfId(this.objectId));
			} catch (DfException e) {
				CMSMFMain_export.this.logger.error("Couldn't retrieve object by ID: " + this.objectId, e);
			}

			DctmObjectRetriever dctmObjRetriever = new DctmObjectRetriever(session);
			DctmObject dctmObj;
			try {
				if (prsstntObj != null) {
					dctmObj = dctmObjRetriever.retrieveObject(prsstntObj);
					DctmObjectWriter.writeBinaryObject(dctmObj);
					// dctmObjWriter.writeXMLObject(xmlEncoder);
				}
			} catch (CMSMFException e) {
				// If for some reason object is not retrieved from the system, or written to
// the
				// filesystem, write to an error log and continue on
				CMSMFMain_export.this.logger.error("Couldn't retrieve object information from repository for id: "
					+ this.objectId, e);
			} catch (IOException e) {
				// If there is IOException, log the error and exit out
				CMSMFMain_export.this.logger.fatal("Couldn't serialize an object to the filesystem for id: "
					+ this.objectId, e);
				// close all of the file streams
				try {
					this.fsm.closeAllStreams();
				} catch (CMSMFIOException e2) {
					CMSMFMain_export.this.logger.error("Couldn't close all of the filestreams", e2);
				}
				throw new RuntimeException(String.format("Failed to serialize object [%s] to the filesystem",
					this.objectId), e);
			} finally {
				closeSession(session);
			}
		}
	}

	CMSMFMain_export() throws Throwable {
		super();
	}

	/**
	 * Starts exporting objects from source directory. It reads up the query from
	 * properties file and executes it against the source repository. It retrieves
	 * objects from the repository and exports it out.
	 *
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	@Override
	protected void run() throws IOException, CMSMFFatalException {
		// First check to see if import.lock file exists, if it does, that means import didn't
// finish
		// cleanly. In that case do not start the export.
		File importLockFile = new File(this.streamFilesDirectoryLocation, CMSMFAppConstants.IMPORT_LOCK_FILE_NAME);
		if (importLockFile.exists()) {
			String msg = "_cmsmf_import.lck file exists in the export directory. Unsafe to continue with the export.";
			throw (new CMSMFFatalException(msg));
		}

		// Create a export.lock file which will be removed at the of exporting. Import
		// will not start if this file exists.
		File exportLockFile = new File(this.streamFilesDirectoryLocation, CMSMFAppConstants.EXPORT_LOCK_FILE_NAME);
		// Make sure that parent folder path exists before trying to create the file.
		File parentFolderPath = new File(exportLockFile.getAbsolutePath());
		parentFolderPath.mkdirs();
		// This is unreliable as per the JDK
		exportLockFile.createNewFile();

		final Date exportStartTime = new Date();
		if (this.logger.isEnabledFor(Level.INFO)) {
			this.logger.info("##### Export Process Started #####");
		}

		IDfSession session;
		try {
			session = getSession();
		} catch (Throwable t) {
			throw new RuntimeException("Failed to get the query session", t);
		}
		String exportDQLQuery = buildExportQueryString(session);

		// Load source repository configuration information
		try {
			RepositoryConfiguration srcRepoConfig = RepositoryConfiguration.getRepositoryConfiguration();
			try {
				srcRepoConfig.loadRepositoryConfiguration(session);
			} catch (DfException e1) {
				this.logger.fatal("Couldn't retrieve repository configuration information", e1);
			}

			// reset the counters that keeps track of how many objects are exported
			AppCounter.getObjectCounter().resetCounters();

			// First set the directory path where all of the files will be created
			FileStreamsManager fsm = FileStreamsManager.getFileStreamManager();
			fsm.setStreamsDirectoryPath(this.streamFilesDirectoryLocation);
			fsm.setContentDirectoryPath(this.contentFilesDirectoryLocation);

			// Delete existing cmsmf stream files before export process
			fsm.deleteStreamFiles();

			// Build the query that will determine what objects will be exported
			String strBatchSize = CMSMFLauncher.getParameter(CLIParam.batch);
			int batchSize = CMSMFAppConstants.BATCH_SIZE;
			if (strBatchSize != null) {
				try {
					batchSize = Integer.valueOf(strBatchSize);
					if (batchSize <= 0) {
						this.logger.warn(String.format("Illegal batch size [%d] - using the default value", batchSize));
						batchSize = CMSMFAppConstants.BATCH_SIZE;
					}
				} catch (NumberFormatException e) {
					this.logger.warn(String.format("Illegal batch size [%s] - using the default value", strBatchSize));
					batchSize = CMSMFAppConstants.BATCH_SIZE;
				}
			}

			IDfQuery dqlQry = new DfClientX().getQuery();
			dqlQry.setDQL(exportDQLQuery);
			dqlQry.setBatchSize(batchSize);
			if (this.logger.isEnabledFor(Level.DEBUG)) {
				this.logger.debug("Export DQL Query is: " + exportDQLQuery);
			}

			IDfCollection resultCol = null;
			try {
				resultCol = dqlQry.execute(session, IDfQuery.READ_QUERY);
				while (resultCol.next()) {
					queueWork(new Worker(fsm, resultCol.getId("r_object_id").getId()));
				} // while (resultCol.next())

				// Export source repository configuration information
				try {
					DctmObjectWriter.writeBinaryObject(srcRepoConfig);
				} catch (CMSMFException e) {
					this.logger.error("Couldn't retrieve source repository configuration information.", e);
				}
			} catch (DfException e) {
				// If there is a DfException while running the export query, log the error and exit
// out
				this.logger.fatal("Export Query failed to run. Query is:  " + exportDQLQuery, e);
			} finally {
				try {
					if (resultCol != null) {
						resultCol.close();
					}
				} catch (DfException e) {
					this.logger.error("Couldn't close the query collection", e);
				}

				// close all of the file streams
				try {
					fsm.closeAllStreams();
				} catch (CMSMFIOException e) {
					this.logger.error("Couldn't close all of the filestreams", e);
				}
				exportLockFile.delete();
			}
		} finally {
			closeSession(session);
		}

		// print the counters to see how many objects were processed
		AppCounter.getObjectCounter().printCounters();

		// email the counters to see how many objects were processed
		AppCounter.getObjectCounter().emailCounters("Export", exportDQLQuery);

		if (this.logger.isEnabledFor(Level.INFO)) {
			this.logger.info("##### Export Process Finished #####");
		}
		// If this is auto run type of an export instead of an adhoc query export, store the value
		// of the current export date in the repository. This value will be looked up in the next
		// run
		String fromWhereClause = PropertiesManager.getProperty(CMSMFProperties.ADHOC_QUERY_WHERE_CLAUSE, "");
		if (StringUtils.isBlank(fromWhereClause)) {
			// This is indeed an auto run type of export
			String dateTimePattern = CMSMFAppConstants.LAST_EXPORT_DATE_PATTERN;
			String exportStartDateStr = DateFormatUtils.format(exportStartTime, dateTimePattern);

			CMSMFUtils.setLastExportDate(session, exportStartDateStr);
		}
	}

	private String buildExportQueryString(IDfSession session) {

		String exportDQLQuery = "";

		// First check to see if addhoc query property has any value. If it does have some value in
// it,
		// use it to build the query string. If this value is blank, look into the source repository
// to see
		// when was the last export run and pick up the sysobjects modified since then.

		String selectClause = CMSMFAppConstants.EXPORT_QUERY_SELECT_CLAUSE;
		String fromWhereClause = PropertiesManager.getProperty(CMSMFProperties.ADHOC_QUERY_WHERE_CLAUSE, "");
		if (StringUtils.isNotBlank(fromWhereClause)) {
			exportDQLQuery = selectClause + " " + fromWhereClause;
		} else {
			// Try to locate a object in source repository that represents a last successful export
			// to a target repository.
			// NOTE : We will create a cabinet named 'CMSMF_SYNC' in source repository. We will
// create a
			// a folder for each target repository in this cabinet, the name of the folder will be
// the name
			// of a target repository. In this folder we will create an object named
// 'cmsmf_last_export' and

			// first get the last export date from the source repository
			String lastExportRunDate = CMSMFUtils.getLastExportDate(session);
			exportDQLQuery = CMSMFAppConstants.EXPORT_QUERY_SELECT_CLAUSE + " "
				+ CMSMFAppConstants.AUTO_EXPORT_QUERY_FROM_WHERE_CLAUSE;
			if (StringUtils.isNotBlank(lastExportRunDate)) {
				String modifiedWhereCondition = " and r_modify_date >= DATE('" + lastExportRunDate + "')";
				exportDQLQuery = exportDQLQuery + modifiedWhereCondition;
			}
		}

		return exportDQLQuery;
	}

}
