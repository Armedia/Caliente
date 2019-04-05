/**
 *
 */

package com.armedia.caliente.engine.dfc.importer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.armedia.caliente.engine.dfc.DctmAttributes;
import com.armedia.caliente.engine.dfc.DctmConstant;
import com.armedia.caliente.engine.dfc.DctmMappingUtils;
import com.armedia.caliente.engine.dfc.DctmObjectType;
import com.armedia.caliente.engine.dfc.common.Setting;
import com.armedia.caliente.engine.importer.ImportException;
import com.armedia.caliente.store.CmfAttribute;
import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.store.CmfValueMapper.Mapping;
import com.armedia.caliente.tools.dfc.DctmQuery;
import com.armedia.caliente.tools.dfc.DfUtils;
import com.armedia.caliente.tools.dfc.DfValueFactory;
import com.armedia.commons.utilities.Tools;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.client.IDfTypedObject;
import com.documentum.fc.client.IDfUser;
import com.documentum.fc.common.DfException;
import com.documentum.fc.common.IDfAttr;
import com.documentum.fc.common.IDfValue;

/**
 * @author Diego Rivera &lt;diego.rivera@armedia.com&gt;
 *
 */
public class DctmImportUser extends DctmImportDelegate<IDfUser> {

	private static final String FIND_USER_BY_LOGIN_DQL = "select distinct user_name, user_login_domain from dm_user where user_login_name = %s order by user_login_domain";
	private static final String USERNAME_MAPPING_NAME = "userName";

	public DctmImportUser(DctmImportDelegateFactory factory, CmfObject<IDfValue> storedObject) throws Exception {
		super(factory, IDfUser.class, DctmObjectType.USER, storedObject);
	}

	@Override
	protected String calculateLabel(IDfUser user) throws DfException {
		return user.getUserName();
	}

	@Override
	protected IDfUser locateInCms(DctmImportContext ctx) throws ImportException, DfException {
		// If that search failed, go by username
		final String userName = this.cmfObject.getAttribute(DctmAttributes.USER_NAME).getValue().asString();
		final String loginName = this.cmfObject.getAttribute(DctmAttributes.USER_LOGIN_NAME).getValue().asString();
		final CmfAttribute<IDfValue> domainAtt = this.cmfObject.getAttribute(DctmAttributes.USER_LOGIN_DOMAIN);
		return DctmImportUser.locateExistingUser(ctx, userName, loginName,
			(domainAtt != null ? domainAtt.getValue().asString() : null));
	}

	protected static IDfUser getUserMapping(DctmImportContext ctx, String userName) throws DfException {
		Mapping m = ctx.getValueMapper().getTargetMapping(CmfObject.Archetype.USER,
			DctmImportUser.USERNAME_MAPPING_NAME, userName);
		if (m == null) { return null; }
		return ctx.getSession().getUser(m.getTargetValue());
	}

	protected static void setUserMapping(DctmImportContext ctx, String userName, IDfUser user) throws DfException {
		ctx.getValueMapper().setMapping(CmfObject.Archetype.USER, DctmImportUser.USERNAME_MAPPING_NAME, userName,
			user.getUserName());
	}

	public static IDfUser locateByLoginName(IDfSession session, String loginName, String domainName)
		throws DfException, MultipleUserMatchesException {
		// Still no match? Ok...try by mapping the username to the login name (+ domain)...
		IDfUser ret = session.getUserByLoginName(loginName, !StringUtils.isBlank(domainName) ? domainName : null);
		if (ret == null) {
			// Still no match? try by just login name, any domain
			String dql = String.format(DctmImportUser.FIND_USER_BY_LOGIN_DQL, DfUtils.quoteString(loginName));
			try (DctmQuery query = new DctmQuery(session, dql, DctmQuery.Type.DF_EXECREAD_QUERY)) {
				List<String> candidates = null;
				while (query.hasNext()) {
					IDfTypedObject c = query.next();
					if (ret != null) {
						// If we've found more than one candidate, we list them all...
						if (candidates == null) {
							candidates = new ArrayList<>();
							candidates.add(String.format("%s (D=%s)", ret.getString(DctmAttributes.USER_NAME),
								ret.getString(DctmAttributes.USER_LOGIN_DOMAIN)));
						}
						candidates.add(String.format("%s (D=%s)", c.getString(DctmAttributes.USER_NAME),
							c.getString(DctmAttributes.USER_LOGIN_DOMAIN)));
						continue;
					}
					ret = session.getUser(c.getString(DctmAttributes.USER_NAME));
				}
				if (candidates != null) {
					throw new MultipleUserMatchesException(String
						.format("Found multiple candidate matches for login name [%s]: %s", loginName, candidates));
				}
			}
		}
		return ret;
	}

	public static IDfUser locateExistingUser(DctmImportContext ctx, String userName)
		throws MultipleUserMatchesException, DfException {
		return DctmImportUser.locateExistingUser(ctx, userName, null, null);
	}

