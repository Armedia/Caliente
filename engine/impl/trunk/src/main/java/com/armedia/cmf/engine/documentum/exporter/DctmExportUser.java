/**
 *
 */

package com.armedia.cmf.engine.documentum.exporter;

import java.util.Collection;

import com.armedia.cmf.storage.StoredObject;
import com.documentum.fc.client.IDfPersistentObject;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.client.IDfUser;
import com.documentum.fc.common.IDfValue;

/**
 * @author diego
 *
 */
public class DctmExportUser extends DctmExportAbstract<IDfUser> {

	protected DctmExportUser(DctmExportEngine engine, IDfUser user) throws Exception {
		super(engine, IDfUser.class, user);
	}

	DctmExportUser(DctmExportEngine engine, IDfPersistentObject user) throws Exception {
		this(engine, DctmExportAbstract.staticCast(IDfUser.class, user));
	}

	@Override
	protected String calculateLabel(IDfUser user) throws Exception {
		return user.getUserName();
	}

	@Override
	protected Collection<IDfPersistentObject> findRequirements(IDfSession session, StoredObject<IDfValue> marshaled,
		IDfUser user, DctmExportContext ctx) throws Exception {
		Collection<IDfPersistentObject> ret = super.findRequirements(session, marshaled, user, ctx);
		final IDfPersistentObject[] deps = {
			// The user's default group
			session.getGroup(user.getUserGroupName()),

			// The user's home folder
			session.getFolderByPath(user.getDefaultFolder()),

			// The user's default ACL
			session.getACL(user.getACLDomain(), user.getACLName())
		};
		for (IDfPersistentObject dep : deps) {
			if (dep == null) {
				continue;
			}
			ret.add(dep);
		}
		return ret;
	}
}