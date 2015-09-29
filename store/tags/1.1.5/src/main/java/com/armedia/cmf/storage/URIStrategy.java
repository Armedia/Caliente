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
		protected String calculateSSP(StoredObject<?> object) {
			return null;
		}
	};

	private static final Pattern VALIDATOR = Pattern.compile("^[a-zA-Z_$][a-zA-Z\\d_$]*$");

	private static final boolean isValidName(String name) {
		return (name != null) && URIStrategy.VALIDATOR.matcher(name).matches();
	}

	private static final Map<String, URIStrategy> STRATEGIES;

	static {
		Map<String, URIStrategy> strategies = new HashMap<String, URIStrategy>();
		PluggableServiceLocator<URIStrategy> l = new PluggableServiceLocator<URIStrategy>(URIStrategy.class);
		l.setHideErrors(true);
		for (URIStrategy s : l) {
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
		STRATEGIES = Tools.freezeMap(strategies);
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

	public String calculateFragment(StoredObject<?> object, String qualifier) {
		return qualifier;
	}

	protected final String getDefaultSSP(StoredObject<?> object) {
		return String.format("%s/%s", object.getType(), object.getId());
	}

	protected abstract String calculateSSP(StoredObject<?> object);

	public final String getSSP(StoredObject<?> object) {
		String ssp = calculateSSP(object);
		if (ssp == null) {
			ssp = getDefaultSSP(object);
		}
		return ssp;
	}
}