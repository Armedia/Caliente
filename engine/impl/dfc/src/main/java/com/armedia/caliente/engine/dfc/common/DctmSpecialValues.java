/*******************************************************************************
 * #%L
 * Armedia Caliente
 * %%
 * Copyright (C) 2013 - 2019 Armedia, LLC
 * %%
 * This file is part of the Caliente software.
 *
 * If the software was purchased under a paid Caliente license, the terms of
 * the paid license agreement will prevail.  Otherwise, the software is
 * provided under the following open source license terms:
 *
 * Caliente is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Caliente is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Caliente. If not, see <http://www.gnu.org/licenses/>.
 * #L%
 *******************************************************************************/
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