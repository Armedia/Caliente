/*******************************************************************************
 * #%L
 * Armedia Caliente
 * %%
 * Copyright (C) 2013 - 2019 Armedia, LLC
 * %%
 * This file is part of the Caliente software.
 *
 * If the software was purchased under a paid Caliente license, the terms of
 * the paid license agreement will prevail.  Otherwise, the software is
 * provided under the following open source license terms:
 *
 * Caliente is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Caliente is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Caliente. If not, see <http://www.gnu.org/licenses/>.
 * #L%
 *******************************************************************************/
package com.armedia.caliente.engine.cmis.exporter;

import java.io.InputStream;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.apache.chemistry.opencmis.client.api.Document;
import org.apache.chemistry.opencmis.client.api.Rendition;
import org.apache.chemistry.opencmis.client.api.Session;
import org.apache.chemistry.opencmis.commons.data.ContentStream;
import org.apache.chemistry.opencmis.commons.impl.IOUtils;
import org.apache.commons.io.FilenameUtils;

import com.armedia.caliente.engine.TransferSetting;
import com.armedia.caliente.engine.cmis.CmisCustomAttributes;
import com.armedia.caliente.engine.converter.IntermediateAttribute;
import com.armedia.caliente.engine.converter.IntermediateProperty;
import com.armedia.caliente.engine.exporter.ExportException;
import com.armedia.caliente.store.CmfAttribute;
import com.armedia.caliente.store.CmfAttributeTranslator;
import com.armedia.caliente.store.CmfContentStore;
import com.armedia.caliente.store.CmfContentStream;
import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.store.CmfProperty;
import com.armedia.caliente.store.CmfStorageException;
import com.armedia.caliente.store.CmfValue;
import com.armedia.caliente.store.tools.MimeTools;
import com.armedia.commons.utilities.Tools;

public class CmisDocumentDelegate extends CmisFileableDelegate<Document> {

	private final String antecedentId;
	private final List<Document> previous;
	private final List<Document> successors;

	protected CmisDocumentDelegate(CmisDocumentDelegate rootElement, Session session, Document object,
		String antecedentId) throws Exception {
		super(rootElement.factory, session, Document.class, object);
		this.previous = Collections.emptyList();
		this.successors = Collections.emptyList();
		this.antecedentId = antecedentId;
	}

	protected CmisDocumentDelegate(CmisExportDelegateFactory factory, Session session, Document object)
		throws Exception {
		super(factory, session, Document.class, object);
		List<Document> all = factory.getHistory(object).getAllVersions();
		List<Document> prev = new ArrayList<>(all.size());
		List<Document> succ = new ArrayList<>(all.size());

		Document first = all.get(0);
		if ((first.isPrivateWorkingCopy() == Boolean.TRUE) || Objects.equals("pwc", first.getVersionLabel())) {
			all.remove(0);
		}
		Collections.reverse(all);
		List<Document> tgt = prev;
		for (Document d : all) {
			if (Objects.equals(object.getId(), d.getId())) {
				tgt = succ;
				continue;
			}
			tgt.add(d);
		}
		this.previous = Tools.freezeList(prev);
		this.successors = Tools.freezeList(succ);
		this.antecedentId = (prev.isEmpty() ? null : prev.get(prev.size() - 1).getId());
	}

	@Override
	protected String calculatePath(Session session, Document d) throws Exception {
		String path = super.calculatePath(session, d);
		if ((path == null) && !d.isLatestVersion()) {
			path = calculatePath(session, this.factory.getHistory(d).lastVersion);
		}
		return path;
	}

	@Override
	protected String calculateHistoryId(Session session, Document object) throws Exception {
		return object.getVersionSeriesId();
	}

	@Override
	protected String calculateVersion(Document obj) throws Exception {
		return obj.getVersionLabel();
	}

	@Override
	protected Collection<CmisExportDelegate<?>> identifyAntecedents(CmfObject<CmfValue> marshalled,
		CmisExportContext ctx) throws Exception {
		Collection<CmisExportDelegate<?>> ret = super.identifyAntecedents(marshalled, ctx);
		String prev = null;
		for (Document d : this.previous) {
			ret.add(new CmisDocumentDelegate(this, ctx.getSession(), d, prev));
			prev = d.getId();
		}
		return ret;
	}

