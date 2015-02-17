package com.armedia.cmf.engine.sharepoint.types;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import com.armedia.cmf.engine.exporter.ExportException;
import com.armedia.cmf.engine.sharepoint.ShptAttributes;
import com.armedia.cmf.engine.sharepoint.exporter.ShptExportContext;
import com.armedia.cmf.storage.StoredAttribute;
import com.armedia.cmf.storage.StoredDataType;
import com.armedia.cmf.storage.StoredObject;
import com.armedia.cmf.storage.StoredObjectType;
import com.armedia.cmf.storage.StoredValue;
import com.armedia.commons.utilities.Tools;
import com.independentsoft.share.Group;
import com.independentsoft.share.PrincipalType;
import com.independentsoft.share.Service;
import com.independentsoft.share.ServiceException;
import com.independentsoft.share.User;

public class ShptGroup extends ShptSecurityObject<Group> {

	private final ShptSecurityObject<?> owner;
	private final List<ShptUser> members;

	public ShptGroup(Service service, Group group) throws ServiceException {
		super(service, group, StoredObjectType.GROUP);
		User u = this.service.getGroupOwner(this.wrapped.getId());
		if (u != null) {
			switch (u.getType()) {
				case USER:
					this.owner = new ShptUser(service, u);
					break;
				case SHARE_POINT_GROUP:
				case SECURITY_GROUP:
					if (group.getId() != u.getId()) {
						this.owner = new ShptGroup(service, service.getGroup(u.getId()));
					} else {
						this.owner = null;
					}
					break;
				default:
					this.owner = null;
			}
		} else {
			this.owner = null;
		}

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

	@Override
	public String getBatchId() {
		return getId();
	}

	@Override
	public String getLabel() {
		return getName();
	}

	public List<ShptUser> getMembers() {
		return this.members;
	}

	@Override
	public int getNumericId() {
		return this.wrapped.getId();
	}

	public ShptSecurityObject<?> getOwner() {
		return this.owner;
	}

	@Override
	public String getName() {
		return this.wrapped.getLoginName();
	}

	@Override
	protected void marshal(StoredObject<StoredValue> object) throws ExportException {
		// UserID
		object.setAttribute(new StoredAttribute<StoredValue>(ShptAttributes.OBJECT_ID.name, StoredDataType.ID, false,
			Collections.singleton(new StoredValue(String.format("USER(%08x)", this.wrapped.getId())))));

		// LoginName
		object.setAttribute(new StoredAttribute<StoredValue>(ShptAttributes.OBJECT_NAME.name, StoredDataType.STRING,
			false, Collections.singleton(new StoredValue(this.wrapped.getLoginName()))));

		// AutoAcceptMembershipRequest
		object.setAttribute(new StoredAttribute<StoredValue>(ShptAttributes.AUTO_ACCEPT_MEMBERSHIP_REQUEST.name,
			StoredDataType.BOOLEAN, false, Collections.singleton(new StoredValue(this.wrapped
				.isAutoAcceptRequestToJoinLeave()))));

		// AllowMembershipRequest
		object.setAttribute(new StoredAttribute<StoredValue>(ShptAttributes.ALLOW_MEMBERSHIP_REQUEST.name,
			StoredDataType.BOOLEAN, false, Collections.singleton(new StoredValue(this.wrapped
				.isRequestToJoinLeaveAllowed()))));

		// AllowMembersEditMembership
		object.setAttribute(new StoredAttribute<StoredValue>(ShptAttributes.ALLOW_MEMBERS_EDIT_MEMBERSHIP.name,
			StoredDataType.BOOLEAN, false, Collections.singleton(new StoredValue(this.wrapped
				.isMembersEditMembershipAllowed()))));

		// PrincipalType
		object.setAttribute(new StoredAttribute<StoredValue>(ShptAttributes.PRINCIPAL_TYPE.name, StoredDataType.STRING,
			false, Collections.singleton(new StoredValue(this.wrapped.getType().name()))));

		// Description
		object.setAttribute(new StoredAttribute<StoredValue>(ShptAttributes.DESCRIPTION.name, StoredDataType.STRING,
			false, Collections.singleton(new StoredValue(this.wrapped.getDescription()))));

		// Email
		object.setAttribute(new StoredAttribute<StoredValue>(ShptAttributes.EMAIL.name, StoredDataType.STRING, false,
			Collections.singleton(new StoredValue(this.wrapped.getRequestToJoinLeaveEmailSetting()))));

		// Title
		object.setAttribute(new StoredAttribute<StoredValue>(ShptAttributes.TITLE.name, StoredDataType.STRING, false,
			Collections.singleton(new StoredValue(this.wrapped.getTitle()))));

		// Owner Title
		object.setAttribute(new StoredAttribute<StoredValue>(ShptAttributes.OWNER_TITLE.name, StoredDataType.STRING,
			false, Collections.singleton(new StoredValue(this.wrapped.getOwnerTitle()))));

		// User Groups
		final List<User> l;
		try {
			l = this.service.getGroupUsers(getNumericId());
		} catch (ServiceException e) {
			throw new ExportException(String.format("Failed to obtain the group list for user [%s](%d)",
				this.wrapped.getLoginName(), this.wrapped.getId()), e);
		}
		StoredAttribute<StoredValue> users = new StoredAttribute<StoredValue>(ShptAttributes.GROUP_MEMBERS.name,
			StoredDataType.STRING, true);
		object.setAttribute(users);
		if ((l != null) && !l.isEmpty()) {
			for (User u : l) {
				users.addValue(new StoredValue(u.getLoginName()));
			}
		}
	}

	@Override
	protected Collection<ShptObject<?>> findRequirements(Service service, StoredObject<StoredValue> marshaled,
		ShptExportContext ctx) throws Exception {
		Collection<ShptObject<?>> ret = super.findRequirements(service, marshaled, ctx);
		// Add the group's users
		if (this.owner != null) {
			ret.add(this.owner);
		}

		List<User> l = service.getGroupUsers(getNumericId());
		if ((l != null) && !l.isEmpty()) {
			for (User u : l) {
				if (u.getType() == PrincipalType.USER) {
					ret.add(new ShptUser(service, u));
				} else {
					ret.add(new ShptGroup(service, service.getGroup(u.getId())));
				}
			}
		}
		return ret;
	}
}