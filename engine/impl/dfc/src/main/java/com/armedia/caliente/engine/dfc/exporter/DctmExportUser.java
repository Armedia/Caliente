/*******************************************************************************
 * #%L
 * Armedia Caliente
 * %%
 * Copyright (c) 2010 - 2019 Armedia LLC
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
/**
 *
 */

package com.armedia.caliente.engine.dfc.exporter;

import java.util.Collection;

import com.armedia.caliente.store.CmfObject;
import com.documentum.fc.client.IDfPersistentObject;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.client.IDfUser;
import com.documentum.fc.common.IDfValue;

/**
 *
 *
 */
public class DctmExportUser extends DctmExportDelegate<IDfUser> {

	protected DctmExportUser(DctmExportDelegateFactory factory, IDfSession session, IDfUser user) throws Exception {
		super(factory, session, IDfUser.class, user);
	}

	DctmExportUser(DctmExportDelegateFactory factory, IDfSession session, IDfPersistentObject user) throws Exception {
		this(factory, session, DctmExportDelegate.staticCast(IDfUser.class, user));
	}

	@Override
	protected String calculateLabel(IDfSession session, IDfUser user) throws Exception {
		return user.getUserName();
	}

	@Override
	protected Collection<DctmExportDelegate<?>> findRequirements(IDfSession session, CmfObject<IDfValue> marshaled,
		IDfUser user, DctmExportContext ctx) throws Exception {
		Collection<DctmExportDelegate<?>> ret = super.findRequirements(session, marshaled, user, ctx);
		IDfPersistentObject[] deps = {
			// The user's default group
			session.getGroup(user.getUserGroupName()),

			// The user's home folder
			session.getFolderByPath(user.getDefaultFolder()),

			// The user's default ACL
			session.getACL(user.getACLDomain(), user.getACLName()),

			// If this user represents a group, export that group
			(user.isGroup() ? session.getGroup(user.getUserName()) : null),
		};

		for (IDfPersistentObject dep : deps) {
			if (dep == null) {
				continue;
			}
			DctmExportDelegate<?> delegate = this.factory.newExportDelegate(session, dep);
			if (delegate != null) {
				ret.add(delegate);
			}
		}

		return ret;
	}

	@Override
	protected String calculateName(IDfSession session, IDfUser user) throws Exception {
		return user.getUserLoginName();
	}
}