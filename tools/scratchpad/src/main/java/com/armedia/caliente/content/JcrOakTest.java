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
package com.armedia.caliente.content;

import java.io.InputStream;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

import javax.jcr.Credentials;
import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.jackrabbit.commons.JcrUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.caliente.tools.ProgressTrigger;
import com.armedia.caliente.tools.ProgressTrigger.ProgressReport;
import com.armedia.commons.utilities.FunctionalPooledWorkersLogic;
import com.armedia.commons.utilities.PooledWorkers;
import com.armedia.commons.utilities.PooledWorkersLogic;
import com.armedia.commons.utilities.concurrent.BaseShareableLockable;
import com.armedia.commons.utilities.function.CheckedSupplier;

public class JcrOakTest extends BaseShareableLockable implements Callable<Void> {

	private static final Consumer<Repository> NULL_CLOSER = (r) -> {
	};

	private static final int MAX_STREAMS = 100;

	private final AtomicLong counter = new AtomicLong(0);
	private final BlockingQueue<Pair<String, Long>> elements = new LinkedBlockingQueue<>();
	final Credentials credentials = new SimpleCredentials("root", "system01".toCharArray());
	final Organizer<?> organizer = new SequentialOrganizer();
	final Logger console = LoggerFactory.getLogger("console");
	private final TestDataGenerator testData;

	private final int threads;
	private final int testCount;
	private final Duration reportInterval = Duration.ofSeconds(1);
	private final CheckedSupplier<Repository, Exception> repositorySupplier;
	private final Consumer<Repository> repositoryCloser;

	public JcrOakTest(int threadCount, int testCount, CheckedSupplier<Repository, Exception> repositorySupplier) {
		this(threadCount, testCount, repositorySupplier, null);
	}

	public JcrOakTest(int threadCount, int testCount, CheckedSupplier<Repository, Exception> repositorySupplier,
		Consumer<Repository> repositoryCloser) {
		this.repositorySupplier = Objects.requireNonNull(repositorySupplier,
			"Must provide a way to initialize the repository");
		this.threads = threadCount;
		this.testCount = Math.max(1000, testCount);
		this.testData = new TestDataGenerator(JcrOakTest.MAX_STREAMS, (i) -> {
			Long factor = FileUtils.ONE_MB;
			return (1 << (i % 6)) * factor.intValue();
		});
		if (repositoryCloser == null) {
			repositoryCloser = JcrOakTest.NULL_CLOSER;
		}
		this.repositoryCloser = repositoryCloser;
	}

	private void writeFiles(Repository repository) throws Exception {
		final Consumer<Long> writeStartTrigger = (s) -> {
			this.console.info("Started generating {} content files", this.testCount);
		};
		final Consumer<ProgressReport> writeTrigger = (pr) -> this.console.info("\n\tWrite progress report: {} | {}\n",
			pr.getIntervalStatistics(), pr.getAggregateStatistics());
		final ProgressTrigger writeProgress = new ProgressTrigger(writeStartTrigger, writeTrigger, this.reportInterval);
		final ContentStoreClient client = new ContentStoreClient("writeTest", "sharedClient");
		final PooledWorkersLogic<Pair<Session, ContentStoreClient>, Integer, Exception> logic = new FunctionalPooledWorkersLogic<>(
			() -> Pair.of(repository.login(this.credentials), client), //
			(p, i) -> {
				final Session session = p.getLeft();
				Pair<Node, String> target = this.organizer.newContentLocation(session, p.getRight());
				Node folder = target.getLeft();
				final long counterPos = this.counter.getAndIncrement();
				final Node file;
				try (InputStream data = this.testData.getInputStream(counterPos)) {
					file = JcrUtils.putFile(folder, target.getRight(), "application/octet-stream", data);
					String path = file.getPath();
					this.console.debug("Generated element # {} @ [{}]", counterPos, path);
					writeProgress.trigger();
					this.elements.offer(Pair.of(path, counterPos));
				}
				session.save();
			}, //
			(p, i, e) -> this.console.error("******** EXCEPTION CAUGHT PROCESSING ITEM # {} ********", i, e), //
			(p) -> p.getLeft().logout());
		final PooledWorkers<Pair<Session, ContentStoreClient>, Integer> writers = //
			new PooledWorkers.Builder<Pair<Session, ContentStoreClient>, Integer, Exception>() //
				.logic(logic) //
				.threads(this.threads) //
				.start() //
		;
		try {
			for (int i = 0; i < this.testCount; i++) {
				writers.addWorkItemNonblock(i);
			}
		} finally {
			List<Integer> remaining = writers.waitForCompletion();
			writeProgress.trigger(true);
			for (Integer i : remaining) {
				this.console.warn("****** UNPROCESSED ITEM # {}", i);
			}
		}
	}

	private void readFiles(final Repository repository) {
		final Consumer<Long> readStartTrigger = (s) -> {
			this.console.info("Started reading back content files");
		};
		final Consumer<ProgressReport> readTrigger = (pr) -> this.console.info("\n\tRead progress report: {} | {}\n",
			pr.getIntervalStatistics(), pr.getAggregateStatistics());
		final ProgressTrigger readProgress = new ProgressTrigger(readStartTrigger, readTrigger, this.reportInterval);
		final PooledWorkersLogic<Session, Pair<String, Long>, Exception> logic = new FunctionalPooledWorkersLogic<>(
			() -> repository.login(this.credentials), //
			(session, target) -> {
				// Handle each target
				try {
					final String path = target.getLeft();
					final Long counterPos = target.getRight();
					final Node node = JcrUtils.getNodeIfExists(path, session);
					if (node == null) {
						this.console.error("*** FAILED TO FIND NODE [{}] (#{}) ***", path, counterPos);
						return;
					}
					try (InputStream in = JcrUtils.readFile(node)) {
						// Validate that the data is the correct data...
						String readHash = DigestUtils.sha256Hex(in);
						String expectedHash = this.testData.getHash(counterPos);
						if (!Objects.equals(readHash, expectedHash)) {
							this.console.debug("Read element # {} @ [{}]", counterPos, path);
						} else {
							this.console.error("*** WRONG DATA RETURNED FROM [{}] (#{}) - expected {} but got {} ***",
								path, counterPos, expectedHash, readHash);
						}
					}
				} finally {
					readProgress.trigger();
				}
			},
			(session, target, e) -> this.console.error("******** EXCEPTION CAUGHT PROCESSING [{}] (#{})",
				target.getLeft(), target.getRight(), e),
			(session) -> session.logout());

		final PooledWorkers<Session, Pair<String, Long>> readers = //
			new PooledWorkers.Builder<Session, Pair<String, Long>, Exception>() //
				.logic(logic) //
				.threads(this.threads) //
				.start() //
		;

		try {
			this.elements.forEach(readers::addWorkItemNonblock);
		} finally {
			List<Pair<String, Long>> remaining = readers.waitForCompletion();
			readProgress.trigger(true);
			for (Pair<String, Long> r : remaining) {
				this.console.warn("****** UNPROCESSED ITEM: {}", r);
			}
		}
	}

	@Override
	public Void call() throws Exception {
		final Repository repository = this.repositorySupplier.get();
		this.console.info("Initializing the test data...");
		this.testData.reset();
		this.console.info("Test data ready!");

		try {
			writeFiles(repository);
			readFiles(repository);
		} finally {
			this.repositoryCloser.accept(repository);
		}
		return null;
	}

}