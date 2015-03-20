/**
 *
 */

package com.armedia.cmf.engine.documentum.importer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang.StringUtils;

import com.armedia.cmf.engine.documentum.DctmAttributes;
import com.armedia.cmf.engine.documentum.DctmConstant;
import com.armedia.cmf.engine.documentum.DctmMappingUtils;
import com.armedia.cmf.engine.documentum.DctmObjectType;
import com.armedia.cmf.engine.documentum.DfUtils;
import com.armedia.cmf.engine.documentum.DfValueFactory;
import com.armedia.cmf.engine.documentum.common.Setting;
import com.armedia.cmf.engine.importer.ImportException;
import com.armedia.cmf.storage.StoredAttribute;
import com.armedia.cmf.storage.StoredObject;
import com.armedia.commons.utilities.Tools;
import com.documentum.fc.client.IDfCollection;
import com.documentum.fc.client.IDfQuery;
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

	public DctmImportUser(DctmImportEngine engine, StoredObject<IDfValue> storedObject) {
		super(engine, DctmObjectType.USER, storedObject);
	}

	@Override
	protected String calculateLabel(IDfUser user) throws DfException {
		return user.getUserName();
	}

	@Override
	protected IDfUser locateInCms(DctmImportContext ctx) throws ImportException, DfException {
		// If that search failed, go by username
		final IDfSession session = ctx.getSession();
		final String userName = this.storedObject.getAttribute(DctmAttributes.USER_NAME).getValue().asString();
		final StoredAttribute<IDfValue> domainAtt = this.storedObject.getAttribute(DctmAttributes.USER_LOGIN_DOMAIN);
		return DctmImportUser.locateExistingUser(session, userName, (domainAtt != null ? domainAtt.getValue()
			.asString() : null));
	}

	public static IDfUser locateExistingUser(IDfSession session, String userName, String domainName)
		throws MultipleUserMatchesException, DfException {
		// If that search failed, go by username
		userName = DctmMappingUtils.resolveMappableUser(session, userName);
		IDfUser ret = session.getUser(userName);
		// No match? Ok...try by login name + domain
		if (ret == null) {
			domainName = Tools.coalesce(domainName, "");
			ret = session.getUserByLoginName(userName, !StringUtils.isBlank(domainName) ? domainName : null);
		}
		// Still no match? try by just login name, any domain
		if (ret == null) {
			String dql = String.format(DctmImportUser.FIND_USER_BY_LOGIN_DQL, DfUtils.quoteString(userName));
			IDfCollection c = DfUtils.executeQuery(session, dql, IDfQuery.DF_EXECREAD_QUERY);
			try {
				List<String> candidates = null;
				while (c.next()) {
					if (ret != null) {
						// If we've found more than one candidate, we list them all...
						if (candidates == null) {
							candidates = new ArrayList<String>();
							candidates.add(String.format("%s (D=%s)", ret.getString(DctmAttributes.USER_NAME),
								ret.getString(DctmAttributes.USER_LOGIN_DOMAIN)));
						}
						candidates.add(String.format("%s (D=%s)", c.getString(DctmAttributes.USER_NAME),
							c.getString(DctmAttributes.USER_LOGIN_DOMAIN)));
						continue;
					}
					ret = session.getUser(c.getString(DctmAttributes.USER_NAME));
				}
				if (candidates != null) { throw new MultipleUserMatchesException(String.format(
					"Found multiple candidate matches for login name [%s]: %s", userName, candidates)); }
			} finally {
				DfUtils.closeQuietly(c);
			}
		}
		return ret;
	}

	@Override
	protected boolean isValidForLoad(DctmImportContext ctx, IDfUser user) throws DfException {
		if (ctx.isSpecialUser(user.getUserName())) { return false; }
		return super.isValidForLoad(ctx, user);
	}

	@Override
	protected boolean skipImport(DctmImportContext ctx) throws DfException {
		IDfValue userNameValue = this.storedObject.getAttribute(DctmAttributes.USER_NAME).getValue();
		final String userName = userNameValue.asString();
		if (ctx.isSpecialUser(userName)) { return true; }
		return super.skipImport(ctx);
	}

	@Override
	protected void prepareForConstruction(IDfUser user, boolean newObject, DctmImportContext context)
		throws DfException {

		StoredAttribute<IDfValue> loginDomain = this.storedObject.getAttribute(DctmAttributes.USER_LOGIN_DOMAIN);
		IDfTypedObject serverConfig = user.getSession().getServerConfig();
		String serverVersion = serverConfig.getString(DctmAttributes.R_SERVER_VERSION);
		StoredAttribute<IDfValue> userSourceAtt = this.storedObject.getAttribute(DctmAttributes.USER_SOURCE);
		String userSource = (userSourceAtt != null ? userSourceAtt.getValue().asString() : null);

		// NOTE for some reason, 6.5 sp2 with ldap requires that user_login_domain be set
		// workaround for [DM_USER_E_MUST_HAVE_LOGINDOMAIN] error
		// Only do this for Documentum 6.5-SP2
		if ((loginDomain == null) && serverVersion.startsWith("6.5") && "LDAP".equalsIgnoreCase(userSource)) {
			IDfAttr attr = user.getAttr(user.findAttrIndex(DctmAttributes.USER_LOGIN_DOMAIN));
			loginDomain = newStoredAttribute(attr, DfValueFactory.newStringValue(""));
			this.storedObject.setAttribute(loginDomain);
		}
	}

	@Override
	protected void finalizeConstruction(IDfUser user, boolean newObject, DctmImportContext ctx) throws DfException {
		// First, set the username - only do this for new objects!!
		if (newObject) {
			copyAttributeToObject(DctmAttributes.USER_NAME, user);
			final String userName = this.storedObject.getAttribute(DctmAttributes.USER_NAME).getValue().asString();

			// Login name + domain
			if (this.storedObject.getAttributeNames().contains(DctmAttributes.USER_LOGIN_DOMAIN)) {
				copyAttributeToObject(DctmAttributes.USER_LOGIN_DOMAIN, user);
			}
			if (this.storedObject.getAttributeNames().contains(DctmAttributes.USER_LOGIN_NAME)) {
				copyAttributeToObject(DctmAttributes.USER_LOGIN_NAME, user);
			} else {
				setAttributeOnObject(DctmAttributes.USER_LOGIN_NAME, user.getValue(DctmAttributes.USER_NAME), user);
			}

			// Next, set the password
			StoredAttribute<IDfValue> att = this.storedObject.getAttribute(DctmAttributes.USER_SOURCE);
			final IDfValue userSource = (att != null ? att.getValue() : null);
			if ((att == null) || Tools.equals(DctmConstant.USER_SOURCE_INLINE_PASSWORD, userSource.asString())) {
				// Default the password to the user's login name, if a specific value hasn't been
				// selected for global use
				final String inlinePasswordValue = ctx.getSettings().getString(
					Setting.DEFAULT_USER_PASSWORD.getLabel(), userName);
				setAttributeOnObject(DctmAttributes.USER_PASSWORD,
					Collections.singletonList(DfValueFactory.newStringValue(inlinePasswordValue)), user);
			}
		}

		// Next, set the home docbase
		// TODO: Disabled, for now...for some reason the dm_job was failing on SPDMS_SI
		/*
		final IDfValue newHomeDocbase = getAttribute(CmsAttributes.HOME_DOCBASE).getValue();
		final String docbase = newHomeDocbase.asString();
		final String existingDocbase = user.getHomeDocbase();
		if (!docbase.equals("") && !Tools.equals(docbase, existingDocbase)) {
			user.changeHomeDocbase(docbase, true);
		}
		 */
	}
}