package com.armedia.cmf.engine.sharepoint;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import com.armedia.cmf.storage.StoredObjectType;
import com.armedia.commons.utilities.Tools;
import com.independentsoft.share.Group;
import com.independentsoft.share.Role;
import com.independentsoft.share.Service;
import com.independentsoft.share.ServiceException;
import com.independentsoft.share.User;

public class ShptUser extends ShptSecurityObject<User> {

	public ShptUser(Service service, User user) {
		super(service, user, StoredObjectType.USER);
	}

	public Collection<Group> getGroups() throws ServiceException {
		return Tools.freezeList(this.service.getUserGroups(this.wrapped.getId()), true);
	}

	public Collection<Role> getRoles() throws ServiceException {
		List<Integer> roles = this.service.getRoleAssignments(this.wrapped.getId());
		if ((roles == null) || roles.isEmpty()) { return Collections.emptyList(); }
		List<Role> l = new ArrayList<Role>(roles.size());
		for (Integer i : roles) {
			try {
				Role r = this.service.getRole(i);
				if (r != null) {
					l.add(r);
				}
			} catch (ServiceException e) {
				String msg = String.format("Failed to retrieve role with ID [%d] (referenced from user %s [%d])", i,
					this.wrapped.getLoginName(), this.wrapped.getId());
				if (this.log.isDebugEnabled()) {
					this.log.warn(msg, e);
				} else {
					this.log.warn(msg);
				}
			}
		}
		return Tools.freezeList(l);
	}

	@Override
	public int getNumericId() {
		return this.wrapped.getId();
	}

	@Override
	public String getName() {
		return this.wrapped.getLoginName();
	}
}