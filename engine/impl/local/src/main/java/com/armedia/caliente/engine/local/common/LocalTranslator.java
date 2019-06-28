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
package com.armedia.caliente.engine.local.common;

import java.text.ParseException;
import java.util.EnumMap;
import java.util.Map;

import org.apache.chemistry.opencmis.commons.enums.PropertyType;
import org.apache.commons.collections4.BidiMap;
import org.apache.commons.collections4.bidimap.DualHashBidiMap;
import org.apache.commons.collections4.bidimap.UnmodifiableBidiMap;

import com.armedia.caliente.engine.converter.IntermediateAttribute;
import com.armedia.caliente.store.CmfAttributeNameMapper;
import com.armedia.caliente.store.CmfAttributeTranslator;
import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.store.CmfValue;
import com.armedia.caliente.store.CmfValueCodec;
import com.armedia.commons.utilities.Tools;

public class LocalTranslator extends CmfAttributeTranslator<CmfValue> {

	private static final Map<PropertyType, CmfValue.Type> DATA_TYPES;
	private static final Map<CmfValue.Type, PropertyType> DATA_TYPES_REV;

	private static final Map<CmfObject.Archetype, BidiMap<String, IntermediateAttribute>> ATTRIBUTE_MAPPINGS;

	static {
		Map<PropertyType, CmfValue.Type> m = new EnumMap<>(PropertyType.class);
		m.put(PropertyType.BOOLEAN, CmfValue.Type.BOOLEAN);
		m.put(PropertyType.INTEGER, CmfValue.Type.INTEGER);
		m.put(PropertyType.DECIMAL, CmfValue.Type.DOUBLE);
		m.put(PropertyType.DATETIME, CmfValue.Type.DATETIME);
		m.put(PropertyType.ID, CmfValue.Type.ID);
		m.put(PropertyType.STRING, CmfValue.Type.STRING);
		m.put(PropertyType.URI, CmfValue.Type.STRING); // TODO: Add this to CmfValue.Type
		m.put(PropertyType.HTML, CmfValue.Type.STRING); // TODO: Add this to CmfValue.Type
		DATA_TYPES = Tools.freezeMap(m);

		Map<CmfValue.Type, PropertyType> n = new EnumMap<>(CmfValue.Type.class);
		n.put(CmfValue.Type.BOOLEAN, PropertyType.BOOLEAN);
		n.put(CmfValue.Type.INTEGER, PropertyType.INTEGER);
		n.put(CmfValue.Type.DOUBLE, PropertyType.DECIMAL);
		n.put(CmfValue.Type.DATETIME, PropertyType.DATETIME);
		n.put(CmfValue.Type.ID, PropertyType.ID);
		n.put(CmfValue.Type.STRING, PropertyType.STRING); // TODO: Need to handle HTML and URI
		DATA_TYPES_REV = Tools.freezeMap(n);

		Map<CmfObject.Archetype, BidiMap<String, IntermediateAttribute>> attributeMappings = new EnumMap<>(
			CmfObject.Archetype.class);

		BidiMap<String, IntermediateAttribute> am = null;

		am = new DualHashBidiMap<>();
		// BASE_TYPE_ID (DOCUMENT)
		// OBJECT_TYPE_ID (cmis:document|...)
		// am.put(LocalAttributes.VERSION_ANTECEDENT_ID.name,
		// IntermediateAttribute.VERSION_ANTECEDENT_ID);
		attributeMappings.put(CmfObject.Archetype.DOCUMENT, UnmodifiableBidiMap.unmodifiableBidiMap(am));

		ATTRIBUTE_MAPPINGS = Tools.freezeMap(attributeMappings);
	}

	public static CmfValue.Type decodePropertyType(PropertyType t) {
		return LocalTranslator.DATA_TYPES.get(t);
	}

	public static PropertyType decodePropertyType(CmfValue.Type t) {
		return LocalTranslator.DATA_TYPES_REV.get(t);
	}

	private static BidiMap<String, IntermediateAttribute> getAttributeMappings(CmfObject.Archetype type) {
		return LocalTranslator.ATTRIBUTE_MAPPINGS.get(type);
	}

	@SuppressWarnings("unused")
	private static final CmfAttributeNameMapper MAPPER = new CmfAttributeNameMapper() {

		@Override
		public String encodeAttributeName(CmfObject.Archetype type, String attributeName) {
			BidiMap<String, IntermediateAttribute> mappings = LocalTranslator.getAttributeMappings(type);
			if (mappings != null) {
				// TODO: normalize the CMS attribute name
				IntermediateAttribute att = mappings.get(attributeName);
				if (att != null) { return att.encode(); }
			}
			return super.encodeAttributeName(type, attributeName);
		}

		@Override
		public String decodeAttributeName(CmfObject.Archetype type, String attributeName) {
			BidiMap<String, IntermediateAttribute> mappings = LocalTranslator.getAttributeMappings(type);
			if (mappings != null) {
				String att = null;
				try {
					// TODO: normalize the intermediate attribute name
					att = mappings.getKey(IntermediateAttribute.decode(attributeName));
				} catch (IllegalArgumentException e) {
					att = null;
				}
				if (att != null) { return att; }
			}
			return super.decodeAttributeName(type, attributeName);
		}
	};

	public LocalTranslator() {
		super(CmfValue.class, null);
	}

	@Override
	public CmfValueCodec<CmfValue> getCodec(CmfValue.Type type) {
		return CmfAttributeTranslator.getStoredValueCodec(type);
	}

	@Override
	public CmfValue getValue(CmfValue.Type type, Object value) throws ParseException {
		return new CmfValue(type, value);
	}
}