package com.delta.cmsmf.launcher.dctm;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.log4j.Logger;

import com.armedia.cmf.engine.documentum.DfUtils;
import com.armedia.cmf.engine.documentum.exporter.DctmExportEngine;
import com.armedia.cmf.engine.exporter.ExportEngine;
import com.armedia.cmf.engine.exporter.ExportEngineListener;
import com.armedia.commons.dfc.pool.DfcSessionFactory;
import com.armedia.commons.dfc.pool.DfcSessionPool;
import com.delta.cmsmf.cfg.CLIParam;
import com.delta.cmsmf.cfg.Setting;
import com.delta.cmsmf.exception.CMSMFException;
import com.delta.cmsmf.launcher.AbstractCMSMFMain;
import com.documentum.fc.client.IDfCollection;
import com.documentum.fc.client.IDfFolder;
import com.documentum.fc.client.IDfQuery;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.common.DfException;
import com.documentum.fc.common.IDfValue;

/**
 * The main method of this class is an entry point for the cmsmf application.
 *
 * @author Shridev Makim 6/15/2010
 */
public class CMSMFMain_counter extends AbstractCMSMFMain<ExportEngineListener, ExportEngine<?, ?, ?, ?, ?>> {

	private static final String COUNTER = "select count(*) from dm_sysobject where folder(ID('%s')) and not type(dm_folder)";
	private static final String RECURSOR = "select distinct r_object_id from dm_sysobject where folder(ID('%s')) and type(dm_folder)";

	CMSMFMain_counter() throws Throwable {
		super(DctmExportEngine.getExportEngine());
	}

	private void printFolderCounts(Set<String> traversed, IDfFolder folder, Logger manifest) throws CMSMFException,
		DfException {
		// If we're already traversed, we skip it
		final String id = folder.getObjectId().getId();
		if (!traversed.add(id)) { return; }
		final IDfSession session = folder.getSession();
		final String path = folder.getFolderPath(0);
		IDfCollection result = null;
		result = DfUtils
			.executeQuery(session, String.format(CMSMFMain_counter.COUNTER, id), IDfQuery.DF_EXECREAD_QUERY);
		if (!result.next()) { throw new CMSMFException("Counter query did not return any values"); }
		final IDfValue count = result.getValueAt(0);
		DfUtils.closeQuietly(result);
		if (count.asInteger() > 0) {
			String msg = String.format("%s,%s,%d", id, path, count.asInteger());
			this.console.info(msg);
			manifest.info(msg);
		}

		if (CLIParam.non_recursive.isPresent()) { return; }

		// Recurse into its children
		result = DfUtils.executeQuery(session, String.format(CMSMFMain_counter.RECURSOR, id),
			IDfQuery.DF_EXECREAD_QUERY);
		List<String> children = new ArrayList<String>();
		while (result.next()) {
			children.add(result.getString("r_object_id"));
		}
		DfUtils.closeQuietly(result);
		for (String childId : children) {
			IDfFolder child = session.getFolderBySpecification(childId);
			if (child == null) {
				// Warning
				this.log.warn(String.format(
					"Failed to find the child folder with ID [%s] (referenced by parent [%s](%s))", childId, path, id));
				continue;
			}
			printFolderCounts(traversed, child, manifest);
		}
	}

	/**
	 * Starts exporting objects from source directory. It reads up the query from properties file
	 * and executes it against the source repository. It retrieves objects from the repository and
	 * exports it out.
	 *
	 */
	@Override
	public void run() throws CMSMFException {

		final Date start = new Date();
		Date end = null;
		StringBuilder report = new StringBuilder();
		String exceptionReport = null;

		Map<String, Object> settings = new HashMap<String, Object>();
		if (this.server != null) {
			settings.put(DfcSessionFactory.DOCBASE, this.server);
		}
		if (this.user != null) {
			settings.put(DfcSessionFactory.USERNAME, this.user);
		}
		if (this.password != null) {
			settings.put(DfcSessionFactory.PASSWORD, this.password);
		}

		final DfcSessionPool pool;
		try {
			pool = new DfcSessionPool(settings);
		} catch (Exception e) {
			throw new CMSMFException("Failed to initialize the connection pool", e);
		}

		final Logger manifest = Logger.getLogger("manifest");
		String folderPath = CLIParam.count_path.getString();
		if (StringUtils.isEmpty(folderPath)) { throw new CMSMFException(
			"Must provide a cabinet name to count the objects for"); }
		try {
			final IDfSession session = pool.acquireSession();
			final Set<String> traversed = new HashSet<String>();

			try {
				session.beginTrans();
				IDfFolder folder = session.getFolderByPath(folderPath);
				if (folder == null) { throw new CMSMFException(String.format("Could not find the cabinet at [%s]",
					folderPath)); }
				this.console.info(String.format("##### Counter Process Started for [%s] #####", folderPath));
				this.log.info(String.format("##### Counter Process Started for [%s] #####", folderPath));
				String msg = "FOLDER_ID,FOLDER_PATH,CHILD_COUNT";
				this.console.info(msg);
				manifest.info(msg);
				printFolderCounts(traversed, folder, manifest);
			} finally {
				this.console.info("##### Counter Process Finished #####");
				this.log.info("##### Counter Process Finished #####");
				try {
					session.abortTrans();
				} catch (DfException e) {
					this.log.error("Exception caught while aborting the transaction for object counts", e);
				}
				pool.releaseSession(session);
			}
		} catch (Throwable t) {
			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			report.append(String.format("%n%nException caught while attempting an object count for [%s]%n%n",
				folderPath));
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
		report.append(String.format("Counter process start    : %s%n",
			DateFormatUtils.format(start, DateFormatUtils.ISO_DATETIME_TIME_ZONE_FORMAT.getPattern())));
		report.append(String.format("Counter process end      : %s%n",
			DateFormatUtils.format(end, DateFormatUtils.ISO_DATETIME_TIME_ZONE_FORMAT.getPattern())));
		report.append(String.format("Counter process duration : %02d:%02d:%02d%n", hours, minutes, seconds));

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

		if (exceptionReport != null) {
			report.append(String.format("%n%n%nEXCEPTION REPORT FOLLOWS:%n%n")).append(exceptionReport);
		}
	}

	@Override
	public boolean requiresCleanData() {
		return false;
	}
}