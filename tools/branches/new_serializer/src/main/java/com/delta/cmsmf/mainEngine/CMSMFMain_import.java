package com.delta.cmsmf.mainEngine;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Level;

import com.delta.cmsmf.cmsobjects.DctmACL;
import com.delta.cmsmf.cmsobjects.DctmDocument;
import com.delta.cmsmf.cmsobjects.DctmFolder;
import com.delta.cmsmf.cmsobjects.DctmFormat;
import com.delta.cmsmf.cmsobjects.DctmGroup;
import com.delta.cmsmf.cmsobjects.DctmObject;
import com.delta.cmsmf.cmsobjects.DctmObjectTypesEnum;
import com.delta.cmsmf.cmsobjects.DctmUser;
import com.delta.cmsmf.constants.CMSMFProperties;
import com.delta.cmsmf.constants.DctmAttrNameConstants;
import com.delta.cmsmf.exception.CMSMFException;
import com.delta.cmsmf.exception.CMSMFFatalException;
import com.delta.cmsmf.exception.CMSMFIOException;
import com.delta.cmsmf.filestreams.FileStreamsManager;
import com.delta.cmsmf.properties.PropertiesManager;
import com.delta.cmsmf.runtime.AppCounter;
import com.delta.cmsmf.runtime.RunTimeProperties;
import com.delta.cmsmf.serialization.DctmObjectReader;
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
public class CMSMFMain_import extends CMSMFMain {

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
	 */
	@Override
	protected void run() throws IOException {
		if (this.logger.isEnabledFor(Level.INFO)) {
			this.logger.info("##### Import Process Started #####");
		}

		// reset the counters that keeps track of how many objects are read
		AppCounter.getObjectCounter().resetCounters();

		// First set the directory path where all of the files were created
		FileStreamsManager fsm = FileStreamsManager.getFileStreamManager();
		fsm.setStreamsDirectoryPath(this.streamFilesDirectoryLocation);
		fsm.setContentDirectoryPath(this.contentFilesDirectoryLocation);

		final IDfSession session;
		try {
			session = getSession();
		} catch (Throwable t) {
			throw new RuntimeException("Failed to get the main session", t);
		}

		try {
			// Check that all prerequisites are met before importing anything
			if (!isItSafeToImport(session)) {
				this.logger.error("Unsafe to continue import into target repository. Import process halted");
				return;
			}

			try {
				// Import user objects
				readAndImportDctmObjects(session, DctmObjectTypesEnum.DCTM_USER);

				// Import group objects
				readAndImportDctmObjects(session, DctmObjectTypesEnum.DCTM_GROUP);

				// Import ACL objects
				readAndImportDctmObjects(session, DctmObjectTypesEnum.DCTM_ACL);

				// Import Type objects
				readAndImportDctmObjects(session, DctmObjectTypesEnum.DCTM_TYPE);

				// Import format objects
				readAndImportDctmObjects(session, DctmObjectTypesEnum.DCTM_FORMAT);

				// Import folder objects
				readAndImportDctmObjects(session, DctmObjectTypesEnum.DCTM_FOLDER);

				// Import document objects
				readAndImportDctmObjects(session, DctmObjectTypesEnum.DCTM_DOCUMENT);

			} catch (CMSMFFatalException e) {
				this.logger.error(e.getMessage());
			} finally {
				// close all of the file streams
				try {
					fsm.closeAllStreams();
				} catch (CMSMFIOException e) {
					this.logger.error("Couldn't close all of the filestreams", e);
				}
			}

			// print the counters to see how many objects were processed
			AppCounter.getObjectCounter().printCounters();

			// Print detailed import report
			CMSMFMain_import.printImportReport();

			if (this.logger.isEnabledFor(Level.INFO)) {
				this.logger.info("##### Import Process Finished #####");
			}
		} finally {
			closeSession(session);
		}
	}

