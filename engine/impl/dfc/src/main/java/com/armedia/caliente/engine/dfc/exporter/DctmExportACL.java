/**
 *
 */

package com.armedia.caliente.engine.dfc.exporter;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import com.armedia.caliente.engine.converter.IntermediateProperty;
import com.armedia.caliente.engine.dfc.DctmDataType;
import com.armedia.caliente.engine.dfc.DctmMappingUtils;
import com.armedia.caliente.engine.dfc.common.DctmACL;
import com.armedia.caliente.engine.dfc.common.DctmCmisACLTools;
import com.armedia.caliente.engine.exporter.ExportException;
import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.store.CmfProperty;
import com.armedia.commons.dfc.util.DctmQuery;
import com.armedia.commons.dfc.util.DfUtils;
import com.armedia.commons.dfc.util.DfValueFactory;
import com.armedia.commons.utilities.Tools;
import com.documentum.fc.client.IDfACL;
import com.documentum.fc.client.IDfGroup;
import com.documentum.fc.client.IDfPermit;
import com.documentum.fc.client.IDfPersistentObject;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.client.IDfUser;
import com.documentum.fc.common.DfException;
import com.documentum.fc.common.IDfValue;

/**
 * @author diego
 *
 */
public class DctmExportACL extends DctmExportDelegate<IDfACL> implements DctmACL {

	/**
	 * This DQL will find all users for which this ACL is marked as the default ACL, and thus all
	 * users for whom it must be restored later on.
	 */
	private static final String DQL_FIND_USERS_WITH_DEFAULT_ACL = "SELECT u.user_name FROM dm_user u, dm_acl a WHERE u.acl_domain = a.owner_name AND u.acl_name = a.object_name AND a.r_object_id = '%s'";

	protected DctmExportACL(DctmExportDelegateFactory factory, IDfSession session, IDfACL acl) throws Exception {
		super(factory, session, IDfACL.class, acl);
	}

	DctmExportACL(DctmExportDelegateFactory factory, IDfSession session, IDfPersistentObject acl) throws Exception {
		this(factory, session, DctmExportDelegate.staticCast(IDfACL.class, acl));
	}

	@Override
	protected String calculateLabel(IDfSession session, IDfACL acl) throws Exception {
		return String.format("%s::%s", acl.getDomain(), acl.getObjectName());
	}

	protected void getDataPropertiesForDocumentum(DctmExportContext ctx, Collection<CmfProperty<IDfValue>> properties,
		IDfACL acl) throws DfException {
		CmfProperty<IDfValue> property = null;

		property = new CmfProperty<>(DctmACL.DOCUMENTUM_MARKER, DctmDataType.DF_BOOLEAN.getStoredType(), false);
		property.setValue(DfValueFactory.newBooleanValue(true));
		properties.add(property);

		final String aclId = acl.getObjectId().getId();
		try (DctmQuery query = new DctmQuery(ctx.getSession(),
			String.format(DctmExportACL.DQL_FIND_USERS_WITH_DEFAULT_ACL, aclId), DctmQuery.Type.DF_EXECREAD_QUERY)) {
			CmfProperty<IDfValue> p = new CmfProperty<>(IntermediateProperty.USERS_WITH_DEFAULT_ACL,
				DctmDataType.DF_STRING.getStoredType());
			query.forEachRemaining((o) -> p.addValue(o.getValueAt(0)));
			properties.add(p);
		}

		CmfProperty<IDfValue> accessors = new CmfProperty<>(DctmACL.ACCESSORS, DctmDataType.DF_STRING.getStoredType(),
			true);
		CmfProperty<IDfValue> accessorTypes = new CmfProperty<>(DctmACL.ACCESSOR_TYPES,
			DctmDataType.DF_STRING.getStoredType(), true);
		CmfProperty<IDfValue> permitTypes = new CmfProperty<>(DctmACL.PERMIT_TYPES,
			DctmDataType.DF_INTEGER.getStoredType(), true);
		CmfProperty<IDfValue> permitValues = new CmfProperty<>(DctmACL.PERMIT_VALUES,
			DctmDataType.DF_STRING.getStoredType(), true);
		final IDfSession session = ctx.getSession();
		Set<String> missingAccessors = new HashSet<>();
		for (IDfPermit p : DfUtils.getPermissionsWithFallback(session, acl)) {
			// First, validate the accessor
			final String accessor = p.getAccessorName();
			final boolean group;
			switch (p.getPermitType()) {
				case IDfPermit.DF_REQUIRED_GROUP:
				case IDfPermit.DF_REQUIRED_GROUP_SET:
					group = true;
					break;

				default:
					group = false;
					break;
			}

			IDfPersistentObject o = (group ? session.getGroup(accessor) : session.getUser(accessor));
			if ((o == null) && !DctmMappingUtils.SPECIAL_NAMES.contains(accessor)) {
				// Accessor not there, skip it...
				if (!missingAccessors.contains(accessor)) {
					this.log.warn("Missing dependency for ACL [{}] - {} [{}] not found (as ACL accessor)", getLabel(),
						(group ? "group" : "user"), accessor);
					missingAccessors.add(accessor);
				}
				continue;
			}

			final String accessorType;
			final IDfGroup g = session.getGroup(accessor);
			if ((g != null) || Tools.equals(DctmACL.DM_GROUP, accessor) || Tools.equals(DctmACL.DM_WORLD, accessor)) {
				accessorType = (g != null ? g.getGroupClass() : "group");
			} else {
				if (Tools.equals(DctmACL.DM_OWNER, accessor) || IDfUser.class.isInstance(o)) {
					accessorType = "user";
				} else {
					// WTF is it?
					accessorType = "?";
				}
			}

			accessors.addValue(DfValueFactory.newStringValue(DctmMappingUtils.substituteMappableUsers(acl, accessor)));
			accessorTypes.addValue(DfValueFactory.newStringValue(accessorType));
			permitTypes.addValue(DfValueFactory.newIntValue(p.getPermitType()));
			permitValues.addValue(DfValueFactory.newStringValue(p.getPermitValueString()));
		}
		properties.add(accessors);
		properties.add(accessorTypes);
		properties.add(permitValues);
		properties.add(permitTypes);
	}

