package com.armedia.caliente.engine.xml.extmeta;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;

import com.armedia.caliente.store.CmfAttribute;
import com.armedia.caliente.store.CmfAttributeTranslator;
import com.armedia.caliente.store.CmfBaseSetting;
import com.armedia.caliente.store.CmfDataType;
import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.store.CmfValue;
import com.armedia.caliente.store.CmfValueCodec;
import com.armedia.commons.utilities.Tools;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "externalMetadataFromDDL.t", propOrder = {
	"query", "ignore", "transformAttributeNames"
})
public class MetadataFromDDL extends MetadataReaderBase {

	@XmlElement(name = "ignore-columns", required = false)
	protected SeparatedValuesNamesSource ignore;

	@XmlTransient
	private volatile Map<String, CmfBaseSetting> structure = null;

	public SeparatedValuesNamesSource getIgnore() {
		return this.ignore;
	}

	public void setIgnore(SeparatedValuesNamesSource value) {
		this.ignore = value;
	}

	@Override
	protected void doInitialize(Connection c) throws Exception {
		if (this.ignore != null) {
			this.ignore.initialize(c);
		}
	}

	private Map<String, CmfBaseSetting> getStructure(ResultSetMetaData md) throws Exception {
		Map<String, CmfBaseSetting> structure = null;
		final int columns = md.getColumnCount();
		columnLoop: for (int i = 1; i <= columns; i++) {
			if (structure == null) {
				structure = new HashMap<>();
			}
			String sqlName = md.getColumnName(i);
			if ((this.ignore != null) && this.ignore.contains(sqlName)) {
				continue columnLoop;
			}

			String finalName = sqlName;
			if (this.transformAttributeNames != null) {
				finalName = this.transformAttributeNames.transformName(sqlName);
			}

			CmfDataType type = decodeSQLType(md.getColumnType(i));
			if (type == CmfDataType.OTHER) {
				continue columnLoop;
			}
			structure.put(sqlName, new CmfBaseSetting(finalName, type, true));
		}
		return Tools.freezeMap(structure, true);

	}

	@Override
	public <V> Map<String, CmfAttribute<V>> getAttributeValues(Connection c, CmfObject<V> object) throws Exception {
		final Lock readLock = this.rwLock.readLock();
		readLock.lock();
		try {
			final CmfAttributeTranslator<V> translator = object.getTranslator();
			try (final PreparedStatement ps = c.prepareStatement(this.finalSql)) {
				try (final ResultSet rs = getResultSet(ps, object, null)) {

					// Do we have a structure defined yet?
					if (this.structure == null) {
						// Step 1: upgrade the read lock to a write lock
						final Lock writeLock = this.rwLock.writeLock();
						readLock.unlock();
						writeLock.lock();
						try {
							// Step 2: check again if initialization is needed
							if (this.structure == null) {
								// Step 3: initialize!
								this.structure = getStructure(rs.getMetaData());
							}
						} finally {
							// Step 4: downgrade to a read lock
							// We downgrade in the finally clause because we have to be holding the
							// read lock so the outer code releases it accordingly
							readLock.lock();
							writeLock.unlock();
						}
					}

					Map<String, CmfAttribute<V>> tempAtts = new HashMap<>();
					while (rs.next()) {
						for (String column : this.structure.keySet()) {
							CmfAttribute<V> attribute = tempAtts.get(column);
							if (attribute == null) {
								CmfBaseSetting base = this. structure.get(column);
								attribute = new CmfAttribute<>(base.getName(), base.getType(), base.isRepeating());
								tempAtts.put(column, attribute);
							}

							CmfValueCodec<V> codec = translator.getCodec(attribute.getType());
							V finalValue = codec.getNull();
							Object value = rs.getObject(column);
							if (!rs.wasNull()) {
								finalValue = codec.decodeValue(new CmfValue(attribute.getType(), value));
							}
							attribute.addValue(finalValue);
						}
					}

					Map<String, CmfAttribute<V>> attributes = new HashMap<>();
					for (String s : tempAtts.keySet()) {
						CmfAttribute<V> attribute = tempAtts.get(s);
						attributes.put(attribute.getName(), attribute);
					}
					if (attributes.isEmpty()) {
						// If we fetched nothing, we return null...that's the convention
						attributes = null;
					}
					return attributes;
				}
			}
		} finally {
			readLock.unlock();
		}
	}

	@Override
	protected void doClose() {
		this.structure = null;
		if (this.ignore != null) {
			try {
				this.ignore.close();
			} finally {
				this.ignore = null;
			}
		}
	}

}