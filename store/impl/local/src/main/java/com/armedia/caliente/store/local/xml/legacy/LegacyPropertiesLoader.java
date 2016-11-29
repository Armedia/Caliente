package com.armedia.caliente.store.local.xml.legacy;

import com.armedia.caliente.store.local.XmlPropertiesLoader;

public class LegacyPropertiesLoader extends XmlPropertiesLoader<PropertyT, StorePropertiesT> {

	public static final String SCHEMA = "store-properties-legacy.xsd";

	public LegacyPropertiesLoader() {
		super(LegacyPropertiesLoader.SCHEMA, StorePropertiesT.class);
	}
}