package com.armedia.caliente.store;

public class CmfAttributeNameMapper {

	public final String encodeAttributeName(CmfType type, CmfEncodeableName attributeName) {
		return encodeAttributeName(type, attributeName.encode());
	}

	public String encodeAttributeName(CmfType type, String attributeName) {
		return attributeName;
	}

	public final String decodeAttributeName(CmfType type, CmfEncodeableName attributeName) {
		return decodeAttributeName(type, attributeName.encode());
	}

	public String decodeAttributeName(CmfType type, String attributeName) {
		return attributeName;
	}

}