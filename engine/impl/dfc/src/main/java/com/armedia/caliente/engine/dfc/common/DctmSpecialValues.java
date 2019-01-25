package com.armedia.caliente.engine.dfc.common;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.TreeSet;

import com.armedia.commons.utilities.CfgTools;
import com.armedia.commons.utilities.Tools;

public class DctmSpecialValues {

	private final Set<String> specialUsers;
	private final Set<String> specialGroups;
	private final Set<String> specialTypes;

	public DctmSpecialValues(CfgTools cfg) {
		Set<String> set = null;

		set = new TreeSet<>(cfg.getStrings(Setting.SPECIAL_USERS));
		this.specialUsers = Tools.freezeSet(new LinkedHashSet<>(set));

		set = new TreeSet<>(cfg.getStrings(Setting.SPECIAL_GROUPS));
		this.specialGroups = Tools.freezeSet(new LinkedHashSet<>(set));

		set = new TreeSet<>(cfg.getStrings(Setting.SPECIAL_TYPES));
		this.specialTypes = Tools.freezeSet(new LinkedHashSet<>(set));
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