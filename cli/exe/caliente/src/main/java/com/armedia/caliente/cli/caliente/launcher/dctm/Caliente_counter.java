package com.armedia.caliente.cli.caliente.launcher.dctm;

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

import com.armedia.caliente.cli.caliente.cfg.CLIParam;
import com.armedia.caliente.cli.caliente.cfg.Setting;
import com.armedia.caliente.cli.caliente.exception.CalienteException;
import com.armedia.caliente.cli.caliente.launcher.AbstractCalienteModule;
import com.armedia.caliente.engine.dfc.DctmAttributes;
import com.armedia.caliente.engine.dfc.exporter.DctmExportEngine;
import com.armedia.caliente.engine.exporter.ExportEngine;
import com.armedia.caliente.engine.exporter.ExportEngineListener;
import com.armedia.commons.dfc.pool.DfcSessionFactory;
import com.armedia.commons.dfc.pool.DfcSessionPool;
import com.armedia.commons.dfc.util.DfUtils;
import com.armedia.commons.utilities.PooledWorkers;
import com.armedia.commons.utilities.Tools;
import com.documentum.fc.client.IDfCollection;
import com.documentum.fc.client.IDfFolder;
import com.documentum.fc.client.IDfLocalTransaction;
import com.documentum.fc.client.IDfQuery;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.common.DfException;
import com.documentum.fc.common.IDfId;
import com.documentum.fc.common.IDfValue;

/**
 * The main method of this class is an entry point for the Caliente application.
 *
 * @author Shridev Makim 6/15/2010
 */
public class Caliente_counter extends AbstractCalienteModule<ExportEngineListener, ExportEngine<?, ?, ?, ?, ?, ?>> {

	private static final String HEADERS = "FOLDER_ID,FOLDER_PATH,CHILD_COUNT,CHILD_SIZE";

	private static final String FOLDER_LISTER = "select r_object_id from dm_folder where folder(ID(%s), DESCEND)";
	private static final String CABINET_ALL_LISTER = "select distinct r_object_id from dm_cabinet";
	// private static final String CABINET_PUBPRIV_LISTER = "select distinct r_object_id from
	// dm_cabinet where is_public = %s";
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

			// Case-insensitive sort...
			int r = Tools.compare(this.path.toLowerCase(), o.path.toLowerCase());
			if (r != 0) { return r; }
			// If case insensitive fails to decide, then let the case decide
			r = Tools.compare(this.path, o.path);
			if (r != 0) { return r; }
			// Path is not enough, look at the object's ID
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

	public Caliente_counter() throws Throwable {
		super(DctmExportEngine.getExportEngine(), false, false, false);
	}

