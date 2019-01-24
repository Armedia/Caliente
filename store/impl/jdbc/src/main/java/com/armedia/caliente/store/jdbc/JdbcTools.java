package com.armedia.caliente.store.jdbc;

import java.math.BigInteger;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.sql.Statement;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.ResultSetHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.store.CmfObjectRef;
import com.armedia.caliente.store.CmfType;

class JdbcTools {

	private static final Logger LOG = LoggerFactory.getLogger(JdbcTools.class);

	private JdbcTools() {
	}

	static final ResultSetHandler<Void> HANDLER_NULL = (rs) -> null;

	static final ResultSetHandler<Integer> HANDLER_INT_COUNTER = (rs) -> {
		int c = 0;
		while (rs.next()) {
			c++;
			if (c < 0) {
				throw new SQLException(String.format("Counter wraparound after %d items", Integer.MAX_VALUE));
			}
		}
		return c;
	};

	static final ResultSetHandler<Long> HANDLER_LONG_COUNTER = (rs) -> {
		long c = 0;
		while (rs.next()) {
			c++;
			if (c < 0) { throw new SQLException(String.format("Counter wraparound after %d items", Long.MAX_VALUE)); }
		}
		return c;
	};

	static final ResultSetHandler<BigInteger> HANDLER_BIGINT_COUNTER = (rs) -> {
		BigInteger c = BigInteger.ZERO;
		while (rs.next()) {
			c = c.add(BigInteger.ONE);
			if (c.compareTo(BigInteger.ZERO) < 0) {
				throw new SQLException("BigInteger counter wraparound (probable Java bug!!!)");
			}
		}
		return c;
	};

	static final ResultSetHandler<Boolean> HANDLER_EXISTS = (rs) -> rs.next();

	static final Pattern OBJECT_ID_PARSER = Pattern.compile("^\\{([\\da-f]{1,8})-(.*)\\}$", Pattern.CASE_INSENSITIVE);

	static final Object[][] NO_PARAMS = new Object[0][0];

	private static final ThreadLocal<QueryRunner> QUERY_RUNNER = new ThreadLocal<>();

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
		if (!m.matches()) {
			throw new IllegalArgumentException(String.format("The string [%s] is not a valid object ID", id));
		}
		// Parse out the ID value
		final CmfType type;
		try {
			type = CmfType.values()[Integer.valueOf(m.group(1), 16)];
		} catch (Exception e) {
			throw new IllegalArgumentException(String.format("The object type [%s] is not a valid object type", id), e);
		}
		return new CmfObjectRef(type, m.group(2));
	}

	private static void handleException(SQLException e) {
		// if (LOG.isTraceEnabled())
		{
			JdbcTools.LOG.error("SQLException caught while closing down resources", e);
		}
	}

	static void closeQuietly(ResultSet rs) {
		if (rs == null) { return; }
		try {
			rs.close();
		} catch (SQLException e) {
			JdbcTools.handleException(e);
		}
	}

	static void closeQuietly(Statement s) {
		if (s == null) { return; }
		try {
			s.close();
		} catch (SQLException e) {
			JdbcTools.handleException(e);
		}
	}

	static void closeQuietly(Connection c) {
		if (c == null) { return; }
		try {
			c.close();
		} catch (SQLException e) {
			JdbcTools.handleException(e);
		}
	}

	static void rollbackSavepoint(Connection c, Savepoint savePoint) {
		if (savePoint == null) { return; }
		try {
			c.rollback(savePoint);
		} catch (SQLException e) {
			JdbcTools.LOG.trace("Failed to roll back to the established SavePoint", e);
		}
	}

	static Savepoint commitSavepoint(Connection c, Savepoint savePoint) {
		if (savePoint == null) { return null; }
		try {
			c.releaseSavepoint(savePoint);
			return null;
		} catch (SQLException e) {
			JdbcTools.LOG.trace("Failed to roll back to the established SavePoint", e);
			return savePoint;
		}
	}
}