package com.armedia.caliente.store.local;

import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;

import com.armedia.caliente.store.local.xml.legacy.StorePropertiesT;

public class XmlObjectFactory<P extends XmlProperty, S extends XmlStoreProperties<P>> {

	private final QName QNAME;

	private final Class<S> rootClass;

	/**
	 * Create a new ObjectFactory that can be used to create new instances of schema derived classes
	 * for package: com.armedia.caliente.store.local.xml
	 *
	 */
	public XmlObjectFactory(String namespace, Class<S> rootClass) {
		this.QNAME = new QName(namespace, "store-properties");
		this.rootClass = rootClass;
	}

	/**
	 * Create an instance of {@link JAXBElement }{@code <}{@link StorePropertiesT }{@code >}
	 *
	 */
	protected final JAXBElement<S> createRoot(S value) {
		return new JAXBElement<>(this.QNAME, this.rootClass, null, value);
	}
}