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

import org.apache.log4j.Logger;

import com.documentum.fc.client.IDfSession;
import com.documentum.fc.client.IDfTypedObject;
import com.documentum.fc.common.DfException;
import com.documentum.fc.common.IDfAttr;
import com.documentum.fc.common.IDfValue;

public class CmsMappingUtils {

	private static final Logger LOG = Logger.getLogger(CmsMappingUtils.class);

	public static final Set<String> SPECIAL_NAMES;
	static {
		Set<String> s = new HashSet<String>();
		s.add("dm_owner");
		s.add("dm_group");
		s.add("dm_world");
		SPECIAL_NAMES = Collections.unmodifiableSet(s);
	}

	private static final Pattern SUBSTITUTION = Pattern.compile("^\\$\\{([\\w]+)\\}$");
	private static final Map<String, Map<String, IDfValue>> FWD_MAPPINGS = new HashMap<String, Map<String, IDfValue>>();
	private static final Map<String, Map<String, IDfValue>> REV_MAPPINGS = new HashMap<String, Map<String, IDfValue>>();

	// TODO: Make this configurable via a configuration setting/CLI parameter
	private static final String[] SUBSTITUTION_ATTRIBUTES = {
		// DO NOT modify this order...this is CRITICAL!
		CmsAttributes.R_CREATOR_NAME, CmsAttributes.R_INSTALL_OWNER, CmsAttributes.OPERATOR_NAME,
		CmsAttributes.OWNER_NAME
	};

	private static IDfTypedObject[] getSources(IDfSession session) throws DfException {
		if (session == null) { throw new IllegalArgumentException("Must provide a session to get the sources from"); }
		// Always add the sources in priority order
		return new IDfTypedObject[] {
			session.getDocbaseConfig(), session.getServerConfig()
		};
	}

	private static Map<String, IDfValue> getMappings(boolean returnForward, IDfSession session) throws DfException {
		String docbase = session.getDocbaseScope();
		Map<String, IDfValue> forward = CmsMappingUtils.FWD_MAPPINGS.get(docbase);
		Map<String, IDfValue> reverse = CmsMappingUtils.REV_MAPPINGS.get(docbase);
		if ((forward == null) || (reverse == null)) {
			synchronized (CmsMappingUtils.class) {
				forward = CmsMappingUtils.FWD_MAPPINGS.get(docbase);
				reverse = CmsMappingUtils.REV_MAPPINGS.get(docbase);
				if ((forward == null) || (reverse == null)) {
					forward = new HashMap<String, IDfValue>();
					reverse = new HashMap<String, IDfValue>();
					for (IDfTypedObject src : CmsMappingUtils.getSources(session)) {
						for (String serverAttribute : CmsMappingUtils.SUBSTITUTION_ATTRIBUTES) {
							int idx = src.findAttrIndex(serverAttribute);
							if (idx < 0) {
								continue;
							}
							if (!forward.containsKey(serverAttribute)) {
								final IDfValue value = src.getValue(serverAttribute);
								final IDfValue substitution = DfValueFactory.newStringValue(String.format("${%s}",
									serverAttribute));
								forward.put(substitution.asString(), value);
								reverse.put(value.asString(), substitution);
							}
						}
					}
					CmsMappingUtils.FWD_MAPPINGS.put(docbase, Collections.unmodifiableMap(forward));
					CmsMappingUtils.REV_MAPPINGS.put(docbase, Collections.unmodifiableMap(reverse));
					CmsMappingUtils.LOG.info(String.format("User Mapping Substitutions configured for [%s]: %s",
						docbase, reverse));
					CmsMappingUtils.LOG.info(String.format("User Mapping Resolutions configured for [%s]: %s", docbase,
						forward));
				}
			}
		}
		return (returnForward ? forward : reverse);
	}

	private static Map<String, IDfValue> getSubstitutionMappings(IDfSession session) throws DfException {
		return CmsMappingUtils.getMappings(false, session);
	}

