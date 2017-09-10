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
import com.armedia.commons.utilities.Tools;
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

	private final DfcLaunchHelper dfcLaunchHelper;
	private final ThreadsLaunchHelper threadHelper;

	public History(DfcLaunchHelper dfcLaunchHelper, ThreadsLaunchHelper threadHelper) {
		this.dfcLaunchHelper = dfcLaunchHelper;
		this.threadHelper = threadHelper;
	}

	protected Set<String> cleanChronicleIds(Collection<String> chronicleIds) {
		Set<String> s = new LinkedHashSet<>();
		for (String id : chronicleIds) {
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
					IDfId chronicleId = new DfId(id);

					@Override
					public String call() throws HistoryException {
						final IDfSession session;
						try {
							session = pool.acquireSession();
						} catch (Exception e) {
							throw new HistoryException(this.chronicleId,
								String.format("Failed to acquire a Documentum session to read the chronicle [%s]",
									this.chronicleId),
								e);
						}

						try {
							boolean ok = false;
							final IDfLocalTransaction tx;
							try {
								tx = DfUtils.openTransaction(session);
							} catch (DfException e) {
								throw new HistoryException(this.chronicleId,
									String.format(
										"DFC reported an error while starting the read-only transaction for chronicle [%s]: %s",
										this.chronicleId, e.getMessage()),
									e);
							}

							try {
								try {
									// It may be an object ID, not a chronicle ID, so make the
									// distinction...
									IDfPersistentObject obj = null;
									boolean changed = false;
									try {
										obj = session.getObject(this.chronicleId);
										if (IDfSysObject.class.isInstance(obj)) {
											IDfId oldId = this.chronicleId;
											this.chronicleId = IDfSysObject.class.cast(obj).getChronicleId();
											changed = !Tools.equals(oldId, this.chronicleId);
										} else {
											throw new HistoryException(this.chronicleId,
												"The given ID is an object ID, but the object is not a dm_sysobject");
										}
									} catch (DfIdNotFoundException e) {
										// It's not an object ID, so it must be a chronicle ID...
									}

									if (changed) {
										History.this.log.info("Scanning history with ID [{}] (from object ID [{}])...",
											this.chronicleId, id);
									} else {
										History.this.log.info("Scanning history with ID [{}]...", this.chronicleId);
									}
									DctmVersionHistory<IDfSysObject> history = new DctmVersionHistory<>(session,
										this.chronicleId);
									History.this.log.info("History with ID [{}] scanned successfully!",
										this.chronicleId);

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
									throw new HistoryException(this.chronicleId,
										String.format(
											"DFC reported an error while retrieving the history for chronicle [%s]: %s",
											this.chronicleId, e.getMessage()),
										e);
								} catch (DctmException e) {
									throw new HistoryException(this.chronicleId,
										String.format("The history for chronicle [%s] is inconsistent: %s",
											this.chronicleId, e.getMessage()),
										e);
								} finally {
									if (!ok) {
										History.this.log.error(
											"An error was encountered while processing chronicle [{}]",
											this.chronicleId);
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
										this.chronicleId, e);
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