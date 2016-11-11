package com.armedia.caliente.engine.alfresco.bulk.importer.cache;

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

	public Cache createCache() {
		return new Cache();
	}

	public CacheItem createCacheItem() {
		return new CacheItem();
	}

	public CacheItemVersion createCacheItemVersion() {
		return new CacheItemVersion();
	}

	@XmlElementDecl(namespace = ObjectFactory.NAMESPACE, name = "scan")
	public JAXBElement<Cache> createScan(Cache value) {
		return new JAXBElement<>(ObjectFactory._Scan_QNAME, Cache.class, null, value);
	}
}