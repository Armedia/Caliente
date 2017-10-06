package com.armedia.caliente.engine.xml.extmeta;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.script.ScriptException;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import org.apache.commons.dbutils.DbUtils;
import org.apache.commons.lang3.text.StrLookup;
import org.apache.commons.lang3.text.StrSubstitutor;

import com.armedia.caliente.store.CmfAttribute;
import com.armedia.caliente.store.CmfAttributeTranslator;
import com.armedia.caliente.store.CmfDataType;
import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.store.CmfValue;
import com.armedia.caliente.store.CmfValueCodec;
import com.armedia.caliente.store.xml.CmfDataTypeAdapter;
import com.armedia.commons.utilities.Tools;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "externalMetadataFromSQL.t", propOrder = {
	"names", "query", "valueColumn", "type", "typeColumn", "multivalued"
})
public class MetadataFromSQL implements AttributeValuesLoader {

	@XmlElements({
		@XmlElement(name = "search-names-list", type = SeparatedValuesList.class),
		@XmlElement(name = "search-names-query", type = MetadataNamesQuery.class)
	})
	protected AttributeNamesSource names;

	// The query should accept declare multiple parameters using the ${} syntax,
	// where the following values are allowed:
	// * ${attribute} -> the name of the attribute being loaded
	// * ${att[attNameX]} -> the first value of the object's "attNameX" attribute
	// The resolver will simply track where in the SQL string these values reside, replace them with
	// ? (since we'll be using prepred statements), and track which value should go at which index
	// when populating query parameters
	@XmlElement(name = "query", required = true)
	protected ParameterizedQuery query;

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

	@XmlTransient
	private final ReadWriteLock rwLock = new ReentrantReadWriteLock();

	@XmlTransient
	private String finalSql = null;

	@XmlTransient
	private Set<String> attributeNames = null;

	@XmlTransient
	private Map<String, String> parameterExpressions = null;

	@XmlTransient
	private Map<Integer, String> indexedNames = null;

	public AttributeNamesSource getNames() {
		return this.names;
	}

	public void setNames(AttributeNamesSource names) {
		this.names = names;
	}

	public ParameterizedQuery getQuery() {
		return this.query;
	}

	public void setQuery(ParameterizedQuery query) {
		this.query = query;
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

	@Override
	public void initialize(Connection c) throws Exception {
		final Lock lock = this.rwLock.writeLock();
		lock.lock();
		try {
			if (this.finalSql != null) { return; }
			if (this.names == null) { throw new Exception("No attribute names defined for this SQL lookup"); }
			if (this.query == null) { throw new Exception("No query defined for this SQL lookup"); }

			final Set<String> attributeNames = this.names.getAttributeNames(c);
			if ((attributeNames == null)
				|| attributeNames.isEmpty()) { throw new Exception("No attribute names found for this SQL lookup"); }

			// Step 1: make an index of the referenced parameters...
			// Here we use a list instead of a set because the parameter's position on the list is
			// relevant, and we can have one parameter referenced multiple times
			final List<String> referencedParameters = new ArrayList<>();
			final String finalSql = new StrSubstitutor(new StrLookup<Object>() {
				@Override
				public String lookup(String key) {
					referencedParameters.add(key);
					return "?";
				}
			}).replace(this.query.getSql());
			// Also, compile it just to make absolutely sure that it's a valid SQL query with all
			// the substitutions and whatnot. We can't store the PS b/c it's connection-specific,
			// and we can't assume we'll ever use this specific connection object again
			DbUtils.closeQuietly(c.prepareStatement(finalSql));

			// Step 2: make sure all the indexed parameters have expressions defined for their
			// assignment
			Map<String, String> parameterExpressions = this.query.getParameterMap();
			Set<String> missing = new LinkedHashSet<>();
			int i = 0;
			Map<Integer, String> indexedNames = new TreeMap<>();
			for (String p : referencedParameters) {
				if (parameterExpressions.containsKey(p)) {
					indexedNames.put(++i, p);
				} else {
					// We have a problem!
					missing.add(p);
				}
			}
			if (!missing.isEmpty()) { throw new Exception(String.format(
				"The given SQL query references the following parameters that have no expression associated: %s",
				missing)); }

			this.parameterExpressions = Tools.freezeMap(parameterExpressions);
			this.indexedNames = Tools.freezeMap(indexedNames);
			this.attributeNames = Tools.freezeSet(attributeNames);
			this.finalSql = finalSql;
		} finally {
			lock.unlock();
		}
	}

	private <V> Object evaluateJexlExpression(String expression, CmfObject<V> object, String attributeName)
		throws ScriptException {
		return null;
	}

	@Override
	public <V> Map<String, CmfAttribute<V>> getAttributeValues(Connection c, CmfObject<V> object) throws Exception {
		final Lock lock = this.rwLock.readLock();
		lock.lock();
		try {
			final CmfAttributeTranslator<V> translator = object.getTranslator();
			final PreparedStatement ps = c.prepareStatement(this.finalSql);
			try {
				Map<String, CmfAttribute<V>> attributes = new TreeMap<>();
				for (final String attributeName : this.attributeNames) {
					Map<String, Object> resultCache = new HashMap<>();
					for (final String parameter : this.parameterExpressions.keySet()) {
						String expression = this.parameterExpressions.get(parameter);
						Object value = evaluateJexlExpression(expression, object, attributeName);
						resultCache.put(parameter, value);
					}
					for (final Integer i : this.indexedNames.keySet()) {
						final String name = this.indexedNames.get(i);
						final Object value = resultCache.get(name);
						if (value == null) {
							ps.setNull(i, Types.NULL);
						} else {
							ps.setObject(i, value);
						}
					}

					ResultSet rs = ps.executeQuery();
					try {
						CmfValueCodec<V> codec = null;
						CmfAttribute<V> attribute = new CmfAttribute<>(null);
						List<V> values = new ArrayList<>();
						while (rs.next()) {
							if (attribute == null) {
								// TODO: Identify the type, whether it's multivalued, and transform
								// the SQL name to an attribute name
							}
							if (codec == null) {
								codec = translator.getCodec(attribute.getType());
							}
							Object value = rs.getObject(this.valueColumn);
							if (rs.wasNull()) {
								values.add(codec.getNull());
								continue;
							}

							CmfValue cmfValue = new CmfValue(attribute.getType(), value);
							values.add(codec.decodeValue(cmfValue));
						}
						if (attribute != null) {
							attributes.put(attribute.getName(), attribute);
						}
					} finally {
						DbUtils.closeQuietly(rs);
					}
				}
				if (attributes.isEmpty()) {
					// If we fetched nothing, we return null...that's the convention
					attributes = null;
				}
				return attributes;
			} finally {
				DbUtils.closeQuietly(ps);
			}
		} finally {
			lock.unlock();
		}
	}

	@Override
	public void close() {
		final Lock lock = this.rwLock.writeLock();
		lock.lock();
		try {
			if (this.finalSql != null) {
				this.finalSql = null;
				this.indexedNames.clear();
				this.indexedNames = null;
				this.parameterExpressions.clear();
				this.parameterExpressions = null;
				this.attributeNames.clear();
				this.attributeNames = null;
			}
		} finally {
			lock.unlock();
		}
	}
}