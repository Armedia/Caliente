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
package com.armedia.caliente.store;

import java.text.ParseException;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

import com.armedia.commons.utilities.Tools;
import com.armedia.commons.utilities.codec.FunctionalCodec;

public abstract class CmfAttributeTranslator<VALUE> {

	private static class DefaultValueCodec extends FunctionalCodec<CmfValue, CmfValue>
		implements CmfValueCodec<CmfValue> {
		private DefaultValueCodec(CmfValue.Type type) {
			super(Function.identity(), type.getNull(), CmfValue::isNull, Function.identity(), type.getNull(),
				CmfValue::isNull);
		}
	};

	public static final CmfAttributeNameMapper NULL_MAPPER = new CmfAttributeNameMapper();

	private static final Map<CmfValue.Type, CmfValueCodec<CmfValue>> CODECS;

	static {
		Map<CmfValue.Type, CmfValueCodec<CmfValue>> codecs = new EnumMap<>(CmfValue.Type.class);
		for (CmfValue.Type t : CmfValue.Type.values()) {
			codecs.put(t, new DefaultValueCodec(t));
		}
		CODECS = Tools.freezeMap(codecs);
	}

	public static final CmfAttributeTranslator<CmfValue> CMFVALUE_TRANSLATOR = new CmfAttributeTranslator<CmfValue>(
		CmfValue.class) {

		@Override
		public CmfValueCodec<CmfValue> getCodec(CmfValue.Type type) {
			return CmfAttributeTranslator.CODECS.get(type);
		}

		@Override
		public CmfValue getValue(CmfValue.Type type, Object value) throws ParseException {
			return new CmfValue(type, value);
		}

	};

	public static CmfValueCodec<CmfValue> getStoredValueCodec(CmfValue.Type type) {
		return CmfAttributeTranslator.CODECS.get(type);
	}

	private final Class<VALUE> valueClass;
	private final CmfAttributeNameMapper nameMapper;

	protected CmfAttributeTranslator(Class<VALUE> valueClass) {
		this(valueClass, null);
	}

	protected CmfAttributeTranslator(Class<VALUE> valueClass, CmfAttributeNameMapper cmfAttributeNameMapper) {
		this.valueClass = Objects.requireNonNull(valueClass, "Must provide a value class");
		this.nameMapper = Tools.coalesce(cmfAttributeNameMapper, CmfAttributeTranslator.NULL_MAPPER);
	}

	public final CmfAttributeNameMapper getAttributeNameMapper() {
		return this.nameMapper;
	}

	public abstract CmfValueCodec<VALUE> getCodec(CmfValue.Type type);

	public abstract VALUE getValue(CmfValue.Type type, Object value) throws ParseException;

	public final CmfObject<VALUE> decodeObject(CmfObject<CmfValue> obj) {
		// Can we optimize this if there are no changes needed?
		if (this.valueClass.equals(CmfValue.class) && (this.nameMapper == CmfAttributeTranslator.NULL_MAPPER)) {
			@SuppressWarnings("unchecked")
			CmfObject<VALUE> ret = (CmfObject<VALUE>) obj;
			return ret;
		}

		CmfObject<VALUE> newObj = new CmfObject<>(//
			this, //
			obj.getType(), //
			obj.getId(), //
			obj.getName(), //
			obj.getParentReferences(), //
			obj.getDependencyTier(), //
			obj.getHistoryId(), //
			obj.isHistoryCurrent(), //
			obj.getLabel(), //
			obj.getSubtype(), //
			obj.getSecondarySubtypes(), //
			obj.getNumber() //
		);

		for (CmfAttribute<CmfValue> att : obj.getAttributes()) {
			String attName = this.nameMapper.decodeAttributeName(newObj.getType(), att.getName());
			CmfAttribute<VALUE> newAtt = new CmfAttribute<>(attName, att.getType(), att.isMultivalued());
			CmfValueCodec<VALUE> codec = getCodec(att.getType());
			if (newAtt.isMultivalued()) {
				for (CmfValue v : att) {
					newAtt.addValue(codec.decode(v));
				}
			} else {
				newAtt.setValue(codec.decode(att.getValue()));
			}
			newObj.setAttribute(newAtt);
		}

		for (CmfProperty<CmfValue> prop : obj.getProperties()) {
			CmfProperty<VALUE> newProp = new CmfProperty<>(prop.getName(), prop.getType(), prop.isMultivalued());
			CmfValueCodec<VALUE> codec = getCodec(prop.getType());
			if (newProp.isMultivalued()) {
				for (CmfValue v : prop) {
					newProp.addValue(codec.decode(v));
				}
			} else {
				newProp.setValue(codec.decode(prop.getValue()));
			}
			newObj.setProperty(newProp);
		}

		return newObj;
	}

	public final CmfObject<CmfValue> encodeObject(CmfObject<VALUE> obj) {
		// Can we optimize this if there are no changes needed?
		if (this.valueClass.equals(CmfValue.class) && (this.nameMapper == CmfAttributeTranslator.NULL_MAPPER)) {
			@SuppressWarnings("unchecked")
			CmfObject<CmfValue> ret = (CmfObject<CmfValue>) obj;
			return ret;
		}

		CmfObject<CmfValue> newObj = new CmfObject<>(//
			CmfAttributeTranslator.CMFVALUE_TRANSLATOR, //
			obj.getType(), //
			obj.getId(), //
			obj.getName(), //
			obj.getParentReferences(), //
			obj.getDependencyTier(), //
			obj.getHistoryId(), //
			obj.isHistoryCurrent(), //
			obj.getLabel(), //
			obj.getSubtype(), //
			obj.getSecondarySubtypes(), //
			obj.getNumber() //
		);

		for (CmfAttribute<VALUE> att : obj.getAttributes()) {
			String attName = this.nameMapper.encodeAttributeName(newObj.getType(), att.getName());
			CmfAttribute<CmfValue> newAtt = new CmfAttribute<>(attName, att.getType(), att.isMultivalued());
			CmfValueCodec<VALUE> codec = getCodec(att.getType());
			if (newAtt.isMultivalued()) {
				for (VALUE v : att) {
					newAtt.addValue(codec.encode(v));
				}
			} else {
				newAtt.setValue(codec.encode(att.getValue()));
			}
			newObj.setAttribute(newAtt);
		}

		for (CmfProperty<VALUE> prop : obj.getProperties()) {
			CmfProperty<CmfValue> newProp = new CmfProperty<>(prop.getName(), prop.getType(), prop.isMultivalued());
			CmfValueCodec<VALUE> codec = getCodec(prop.getType());
			if (newProp.isMultivalued()) {
				for (VALUE v : prop) {
					newProp.addValue(codec.encode(v));
				}
			} else {
				newProp.setValue(codec.encode(prop.getValue()));
			}
			newObj.setProperty(newProp);
		}

		return newObj;
	}
}