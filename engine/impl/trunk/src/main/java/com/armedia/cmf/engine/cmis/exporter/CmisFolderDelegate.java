package com.armedia.cmf.engine.cmis.exporter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.chemistry.opencmis.client.api.CmisObject;
import org.apache.chemistry.opencmis.client.api.Document;
import org.apache.chemistry.opencmis.client.api.Folder;

import com.armedia.cmf.engine.cmis.CmisPagingIterator;
import com.armedia.cmf.engine.exporter.ExportException;
import com.armedia.cmf.engine.exporter.ExportTarget;
import com.armedia.cmf.storage.CmfObject;
import com.armedia.cmf.storage.CmfValue;
import com.armedia.commons.utilities.Tools;

public class CmisFolderDelegate extends CmisFileableDelegate<Folder> {

	protected CmisFolderDelegate(CmisExportDelegateFactory factory, Folder folder) throws Exception {
		super(factory, Folder.class, folder);
	}

	@Override
	protected boolean marshal(CmisExportContext ctx, CmfObject<CmfValue> object) throws ExportException {
		return super.marshal(ctx, object);
	}

	protected int calculateDepth(Folder f, final Set<String> visited) throws Exception {
		if (f == null) { throw new IllegalArgumentException("Must provide a folder whose depth to calculate"); }
		if (!visited.add(f.getId())) { throw new IllegalStateException(String.format(
			"Folder [%s] was visited twice - visited set: %s", f.getId(), visited)); }
		try {
			if (f.isRootFolder()) { return 0; }
			List<Folder> parents = f.getParents();
			int maxDepth = -1;
			for (Folder p : parents) {
				maxDepth = Math.max(maxDepth, calculateDepth(p, visited));
			}
			return maxDepth + 1;
		} finally {
			visited.remove(f.getId());
		}
	}

	@Override
	protected String calculateBatchId(Folder object) throws Exception {
		return String.format("%016x", calculateDepth(object, new LinkedHashSet<String>()));
	}

	@Override
	protected Collection<CmisExportDelegate<?>> identifyDependents(CmfObject<CmfValue> marshalled, CmisExportContext ctx)
		throws Exception {
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
				childFolders.add(new CmisFolderDelegate(this.factory, Folder.class.cast(o)));
			} else if (o instanceof Document) {
				childDocs.add(new CmisDocumentDelegate(this.factory, Document.class.cast(o)));
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