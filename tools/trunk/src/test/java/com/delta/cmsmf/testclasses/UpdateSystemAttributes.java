package com.delta.cmsmf.testclasses;

import com.documentum.com.DfClientX;
import com.documentum.fc.client.IDfCollection;
import com.documentum.fc.client.IDfQuery;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.common.DfException;

public class UpdateSystemAttributes {

	/**
	 * @param args
	 * @throws DfException
	 */
	public static void main(String[] args) throws DfException {

		IDfSession dctmSession = (new GetDMCLSession("cobtest", "dmadmin", "dmadmin")).getSession();

		String sqlStr = "UPDATE dm_sysobject_s SET acl_name = ''dm_4501116f80000102'', acl_domain = ''dmadmin'' WHERE r_object_id = ''0c01116f80000107''";

		UpdateSystemAttributes.runExecSQL(dctmSession, sqlStr);

	}

	private static void runExecSQL(IDfSession session, String sqlQueryString) throws DfException {
		IDfQuery dqlQry = new DfClientX().getQuery();
		dqlQry.setDQL("EXECUTE exec_sql WITH query='" + sqlQueryString + "'");
		IDfCollection resultCol = dqlQry.execute(session, IDfQuery.EXEC_QUERY);
		if (resultCol.next()) {
			if (resultCol.getValueAt(0).toString().equalsIgnoreCase("F")) {
				System.out.println("Error running exec_sql, rolling back changes.");
				dqlQry.setDQL("EXECUTE exec_sql with query='rollback';");
				resultCol.close();
				resultCol = dqlQry.execute(session, IDfQuery.EXEC_QUERY);
			} else {
				System.out.println("Success running exec_sql, committing changes.");
				dqlQry.setDQL("EXECUTE exec_sql with query='commit';");
				resultCol.close();
				resultCol = dqlQry.execute(session, IDfQuery.EXEC_QUERY);
			}
		}
		resultCol.close();
	}

	/**
	 * Test changing internal attributes.
	 * 
	 * @param dctmSession
	 *            the dctm session
	 * @throws DfException
	 *             Signals that Dctm Server error has occurred.
	 */
	@SuppressWarnings({
		"deprecation", "unused"
	})
	private void testChangingInternalAttributes(IDfSession dctmSession) throws DfException {
		String sqlQuery = "update dm_sysobject_s set r_creation_date = to_date('01/05/2010','mm/dd/yyyy') where r_object_id = '0901116f80018873'";
		boolean retVal = dctmSession.apiExec("execsql", sqlQuery);
		// dctmSession.c
		System.out.println(retVal);

	}

}
