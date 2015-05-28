/**
 *
 */

package com.armedia.cmf.engine.documentum.common;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.cmf.engine.converter.IntermediateProperty;
import com.armedia.cmf.engine.documentum.DctmAttributes;
import com.armedia.cmf.engine.documentum.DctmDataType;
import com.armedia.cmf.engine.documentum.DctmMappingUtils;
import com.armedia.cmf.engine.documentum.DfUtils;
import com.armedia.cmf.engine.documentum.DfValueFactory;
import com.armedia.cmf.engine.exporter.ExportException;
import com.armedia.cmf.storage.CmfAclActorType;
import com.armedia.cmf.storage.CmfObject;
import com.armedia.cmf.storage.CmfProperty;
import com.armedia.commons.utilities.FileNameTools;
import com.armedia.commons.utilities.Tools;
import com.documentum.fc.client.DfPermit;
import com.documentum.fc.client.IDfACL;
import com.documentum.fc.client.IDfCollection;
import com.documentum.fc.client.IDfPermit;
import com.documentum.fc.client.IDfPersistentObject;
import com.documentum.fc.client.IDfQuery;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.common.DfException;
import com.documentum.fc.common.IDfList;
import com.documentum.fc.common.IDfValue;

/**
 * @author diego
 *
 */
public class DctmCmisACLTools implements DctmACL {

	private static final Logger LOG = LoggerFactory.getLogger(DctmCmisACLTools.class);

	/**
	 * This DQL will find all users for which this ACL is marked as the default ACL, and thus all
	 * users for whom it must be restored later on.
	 */
	private static final String DQL_FIND_USERS_WITH_DEFAULT_ACL = "SELECT u.user_name FROM dm_user u, dm_acl a WHERE u.acl_domain = a.owner_name AND u.acl_name = a.object_name AND a.r_object_id = '%s'";

	private static enum PermitToAction {
		//
		DF_PERMIT_NONE(IDfACL.DF_PERMIT_NONE),
		DF_PERMIT_BROWSE(IDfACL.DF_PERMIT_BROWSE, //
			"canGetACL.Object", //
			"canGetChildren.Folder", //
			"canGetDescendants.Folder", //
			"canGetFolderParent.Folder", //
			"canGetObjectParents.Object", //
			"canGetObjectRelationships.Object", //
			"canGetProperties.Object" //
		),
		DF_PERMIT_READ(IDfACL.DF_PERMIT_READ, //
			"canGetAllVersions.Document", //
			"canGetContentStream.Object" //
		),
		DF_PERMIT_RELATE(IDfACL.DF_PERMIT_RELATE, //
			"canCreateRelationship.Source", //
			"canCreateRelationship.Target" //
		),
		DF_PERMIT_VERSION(IDfACL.DF_PERMIT_VERSION, //
			"canCancelCheckout.Document", //
			"canCheckin.Document", //
			"canCheckOut.Document" //
		),
		DF_PERMIT_WRITE(IDfACL.DF_PERMIT_WRITE, //
			"canAddToFolder.Object", //
			"canCreateDocument.Folder", //
			"canCreateFolder.Folder", //
			"canDeleteContentStream.Document", //
			"canMoveObject.Object", //
			"canMoveObject.Source", //
			"canMoveObject.Target", //
			"canRemoveObjectFromFolder.Folder", //
			"canSetContentStream.Document", //
			"canUpdateProperties.Object" //
		),
		DF_PERMIT_DELETE(IDfACL.DF_PERMIT_DELETE, //
			"canDelete.Object" //
		),
		//
		;

		public final int permit;
		public final Set<String> actions;

		private PermitToAction(int permit, String... actions) {
			DfUtils.decodeAccessPermission(permit);
			this.permit = permit;
			Set<String> a = new HashSet<String>();
			for (String s : actions) {
				if (s == null) {
					continue;
				}
				a.add(s);
			}
			this.actions = Tools.freezeSet(a);
		}
	}

	private static final Map<Integer, Set<String>> PERMIT_TO_ACTION;
	private static final Map<String, Integer> ACTION_TO_PERMIT;

