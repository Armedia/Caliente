/*******************************************************************************
 * #%L
 * Armedia Caliente
 * %%
 * Copyright (C) 2013 - 2019 Armedia, LLC
 * %%
 * This file is part of the Caliente software.
 *
 * If the software was purchased under a paid Caliente license, the terms of
 * the paid license agreement will prevail.  Otherwise, the software is
 * provided under the following open source license terms:
 *
 * Caliente is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Caliente is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Caliente. If not, see <http://www.gnu.org/licenses/>.
 * #L%
 *******************************************************************************/
package com.armedia.caliente.cli.ticketdecoder;

import java.io.File;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.caliente.cli.OptionValues;
import com.armedia.caliente.cli.ticketdecoder.xml.Content;
import com.armedia.caliente.cli.utils.DfcLaunchHelper;
import com.armedia.caliente.cli.utils.ThreadsLaunchHelper;
import com.armedia.caliente.tools.dfc.DfcCrypto;
import com.armedia.caliente.tools.dfc.pool.DfcSessionPool;
import com.armedia.commons.utilities.CloseableIterator;
import com.armedia.commons.utilities.PooledWorkers;
import com.armedia.commons.utilities.Tools;
import com.armedia.commons.utilities.concurrent.ShareableCollection;
import com.armedia.commons.utilities.concurrent.ShareableSet;
import com.armedia.commons.utilities.line.LineScanner;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.common.DfException;
import com.documentum.fc.common.IDfId;

public class DctmTicketDecoder {

	private final Logger console = LoggerFactory.getLogger("console");
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
			pool = new DfcSessionPool(docbase, user, new DfcCrypto().decrypt(password));
		} catch (DfException e) {
			this.log.error("Failed to create the DFC session pool", e);
			return 1;
		}

		final BlockingQueue<Content> contents = new LinkedBlockingQueue<>();
		final AtomicBoolean running = new AtomicBoolean(true);
		Thread persistenceThread = null;

		final Set<String> scannedIds = new ShareableSet<>(new HashSet<>());

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

			final AtomicLong submittedCounter = new AtomicLong(0);
			final AtomicLong outputCounter = new AtomicLong(0);
			final Collection<Pair<IDfId, Exception>> failedSubmissions = new ShareableCollection<>(new LinkedList<>());
			final Collection<Pair<Content, Exception>> failedOutput = new ShareableCollection<>(new LinkedList<>());
			try (ContentPersistor persistor = format.newPersistor()) {
				persistor.initialize(target);

				final Set<String> submittedSources = new HashSet<>();
				this.console.info("Starting the background searches...");
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
							this.console.info("{}", c);
							try {
								persistor.persist(c);
								outputCounter.incrementAndGet();
							} catch (Exception e) {
								failedOutput.add(Pair.of(c, e));
								this.log.error("Failed to output the content object {}", c, e);
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
										failedSubmissions.add(Pair.of(id, e));
										this.log.error("Failed to add ID [{}] to the work queue", id, e);
									}
								}).call();
							} catch (Exception e) {
								this.log.error("Failed to search for elements from the source [{}]", source, e);
							}
						}) //
					;
					this.console.info("Finished searching from {} source{}...", submittedSources.size(),
						submittedSources.size() > 1 ? "s" : "");
					this.console.info(
						"Submitted a total of {} work items for extraction from ({} failed), waiting for generation to conclude...",
						submittedCounter.get(), failedSubmissions.size());
				} finally {
					extractors.waitForCompletion();
					this.console.info("Object retrieval is complete, waiting for the document rendering to finish...");
					if (persistenceThread != null) {
						running.set(false);
						persistenceThread.interrupt();
					}
					persistenceThread.join();

					this.console.info("Generated a total of {} content elements ({} failed) from the {} submitted",
						outputCounter.get(), failedOutput.size(), submittedCounter.get());
					try {
						if (!failedSubmissions.isEmpty()) {
							this.log.error("SUBMISSION ERRORS:");
							failedSubmissions.forEach(
								(p) -> this.log.error("Failed to submit the ID {}", p.getLeft(), p.getRight()));
						}
					} catch (Exception e) {
						this.log.error("UNABLE TO LOG {} SUBMISSION ERRORS", failedSubmissions.size());
					}
					try {
						if (!failedOutput.isEmpty()) {
							this.log.error("OUTPUT ERRORS:");
							failedOutput.forEach(
								(p) -> this.log.error("Failed to output content {}", p.getLeft(), p.getRight()));
						}
					} catch (Exception e) {
						this.log.error("UNABLE TO LOG {} OUTPUT ERRORS", failedSubmissions.size());
					}
				}
			}
			ret = 0;
		} finally {
			pool.close();
		}
		return ret;
	}
}