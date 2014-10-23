package com.armedia.cmf.storage.xml;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlElementDecl;
import javax.xml.bind.annotation.XmlRegistry;
import javax.xml.namespace.QName;

@XmlRegistry
public class ObjectFactory {

	public static final String NAMESPACE = "http://www.armedia.com/ns/cmf/objectstores";

	private final static QName _Objectstores_QNAME = new QName(ObjectFactory.NAMESPACE, "objecstores");

	/**
	 * Create a new ObjectFactory that can be used to create new instances of schema derived classes
	 * for package: com.armedia.cmf.storage.cfg
	 *
	 */
	public ObjectFactory() {
	}

	/**
	 * Create an instance of {@link CmsObjectStoreDefinitions }
	 *
	 */
	public CmsObjectStoreDefinitions createObjectstoresT() {
		return new CmsObjectStoreDefinitions();
	}

	/**
	 * Create an instance of {@link CmsObjectStoreConfiguration }
	 *
	 */
	public CmsObjectStoreConfiguration createObjectstoreT() {
		return new CmsObjectStoreConfiguration();
	}

	/**
	 * Create an instance of {@link Setting }
	 *
	 */
	public Setting createSettingT() {
		return new Setting();
	}

	@XmlElementDecl(namespace = ObjectFactory.NAMESPACE, name = "objectstores")
	public JAXBElement<CmsObjectStoreDefinitions> createObjectstores(CmsObjectStoreDefinitions value) {
		return new JAXBElement<CmsObjectStoreDefinitions>(ObjectFactory._Objectstores_QNAME, CmsObjectStoreDefinitions.class, null, value);
	}

}
