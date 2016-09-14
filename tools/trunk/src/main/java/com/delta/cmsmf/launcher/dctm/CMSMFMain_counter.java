package com.delta.cmsmf.launcher.dctm;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
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
import com.armedia.cmf.engine.tools.PooledWorkers;
import com.armedia.commons.dfc.pool.DfcSessionFactory;
import com.armedia.commons.dfc.pool.DfcSessionPool;
import com.armedia.commons.utilities.Tools;
import com.delta.cmsmf.cfg.CLIParam;
import com.delta.cmsmf.cfg.Setting;
import com.delta.cmsmf.exception.CMSMFException;
import com.delta.cmsmf.launcher.AbstractCMSMFMain;
import com.documentum.fc.client.IDfCollection;
import com.documentum.fc.client.IDfFolder;
import com.documentum.fc.client.IDfLocalTransaction;
import com.documentum.fc.client.IDfQuery;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.common.DfException;
import com.documentum.fc.common.DfId;
import com.documentum.fc.common.IDfId;
import com.documentum.fc.common.IDfValue;

/**
 * The main method of this class is an entry point for the cmsmf application.
 *
 * @author Shridev Makim 6/15/2010
 */
public class CMSMFMain_counter extends AbstractCMSMFMain<ExportEngineListener, ExportEngine<?, ?, ?, ?, ?, ?>> {

	private static final String HEADERS = "FOLDER_ID,FOLDER_PATH,CHILD_COUNT,CHILD_SIZE";

	private static final String LISTER = "select r_object_id from dm_folder where folder(ID(%s), DESCEND)";
	private static final String COUNTER = "select count(*) from dm_sysobject (ALL) where folder(ID(%s)) and not type(dm_folder)";
	private static final String SIZER = "select sum(r_full_content_size) from dm_sysobject (ALL) where folder(ID(%s)) and not type(dm_folder)";

	private static class CounterResult implements Comparable<CounterResult> {
		private final IDfId id;
		private final String path;
		private final long count;
		private final long size;

		private volatile String string;

		/**
		 * @param id
		 * @param path
		 * @param count
		 * @param size
		 */
		public CounterResult(IDfId id, String path, long count, long size) {
			this.id = id;
			this.path = path;
			this.count = count;
			this.size = size;
		}

		public boolean isEmpty() {
			return (this.count == 0);
		}

		@Override
		public String toString() {
			if (this.string == null) {
				synchronized (this) {
					if (this.string == null) {
						this.string = String.format("%s,\"%s\",%d,%d", this.id, this.path.replaceAll("\"", "\"\""),
							this.count, this.size);
					}
				}
			}
			return this.string;
		}

		@Override
		public int compareTo(CounterResult o) {
			if (o == null) { return 1; }
			int r = Tools.compare(this.path, o.path);
			if (r != 0) { return r; }
			r = Tools.compare(this.id.getId(), o.id.getId());
			if (r != 0) { return r; }
			return 0;
		}

		@Override
		public int hashCode() {
			return Tools.hashTool(this, null, this.path, this.id);
		}

		@Override
		public boolean equals(Object obj) {
			if (!Tools.baseEquals(this, obj)) { return false; }
			return (compareTo(CounterResult.class.cast(obj)) == 0);
		}
	}

