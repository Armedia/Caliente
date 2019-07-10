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
package com.armedia.caliente.engine.cmis.exporter;

import org.apache.chemistry.opencmis.client.api.FileableCmisObject;
import org.apache.chemistry.opencmis.client.api.Session;

import com.armedia.caliente.engine.converter.IntermediateAttribute;
import com.armedia.caliente.engine.exporter.ExportException;
import com.armedia.caliente.store.CmfAttribute;
import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.store.CmfValue;

public class CmisUserDelegate extends CmisExportDelegate<FileableCmisObject> {

	protected CmisUserDelegate(CmisExportDelegateFactory factory, Session session, FileableCmisObject object)
		throws Exception {
		super(factory, session, FileableCmisObject.class, object);
	}

	@Override
	protected CmfObject.Archetype calculateType(Session session, FileableCmisObject object) throws Exception {
		return CmfObject.Archetype.USER;
	}

	@Override
	protected String calculateLabel(Session session, FileableCmisObject object) throws Exception {
		return object.getCreatedBy();
	}

	@Override
	protected String calculateObjectId(Session session, FileableCmisObject object) throws Exception {
		return String.format("%s", object.getCreatedBy());
	}

	@Override
	protected String calculateSearchKey(Session session, FileableCmisObject object) throws Exception {
		return object.getCreatedBy();
	}

	@Override
	protected boolean marshal(CmisExportContext ctx, CmfObject<CmfValue> object) throws ExportException {
		CmfAttribute<CmfValue> userName = new CmfAttribute<>(IntermediateAttribute.NAME, CmfValue.Type.STRING, false);
		userName.setValue(new CmfValue(this.object.getCreatedBy()));
		userName = new CmfAttribute<>(IntermediateAttribute.LOGIN_NAME, CmfValue.Type.STRING, false);
		userName.setValue(new CmfValue(this.object.getCreatedBy()));
		return true;
	}

	@Override
	protected String calculateName(Session session, FileableCmisObject object) throws Exception {
		return object.getCreatedBy();
	}
}