package com.delta.cmsmf.testclasses;

import java.io.IOException;
import java.util.List;

import com.delta.cmsmf.exception.CMSMFException;
import com.delta.cmsmf.filestreams.FileStreamsManager;
import com.delta.cmsmf.mainEngine.RepositoryConfiguration;
import com.delta.cmsmf.serialization.DctmObjectReader;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.common.DfException;

public class ReposotiryConfigurationTester {

	/**
	 * @param args
	 * @throws DfException
	 */
	public static void main(String[] args) throws DfException {
		IDfSession dctmSession = (new GetDMCLSession("cobtest", "dmadmin", "dmadmin")).getSession();

		RepositoryConfiguration cobtestConfig = RepositoryConfiguration.getRepositoryConfiguration();
		cobtestConfig.loadRepositoryConfiguration(dctmSession);
		System.out.println(cobtestConfig.printRepositoryConfiguration());

	}

	/**
	 * The main method used for testing.
	 * 
	 * @throws CMSMFException
	 *             the cMSMF exception
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	public static void main2() throws CMSMFException, IOException {

		String streamFilesDirectoryLocation = "C://CMSMF_Streams";

		// First set the directory path where all of the files were created
		FileStreamsManager fsm = FileStreamsManager.getFileStreamManager();
		fsm.setStreamsDirectoryPath(streamFilesDirectoryLocation);

		RepositoryConfiguration srcRepoConfig = DctmObjectReader.readSrcRepoConfig();
		List<String> fileStores = srcRepoConfig.getFileStores();

		System.out.println(fileStores);
		System.out.println("DONE");

	}

}
