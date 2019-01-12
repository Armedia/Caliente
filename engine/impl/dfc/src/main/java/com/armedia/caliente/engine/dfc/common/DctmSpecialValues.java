package com.armedia.caliente.engine.dfc.common;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.text.StringTokenizer;

import com.armedia.commons.utilities.CfgTools;

public class DctmSpecialValues {

	private final Set<String> specialUsers;
	private final Set<String> specialGroups;
	private final Set<String> specialTypes;

	public DctmSpecialValues(CfgTools cfg) {
		this.specialUsers = DctmSpecialValues.parseCSV(cfg, Setting.SPECIAL_USERS);
		this.specialGroups = DctmSpecialValues.parseCSV(cfg, Setting.SPECIAL_GROUPS);
		this.specialTypes = DctmSpecialValues.parseCSV(cfg, Setting.SPECIAL_TYPES);
	}

	private static Set<String> parseCSV(CfgTools cfg, Setting setting) {
		String str = cfg.getString(setting);
		StringTokenizer strTokenizer = StringTokenizer.getCSVInstance(str);
		Set<String> ret = Collections.unmodifiableSet(new HashSet<>(strTokenizer.getTokenList()));
		if (ret.isEmpty()) {
			ret = Collections.emptySet();
		}
		return ret;
	}

	public final boolean isSpecialGroup(String group) {
		return this.specialGroups.contains(group);
	}

	public final boolean isSpecialUser(String user) {
		return this.specialUsers.contains(user);
	}

	public final boolean isSpecialType(String type) {
		return this.specialTypes.contains(type);
	}
}