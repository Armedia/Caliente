package com.armedia.caliente.engine.dynamic.xml.metadata;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
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

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptException;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlTransient;

import org.apache.commons.dbutils.DbUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.text.StringSubstitutor;

import com.armedia.caliente.engine.dynamic.metadata.ExternalMetadataException;
import com.armedia.caliente.engine.dynamic.xml.Expression;
import com.armedia.caliente.engine.dynamic.xml.Expression.ScriptContextConfig;
import com.armedia.caliente.store.CmfAttribute;
import com.armedia.caliente.store.CmfAttributeTranslator;
import com.armedia.caliente.store.CmfDataType;
import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.store.CmfProperty;
import com.armedia.caliente.store.CmfValueCodec;
import com.armedia.commons.utilities.Tools;

@XmlTransient
public abstract class MetadataReaderBase implements AttributeValuesLoader {

	@XmlElement(name = "query", required = true)
	protected ParameterizedQuery query;

	@XmlElement(name = "transform-names", required = false)
	protected AttributeNameMapping attributeNameMapping;

	@XmlElement(name = "attribute-types", required = false)
	protected AttributeTypeMapping attributeTypeMapping;

	@XmlAttribute(name = "dataSource", required = true)
	protected String dataSource;

	@XmlTransient
	protected final ReadWriteLock rwLock = new ReentrantReadWriteLock();

	@XmlTransient
	protected String finalSql = null;

	@XmlTransient
	protected int skip = 0;

	@XmlTransient
	protected int count = 0;

	@XmlTransient
	protected Map<String, Expression> parameterExpressions = null;

	@XmlTransient
	protected Map<Integer, String> indexedNames = null;

	@XmlTransient
	protected Boolean columnNamesCaseSensitive = false;

	@Override
	public final String getDataSource() {
		return this.dataSource;
	}

	public final void setDataSource(String dataSource) {
		this.dataSource = dataSource;
	}

	public final ParameterizedQuery getQuery() {
		return this.query;
	}

	public final void setQuery(ParameterizedQuery value) {
		this.query = value;
	}

	public final AttributeNameMapping getAttributeNameMapping() {
		return this.attributeNameMapping;
	}

	public final void setAttributeNameMapping(AttributeNameMapping value) {
		this.attributeNameMapping = value;
	}

	protected final String transformAttributeName(String name) throws ScriptException {
		if (this.attributeNameMapping == null) { return name; }
		return this.attributeNameMapping.transformName(name);
	}

	public AttributeTypeMapping getAttributeTypeMapping() {
		return this.attributeTypeMapping;
	}

	public void setAttributeTypeMapping(AttributeTypeMapping attributeTypeMapping) {
		this.attributeTypeMapping = attributeTypeMapping;
	}

	protected CmfDataType getMappedAttributeType(String sqlAttributeName) {
		if (this.attributeTypeMapping == null) { return null; }
		return this.attributeTypeMapping.getMappedType(sqlAttributeName);
	}

