package com.delta.cmsmf.mainEngine;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import javax.mail.MessagingException;

import com.delta.cmsmf.cfg.Constant;
import com.delta.cmsmf.cfg.Setting;
import com.delta.cmsmf.cms.CmsCounter;
import com.delta.cmsmf.cms.CmsImportResult;
import com.delta.cmsmf.cms.CmsObjectType;
import com.delta.cmsmf.engine.CmsImporter;
import com.delta.cmsmf.exception.CMSMFException;
import com.delta.cmsmf.utils.CMSMFUtils;

/**
 * The main method of this class is an entry point for the cmsmf application.
 *
 * @author Shridev Makim 6/15/2010
 */
public class CMSMFMain_import extends AbstractCMSMFMain {

	CMSMFMain_import() throws Throwable {
		super();
	}

	/**
	 * Starts exporting objects from source directory. It reads up the query from properties file
	 * and executes it against the source repository. It retrieves objects from the repository and
	 * exports it out.
	 *
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 * @throws CMSMFException
	 */
	@Override
	public void run() throws IOException, CMSMFException {
		// lock
		final CmsImporter importer = new CmsImporter(Setting.THREADS.getInt());
		final StringBuilder report = new StringBuilder();
		Date start = new Date();
		Date end = null;
		try {
			this.log.info("##### Import Process Started #####");
			importer.doImport(this.objectStore, this.sessionManager, this.fileSystem,
				Setting.POST_PROCESS_IMPORT.getBoolean());
			this.log.info("##### Import Process Completed #####");
		} catch (Throwable t) {
			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			report.append(String.format("%n%nException caught while attempting an export%n%n"));
			t.printStackTrace(pw);
			report.append(sw.toString());
		} finally {
			end = new Date();
			// unlock
		}

		DateFormat dateFormat = new SimpleDateFormat(Constant.JAVA_SQL_DATETIME_PATTERN);
		long duration = (end.getTime() - start.getTime());
		long hours = TimeUnit.HOURS.convert(duration, TimeUnit.MILLISECONDS);
		duration -= hours * TimeUnit.MILLISECONDS.convert(1, TimeUnit.HOURS);
		long minutes = duration / (TimeUnit.MINUTES.convert(duration, TimeUnit.MILLISECONDS));
		duration -= minutes * TimeUnit.MILLISECONDS.convert(1, TimeUnit.MINUTES);
		long seconds = duration / (TimeUnit.SECONDS.convert(duration, TimeUnit.MILLISECONDS));
		report.append(String.format("Import process start    : %s%n", dateFormat.format(start)));
		report.append(String.format("Import process end      : %s%n", dateFormat.format(end)));
		report.append(String.format("Import process duration : %02d:%02d:%02d%n", hours, minutes, seconds));

		CmsCounter<CmsImportResult> counter = importer.getCounter();
		for (CmsObjectType t : CmsObjectType.values()) {
			report.append(String.format("%n%n%n"));
			report.append(counter.generateReport(t));
		}
		report.append(String.format("%n%n%n"));
		report.append(counter.generateCummulativeReport());

		String reportString = report.toString();
		this.log.info(String.format("Action report for import operation:%n%n%s%n", reportString));
		try {
			CMSMFUtils.postCmsmfMail(String.format("Action report for CMSMF Import"), reportString);
		} catch (MessagingException e) {
			this.log.error("Exception caught attempting to send the report e-mail", e);
		}
	}
}