	static {
		Map<Integer, Set<String>> p2a = new HashMap<Integer, Set<String>>();
		Map<String, Integer> a2p = new HashMap<String, Integer>();
		for (PermitToAction e : PermitToAction.values()) {
			if (e.actions.isEmpty()) {
				continue;
			}
			int permit = e.permit;
			Set<String> s = new HashSet<String>();
			s.addAll(e.actions);
			Set<String> oldS = p2a.put(permit, Tools.freezeSet(s));
			if (oldS != null) { throw new RuntimeException(String.format(
				"Permission [%d] is defined for two sets of Actions: [%s] and [%s]", permit, s, oldS)); }

			for (String a : s) {
				Integer old = a2p.put(a, permit);
				if (old != null) { throw new RuntimeException(String.format(
					"Action [%s] is mapped for two Permissions: [%d] and [%d]", a, permit, old)); }
			}
		}
		PERMIT_TO_ACTION = Tools.freezeMap(p2a);
		ACTION_TO_PERMIT = Tools.freezeMap(a2p);
	}

	private static enum XPermitToAction {
		//
		DF_XPERMIT_CHANGE_FOLDER_LINKS(IDfACL.DF_XPERMIT_CHANGE_FOLDER_LINKS_STR),
		DF_XPERMIT_CHANGE_LOCATION(IDfACL.DF_XPERMIT_CHANGE_LOCATION_STR, //
			"canAddToFolder.Object", //
			"canMoveObject.Object" //
		),
		DF_XPERMIT_CHANGE_OWNER(IDfACL.DF_XPERMIT_CHANGE_OWNER_STR),
		DF_XPERMIT_CHANGE_PERMIT(IDfACL.DF_XPERMIT_CHANGE_PERMIT_STR, //
			"canApplyACL.Object" //
		),
		DF_XPERMIT_CHANGE_STATE(IDfACL.DF_XPERMIT_CHANGE_STATE_STR),
		DF_XPERMIT_DELETE_OBJECT(IDfACL.DF_XPERMIT_DELETE_OBJECT_STR),
		DF_XPERMIT_EXECUTE_PROC(IDfACL.DF_XPERMIT_EXECUTE_PROC_STR),
		//
		;

		public final String permit;
		public final Set<String> actions;

		private XPermitToAction(String permit, String... actions) {
			this.permit = permit;
			Set<String> a = new HashSet<String>();
			for (String s : actions) {
				if (s == null) {
					continue;
				}
				a.add(s);
			}
			this.actions = Tools.freezeSet(a);
		}
	}

	private static final Map<String, Set<String>> XPERMIT_TO_ACTION;
	private static final Map<String, String> ACTION_TO_XPERMIT;

	static {
		Map<String, Set<String>> x2a = new HashMap<String, Set<String>>();
		Map<String, String> a2x = new HashMap<String, String>();
		for (XPermitToAction e : XPermitToAction.values()) {
			if (e.actions.isEmpty()) {
				continue;
			}
			String permit = e.permit;
			Set<String> s = new HashSet<String>();
			s.addAll(e.actions);
			Set<String> oldS = x2a.put(permit, Tools.freezeSet(s));
			if (oldS != null) { throw new RuntimeException(String.format(
				"Extended Permission [%s] is defined for two sets of Actions: [%s] and [%s]", permit, s, oldS)); }

			for (String a : s) {
				String old = a2x.put(a, permit);
				if (old != null) { throw new RuntimeException(String.format(
					"Action [%s] is mapped for two Extended Permissions: [%s] and [%s]", permit, a, old)); }
			}
		}
		XPERMIT_TO_ACTION = Tools.freezeMap(x2a);
		ACTION_TO_XPERMIT = Tools.freezeMap(a2x);
	}

	private static Set<String> calculateActionsForPermissions(int permit, String extended) {
		Set<String> ret = new TreeSet<String>();
		Set<String> actions = null;

		permit = Tools.ensureBetween(IDfACL.DF_PERMIT_NONE, permit, IDfACL.DF_PERMIT_DELETE);

		// We must include all the actions from permission requested as well as all
		// actions from prior permissions, since that's how Documentum permissions work
		for (int i = permit; i >= IDfACL.DF_PERMIT_NONE; i--) {
			actions = DctmCmisACLTools.PERMIT_TO_ACTION.get(i);
			if (actions != null) {
				ret.addAll(actions);
			}
		}

		if (extended != null) {
			for (String xp : FileNameTools.tokenize(extended, ',')) {
				actions = DctmCmisACLTools.XPERMIT_TO_ACTION.get(xp);
				if (actions != null) {
					ret.addAll(actions);
				}
			}
		}
		return ret;
	}

