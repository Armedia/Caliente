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
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;

import com.armedia.caliente.cli.OptionValue;
import com.armedia.caliente.cli.OptionValues;
import com.armedia.caliente.cli.caliente.cfg.CalienteState;
import com.armedia.caliente.cli.caliente.exception.CalienteException;
import com.armedia.caliente.cli.caliente.launcher.CalienteWarningTracker;
import com.armedia.caliente.cli.caliente.launcher.ImportCommandListener;
import com.armedia.caliente.cli.caliente.launcher.ImportManifest;
import com.armedia.caliente.cli.caliente.options.CLIParam;
import com.armedia.caliente.engine.importer.ImportEngine;
import com.armedia.caliente.engine.importer.ImportEngineFactory;
import com.armedia.caliente.engine.importer.ImportEngineListener;
import com.armedia.caliente.engine.importer.ImportResult;
import com.armedia.caliente.engine.importer.ImportSetting;
import com.armedia.caliente.store.CmfContentStore;
import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.store.CmfObjectCounter;
import com.armedia.caliente.store.CmfObjectStore;
import com.armedia.commons.utilities.CfgTools;
import com.armedia.commons.utilities.PluggableServiceLocator;
import com.armedia.commons.utilities.Tools;

public class ImportCommandModule extends CommandModule<ImportEngineFactory<?, ?, ?, ?, ?, ?>> {

	public ImportCommandModule(ImportEngineFactory<?, ?, ?, ?, ?, ?> engine) {
		super(CalienteCommand.IMPORT, engine);
	}

	@Override
	protected boolean preConfigure(CalienteState state, OptionValues commandValues, Map<String, Object> settings)
		throws CalienteException {
		settings.put(ImportSetting.TARGET_LOCATION.getLabel(), commandValues.getString(CLIParam.target, "/"));
		settings.put(ImportSetting.TRIM_PREFIX.getLabel(), commandValues.getInteger(CLIParam.trim_path, 0));
		settings.put(ImportSetting.RESTRICT_TO.getLabel(), commandValues.getStrings(CLIParam.restrict_to));
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

	@Override
	protected int execute(CalienteState state, OptionValues commandValues, Collection<String> positionals)
		throws CalienteException {
		Set<ImportResult> outcomes = commandValues.getEnums(ImportResult.class, CommandModule.ALL,
			CfgTools.ignoreFailures(), CLIParam.manifest_outcomes_import, EnumSet.allOf(ImportResult.class));
		Set<CmfObject.Archetype> types = commandValues.getEnums(CmfObject.Archetype.class, CommandModule.ALL,
			CfgTools.ignoreFailures(), CLIParam.manifest_types, EnumSet.allOf(CmfObject.Archetype.class));

		final ImportCommandListener mainListener = new ImportCommandListener(this.console);
		final CalienteWarningTracker warningTracker = mainListener.getWarningTracker();

		PluggableServiceLocator<ImportEngineListener> extraListeners = new PluggableServiceLocator<>(
			ImportEngineListener.class);
		extraListeners.setErrorListener((serviceClass, t) -> ImportCommandModule.this.log
			.warn("Failed to register an additional listener class [{}]", serviceClass.getCanonicalName(), t));
		extraListeners.setHideErrors(false);

		// lock
		Map<String, Object> settings = new HashMap<>();
		initialize(state, settings);

		final CmfObjectStore<?> objectStore = state.getObjectStore();
		final CmfContentStore<?, ?> contentStore = state.getContentStore();
		final Date start;
		final Date end;
		String exceptionReport = null;
		final StringBuilder report = new StringBuilder();
		final CmfObjectCounter<ImportResult> counter = new CmfObjectCounter<>(ImportResult.class);
		try {
			configure(state, commandValues, settings);
			start = new Date();
			try {
				ImportEngine<?, ?, ?, ?, ?, ?, ?> engine = this.engineFactory.newInstance(this.console, warningTracker,
					state.getBaseDataLocation(), objectStore, contentStore, new CfgTools(settings));
				engine.addListener(mainListener);
				engine.addListener(new ImportManifest(outcomes, types));
				extraListeners.forEach(engine::addListener);
				this.log.info("##### Import Process Started #####");
				engine.run(counter);
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
				report.append(String.format("%n\t%s = %s", key, value.getStrings()));
			} else {
				report.append(String.format("%n\t%s", key));
			}
		}

		report.append(String.format("%n%nAction Summary:%n%s%n", StringUtils.repeat("=", 30)));
		for (CmfObject.Archetype t : CmfObject.Archetype.values()) {
			report.append(String.format("%n%n%n"));
			report.append(counter.generateReport(t, 1));
		}
		report.append(String.format("%n%n%n"));
		report.append(counter.generateCummulativeReport(1));

		if (exceptionReport != null) {
			report.append(String.format("%n%n%nEXCEPTION REPORT FOLLOWS:%n%n")).append(exceptionReport);
		}

		String reportString = report.toString();
		String fmt = "Action report for import operation:{}{}{}{}";
		Object[] args = {
			Tools.NL, Tools.NL, reportString, Tools.NL
		};
		this.log.info(fmt, args);
		this.console.info(fmt, args);
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