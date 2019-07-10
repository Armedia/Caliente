/*******************************************************************************
 * #%L
 * Armedia Caliente
 * %%
 * Copyright (c) 2010 - 2019 Armedia LLC
 * %%
 * This file is part of the Caliente software.
 *
 * If the software was purchased under a paid Caliente license, the terms of
 * the paid license agreement will prevail.  Otherwise, the software is
 * provided under the following open source license terms:
 *
 * Caliente is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Caliente is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Caliente. If not, see <http://www.gnu.org/licenses/>.
 * #L%
 *******************************************************************************/
/**
 *
 */

package com.armedia.caliente.engine.dfc;

import org.apache.commons.pool2.PooledObject;

import com.armedia.caliente.engine.SessionFactory;
import com.armedia.caliente.engine.SessionFactoryException;
import com.armedia.caliente.tools.CmfCrypt;
import com.armedia.caliente.tools.dfc.pool.DfcSessionFactory;
import com.armedia.commons.utilities.CfgTools;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.common.DfException;

/**
 *
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
	protected DctmSessionWrapper newWrapper(IDfSession session) throws SessionFactoryException {
		return new DctmSessionWrapper(this, session);
	}
}