	private static Map<String, IDfValue> getResolutionMappings(IDfSession session) throws DfException {
		return CmsMappingUtils.getMappings(true, session);
	}

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
		List<IDfValue> ret = new ArrayList<IDfValue>(values.size());
		// These are the mappings indexed by username
		Map<String, IDfValue> substitutions = CmsMappingUtils.getSubstitutionMappings(session);
		for (IDfValue user : values) {
			IDfValue substitution = substitutions.get(user.asString());
			if (substitution != null) {
				CmsMappingUtils.LOG.info(String.format("Substituted user [%s] as %s", user.asString(),
					substitution.asString()));
				user = substitution;
			}
			ret.add(user);
		}
		return ret;
	}

	public static List<IDfValue> resolveMappableUsers(IDfTypedObject object, CmsProperty property) throws DfException {
		if (object == null) { throw new IllegalArgumentException("Must provide an object to get the session from"); }
		if (property == null) { throw new IllegalArgumentException("Must provide a property to expand"); }
		return CmsMappingUtils.resolveMappableUsers(object, property.getValues());
	}

	public static List<IDfValue> resolveMappableUsers(IDfTypedObject object, Collection<IDfValue> values)
		throws DfException {
		if (object == null) { throw new IllegalArgumentException("Must provide an object to get the session from"); }
		if (values == null) { throw new IllegalArgumentException("Must provide a collection of values to expand"); }
		if (values.isEmpty()) { return new ArrayList<IDfValue>(); }
		Map<String, IDfValue> resolutions = null;
		List<IDfValue> ret = new ArrayList<IDfValue>(values.size());
		for (IDfValue oldValue : values) {
			Matcher m = CmsMappingUtils.SUBSTITUTION.matcher(oldValue.asString());
			if (m.matches()) {
				if (resolutions == null) {
					// We delay this until we actually need it, to reduce performance hits
					resolutions = CmsMappingUtils.getResolutionMappings(object.getSession());
				}
				IDfValue actual = resolutions.get(oldValue.asString());
				if (actual != null) {
					CmsMappingUtils.LOG.info(String.format("Resolved user %s as [%s]", oldValue.asString(),
						actual.asString()));
					oldValue = actual;
				}
			}
			// Mapping failed, or no mapping found
			ret.add(oldValue);
		}
		return ret;
	}

	public static String resolveMappableUser(IDfTypedObject object, String user) throws DfException {
		if (object == null) { throw new IllegalArgumentException("Must provide an object to get the session from"); }
		return CmsMappingUtils.resolveMappableUser(object.getSession(), user);
	}

	public static String resolveMappableUser(IDfSession session, String user) throws DfException {
		if (session == null) { throw new IllegalArgumentException("Must provide a session to resolve through"); }
		if (user == null) { throw new IllegalArgumentException("Must provide a username to resolve"); }
		Matcher m = CmsMappingUtils.SUBSTITUTION.matcher(user);
		if (m.matches()) {
			// We delay this until we actually need it, to reduce performance hits
			Map<String, IDfValue> resolutions = CmsMappingUtils.getResolutionMappings(session);
			IDfValue actual = resolutions.get(user);
			if (actual != null) {
				CmsMappingUtils.LOG.info(String.format("Resolved user %s as [%s]", user, actual.asString()));
				return actual.asString();
			}
		}
		return user;
	}

	public static boolean isMappableUser(IDfSession session, String user) throws DfException {
		if (session == null) { throw new IllegalArgumentException("Must provide a session to analyze with"); }
		if (user == null) { throw new IllegalArgumentException("Must provide a username to analyze"); }
		return CmsMappingUtils.getResolutionMappings(session).containsKey(user);
	}

	public static boolean isSubstitutionForMappableUser(String user) throws DfException {
		if (user == null) { throw new IllegalArgumentException("Must provide a username to analyze"); }
		return CmsMappingUtils.SUBSTITUTION.matcher(user).matches();
	}
}