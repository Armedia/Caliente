/**
 *
 */

package com.armedia.cmf.engine.documentum;

import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;

import com.armedia.cmf.engine.CmfCrypt;
import com.armedia.cmf.engine.SessionFactory;
import com.armedia.commons.dfc.pool.DfcSessionFactory;
import com.armedia.commons.utilities.CfgTools;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.common.DfException;

/**
 * @author diego
 *
 */
public class DctmSessionFactory extends SessionFactory<IDfSession> {
	public static final String DOCBASE = DfcSessionFactory.DOCBASE;
	public static final String USERNAME = DfcSessionFactory.USERNAME;
	public static final String PASSWORD = DfcSessionFactory.PASSWORD;

	private final DfcSessionFactory factory;

	public DctmSessionFactory(CfgTools settings, CmfCrypt crypto) throws DfException {
		super(settings, crypto);
		String username = settings.getString(DctmSessionFactory.USERNAME);
		String password = settings.getString(DctmSessionFactory.PASSWORD);
		try {
			password = crypto.encrypt(crypto.decrypt(password));
		} catch (Exception e) {
			throw new DfException("Failed to re-encrypt the password", e);
		}
		String docbase = settings.getString(DctmSessionFactory.DOCBASE);
		this.factory = new DfcSessionFactory(username, password, docbase);
	}

	@Override
	public PooledObject<IDfSession> makeObject() throws Exception {
		return new DefaultPooledObject<IDfSession>(this.factory.makeObject());
	}

	@Override
	public void destroyObject(PooledObject<IDfSession> obj) throws Exception {
		this.factory.destroyObject(obj.getObject());
	}

	@Override
	public boolean validateObject(PooledObject<IDfSession> obj) {
		return this.factory.validateObject(obj.getObject());
	}

	@Override
	public void activateObject(PooledObject<IDfSession> obj) throws Exception {
		this.factory.activateObject(obj.getObject());
	}

	@Override
	public void passivateObject(PooledObject<IDfSession> obj) throws Exception {
		this.factory.passivateObject(obj.getObject());
	}

	@Override
	protected DctmSessionWrapper newWrapper(IDfSession session) throws Exception {
		return new DctmSessionWrapper(this, session);
	}
}