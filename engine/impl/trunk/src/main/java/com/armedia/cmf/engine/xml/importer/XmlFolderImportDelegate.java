package com.armedia.cmf.engine.xml.importer;

import com.armedia.cmf.engine.importer.ImportException;
import com.armedia.cmf.engine.xml.importer.jaxb.FolderT;
import com.armedia.cmf.engine.xml.importer.jaxb.FoldersT;
import com.armedia.cmf.storage.CmfAttributeTranslator;
import com.armedia.cmf.storage.CmfObject;
import com.armedia.cmf.storage.CmfStorageException;
import com.armedia.cmf.storage.CmfValue;
import com.armedia.cmf.storage.CmfValueDecoderException;

public class XmlFolderImportDelegate extends XmlAggregatedImportDelegate<FolderT, FoldersT> {

	protected XmlFolderImportDelegate(XmlImportDelegateFactory factory, CmfObject<CmfValue> storedObject)
		throws Exception {
		super(factory, storedObject, FoldersT.class);
	}

	@Override
	protected FolderT createItem(CmfAttributeTranslator<CmfValue> translator, XmlImportContext ctx)
		throws ImportException, CmfStorageException, CmfValueDecoderException {
		FolderT f = new FolderT();

		f.setId(this.cmfObject.getId());
		f.setAcl(null);
		f.setCreationDate(null);
		f.setCreator(null);
		f.setLastAccessDate(null);
		f.setLastAccessor(null);
		f.setModificationDate(null);
		f.setModifier(null);
		f.setName(null);
		f.setParentId(null);
		f.setSourcePath(null);
		f.setType(null);

		return f;
	}
}