	/**
	 * Starts exporting objects from source directory. It reads up the query from properties file
	 * and executes it against the source repository. It retrieves objects from the repository and
	 * exports it out.
	 *
	 */
	@Override
	public void run() throws CalienteException {

		final Date start = new Date();
		Date end = null;
		StringBuilder report = new StringBuilder();
		String exceptionReport = null;

		Map<String, Object> settings = new HashMap<>();
		if (this.server != null) {
			settings.put(DfcSessionFactory.DOCBASE, this.server);
		}
		if (this.user != null) {
			settings.put(DfcSessionFactory.USERNAME, this.user);
		}
		if (this.password != null) {
			settings.put(DfcSessionFactory.PASSWORD, this.password);
		}

		List<String> includedPaths = CLIParam.count_include.getAllString();
		List<String> excludedPaths = CLIParam.count_exclude.getAllString();
		final boolean countEmpty = CLIParam.count_empty.getBoolean(false);
		final Boolean privateMode = CLIParam.count_private.getBoolean();
		final Boolean hiddenMode = CLIParam.count_hidden.getBoolean();

		final DfcSessionPool pool;
		try {
			pool = new DfcSessionPool(settings);
		} catch (Exception e) {
			throw new CalienteException("Failed to initialize the connection pool", e);
		}

		try {
			final Logger manifest = Logger.getLogger("manifest");

			final List<CounterResult> results = Collections.synchronizedList(new ArrayList<CounterResult>());

			final PooledWorkers<DfcSessionPool, IDfSession, IDfId> workers = new PooledWorkers<DfcSessionPool, IDfSession, IDfId>() {

				private IDfLocalTransaction localTx = null;

				@Override
				protected IDfSession initialize(DfcSessionPool pool) throws Exception {
					IDfSession s = pool.acquireSession();
					if (s.isTransactionActive()) {
						this.localTx = s.beginTransEx();
					} else {
						s.beginTrans();
					}
					return s;
				}

				private CounterResult doWork(IDfSession session, IDfId id) throws DfException, CalienteException {
					IDfFolder folder = session.getFolderBySpecification(id.getId());
					if (folder == null) {
						Caliente_counter.this.console.warn("Failed to locate the folder with ID [{}]", id.getId());
						return null;
					}

					final String path = folder.getFolderPath(0);
					final String quotedId = DfUtils.quoteString(id.getId());

					IDfCollection queryResult = null;
					queryResult = DfUtils.executeQuery(session, String.format(Caliente_counter.COUNTER, quotedId),
						IDfQuery.DF_EXECREAD_QUERY);
					final IDfValue count;
					try {
						if (!queryResult.next()) { throw new CalienteException(
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
						queryResult = DfUtils.executeQuery(session, String.format(Caliente_counter.SIZER, quotedId),
							IDfQuery.DF_EXECREAD_QUERY);
						final IDfValue size;
						try {
							if (!queryResult.next()) { throw new CalienteException(
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
					Caliente_counter.this.console.info(result.toString());
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

			String activity = "retrieving the list of cabinets";
			try {
				final IDfSession session = pool.acquireSession();
				final Set<IDfId> traversed = new HashSet<>();

				workers.start(pool, Setting.THREADS.getInt(), "Counter", true);
				try {
					session.beginTrans();

					// If paths are both included and excluded, then the exclusions are applied to
					// the included set. First, we identify the excluded folders so we may filter
					// them out. It's OK for exclusions to not exist...
					Set<IDfId> excludedIds = new HashSet<>();
					for (String folderSpec : excludedPaths) {
						IDfFolder f = session.getFolderBySpecification(folderSpec);
						if (f != null) {
							excludedIds.add(f.getObjectId());
						}
					}
					excludedIds = Tools.freezeSet(excludedIds);

					if (includedPaths.isEmpty()) {
						// Make sure we use a list we can modify...
						includedPaths = new ArrayList<>();
						// No paths...we find all cabinets then
						IDfCollection c = null;
						String dql = Caliente_counter.CABINET_ALL_LISTER;
						try {
							c = DfUtils.executeQuery(session, dql);
							while (c.next()) {
								IDfId id = c.getId("r_object_id");
								if ((id != null) && !id.isNull()) {
									includedPaths.add(id.getId());
								}
							}
						} finally {
							DfUtils.closeQuietly(c);
						}
					}

					Set<String> includedFolders = new HashSet<>();
					for (String folderSpec : includedPaths) {
						IDfFolder f = session.getFolderBySpecification(folderSpec);
						if (f == null) {
							this.log.warn(
								"Failed to locate the folder specified by [{}] - its contents will not be counted",
								folderSpec);
							continue;
						}
						includedFolders.add(f.getFolderPath(0));
					}
					includedFolders = Tools.freezeSet(includedFolders);

					// Simple trick to make sure we don't visit any of the folders we're not
					// interested in
					traversed.addAll(excludedIds);

					int count = 0;
					manifest.info(Caliente_counter.HEADERS);
					for (String folderPath : includedFolders) {
						activity = String.format("analyzing the contents of folder [%s]", folderPath);
						IDfFolder folder = session.getFolderByPath(folderPath);
						if (folder == null) { throw new CalienteException(
							String.format("Could not find the folder at [%s]", folderPath)); }

						if (traversed.contains(folder.getObjectId())) {
							// We've already been here, so we skip it.
							continue;
						}

						// Is this a folder that should be excluded due to privacy mode?
						final boolean folderPrivate = folder.hasAttr(DctmAttributes.IS_PRIVATE)
							&& folder.getBoolean(DctmAttributes.IS_PRIVATE);
						if ((privateMode != null) && !privateMode.booleanValue() && folderPrivate) {
							continue;
						}

						// Is this a folder that should be excluded due to hidden mode?
						final boolean folderHidden = folder.getBoolean(DctmAttributes.A_IS_HIDDEN);
						if ((hiddenMode != null) && !hiddenMode.booleanValue() && folderHidden) {
							continue;
						}

						activity = String.format("analyzing the contents of folder [%s]", folderPath);
						this.console.info(String.format("##### Counter Process Started for [%s] #####", folderPath));
						workers.addWorkItem(folder.getObjectId());

						if (!CLIParam.non_recursive.isPresent()) {
							String dql = String.format(Caliente_counter.FOLDER_LISTER,
								DfUtils.quoteString(folder.getObjectId().getId()));
							IDfCollection c = DfUtils.executeQuery(session, dql, IDfQuery.DF_EXECREAD_QUERY);
							try {
								while (c.next()) {
									IDfId id = c.getValueAt(0).asId();
									if ((id == null) || id.isNull()) {
										continue;
									}
									if (traversed.add(id)) {
										workers.addWorkItem(id);
										if ((++count % 1000) == 0) {
											this.console.info("Submitted {} folders for analysis", count);
										}
									}
								}
							} finally {
								DfUtils.closeQuietly(c);
							}
						}
					}
					this.console.info("Submitted a total of {} folders for analysis", count);
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
				report.append(String.format("%n%nException caught while %s%n%n", activity));
				t.printStackTrace(pw);
				exceptionReport = sw.toString();
			} finally {
				// unlock
				end = new Date();
			}

			this.console.info("Sorting the obtained results ({} entries)", results.size());
			Collections.sort(results);
			this.console.info("Results sorted, outputting the manifest", results.size());
			for (CounterResult r : results) {
				if (r.isEmpty() && !countEmpty) {
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