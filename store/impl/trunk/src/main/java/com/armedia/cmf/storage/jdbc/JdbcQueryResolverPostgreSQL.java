package com.armedia.cmf.storage.jdbc;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.commons.dbutils.ResultSetHandler;

public class JdbcQueryResolverPostgreSQL extends JdbcQueryResolver {
	private static final String OBJECT_NUMBER_COLUMN = "object_number";
	private static final ResultSetHandler<Long> OBJECT_NUMBER_HANDLER = new ResultSetHandler<Long>() {

		@Override
		public Long handle(ResultSet rs) throws SQLException {
			if (rs.next()) { return rs.getLong(JdbcQueryResolverPostgreSQL.OBJECT_NUMBER_COLUMN); }
			return null;
		}
	};

	private static final String LOAD_OBJECTS_BY_ID = //
	"       select * " + //
		"     from cmf_object " + //
		"    where object_type = ? " + //
		"      and object_id = any ( ? ) " + //
		" order by object_number" //
		;

	private static final String LOAD_OBJECTS_BY_ID_BATCHED = //
	"       select * " + //
		"     from cmf_object " + //
		"    where object_type = ? " + //
		"      and object_id = any ( ? ) " + //
		" order by batch_id, object_number" //
		;

	public JdbcQueryResolverPostgreSQL(String dbName, int dbMajor, int dbMinor, String dbVersion) {
		super(EngineType.PostgreSQL, dbName, dbMajor, dbMinor, dbVersion);
	}

	@Override
	protected boolean isSupportsArrays() {
		return true;
	}

	@Override
	protected String doResolve(JdbcQuery sql) {
		switch (sql) {
			case LOAD_OBJECTS_BY_ID:
				return JdbcQueryResolverPostgreSQL.LOAD_OBJECTS_BY_ID;
			case LOAD_OBJECTS_BY_ID_BATCHED:
				return JdbcQueryResolverPostgreSQL.LOAD_OBJECTS_BY_ID_BATCHED;
			default:
				break;
		}
		return super.doResolve(sql);
	}

	@Override
	protected ResultSetHandler<Long> getObjectNumberHandler() {
		return JdbcQueryResolverPostgreSQL.OBJECT_NUMBER_HANDLER;
	}
}