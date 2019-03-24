package com.armedia.caliente.cli.usermapper;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.Callable;

import com.armedia.commons.dfc.pool.DfcSessionPool;
import com.armedia.commons.dfc.util.DfUtils;
import com.armedia.commons.utilities.Tools;
import com.documentum.fc.client.IDfCollection;
import com.documentum.fc.client.IDfGroup;
import com.documentum.fc.client.IDfLocalTransaction;
import com.documentum.fc.client.IDfQuery;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.common.DfException;

public class DctmGroup extends DctmPrincipal {
	private static final long serialVersionUID = 1L;

	/**
	 * @param name
	 * @param source
	 * @param guid
	 */
	public DctmGroup(String name, String source, String guid) {
		super(name, source, guid);
	}

	public Set<String> getUsers(IDfSession session) throws DfException {
		IDfGroup group = session.getGroup(getName());
		if (group == null) { throw new DfException(String.format("Failed to locate the group named [%s]", getName())); }

		Set<String> u = new TreeSet<>();
		IDfCollection users = group.getUsersNames();
		try {
			while (users.next()) {
				u.add(users.getString("users_names"));
			}
		} finally {
			DfUtils.closeQuietly(users);
		}
		return Tools.freezeSet(u);
	}

	public Set<String> getGroups(IDfSession session) throws DfException {
		IDfGroup group = session.getGroup(getName());
		if (group == null) { throw new DfException(String.format("Failed to locate the group named [%s]", getName())); }

		Set<String> u = new TreeSet<>();
		IDfCollection users = group.getGroupsNames();
		try {
			while (users.next()) {
				u.add(users.getString("groups_names"));
			}
		} finally {
			DfUtils.closeQuietly(users);
		}
		return Tools.freezeSet(u);
	}

	@Override
	public String toString() {
		return String.format("DctmGroup [name=%s, source=%s, guid=%s]", getName(), getSource(), getGuid());
	}

	public static Callable<Map<String, DctmGroup>> getGroupLoader(final DfcSessionPool pool) {
		return new Callable<Map<String, DctmGroup>>() {

			@Override
			public Map<String, DctmGroup> call() throws DfException {
				final IDfSession session = pool.acquireSession();
				try {
					IDfLocalTransaction tx = null;
					if (session.isTransactionActive()) {
						tx = session.beginTransEx();
					} else {
						session.beginTrans();
					}
					try {
						IDfCollection c = DfUtils.executeQuery(session,
							"select group_name, group_source, group_global_unique_id from dm_group order by 2, 1",
							IDfQuery.DF_READ_QUERY);
						try {
							Map<String, DctmGroup> allGroups = new LinkedHashMap<>();
							int i = 0;
							while (c.next()) {
								String name = c.getString("group_name");
								String source = c.getString("group_source");
								String guid = c.getString("group_global_unique_id");

								// Stow this crap elsewhere
								allGroups.put(name, new DctmGroup(name, source, guid));
								if ((++i % 100) == 0) {
									DctmPrincipal.LOG.info("Loaded {} Documentum Groups", i);
								}
							}
							DctmPrincipal.LOG.info("Finished loading {} Documentum Groups", allGroups.size());
							return Tools.freezeMap(allGroups);
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