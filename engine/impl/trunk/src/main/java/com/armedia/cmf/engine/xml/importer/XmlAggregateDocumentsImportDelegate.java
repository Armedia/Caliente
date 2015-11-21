package com.armedia.cmf.engine.xml.importer;

import java.io.File;
import java.text.ParseException;
import java.util.GregorianCalendar;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;

import com.armedia.cmf.engine.converter.IntermediateAttribute;
import com.armedia.cmf.engine.converter.IntermediateProperty;
import com.armedia.cmf.engine.importer.ImportException;
import com.armedia.cmf.engine.xml.importer.jaxb.DocumentT;
import com.armedia.cmf.engine.xml.importer.jaxb.DocumentVersionT;
import com.armedia.cmf.engine.xml.importer.jaxb.DocumentsT;
import com.armedia.cmf.storage.CmfAttributeTranslator;
import com.armedia.cmf.storage.CmfContentInfo;
import com.armedia.cmf.storage.CmfContentStore;
import com.armedia.cmf.storage.CmfObject;
import com.armedia.cmf.storage.CmfStorageException;
import com.armedia.cmf.storage.CmfValue;
import com.armedia.cmf.storage.CmfValueDecoderException;

public class XmlAggregateDocumentsImportDelegate extends XmlAggregatedImportDelegate<DocumentT, DocumentsT> {

	protected XmlAggregateDocumentsImportDelegate(XmlImportDelegateFactory factory, CmfObject<CmfValue> storedObject)
		throws Exception {
		super(factory, storedObject, DocumentsT.class);
	}

	@Override
	protected DocumentT createItem(CmfAttributeTranslator<CmfValue> translator, XmlImportContext ctx)
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

			gcal.setTime(getAttributeValue(IntermediateAttribute.LAST_ACCESS_DATE).asTime());
			v.setLastAccessDate(dtf.newXMLGregorianCalendar(gcal));
			v.setLastAccessor(getAttributeValue(IntermediateAttribute.LAST_ACCESSED_BY).asString());
		} catch (ParseException e) {
			throw new CmfValueDecoderException("Failed to parse a date value", e);
		}

		v.setName(getAttributeValue(IntermediateAttribute.NAME).asString());
		v.setParentId(getAttributeValue(IntermediateAttribute.PARENT_ID).asString());
		v.setSourcePath(getAttributeValue(IntermediateAttribute.PATH).asString());
		v.setType(getAttributeValue(IntermediateAttribute.OBJECT_TYPE_ID).asString());

		v.setHistoryId(getAttributeValue(IntermediateAttribute.VERSION_SERIES_ID).asString());
		v.setAntecedentId(getAttributeValue(IntermediateAttribute.VERSION_ANTECEDENT_ID).asString());
		v.setCurrent(getAttributeValue(IntermediateAttribute.IS_LAST_VERSION).asBoolean());
		v.setVersion(getAttributeValue(IntermediateAttribute.VERSION_LABEL).asString());

		for (CmfContentInfo info : ctx.getContentInfo(this.cmfObject)) {
			CmfContentStore<?>.Handle h = ctx.getContentStore().getHandle(translator, this.cmfObject,
				info.getQualifier());
			File f = h.getFile();
			// TODO: Relativize the path with relation to the content root
			v.setContentLocation(f.getAbsolutePath());
			v.setContentSize((int) info.getLength());
			break;
		}

		dumpAttributes(v.getAttributes());

		DocumentT ret = new DocumentT();
		ret.getVersion().add(v);
		return ret;
	}
}