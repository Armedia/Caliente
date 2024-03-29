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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.armedia.caliente.engine.converter.IntermediateProperty;
import com.armedia.caliente.engine.converter.PathIdHelper;
import com.armedia.caliente.engine.exporter.ExportException;
import com.armedia.caliente.engine.ucm.UcmSession;
import com.armedia.caliente.engine.ucm.model.UcmException;
import com.armedia.caliente.engine.ucm.model.UcmFSObject;
import com.armedia.caliente.engine.ucm.model.UcmFolder;
import com.armedia.caliente.engine.ucm.model.UcmModel;
import com.armedia.caliente.engine.ucm.model.UcmObjectNotFoundException;
import com.armedia.caliente.engine.ucm.model.UcmServiceException;
import com.armedia.caliente.store.CmfAttribute;
import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.store.CmfObjectRef;
import com.armedia.caliente.store.CmfProperty;
import com.armedia.caliente.store.CmfValue;
import com.armedia.commons.utilities.Tools;

public abstract class UcmFSObjectExportDelegate<T extends UcmFSObject> extends UcmExportDelegate<T> {

	protected UcmFSObjectExportDelegate(UcmExportDelegateFactory factory, UcmSession session, Class<T> objectClass,
		T object) throws Exception {
		super(factory, session, objectClass, object);
	}

	@Override
	protected final CmfObject.Archetype calculateType(UcmSession session, T object) throws Exception {
		return object.getType().archetype;
	}

	@Override
	protected String calculateLabel(UcmSession session, T object) throws Exception {
		return object.getPath();
	}

	@Override
	protected Collection<CmfObjectRef> calculateParentIds(UcmSession session, T object) throws Exception {
		Collection<CmfObjectRef> parents = new ArrayList<>();
		UcmFolder folder = object.getParentFolder(session);
		if (folder != null) {
			parents.add(new CmfObjectRef(CmfObject.Archetype.FOLDER, folder.getUniqueURI().toString()));
		}
		return parents;
	}

	@Override
	protected final String calculateObjectId(UcmSession session, T object) throws Exception {
		return object.getUniqueURI().toString();
	}

	@Override
	protected final String calculateSearchKey(UcmSession session, T object) throws Exception {
		return object.getUniqueURI().toString();
	}

	@Override
	protected final String calculateName(UcmSession session, T object) throws Exception {
		return object.getName();
	}

	@Override
	protected String calculateHistoryId(UcmSession session, T object) throws Exception {
		return object.getURI().getSchemeSpecificPart();
	}

	@Override
	protected Collection<UcmExportDelegate<?>> identifyRequirements(CmfObject<CmfValue> marshalled,
		UcmExportContext ctx) throws Exception {
		Collection<UcmExportDelegate<?>> requirements = super.identifyRequirements(marshalled, ctx);

		// First things first - add the parent folder, but only if it's not the root folder
		UcmFolder parent = this.object.getParentFolder(ctx.getSession());
		if ((parent != null) && !parent.isRoot()) {
			requirements.add(new UcmFolderExportDelegate(this.factory, ctx.getSession(), parent));
		}

		if (this.object.isShortcut()) {
			final String targetGuid = this.object.getTargetGUID();
			switch (this.object.getType()) {
				case FILE:
					requirements.add(new UcmFileExportDelegate(this.factory, ctx.getSession(),
						ctx.getSession().getFileByGUID(targetGuid)));
					break;

				case FOLDER:
					requirements.add(new UcmFolderExportDelegate(this.factory, ctx.getSession(),
						ctx.getSession().getFolderByGUID(targetGuid)));
					break;
				default:
					break;
			}
		}
		return requirements;
	}

	@Override
	protected boolean marshal(UcmExportContext ctx, CmfObject<CmfValue> object) throws ExportException {
		T typedObject = castObject(this.object);
		for (String att : this.object.getAttributeNames()) {
			CmfValue v = this.object.getValue(att);
			if ((v == null) || v.isNull()) {
				// Skip null-values
				continue;
			}

			CmfAttribute<CmfValue> attribute = new CmfAttribute<>(att, v.getDataType(), false,
				Collections.singleton(v));
			object.setAttribute(attribute);
		}

		// Properties are different from attributes in that they require special handling. For
		// instance, a property would only be settable via direct SQL, or via an explicit method
		// call, etc., because setting it directly as an attribute would cmsImportResult in an
		// error from DFC, and therefore specialized code is required to handle it
		List<CmfProperty<CmfValue>> properties = new ArrayList<>();
		getDataProperties(ctx, properties, typedObject);
		properties.forEach(object::setProperty);
		return true;
	}

	protected boolean getDataProperties(UcmExportContext ctx, Collection<CmfProperty<CmfValue>> properties, T object)
		throws ExportException {
		String path = object.getParentPath();
		CmfProperty<CmfValue> p = new CmfProperty<>(IntermediateProperty.PATH, CmfValue.Type.STRING,
			CmfValue.of(Tools.coalesce(path, StringUtils.EMPTY)));
		properties.add(p);

		p = new CmfProperty<>(IntermediateProperty.FULL_PATH, CmfValue.Type.STRING,
			CmfValue.of(String.format("%s/%s", Tools.coalesce(path, StringUtils.EMPTY), object.getName())));
		properties.add(p);

		p = new CmfProperty<>(IntermediateProperty.IS_REFERENCE, CmfValue.Type.BOOLEAN,
			CmfValue.of(object.isShortcut()));
		properties.add(p);
		if (object.isShortcut()) {
			String targetGuid = object.getTargetGUID();
			UcmFSObject target = null;
			try {
				if (getType() == CmfObject.Archetype.DOCUMENT) {
					target = ctx.getSession().getFileByGUID(targetGuid);
				} else {
					target = ctx.getSession().getFolderByGUID(targetGuid);
				}
			} catch (UcmObjectNotFoundException | UcmServiceException e) {
				throw new ExportException(
					String.format("Failed to locate the referenced %s with GUID %s", getType().name(), targetGuid), e);
			}
			p = new CmfProperty<>(IntermediateProperty.REF_TARGET, CmfValue.Type.STRING,
				CmfValue.of(target.getURI().toString()));
			properties.add(p);
			p = new CmfProperty<>(IntermediateProperty.REF_VERSION, CmfValue.Type.STRING, CmfValue.of("HEAD"));
			properties.add(p);
		}

		URI parentUri = object.getParentURI();
		p = new CmfProperty<>(IntermediateProperty.PARENT_ID, CmfValue.Type.ID, true);
		properties.add(p);

		if (!UcmModel.isRoot(parentUri)) {
			p.addValue(CmfValue.of(parentUri.toString()));
		}

		p = new CmfProperty<>(IntermediateProperty.PARENT_TREE_IDS, CmfValue.Type.STRING, true);
		properties.add(p);
		if (!UcmModel.isRoot(parentUri)) {
			LinkedList<String> l = new LinkedList<>();
			UcmFolder parentFolder = null;
			while (true) {
				try {
					parentFolder = (parentFolder == null ? object : parentFolder).getParentFolder(ctx.getSession());
					if ((parentFolder == null) || parentFolder.isRoot()) {
						// If this object has no parent, then there's no parent IDs to generate
						break;
					}
				} catch (UcmException e) {
					throw new ExportException(e.getMessage(), e);
				}
				l.addFirst(parentFolder.getURI().toString());
			}
			p.addValue(CmfValue.of(PathIdHelper.encodePaths(l)));
		}
		return true;
	}
}