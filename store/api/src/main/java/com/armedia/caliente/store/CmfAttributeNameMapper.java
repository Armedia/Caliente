package com.armedia.caliente.store;

public class CmfAttributeNameMapper {

	public final String encodeAttributeName(CmfObject.Archetype type, CmfEncodeableName attributeName) {
		return encodeAttributeName(type, attributeName.encode());
	}

	public String encodeAttributeName(CmfObject.Archetype type, String attributeName) {
		return attributeName;
	}

	public final String decodeAttributeName(CmfObject.Archetype type, CmfEncodeableName attributeName) {
		return decodeAttributeName(type, attributeName.encode());
	}

	public String decodeAttributeName(CmfObject.Archetype type, String attributeName) {
		return attributeName;
	}

}