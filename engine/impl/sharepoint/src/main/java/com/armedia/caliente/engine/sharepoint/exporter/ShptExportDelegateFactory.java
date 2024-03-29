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
package com.armedia.caliente.engine.sharepoint.exporter;

import com.armedia.caliente.engine.exporter.ExportDelegateFactory;
import com.armedia.caliente.engine.exporter.ExportTarget;
import com.armedia.caliente.engine.sharepoint.ShptSession;
import com.armedia.caliente.engine.sharepoint.ShptSessionWrapper;
import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.store.CmfValue;
import com.armedia.commons.utilities.CfgTools;
import com.armedia.commons.utilities.Tools;

public class ShptExportDelegateFactory
	extends ExportDelegateFactory<ShptSession, ShptSessionWrapper, CmfValue, ShptExportContext, ShptExportEngine> {

	protected ShptExportDelegateFactory(ShptExportEngine engine, CfgTools configuration) {
		super(engine, configuration);
	}

	@Override
	protected ShptExportDelegate<?> newExportDelegate(ShptSession session, ExportTarget target) throws Exception {
		CmfObject.Archetype type = target.getType();
		String searchKey = target.getSearchKey();
		switch (type) {
			case USER:
				return new ShptUser(this, session, session.getUser(Tools.decodeInteger(searchKey)));
			case GROUP:
				return new ShptGroup(this, session, session.getGroup(Tools.decodeInteger(searchKey)));
			case FOLDER:
				return new ShptFolder(this, session, session.getFolder(searchKey));
			case DOCUMENT:
				return ShptFile.locateFile(this, session, searchKey);
			default:
				throw new Exception(String.format("Unsupported object type [%s]", type));
		}
	}
}