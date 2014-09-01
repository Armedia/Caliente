package com.delta.cmsmf.mainEngine;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.mail.MessagingException;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Level;

import com.delta.cmsmf.cmsobjects.DctmACL;
import com.delta.cmsmf.cmsobjects.DctmDocument;
import com.delta.cmsmf.cmsobjects.DctmFolder;
import com.delta.cmsmf.cmsobjects.DctmFormat;
import com.delta.cmsmf.cmsobjects.DctmGroup;
import com.delta.cmsmf.cmsobjects.DctmObject;
import com.delta.cmsmf.cmsobjects.DctmObjectType;
import com.delta.cmsmf.cmsobjects.DctmReferenceDocument;
import com.delta.cmsmf.cmsobjects.DctmType;
import com.delta.cmsmf.cmsobjects.DctmUser;
import com.delta.cmsmf.constants.CMSMFAppConstants;
import com.delta.cmsmf.constants.DctmAttrNameConstants;
import com.delta.cmsmf.datastore.DataObject;
import com.delta.cmsmf.datastore.DataStore;
import com.delta.cmsmf.exception.CMSMFException;
import com.delta.cmsmf.exception.CMSMFFatalException;
import com.delta.cmsmf.exception.CMSMFIOException;
import com.delta.cmsmf.filestreams.FileStreamsManager;
import com.delta.cmsmf.properties.CMSMFProperties;
import com.delta.cmsmf.runtime.AppCounter;
import com.delta.cmsmf.runtime.DctmConnectionPool;
import com.delta.cmsmf.runtime.RunTimeProperties;
import com.delta.cmsmf.serialization.DctmObjectReader;
import com.delta.cmsmf.utils.CMSMFUtils;
import com.documentum.com.DfClientX;
import com.documentum.fc.client.IDfCollection;
import com.documentum.fc.client.IDfQuery;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.common.DfException;

/**
 * The main method of this class is an entry point for the cmsmf application.
 *
 * @author Shridev Makim 6/15/2010
 */
public class CMSMFMain_import extends AbstractCMSMFMain {

	CMSMFMain_import() throws Throwable {
		super();
	}

	/**
	 * Starts exporting objects from source directory. It reads up the query from
	 * properties file and executes it against the source repository. It retrieves
	 * objects from the repository and exports it out.
	 *
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 * @throws CMSMFFatalException
	 */
	@Override
	public void run() throws IOException, CMSMFFatalException {
		File exportLockFile = new File(this.streamFilesDirectoryLocation, CMSMFAppConstants.EXPORT_LOCK_FILE_NAME);
		if (exportLockFile.exists()) {
			String msg = "_cmsmf_export.lck file exists in the export directory. Unsafe to continue with the import.";
			throw (new CMSMFFatalException(msg));
		}
		// Create an import.lock file which will be removed at the of exporting. Import
		// will not start if this file exists.
		File importLockFile = new File(this.streamFilesDirectoryLocation, CMSMFAppConstants.IMPORT_LOCK_FILE_NAME);
		// Make sure that parent folder path exists before trying to create the file.
		File parentFolderPath = new File(importLockFile.getAbsolutePath());
		parentFolderPath.mkdirs();
		// This is unreliable, as per Javadoc...
		importLockFile.createNewFile();

		if (this.logger.isEnabledFor(Level.INFO)) {
			this.logger.info("##### Import Process Started #####");
		}

		// reset the counters that keeps track of how many objects are read
		AppCounter.getObjectCounter().resetCounters();

		// First set the directory path where all of the files were created
		FileStreamsManager fsm = FileStreamsManager.getFileStreamManager();
		fsm.setStreamsDirectoryPath(this.streamFilesDirectoryLocation);
		fsm.setContentDirectoryPath(this.contentFilesDirectoryLocation);

		// Check that all prerequisites are met before importing anything
		if (!isItSafeToImport()) {
			this.logger.error("Unsafe to continue import into target repository. Import process halted");
			return;
		}

		final ThreadPoolExecutor executor = new ThreadPoolExecutor(1, 16, 30, TimeUnit.SECONDS,
			new LinkedBlockingQueue<Runnable>());

		try {
			// IMPORTANT: The object types must be declared in the proper import order
			// otherwise this may fail.
			for (DctmObjectType type : DctmObjectType.values()) {
				readAndImportDctmObjects(executor, type);
			}
		} finally {
			// close all of the file streams
			try {
				fsm.closeAllStreams();
			} catch (CMSMFIOException e) {
				this.logger.error("Couldn't close all of the filestreams", e);
			}
			importLockFile.delete();
		}

		// print the counters to see how many objects were processed
		AppCounter.getObjectCounter().printCounters();

		// Print detailed import report
		printImportReport();

		// Email detailed import report
		emailImportReport();

		if (this.logger.isEnabledFor(Level.INFO)) {
			this.logger.info("##### Import Process Finished #####");
		}

		if (CMSMFProperties.POST_PROCESS_IMPORT.getBoolean()) {
			postProcessImport();
		}
	}