	@Override
	protected boolean marshal(CmisExportContext ctx, CmfObject<CmfValue> object) throws ExportException {
		if (!super.marshal(ctx, object)) { return false; }
		if (this.antecedentId != null) {
			CmfAttribute<CmfValue> antecedentId = new CmfAttribute<>(CmisCustomAttributes.VERSION_ANTECEDENT_ID.name,
				CmfValue.Type.ID, false);
			try {
				antecedentId.setValue(CmfValue.of(CmfValue.Type.ID, Object.class.cast(this.antecedentId)));
			} catch (ParseException e) {
				throw new ExportException(String.format("Failed to create an object ID value from [%s] for %s",
					this.antecedentId, object.getDescription()));
			}
			object.setAttribute(antecedentId);
		}
		object.setAttribute(new CmfAttribute<>(IntermediateAttribute.IS_LATEST_VERSION,
			IntermediateAttribute.IS_LATEST_VERSION.type, CmfValue.of(this.object.isLatestVersion())));

		Document headVersion = null;
		if (this.object.isLatestVersion() || this.successors.isEmpty()) {
			headVersion = this.object;
		} else {
			headVersion = this.successors.stream().filter(Document::isLatestVersion).findFirst().orElse(null);
			if (headVersion == null) {
				throw new ExportException(
					String.format("Failed to find the latest version for [%s]", object.getDescription()));
			}
		}
		object.setProperty(new CmfProperty<>(IntermediateProperty.HEAD_NAME, IntermediateProperty.HEAD_NAME.type,
			CmfValue.of(headVersion.getName())));
		object.setProperty(
			new CmfProperty<>(IntermediateProperty.VERSION_TREE_ROOT, IntermediateProperty.VERSION_TREE_ROOT.type,
				CmfValue.of((this.antecedentId == null) || ctx.getSettings().getBoolean(TransferSetting.LATEST_ONLY))));
		object.setProperty(new CmfProperty<>(IntermediateProperty.VERSION_INDEX,
			IntermediateProperty.VERSION_INDEX.type, CmfValue.of(this.previous.size())));
		object.setProperty(new CmfProperty<>(IntermediateProperty.VERSION_COUNT,
			IntermediateProperty.VERSION_COUNT.type, CmfValue.of(this.previous.size() + this.successors.size() + 1)));
		object.setProperty(new CmfProperty<>(IntermediateProperty.VERSION_HEAD_INDEX,
			IntermediateProperty.VERSION_HEAD_INDEX.type, CmfValue.of(this.previous.size() + this.successors.size())));
		return true;
	}

	@Override
	protected void marshalParentsAndPaths(CmisExportContext ctx, CmfObject<CmfValue> marshaled, Document object)
		throws ExportException {
		Document doc = object;
		if (!doc.isLatestVersion()) {
			doc = this.factory.getHistory(object).lastVersion;
		}
		super.marshalParentsAndPaths(ctx, marshaled, doc);
	}

	@Override
	protected List<CmfContentStream> storeContent(CmisExportContext ctx, CmfAttributeTranslator<CmfValue> translator,
		CmfObject<CmfValue> marshalled, CmfContentStore<?, ?> streamStore, boolean includeRenditions) {
		List<CmfContentStream> ret = super.storeContent(ctx, translator, marshalled, streamStore, includeRenditions);
		ContentStream main = this.object.getContentStream();
		if (main == null) { return ret; }
		CmfContentStream mainInfo = new CmfContentStream(marshalled, 0);
		mainInfo.setMimeType(MimeTools.resolveMimeType(main.getMimeType()));
		String name = main.getFileName();
		mainInfo.setFileName(name);
		mainInfo.setExtension(FilenameUtils.getExtension(name));

		final boolean ignoreContent = ctx.getSettings().getBoolean(TransferSetting.IGNORE_CONTENT);
		try {
			final long length = storeContentStream(marshalled, translator, null, main, streamStore, mainInfo,
				!ignoreContent);
			mainInfo.setLength(length);
		} catch (CmfStorageException e) {
			this.log.error("Failed to store the primary content stream for {}", marshalled.getDescription(), e);
		}
		ret.add(mainInfo);
		if (includeRenditions) {
			int i = 0;
			Collection<Rendition> renditions = this.object.getRenditions();
			if ((renditions != null) && !renditions.isEmpty()) {
				for (Rendition r : renditions) {
					CmfContentStream info = new CmfContentStream(marshalled, ++i, r.getKind());
					ContentStream cs = r.getContentStream();
					info.setMimeType(MimeTools.resolveMimeType(r.getMimeType()));
					name = cs.getFileName();
					info.setFileName(name);
					info.setExtension(FilenameUtils.getExtension(name));
					info.setProperty("kind", r.getKind());
					info.setProperty("docId", r.getRenditionDocumentId());
					info.setProperty("streamId", r.getStreamId());
					info.setProperty("title", r.getTitle());
					info.setProperty("height", String.valueOf(r.getHeight()));
					info.setProperty("width", String.valueOf(r.getWidth()));
					try {
						final long length = storeContentStream(marshalled, translator, r, cs, streamStore, info,
							!ignoreContent);
						info.setLength(length);
					} catch (CmfStorageException e) {
						this.log.error("Failed to store the {} rendition (# {}) for {}", r.getKind(), info.getIndex(),
							marshalled.getDescription(), e);
					}
					ret.add(info);
				}
			}
		}
		return ret;
	}

	protected long storeContentStream(CmfObject<CmfValue> marshalled, CmfAttributeTranslator<CmfValue> translator,
		Rendition r, ContentStream cs, CmfContentStore<?, ?> streamStore, CmfContentStream info, boolean includeContent)
		throws CmfStorageException {
		CmfContentStore<?, ?>.Handle<CmfValue> h = streamStore.addContentStream(translator, marshalled, info);
		InputStream src = cs.getStream();
		try {
			return (includeContent ? h.store(src, cs.getLength()) : cs.getLength());
		} finally {
			IOUtils.closeQuietly(src);
		}
	}

	@Override
	protected Collection<CmisExportDelegate<?>> identifySuccessors(CmfObject<CmfValue> marshalled,
		CmisExportContext ctx) throws Exception {
		Collection<CmisExportDelegate<?>> ret = super.identifySuccessors(marshalled, ctx);
		String prev = this.object.getId();
		for (Document d : this.successors) {
			ret.add(new CmisDocumentDelegate(this, ctx.getSession(), d, prev));
			prev = d.getId();
		}
		return ret;
	}

	@Override
	protected String calculateName(Session session, Document document) throws Exception {
		return document.getName();
	}

	@Override
	protected boolean calculateHistoryCurrent(Session session, Document document) throws Exception {
		return document.isLatestVersion();
	}
}