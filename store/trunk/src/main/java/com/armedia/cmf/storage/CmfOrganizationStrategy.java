package com.armedia.cmf.storage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.commons.utilities.PluggableServiceLocator;
import com.armedia.commons.utilities.Tools;

public abstract class CmfOrganizationStrategy {

	private static final Logger LOG = LoggerFactory.getLogger(CmfOrganizationStrategy.class);

	private static final CmfOrganizationStrategy DEFAULT_STRATEGY = new CmfOrganizationStrategy() {

		@Override
		public List<String> calculatePath(CmfAttributeTranslator<?> translator, CmfObject<?> object,
			CmfContentInfo info) {
			List<String> ssp = new ArrayList<String>(3);
			ssp.add(object.getType().name());
			ssp.add(object.getSubtype());
			return ssp;
		}

		@Override
		protected String calculateBaseName(CmfAttributeTranslator<?> translator, CmfObject<?> object,
			CmfContentInfo info) {
			return object.getId();
		}

		@Override
		protected String calculateAppendix(CmfAttributeTranslator<?> translator, CmfObject<?> object,
			CmfContentInfo info) {
			return "";
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
				CmfOrganizationStrategy.LOG
					.warn(String.format("Path Strategy [%s] did not provide a name, so it won't be registered",
						s.getClass().getCanonicalName()));
				continue;
			}
			CmfOrganizationStrategy old = strategies.get(name);
			if (old != null) {
				CmfOrganizationStrategy.LOG.warn(String.format(
					"CmfOrganizationStrategy [%s] provides the name [%s], but this collides with already-registered strategy [%s]. The newcomer will be ignored.",
					s.getClass().getCanonicalName(), name, old.getClass().getCanonicalName()));
				continue;
			}
			CmfOrganizationStrategy.LOG.debug("Registering CmfOrganizationStrategy [{}] as [{}]",
				s.getClass().getCanonicalName(), name);
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
		if (!CmfOrganizationStrategy.isValidName(name)) { throw new IllegalArgumentException(
			String.format("The string [%s] is not valid for a strategy name", name)); }
		this.name = name;
	}

	public final String getName() {
		return this.name;
	}

	public String calculateDescriptor(CmfAttributeTranslator<?> translator, CmfObject<?> object, CmfContentInfo info) {
		return String.format("%s.%08x", info.getRenditionIdentifier(), info.getRenditionPage());
	}

	protected abstract List<String> calculatePath(CmfAttributeTranslator<?> translator, CmfObject<?> object,
		CmfContentInfo info);

	public final List<String> getPath(CmfAttributeTranslator<?> translator, CmfObject<?> object, CmfContentInfo info) {
		if (translator == null) { throw new IllegalArgumentException("Must provide an attribute translator"); }
		if (object == null) { throw new IllegalArgumentException("Must provide a CMF object"); }
		if (info == null) { throw new IllegalArgumentException("Must provide a valid Content Information object"); }
		return calculatePath(translator, object, info);
	}

	protected abstract String calculateBaseName(CmfAttributeTranslator<?> translator, CmfObject<?> object,
		CmfContentInfo info);

	public final String getBaseName(CmfAttributeTranslator<?> translator, CmfObject<?> object, CmfContentInfo info) {
		if (translator == null) { throw new IllegalArgumentException("Must provide an attribute translator"); }
		if (object == null) { throw new IllegalArgumentException("Must provide a CMF object"); }
		if (info == null) { throw new IllegalArgumentException("Must provide a valid Content Information object"); }
		return calculateBaseName(translator, object, info);
	}

	protected String calculateExtension(CmfAttributeTranslator<?> translator, CmfObject<?> object,
		CmfContentInfo info) {
		return StringUtils.isEmpty(info.getExtension()) ? "" : String.format(".%s", info.getExtension());
	}

	public final String getExtension(CmfAttributeTranslator<?> translator, CmfObject<?> object, CmfContentInfo info) {
		if (translator == null) { throw new IllegalArgumentException("Must provide an attribute translator"); }
		if (object == null) { throw new IllegalArgumentException("Must provide a CMF object"); }
		if (info == null) { throw new IllegalArgumentException("Must provide a valid Content Information object"); }
		return calculateExtension(translator, object, info);
	}

	protected String calculateAppendix(CmfAttributeTranslator<?> translator, CmfObject<?> object, CmfContentInfo info) {
		return null;
	}

	public final String getAppendix(CmfAttributeTranslator<?> translator, CmfObject<?> object, CmfContentInfo info) {
		if (translator == null) { throw new IllegalArgumentException("Must provide an attribute translator"); }
		if (object == null) { throw new IllegalArgumentException("Must provide a CMF object"); }
		if (info == null) { throw new IllegalArgumentException("Must provide a valid Content Information object"); }
		return calculateAppendix(translator, object, info);
	}
}