	private void postProcessImport() {
		if (this.logger.isEnabledFor(Level.INFO)) {
			this.logger.info("Started executing import post process jobs");
		}
		IDfSession session = DctmConnectionPool.acquireSession();
		try {
			// Run a dm_clean job to clean up any unwanted internal acls created
			CMSMFUtils.runDctmJob(session, "dm_DMClean");

			// Run a UpdateStats job
			CMSMFUtils.runDctmJob(session, "dm_UpdateStats");
		} catch (DfException e) {
			this.logger.error("Error running a post import process steps.", e);
		} finally {
			DctmConnectionPool.releaseSession(session);
		}
		if (this.logger.isEnabledFor(Level.INFO)) {
			this.logger.info("Finished executing import post process jobs");
		}
	}

	/**
	 * Prints the import report. It prints how many objects of each
	 * type were read, created, skipped and updated during import process.
	 */
	private void printImportReport() {
		DctmUser.printImportReport();
		DctmGroup.printImportReport();
		DctmACL.printImportReport();
		DctmType.printImportReport();
		DctmFormat.printImportReport();
		DctmFolder.printImportReport();
		DctmDocument.printImportReport();
		DctmReferenceDocument.printImportReport();
	}

	/**
	 * Email import report.
	 */
	private void emailImportReport() {
		StringBuffer emailMsg = new StringBuffer("Following is a detailed report from import step. \n");

		emailMsg.append("\n" + DctmUser.getDetailedUserImportReport());
		emailMsg.append("\n" + DctmGroup.getDetailedGroupImportReport());
		emailMsg.append("\n" + DctmACL.getDetailedAclImportReport());
		emailMsg.append("\n" + DctmType.getDetailedTypeImportReport());
		emailMsg.append("\n" + DctmFormat.getDetailedFormatImportReport());
		emailMsg.append("\n" + DctmFolder.getDetailedFolderImportReport());
		emailMsg.append("\n" + DctmDocument.getDetailedDocumentImportReport());
		emailMsg.append("\n" + DctmReferenceDocument.getDetailedReferenceDocumentImportReport());

		try {
			CMSMFUtils.postCmsmfMail("CMSMF detailed Import Report", emailMsg.toString());
		} catch (MessagingException e) {
			this.logger.error("Error sending CMSMF detailed Import report", e);
		}
	}