	public static Collection<IDfPermit> calculatePermissionsFromCMIS(String actorName, String... actions) {
		return DctmCmisACLTools
			.calculatePermissionsFromCMIS(actorName, actions != null ? Arrays.asList(actions) : null);
	}

	public static Collection<IDfPermit> calculatePermissionsFromCMIS(String actorName, Collection<String> actions) {
		if (actorName == null) { throw new IllegalArgumentException("Must provide an actor"); }
		List<IDfPermit> ret = new ArrayList<IDfPermit>();
		if (actions != null) {
			int perm = IDfACL.DF_PERMIT_NONE;
			Set<String> extended = new TreeSet<String>();
			for (String a : actions) {
				Integer newPerm = DctmCmisACLTools.ACTION_TO_PERMIT.get(a);
				if ((newPerm != null) && (newPerm.intValue() > perm)) {
					perm = newPerm.intValue();
				}
				String e = DctmCmisACLTools.ACTION_TO_XPERMIT.get(a);
				if (e != null) {
					extended.add(e);
				}
			}

			DfPermit permit = new DfPermit();
			permit.setAccessorName(actorName);
			permit.setPermitType(IDfPermit.DF_ACCESS_PERMIT);
			permit.setPermitValue(DfUtils.decodeAccessPermission(perm));
			ret.add(permit);

			for (String x : extended) {
				permit = new DfPermit();
				permit.setAccessorName(actorName);
				permit.setPermitType(IDfPermit.DF_EXTENDED_PERMIT);
				permit.setPermitValue(x);
				ret.add(permit);
			}
		}
		return ret;
	}

	public static void calculateDocumentumACLData(final IDfACL acl, CmfObject<IDfValue> cmfAcl) throws DfException {
		if ((acl == null) || (cmfAcl == null)) { return; }
		final IDfSession session = acl.getSession();
		final String aclId = acl.getObjectId().getId();
		CmfProperty<IDfValue> property = null;
		property = new CmfProperty<IDfValue>(DctmACL.DOCUMENTUM_MARKER, DctmDataType.DF_BOOLEAN.getStoredType());
		property.setValue(DfValueFactory.newBooleanValue(true));

		// Add all the object's attributes...for safekeeping
		/*
		final int attCount = acl.getAttrCount();
		for (int i = 0; i < attCount; i++) {
			final IDfAttr attr = acl.getAttr(i);
			final CmfDataType type = DctmDataType.fromAttribute(attr).getStoredType();
			final CmfProperty<IDfValue> prop = new CmfProperty<IDfValue>(attr.getName(), type, attr.isRepeating());
			AttributeHandler h = DctmAttributeHandlers.getAttributeHandler(DctmObjectType.ACL, attr);
			prop.setValues(h.getExportableValues(acl, attr));
			cmfAcl.setProperty(prop);
		}
		 */

		CmfProperty<IDfValue> accessors = new CmfProperty<IDfValue>(DctmACL.ACCESSORS,
			DctmDataType.DF_STRING.getStoredType(), true);
		CmfProperty<IDfValue> permitTypes = new CmfProperty<IDfValue>(DctmACL.PERMIT_TYPE,
			DctmDataType.DF_INTEGER.getStoredType(), true);
		CmfProperty<IDfValue> permitValues = new CmfProperty<IDfValue>(DctmACL.PERMIT_VALUE,
			DctmDataType.DF_STRING.getStoredType(), true);
		IDfList permits = acl.getPermissions();
		final int permitCount = permits.getCount();
		Set<String> missingAccessors = new HashSet<String>();
		for (int i = 0; i < permitCount; i++) {
			IDfPermit p = IDfPermit.class.cast(permits.get(i));
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
				if (missingAccessors.add(accessor)) {
					DctmCmisACLTools.LOG.warn(String.format(
						"Missing dependency for ACL [%s] - %s [%s] not exported (as ACL accessor)", acl.getObjectId()
							.getId(), (group ? "group" : "user"), accessor));
				}
				continue;
			}

			accessors.addValue(DfValueFactory.newStringValue(DctmMappingUtils.substituteMappableUsers(acl, accessor)));
			permitTypes.addValue(DfValueFactory.newIntValue(p.getPermitType()));
			permitValues.addValue(DfValueFactory.newStringValue(p.getPermitValueString()));
		}
		cmfAcl.setProperty(accessors);
		cmfAcl.setProperty(permitValues);
		cmfAcl.setProperty(permitTypes);

