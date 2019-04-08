/**
 *
 */

package com.armedia.caliente.engine.dfc.common;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.chemistry.opencmis.commons.data.PermissionMapping;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.caliente.engine.converter.IntermediateProperty;
import com.armedia.caliente.engine.dfc.DctmAttributes;
import com.armedia.caliente.engine.dfc.DctmDataType;
import com.armedia.caliente.engine.dfc.DctmMappingUtils;
import com.armedia.caliente.engine.tools.AclTools;
import com.armedia.caliente.engine.tools.AclTools.AccessorType;
import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.store.CmfProperty;
import com.armedia.caliente.tools.dfc.DfcUtils;
import com.armedia.caliente.tools.dfc.DfValueFactory;
import com.armedia.commons.utilities.FileNameTools;
import com.armedia.commons.utilities.Tools;
import com.documentum.fc.client.DfPermit;
import com.documentum.fc.client.IDfACL;
import com.documentum.fc.client.IDfGroup;
import com.documentum.fc.client.IDfPermit;
import com.documentum.fc.client.IDfPersistentObject;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.common.DfException;
import com.documentum.fc.common.IDfValue;

/**
 * @author diego
 *
 */
public class DctmCmisACLTools implements DctmACL {

	private static final Logger LOG = LoggerFactory.getLogger(DctmCmisACLTools.class);

	private static enum PermitToAction {
		//
		DF_PERMIT_NONE(IDfACL.DF_PERMIT_NONE),
		DF_PERMIT_BROWSE(
			IDfACL.DF_PERMIT_BROWSE, //
			PermissionMapping.CAN_GET_ACL_OBJECT, //
			PermissionMapping.CAN_GET_CHILDREN_FOLDER, //
			PermissionMapping.CAN_GET_DESCENDENTS_FOLDER, //
			PermissionMapping.CAN_GET_PARENTS_FOLDER, //
			PermissionMapping.CAN_GET_FOLDER_PARENT_OBJECT, //
			PermissionMapping.CAN_GET_OBJECT_RELATIONSHIPS_OBJECT, //
			PermissionMapping.CAN_GET_PROPERTIES_OBJECT //
		),
		DF_PERMIT_READ(
			IDfACL.DF_PERMIT_READ, //
			PermissionMapping.CAN_GET_ALL_VERSIONS_VERSION_SERIES, //
			PermissionMapping.CAN_VIEW_CONTENT_OBJECT //
		),
		DF_PERMIT_RELATE(
			IDfACL.DF_PERMIT_RELATE, //
			PermissionMapping.CAN_CREATE_RELATIONSHIP_SOURCE, //
			PermissionMapping.CAN_CREATE_RELATIONSHIP_TARGET //
		),
		DF_PERMIT_VERSION(
			IDfACL.DF_PERMIT_VERSION, //
			PermissionMapping.CAN_CANCEL_CHECKOUT_DOCUMENT, //
			PermissionMapping.CAN_CHECKIN_DOCUMENT, //
			PermissionMapping.CAN_CHECKOUT_DOCUMENT //
		),
		DF_PERMIT_WRITE(
			IDfACL.DF_PERMIT_WRITE, //
			PermissionMapping.CAN_ADD_TO_FOLDER_OBJECT, //
			PermissionMapping.CAN_CREATE_DOCUMENT_FOLDER, //
			PermissionMapping.CAN_CREATE_FOLDER_FOLDER, //
			PermissionMapping.CAN_DELETE_CONTENT_DOCUMENT, //
			PermissionMapping.CAN_MOVE_OBJECT, //
			PermissionMapping.CAN_MOVE_SOURCE, //
			PermissionMapping.CAN_MOVE_TARGET, //
			PermissionMapping.CAN_REMOVE_FROM_FOLDER_FOLDER, //
			PermissionMapping.CAN_SET_CONTENT_DOCUMENT, //
			PermissionMapping.CAN_UPDATE_PROPERTIES_OBJECT //
		),
		DF_PERMIT_DELETE(
			IDfACL.DF_PERMIT_DELETE, //
			PermissionMapping.CAN_DELETE_OBJECT //
		),
		//
		;

