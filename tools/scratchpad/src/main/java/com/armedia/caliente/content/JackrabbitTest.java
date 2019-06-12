package com.armedia.caliente.content;

import java.util.concurrent.Callable;

import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

import org.apache.jackrabbit.commons.JcrUtils;

public class JackrabbitTest implements Callable<Void> {

	@Override
	public Void call() throws Exception {
		Repository repository = JcrUtils.getRepository();
		Session session = repository.login(new SimpleCredentials("admin", "admin".toCharArray()));
		try {

			Node root = session.getRootNode();

			Node base = root.addNode("base");
			Node child = base.addNode("child");
			Node grandchild = child.addNode("grandchild");
			session.save();

		} finally {
			session.logout();
		}
		return null;
	}

}