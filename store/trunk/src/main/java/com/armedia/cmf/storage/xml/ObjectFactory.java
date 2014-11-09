package com.armedia.cmf.storage.xml;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlElementDecl;
import javax.xml.bind.annotation.XmlRegistry;
import javax.xml.namespace.QName;

@XmlRegistry
public class ObjectFactory {

	public static final String NAMESPACE = "http://www.armedia.com/ns/cmf/stores";

	private final static QName _Stores_QNAME = new QName(ObjectFactory.NAMESPACE, "stores");

	/**
	 * Create a new ObjectFactory that can be used to create new instances of schema derived classes
	 * for package: com.armedia.cmf.storage.cfg
	 *
	 */
	public ObjectFactory() {
	}

	/**
	 * Create an instance of {@link CmsStoreDefinitions }
	 *
	 */
	public CmsStoreDefinitions createObjectstoresT() {
		return new CmsStoreDefinitions();
	}

	/**
	 * Create an instance of {@link CmsStoreConfiguration }
	 *
	 */
	public CmsStoreConfiguration createObjectstoreT() {
		return new CmsStoreConfiguration();
	}

	/**
	 * Create an instance of {@link Setting }
	 *
	 */
	public Setting createSettingT() {
		return new Setting();
	}

	@XmlElementDecl(namespace = ObjectFactory.NAMESPACE, name = "stores")
	public JAXBElement<CmsStoreDefinitions> createStores(CmsStoreDefinitions value) {
		return new JAXBElement<CmsStoreDefinitions>(ObjectFactory._Stores_QNAME, CmsStoreDefinitions.class, null, value);
	}

}
