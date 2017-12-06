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
 * @author diego
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
			DctmExportDelegate<?> delegate = this.factory.newExportDelegate(dep);
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