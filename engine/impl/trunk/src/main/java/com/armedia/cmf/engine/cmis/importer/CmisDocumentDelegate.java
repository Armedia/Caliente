package com.armedia.cmf.engine.cmis.importer;

import java.io.IOException;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;

import org.apache.chemistry.opencmis.client.api.Document;
import org.apache.chemistry.opencmis.client.api.Folder;
import org.apache.chemistry.opencmis.client.api.ObjectId;
import org.apache.chemistry.opencmis.commons.PropertyIds;
import org.apache.chemistry.opencmis.commons.data.ContentStream;
import org.apache.chemistry.opencmis.commons.enums.VersioningState;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.ContentStreamImpl;

import com.armedia.cmf.engine.ContentInfo;
import com.armedia.cmf.engine.importer.ImportException;
import com.armedia.cmf.storage.CmfAttribute;
import com.armedia.cmf.storage.CmfContentStore;
import com.armedia.cmf.storage.CmfObject;
import com.armedia.cmf.storage.CmfValue;
import com.armedia.commons.utilities.Tools;

public class CmisDocumentDelegate extends CmisFileableDelegate<Document> {

	public CmisDocumentDelegate(CmisImportDelegateFactory factory, CmfObject<CmfValue> storedObject) throws Exception {
		super(factory, Document.class, storedObject);
	}

	protected ContentStream getContentsStream(CmisImportContext ctx) throws ImportException {
		CmfContentStore<?> store = ctx.getContentStore();
		List<ContentInfo> info = null;
		try {
			info = ctx.getContentInfo(this.cmfObject);
		} catch (Exception e) {
			throw new ImportException(String.format("Failed to retrieve the content info for DOCUMENT [%s](%s)",
				this.cmfObject.getLabel(), this.cmfObject.getId()), e);
		}
		if ((info == null) || info.isEmpty()) { return null; }
		ContentInfo content = info.get(0);
		CmfContentStore<?>.Handle h = store.getHandle(this.factory.getEngine().getTranslator(), this.cmfObject,
			content.getQualifier());
		String fileName = this.cmfObject.getAttribute(PropertyIds.NAME).getValue().asString();

		// MimeType... how to detect?
		String mimeType = null;

		try {
			return new ContentStreamImpl(fileName, BigInteger.valueOf(h.getStreamSize()), mimeType, h.openInput());
		} catch (IOException e) {
			throw new ImportException(String.format("Failed to access the [%s] content for DOCUMENT [%s](%s)",
				h.getQualifier(), this.cmfObject.getLabel(), this.cmfObject.getId()), e);
		}
	}

	@Override
	protected Document createNewVersion(CmisImportContext ctx, Document existing, Map<String, ?> properties)
		throws ImportException {
		ObjectId checkOutId = existing.checkOut();
		try {
			Document newVersion = Document.class.cast(ctx.getSession().getObject(checkOutId));

			// TODO: Handle the versioning state...decide if major or minor checkin
			boolean majorVersion = false;

			String checkinComment = Tools.toString(properties.get(PropertyIds.CHECKIN_COMMENT));
			ObjectId newId = newVersion.checkIn(majorVersion, properties, getContentsStream(ctx), checkinComment);
			newVersion = Document.class.cast(ctx.getSession().getObject(newId));
			checkOutId = null;
			return newVersion;
		} finally {
			if (checkOutId != null) {
				existing.cancelCheckOut();
			}
		}
	}

	@Override
	protected boolean isVersionable(Document existing) {
		return true;
	}

	@Override
	protected boolean isSameObject(Document existing) {
		if (!super.isSameObject(existing)) { return false; }
		CmfAttribute<CmfValue> att = this.cmfObject.getAttribute(PropertyIds.VERSION_LABEL);
		if ((att == null) || !att.hasValues()) { return false; }
		CmfValue v = att.getValue();
		if ((v == null) || v.isNull()) { return false; }
		return Tools.equals(existing.getVersionLabel(), v.asString());
	}

	@Override
	protected Document createNew(CmisImportContext ctx, Folder parent, Map<String, ?> properties)
		throws ImportException {
		return parent.createDocument(properties, getContentsStream(ctx), VersioningState.NONE);
	}
}