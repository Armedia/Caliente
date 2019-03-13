package com.armedia.caliente.engine.cmis.importer;

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
import org.apache.commons.text.StringTokenizer;

import com.armedia.caliente.engine.converter.IntermediateProperty;
import com.armedia.caliente.engine.importer.ImportException;
import com.armedia.caliente.store.CmfAttribute;
import com.armedia.caliente.store.CmfAttributeTranslator;
import com.armedia.caliente.store.CmfContentStore;
import com.armedia.caliente.store.CmfContentStream;
import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.store.CmfProperty;
import com.armedia.caliente.store.CmfStorageException;
import com.armedia.caliente.store.CmfValue;
import com.armedia.caliente.store.CmfValueMapper.Mapping;
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
			String[] arr = new StringTokenizer(this.versionLabel, '.').getTokenArray();
			last = (arr != null ? Integer.valueOf(arr[arr.length - 1]) : 0);
		}
		this.major = (last == 0);
	}

	protected ContentStream getContentStream(CmisImportContext ctx) throws ImportException {
		CmfContentStore<?, ?> store = ctx.getContentStore();
		List<CmfContentStream> info = null;
		try {
			info = ctx.getContentStreams(this.cmfObject);
		} catch (Exception e) {
			throw new ImportException(
				String.format("Failed to retrieve the content info for %s", this.cmfObject.getDescription()), e);
		}
		if ((info == null) || info.isEmpty()) { return null; }
		CmfContentStream content = info.get(0);
		CmfContentStore<?, ?>.Handle h = store.getHandle(this.factory.getEngine().getTranslator(), this.cmfObject,
			content);

		String fileName = content.getFileName();
		// String size = content.getProperty(ContentProperty.SIZE);
		String mimeType = content.getMimeType().toString();

		try {
			return new ContentStreamImpl(fileName, BigInteger.valueOf(h.getStreamSize()), mimeType, h.openInput());
		} catch (CmfStorageException e) {
			throw new ImportException(
				String.format("Failed to access the [%s] content for %s", h.getInfo(), this.cmfObject.getDescription()),
				e);
		}
	}

	@Override
	protected Map<String, Object> prepareProperties(CmfAttributeTranslator<CmfValue> translator, CmisImportContext ctx)
		throws ImportException {
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
					this.log.warn("Failed to cancel the checkout for [{}], checked out from [{}] (for {})",
						newVersion.getId(), existing.getId(), this.cmfObject.getDescription(), e);
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
				CmfProperty<CmfValue> rootProp = this.cmfObject.getProperty(IntermediateProperty.VERSION_TREE_ROOT);
				if ((rootProp == null) || !rootProp.hasValues() || !rootProp.getValue().asBoolean()) {
					Mapping m = ctx.getValueMapper().getTargetMapping(this.cmfObject.getType(),
						PropertyIds.VERSION_SERIES_ID, vsi.asString());
					if (m != null) {
						String seriesId = m.getTargetValue();
						try {
							CmisObject obj = ctx.getSession().getObject(seriesId);
							if (obj instanceof Document) {
								Document doc = Document.class.cast(obj);
								for (Document d : doc.getAllVersions()) {
									Boolean pwc = d.isPrivateWorkingCopy();
									if ((pwc != null) && pwc.booleanValue()) {
										throw new ImportException(String.format(
											"The document is already checked out %s", this.cmfObject.getDescription()));
									}
									Boolean lv = d.isLatestVersion();
									if ((lv != null) && lv.booleanValue()) { return d; }
								}
								throw new ImportException(String.format("Failed to locate the latest version for %s",
									this.cmfObject.getDescription()));
							}
							// If the object isn't a document, we have a problem
							throw new ImportException(
								String.format("Root object for version series [%s] is not a document for %s", seriesId,
									this.cmfObject.getDescription()));
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
			Document document = parent.createDocument(properties, content, state);
			CmfAttribute<CmfValue> versionSeriesId = this.cmfObject.getAttribute(PropertyIds.VERSION_SERIES_ID);
			if ((versionSeriesId != null) && versionSeriesId.hasValues()) {
				ctx.getValueMapper().setMapping(this.cmfObject.getType(), PropertyIds.VERSION_SERIES_ID,
					versionSeriesId.getValue().asString(), document.getVersionSeriesId());
			}
			return document;
		} finally {
			IOUtils.closeQuietly(content);
		}
	}

	@Override
	protected boolean isMultifilable(Document existing) {
		return true;
	}
}