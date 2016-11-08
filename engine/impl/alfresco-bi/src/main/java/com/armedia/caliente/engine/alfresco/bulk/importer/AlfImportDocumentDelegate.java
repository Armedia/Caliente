package com.armedia.caliente.engine.alfresco.bulk.importer;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;

import org.apache.commons.io.FileUtils;

import com.armedia.caliente.engine.alfresco.bulk.importer.model.AlfrescoType;
import com.armedia.caliente.engine.importer.ImportException;
import com.armedia.caliente.store.CmfContentInfo;
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
	protected AlfrescoType calculateTargetType(CmfContentInfo content) throws ImportException {
		if (!content.isDefaultRendition() || (content.getRenditionPage() > 0)) {
			// If this is a rendition or rendition extra page...
			return this.renditionType;
		}
		return super.calculateTargetType(content);
	}

	@Override
	protected boolean createStub(File target, String content) throws ImportException {
		try {
			FileUtils.write(target, content, Charset.defaultCharset());
			return true;
		} catch (IOException e) {
			throw new ImportException(String.format(
				"Failed to create the stub file for %s [%s](%s) at [%s] with contents [%s]", this.cmfObject.getType(),
				this.cmfObject.getLabel(), this.cmfObject.getId(), target.getAbsolutePath(), content), e);
		}
	}
}