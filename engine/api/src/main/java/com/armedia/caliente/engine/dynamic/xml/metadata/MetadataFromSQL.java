package com.armedia.caliente.engine.dynamic.xml.metadata;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.locks.Lock;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlType;

import org.apache.commons.lang3.StringUtils;

import com.armedia.caliente.engine.dynamic.metadata.ExternalMetadataException;
import com.armedia.caliente.store.CmfAttribute;
import com.armedia.caliente.store.CmfAttributeTranslator;
import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.store.CmfValue;
import com.armedia.caliente.store.CmfValueCodec;
import com.armedia.commons.utilities.Tools;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "externalMetadataFromSQL.t", propOrder = {
	"names", "query", "valueColumn", "attributeNameMapping", "attributeTypeMapping"
})
public class MetadataFromSQL extends MetadataReaderBase {

	@XmlElements({
		@XmlElement(name = "search-names-list", type = SeparatedValuesNamesSource.class),
		@XmlElement(name = "search-names-query", type = SQLQueryNamesSource.class)
	})
	protected AttributeNamesSource names;

	@XmlElement(name = "value-column", required = true)
	protected String valueColumn;

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
	protected boolean isRequiresCaseAwareTransform() {
		return false;
	}

	@Override
	protected void doInitialize(Connection c) throws Exception {
		if (this.names == null) {
			throw new ExternalMetadataException("No attribute names defined for this SQL lookup");
		}
		super.doInitialize(c);
		this.names.initialize(c);
	}

	@Override
	public <V> Map<String, CmfAttribute<V>> getAttributeValues(Connection c, CmfObject<V> object) throws Exception {
		final Lock lock = this.rwLock.readLock();
		lock.lock();
		try {
			final CmfAttributeTranslator<V> translator = object.getTranslator();
			try (final PreparedStatement ps = c.prepareStatement(this.finalSql)) {
				Map<String, CmfAttribute<V>> attributes = new TreeMap<>();
				final String columnName = getValueColumn();
				for (final String sqlAttributeName : this.names) {
					try (final ResultSet rs = getResultSet(ps, object, sqlAttributeName)) {
						CmfValueCodec<V> codec = null;
						CmfAttribute<V> attribute = null;
						Integer columnType = null;
						int columnIndex = 0;
						int pos = 0;
						int skip = this.skip;
						final int count = this.count;
						while (rs.next()) {
							if (attribute == null) {
								// Deduce the type from the SQL type
								// Find the column we're interested in
								ResultSetMetaData md = rs.getMetaData();
								final int columns = md.getColumnCount();
								CmfValue.Type type = getMappedAttributeType(sqlAttributeName);
								for (int i = 1; i <= columns; i++) {
									String soughtColumnName = columnName;
									String thisColumnName = md.getColumnName(i);
									if (!this.columnNamesCaseSensitive) {
										// If column names aren't case sensitive, we fold to
										// uppercase for the comparison
										thisColumnName = thisColumnName.toUpperCase();
										soughtColumnName = StringUtils.upperCase(soughtColumnName);
									}
									if ((soughtColumnName == null) || Tools.equals(soughtColumnName, thisColumnName)) {
										// Here we try to decode the data type, but we also find the
										// column's index
										columnType = md.getColumnType(i);
										if (type == null) {
											type = decodeSQLType(columnType);
										}
										if (type == CmfValue.Type.OTHER) {
											throw new Exception(String.format(
												"Unsupported data type [%s] for column [%s] (query = %s), searching for attribute [%s]",
												md.getColumnTypeName(i), this.valueColumn, this.finalSql,
												sqlAttributeName));
										}
										columnIndex = i;
										break;
									}
								}

								// Assume attributes are multivalued
								String attName = transformAttributeName(sqlAttributeName);
								// By default, all attributes are multivalued...
								attribute = new CmfAttribute<>(attName, type, true);
								codec = translator.getCodec(type);
							}

							// Make sure we skip the correct number of records
							if (skip > 0) {
								--skip;
								continue;
							}

							// Increase our counter, since rs.getRow() isn't mandatory
							++pos;

							V finalValue = codec.getNull();
							// If we have a column name, use it. Otherwise, default to the first
							// column in the result set
							Object value = getValue(rs, columnIndex, attribute.getType());
							if (!rs.wasNull()) {
								finalValue = codec.decodeValue(new CmfValue(attribute.getType(), value));
							}
							attribute.addValue(finalValue);

							// If we're count-limited, and our position matches or exceeds our
							// count, we stop processing records
							if ((count > 0) && (pos >= count)) {
								break;
							}
						}
						// Only include attributes with values...
						if ((attribute != null) && attribute.hasValues()) {
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
		this.names.close();
		this.names = null;
		super.doClose();
	}
}