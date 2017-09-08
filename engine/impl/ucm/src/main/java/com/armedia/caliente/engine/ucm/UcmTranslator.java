package com.armedia.caliente.engine.ucm;

import java.text.ParseException;
import java.util.EnumMap;
import java.util.Map;

import org.apache.chemistry.opencmis.commons.enums.BaseTypeId;
import org.apache.commons.collections4.BidiMap;
import org.apache.commons.collections4.bidimap.DualHashBidiMap;
import org.apache.commons.collections4.bidimap.UnmodifiableBidiMap;

import com.armedia.caliente.engine.converter.IntermediateAttribute;
import com.armedia.caliente.store.CmfAttributeTranslator;
import com.armedia.caliente.store.CmfDataType;
import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.store.CmfType;
import com.armedia.caliente.store.CmfValue;
import com.armedia.caliente.store.CmfValueCodec;
import com.armedia.commons.utilities.Tools;

public class UcmTranslator extends CmfAttributeTranslator<CmfValue> {

	private static final Map<CmfType, BidiMap<String, IntermediateAttribute>> ATTRIBUTE_MAPPINGS;

	static {
		Map<CmfType, BidiMap<String, IntermediateAttribute>> attributeMappings = new EnumMap<>(CmfType.class);

		BidiMap<String, IntermediateAttribute> am = null;
		am = new DualHashBidiMap<>();
		// BASE_TYPE_ID (DOCUMENT)
		// OBJECT_TYPE_ID (cmis:document|...)
		attributeMappings.put(CmfType.DOCUMENT, UnmodifiableBidiMap.unmodifiableBidiMap(am));

		ATTRIBUTE_MAPPINGS = Tools.freezeMap(attributeMappings);
	}

	@Override
	public CmfObject<CmfValue> decodeObject(CmfObject<CmfValue> rawObject) {
		return super.decodeObject(rawObject);
	}

	@Override
	public CmfObject<CmfValue> encodeObject(CmfObject<CmfValue> rawObject) {
		return super.encodeObject(rawObject);
	}

	private BidiMap<String, IntermediateAttribute> getAttributeMappings(CmfType type) {
		return UcmTranslator.ATTRIBUTE_MAPPINGS.get(type);
	}

	@Override
	public String encodeAttributeName(CmfType type, String attributeName) {
		BidiMap<String, IntermediateAttribute> mappings = getAttributeMappings(type);
		if (mappings != null) {
			// TODO: normalize the CMS attribute name
			IntermediateAttribute att = mappings.get(attributeName);
			if (att != null) { return att.encode(); }
		}
		return super.encodeAttributeName(type, attributeName);
	}

	@Override
	public String decodeAttributeName(CmfType type, String attributeName) {
		BidiMap<String, IntermediateAttribute> mappings = getAttributeMappings(type);
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

	@Override
	public CmfValueCodec<CmfValue> getCodec(CmfDataType type) {
		return CmfAttributeTranslator.getStoredValueCodec(type);
	}

	@Override
	public CmfValue getValue(CmfDataType type, Object value) throws ParseException {
		return new CmfValue(type, value);
	}

	@Override
	public String getDefaultSubtype(CmfType baseType) {
		switch (baseType) {
			case DOCUMENT:
				return BaseTypeId.CMIS_DOCUMENT.value();

			case FOLDER:
				return BaseTypeId.CMIS_FOLDER.value();

			default:
				break;
		}
		return null;
	}
}