package com.armedia.cmf.engine.sharepoint;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import com.armedia.cmf.engine.exporter.ExportException;
import com.armedia.cmf.engine.sharepoint.exporter.ShptExportContext;
import com.armedia.cmf.storage.StoredObject;
import com.armedia.cmf.storage.StoredObjectType;
import com.armedia.commons.utilities.Tools;
import com.independentsoft.share.Group;
import com.independentsoft.share.Service;
import com.independentsoft.share.ServiceException;
import com.independentsoft.share.User;

public class ShptGroup extends ShptSecurityObject<Group> {

	private final ShptUser owner;
	private final List<ShptUser> members;

	public ShptGroup(Service service, Group group) throws ServiceException {
		super(service, group, StoredObjectType.GROUP);
		User u = this.service.getGroupOwner(this.wrapped.getId());
		this.owner = (u != null ? new ShptUser(service, u) : null);
		List<User> l = service.getGroupUsers(group.getId());
		if ((l == null) || l.isEmpty()) {
			this.members = Collections.emptyList();
		} else {
			List<ShptUser> users = new ArrayList<ShptUser>(l.size());
			for (User user : l) {
				users.add(new ShptUser(service, user));
			}
			this.members = Tools.freezeList(users);
		}
	}

	public List<ShptUser> getMembers() {
		return this.members;
	}

	@Override
	public int getNumericId() {
		return this.wrapped.getId();
	}

	public ShptUser getOwner() {
		return this.owner;
	}

	@Override
	public String getName() {
		return this.wrapped.getLoginName();
	}

	@Override
	public StoredObject<Object> marshal() throws ExportException {
		StoredObject<Object> ret = new StoredObject<Object>(StoredObjectType.GROUP, getId(), getId(),
			this.wrapped.getLoginName(), null);
		// TODO: Start adding attributes and properties
		return ret;
	}

	@Override
	protected Collection<ShptObject<?>> findDependents(Service service, StoredObject<Object> marshaled,
		ShptExportContext ctx) throws Exception {
		Collection<ShptObject<?>> ret = super.findDependents(service, marshaled, ctx);
		return ret;
	}

	@Override
	protected Collection<ShptObject<?>> findRequirements(Service service, StoredObject<Object> marshaled,
		ShptExportContext ctx) throws Exception {
		Collection<ShptObject<?>> ret = super.findRequirements(service, marshaled, ctx);
		// Add the group's users
		User owner = service.getGroupOwner(getNumericId());
		if (owner != null) {
			ret.add(new ShptUser(service, owner));
		}
		List<User> l = service.getGroupUsers(getNumericId());
		if ((l != null) && !l.isEmpty()) {
			for (User u : l) {
				ret.add(new ShptUser(service, u));
			}
		}
		return ret;
	}
}