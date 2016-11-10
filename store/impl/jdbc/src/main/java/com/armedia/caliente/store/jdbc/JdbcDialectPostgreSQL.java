package com.armedia.caliente.store.jdbc;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.commons.dbutils.ResultSetHandler;
import org.apache.commons.lang3.StringUtils;

public class JdbcDialectPostgreSQL extends JdbcDialect {

	private static final String OBJECT_COLUMN_NAME = "object_number";
	private static final ResultSetHandler<Long> OBJECT_NUMBER_HANDLER = new ResultSetHandler<Long>() {
		@Override
		public Long handle(ResultSet rs) throws SQLException {
			if (rs.next()) { return rs.getLong(JdbcDialectPostgreSQL.OBJECT_COLUMN_NAME); }
			return null;
		}
	};

	private static final String LOAD_OBJECTS_BY_ID = //
		"       select o.*, n.new_name " + //
			"     from cmf_object o, cmf_alt_name n " + //
			"    where o.object_id = n.object_id " + //
			"      and o.object_type = ? " + //
			"      and o.object_id = any ( ? ) " + //
			" order by o.tier_id, o.history_id, o.object_number" //
	;

	private static final String LOAD_OBJECTS_BY_ID_CURRENT = //
		"       select o.*, n.new_name " + //
			"     from cmf_object o, cmf_alt_name n " + //
			"    where o.object_id = n.object_id " + //
			"      and o.object_type = ? " + //
			"      and o.object_id = any ( ? ) " + //
			"      and o.history_current = true " + //
			" order by o.tier_id, o.history_id, o.object_number" //
	;

	private static final String RESET_ALT_NAME = //
		"       update cmf_alt_name " + //
			"      set new_name = o.object_name " + //
			"     from cmf_object o " + //
			"    where cmf_alt_name.object_id = o.object_id " //
	;

	private static final String TRUNCATE_TABLE_FMT = //
		"     truncate table %s cascade " //
	;

	private static final String DISABLE_REFERENTIAL_INTEGRITY = //
		"     set session_replication_role = replica " //
	;

	private static final String ENABLE_REFERENTIAL_INTEGRITY = //
		"     set session_replication_role = DEFAULT " //
	;

	public JdbcDialectPostgreSQL(DatabaseMetaData md) throws SQLException {
		super(EngineType.PostgreSQL, md);
	}

	@Override
	protected boolean isSupportsArrays() {
		return true;
	}

	@Override
	protected String doTranslate(Query sql) {
		switch (sql) {
			case LOAD_OBJECTS_BY_ID:
				return JdbcDialectPostgreSQL.LOAD_OBJECTS_BY_ID;
			case LOAD_OBJECTS_BY_ID_CURRENT:
				return JdbcDialectPostgreSQL.LOAD_OBJECTS_BY_ID_CURRENT;
			case TRUNCATE_TABLE_FMT:
				return JdbcDialectPostgreSQL.TRUNCATE_TABLE_FMT;
			case ENABLE_REFERENTIAL_INTEGRITY:
				return JdbcDialectPostgreSQL.ENABLE_REFERENTIAL_INTEGRITY;
			case DISABLE_REFERENTIAL_INTEGRITY:
				return JdbcDialectPostgreSQL.DISABLE_REFERENTIAL_INTEGRITY;
			case RESET_ALT_NAME:
				return JdbcDialectPostgreSQL.RESET_ALT_NAME;
			default:
				break;
		}
		return super.doTranslate(sql);
	}

	@Override
	protected ResultSetHandler<Long> getObjectNumberHandler() {
		return JdbcDialectPostgreSQL.OBJECT_NUMBER_HANDLER;
	}

	@Override
	protected boolean isDuplicateKeyException(SQLException e) {
		return StringUtils.equalsIgnoreCase(e.getSQLState(), "23505");
	}
}