package com.armedia.caliente.engine.alfresco.bulk.importer;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.commons.io.FileUtils;

import com.armedia.caliente.engine.alfresco.bulk.importer.model.AlfrescoType;
import com.armedia.caliente.engine.converter.IntermediateProperty;
import com.armedia.caliente.engine.exporter.ExportTarget;
import com.armedia.caliente.engine.importer.ImportException;
import com.armedia.caliente.store.CmfContentInfo;
import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.store.CmfProperty;
import com.armedia.caliente.store.CmfType;
import com.armedia.caliente.store.CmfValue;
import com.armedia.commons.utilities.FileNameTools;

public class AlfImportFolderDelegate extends AlfImportFileableDelegate {
	private static final String BASE_TYPE = "cm:folder";
	private static final String FOLDER_ASPECT = "arm:folder";
	private static final String[] BASE_ASPECTS = {
		AlfImportFolderDelegate.FOLDER_ASPECT, AlfImportFileableDelegate.STATUS_ASPECT
	};

	private final AlfrescoType folderType;

	public AlfImportFolderDelegate(AlfImportDelegateFactory factory, CmfObject<CmfValue> storedObject)
		throws Exception {
		super(null, factory, storedObject);
		this.folderType = this.factory.schema.buildType(AlfImportFolderDelegate.BASE_TYPE,
			AlfImportFolderDelegate.BASE_ASPECTS);
	}

	@Override
	protected AlfrescoType calculateTargetType(CmfContentInfo content) throws ImportException {
		AlfrescoType type = super.calculateTargetType(content);
		if (type == null) {
			type = this.folderType;
		}
		return type;
	}

	protected int getFolderDepth() {
		CmfProperty<CmfValue> p = this.cmfObject.getProperty(IntermediateProperty.PATH);
		if (p == null) { return 0; }
		int min = -1;
		for (CmfValue v : p) {
			List<String> l = FileNameTools.tokenize(v.asString(), '/');
			if (min < 0) {
				min = l.size();
			} else {
				min = Math.min(min, l.size());
			}
		}
		return (min < 0 ? 0 : min);
	}

	private boolean hasPropertyValues(IntermediateProperty property) {
		CmfProperty<CmfValue> p = this.cmfObject.getProperty(property);
		if (p == null) { return false; }
		if (p.isRepeating()) { return p.hasValues(); }
		CmfValue v = p.getValue();
		return ((v != null) && !v.isNull());
	}

	@Override
	protected boolean createStub(AlfImportContext ctx, File target, String content) throws ImportException {
		if (this.cmfObject.getType() == CmfType.FOLDER) {
			if ((getFolderDepth() == 0) && ctx.getContainedObjects(this.cmfObject).isEmpty()
				&& (hasPropertyValues(IntermediateProperty.GROUPS_WITH_DEFAULT_FOLDER)
					|| hasPropertyValues(IntermediateProperty.USERS_WITH_DEFAULT_FOLDER))) {
				// If the object is a top-level folder that is also a user's or group's
				// home and is also empty (i.e. no children), then we don't create a stub
				return false;
			}
			// The folder is either not empty, or not a user's or group's home, so we
			// include it to avoid problems with its children
		}
		ExportTarget referrent = this.factory.getEngine().getReferrent(this.cmfObject);
		if (referrent != null) {
			switch (referrent.getType()) {
				case FOLDER:
				case DOCUMENT:
					break;
				default:
					// If this folder is referenced by anything other than a folder
					// or another document, we're not interested in creating it.
					return false;
			}
		}

		try {
			FileUtils.forceMkdir(target);
			return true;
		} catch (IOException e) {
			throw new ImportException(String.format("Failed to create the folder for %s [%s](%s) at [%s]",
				this.cmfObject.getType(), this.cmfObject.getLabel(), this.cmfObject.getId(), target.getAbsolutePath()),
				e);
		}
	}
}