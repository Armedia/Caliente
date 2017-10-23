package com.armedia.caliente.engine.alfresco.bi.importer.index;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlElementDecl;
import javax.xml.bind.annotation.XmlRegistry;
import javax.xml.namespace.QName;

@XmlRegistry
public class ObjectFactory {

	static final String NAMESPACE = "http://www.armedia.com/ns/caliente/engine/alfresco-bi/cache";
	private final static QName _Scan_QNAME = new QName(ObjectFactory.NAMESPACE, "scan");

	public ObjectFactory() {
	}

	public ScanIndex createCache() {
		return new ScanIndex();
	}

	public ScanIndexItem createCacheItem() {
		return new ScanIndexItem();
	}

	public ScanIndexItemVersion createCacheItemVersion() {
		return new ScanIndexItemVersion();
	}

	@XmlElementDecl(namespace = ObjectFactory.NAMESPACE, name = "scan")
	public JAXBElement<ScanIndex> createScan(ScanIndex value) {
		return new JAXBElement<>(ObjectFactory._Scan_QNAME, ScanIndex.class, null, value);
	}
}