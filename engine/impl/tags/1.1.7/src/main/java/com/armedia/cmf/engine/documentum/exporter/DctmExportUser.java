/**
 *
 */

package com.armedia.cmf.engine.documentum.exporter;

import java.util.Collection;

import com.armedia.cmf.engine.documentum.DctmObjectType;
import com.armedia.cmf.storage.StoredObject;
import com.documentum.fc.client.IDfPersistentObject;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.client.IDfUser;
import com.documentum.fc.common.DfException;
import com.documentum.fc.common.IDfValue;

/**
 * @author diego
 *
 */
public class DctmExportUser extends DctmExportAbstract<IDfUser> {

	protected DctmExportUser(DctmExportEngine engine) {
		super(engine, DctmObjectType.USER);
	}

	@Override
	protected String calculateLabel(IDfSession session, IDfUser user) throws DfException {
		return user.getUserName();
	}

	@Override
	protected Collection<IDfPersistentObject> findRequirements(IDfSession session, StoredObject<IDfValue> marshaled,
		IDfUser user, DctmExportContext ctx) throws Exception {
		Collection<IDfPersistentObject> ret = super.findRequirements(session, marshaled, user, ctx);
		final IDfPersistentObject[] deps = {
			session.getGroup(user.getUserGroupName()), session.getFolderByPath(user.getDefaultFolder()),
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