package com.armedia.caliente.content;

import java.io.InputStream;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

import javax.jcr.Credentials;
import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.jackrabbit.commons.JcrUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.caliente.tools.ProgressTrigger;
import com.armedia.caliente.tools.ProgressTrigger.ProgressReport;

public class JackrabbitTest implements Callable<Void> {

	private InputStream generateInputStream(long count) {
		// TODO: Generate random data? Maybe just grab a random chunk
		// from a larger one?
		return null;
	}

	@Override
	public Void call() throws Exception {
		final Repository repository = JcrUtils.getRepository();
		// TODO: initialize a local filesystem repository
		final Credentials credentials = new SimpleCredentials("admin", "admin".toCharArray());
		final ExecutorService executor = Executors.newWorkStealingPool(10);
		final Organizer<?> organizer = new SequentialOrganizer();
		final Logger console = LoggerFactory.getLogger("console");
		final Consumer<Long> startTrigger = (s) -> {
			console.info("Started generating content files");
		};
		final Consumer<ProgressReport> progressTrigger = (pr) -> {
			String id = pr.getIntervalDuration().toString();
			String irps = String.format("%.3f", pr.getIntervalRatePerSecond());
			String td = pr.getTotalDuration().toString();
			String trps = String.format("%.3f", pr.getTotalRatePerSecond());
			console.info("\n\tProgress report: {}/{} (~{}/s) | {}/{} (~{}/s)\n", pr.getIntervalCount(), id, irps,
				pr.getTotalCount(), td, trps);
		};
		final AtomicLong counter = new AtomicLong(0);
		final ProgressTrigger progress = new ProgressTrigger(startTrigger, progressTrigger);
		try {
			Callable<Void> worker = () -> {
				final ContentStoreClient client = new ContentStoreClient("JackrabbitTest",
					Thread.currentThread().getName());
				final Session session = repository.login(credentials);
				try {
					Pair<Node, String> target = organizer.newContentLocation(session, client);
					Node folder = target.getLeft();
					try (InputStream data = generateInputStream(counter.getAndIncrement())) {
						Node file = JcrUtils.putFile(folder, target.getRight(), "application/octet-stream", data);
						console.info("Generated: {}", file.getPath());
						progress.trigger();
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
		return null;
	}

}