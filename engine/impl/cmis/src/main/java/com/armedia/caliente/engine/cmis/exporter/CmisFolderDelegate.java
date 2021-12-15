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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import org.apache.chemistry.opencmis.client.api.CmisObject;
import org.apache.chemistry.opencmis.client.api.Document;
import org.apache.chemistry.opencmis.client.api.Folder;
import org.apache.chemistry.opencmis.client.api.Session;

import com.armedia.caliente.engine.cmis.CmisPagingIterator;
import com.armedia.caliente.engine.exporter.ExportException;
import com.armedia.caliente.engine.exporter.ExportTarget;
import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.store.CmfValue;
import com.armedia.commons.utilities.FileNameTools;

public class CmisFolderDelegate extends CmisFileableDelegate<Folder> {

	protected CmisFolderDelegate(CmisExportDelegateFactory factory, Session session, Folder folder) throws Exception {
		super(factory, session, Folder.class, folder);
	}

	@Override
	protected boolean marshal(CmisExportContext ctx, CmfObject<CmfValue> object) throws ExportException {
		return super.marshal(ctx, object);
	}

	@Override
	protected int calculateDependencyTier(Session session, Folder folder) throws Exception {
		if (folder == null) { throw new IllegalArgumentException("Must provide a folder whose depth to calculate"); }
		if (folder.isRootFolder()) { return 0; }
		List<Folder> parents = folder.getParents();
		int max = -1;
		for (Folder parent : parents) {
			for (String path : this.factory.getPaths(parent)) {
				List<String> elements = FileNameTools.tokenize(path, '/');
				max = Math.max(max, elements.size());
			}
		}
		return max + 1;
	}

	@Override
	protected Collection<CmisExportDelegate<?>> identifyDependents(CmfObject<CmfValue> marshalled,
		CmisExportContext ctx) throws Exception {
		Collection<CmisExportDelegate<?>> ret = super.identifyDependents(marshalled, ctx);
		// We will only include the folder's contents if the referencing object is NOT one of our
		// children
		ExportTarget referrent = ctx.getReferrent();
		CmisPagingIterator<CmisObject> it = new CmisPagingIterator<>(this.object.getChildren());
		Collection<CmisFolderDelegate> childFolders = new ArrayList<>();
		Collection<CmisDocumentDelegate> childDocs = new ArrayList<>();
		while (it.hasNext()) {
			CmisObject o = it.next();
			// Don't continue if the referrent object is one of this object's children
			if ((referrent != null) && Objects.equals(referrent.getId(), o.getId())) { return ret; }
			if (o instanceof Folder) {
				childFolders.add(new CmisFolderDelegate(this.factory, ctx.getSession(), Folder.class.cast(o)));
			} else if (o instanceof Document) {
				childDocs.add(new CmisDocumentDelegate(this.factory, ctx.getSession(), Document.class.cast(o)));
			}
		}

		// We're supposed to add the child objects because we're not being added by an upwards
		// recursion, so we add them
		for (CmisFolderDelegate d : childFolders) {
			ret.add(d);
		}
		for (CmisDocumentDelegate d : childDocs) {
			ret.add(d);
		}
		return ret;
	}

	@Override
	protected String calculateName(Session session, Folder folder) throws Exception {
		return folder.getName();
	}
}