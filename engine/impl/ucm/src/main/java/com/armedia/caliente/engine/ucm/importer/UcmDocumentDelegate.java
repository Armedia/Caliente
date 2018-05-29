package com.armedia.caliente.engine.ucm.importer;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;

import org.apache.chemistry.opencmis.commons.data.ContentStream;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.ContentStreamImpl;

import com.armedia.caliente.engine.importer.ImportException;
import com.armedia.caliente.engine.ucm.model.UcmFile;
import com.armedia.caliente.engine.ucm.model.UcmFolder;
import com.armedia.caliente.store.CmfContentStream;
import com.armedia.caliente.store.CmfContentStore;
import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.store.CmfStorageException;
import com.armedia.caliente.store.CmfValue;

public class UcmDocumentDelegate extends UcmFSObjectDelegate<UcmFile> {

	public UcmDocumentDelegate(UcmImportDelegateFactory factory, CmfObject<CmfValue> storedObject) throws Exception {
		super(factory, UcmFile.class, storedObject);
	}

	protected ContentStream getContentStream(UcmImportContext ctx) throws ImportException {
		CmfContentStore<?, ?, ?> store = ctx.getContentStore();
		List<CmfContentStream> info = null;
		try {
			info = ctx.getContentStreams(this.cmfObject);
		} catch (Exception e) {
			throw new ImportException(String.format("Failed to retrieve the content info for DOCUMENT [%s](%s)",
				this.cmfObject.getLabel(), this.cmfObject.getId()), e);
		}
		if ((info == null) || info.isEmpty()) { return null; }
		CmfContentStream content = info.get(0);
		CmfContentStore<?, ?, ?>.Handle h = store.getHandle(this.factory.getEngine().getTranslator(), this.cmfObject,
			content);

		String fileName = content.getFileName();
		// String size = content.getProperty(ContentProperty.SIZE);
		String mimeType = content.getMimeType().toString();

		try {
			return new ContentStreamImpl(fileName, BigInteger.valueOf(h.getStreamSize()), mimeType, h.openInput());
		} catch (CmfStorageException e) {
			throw new ImportException(String.format("Failed to access the [%s] content for DOCUMENT [%s](%s)",
				h.getInfo(), this.cmfObject.getLabel(), this.cmfObject.getId()), e);
		}
	}

	@Override
	protected boolean isVersionable(UcmFile existing) {
		return true;
	}

	@Override
	protected boolean isSameObject(UcmFile existing) {
		if (!super.isSameObject(existing)) { return false; }
		/*
		return Tools.equals(existing.getVersionLabel(), this.versionLabel);
		*/
		return true;
	}

	@Override
	protected UcmFile createNew(UcmImportContext ctx, UcmFolder parent, Map<String, Object> properties)
		throws ImportException {
		/*
		ContentStream content = getContentStream(ctx);
		try {
			VersioningState state = (this.major ? VersioningState.MAJOR : VersioningState.MINOR);
			Document document = parent.createDocument(properties, content, state);
			CmfAttribute<CmfValue> versionSeriesId = this.cmfObject.getAttribute(PropertyIds.VERSION_SERIES_ID);
			if ((versionSeriesId != null) && versionSeriesId.hasValues()) {
				ctx.getAttributeMapper().setMapping(this.cmfObject.getType(), PropertyIds.VERSION_SERIES_ID,
					versionSeriesId.getValue().asString(), document.getVersionSeriesId());
			}
			return document;
		} finally {
			IOUtils.closeQuietly(content);
		}
		*/
		return null;
	}

	@Override
	protected boolean isMultifilable(UcmFile existing) {
		return true;
	}
}