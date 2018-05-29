package com.armedia.caliente.cli.caliente.newlauncher;

import java.util.Collection;
import java.util.NoSuchElementException;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

import com.armedia.caliente.cli.OptionValues;
import com.armedia.caliente.cli.launcher.LaunchClasspathHelper;
import com.armedia.caliente.engine.exporter.ExportEngine;
import com.armedia.caliente.engine.importer.ImportEngine;
import com.armedia.caliente.tools.CmfCrypt;
import com.armedia.commons.utilities.PluggableServiceLocator;
import com.armedia.commons.utilities.PluggableServiceSelector;
import com.armedia.commons.utilities.Tools;

public abstract class EngineProxy {

	public abstract String getName();

	public abstract Set<String> getAliases();

	public abstract CmfCrypt getCrypt();

	@SuppressWarnings("rawtypes")
	public abstract ExportEngine getExportEngine(OptionValues commandValues, Collection<String> positionals);

	@SuppressWarnings("rawtypes")
	public abstract ImportEngine getImportEngine(OptionValues commandValues, Collection<String> positionals);

	public abstract Collection<? extends LaunchClasspathHelper> getClasspathHelpers();

	public static EngineProxy getInstance(final String engine) {
		return EngineProxy.get(null, engine);
	}

	public static EngineProxy get(final Logger log, final String engine) {
		if (StringUtils.isEmpty(engine)) { throw new IllegalArgumentException("Must provide a non-empty engine name"); }

		final PluggableServiceLocator<EngineProxy> engineProxies = new PluggableServiceLocator<>(EngineProxy.class);
		engineProxies.setHideErrors(log == null);
		if (!engineProxies.isHideErrors()) {
			engineProxies.setErrorListener(new PluggableServiceLocator.ErrorListener() {
				@Override
				public void errorRaised(Class<?> serviceClass, Throwable t) {
					log.error("Failed to initialize the EngineProxy class {}", serviceClass.getCanonicalName(), t);
				}
			});
		}

		engineProxies.setDefaultSelector(new PluggableServiceSelector<EngineProxy>() {
			@Override
			public boolean matches(EngineProxy service) {
				if (StringUtils.equalsIgnoreCase(engine, service.getName())) { return true; }
				for (Object alias : service.getAliases()) {
					if (StringUtils.equalsIgnoreCase(engine, Tools.toString(alias))) { return true; }
				}
				return false;
			}
		});

		try {
			return engineProxies.getFirst();
		} catch (NoSuchElementException e) {
			return null;
		}
	}
}