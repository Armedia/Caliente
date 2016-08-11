package com.armedia.cmf.storage.jdbc;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.ResultSetHandler;

import com.armedia.cmf.storage.CmfObject;
import com.armedia.cmf.storage.CmfObjectRef;
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

	static String composeDatabaseId(CmfObjectRef objectRef) {
		return JdbcTools.composeDatabaseId(objectRef.getType(), objectRef.getId());
	}

	static String composeDatabaseId(CmfObject<?> obj) {
		return JdbcTools.composeDatabaseId(obj.getType(), obj.getId());
	}

	static CmfObjectRef decodeDatabaseId(String id) {
		if (id == null) { throw new IllegalArgumentException("Must provide an ID to parse"); }
		Matcher m = JdbcTools.OBJECT_ID_PARSER.matcher(id);
		if (!m.matches()) { throw new IllegalArgumentException(
			String.format("The string [%s] is not a valid object ID", id)); }
		// Parse out the ID value
		final CmfType type;
		try {
			type = CmfType.values()[Integer.valueOf(m.group(1), 16)];
		} catch (Exception e) {
			throw new IllegalArgumentException(String.format("The object type [%s] is not a valid object type", id), e);
		}
		return new CmfObjectRef(type, m.group(2));
	}
}