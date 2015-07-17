package com.armedia.cmf.engine.cmis.importer;

import java.io.IOException;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;

import org.apache.chemistry.opencmis.client.api.CmisObject;
import org.apache.chemistry.opencmis.client.api.Document;
import org.apache.chemistry.opencmis.client.api.Folder;
import org.apache.chemistry.opencmis.client.api.ObjectId;
import org.apache.chemistry.opencmis.commons.PropertyIds;
import org.apache.chemistry.opencmis.commons.data.ContentStream;
import org.apache.chemistry.opencmis.commons.enums.VersioningState;
import org.apache.chemistry.opencmis.commons.exceptions.CmisObjectNotFoundException;
import org.apache.chemistry.opencmis.commons.impl.IOUtils;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.ContentStreamImpl;
import org.apache.commons.lang3.text.StrTokenizer;

import com.armedia.cmf.engine.converter.ContentProperty;
import com.armedia.cmf.engine.importer.ImportException;
import com.armedia.cmf.storage.CmfAttribute;
import com.armedia.cmf.storage.CmfAttributeMapper.Mapping;
import com.armedia.cmf.storage.CmfAttributeTranslator;
import com.armedia.cmf.storage.CmfContentInfo;
import com.armedia.cmf.storage.CmfContentStore;
import com.armedia.cmf.storage.CmfObject;
import com.armedia.cmf.storage.CmfType;
import com.armedia.cmf.storage.CmfValue;
import com.armedia.cmf.storage.CmfValueDecoderException;
import com.armedia.commons.utilities.Tools;

public class CmisDocumentDelegate extends CmisFileableDelegate<Document> {

	private final boolean major;
	private final String versionLabel;

	public CmisDocumentDelegate(CmisImportDelegateFactory factory, CmfObject<CmfValue> storedObject) throws Exception {
		super(factory, Document.class, storedObject);
		CmfAttribute<CmfValue> att = this.cmfObject.getAttribute(PropertyIds.VERSION_LABEL);
		CmfValue v = null;
		if ((att != null) && att.hasValues()) {
			v = att.getValue();
		}
		this.versionLabel = ((v != null) && !v.isNull() ? v.asString() : null);

		int last = 0;
		if (this.versionLabel != null) {
			String[] arr = new StrTokenizer(this.versionLabel, '.').getTokenArray();
			last = (arr != null ? Integer.valueOf(arr[arr.length - 1]) : 0);
		}
		this.major = (last == 0);
	}

	protected ContentStream getContentStream(CmisImportContext ctx) throws ImportException {
		CmfContentStore<?> store = ctx.getContentStore();
		List<CmfContentInfo> info = null;
		try {
			info = ctx.getContentInfo(this.cmfObject);
		} catch (Exception e) {
			throw new ImportException(String.format("Failed to retrieve the content info for DOCUMENT [%s](%s)",
				this.cmfObject.getLabel(), this.cmfObject.getId()), e);
		}
		if ((info == null) || info.isEmpty()) { return null; }
		CmfContentInfo content = info.get(0);
		CmfContentStore<?>.Handle h = store.getHandle(this.factory.getEngine().getTranslator(), this.cmfObject,
			content.getQualifier());

		String fileName = content.getProperty(ContentProperty.FILE_NAME);
		// String size = content.getProperty(ContentProperty.SIZE);
		String mimeType = content.getProperty(ContentProperty.MIME_TYPE);

		try {
			return new ContentStreamImpl(fileName, BigInteger.valueOf(h.getStreamSize()), mimeType, h.openInput());
		} catch (IOException e) {
			throw new ImportException(String.format("Failed to access the [%s] content for DOCUMENT [%s](%s)",
				h.getQualifier(), this.cmfObject.getLabel(), this.cmfObject.getId()), e);
		}
	}

	@Override
	protected Map<String, Object> prepareProperties(CmfAttributeTranslator<CmfValue> translator, CmisImportContext ctx)
		throws ImportException, CmfValueDecoderException {
		Map<String, Object> properties = super.prepareProperties(translator, ctx);
		properties.put(PropertyIds.IS_LATEST_VERSION, true);
		return properties;
	}

