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
package com.armedia.caliente.engine.cmis.exporter;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.chemistry.opencmis.client.api.ObjectType;
import org.apache.chemistry.opencmis.client.api.Session;

import com.armedia.caliente.engine.common.TypeDefinitionEncoder;
import com.armedia.caliente.engine.exporter.ExportException;
import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.store.CmfValue;

public class CmisObjectTypeDelegate extends CmisExportDelegate<ObjectType> {

	protected CmisObjectTypeDelegate(CmisExportDelegateFactory factory, Session session, ObjectType objectType)
		throws Exception {
		super(factory, session, ObjectType.class, objectType);
	}

	@Override
	protected boolean marshal(CmisExportContext ctx, CmfObject<CmfValue> object) throws ExportException {
		TypeDefinitionEncoder.encode(this.object, object, CmfValue::of);
		return true;
	}

	protected int calculateDepth(ObjectType objectType, final Set<String> visited) throws Exception {
		if (objectType == null) {
			throw new IllegalArgumentException("Must provide a folder whose depth to calculate");
		}
		if (!visited.add(objectType.getId())) {
			throw new IllegalStateException(
				String.format("ObjectType [%s] was visited twice - visited set: %s", objectType.getId(), visited));
		}
		try {
			if (objectType.isBaseType()) { return 0; }
			return calculateDepth(objectType.getParentType(), visited) + 1;
		} finally {
			visited.remove(objectType.getId());
		}
	}

	@Override
	protected int calculateDependencyTier(Session session, ObjectType objectType) throws Exception {
		return calculateDepth(objectType, new LinkedHashSet<String>());
	}

	@Override
	protected Collection<CmisExportDelegate<?>> identifyRequirements(CmfObject<CmfValue> marshalled,
		CmisExportContext ctx) throws Exception {
		Collection<CmisExportDelegate<?>> ret = super.identifyRequirements(marshalled, ctx);
		ObjectType objectType = this.object.getParentType();
		if (objectType != null) {
			ret.add(new CmisObjectTypeDelegate(this.factory, ctx.getSession(), objectType));
		}
		return ret;
	}

	@Override
	protected CmfObject.Archetype calculateType(Session session, ObjectType object) throws Exception {
		return CmfObject.Archetype.TYPE;
	}

	@Override
	protected String calculateLabel(Session session, ObjectType object) throws Exception {
		return object.getId();
	}

	@Override
	protected String calculateObjectId(Session session, ObjectType object) throws Exception {
		return object.getId();
	}

	@Override
	protected String calculateSearchKey(Session session, ObjectType object) throws Exception {
		return object.getId();
	}

	@Override
	protected String calculateName(Session session, ObjectType object) throws Exception {
		return object.getId();
	}
}