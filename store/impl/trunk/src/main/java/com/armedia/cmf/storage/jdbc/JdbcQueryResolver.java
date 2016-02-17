package com.armedia.cmf.storage.jdbc;

import org.apache.commons.dbutils.ResultSetHandler;
import org.apache.commons.lang3.StringUtils;

import com.armedia.cmf.storage.CmfStorageException;
import com.armedia.commons.utilities.Tools;

public abstract class JdbcQueryResolver {

	public static enum EngineType {
		//
		H2, PostgreSQL,
		//
		;

		private boolean matches(String dbName) {
			if (dbName == null) { throw new IllegalArgumentException("Must provide a string to check against"); }
			return StringUtils.equalsIgnoreCase(name(), dbName);
		}

		private static EngineType parse(String dbName) throws CmfStorageException {
			if (dbName == null) { throw new IllegalArgumentException("Must provide a DB Name to check against"); }
			for (EngineType t : EngineType.values()) {
				if (t.matches(dbName)) { return t; }
			}
			throw new CmfStorageException(String.format("DB Type [%s] is unsupported", dbName));
		}
	}

	public final String dbName;
	public final int dbMajor;
	public final int dbMinor;
	public final String dbVersion;
	public final EngineType engineType;

	protected JdbcQueryResolver(EngineType engineType, String dbName, int dbMajor, int dbMinor, String dbVersion) {
		this.engineType = engineType;
		this.dbName = dbName;
		this.dbMajor = dbMajor;
		this.dbMinor = dbMinor;
		this.dbVersion = dbVersion;
	}

	protected abstract boolean isSupportsArrays();

	protected abstract ResultSetHandler<Long> getObjectNumberHandler();

	final String resolveSql(JdbcQuery query, boolean required) {
		if (query == null) { throw new IllegalArgumentException("Must provide a SQL query to resolve"); }
		String sql = doResolve(query);
		if (required) {
			sql = Tools.coalesce(sql, query.sql);
			if (sql == null) { throw new IllegalStateException(
				String.format("Required query [%s] is missing", query)); }
		}
		return sql;
	}

	protected String doResolve(JdbcQuery sql) {
		return sql.sql;
	}

	public static JdbcQueryResolver getResolver(String dbName, int dbMajor, int dbMinor, String dbVersion)
		throws CmfStorageException {
		EngineType type = EngineType.parse(dbName);

		switch (type) {
			case H2:
				return new JdbcQueryResolverH2(dbName, dbMajor, dbMinor, dbVersion);
			case PostgreSQL:
				return new JdbcQueryResolverPostgreSQL(dbName, dbMajor, dbMinor, dbVersion);
			default:
				throw new CmfStorageException(String.format("Unsupported DB type [%s]", dbName));
		}
	}
}