package com.armedia.cmf.storage;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.commons.utilities.PluggableServiceLocator;
import com.armedia.commons.utilities.Tools;

public abstract class URIStrategy {

	private static final Logger LOG = LoggerFactory.getLogger(URIStrategy.class);

	private static final URIStrategy DEFAULT_STRATEGY = new URIStrategy() {
		@Override
		public String calculateSSP(StoredObjectType objectType, String objectId) {
			return String.format("%s/%s", objectType, objectId);
		}
	};

	private static final Pattern VALIDATOR = Pattern.compile("^[a-zA-Z_$][a-zA-Z\\d_$]*$");

	private static final boolean isValidName(String name) {
		return (name != null) && URIStrategy.VALIDATOR.matcher(name).matches();
	}

	private static Map<String, URIStrategy> STRATEGIES;

	static {
		Map<String, URIStrategy> strategies = new HashMap<String, URIStrategy>();
		for (URIStrategy s : new PluggableServiceLocator<URIStrategy>(URIStrategy.class)) {
			String name = s.getName();
			if (name == null) {
				URIStrategy.LOG.warn(String.format(
					"Path Strategy [%s] did not provide a name, so it won't be registered", s.getClass()
					.getCanonicalName()));
				continue;
			}
			URIStrategy old = strategies.get(name);
			if (old != null) {
				URIStrategy.LOG
				.warn(String
					.format(
						"URIStrategy [%s] provides the name [%s], but this collides with already-registered strategy [%s]. The newcomer will be ignored.",
						s.getClass().getCanonicalName(), name, old.getClass().getCanonicalName()));
				continue;
			}
			URIStrategy.LOG.debug("Registering URIStrategy [{}] as [{}]", s.getClass().getCanonicalName(), name);
			strategies.put(name, s);
		}
		URIStrategy.STRATEGIES = Tools.freezeMap(strategies);
	}

	public static URIStrategy getStrategy(String name) {
		if (name == null) { return URIStrategy.DEFAULT_STRATEGY; }
		return Tools.coalesce(URIStrategy.STRATEGIES.get(name), URIStrategy.DEFAULT_STRATEGY);
	}

	private final String name;

	URIStrategy() {
		this.name = null;
	}

	protected URIStrategy(String name) {
		if (!URIStrategy.isValidName(name)) { throw new IllegalArgumentException(String.format(
			"The string [%s] is not valid for a strategy name", name)); }
		this.name = name;
	}

	public final String getName() {
		return this.name;
	}

	public String calculateFragment(StoredObjectType objectType, String objectId) {
		return null;
	}

	public abstract String calculateSSP(StoredObjectType objectType, String objectId);
}