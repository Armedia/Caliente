package com.armedia.cmf.storage.jdbc;

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
			" order by o.object_number" //
	;

	private static final String LOAD_OBJECTS_BY_ID_HEAD = //
		"       select o.*, n.new_name " + //
			"     from cmf_object o, cmf_alt_name n " + //
			"    where o.object_id = n.object_id " + //
			"      and o.object_type = ? " + //
			"      and o.object_id = any ( ? ) " + //
			"      and o.batch_head = true " + //
			" order by o.object_number" //
	;

	private static final String LOAD_OBJECTS_BY_ID_BATCHED = //
		"       select o.*, n.new_name " + //
			"     from cmf_object o " + //
			"    where o.object_id = n.object_id " + //
			"      and o.object_type = ? " + //
			"      and o.object_id = any ( ? ) " + //
			" order by o.batch_id, o.object_number" //
	;

	private static final String TRUNCATE_TABLE_FMT = //
		"     truncate table %s cascade " //
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
			case LOAD_OBJECTS_BY_ID_BATCHED:
				return JdbcDialectPostgreSQL.LOAD_OBJECTS_BY_ID_BATCHED;
			case LOAD_OBJECTS_BY_ID_HEAD:
				return JdbcDialectPostgreSQL.LOAD_OBJECTS_BY_ID_HEAD;
			case TRUNCATE_TABLE_FMT:
				return JdbcDialectPostgreSQL.TRUNCATE_TABLE_FMT;
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