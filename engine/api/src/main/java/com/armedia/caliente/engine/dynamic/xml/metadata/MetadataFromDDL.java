package com.armedia.caliente.engine.dynamic.xml.metadata;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.HashMap;
import java.util.Map;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;

import com.armedia.caliente.store.CmfAttribute;
import com.armedia.caliente.store.CmfAttributeTranslator;
import com.armedia.caliente.store.CmfBaseSetting;
import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.store.CmfValue;
import com.armedia.caliente.store.CmfValueCodec;
import com.armedia.commons.utilities.Tools;
import com.armedia.commons.utilities.concurrent.SharedAutoLock;
import com.armedia.commons.utilities.function.CheckedLazySupplier;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "externalMetadataFromDDL.t", propOrder = {
	"query", "ignore", "attributeNameMapping", "attributeTypeMapping"
})
public class MetadataFromDDL extends MetadataReaderBase {

	private static class ColumnStructure extends CmfBaseSetting {
		private final int sqlType;
		private final String sqlTypeName;

		private ColumnStructure(String name, CmfValue.Type type, boolean repeating, int sqlType, String sqlTypeName) {
			super(name, type, repeating);
			this.sqlType = sqlType;
			this.sqlTypeName = sqlTypeName;
		}
	}

	@XmlElement(name = "ignore-columns", required = false)
	protected SeparatedValuesNamesSource ignore;

	@XmlTransient
	private final CheckedLazySupplier<Map<String, ColumnStructure>, Exception> structure = new CheckedLazySupplier<>();

	public SeparatedValuesNamesSource getIgnore() {
		return this.ignore;
	}

	public void setIgnore(SeparatedValuesNamesSource value) {
		this.ignore = value;
	}

	@Override
	protected void doInitialize(Connection c) throws Exception {
		super.doInitialize(c);
		if (this.ignore != null) {
			this.ignore.setCaseSensitive(this.columnNamesCaseSensitive);
			this.ignore.initialize(c);
		}
		if (this.attributeNameMapping != null) {
			this.attributeNameMapping.initialize(this.columnNamesCaseSensitive);
		}
	}

	@Override
	protected boolean isRequiresCaseAwareTransform() {
		return true;
	}

	private Map<String, ColumnStructure> getStructure(ResultSetMetaData md) throws Exception {
		Map<String, ColumnStructure> structure = null;
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
			if (this.attributeNameMapping != null) {
				finalName = this.attributeNameMapping.transformName(sqlName);
			}

			CmfValue.Type type = getMappedAttributeType(sqlName);
			if (type == null) {
				type = decodeSQLType(md.getColumnType(i));
			}
			if (type == CmfValue.Type.OTHER) {
				continue columnLoop;
			}
			structure.put(sqlName,
				new ColumnStructure(finalName, type, true, md.getColumnType(i), md.getColumnTypeName(i)));
		}
		return Tools.freezeMap(structure, true);

	}

	@Override
	public <V> Map<String, CmfAttribute<V>> getAttributeValues(Connection c, CmfObject<V> object) throws Exception {
		try (SharedAutoLock lock = autoSharedLock()) {
			final CmfAttributeTranslator<V> translator = object.getTranslator();
			try (final PreparedStatement ps = c.prepareStatement(this.finalSql)) {
				try (final ResultSet rs = getResultSet(ps, object, null)) {

					// Do we have a structure defined yet?
					Map<String, ColumnStructure> localStructure = this.structure
						.getChecked(() -> getStructure(rs.getMetaData()));

					int pos = 0;
					int skip = this.skip;
					final int count = this.count;
					Map<String, CmfAttribute<V>> tempAtts = new HashMap<>();
					while (rs.next()) {
						// Make sure we skip the correct number of records
						if (skip > 0) {
							--skip;
							continue;
						}

						// Increase our counter, since rs.getRow() isn't mandatory
						++pos;

						for (String column : localStructure.keySet()) {
							final ColumnStructure structure = localStructure.get(column);

							Tools.equals(structure.sqlType, structure.sqlTypeName); // to disable a
																					// warning...
							CmfAttribute<V> attribute = tempAtts.get(column);
							if (attribute == null) {
								attribute = new CmfAttribute<>(structure.getName(), structure.getType(),
									structure.isMultivalued());
								tempAtts.put(column, attribute);
							}

							CmfValueCodec<V> codec = translator.getCodec(attribute.getType());
							V finalValue = codec.getNullValue();
							Object value = getValue(rs, column, attribute.getType());
							// TODO: Use the structure information to determine if we need to
							// deserialize the value or not... should we also make this bit
							// configurable?
							if (!rs.wasNull()) {
								finalValue = codec.decode(new CmfValue(attribute.getType(), value));
							}
							attribute.addValue(finalValue);
						}

						// If we're count-limited, and our position matches or exceeds our count, we
						// stop processing records
						if ((count > 0) && (pos >= count)) {
							break;
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
		}
	}

	@Override
	protected void doClose() {
		if (this.ignore != null) {
			try {
				this.ignore.close();
			} finally {
				this.ignore = null;
			}
		}
		super.doClose();
	}

}