	protected void doInitialize(Connection c) throws Exception {
		if (this.query == null) { throw new ExternalMetadataException("No query defined for this SQL lookup"); }
		// Step 1: make an index of the referenced parameters...
		// Here we use a list instead of a set because the parameter's position on the list is
		// relevant, and we can have one parameter referenced multiple times
		final List<String> referencedParameters = new ArrayList<>();
		final String finalSql = new StringSubstitutor((key) -> {
			referencedParameters.add(key);
			return "?";
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
		if (!missing.isEmpty()) {
			throw new ExternalMetadataException(String.format(
				"The given SQL query references the following parameters that have no expression associated: %s",
				missing));
		}

		DatabaseMetaData md = c.getMetaData();
		this.columnNamesCaseSensitive = md.supportsMixedCaseIdentifiers();
		if (this.attributeNameMapping != null) {
			if (isRequiresCaseAwareTransform()) {
				this.attributeNameMapping.initialize(this.columnNamesCaseSensitive);
			} else {
				this.attributeNameMapping.initialize(null);
			}
		}
		this.parameterExpressions = Tools.freezeMap(parameterExpressions);
		this.indexedNames = Tools.freezeMap(indexedNames);
		this.finalSql = finalSql;
		this.skip = this.query.getSkip();
		this.count = this.query.getCount();
	}

	protected abstract boolean isRequiresCaseAwareTransform();

	@Override
	public final void initialize(Connection c) throws Exception {
		final Lock lock = this.rwLock.writeLock();
		lock.lock();
		try {
			if (this.finalSql != null) { return; }
			doInitialize(c);
		} finally {
			lock.unlock();
		}
	}

	protected final <V> Object evaluateExpression(Expression expression, final CmfObject<V> object,
		final String sqlName) throws ScriptException {
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
				if (sqlName != null) {
					bindings.put("sqlName", sqlName);
				}
			}
		});
	}

	protected final <V> ResultSet getResultSet(PreparedStatement ps, CmfObject<V> object, String sqlAttributeName)
		throws Exception {
		Map<String, Object> resultCache = new HashMap<>();
		for (final String parameter : this.parameterExpressions.keySet()) {
			Expression expression = this.parameterExpressions.get(parameter);
			Object result = evaluateExpression(expression, object, sqlAttributeName);
			resultCache.put(parameter, result);
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
		return ps.executeQuery();
	}

	protected final Object getValue(ResultSet rs, String columnName, CmfDataType type) throws Exception {
		switch (type) {
			case BOOLEAN:
				return rs.getBoolean(columnName);

			case DATETIME:
				return rs.getDate(columnName);

			case DOUBLE:
				return rs.getDouble(columnName);

			case INTEGER:
				return rs.getLong(columnName);

			case URI:
			case HTML:
			case ID:
			case STRING:
				return rs.getString(columnName);

			case BASE64_BINARY:
				try (InputStream in = rs.getBinaryStream(columnName)) {
					if (in == null) { return null; }
					return IOUtils.toByteArray(in);
				}

			default:
				throw new Exception(String.format("Unsupported data type %s for column %s", type.name(), columnName));
		}
	}

	protected final Object getValue(ResultSet rs, int columnIndex, CmfDataType type) throws Exception {
		switch (type) {
			case BOOLEAN:
				return rs.getBoolean(columnIndex);

			case DATETIME:
				return rs.getDate(columnIndex);

			case DOUBLE:
				return rs.getDouble(columnIndex);

			case INTEGER:
				return rs.getLong(columnIndex);

			case URI:
			case HTML:
			case ID:
			case STRING:
				return rs.getString(columnIndex);

			case BASE64_BINARY:
				try (InputStream in = rs.getBinaryStream(columnIndex)) {
					if (in == null) { return null; }
					return IOUtils.toByteArray(in);
				}

			default:
				throw new Exception(String.format("Unsupported data type %s for column %s", type.name(), columnIndex));
		}
	}

	protected final CmfDataType decodeSQLType(int type) {
		switch (type) {
			case Types.BIT:
			case Types.BOOLEAN:
				return CmfDataType.BOOLEAN;

			case Types.CHAR:
			case Types.CLOB:
			case Types.LONGVARCHAR:
			case Types.LONGNVARCHAR:
			case Types.VARCHAR:
			case Types.NVARCHAR:
				return CmfDataType.STRING;

			case Types.SMALLINT:
			case Types.TINYINT:
			case Types.INTEGER:
			case Types.BIGINT:
				return CmfDataType.INTEGER;

			case Types.REAL:
			case Types.FLOAT:
			case Types.DOUBLE:
			case Types.NUMERIC:
				return CmfDataType.DOUBLE;

			case Types.TIME:
			case Types.TIMESTAMP:
			case Types.DATE:
				return CmfDataType.DATETIME;

			default:
				return CmfDataType.OTHER;
		}
	}

	protected void doClose() {
		if (this.attributeNameMapping != null) {
			this.attributeNameMapping.close();
		}
		this.columnNamesCaseSensitive = null;
		this.parameterExpressions = null;
		this.indexedNames = null;
		this.finalSql = null;
		this.skip = 0;
		this.count = 0;
	}

	@Override
	public final void close() {
		final Lock lock = this.rwLock.writeLock();
		lock.lock();
		try {
			if (this.finalSql != null) {
				doClose();
			}
		} finally {
			lock.unlock();
		}
	}

}