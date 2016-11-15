package com.armedia.caliente.tools;

import java.io.ByteArrayOutputStream;
import java.util.Date;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.armedia.commons.dfc.pool.DfcSessionPool;
import com.armedia.commons.dfc.util.DfUtils;
import com.documentum.fc.client.IDfFolder;
import com.documentum.fc.client.IDfLocalTransaction;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.client.IDfSysObject;
import com.documentum.fc.common.DfTime;

/**
 * This class is used as a testbed to run quick'n'dirty DFC test programs
 *
 * @author diego.rivera@armedia.com
 *
 */
public class Scratchpad {

	// private final Logger log = LoggerFactory.getLogger(getClass());

	public static void main(String... args) throws Exception {

		Pattern FILE_COMMENT = Pattern.compile("(?<!\\\\)#");

		String[] S = {
			"asdfasdfasdf # asdfasdf", "asdfasdfasdf#asdfasdfasdf", "012345678\\#abcdef#"
		};

		for (String s : S) {
			Matcher m = FILE_COMMENT.matcher(s);
			if (m.find()) {
				int p = m.start();
				System.out.printf("%d", p);
				continue;
			}
			throw new RuntimeException();
		}

		final DfcSessionPool pool;

		pool = new DfcSessionPool("documentum", "dmadmin2", "ArM3D!A");
		// pool = new DfcSessionPool("dctmvm01", "dctmadmin", "123");
		// pool = new DfcSessionPool("armrdreponew", "dmadmin", "ArM3D!A");

		try {
			new Scratchpad(pool).run(args);
		} finally {
			pool.close();
		}
	}

	private final DfcSessionPool pool;

	Scratchpad(DfcSessionPool pool) {
		this.pool = pool;
	}

	public void run(String... args) throws Exception {

		final IDfSession session = this.pool.acquireSession();
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
			this.pool.releaseSession(session);
		}
	}
}