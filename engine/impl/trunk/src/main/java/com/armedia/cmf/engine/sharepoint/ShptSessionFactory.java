/**
 *
 */

package com.armedia.cmf.engine.sharepoint;

import com.armedia.cmf.engine.SessionFactory;
import com.armedia.commons.utilities.CfgTools;
import com.independentsoft.share.Service;

/**
 * @author diego
 *
 */
public class ShptSessionFactory extends SessionFactory<Service> {

	public static final String URL = Setting.URL.name;
	public static final String USER = Setting.USER.name;
	public static final String DOMAIN = Setting.DOMAIN.name;
	public static final String PASSWORD = Setting.PASSWORD.name;

	private final String url;
	private final String user;
	private final String password;
	private final String domain;

	public ShptSessionFactory(CfgTools settings) {
		super(settings);
		this.url = settings.getString(Setting.URL);
		this.user = settings.getString(Setting.USER);
		this.password = settings.getString(Setting.PASSWORD);
		this.domain = settings.getString(Setting.DOMAIN);
	}

	@Override
	public Service makeObject() {
		return new Service(this.url, this.user, this.password, this.domain);
	}

	@Override
	public void destroyObject(Service service) {
		// Apparently, the client is stateless...
	}

	@Override
	public boolean validateObject(Service service) {
		if (service == null) { return false; }
		try {
			service.getContextInfo();
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	@Override
	public void activateObject(Service service) throws Exception {
		service.getContextInfo();
	}

	@Override
	public void passivateObject(Service service) throws Exception {
	}

	@Override
	protected ShptSessionWrapper newWrapper(Service session) throws Exception {
		return new ShptSessionWrapper(this, session);
	}
}