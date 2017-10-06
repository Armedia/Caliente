package com.armedia.caliente.engine.xml.extmeta;

import java.sql.Connection;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import com.armedia.caliente.store.CmfAttribute;
import com.armedia.caliente.store.CmfDataType;
import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.store.xml.CmfDataTypeAdapter;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "externalMetadataAttributeQuery.t", propOrder = {
	"names", "sql", "valueColumn", "type", "typeColumn", "multivalued"
})
public class MetadataAttributeQuery implements AttributeValuesLoader {

	@XmlElements({
		@XmlElement(name = "names-list", type = SeparatedValuesList.class),
		@XmlElement(name = "names-query", type = MetadataNamesQuery.class)
	})
	protected AttributeNamesSource names;

	// The query should accept declare multiple parameters using the ${} syntax,
	// where the following values are allowed:
	// * ${attribute} -> the name of the attribute being loaded
	// * ${att[attNameX]} -> the first value of the object's "attNameX" attribute
	// The resolver will simply track where in the SQL string these values reside, replace them with
	// ? (since we'll be using prepred statements), and track which value should go at which index
	// when populating query parameters
	@XmlElement(name = "sql", required = true)
	protected String sql;

	@XmlElement(name = "value-column", required = true)
	protected String valueColumn;

	// If no type is given, it'll be decoded from the column's SQL type
	@XmlElement(name = "type")
	@XmlJavaTypeAdapter(CmfDataTypeAdapter.class)
	protected CmfDataType type;

	@XmlElement(name = "type-column") //
	protected String typeColumn;

	// If no multivalue is given, it'll be guessed from the number of values returned
	@XmlElements({
		@XmlElement(name = "multivalued", type = Boolean.class), //
		@XmlElement(name = "multivalued-column", type = String.class), //
	})
	protected Object multivalued;

	public AttributeNamesSource getNames() {
		return this.names;
	}

	public void setNames(AttributeNamesSource names) {
		this.names = names;
	}

	public String getSql() {
		return this.sql;
	}

	public void setSql(String sql) {
		this.sql = sql;
	}

	public String getValueColumn() {
		return this.valueColumn;
	}

	public void setValueColumn(String valueColumn) {
		this.valueColumn = valueColumn;
	}

	public String getTypeColumn() {
		return this.typeColumn;
	}

	public void setTypeColumn(String typeColumn) {
		this.typeColumn = typeColumn;
		if (typeColumn != null) {
			this.type = null;
		}
	}

	public CmfDataType getType() {
		return this.type;
	}

	public void setType(CmfDataType type) {
		this.type = type;
		if (type != null) {
			this.typeColumn = null;
		}
	}

	public Object getMultivalued() {
		return this.multivalued;
	}

	public void setMultivalued(Object multivalued) {
		this.multivalued = multivalued;
	}

	@XmlTransient
	private final ReadWriteLock rwLock = new ReentrantReadWriteLock();

	@XmlTransient
	private String finalQuery = null;

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
			if (this.finalQuery != null) {
				this.finalQuery = null;
			}
		} finally {
			lock.unlock();
		}
	}
}