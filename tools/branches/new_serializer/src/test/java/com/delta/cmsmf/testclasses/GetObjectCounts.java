package com.delta.cmsmf.testclasses;

import com.documentum.com.DfClientX;
import com.documentum.com.IDfClientX;
import com.documentum.fc.client.IDfCollection;
import com.documentum.fc.client.IDfQuery;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.common.DfException;

public class GetObjectCounts {

	private static final String userCounts = "Select count(*) from dm_user";
	private static final String groupCounts = "Select count(*) from dm_group";
	private static final String aclCounts = "Select count(*) from dm_acl";
	private static final String namedAclCounts = "Select count(*) from dm_acl where r_is_internal = 0";
	private static final String internalAclCounts = "Select count(*) from dm_acl where r_is_internal = 1";
	private static final String cabinetCounts = "Select count(*) from dm_cabinet";
	private static final String folderCounts = "Select count(*) from dm_folder";
	private static final String documentCounts = "Select count(*) from dm_document";

	/**
	 * @param args
	 * @throws DfException
	 */
	public static void main(String[] args) throws DfException {
		IDfSession dctmSession = (new GetDMCLSession("cobtest", "dmadmin", "dmadmin")).getSession();

		GetObjectCounts newInstance = new GetObjectCounts();
		newInstance.printCounts(dctmSession);

	}

	protected void printCounts(IDfSession dctmSession) throws DfException {
		System.out.println("Total no of users: " + runSimpleCountQuery(dctmSession, GetObjectCounts.userCounts));
		System.out.println("Total no of groups: " + runSimpleCountQuery(dctmSession, GetObjectCounts.groupCounts));
		System.out.println("Total no of ACLs: " + runSimpleCountQuery(dctmSession, GetObjectCounts.aclCounts));
		System.out.println("Total no of Named ACLs: "
			+ runSimpleCountQuery(dctmSession, GetObjectCounts.namedAclCounts));
		System.out.println("Total no of Internal ACLs: "
			+ runSimpleCountQuery(dctmSession, GetObjectCounts.internalAclCounts));
		System.out.println("Total no of Cabinets: " + runSimpleCountQuery(dctmSession, GetObjectCounts.cabinetCounts));
		System.out.println("Total no of Folders: " + runSimpleCountQuery(dctmSession, GetObjectCounts.folderCounts));
		System.out
			.println("Total no of Documents: " + runSimpleCountQuery(dctmSession, GetObjectCounts.documentCounts));
	}

	private int runSimpleCountQuery(IDfSession dctmSession, String qryString) throws DfException {
		int rtrnCnt = 0;
		IDfClientX clientX = new DfClientX();
		IDfQuery groupsQry = clientX.getQuery();
		groupsQry.setDQL(qryString);
		groupsQry.setBatchSize(5);
		IDfCollection collection = groupsQry.execute(dctmSession, IDfQuery.DF_READ_QUERY);

		if (collection.next()) {
			rtrnCnt = collection.getInt("count(*)");
		}

		return rtrnCnt;
	}

}
