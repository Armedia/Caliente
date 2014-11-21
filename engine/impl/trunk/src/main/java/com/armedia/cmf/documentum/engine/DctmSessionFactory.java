/**
 *
 */

package com.armedia.cmf.documentum.engine;

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

	public DctmSessionFactory(CfgTools settings) throws DfException {
		super(settings);
		this.factory = new DfcSessionFactory(settings);
	}

	@Override
	public IDfSession makeObject() throws Exception {
		return this.factory.makeObject();
	}

	@Override
	public void destroyObject(IDfSession obj) throws Exception {
		this.factory.destroyObject(obj);
	}

	@Override
	public boolean validateObject(IDfSession obj) {
		return this.factory.validateObject(obj);
	}

	@Override
	public void activateObject(IDfSession obj) throws Exception {
		this.factory.activateObject(obj);
	}

	@Override
	public void passivateObject(IDfSession obj) throws Exception {
		this.factory.passivateObject(obj);
	}

	@Override
	protected DctmSessionWrapper newWrapper(IDfSession session) throws Exception {
		return new DctmSessionWrapper(this, session);
	}
}