		IDfCollection resultCol = DfUtils.executeQuery(acl.getSession(),
			String.format(DctmCmisACLTools.DQL_FIND_USERS_WITH_DEFAULT_ACL, aclId), IDfQuery.DF_EXECREAD_QUERY);
		try {
			property = new CmfProperty<IDfValue>(DctmACL.USERS_WITH_DEFAULT_ACL, DctmDataType.DF_STRING.getStoredType());
			while (resultCol.next()) {
				IDfValue user = resultCol.getValueAt(0);
				property.addValue(user);
			}
			cmfAcl.setProperty(property);
		} finally {
			DfUtils.closeQuietly(resultCol);
		}
	}

	public static void calculateCmisActions(final IDfACL acl, Collection<CmfProperty<IDfValue>> properties)
		throws DfException, ExportException {
		if (acl == null) { return; }
		final IDfSession session = acl.getSession();

		Set<String> missingAccessors = new HashSet<String>();

		CmfProperty<IDfValue> accessors = new CmfProperty<IDfValue>(IntermediateProperty.ACL_ACCESSOR_NAME,
			DctmDataType.DF_STRING.getStoredType(), true);
		CmfProperty<IDfValue> accessorTypes = new CmfProperty<IDfValue>(IntermediateProperty.ACL_ACCESSOR_TYPE,
			DctmDataType.DF_STRING.getStoredType(), true);
		CmfProperty<IDfValue> accessorActions = new CmfProperty<IDfValue>(IntermediateProperty.ACL_ACCESSOR_ACTIONS,
			DctmDataType.DF_STRING.getStoredType(), true);

		// Now do the CMIS part...
		final int accCount = acl.getValueCount(DctmAttributes.R_ACCESSOR_NAME);
		for (int i = 0; i < accCount; i++) {
			// We're only interested in the basic permissions
			final String accessorName = acl.getRepeatingString(DctmAttributes.R_ACCESSOR_NAME, i);
			final boolean group = acl.getRepeatingBoolean(DctmAttributes.R_IS_GROUP, i);
			final CmfAclActorType accessorType = (group ? CmfAclActorType.GROUP : CmfAclActorType.USER);
			final int accessorPermit = acl.getRepeatingInt(DctmAttributes.R_ACCESSOR_PERMIT, i);
			final String extendedPermits = acl.getAccessorXPermitNames(i);

			IDfPersistentObject o = (group ? session.getGroup(accessorName) : session.getUser(accessorName));
			if ((o == null) && !DctmMappingUtils.SPECIAL_NAMES.contains(accessorName)) {
				// Accessor not there, skip it...
				if (missingAccessors.add(accessorName)) {
					DctmCmisACLTools.LOG.warn(String.format(
						"Missing dependency for ACL [%s] - %s [%s] not exported (as ACL accessor)", acl.getObjectId()
							.getId(), (group ? "group" : "user"), accessorName));
				}
				continue;
			}

			// Set the actor name property
			accessors.addValue(DfValueFactory.newStringValue(accessorName));

			// Set the actor type property
			accessorTypes.addValue(DfValueFactory.newStringValue(accessorType.name()));

			// Comma-concatenate the actions into the actions property
			Set<String> actions = DctmCmisACLTools.calculateActionsForPermissions(accessorPermit, extendedPermits);
			String allActions = FileNameTools.reconstitute(actions, false, false, '|');
			accessorActions.addValue(DfValueFactory.newStringValue(allActions));
		}

		properties.add(accessors);
		properties.add(accessorTypes);
		properties.add(accessorActions);
	}
}