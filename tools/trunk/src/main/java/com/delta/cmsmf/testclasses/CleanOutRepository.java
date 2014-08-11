package com.delta.cmsmf.testclasses;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.documentum.com.DfClientX;
import com.documentum.com.IDfClientX;
import com.documentum.fc.client.IDfACL;
import com.documentum.fc.client.IDfCollection;
import com.documentum.fc.client.IDfFolder;
import com.documentum.fc.client.IDfGroup;
import com.documentum.fc.client.IDfQuery;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.client.IDfUser;
import com.documentum.fc.common.DfException;
import com.documentum.fc.common.DfId;
import com.documentum.fc.common.IDfList;
import com.documentum.operations.IDfDeleteOperation;
import com.documentum.operations.IDfOperationError;

public class CleanOutRepository {

	private static boolean clearUsersFlag = true;
	private static boolean clearGroupsFlag = true;
	private static boolean clearACLsFlag = true;
	private static boolean clearFoldersFlag = true;

	private static List<String> foldersToBeDeleted = new ArrayList<String>() {
		/**
		 *
		 */
		private static final long serialVersionUID = 1L;

		{
			add("/1 - Continuity of Business");
			add("/SKM Temp11");
			add("/Temp1");
			add("/Website");
			add("/Camille Whitton");
			add("/cobprod");
			add("/cobsi");
			add("/dtedm");
			add("/Harvey, Rob");
			add("/Hobbs, Tommy");
			add("/Holmes, Valecia");
			add("/Jeannine Hill");
			add("/Lewis, Tim");
			add("/Maher Razick");
			add("/Sarbanes Oxley");
			add("/Skelton, Kathy");
			add("/Spann, Dorothy");
			add("/Weathersby, Raymond T.");
		}
	};

	private static String deleteGroupsQuery = "select r_object_id, group_name from dm_group "
		+ "where group_name not like 'dm_%' " + "and group_name not like 'queue%' " + "and group_name not like 'skm%' "
		+ "and group_name not in ('process_report_admin', 'admingroup', 'docu')";

	private static String deleteUsersQuery = "select r_object_id, user_name from dm_user "
		+ "where user_name not like 'dm_%' " + "and user_name not like 'queue%' " + "and user_name not like 'skm%' "
		+ "and user_name not in ('process_report_admin', 'admingroup', 'cobtest', 'docu') " + "and r_is_group = false";

	private static String dmCleanQuery = "EXECUTE do_method WITH method = 'dm_DMClean'";

	private static String deleteTempACLQuery = "select r_object_id, object_name, owner_name from dm_acl "
		+ " where description = 'CMSMF Temp ACL'";

	private static String deleteInternalACLQuery = "select r_object_id, object_name, owner_name from dm_acl "
		+ " where (owner_name not in ( select user_name from dm_user)) "
		+ " or object_name like 'BCP%' or object_name like 'COB%' or object_name like 'SOX%' "
		+ " or object_name like 'Comp%' or object_name like 'Conf%' or object_name like 'PCI%' "
		+ " or object_name like 'Go Team%' or object_name like 'IS%' or object_name like 'PKI%' or object_name like 'Website%'";

	/**
	 * @param args
	 * @throws DfException
	 */
	public static void main(String[] args) throws DfException {

		System.out.println("Docbase CleanUp Started " + new Date());

		IDfSession dctmSession = (new GetDMCLSession("cobtest", "dmadmin", "dmadmin")).getSession();

		// print out counts
		GetObjectCounts newInstance = new GetObjectCounts();
		newInstance.printCounts(dctmSession);

		// Remove folders and documents
		if (CleanOutRepository.clearFoldersFlag) {
			CleanOutRepository.removeFolders(dctmSession);
		}

		// Remove ACLS
		if (CleanOutRepository.clearACLsFlag) {
			CleanOutRepository.removeACLs(dctmSession);
		}

		// Remove groups
		if (CleanOutRepository.clearGroupsFlag) {
			CleanOutRepository.removeGroups(dctmSession);
		}

		// Remove users
		if (CleanOutRepository.clearUsersFlag) {
			CleanOutRepository.removeUsers(dctmSession);
		}

		// Remove dm_clean to remove orphaned objects
		CleanOutRepository.runDmClean(dctmSession);

		// print out counts
		newInstance.printCounts(dctmSession);

		System.out.println("Docbase CleanUp Finished " + new Date());
	}

