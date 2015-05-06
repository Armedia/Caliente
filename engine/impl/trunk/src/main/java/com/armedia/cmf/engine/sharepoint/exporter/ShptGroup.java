package com.armedia.cmf.engine.sharepoint.exporter;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import com.armedia.cmf.engine.exporter.ExportException;
import com.armedia.cmf.engine.sharepoint.IncompleteDataException;
import com.armedia.cmf.engine.sharepoint.ShptAttributes;
import com.armedia.cmf.engine.sharepoint.ShptSession;
import com.armedia.cmf.engine.sharepoint.ShptSessionException;
import com.armedia.cmf.storage.StoredAttribute;
import com.armedia.cmf.storage.StoredDataType;
import com.armedia.cmf.storage.StoredObject;
import com.armedia.cmf.storage.StoredValue;
import com.independentsoft.share.Group;
import com.independentsoft.share.PrincipalType;
import com.independentsoft.share.User;

public class ShptGroup extends ShptSecurityObject<Group> {

	public ShptGroup(ShptExportDelegateFactory factory, Group object) throws Exception {
		super(factory, Group.class, object);
	}

	@Override
	protected String calculateLabel(Group object) throws Exception {
		return object.getLoginName();
	}

	@Override
	protected int calculateNumericId(Group object) {
		return object.getId();
	}

	@Override
	protected String calculateBatchId(Group object) throws Exception {
		return calculateObjectId(object);
	}

	@Override
	public String getName() {
		return this.object.getLoginName();
	}

	@Override
	protected void marshal(ShptExportContext ctx, StoredObject<StoredValue> object) throws ExportException {
		super.marshal(ctx, object);
		// UserID
		object.setAttribute(new StoredAttribute<StoredValue>(ShptAttributes.OBJECT_ID.name, StoredDataType.ID, false,
			Collections.singleton(new StoredValue(String.format("USER(%08x)", this.object.getId())))));

		// LoginName
		object.setAttribute(new StoredAttribute<StoredValue>(ShptAttributes.OBJECT_NAME.name, StoredDataType.STRING,
			false, Collections.singleton(new StoredValue(this.object.getLoginName()))));

		// AutoAcceptMembershipRequest
		object.setAttribute(new StoredAttribute<StoredValue>(ShptAttributes.AUTO_ACCEPT_MEMBERSHIP_REQUEST.name,
			StoredDataType.BOOLEAN, false, Collections.singleton(new StoredValue(this.object
				.isAutoAcceptRequestToJoinLeave()))));

		// AllowMembershipRequest
		object.setAttribute(new StoredAttribute<StoredValue>(ShptAttributes.ALLOW_MEMBERSHIP_REQUEST.name,
			StoredDataType.BOOLEAN, false, Collections.singleton(new StoredValue(this.object
				.isRequestToJoinLeaveAllowed()))));

		// AllowMembersEditMembership
		object.setAttribute(new StoredAttribute<StoredValue>(ShptAttributes.ALLOW_MEMBERS_EDIT_MEMBERSHIP.name,
			StoredDataType.BOOLEAN, false, Collections.singleton(new StoredValue(this.object
				.isMembersEditMembershipAllowed()))));

		// PrincipalType
		object.setAttribute(new StoredAttribute<StoredValue>(ShptAttributes.PRINCIPAL_TYPE.name, StoredDataType.STRING,
			false, Collections.singleton(new StoredValue(this.object.getType().name()))));

		// Description
		object.setAttribute(new StoredAttribute<StoredValue>(ShptAttributes.DESCRIPTION.name, StoredDataType.STRING,
			false, Collections.singleton(new StoredValue(this.object.getDescription()))));

		// Email
		object.setAttribute(new StoredAttribute<StoredValue>(ShptAttributes.EMAIL.name, StoredDataType.STRING, false,
			Collections.singleton(new StoredValue(this.object.getRequestToJoinLeaveEmailSetting()))));

		// Title
		object.setAttribute(new StoredAttribute<StoredValue>(ShptAttributes.TITLE.name, StoredDataType.STRING, false,
			Collections.singleton(new StoredValue(this.object.getTitle()))));

		// Owner Title
		object.setAttribute(new StoredAttribute<StoredValue>(ShptAttributes.OWNER_TITLE.name, StoredDataType.STRING,
			false, Collections.singleton(new StoredValue(this.object.getOwnerTitle()))));

		// User Groups
		final List<User> l;
		final ShptSession service = ctx.getSession();
		try {
			l = service.getGroupUsers(this.object.getId());
		} catch (ShptSessionException e) {
			throw new ExportException(String.format("Failed to obtain the group list for user [%s](%d)",
				this.object.getLoginName(), this.object.getId()), e);
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
	protected Collection<ShptObject<?>> findRequirements(ShptSession service, StoredObject<StoredValue> marshaled,
		ShptExportContext ctx) throws Exception {
		Collection<ShptObject<?>> ret = super.findRequirements(service, marshaled, ctx);

		List<User> l = service.getGroupUsers(this.object.getId());
		if ((l != null) && !l.isEmpty()) {
			for (User u : l) {
				if (u.getType() == PrincipalType.USER) {
					try {
						ret.add(new ShptUser(this.factory, u));
					} catch (IncompleteDataException e) {
						this.log.warn(e.getMessage());
					}
				} else {
					try {
						ret.add(new ShptGroup(this.factory, service.getGroup(u.getId())));
					} catch (ShptSessionException e) {
						this.log.warn(String.format("Failed to locate group with ID [%d]", u.getId()));
					}
				}
			}
		}

		ShptSecurityObject<?> owner = null;
		try {
			final User u = service.getGroupOwner(this.object.getId());
			if (u != null) {
				switch (u.getType()) {
					case USER:
						try {
							owner = new ShptUser(this.factory, u);
						} catch (IncompleteDataException e) {
							this.log.warn(e.getMessage());
						}
						break;
					case SHARE_POINT_GROUP:
					case SECURITY_GROUP:
						if (this.object.getId() != u.getId()) {
							try {
								owner = new ShptGroup(this.factory, service.getGroup(u.getId()));
							} catch (ShptSessionException e) {
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
		} catch (ShptSessionException e) {
			this.log.warn(String.format("Failed to find the owner for group [%s] (ID[%d])", getLabel(),
				this.object.getId()));
		}
		if (owner != null) {
			ret.add(owner);
		}

		return ret;
	}
}