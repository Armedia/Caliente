package com.armedia.cmf.engine.cmis.exporter;

import org.apache.chemistry.opencmis.client.api.Folder;

import com.armedia.cmf.engine.exporter.ExportException;
import com.armedia.cmf.storage.StoredObject;
import com.armedia.cmf.storage.StoredValue;

public class CmisFolderDelegate extends CmisFileableDelegate<Folder> {

	protected CmisFolderDelegate(CmisExportEngine engine, Folder folder) throws Exception {
		super(engine, Folder.class, folder);
	}

	@Override
	protected void marshal(CmisExportContext ctx, StoredObject<StoredValue> object) throws ExportException {
	}
}