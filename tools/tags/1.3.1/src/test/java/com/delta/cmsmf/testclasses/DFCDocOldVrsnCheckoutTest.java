package com.delta.cmsmf.testclasses;

import com.documentum.fc.client.IDfSession;
import com.documentum.fc.client.IDfSysObject;
import com.documentum.fc.common.DfException;
import com.documentum.fc.common.DfId;

// TODO: Auto-generated Javadoc
/**
 * The Class DupObjectTest.
 */
public class DFCDocOldVrsnCheckoutTest {

	/**
	 * @param args
	 * @throws DfException
	 */
	public static void main(String[] args) throws DfException {

		IDfSession dctmSession = (new GetDMCLSession("cobtest", "dmadmin", "dmadmin")).getSession();

		IDfSysObject oldVerDoc = (IDfSysObject) dctmSession.getObject(new DfId("0901116f80022830"));
		oldVerDoc.checkout();

		System.out.println("Done!!");

	}

}
