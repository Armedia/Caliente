package com.armedia.caliente.cli.history;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.regex.Pattern;

import org.apache.commons.lang3.time.DateFormatUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.caliente.cli.OptionValues;
import com.armedia.caliente.cli.utils.DfcLaunchHelper;
import com.armedia.caliente.cli.utils.ThreadsLaunchHelper;
import com.armedia.caliente.tools.dfc.DctmCrypto;
import com.armedia.commons.dfc.pool.DfcSessionPool;
import com.armedia.commons.dfc.util.DctmException;
import com.armedia.commons.dfc.util.DctmVersion;
import com.armedia.commons.dfc.util.DctmVersionHistory;
import com.armedia.commons.dfc.util.DfUtils;
import com.documentum.fc.client.DfIdNotFoundException;
import com.documentum.fc.client.IDfLocalTransaction;
import com.documentum.fc.client.IDfPersistentObject;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.client.IDfSysObject;
import com.documentum.fc.common.DfException;
import com.documentum.fc.common.DfId;
import com.documentum.fc.common.IDfId;

public class History {
	private final Logger log = LoggerFactory.getLogger(getClass());

	private static final Pattern ID_SCANNER = Pattern.compile("^[0-9a-f]{16}$", Pattern.CASE_INSENSITIVE);

	private final DfcLaunchHelper dfcLaunchHelper;
	private final ThreadsLaunchHelper threadHelper;

	public History(DfcLaunchHelper dfcLaunchHelper, ThreadsLaunchHelper threadHelper) {
		this.dfcLaunchHelper = dfcLaunchHelper;
		this.threadHelper = threadHelper;
	}

	protected Set<String> cleanChronicleIds(Collection<String> chronicleIds) {
		Set<String> s = new LinkedHashSet<>();
		for (String id : chronicleIds) {
			IDfId testId = new DfId(id);
			if (!testId.isObjectId()) {
				// If this is not an object ID, it MUST be a path...
				s.add(id);
				continue;
			}

			if (testId.isNull()) {
				// If this is the null id, we skip it...
				continue;
			}

			// If it's an object ID, we fold it to lowercase
			if (!s.add(id.toLowerCase())) {
				continue;
			}
		}
		return s;
	}

	private String formatVersion(DctmVersion<?> version) {
		if (version == null) { return "(N/A)"; }
		return String.format("%s (%s @ %s)", version.getVersionNumber(), version.getId(),
			DateFormatUtils.ISO_DATE_TIME_ZONE_FORMAT.format(version.getCreationDate().getDate()));
	}

