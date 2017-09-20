package com.armedia.caliente.engine.ucm.exporter;

import java.util.Collection;

import com.armedia.caliente.engine.exporter.ExportException;
import com.armedia.caliente.engine.ucm.model.UcmFSObject;
import com.armedia.caliente.engine.ucm.model.UcmFolder;
import com.armedia.caliente.engine.ucm.model.UcmFolderNotFoundException;
import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.store.CmfType;
import com.armedia.caliente.store.CmfValue;

public abstract class UcmFSObjectExportDelegate<T extends UcmFSObject> extends UcmExportDelegate<T> {

	protected UcmFSObjectExportDelegate(UcmExportDelegateFactory factory, Class<T> objectClass, T object)
		throws Exception {
		super(factory, objectClass, object);
	}

	@Override
	protected final CmfType calculateType(T object) throws Exception {
		return object.getType().cmfType;
	}

	@Override
	protected String calculateLabel(T object) throws Exception {
		return object.getPath();
	}

	@Override
	protected final String calculateObjectId(T object) throws Exception {
		return object.getUniqueURI().toString();
	}

	@Override
	protected final String calculateSearchKey(T object) throws Exception {
		return object.getUniqueURI().toString();
	}

	@Override
	protected final String calculateName(T object) throws Exception {
		return object.getName();
	}

	@Override
	protected Collection<UcmExportDelegate<?>> identifyRequirements(CmfObject<CmfValue> marshalled,
		UcmExportContext ctx) throws Exception {
		Collection<UcmExportDelegate<?>> requirements = super.identifyRequirements(marshalled, ctx);

		// First things first - add the parent folder
		try {
			UcmFolder parent = this.object.getParentFolder(ctx.getSession());
			requirements.add(new UcmFolderExportDelegate(this.factory, parent));
		} catch (final UcmFolderNotFoundException e) {
			switch (this.object.getType()) {
				case FOLDER:
					UcmFolder folder = UcmFolder.class.cast(this.object);
					if (!folder.isRoot()) { throw e; }
					// This is the root folder, so this isn't a problem...
					break;
				case FILE:
				default:
					throw e;
			}
		}

		if (this.object.isShortcut()) {
			final String targetGuid = this.object.getTargetGUID();
			switch (this.object.getType()) {
				case FILE:
					requirements
						.add(new UcmFileExportDelegate(this.factory, ctx.getSession().getFileByGUID(targetGuid)));
					break;

				case FOLDER:
					requirements
						.add(new UcmFolderExportDelegate(this.factory, ctx.getSession().getFolderByGUID(targetGuid)));
					break;
				default:
					break;
			}
		}
		return requirements;
	}

	@Override
	protected boolean marshal(UcmExportContext ctx, CmfObject<CmfValue> object) throws ExportException {
		// TODO Auto-generated method stub
		return false;
	}
}