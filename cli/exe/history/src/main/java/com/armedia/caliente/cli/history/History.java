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
import java.util.concurrent.TimeUnit;

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
			try {
				for (String id : chronicleIds) {
					final IDfId chronicleId = new DfId(id);
					futures.add(executors.submit(new Callable<String>() {
						@Override
						public String call() throws HistoryException {
							final IDfSession session;
							try {
								session = pool.acquireSession();
							} catch (Exception e) {
								throw new HistoryException(chronicleId,
									String.format("Failed to acquire a Documentum session to read the chronicle [%s]",
										chronicleId),
									e);
							}
							try {
								History.this.log.info("Scanning history with ID [{}]...", chronicleId);

								DctmVersionHistory<IDfSysObject> history = new DctmVersionHistory<>(session,
									chronicleId);
								StringWriter w = new StringWriter();
								PrintWriter pw = new PrintWriter(w);

								pw.printf("HISTORY ID: [%s]%n", history.getChronicleId());
								pw.printf("\tTOTAL VERSIONS: %d%n", history.size());
								pw.printf("\tROOT VERSION  : %s%n", formatVersion(history.getRootVersion()));
								pw.printf("\tHEAD VERSION  : %s%n", formatVersion(history.getCurrentVersion()));
								String fmt = String.format("\t\t%%s [%%0%dd]: %%s%n", history.size());
								for (DctmVersion<IDfSysObject> v : history) {
									pw.printf(fmt, v == history.getCurrentVersion() ? "*" : " ", formatVersion(v));
								}
								pw.flush();
								w.flush();
								return w.toString();
							} catch (DfException e) {
								throw new HistoryException(chronicleId,
									String.format(
										"DFC reported an error while retrieving the history for chronicle [%s]",
										chronicleId),
									e);
							} catch (DctmException e) {
								throw new HistoryException(chronicleId,
									String.format("The history for chronicle [%s] is inconsistent", chronicleId), e);
							} finally {
								pool.releaseSession(session);
							}
						}
					}));
				}
				executors.shutdown();
			} finally {
				executors.awaitTermination(5, TimeUnit.MINUTES);
			}

			this.log.info("Retrieving data from the background workers...");
			int ret = 0;
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
							this.log.error(he.getMessage());
						}
					} else {
						this.log.error("An exception was caught while reading a chronicle", cause);
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