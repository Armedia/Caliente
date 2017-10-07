package com.armedia.caliente.engine.xml.extmeta;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
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

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;

import org.apache.commons.dbutils.DbUtils;
import org.apache.commons.lang3.text.StrLookup;
import org.apache.commons.lang3.text.StrSubstitutor;

import com.armedia.caliente.engine.xml.Expression;
import com.armedia.caliente.engine.xml.Expression.ScriptContextConfig;
import com.armedia.caliente.engine.xml.ExpressionException;
import com.armedia.caliente.store.CmfAttribute;
import com.armedia.caliente.store.CmfAttributeTranslator;
import com.armedia.caliente.store.CmfDataType;
import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.store.CmfProperty;
import com.armedia.caliente.store.CmfValue;
import com.armedia.caliente.store.CmfValueCodec;
import com.armedia.commons.utilities.Tools;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "externalMetadataFromSQL.t", propOrder = {
	"names", "transformNames", "valueColumn", "query"
})
public class MetadataFromSQL implements AttributeValuesLoader {

	@XmlElements({
		@XmlElement(name = "search-names-list", type = SeparatedValuesList.class),
		@XmlElement(name = "search-names-query", type = MetadataNamesQuery.class)
	})
	protected AttributeNamesSource names;

	@XmlElement(name = "transform-names", required = false)
	protected TransformAttributeNames transformNames;

	@XmlElement(name = "value-column", required = true)
	protected String valueColumn;

	// The query should accept declare multiple parameters using the ${} syntax,
	// where the following values are allowed:
	// * ${attribute} -> the name of the attribute being loaded
	// * ${att[attNameX]} -> the first value of the object's "attNameX" attribute
	// The resolver will simply track where in the SQL string these values reside, replace them with
	// ? (since we'll be using prepred statements), and track which value should go at which index
	// when populating query parameters
	@XmlElement(name = "query", required = true)
	protected ParameterizedQuery query;

	@XmlTransient
	private final ReadWriteLock rwLock = new ReentrantReadWriteLock();

	@XmlTransient
	private String finalSql = null;

	@XmlTransient
	private Set<String> sqlAttributeNames = null;

	@XmlTransient
	private Map<String, Expression> parameterExpressions = null;

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
			Map<String, Expression> parameterExpressions = this.query.getParameterMap();
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
			this.sqlAttributeNames = Tools.freezeSet(attributeNames);
			this.finalSql = finalSql;
		} finally {
			lock.unlock();
		}
	}

	private <V> Object evaluateExpression(Expression expression, final CmfObject<V> object, final String attributeName)
		throws ExpressionException {
		if (expression == null) { return null; }
		return expression.evaluate(new ScriptContextConfig() {
			@Override
			public void configure(ScriptContext ctx) {
				final Bindings bindings = ctx.getBindings(ScriptContext.ENGINE_SCOPE);
				if (object != null) {
					CmfAttributeTranslator<V> translator = object.getTranslator();
					Map<String, Object> attributes = new HashMap<>();
					for (CmfAttribute<V> att : object.getAttributes()) {
						Object value = null;
						if (att.hasValues()) {
							CmfValueCodec<V> codec = translator.getCodec(att.getType());
							value = att.getType().getValue(codec.encodeValue(att.getValue()));
						}
						attributes.put(att.getName(), value);
					}
					Map<String, Object> properties = new HashMap<>();
					for (CmfProperty<V> prop : object.getProperties()) {
						Object value = null;
						if (prop.hasValues()) {
							CmfValueCodec<V> codec = translator.getCodec(prop.getType());
							value = prop.getType().getValue(codec.encodeValue(prop.getValue()));
						}
						attributes.put(prop.getName(), value);
					}
					bindings.put("att", attributes);
					bindings.put("prop", properties);
					bindings.put("obj", object);
				}
				bindings.put("sqlName", attributeName);
			}
		});
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
				for (final String sqlAttributeName : this.sqlAttributeNames) {
					Map<String, Object> resultCache = new HashMap<>();
					for (final String parameter : this.parameterExpressions.keySet()) {
						Expression expression = this.parameterExpressions.get(parameter);
						Object value = evaluateExpression(expression, object, sqlAttributeName);
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
								String attName = sqlAttributeName;
								if (this.transformNames != null) {
									attName = this.transformNames.transformName(sqlAttributeName);
								}
								attribute = new CmfAttribute<>(attName, type, true);
							}
							if (codec == null) {
								codec = translator.getCodec(attribute.getType());
							}
							Object value = rs.getObject(this.valueColumn);
							if (rs.wasNull()) {
								values.add(codec.getNull());
								continue;
							}

							values.add(codec.decodeValue(new CmfValue(attribute.getType(), value)));
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
				this.sqlAttributeNames.clear();
				this.sqlAttributeNames = null;
			}
		} finally {
			lock.unlock();
		}
	}
}