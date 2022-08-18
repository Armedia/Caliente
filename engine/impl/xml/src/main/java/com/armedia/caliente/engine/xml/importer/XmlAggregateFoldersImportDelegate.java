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

import java.text.ParseException;
import java.util.GregorianCalendar;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;

import org.apache.commons.lang3.StringUtils;

import com.armedia.caliente.engine.converter.IntermediateAttribute;
import com.armedia.caliente.engine.converter.IntermediateProperty;
import com.armedia.caliente.engine.importer.ImportException;
import com.armedia.caliente.engine.xml.importer.jaxb.FolderT;
import com.armedia.caliente.engine.xml.importer.jaxb.FoldersT;
import com.armedia.caliente.store.CmfAttributeTranslator;
import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.store.CmfStorageException;
import com.armedia.caliente.store.CmfValue;

public class XmlAggregateFoldersImportDelegate extends XmlAggregatedImportDelegate<FolderT, FoldersT> {

	protected XmlAggregateFoldersImportDelegate(XmlImportDelegateFactory factory, CmfObject<CmfValue> storedObject)
		throws Exception {
		super(factory, storedObject, FoldersT.class);
	}

	@Override
	protected FolderT createItem(CmfAttributeTranslator<CmfValue> translator, XmlImportContext ctx)
		throws ImportException, CmfStorageException {
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
			gcal.setTimeZone(XmlImportDelegate.TZUTC);
			f.setCreationDate(dtf.newXMLGregorianCalendar(gcal));
			f.setCreator(getAttributeValue(IntermediateAttribute.CREATED_BY).asString());

			gcal.setTime(getAttributeValue(IntermediateAttribute.LAST_MODIFICATION_DATE).asTime());
			gcal.setTimeZone(XmlImportDelegate.TZUTC);
			f.setModificationDate(dtf.newXMLGregorianCalendar(gcal));
			f.setModifier(getAttributeValue(IntermediateAttribute.LAST_MODIFIED_BY).asString());
		} catch (ParseException e) {
			throw new ImportException("Failed to parse a date value", e);
		}

		f.setName(getAttributeValue(IntermediateAttribute.NAME).asString());
		f.setParentId(getAttributeValue(IntermediateAttribute.PARENT_ID).asString());
		String path = getFixedPath(ctx);
		if (StringUtils.isEmpty(path)) {
			path = StringUtils.EMPTY;
		} else {
			path = path.replaceAll("^/+", "");
		}
		f.setSourcePath(path);
		f.setType(getAttributeValue(IntermediateAttribute.OBJECT_TYPE_ID).asString());

		dumpAttributes(f.getAttributes());
		dumpProperties(f.getProperties());
		return f;
	}
}