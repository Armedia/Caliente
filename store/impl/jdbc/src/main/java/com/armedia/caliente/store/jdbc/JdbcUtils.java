package com.armedia.caliente.store.jdbc;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JdbcUtils {

	private static final Logger LOG = LoggerFactory.getLogger(JdbcUtils.class);

	private static void handleException(SQLException e) {
		if (JdbcUtils.LOG.isTraceEnabled()) {
			JdbcUtils.LOG.error("SQLException caught while closing down resources", e);
		}
	}

	static void closeQuietly(ResultSet rs) {
		if (rs == null) { return; }
		try {
			rs.close();
		} catch (SQLException e) {
			JdbcUtils.handleException(e);
		}
	}

	static void closeQuietly(Statement s) {
		if (s == null) { return; }
		try {
			s.close();
		} catch (SQLException e) {
			JdbcUtils.handleException(e);
		}
	}

	static void closeQuietly(Connection c) {
		if (c == null) { return; }
		try {
			c.close();
		} catch (SQLException e) {
			JdbcUtils.handleException(e);
		}
	}
}