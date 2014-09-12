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

	private static final String[] SUBSTITUTION_ATTRIBUTES = {
		// DO NOT modify this order...this is CRITICAL!
		CmsAttributes.R_INSTALL_OWNER, CmsAttributes.OWNER_NAME, CmsAttributes.OPERATOR_NAME
	};

	public static Collection<IDfValue> substituteSpecialUsers(IDfTypedObject object, IDfAttr attr) throws DfException {
		IDfTypedObject serverConfig = object.getSession().getServerConfig();
		final int count = object.getValueCount(attr.getName());
		List<IDfValue> ret = new ArrayList<IDfValue>(count);
		Map<String, String> valueMap = new HashMap<String, String>();
		for (String serverAttribute : CmsMappingUtils.SUBSTITUTION_ATTRIBUTES) {
			valueMap.put(serverConfig.getString(serverAttribute), serverAttribute);
		}
		for (int v = 0; v < count; v++) {
			IDfValue value = object.getRepeatingValue(attr.getName(), v);
			String mapping = valueMap.get(value.asString());
			if (mapping != null) {
				value = DfValueFactory.newStringValue(String.format("${%s}", mapping));
			}
			ret.add(value);
		}
		return ret;
	}

	public static Collection<IDfValue> resolveSpecialUsers(IDfTypedObject object, CmsProperty property)
		throws DfException {
		IDfTypedObject sessionConfig = null;
		List<IDfValue> ret = new ArrayList<IDfValue>(property.getValueCount());
		for (IDfValue oldValue : property) {
			Matcher m = CmsMappingUtils.SUBSTITUTION.matcher(oldValue.asString());
			if (m.matches()) {
				if (sessionConfig == null) {
					sessionConfig = object.getSession().getServerConfig();
				}
				ret.add(sessionConfig.getValue(m.group(1)));
			} else {
				ret.add(oldValue);
			}
		}
		return ret;
	}

	public static String resolveSpecialUser(IDfSession session, String user) throws DfException {
		IDfTypedObject serverConfig = session.getServerConfig();
		Matcher m = CmsMappingUtils.SUBSTITUTION.matcher(user);
		if (m.matches()) {
			return serverConfig.getString(m.group(1));
		} else {
			return user;
		}
	}

	public static String resolveSpecialUser(IDfTypedObject object, String user) throws DfException {
		return CmsMappingUtils.resolveSpecialUser(object.getSession(), user);
	}

	public static boolean isSpecialUser(IDfSession session, String user) throws DfException {
		IDfTypedObject serverConfig = session.getServerConfig();
		Map<String, String> valueMap = new HashMap<String, String>();
		for (String serverAttribute : CmsMappingUtils.SUBSTITUTION_ATTRIBUTES) {
			valueMap.put(serverConfig.getString(serverAttribute), serverAttribute);
		}
		return (valueMap.get(user) != null);
	}

	public static boolean isSpecialUserSubstitution(String user) throws DfException {
		return CmsMappingUtils.SUBSTITUTION.matcher(user).matches();
	}
}