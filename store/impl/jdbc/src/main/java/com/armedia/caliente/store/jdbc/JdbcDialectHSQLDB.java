package com.armedia.caliente.store.jdbc;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.commons.dbutils.ResultSetHandler;

public class JdbcDialectHSQLDB extends JdbcDialect {

	private static final ResultSetHandler<Long> OBJECT_NUMBER_HANDLER = new ResultSetHandler<Long>() {
		@Override
		public Long handle(ResultSet rs) throws SQLException {
			if (rs.next()) { return rs.getLong(1); }
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
		"     truncate table %s restart identity no check " //
	;

	private static final String UPSERT_ALT_NAME = //
		"     merge into cmf_alt_name " + //
			" using (values(?, ?)) as vals(object_id, new_name) " + //
			"  when matched then update set cmf_alt_name.new_name = vals.new_name " + //
			"  when not matched insert values vals.object_id, vals.new_name " //
	;

	private static final String RESTART_SEQUENCE = //
		"     alter sequence %s restart with 1" //
	;

	public JdbcDialectHSQLDB(DatabaseMetaData md) throws SQLException {
		super(EngineType.HSQLDB, md);
	}

	@Override
	protected boolean isSupportsArrays() {
		return true;
	}

	@Override
	protected boolean isTruncateBypassesConstraints() {
		return true;
	}

	@Override
	protected boolean isTruncateRestartsSequences() {
		return true;
	}

	@Override
	protected String doTranslate(Query sql) {
		switch (sql) {
			case LOAD_OBJECTS_BY_ID:
				return JdbcDialectHSQLDB.LOAD_OBJECTS_BY_ID;
			case LOAD_OBJECTS_BY_ID_CURRENT:
				return JdbcDialectHSQLDB.LOAD_OBJECTS_BY_ID_CURRENT;
			case LOAD_OBJECT_NAMES_BY_ID:
				return JdbcDialectHSQLDB.LOAD_OBJECT_NAMES_BY_ID;
			case LOAD_OBJECT_NAMES_BY_ID_CURRENT:
				return JdbcDialectHSQLDB.LOAD_OBJECT_NAMES_BY_ID_CURRENT;
			case TRUNCATE_TABLE_FMT:
				return JdbcDialectHSQLDB.TRUNCATE_TABLE_FMT;
			case UPSERT_ALT_NAME:
				return JdbcDialectHSQLDB.UPSERT_ALT_NAME;
			case RESTART_SEQUENCE:
				return JdbcDialectHSQLDB.RESTART_SEQUENCE;
			default:
				break;
		}
		return super.doTranslate(sql);
	}

	@Override
	protected ResultSetHandler<Long> getObjectNumberHandler() {
		return JdbcDialectHSQLDB.OBJECT_NUMBER_HANDLER;
	}

	@Override
	protected boolean isDuplicateKeyException(SQLException e) {
		return (e.getErrorCode() == 23505);
	}
}