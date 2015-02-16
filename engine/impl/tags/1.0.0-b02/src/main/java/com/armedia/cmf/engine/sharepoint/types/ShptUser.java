package com.armedia.cmf.engine.sharepoint.types;

import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import com.armedia.cmf.engine.exporter.ExportException;
import com.armedia.cmf.engine.sharepoint.ShptAttributes;
import com.armedia.cmf.engine.sharepoint.exporter.ShptExportContext;
import com.armedia.cmf.storage.StoredAttribute;
import com.armedia.cmf.storage.StoredDataType;
import com.armedia.cmf.storage.StoredObject;
import com.armedia.cmf.storage.StoredObjectType;
import com.armedia.cmf.storage.StoredValue;
import com.independentsoft.share.Group;
import com.independentsoft.share.Role;
import com.independentsoft.share.Service;
import com.independentsoft.share.ServiceException;
import com.independentsoft.share.User;

public class ShptUser extends ShptSecurityObject<User> {

	private final List<Role> roles;
	private final String userName;
	private final String userDomain;

	public ShptUser(Service service, User user) throws ServiceException {
		super(service, user, StoredObjectType.USER);
		/*
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
		 */
		this.roles = Collections.emptyList();
		String loginName = this.wrapped.getLoginName();
		final int backslash = loginName.indexOf('\\');
		final int atSign = loginName.indexOf('@');
		if (backslash >= 0) {
			// 1) ^.*|domain\\user$
			this.userName = loginName.substring(backslash + 1);
			this.userDomain = loginName.substring(loginName.indexOf('|') + 1, backslash);
		} else if (atSign >= 0) {
			// 2) ^.*|user@domain$
			this.userName = loginName.substring(loginName.indexOf('|') + 1, atSign);
			this.userDomain = loginName.substring(atSign + 1);
		} else {
			this.userName = loginName;
			this.userDomain = "";
		}
	}

	@Override
	public String getBatchId() {
		return getId();
	}

	@Override
	public String getLabel() {
		return getName();
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
		return this.userName;
	}

	public String getDomain() {
		return this.userDomain;
	}

	@Override
	public void marshal(StoredObject<StoredValue> object) throws ExportException {
		// UserID
		object.setAttribute(new StoredAttribute<StoredValue>(ShptAttributes.OBJECT_ID.name, StoredDataType.ID, false,
			Collections.singleton(new StoredValue(String.format("USER(%08x)", this.wrapped.getId())))));

		// LoginName
		object.setAttribute(new StoredAttribute<StoredValue>(ShptAttributes.OBJECT_NAME.name, StoredDataType.STRING,
			false, Collections.singleton(new StoredValue(this.userName))));
		object.setAttribute(new StoredAttribute<StoredValue>(ShptAttributes.LOGIN_NAME.name, StoredDataType.STRING,
			false, Collections.singleton(new StoredValue(this.userName))));
		if (this.userDomain != null) {
			object.setAttribute(new StoredAttribute<StoredValue>(ShptAttributes.LOGIN_DOMAIN.name,
				StoredDataType.STRING, false, Collections.singleton(new StoredValue(this.userDomain))));
		}

		// SiteAdmin
		object.setAttribute(new StoredAttribute<StoredValue>(ShptAttributes.SITE_ADMIN.name, StoredDataType.BOOLEAN,
			false, Collections.singleton(new StoredValue(this.wrapped.isSiteAdmin()))));

		// PrincipalType
		object.setAttribute(new StoredAttribute<StoredValue>(ShptAttributes.PRINCIPAL_TYPE.name, StoredDataType.STRING,
			false, Collections.singleton(new StoredValue(this.wrapped.getType().name()))));

		// UserIdName
		object.setAttribute(new StoredAttribute<StoredValue>(ShptAttributes.PRINCIPAL_ID.name, StoredDataType.STRING,
			false, Collections.singleton(new StoredValue(this.wrapped.getUserId().getNameId()))));

		// UserIdIssuer
		object.setAttribute(new StoredAttribute<StoredValue>(ShptAttributes.PRINCIPAL_ID_ISSUER.name,
			StoredDataType.STRING, false, Collections.singleton(new StoredValue(this.wrapped.getUserId()
				.getNameIdIssuer()))));

		object.setAttribute(new StoredAttribute<StoredValue>(ShptAttributes.MODIFICATION_DATE.name,
			StoredDataType.TIME, false, Collections.singleton(new StoredValue(new Date()))));

		// Email
		object.setAttribute(new StoredAttribute<StoredValue>(ShptAttributes.EMAIL.name, StoredDataType.STRING, false,
			Collections.singleton(new StoredValue(this.wrapped.getEmail()))));

		// Title
		object.setAttribute(new StoredAttribute<StoredValue>(ShptAttributes.TITLE.name, StoredDataType.STRING, false,
			Collections.singleton(new StoredValue(this.wrapped.getTitle()))));

		// User Groups
		final List<Group> l;
		try {
			l = this.service.getUserGroups(getNumericId());
		} catch (ServiceException e) {
			throw new ExportException(String.format("Failed to obtain the group list for user [%s](%d)",
				this.wrapped.getLoginName(), this.wrapped.getId()), e);
		}
		StoredAttribute<StoredValue> groups = new StoredAttribute<StoredValue>(ShptAttributes.USER_GROUPS.name,
			StoredDataType.STRING, true);
		object.setAttribute(groups);
		if ((l != null) && !l.isEmpty()) {
			for (Group g : l) {
				groups.addValue(new StoredValue(g.getLoginName()));
			}
		}

		StoredAttribute<StoredValue> roles = new StoredAttribute<StoredValue>(ShptAttributes.USER_ROLES.name,
			StoredDataType.STRING, true);
		object.setAttribute(groups);
		for (Role r : this.roles) {
			roles.addValue(new StoredValue(r.getName()));
		}
	}

	@Override
	protected Collection<ShptObject<?>> findRequirements(Service service, StoredObject<StoredValue> marshaled,
		ShptExportContext ctx) throws Exception {
		Collection<ShptObject<?>> ret = super.findRequirements(service, marshaled, ctx);
		List<Group> l = service.getUserGroups(getNumericId());
		if ((l != null) && !l.isEmpty()) {
			for (Group g : l) {
				ret.add(new ShptGroup(service, g));
			}
		}

		/*
		for (Role r : this.roles) {
			ret.add(new ShptRole(service, r));
		}
		 */
		return ret;
	}
}