	private static IDfUser locateExistingUser(DctmImportContext ctx, String userName, String loginName,
		String domainName) throws MultipleUserMatchesException, DfException {
		if (ctx == null) { throw new IllegalArgumentException("Must provide an import context to search with"); }
		if (userName == null) { throw new IllegalArgumentException("Must provide a username to locate"); }
		final IDfSession session = ctx.getSession();
		userName = DctmMappingUtils.resolveMappableUser(session, userName);

		// Ok...let's first try the direct approach...
		IDfUser ret = session.getUser(userName);
		if (ret != null) { return ret; }

		// No match? Let's see if we've already mapped this username to a different user...
		ret = DctmImportUser.getUserMapping(ctx, userName);
		if (ret != null) { return ret; }

		// Still no match? Ok...try by mapping the username to the login name (+ domain)...
		domainName = Tools.coalesce(domainName, "");
		ret = DctmImportUser.locateByLoginName(session, userName, domainName);
		if ((ret == null) && !StringUtils.isBlank(loginName)) {
			// Still no match? try by the login name as the username
			ret = session.getUser(loginName);
			if (ret == null) {
				// Still no match? try login name to login name
				ret = DctmImportUser.locateByLoginName(session, loginName, domainName);
			}
		}

		if (ret != null) {
			// We have a match!! Set the mapping so we don't have to run through circles again...
			DctmImportUser.setUserMapping(ctx, userName, ret);
		}
		return ret;
	}

	@Override
	protected boolean skipImport(DctmImportContext ctx) throws DfException, ImportException {
		CmfAttribute<IDfValue> groupAtt = this.cmfObject.getAttribute(DctmAttributes.R_IS_GROUP);
		boolean group = ((groupAtt != null) && groupAtt.hasValues() && groupAtt.getValue().asBoolean());
		if (group) { return true; }
		IDfValue userNameValue = this.cmfObject.getAttribute(DctmAttributes.USER_NAME).getValue();
		final String userName = userNameValue.asString();
		return ctx.isUntouchableUser(userName) || super.skipImport(ctx);
	}

	@Override
	protected void prepareForConstruction(IDfUser user, boolean newObject, DctmImportContext context)
		throws DfException {

		CmfAttribute<IDfValue> loginDomain = this.cmfObject.getAttribute(DctmAttributes.USER_LOGIN_DOMAIN);
		IDfTypedObject serverConfig = context.getSession().getServerConfig();
		String serverVersion = serverConfig.getString(DctmAttributes.R_SERVER_VERSION);
		CmfAttribute<IDfValue> userSourceAtt = this.cmfObject.getAttribute(DctmAttributes.USER_SOURCE);
		String userSource = (userSourceAtt != null ? userSourceAtt.getValue().asString() : null);

		// NOTE for some reason, 6.5 sp2 with ldap requires that user_login_domain be set
		// workaround for [DM_USER_E_MUST_HAVE_LOGINDOMAIN] error
		// Only do this for Documentum 6.5-SP2
		if ((loginDomain == null) && serverVersion.startsWith("6.5") && "LDAP".equalsIgnoreCase(userSource)) {
			IDfAttr attr = user.getAttr(user.findAttrIndex(DctmAttributes.USER_LOGIN_DOMAIN));
			loginDomain = newStoredAttribute(attr, DfValueFactory.newStringValue(""));
			this.cmfObject.setAttribute(loginDomain);
		}
	}

	@Override
	protected void finalizeConstruction(IDfUser user, boolean newObject, DctmImportContext ctx) throws DfException {
		// First, set the username - only do this for new objects!!
		if (newObject) {
			copyAttributeToObject(DctmAttributes.USER_NAME, user);
			final String userName = this.cmfObject.getAttribute(DctmAttributes.USER_NAME).getValue().asString();

			// Login name + domain
			if (this.cmfObject.getAttributeNames().contains(DctmAttributes.USER_LOGIN_DOMAIN)) {
				copyAttributeToObject(DctmAttributes.USER_LOGIN_DOMAIN, user);
			}
			if (this.cmfObject.getAttributeNames().contains(DctmAttributes.USER_LOGIN_NAME)) {
				copyAttributeToObject(DctmAttributes.USER_LOGIN_NAME, user);
			} else {
				setAttributeOnObject(DctmAttributes.USER_LOGIN_NAME, user.getValue(DctmAttributes.USER_NAME), user);
			}

			// Next, set the password
			CmfAttribute<IDfValue> att = this.cmfObject.getAttribute(DctmAttributes.USER_SOURCE);
			final IDfValue userSource = (att != null ? att.getValue() : null);
			if ((att == null) || Tools.equals(DctmConstant.USER_SOURCE_INLINE_PASSWORD, userSource.asString())) {
				// Default the password to the user's login name, if a specific value hasn't been
				// selected for global use
				final String inlinePasswordValue = ctx.getSettings().getString(Setting.DEFAULT_USER_PASSWORD.getLabel(),
					userName);
				setAttributeOnObject(DctmAttributes.USER_PASSWORD,
					Collections.singletonList(DfValueFactory.newStringValue(inlinePasswordValue)), user);
			}
		}

		// Next, set the home docbase
		/*
		final IDfValue newHomeDocbase = this.cmfObject.getAttribute(DctmAttributes.HOME_DOCBASE).getValue();
		final String docbase = newHomeDocbase.asString();
		if (newObject) {
			user.setHomeDocbase(docbase);
		} else {
			final String existingDocbase = user.getHomeDocbase();
			if (!docbase.equals("") && !Tools.equals(docbase, existingDocbase)) {
				user.changeHomeDocbase(docbase, true);
			}
		}
		*/
	}
}