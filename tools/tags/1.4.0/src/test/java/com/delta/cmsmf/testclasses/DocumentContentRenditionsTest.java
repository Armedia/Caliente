package com.delta.cmsmf.testclasses;

import com.documentum.com.DfClientX;
import com.documentum.fc.client.IDfCollection;
import com.documentum.fc.client.IDfContentCollection;
import com.documentum.fc.client.IDfQuery;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.client.IDfSysObject;
import com.documentum.fc.common.DfException;
import com.documentum.fc.common.DfId;

public class DocumentContentRenditionsTest {

	/**
	 * @param args
	 * @throws DfException
	 */
	public static void main(String[] args) throws DfException {
		// TODO Auto-generated method stub

		IDfSession dctmSession = (new GetDMCLSession("cobtest", "dmadmin", "dmadmin")).getSession();

		IDfSysObject obj = (IDfSysObject) dctmSession.getObject(new DfId("0901116f80021883"));

		// obj.setFileEx("c:\\shridev.pdf", "pdf", 0, null);
		// obj.addRendition("c:\\hostname.out", "crtext");
		// obj.addRendition("c:\\temp1.xls", "excel8book");
		// obj.setFileEx("c:\\skm1.pdf", null, 1, null);
		// obj.addRenditionEx("c:\\temp3.xls", "excel8book", 1, null, false);
		// obj.addRenditionEx("c:\\skmtest.doc", "msw8", 1, null, false);
		obj.addRenditionEx2("C:\\temp1.xls", "pdf", 0, "pm_test", null, false, false, false);
		obj.save();

		int pageCnt = obj.getPageCount();

		for (int i = 0; i < pageCnt; i++) {
			StringBuffer contentDQLBuffer = new StringBuffer(
				"select dcs.r_object_id, dcr.parent_id, dcs.full_format, dcr.page, dcr.page_modifier, dcs.rendition, ");
			contentDQLBuffer.append("dcs.content_size, dcs.set_file, dcs.set_time ");
			contentDQLBuffer.append("from dmr_content_r  dcr, dmr_content_s dcs ");
			contentDQLBuffer.append("where dcr.parent_id = '0901116f80021883' ");
			contentDQLBuffer.append("and dcr.r_object_id = dcs.r_object_id ");
			contentDQLBuffer.append("and page = ");
			contentDQLBuffer.append(i);
			contentDQLBuffer.append(" order by rendition");

			System.out.println(contentDQLBuffer.toString());
			IDfQuery contentQuery = new DfClientX().getQuery();
			contentQuery.setDQL(contentDQLBuffer.toString());
			IDfCollection contentColl = contentQuery.execute(dctmSession, IDfQuery.READ_QUERY);
			while (contentColl.next()) {
				System.out.println(contentColl.getId("r_object_id"));
				System.out.println(contentColl.getString("full_format"));
				System.out.println(contentColl.getInt("page"));
				System.out.println(contentColl.getString("page_modifier"));
			}

			contentColl.close();
		}

		IDfCollection renditions = obj.getRenditions("");
		while (renditions.next()) {
			System.out.println(renditions.getString("full_format"));
		}
		renditions.close();

		IDfContentCollection collectionForContent = (IDfContentCollection) obj
			.getCollectionForContentEx2(null, 1, null);
		while (collectionForContent.next()) {
// System.out.println(collectionForContent.getContentSize());
		}

		System.out.println("DONE!");

	}
}
