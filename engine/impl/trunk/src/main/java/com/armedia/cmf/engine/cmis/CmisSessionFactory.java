package com.armedia.cmf.engine.cmis;

import java.util.HashMap;
import java.util.Map;

import org.apache.chemistry.opencmis.client.api.Session;
import org.apache.chemistry.opencmis.client.runtime.SessionFactoryImpl;
import org.apache.chemistry.opencmis.commons.SessionParameter;
import org.apache.chemistry.opencmis.commons.enums.BindingType;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;

import com.armedia.cmf.engine.Crypt;
import com.armedia.cmf.engine.CryptException;
import com.armedia.cmf.engine.SessionFactory;
import com.armedia.commons.utilities.CfgTools;
import com.armedia.commons.utilities.Tools;

public class CmisSessionFactory extends SessionFactory<Session> {

	private final org.apache.chemistry.opencmis.client.api.SessionFactory factory = SessionFactoryImpl.newInstance();
	private final Map<String, String> parameters;

	public CmisSessionFactory(CfgTools settings) throws CryptException {
		super(settings);
		Map<String, String> parameters = new HashMap<String, String>();
		for (CmisSessionSetting s : CmisSessionSetting.values()) {
			String v = settings.getString(s);
			if (s == CmisSessionSetting.PASSWORD) {
				// Decrypt the password
				v = Crypt.decrypt(v);
			}
			if (!StringUtils.isBlank(v)) {
				parameters.put(s.getSessionParameter(), v);
			}
		}
		if (!parameters.containsKey(SessionParameter.BINDING_TYPE)) {
			parameters.put(SessionParameter.BINDING_TYPE, BindingType.ATOMPUB.value());
		}
		this.parameters = Tools.freezeMap(parameters);
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