package com.armedia.caliente.cli.caliente.command;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.slf4j.Logger;

import com.armedia.caliente.cli.OptionValue;
import com.armedia.caliente.cli.OptionValues;
import com.armedia.caliente.cli.caliente.cfg.CalienteState;
import com.armedia.caliente.cli.caliente.exception.CalienteException;
import com.armedia.caliente.cli.caliente.launcher.CalienteWarningTracker;
import com.armedia.caliente.cli.caliente.launcher.ImportCommandListener;
import com.armedia.caliente.cli.caliente.launcher.ImportManifest;
import com.armedia.caliente.cli.caliente.options.CLIParam;
import com.armedia.caliente.engine.WarningTracker;
import com.armedia.caliente.engine.importer.ImportEngine;
import com.armedia.caliente.engine.importer.ImportEngineListener;
import com.armedia.caliente.engine.importer.ImportException;
import com.armedia.caliente.engine.importer.ImportResult;
import com.armedia.caliente.engine.importer.ImportSetting;
import com.armedia.caliente.store.CmfContentStore;
import com.armedia.caliente.store.CmfObjectCounter;
import com.armedia.caliente.store.CmfObjectStore;
import com.armedia.caliente.store.CmfStorageException;
import com.armedia.caliente.store.CmfType;
import com.armedia.commons.utilities.PluggableServiceLocator;

public class ImportCommandModule extends CommandModule<ImportEngine<?, ?, ?, ?, ?, ?>> {

	public ImportCommandModule(ImportEngine<?, ?, ?, ?, ?, ?> engine) {
		super(CalienteCommand.IMPORT, engine);
	}

	@Override
	protected boolean preConfigure(CalienteState state, OptionValues commandValues, Map<String, Object> settings)
		throws CalienteException {
		settings.put(ImportSetting.TARGET_LOCATION.getLabel(), commandValues.getString(CLIParam.target, "/"));
		settings.put(ImportSetting.TRIM_PREFIX.getLabel(), commandValues.getInteger(CLIParam.trim_path, 0));

		settings.put(ImportSetting.NO_FILENAME_MAP.getLabel(), commandValues.isPresent(CLIParam.no_filename_map));
		settings.put(ImportSetting.FILENAME_MAP.getLabel(), commandValues.getString(CLIParam.filename_map));
		settings.put(ImportSetting.VALIDATE_REQUIREMENTS.getLabel(),
			commandValues.isPresent(CLIParam.validate_requirements));
		return super.preConfigure(state, commandValues, settings);
	}

	@Override
	protected boolean doConfigure(CalienteState state, OptionValues commandValues, Map<String, Object> settings)
		throws CalienteException {
		return super.doConfigure(state, commandValues, settings);
	}

	public final CmfObjectCounter<ImportResult> runImport(Logger output, WarningTracker warningTracker, File baseData,
		CmfObjectStore<?, ?> objectStore, CmfContentStore<?, ?, ?> streamStore, Map<String, ?> settings)
		throws ImportException, CmfStorageException {
		return this.engine.runImport(output, warningTracker, baseData, objectStore, streamStore, settings);
	}

	public final CmfObjectCounter<ImportResult> runImport(Logger output, WarningTracker warningTracker, File baseData,
		CmfObjectStore<?, ?> objectStore, CmfContentStore<?, ?, ?> streamStore, Map<String, ?> settings,
		CmfObjectCounter<ImportResult> counter) throws ImportException, CmfStorageException {
		return this.engine.runImport(output, warningTracker, baseData, objectStore, streamStore, settings, counter);
	}

