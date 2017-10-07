package com.armedia.caliente.engine.xml.extmeta;

import java.sql.Connection;
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
@XmlType(name = "externalMetadataFromDDL.t", propOrder = {
	"query", "ignore", "transformAttributeNames"
})
public class MetadataFromDDL implements AttributeValuesLoader {

	@XmlElement(name = "query", required = true)
	protected String query;

	@XmlElement(name = "ignore-columns", required = false)
	protected SeparatedValuesList ignore;

	@XmlElement(name = "transform-column-names", required = true)
	protected TransformAttributeNames transformAttributeNames;

	@XmlTransient
	private final ReadWriteLock rwLock = new ReentrantReadWriteLock();

	public String getQuery() {
		return this.query;
	}

	public void setQuery(String value) {
		this.query = value;
	}

	public SeparatedValuesList getIgnore() {
		return this.ignore;
	}

	public void setIgnore(SeparatedValuesList value) {
		this.ignore = value;
	}

	public TransformAttributeNames getTransformNames() {
		return this.transformAttributeNames;
	}

	public void setTransformNames(TransformAttributeNames value) {
		this.transformAttributeNames = value;
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