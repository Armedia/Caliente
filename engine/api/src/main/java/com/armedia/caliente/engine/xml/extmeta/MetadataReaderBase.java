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

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlTransient;

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
import com.armedia.caliente.store.CmfValueCodec;
import com.armedia.commons.utilities.Tools;

@XmlTransient
public abstract class MetadataReaderBase implements AttributeValuesLoader {

	@XmlElement(name = "query", required = true)
	protected ParameterizedQuery query;

	@XmlElement(name = "transform-names", required = true)
	protected TransformAttributeNames transformAttributeNames;

	@XmlTransient
	protected final ReadWriteLock rwLock = new ReentrantReadWriteLock();

	@XmlTransient
	protected String finalSql = null;

	@XmlTransient
	protected Map<String, Expression> parameterExpressions = null;

	@XmlTransient
	protected Map<Integer, String> indexedNames = null;

	public final ParameterizedQuery getQuery() {
		return this.query;
	}

	public final void setQuery(ParameterizedQuery value) {
		this.query = value;
	}

	public final TransformAttributeNames getTransformNames() {
		return this.transformAttributeNames;
	}

	public final void setTransformNames(TransformAttributeNames value) {
		this.transformAttributeNames = value;
	}

	protected final String transformAttributeName(String name) throws ExpressionException {
		if (this.transformAttributeNames == null) { return name; }
		return this.transformAttributeNames.transformName(name);
	}

	protected void doInitialize(Connection c) throws Exception {
		if (this.query == null) { throw new Exception("No query defined for this SQL lookup"); }
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
		this.finalSql = finalSql;
	}

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
		final String sqlName) throws ExpressionException {
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
		return ps.executeQuery();
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
		this.parameterExpressions = null;
		this.indexedNames = null;
		this.finalSql = null;
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