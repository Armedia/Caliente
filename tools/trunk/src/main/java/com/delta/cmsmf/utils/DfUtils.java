package com.delta.cmsmf.utils;

import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import com.delta.cmsmf.cfg.Constant;
import com.documentum.com.DfClientX;
import com.documentum.fc.client.IDfCollection;
import com.documentum.fc.client.IDfQuery;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.common.DfException;
import com.documentum.fc.common.IDfTime;

public class DfUtils {

	public static enum DbType {
		//
		ORACLE("^.*\\.Oracle$"),
		MSSQL("^.*\\.SQLServer$");

		private final Pattern pattern;

		private DbType(String pattern) {
			this.pattern = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);
		}

		public boolean matches(String serverVersion) {
			if (serverVersion == null) { throw new IllegalArgumentException(
				"Must provide a server version string to examine"); }
			return this.pattern.matcher(serverVersion).matches();
		}

		public boolean matches(IDfSession session) throws DfException {
			if (session == null) { throw new IllegalArgumentException("Must provide a session to examine"); }
			return matches(session.getServerVersion());
		}
	}

	private static final Logger LOG = Logger.getLogger(DfUtils.class);

	public static void closeQuietly(IDfCollection c) {
		if (c == null) { return; }
		try {
			c.close();
		} catch (DfException e) {
			// quietly swallowed
			if (DfUtils.LOG.isTraceEnabled()) {
				DfUtils.LOG.trace("Swallowing exception on close", e);
			}
		}
	}

	public static IDfQuery newQuery() {
		return new DfClientX().getQuery();
	}

	public static IDfCollection executeQuery(IDfSession session, String dql) throws DfException {
		return DfUtils.executeQuery(session, dql, IDfQuery.DF_QUERY);
	}

	public static IDfCollection executeQuery(IDfSession session, String dql, int queryType) throws DfException {
		if (session == null) { throw new IllegalArgumentException("Must provide a session to execute the DQL on"); }
		if (dql == null) { throw new IllegalArgumentException("Must provide a DQL statement to execute"); }
		IDfQuery query = DfUtils.newQuery();
		if (DfUtils.LOG.isTraceEnabled()) {
			DfUtils.LOG.trace(String.format("Executing DQL (type=%d): %s", queryType, dql));
		}
		query.setDQL(dql);
		boolean ok = false;
		try {
			IDfCollection ret = query.execute(session, queryType);
			ok = true;
			return ret;
		} finally {
			if (!ok) {
				DfUtils.LOG.fatal(String.format("Exception raised while executing the query: %s", dql));
			}
		}
	}

	public static String getSessionId(IDfSession session) {
		if (session == null) { throw new IllegalArgumentException("Must provide a session to get the ID from"); }
		try {
			return session.getSessionId();
		} catch (DfException e) {
			return "(unknown)";
		}
	}

	public static DbType getDbType(IDfSession session) throws DfException {
		if (session == null) { throw new IllegalArgumentException(
			"Must provide a session to identify the database from"); }
		final String serverVersion = session.getServerVersion();
		for (DbType type : DbType.values()) {
			if (type.matches(serverVersion)) { return type; }
		}
		throw new UnsupportedOperationException(String.format(
			"Failed to identify a supported database from the server version string [%s]", serverVersion));
	}

	public static String generateSqlDateClause(IDfTime date, IDfSession session) throws DfException {
		// First, output to the "netural" format
		String dateString = date.asString(Constant.SQL_DATETIME_PATTERN);
		// Now, select the database format string
		final String ret;
		DbType dbType = DfUtils.getDbType(session);
		switch (dbType) {
			case ORACLE:
				ret = String.format("TO_DATE(''%s'', ''%s'')", dateString, Constant.ORACLE_DATETIME_PATTERN);
				break;
			case MSSQL:
				ret = String.format("CONVERT(DATETIME, ''%s'', %d)", dateString, Constant.MSSQL_DATETIME_PATTERN);
				break;
			default:
				throw new UnsupportedOperationException(String.format("Unsupported database type [%s]", dbType));
		}
		if (DfUtils.LOG.isTraceEnabled()) {
			DfUtils.LOG.trace(String.format("Generated %s SQL Date string [%s]", dbType, ret));
		}
		return ret;
	}
}