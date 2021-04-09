/*******************************************************************************
 * #%L
 * Armedia Caliente
 * %%
 * Copyright (C) 2013 - 2019 Armedia, LLC
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
package com.armedia.caliente.engine.importer;

import java.util.Collection;
import java.util.List;
import java.util.function.UnaryOperator;

import com.armedia.caliente.engine.TransferDelegate;
import com.armedia.caliente.engine.common.SessionWrapper;
import com.armedia.caliente.store.CmfAttributeTranslator;
import com.armedia.caliente.store.CmfEncodeableName;
import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.store.CmfStorageException;

public abstract class ImportDelegate< //
	ECM_OBJECT, //
	SESSION, //
	SESSION_WRAPPER extends SessionWrapper<SESSION>, //
	VALUE, //
	CONTEXT extends ImportContext<SESSION, VALUE, ?>, //
	DELEGATE_FACTORY extends ImportDelegateFactory<SESSION, SESSION_WRAPPER, VALUE, CONTEXT, ENGINE>, //
	ENGINE extends ImportEngine<SESSION, SESSION_WRAPPER, VALUE, CONTEXT, ?, DELEGATE_FACTORY, ?> //
> extends TransferDelegate<ECM_OBJECT, SESSION, VALUE, CONTEXT, DELEGATE_FACTORY, ENGINE> {

	protected final CmfObject<VALUE> cmfObject;
	protected final ImportStrategy strategy;

	protected ImportDelegate(DELEGATE_FACTORY factory, Class<ECM_OBJECT> objectClass, CmfObject<VALUE> storedObject)
		throws Exception {
		super(factory, objectClass);
		this.cmfObject = storedObject;
		this.strategy = factory.getEngine().getImportStrategy(storedObject.getType());
	}

	protected abstract Collection<ImportOutcome> importObject(CmfAttributeTranslator<VALUE> translator, CONTEXT ctx)
		throws ImportException, CmfStorageException;

	protected final VALUE getAttributeValue(CmfEncodeableName attribute) {
		return this.factory.getAttributeValue(this.cmfObject, attribute);
	}

	protected final VALUE getAttributeValue(String attribute) {
		return this.factory.getAttributeValue(this.cmfObject, attribute);
	}

	protected final List<VALUE> getAttributeValues(CmfEncodeableName attribute) {
		return this.factory.getAttributeValues(this.cmfObject, attribute);
	}

	protected final List<VALUE> getAttributeValues(String attribute) {
		return this.factory.getAttributeValues(this.cmfObject, attribute);
	}

	protected final VALUE getPropertyValue(CmfEncodeableName property) {
		return this.factory.getPropertyValue(this.cmfObject, property);
	}

	protected final VALUE getPropertyValue(String property) {
		return this.factory.getPropertyValue(this.cmfObject, property);
	}

	protected final List<VALUE> getPropertyValues(CmfEncodeableName property) {
		return this.factory.getPropertyValues(this.cmfObject, property);
	}

	protected final List<VALUE> getPropertyValues(String property) {
		return this.factory.getPropertyValues(this.cmfObject, property);
	}

	public final String getFixedPath(CONTEXT ctx) throws ImportException {
		return getFixedPath(ctx, null);
	}

	public final String getFixedPath(CONTEXT ctx, UnaryOperator<String> pathFix) throws ImportException {
		return this.factory.getFixedPath(this.cmfObject, ctx, pathFix);
	}
}