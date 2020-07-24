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
package com.armedia.caliente.engine.ucm.exporter;

import java.net.URI;

import com.armedia.caliente.engine.exporter.ExportDelegateFactory;
import com.armedia.caliente.engine.exporter.ExportTarget;
import com.armedia.caliente.engine.ucm.UcmSession;
import com.armedia.caliente.engine.ucm.UcmSessionWrapper;
import com.armedia.caliente.store.CmfValue;
import com.armedia.commons.utilities.CfgTools;

public class UcmExportDelegateFactory
	extends ExportDelegateFactory<UcmSession, UcmSessionWrapper, CmfValue, UcmExportContext, UcmExportEngine> {

	UcmExportDelegateFactory(UcmExportEngine engine, CfgTools configuration) {
		super(engine, configuration);
	}

	@Override
	protected UcmExportDelegate<?> newExportDelegate(UcmSession session, ExportTarget target) throws Exception {
		URI uri = URI.create(target.getSearchKey());
		switch (target.getType()) {
			case DOCUMENT:
				return new UcmFileExportDelegate(this, session, session.getFile(uri));
			case FOLDER:
				return new UcmFolderExportDelegate(this, session, session.getFolder(uri));
			default:
				return null;
		}
	}
}