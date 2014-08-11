package com.delta.cmsmf.testclasses;

import java.io.IOException;

import org.apache.commons.configuration.ConfigurationException;

import com.delta.cmsmf.cmsobjects.DctmGroup;
import com.delta.cmsmf.cmsobjects.DctmObject;
import com.delta.cmsmf.cmsobjects.DctmObjectTypesEnum;
import com.delta.cmsmf.constants.CMSMFProperties;
import com.delta.cmsmf.constants.DctmAttrNameConstants;
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

public class DctmGroupTest {

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
			CMSMFProperties.CMSMF_APP_IMPORTEXPORT_DIRECTORY, "");

		// First set the directory path where all of the files will be created
		FileStreamsManager fsm = FileStreamsManager.getFileStreamManager();
		fsm.setStreamsDirectoryPath(streamFilesDirectoryLocation);

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

		DctmObjectExportHelper.serializeGroupByName(dctmSession, "skm_test_grp1");

		// print the counters to see how many objects were processed
		AppCounter.getObjectCounter().printCounters();

		// Import portion
		// reset the counters that keeps track of how many objects are read
		AppCounter.getObjectCounter().resetCounters();

		DctmObject dctmObject = null;
		dctmObject = (DctmObject) DctmObjectReader.readObject(DctmObjectTypesEnum.DCTM_GROUP);

		while (dctmObject != null) {

			// add 1 at the end of group name
			String groupName = dctmObject.getStrSingleAttrValue(DctmAttrNameConstants.GROUP_NAME) + "1";
			dctmObject.findAttribute(DctmAttrNameConstants.GROUP_NAME).setSingleValue(groupName);

			// add 1 at the end of group_display_name field to make it unique
			String groupDisplayName = dctmObject.getStrSingleAttrValue(DctmAttrNameConstants.GROUP_DISPLAY_NAME) + "1";
			dctmObject.findAttribute(DctmAttrNameConstants.GROUP_DISPLAY_NAME).setSingleValue(groupDisplayName);

			// Increment appropriate counter
			AppCounter.getObjectCounter().incrementCounter(DctmObjectTypesEnum.DCTM_GROUP);
			try {
				// Create appropriate object in target repository
				dctmObject.setDctmSession(dctmSession);
				dctmObject.createInCMS();
			} catch (DfException e) {
				e.printStackTrace();
			}
			// Read next object from the file until you reach end of the file
			dctmObject = (DctmObject) DctmObjectReader.readObject(DctmObjectTypesEnum.DCTM_GROUP);
		}

		// Print groups import report
		DctmGroup.printImportReport();

		System.out.println("Done!");

	}

}