		public final int permit;
		public final Set<String> actions;

		private PermitToAction(int permit, String... actions) {
			DfcUtils.decodeAccessPermission(permit);
			this.permit = permit;
			Set<String> a = new HashSet<>();
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
		Map<Integer, Set<String>> p2a = new HashMap<>();
		Map<String, Integer> a2p = new HashMap<>();
		for (PermitToAction e : PermitToAction.values()) {
			if (e.actions.isEmpty()) {
				continue;
			}
			int permit = e.permit;
			Set<String> s = new HashSet<>();
			s.addAll(e.actions);
			Set<String> oldS = p2a.put(permit, Tools.freezeSet(s));
			if (oldS != null) {
				throw new RuntimeException(String
					.format("Permission [%d] is defined for two sets of Actions: [%s] and [%s]", permit, s, oldS));
			}

			for (String a : s) {
				Integer old = a2p.put(a, permit);
				if (old != null) {
					throw new RuntimeException(
						String.format("Action [%s] is mapped for two Permissions: [%d] and [%d]", a, permit, old));
				}
			}
		}
		PERMIT_TO_ACTION = Tools.freezeMap(p2a);
		ACTION_TO_PERMIT = Tools.freezeMap(a2p);
	}

	private static enum XPermitToAction {
		//
		DF_XPERMIT_CHANGE_FOLDER_LINKS(IDfACL.DF_XPERMIT_CHANGE_FOLDER_LINKS_STR),
		DF_XPERMIT_CHANGE_LOCATION(
			IDfACL.DF_XPERMIT_CHANGE_LOCATION_STR, //
			PermissionMapping.CAN_ADD_TO_FOLDER_OBJECT, //
			PermissionMapping.CAN_MOVE_OBJECT //
		),
		DF_XPERMIT_CHANGE_OWNER(IDfACL.DF_XPERMIT_CHANGE_OWNER_STR),
		DF_XPERMIT_CHANGE_PERMIT(
			IDfACL.DF_XPERMIT_CHANGE_PERMIT_STR, //
			PermissionMapping.CAN_APPLY_ACL_OBJECT //
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
			Set<String> a = new HashSet<>();
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
		Map<String, Set<String>> x2a = new HashMap<>();
		Map<String, String> a2x = new HashMap<>();
		for (XPermitToAction e : XPermitToAction.values()) {
			if (e.actions.isEmpty()) {
				continue;
			}
			String permit = e.permit;
			Set<String> s = new HashSet<>();
			s.addAll(e.actions);
			Set<String> oldS = x2a.put(permit, Tools.freezeSet(s));
			if (oldS != null) {
				throw new RuntimeException(String.format(
					"Extended Permission [%s] is defined for two sets of Actions: [%s] and [%s]", permit, s, oldS));
			}

			for (String a : s) {
				String old = a2x.put(a, permit);
				if (old != null) {
					throw new RuntimeException(String
						.format("Action [%s] is mapped for two Extended Permissions: [%s] and [%s]", permit, a, old));
				}
			}
		}
		XPERMIT_TO_ACTION = Tools.freezeMap(x2a);
		ACTION_TO_XPERMIT = Tools.freezeMap(a2x);
	}

