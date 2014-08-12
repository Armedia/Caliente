package com.delta.cmsmf.mainEngine;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
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

	private enum CLIParam {
		//
		cfg(null, true, "The configuration file to use"),
		test(CMSMFProperties.TEST_MODE, false, "Enable test mode"),
		mode(CMSMFProperties.OPERATING_MODE, true, "The mode of operation, either 'import' or 'export'"),
		predicate(CMSMFProperties.EXPORT_QUERY_PREDICATE, true, "The DQL Predicate to use for exporting"),
		buffer(CMSMFProperties.CONTENT_READ_BUFFER_SIZE, true, "The size of the read buffer"),
		streams(CMSMFProperties.STREAMS_DIRECTORY, true, "The Streams directory to use"),
		content(CMSMFProperties.CONTENT_DIRECTORY, true, "The Content directory to use"),
		compress(CMSMFProperties.COMPRESSDATA_FLAG, false, "Enable compression for the data exported (GZip)"),
		attributes(CMSMFProperties.OWNER_ATTRIBUTES, true, "The attributes to check for"),
		errorCount(CMSMFProperties.IMPORT_ERRORCOUNT_THRESHOLD, true,
			"The number of errors to accept before aborting an import"),
		defaultPassword(CMSMFProperties.DEFAULT_USER_PASSWORD, true,
			"The default password to use for users being copied over (leave blank to use the same login name)"),
		docbase(CMSMFProperties.DOCBASE_NAME, true, "The docbase name to connect to"),
		user(CMSMFProperties.DOCBASE_USER, true, "The username to connect with"),
		password(CMSMFProperties.DOCBASE_PASSWORD, true, "The password to connect with");

		private final CMSMFProperties property;
		private final Option option;

		private CLIParam(CMSMFProperties property, boolean hasParameter, String description) {
			this.property = property;
			this.option = new Option(null, name().replace('_', '-'), hasParameter, description);
		}
	}

	/** The logger object used for logging. */
	private final Logger logger = Logger.getLogger(getClass());

	private static CMSMFMain instance = null;

	/** The dctm session. */
	private IDfSession dctmSession = null;

	/** The directory location where stream files will be created. */
	private final File streamFilesDirectoryLocation;

	/** The directory location where content files will be created. */
	private final File contentFilesDirectoryLocation;

	private final boolean testMode;

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
		CMSMFMain.instance = new CMSMFMain(args);
		CMSMFMain.instance.start();
	}

	private CMSMFMain(String[] args) throws ConfigurationException, ParseException {
		// Next, identify the run mode - import or export
		Options options = new Options();
		for (CLIParam p : CLIParam.values()) {
			options.addOption(p.option);
		}

		CommandLineParser parser = new PosixParser();
		CommandLine cli = parser.parse(options, args);

		// Convert the command-line parameters into "configuration properties"
		Properties parameters = new Properties();
		for (CLIParam p : CLIParam.values()) {
			if (!cli.hasOption(p.option.getLongOpt())) {
				continue;
			}
			if (p.property != null) {
				parameters.setProperty(p.property.name, cli.getOptionValue(p.option.getLongOpt()));
			}
		}

		// TODO: Initialize the properties manager with all this crap
		PropertiesManager.addPropertySource(CMSMFAppConstants.FULLY_QUALIFIED_CONFIG_FILE_NAME);

		// A configuration file has been specifed, so use its values ahead of the defaults
		if (cli.hasOption(CLIParam.cfg.option.getLongOpt())) {
			PropertiesManager.addPropertySource(cli.getOptionValue(CLIParam.cfg.option.getLongOpt()));
		}

		// If we have command-line parameters, these supersede all other configurations, even if
		// we have a configuration file explicitly listed.
		if (!parameters.isEmpty()) {
			PropertiesManager.addPropertySource(parameters);
		}
		PropertiesManager.init();

		this.testMode = "test".equalsIgnoreCase(PropertiesManager.getProperty(CMSMFProperties.TEST_MODE, ""));

		// Set the filesystem location where files will be created or read from
		this.streamFilesDirectoryLocation = new File(PropertiesManager.getProperty(CMSMFProperties.STREAMS_DIRECTORY,
			""));

		// Set the filesystem location where the content files will be created or read from
		this.contentFilesDirectoryLocation = new File(PropertiesManager.getProperty(CMSMFProperties.CONTENT_DIRECTORY,
			""));
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
	private void start() throws Throwable {
		if (this.logger.isEnabledFor(Level.INFO)) {
			this.logger.info("##### CMS Migration Process Started #####");
		}

		// Determine if this is a export step or import step
		String importOrExport = null;
		if (importOrExport == null) {
			// Support legacy configurations
			importOrExport = PropertiesManager.getProperty(CMSMFProperties.OPERATING_MODE, "");
		}

		final String docbaseName = PropertiesManager.getProperty(CMSMFProperties.DOCBASE_NAME, "");
		final String docbaseUser = PropertiesManager.getProperty(CMSMFProperties.DOCBASE_USER, "");
		String passTmp = PropertiesManager.getProperty(CMSMFProperties.DOCBASE_PASSWORD, "");

		final IDfClient dfClient;
		try {
			dfClient = DfClient.getLocalClient();
			if (dfClient == null) {
				// If I don't have a local client then something was not
				// installed
				// correctly so throw an error
				String msg = "No local client was established.  You may want to check the installation of "
					+ "Documentum or this application on this machine.";
				this.logger.error(msg);
				throw new RuntimeException(msg);
			}
		} catch (DfException e) {
			String msg = "No local client was established.  You may want to check the installation of "
				+ "Documentum or this application on this machine.";
			this.logger.error(msg);
			throw new RuntimeException(msg, e);
		}

		try {
			passTmp = EncryptPasswordUtil.decryptPassword(passTmp);
		} catch (Throwable t) {
			// Not encrypted, use literal
		}
		final String docbasePassword = passTmp;

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
			this.logger.error("Error establishing Documentum session", e);
		} catch (DfAuthenticationException e) {
			this.logger.error("Error establishing Documentum session", e);
		} catch (DfPrincipalException e) {
			this.logger.error("Error establishing Documentum session", e);
		} catch (DfServiceException e) {
			this.logger.error("Error establishing Documentum session", e);
		}

		if (importOrExport.equalsIgnoreCase("Export")) {
			// Start the export process
			startExporting();
		} else {
			// Start the import process
			startImporting();
		}
		if (this.logger.isEnabledFor(Level.INFO)) {
			this.logger.debug("##### CMS Migration Process finished #####");
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
		if (this.logger.isEnabledFor(Level.INFO)) {
			this.logger.info("##### CMS Migration Process Started #####");
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
				this.logger.error("No local client was established.  You may want to check the installation of "
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
			this.logger.error("Error establishing Documentum session", e);
		} catch (DfAuthenticationException e) {
			this.logger.error("Error establishing Documentum session", e);
		} catch (DfPrincipalException e) {
			this.logger.error("Error establishing Documentum session", e);
		} catch (DfServiceException e) {
			this.logger.error("Error establishing Documentum session", e);
		} catch (DfException e) {
			this.logger.error("Error establishing Documentum session", e);
		}

		// Start the export process
		startExporting2();

		// Start the import process
		startImporting2();

		// test on r_modify_date modification
		// testChangingInternalAttributes(dctmSession);

		// test xml synchronization
		// testXMLSynchronization();

		if (this.logger.isEnabledFor(Level.INFO)) {
			this.logger.debug("##### CMS Migration Process finished #####");
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
		if (this.logger.isEnabledFor(Level.INFO)) {
			this.logger.info("##### Export Process Started #####");
		}

		// Load source repository configuration information
		RepositoryConfiguration srcRepoConfig = RepositoryConfiguration.getRepositoryConfiguration();
		try {
			srcRepoConfig.loadRepositoryConfiguration(this.dctmSession);
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
		String selectClause = CMSMFAppConstants.EXPORT_QUERY_SELECT_CLAUSE;
		String fromWhereClause = PropertiesManager.getProperty(CMSMFProperties.EXPORT_QUERY_PREDICATE, "");

		IDfQuery dqlQry = new DfClientX().getQuery();
		dqlQry.setDQL(selectClause + fromWhereClause);
		dqlQry.setBatchSize(20000);
		if (this.logger.isEnabledFor(Level.DEBUG)) {
			this.logger.debug("Export DQL Query is: " + selectClause + fromWhereClause);
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
					this.logger.error("Couldn't retrieve object by ID: " + objID, e);
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
					this.logger.error("Couldn't retrieve object information from repository for id: " + objID, e);
				} catch (IOException e) {
					// If there is IOException, log the error and exit out
					this.logger.fatal("Couldn't serialize an object to the filesystem for id: " + objID, e);
					// close all of the file streams
					try {
						fsm.closeAllStreams();
					} catch (CMSMFIOException e2) {
						this.logger.error("Couldn't close all of the filestreams", e2);
					}
					throw (e);
				}
			} // while (resultCol.next())
			resultCol.close();

			// Export source repository configuration information
			try {
				DctmObjectWriter.writeBinaryObject(srcRepoConfig);
			} catch (CMSMFException e) {
				this.logger.error("Couldn't retrieve source repository configuration information.", e);
			}
		} catch (DfException e) {
			// If there is a DfException while running the export query, log the error and exit out
			this.logger.fatal("Export Query failed to run. Query is:  " + selectClause + fromWhereClause, e);
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

		if (this.logger.isEnabledFor(Level.INFO)) {
			this.logger.info("##### Export Process Finished #####");
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
		if (this.logger.isEnabledFor(Level.INFO)) {
			this.logger.info("##### Export Process Started #####");
		}

		// Load source repository configuration information
		RepositoryConfiguration srcRepoConfig = RepositoryConfiguration.getRepositoryConfiguration();
		try {
			srcRepoConfig.loadRepositoryConfiguration(this.dctmSession);
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
				this.logger.error("Couldn't retrieve object by ID: " + objID, e);
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
					this.logger.error("Couldn't retrieve source repository configuration information.", e);
				}

			} catch (CMSMFException e) {
				// If for some reason object is not retrieved from the system,
				// or written to the
				// filesystem, write to an error log and continue on
				this.logger.error("Couldn't retrieve object information from repository for id: " + objID, e);
			} catch (IOException e) {
				// If there is IOException, log the error and exit out
				this.logger.fatal("Couldn't serialize an object to the filesystem for id: " + objID, e);
				throw (e);
			} finally {
				// close all of the file streams
				try {
					fsm.closeAllStreams();
				} catch (CMSMFIOException e) {
					this.logger.error("Couldn't close all of the filestreams", e);
				}
			}
		}

		// print the counters to see how many objects were processed
		AppCounter.getObjectCounter().printCounters();

		if (this.logger.isEnabledFor(Level.INFO)) {
			this.logger.info("##### Export Process Finished #####");
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
		CMSMFMain.printImportReport();

		if (this.logger.isEnabledFor(Level.INFO)) {
			this.logger.info("##### Import Process Finished #####");
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
	 * Start importing2. This method is deprecated. Use startImporting() instead
	 * 
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	@Deprecated
	private void startImporting2() throws IOException {
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
					this.logger.error("Error creating/Updating user in dctm repository.", e);
				} catch (CMSMFException e) {
					// log the error and continue on with next object
					this.logger.error("Error reading user object from file.", e);
				}
			}
		} catch (IOException e) {
			// If there is IOException, close all of the streams, log the error and exit out
			try {
				fsm.closeAllStreams();
			} catch (CMSMFIOException e1) {
				this.logger.error("Couldn't close all of the filestreams", e1);
			}
			this.logger.fatal("Couldn't deserialize an object from the filesystem.", e);
			throw (e);
		} catch (CMSMFException e) {
			// log the error and continue on with next object
			this.logger.error("Error reading user object from file.", e);
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
					this.logger.error("Error creating/Updating group in dctm repository.", e);
				} catch (CMSMFException e) {
					// log the error and continue on with next object
					this.logger.error("Error reading group object from file.", e);
				}
			}
		} catch (IOException e) {
			// If there is IOException, close all of the streams, log the error and exit out
			try {
				fsm.closeAllStreams();
			} catch (CMSMFIOException e1) {
				this.logger.error("Couldn't close all of the filestreams", e1);
			}
			this.logger.fatal("Couldn't deserialize an object from the filesystem.", e);
			throw (e);
		} catch (CMSMFException e) {
			// log the error and continue
			this.logger.error("Error reading group object from file.", e);
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
					this.logger.error("Error creating/Updating ACL in dctm repository.", e);
				} catch (CMSMFException e) {
					// log the error and continue on with next object
					this.logger.error("Error reading ACL object from file.", e);
				}
			}
		} catch (IOException e) {
			// If there is IOException, close all of the streams, log the error and exit out
			try {
				fsm.closeAllStreams();
			} catch (CMSMFIOException e1) {
				this.logger.error("Couldn't close all of the filestreams", e1);
			}
			this.logger.fatal("Couldn't deserialize an object from the filesystem.", e);
			throw (e);
		} catch (CMSMFException e) {
			// log the error and continue on with next object
			this.logger.error("Error reading ACL object from file.", e);
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
					this.logger.error("Error creating/Updating folder in dctm repository.", e);
				} catch (CMSMFException e) {
					// log the error and continue on with next object
					this.logger.error("Error reading folder object from file.", e);
				}
			}
		} catch (IOException e) {
			// If there is IOException, close all of the streams, log the error and exit out
			try {
				fsm.closeAllStreams();
			} catch (CMSMFIOException e1) {
				this.logger.error("Couldn't close all of the filestreams", e1);
			}
			this.logger.fatal("Couldn't deserialize an object from the filesystem.", e);
			throw (e);
		} catch (CMSMFException e) {
			// log the error and continue on with next object
			this.logger.error("Error reading folder object from file.", e);
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
					this.logger.error("Error creating/Updating document in dctm repository.", e);
				} catch (CMSMFException e) {
					// log the error and continue on with next object
					this.logger.error("Error reading document object from file.", e);
				}
			}
		} catch (CMSMFException e) {
			// log the error and continue on with next object
			this.logger.error("Error reading document object from file.", e);
		} catch (IOException e) {
			// If there is IOException, close all of the streams, log the error and exit out
			try {
				fsm.closeAllStreams();
			} catch (CMSMFIOException e1) {
				this.logger.error("Couldn't close all of the filestreams", e1);
			}
			this.logger.fatal("Couldn't deserialize an object from the filesystem.", e);
			throw (e);
		}

		// close all of the file streams
		try {
			fsm.closeAllStreams();
		} catch (CMSMFIOException e) {
			this.logger.error("Couldn't close all of the filestreams", e);
		}

		// print the counters to see how many objects were processed
		AppCounter.getObjectCounter().printCounters();

		if (this.logger.isEnabledFor(Level.INFO)) {
			this.logger.info("##### Import Process Finished #####");
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
		if (this.logger.isEnabledFor(Level.DEBUG)) {
			this.logger.debug("Started Importing: " + dctmObjectType);
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
					this.logger.error("Error creating/Updating " + dctmObjectType + " in dctm repository.", e);

					// increment import process error count; If the error count is more than the
// error
					// threshold specified in the properties file, quit the import process after
// closing all
					// of the file streams
					RunTimeProperties.getRunTimePropertiesInstance().incrementImportProcessErrorCount();

					int importErrorThreshold = PropertiesManager.getProperty(
						CMSMFProperties.IMPORT_ERRORCOUNT_THRESHOLD, 0);
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
