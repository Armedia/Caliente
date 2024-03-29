/*******************************************************************************
 * #%L
 * Armedia Caliente
 * %%
 * Copyright (C) 2013 - 2019 Armedia, LLC
 * %%
 * This file is part of the Caliente software.
 *
 * If the software was purchased under a paid Caliente license, the terms of
 * the paid license agreement will prevail.  Otherwise, the software is
 * provided under the following open source license terms:
 *
 * Caliente is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Caliente is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Caliente. If not, see <http://www.gnu.org/licenses/>.
 * #L%
 *******************************************************************************/
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

import com.armedia.caliente.cli.caliente.cfg.CalienteState;
import com.armedia.caliente.cli.caliente.exception.CalienteException;
import com.armedia.caliente.cli.caliente.launcher.CalienteWarningTracker;
import com.armedia.caliente.cli.caliente.launcher.ExportCommandListener;
import com.armedia.caliente.cli.caliente.launcher.ExportManifest;
import com.armedia.caliente.cli.caliente.launcher.ExportRetryManifest;
import com.armedia.caliente.cli.caliente.options.CLIParam;
import com.armedia.caliente.engine.exporter.ExportEngine;
import com.armedia.caliente.engine.exporter.ExportEngineFactory;
import com.armedia.caliente.engine.exporter.ExportEngineListener;
import com.armedia.caliente.engine.exporter.ExportResult;
import com.armedia.caliente.engine.exporter.ExportSetting;
import com.armedia.caliente.store.CmfContentStore;
import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.store.CmfObjectCounter;
import com.armedia.caliente.store.CmfObjectStore;
import com.armedia.commons.utilities.CfgTools;
import com.armedia.commons.utilities.PluggableServiceLocator;
import com.armedia.commons.utilities.Tools;
import com.armedia.commons.utilities.cli.OptionValue;
import com.armedia.commons.utilities.cli.OptionValues;

public class ExportCommandModule extends CommandModule<ExportEngineFactory<?, ?, ?, ?, ?, ?>> {

	public ExportCommandModule(ExportEngineFactory<?, ?, ?, ?, ?, ?> engine) {
		super(CalienteCommand.EXPORT, engine);
	}

	@Override
	protected boolean preConfigure(CalienteState state, OptionValues commandValues, Map<String, Object> settings)
		throws CalienteException {
		if (!super.preConfigure(state, commandValues, settings)) { return false; }
		settings.put(ExportSetting.FROM.getLabel(), commandValues.getStrings(CLIParam.from));
		settings.put(ExportSetting.METADATA_XML.getLabel(), commandValues.getStrings(CLIParam.metadata_xml));
		settings.put(ExportSetting.IGNORE_EMPTY_FOLDERS.getLabel(),
			commandValues.isPresent(CLIParam.ignore_empty_folders));
		return true;
	}

	@Override
	public boolean isShouldStoreContentLocationRequirement() {
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

		Set<ExportResult> outcomes = commandValues.getEnums(ExportResult.class, CfgTools.ignoreFailures(),
			CLIParam.manifest_outcomes_export);
		if (outcomes == null) {
			outcomes = EnumSet.allOf(ExportResult.class);
		}
		Set<CmfObject.Archetype> types = commandValues.getEnums(CmfObject.Archetype.class, CfgTools.ignoreFailures(),
			CLIParam.manifest_types);
		if (types == null) {
			types = EnumSet.allOf(CmfObject.Archetype.class);
		}

		final ExportCommandListener mainListener = new ExportCommandListener(this.console);
		final CalienteWarningTracker warningTracker = mainListener.getWarningTracker();
		final CmfObjectCounter<ExportResult> counter = mainListener.getCounter();

		PluggableServiceLocator<ExportEngineListener> extraListeners = new PluggableServiceLocator<>(
			ExportEngineListener.class);
		extraListeners.setErrorListener((serviceClass, t) -> ExportCommandModule.this.console
			.warn("Failed to register an additional listener class [{}]", serviceClass.getCanonicalName(), t));
		extraListeners.setHideErrors(false);

		Map<String, Object> settings = new TreeMap<>();
		initialize(state, settings);

		final CmfObjectStore<?> objectStore = state.getObjectStore();
		final CmfContentStore<?, ?> contentStore = state.getContentStore();

		final Date start;
		final Date end;
		Map<CmfObject.Archetype, Long> summary = null;
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
				engine.addListener(new ExportRetryManifest(types));
				extraListeners.forEach(engine::addListener);
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
				report.append(String.format("%n\t%s = %s", key, value.getStrings()));
			} else {
				report.append(String.format("%n\t%s", key));
			}
		}

		if (summary != null) {
			report.append(String.format("%n%n%nExported Object Summary:%n")).append(StringUtils.repeat("=", 30));
			long total = 0;
			for (CmfObject.Archetype t : summary.keySet()) {
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
			this.console.error("{}{}EXCEPTION CAUGHT WHILE RUNNING THE EXPORT:{}{}{}{}", Tools.NL, Tools.NL, Tools.NL,
				Tools.NL, exceptionReport, Tools.NL);
		}

		String reportString = report.toString();
		this.log.info("Action report for export operation:{}{}{}{}", Tools.NL, Tools.NL, reportString, Tools.NL);
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