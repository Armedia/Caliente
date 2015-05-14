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

public abstract class OrganizationStrategy {

	private static final Logger LOG = LoggerFactory.getLogger(OrganizationStrategy.class);

	private static final OrganizationStrategy DEFAULT_STRATEGY = new OrganizationStrategy() {

		@Override
		public List<String> calculatePath(ObjectStorageTranslator<?> translator, StoredObject<?> object) {
			return null;
		}
	};

	private static final Pattern VALIDATOR = Pattern.compile("^[a-zA-Z_$][a-zA-Z\\d_$]*$");

	private static final boolean isValidName(String name) {
		return (name != null) && OrganizationStrategy.VALIDATOR.matcher(name).matches();
	}

	private static final Map<String, OrganizationStrategy> STRATEGIES;

	static {
		Map<String, OrganizationStrategy> strategies = new HashMap<String, OrganizationStrategy>();
		PluggableServiceLocator<OrganizationStrategy> l = new PluggableServiceLocator<OrganizationStrategy>(OrganizationStrategy.class);
		l.setHideErrors(true);
		for (OrganizationStrategy s : l) {
			String name = s.getName();
			if (name == null) {
				OrganizationStrategy.LOG.warn(String.format(
					"Path Strategy [%s] did not provide a name, so it won't be registered", s.getClass()
						.getCanonicalName()));
				continue;
			}
			OrganizationStrategy old = strategies.get(name);
			if (old != null) {
				OrganizationStrategy.LOG
					.warn(String
						.format(
							"OrganizationStrategy [%s] provides the name [%s], but this collides with already-registered strategy [%s]. The newcomer will be ignored.",
							s.getClass().getCanonicalName(), name, old.getClass().getCanonicalName()));
				continue;
			}
			OrganizationStrategy.LOG.debug("Registering OrganizationStrategy [{}] as [{}]", s.getClass().getCanonicalName(), name);
			strategies.put(name, s);
		}
		STRATEGIES = Tools.freezeMap(strategies);
	}

	public static OrganizationStrategy getStrategy(String name) {
		if (name == null) { return OrganizationStrategy.DEFAULT_STRATEGY; }
		return Tools.coalesce(OrganizationStrategy.STRATEGIES.get(name), OrganizationStrategy.DEFAULT_STRATEGY);
	}

	protected final Logger log = LoggerFactory.getLogger(getClass());

	private final String name;

	OrganizationStrategy() {
		this.name = null;
	}

	protected OrganizationStrategy(String name) {
		if (!OrganizationStrategy.isValidName(name)) { throw new IllegalArgumentException(String.format(
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