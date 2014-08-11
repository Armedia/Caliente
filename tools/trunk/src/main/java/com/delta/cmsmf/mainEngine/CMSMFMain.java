package com.delta.cmsmf.mainEngine;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import com.delta.cmsmf.cmsobjects.DctmACL;
import com.delta.cmsmf.cmsobjects.DctmDocument;
import com.delta.cmsmf.cmsobjects.DctmFolder;
import com.delta.cmsmf.cmsobjects.DctmFormat;
import com.delta.cmsmf.cmsobjects.DctmGroup;
import com.delta.cmsmf.cmsobjects.DctmObject;
import com.delta.cmsmf.cmsobjects.DctmObjectTypesEnum;
import com.delta.cmsmf.cmsobjects.DctmUser;
import com.delta.cmsmf.constants.CMSMFAppConstants;
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
import com.delta.cmsmf.serialization.DctmObjectWriter;
import com.delta.cmsmf.utils.EncryptPasswordUtil;
import com.documentum.com.DfClientX;
import com.documentum.fc.client.DfAuthenticationException;
import com.documentum.fc.client.DfClient;
import com.documentum.fc.client.DfIdentityException;
import com.documentum.fc.client.DfPrincipalException;
import com.documentum.fc.client.DfServiceException;
import com.documentum.fc.client.IDfClient;
import com.documentum.fc.client.IDfCollection;
import com.documentum.fc.client.IDfPersistentObject;
import com.documentum.fc.client.IDfQuery;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.client.IDfSessionManager;
import com.documentum.fc.common.DfException;
import com.documentum.fc.common.DfId;
import com.documentum.fc.common.DfLoginInfo;
import com.documentum.fc.common.IDfLoginInfo;

/**
 * The main method of this class is an entry point for the cmsmf application.
 * 
 * @author Shridev Makim 6/15/2010
 */
public class CMSMFMain {

	/** The logger object used for logging. */
	private static Logger logger = Logger.getLogger(CMSMFMain.class);

	private static CMSMFMain instance = null;

	/** The dctm session. */
	private IDfSession dctmSession = null;

	/** The directory location where stream files will be created. */
	private File streamFilesDirectoryLocation = null;

	/** The directory location where content files will be created. */
	private File contentFilesDirectoryLocation = null;

	private boolean testMode = false;

	/**
	 * The main method.
	 * 
	 * @param args
	 *            the arguments
	 * @throws ConfigurationException
	 *             the configuration exception
	 */
	public static void main(String[] args) throws Throwable {
		// Initialize Application
		CMSMFMain.instance = CMSMFMain.initApp(args);
		CMSMFMain.instance.start(args);
	}

	/**
	 * Initializes the cmsmf application.
	 * 
	 * @throws ConfigurationException
	 *             the configuration exception
	 */
	private static CMSMFMain initApp(String[] args) throws ConfigurationException {
		PropertiesManager pm = PropertiesManager.getPropertiesManager();
		// Load properties from config file
		pm.loadProperties(CMSMFAppConstants.FULLY_QUALIFIED_CONFIG_FILE_NAME);
		return new CMSMFMain();
	}

	public File getStreamFilesDirectory() {
		return this.streamFilesDirectoryLocation;
	}

	public File getContentFilesDirectory() {
		return this.contentFilesDirectoryLocation;
	}

	public boolean isTestMode() {
		return this.testMode;
	}

