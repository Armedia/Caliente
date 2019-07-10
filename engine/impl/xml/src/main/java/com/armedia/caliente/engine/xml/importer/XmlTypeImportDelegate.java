/*******************************************************************************
 * #%L
 * Armedia Caliente
 * %%
 * Copyright (c) 2010 - 2019 Armedia LLC
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

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.armedia.caliente.engine.converter.IntermediateAttribute;
import com.armedia.caliente.engine.converter.IntermediateProperty;
import com.armedia.caliente.engine.importer.ImportException;
import com.armedia.caliente.engine.xml.importer.jaxb.AttributeDefT;
import com.armedia.caliente.engine.xml.importer.jaxb.DataTypeT;
import com.armedia.caliente.engine.xml.importer.jaxb.TypeT;
import com.armedia.caliente.engine.xml.importer.jaxb.TypesT;
import com.armedia.caliente.store.CmfAttribute;
import com.armedia.caliente.store.CmfAttributeTranslator;
import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.store.CmfProperty;
import com.armedia.caliente.store.CmfStorageException;
import com.armedia.caliente.store.CmfValue;
import com.armedia.commons.utilities.Tools;

public class XmlTypeImportDelegate extends XmlAggregatedImportDelegate<TypeT, TypesT> {

	protected XmlTypeImportDelegate(XmlImportDelegateFactory factory, CmfObject<CmfValue> storedObject)
		throws Exception {
		super(factory, storedObject, TypesT.class);
	}

	@Override
	protected TypeT createItem(CmfAttributeTranslator<CmfValue> translator, XmlImportContext ctx)
		throws ImportException, CmfStorageException {
		TypeT type = new TypeT();

		type.setName(getAttributeValue(IntermediateAttribute.NAME).asString());

		String superType = getAttributeValue(IntermediateAttribute.SUPER_NAME).asString();
		if (!StringUtils.isBlank(superType)) {
			type.setSuperType(superType);
		} else {
			type.setSuperType(null);
		}

		final int attrCount = getAttributeValue("dctm:attr_count").asInteger();
		final int startPosition = getAttributeValue("dctm:start_pos").asInteger();
		CmfAttribute<CmfValue> attName = this.cmfObject.getAttribute("dctm:attr_name");
		CmfAttribute<CmfValue> attType = this.cmfObject.getAttribute("dctm:attr_type");
		CmfAttribute<CmfValue> attRep = this.cmfObject.getAttribute("dctm:attr_repeating");
		CmfAttribute<CmfValue> attLen = this.cmfObject.getAttribute("dctm:attr_length");

		CmfProperty<CmfValue> origName = this.cmfObject.getProperty(IntermediateProperty.ORIG_ATTR_NAME);
		CmfProperty<CmfValue> mappedName = this.cmfObject.getProperty(IntermediateProperty.MAPPED_ATTR_NAME);

		Map<String, String> mapping = new HashMap<>();
		// If the mappings are in order, use them...
		if ((origName != null) && (mappedName != null) && (origName.getValueCount() == mappedName.getValueCount())) {
			for (int i = 0; i < origName.getValueCount(); i++) {
				mapping.put(origName.getValue(i).asString(), mappedName.getValue(i).asString());
			}
		}

		for (int i = 0; i < attrCount; ++i) {
			boolean inherited = (i < startPosition);
			AttributeDefT def = new AttributeDefT();
			final String srcName = attName.getValue(i).asString();
			final String tgtName = Tools.coalesce(mapping.get(srcName), srcName);
			def.setName(tgtName);
			def.setSourceName(srcName);
			DataTypeT dt = null;
			// We convert from the documentum values, because we can't do it more cleanly
			switch (attType.getValue(i).asInteger()) {
				case 0:
					dt = DataTypeT.BOOLEAN;
					break;
				case 1:
					dt = DataTypeT.INTEGER;
					break;
				case 2:
					dt = DataTypeT.STRING;
					break;
				case 3:
					dt = DataTypeT.ID;
					break;
				case 4:
					dt = DataTypeT.DATETIME;
					break;
				case 5:
					dt = DataTypeT.DOUBLE;
					break;

			}
			def.setDataType(dt);
			def.setInherited(inherited);
			def.setRepeating(attRep.getValue(i).asBoolean());
			int len = attLen.getValue(i).asInteger();
			if (len > 0) {
				def.setLength(len);
			}
			type.getAttributes().add(def);
		}
		return type;
	}
}