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
			"     from cmf_object o left outer join cmf_alt_name n on (o.object_id = n.object_id)" + //
			"    where o.object_type = ? " + //
			"      and o.object_id = any ( ? ) " + //
			" order by o.tier_id, o.history_id, o.object_number" //
	;

	private static final String LOAD_OBJECTS_BY_ID_CURRENT = //
		"       select o.*, n.new_name " + //
			"     from cmf_object o left outer join cmf_alt_name n on (o.object_id = n.object_id)" + //
			"    where o.object_type = ? " + //
			"      and o.object_id = any ( ? ) " + //
			"      and o.history_current = true " + //
			" order by o.tier_id, o.history_id, o.object_number" //
	;

	private static final String LOAD_OBJECT_NAMES_BY_ID = //
		"       select o.object_id, o.object_name, n.new_name " + //
			"     from cmf_object o left outer join cmf_alt_name n on (o.object_id = n.object_id)" + //
			"    where o.object_id = any ( ? ) " + //
			" order by o.object_id " //
	;

	private static final String LOAD_OBJECT_NAMES_BY_ID_CURRENT = //
		"       select o.object_id, o2.object_name, n.new_name " + //
			"     from cmf_object o, " + //
			"          cmf_object o2 left outer join cmf_alt_name n on (o2.object_id = n.object_id) " + //
			"    where o.object_id = any ( ? ) " + //
			"      and o.object_type = o2.object_type " + //
			"      and o.history_id = o2.history_id " + //
			"      and o2.history_current = true " + //
			" order by o.object_id " //
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

	private static final String UPSERT_ALT_NAME = //
		"     insert " + //
			"   into cmf_alt_name (object_id, new_name) values ( ?, ? ) " + //
			"     on conflict (object_id) do " + //
			"                 update set new_name = excluded.new_name " //
	;

	private static final String RESTART_SEQUENCE = //
		"     alter sequence %s restart" //
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
			case LOAD_OBJECT_NAMES_BY_ID:
				return JdbcDialectPostgreSQL.LOAD_OBJECT_NAMES_BY_ID;
			case LOAD_OBJECT_NAMES_BY_ID_CURRENT:
				return JdbcDialectPostgreSQL.LOAD_OBJECT_NAMES_BY_ID_CURRENT;
			case TRUNCATE_TABLE_FMT:
				return JdbcDialectPostgreSQL.TRUNCATE_TABLE_FMT;
			case ENABLE_REFERENTIAL_INTEGRITY:
				return JdbcDialectPostgreSQL.ENABLE_REFERENTIAL_INTEGRITY;
			case DISABLE_REFERENTIAL_INTEGRITY:
				return JdbcDialectPostgreSQL.DISABLE_REFERENTIAL_INTEGRITY;
			case UPSERT_ALT_NAME:
				return JdbcDialectPostgreSQL.UPSERT_ALT_NAME;
			case RESTART_SEQUENCE:
				return JdbcDialectPostgreSQL.RESTART_SEQUENCE;
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