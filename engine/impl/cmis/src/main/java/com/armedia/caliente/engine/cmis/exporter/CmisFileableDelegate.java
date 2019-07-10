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

import java.text.ParseException;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.apache.chemistry.opencmis.client.api.Document;
import org.apache.chemistry.opencmis.client.api.FileableCmisObject;
import org.apache.chemistry.opencmis.client.api.Folder;
import org.apache.chemistry.opencmis.client.api.SecondaryType;
import org.apache.chemistry.opencmis.client.api.Session;
import org.apache.commons.lang3.StringUtils;

import com.armedia.caliente.engine.converter.IntermediateProperty;
import com.armedia.caliente.engine.exporter.ExportException;
import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.store.CmfProperty;
import com.armedia.caliente.store.CmfValue;

public abstract class CmisFileableDelegate<T extends FileableCmisObject> extends CmisObjectDelegate<T> {

	protected CmisFileableDelegate(CmisExportDelegateFactory factory, Session session, Class<T> objectClass, T object)
		throws Exception {
		super(factory, session, objectClass, object);
	}

	protected String calculatePath(Session session, T f) throws Exception {
		List<String> paths = f.getPaths();
		if (paths.isEmpty()) { return null; }
		return paths.get(0);
	}

	@Override
	protected final String calculateLabel(Session session, T f) throws Exception {
		String path = calculatePath(session, f);
		if (path == null) {
			path = String.format("${unfiled}:%s:%s", f.getName(), f.getId());
		}
		String version = calculateVersion(f);
		if (StringUtils.isBlank(version)) { return path; }
		return String.format("%s#%s", path, version);
	}

	@Override
	protected Set<String> calculateSecondarySubtypes(Session session, CmfObject.Archetype type, String subtype,
		T object) throws Exception {
		Set<String> secondaries = super.calculateSecondarySubtypes(session, type, subtype, object);
		List<SecondaryType> t = object.getSecondaryTypes();
		if ((t != null) && !t.isEmpty()) {
			for (SecondaryType st : t) {
				secondaries.add(st.getId());
			}
		}
		return secondaries;
	}

	protected String calculateVersion(T obj) throws Exception {
		return null;
	}

	@Override
	protected String calculateSubType(Session session, CmfObject.Archetype type, T obj) throws Exception {
		return obj.getType().getId();
	}

	protected void marshalParentsAndPaths(CmisExportContext ctx, CmfObject<CmfValue> marshaled, T object)
		throws ExportException {
		CmfProperty<CmfValue> parents = new CmfProperty<>(IntermediateProperty.PARENT_ID, CmfValue.Type.ID, true);
		CmfProperty<CmfValue> paths = new CmfProperty<>(IntermediateProperty.PATH, CmfValue.Type.STRING, true);
		final String rootPath = ctx.getSession().getRootFolder().getName();
		for (Folder f : object.getParents()) {
			try {
				parents.addValue(new CmfValue(CmfValue.Type.ID, f.getId()));
			} catch (ParseException e) {
				// Will not happen...but still
				throw new ExportException(String.format("Failed to store the parent ID [%s] for %s [%s]", f.getId(),
					this.object.getType(), this.object.getId()), e);
			}
			for (String p : f.getPaths()) {
				paths.addValue(new CmfValue(String.format("/%s%s", rootPath, p)));
			}
		}
		marshaled.setProperty(paths);
		marshaled.setProperty(parents);
	}

	@Override
	protected boolean marshal(CmisExportContext ctx, CmfObject<CmfValue> object) throws ExportException {
		if (!super.marshal(ctx, object)) { return false; }
		marshalParentsAndPaths(ctx, object, this.object);
		return true;
	}

	@Override
	protected Collection<CmisExportDelegate<?>> identifyRequirements(CmfObject<CmfValue> marshalled,
		CmisExportContext ctx) throws Exception {
		Collection<CmisExportDelegate<?>> ret = super.identifyRequirements(marshalled, ctx);
		for (Folder f : this.object.getParents()) {
			ret.add(new CmisFolderDelegate(this.factory, ctx.getSession(), f));
		}
		ret.add(new CmisAclDelegate(this.factory, ctx.getSession(), this.object));
		ret.add(new CmisObjectTypeDelegate(this.factory, ctx.getSession(), this.object.getType()));
		ret.add(new CmisUserDelegate(this.factory, ctx.getSession(), this.object));
		return ret;
	}

	@Override
	protected final CmfObject.Archetype calculateType(Session session, T object) throws Exception {
		if (Document.class.isInstance(object)) { return CmfObject.Archetype.DOCUMENT; }
		if (Folder.class.isInstance(object)) { return CmfObject.Archetype.FOLDER; }
		throw new Exception(String.format("Can't identify the type for object with ID [%s] of class [%s] and type [%s]",
			object.getId(), object.getClass().getCanonicalName(), object.getType().getId()));
	}
}