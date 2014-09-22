package com.delta.cmsmf.cms;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.documentum.fc.client.IDfSession;
import com.documentum.fc.client.IDfTypedObject;
import com.documentum.fc.common.DfException;
import com.documentum.fc.common.IDfAttr;
import com.documentum.fc.common.IDfValue;

public class CmsMappingUtils {

	public static final Set<String> SPECIAL_NAMES;
	static {
		Set<String> s = new HashSet<String>();
		s.add("dm_owner");
		s.add("dm_group");
		s.add("dm_world");
		SPECIAL_NAMES = Collections.unmodifiableSet(s);
	}

	private static final Pattern SUBSTITUTION = Pattern.compile("^\\$\\{([\\w]+)\\}$");

	// TODO: Make this configurable via a configuration setting/CLI parameter
	private static final String[] SUBSTITUTION_ATTRIBUTES = {
		// DO NOT modify this order...this is CRITICAL!
		CmsAttributes.R_CREATOR_NAME, CmsAttributes.R_INSTALL_OWNER, CmsAttributes.OPERATOR_NAME,
		CmsAttributes.OWNER_NAME
	};

	public static List<IDfValue> substituteMappableUsers(IDfTypedObject object, IDfAttr attr) throws DfException {
		if (object == null) { throw new IllegalArgumentException("Must provide an object to get the session from"); }
		if (attr == null) { throw new IllegalArgumentException("Must provide an attribute to expand"); }
		return CmsMappingUtils.substituteMappableUsers(object, DfValueFactory.getAllRepeatingValues(attr, object));
	}

	public static String substituteMappableUsers(IDfTypedObject object, String user) throws DfException {
		if (user == null) { throw new IllegalArgumentException("Must provide a user to substitute"); }
		return CmsMappingUtils.substituteMappableUsers(object, DfValueFactory.newStringValue(user)).asString();
	}

	public static IDfValue substituteMappableUsers(IDfTypedObject object, IDfValue value) throws DfException {
		if (value == null) { throw new IllegalArgumentException("Must provide a value to substitute"); }
		return CmsMappingUtils.substituteMappableUsers(object, Collections.singleton(value)).get(0);
	}

	public static List<IDfValue> substituteMappableUsers(IDfTypedObject object, Collection<IDfValue> values)
		throws DfException {
		if (object == null) { throw new IllegalArgumentException("Must provide an object to get the session from"); }
		return CmsMappingUtils.substituteMappableUsers(object.getSession(), values);
	}

	public static String substituteMappableUsers(IDfSession session, String user) throws DfException {
		if (user == null) { throw new IllegalArgumentException("Must provide a user to substitute"); }
		return CmsMappingUtils.substituteMappableUsers(session, DfValueFactory.newStringValue(user)).asString();
	}

	public static IDfValue substituteMappableUsers(IDfSession session, IDfValue value) throws DfException {
		if (value == null) { throw new IllegalArgumentException("Must provide a value to substitute"); }
		return CmsMappingUtils.substituteMappableUsers(session, Collections.singleton(value)).get(0);
	}

	public static List<IDfValue> substituteMappableUsers(IDfSession session, Collection<IDfValue> values)
		throws DfException {
		if (session == null) { throw new IllegalArgumentException(
			"Must provide a session to calculate the mappings from"); }
		if (values == null) { throw new IllegalArgumentException("Must provide a collection of values to expand"); }
		if (values.isEmpty()) { return new ArrayList<IDfValue>(); }
		IDfTypedObject[] srcObjects = CmsMappingUtils.getSources(session);
		List<IDfValue> ret = new ArrayList<IDfValue>(values.size());
		Map<String, String> valueMap = new HashMap<String, String>();
		for (String serverAttribute : CmsMappingUtils.SUBSTITUTION_ATTRIBUTES) {
			for (IDfTypedObject src : srcObjects) {
				int idx = src.findAttrIndex(serverAttribute);
				if (idx < 0) {
					continue;
				}
				final String key = src.getString(serverAttribute);
				if (!valueMap.containsKey(key)) {
					// Avoid duplicates - we already have a higher-priority mapping
					valueMap.put(key, serverAttribute);
				}
			}
		}
		for (IDfValue value : values) {
			String mapping = valueMap.get(value.asString());
			if (mapping != null) {
				value = DfValueFactory.newStringValue(String.format("${%s}", mapping));
			}
			ret.add(value);
		}
		return ret;
	}

