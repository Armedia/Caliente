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
package com.armedia.caliente.engine.exporter;

import com.armedia.caliente.engine.SessionWrapper;
import com.armedia.caliente.engine.TransferDelegateFactory;
import com.armedia.caliente.store.CmfObject;
import com.armedia.commons.utilities.CfgTools;

public abstract class ExportDelegateFactory< //
	SESSION, //
	SESSION_WRAPPER extends SessionWrapper<SESSION>, //
	VALUE, //
	CONTEXT extends ExportContext<SESSION, VALUE, ?>, //
	ENGINE extends ExportEngine<SESSION, SESSION_WRAPPER, VALUE, CONTEXT, ?, ?, ?> //
> extends TransferDelegateFactory<SESSION, VALUE, CONTEXT, ENGINE> {

	protected ExportDelegateFactory(ENGINE engine, CfgTools configuration) {
		super(engine, configuration);
	}

	protected abstract ExportDelegate<?, SESSION, SESSION_WRAPPER, VALUE, CONTEXT, ?, ENGINE> newExportDelegate(
		SESSION session, CmfObject.Archetype type, String searchKey) throws Exception;
}