	/**
	 * Checks if it is safe to import. It checks to see if all required file stores exists
	 * in the target repository before the import process begins.
	 *
	 * @return true, if it is safe to import
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	private boolean isItSafeToImport() throws IOException {
		boolean isItSafeToImport = false;

		// Make sure that all of the fileStores that we need exists in the target repository
		try {
			RepositoryConfiguration srcRepoConfig = DctmObjectReader.readSrcRepoConfig();
			if (srcRepoConfig != null) {
				if (doesFileStoresExist(srcRepoConfig.getFileStores())) {
					isItSafeToImport = true;
				}
			}
		} catch (CMSMFException e) {
			this.logger.error("Error reading source repository configuration file.", e);
		}

		return isItSafeToImport;
	}

	/**
	 * Checks to see if file stores exist in the target repository.
	 *
	 * @param srcRepoFileStores
	 *            the src repo file stores
	 * @return true, if successful
	 * @throws CMSMFException
	 *             the cMSMF exception
	 */
	private boolean doesFileStoresExist(Collection<String> srcRepoFileStores) throws CMSMFException {

		boolean doesFileStoresExistInTargetRepo = false;

		// Query the target repository to get names of all filestores
		final IDfSession session = DctmConnectionPool.acquireSession();
		List<String> targetRepoFileStores = new ArrayList<String>();
		IDfQuery dqlQry = new DfClientX().getQuery();
		try {
			String fileStoreQry = "select distinct name from dm_store";
			dqlQry.setDQL(fileStoreQry);
			IDfCollection resultCol = dqlQry.execute(session, IDfQuery.READ_QUERY);
			while (resultCol.next()) {
				String fileStoreName = resultCol.getString(DctmAttrNameConstants.NAME);
				if (this.logger.isEnabledFor(Level.DEBUG)) {
					this.logger.debug("FileStore in Target Repo: " + fileStoreName);
				}
				if (StringUtils.isNotBlank(fileStoreName)) {
					targetRepoFileStores.add(fileStoreName);
				}
			}
			resultCol.close();
		} catch (DfException e) {
			throw (new CMSMFException("Couldn't retrieve file store names from target repository ", e));
		} finally {
			DctmConnectionPool.releaseSession(session);
		}

		// Check to see if target file store names contains all of the file stores that are needed
// for import
		if (targetRepoFileStores.containsAll(srcRepoFileStores)) {
			doesFileStoresExistInTargetRepo = true;
		} else {
			// Some of the required filestores are missig, write the ones that are missing to log
// file
			for (String fileStore : srcRepoFileStores) {
				if (!targetRepoFileStores.contains(fileStore)) {
					this.logger.error("The required filestore: " + fileStore + " must exist in target repository");
				}
			}
		}
		return doesFileStoresExistInTargetRepo;
	}

	/**
	 * Reads and imports dctm objects of given object type.
	 *
	 * @param dctmObjectType
	 *            the dctm object type
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 * @throws CMSMFFatalException
	 */
	private void readAndImportDctmObjects(final ExecutorService executor, final DctmObjectType dctmObjectType)
		throws IOException {
		if (this.logger.isEnabledFor(Level.DEBUG)) {
			this.logger.debug("Started Importing: " + dctmObjectType);
		}

		try {
			DataStore.deserializeObjects(dctmObjectType, new DataStore.ImportHandler() {
				@Override
				public boolean handle(final DataObject dataObject) throws Exception {
					final DctmObject dctmObject = dctmObjectType.newInstance(dataObject);
					final RunTimeProperties runtimeProps = RunTimeProperties.getRunTimePropertiesInstance();

					final IDfSession session = DctmConnectionPool.acquireSession();
					try {
						dctmObject.setDctmSession(session);
						dctmObject.createInCMS();
						return true;
					} catch (DfException e) {
						// log the error and continue on with next object
						CMSMFMain_import.this.logger.error(String.format(
							"Error creating/Updating %s with id [%s] in the target repository.", dctmObjectType,
							dataObject.getId()), e);

						// increment import process error count; If the error count is more
						// than the error threshold specified in the properties file, quit
						// the import process after closing all of the file streams
						runtimeProps.incrementImportProcessErrorCount();

						int importErrorThreshold = CMSMFProperties.IMPORT_MAX_ERRORS.getInt();
						CMSMFMain_import.this.logger.warn(String.format(
							"Total nbr of errors detected so far: %d of %d allowed",
							runtimeProps.getImportProcessErrorCount(), importErrorThreshold));
						return false;
					} finally {
						DctmConnectionPool.releaseSession(session);
					}
				}
			});
		} catch (SQLException e) {
			// Something happened....
			this.logger.fatal(String.format("SQL Error reading the %s objects", dctmObjectType), e);
		} catch (CMSMFException e) {
			this.logger.fatal(String.format("CMSMF Error reading the %s objects", dctmObjectType), e);
		} finally {
			FileStreamsManager.getFileStreamManager().closeAllStreams();
		}

		if (this.logger.isEnabledFor(Level.DEBUG)) {
			this.logger.debug("Finished Importing: " + dctmObjectType);
		}
	}
}