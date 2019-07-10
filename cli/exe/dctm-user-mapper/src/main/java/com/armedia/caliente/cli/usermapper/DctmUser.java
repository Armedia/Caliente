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

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Callable;

import com.armedia.caliente.tools.dfc.DfcQuery;
import com.armedia.caliente.tools.dfc.pool.DfcSessionPool;
import com.armedia.commons.utilities.Tools;
import com.documentum.fc.client.IDfLocalTransaction;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.client.IDfTypedObject;
import com.documentum.fc.common.DfException;
import com.documentum.fc.common.IDfAttr;

public class DctmUser extends DctmPrincipal {
	private static final long serialVersionUID = 1L;

	private final String login;
	private final String osName;
	private final Map<String, String> attributes;

	protected DctmUser(String name, String source, String guid, IDfTypedObject obj) throws DfException {
		super(name, source, guid);
		this.login = obj.getString("user_login_name");
		this.osName = obj.getString("user_os_name");
		final int attCount = obj.getAttrCount();
		Map<String, String> attributes = new HashMap<>(attCount);
		for (int i = 0; i < attCount; i++) {
			final IDfAttr att = obj.getAttr(i);
			final String attName = att.getName();
			if (att.isRepeating()) {
				continue;
			}
			switch (att.getDataType()) {
				case IDfAttr.DM_STRING:
				case IDfAttr.DM_ID:
					break;
				default: // Only string or string-ish attributes are allowed
					continue;
			}
			attributes.put(attName.toLowerCase(), obj.getString(attName));
		}
		this.attributes = Tools.freezeMap(attributes);
	}

	public final String getLogin() {
		return this.login;
	}

	public final String getOsName() {
		return this.osName;
	}

	public final String getAttribute(String name) {
		if (name == null) { throw new IllegalArgumentException("Must provide a non-null attribute name"); }
		return this.attributes.get(name.toLowerCase());
	}

	@Override
	public String toString() {
		return String.format("DctmUser [name=%s, login=%s, osName=%s, source=%s, guid=%s, attributes=%s]", getName(),
			this.login, this.osName, getSource(), getGuid(), this.attributes);
	}

	public static Callable<Map<String, DctmUser>> getUserLoader(final DfcSessionPool pool) {
		return new Callable<Map<String, DctmUser>>() {

			@Override
			public Map<String, DctmUser> call() throws DfException {
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
						int i = 0;
						try (DfcQuery query = new DfcQuery(session,
							"select * from dm_user where r_is_group = 0 order by 1", DfcQuery.Type.DF_READ_QUERY)) {
							Map<String, DctmUser> users = new LinkedHashMap<>();
							while (query.hasNext()) {
								IDfTypedObject c = query.next();
								String name = c.getString("user_name");
								String source = c.getString("user_source");
								String guid = c.getString("user_global_unique_id");

								// Stow this crap elsewhere
								users.put(name, new DctmUser(name, source, guid, c));
								if ((++i % 100) == 0) {
									DctmPrincipal.LOG.info("Loaded {} Documentum Users", i);
								}
							}
							DctmPrincipal.LOG.info("Finished loading {} Documentum Users", users.size());
							return Tools.freezeMap(users);
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