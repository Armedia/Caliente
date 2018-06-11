package com.armedia.caliente.cli.caliente.newlauncher;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

import javax.mail.MessagingException;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;

import com.armedia.caliente.cli.OptionValue;
import com.armedia.caliente.cli.OptionValues;
import com.armedia.caliente.cli.caliente.cfg.CLIParam;
import com.armedia.caliente.cli.caliente.cfg.Setting;
import com.armedia.caliente.cli.caliente.exception.CalienteException;
import com.armedia.caliente.cli.caliente.utils.EmailUtils;
import com.armedia.caliente.engine.TransferSetting;
import com.armedia.caliente.engine.exporter.ExportEngine;
import com.armedia.caliente.engine.exporter.ExportEngineListener;
import com.armedia.caliente.engine.exporter.ExportResult;
import com.armedia.caliente.store.CmfContentStore;
import com.armedia.caliente.store.CmfObjectCounter;
import com.armedia.caliente.store.CmfObjectStore;
import com.armedia.caliente.store.CmfType;
import com.armedia.commons.utilities.ConfigurationSetting;
import com.armedia.commons.utilities.PluggableServiceLocator;
import com.armedia.commons.utilities.Tools;

public class ExportCommandModule extends CommandModule {

	protected static final String EXPORT_START = "calienteExportStart";
	protected static final String EXPORT_END = "calienteExportEnd";
	protected static final String BASE_SELECTOR = "calienteBaseSelector";
	protected static final String FINAL_SELECTOR = "calienteFinalSelector";

	private static final Descriptor DESCRIPTOR = new Descriptor("export",
		"Extract content from an ECM or Local Filesystem", "exp", "ex");

	public ExportCommandModule() {
		super(true, true, ExportCommandModule.DESCRIPTOR);
	}

	@Override
	protected int execute(EngineProxy engineProxy, CmfObjectStore<?, ?> objectStore,
		CmfContentStore<?, ?, ?> contentStore, OptionValues commandValues, Collection<String> positionals)
		throws CalienteException {

		Set<ExportResult> outcomes = Tools.parseEnumCSV(ExportResult.class, Setting.MANIFEST_OUTCOMES.getString(),
			CommandModule.ALL, false);
		Set<CmfType> types = Tools.parseEnumCSV(CmfType.class, Setting.MANIFEST_TYPES.getString(), CommandModule.ALL,
			false);

		final ExportEngine<?, ?, ?, ?, ?, ?> engine = engineProxy.getExportEngine();
		final ExportCommandListener mainListener = new ExportCommandListener(this.console);
		final CalienteWarningTracker warningTracker = mainListener.getWarningTracker();
		final CmfObjectCounter<ExportResult> counter = mainListener.getCounter();

		engine.addListener(mainListener);
		engine.addListener(new ExportManifest(outcomes, types));

		PluggableServiceLocator<ExportEngineListener> extraListeners = new PluggableServiceLocator<>(
			ExportEngineListener.class);
		extraListeners.setErrorListener(new PluggableServiceLocator.ErrorListener() {
			@Override
			public void errorRaised(Class<?> serviceClass, Throwable t) {
				ExportCommandModule.this.console.warn(String.format(
					"Failed to register an additional listener class [%s]", serviceClass.getCanonicalName()), t);
			}
		});
		extraListeners.setHideErrors(false);
		for (ExportEngineListener l : extraListeners) {
			engine.addListener(l);
		}

		Map<String, Object> settings = new TreeMap<>();
		prepareSettings(settings);

		settings.put(TransferSetting.THREAD_COUNT.getLabel(), Setting.THREADS.getInt(CommandModule.DEFAULT_THREADS));

		Date end = null;
		Map<CmfType, Long> summary = null;
		String exceptionReport = null;
		StringBuilder report = new StringBuilder();
		Date start = null;
		try {
			prepareState(settings);

			Map<String, Object> m = loadDefaultSettings();
			if ((m != null) && !m.isEmpty()) {
				settings.putAll(m);
			}

			processSettings(settings);
			start = new Date();
			try {
				this.log.info("##### Export Process Started #####");
				engine.runExport(this.console, warningTracker, objectStore, contentStore, settings);
				this.log.info("##### Export Process Finished #####");
				summary = objectStore.getStoredObjectTypes();
			} catch (Throwable t) {
				StringWriter sw = new StringWriter();
				PrintWriter pw = new PrintWriter(sw);
				report.append(String.format("%n%nException caught while attempting an export%n%n"));
				t.printStackTrace(pw);
				exceptionReport = sw.toString();
			}
		} finally {
			// unlock
			end = new Date();
			cleanupState();
		}

		long duration = (end.getTime() - start.getTime());
		long hours = TimeUnit.HOURS.convert(duration, TimeUnit.MILLISECONDS);
		duration -= hours * TimeUnit.MILLISECONDS.convert(1, TimeUnit.HOURS);
		long minutes = TimeUnit.MINUTES.convert(duration, TimeUnit.MILLISECONDS);
		duration -= minutes * TimeUnit.MILLISECONDS.convert(1, TimeUnit.MINUTES);
		long seconds = TimeUnit.SECONDS.convert(duration, TimeUnit.MILLISECONDS);
		report.append(String.format("Export process start    : %s%n",
			DateFormatUtils.format(start, CommandModule.JAVA_SQL_DATETIME_PATTERN)));
		report.append(String.format("Export process end      : %s%n",
			DateFormatUtils.format(end, CommandModule.JAVA_SQL_DATETIME_PATTERN)));
		report.append(String.format("Export process duration : %02d:%02d:%02d%n", hours, minutes, seconds));

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

		report.append(String.format("%n%n%nSettings in use:%n")).append(StringUtils.repeat("=", 30));
		for (Setting s : Setting.values()) {
			report.append(String.format("%n\t%s = [%s]", s.name, s.getString()));
		}

		if (summary != null) {
			report.append(String.format("%n%n%nExported Object Summary:%n")).append(StringUtils.repeat("=", 30));
			long total = 0;
			for (CmfType t : summary.keySet()) {
				Long count = summary.get(t);
				if ((count == null) || (count == 0)) {
					continue;
				}
				report.append(String.format("%n%-16s: %6d", t.name(), count));
				total += count;
			}
			report.append(String.format("%n%s%n%-16s: %6d%n", StringUtils.repeat("=", 30), "Total", total));
		}

		report.append(String.format("%n%n%nFull Result Report:%n")).append(StringUtils.repeat("=", 30));
		report.append(String.format("%n%s%n", counter.generateFullReport(0)));
		if (warningTracker.hasWarnings()) {
			report.append(String.format("%n%s%n", warningTracker.generateReport()));
		}

		Map<ExportResult, Long> m = counter.getCummulative();
		final Long zero = Long.valueOf(0);
		report.append(String.format("Result summary:%n%n")).append(StringUtils.repeat("=", 30));
		for (ExportResult r : ExportResult.values()) {
			Long i = m.get(r);
			if (i == null) {
				i = zero;
			}
			report.append(String.format("%n%-16s : %8d", r.name(), i.intValue()));
		}

		if (exceptionReport != null) {
			report.append(String.format("%n%n%nEXCEPTION REPORT FOLLOWS:%n%n")).append(exceptionReport);
			this.console
				.error(String.format("%n%nEXCEPTION CAUGHT WHILE RUNNING THE EXPORT:%n%n%s%n", exceptionReport));
		}

		String reportString = report.toString();
		this.log.info(String.format("Action report for export operation:%n%n%s%n", reportString));
		try {
			EmailUtils.postCalienteMail(String.format("Action report for Caliente Export"), reportString);
		} catch (MessagingException e) {
			this.log.error("Exception caught attempting to send the report e-mail", e);
		}

		return 0;
	}

