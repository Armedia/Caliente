package com.armedia.caliente.store;

public class CmfAttributeNameMapper {

	public final String encodeAttributeName(CmfArchetype type, CmfEncodeableName attributeName) {
		return encodeAttributeName(type, attributeName.encode());
	}

	public String encodeAttributeName(CmfArchetype type, String attributeName) {
		return attributeName;
	}

	public final String decodeAttributeName(CmfArchetype type, CmfEncodeableName attributeName) {
		return decodeAttributeName(type, attributeName.encode());
	}

	public String decodeAttributeName(CmfArchetype type, String attributeName) {
		return attributeName;
	}

}