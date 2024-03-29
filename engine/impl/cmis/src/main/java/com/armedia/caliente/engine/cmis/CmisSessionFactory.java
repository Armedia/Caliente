/*******************************************************************************
 * #%L
 * Armedia Caliente
 * %%
 * Copyright (C) 2013 - 2019 Armedia, LLC
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
package com.armedia.caliente.engine.cmis;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

import org.apache.chemistry.opencmis.client.api.OperationContext;
import org.apache.chemistry.opencmis.client.api.Repository;
import org.apache.chemistry.opencmis.client.api.Session;
import org.apache.chemistry.opencmis.client.runtime.SessionFactoryImpl;
import org.apache.chemistry.opencmis.commons.SessionParameter;
import org.apache.chemistry.opencmis.commons.enums.BindingType;
import org.apache.chemistry.opencmis.commons.enums.IncludeRelationships;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;

import com.armedia.caliente.engine.common.SessionFactory;
import com.armedia.caliente.engine.common.SessionFactoryException;
import com.armedia.caliente.tools.CmfCrypt;
import com.armedia.caliente.tools.CryptException;
import com.armedia.commons.utilities.CfgTools;
import com.armedia.commons.utilities.EncodedString;
import com.armedia.commons.utilities.Tools;

public class CmisSessionFactory extends SessionFactory<Session> {

	private static final int MIN_PAGE_SIZE = 10;
	private static final int MAX_PAGE_SIZE = Integer.MAX_VALUE;

	private final org.apache.chemistry.opencmis.client.api.SessionFactory factory = SessionFactoryImpl.newInstance();
	private final Map<String, String> parameters;
	private final int defaultPageSize;

	public CmisSessionFactory(CfgTools settings, CmfCrypt crypto) throws Exception, CryptException {
		super(settings, crypto);
		Map<String, String> parameters = new HashMap<>();

		final String url = settings.getString(CmisSessionSetting.URL);

		String repoId = null;
		for (CmisSessionSetting s : CmisSessionSetting.values()) {
			if ((s.getSessionParameter() == null) || !settings.hasValue(s)) {
				continue;
			}
			String v = settings.getString(s);
			switch (s) {
				case PASSWORD:
					v = settings.getAs(s, EncodedString.class).decode().toString();
					break;
				case REPOSITORY_ID:
					repoId = v;
					break;
				default:
					break;
			}
			if (!StringUtils.isBlank(v)) {
				parameters.put(s.getSessionParameter(), v);
			}
		}

		BindingType bindingType = BindingType.BROWSER;

		if (parameters.containsKey(SessionParameter.BINDING_TYPE)) {
			String bt = parameters.get(SessionParameter.BINDING_TYPE);
			bindingType = BindingType.fromValue(bt);
		}

		String urlParameterName = SessionParameter.BROWSER_URL;
		switch (bindingType) {
			case ATOMPUB:
				urlParameterName = SessionParameter.ATOMPUB_URL;
				break;

			case BROWSER:
				urlParameterName = SessionParameter.BROWSER_URL;
				break;

			case CUSTOM:
			case LOCAL:
			case WEBSERVICES:
				urlParameterName = null;
				break;
		}

		if (urlParameterName != null) {
			if (StringUtils.isBlank(url)) { throw new Exception("No URL given for " + bindingType + " binding"); }
			parameters.put(urlParameterName, url);
		}

		parameters.put(SessionParameter.BINDING_TYPE, bindingType.value());

		int ps = settings.getInteger(CmisSessionSetting.DEFAULT_PAGE_SIZE);
		if (ps <= 0) {
			// Any value <= 0 means don't alter the default page size setting
			ps = 0;
		} else {
			ps = Tools.ensureBetween(CmisSessionFactory.MIN_PAGE_SIZE, ps, CmisSessionFactory.MAX_PAGE_SIZE);
		}
		Repository repo = null;
		List<Repository> repositories = this.factory.getRepositories(parameters);
		if (repositories == null) {
			repositories = Collections.emptyList(); // safety net
		}
		Map<String, String> ids = new TreeMap<>();
		for (Repository r : repositories) {
			if ((repoId == null) || Objects.equals(repoId, r.getId())) {
				repo = r;
				break;
			}
			// If we don't have a match, keep track of what we've checked against
			ids.put(r.getId(), r.getName());
		}
		if (repo == null) {
			throw new RuntimeException(String.format(
				"No repository with ID [%s] was found - only found these repositories (id -> name): %s", repoId, ids));
		}
		parameters.put(CmisSessionSetting.REPOSITORY_ID.getSessionParameter(), repo.getId());

		// Allow for Alfresco extensions to be used if available
		if (StringUtils.equalsIgnoreCase(repo.getVendorName(), "Alfresco")) {
			final String alfresco = "org.alfresco.cmis.client.impl.AlfrescoObjectFactoryImpl";
			try {
				Class.forName(alfresco);
				parameters.put(SessionParameter.OBJECT_FACTORY_CLASS, alfresco);
			} catch (ClassNotFoundException e) {
				// Do nothing...no alfresco extensions available and we don't care
			}
		}

		this.defaultPageSize = ps;
		this.parameters = Tools.freezeMap(parameters);
	}

	@Override
	public PooledObject<Session> makeObject() throws Exception {
		Session session = this.factory.createSession(new HashMap<>(this.parameters));
		// NOTE: This context MUST NOT be modified elsewhere, under any circumstances
		OperationContext ctx = session.createOperationContext();
		ctx.setCacheEnabled(true);
		ctx.setFilterString("*");
		ctx.setIncludeAcls(true);
		ctx.setIncludePathSegments(true);
		ctx.setIncludeRelationships(IncludeRelationships.SOURCE);
		ctx.setLoadSecondaryTypeProperties(true);
		if (this.defaultPageSize != 0) {
			ctx.setMaxItemsPerPage(this.defaultPageSize);
		}
		ctx.setRenditionFilterString("*");
		session.setDefaultContext(ctx);
		return new DefaultPooledObject<>(session);
	}

	@Override
	public void destroyObject(PooledObject<Session> p) throws Exception {
		p.getObject().getBinding().close();
	}

	@Override
	public boolean validateObject(PooledObject<Session> p) {
		return (p != null);
	}

	@Override
	public void activateObject(PooledObject<Session> p) throws Exception {
	}

	@Override
	public void passivateObject(PooledObject<Session> p) throws Exception {
	}

	@Override
	protected CmisSessionWrapper newWrapper(Session session) throws SessionFactoryException {
		return new CmisSessionWrapper(this, session);
	}
}