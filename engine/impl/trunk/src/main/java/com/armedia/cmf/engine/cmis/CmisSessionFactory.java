package com.armedia.cmf.engine.cmis;

import java.util.Map;

import org.apache.chemistry.opencmis.client.api.Session;
import org.apache.chemistry.opencmis.client.runtime.SessionFactoryImpl;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;

import com.armedia.cmf.engine.SessionFactory;
import com.armedia.commons.utilities.CfgTools;

public class CmisSessionFactory extends SessionFactory<Session> {

	private final org.apache.chemistry.opencmis.client.api.SessionFactory factory = SessionFactoryImpl.newInstance();
	private final Map<String, String> parameters;

	public CmisSessionFactory(CfgTools settings) {
		super(settings);
		this.parameters = null;
	}

	@Override
	public PooledObject<Session> makeObject() throws Exception {
		return new DefaultPooledObject<Session>(this.factory.createSession(this.parameters));
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