package com.armedia.cmf.usermapper;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Callable;

import com.armedia.cmf.usermapper.tools.DfUtils;
import com.armedia.commons.dfc.pool.DfcSessionPool;
import com.armedia.commons.utilities.Tools;
import com.documentum.fc.client.IDfCollection;
import com.documentum.fc.client.IDfLocalTransaction;
import com.documentum.fc.client.IDfQuery;
import com.documentum.fc.client.IDfSession;

public class DctmUser extends DctmPrincipal {
	private static final long serialVersionUID = 1L;

	private final String login;

	/**
	 * @param name
	 * @param login
	 * @param source
	 * @param guid
	 */
	public DctmUser(String name, String login, String source, String guid) {
		super(name, source, guid);
		this.login = login;
	}

	public String getLogin() {
		return this.login;
	}

	@Override
	public String toString() {
		return String.format("DctmUser [name=%s, login=%s source=%s, guid=%s]", getName(), this.login, getSource(),
			getGuid());
	}

	public static Callable<Map<String, DctmUser>> getUserLoader(final DfcSessionPool pool) {
		return new Callable<Map<String, DctmUser>>() {

			@Override
			public Map<String, DctmUser> call() throws Exception {
				final IDfSession session = pool.acquireSession();
				try {
					IDfLocalTransaction tx = null;
					if (session.isTransactionActive()) {
						tx = session.beginTransEx();
					} else {
						session.beginTrans();
					}
					try {
						// Only pull users that aren't groups
						final IDfCollection c = DfUtils.executeQuery(session,
							"select user_name, user_login_name, user_source, user_global_unique_id from dm_user where r_is_group = 0 order by 1",
							IDfQuery.DF_READ_QUERY);
						int i = 0;
						try {
							Map<String, DctmUser> users = new LinkedHashMap<String, DctmUser>();
							while (c.next()) {
								String name = c.getString("user_name");
								String login = c.getString("user_login_name");
								String source = c.getString("user_source");
								String guid = c.getString("user_global_unique_id");

								// Stow this crap elsewhere
								users.put(name, new DctmUser(name, login, source, guid));
								if ((++i % 100) == 0) {
									DctmPrincipal.LOG.info("Loaded {} Documentum Users", i);
								}
							}
							DctmPrincipal.LOG.info("Finished loading {} Documentum Users", users.size());
							return Tools.freezeMap(users);
						} finally {
							DfUtils.closeQuietly(c);
						}
					} finally {
						if (tx != null) {
							session.abortTransEx(tx);
						} else {
							session.abortTrans();
						}
					}
				} finally {
					pool.releaseSession(session);
				}
			}

		};
	}
}