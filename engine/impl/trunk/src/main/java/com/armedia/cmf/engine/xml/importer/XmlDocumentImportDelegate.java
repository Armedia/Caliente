package com.armedia.cmf.engine.xml.importer;

import java.io.File;
import java.text.ParseException;
import java.util.Collection;
import java.util.Collections;
import java.util.GregorianCalendar;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;

import org.apache.commons.lang3.StringUtils;

import com.armedia.cmf.engine.converter.IntermediateAttribute;
import com.armedia.cmf.engine.converter.IntermediateProperty;
import com.armedia.cmf.engine.importer.ImportException;
import com.armedia.cmf.engine.importer.ImportOutcome;
import com.armedia.cmf.engine.importer.ImportResult;
import com.armedia.cmf.engine.xml.importer.jaxb.ContentInfoT;
import com.armedia.cmf.engine.xml.importer.jaxb.DocumentVersionT;
import com.armedia.cmf.storage.CmfAttributeTranslator;
import com.armedia.cmf.storage.CmfContentInfo;
import com.armedia.cmf.storage.CmfContentStore;
import com.armedia.cmf.storage.CmfObject;
import com.armedia.cmf.storage.CmfStorageException;
import com.armedia.cmf.storage.CmfValue;
import com.armedia.cmf.storage.CmfValueDecoderException;

public class XmlDocumentImportDelegate extends XmlImportDelegate {

	protected XmlDocumentImportDelegate(XmlImportDelegateFactory factory, CmfObject<CmfValue> storedObject)
		throws Exception {
		super(factory, storedObject);
	}

	protected DocumentVersionT createVersion(CmfAttributeTranslator<CmfValue> translator, XmlImportContext ctx)
		throws ImportException, CmfStorageException, CmfValueDecoderException {
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
			v.setCreationDate(dtf.newXMLGregorianCalendar(gcal));
			v.setCreator(getAttributeValue(IntermediateAttribute.CREATED_BY).asString());

			gcal.setTime(getAttributeValue(IntermediateAttribute.LAST_MODIFICATION_DATE).asTime());
			v.setModificationDate(dtf.newXMLGregorianCalendar(gcal));
			v.setModifier(getAttributeValue(IntermediateAttribute.LAST_MODIFIED_BY).asString());

			CmfValue lad = getAttributeValue(IntermediateAttribute.LAST_ACCESS_DATE);
			if (!lad.isNull()) {
				gcal.setTime(lad.asTime());
				v.setLastAccessDate(dtf.newXMLGregorianCalendar(gcal));
			}
			v.setLastAccessor(getAttributeValue(IntermediateAttribute.LAST_ACCESSED_BY).asString());
		} catch (ParseException e) {
			throw new CmfValueDecoderException("Failed to parse a date value", e);
		}

		v.setName(getAttributeValue(IntermediateAttribute.NAME).asString());
		v.setParentId(getAttributeValue(IntermediateAttribute.PARENT_ID).asString());
		String path = getPropertyValue(IntermediateProperty.PATH).asString();
		if (StringUtils.isEmpty(path)) {
			path = "/";
		}
		v.setSourcePath(path);
		v.setType(getAttributeValue(IntermediateAttribute.OBJECT_TYPE_ID).asString());

		v.setHistoryId(getAttributeValue(IntermediateAttribute.VERSION_SERIES_ID).asString());
		v.setAntecedentId(getAttributeValue(IntermediateAttribute.VERSION_ANTECEDENT_ID).asString());
		v.setCurrent(getAttributeValue(IntermediateAttribute.IS_LAST_VERSION).asBoolean());
		v.setVersion(getAttributeValue(IntermediateAttribute.VERSION_LABEL).asString());

		for (CmfContentInfo info : ctx.getContentInfo(this.cmfObject)) {
			CmfContentStore<?>.Handle h = ctx.getContentStore().getHandle(translator, this.cmfObject,
				info.getQualifier());
			File f = h.getFile();
			ContentInfoT xml = new ContentInfoT();
			xml.setFileName(info.getFileName());
			// xml.setHash(null);
			xml.setLocation(this.factory.relativizeXmlLocation(f.getAbsolutePath()));
			xml.setMimeType(info.getMimeType().getBaseType());
			xml.setQualifier(info.getQualifier());
			xml.setSize(info.getLength());
			for (String k : info.getPropertyNames()) {
				xml.setProperty(k, info.getProperty(k));
			}
			v.getContents().add(xml);
		}

		dumpAttributes(v.getAttributes());
		dumpProperties(v.getProperties());

		this.factory.storeDocumentVersion(v);

		return v;
	}

	@Override
	protected Collection<ImportOutcome> importObject(CmfAttributeTranslator<CmfValue> translator, XmlImportContext ctx)
		throws ImportException, CmfStorageException, CmfValueDecoderException {
		ImportOutcome outcome = ImportOutcome.SKIPPED;
		DocumentVersionT v = createVersion(translator, ctx);
		if (v != null) {
			outcome = new ImportOutcome(ImportResult.CREATED, this.cmfObject.getId(), this.cmfObject.getLabel());
		}
		return Collections.singleton(outcome);
	}
}