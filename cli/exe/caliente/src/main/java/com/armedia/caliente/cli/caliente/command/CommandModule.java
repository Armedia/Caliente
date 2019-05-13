package com.armedia.caliente.cli.caliente.command;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.caliente.cli.OptionValues;
import com.armedia.caliente.cli.caliente.cfg.CalienteState;
import com.armedia.caliente.cli.caliente.exception.CalienteException;
import com.armedia.caliente.cli.caliente.options.CLIParam;
import com.armedia.caliente.cli.utils.ThreadsLaunchHelper;
import com.armedia.caliente.engine.TransferEngineFactory;
import com.armedia.caliente.engine.TransferSetting;
import com.armedia.caliente.store.xml.StoreConfiguration;
import com.armedia.caliente.tools.CmfCrypt;
import com.armedia.commons.utilities.Tools;

public abstract class CommandModule<ENGINE_FACTORY extends TransferEngineFactory<?, ?, ?, ?, ?, ?, ?, ?, ?>>
	implements AutoCloseable {

	protected static final String ALL = "ALL";
	protected static final String JAVA_SQL_DATETIME_PATTERN = "yyyy-MM-dd HH:mm:ss";
	protected static final String LAST_EXPORT_DATE_PATTERN = CommandModule.JAVA_SQL_DATETIME_PATTERN;

	protected final Logger log = LoggerFactory.getLogger(getClass());
	protected final Logger console = LoggerFactory.getLogger("console");
	protected final CalienteCommand descriptor;
	protected final ENGINE_FACTORY engineFactory;

	protected CommandModule(CalienteCommand descriptor, ENGINE_FACTORY engine) {
		this.descriptor = Objects.requireNonNull(descriptor, "Must provide a valid descriptor instance");
		this.engineFactory = Objects.requireNonNull(engine, "Must provide a valid engine instance");
	}

	public CalienteCommand getDescriptor() {
		return this.descriptor;
	}

	public boolean isShouldStoreContentLocationRequirement() {
		return false;
	}

	public boolean isContentStreamsExternal(OptionValues commandValues) {
		return false;
	}

	public final ENGINE_FACTORY getEngineFactory() {
		return this.engineFactory;
	}

	public final CmfCrypt getCrypto() {
		if (this.engineFactory == null) { return null; }
		return this.engineFactory.getCrypto();
	}

	public final void initialize(CalienteState state, Map<String, Object> settings) {
		// By default, do nothing...maybe do threading configurations? Common stuff?
		if (!preInitialize(state, settings)) { return; }
		if (!doInitialize(state, settings)) { return; }
		if (!postInitialize(state, settings)) { return; }
	}

	protected boolean preInitialize(CalienteState state, Map<String, Object> settings) {
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

		if (commandValues.hasValues(CLIParam.only_types)) {
			settings.put(TransferSetting.ONLY_TYPES.getLabel(), commandValues.getStrings(CLIParam.only_types));
		} else if (commandValues.hasValues(CLIParam.except_types)) {
			settings.put(TransferSetting.EXCEPT_TYPES.getLabel(), commandValues.getStrings(CLIParam.except_types));
		}
		settings.put(TransferSetting.EXTERNAL_METADATA.getLabel(), commandValues.getString(CLIParam.external_metadata));
		settings.put(TransferSetting.FILTER.getLabel(), commandValues.getString(CLIParam.filter));
		settings.put(TransferSetting.IGNORE_CONTENT.getLabel(), commandValues.isPresent(CLIParam.skip_content));
		settings.put(TransferSetting.LATEST_ONLY.getLabel(), commandValues.isPresent(CLIParam.no_versions));
		settings.put(TransferSetting.NO_RENDITIONS.getLabel(), commandValues.isPresent(CLIParam.no_renditions));
		settings.put(TransferSetting.TRANSFORMATION.getLabel(), commandValues.getString(CLIParam.transformations));

		int threads = commandValues.getInteger(CLIParam.threads);
		threads = Tools.ensureBetween(ThreadsLaunchHelper.DEFAULT_MIN_THREADS, threads,
			ThreadsLaunchHelper.DEFAULT_MAX_THREADS);
		settings.put(TransferSetting.THREAD_COUNT.getLabel(), threads);

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

	public String getContentOrganizerName(OptionValues commandValues) {
		return null;
	}

	public void customizeObjectStoreProperties(OptionValues commandValues, StoreConfiguration cfg) {
		// Do nothing by default
	}

	public void customizeContentStoreProperties(OptionValues commandValues, StoreConfiguration cfg) {
		// Do nothing by default
	}

	protected abstract int execute(CalienteState state, OptionValues commandValues, Collection<String> positionals)
		throws CalienteException;

}