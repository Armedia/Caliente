package com.armedia.caliente.store.local.xml;

import com.armedia.caliente.store.local.XmlPropertiesLoader;

public class PropertiesLoader extends XmlPropertiesLoader<PropertyT, StorePropertiesT> {

	public static final String SCHEMA = "store-properties.xsd";

	public PropertiesLoader() {
		super(PropertiesLoader.SCHEMA, StorePropertiesT.class);
	}
}