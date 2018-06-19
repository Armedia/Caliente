package com.armedia.caliente.cli.caliente.command;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

import javax.mail.MessagingException;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.slf4j.Logger;

import com.armedia.caliente.cli.OptionValue;
import com.armedia.caliente.cli.OptionValues;
import com.armedia.caliente.cli.caliente.cfg.CLIParam;
import com.armedia.caliente.cli.caliente.exception.CalienteException;
import com.armedia.caliente.cli.caliente.newlauncher.CalienteWarningTracker;
import com.armedia.caliente.cli.caliente.newlauncher.ExportCommandListener;
import com.armedia.caliente.cli.caliente.newlauncher.ExportManifest;
import com.armedia.caliente.cli.caliente.utils.EmailUtils;
import com.armedia.caliente.engine.TransferSetting;
import com.armedia.caliente.engine.WarningTracker;
import com.armedia.caliente.engine.exporter.ExportEngine;
import com.armedia.caliente.engine.exporter.ExportEngineListener;
import com.armedia.caliente.engine.exporter.ExportException;
import com.armedia.caliente.engine.exporter.ExportResult;
import com.armedia.caliente.store.CmfContentStore;
import com.armedia.caliente.store.CmfObjectCounter;
import com.armedia.caliente.store.CmfObjectStore;
import com.armedia.caliente.store.CmfStorageException;
import com.armedia.caliente.store.CmfType;
import com.armedia.commons.utilities.PluggableServiceLocator;

public class ExportCommandModule extends CommandModule<ExportEngine<?, ?, ?, ?, ?, ?>> {

	protected static final String EXPORT_START = "calienteExportStart";
	protected static final String EXPORT_END = "calienteExportEnd";
	protected static final String BASE_SELECTOR = "calienteBaseSelector";
	protected static final String FINAL_SELECTOR = "calienteFinalSelector";

	public ExportCommandModule(ExportEngine<?, ?, ?, ?, ?, ?> engine) {
		super(CalienteCommand.EXPORT, engine);
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
	protected boolean preConfigure(OptionValues commandValues, Map<String, Object> settings) throws CalienteException {
		boolean ret = super.preConfigure(commandValues, settings);
		if (ret) {
			settings.put(TransferSetting.LATEST_ONLY.getLabel(),
				commandValues.isPresent(CLIParam.no_versions) || commandValues.isPresent(CLIParam.direct_fs));
		}
		return ret;
	}

	@Override
	protected boolean doConfigure(OptionValues commandValues, Map<String, Object> settings) throws CalienteException {
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

	@Override
	protected int execute(CmfObjectStore<?, ?> objectStore, CmfContentStore<?, ?, ?> contentStore,
		OptionValues commandValues, Collection<String> positionals) throws CalienteException {

		Set<ExportResult> outcomes = commandValues.getAllEnums(ExportResult.class, false, CLIParam.manifest_outcomes);
		Set<CmfType> types = commandValues.getAllEnums(CmfType.class, false, CLIParam.manifest_types);

		final ExportCommandListener mainListener = new ExportCommandListener(this.console);
		final CalienteWarningTracker warningTracker = mainListener.getWarningTracker();
		final CmfObjectCounter<ExportResult> counter = mainListener.getCounter();

		this.engine.addListener(mainListener);
		this.engine.addListener(new ExportManifest(outcomes, types));

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
			this.engine.addListener(l);
		}

		Map<String, Object> settings = new TreeMap<>();
		this.initialize(settings);

		final Date start;
		final Date end;
		Map<CmfType, Long> summary = null;
		String exceptionReport = null;
		final StringBuilder report = new StringBuilder();
		try {

			this.configure(commandValues, settings);
			start = new Date();
			try {
				this.log.info("##### Export Process Started #####");
				runExport(this.console, warningTracker, objectStore, contentStore, settings);
				this.log.info("##### Export Process Finished #####");
				summary = objectStore.getStoredObjectTypes();
			} catch (Throwable t) {
				StringWriter sw = new StringWriter();
				PrintWriter pw = new PrintWriter(sw);
				report.append(String.format("%n%nException caught while attempting an export%n%n"));
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

	@Override
	public void close() throws Exception {
	}
}