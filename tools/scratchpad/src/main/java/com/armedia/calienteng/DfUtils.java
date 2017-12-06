package com.armedia.calienteng;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.commons.lang3.time.DateFormatUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.commons.utilities.Tools;
import com.documentum.com.DfClientX;
import com.documentum.fc.client.IDfACL;
import com.documentum.fc.client.IDfCollection;
import com.documentum.fc.client.IDfPermit;
import com.documentum.fc.client.IDfPersistentObject;
import com.documentum.fc.client.IDfQuery;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.client.IDfSysObject;
import com.documentum.fc.client.content.IDfStore;
import com.documentum.fc.common.DfException;

public class DfUtils {

	private static final String ORACLE_DATETIME_PATTERN = DfUtils.sqlQuoteString(DctmConstant.ORACLE_DATETIME_PATTERN);

	private static final int MIN_BATCH_SIZE = 100;
	private static final int MAX_BATCH_SIZE = Integer.MAX_VALUE;

	public static enum DbType {
		//
		ORACLE("^.*\\.Oracle$"), //
		MSSQL("^.*\\.SQLServer$"), //
		//
		;

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

	private static final Logger LOG = LoggerFactory.getLogger(DfUtils.class);

	private static enum Permission {
		//
		DF_PERMIT_NONE(IDfACL.DF_PERMIT_NONE, IDfACL.DF_PERMIT_NONE_STR),
		DF_PERMIT_BROWSE(IDfACL.DF_PERMIT_BROWSE, IDfACL.DF_PERMIT_BROWSE_STR),
		DF_PERMIT_READ(IDfACL.DF_PERMIT_READ, IDfACL.DF_PERMIT_READ_STR),
		DF_PERMIT_RELATE(IDfACL.DF_PERMIT_RELATE, IDfACL.DF_PERMIT_RELATE_STR),
		DF_PERMIT_VERSION(IDfACL.DF_PERMIT_VERSION, IDfACL.DF_PERMIT_VERSION_STR),
		DF_PERMIT_WRITE(IDfACL.DF_PERMIT_WRITE, IDfACL.DF_PERMIT_WRITE_STR),
		DF_PERMIT_DELETE(IDfACL.DF_PERMIT_DELETE, IDfACL.DF_PERMIT_DELETE_STR);
		private final int num;
		private final String str;

		private Permission(int num, String str) {
			this.str = str;
			this.num = num;
		}
	}

	private static enum PermitType {
		//
		DF_ACCESS_PERMIT(IDfPermit.DF_ACCESS_PERMIT, IDfPermit.DF_ACCESS_PERMIT_STR),
		DF_ACCESS_RESTRICTION(IDfPermit.DF_ACCESS_RESTRICTION, IDfPermit.DF_ACCESS_RESTRICTION_STR),
		DF_APPLICATION_PERMIT(IDfPermit.DF_APPLICATION_PERMIT, IDfPermit.DF_APPLICATION_PERMIT_STR),
		DF_APPLICATION_RESTRICTION(IDfPermit.DF_APPLICATION_RESTRICTION, IDfPermit.DF_APPLICATION_RESTRICTION_STR),
		DF_EXTENDED_PERMIT(IDfPermit.DF_EXTENDED_PERMIT, IDfPermit.DF_EXTENDED_PERMIT_STR),
		DF_EXTENDED_RESTRICTION(IDfPermit.DF_EXTENDED_RESTRICTION, IDfPermit.DF_EXTENDED_RESTRICTION_STR),
		DF_REQUIRED_GROUP(IDfPermit.DF_REQUIRED_GROUP, IDfPermit.DF_REQUIRED_GROUP_STR),
		DF_REQUIRED_GROUP_SET(IDfPermit.DF_REQUIRED_GROUP_SET, IDfPermit.DF_REQUIRED_GROUP_SET_STR);
		private final int num;
		private final String str;

		private PermitType(int num, String str) {
			this.str = str;
			this.num = num;
		}
	}

	private static final Map<String, Integer> PERMISSIONS_MAP;
	private static final Map<String, Integer> PERMIT_TYPES_MAP;
	static {
		Map<String, Integer> m = new HashMap<>();
		for (Permission p : Permission.values()) {
			m.put(p.str, p.num);
		}
		PERMISSIONS_MAP = Collections.unmodifiableMap(m);
		m = new HashMap<>();
		for (PermitType p : PermitType.values()) {
			m.put(p.str, p.num);
		}
		PERMIT_TYPES_MAP = Collections.unmodifiableMap(m);
	}

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
		return DfUtils.executeQuery(session, dql, IDfQuery.DF_QUERY, 0);
	}

