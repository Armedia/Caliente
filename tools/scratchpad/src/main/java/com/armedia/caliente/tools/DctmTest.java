package com.armedia.caliente.tools;

import java.io.ByteArrayOutputStream;
import java.util.Date;
import java.util.UUID;

import com.armedia.commons.dfc.pool.DfcSessionPool;
import com.armedia.commons.dfc.util.DfUtils;
import com.documentum.fc.client.IDfFolder;
import com.documentum.fc.client.IDfLocalTransaction;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.client.IDfSysObject;
import com.documentum.fc.common.DfTime;

public class DctmTest {

	public static void test() throws Exception {
		final DfcSessionPool pool = new DfcSessionPool("documentum", "dmadmin2", "ArM3D!A");
		final IDfSession session = pool.acquireSession();
		try {
			final IDfLocalTransaction tx = DfUtils.openTransaction(session);
			boolean ok = false;
			try {
				IDfFolder parent = session.getFolderByPath("/CMSMFTests/Specials");

				IDfSysObject obj = IDfSysObject.class.cast(session.newObject("sysobject_child_test"));
				obj.setObjectName("SysObject Child Test.bin");
				obj.setContentType("binary");
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				baos.write(obj.getObjectName().getBytes());
				obj.setContent(baos);
				for (int i = 0; i < 10; i++) {
					obj.setRepeatingString("property_one", i, UUID.randomUUID().toString());
				}
				obj.setInt("property_two", 10);
				obj.link(parent.getObjectId().getId());
				obj.save();

				obj = IDfSysObject.class.cast(session.newObject("sysobject_grandchild_test"));
				obj.setObjectName("SysObject Grandchild Test.bin");
				obj.setContentType("binary");
				baos = new ByteArrayOutputStream();
				baos.write(obj.getObjectName().getBytes());
				obj.setContent(baos);
				for (int i = 0; i < 10; i++) {
					obj.setRepeatingString("property_one", i, UUID.randomUUID().toString());
				}
				obj.setInt("property_two", 10);
				obj.setTime("property_three", new DfTime(new Date()));
				obj.link(parent.getObjectId().getId());
				obj.save();

				DfUtils.commitTransaction(session, tx);
				ok = true;
			} finally {
				if (!ok) {
					DfUtils.abortTransaction(session, tx);
				}
			}
		} finally {
			pool.releaseSession(session);
		}
	}
}