	protected int run(OptionValues cli, Collection<String> chronicleIds) throws Exception {
		final boolean debug = cli.isPresent(CLIParam.debug);

		// Doing it like this helps us eliminate duplicates
		chronicleIds = cleanChronicleIds(chronicleIds);

		final String docbase = this.dfcLaunchHelper.getDfcDocbase(cli);
		final String user = this.dfcLaunchHelper.getDfcUser(cli);
		final String password = this.dfcLaunchHelper.getDfcPassword(cli);

		final int threads = this.threadHelper.getThreads(cli);

		final DfcSessionPool pool;
		try {
			pool = new DfcSessionPool(docbase, user, new DctmCrypto().decrypt(password));
		} catch (DfException e) {
			this.log.error("Failed to create the DFC session pool", e);
			return 1;
		}

		try {
			final List<Future<String>> futures = new ArrayList<>(chronicleIds.size());
			final ExecutorService executors = Executors.newFixedThreadPool(Math.max(1, threads));
			for (final String id : chronicleIds) {
				futures.add(executors.submit(new Callable<String>() {
					@Override
					public String call() throws HistoryException {
						final IDfSession session;
						try {
							session = pool.acquireSession();
						} catch (Exception e) {
							throw new HistoryException(id,
								String.format("Failed to acquire a Documentum session to read the chronicle [%s]", id),
								e);
						}

						try {
							boolean ok = false;
							final IDfLocalTransaction tx;
							try {
								tx = DfUtils.openTransaction(session);
							} catch (DfException e) {
								throw new HistoryException(id,
									String.format(
										"DFC reported an error while starting the read-only transaction for chronicle [%s]: %s",
										id, e.getMessage()),
									e);
							}

							try {
								try {
									IDfId chronicleId = null;
									IDfPersistentObject obj = null;
									if (!History.ID_SCANNER.matcher(id).matches()) {
										// It's a path...
										obj = session.getObjectByPath(id);
										if (obj == null) {
											// No match...skip this one!
											throw new HistoryException(id,
												String.format("No object found at path [%s]", id));
										}
									} else {
										// Must be an ID...
										chronicleId = new DfId(id);
										try {
											obj = session.getObject(chronicleId);
										} catch (DfIdNotFoundException e) {
											// It's not an object ID, so assume it's a chronicle
											// ID...
										}
									}

									if (IDfSysObject.class.isInstance(obj)) {
										chronicleId = IDfSysObject.class.cast(obj).getChronicleId();
									} else {
										throw new HistoryException(id, String
											.format("The given object for search spec [%s] is not a dm_sysobject", id));
									}

									History.this.log.info("Scanning history with ID [{}] (from search spec [{}])...",
										chronicleId, id);
									DctmVersionHistory<IDfSysObject> history = new DctmVersionHistory<>(session,
										chronicleId);
									History.this.log.info("History with ID [{}] scanned successfully!", chronicleId);

									StringWriter w = new StringWriter();
									PrintWriter pw = new PrintWriter(w);

									pw.printf("HISTORY ID: [%s]%n", history.getChronicleId());
									pw.printf("\tTOTAL VERSIONS: %d%n", history.size());
									pw.printf("\tROOT VERSION  : %s%n", formatVersion(history.getRootVersion()));
									pw.printf("\tHEAD VERSION  : %s%n", formatVersion(history.getCurrentVersion()));
									String fmt = String.format("\t\t%%s [%%0%dd]: %%s%n",
										String.valueOf(history.size()).length());
									int i = 0;
									for (DctmVersion<IDfSysObject> v : history) {
										pw.printf(fmt, v == history.getCurrentVersion() ? "*" : " ", ++i,
											formatVersion(v));
									}
									pw.flush();
									w.flush();
									ok = true;
									return w.toString();
								} catch (DfException e) {
									throw new HistoryException(id,
										String.format("DFC reported an error while retrieving the history for [%s]: %s",
											id, e.getMessage()),
										e);
								} catch (DctmException e) {
									throw new HistoryException(id,
										String.format("The history for [%s] is inconsistent: %s", id, e.getMessage()),
										e);
								} finally {
									if (!ok) {
										History.this.log
											.error("An error was encountered while processing chronicle [{}]", id);
									}
								}
							} finally {
								try {
									DfUtils.abortTransaction(session, tx);
								} catch (DfException e) {
									// Here we don't raise an exception since we don't want to blot
									// out any that are already being raised...
									History.this.log.error(
										"DFC reported an error while rolling back the read-only transaction for chronicle [{}]",
										id, e);
								}
							}
						} finally {
							pool.releaseSession(session);
						}
					}
				}));
			}
			this.log.info("Submitted {} history search%s...", chronicleIds.size(), chronicleIds.size() > 1 ? "es" : "");
			executors.shutdown();

			int ret = 0;
			this.log.info("Retrieving data from the background workers...");
			for (Future<String> f : futures) {
				try {
					this.log.info(f.get());
				} catch (ExecutionException e) {
					Throwable cause = e.getCause();
					if (HistoryException.class.isInstance(cause)) {
						HistoryException he = HistoryException.class.cast(cause);
						if (debug) {
							this.log.error(he.getMessage(), he.getCause());
						} else {
							this.log.error("{} (use --debug for more information)", he.getMessage());
						}
					} else {
						this.log.error("An unexpected exception was raised while reading a chronicle", cause);
					}
					ret = 1;
				}
			}
			return ret;
		} finally {
			pool.close();
		}
	}
}