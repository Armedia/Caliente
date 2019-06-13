package com.armedia.caliente.content;

import java.io.InputStream;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

import javax.jcr.Binary;
import javax.jcr.Credentials;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.jackrabbit.commons.JcrUtils;
import org.apache.jackrabbit.oak.Oak;
import org.apache.jackrabbit.oak.jcr.Jcr;
import org.apache.jackrabbit.oak.plugins.document.mongo.MongoDocumentNodeStoreBuilder;
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
import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;

public class JcrOakTest extends BaseShareableLockable implements Callable<Void> {

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

	public JcrOakTest(int threadCount, int testCount) {
		this.threads = threadCount;
		this.testCount = Math.max(1000, testCount);
		this.testData = new TestDataGenerator(JcrOakTest.MAX_STREAMS, (i) -> {
			Long factor = FileUtils.ONE_MB;
			return (1 << (i % 6)) * factor.intValue();
		});
	}

	private Repository buildRepository() throws RepositoryException {
		final String host = ServerAddress.defaultHost();
		final int port = ServerAddress.defaultPort();
		final String db = "oak";
		final ServerAddress addx = new ServerAddress(host, port);
		this.console.info("Initializing the repository connection to {}@{}", db, addx);
		MongoCredential credentials = MongoCredential.createCredential("oak", db, "oak".toCharArray()) //
		//
		;
		MongoClientOptions.Builder options = new MongoClientOptions.Builder() //
		//
		;
		this.console.info("Readying the Mongo client");
		MongoClient client = new MongoClient(addx, credentials, options.build()) //
		//
		;

		this.console.info("Mongo client ready!! Creating the NodeStore...");
		MongoDocumentNodeStoreBuilder builder = new MongoDocumentNodeStoreBuilder() //
			.setMongoDB(client, db) //
		//
		;

		// Configure oak
		this.console.info("NodeStore ready!! Creating the Oak instance...");
		Oak oak = new Oak(builder.build()) //
			.with(new PropertyIndexProvider()) //
		//
		;

		// Configure jcr
		this.console.info("Oak ready!! Creating the Jcr instance...");
		Jcr jcr = new Jcr(oak) //
			.with(new OpenSecurityProvider()) //
		//
		;

		// Return the repository
		this.console.info("Jcr ready!! Creating the Repository...");
		Repository repository = jcr.createRepository();
		this.console.info("Repository ready!");
		return repository;
	}

	private void analyzeProperties(Node node) {
		try {
			Node content = node.getNode("jcr:content");
			Property p = content.getProperty("jcr:data");
			Binary b = p.getBinary();
			int size = (int) b.getSize();
			byte[] buf = new byte[size / 2];
			Integer read = b.read(buf, size / 4);
			read.hashCode();
		} catch (Exception e) {
			// Ignore!
			e.hashCode();
		}
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
				/*
				try {
					analyzeProperties(file);
				} catch (Exception e) {
					// ignore!!
				}
				*/
			}, //
			(p, i, e) -> this.console.error("******** EXCEPTION CAUGHT PROCESSING ITEM # {} ********", i, e), //
			(p) -> p.getLeft().logout());
		final PooledWorkers<Pair<Session, ContentStoreClient>, Integer> writers = new PooledWorkers<>();
		try {
			writers.start(logic, this.threads);
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
					analyzeProperties(node);
					try (InputStream in = JcrUtils.readFile(node)) {
						// Validate that the data is the correct data...
						String readHash = DigestUtils.sha256Hex(in);
						String expectedHash = this.testData.getHash(counterPos);
						if (!Tools.equals(readHash, expectedHash)) {
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
		final PooledWorkers<Session, Pair<String, Long>> readers = new PooledWorkers<>();

		try {
			readers.start(logic, this.threads);
			this.elements.forEach(readers::addWorkItemNonblock);
		} finally {
			List<Pair<String, Long>> remaining = readers.waitForCompletion();
			readProgress.trigger(true);
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
		this.console.info("Initializing the test data...");
		this.testData.reset();
		this.console.info("Test data ready!");

		try {
			writeFiles(repository);
			readFiles(repository);
		} finally {
			closeRepository(repository);
		}
		return null;
	}

}