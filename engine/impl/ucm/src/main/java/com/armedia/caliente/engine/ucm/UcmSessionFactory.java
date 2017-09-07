package com.armedia.caliente.engine.ucm;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;

import com.armedia.caliente.engine.SessionFactory;
import com.armedia.caliente.tools.CmfCrypt;
import com.armedia.caliente.tools.CryptException;
import com.armedia.commons.utilities.CfgTools;

import oracle.stellent.ridc.IdcClientManager;
import oracle.stellent.ridc.IdcContext;

public class UcmSessionFactory extends SessionFactory<IdcSession> {

	private final IdcClientManager manager;
	private final String url;
	private final IdcContext context;

	public UcmSessionFactory(CfgTools settings, CmfCrypt crypto) throws CryptException {
		super(settings, crypto);

		this.manager = new IdcClientManager();
		this.context = null;
		this.url = null;

		Map<String, String> parameters = new HashMap<>();

		for (UcmSessionSetting s : UcmSessionSetting.values()) {
			if (!settings.hasValue(s)) {
				continue;
			}
			String v = settings.getString(s);
			switch (s) {
				case PASSWORD:
					v = this.crypto.decrypt(v);
					break;
				default:
					break;
			}
			if (!StringUtils.isBlank(v)) {
				parameters.put(s.name(), v);
			}
		}
	}

	@Override
	public PooledObject<IdcSession> makeObject() throws Exception {
		return new DefaultPooledObject<>(new IdcSession(this.manager.createClient(this.url), this.context));
	}

	@Override
	public void destroyObject(PooledObject<IdcSession> p) throws Exception {
		p.getObject().logout();
	}

	@Override
	public boolean validateObject(PooledObject<IdcSession> p) {
		// TODO: Check the idle state against max idle...
		return (p != null) && p.getObject().isInitialized();
	}

	@Override
	public void activateObject(PooledObject<IdcSession> p) throws Exception {
		// TODO: do we need to do something here?
	}

	@Override
	public void passivateObject(PooledObject<IdcSession> p) throws Exception {
		// TODO: do we need to do something here?
	}

	@Override
	protected UcmSessionWrapper newWrapper(IdcSession session) throws Exception {
		return new UcmSessionWrapper(this, session);
	}
}