	@Override
	protected Document createNewVersion(CmisImportContext ctx, Document existing, Map<String, Object> properties)
		throws ImportException {
		ObjectId checkOutId = existing.checkOut();
		Document newVersion = null;
		ContentStream content = null;
		properties.remove(PropertyIds.NAME);
		try {
			newVersion = Document.class.cast(ctx.getSession().getObject(checkOutId));

			String checkinComment = Tools.toString(properties.get(PropertyIds.CHECKIN_COMMENT));
			content = getContentStream(ctx);
			ObjectId newId = newVersion.checkIn(this.major, properties, content, checkinComment);
			Document finalVersion = Document.class.cast(ctx.getSession().getObject(newId));
			newVersion = null;
			return finalVersion;
		} finally {
			// properties.put(PropertyIds.NAME, name);
			IOUtils.closeQuietly(content);
			if (newVersion != null) {
				try {
					newVersion.cancelCheckOut();
				} catch (Exception e) {
					this.log.warn(String.format(
						"Failed to cancel the checkout for [%s], checked out from [%s] (for object [%s](%s))",
						newVersion.getId(), existing.getId(), this.cmfObject.getLabel(), this.cmfObject.getId()), e);
				}
			}
		}
	}

	@Override
	protected Document findExisting(CmisImportContext ctx, List<Folder> parents) throws ImportException {
		CmfAttribute<CmfValue> versionSeriesId = this.cmfObject.getAttribute(PropertyIds.VERSION_SERIES_ID);
		if ((versionSeriesId != null) && versionSeriesId.hasValues()) {
			CmfValue vsi = versionSeriesId.getValue();
			if (!vsi.isNull()) {
				if (!Tools.equals(this.cmfObject.getId(), vsi.asString())) {
					Mapping m = ctx.getAttributeMapper().getTargetMapping(CmfType.DOCUMENT, PropertyIds.OBJECT_ID,
						vsi.asString());
					if (m != null) {
						String seriesId = m.getTargetValue();
						try {
							CmisObject obj = ctx.getSession().getObject(seriesId);
							if (obj instanceof Document) {
								Document doc = Document.class.cast(obj);
								for (Document d : doc.getAllVersions()) {
									Boolean pwc = d.isPrivateWorkingCopy();
									if ((pwc != null) && pwc.booleanValue()) { throw new ImportException(String.format(
										"The document is already checked out [%s](%s)", this.cmfObject.getLabel(),
										this.cmfObject.getId())); }
									Boolean lv = d.isLatestVersion();
									if ((lv != null) && lv.booleanValue()) { return d; }
								}
								throw new ImportException(String.format(
									"Failed to locate the latest version for [%s](%s)", this.cmfObject.getLabel(),
									this.cmfObject.getId()));
							}
							// If the object isn't a document, we have a problem
							throw new ImportException(String.format(
								"Root object for version series [%s] is not a document for %s [%s](%s)", seriesId,
								this.cmfObject.getType(), this.cmfObject.getLabel(), this.cmfObject.getId()));
						} catch (CmisObjectNotFoundException e) {
							// Do nothing...
						}
					}
				}
			}
		}
		return super.findExisting(ctx, parents);
	}

	@Override
	protected boolean isVersionable(Document existing) {
		return true;
	}

	@Override
	protected boolean isSameObject(Document existing) {
		if (!super.isSameObject(existing)) { return false; }
		return Tools.equals(existing.getVersionLabel(), this.versionLabel);
	}

	@Override
	protected Document createNew(CmisImportContext ctx, Folder parent, Map<String, Object> properties)
		throws ImportException {
		ContentStream content = getContentStream(ctx);
		try {
			VersioningState state = (this.major ? VersioningState.MAJOR : VersioningState.MINOR);
			return parent.createDocument(properties, content, state);
		} finally {
			IOUtils.closeQuietly(content);
		}
	}

	@Override
	protected boolean isMultifilable(Document existing) {
		return true;
	}
}