	private static void runDmClean(IDfSession dctmSession) throws DfException {
		System.out.println("Running dm_DMClean method");
		IDfClientX clientX = new DfClientX();
		IDfQuery dmCleanQry = clientX.getQuery();
		dmCleanQry.setDQL(CleanOutRepository.dmCleanQuery);
		dmCleanQry.execute(dctmSession, IDfQuery.READ_QUERY);
		System.out.println("Finished running dm_DMClean method");
	}

	private static void removeUsers(IDfSession dctmSession) throws DfException {
		// Run a query to get all of the users that does not start with dm and delete them
		IDfClientX clientX = new DfClientX();
		IDfQuery groupsQry = clientX.getQuery();
		groupsQry.setDQL(CleanOutRepository.deleteUsersQuery);
		groupsQry.setBatchSize(5000);
		IDfCollection collection = groupsQry.execute(dctmSession, IDfQuery.DF_READ_QUERY);

		while (collection.next()) {
			// String userID = collection.getString("r_object_id");
			String userName = collection.getString("user_name");

			IDfUser userObj = dctmSession.getUser(userName);
			System.out.println("Deleting user: " + userName);
			userObj.destroy();
		}

		collection.close();
	}

	private static void removeGroups(IDfSession dctmSession) throws DfException {

		// Run a query to get all of the groups that does not start with dm and delete them
		IDfClientX clientX = new DfClientX();
		IDfQuery groupsQry = clientX.getQuery();
		groupsQry.setDQL(CleanOutRepository.deleteGroupsQuery);
		IDfCollection collection = groupsQry.execute(dctmSession, IDfQuery.DF_READ_QUERY);

		while (collection.next()) {
			// String groupID = collection.getString("r_object_id");
			String groupName = collection.getString("group_name");

			IDfGroup groupObj = dctmSession.getGroup(groupName);
			System.out.println("Deleting group: " + groupName);
			groupObj.destroy();
		}

		collection.close();
	}

	private static void removeACLs(IDfSession dctmSession) throws DfException {
		// Run a query to get all of the temporary ACLs that were created using CMSMF program
		IDfClientX clientX = new DfClientX();
		IDfQuery tempACLQry = clientX.getQuery();
		tempACLQry.setDQL(CleanOutRepository.deleteTempACLQuery);
		tempACLQry.setBatchSize(5000);
		IDfCollection collection = tempACLQry.execute(dctmSession, IDfQuery.DF_READ_QUERY);

		while (collection.next()) {
			String aclID = collection.getString("r_object_id");
			String aclName = collection.getString("object_name");
			String aclOwnerName = collection.getString("owner_name");

			IDfACL aclObj = (IDfACL) dctmSession.getObject(new DfId(aclID));

			System.out.println("Deleting ACL: <" + aclName + ", " + aclOwnerName + ">");
			aclObj.destroyACL(true);
		}

		collection.close();

		tempACLQry.setDQL(CleanOutRepository.deleteInternalACLQuery);
		tempACLQry.setBatchSize(5000);
		collection = tempACLQry.execute(dctmSession, IDfQuery.DF_READ_QUERY);

		while (collection.next()) {
			String aclID = collection.getString("r_object_id");
			String aclName = collection.getString("object_name");
			String aclOwnerName = collection.getString("owner_name");

			IDfACL aclObj = (IDfACL) dctmSession.getObject(new DfId(aclID));

			System.out.println("Deleting ACL: <" + aclName + ", " + aclOwnerName + ">");
			aclObj.destroyACL(true);
		}

		collection.close();

	}

	private static void removeFolders(IDfSession dctmSession) throws DfException {

		// Prepare delete operation
		IDfClientX clientX = new DfClientX();
		IDfDeleteOperation deleteOp = clientX.getDeleteOperation();

		deleteOp.setSession(dctmSession);
		deleteOp.setDeepFolders(true);
		deleteOp.setVersionDeletionPolicy(IDfDeleteOperation.ALL_VERSIONS);

		// Add various folders
		for (String fldrPath : CleanOutRepository.foldersToBeDeleted) {
			System.out.println("Deleting folder: " + fldrPath);
			IDfFolder fldrObj = dctmSession.getFolderByPath(fldrPath);
			if (fldrObj != null) {
				deleteOp.add(fldrObj);
			}
		}

		// Execute the delete operation
		if (deleteOp.execute() == false) {
			// Display an errors encountered.
			CleanOutRepository.displayErrorList(deleteOp.getErrors());
		}
	}

	private static void displayErrorList(IDfList errors) throws DfException {
		for (int i = 0; i < errors.getCount(); i++) {
			IDfOperationError error = (IDfOperationError) errors.get(i);
			System.out.println(error.getErrorCode() + " : " + error.getMessage());
			System.out.println(error.getException().getMessage());
		}
	}

}
