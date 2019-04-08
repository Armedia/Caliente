package com.armedia.caliente.engine.dfc;

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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.caliente.engine.dfc.common.DctmACL;
import com.armedia.caliente.store.CmfProperty;
import com.armedia.caliente.tools.dfc.DfValueFactory;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.client.IDfTypedObject;
import com.documentum.fc.common.DfException;
import com.documentum.fc.common.IDfAttr;
import com.documentum.fc.common.IDfValue;

public class DctmMappingUtils {

	private static final Logger LOG = LoggerFactory.getLogger(DctmMappingUtils.class);

	public static final Set<String> SPECIAL_NAMES;
	static {
		Set<String> s = new HashSet<>();
		s.add(DctmACL.DM_OWNER);
		s.add(DctmACL.DM_GROUP);
		s.add(DctmACL.DM_WORLD);
		SPECIAL_NAMES = Collections.unmodifiableSet(s);
	}

	private static final Pattern SUBSTITUTION = Pattern.compile("^\\$\\{([\\w]+)\\}$");
	private static final Map<String, Map<String, IDfValue>> FWD_MAPPINGS = new HashMap<>();
	private static final Map<String, Map<String, IDfValue>> REV_MAPPINGS = new HashMap<>();

	// TODO: Make this configurable via a configuration setting/CLI parameter
	private static final String[] SUBSTITUTION_ATTRIBUTES = {
		// DO NOT modify this order...this is CRITICAL!
		DctmAttributes.R_CREATOR_NAME, DctmAttributes.R_INSTALL_OWNER, DctmAttributes.OPERATOR_NAME,
		DctmAttributes.OWNER_NAME
	};

	private static IDfTypedObject[] getSources(IDfSession session) throws DfException {
		if (session == null) {
			throw new IllegalArgumentException("Must provide a session to get the nameSources from");
		}
		// Always add the nameSources in priority order
		return new IDfTypedObject[] {
			session.getDocbaseConfig(), session.getServerConfig()
		};
	}

	private static Map<String, IDfValue> getMappings(boolean returnForward, IDfSession session) throws DfException {
		String docbase = session.getDocbaseScope();
		Map<String, IDfValue> forward = DctmMappingUtils.FWD_MAPPINGS.get(docbase);
		Map<String, IDfValue> reverse = DctmMappingUtils.REV_MAPPINGS.get(docbase);
		if ((forward == null) || (reverse == null)) {
			synchronized (DctmMappingUtils.class) {
				forward = DctmMappingUtils.FWD_MAPPINGS.get(docbase);
				reverse = DctmMappingUtils.REV_MAPPINGS.get(docbase);
				if ((forward == null) || (reverse == null)) {
					forward = new HashMap<>();
					reverse = new HashMap<>();
					for (IDfTypedObject src : DctmMappingUtils.getSources(session)) {
						for (String serverAttribute : DctmMappingUtils.SUBSTITUTION_ATTRIBUTES) {
							int idx = src.findAttrIndex(serverAttribute);
							if (idx < 0) {
								continue;
							}
							if (!forward.containsKey(serverAttribute)) {
								final IDfValue value = src.getValue(serverAttribute);
								final IDfValue substitution = DfValueFactory
									.of(String.format("${%s}", serverAttribute));
								forward.put(substitution.asString(), value);
								reverse.put(value.asString(), substitution);
							}
						}
					}
					DctmMappingUtils.FWD_MAPPINGS.put(docbase, Collections.unmodifiableMap(forward));
					DctmMappingUtils.REV_MAPPINGS.put(docbase, Collections.unmodifiableMap(reverse));
					DctmMappingUtils.LOG.info("User Mapping Substitutions configured for [{}]: {}", docbase, reverse);
					DctmMappingUtils.LOG.info("User Mapping Resolutions configured for [{}]: {}", docbase, forward);
				}
			}
		}
		return (returnForward ? forward : reverse);
	}

	private static Map<String, IDfValue> getSubstitutionMappings(IDfSession session) throws DfException {
		return DctmMappingUtils.getMappings(false, session);
	}

	private static Map<String, IDfValue> getResolutionMappings(IDfSession session) throws DfException {
		return DctmMappingUtils.getMappings(true, session);
	}

	public static List<IDfValue> substituteMappableUsers(IDfTypedObject object, IDfAttr attr) throws DfException {
		if (object == null) { throw new IllegalArgumentException("Must provide an object to get the session from"); }
		if (attr == null) { throw new IllegalArgumentException("Must provide an attribute to expand"); }
		return DctmMappingUtils.substituteMappableUsers(object, DfValueFactory.getAllValues(attr, object));
	}

	public static String substituteMappableUsers(IDfTypedObject object, String user) throws DfException {
		if (user == null) { throw new IllegalArgumentException("Must provide a user to substitute"); }
		return DctmMappingUtils.substituteMappableUsers(object, DfValueFactory.of(user)).asString();
	}

