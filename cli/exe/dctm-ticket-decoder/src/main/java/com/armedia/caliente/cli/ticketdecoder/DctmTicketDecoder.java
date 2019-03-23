package com.armedia.caliente.cli.ticketdecoder;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.caliente.cli.OptionValues;
import com.armedia.caliente.cli.ticketdecoder.xml.Content;
import com.armedia.caliente.cli.utils.DfcLaunchHelper;
import com.armedia.caliente.cli.utils.ThreadsLaunchHelper;
import com.armedia.caliente.tools.dfc.DctmCrypto;
import com.armedia.commons.dfc.pool.DfcSessionPool;
import com.armedia.commons.utilities.CloseableIterator;
import com.armedia.commons.utilities.concurrent.ReadWriteSet;
import com.armedia.commons.utilities.line.LineScanner;
import com.documentum.fc.common.DfException;

public class DctmTicketDecoder {
	private final Logger log = LoggerFactory.getLogger(getClass());

	private final DfcLaunchHelper dfcLaunchHelper;
	private final ThreadsLaunchHelper threadHelper;

	public DctmTicketDecoder(DfcLaunchHelper dfcLaunchHelper, ThreadsLaunchHelper threadHelper) {
		this.dfcLaunchHelper = dfcLaunchHelper;
		this.threadHelper = threadHelper;
	}

	private TicketDecoder buildTicketDecoder(DfcSessionPool pool, Set<String> scannedIds, String source) {
		if (source.startsWith("%")) { return new SingleTicketDecoder(pool, scannedIds, source); }
		if (source.startsWith("/")) { return new PathTicketDecoder(pool, scannedIds, source); }
		return new PredicateTicketDecoder(pool, scannedIds, source);
	}

	private void formatResults(Content record) {
		this.log.info("{}", record);
	}

	protected int run(OptionValues cli, Collection<String> sources) throws Exception {
		final boolean debug = cli.isPresent(CLIParam.debug);

		final String docbase = this.dfcLaunchHelper.getDfcDocbase(cli);
		final String user = this.dfcLaunchHelper.getDfcUser(cli);
		final String password = this.dfcLaunchHelper.getDfcPassword(cli);
		final int threads = this.threadHelper.getThreads(cli);

		final CloseableIterator<String> sourceIterator = new LineScanner().iterator(sources);

		final DfcSessionPool pool;
		try {
			pool = new DfcSessionPool(docbase, user, new DctmCrypto().decrypt(password));
		} catch (DfException e) {
			this.log.error("Failed to create the DFC session pool", e);
			return 1;
		}

		final Set<String> scannedIds = new ReadWriteSet<>(new HashSet<>());

		try (Stream<String> sourceStream = sourceIterator.stream()) {
			final List<Future<Collection<Content>>> futures = new LinkedList<>();
			final ExecutorService executors = Executors.newFixedThreadPool(Math.max(1, threads));
			final Set<String> submittedSources = new HashSet<>();
			sourceStream //
				.filter((source) -> submittedSources.add(source)) //
				.forEach((source) -> {
					TicketDecoder decoder = buildTicketDecoder(pool, scannedIds, source);
					futures.add(executors.submit(decoder));
				}) //
			;

			this.log.info("Submitted {} history search%s...", submittedSources.size(),
				submittedSources.size() > 1 ? "es" : "");
			executors.shutdown();

			int ret = 0;
			this.log.info("Retrieving data from the background workers...");
			this.log.info("RESULTS:");
			for (Future<Collection<Content>> f : futures) {
				try {
					f.get().forEach(this::formatResults);
				} catch (ExecutionException e) {
					Throwable cause = e.getCause();
					if (DctmDicketDecoderException.class.isInstance(cause)) {
						DctmDicketDecoderException he = DctmDicketDecoderException.class.cast(cause);
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