	/**
	 * Prints the import report. It prints how many objects of each
	 * type were read, created, skipped and updated during import process.
	 */
	private static void printImportReport() {
		DctmUser.printImportReport();
		DctmGroup.printImportReport();
		DctmACL.printImportReport();
		DctmFormat.printImportReport();
		DctmFolder.printImportReport();
		DctmDocument.printImportReport();
	}

	/**
	 * Checks if it is safe to import. It checks to see if all required file stores exists
	 * in the target repository before the import process begins.
	 * 
	 * @return true, if it is safe to import
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	private boolean isItSafeToImport(IDfSession session) throws IOException {
		boolean isItSafeToImport = false;

		// Make sure that all of the fileStores that we need exists in the target repository
		try {
			RepositoryConfiguration srcRepoConfig = DctmObjectReader.readSrcRepoConfig();
			List<String> fileStores = srcRepoConfig.getFileStores();
			if (doesFileStoresExist(session, fileStores)) {
				isItSafeToImport = true;
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
	private boolean doesFileStoresExist(IDfSession session, List<String> srcRepoFileStores) throws CMSMFException {

		boolean doesFileStoresExistInTargetRepo = false;

		// Query the target repository to get names of all filestores
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
	private void readAndImportDctmObjects(IDfSession session, DctmObjectTypesEnum dctmObjectType) throws IOException,
		CMSMFFatalException {
		if (this.logger.isEnabledFor(Level.DEBUG)) {
			this.logger.debug("Started Importing: " + dctmObjectType);
		}

		try {
			DctmObject dctmObject = (DctmObject) readDctmObject(dctmObjectType);

			while (dctmObject != null) {

				try {
					// Create appropriate object in target repository
					dctmObject.setDctmSession(session);
					dctmObject.createInCMS();
				} catch (DfException e) {
					// log the error and continue on with next object
					this.logger.error("Error creating/Updating " + dctmObjectType + " in dctm repository.", e);

					// increment import process error count; If the error count is more than the
// error
					// threshold specified in the properties file, quit the import process after
// closing all
					// of the file streams
					RunTimeProperties.getRunTimePropertiesInstance().incrementImportProcessErrorCount();

					int importErrorThreshold = PropertiesManager.getProperty(CMSMFProperties.IMPORT_MAX_ERRORS, 0);
					if (RunTimeProperties.getRunTimePropertiesInstance().getImportProcessErrorCount() >= importErrorThreshold) {
						// Raise the cmsmf fatal exception.
						throw (new CMSMFFatalException(
							"Total nbr of errors during import exceeds the error threshold of " + importErrorThreshold
								+ " specified in properties file"));
					}

				}
				// Read next object from the file until you reach end of the file
				dctmObject = (DctmObject) readDctmObject(dctmObjectType);
			}
		} catch (IOException e) {
			// If there is IOException, close all of the streams, log the error and exit out
			try {
				FileStreamsManager.getFileStreamManager().closeAllStreams();
			} catch (CMSMFIOException e1) {
				this.logger.error("Couldn't close all of the filestreams", e1);
			}
			this.logger.fatal("Couldn't deserialize " + dctmObjectType + " object from the filesystem.", e);
			throw (e);
		} catch (CMSMFException e) {
			// log the error and continue on with next object
			this.logger.error("Error reading " + dctmObjectType + " object from file.", e);
		}

		if (this.logger.isEnabledFor(Level.DEBUG)) {
			this.logger.debug("Finished Importing: " + dctmObjectType);
		}
	}

	/**
	 * Reads and returns dctm object of given type from stream file.
	 * 
	 * @param dctmObjectType
	 *            the dctm object type
	 * @return the dctm object
	 * @throws CMSMFException
	 *             the CMSMF exception
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	private Object readDctmObject(DctmObjectTypesEnum dctmObjectType) throws CMSMFException, IOException {
		return DctmObjectReader.readObject(dctmObjectType);
	}

}