	public static IDfValue substituteMappableUsers(IDfTypedObject object, IDfValue value) throws DfException {
		if (value == null) { throw new IllegalArgumentException("Must provide a value to substitute"); }
		return DctmMappingUtils.substituteMappableUsers(object, Collections.singleton(value)).get(0);
	}

	public static List<IDfValue> substituteMappableUsers(IDfTypedObject object, Collection<IDfValue> values)
		throws DfException {
		if (object == null) { throw new IllegalArgumentException("Must provide an object to get the session from"); }
		return DctmMappingUtils.substituteMappableUsers(object.getSession(), values);
	}

	public static String substituteMappableUsers(IDfSession session, String user) throws DfException {
		if (user == null) { throw new IllegalArgumentException("Must provide a user to substitute"); }
		return DctmMappingUtils.substituteMappableUsers(session, DfValueFactory.of(user)).asString();
	}

	public static IDfValue substituteMappableUsers(IDfSession session, IDfValue value) throws DfException {
		if (value == null) { throw new IllegalArgumentException("Must provide a value to substitute"); }
		return DctmMappingUtils.substituteMappableUsers(session, Collections.singleton(value)).get(0);
	}

	public static List<IDfValue> substituteMappableUsers(IDfSession session, Collection<IDfValue> values)
		throws DfException {
		if (session == null) {
			throw new IllegalArgumentException("Must provide a session to calculate the mappings from");
		}
		if (values == null) { throw new IllegalArgumentException("Must provide a collection of values to expand"); }
		if (values.isEmpty()) { return new ArrayList<>(); }
		List<IDfValue> ret = new ArrayList<>(values.size());
		// These are the mappings indexed by username
		Map<String, IDfValue> substitutions = DctmMappingUtils.getSubstitutionMappings(session);
		for (IDfValue user : values) {
			IDfValue substitution = substitutions.get(user.asString());
			if (substitution != null) {
				DctmMappingUtils.LOG.info("Substituted user [{}] as {}", user.asString(), substitution.asString());
				user = substitution;
			}
			ret.add(user);
		}
		return ret;
	}

	public static List<IDfValue> resolveMappableUsers(IDfTypedObject object, CmfProperty<IDfValue> property)
		throws DfException {
		if (object == null) { throw new IllegalArgumentException("Must provide an object to get the session from"); }
		if (property == null) { throw new IllegalArgumentException("Must provide a property to expand"); }
		return DctmMappingUtils.resolveMappableUsers(object, property.getValues());
	}

	public static List<IDfValue> resolveMappableUsers(IDfTypedObject object, Collection<IDfValue> values)
		throws DfException {
		if (object == null) { throw new IllegalArgumentException("Must provide an object to get the session from"); }
		if (values == null) { throw new IllegalArgumentException("Must provide a collection of values to expand"); }
		if (values.isEmpty()) { return new ArrayList<>(); }
		Map<String, IDfValue> resolutions = null;
		List<IDfValue> ret = new ArrayList<>(values.size());
		for (IDfValue oldValue : values) {
			Matcher m = DctmMappingUtils.SUBSTITUTION.matcher(oldValue.asString());
			if (m.matches()) {
				if (resolutions == null) {
					// We delay this until we actually need it, to reduce performance hits
					resolutions = DctmMappingUtils.getResolutionMappings(object.getSession());
				}
				IDfValue actual = resolutions.get(oldValue.asString());
				if (actual != null) {
					DctmMappingUtils.LOG.info("Resolved user {} as [{}]", oldValue.asString(), actual.asString());
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
		return DctmMappingUtils.resolveMappableUser(object.getSession(), user);
	}

	public static String resolveMappableUser(IDfSession session, String user) throws DfException {
		if (session == null) { throw new IllegalArgumentException("Must provide a session to resolve through"); }
		if (user == null) { throw new IllegalArgumentException("Must provide a username to resolve"); }
		Matcher m = DctmMappingUtils.SUBSTITUTION.matcher(user);
		if (m.matches()) {
			// We delay this until we actually need it, to reduce performance hits
			Map<String, IDfValue> resolutions = DctmMappingUtils.getResolutionMappings(session);
			IDfValue actual = resolutions.get(user);
			if (actual != null) {
				DctmMappingUtils.LOG.info("Resolved user {} as [{}]", user, actual.asString());
				return actual.asString();
			}
		}
		return user;
	}

	public static boolean isMappableUser(IDfSession session, String user) throws DfException {
		if (session == null) { throw new IllegalArgumentException("Must provide a session to analyze with"); }
		if (user == null) { throw new IllegalArgumentException("Must provide a username to analyze"); }
		return DctmMappingUtils.getSubstitutionMappings(session).containsKey(user);
	}

	public static boolean isSubstitutionForMappableUser(String user) throws DfException {
		if (user == null) { throw new IllegalArgumentException("Must provide a username to analyze"); }
		return DctmMappingUtils.SUBSTITUTION.matcher(user).matches();
	}
}