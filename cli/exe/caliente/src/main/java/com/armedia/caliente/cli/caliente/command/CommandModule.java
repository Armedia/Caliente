package com.armedia.caliente.cli.caliente.command;

import java.io.File;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.caliente.cli.OptionValues;
import com.armedia.caliente.cli.caliente.cfg.CLIParam;
import com.armedia.caliente.cli.caliente.cfg.CalienteState;
import com.armedia.caliente.cli.caliente.exception.CalienteException;
import com.armedia.caliente.cli.caliente.options.CalienteCommonOptions;
import com.armedia.caliente.engine.TransferEngine;
import com.armedia.caliente.engine.TransferEngineSetting;
import com.armedia.caliente.engine.TransferSetting;
import com.armedia.caliente.engine.tools.LocalOrganizationStrategy;
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

	public final void initialize(CalienteState state, Map<String, Object> settings) {
		// By default, do nothing...maybe do threading configurations? Common stuff?
		if (!preInitialize(state, settings)) { return; }
		if (!doInitialize(state, settings)) { return; }
		if (!postInitialize(state, settings)) { return; }
	}

	protected boolean preInitialize(CalienteState state, Map<String, Object> settings) {
		settings.put(TransferSetting.THREAD_COUNT.getLabel(), CommandModule.DEFAULT_THREADS);
		return true;
	}

	protected boolean doInitialize(CalienteState state, Map<String, Object> settings) {
		return true;
	}

	protected boolean postInitialize(CalienteState state, Map<String, Object> settings) {
		return true;
	}

	public final void configure(CalienteState state, OptionValues commandValues, Map<String, Object> settings)
		throws CalienteException {
		preValidateSettings(state, settings);
		if (preConfigure(state, commandValues, settings)) {
			if (doConfigure(state, commandValues, settings)) {
				postConfigure(state, commandValues, settings);
			}
		}
		postValidateSettings(state, settings);
	}

	protected void preValidateSettings(CalienteState state, Map<String, Object> settings) throws CalienteException {
	}

	protected boolean preConfigure(CalienteState state, OptionValues commandValues, Map<String, Object> settings)
		throws CalienteException {
		settings.put(TransferSetting.EXCLUDE_TYPES.getLabel(), commandValues.getAllStrings(CLIParam.exclude_types));
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

	protected boolean doConfigure(CalienteState state, OptionValues commandValues, Map<String, Object> settings)
		throws CalienteException {
		return true;
	}

	protected void postConfigure(CalienteState state, OptionValues commandValues, Map<String, Object> settings)
		throws CalienteException {
	}

	protected void postValidateSettings(CalienteState state, Map<String, Object> settings) throws CalienteException {
	}

	public final int run(CalienteState state, final OptionValues commandValues, final Collection<String> positionals)
		throws CalienteException {
		if (this.descriptor.isRequiresStorage()) {
			// Make sure the storage engines are there
			Objects.requireNonNull(state.getObjectStore(),
				String.format("The %s command requires an object store!", this.descriptor.getTitle()));
			Objects.requireNonNull(state.getContentStore(),
				String.format("The %s command requires a content store!", this.descriptor.getTitle()));
		}
		return execute(state, commandValues, positionals);
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

	protected abstract int execute(CalienteState state, OptionValues commandValues, Collection<String> positionals)
		throws CalienteException;

}