	public CMSMFMain_counter() throws Throwable {
		super(DctmExportEngine.getExportEngine(), false, false);
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

		try {
			final Logger manifest = Logger.getLogger("manifest");
			String folderPath = CLIParam.count_path.getString();
			if (StringUtils.isEmpty(
				folderPath)) { throw new CMSMFException("Must provide a cabinet name to count the objects for"); }

			final List<CounterResult> results = Collections.synchronizedList(new ArrayList<CounterResult>());

			final PooledWorkers<IDfSession, IDfId> workers = new PooledWorkers<IDfSession, IDfId>() {

				private IDfLocalTransaction localTx = null;

				@Override
				protected IDfSession prepare() throws Exception {
					IDfSession s = pool.acquireSession();
					if (s.isTransactionActive()) {
						this.localTx = s.beginTransEx();
					} else {
						s.beginTrans();
					}
					return s;
				}

				private CounterResult doWork(IDfSession session, IDfId id) throws DfException, CMSMFException {
					IDfFolder folder = session.getFolderBySpecification(id.getId());
					if (folder == null) {
						CMSMFMain_counter.this.console.warn("Failed to locate the folder with ID [{}]", id.getId());
						return null;
					}

					final String path = folder.getFolderPath(0);
					final String quotedId = DfUtils.quoteString(id.getId());

					IDfCollection queryResult = null;
					queryResult = DfUtils.executeQuery(session, String.format(CMSMFMain_counter.COUNTER, quotedId),
						IDfQuery.DF_EXECREAD_QUERY);
					final IDfValue count;
					try {
						if (!queryResult.next()) { throw new CMSMFException(
							String.format("Counter query for [%s] did not return any values", path)); }
						count = queryResult.getValueAt(0);
					} finally {
						DfUtils.closeQuietly(queryResult);
						queryResult = null;
					}

					long c = 0;
					long s = 0;
					final Double cDouble = count.asDouble();
					if (cDouble.longValue() > 0) {
						queryResult = DfUtils.executeQuery(session, String.format(CMSMFMain_counter.SIZER, quotedId),
							IDfQuery.DF_EXECREAD_QUERY);
						final IDfValue size;
						try {
							if (!queryResult.next()) { throw new CMSMFException(
								String.format("Sizer query for [%s] did not return any values", path)); }
							size = queryResult.getValueAt(0);
							Double sDouble = size.asDouble();
							c = cDouble.longValue();
							s = sDouble.longValue();
						} finally {
							DfUtils.closeQuietly(queryResult);
							queryResult = null;
						}
					}
					return new CounterResult(id, path, c, s);
				}

				@Override
				protected void process(IDfSession session, IDfId id) throws Exception {
					if ((id == null) || id.isNull()) { return; }
					CounterResult result = doWork(session, id);
					if (result == null) { return; }
					CMSMFMain_counter.this.console.info(result.toString());
					results.add(result);
				}

				@Override
				protected void cleanup(IDfSession session) {
					try {
						if (this.localTx != null) {
							session.abortTransEx(this.localTx);
						} else {
							session.abortTrans();
						}
					} catch (DfException e) {
						this.log.warn("Exception caught aborting a read-only transaction", e);
					} finally {
						this.localTx = null;
						pool.releaseSession(session);
					}
				}
			};

			try {
				final IDfSession session = pool.acquireSession();
				final Set<String> traversed = new HashSet<String>();

				workers.start(Setting.THREADS.getInt(), DfId.DF_NULLID, true);
				try {
					session.beginTrans();

					IDfFolder root = session.getFolderByPath(folderPath);
					if (root == null) { throw new CMSMFException(
						String.format("Could not find the folder at [%s]", folderPath)); }

					this.console.info(String.format("##### Counter Process Started for [%s] #####", folderPath));
					manifest.info(CMSMFMain_counter.HEADERS);
					workers.addWorkItem(root.getObjectId());

					if (!CLIParam.non_recursive.isPresent()) {
						String dql = String.format(CMSMFMain_counter.LISTER,
							DfUtils.quoteString(root.getObjectId().getId()));
						IDfCollection c = DfUtils.executeQuery(session, dql, IDfQuery.DF_EXECREAD_QUERY);
						try {
							int count = 0;
							while (c.next()) {
								IDfId id = c.getValueAt(0).asId();
								if ((id == null) || id.isNull()) {
									continue;
								}
								if (traversed.add(id.getId())) {
									workers.addWorkItem(id);
									if ((++count % 1000) == 0) {
										this.console.info("Submitted {} folders for analysis", count);
									}
								}
							}
							this.console.info("Submitted a total of {} folders for analysis", count);
						} finally {
							DfUtils.closeQuietly(c);
						}
					}
				} finally {
					workers.waitForCompletion();
					this.console.info("##### Counter Process Finished #####");
					try {
						session.abortTrans();
					} catch (DfException e) {
						this.log.error("Exception caught while aborting the transaction for folder lists", e);
					}
					pool.releaseSession(session);
				}
			} catch (Throwable t) {
				StringWriter sw = new StringWriter();
				PrintWriter pw = new PrintWriter(sw);
				report.append(
					String.format("%n%nException caught while attempting an object count for [%s]%n%n", folderPath));
				t.printStackTrace(pw);
				exceptionReport = sw.toString();
			} finally {
				// unlock
				end = new Date();
			}

			this.console.info("Sorting the obtained results ({} entries)", results.size());
			Collections.sort(results);
			this.console.info("Results sorted, outputting the manifest", results.size());
			final boolean excludeEmpty = !CLIParam.count_empty.isPresent();
			for (CounterResult r : results) {
				if (r.isEmpty() && excludeEmpty) {
					continue;
				}
				manifest.info(r.toString());
			}
			this.console.info("Manifest completed", results.size());

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
				if (!p.isPresent()) {
					continue;
				}
				String v = p.getString();
				if (v == null) {
					report.append(String.format("%n\t--%s", p.option.getLongOpt()));
				} else {
					report.append(String.format("%n\t--%s = [%s]", p.option.getLongOpt(), v));
				}
			}

			report.append(String.format("%n%n%nSettings in use:%n")).append(StringUtils.repeat("=", 30));
			for (Setting s : Setting.values()) {
				report.append(String.format("%n\t%s = [%s]", s.name, s.getString()));
			}

			if (exceptionReport != null) {
				report.append(String.format("%n%n%nEXCEPTION REPORT FOLLOWS:%n%n")).append(exceptionReport);
				this.console.info(exceptionReport);
			}
			this.log.info(report.toString());
		} finally {
			pool.close();
		}
	}
}