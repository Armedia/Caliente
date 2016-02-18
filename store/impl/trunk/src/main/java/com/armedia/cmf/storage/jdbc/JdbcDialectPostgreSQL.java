package com.armedia.cmf.storage.jdbc;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.commons.dbutils.ResultSetHandler;

public class JdbcDialectPostgreSQL extends JdbcDialect {
	private static final String OBJECT_NUMBER_COLUMN = "object_number";
	private static final ResultSetHandler<Long> OBJECT_NUMBER_HANDLER = new ResultSetHandler<Long>() {

		@Override
		public Long handle(ResultSet rs) throws SQLException {
			if (rs.next()) { return rs.getLong(JdbcDialectPostgreSQL.OBJECT_NUMBER_COLUMN); }
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
	protected boolean isObjectNumberReturnedFromInsert() {
		return true;
	}

	@Override
	protected String doTranslate(JdbcQuery sql) {
		switch (sql) {
			case LOAD_OBJECTS_BY_ID:
				return JdbcDialectPostgreSQL.LOAD_OBJECTS_BY_ID;
			case LOAD_OBJECTS_BY_ID_BATCHED:
				return JdbcDialectPostgreSQL.LOAD_OBJECTS_BY_ID_BATCHED;
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
}