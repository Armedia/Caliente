package com.armedia.cmf.storage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
		public List<String> calculatePath(ObjectStorageTranslator<?> translator, StoredObject<?> object) {
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

	protected final Logger log = LoggerFactory.getLogger(getClass());

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

	public String calculateAddendum(ObjectStorageTranslator<?> translator, StoredObject<?> object, String qualifier) {
		return qualifier;
	}

	protected abstract List<String> calculatePath(ObjectStorageTranslator<?> translator, StoredObject<?> object);

	protected final List<String> getDefaultPath(StoredObject<?> object) {
		List<String> ret = new ArrayList<String>(2);
		ret.add(object.getType().name());
		ret.add(object.getId());
		return ret;
	}

	public final List<String> getPath(ObjectStorageTranslator<?> translator, StoredObject<?> object) {
		List<String> ssp = calculatePath(translator, object);
		if ((ssp == null) || ssp.isEmpty()) {
			ssp = getDefaultPath(object);
		}
		return ssp;
	}
}