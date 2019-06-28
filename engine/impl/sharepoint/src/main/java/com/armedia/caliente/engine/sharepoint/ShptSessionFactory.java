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
 *
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