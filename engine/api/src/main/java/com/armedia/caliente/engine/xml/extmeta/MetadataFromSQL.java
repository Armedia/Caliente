package com.armedia.caliente.engine.xml.extmeta;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;

import com.armedia.caliente.store.CmfAttribute;
import com.armedia.caliente.store.CmfObject;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "externalMetadataFromSQL.t", propOrder = {
	"attributeQueries"
})
public class MetadataFromSQL implements AttributeValuesLoader {

	@XmlElement(name = "attributes", required = false)
	protected List<MetadataAttributeQuery> attributeQueries;

	@XmlTransient
	private final ReadWriteLock rwLock = new ReentrantReadWriteLock();

	public List<MetadataAttributeQuery> getAttributeQueries() {
		if (this.attributeQueries == null) {
			this.attributeQueries = new ArrayList<>();
		}
		return this.attributeQueries;
	}

	@Override
	public void initialize(Connection c) throws Exception {
		final Lock lock = this.rwLock.writeLock();
		lock.lock();
		try {

		} finally {
			lock.unlock();
		}
	}

	@Override
	public <V> Map<String, CmfAttribute<V>> getAttributeValues(Connection c, CmfObject<V> object) throws Exception {
		final Lock lock = this.rwLock.readLock();
		lock.lock();
		try {
			return null;
		} finally {
			lock.unlock();
		}
	}

	@Override
	public void close() {
		final Lock lock = this.rwLock.writeLock();
		lock.lock();
		try {

		} finally {
			lock.unlock();
		}
	}
}