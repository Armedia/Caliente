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
package com.armedia.caliente.engine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class TransferDelegate< //
	ECM_OBJECT, //
	SESSION, //
	VALUE, //
	CONTEXT extends TransferContext<SESSION, VALUE, ?>, //
	DELEGATE_FACTORY extends TransferDelegateFactory<SESSION, VALUE, CONTEXT, ENGINE>, //
	ENGINE extends TransferEngine<?, ?, ?, SESSION, VALUE, CONTEXT, ?, DELEGATE_FACTORY, ?> //
> {
	protected final Logger log = LoggerFactory.getLogger(getClass());

	protected final DELEGATE_FACTORY factory;
	protected final Class<ECM_OBJECT> objectClass;

	protected TransferDelegate(DELEGATE_FACTORY factory, Class<ECM_OBJECT> objectClass) throws Exception {
		if (factory == null) { throw new IllegalArgumentException("Must provide a factory to process with"); }
		if (objectClass == null) { throw new IllegalArgumentException("Must provide an object class to work with"); }
		this.factory = factory;
		this.objectClass = objectClass;
	}

	protected final ECM_OBJECT castObject(Object o) {
		// This should NEVER fail...but if it does, it's well-deserved and we make no effort to
		// catch it or soften the blow
		return this.objectClass.cast(o);
	}

	public final Class<ECM_OBJECT> getObjectClass() {
		return this.objectClass;
	}
}