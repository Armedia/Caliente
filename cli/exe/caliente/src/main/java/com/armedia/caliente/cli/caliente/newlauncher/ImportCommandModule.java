package com.armedia.caliente.cli.caliente.newlauncher;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.mail.MessagingException;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;

import com.armedia.caliente.cli.OptionValue;
import com.armedia.caliente.cli.OptionValues;
import com.armedia.caliente.cli.caliente.cfg.Setting;
import com.armedia.caliente.cli.caliente.exception.CalienteException;
import com.armedia.caliente.cli.caliente.newlauncher.EngineProxy.Importer;
import com.armedia.caliente.cli.caliente.utils.EmailUtils;
import com.armedia.caliente.engine.importer.ImportEngineListener;
import com.armedia.caliente.engine.importer.ImportResult;
import com.armedia.caliente.store.CmfContentStore;
import com.armedia.caliente.store.CmfObjectCounter;
import com.armedia.caliente.store.CmfObjectStore;
import com.armedia.caliente.store.CmfType;
import com.armedia.commons.utilities.PluggableServiceLocator;
import com.armedia.commons.utilities.Tools;

public class ImportCommandModule extends CommandModule {

	private static final Descriptor DESCRIPTOR = new Descriptor("import",
		"Import content from Caliente into an ECM or an intermediate format", "imp", "im");

	public ImportCommandModule(CalienteWarningTracker warningTracker) {
		super(true, false, ImportCommandModule.DESCRIPTOR);
	}

	@Override
	protected int execute(EngineProxy engineProxy, CmfObjectStore<?, ?> objectStore,
		CmfContentStore<?, ?, ?> contentStore, OptionValues commandValues, Collection<String> positionals)
		throws CalienteException {
		Set<ImportResult> outcomes = Tools.parseEnumCSV(ImportResult.class, Setting.MANIFEST_OUTCOMES.getString(),
			CommandModule.ALL, false);
		Set<CmfType> types = Tools.parseEnumCSV(CmfType.class, Setting.MANIFEST_TYPES.getString(), CommandModule.ALL,
			false);

		final Importer engine = engineProxy.getImporter();
		final ImportCommandListener mainListener = new ImportCommandListener(this.console);
		final CalienteWarningTracker warningTracker = mainListener.getWarningTracker();

		engine.addListener(mainListener);
		engine.addListener(new ImportManifest(outcomes, types));
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
			engine.addListener(l);
		}

		// lock
		Map<String, Object> settings = new HashMap<>();
		engine.initialize(settings);

		final Date start;
		final Date end;
		String exceptionReport = null;
		final StringBuilder report = new StringBuilder();
		final CmfObjectCounter<ImportResult> results = new CmfObjectCounter<>(ImportResult.class);
		try {
			engine.configure(commandValues, settings);
			start = new Date();
			try {
				this.log.info("##### Import Process Started #####");
				engine.runImport(this.console, warningTracker, objectStore, contentStore, settings, results);
				this.log.info("##### Import Process Completed #####");
			} catch (Throwable t) {
				StringWriter sw = new StringWriter();
				PrintWriter pw = new PrintWriter(sw);
				report.append(String.format("%n%nException caught while attempting an import%n%n"));
				t.printStackTrace(pw);
				exceptionReport = sw.toString();
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

		report.append(String.format("%n%n%nSettings in use:%n")).append(StringUtils.repeat("=", 30));
		for (Setting s : Setting.values()) {
			report.append(String.format("%n\t%s = [%s]", s.name, s.getString()));
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
		try {
			EmailUtils.postCalienteMail(String.format("Action report for Caliente Import"), reportString);
		} catch (MessagingException e) {
			this.log.error("Exception caught attempting to send the report e-mail", e);
			this.console.error("Exception caught attempting to send the report e-mail", e);
		}
		return 0;
	}

	@Override
	public void close() throws Exception {
	}
}