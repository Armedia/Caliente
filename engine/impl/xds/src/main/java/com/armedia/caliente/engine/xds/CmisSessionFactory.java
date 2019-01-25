package com.armedia.caliente.engine.xds;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

import com.armedia.caliente.engine.SessionFactory;
import com.armedia.caliente.engine.SessionFactoryException;
import com.armedia.caliente.tools.CmfCrypt;
import com.armedia.caliente.tools.CryptException;
import com.armedia.commons.utilities.CfgTools;
import com.armedia.commons.utilities.Tools;

public class CmisSessionFactory extends SessionFactory<Session> {

	private static final int MIN_PAGE_SIZE = 10;
	private static final int MAX_PAGE_SIZE = Integer.MAX_VALUE;

	private final org.apache.chemistry.opencmis.client.api.SessionFactory factory = SessionFactoryImpl.newInstance();
	private final Map<String, String> parameters;
	private final int defaultPageSize;

	public CmisSessionFactory(CfgTools settings, CmfCrypt crypto) throws CryptException {
		super(settings, crypto);
		Map<String, String> parameters = new HashMap<>();

		String repoId = null;
		for (CmisSessionSetting s : CmisSessionSetting.values()) {
			if ((s.getSessionParameter() == null) || !settings.hasValue(s)) {
				continue;
			}
			String v = settings.getString(s);
			switch (s) {
				case PASSWORD:
					v = this.crypto.decrypt(v);
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
		if (!parameters.containsKey(SessionParameter.BINDING_TYPE)) {
			parameters.put(SessionParameter.BINDING_TYPE, BindingType.ATOMPUB.value());
		}
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
			if ((repoId == null) || Tools.equals(repoId, r.getId())) {
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