package com.delta.cmsmf.testclasses;

import com.documentum.fc.client.IDfFolder;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.client.IDfSysObject;
import com.documentum.fc.common.DfException;

// TODO: Auto-generated Javadoc
/**
 * The Class DupObjectTest.
 */
public class DFCSysObjectLinkTest {

	/**
	 * @param args
	 * @throws DfException
	 */
	public static void main(String[] args) throws DfException {

		IDfSession dctmSession = (new GetDMCLSession("cobtest", "dmadmin", "dmadmin")).getSession();

		IDfFolder testFldr = dctmSession.getFolderByPath("/SKM_TEST/bkup");

		try {
			testFldr.link("/SKM_TEST");
		} catch (DfException dfe) {
			if (dfe.getMessageId().equals("DM_SYSOBJECT_E_ALREADY_LINKED")) {
				System.out.println("Already Linked error ignored");
			} else {
				throw (dfe);
			}
		}

		IDfSysObject doc = (IDfSysObject) dctmSession
			.getObjectByQualification("dm_document where folder('/SKM_TEST/bkup') and object_name = 'shridev.xls'");
		try {
			doc.link("/SKM_TEST/bkup");
		} catch (DfException dfe) {
			if (dfe.getMessageId().equals("DM_SYSOBJECT_E_ALREADY_LINKED")) {
				System.out.println("Already Linked error ignored");
			} else {
				throw (dfe);
			}
		}

		String objName = "someone has 'F' in their name";
		System.out.println(objName);
// StringUtils.replace(objName, "'", "''", 5);
		objName = objName.replaceAll("'", "''");

		System.out.println(objName);

		System.out.println("Done!!");

	}

}
