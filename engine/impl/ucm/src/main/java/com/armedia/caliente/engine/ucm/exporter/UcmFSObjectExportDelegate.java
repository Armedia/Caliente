package com.armedia.caliente.engine.ucm.exporter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import com.armedia.caliente.engine.converter.IntermediateProperty;
import com.armedia.caliente.engine.exporter.ExportException;
import com.armedia.caliente.engine.ucm.model.UcmException;
import com.armedia.caliente.engine.ucm.model.UcmFSObject;
import com.armedia.caliente.engine.ucm.model.UcmFolder;
import com.armedia.caliente.store.CmfAttribute;
import com.armedia.caliente.store.CmfDataType;
import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.store.CmfProperty;
import com.armedia.caliente.store.CmfType;
import com.armedia.caliente.store.CmfValue;
import com.armedia.commons.utilities.FileNameTools;

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
		UcmFolder parent = this.object.getParentFolder(ctx.getSession());
		if (parent != null) {
			requirements.add(new UcmFolderExportDelegate(this.factory, parent));
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
		for (CmfProperty<CmfValue> property : properties) {
			object.setProperty(property);
		}
		return true;
	}

	protected boolean getDataProperties(UcmExportContext ctx, Collection<CmfProperty<CmfValue>> properties, T object)
		throws ExportException {
		CmfProperty<CmfValue> paths = new CmfProperty<>(IntermediateProperty.PATH, CmfDataType.STRING, false);
		properties.add(paths);
		paths.setValue(new CmfValue(object.getParentPath()));

		CmfProperty<CmfValue> parents = new CmfProperty<>(IntermediateProperty.PARENT_ID, CmfDataType.ID, false);
		paths.setValue(new CmfValue(object.getParentURI().toString()));
		properties.add(parents);

		CmfProperty<CmfValue> idtree = new CmfProperty<>(IntermediateProperty.PARENT_TREE_IDS, CmfDataType.STRING,
			false);
		properties.add(idtree);
		LinkedList<String> l = new LinkedList<>();
		UcmFolder parent = null;
		while (true) {
			try {
				parent = (parent == null ? object : parent).getParentFolder(ctx.getSession());
				if (parent == null) {
					// If this object has no parent, then there's no parent IDs to generate
					break;
				}
			} catch (UcmException e) {
				throw new ExportException(e.getMessage(), e);
			}
			l.addFirst(parent.getURI().getSchemeSpecificPart());
		}
		idtree.setValue(new CmfValue(FileNameTools.reconstitute(l, false, false)));
		return true;
	}
}