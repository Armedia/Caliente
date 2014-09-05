package com.delta.cmsmf.testclasses;

import com.documentum.fc.client.IDfSession;
import com.documentum.fc.client.IDfSysObject;
import com.documentum.fc.common.DfException;
import com.documentum.fc.common.IDfId;

// TODO: Auto-generated Javadoc
/**
 * The Class TestBranches.
 */
public class TestBranches {

	/**
	 * The main method.
	 * 
	 * @param args
	 *            the arguments
	 * @throws DfException
	 *             Signals that Dctm Server error has occurred.
	 */
	public static void main(String[] args) throws DfException {

		System.out.println("Test Started");
		String folderPath = "/SKM_TEST/DFC_TEST";
		String dmDocument = "dm_document";

		IDfSession dctmSession = (new GetDMCLSession("cobtest", "dmadmin", "dmadmin")).getSession();

		// create version 1.0
		IDfSysObject obj10 = (IDfSysObject) dctmSession.newObject(dmDocument);
		obj10.setObjectName("doc 1.0");
		obj10.mark("1.0");
		obj10.link(folderPath);
		obj10.save();

		obj10.checkout();
		obj10.setObjectName("doc 1.1");
		obj10.mark("1.1");
		IDfId obj11ID = obj10.checkin(false, "");
		IDfSysObject obj11 = (IDfSysObject) dctmSession.getObject(obj11ID);

		obj11.checkout();
		obj11.setObjectName("doc 1.2");
		obj11.mark("1.2");
		IDfId obj12ID = obj11.checkin(false, "");
		IDfSysObject obj12 = (IDfSysObject) dctmSession.getObject(obj12ID);

		obj12.checkout();
		obj12.setObjectName("doc 2.0");
		obj12.mark("2.0");
		// IDfId obj20ID = obj12.checkin(false, "");
		// IDfSysObject obj20 = (IDfSysObject) dctmSession.getObject(obj20ID);

		// branch
		IDfId obj1110ID = obj11.branch("");
		IDfSysObject obj1110 = (IDfSysObject) dctmSession.getObject(obj1110ID);
		obj1110.setObjectName("doc 1.1.1.0");
		obj1110.save();

		System.out.println("Test Finished");
	}

}
