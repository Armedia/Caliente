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
		locator.setErrorListener(new PluggableServiceLocator.ErrorListener() {
			@Override
			public void errorRaised(Class<?> serviceClass, Throwable t) {
				CustomComponents.LOG.error("Failed to load initialize the factory class {} (as a subclass of {})",
					serviceClass.getCanonicalName(), klass.getCanonicalName(), t);
			}
		});
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