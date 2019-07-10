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
package com.armedia.caliente.engine.xml.importer;

import java.util.Collection;
import java.util.Collections;

import com.armedia.caliente.engine.importer.ImportException;
import com.armedia.caliente.engine.importer.ImportOutcome;
import com.armedia.caliente.engine.importer.ImportResult;
import com.armedia.caliente.engine.xml.importer.jaxb.AggregatorBase;
import com.armedia.caliente.store.CmfAttributeTranslator;
import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.store.CmfStorageException;
import com.armedia.caliente.store.CmfValue;

abstract class XmlAggregatedImportDelegate<I, T extends AggregatorBase<I>> extends XmlImportDelegate {

	private final Class<T> xmlClass;

	protected XmlAggregatedImportDelegate(XmlImportDelegateFactory factory, CmfObject<CmfValue> storedObject,
		Class<T> xmlClass) throws Exception {
		super(factory, storedObject);
		this.xmlClass = xmlClass;
	}

	@Override
	protected final Collection<ImportOutcome> importObject(CmfAttributeTranslator<CmfValue> translator,
		XmlImportContext ctx) throws ImportException, CmfStorageException {
		I item = createItem(translator, ctx);
		if (item == null) { return Collections.singleton(ImportOutcome.SKIPPED); }
		getXmlObject().add(item);
		return Collections
			.singleton(new ImportOutcome(ImportResult.CREATED, this.cmfObject.getId(), this.cmfObject.getLabel()));
	}

	protected abstract I createItem(CmfAttributeTranslator<CmfValue> translator, XmlImportContext ctx)
		throws ImportException, CmfStorageException;

	private T getXmlObject() {
		return this.factory.getXmlObject(this.cmfObject.getType(), this.xmlClass);
	}
}