	private static Set<String> calculateActionsForPermissions(int permit, String extended) {
		Set<String> ret = new TreeSet<>();
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

	public static List<IDfPermit> calculatePermissionsFromCMIS(CmfObject<IDfValue> cmisAcl) throws DfException {
		CmfProperty<IDfValue> accessors = cmisAcl.getProperty(IntermediateProperty.ACL_ACCESSOR_NAME);
		CmfProperty<IDfValue> accessorActions = cmisAcl.getProperty(IntermediateProperty.ACL_ACCESSOR_ACTIONS);
		if (accessors == null) {
			throw new DfException(String.format("Failed to find the [%s] property for %s",
				IntermediateProperty.ACL_ACCESSOR_NAME.encode(), cmisAcl.getDescription()));
		}
		if (accessorActions == null) {
			throw new DfException(String.format("Failed to find the [%s] property for %s",
				IntermediateProperty.ACL_ACCESSOR_ACTIONS.encode(), cmisAcl.getDescription()));
		}

		if (accessors.getValueCount() != accessorActions.getValueCount()) {
			throw new DfException(String.format("Value count mismatches for %s (accessors=%d | actions=%d)",
				cmisAcl.getDescription(), accessors.getValueCount(), accessorActions.getValueCount()));
		}

		// Ok...we have the triplets, so we start walking...
		List<IDfPermit> ret = new ArrayList<>();
		for (int i = 0; i < accessors.getValueCount(); i++) {
			final String accessorName = accessors.getValue(i).asString();

			int perm = IDfACL.DF_PERMIT_NONE;
			Set<String> extended = new TreeSet<>();
			Set<String> actions = AclTools.decode(accessorActions.getValue(i).asString());
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
			permit.setAccessorName(accessorName);
			permit.setPermitType(IDfPermit.DF_ACCESS_PERMIT);
			permit.setPermitValue(DfcUtils.decodeAccessPermission(perm));
			ret.add(permit);

			for (String x : extended) {
				permit = new DfPermit();
				permit.setAccessorName(accessorName);
				permit.setPermitType(IDfPermit.DF_EXTENDED_PERMIT);
				permit.setPermitValue(x);
				ret.add(permit);
			}
		}
		return ret;
	}

	public static void calculateCmisActions(final IDfSession session, final IDfACL acl,
		Collection<CmfProperty<IDfValue>> properties) throws DfException {
		if (acl == null) { return; }

		Set<String> missingAccessors = new HashSet<>();

		CmfProperty<IDfValue> accessors = new CmfProperty<>(IntermediateProperty.ACL_ACCESSOR_NAME,
			DctmDataType.DF_STRING.getStoredType(), true);
		CmfProperty<IDfValue> accessorTypes = new CmfProperty<>(IntermediateProperty.ACL_ACCESSOR_TYPE,
			DctmDataType.DF_STRING.getStoredType(), true);
		CmfProperty<IDfValue> accessorActions = new CmfProperty<>(IntermediateProperty.ACL_ACCESSOR_ACTIONS,
			DctmDataType.DF_STRING.getStoredType(), true);

		// Now do the CMIS part...
		final int accCount = acl.getValueCount(DctmAttributes.R_ACCESSOR_NAME);
		for (int i = 0; i < accCount; i++) {
			// We're only interested in the basic permissions
			final String accessorName = acl.getRepeatingString(DctmAttributes.R_ACCESSOR_NAME, i);
			final boolean group = acl.getRepeatingBoolean(DctmAttributes.R_IS_GROUP, i);
			final int accessorPermit = acl.getRepeatingInt(DctmAttributes.R_ACCESSOR_PERMIT, i);
			final String extendedPermits = acl.getAccessorXPermitNames(i);

			IDfPersistentObject o = (group ? session.getGroup(accessorName) : session.getUser(accessorName));
			if (o == null) {
				// Accessor not there, skip it...
				// But only warn if it's not a "special name"
				if (missingAccessors.add(accessorName) && !DctmMappingUtils.SPECIAL_NAMES.contains(accessorName)) {
					DctmCmisACLTools.LOG.warn(
						"Missing dependency for ACL [{}] - {} [{}] not exported (as ACL accessor)",
						acl.getObjectId().getId(), (group ? "group" : "user"), accessorName);

				}
				continue;
			}

			// Set the actor name property
			accessors.addValue(DfValueFactory.of(accessorName));
			final AccessorType type;
			if (group) {
				IDfGroup g = IDfGroup.class.cast(o);
				if (g.getGroupClass().indexOf("role") < 0) {
					type = AccessorType.GROUP;
				} else {
					type = AccessorType.ROLE;
				}
			} else {
				type = AccessorType.USER;
			}
			accessorTypes.addValue(DfValueFactory.of(type.name()));

			// Comma-concatenate the actions into the actions property
			Set<String> actions = DctmCmisACLTools.calculateActionsForPermissions(accessorPermit, extendedPermits);
			String allActions = AclTools.encode(actions);
			accessorActions.addValue(DfValueFactory.of(allActions));
		}

		properties.add(accessors);
		properties.add(accessorTypes);
		properties.add(accessorActions);
	}
}