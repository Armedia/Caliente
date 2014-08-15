package com.delta.cmsmf.testclasses;

import com.documentum.fc.client.IDfGroup;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.common.DfException;
import com.documentum.fc.common.DfId;

// TODO: Auto-generated Javadoc
/**
 * The Class DupObjectTest.
 */
public class DupObjectTest {

	/**
	 * @param args
	 * @throws DfException
	 */
	public static void main(String[] args) throws DfException {

		IDfSession dctmSession = (new GetDMCLSession("cobtest", "dmadmin", "dmadmin")).getSession();

		IDfGroup group = (IDfGroup) dctmSession.getObject(new DfId("1201116f80000904"));

		if (group != null) {
			group.setDescription("test Description!");
			group.save();
		}
		System.out.println("Done!!");

	}

}
