/*******************************************************************************
 * #%L
 * Armedia Caliente
 * %%
 * Copyright (c) 2010 - 2019 Armedia LLC
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
package com.armedia.caliente.cli.usermapper;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.Callable;

import com.armedia.caliente.tools.dfc.DfcQuery;
import com.armedia.caliente.tools.dfc.DfcUtils;
import com.armedia.caliente.tools.dfc.pool.DfcSessionPool;
import com.armedia.commons.utilities.Tools;
import com.documentum.fc.client.IDfCollection;
import com.documentum.fc.client.IDfGroup;
import com.documentum.fc.client.IDfLocalTransaction;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.client.IDfTypedObject;
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
			DfcUtils.closeQuietly(users);
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
			DfcUtils.closeQuietly(users);
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
						try (DfcQuery query = new DfcQuery(session,
							"select group_name, group_source, group_global_unique_id from dm_group order by 2, 1",
							DfcQuery.Type.DF_READ_QUERY)) {
							Map<String, DctmGroup> allGroups = new LinkedHashMap<>();
							int i = 0;
							while (query.hasNext()) {
								IDfTypedObject c = query.next();
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