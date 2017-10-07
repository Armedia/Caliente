package com.armedia.caliente.engine.xml.extmeta;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.locks.Lock;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;

import com.armedia.caliente.store.CmfAttribute;
import com.armedia.caliente.store.CmfAttributeTranslator;
import com.armedia.caliente.store.CmfDataType;
import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.store.CmfValue;
import com.armedia.caliente.store.CmfValueCodec;
import com.armedia.commons.utilities.Tools;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "externalMetadataFromSQL.t", propOrder = {
	"names", "query", "transformAttributeNames", "valueColumn",
})
public class MetadataFromSQL extends MetadataReaderBase {

	@XmlElements({
		@XmlElement(name = "search-names-list", type = SeparatedValuesList.class),
		@XmlElement(name = "search-names-query", type = MetadataNamesQuery.class)
	})
	protected AttributeNamesSource names;

	@XmlElement(name = "value-column", required = true)
	protected String valueColumn;

	@XmlTransient
	private Set<String> sqlAttributeNames = null;

	public AttributeNamesSource getNames() {
		return this.names;
	}

	public void setNames(AttributeNamesSource names) {
		this.names = names;
	}

	public String getValueColumn() {
		return this.valueColumn;
	}

	public void setValueColumn(String valueColumn) {
		this.valueColumn = valueColumn;
	}

	@Override
	protected void doInitialize(Connection c) throws Exception {
		if (this.names == null) { throw new Exception("No attribute names defined for this SQL lookup"); }
		super.doInitialize(c);
		final Set<String> attributeNames = this.names.getAttributeNames(c);
		if ((attributeNames == null)
			|| attributeNames.isEmpty()) { throw new Exception("No attribute names found for this SQL lookup"); }
		this.sqlAttributeNames = Tools.freezeSet(attributeNames);
	}

	@Override
	public <V> Map<String, CmfAttribute<V>> getAttributeValues(Connection c, CmfObject<V> object) throws Exception {
		final Lock lock = this.rwLock.readLock();
		lock.lock();
		try {
			final CmfAttributeTranslator<V> translator = object.getTranslator();
			try (final PreparedStatement ps = c.prepareStatement(this.finalSql)) {
				Map<String, CmfAttribute<V>> attributes = new TreeMap<>();
				for (final String sqlAttributeName : this.sqlAttributeNames) {
					try (final ResultSet rs = getResultSet(ps, object, sqlAttributeName)) {
						CmfValueCodec<V> codec = null;
						CmfAttribute<V> attribute = new CmfAttribute<>(null);
						List<V> values = new ArrayList<>();
						while (rs.next()) {
							if (attribute == null) {
								// Deduce the type from the SQL type
								// Find the column we're interested in
								ResultSetMetaData md = rs.getMetaData();
								final int columns = md.getColumnCount();
								CmfDataType type = null;
								columnLoop: for (int i = 1; i <= columns; i++) {
									if (Tools.equals(this.valueColumn, md.getColumnName(i))) {
										switch (md.getColumnType(i)) {
											case Types.BIT:
											case Types.BOOLEAN:
												type = CmfDataType.BOOLEAN;
												break columnLoop;

											case Types.CHAR:
											case Types.CLOB:
											case Types.LONGVARCHAR:
											case Types.LONGNVARCHAR:
											case Types.VARCHAR:
											case Types.NVARCHAR:
												type = CmfDataType.STRING;
												break columnLoop;

											case Types.SMALLINT:
											case Types.TINYINT:
											case Types.INTEGER:
											case Types.BIGINT:
												type = CmfDataType.INTEGER;
												break columnLoop;

											case Types.REAL:
											case Types.FLOAT:
											case Types.DOUBLE:
											case Types.NUMERIC:
												type = CmfDataType.DOUBLE;
												break columnLoop;

											case Types.TIME:
											case Types.TIMESTAMP:
											case Types.DATE:
												type = CmfDataType.DATETIME;
												break columnLoop;

											default:
												throw new Exception(String.format(
													"Unsupported data type [%s] for column [%s] (query = %s), searching for attribute [%s]",
													md.getColumnTypeName(i), this.valueColumn, this.finalSql,
													sqlAttributeName));
										}
									}
								}

								// Assume attributes are multivalued
								String attName = transformAttributeName(sqlAttributeName);
								attribute = new CmfAttribute<>(attName, type, true);
							}
							if (codec == null) {
								codec = translator.getCodec(attribute.getType());
							}
							V finalValue = codec.getNull();
							Object value = rs.getObject(this.valueColumn);
							if (!rs.wasNull()) {
								finalValue = codec.decodeValue(new CmfValue(attribute.getType(), value));
							}
							values.add(finalValue);
						}
						if (attribute != null) {
							attributes.put(attribute.getName(), attribute);
						}
					}
				}
				if (attributes.isEmpty()) {
					// If we fetched nothing, we return null...that's the convention
					attributes = null;
				}
				return attributes;
			}
		} finally {
			lock.unlock();
		}
	}

	@Override
	protected void doClose() {
		this.sqlAttributeNames = null;
	}
}