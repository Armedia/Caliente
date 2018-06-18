package com.armedia.caliente.cli.caliente.newlauncher;

import java.util.Collection;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.caliente.cli.OptionValues;
import com.armedia.caliente.cli.caliente.cfg.CLIParam;
import com.armedia.caliente.cli.caliente.cfg.Setting;
import com.armedia.caliente.cli.caliente.exception.CalienteException;
import com.armedia.caliente.cli.launcher.LaunchClasspathHelper;
import com.armedia.caliente.engine.TransferEngine;
import com.armedia.caliente.engine.TransferEngineSetting;
import com.armedia.caliente.engine.TransferSetting;
import com.armedia.caliente.engine.WarningTracker;
import com.armedia.caliente.engine.exporter.ExportEngine;
import com.armedia.caliente.engine.exporter.ExportEngineListener;
import com.armedia.caliente.engine.exporter.ExportException;
import com.armedia.caliente.engine.exporter.ExportResult;
import com.armedia.caliente.engine.importer.ImportEngine;
import com.armedia.caliente.engine.importer.ImportEngineListener;
import com.armedia.caliente.engine.importer.ImportException;
import com.armedia.caliente.engine.importer.ImportResult;
import com.armedia.caliente.engine.importer.ImportSetting;
import com.armedia.caliente.store.CmfContentStore;
import com.armedia.caliente.store.CmfObjectCounter;
import com.armedia.caliente.store.CmfObjectStore;
import com.armedia.caliente.store.CmfStorageException;
import com.armedia.caliente.tools.CmfCrypt;
import com.armedia.commons.utilities.PluggableServiceLocator;
import com.armedia.commons.utilities.PluggableServiceSelector;
import com.armedia.commons.utilities.Tools;

public abstract class EngineProxy implements AutoCloseable {

	abstract class ProxyBase<LISTENER, ENGINE extends TransferEngine<?, ?, ?, ?, ?, LISTENER>> {

		protected final ENGINE engine;

		private ProxyBase(ENGINE engine) {
			Objects.requireNonNull(engine, "Must provide a valid engine instance");
			this.engine = engine;
		}

		public final boolean addListener(LISTENER listener) {
			return this.engine.addListener(listener);
		}

		public final boolean removeListener(LISTENER listener) {
			return this.engine.removeListener(listener);
		}

		public final CmfCrypt getCrypto() {
			return this.engine.getCrypto();
		}

		public final boolean isSupportsDuplicateFileNames() {
			return this.engine.isSupportsDuplicateFileNames();
		}

		public final Collection<TransferEngineSetting> getSupportedSettings() {
			return this.engine.getSupportedSettings();
		}

		public final void initialize(Map<String, Object> settings) {
			// By default, do nothing...maybe do threading configurations? Common stuff?
			if (!preInitialize(settings)) { return; }
			if (!doInitialize(settings)) { return; }
			if (!postInitialize(settings)) { return; }
		}

		protected boolean preInitialize(Map<String, Object> settings) {
			settings.put(TransferSetting.THREAD_COUNT.getLabel(),
				Setting.THREADS.getInt(CommandModule.DEFAULT_THREADS));
			return true;
		}

		protected boolean doInitialize(Map<String, Object> settings) {
			return true;
		}

		protected boolean postInitialize(Map<String, Object> settings) {
			return true;
		}

		public final void configure(OptionValues commandValues, Map<String, Object> settings) throws CalienteException {
			preValidateSettings(settings);
			if (preConfigure(commandValues, settings)) {
				if (doConfigure(commandValues, settings)) {
					postConfigure(commandValues, settings);
				}
			}
			postValidateSettings(settings);
		}

		protected void preValidateSettings(Map<String, Object> settings) throws CalienteException {
		}

		protected boolean preConfigure(OptionValues commandValues, Map<String, Object> settings)
			throws CalienteException {
			settings.put(TransferSetting.EXCLUDE_TYPES.getLabel(), Setting.CMF_EXCLUDE_TYPES.getString(""));
			settings.put(TransferSetting.IGNORE_CONTENT.getLabel(), commandValues.isPresent(CLIParam.skip_content));
			settings.put(TransferSetting.THREAD_COUNT.getLabel(),
				Setting.THREADS.getInt(CommandModule.DEFAULT_THREADS));
			settings.put(TransferSetting.NO_RENDITIONS.getLabel(), commandValues.isPresent(CLIParam.no_renditions));
			settings.put(TransferSetting.TRANSFORMATION.getLabel(), commandValues.getString(CLIParam.transformations));
			settings.put(TransferSetting.EXTERNAL_METADATA.getLabel(),
				commandValues.getString(CLIParam.external_metadata));
			settings.put(TransferSetting.FILTER.getLabel(), commandValues.getString(CLIParam.filters));
			return true;
		}

		protected boolean doConfigure(OptionValues commandValues, Map<String, Object> settings)
			throws CalienteException {
			return true;
		}

		protected void postConfigure(OptionValues commandValues, Map<String, Object> settings)
			throws CalienteException {
		}

		protected void postValidateSettings(Map<String, Object> settings) throws CalienteException {

		}
	}

