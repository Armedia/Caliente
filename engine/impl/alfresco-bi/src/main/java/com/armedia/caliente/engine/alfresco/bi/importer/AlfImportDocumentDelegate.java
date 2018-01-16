package com.armedia.caliente.engine.alfresco.bi.importer;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;

import com.armedia.caliente.engine.alfresco.bi.importer.model.AlfrescoType;
import com.armedia.caliente.engine.importer.ImportException;
import com.armedia.caliente.store.CmfContentStream;
import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.store.CmfValue;

public class AlfImportDocumentDelegate extends AlfImportFileableDelegate {

	private static final String BASE_TYPE = "arm:document";
	private static final String RENDITION_TYPE = "arm:rendition";

	private final AlfrescoType renditionType;

	public AlfImportDocumentDelegate(AlfImportDelegateFactory factory, CmfObject<CmfValue> storedObject)
		throws Exception {
		super(AlfImportDocumentDelegate.BASE_TYPE, factory, storedObject);
		this.renditionType = factory.getType(AlfImportDocumentDelegate.RENDITION_TYPE);
	}

	@Override
	protected AlfrescoType calculateTargetType(CmfContentStream content) throws ImportException {
		if (!content.isDefaultRendition() || (content.getRenditionPage() > 0)) {
			// If this is a rendition or rendition extra page...
			return this.renditionType;
		}
		return super.calculateTargetType(content);
	}

	@Override
	protected boolean createStub(AlfImportContext ctx, File target, String content) throws ImportException {
		try {
			FileUtils.write(target, content, AlfImportFileableDelegate.DEFAULT_CHARSET);
			return true;
		} catch (IOException e) {
			throw new ImportException(String.format("Failed to create the stub file for %s at [%s] with contents [%s]",
				this.cmfObject.getDescription(), target.getAbsolutePath(), content), e);
		}
	}
}