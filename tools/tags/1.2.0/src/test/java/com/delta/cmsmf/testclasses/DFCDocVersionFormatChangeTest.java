package com.delta.cmsmf.testclasses;

import com.documentum.fc.client.IDfSession;
import com.documentum.fc.client.IDfSysObject;
import com.documentum.fc.common.DfException;
import com.documentum.fc.common.DfId;

// TODO: Auto-generated Javadoc
/**
 * The Class DupObjectTest.
 */
public class DFCDocVersionFormatChangeTest {

	/**
	 * @param args
	 * @throws DfException
	 */
	public static void main(String[] args) throws DfException {

		IDfSession dctmSession = (new GetDMCLSession("cobtest2", "dmadmin", "dmadmin")).getSession();

		IDfSysObject oldVerDoc = (IDfSysObject) dctmSession.getObject(new DfId("090bde3180000fc3"));
		oldVerDoc.checkout();
		oldVerDoc.setFileEx("C:\\CMSMF_Streams\\ContentFiles\\80\\0e\\c6\\0600a92b800ec6e1_excel8book_0", "excel", 0,
			null);
		oldVerDoc.setString("a_content_type", "excel");
		oldVerDoc.checkin(false, "1.3");

		System.out.println("Done!!");

	}

}
