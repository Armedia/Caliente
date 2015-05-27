/**
 *
 */

package com.armedia.cmf.engine.documentum.exporter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.armedia.cmf.engine.exporter.ExportException;
import com.armedia.cmf.storage.CmfACL;
import com.armedia.cmf.storage.CmfObject;
import com.documentum.fc.client.IDfPersistentObject;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.client.IDfUser;
import com.documentum.fc.common.DfException;
import com.documentum.fc.common.IDfValue;

/**
 * @author diego
 *
 */
public class DctmExportUser extends DctmExportDelegate<IDfUser> {

	protected DctmExportUser(DctmExportDelegateFactory factory, IDfUser user) throws Exception {
		super(factory, IDfUser.class, user);
	}

	DctmExportUser(DctmExportDelegateFactory factory, IDfPersistentObject user) throws Exception {
		this(factory, DctmExportDelegate.staticCast(IDfUser.class, user));
	}

	@Override
	protected String calculateLabel(IDfUser user) throws Exception {
		return user.getUserName();
	}

	@Override
	protected Collection<DctmExportDelegate<?>> findRequirements(IDfSession session, CmfObject<IDfValue> marshaled,
		IDfUser user, DctmExportContext ctx) throws Exception {
		Collection<DctmExportDelegate<?>> ret = super.findRequirements(session, marshaled, user, ctx);
		List<IDfPersistentObject> deps = new ArrayList<IDfPersistentObject>();
		// The user's default group
		deps.add(session.getGroup(user.getUserGroupName()));
		// The user's home folder
		deps.add(session.getFolderByPath(user.getDefaultFolder()));

		// The user's default ACL
		deps.addAll(DctmACLTools.gatherRequirements(ctx, marshaled.getAcl()));

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
	protected CmfACL<IDfValue> calculateACL(final IDfUser user) throws DfException, ExportException {
		return DctmACLTools.calculateACL(user.getSession().getACL(user.getACLDomain(), user.getACLName()));
	}
}