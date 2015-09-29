/**
 *
 */

package com.armedia.cmf.engine.sharepoint;

import java.net.MalformedURLException;
import java.net.URL;

import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;

import com.armedia.cmf.engine.Crypt;
import com.armedia.cmf.engine.CryptException;
import com.armedia.cmf.engine.SessionFactory;
import com.armedia.commons.utilities.CfgTools;

/**
 * @author diego
 *
 */
public class ShptSessionFactory extends SessionFactory<ShptSession> {

	public static final String BASE_URL = Setting.BASE_URL.name;
	public static final String USER = Setting.USER.name;
	public static final String DOMAIN = Setting.DOMAIN.name;
	public static final String PASSWORD = Setting.PASSWORD.name;

	private final URL url;
	private final String user;
	private final String password;
	private final String domain;

	public ShptSessionFactory(CfgTools settings) throws MalformedURLException {
		super(settings);
		this.url = new URL(settings.getString(Setting.BASE_URL));
		this.user = settings.getString(Setting.USER);
		String pass = settings.getString(Setting.PASSWORD);
		try {
			pass = Crypt.decrypt(pass);
		} catch (CryptException e) {
			// Do nothing, use as literal
		}
		this.password = pass;
		this.domain = settings.getString(Setting.DOMAIN);
	}

	@Override
	public PooledObject<ShptSession> makeObject() {
		return new DefaultPooledObject<ShptSession>(new ShptSession(this.url, this.user, this.password, this.domain));
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
	protected ShptSessionWrapper newWrapper(ShptSession session) throws Exception {
		return new ShptSessionWrapper(this, session);
	}
}