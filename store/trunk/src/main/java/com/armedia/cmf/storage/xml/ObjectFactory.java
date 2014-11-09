package com.armedia.cmf.storage.xml;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlElementDecl;
import javax.xml.bind.annotation.XmlRegistry;
import javax.xml.namespace.QName;

@XmlRegistry
public class ObjectFactory {

	public static final String NAMESPACE = "http://www.armedia.com/ns/cmf/objectstore";

	private final static QName _Objectstores_QNAME = new QName(ObjectFactory.NAMESPACE, "objecstores");

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

	@XmlElementDecl(namespace = ObjectFactory.NAMESPACE, name = "objectstores")
	public JAXBElement<CmsStoreDefinitions> createObjectstores(CmsStoreDefinitions value) {
		return new JAXBElement<CmsStoreDefinitions>(ObjectFactory._Objectstores_QNAME,
			CmsStoreDefinitions.class, null, value);
	}

}
