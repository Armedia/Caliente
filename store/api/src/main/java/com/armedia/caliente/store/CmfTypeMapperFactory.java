package com.armedia.caliente.store;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.commons.utilities.CfgTools;
import com.armedia.commons.utilities.PluggableServiceLocator;
import com.armedia.commons.utilities.Tools;

public abstract class CmfTypeMapperFactory {

	protected static final Logger LOG = LoggerFactory.getLogger(CmfTypeMapperFactory.class);
	private static final Pattern VALIDATOR = Pattern.compile("^[a-zA-Z_$][a-zA-Z\\d_$]*$");
	private static final Map<String, CmfTypeMapperFactory> FACTORIES;

	static {
		Map<String, CmfTypeMapperFactory> factories = new HashMap<String, CmfTypeMapperFactory>();
		PluggableServiceLocator<CmfTypeMapperFactory> locator = new PluggableServiceLocator<CmfTypeMapperFactory>(
			CmfTypeMapperFactory.class);
		locator.setHideErrors(false);
		locator.setErrorListener(new PluggableServiceLocator.ErrorListener() {
			@Override
			public void errorRaised(Class<?> serviceClass, Throwable t) {
				CmfTypeMapperFactory.LOG.warn(
					String.format("Error raised initializing CmfTypeMapperFactory subclass [%s]",
						serviceClass.getCanonicalName()), t);
			}
		});
		for (CmfTypeMapperFactory factory : locator) {
			CmfTypeMapperFactory old = factories.get(factory.getName());
			if (old != null) {
				CmfTypeMapperFactory.LOG.warn(String.format(
					"Duplicate factories with name [%s]: old=[%s] and new=[%s] - the new one will be ignored",
					factory.getName(), old.getClass().getCanonicalName(), factory.getClass().getCanonicalName()));
				continue;
			}
			factories.put(factory.getName(), factory);
		}
		FACTORIES = Tools.freezeMap(factories);
	}

	private final String name;

	protected CmfTypeMapperFactory(String name) {
		if (name == null) { throw new IllegalArgumentException("Name must not be null"); }
		if (!CmfTypeMapperFactory.VALIDATOR.matcher(name).matches()) { throw new IllegalArgumentException(
			String.format("The name [%s] is not valid - it must match the regular expression /%s/", name,
				CmfTypeMapperFactory.VALIDATOR.pattern())); }
		this.name = name;
	}

	protected final String getName() {
		return this.name;
	}

	public abstract CmfTypeMapper getMapperInstance(CfgTools cfg) throws Exception;

	static CmfTypeMapperFactory getFactory(String name) {
		return CmfTypeMapperFactory.FACTORIES.get(name);
	}
}