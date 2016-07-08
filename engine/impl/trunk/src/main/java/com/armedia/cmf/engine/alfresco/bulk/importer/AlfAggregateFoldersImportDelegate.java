package com.armedia.cmf.engine.alfresco.bulk.importer;

import java.text.ParseException;
import java.util.GregorianCalendar;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;

import org.apache.commons.lang3.StringUtils;

import com.armedia.cmf.engine.alfresco.bulk.importer.jaxb.FolderT;
import com.armedia.cmf.engine.alfresco.bulk.importer.jaxb.FoldersT;
import com.armedia.cmf.engine.converter.IntermediateAttribute;
import com.armedia.cmf.engine.converter.IntermediateProperty;
import com.armedia.cmf.engine.importer.ImportException;
import com.armedia.cmf.storage.CmfAttributeTranslator;
import com.armedia.cmf.storage.CmfObject;
import com.armedia.cmf.storage.CmfStorageException;
import com.armedia.cmf.storage.CmfValue;
import com.armedia.cmf.storage.CmfValueDecoderException;

public class AlfAggregateFoldersImportDelegate extends AlfAggregatedImportDelegate<FolderT, FoldersT> {

	protected AlfAggregateFoldersImportDelegate(AlfImportDelegateFactory factory, CmfObject<CmfValue> storedObject)
		throws Exception {
		super(factory, storedObject, FoldersT.class);
	}

	@Override
	protected FolderT createItem(CmfAttributeTranslator<CmfValue> translator, AlfImportContext ctx)
		throws ImportException, CmfStorageException, CmfValueDecoderException {
		FolderT f = new FolderT();
		DatatypeFactory dtf;
		try {
			dtf = DatatypeFactory.newInstance();
		} catch (DatatypeConfigurationException e) {
			throw new ImportException(e);
		}
		GregorianCalendar gcal = new GregorianCalendar();

		f.setId(this.cmfObject.getId());
		f.setAcl(getPropertyValue(IntermediateProperty.ACL_ID).asString());

		try {
			gcal.setTime(getAttributeValue(IntermediateAttribute.CREATION_DATE).asTime());
			gcal.setTimeZone(AlfImportDelegate.TZUTC);
			f.setCreationDate(dtf.newXMLGregorianCalendar(gcal));
			f.setCreator(getAttributeValue(IntermediateAttribute.CREATED_BY).asString());

			gcal.setTime(getAttributeValue(IntermediateAttribute.LAST_MODIFICATION_DATE).asTime());
			gcal.setTimeZone(AlfImportDelegate.TZUTC);
			f.setModificationDate(dtf.newXMLGregorianCalendar(gcal));
			f.setModifier(getAttributeValue(IntermediateAttribute.LAST_MODIFIED_BY).asString());
		} catch (ParseException e) {
			throw new CmfValueDecoderException("Failed to parse a date value", e);
		}

		f.setName(getAttributeValue(IntermediateAttribute.NAME).asString());
		f.setParentId(getAttributeValue(IntermediateAttribute.PARENT_ID).asString());
		String path = getPropertyValue(IntermediateProperty.PATH).asString();
		if (StringUtils.isEmpty(path)) {
			path = "/";
		}
		f.setSourcePath(path);
		f.setType(getAttributeValue(IntermediateAttribute.OBJECT_TYPE_ID).asString());

		dumpAttributes(f.getAttributes());
		dumpProperties(f.getProperties());
		return f;
	}
}