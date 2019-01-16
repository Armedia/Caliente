/**
 *
 */

package com.armedia.caliente.engine.sharepoint;

import java.net.MalformedURLException;
import java.net.URL;

import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;

import com.armedia.caliente.engine.SessionFactory;
import com.armedia.caliente.engine.SessionFactoryException;
import com.armedia.caliente.tools.CmfCrypt;
import com.armedia.commons.utilities.CfgTools;

/**
 * @author diego
 *
 */
public class ShptSessionFactory extends SessionFactory<ShptSession> {

	private final URL url;
	private final String user;
	private final String password;
	private final String domain;

	public ShptSessionFactory(CfgTools settings, CmfCrypt crypto) throws MalformedURLException {
		super(settings, crypto);
		this.url = new URL(settings.getString(ShptSetting.URL));
		this.user = settings.getString(ShptSetting.USER);
		this.password = crypto.decrypt(settings.getString(ShptSetting.PASSWORD));
		this.domain = settings.getString(ShptSetting.DOMAIN);
	}

	@Override
	public PooledObject<ShptSession> makeObject() {
		return new DefaultPooledObject<>(new ShptSession(this.url, this.user, this.password, this.domain));
	}

	@Override
	public void destroyObject(PooledObject<ShptSession> service) {
		// Apparently, the client is stateless...
	}

	@Override
	public boolean validateObject(PooledObject<ShptSession> service) {
		if ((service == null) || (service.getObject() == null)) { return false; }
		try {
			service.getObject().getContextInfo();
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	@Override
	public void activateObject(PooledObject<ShptSession> service) throws Exception {
		service.getObject().getContextInfo();
	}

	@Override
	public void passivateObject(PooledObject<ShptSession> service) throws Exception {
	}

	@Override
	protected ShptSessionWrapper newWrapper(ShptSession session) throws SessionFactoryException {
		return new ShptSessionWrapper(this, session);
	}
}