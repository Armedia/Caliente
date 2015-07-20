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

public abstract class CmfOrganizationStrategy {

	private static final Logger LOG = LoggerFactory.getLogger(CmfOrganizationStrategy.class);

	private static final CmfOrganizationStrategy DEFAULT_STRATEGY = new CmfOrganizationStrategy() {

		@Override
		public List<String> calculatePath(CmfAttributeTranslator<?> translator, CmfObject<?> object) {
			return null;
		}
	};

	private static final Pattern VALIDATOR = Pattern.compile("^[a-zA-Z_$][a-zA-Z\\d_$]*$");

	private static final boolean isValidName(String name) {
		return (name != null) && CmfOrganizationStrategy.VALIDATOR.matcher(name).matches();
	}

	private static final Map<String, CmfOrganizationStrategy> STRATEGIES;

	static {
		Map<String, CmfOrganizationStrategy> strategies = new HashMap<String, CmfOrganizationStrategy>();
		PluggableServiceLocator<CmfOrganizationStrategy> l = new PluggableServiceLocator<CmfOrganizationStrategy>(
			CmfOrganizationStrategy.class);
		l.setHideErrors(true);
		for (CmfOrganizationStrategy s : l) {
			String name = s.getName();
			if (name == null) {
				CmfOrganizationStrategy.LOG.warn(String.format(
					"Path Strategy [%s] did not provide a name, so it won't be registered", s.getClass()
						.getCanonicalName()));
				continue;
			}
			CmfOrganizationStrategy old = strategies.get(name);
			if (old != null) {
				CmfOrganizationStrategy.LOG
					.warn(String
						.format(
							"CmfOrganizationStrategy [%s] provides the name [%s], but this collides with already-registered strategy [%s]. The newcomer will be ignored.",
							s.getClass().getCanonicalName(), name, old.getClass().getCanonicalName()));
				continue;
			}
			CmfOrganizationStrategy.LOG.debug("Registering CmfOrganizationStrategy [{}] as [{}]", s.getClass()
				.getCanonicalName(), name);
			strategies.put(name, s);
		}
		STRATEGIES = Tools.freezeMap(strategies);
	}

	public static CmfOrganizationStrategy getStrategy(String name) {
		if (name == null) { return CmfOrganizationStrategy.DEFAULT_STRATEGY; }
		return Tools.coalesce(CmfOrganizationStrategy.STRATEGIES.get(name), CmfOrganizationStrategy.DEFAULT_STRATEGY);
	}

	protected final Logger log = LoggerFactory.getLogger(getClass());

	private final String name;

	CmfOrganizationStrategy() {
		this.name = null;
	}

	protected CmfOrganizationStrategy(String name) {
		if (!CmfOrganizationStrategy.isValidName(name)) { throw new IllegalArgumentException(String.format(
			"The string [%s] is not valid for a strategy name", name)); }
		this.name = name;
	}

	public final String getName() {
		return this.name;
	}

	public String calculateAddendum(CmfAttributeTranslator<?> translator, CmfObject<?> object, String qualifier) {
		return qualifier;
	}

	protected abstract List<String> calculatePath(CmfAttributeTranslator<?> translator, CmfObject<?> object);

	protected final List<String> getDefaultPath(CmfObject<?> object) {
		List<String> ret = new ArrayList<String>(2);
		ret.add(object.getType().name());
		ret.add(object.getId());
		return ret;
	}

	public final List<String> getPath(CmfAttributeTranslator<?> translator, CmfObject<?> object) {
		List<String> ssp = calculatePath(translator, object);
		if ((ssp == null) || ssp.isEmpty()) {
			ssp = getDefaultPath(object);
		}
		return ssp;
	}
}