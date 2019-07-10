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
package com.armedia.caliente.engine.dfc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.caliente.engine.TransferEngine;
import com.armedia.caliente.store.CmfObject;
import com.documentum.fc.client.IDfPersistentObject;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.common.DfException;
import com.documentum.fc.common.IDfValue;

public class DctmDelegateBase<T extends IDfPersistentObject, E extends TransferEngine<?, ?, ?, IDfSession, IDfValue, ?, ?, ?, ?>> {
	protected final Logger log = LoggerFactory.getLogger(getClass());

	private final Class<T> dfClass;
	private final DctmObjectType type;
	private final E engine;

	protected DctmDelegateBase(E engine, CmfObject.Archetype type) {
		this(engine, DctmObjectType.decodeType(type));
	}

	protected DctmDelegateBase(E engine, DctmObjectType type) {
		if (engine == null) {
			throw new IllegalArgumentException("Must provide the engine that will interact with this delegate");
		}
		if (type == null) {
			throw new IllegalArgumentException("Must provide the object type for which this delegate will operate");
		}
		this.engine = engine;
		this.type = type;
		@SuppressWarnings("unchecked")
		Class<T> c = (Class<T>) type.getDfClass();
		this.dfClass = c;
	}

	protected final Class<T> getDfClass() {
		return this.dfClass;
	}

	protected final E getEngine() {
		return this.engine;
	}

	protected final DctmObjectType getDctmType() {
		return this.type;
	}

	protected final T castObject(IDfPersistentObject object) throws DfException {
		if (object == null) { return null; }
		if (!this.dfClass.isInstance(object)) {
			throw new DfException(String.format("Expected an object of class %s, but got one of class %s",
				this.dfClass.getCanonicalName(), object.getClass().getCanonicalName()));
		}
		return this.dfClass.cast(object);
	}
}