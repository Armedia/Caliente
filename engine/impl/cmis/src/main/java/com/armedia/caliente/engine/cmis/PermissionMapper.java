package com.armedia.caliente.engine.cmis;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.chemistry.opencmis.client.api.Session;
import org.apache.chemistry.opencmis.commons.data.PermissionMapping;
import org.apache.chemistry.opencmis.commons.data.RepositoryInfo;
import org.apache.chemistry.opencmis.commons.definitions.PermissionDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.commons.utilities.Tools;

public class PermissionMapper {

	private class PriorityKey implements Comparable<PriorityKey> {
		/* How many actions are granted beyond the requested set */
		private final Integer extra;

		/* How many actions are granted total */
		private final Integer total;

		/* The name of the permission */
		private final String permission;

		private PriorityKey(Integer extra, Integer total, String permission) {
			this.extra = extra;
			this.total = total;
			this.permission = permission;
		}

		@Override
		public int hashCode() {
			return Tools.hashTool(this, null, this.extra, this.total, this.permission);
		}

		@Override
		public boolean equals(Object obj) {
			if (!Tools.baseEquals(this, obj)) { return false; }
			PriorityKey other = PriorityKey.class.cast(obj);
			if (!Tools.equals(this.extra, other.extra)) { return false; }
			if (!Tools.equals(this.total, other.total)) { return false; }
			if (!Tools.equals(this.permission, other.permission)) { return false; }
			return true;
		}

		@Override
		public int compareTo(PriorityKey o) {
			int r = 0;
			r = this.extra.compareTo(o.extra);
			if (r != 0) { return r; }
			r = this.total.compareTo(o.total);
			if (r != 0) { return r; }
			return this.permission.compareTo(o.permission);
		}

		@Override
		public String toString() {
			return String.format("PriorityKey [extra=%s, total=%s, permission=%s, actions=%s]", this.extra, this.total,
				this.permission, PermissionMapper.this.permissionsToActions.get(this.permission));
		}
	}

	private final Logger log = LoggerFactory.getLogger(getClass());

	private final Map<String, Set<String>> permissionsToActions;
	private final Map<String, Set<String>> actionsToPermissions;

	public PermissionMapper(Session session) {
		final RepositoryInfo info = session.getRepositoryInfo();
		Map<String, Set<String>> permissionsToActions = new TreeMap<>();
		Map<String, Set<String>> actionsToPermissions = new TreeMap<>();
		Map<String, PermissionMapping> m = info.getAclCapabilities().getPermissionMapping();
		final Set<String> empty = Collections.emptySet();

		Set<String> permissions = new HashSet<>();
		for (String action : m.keySet()) {
			PermissionMapping mapping = m.get(action);
			Set<String> p = new TreeSet<>();
			for (String permission : mapping.getPermissions()) {
				p.add(permission);
				permissions.add(permission);
				Set<String> s = permissionsToActions.get(permission);
				if (s == null) {
					s = new TreeSet<>();
					permissionsToActions.put(permission, s);
				}
				s.add(action);
			}
			if (p.isEmpty()) {
				// WARNING!!! The action requested can't be mapped to any permissions!!
				this.log.warn("Allowable Action [{}] is not granted by any existing permissions", action);
				actionsToPermissions.put(action, empty);
			} else {
				actionsToPermissions.put(action, Tools.freezeSet(new LinkedHashSet<>(p)));
			}
		}

		// Use Linked* to preserve order, but get better performance
		for (String p : permissions) {
			Set<String> s = permissionsToActions.get(p);
			permissionsToActions.put(p, Tools.freezeSet(new LinkedHashSet<>(s)));
		}

		// Make sure all permissions have "some" mapping - even if it's an empty mapping
		for (PermissionDefinition p : info.getAclCapabilities().getPermissions()) {
			if (permissionsToActions.containsKey(p.getId())) {
				continue;
			}
			// WARNING!!! The action requested can't be mapped to any permissions!!
			this.log.warn("Permission [{}] grants no Allowable Actions", p.getId());
			permissionsToActions.put(p.getId(), empty);
		}
		this.permissionsToActions = Tools.freezeMap(new LinkedHashMap<>(permissionsToActions));

		// This only works in OpenCMIS...but will do for now
		// Make sure all actions have "some" mapping - even if it's an empty mapping
		for (Field f : PermissionMapping.class.getDeclaredFields()) {
			if (Modifier.isStatic(f.getModifiers()) && (f.getType() == String.class)) {
				Object v;
				try {
					v = f.get(null);
				} catch (Exception e) {
					throw new RuntimeException("Failed to initialize the actions-to-permissions mappings", e);
				}
				if (v == null) {
					continue;
				}
				if (actionsToPermissions.containsKey(v.toString())) {
					continue;
				}
				this.log.warn("Allowable Action [{}] is not granted by any existing permissions", v.toString());
				actionsToPermissions.put(v.toString(), empty);
			}
		}
		this.actionsToPermissions = Tools.freezeMap(new LinkedHashMap<>(actionsToPermissions));
	}

	/**
	 * <p>
	 * Returns the list of allowable actions that the given permission implies, or an empty list if
	 * the given permission doesn't translate into any allowable actions.
	 * </p>
	 *
	 * @param permission
	 * @return the list of allowable actions that the given permission implies
	 */
	public Set<String> convertPermissionToAllowableActions(String permission) {
		if (permission == null) { throw new IllegalArgumentException("Must provide a non-null permission"); }
		Set<String> ret = this.permissionsToActions.get(permission);
		if (ret == null) {
			throw new IllegalArgumentException(String.format("Unsupported permission [%s]", permission));
		}
		return ret;
	}

	/**
	 * <p>
	 * Converts the set of permissions that, when aggregated, grant the set of allowable actions
	 * that most closely matches the given collection of action names.
	 * </p>
	 *
	 * @param actions
	 * @return a list containing permission names
	 */
	public Set<String> convertAllowableActionsToPermissions(Collection<String> actions) {
		if (actions == null) { throw new IllegalArgumentException("Must provide a non-null collection of actions"); }
		// For every action:
		// find the permissions that allow that action
		// for each permission
		// find the actions that permission allows
		// select the permission that allows the fewest extra actions that aren't included
		// in the original set of actions
		actions = new TreeSet<>(actions);
		Set<String> ret = new TreeSet<>();
		for (String a : actions) {

			// First, find the permissions that may allow that action to be performed
			Set<String> permissions = this.actionsToPermissions.get(a);
			if (permissions == null) {
				throw new IllegalArgumentException(String.format("The action [%s] is not a valid allowable action", a));
			}

			if (permissions.isEmpty()) {
				continue;
			}

			// For each permission, index them based on their PriorityKey
			Set<PriorityKey> sortedPermissions = new TreeSet<>();
			for (String p : permissions) {
				Set<String> permissionActions = this.permissionsToActions.get(p);
				int extra = 0;
				int match = 0;
				for (String pa : permissionActions) {
					if (!actions.contains(pa)) {
						extra++;
					} else {
						match++;
					}
				}
				if (match == 0) {
					// This should be impossible, but still...
					continue;
				}
				sortedPermissions.add(new PriorityKey(extra, extra + match, p));
			}

			if (sortedPermissions.isEmpty()) {
				// This should be impossible, but still...
				continue;
			}

			// Now, add the permission with the highest priority to the return set
			ret.add(sortedPermissions.iterator().next().permission);
		}
		return ret;
	}
}