package com.delta.cmsmf.testclasses;

import java.io.IOException;

import org.apache.commons.configuration.ConfigurationException;

import com.delta.cmsmf.cmsobjects.DctmFolder;
import com.delta.cmsmf.cmsobjects.DctmObject;
import com.delta.cmsmf.cmsobjects.DctmObjectTypesEnum;
import com.delta.cmsmf.exception.CMSMFException;
import com.delta.cmsmf.filestreams.FileStreamsManager;
import com.delta.cmsmf.mainEngine.DctmObjectExportHelper;
import com.delta.cmsmf.mainEngine.RepositoryConfiguration;
import com.delta.cmsmf.properties.PropertiesManager;
import com.delta.cmsmf.runtime.AppCounter;
import com.delta.cmsmf.serialization.DctmObjectReader;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.common.DfException;
import com.documentum.fc.tools.RegistryPasswordUtils;

public class DctmFolderTest {

	/**
	 * The main method.
	 * 
	 * @param args
	 *            the arguments
	 * @throws DfException
	 *             Signals that Dctm Server error has occurred.
	 * @throws CMSMFException
	 * @throws ConfigurationException
	 * @throws IOException
	 */
	public static void main(String[] args) throws DfException, CMSMFException, ConfigurationException, IOException {
		PropertiesManager pm = PropertiesManager.getPropertiesManager();

		pm.loadProperties("config/CMSMF_app.properties");
		// Set the filesystem location where files will be created or read from
		String streamFilesDirectoryLocation = PropertiesManager.getPropertiesManager().getProperty(
			"cmsmf.app.importexport.directory", "");

		// First set the directory path where all of the files will be created
		FileStreamsManager fsm = FileStreamsManager.getFileStreamManager();
		fsm.setStremsDiretoryPath(streamFilesDirectoryLocation);

		// Delete existing cmsmf stream files before export process
		fsm.deleteStreamFiles();

		String encryptedPasswd = "IL9llvkXbxs=";
		String password = RegistryPasswordUtils.decrypt(encryptedPasswd);

		IDfSession dctmSession = (new GetDMCLSession("cobtest", "dmadmin", password)).getSession();
		// IDfSession dctmSession = (new GetDMCLSession("cobtest", "dmadmin",
		// "DM_ENCR_PASS=k5B43znDAL0Jnej4wBcYEYKOe4te080xbb4rdoQMTkRk1joXWgNzcUhgWJnbvD55")).getSession();

		// Load source repository configuration information
		RepositoryConfiguration srcRepoConfig = RepositoryConfiguration.getRepositoryConfiguration();
		srcRepoConfig.loadRepositoryConfiguration(dctmSession);

		DctmObjectExportHelper.serializeFolderByPath(dctmSession, "/skm_cab_test");

		// print the counters to see how many objects were processed
		AppCounter.getObjectCounter().printCounters();

		// Import portion
		// reset the counters that keeps track of how many objects are read
		AppCounter.getObjectCounter().resetCounters();

		DctmObject dctmObject = null;
		dctmObject = (DctmObject) DctmObjectReader.readObject(DctmObjectTypesEnum.DCTM_FOLDER);

		while (dctmObject != null) {

			// Increment appropriate counter
			AppCounter.getObjectCounter().incrementCounter(DctmObjectTypesEnum.DCTM_FOLDER);
			try {
				// Create appropriate object in target repository
				dctmObject.setDctmSession(dctmSession);
				dctmObject.createInCMS();
			} catch (DfException e) {
				e.printStackTrace();
			}
			// Read next object from the file until you reach end of the file
			dctmObject = (DctmObject) DctmObjectReader.readObject(DctmObjectTypesEnum.DCTM_FOLDER);
		}

		// Print folsers import report
		DctmFolder.printImportReport();

		System.out.println("Done!");

	}

}
