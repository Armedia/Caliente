package com.armedia.cmf.storage.jdbc;

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
	"       select o.* " + //
		"     from cmf_object o, table(x varchar=?) t " + //
		"    where o.object_type = ? " + //
		"      and o.object_id = t.x " + //
		" order by o.object_number" //
		;

	private static final String LOAD_OBJECTS_BY_ID_BATCHED = //
	"       select o.* " + //
		"     from cmf_object o, table(x varchar=?) t " + //
		"    where o.object_type = ? " + //
		"      and o.object_id = t.x " + //
		" order by o.batch_id, o.object_number" //
		;

	private static final String ENABLE_REFERENTIAL_INTEGRITY = //
	"          set REFERENTIAL_INTEGRITY true" //
	;

	private static final String DISABLE_REFERENTIAL_INTEGRITY = //
	"          set REFERENTIAL_INTEGRITY false" //
	;

	public JdbcDialectH2(DatabaseMetaData md) throws SQLException {
		super(EngineType.H2, md);
	}

	@Override
	protected boolean isSupportsArrays() {
		return false;
	}

	@Override
	protected String doTranslate(Query sql) {
		switch (sql) {
			case LOAD_OBJECTS_BY_ID:
				return JdbcDialectH2.LOAD_OBJECTS_BY_ID;
			case LOAD_OBJECTS_BY_ID_BATCHED:
				return JdbcDialectH2.LOAD_OBJECTS_BY_ID_BATCHED;
			case ENABLE_REFERENTIAL_INTEGRITY:
				return JdbcDialectH2.ENABLE_REFERENTIAL_INTEGRITY;
			case DISABLE_REFERENTIAL_INTEGRITY:
				return JdbcDialectH2.DISABLE_REFERENTIAL_INTEGRITY;
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