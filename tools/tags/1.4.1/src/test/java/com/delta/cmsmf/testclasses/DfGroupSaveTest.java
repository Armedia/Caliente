package com.delta.cmsmf.testclasses;

import com.documentum.fc.client.IDfSession;
import com.documentum.fc.client.IDfSysObject;
import com.documentum.fc.common.DfException;
import com.documentum.fc.common.DfId;
import com.documentum.fc.common.IDfTime;

// TODO: Auto-generated Javadoc
/**
 * The Class DupObjectTest.
 */
public class DfGroupSaveTest {

	/**
	 * @param args
	 * @throws DfException
	 */
	public static void main(String[] args) throws DfException {

		IDfSession dctmSession = (new GetDMCLSession("cobtest", "dmadmin", "dmadmin")).getSession();

		IDfSysObject object = (IDfSysObject) dctmSession.getObject(new DfId("0901116f8001b953"));
		String objectName = object.getObjectName();
		String objectType = object.getTypeName();
		IDfTime creationDate = object.getCreationDate();
		// Date createDate = creationDate.getDate();
		String fldrLoc = "/SKM_TEST/DFC_TEST";
		String dctmDateTimePattern = "mm/dd/yyyy hh:mi:ss";

		// Build a query for ex: " dm_document where object_name='xxx' and folder('/xxx/xxx') and
		// r_creation_date=DATE('xxxxxx')
		StringBuffer objLookUpQry = new StringBuffer(50);
		objLookUpQry.append(objectType);
		objLookUpQry.append(" where object_name='");
		objLookUpQry.append(objectName);
		objLookUpQry.append("' and folder('");
		objLookUpQry.append(fldrLoc);
		objLookUpQry.append("') and r_creation_date=DATE('");
		objLookUpQry.append(creationDate.asString(dctmDateTimePattern));
		objLookUpQry.append("')");

		IDfSysObject objectByQualification = (IDfSysObject) dctmSession.getObjectByQualification(objLookUpQry
			.toString());
		System.out.println(objectByQualification.getObjectId().getId());
		System.out.println("Done!!");

	}

}
