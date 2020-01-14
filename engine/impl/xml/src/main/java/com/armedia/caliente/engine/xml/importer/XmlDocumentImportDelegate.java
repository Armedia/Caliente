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
package com.armedia.caliente.engine.xml.importer;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.Collection;
import java.util.Collections;
import java.util.GregorianCalendar;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;

import org.apache.commons.lang3.StringUtils;

import com.armedia.caliente.engine.converter.IntermediateAttribute;
import com.armedia.caliente.engine.converter.IntermediateProperty;
import com.armedia.caliente.engine.importer.ImportException;
import com.armedia.caliente.engine.importer.ImportOutcome;
import com.armedia.caliente.engine.importer.ImportResult;
import com.armedia.caliente.engine.xml.importer.jaxb.ContentStreamT;
import com.armedia.caliente.engine.xml.importer.jaxb.DocumentVersionT;
import com.armedia.caliente.store.CmfAttributeTranslator;
import com.armedia.caliente.store.CmfContentStore;
import com.armedia.caliente.store.CmfContentStream;
import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.store.CmfStorageException;
import com.armedia.caliente.store.CmfValue;
import com.armedia.caliente.store.tools.MimeTools;

public class XmlDocumentImportDelegate extends XmlImportDelegate {

	protected XmlDocumentImportDelegate(XmlImportDelegateFactory factory, CmfObject<CmfValue> storedObject)
		throws Exception {
		super(factory, storedObject);
	}

	protected DocumentVersionT createVersion(CmfAttributeTranslator<CmfValue> translator, XmlImportContext ctx)
		throws ImportException, CmfStorageException {
		DocumentVersionT v = new DocumentVersionT();
		DatatypeFactory dtf;
		try {
			dtf = DatatypeFactory.newInstance();
		} catch (DatatypeConfigurationException e) {
			throw new ImportException(e);
		}
		GregorianCalendar gcal = new GregorianCalendar();

		v.setId(this.cmfObject.getId());
		v.setAcl(getPropertyValue(IntermediateProperty.ACL_ID).asString());

		try {
			gcal.setTime(getAttributeValue(IntermediateAttribute.CREATION_DATE).asTime());
			gcal.setTimeZone(XmlImportDelegate.TZUTC);
			v.setCreationDate(dtf.newXMLGregorianCalendar(gcal));
			v.setCreator(getAttributeValue(IntermediateAttribute.CREATED_BY).asString());

			gcal.setTime(getAttributeValue(IntermediateAttribute.LAST_MODIFICATION_DATE).asTime());
			gcal.setTimeZone(XmlImportDelegate.TZUTC);
			v.setModificationDate(dtf.newXMLGregorianCalendar(gcal));
			v.setModifier(getAttributeValue(IntermediateAttribute.LAST_MODIFIED_BY).asString());

			CmfValue lad = getAttributeValue(IntermediateAttribute.LAST_ACCESS_DATE);
			if (!lad.isNull()) {
				gcal.setTime(lad.asTime());
				v.setLastAccessDate(dtf.newXMLGregorianCalendar(gcal));
			}
			v.setLastAccessor(getAttributeValue(IntermediateAttribute.LAST_ACCESSED_BY).asString());
		} catch (ParseException e) {
			throw new ImportException("Failed to parse a date value", e);
		}

		v.setName(getAttributeValue(IntermediateAttribute.NAME).asString());
		v.setParentId(getAttributeValue(IntermediateAttribute.PARENT_ID).asString());
		String path = getPropertyValue(IntermediateProperty.PATH).asString();
		if (StringUtils.isEmpty(path)) {
			path = "/";
		}
		v.setSourcePath(path);
		v.setType(getAttributeValue(IntermediateAttribute.OBJECT_TYPE_ID).asString());
		v.setFormat(getAttributeValue(IntermediateAttribute.CONTENT_STREAM_MIME_TYPE).asString());
		v.setHistoryId(getAttributeValue(IntermediateAttribute.VERSION_SERIES_ID).asString());
		v.setAntecedentId(getAttributeValue(IntermediateAttribute.VERSION_ANTECEDENT_ID).asString());
		v.setCurrent(getAttributeValue(IntermediateAttribute.IS_LATEST_VERSION).asBoolean());
		v.setVersion(getAttributeValue(IntermediateAttribute.VERSION_LABEL).asString());

		int contents = 0;
		final boolean skipRenditions = this.factory.isSkipRenditions();
		for (CmfContentStream info : ctx.getContentStreams(this.cmfObject)) {
			if (skipRenditions && !info.isDefaultRendition()) {
				// Skip the non-default rendition
				continue;
			}
			CmfContentStore<?, ?>.Handle h = ctx.getContentStore().createHandle(translator, this.cmfObject, info);
			final File f;
			try {
				f = h.getFile();
			} catch (IOException e) {
				// Failed to get the file, so we can't handle this
				throw new CmfStorageException(
					String.format("Failed to locate the content file for %s, content qualifier [%s]",
						this.cmfObject.getDescription(), info),
					e);
			}
			ContentStreamT xml = new ContentStreamT();
			xml.setFileName(info.getFileName());
			// xml.setHash(null);
			xml.setLocation(this.factory.relativizeXmlLocation(f.getAbsolutePath()));
			xml.setMimeType(info.getMimeType().getBaseType());
			xml.setRenditionId(info.getRenditionIdentifier());
			xml.setRenditionPage(info.getRenditionPage());
			xml.setModifier(info.getModifier());
			xml.setSize(info.getLength());
			for (String k : info.getPropertyNames()) {
				xml.setProperty(k, info.getProperty(k));
			}
			v.getContents().add(xml);
			contents++;
		}

		if (contents == 0) {
			// Generate a placeholder, empty file
			CmfContentStream info = new CmfContentStream(this.cmfObject, 0);
			CmfContentStore<?, ?>.Handle h = ctx.getContentStore().createHandle(translator, this.cmfObject, info);
			File f = null;
			try {
				f = h.getFile(true);
				if (f == null) {
					throw new CmfStorageException("The given content store doesn't support file-level access");
				}
				f.createNewFile();
			} catch (IOException e) {
				// Failed to get the file, so we can't handle this
				throw new CmfStorageException(
					String.format("Failed to generate the placeholder content file for %s at [%s]",
						this.cmfObject.getDescription(), f.getAbsolutePath()),
					e);
			}
			ContentStreamT xml = new ContentStreamT();
			xml.setFileName(v.getName());
			// xml.setHash(null);
			xml.setLocation(this.factory.relativizeXmlLocation(f.getAbsolutePath()));
			xml.setMimeType(MimeTools.DEFAULT_MIME_TYPE.toString());
			xml.setRenditionId(info.getRenditionIdentifier());
			xml.setRenditionPage(info.getRenditionPage());
			xml.setModifier(info.getModifier());
			xml.setSize(0);
			v.getContents().add(xml);
		}

		dumpAttributes(v.getAttributes());
		dumpProperties(v.getProperties());

		this.factory.storeDocumentVersion(v);

		return v;
	}

	@Override
	protected Collection<ImportOutcome> importObject(CmfAttributeTranslator<CmfValue> translator, XmlImportContext ctx)
		throws ImportException, CmfStorageException {
		ImportOutcome outcome = ImportOutcome.SKIPPED;
		DocumentVersionT v = createVersion(translator, ctx);
		if (v != null) {
			outcome = new ImportOutcome(ImportResult.CREATED, this.cmfObject.getId(), this.cmfObject.getLabel());
		}
		return Collections.singleton(outcome);
	}
}