	@Override
	protected boolean getDataProperties(DctmExportContext ctx, Collection<CmfProperty<IDfValue>> properties, IDfACL acl)
		throws DfException, ExportException {
		if (!super.getDataProperties(ctx, properties, acl)) { return false; }
		getDataPropertiesForDocumentum(ctx, properties, acl);
		DctmCmisACLTools.calculateCmisActions(ctx.getSession(), acl, properties);
		return true;
	}

	@Override
	protected Collection<DctmExportDelegate<?>> findRequirements(IDfSession session, CmfObject<IDfValue> marshaled,
		IDfACL acl, DctmExportContext ctx) throws Exception {
		Collection<DctmExportDelegate<?>> ret = super.findRequirements(session, marshaled, acl, ctx);

		final int count = acl.getAccessorCount();
		for (int i = 0; i < count; i++) {
			final String name = acl.getAccessorName(i);
			final boolean group = acl.isGroup(i);

			if (!group) {
				if (DctmMappingUtils.isMappableUser(session, name) || ctx.isSpecialUser(name)) {
					// User is mapped to a special user, so we shouldn't include it as a dependency
					// because it will be mapped on the target
					continue;
				}
			} else {
				if (ctx.isSpecialGroup(name)) {
					continue;
				}
			}

			if (DctmMappingUtils.SPECIAL_NAMES.contains(name)) {
				// This is a special name - non-existent per-se, but supported by the system
				// such as dm_owner, dm_group, dm_world
				continue;
			}

			final IDfPersistentObject obj = (group ? session.getGroup(name) : session.getUser(name));
			if (obj == null) {
				this.log.warn("Missing dependency for ACL [{}] - {} [{}] not found (as ACL accessor)",
					acl.getObjectName(), (group ? "group" : "user"), name);
				continue;
			}
			ret.add(this.factory.newExportDelegate(session, obj));
		}

		// Do the owner
		final String owner = acl.getDomain();
		if (DctmMappingUtils.isMappableUser(session, owner) || ctx.isSpecialUser(owner)) {
			this.log.warn("Skipping export of special user [{}]", owner);
		} else {
			IDfUser user = session.getUser(owner);
			if (user == null) {
				throw new Exception(
					String.format("Missing dependency for ACL [%s:%s] - user [%s] not found (as ACL domain)", owner,
						acl.getObjectName(), owner));
			}
			ret.add(this.factory.newExportDelegate(session, user));
		}
		return ret;
	}

	@Override
	protected String calculateName(IDfSession session, IDfACL acl) throws Exception {
		return String.format("%s::%s", acl.getDomain(), acl.getObjectName());
	}
}