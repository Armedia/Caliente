package com.armedia.caliente.store.local.xml;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlElementDecl;
import javax.xml.bind.annotation.XmlRegistry;

import com.armedia.caliente.store.local.XmlObjectFactory;

@XmlRegistry
public class ObjectFactory extends XmlObjectFactory<PropertyT, StorePropertiesT> {

	public static final String NAMESPACE = "http://www.armedia.com/ns/caliente/stores/local/store-properties";

	public ObjectFactory() {
		super(ObjectFactory.NAMESPACE, StorePropertiesT.class);
	}

	public StorePropertiesT createStorePropertiesT() {
		return new StorePropertiesT();
	}

	public PropertyT createPropertyT() {
		return new PropertyT();
	}

	@XmlElementDecl(namespace = ObjectFactory.NAMESPACE, name = "store-properties")
	public JAXBElement<StorePropertiesT> createStoreProperties(StorePropertiesT value) {
		return super.createRoot(value);
	}
}