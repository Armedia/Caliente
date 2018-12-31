package com.armedia.caliente.cli.caliente.command;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collection;
import java.util.Date;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;

import com.armedia.caliente.cli.OptionValue;
import com.armedia.caliente.cli.OptionValues;
import com.armedia.caliente.cli.caliente.cfg.CalienteState;
import com.armedia.caliente.cli.caliente.exception.CalienteException;
import com.armedia.caliente.cli.caliente.launcher.CalienteWarningTracker;
import com.armedia.caliente.cli.caliente.launcher.ExportCommandListener;
import com.armedia.caliente.cli.caliente.launcher.ExportManifest;
import com.armedia.caliente.cli.caliente.options.CLIParam;
import com.armedia.caliente.engine.TransferSetting;
import com.armedia.caliente.engine.exporter.ExportEngine;
import com.armedia.caliente.engine.exporter.ExportEngineFactory;
import com.armedia.caliente.engine.exporter.ExportEngineListener;
import com.armedia.caliente.engine.exporter.ExportResult;
import com.armedia.caliente.store.CmfContentStore;
import com.armedia.caliente.store.CmfObjectCounter;
import com.armedia.caliente.store.CmfObjectStore;
import com.armedia.caliente.store.CmfType;
import com.armedia.commons.utilities.CfgTools;
import com.armedia.commons.utilities.PluggableServiceLocator;

public class ExportCommandModule extends CommandModule<ExportEngineFactory<?, ?, ?, ?, ?, ?>> {

	public ExportCommandModule(ExportEngineFactory<?, ?, ?, ?, ?, ?> engine) {
		super(CalienteCommand.EXPORT, engine);
	}

	@Override
	protected boolean preConfigure(CalienteState state, OptionValues commandValues, Map<String, Object> settings)
		throws CalienteException {
		if (!super.preConfigure(state, commandValues, settings)) { return false; }
		settings.put(TransferSetting.LATEST_ONLY.getLabel(),
			commandValues.isPresent(CLIParam.no_versions) || commandValues.isPresent(CLIParam.direct_fs));

		return true;
	}

	@Override
	protected boolean doConfigure(CalienteState state, OptionValues commandValues, Map<String, Object> settings)
		throws CalienteException {
		return true;
	}

	@Override
	protected int execute(CalienteState state, OptionValues commandValues, Collection<String> positionals)
		throws CalienteException {

		Set<ExportResult> outcomes = commandValues.getAllEnums(ExportResult.class, false,
			CLIParam.manifest_outcomes_export);
		if (outcomes == null) {
			outcomes = EnumSet.allOf(ExportResult.class);
		}
		Set<CmfType> types = commandValues.getAllEnums(CmfType.class, false, CLIParam.manifest_types);
		if (types == null) {
			types = EnumSet.allOf(CmfType.class);
		}

		final ExportCommandListener mainListener = new ExportCommandListener(this.console);
		final CalienteWarningTracker warningTracker = mainListener.getWarningTracker();
		final CmfObjectCounter<ExportResult> counter = mainListener.getCounter();

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

		Map<String, Object> settings = new TreeMap<>();
		initialize(state, settings);

		final CmfObjectStore<?, ?> objectStore = state.getObjectStore();
		final CmfContentStore<?, ?, ?> contentStore = state.getContentStore();

		final Date start;
		final Date end;
		Map<CmfType, Long> summary = null;
		String exceptionReport = null;
		final StringBuilder report = new StringBuilder();
		try {

			configure(state, commandValues, settings);
			start = new Date();
			try {
				ExportEngine<?, ?, ?, ?, ?, ?, ?> engine = this.engineFactory.newInstance(this.console, warningTracker,
					state.getBaseDataLocation(), objectStore, contentStore, new CfgTools(settings));
				engine.addListener(mainListener);
				engine.addListener(new ExportManifest(outcomes, types));
				for (ExportEngineListener l : extraListeners) {
					engine.addListener(l);
				}
				this.log.info("##### Export Process Started #####");
				engine.run();
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
		// TODO: Send the e-mail report
		/*
		try {
			EmailUtils.postCalienteMail(String.format("Action report for Caliente Export"), reportString);
		} catch (MessagingException e) {
			this.log.error("Exception caught attempting to send the report e-mail", e);
		}
		*/

		return 0;
	}

	@Override
	public void close() throws Exception {
	}
}