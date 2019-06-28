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

package com.armedia.caliente.engine.sharepoint.exporter;

import com.armedia.caliente.engine.sharepoint.ShptSession;

/**
 *
 *
 */
public abstract class ShptSecurityObject<T> extends ShptObject<T> {

	protected ShptSecurityObject(ShptExportDelegateFactory factory, ShptSession session, Class<T> objectClass, T object)
		throws Exception {
		super(factory, session, objectClass, object);
	}

	@Override
	protected final String calculateSearchKey(ShptSession session, T object) {
		return String.valueOf(calculateNumericId(session, object));
	}

	@Override
	public String calculateObjectId(ShptSession session, T object) throws Exception {
		return String.format("%04x", calculateNumericId(session, object));
	}

	protected abstract int calculateNumericId(ShptSession session, T object);
}