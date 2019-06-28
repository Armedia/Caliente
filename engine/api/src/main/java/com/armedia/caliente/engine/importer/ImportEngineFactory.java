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
package com.armedia.caliente.engine.importer;

import java.util.Set;

import com.armedia.caliente.engine.TransferEngineFactory;
import com.armedia.caliente.tools.CmfCrypt;

public abstract class ImportEngineFactory< //
	SESSION, //
	VALUE, //
	CONTEXT extends ImportContext<SESSION, VALUE, CONTEXT_FACTORY>, //
	CONTEXT_FACTORY extends ImportContextFactory<SESSION, ?, VALUE, CONTEXT, ?, ?>, //
	DELEGATE_FACTORY extends ImportDelegateFactory<SESSION, ?, VALUE, CONTEXT, ?>, //
	ENGINE extends ImportEngine<SESSION, ?, VALUE, CONTEXT, CONTEXT_FACTORY, DELEGATE_FACTORY, ?> //
> extends
	TransferEngineFactory<ImportEngineListener, ImportResult, ImportException, SESSION, VALUE, CONTEXT, CONTEXT_FACTORY, DELEGATE_FACTORY, ENGINE> {

	protected ImportEngineFactory(boolean supportsDuplicateFileNames, CmfCrypt crypto) {
		super(supportsDuplicateFileNames, crypto);
	}

	public static Set<String> getAvailableImportEngineFactories() {
		return TransferEngineFactory.getAvailableEngineFactories(ImportEngineFactory.class);
	}

	public static ImportEngineFactory<?, ?, ?, ?, ?, ?> getImportEngineFactory(String targetName) {
		return TransferEngineFactory.getEngineFactory(ImportEngineFactory.class, targetName);
	}
}