	public static IDfCollection executeQuery(IDfSession session, String dql, int queryType) throws DfException {
		return DfUtils.executeQuery(session, dql, queryType, 0);
	}

	public static IDfCollection executeQuery(IDfSession session, String dql, int queryType, int queryBatchSize)
		throws DfException {
		if (session == null) { throw new IllegalArgumentException("Must provide a session to execute the DQL on"); }
		if (dql == null) { throw new IllegalArgumentException("Must provide a DQL statement to execute"); }
		switch (queryType) {
			case IDfQuery.DF_APPLY:
			case IDfQuery.DF_CACHE_QUERY:
			case IDfQuery.DF_EXEC_QUERY:
			case IDfQuery.DF_EXECREAD_QUERY:
			case IDfQuery.DF_GETCONTENT:
			case IDfQuery.DF_GETEVENTS:
			case IDfQuery.DF_GETLASTCOLL:
			case IDfQuery.DF_INTERNAL_READ_QUERY:
			case IDfQuery.DF_QUERY:
			case IDfQuery.DF_READ_QUERY:
				break;
			default:
				if (DfUtils.LOG.isDebugEnabled()) {
					try {
						throw new IllegalArgumentException();
					} catch (Throwable t) {
						DfUtils.LOG.warn(String.format("Unknown query type value [%d] given", queryType), t);
					}
				}
				break;
		}
		IDfQuery query = DfUtils.newQuery();
		if (DfUtils.LOG.isTraceEnabled()) {
			DfUtils.LOG.trace(String.format("Executing DQL (type=%d):%n%s", queryType, dql));
		}
		query.setDQL(dql);
		if (queryBatchSize > 0) {
			query.setBatchSize(Tools.ensureBetween(DfUtils.MIN_BATCH_SIZE, queryBatchSize, DfUtils.MAX_BATCH_SIZE));
		}
		boolean ok = false;
		try {
			IDfCollection ret = query.execute(session, queryType);
			ok = true;
			return ret;
		} finally {
			if (!ok) {
				DfUtils.LOG.error(String.format("Exception raised while executing the query:%n%s", dql));
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
		throw new UnsupportedOperationException(String
			.format("Failed to identify a supported database from the server version string [%s]", serverVersion));
	}

	public static String generateSqlDateClause(Date date, IDfSession session) throws DfException {
		// First, output to the "netural" format
		final String dateString = DateFormatUtils.formatUTC(date, DctmConstant.JAVA_SQL_DATETIME_PATTERN);
		// Now, select the database format string
		final String ret;
		DbType dbType = DfUtils.getDbType(session);
		switch (dbType) {
			case ORACLE:
				ret = String.format("TO_DATE(%s, %s)", DfUtils.sqlQuoteString(dateString),
					DfUtils.ORACLE_DATETIME_PATTERN);
				break;
			case MSSQL:
				ret = String.format("CONVERT(DATETIME, %s, %d)", DfUtils.sqlQuoteString(dateString),
					DctmConstant.MSSQL_DATETIME_PATTERN);
				break;
			default:
				throw new UnsupportedOperationException(String.format("Unsupported database type [%s]", dbType));
		}
		if (DfUtils.LOG.isTraceEnabled()) {
			DfUtils.LOG.trace(String.format("Generated %s SQL Date string [%s] from [%s](%d)", dbType, ret,
				DateFormatUtils.ISO_DATETIME_TIME_ZONE_FORMAT.format(date), date.getTime()));
		}
		return ret;
	}

	public static String decodeAccessPermission(int permission) throws DfException {
		// We do it the "hardcoded" way here because it's MUCH faster than maps...
		switch (permission) {
			case IDfACL.DF_PERMIT_NONE:
				return IDfACL.DF_PERMIT_NONE_STR;
			case IDfACL.DF_PERMIT_BROWSE:
				return IDfACL.DF_PERMIT_BROWSE_STR;
			case IDfACL.DF_PERMIT_READ:
				return IDfACL.DF_PERMIT_READ_STR;
			case IDfACL.DF_PERMIT_RELATE:
				return IDfACL.DF_PERMIT_RELATE_STR;
			case IDfACL.DF_PERMIT_VERSION:
				return IDfACL.DF_PERMIT_VERSION_STR;
			case IDfACL.DF_PERMIT_WRITE:
				return IDfACL.DF_PERMIT_WRITE_STR;
			case IDfACL.DF_PERMIT_DELETE:
				return IDfACL.DF_PERMIT_DELETE_STR;
			default:
				throw new DfException(String.format("Unknown permissions value [%d] detected", permission));
		}
	}

	public static int decodeAccessPermission(String permission) throws DfException {
		if (permission == null) { throw new IllegalArgumentException("Must provide a permission to map"); }
		Integer ret = DfUtils.PERMISSIONS_MAP.get(permission);
		if (ret == null) { throw new DfException(
			String.format("Unknown permissions value [%s] detected", permission)); }
		return ret;
	}

	public static String decodePermitType(int permitType) throws DfException {
		switch (permitType) {
			case IDfPermit.DF_ACCESS_PERMIT:
				return IDfPermit.DF_ACCESS_PERMIT_STR;
			case IDfPermit.DF_ACCESS_RESTRICTION:
				return IDfPermit.DF_ACCESS_RESTRICTION_STR;
			case IDfPermit.DF_APPLICATION_PERMIT:
				return IDfPermit.DF_APPLICATION_PERMIT_STR;
			case IDfPermit.DF_APPLICATION_RESTRICTION:
				return IDfPermit.DF_APPLICATION_RESTRICTION_STR;
			case IDfPermit.DF_EXTENDED_PERMIT:
				return IDfPermit.DF_EXTENDED_PERMIT_STR;
			case IDfPermit.DF_EXTENDED_RESTRICTION:
				return IDfPermit.DF_EXTENDED_RESTRICTION_STR;
			case IDfPermit.DF_REQUIRED_GROUP:
				return IDfPermit.DF_REQUIRED_GROUP_STR;
			case IDfPermit.DF_REQUIRED_GROUP_SET:
				return IDfPermit.DF_REQUIRED_GROUP_SET_STR;
		}
		throw new DfException(String.format("Unknown permit type [%d] detected", permitType));
	}

	public static int decodePermitType(String permitType) throws DfException {
		if (permitType == null) { throw new IllegalArgumentException("Must provide a permit type to map"); }
		Integer ret = DfUtils.PERMIT_TYPES_MAP.get(permitType);
		if (ret == null) { throw new DfException(
			String.format("Unknown permit type value [%s] detected", permitType)); }
		return ret;
	}

	public static IDfStore getStore(IDfSession session, String name) throws DfException {
		if (session == null) { throw new IllegalArgumentException("Must provide a session to seek the store with"); }
		if (name == null) { throw new IllegalArgumentException("Must provide a store name to look for"); }
		return IDfStore.class.cast(
			session.getObjectByQualification(String.format("dm_store where name = %s", DfUtils.quoteString(name))));
	}

	/**
	 * Runs a dctm job by given name.
	 *
	 * @param dctmSession
	 *            the dctm session
	 * @param jobName
	 *            the job name
	 * @throws DfException
	 *             the df exception
	 */
	public static void runDctmJob(IDfSession dctmSession, String jobName) throws DfException {
		// Set run_now attribute of a job to true to run a job.
		String qualification = String.format("dm_job where object_name = %s", DfUtils.quoteString(jobName));
		IDfSysObject oJob = (IDfSysObject) dctmSession.getObjectByQualification(qualification);
		DfUtils.lockObject(DfUtils.LOG, oJob);
		oJob.setBoolean(DctmConstant.RUN_NOW, true);
		oJob.save();
	}

	public static <T extends IDfPersistentObject> T lockObject(T obj) throws DfException {
		return DfUtils.lockObject(null, obj);
	}

	public static <T extends IDfPersistentObject> T lockObject(Logger log, T obj) throws DfException {
		if (obj == null) { return null; }
		log = Tools.coalesce(log, DfUtils.LOG);
		boolean ok = false;
		final String objectId = obj.getObjectId().getId();
		final String objectClass = obj.getClass().getSimpleName();
		try {
			if (obj.getSession().isTransactionActive()) {
				if (log.isTraceEnabled()) {
					log.trace(String.format("LOCKING OBJECT [%s](%s)", objectId, objectClass));
				}
				obj.lock();
				if (log.isTraceEnabled()) {
					log.trace(String.format("SUCCESSFULLY LOCKED OBJECT [%s](%s)", objectId, objectClass));
				}
			}
			ok = true;
			return obj;
		} finally {
			if (!ok) {
				log.error(String.format("ERROR LOCKING OBJECT WITH ID [%s](%s)", objectId, objectClass));
			}
		}
	}

	public static String quoteString(String str) {
		if (str == null) { return null; }
		return String.format("'%s'", str.replace("'", "''"));
	}

	public static String sqlQuoteString(String str) {
		if (str == null) { return null; }
		return String.format("''%s''", str.replace("'", "''''"));
	}
}