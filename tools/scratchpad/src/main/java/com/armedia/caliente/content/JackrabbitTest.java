package com.armedia.caliente.content;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.jcr.Credentials;
import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

import org.apache.jackrabbit.commons.JcrUtils;

public class JackrabbitTest implements Callable<Void> {

	@Override
	public Void call() throws Exception {
		final Repository repository = JcrUtils.getRepository();
		final Credentials credentials = new SimpleCredentials("admin", "admin".toCharArray());
		final ExecutorService executor = Executors.newWorkStealingPool(10);
		final Organizer<?> organizer = null;
		try {

			Session session = repository.login(credentials);
			Node root = session.getRootNode();

			Node base = root.addNode("base");
			Node child = base.addNode("child");
			Node grandchild = child.addNode("grandchild");

			session.save();

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