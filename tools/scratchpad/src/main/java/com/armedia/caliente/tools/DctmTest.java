package com.armedia.caliente.tools;

import java.io.ByteArrayOutputStream;

import com.armedia.caliente.tools.dfc.DfUtils;
import com.armedia.caliente.tools.dfc.pool.DfcSessionPool;
import com.documentum.fc.client.IDfFolder;
import com.documentum.fc.client.IDfLocalTransaction;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.client.IDfSysObject;

public class DctmTest {

	public static void test() throws Exception {
		final DfcSessionPool pool = new DfcSessionPool("documentum", "dmadmin2", "ArM3D!A");
		final IDfSession session = pool.acquireSession();
		try {
			final IDfLocalTransaction tx = DfUtils.openTransaction(session);
			boolean ok = false;
			try {
				IDfFolder parent = session.getFolderByPath("/CMSMFTests/Specials");

				IDfSysObject obj = IDfSysObject.class.cast(session.newObject("dm_document"));
				obj.setObjectName("Weird Characters in Title.bin");
				obj.setContentType("binary");
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				baos.write(obj.getObjectName().getBytes());
				obj.setContent(baos);

				StringBuilder buf = new StringBuilder();
				for (int i = 1; i < 255; i++) {
					buf.append((char) i);
				}
				String weirdTitle = buf.toString();

				obj.setTitle(weirdTitle);
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