	protected class Exporter extends ProxyBase<ExportEngineListener, ExportEngine<?, ?, ?, ?, ?, ?>> {

		protected Exporter(ExportEngine<?, ?, ?, ?, ?, ?> engine) {
			super(engine);
		}

		public final CmfObjectCounter<ExportResult> runExport(Logger output, WarningTracker warningTracker,
			CmfObjectStore<?, ?> objectStore, CmfContentStore<?, ?, ?> contentStore, Map<String, ?> settings)
			throws ExportException, CmfStorageException {
			return this.engine.runExport(output, warningTracker, objectStore, contentStore, settings);
		}

		public final CmfObjectCounter<ExportResult> runExport(Logger output, WarningTracker warningTracker,
			CmfObjectStore<?, ?> objectStore, CmfContentStore<?, ?, ?> contentStore, Map<String, ?> settings,
			CmfObjectCounter<ExportResult> counter) throws ExportException, CmfStorageException {
			return this.engine.runExport(output, warningTracker, objectStore, contentStore, settings, counter);
		}

		@Override
		protected boolean preConfigure(OptionValues commandValues, Map<String, Object> settings)
			throws CalienteException {
			boolean ret = super.preConfigure(commandValues, settings);
			if (ret) {
				settings.put(TransferSetting.LATEST_ONLY.getLabel(),
					commandValues.isPresent(CLIParam.no_versions) || commandValues.isPresent(CLIParam.direct_fs));
			}
			return ret;
		}

		@Override
		protected boolean doConfigure(OptionValues commandValues, Map<String, Object> settings)
			throws CalienteException {
			/*
			
			ConfigurationSetting setting = null;
			
			setting = getUserSetting();
			if ((this.user != null) && (setting != null)) {
				settings.put(setting.getLabel(), this.user);
			}
			
			setting = getPasswordSetting();
			if ((this.password != null) && (setting != null)) {
				settings.put(setting.getLabel(), this.password);
			}
			
			setting = getDomainSetting();
			if ((this.domain != null) && (setting != null)) {
				settings.put(setting.getLabel(), this.domain);
			}
			*/
			return true;
		}
	}

	protected class Importer extends ProxyBase<ImportEngineListener, ImportEngine<?, ?, ?, ?, ?, ?>> {

		protected Importer(ImportEngine<?, ?, ?, ?, ?, ?> engine) {
			super(engine);
		}

		@Override
		protected boolean preConfigure(OptionValues commandValues, Map<String, Object> settings)
			throws CalienteException {
			settings.put(ImportSetting.NO_FILENAME_MAP.getLabel(), commandValues.isPresent(CLIParam.no_filename_map));
			settings.put(ImportSetting.FILENAME_MAP.getLabel(), commandValues.getString(CLIParam.filename_map));
			settings.put(ImportSetting.VALIDATE_REQUIREMENTS.getLabel(),
				commandValues.isPresent(CLIParam.validate_requirements));
			return super.preConfigure(commandValues, settings);
		}

		@Override
		protected boolean doConfigure(OptionValues commandValues, Map<String, Object> settings)
			throws CalienteException {
			return super.doConfigure(commandValues, settings);
		}

		public final CmfObjectCounter<ImportResult> runImport(Logger output, WarningTracker warningTracker,
			CmfObjectStore<?, ?> objectStore, CmfContentStore<?, ?, ?> streamStore, Map<String, ?> settings)
			throws ImportException, CmfStorageException {
			return this.engine.runImport(output, warningTracker, objectStore, streamStore, settings);
		}

		public final CmfObjectCounter<ImportResult> runImport(Logger output, WarningTracker warningTracker,
			CmfObjectStore<?, ?> objectStore, CmfContentStore<?, ?, ?> streamStore, Map<String, ?> settings,
			CmfObjectCounter<ImportResult> counter) throws ImportException, CmfStorageException {
			return this.engine.runImport(output, warningTracker, objectStore, streamStore, settings, counter);
		}

	}

	protected final Logger log = LoggerFactory.getLogger(getClass());

	public abstract String getName();

	public abstract Set<String> getAliases();

	public abstract CmfCrypt getCrypt();

	public final Exporter getExporter() {
		ExportEngine<?, ?, ?, ?, ?, ?> engine = getExportEngine();
		if (engine == null) { throw new IllegalStateException("This proxy does not support an Export engine"); }
		return newExporter(engine);
	}

	protected Exporter newExporter(ExportEngine<?, ?, ?, ?, ?, ?> engine) {
		return new Exporter(engine);
	}

	protected abstract ExportEngine<?, ?, ?, ?, ?, ?> getExportEngine();

	public final Importer getImporter() {
		ImportEngine<?, ?, ?, ?, ?, ?> engine = getImportEngine();
		if (engine == null) { throw new IllegalStateException("This proxy does not support an Import engine"); }
		return newImporter(engine);
	}

	protected Importer newImporter(ImportEngine<?, ?, ?, ?, ?, ?> engine) {
		return new Importer(engine);
	}

	protected abstract ImportEngine<?, ?, ?, ?, ?, ?> getImportEngine();

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

	protected abstract boolean isCommandSupported(String command);

	@Override
	public void close() throws Exception {
		// By default, do nothing...
	}
}