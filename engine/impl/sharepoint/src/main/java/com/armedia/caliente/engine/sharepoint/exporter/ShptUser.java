package com.armedia.caliente.engine.sharepoint.exporter;

import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.armedia.caliente.engine.exporter.ExportException;
import com.armedia.caliente.engine.sharepoint.IncompleteDataException;
import com.armedia.caliente.engine.sharepoint.ShptAttributes;
import com.armedia.caliente.engine.sharepoint.ShptSession;
import com.armedia.caliente.engine.sharepoint.ShptSessionException;
import com.armedia.caliente.store.CmfAttribute;
import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.store.CmfValue;
import com.armedia.commons.utilities.Tools;
import com.independentsoft.share.Group;
import com.independentsoft.share.Role;
import com.independentsoft.share.User;
import com.independentsoft.share.UserId;

public class ShptUser extends ShptSecurityObject<User> {

	private final List<Role> roles;
	private final String userName;
	private final String userDomain;

	public ShptUser(ShptExportDelegateFactory factory, ShptSession session, User user) throws Exception {
		super(factory, session, User.class, user);
		/*
		List<Integer> roles = this.service.getRoleAssignments(this.object.getId());
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
		String loginName = this.object.getLoginName();
		if (loginName == null) { throw new IncompleteDataException(String.format(
			"The given user lacks a login name - cannot identify the user with ID [%s]", this.object.getId())); }
		final int backslash = loginName.indexOf('\\');
		final int atSign = loginName.indexOf('@');
		if (backslash >= 0) {
			// 1) ^.*|domain\\user$
			this.userName = loginName.substring(backslash + 1);
			this.userDomain = loginName.substring(loginName.indexOf('|') + 1, backslash).toLowerCase();
		} else if (atSign >= 0) {
			// 2) ^.*|user@domain$
			this.userName = loginName.substring(loginName.indexOf('|') + 1, atSign);
			this.userDomain = loginName.substring(atSign + 1).toLowerCase();
		} else {
			this.userName = loginName;
			this.userDomain = "";
		}
	}

	@Override
	public String calculateObjectId(ShptSession session, User user) throws Exception {
		UserId uid = user.getUserId();
		if (uid == null) { throw new IncompleteDataException(
			String.format("No userId information available for user [%s\\%s]", this.userDomain, this.userName)); }
		return String.format("%08X",
			Tools.hashTool(this, null, uid.getNameId().toLowerCase(), uid.getNameIdIssuer().toLowerCase()));
	}

	@Override
	protected int calculateNumericId(ShptSession session, User object) {
		return object.getId();
	}

	@Override
	protected String calculateLabel(ShptSession session, User object) throws Exception {
		String loginName = object.getLoginName();
		if (loginName == null) { throw new IncompleteDataException(String.format(
			"The given user lacks a login name - cannot identify the user with ID [%s]", this.object.getId())); }
		final int backslash = loginName.indexOf('\\');
		final int atSign = loginName.indexOf('@');
		String userName = null;
		String userDomain = null;
		if (backslash >= 0) {
			// 1) ^.*|domain\\user$
			userName = loginName.substring(backslash + 1);
			userDomain = loginName.substring(loginName.indexOf('|') + 1, backslash).toLowerCase();
		} else if (atSign >= 0) {
			// 2) ^.*|user@domain$
			userName = loginName.substring(loginName.indexOf('|') + 1, atSign);
			userDomain = loginName.substring(atSign + 1).toLowerCase();
		} else {
			userName = loginName;
			userDomain = null;
		}
		if (!StringUtils.isBlank(userDomain)) { return String.format("\\\\%s\\%s", userDomain, userName); }
		return userName;
	}

	public Collection<Role> getRoles() {
		return this.roles;
	}

	public String getDomain() {
		return this.userDomain;
	}

	@Override
	protected boolean marshal(ShptExportContext ctx, CmfObject<CmfValue> object) throws ExportException {
		if (!super.marshal(ctx, object)) { return false; }
		// UserID
		object.setAttribute(new CmfAttribute<>(ShptAttributes.OBJECT_ID.name, CmfValue.Type.ID, false,
			Collections.singleton(new CmfValue(String.format("USER(%s)", getObjectId())))));

		// LoginName
		object.setAttribute(new CmfAttribute<>(ShptAttributes.OBJECT_NAME.name, CmfValue.Type.STRING, false,
			Collections.singleton(new CmfValue(this.userName))));
		object.setAttribute(new CmfAttribute<>(ShptAttributes.LOGIN_NAME.name, CmfValue.Type.STRING, false,
			Collections.singleton(new CmfValue(this.userName))));
		if (this.userDomain != null) {
			object.setAttribute(new CmfAttribute<>(ShptAttributes.LOGIN_DOMAIN.name, CmfValue.Type.STRING, false,
				Collections.singleton(new CmfValue(this.userDomain))));
		}

		// SiteAdmin
		object.setAttribute(new CmfAttribute<>(ShptAttributes.SITE_ADMIN.name, CmfValue.Type.BOOLEAN, false,
			Collections.singleton(new CmfValue(this.object.isSiteAdmin()))));

		// PrincipalType
		object.setAttribute(new CmfAttribute<>(ShptAttributes.PRINCIPAL_TYPE.name, CmfValue.Type.STRING, false,
			Collections.singleton(new CmfValue(this.object.getType().name()))));

		// UserIdName
		object.setAttribute(new CmfAttribute<>(ShptAttributes.PRINCIPAL_ID.name, CmfValue.Type.STRING, false,
			Collections.singleton(new CmfValue(this.object.getUserId().getNameId()))));

		// UserIdIssuer
		object.setAttribute(new CmfAttribute<>(ShptAttributes.PRINCIPAL_ID_ISSUER.name, CmfValue.Type.STRING, false,
			Collections.singleton(new CmfValue(this.object.getUserId().getNameIdIssuer()))));

		object.setAttribute(new CmfAttribute<>(ShptAttributes.MODIFICATION_DATE.name, CmfValue.Type.DATETIME, false,
			Collections.singleton(new CmfValue(new Date()))));

		// Email
		object.setAttribute(new CmfAttribute<>(ShptAttributes.EMAIL.name, CmfValue.Type.STRING, false,
			Collections.singleton(new CmfValue(this.object.getEmail()))));

		// Title
		object.setAttribute(new CmfAttribute<>(ShptAttributes.TITLE.name, CmfValue.Type.STRING, false,
			Collections.singleton(new CmfValue(this.object.getTitle()))));

		// User Groups
		final List<Group> l;
		final ShptSession service = ctx.getSession();
		try {
			l = service.getUserGroups(this.object.getId());
		} catch (ShptSessionException e) {
			throw new ExportException(String.format("Failed to obtain the group list for user [%s](%d)",
				this.object.getLoginName(), this.object.getId()), e);
		}
		CmfAttribute<CmfValue> groups = new CmfAttribute<>(ShptAttributes.USER_GROUPS.name, CmfValue.Type.STRING, true);
		object.setAttribute(groups);
		if ((l != null) && !l.isEmpty()) {
			for (Group g : l) {
				groups.addValue(new CmfValue(g.getLoginName()));
			}
		}

		CmfAttribute<CmfValue> roles = new CmfAttribute<>(ShptAttributes.USER_ROLES.name, CmfValue.Type.STRING, true);
		object.setAttribute(groups);
		for (Role r : this.roles) {
			roles.addValue(new CmfValue(r.getName()));
		}
		return true;
	}

	@Override
	protected Collection<ShptObject<?>> findRequirements(ShptSession service, CmfObject<CmfValue> marshaled,
		ShptExportContext ctx) throws Exception {
		Collection<ShptObject<?>> ret = super.findRequirements(service, marshaled, ctx);
		List<Group> l = service.getUserGroups(this.object.getId());
		if ((l != null) && !l.isEmpty()) {
			for (Group g : l) {
				ret.add(new ShptGroup(this.factory, service, g));
			}
		}

		/*
		for (Role r : this.roles) {
			ret.add(new ShptRole(service, r));
		}
		 */
		return ret;
	}

	@Override
	protected String calculateName(ShptSession session, User user) throws Exception {
		String loginName = user.getLoginName();
		if (loginName == null) { throw new IncompleteDataException(String.format(
			"The given user lacks a login name - cannot identify the user with ID [%s]", this.object.getId())); }
		final int backslash = loginName.indexOf('\\');
		final int atSign = loginName.indexOf('@');
		if (backslash >= 0) {
			// 1) ^.*|domain\\user$
			return loginName.substring(backslash + 1);
		} else if (atSign >= 0) {
			// 2) ^.*|user@domain$
			return loginName.substring(loginName.indexOf('|') + 1, atSign);
		} else {
			return loginName;
		}
	}
}