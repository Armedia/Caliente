package com.delta.cmsmf.testclasses;

import com.delta.cmsmf.exception.CMSMFException;
import com.delta.cmsmf.mainEngine.DctmObjectImportHelper;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.common.DfException;

public class CreateFolderByPathTest {

	/**
	 * @param args
	 * @throws DfException
	 * @throws CMSMFException
	 */
	public static void main(String[] args) throws DfException, CMSMFException {
		IDfSession dctmSession = (new GetDMCLSession("cobtest", "dmadmin", "dmadmin")).getSession();

		DctmObjectImportHelper.createFolderByPath(dctmSession, "/sometemp/deleteme");

	}

}
