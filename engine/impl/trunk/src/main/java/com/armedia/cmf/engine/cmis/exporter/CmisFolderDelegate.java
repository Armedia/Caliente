package com.armedia.cmf.engine.cmis.exporter;

import java.util.ArrayList;
import java.util.Collection;

import org.apache.chemistry.opencmis.client.api.CmisObject;
import org.apache.chemistry.opencmis.client.api.Document;
import org.apache.chemistry.opencmis.client.api.Folder;

import com.armedia.cmf.engine.cmis.CmisPagingIterator;
import com.armedia.cmf.engine.exporter.ExportException;
import com.armedia.cmf.engine.exporter.ExportTarget;
import com.armedia.cmf.storage.StoredObject;
import com.armedia.cmf.storage.StoredValue;
import com.armedia.commons.utilities.Tools;

public class CmisFolderDelegate extends CmisFileableDelegate<Folder> {

	protected CmisFolderDelegate(CmisExportEngine engine, Folder folder) throws Exception {
		super(engine, Folder.class, folder);
	}

	@Override
	protected void marshal(CmisExportContext ctx, StoredObject<StoredValue> object) throws ExportException {
		super.marshal(ctx, object);
	}

	@Override
	protected Collection<CmisExportDelegate<?>> identifyDependents(StoredObject<StoredValue> marshalled,
		CmisExportContext ctx) throws Exception {
		Collection<CmisExportDelegate<?>> ret = super.identifyDependents(marshalled, ctx);
		// We will only include the folder's contents if the referencing object is NOT one of our
		// children
		ExportTarget referrent = ctx.getReferrent();
		CmisPagingIterator<CmisObject> it = new CmisPagingIterator<CmisObject>(this.object.getChildren());
		Collection<CmisFolderDelegate> childFolders = new ArrayList<CmisFolderDelegate>();
		Collection<CmisDocumentDelegate> childDocs = new ArrayList<CmisDocumentDelegate>();
		while (it.hasNext()) {
			CmisObject o = it.next();
			// Don't continue if the referrent object is one of this object's children
			if ((referrent != null) && Tools.equals(referrent.getId(), o.getId())) { return ret; }
			if (o instanceof Folder) {
				childFolders.add(new CmisFolderDelegate(this.engine, Folder.class.cast(o)));
			} else if (o instanceof Document) {
				childDocs.add(new CmisDocumentDelegate(this.engine, Document.class.cast(o)));
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
}