	public static List<IDfValue> resolveMappableUsers(IDfTypedObject object, CmsProperty property) throws DfException {
		if (object == null) { throw new IllegalArgumentException("Must provide an object to get the session from"); }
		if (property == null) { throw new IllegalArgumentException("Must provide a property to expand"); }
		return CmsMappingUtils.resolveMappableUsers(object, property.getValues());
	}

	private static IDfTypedObject[] getSources(IDfTypedObject src) throws DfException {
		if (src == null) { throw new IllegalArgumentException("Must provide an object to get the session from"); }
		return CmsMappingUtils.getSources(src.getSession());
	}

	private static IDfTypedObject[] getSources(IDfSession session) throws DfException {
		if (session == null) { throw new IllegalArgumentException("Must provide a session to get the sources from"); }
		// Always add the sources in priority order
		return new IDfTypedObject[] {
			session.getDocbaseConfig(), session.getServerConfig()
		};
	}

	public static List<IDfValue> resolveMappableUsers(IDfTypedObject object, Collection<IDfValue> values)
		throws DfException {
		if (object == null) { throw new IllegalArgumentException("Must provide an object to get the session from"); }
		if (values == null) { throw new IllegalArgumentException("Must provide a collection of values to expand"); }
		if (values.isEmpty()) { return new ArrayList<IDfValue>(); }
		IDfTypedObject[] srcObjects = null;
		List<IDfValue> ret = new ArrayList<IDfValue>(values.size());
		outer: for (IDfValue oldValue : values) {
			Matcher m = CmsMappingUtils.SUBSTITUTION.matcher(oldValue.asString());
			if (m.matches()) {
				final String serverAttribute = m.group(1);
				if (srcObjects == null) {
					// We delay this until we actually need it, to reduce performance hits
					srcObjects = CmsMappingUtils.getSources(object);
				}
				inner: for (IDfTypedObject src : srcObjects) {
					int idx = src.findAttrIndex(serverAttribute);
					if (idx < 0) {
						continue inner;
					}
					ret.add(src.getValue(serverAttribute));
					continue outer;
				}
			}
			// Mapping failed, or no mapping found
			ret.add(oldValue);
		}
		return ret;
	}

	public static String resolveMappableUser(IDfSession session, String user) throws DfException {
		if (session == null) { throw new IllegalArgumentException("Must provide a session to resolve through"); }
		if (user == null) { throw new IllegalArgumentException("Must provide a username to resolve"); }
		Matcher m = CmsMappingUtils.SUBSTITUTION.matcher(user);
		if (m.matches()) {
			IDfTypedObject[] srcObjects = CmsMappingUtils.getSources(session);
			final String serverAttribute = m.group(1);
			for (IDfTypedObject src : srcObjects) {
				int idx = src.findAttrIndex(serverAttribute);
				if (idx < 0) {
					continue;
				}
				return src.getString(serverAttribute);
			}
		}
		return user;
	}

	public static String resolveMappableUser(IDfTypedObject object, String user) throws DfException {
		if (object == null) { throw new IllegalArgumentException("Must provide an object to get the session from"); }
		return CmsMappingUtils.resolveMappableUser(object.getSession(), user);
	}

	public static boolean isMappableUser(IDfSession session, String user) throws DfException {
		if (session == null) { throw new IllegalArgumentException("Must provide a session to analyze with"); }
		if (user == null) { throw new IllegalArgumentException("Must provide a username to analyze"); }
		IDfTypedObject[] srcObjects = CmsMappingUtils.getSources(session);
		Map<String, String> valueMap = new HashMap<String, String>();
		for (String serverAttribute : CmsMappingUtils.SUBSTITUTION_ATTRIBUTES) {
			for (IDfTypedObject src : srcObjects) {
				int idx = src.findAttrIndex(serverAttribute);
				if (idx < 0) {
					continue;
				}
				valueMap.put(src.getString(serverAttribute), serverAttribute);
			}
		}
		return (valueMap.get(user) != null);
	}

	public static boolean isSubstitutionForMappableUser(String user) throws DfException {
		if (user == null) { throw new IllegalArgumentException("Must provide a username to analyze"); }
		return CmsMappingUtils.SUBSTITUTION.matcher(user).matches();
	}
}