	@Override
	protected int execute(CalienteState state, OptionValues commandValues, Collection<String> positionals)
		throws CalienteException {
		Set<ImportResult> outcomes = commandValues.getAllEnums(ImportResult.class, CommandModule.ALL, false,
			CLIParam.manifest_outcomes_import);
		Set<CmfType> types = commandValues.getAllEnums(CmfType.class, CommandModule.ALL, false,
			CLIParam.manifest_types);

		final ImportCommandListener mainListener = new ImportCommandListener(this.console);
		final CalienteWarningTracker warningTracker = mainListener.getWarningTracker();

		this.engine.addListener(mainListener);
		this.engine.addListener(new ImportManifest(outcomes, types));
		PluggableServiceLocator<ImportEngineListener> extraListeners = new PluggableServiceLocator<>(
			ImportEngineListener.class);
		extraListeners.setErrorListener(new PluggableServiceLocator.ErrorListener() {
			@Override
			public void errorRaised(Class<?> serviceClass, Throwable t) {
				ImportCommandModule.this.log.warn(String.format("Failed to register an additional listener class [%s]",
					serviceClass.getCanonicalName()), t);
			}
		});
		extraListeners.setHideErrors(false);

		for (ImportEngineListener l : extraListeners) {
			this.engine.addListener(l);
		}

		// lock
		Map<String, Object> settings = new HashMap<>();
		initialize(state, settings);

		final CmfObjectStore<?, ?> objectStore = state.getObjectStore();
		final CmfContentStore<?, ?, ?> contentStore = state.getContentStore();
		final Date start;
		final Date end;
		String exceptionReport = null;
		final StringBuilder report = new StringBuilder();
		final CmfObjectCounter<ImportResult> results = new CmfObjectCounter<>(ImportResult.class);
		try {
			configure(state, commandValues, settings);
			start = new Date();
			try {
				this.log.info("##### Import Process Started #####");
				this.engine.runImport(this.console, warningTracker, state.getBaseDataLocation(), objectStore,
					contentStore, settings, results);
				this.log.info("##### Import Process Completed #####");
			} catch (Throwable t) {
				StringWriter sw = new StringWriter();
				PrintWriter pw = new PrintWriter(sw);
				report.append(String.format("%n%nException caught while attempting an import%n%n"));
				t.printStackTrace(pw);
				exceptionReport = sw.toString();
			} finally {
				try {
					close();
				} catch (Exception e) {
					this.log.error("Exception caught while closing the proxy", e);
				}
			}
		} finally {
			// unlock
			end = new Date();
		}

		long duration = (end.getTime() - start.getTime());
		long hours = TimeUnit.HOURS.convert(duration, TimeUnit.MILLISECONDS);
		duration -= hours * TimeUnit.MILLISECONDS.convert(1, TimeUnit.HOURS);
		long minutes = TimeUnit.MINUTES.convert(duration, TimeUnit.MILLISECONDS);
		duration -= minutes * TimeUnit.MILLISECONDS.convert(1, TimeUnit.MINUTES);
		long seconds = TimeUnit.SECONDS.convert(duration, TimeUnit.MILLISECONDS);
		report.append(String.format("Import process start    : %s%n",
			DateFormatUtils.format(start, CommandModule.JAVA_SQL_DATETIME_PATTERN)));
		report.append(String.format("Import process end      : %s%n",
			DateFormatUtils.format(end, CommandModule.JAVA_SQL_DATETIME_PATTERN)));
		report.append(String.format("Import process duration : %02d:%02d:%02d%n", hours, minutes, seconds));

		report.append(String.format("%n%nCommand parameters in use:%n")).append(StringUtils.repeat("=", 30));
		for (OptionValue value : commandValues) {
			String key = value.getLongOpt();
			if (key != null) {
				key = String.format("--%s", key);
			} else {
				key = String.format("-%s", value.getShortOpt());
			}
			if (value.hasValues()) {
				report.append(String.format("%n\t%s = %s", key, value.getAllStrings()));
			} else {
				report.append(String.format("%n\t%s", key));
			}
		}

		report.append(String.format("%n%nAction Summary:%n%s%n", StringUtils.repeat("=", 30)));
		for (CmfType t : CmfType.values()) {
			report.append(String.format("%n%n%n"));
			report.append(results.generateReport(t, 1));
		}
		report.append(String.format("%n%n%n"));
		report.append(results.generateCummulativeReport(1));

		if (exceptionReport != null) {
			report.append(String.format("%n%n%nEXCEPTION REPORT FOLLOWS:%n%n")).append(exceptionReport);
		}

		String reportString = report.toString();
		this.log.info(String.format("Action report for import operation:%n%n%s%n", reportString));
		this.console.info(String.format("Action report for import operation:%n%n%s%n", reportString));
		// TODO: Send the e-mail report
		/*
		try {
			EmailUtils.postCalienteMail(String.format("Action report for Caliente Import"), reportString);
		} catch (MessagingException e) {
			this.log.error("Exception caught attempting to send the report e-mail", e);
			this.console.error("Exception caught attempting to send the report e-mail", e);
		}
		*/
		return 0;
	}

	@Override
	public void close() throws Exception {
	}
}