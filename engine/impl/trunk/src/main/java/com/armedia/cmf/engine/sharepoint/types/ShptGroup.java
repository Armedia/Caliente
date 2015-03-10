package com.armedia.cmf.engine.sharepoint.types;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import com.armedia.cmf.engine.exporter.ExportException;
import com.armedia.cmf.engine.sharepoint.IncompleteDataException;
import com.armedia.cmf.engine.sharepoint.ShptAttributes;
import com.armedia.cmf.engine.sharepoint.exporter.ShptExportContext;
import com.armedia.cmf.storage.StoredAttribute;
import com.armedia.cmf.storage.StoredDataType;
import com.armedia.cmf.storage.StoredObject;
import com.armedia.cmf.storage.StoredObjectType;
import com.armedia.cmf.storage.StoredValue;
import com.independentsoft.share.Group;
import com.independentsoft.share.PrincipalType;
import com.independentsoft.share.Service;
import com.independentsoft.share.ServiceException;
import com.independentsoft.share.User;

public class ShptGroup extends ShptSecurityObject<Group> {

	public ShptGroup(Service service, Group group) {
		super(service, group, StoredObjectType.GROUP);
	}

	@Override
	public String getBatchId() {
		return getId();
	}

	@Override
	public String getLabel() {
		return getName();
	}

	@Override
	public int getNumericId() {
		return this.wrapped.getId();
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

		List<User> l = service.getGroupUsers(getNumericId());
		if ((l != null) && !l.isEmpty()) {
			for (User u : l) {
				if (u.getType() == PrincipalType.USER) {
					try {
						ret.add(new ShptUser(service, u));
					} catch (IncompleteDataException e) {
						this.log.warn(e.getMessage());
					}
				} else {
					try {
						ret.add(new ShptGroup(service, service.getGroup(u.getId())));
					} catch (ServiceException e) {
						this.log.warn(String.format("Failed to locate group with ID [%d]", u.getId()));
					}
				}
			}
		}

		ShptSecurityObject<?> owner = null;
		try {
			final User u = this.service.getGroupOwner(this.wrapped.getId());
			if (u != null) {
				switch (u.getType()) {
					case USER:
						try {
							owner = new ShptUser(service, u);
						} catch (IncompleteDataException e) {
							this.log.warn(e.getMessage());
						}
						break;
					case SHARE_POINT_GROUP:
					case SECURITY_GROUP:
						if (getWrapped().getId() != u.getId()) {
							try {
								owner = new ShptGroup(service, service.getGroup(u.getId()));
							} catch (ServiceException e) {
								// Did not find an owner group
								if (this.log.isDebugEnabled()) {
									this.log.warn(String.format("Failed to find the group with ID [%d]", u.getId()), e);
								} else {
									this.log.warn(String.format("Failed to find the group with ID [%d]", u.getId()));
								}
							}
						}
						break;
					default:
						break;
				}
			}
		} catch (ServiceException e) {
			this.log.warn(String.format("Failed to find the owner for group [%s] (ID[%d])", getLabel(),
				this.wrapped.getId()));
		}
		if (owner != null) {
			ret.add(owner);
		}

		return ret;
	}
}