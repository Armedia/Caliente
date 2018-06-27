package com.armedia.caliente.cli.caliente.command;

import java.io.File;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.caliente.cli.OptionValues;
import com.armedia.caliente.cli.caliente.cfg.CLIParam;
import com.armedia.caliente.cli.caliente.cfg.CalienteCommonOptions;
import com.armedia.caliente.cli.caliente.cfg.Setting;
import com.armedia.caliente.cli.caliente.exception.CalienteException;
import com.armedia.caliente.engine.TransferEngine;
import com.armedia.caliente.engine.TransferEngineSetting;
import com.armedia.caliente.engine.TransferSetting;
import com.armedia.caliente.engine.tools.LocalOrganizationStrategy;
import com.armedia.caliente.store.CmfContentStore;
import com.armedia.caliente.store.CmfObjectStore;
import com.armedia.caliente.store.xml.StoreConfiguration;
import com.armedia.caliente.tools.CmfCrypt;
import com.armedia.commons.utilities.Tools;

public abstract class CommandModule<ENGINE extends TransferEngine<?, ?, ?, ?, ?, ?>> implements AutoCloseable {

	protected static final String ALL = "ALL";
	protected static final int DEFAULT_THREADS = (Runtime.getRuntime().availableProcessors() * 2);
	protected static final int MAX_THREADS = (Runtime.getRuntime().availableProcessors() * 8);
	protected static final String JAVA_SQL_DATETIME_PATTERN = "yyyy-MM-dd HH:mm:ss";
	protected static final String LAST_EXPORT_DATE_PATTERN = CommandModule.JAVA_SQL_DATETIME_PATTERN;

	protected final Logger log = LoggerFactory.getLogger(getClass());
	protected final Logger console = LoggerFactory.getLogger("console");
	protected final CalienteCommand descriptor;
	protected final ENGINE engine;

	protected CommandModule(CalienteCommand descriptor, ENGINE engine) {
		Objects.requireNonNull(descriptor, "Must provide a valid descriptor instance");
		Objects.requireNonNull(engine, "Must provide a valid engine instance");
		this.descriptor = descriptor;
		this.engine = engine;
	}

	public CalienteCommand getDescriptor() {
		return this.descriptor;
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
		settings.put(TransferSetting.THREAD_COUNT.getLabel(), Setting.THREADS.getInt(CommandModule.DEFAULT_THREADS));
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

	protected boolean preConfigure(OptionValues commandValues, Map<String, Object> settings) throws CalienteException {
		settings.put(TransferSetting.EXCLUDE_TYPES.getLabel(), Setting.CMF_EXCLUDE_TYPES.getString(""));
		settings.put(TransferSetting.IGNORE_CONTENT.getLabel(), commandValues.isPresent(CLIParam.skip_content));

		int threads = CommandModule.DEFAULT_THREADS;
		if (commandValues.isPresent(CalienteCommonOptions.THREADS)) {
			threads = commandValues.getInteger(CalienteCommonOptions.THREADS);
		}
		threads = Tools.ensureBetween(1, threads, CommandModule.MAX_THREADS);
		settings.put(TransferSetting.THREAD_COUNT.getLabel(), threads);

		settings.put(TransferSetting.NO_RENDITIONS.getLabel(), commandValues.isPresent(CLIParam.no_renditions));
		settings.put(TransferSetting.TRANSFORMATION.getLabel(),
			commandValues.getString(CalienteCommonOptions.TRANSFORMATIONS));
		settings.put(TransferSetting.EXTERNAL_METADATA.getLabel(),
			commandValues.getString(CalienteCommonOptions.EXTERNAL_METADATA));
		settings.put(TransferSetting.FILTER.getLabel(), commandValues.getString(CalienteCommonOptions.FILTERS));
		return true;
	}

	protected boolean doConfigure(OptionValues commandValues, Map<String, Object> settings) throws CalienteException {
		return true;
	}

	protected void postConfigure(OptionValues commandValues, Map<String, Object> settings) throws CalienteException {
	}

	protected void postValidateSettings(Map<String, Object> settings) throws CalienteException {
	}

	public final int run(CmfObjectStore<?, ?> objectStore, CmfContentStore<?, ?, ?> contentStore,
		final OptionValues commandValues, final Collection<String> positionals) throws CalienteException {
		if (this.descriptor.isRequiresStorage()) {
			// Make sure the storage engines are there
			Objects.requireNonNull(objectStore,
				String.format("The %s command requires an object store!", this.descriptor.getTitle()));
			Objects.requireNonNull(contentStore,
				String.format("The %s command requires a content store!", this.descriptor.getTitle()));
		} else {
			// Make sure they always go null downstream
			objectStore = null;
			contentStore = null;
		}

		return execute(objectStore, contentStore, commandValues, positionals);
	}

	public String getContentStrategyName() {
		return LocalOrganizationStrategy.NAME;
	}

	public void customizeObjectStoreProperties(StoreConfiguration cfg) {
		// Do nothing by default
	}

	public void customizeContentStoreProperties(StoreConfiguration cfg) {
		// Do nothing by default
	}

	public File getMetadataFilesLocation() {
		return null;
	}

	public File getContentFilesLocation() {
		return null;
	}

	protected abstract int execute(CmfObjectStore<?, ?> objectStore, CmfContentStore<?, ?, ?> contentStore,
		OptionValues commandValues, Collection<String> positionals) throws CalienteException;

}