	protected void validateState() throws CalienteException {
		if (this.server == null) { throw new CalienteException(
			"Must provide the location where the repository may be accessed"); }
	}

	protected final void prepareSettings(Map<String, Object> settings) throws CalienteException {
		settings.put(TransferSetting.EXCLUDE_TYPES.getLabel(), Setting.CMF_EXCLUDE_TYPES.getString(""));
		settings.put(TransferSetting.IGNORE_CONTENT.getLabel(), CLIParam.skip_content.isPresent());
		settings.put(TransferSetting.LATEST_ONLY.getLabel(),
			CLIParam.no_versions.isPresent() || CLIParam.direct_fs.isPresent());
		settings.put(TransferSetting.NO_RENDITIONS.getLabel(),
			CLIParam.no_renditions.isPresent() || CLIParam.direct_fs.isPresent());
		settings.put(TransferSetting.TRANSFORMATION.getLabel(), CLIParam.transformations.getString());
		settings.put(TransferSetting.EXTERNAL_METADATA.getLabel(), CLIParam.external_metadata.getString());
		settings.put(TransferSetting.FILTER.getLabel(), CLIParam.filters.getString());

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
		customizeSettings(settings);
	}

	protected ConfigurationSetting getUserSetting() {
		return null;
	}

	protected ConfigurationSetting getPasswordSetting() {
		return null;
	}

	protected ConfigurationSetting getDomainSetting() {
		return null;
	}

	protected void customizeSettings(Map<String, Object> settings) throws CalienteException {
	}

	protected Map<String, Object> loadSettings(String jobName) throws CalienteException {
		return null;
	}

	protected Map<String, Object> loadDefaultSettings() throws CalienteException {
		return new HashMap<>();
	}

	protected boolean storeSettings(String jobName, Map<String, Object> settings, Date exportStart, Date exportEnd)
		throws CalienteException {
		return false;
	}

	protected void processSettings(Map<String, Object> settings) throws CalienteException {
	}

	protected void prepareState(Map<String, Object> settings) throws CalienteException {

	}

	protected void cleanupState() {

	}

	@Override
	public void close() throws Exception {
	}
}