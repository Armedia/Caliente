package com.armedia.cmf.engine.sharepoint;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import com.armedia.cmf.storage.StoredObjectType;
import com.armedia.commons.utilities.Tools;
import com.independentsoft.share.Role;
import com.independentsoft.share.Service;
import com.independentsoft.share.ServiceException;
import com.independentsoft.share.User;

public class ShptUser extends ShptSecurityObject<User> {

	private final List<Role> roles;

	public ShptUser(Service service, User user) throws ServiceException {
		super(service, user, StoredObjectType.USER);
		List<Integer> roles = this.service.getRoleAssignments(this.wrapped.getId());
		if ((roles == null) || roles.isEmpty()) {
			this.roles = Collections.emptyList();
		} else {
			List<Role> l = new ArrayList<Role>(roles.size());
			for (Integer i : roles) {
				Role r = this.service.getRole(i);
				if (r != null) {
					l.add(r);
				}
			}
			this.roles = Tools.freezeList(l);
		}
	}

	public Collection<Role> getRoles() {
		return this.roles;
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