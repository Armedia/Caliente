package com.armedia.cmf.engine.sharepoint;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
}