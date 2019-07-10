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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.bind.DatatypeConverter;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.commons.utilities.Tools;
import com.unboundid.ldap.sdk.DN;
import com.unboundid.ldap.sdk.Filter;
import com.unboundid.ldap.sdk.LDAPConnection;
import com.unboundid.ldap.sdk.LDAPConnectionPool;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.SearchRequest;
import com.unboundid.ldap.sdk.SearchResultEntry;
import com.unboundid.ldap.sdk.SearchResultListener;
import com.unboundid.ldap.sdk.SearchResultReference;
import com.unboundid.ldap.sdk.SearchScope;

public abstract class LdapPrincipalDb<P extends LdapPrincipal> {

	protected static final String[] NO_ATTRIBUTES = new String[0];

	private static final Pattern PREFIXED_GUID = Pattern.compile("^(?:([^:]+):)?(?i:([0-9A-F]+))$");

	private static final String GUID_ATTRIBUTE = "objectGUID";

	private final Class<P> pClass;
	private final Map<String, P> byName;
	private final Map<String, P> byGuid;
	private final Map<DN, P> byDn;
	private final LDAPConnectionPool pool;

	private final String baseDn;
	private final Filter baseFilter;
	private final List<String> searchAttributes;
	private final String nameAttribute;

	protected final Logger log = LoggerFactory.getLogger(getClass());

	protected LdapPrincipalDb(Class<P> pClass) {
		this.pClass = pClass;
		this.byName = Collections.emptyMap();
		this.byGuid = Collections.emptyMap();
		this.byDn = Collections.emptyMap();
		this.pool = null;
		this.baseDn = null;
		this.baseFilter = null;
		this.searchAttributes = null;
		this.nameAttribute = null;
	}

	protected LdapPrincipalDb(Class<P> pClass, LDAPConnectionPool pool, boolean onDemand, String baseDn,
		String baseFilter, String... searchAttributes) throws LDAPException {
		if ((searchAttributes == null) || (searchAttributes.length < 1) || StringUtils.isEmpty(searchAttributes[0])) {
			throw new IllegalArgumentException("Must provide at least one attribute to search against");
		}
		this.pClass = pClass;
		this.baseFilter = Filter.create(baseFilter);
		this.baseDn = baseDn;
		this.nameAttribute = searchAttributes[0];
		// if batched, populate the DB's in one go.
		// If not batched, the DBs will only be populated as we go
		List<String> l = new ArrayList<>();
		for (String s : searchAttributes) {
			if (s != null) {
				l.add(s);
			}
		}
		this.searchAttributes = Tools.freezeList(l);

		if (!onDemand) {
			// Populate up front
			final Map<String, P> byName = new HashMap<>();
			final Map<String, P> byGuid = new HashMap<>();
			final Map<DN, P> byDn = new HashMap<>();
			final AtomicInteger counter = new AtomicInteger(0);
			final String label = pClass.getSimpleName();
			SearchRequest request = new SearchRequest(new SearchResultListener() {
				private static final long serialVersionUID = 1L;

				@Override
				public void searchEntryReturned(SearchResultEntry e) {
					P p = buildObject(e);
					byName.put(p.getName().toLowerCase(), p);
					byGuid.put(p.getGuid().toLowerCase(), p);
					byDn.put(p.getDn(), p);
					int i = counter.incrementAndGet();
					if ((i % 100) == 0) {
						LdapPrincipalDb.this.log.info("Loaded {} {} objects", i, label);
					}
				}

				@Override
				public void searchReferenceReturned(SearchResultReference r) {
				}
			}, this.baseDn, SearchScope.SUB, this.baseFilter);
			request.setAttributes(this.searchAttributes);

			LDAPConnection c = pool.getConnection();
			try {
				c.search(request);
				this.log.info("Finished loading {} {} objects", byDn.size(), label);
			} finally {
				pool.releaseConnection(c);
			}

			this.byName = Tools.freezeMap(byName);
			this.byGuid = Tools.freezeMap(byGuid);
			this.byDn = Tools.freezeMap(byDn);
			this.pool = null;
		} else {
			// Populate on demand...
			this.byName = new ConcurrentHashMap<>();
			this.byGuid = new ConcurrentHashMap<>();
			this.byDn = new ConcurrentHashMap<>();
			this.pool = pool;
		}
	}

	public final boolean isOnDemand() {
		return (this.pool != null);
	}

	protected abstract P buildObject(SearchResultEntry entry);

	protected final void listAll(LDAPConnection c, SearchResultListener listener) throws LDAPException {
	}

	private P searchRealTime(Object key, Map<?, P> source) throws LDAPException {
		if (this.pool == null) { return null; }
		LDAPConnection c = this.pool.getConnection();
		try {
			SearchRequest request = new SearchRequest(this.baseDn, SearchScope.SUB, this.baseFilter);
			final Filter filter;
			if (source == this.byName) {
				filter = Filter.createANDFilter(this.baseFilter,
					Filter.createEqualityFilter(this.nameAttribute, key.toString()));
			} else if (source == this.byGuid) {
				filter = Filter.createANDFilter(this.baseFilter, Filter.createEqualityFilter(
					LdapPrincipalDb.GUID_ATTRIBUTE, DatatypeConverter.parseHexBinary(key.toString())));
			} else if (source == this.byDn) {
				if (DN.class.isInstance(key)) {
					filter = Filter.createEqualityFilter("dn", key.toString());
				} else {
					throw new IllegalArgumentException(String.format("Must provide a key of type DN instead of %s",
						key.getClass().getCanonicalName()));
				}
			} else {
				throw new IllegalArgumentException("Unknown source provided");
			}
			request.setFilter(filter);
			request.setAttributes(this.searchAttributes);
			// Return only the first match
			request.setSizeLimit(1);

			for (SearchResultEntry e : c.search(request).getSearchEntries()) {
				P p = buildObject(e);
				this.log.info("Loaded LDAP {} object [{}]", this.pClass.getSimpleName(), p.getName());
				return p;
			}
			return null;
		} finally {
			this.pool.releaseConnection(c);
		}
	}

	private P get(Object key, Map<?, P> source) throws LDAPException {
		P p = source.get(key);
		if (p == null) {
			p = searchRealTime(key, source);
			if (p != null) {
				this.byName.put(p.getName().toLowerCase(), p);
				this.byGuid.put(p.getGuid().toLowerCase(), p);
				this.byDn.put(p.getDn(), p);
			}
		}
		return p;
	}

	public final P getByName(String name) throws LDAPException {
		return get(name.toLowerCase(), this.byName);
	}

	public final P getByGuid(String guid) throws LDAPException {
		// Parse out the GUID in the form <prefix>:<hex>. If it doesn't match,
		// then use the guid as-is
		if (guid == null) { throw new IllegalArgumentException("Must provide a valid GUID"); }
		Matcher m = LdapPrincipalDb.PREFIXED_GUID.matcher(guid);
		// If this isn't a valid hex-encoded GUID format, we simply skip it...
		if (!m.matches()) { return null; }
		return get(m.group(2).toLowerCase(), this.byGuid);
	}

	public final P getByDn(DN dn) throws LDAPException {
		return get(dn, this.byDn);
	}
}