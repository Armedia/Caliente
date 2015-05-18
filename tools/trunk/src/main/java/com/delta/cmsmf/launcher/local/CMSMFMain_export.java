package com.delta.cmsmf.launcher.local;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.mail.MessagingException;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;

import com.armedia.cmf.engine.exporter.ExportEngine;
import com.armedia.cmf.engine.exporter.ExportEngineListener;
import com.armedia.cmf.engine.exporter.ExportResult;
import com.armedia.cmf.engine.local.exporter.LocalExportEngine;
import com.armedia.cmf.engine.tools.LocalOrganizationStrategy;
import com.armedia.cmf.storage.CmfObject;
import com.armedia.cmf.storage.CmfType;
import com.armedia.commons.utilities.CfgTools;
import com.armedia.commons.utilities.Tools;
import com.delta.cmsmf.cfg.CLIParam;
import com.delta.cmsmf.cfg.Setting;
import com.delta.cmsmf.exception.CMSMFException;
import com.delta.cmsmf.launcher.AbstractCMSMFMain;
import com.delta.cmsmf.launcher.ExportManifest;
import com.delta.cmsmf.utils.CMSMFUtils;

public class CMSMFMain_export extends AbstractCMSMFMain<ExportEngineListener, ExportEngine<?, ?, ?, ?, ?>> implements
	ExportEngineListener {

	public CMSMFMain_export() throws Throwable {
		super(LocalExportEngine.getExportEngine());
	}

	/**
	 * Starts exporting objects from source directory. It reads up the query from properties file
	 * and executes it against the source repository. It retrieves objects from the repository and
	 * exports it out.
	 *
	 */
	@Override
	public void run() throws CMSMFException {
		Set<ExportResult> outcomes = Tools.parseEnumCSV(ExportResult.class, Setting.MANIFEST_OUTCOMES.getString(),
			AbstractCMSMFMain.ALL, false);
		Set<CmfType> types = Tools.parseEnumCSV(CmfType.class, Setting.MANIFEST_TYPES.getString(),
			AbstractCMSMFMain.ALL, false);
		this.engine.addListener(this);
		this.engine.addListener(new ExportManifest(outcomes, types));

		Map<String, Object> settings = new HashMap<String, Object>();

		String srcPath = CLIParam.source.getString();
		if (srcPath == null) { throw new CMSMFException("Must provide the source path to export from"); }

		final File root;
		try {
			root = new File(srcPath).getCanonicalFile();
		} catch (IOException e) {
			throw new CMSMFException(String.format("Failed to find the canonical path of [%s]", srcPath), e);
		}

		settings.put("root", root.getAbsolutePath());

		Date end = null;
		Map<CmfType, Integer> summary = null;
		String exceptionReport = null;
		StringBuilder report = new StringBuilder();
		Date start = null;

		start = new Date();
		try {
			this.log.info("##### Export Process Started #####");
			this.engine.runExport(this.console, this.cmfObjectStore, this.cmfContentStore, settings);
			this.log.info("##### Export Process Finished #####");

			summary = this.cmfObjectStore.getStoredObjectTypes();
		} catch (Throwable t) {
			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			report.append(String.format("%n%nException caught while attempting an export%n%n"));
			t.printStackTrace(pw);
			exceptionReport = sw.toString();
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
			DateFormatUtils.format(start, AbstractCMSMFMain.JAVA_SQL_DATETIME_PATTERN)));
		report.append(String.format("Export process end      : %s%n",
			DateFormatUtils.format(end, AbstractCMSMFMain.JAVA_SQL_DATETIME_PATTERN)));
		report.append(String.format("Export process duration : %02d:%02d:%02d%n", hours, minutes, seconds));

		report.append(String.format("%n%nParameters in use:%n")).append(StringUtils.repeat("=", 30));
		for (CLIParam p : CLIParam.values()) {
			String v = p.getString();
			if (v == null) {
				continue;
			}
			report.append(String.format("%n\t--%s = [%s]", p.option.getLongOpt(), v));
		}

		report.append(String.format("%n%n%nSettings in use:%n")).append(StringUtils.repeat("=", 30));
		for (Setting s : Setting.values()) {
			report.append(String.format("%n\t%s = [%s]", s.name, s.getString()));
		}

		if (summary != null) {
			report.append(String.format("%n%n%nExported Object Summary:%n")).append(StringUtils.repeat("=", 30));
			int total = 0;
			for (CmfType t : summary.keySet()) {
				Integer count = summary.get(t);
				if ((count == null) || (count == 0)) {
					continue;
				}
				report.append(String.format("%n%-16s: %6d", t.name(), count));
				total += count;
			}
			report.append(String.format("%n%s%n%-16s: %6d%n", StringUtils.repeat("=", 30), "Total", total));
		}

		if (exceptionReport != null) {
			report.append(String.format("%n%n%nEXCEPTION REPORT FOLLOWS:%n%n")).append(exceptionReport);
		}

		String reportString = report.toString();
		this.log.info(String.format("Action report for export operation:%n%n%s%n", reportString));
		try {
			CMSMFUtils.postCmsmfMail(String.format("Action report for CMSMF Export"), reportString);
		} catch (MessagingException e) {
			this.log.error("Exception caught attempting to send the report e-mail", e);
		}
	}

	@Override
	public boolean requiresCleanData() {
		return true;
	}

	@Override
	public void exportStarted(CfgTools config) {
		this.console.info(String.format("Export process started with settings:%n%n\t%s%n%n", config));
	}

	@Override
	public void objectExportStarted(CmfType objectType, String objectId) {
		this.console.info(String.format("Object export started for %s[%s]", objectType.name(), objectId));
	}

	@Override
	public void objectExportCompleted(CmfObject<?> object, Long objectNumber) {
		if (objectNumber != null) {
			this.console.info(String.format("%s export completed for [%s](%s) as object #%d", object.getType().name(),
				object.getLabel(), object.getId(), objectNumber));
		}
	}

	@Override
	public void objectSkipped(CmfType objectType, String objectId) {
		this.console.info(String.format("%s object [%s] was skipped", objectType.name(), objectId));
	}

	@Override
	public void objectExportFailed(CmfType objectType, String objectId, Throwable thrown) {
		this.console.warn(String.format("Object export failed for %s[%s]", objectType.name(), objectId), thrown);
	}

	@Override
	public void exportFinished(Map<CmfType, Integer> summary) {
		this.console.info("Export process finished");
		for (CmfType t : CmfType.values()) {
			Integer v = summary.get(t);
			if ((v == null) || (v.intValue() == 0)) {
				continue;
			}
			this.console.info(String.format("%-16s : %8d", t.name(), v.intValue()));
		}
	}

	@Override
	protected String getContentStrategyName() {
		return LocalOrganizationStrategy.NAME;
	}
}