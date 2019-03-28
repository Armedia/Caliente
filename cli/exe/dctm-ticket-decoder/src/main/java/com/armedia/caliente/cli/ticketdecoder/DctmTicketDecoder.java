package com.armedia.caliente.cli.ticketdecoder;

import java.io.File;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
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
import com.armedia.commons.utilities.PooledWorkers;
import com.armedia.commons.utilities.Tools;
import com.armedia.commons.utilities.concurrent.ReadWriteSet;
import com.armedia.commons.utilities.line.LineScanner;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.common.DfException;
import com.documentum.fc.common.IDfId;

public class DctmTicketDecoder {

	private final Logger log = LoggerFactory.getLogger(getClass());

	private final DfcLaunchHelper dfcLaunchHelper;
	private final ThreadsLaunchHelper threadHelper;

	public DctmTicketDecoder(DfcLaunchHelper dfcLaunchHelper, ThreadsLaunchHelper threadHelper) {
		this.dfcLaunchHelper = dfcLaunchHelper;
		this.threadHelper = threadHelper;
	}

	private ContentFinder buildContentFinder(DfcSessionPool pool, Set<String> scannedIds, String source,
		Consumer<IDfId> consumer) {
		if (source.startsWith("%")) { return new SingleContentFinder(pool, scannedIds, source, consumer); }
		if (source.startsWith("/")) { return new PathContentFinder(pool, scannedIds, source, consumer); }
		return new PredicateContentFinder(pool, scannedIds, source, consumer);
	}

	protected int run(OptionValues cli) throws Exception {
		// final boolean debug = cli.isPresent(CLIParam.debug);
		final Collection<String> sources = cli.getStrings(CLIParam.from);
		final PersistenceFormat format = cli.getEnum(PersistenceFormat.class, CLIParam.format);
		final File target = Tools.canonicalize(new File(cli.getString(CLIParam.target)));
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

		final BlockingQueue<Content> contents = new LinkedBlockingQueue<>();
		final AtomicBoolean running = new AtomicBoolean(true);
		Thread persistenceThread = null;

		final Set<String> scannedIds = new ReadWriteSet<>(new HashSet<>());

		int ret = 1;
		try (Stream<String> sourceStream = sourceIterator.stream()) {
			final PooledWorkers<IDfSession, IDfId> extractors = new PooledWorkers<>();
			final Consumer<Content> queueConsumer = (c) -> {
				try {
					this.log.debug("Queueing {}", c);
					contents.put(c);
				} catch (InterruptedException e) {
					this.log.error("Failed to queue Content object {}", c, e);
				}
			};
			final ExtractorLogic extractorLogic = new ExtractorLogic(pool //
				, queueConsumer //
				, cli.getString(CLIParam.content_filter) //
				, cli.getString(CLIParam.rendition_filter) //
				, cli.getStrings(CLIParam.prefer_rendition) //
			);

			;
			final AtomicLong submittedCounter = new AtomicLong(0);
			final AtomicLong submitFailedCounter = new AtomicLong(0);
			final AtomicLong renderedCounter = new AtomicLong(0);
			final AtomicLong renderFailedCounter = new AtomicLong(0);
			try (ContentPersistor persistor = format.newPersistor()) {
				persistor.initialize(target);

				final Set<String> submittedSources = new HashSet<>();
				this.log.info("Starting the background searches...");
				extractors.start(extractorLogic, Math.max(1, threads), "Extractor", true);
				try {
					persistenceThread = new Thread(() -> {
						while (true) {
							final Content c;
							try {
								if (running.get()) {
									c = contents.take();
								} else {
									c = contents.poll();
								}
								if (c == null) { return; }
							} catch (InterruptedException e) {
								continue;
							}
							this.log.info("{}", c);
							try {
								persistor.persist(c);
								renderedCounter.incrementAndGet();
							} catch (Exception e) {
								renderFailedCounter.incrementAndGet();
								this.log.error("Failed to marshal the content object {}", c, e);
							}
						}
					});
					persistenceThread.setDaemon(true);
					running.set(true);
					persistenceThread.start();

					sourceStream //
						.filter((source) -> submittedSources.add(source)) //
						.forEach((source) -> {
							try {
								buildContentFinder(pool, scannedIds, source, (id) -> {
									try {
										this.log.debug("Submitting {}", id);
										extractors.addWorkItem(id);
										submittedCounter.incrementAndGet();
									} catch (InterruptedException e) {
										submitFailedCounter.incrementAndGet();
										this.log.error("Failed to add ID [{}] to the work queue", id, e);
									}
								}).call();
							} catch (Exception e) {
								this.log.error("Failed to search for elements from the source [{}]", source, e);
							}
						}) //
					;
					this.log.info("Finished searching from {} source{}...", submittedSources.size(),
						submittedSources.size() > 1 ? "s" : "");
					this.log.info(
						"Submitted a total of {} work items for extraction from ({} failed), waiting for generation to conclude...",
						submittedCounter.get(), submitFailedCounter.get());
				} finally {
					extractors.waitForCompletion();
					this.log.info("Object retrieval is complete, waiting for XML generation to finish...");
					if (persistenceThread != null) {
						running.set(false);
						persistenceThread.interrupt();
					}
					persistenceThread.join();
					this.log.info("Generated a total of {} content elements ({} failed) from the {} submitted",
						renderedCounter.get(), renderFailedCounter.get(), submittedCounter.get());
				}
			}
			ret = 0;
		} finally {
			pool.close();
		}
		return ret;
	}
}