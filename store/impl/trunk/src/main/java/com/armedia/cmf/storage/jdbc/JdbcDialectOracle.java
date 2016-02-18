package com.armedia.cmf.storage.jdbc;

import java.sql.DatabaseMetaData;
import java.sql.SQLException;

public class JdbcDialectOracle extends JdbcDialect {

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

	public JdbcDialectOracle(DatabaseMetaData md) throws SQLException {
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
				return JdbcDialectOracle.LOAD_OBJECTS_BY_ID;
			case LOAD_OBJECTS_BY_ID_BATCHED:
				return JdbcDialectOracle.LOAD_OBJECTS_BY_ID_BATCHED;
			case TRUNCATE_TABLE_FMT:
				return JdbcDialectOracle.TRUNCATE_TABLE_FMT;
			default:
				break;
		}
		return super.doTranslate(sql);
	}
}