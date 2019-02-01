package com.armedia.caliente.engine.sharepoint.exporter;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import com.armedia.caliente.engine.exporter.ExportException;
import com.armedia.caliente.engine.sharepoint.IncompleteDataException;
import com.armedia.caliente.engine.sharepoint.ShptAttributes;
import com.armedia.caliente.engine.sharepoint.ShptSession;
import com.armedia.caliente.engine.sharepoint.ShptSessionException;
import com.armedia.caliente.store.CmfAttribute;
import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.store.CmfValue;
import com.independentsoft.share.Group;
import com.independentsoft.share.PrincipalType;
import com.independentsoft.share.User;

public class ShptGroup extends ShptSecurityObject<Group> {

	public ShptGroup(ShptExportDelegateFactory factory, ShptSession session, Group object) throws Exception {
		super(factory, session, Group.class, object);
	}

	@Override
	protected String calculateLabel(ShptSession session, Group object) throws Exception {
		return object.getLoginName();
	}

	@Override
	protected int calculateNumericId(ShptSession session, Group object) {
		return object.getId();
	}

	@Override
	protected String calculateHistoryId(ShptSession session, Group object) throws Exception {
		return calculateObjectId(session, object);
	}

	@Override
	protected boolean marshal(ShptExportContext ctx, CmfObject<CmfValue> object) throws ExportException {
		if (!super.marshal(ctx, object)) { return false; }
		// UserID
		object.setAttribute(new CmfAttribute<>(ShptAttributes.OBJECT_ID.name, CmfValue.Type.ID, false,
			Collections.singleton(new CmfValue(String.format("USER(%08x)", this.object.getId())))));

		// LoginName
		object.setAttribute(new CmfAttribute<>(ShptAttributes.OBJECT_NAME.name, CmfValue.Type.STRING, false,
			Collections.singleton(new CmfValue(this.object.getLoginName()))));

		// AutoAcceptMembershipRequest
		object
			.setAttribute(new CmfAttribute<>(ShptAttributes.AUTO_ACCEPT_MEMBERSHIP_REQUEST.name, CmfValue.Type.BOOLEAN,
				false, Collections.singleton(new CmfValue(this.object.isAutoAcceptRequestToJoinLeave()))));

		// AllowMembershipRequest
		object.setAttribute(new CmfAttribute<>(ShptAttributes.ALLOW_MEMBERSHIP_REQUEST.name, CmfValue.Type.BOOLEAN,
			false, Collections.singleton(new CmfValue(this.object.isRequestToJoinLeaveAllowed()))));

		// AllowMembersEditMembership
		object.setAttribute(new CmfAttribute<>(ShptAttributes.ALLOW_MEMBERS_EDIT_MEMBERSHIP.name, CmfValue.Type.BOOLEAN,
			false, Collections.singleton(new CmfValue(this.object.isMembersEditMembershipAllowed()))));

		// PrincipalType
		object.setAttribute(new CmfAttribute<>(ShptAttributes.PRINCIPAL_TYPE.name, CmfValue.Type.STRING, false,
			Collections.singleton(new CmfValue(this.object.getType().name()))));

		// Description
		object.setAttribute(new CmfAttribute<>(ShptAttributes.DESCRIPTION.name, CmfValue.Type.STRING, false,
			Collections.singleton(new CmfValue(this.object.getDescription()))));

		// Email
		object.setAttribute(new CmfAttribute<>(ShptAttributes.EMAIL.name, CmfValue.Type.STRING, false,
			Collections.singleton(new CmfValue(this.object.getRequestToJoinLeaveEmailSetting()))));

		// Title
		object.setAttribute(new CmfAttribute<>(ShptAttributes.TITLE.name, CmfValue.Type.STRING, false,
			Collections.singleton(new CmfValue(this.object.getTitle()))));

		// Owner Title
		object.setAttribute(new CmfAttribute<>(ShptAttributes.OWNER_TITLE.name, CmfValue.Type.STRING, false,
			Collections.singleton(new CmfValue(this.object.getOwnerTitle()))));

		// User Groups
		final List<User> l;
		final ShptSession service = ctx.getSession();
		try {
			l = service.getGroupUsers(this.object.getId());
		} catch (ShptSessionException e) {
			throw new ExportException(String.format("Failed to obtain the group list for user [%s](%d)",
				this.object.getLoginName(), this.object.getId()), e);
		}
		CmfAttribute<CmfValue> users = new CmfAttribute<>(ShptAttributes.GROUP_MEMBERS.name, CmfValue.Type.STRING,
			true);
		object.setAttribute(users);
		if ((l != null) && !l.isEmpty()) {
			for (User u : l) {
				users.addValue(new CmfValue(u.getLoginName()));
			}
		}
		return true;
	}

	@Override
	protected Collection<ShptObject<?>> findRequirements(ShptSession service, CmfObject<CmfValue> marshaled,
		ShptExportContext ctx) throws Exception {
		Collection<ShptObject<?>> ret = super.findRequirements(service, marshaled, ctx);

		List<User> l = service.getGroupUsers(this.object.getId());
		if ((l != null) && !l.isEmpty()) {
			for (User u : l) {
				if (u.getType() == PrincipalType.USER) {
					try {
						ret.add(new ShptUser(this.factory, service, u));
					} catch (IncompleteDataException e) {
						this.log.warn(e.getMessage());
					}
				} else {
					try {
						ret.add(new ShptGroup(this.factory, service, service.getGroup(u.getId())));
					} catch (ShptSessionException e) {
						this.log.warn("Failed to locate group with ID [{}]", u.getId());
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
							owner = new ShptUser(this.factory, service, u);
						} catch (IncompleteDataException e) {
							this.log.warn(e.getMessage());
						}
						break;
					case SHARE_POINT_GROUP:
					case SECURITY_GROUP:
						if (this.object.getId() != u.getId()) {
							try {
								owner = new ShptGroup(this.factory, service, service.getGroup(u.getId()));
							} catch (ShptSessionException e) {
								// Did not find an owner group
								if (this.log.isDebugEnabled()) {
									this.log.warn("Failed to find the group with ID [{}]", u.getId(), e);
								} else {
									this.log.warn("Failed to find the group with ID [{}]", u.getId());
								}
							}
						}
						break;
					default:
						break;
				}
			}
		} catch (ShptSessionException e) {
			this.log.warn("Failed to find the owner for group [{}] (ID[{}])", getLabel(), this.object.getId());
		}
		if (owner != null) {
			ret.add(owner);
		}

		return ret;
	}

	@Override
	protected String calculateName(ShptSession session, Group group) throws Exception {
		return group.getLoginName();
	}
}