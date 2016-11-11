package com.armedia.caliente.store.local.xml;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlElementDecl;
import javax.xml.bind.annotation.XmlRegistry;
import javax.xml.namespace.QName;

@XmlRegistry
public class ObjectFactory {

	public static final String NAMESPACE = "http://www.armedia.com/ns/caliente/stores/local/store-properties";

	private final static QName _StoreProperties_QNAME = new QName(ObjectFactory.NAMESPACE, "store-properties");

	/**
	 * Create a new ObjectFactory that can be used to create new instances of schema derived classes
	 * for package: com.armedia.caliente.store.local.xml
	 *
	 */
	public ObjectFactory() {
	}

	/**
	 * Create an instance of {@link StorePropertiesT }
	 *
	 */
	public StorePropertiesT createStorePropertiesT() {
		return new StorePropertiesT();
	}

	/**
	 * Create an instance of {@link PropertyT }
	 *
	 */
	public PropertyT createPropertyT() {
		return new PropertyT();
	}

	/**
	 * Create an instance of {@link JAXBElement }{@code <}{@link StorePropertiesT }{@code >}
	 *
	 */
	@XmlElementDecl(namespace = ObjectFactory.NAMESPACE, name = "store-properties")
	public JAXBElement<StorePropertiesT> createStoreProperties(StorePropertiesT value) {
		return new JAXBElement<>(ObjectFactory._StoreProperties_QNAME, StorePropertiesT.class, null, value);
	}

}
