package com.delta.cmsmf.testclasses;

import com.documentum.fc.client.IDfEnumeration;
import com.documentum.fc.client.IDfPersistentObject;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.common.DfException;

public class ExportQueryTest {

	/**
	 * @param args
	 * @throws DfException
	 */
	public static void main(String[] args) throws DfException {

		String query = "select r_object_id, i_vstamp, r_object_type, r_aspect_name, i_is_replica, i_is_reference"
			+ " from dm_sysobject where folder('/SKM_TEST/SKM1', descend)";
		IDfSession dctmSession = (new GetDMCLSession("cobtest", "dmadmin", "dmadmin")).getSession();

		IDfEnumeration objectsByQuery = dctmSession.getObjectsByQuery(query, null);

		while (objectsByQuery.hasMoreElements()) {
			IDfPersistentObject prsstntObj = (IDfPersistentObject) objectsByQuery.nextElement();
			System.out.println("ID: " + prsstntObj.getObjectId().getId() + " name: "
				+ prsstntObj.getString("object_name") + " type: " + prsstntObj.getString("r_object_type"));
		}

		System.out.println("Done!!");

	}
}
