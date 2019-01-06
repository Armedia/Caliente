package com.armedia.caliente.store.jdbc;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.commons.dbutils.ResultSetHandler;

public class JdbcDialectH2 extends JdbcDialect {

	private static final ResultSetHandler<Long> OBJECT_NUMBER_HANDLER = new ResultSetHandler<Long>() {
		@Override
		public Long handle(ResultSet rs) throws SQLException {
			if (rs.next()) { return rs.getLong(1); }
			return null;
		}
	};

	private static final String LOAD_OBJECTS_BY_ID = //
		"       select o.*, n.new_name " + //
			"     from cmf_object o left outer join cmf_alt_name n on (o.object_id = n.object_id), " + //
			"          table(x varchar=?) t " + //
			"    where o.object_id = t.x " + //
			"      and o.object_type = ? " + //
			" order by o.tier_id, o.history_id, o.object_number" //
	;

	private static final String LOAD_OBJECTS_BY_ID_CURRENT = //
		"       select o.*, n.new_name " + //
			"     from cmf_object o left outer join cmf_alt_name n on (o.object_id = n.object_id), " + //
			"          table(x varchar=?) t " + //
			"    where o.object_id = t.x " + //
			"      and o.history_current = true " + //
			"      and o.object_type = ? " + //
			" order by o.tier_id, o.history_id, o.object_number" //
	;

	private static final String LOAD_OBJECT_NAMES_BY_ID = //
		"       select o.object_id, o.object_name, n.new_name " + //
			"     from cmf_object o left outer join cmf_alt_name n on (o.object_id = n.object_id), " + //
			"          table(x varchar=?) t " + //
			"    where o.object_id = t.x " + //
			" order by o.object_id " //
	;

	private static final String LOAD_OBJECT_NAMES_BY_ID_CURRENT = //
		"       select o.object_id, o.object_label, o2.object_name, n.new_name " + //
			"     from cmf_object o, table(x varchar=?) t, " + //
			"          cmf_object o2 left outer join cmf_alt_name n on (o2.object_id = n.object_id) " + //
			"    where o.object_id = t.x " + //
			"      and o.object_type = o2.object_type " + //
			"      and o.history_id = o2.history_id " + //
			"      and o2.history_current = true " + //
			" order by o.object_id " //
	;

	private static final String ENABLE_REFERENTIAL_INTEGRITY = //
		"          set REFERENTIAL_INTEGRITY true" //
	;

	private static final String DISABLE_REFERENTIAL_INTEGRITY = //
		"          set REFERENTIAL_INTEGRITY false" //
	;

	private static final String UPSERT_ALT_NAME = //
		"     merge into cmf_alt_name (object_id, new_name) key (object_id) values ( ?, ? ) " //
	;

	private static final String RESTART_SEQUENCE = //
		"     alter sequence %s restart with 1" //
	;

	private static final String LIST_SEQUENCES = //
		"    select sequence_name " + //
			"  from information_schema.columns " + //
			" where table_name like 'CMF_%' " + //
			"   and sequence_name is not null" //
	;

	private static final String SHUTDOWN_DB = //
		"    shutdown compact " //
	;

	public JdbcDialectH2(DatabaseMetaData md) throws SQLException {
		super(EngineType.H2, md);
	}

	@Override
	protected boolean isSupportsArrays() {
		return false;
	}

	@Override
	protected boolean isTruncateBypassesConstraints() {
		return false;
	}

	@Override
	protected boolean isTruncateRestartsSequences() {
		return false;
	}

	@Override
	protected boolean isShutdownOnLastConnectionClose() {
		return true;
	}

	@Override
	protected String doTranslate(Query sql) {
		switch (sql) {
			case LOAD_OBJECTS_BY_ID:
				return JdbcDialectH2.LOAD_OBJECTS_BY_ID;
			case LOAD_OBJECTS_BY_ID_CURRENT:
				return JdbcDialectH2.LOAD_OBJECTS_BY_ID_CURRENT;
			case LOAD_OBJECT_NAMES_BY_ID:
				return JdbcDialectH2.LOAD_OBJECT_NAMES_BY_ID;
			case LOAD_OBJECT_NAMES_BY_ID_CURRENT:
				return JdbcDialectH2.LOAD_OBJECT_NAMES_BY_ID_CURRENT;
			case ENABLE_REFERENTIAL_INTEGRITY:
				return JdbcDialectH2.ENABLE_REFERENTIAL_INTEGRITY;
			case DISABLE_REFERENTIAL_INTEGRITY:
				return JdbcDialectH2.DISABLE_REFERENTIAL_INTEGRITY;
			case UPSERT_ALT_NAME:
				return JdbcDialectH2.UPSERT_ALT_NAME;
			case RESTART_SEQUENCE:
				return JdbcDialectH2.RESTART_SEQUENCE;
			case LIST_SEQUENCES:
				return JdbcDialectH2.LIST_SEQUENCES;
			case SHUTDOWN_DB:
				return JdbcDialectH2.SHUTDOWN_DB;
			default:
				break;
		}
		return super.doTranslate(sql);
	}

	@Override
	protected ResultSetHandler<Long> getObjectNumberHandler() {
		return JdbcDialectH2.OBJECT_NUMBER_HANDLER;
	}
}