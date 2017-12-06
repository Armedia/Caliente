package com.armedia.caliente.store.jdbc;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.dbutils.ResultSetHandler;

import com.armedia.caliente.store.CmfDataType;
import com.armedia.caliente.store.CmfStorageException;
import com.armedia.caliente.store.CmfValue;
import com.armedia.caliente.store.CmfValueSerializer;
import com.armedia.commons.utilities.Tools;

public class JdbcStorePropertyManager {

	private static enum Op {
		//
		INSERT("insert into %s (name, data_type, value) values (?, ?, ?)"),
		GET("select * from %s where name = ?"),
		UPDATE("update %s set value = ? where name = ?"),
		DELETE("delete from %s where name = ?"),
		GET_NAMES("select name from %s order by name"),
		DELETE_ALL("delete from %s"),
		//
		;

		private final String sql;

		Op(String sql) {
			this.sql = sql;
		}
	}

	private final Map<Op, String> sql;

	JdbcStorePropertyManager(String table) {
		if (table == null) { throw new IllegalArgumentException(
			"Must provide the name of the table holding the properties"); }
		Map<Op, String> sql = new EnumMap<>(Op.class);
		for (Op op : Op.values()) {
			sql.put(op, String.format(op.sql, table));
		}
		this.sql = Tools.freezeMap(sql);
	}

	private String getSql(Op op) {
		return this.sql.get(op);
	}

	CmfValue getProperty(JdbcOperation operation, String property) throws CmfStorageException {
		final Connection c = operation.getConnection();
		try {
			return JdbcTools.getQueryRunner().query(c, getSql(Op.GET), new ResultSetHandler<CmfValue>() {
				@Override
				public CmfValue handle(ResultSet rs) throws SQLException {
					if (!rs.next()) { return null; }
					String name = rs.getString("name");
					String type = rs.getString("data_type");
					final CmfDataType t;

					try {
						t = CmfDataType.valueOf(type);
					} catch (IllegalArgumentException e) {
						throw new SQLException(String.format("Unsupported data type name: [%s]", type), e);
					}
					final CmfValueSerializer deserializer = CmfValueSerializer.get(t);
					if (deserializer == null) { throw new SQLException(
						String.format("Unsupported data type name for serialization: [%s]", type)); }
					String value = rs.getString("value");
					try {
						return deserializer.deserialize(value);
					} catch (ParseException e) {
						throw new SQLException(
							String.format("Failed to deserialize store property [%s]:[%s] as a %s", name, value, type),
							e);
					}
				}
			}, property);
		} catch (SQLException e) {
			throw new CmfStorageException(
				String.format("Failed to retrieve the value of store property [%s]", property), e);
		}
	}

	CmfValue setProperty(JdbcOperation operation, String property, final CmfValue newValue) throws CmfStorageException {
		final CmfValue oldValue = getProperty(operation, property);
		final Connection c = operation.getConnection();
		final CmfValueSerializer serializer = CmfValueSerializer.get(newValue.getDataType());
		final String newValueString;
		try {
			newValueString = serializer.serialize(newValue);
		} catch (ParseException e) {
			throw new CmfStorageException(
				String.format("Failed to serialize the value [%s] for the store property [%s]", newValue, property));
		}
		try {
			if (oldValue != null) {
				int n = JdbcTools.getQueryRunner().update(c, getSql(Op.UPDATE), newValueString, property);
				if (n != 1) { throw new CmfStorageException(
					String.format("Failed to properly update store property [%s] - updated %d values instead of just 1",
						property, n)); }
			} else {
				JdbcTools.getQueryRunner().insert(c, getSql(Op.INSERT), JdbcTools.HANDLER_NULL, property,
					newValue.getDataType().name(), newValueString);
			}
		} catch (SQLException e) {
			throw new CmfStorageException(
				String.format("Failed to set the value of store property [%s] to [%s]", property, newValueString), e);
		}
		return oldValue;
	}

	Set<String> getPropertyNames(JdbcOperation operation) throws CmfStorageException {
		final Connection c = operation.getConnection();
		try {
			return JdbcTools.getQueryRunner().query(c, getSql(Op.GET_NAMES), new ResultSetHandler<Set<String>>() {
				@Override
				public Set<String> handle(ResultSet rs) throws SQLException {
					Set<String> ret = new TreeSet<>();
					while (rs.next()) {
						ret.add(rs.getString("name"));
					}
					return ret;
				}
			});
		} catch (SQLException e) {
			throw new CmfStorageException("Failed to retrieve the store property names", e);
		}
	}

	CmfValue clearProperty(JdbcOperation operation, String property) throws CmfStorageException {
		final CmfValue oldValue = getProperty(operation, property);
		final Connection c = operation.getConnection();
		try {
			JdbcTools.getQueryRunner().update(c, getSql(Op.DELETE), property);
		} catch (SQLException e) {
			throw new CmfStorageException(String.format("Failed to delete the store property [%s]", property), e);
		}
		return oldValue;
	}

	void clearProperties(JdbcOperation operation) throws CmfStorageException {
		try {
			clearProperties(operation.getConnection());
		} catch (SQLException e) {
			throw new CmfStorageException("Failed to delete all the store properties", e);
		}
	}

	void clearProperties(Connection c) throws SQLException {
		JdbcTools.getQueryRunner().update(c, getSql(Op.DELETE_ALL));
	}
}