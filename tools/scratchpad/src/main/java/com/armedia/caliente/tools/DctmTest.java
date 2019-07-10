/*******************************************************************************
 * #%L
 * Armedia Caliente
 * %%
 * Copyright (C) 2013 - 2019 Armedia, LLC
 * %%
 * This file is part of the Caliente software.
 *
 * If the software was purchased under a paid Caliente license, the terms of
 * the paid license agreement will prevail.  Otherwise, the software is
 * provided under the following open source license terms:
 *
 * Caliente is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Caliente is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Caliente. If not, see <http://www.gnu.org/licenses/>.
 * #L%
 *******************************************************************************/
package com.armedia.caliente.tools;

import java.io.ByteArrayOutputStream;

import com.armedia.caliente.tools.dfc.DfcUtils;
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
			final IDfLocalTransaction tx = DfcUtils.openTransaction(session);
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

				DfcUtils.commitTransaction(session, tx);
				ok = true;
			} finally {
				if (!ok) {
					DfcUtils.abortTransaction(session, tx);
				}
			}
		} finally {
			pool.releaseSession(session);
		}
	}
}