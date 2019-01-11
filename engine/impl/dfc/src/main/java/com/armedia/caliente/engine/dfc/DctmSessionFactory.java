/**
 *
 */

package com.armedia.caliente.engine.dfc;

import org.apache.commons.pool2.PooledObject;

import com.armedia.caliente.engine.SessionFactory;
import com.armedia.caliente.tools.CmfCrypt;
import com.armedia.commons.dfc.pool.DfcSessionFactory;
import com.armedia.commons.utilities.CfgTools;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.common.DfException;

/**
 * @author diego
 *
 */
public class DctmSessionFactory extends SessionFactory<IDfSession> {

	private final DfcSessionFactory factory;

	public DctmSessionFactory(CfgTools settings, CmfCrypt crypto) throws DfException {
		super(settings, crypto);
		String username = settings.getString(DctmSetting.USERNAME);
		String password = settings.getString(DctmSetting.PASSWORD);
		try {
			password = crypto.encrypt(crypto.decrypt(password));
		} catch (Exception e) {
			throw new DfException("Failed to re-encrypt the password", e);
		}
		String docbase = settings.getString(DctmSetting.DOCBASE);
		this.factory = new DfcSessionFactory(username, password, docbase);
	}

	@Override
	public PooledObject<IDfSession> makeObject() throws Exception {
		return this.factory.makeObject();
	}

	@Override
	public void destroyObject(PooledObject<IDfSession> obj) throws Exception {
		this.factory.destroyObject(obj);
	}

	@Override
	public boolean validateObject(PooledObject<IDfSession> obj) {
		return this.factory.validateObject(obj);
	}

	@Override
	public void activateObject(PooledObject<IDfSession> obj) throws Exception {
		this.factory.activateObject(obj);
	}

	@Override
	public void passivateObject(PooledObject<IDfSession> obj) throws Exception {
		this.factory.passivateObject(obj);
	}

	@Override
	protected DctmSessionWrapper newWrapper(IDfSession session) throws Exception {
		return new DctmSessionWrapper(this, session);
	}
}