	/**
	 * Starts the main processing of the application. It checks the properties
	 * file to see if a user has selected export step or import step. It accordingly
	 * establishes a session with either source repository or target repository.
	 * 
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	private void start(String[] args) throws Throwable {
		if (CMSMFMain.logger.isEnabledFor(Level.INFO)) {
			CMSMFMain.logger.info("##### CMS Migration Process Started #####");
		}

		// Determine if this is a export step or import step
		String importOrExport = null;
		if (importOrExport == null) {
			// Support legacy configurations
			importOrExport = PropertiesManager.getPropertiesManager().getProperty(
				CMSMFProperties.CMSMF_APP_IMPORTEXPORT_MODE, "");
		}

		this.testMode = "test".equalsIgnoreCase(PropertiesManager.getPropertiesManager().getProperty(
			CMSMFProperties.CMSMF_APP_RUN_MODE, ""));

		final String docbaseName = PropertiesManager.getPropertiesManager().getProperty(CMSMFProperties.DOCBASE_NAME,
			"");
		final String docbaseUser = PropertiesManager.getPropertiesManager().getProperty(CMSMFProperties.DOCBASE_USER,
			"");
		String passTmp = PropertiesManager.getPropertiesManager().getProperty(CMSMFProperties.DOCBASE_PASSWORD, "");

		final IDfClient dfClient;
		try {
			dfClient = DfClient.getLocalClient();
			if (dfClient == null) {
				// If I don't have a local client then something was not
				// installed
				// correctly so throw an error
				String msg = "No local client was established.  You may want to check the installation of "
					+ "Documentum or this application on this machine.";
				CMSMFMain.logger.error(msg);
				throw new RuntimeException(msg);
			}
		} catch (DfException e) {
			String msg = "No local client was established.  You may want to check the installation of "
				+ "Documentum or this application on this machine.";
			CMSMFMain.logger.error(msg);
			throw new RuntimeException(msg, e);
		}

		try {
			passTmp = EncryptPasswordUtil.decryptPassword(passTmp);
		} catch (Throwable t) {
			// Not encrypted, use literal
		}
		final String docbasePassword = passTmp;

		// Set the filesystem location where files will be created or read from
		this.streamFilesDirectoryLocation = new File(PropertiesManager.getPropertiesManager().getProperty(
			CMSMFProperties.CMSMF_APP_IMPORTEXPORT_DIRECTORY, ""));

		// Set the filesystem location where the content files will be created or read from
		this.contentFilesDirectoryLocation = new File(PropertiesManager.getPropertiesManager().getProperty(
			CMSMFProperties.CMSMF_APP_IMPORTEXPORT_CONTENT_DIRECTORY, ""));

		// get a local client
		try {
			// Prepare login object
			IDfLoginInfo li = new DfLoginInfo();
			li.setUser(docbaseUser);
			li.setPassword(docbasePassword);
			li.setDomain(null);

			// Get a documentum session using session manager
			IDfSessionManager sessionManager = dfClient.newSessionManager();
			sessionManager.setIdentity(docbaseName, li);
			this.dctmSession = sessionManager.getSession(docbaseName);
		} catch (DfIdentityException e) {
			CMSMFMain.logger.error("Error establishing Documentum session", e);
		} catch (DfAuthenticationException e) {
			CMSMFMain.logger.error("Error establishing Documentum session", e);
		} catch (DfPrincipalException e) {
			CMSMFMain.logger.error("Error establishing Documentum session", e);
		} catch (DfServiceException e) {
			CMSMFMain.logger.error("Error establishing Documentum session", e);
		}

		if (importOrExport.equalsIgnoreCase("Export")) {
			// Start the export process
			startExporting();
		} else {
			// Start the import process
			startImporting();
		}
		if (CMSMFMain.logger.isEnabledFor(Level.INFO)) {
			CMSMFMain.logger.debug("##### CMS Migration Process finished #####");
		}
	}

	/**
	 * Start2 method used mainly for testing.
	 * 
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	@SuppressWarnings("unused")
	@Deprecated
	private void start2() throws IOException {
		if (CMSMFMain.logger.isEnabledFor(Level.INFO)) {
			CMSMFMain.logger.info("##### CMS Migration Process Started #####");
		}

		String docbaseName = "cobtest";
		String docbaseUser = "dmadmin";
		String docbasePassword = "dmadmin";

		// get a local client
		try {
			IDfClient dfClient = DfClient.getLocalClient();
			if (dfClient == null) {
				// If I don't have a local client then something was not
				// installed
				// correctly so throw an error
				CMSMFMain.logger.error("No local client was established.  You may want to check the installation of "
					+ "Documentum or this application on this machine.");
			}// End if(dfClient.equals(null))
			else {
				// Prepare login object
				IDfLoginInfo li = new DfLoginInfo();
				li.setUser(docbaseUser);
				li.setPassword(docbasePassword);
				li.setDomain(null);

				// Get a documentum session using session manager
				IDfSessionManager sessionManager = dfClient.newSessionManager();
				sessionManager.setIdentity(docbaseName, li);
				this.dctmSession = sessionManager.getSession(docbaseName);
			}

		} catch (DfIdentityException e) {
			CMSMFMain.logger.error("Error establishing Documentum session", e);
		} catch (DfAuthenticationException e) {
			CMSMFMain.logger.error("Error establishing Documentum session", e);
		} catch (DfPrincipalException e) {
			CMSMFMain.logger.error("Error establishing Documentum session", e);
		} catch (DfServiceException e) {
			CMSMFMain.logger.error("Error establishing Documentum session", e);
		} catch (DfException e) {
			CMSMFMain.logger.error("Error establishing Documentum session", e);
		}

		// Set the filesystem location where files will be created or read from
		this.streamFilesDirectoryLocation = new File(PropertiesManager.getPropertiesManager().getProperty(
			CMSMFProperties.CMSMF_APP_IMPORTEXPORT_DIRECTORY, ""));

		// Set the filesystem location where the content files will be created or read from
		this.contentFilesDirectoryLocation = new File(PropertiesManager.getPropertiesManager().getProperty(
			CMSMFProperties.CMSMF_APP_IMPORTEXPORT_CONTENT_DIRECTORY, ""));

		// Start the export process
		startExporting2();

		// Start the import process
		startImporting2();

		// test on r_modify_date modification
		// testChangingInternalAttributes(dctmSession);

		// test xml synchronization
		// testXMLSynchronization();

		if (CMSMFMain.logger.isEnabledFor(Level.INFO)) {
			CMSMFMain.logger.debug("##### CMS Migration Process finished #####");
		}
	}

	/**
	 * Starts exporting objects from source directory. It reads up the query from
	 * properties file and executes it against the source repository. It retrieves
	 * objects from the repository and exports it out.
	 * 
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	private void startExporting() throws IOException {
		if (CMSMFMain.logger.isEnabledFor(Level.INFO)) {
			CMSMFMain.logger.info("##### Export Process Started #####");
		}

		// Load source repository configuration information
		RepositoryConfiguration srcRepoConfig = RepositoryConfiguration.getRepositoryConfiguration();
		try {
			srcRepoConfig.loadRepositoryConfiguration(this.dctmSession);
		} catch (DfException e1) {
			CMSMFMain.logger.fatal("Couldn't retrieve repository configuration information", e1);
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
		String selectClause = CMSMFAppConstants.EXPORT_QUERY_SELECT_CLAUSE;
		String fromWhereClause = PropertiesManager.getPropertiesManager().getProperty(
			CMSMFProperties.CMSMF_APP_EXPORT_QUERY_PREDICATE, "");

		IDfQuery dqlQry = new DfClientX().getQuery();
		dqlQry.setDQL(selectClause + fromWhereClause);
		dqlQry.setBatchSize(20000);
		if (CMSMFMain.logger.isEnabledFor(Level.DEBUG)) {
			CMSMFMain.logger.debug("Export DQL Query is: " + selectClause + fromWhereClause);
		}

		try {
			IDfCollection resultCol = dqlQry.execute(this.dctmSession, IDfQuery.READ_QUERY);
			while (resultCol.next()) {
				String objID = resultCol.getId("r_object_id").getId();
				// get the object
				IDfPersistentObject prsstntObj = null;
				try {
					prsstntObj = this.dctmSession.getObject(new DfId(objID));
				} catch (DfException e) {
					CMSMFMain.logger.error("Couldn't retrieve object by ID: " + objID, e);
				}

				DctmObjectRetriever dctmObjRetriever = new DctmObjectRetriever(this.dctmSession);
				DctmObject dctmObj;
				try {
					dctmObj = dctmObjRetriever.retrieveObject(prsstntObj);
					DctmObjectWriter.writeBinaryObject(dctmObj);
					// dctmObjWriter.writeXMLObject(xmlEncoder);
				} catch (CMSMFException e) {
					// If for some reason object is not retrieved from the system, or written to the
					// filesystem,
					// write to an error log and continue on
					CMSMFMain.logger.error("Couldn't retrieve object information from repository for id: " + objID, e);
				} catch (IOException e) {
					// If there is IOException, log the error and exit out
					CMSMFMain.logger.fatal("Couldn't serialize an object to the filesystem for id: " + objID, e);
					// close all of the file streams
					try {
						fsm.closeAllStreams();
					} catch (CMSMFIOException e2) {
						CMSMFMain.logger.error("Couldn't close all of the filestreams", e2);
					}
					throw (e);
				}
			} // while (resultCol.next())
			resultCol.close();

			// Export source repository configuration information
			try {
				DctmObjectWriter.writeBinaryObject(srcRepoConfig);
			} catch (CMSMFException e) {
				CMSMFMain.logger.error("Couldn't retrieve source repository configuration information.", e);
			}
		} catch (DfException e) {
			// If there is a DfException while running the export query, log the error and exit out
			CMSMFMain.logger.fatal("Export Query failed to run. Query is:  " + selectClause + fromWhereClause, e);
		} finally {
			// close all of the file streams
			try {
				fsm.closeAllStreams();
			} catch (CMSMFIOException e) {
				CMSMFMain.logger.error("Couldn't close all of the filestreams", e);
			}
		}

		// print the counters to see how many objects were processed
		AppCounter.getObjectCounter().printCounters();

		if (CMSMFMain.logger.isEnabledFor(Level.INFO)) {
			CMSMFMain.logger.info("##### Export Process Finished #####");
		}
	}

	/**
	 * Start exporting. Used for testing only.
	 * 
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	@Deprecated
	private void startExporting2() throws IOException {
		if (CMSMFMain.logger.isEnabledFor(Level.INFO)) {
			CMSMFMain.logger.info("##### Export Process Started #####");
		}

		// Load source repository configuration information
		RepositoryConfiguration srcRepoConfig = RepositoryConfiguration.getRepositoryConfiguration();
		try {
			srcRepoConfig.loadRepositoryConfiguration(this.dctmSession);
		} catch (DfException e1) {
			CMSMFMain.logger.fatal("Couldn't retrieve repository configuration information", e1);
		}

		// reset the counters that keeps track of how many objects are exported
		AppCounter.getObjectCounter().resetCounters();

		// First set the directory path where all of the files will be created
		FileStreamsManager fsm = FileStreamsManager.getFileStreamManager();
		fsm.setStreamsDirectoryPath(this.streamFilesDirectoryLocation);
		fsm.setContentDirectoryPath(this.contentFilesDirectoryLocation);

		// Delete existing cmsmf stream files before export process
		fsm.deleteStreamFiles();

		// XMLEncoder xmlEncoder = new XMLEncoder(new
		// FileOutputStream(xmlObjFileName));

		List<String> objIDList = new ArrayList<String>(); // 0901116f80018712
		// objIDList.add("0901116f80018874"); //cobtest text document
		// objIDList.add("0901116f80018873"); //cobtest word document
		// objIDList.add("0901116f80018878"); // cobtest text document who is in
		// objIDList.add("0901116f8001aae2"); // cobtest document with multiple versions
		// objIDList.add("0901116f8001ae6f"); // cobtest document with version label test
		// objIDList.add("0901116f8001af4d"); // cobtest document with minor version
		// objIDList.add("0901116f8001b201"); // cobtest document very large document
		// objIDList.add("0901116f8001b202"); // cobtest document large document
		objIDList.add("0901116f8001bbce"); // cobtest document small pdf with 2 versions
		// objIDList.add("0901116f8001ba24"); // cobtest document branched versions
		// objIDList.add("0b01116f8001a532"); // cobtest folder {/SKM_TEST/SKM1}
		// objIDList.add("1101116f80000ab7"); // cobtest user (Allen, Scott)
		// objIDList.add("1101116f80001552"); // cobtest user (SKM Temp1)
		// objIDList.add("1201116f8000051f"); // cobtest group (cob_im_netsys_coor)
		// objIDList.add("1201116f80001104"); // cobtest group (skm_test_grp1)
		// objIDList.add("4501116f80000925"); // cobtest acl (COB_DR - Public)
		// objIDList.add("4501116f80000959"); // cobtest acl (COB_DR - Web)

		for (String objID : objIDList) {
			// get the object
			IDfPersistentObject prsstntObj = null;
			try {
				prsstntObj = this.dctmSession.getObject(new DfId(objID));
			} catch (DfException e) {
				CMSMFMain.logger.error("Couldn't retrieve object by ID: " + objID, e);
			}

			DctmObjectRetriever dctmObjRetriever = new DctmObjectRetriever(this.dctmSession);
			DctmObject dctmObj;
			try {
				dctmObj = dctmObjRetriever.retrieveObject(prsstntObj);
				DctmObjectWriter.writeBinaryObject(dctmObj);
				// dctmObjWriter.writeXMLObject(xmlEncoder);

				// Export source repository configuration information
				try {
					DctmObjectWriter.writeBinaryObject(srcRepoConfig);
				} catch (CMSMFException e) {
					CMSMFMain.logger.error("Couldn't retrieve source repository configuration information.", e);
				}

			} catch (CMSMFException e) {
				// If for some reason object is not retrieved from the system,
				// or written to the
				// filesystem, write to an error log and continue on
				CMSMFMain.logger.error("Couldn't retrieve object information from repository for id: " + objID, e);
			} catch (IOException e) {
				// If there is IOException, log the error and exit out
				CMSMFMain.logger.fatal("Couldn't serialize an object to the filesystem for id: " + objID, e);
				throw (e);
			} finally {
				// close all of the file streams
				try {
					fsm.closeAllStreams();
				} catch (CMSMFIOException e) {
					CMSMFMain.logger.error("Couldn't close all of the filestreams", e);
				}
			}
		}

		// print the counters to see how many objects were processed
		AppCounter.getObjectCounter().printCounters();

		if (CMSMFMain.logger.isEnabledFor(Level.INFO)) {
			CMSMFMain.logger.info("##### Export Process Finished #####");
		}
	}

	/**
	 * Starts importing objects into target repository. This
	 * method imports objects in users, groups, acls, types,
	 * formats, folders and documents order.
	 * 
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	private void startImporting() throws IOException {
		if (CMSMFMain.logger.isEnabledFor(Level.INFO)) {
			CMSMFMain.logger.info("##### Import Process Started #####");
		}

		// reset the counters that keeps track of how many objects are read
		AppCounter.getObjectCounter().resetCounters();

		// First set the directory path where all of the files were created
		FileStreamsManager fsm = FileStreamsManager.getFileStreamManager();
		fsm.setStreamsDirectoryPath(this.streamFilesDirectoryLocation);
		fsm.setContentDirectoryPath(this.contentFilesDirectoryLocation);

		// Check that all prerequisites are met before importing anything
		if (!isItSafeToImport()) {
			CMSMFMain.logger.error("Unsafe to continue import into target repository. Import process halted");
			return;
		}

		try {
			// Import user objects
			readAndImportDctmObjects(DctmObjectTypesEnum.DCTM_USER);

			// Import group objects
			readAndImportDctmObjects(DctmObjectTypesEnum.DCTM_GROUP);

			// Import ACL objects
			readAndImportDctmObjects(DctmObjectTypesEnum.DCTM_ACL);

			// Import Type objects
			readAndImportDctmObjects(DctmObjectTypesEnum.DCTM_TYPE);

			// Import format objects
			readAndImportDctmObjects(DctmObjectTypesEnum.DCTM_FORMAT);

			// Import folder objects
			readAndImportDctmObjects(DctmObjectTypesEnum.DCTM_FOLDER);

			// Import document objects
			readAndImportDctmObjects(DctmObjectTypesEnum.DCTM_DOCUMENT);

		} catch (CMSMFFatalException e) {
			CMSMFMain.logger.error(e.getMessage());
		} finally {
			// close all of the file streams
			try {
				fsm.closeAllStreams();
			} catch (CMSMFIOException e) {
				CMSMFMain.logger.error("Couldn't close all of the filestreams", e);
			}
		}

		// print the counters to see how many objects were processed
		AppCounter.getObjectCounter().printCounters();

		// Print detailed import report
		CMSMFMain.printImportReport();

		if (CMSMFMain.logger.isEnabledFor(Level.INFO)) {
			CMSMFMain.logger.info("##### Import Process Finished #####");
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
	private boolean isItSafeToImport() throws IOException {
		boolean isItSafeToImport = false;

		// Make sure that all of the fileStores that we need exists in the target repository
		try {
			RepositoryConfiguration srcRepoConfig = DctmObjectReader.readSrcRepoConfig();
			List<String> fileStores = srcRepoConfig.getFileStores();
			if (doesFileStoresExist(fileStores)) {
				isItSafeToImport = true;
			}
		} catch (CMSMFException e) {
			CMSMFMain.logger.error("Error reading source repository configuration file.", e);
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
	private boolean doesFileStoresExist(List<String> srcRepoFileStores) throws CMSMFException {

		boolean doesFileStoresExistInTargetRepo = false;

		// Query the target repository to get names of all filestores
		List<String> targetRepoFileStores = new ArrayList<String>();
		IDfQuery dqlQry = new DfClientX().getQuery();
		try {
			String fileStoreQry = "select distinct name from dm_store";
			dqlQry.setDQL(fileStoreQry);
			IDfCollection resultCol = dqlQry.execute(this.dctmSession, IDfQuery.READ_QUERY);
			while (resultCol.next()) {
				String fileStoreName = resultCol.getString(DctmAttrNameConstants.NAME);
				if (CMSMFMain.logger.isEnabledFor(Level.DEBUG)) {
					CMSMFMain.logger.debug("FileStore in Target Repo: " + fileStoreName);
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
					CMSMFMain.logger.error("The required filestore: " + fileStore + " must exist in target repository");
				}
			}
		}
		return doesFileStoresExistInTargetRepo;
	}

	/**
	 * Start importing2. This method is deprecated. Use startImporting() instead
	 * 
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	@Deprecated
	private void startImporting2() throws IOException {
		if (CMSMFMain.logger.isEnabledFor(Level.INFO)) {
			CMSMFMain.logger.info("##### Import Process Started #####");
		}

		// reset the counters that keeps track of how many objects are read
		AppCounter.getObjectCounter().resetCounters();

		// First set the directory path where all of the files were created
		FileStreamsManager fsm = FileStreamsManager.getFileStreamManager();
		fsm.setStreamsDirectoryPath(this.streamFilesDirectoryLocation);
		fsm.setContentDirectoryPath(this.contentFilesDirectoryLocation);

		// Check that all prerequisites are met before importing anything
		if (!isItSafeToImport()) {
			CMSMFMain.logger.error("Unsafe to continue import into target repository. Import process halted");
			return;
		}

		// Read users from the file and create them
		try {
			DctmUser dctmUsr = (DctmUser) DctmObjectReader.readObject(DctmObjectTypesEnum.DCTM_USER);
			while (dctmUsr != null) {
				try {
					dctmUsr.setDctmSession(this.dctmSession);
					dctmUsr.createInCMS();
					dctmUsr = (DctmUser) DctmObjectReader.readObject(DctmObjectTypesEnum.DCTM_USER);
				} catch (DfException e) {
					// log the error and continue on with next object
					CMSMFMain.logger.error("Error creating/Updating user in dctm repository.", e);
				} catch (CMSMFException e) {
					// log the error and continue on with next object
					CMSMFMain.logger.error("Error reading user object from file.", e);
				}
			}
		} catch (IOException e) {
			// If there is IOException, close all of the streams, log the error and exit out
			try {
				fsm.closeAllStreams();
			} catch (CMSMFIOException e1) {
				CMSMFMain.logger.error("Couldn't close all of the filestreams", e1);
			}
			CMSMFMain.logger.fatal("Couldn't deserialize an object from the filesystem.", e);
			throw (e);
		} catch (CMSMFException e) {
			// log the error and continue on with next object
			CMSMFMain.logger.error("Error reading user object from file.", e);
		}

		// Read groups from the file and create them
		try {
			DctmGroup dctmGrp = (DctmGroup) DctmObjectReader.readObject(DctmObjectTypesEnum.DCTM_GROUP);
			while (dctmGrp != null) {
				try {
					dctmGrp.setDctmSession(this.dctmSession);
					dctmGrp.createInCMS();
					dctmGrp = (DctmGroup) DctmObjectReader.readObject(DctmObjectTypesEnum.DCTM_GROUP); // continue
					// the
					// loop
				} catch (DfException e) {
					// log the error and continue on with next object
					CMSMFMain.logger.error("Error creating/Updating group in dctm repository.", e);
				} catch (CMSMFException e) {
					// log the error and continue on with next object
					CMSMFMain.logger.error("Error reading group object from file.", e);
				}
			}
		} catch (IOException e) {
			// If there is IOException, close all of the streams, log the error and exit out
			try {
				fsm.closeAllStreams();
			} catch (CMSMFIOException e1) {
				CMSMFMain.logger.error("Couldn't close all of the filestreams", e1);
			}
			CMSMFMain.logger.fatal("Couldn't deserialize an object from the filesystem.", e);
			throw (e);
		} catch (CMSMFException e) {
			// log the error and continue
			CMSMFMain.logger.error("Error reading group object from file.", e);
		}

		// Read ACLs from the file and create them
		try {
			DctmACL dctmACL = (DctmACL) DctmObjectReader.readObject(DctmObjectTypesEnum.DCTM_ACL);
			while (dctmACL != null) {
				try {
					dctmACL.setDctmSession(this.dctmSession);
					dctmACL.createInCMS();
					dctmACL = (DctmACL) DctmObjectReader.readObject(DctmObjectTypesEnum.DCTM_ACL); // continue
					// the
					// loop
				} catch (DfException e) {
					// log the error and continue on with next object
					CMSMFMain.logger.error("Error creating/Updating ACL in dctm repository.", e);
				} catch (CMSMFException e) {
					// log the error and continue on with next object
					CMSMFMain.logger.error("Error reading ACL object from file.", e);
				}
			}
		} catch (IOException e) {
			// If there is IOException, close all of the streams, log the error and exit out
			try {
				fsm.closeAllStreams();
			} catch (CMSMFIOException e1) {
				CMSMFMain.logger.error("Couldn't close all of the filestreams", e1);
			}
			CMSMFMain.logger.fatal("Couldn't deserialize an object from the filesystem.", e);
			throw (e);
		} catch (CMSMFException e) {
			// log the error and continue on with next object
			CMSMFMain.logger.error("Error reading ACL object from file.", e);
		}

		// Read the cabinets/folders and create them
		try {
			DctmFolder dctmFldr = (DctmFolder) DctmObjectReader.readObject(DctmObjectTypesEnum.DCTM_FOLDER);
			while (dctmFldr != null) {
				try {
					dctmFldr.setDctmSession(this.dctmSession);
					dctmFldr.createInCMS();
					dctmFldr = (DctmFolder) DctmObjectReader.readObject(DctmObjectTypesEnum.DCTM_FOLDER);
				} catch (DfException e) {
					// log the error and continue on with next object
					CMSMFMain.logger.error("Error creating/Updating folder in dctm repository.", e);
				} catch (CMSMFException e) {
					// log the error and continue on with next object
					CMSMFMain.logger.error("Error reading folder object from file.", e);
				}
			}
		} catch (IOException e) {
			// If there is IOException, close all of the streams, log the error and exit out
			try {
				fsm.closeAllStreams();
			} catch (CMSMFIOException e1) {
				CMSMFMain.logger.error("Couldn't close all of the filestreams", e1);
			}
			CMSMFMain.logger.fatal("Couldn't deserialize an object from the filesystem.", e);
			throw (e);
		} catch (CMSMFException e) {
			// log the error and continue on with next object
			CMSMFMain.logger.error("Error reading folder object from file.", e);
		}

		// Read the objects and create them
		try {
			DctmDocument dctmDoc = (DctmDocument) DctmObjectReader.readObject(DctmObjectTypesEnum.DCTM_DOCUMENT);
			while (dctmDoc != null) {
				try {
					dctmDoc.setDctmSession(this.dctmSession);
					dctmDoc.createInCMS();
					dctmDoc = (DctmDocument) DctmObjectReader.readObject(DctmObjectTypesEnum.DCTM_DOCUMENT);
				} catch (DfException e) {
					// log the error and continue on with next object
					CMSMFMain.logger.error("Error creating/Updating document in dctm repository.", e);
				} catch (CMSMFException e) {
					// log the error and continue on with next object
					CMSMFMain.logger.error("Error reading document object from file.", e);
				}
			}
		} catch (CMSMFException e) {
			// log the error and continue on with next object
			CMSMFMain.logger.error("Error reading document object from file.", e);
		} catch (IOException e) {
			// If there is IOException, close all of the streams, log the error and exit out
			try {
				fsm.closeAllStreams();
			} catch (CMSMFIOException e1) {
				CMSMFMain.logger.error("Couldn't close all of the filestreams", e1);
			}
			CMSMFMain.logger.fatal("Couldn't deserialize an object from the filesystem.", e);
			throw (e);
		}

		// close all of the file streams
		try {
			fsm.closeAllStreams();
		} catch (CMSMFIOException e) {
			CMSMFMain.logger.error("Couldn't close all of the filestreams", e);
		}

		// print the counters to see how many objects were processed
		AppCounter.getObjectCounter().printCounters();

		if (CMSMFMain.logger.isEnabledFor(Level.INFO)) {
			CMSMFMain.logger.info("##### Import Process Finished #####");
		}
	}

	public static CMSMFMain getInstance() {
		return CMSMFMain.instance;
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
	private void readAndImportDctmObjects(DctmObjectTypesEnum dctmObjectType) throws IOException, CMSMFFatalException {
		if (CMSMFMain.logger.isEnabledFor(Level.DEBUG)) {
			CMSMFMain.logger.debug("Started Importing: " + dctmObjectType);
		}

		try {
			DctmObject dctmObject = (DctmObject) readDctmObject(dctmObjectType);

			while (dctmObject != null) {

				try {
					// Create appropriate object in target repository
					dctmObject.setDctmSession(this.dctmSession);
					dctmObject.createInCMS();
				} catch (DfException e) {
					// log the error and continue on with next object
					CMSMFMain.logger.error("Error creating/Updating " + dctmObjectType + " in dctm repository.", e);

					// increment import process error count; If the error count is more than the
// error
					// threshold specified in the properties file, quit the import process after
// closing all
					// of the file streams
					RunTimeProperties.getRunTimePropertiesInstance().incrementImportProcessErrorCount();

					int importErrorThreshold = PropertiesManager.getPropertiesManager().getProperty(
						CMSMFProperties.CMSMF_APP_IMPORT_ERRORCOUNT_THRESHOLD, 0);
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
				CMSMFMain.logger.error("Couldn't close all of the filestreams", e1);
			}
			CMSMFMain.logger.fatal("Couldn't deserialize " + dctmObjectType + " object from the filesystem.", e);
			throw (e);
		} catch (CMSMFException e) {
			// log the error and continue on with next object
			CMSMFMain.logger.error("Error reading " + dctmObjectType + " object from file.", e);
		}

		if (CMSMFMain.logger.isEnabledFor(Level.DEBUG)) {
			CMSMFMain.logger.debug("Finished Importing: " + dctmObjectType);
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
