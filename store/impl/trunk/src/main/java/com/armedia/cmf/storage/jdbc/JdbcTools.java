package com.armedia.cmf.storage.jdbc;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.ResultSetHandler;

import com.armedia.cmf.storage.CmfObject;
import com.armedia.cmf.storage.CmfType;

class JdbcTools {

	private JdbcTools() {
	}

	static final ResultSetHandler<Object> HANDLER_NULL = new ResultSetHandler<Object>() {
		@Override
		public Object handle(ResultSet rs) throws SQLException {
			return null;
		}
	};

	static final ResultSetHandler<Boolean> HANDLER_EXISTS = new ResultSetHandler<Boolean>() {
		@Override
		public Boolean handle(ResultSet rs) throws SQLException {
			return rs.next();
		}
	};

	static final Pattern OBJECT_ID_PARSER = Pattern.compile("^\\{(?:[\\da-fA-F][\\da-fA-F])+-(.*)\\}$");

	static final Object[][] NO_PARAMS = new Object[0][0];

	private static final ThreadLocal<QueryRunner> QUERY_RUNNER = new ThreadLocal<QueryRunner>();

	static boolean isValidId(String id) {
		if (id == null) { return false; }
		Matcher m = JdbcTools.OBJECT_ID_PARSER.matcher(id);
		return m.matches();
	}

	static QueryRunner getQueryRunner() {
		QueryRunner q = JdbcTools.QUERY_RUNNER.get();
		if (q == null) {
			q = new QueryRunner();
			JdbcTools.QUERY_RUNNER.set(q);
		}
		return q;
	}

	static String composeDatabaseId(CmfType type, String id) {
		return String.format("{%02x-%s}", type.ordinal(), id);
	}

	static String composeDatabaseId(CmfObject<?> obj) {
		return JdbcTools.composeDatabaseId(obj.getType(), obj.getId());
	}
}