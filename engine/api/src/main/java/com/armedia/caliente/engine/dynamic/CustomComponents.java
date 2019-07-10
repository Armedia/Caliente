/*******************************************************************************
 * #%L
 * Armedia Caliente
 * %%
 * Copyright (c) 2010 - 2019 Armedia LLC
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
package com.armedia.caliente.engine.dynamic;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.commons.utilities.PluggableServiceLocator;
import com.armedia.commons.utilities.Tools;

public class CustomComponents {

	private static final Logger LOG = LoggerFactory.getLogger(CustomComponents.class);

	private static <C, F extends CustomComponentFactory<C>> Map<String, F> buildFactoryMap(final Class<F> klass) {
		PluggableServiceLocator<F> locator = new PluggableServiceLocator<>(klass);
		locator.setErrorListener((serviceClass, t) -> CustomComponents.LOG.error(
			"Failed to load initialize the factory class {} (as a subclass of {})", serviceClass.getCanonicalName(),
			klass.getCanonicalName(), t));
		locator.setHideErrors(false);
		Map<String, F> map = new TreeMap<>();
		for (F f : locator) {
			Set<String> aliases = f.getClassNamesOrAliases();
			if ((aliases == null) || aliases.isEmpty()) {
				CustomComponents.LOG.warn(
					"Factory class [{}] does not define any class names or aliases - it will not be used",
					f.getClass().getCanonicalName());
				continue;
			}

			for (String alias : aliases) {
				F oldF = map.put(alias, f);
				if (oldF != null) {
					CustomComponents.LOG.warn(
						"Factory conflict detected for alias [{}]: {} and {} - only the latter will be used", alias,
						oldF.getClass().getCanonicalName(), f.getClass().getCanonicalName());
				}
			}
		}
		return Tools.freezeMap(new LinkedHashMap<>(map));
	}

	private static final Map<String, ActionFactory> ACTIONS = CustomComponents.buildFactoryMap(ActionFactory.class);
	private static final Map<String, ConditionFactory> CONDITIONS = CustomComponents
		.buildFactoryMap(ConditionFactory.class);

	public static ActionFactory getActionFactory(String className) {
		return CustomComponents.ACTIONS.get(className);
	}

	public static ConditionFactory getConditionFactory(String className) {
		return CustomComponents.CONDITIONS.get(className);
	}
}