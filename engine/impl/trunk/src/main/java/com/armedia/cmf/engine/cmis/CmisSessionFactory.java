package com.armedia.cmf.engine.cmis;

import java.util.HashMap;
import java.util.Map;

import org.apache.chemistry.opencmis.client.api.OperationContext;
import org.apache.chemistry.opencmis.client.api.Session;
import org.apache.chemistry.opencmis.client.runtime.SessionFactoryImpl;
import org.apache.chemistry.opencmis.commons.SessionParameter;
import org.apache.chemistry.opencmis.commons.enums.BindingType;
import org.apache.chemistry.opencmis.commons.enums.IncludeRelationships;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;

import com.armedia.cmf.engine.CMFCrypto;
import com.armedia.cmf.engine.CryptException;
import com.armedia.cmf.engine.SessionFactory;
import com.armedia.commons.utilities.CfgTools;
import com.armedia.commons.utilities.Tools;

public class CmisSessionFactory extends SessionFactory<Session> {

	private static final int MIN_PAGE_SIZE = 10;
	private static final int MAX_PAGE_SIZE = Integer.MAX_VALUE;

	private final org.apache.chemistry.opencmis.client.api.SessionFactory factory = SessionFactoryImpl.newInstance();
	private final Map<String, String> parameters;
	private final int defaultPageSize;

	public CmisSessionFactory(CfgTools settings) throws CryptException {
		super(settings);
		Map<String, String> parameters = new HashMap<String, String>();

		for (CmisSessionSetting s : CmisSessionSetting.values()) {
			if ((s.getSessionParameter() == null) || !settings.hasValue(s)) {
				continue;
			}
			String v = settings.getString(s);
			switch (s) {
				case PASSWORD:
					v = new CMFCrypto().decryptPassword(v);
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
		this.defaultPageSize = ps;
		this.parameters = Tools.freezeMap(parameters);
	}

	@Override
	public PooledObject<Session> makeObject() throws Exception {
		Session session = this.factory.createSession(new HashMap<String, String>(this.parameters));
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
		return new DefaultPooledObject<Session>(session);
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
	protected CmisSessionWrapper newWrapper(Session session) throws Exception {
		return new CmisSessionWrapper(this, session);
	}
}