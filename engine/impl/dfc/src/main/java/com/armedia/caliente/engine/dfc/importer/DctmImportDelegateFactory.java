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
package com.armedia.caliente.engine.dfc.importer;

import com.armedia.caliente.engine.dfc.DctmObjectType;
import com.armedia.caliente.engine.dfc.DctmSessionWrapper;
import com.armedia.caliente.engine.dfc.UnsupportedDctmObjectTypeException;
import com.armedia.caliente.engine.importer.ImportDelegateFactory;
import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.store.UnsupportedCmfObjectArchetypeException;
import com.armedia.commons.utilities.CfgTools;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.common.IDfValue;

public class DctmImportDelegateFactory
	extends ImportDelegateFactory<IDfSession, DctmSessionWrapper, IDfValue, DctmImportContext, DctmImportEngine> {

	protected DctmImportDelegateFactory(DctmImportEngine engine, CfgTools configuration) {
		super(engine, configuration);
	}

	@Override
	protected DctmImportDelegate<?> newImportDelegate(CmfObject<IDfValue> marshaled) throws Exception {
		DctmObjectType type = DctmObjectType.decodeType(marshaled.getType());
		if (type == null) { throw new UnsupportedCmfObjectArchetypeException(marshaled.getType()); }
		switch (type) {
			case DOCUMENT:
				return new DctmImportDocument(this, marshaled);
			case STORE:
				return new DctmImportStore(this, marshaled);
			case FOLDER:
				return new DctmImportFolder(this, marshaled);
			case FORMAT:
				return new DctmImportFormat(this, marshaled);
			case GROUP:
				return new DctmImportGroup(this, marshaled);
			case ACL:
				return new DctmImportACL(this, marshaled);
			case TYPE:
				return new DctmImportType(this, marshaled);
			case USER:
				return new DctmImportUser(this, marshaled);
			default:
				break;
		}
		throw new UnsupportedDctmObjectTypeException(String.format("Type [%s] is not supported", type.name()));
	}
}