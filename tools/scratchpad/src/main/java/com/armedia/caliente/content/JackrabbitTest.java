package com.armedia.caliente.content;

import java.io.InputStream;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

import javax.jcr.Credentials;
import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.jackrabbit.commons.JcrUtils;
import org.apache.jackrabbit.oak.InitialContent;
import org.apache.jackrabbit.oak.Oak;
import org.apache.jackrabbit.oak.jcr.Jcr;
import org.apache.jackrabbit.oak.plugins.index.property.PropertyIndexProvider;
import org.apache.jackrabbit.oak.spi.security.OpenSecurityProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.caliente.tools.ProgressTrigger;
import com.armedia.caliente.tools.ProgressTrigger.ProgressReport;
import com.armedia.commons.utilities.FunctionalPooledWorkersLogic;
import com.armedia.commons.utilities.PooledWorkers;
import com.armedia.commons.utilities.PooledWorkersLogic;
import com.armedia.commons.utilities.Tools;
import com.armedia.commons.utilities.concurrent.BaseShareableLockable;

public class JackrabbitTest extends BaseShareableLockable implements Callable<Void> {

	private static final int MAX_STREAMS = 100;

	private final AtomicLong counter = new AtomicLong(0);
	private final BlockingQueue<Pair<String, Long>> elements = new LinkedBlockingQueue<>();
	final Credentials credentials = new SimpleCredentials("admin", "admin".toCharArray());
	final Organizer<?> organizer = new SequentialOrganizer();
	final Logger console = LoggerFactory.getLogger("console");
	private final TestDataGenerator testData = new TestDataGenerator(JackrabbitTest.MAX_STREAMS);

	private final int threads;

	public JackrabbitTest(int threadCount) {
		this.threads = threadCount;
	}

	private Repository buildRepository() throws RepositoryException {
		Oak oak = new Oak() //
			.with(new OpenSecurityProvider()) //
			.with(new InitialContent()) //
			.with(new PropertyIndexProvider()) //
		;
		/// Configure oak
		Jcr jcr = new Jcr(oak) //
		//
		;
		// Configure Jcr
		return jcr.createRepository();
	}

	private void writeFiles(Repository repository) throws Exception {
		final Consumer<Long> writeStartTrigger = (s) -> {
			this.console.info("Started generating content files");
		};
		final Consumer<ProgressReport> writeTrigger = (pr) -> {
			String id = pr.getIntervalDuration().toString();
			String irps = String.format("%.3f", pr.getIntervalRatePerSecond());
			String td = pr.getTotalDuration().toString();
			String trps = String.format("%.3f", pr.getTotalRatePerSecond());
			this.console.info("\n\tProgress report: {}/{} (~{}/s) | {}/{} (~{}/s)\n", pr.getIntervalCount(), id, irps,
				pr.getTotalCount(), td, trps);
		};
		final ProgressTrigger writeProgress = new ProgressTrigger(writeStartTrigger, writeTrigger);
		final ExecutorService executor = Executors.newWorkStealingPool(this.threads);
		try {
			Callable<Void> worker = () -> {
				final ContentStoreClient client = new ContentStoreClient("writeTest", Thread.currentThread().getName());
				final Session session = repository.login(this.credentials);
				try {
					Pair<Node, String> target = this.organizer.newContentLocation(session, client);
					Node folder = target.getLeft();
					final long counterPos = this.counter.getAndIncrement();
					try (InputStream data = this.testData.getInputStream(counterPos)) {
						Node file = JcrUtils.putFile(folder, target.getRight(), "application/octet-stream", data);
						String path = file.getPath();
						this.console.info("Generated element # {} @ [{}]", counterPos, path);
						writeProgress.trigger();
						this.elements.offer(Pair.of(path, counterPos));
					}
					session.save();
					return null;
				} finally {
					session.logout();
				}
			};

			for (int i = 0; i < 100000; i++) {
				executor.submit(worker);
			}
		} finally {
			executor.shutdown();
			boolean abort = false;
			try {
				if (!executor.awaitTermination(15, TimeUnit.SECONDS)) {
					abort = true;
				}
			} finally {
				if (abort) {
					executor.shutdownNow();
				}
			}
		}
	}

	private void readFiles(final Repository repository) {
		final Consumer<Long> readStartTrigger = (s) -> {
			this.console.info("Started reading back content files");
		};
		final Consumer<ProgressReport> readTrigger = (pr) -> {
			String id = pr.getIntervalDuration().toString();
			String irps = String.format("%.3f", pr.getIntervalRatePerSecond());
			String td = pr.getTotalDuration().toString();
			String trps = String.format("%.3f", pr.getTotalRatePerSecond());
			this.console.info("\n\tRead progress report: {}/{} (~{}/s) | {}/{} (~{}/s)\n", pr.getIntervalCount(), id,
				irps, pr.getTotalCount(), td, trps);
		};
		final ProgressTrigger readProgress = new ProgressTrigger(readStartTrigger, readTrigger);
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
						if (!Tools.equals(readHash, expectedHash)) {
							this.console.info("Read element # {} @ [{}]", counterPos, path);
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
		final PooledWorkers<Session, Pair<String, Long>> readers = new PooledWorkers<>();

		try {
			readers.start(logic, this.threads);
			this.elements.forEach(readers::addWorkItemNonblock);
		} finally {
			List<Pair<String, Long>> remaining = readers.waitForCompletion();
			for (Pair<String, Long> r : remaining) {
				this.console.warn("****** UNPROCESSED ITEM: {}", r);
			}
		}
	}

	private void closeRepository(Repository repository) {
		//
	}

	@Override
	public Void call() throws Exception {
		final Repository repository = buildRepository();
		this.testData.reset();

		try {
			writeFiles(repository);
			readFiles(repository);
		} finally {
			closeRepository(repository);
		}
		return null;
	}

}