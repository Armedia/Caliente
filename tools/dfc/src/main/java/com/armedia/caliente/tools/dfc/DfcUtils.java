/*******************************************************************************
 * #%L
 * Armedia Caliente
 * %%
 * Copyright (C) 2013 - 2019 Armedia, LLC
 * %%
 * This file is part of the Caliente software.
 *
 * If the software was purchased under a paid Caliente license, the terms of
 * the paid license agreement will prevail.  Otherwise, the software is
 * provided under the following open source license terms:
 *
 * Caliente is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Caliente is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Caliente. If not, see <http://www.gnu.org/licenses/>.
 * #L%
 *******************************************************************************/
package com.armedia.caliente.tools.dfc;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.commons.utilities.StreamTools;
import com.armedia.commons.utilities.Tools;
import com.documentum.fc.client.DfIdNotFoundException;
import com.documentum.fc.client.DfPermit;
import com.documentum.fc.client.IDfACL;
import com.documentum.fc.client.IDfCollection;
import com.documentum.fc.client.IDfFormat;
import com.documentum.fc.client.IDfLocalTransaction;
import com.documentum.fc.client.IDfPermit;
import com.documentum.fc.client.IDfPersistentObject;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.client.IDfSysObject;
import com.documentum.fc.client.IDfTypedObject;
import com.documentum.fc.client.content.IDfContent;
import com.documentum.fc.client.content.IDfStore;
import com.documentum.fc.common.DfException;
import com.documentum.fc.common.IDfId;
import com.documentum.fc.common.IDfList;

public class DfcUtils {

	private static final boolean DEFAULT_LOCK_FETCH = true;

	private static final String ORACLE_DATETIME_PATTERN = DfcUtils
		.quoteStringForSql(DfcConstant.ORACLE_DATETIME_PATTERN);

	private static final String POSTGRES_DATETIME_PATTERN = DfcUtils
		.quoteStringForSql(DfcConstant.POSTGRES_DATETIME_PATTERN);

	@FunctionalInterface
	public static interface DfOperation {
		public void execute(IDfSession session) throws DfException;
	}

	public static class DfMessageIdFilter implements Predicate<DfException> {
		private final Set<String> ids;

		public DfMessageIdFilter(String... ids) {
			Set<String> s = new TreeSet<>();
			if ((ids != null) && (ids.length > 0)) {
				for (int i = 0; i < ids.length; i++) {
					if (!StringUtils.isEmpty(ids[i])) {
						s.add(StringUtils.upperCase(ids[i]));
					}
				}
			}
			if (s.isEmpty()) {
				this.ids = Collections.emptySet();
			} else {
				this.ids = Tools.freezeSet(new LinkedHashSet<>(s));
			}
		}

		public DfMessageIdFilter(Collection<String> ids) {
			Set<String> s = new TreeSet<>();
			if ((ids != null) && !ids.isEmpty()) {
				ids.forEach((str) -> {
					if (!StringUtils.isEmpty(str)) {
						s.add(StringUtils.upperCase(str));
					}
				});
			}
			if (s.isEmpty()) {
				this.ids = Collections.emptySet();
			} else {
				this.ids = Tools.freezeSet(new LinkedHashSet<>(s));
			}
		}

		@Override
		public boolean test(DfException e) {
			return (e != null) && this.ids.contains(StringUtils.upperCase(e.getMessageId()));
		}
	}

	public static enum DbType {
		//
		ORACLE, //
		SQLSERVER, //
		// DB2, // TODO: Enable this when we support DB2
		POSTGRES, //
		UNKNOWN, //
		//
		;

		public static DbType decode(String dbmsName) {
			if (StringUtils.isBlank(dbmsName)) { return UNKNOWN; }
			try {
				return DbType.valueOf(StringUtils.upperCase(dbmsName));
			} catch (IllegalArgumentException e) {
				return UNKNOWN;
			}
		}

		public static DbType decode(IDfSession session) throws DfException {
			return DbType
				.decode(Objects.requireNonNull(session, "Must provide a session to check against").getDBMSName());
		}

		public static DbType decode(IDfTypedObject object) throws DfException {
			return DbType.decode(Objects.requireNonNull(object, "Must provide an object to check with").getSession());
		}
	}

	public static enum Platform {
		//
		LINUX("Linux"), //
		WINDOWS("Win(32|64)", '\\'), //
		// SOLARIS("Solaris"), // TODO: Enable when we support Solaris
		// AIX("AIX"), // TODO: Enable when we support AIX
		//
		;

		private final Pattern pattern;
		private final char sep;

		private Platform(String pattern) {
			this(pattern, '/');
		}

		private Platform(String pattern, char sep) {
			this.pattern = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);
			this.sep = sep;
		}

		public static Platform decode(String serverVersionString) {
			if (serverVersionString == null) {
				throw new IllegalArgumentException("Must provide a server version string to examine");
			}
			for (Platform p : Platform.values()) {
				if (p.pattern.matcher(serverVersionString).find()) { return p; }
			}
			throw new IllegalArgumentException(
				String.format("No supported platform could be decoded from [%s]", serverVersionString));
		}

		public static Platform decode(IDfSession session) throws DfException {
			return Platform
				.decode(Objects.requireNonNull(session, "Must provide a session to check from").getServerVersion());
		}

		public static Platform decode(IDfTypedObject object) throws DfException {
			return Platform.decode(Objects.requireNonNull(object, "Must provide an object to check with").getSession());
		}
	}

	private static final Logger LOG = LoggerFactory.getLogger(DfcUtils.class);

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

	private static final Map<String, Integer> PERMISSIONS_STR_MAP;
	private static final Map<String, Integer> PERMIT_TYPES_STR_MAP;
	static {
		Map<String, Integer> strMap = new HashMap<>();
		for (Permission p : Permission.values()) {
			strMap.put(p.str, p.num);
		}
		PERMISSIONS_STR_MAP = Collections.unmodifiableMap(strMap);
		strMap = new HashMap<>();
		for (PermitType p : PermitType.values()) {
			strMap.put(p.str, p.num);
		}
		PERMIT_TYPES_STR_MAP = Collections.unmodifiableMap(strMap);
	}

	public static void closeQuietly(IDfCollection c) {
		if (c == null) { return; }
		try {
			c.close();
		} catch (DfException e) {
			// quietly swallowed
			if (DfcUtils.LOG.isTraceEnabled()) {
				DfcUtils.LOG.trace("Swallowing exception on close", e);
			}
		}
	}

	public static IDfLocalTransaction openTransaction(IDfSession session) throws DfException {
		if (session.isTransactionActive()) { return session.beginTransEx(); }
		session.beginTrans();
		return null;
	}

	public static void commitTransaction(IDfSession session, IDfLocalTransaction tx) throws DfException {
		if (tx != null) {
			session.commitTransEx(tx);
			return;
		}
		session.commitTrans();
	}

	public static void abortTransaction(IDfSession session, IDfLocalTransaction tx) throws DfException {
		if (tx != null) {
			session.abortTransEx(tx);
			return;
		}
		session.abortTrans();
	}

	public static Stream<IDfTypedObject> stream(IDfCollection c) {
		return DfcUtils.stream(c, true);
	}

	public static Stream<IDfTypedObject> stream(IDfCollection c, boolean closeOnEnd) {
		Stream<IDfTypedObject> ret = StreamTools.of(new DfcCollectionIterator(c), false);
		if (closeOnEnd) {
			ret = ret.onClose(() -> {
				try {
					c.close();
				} catch (DfException e) {
					throw new RuntimeException("Failed to close the IDfCollection instance", e);
				}
			});
		}
		return ret;
	}

	public static String getSessionId(IDfSession session) {
		if (session == null) { throw new IllegalArgumentException("Must provide a session to get the ID from"); }
		try {
			return session.getSessionId();
		} catch (DfException e) {
			return "(unknown)";
		}
	}

	public static String generateSqlDateClause(Date date, IDfSession session) throws DfException {
		// First, output to the "netural" format
		final String dateString = DateFormatUtils.formatUTC(date, DfcConstant.JAVA_SQL_DATETIME_PATTERN);
		// Now, select the database format string
		final String ret;
		DbType dbType = DbType.decode(session);
		switch (dbType) {
			case ORACLE:
				ret = String.format("TO_DATE(%s, %s)", DfcUtils.quoteStringForSql(dateString),
					DfcUtils.ORACLE_DATETIME_PATTERN);
				break;
			case SQLSERVER:
				ret = String.format("CONVERT(DATETIME, %s, %d)", DfcUtils.quoteStringForSql(dateString),
					DfcConstant.MSSQL_DATETIME_PATTERN);
				break;
			case POSTGRES:
				ret = String.format("TO_TIMESTAMP(%s, %s)", DfcUtils.quoteStringForSql(dateString),
					DfcUtils.POSTGRES_DATETIME_PATTERN);
				break;
			default:
				throw new UnsupportedOperationException(String.format("Unsupported database type [%s]", dbType));
		}
		if (DfcUtils.LOG.isTraceEnabled()) {
			DfcUtils.LOG.trace("Generated {} SQL Date string [{}] from [{}]({})", dbType, ret,
				DateFormatUtils.ISO_8601_EXTENDED_DATETIME_TIME_ZONE_FORMAT.format(date), date.getTime());
		}
		return ret;
	}

	private static final Map<String, Pair<Integer, Boolean>> EXTENDED_PERMISSIONS;
	static {
		Map<String, Pair<Integer, Boolean>> ep = new LinkedHashMap<>();
		ep.put(StringUtils.upperCase(IDfACL.DF_XPERMIT_EXECUTE_PROC_STR),
			Pair.of(0b00000000000000000000000000000001, true));
		ep.put(StringUtils.upperCase(IDfACL.DF_XPERMIT_CHANGE_LOCATION_STR),
			Pair.of(0b00000000000000000000000000000010, true));
		ep.put(StringUtils.upperCase(IDfACL.DF_XPERMIT_CHANGE_STATE_STR),
			Pair.of(0b00000000000000010000000000000000, false));
		ep.put(StringUtils.upperCase(IDfACL.DF_XPERMIT_CHANGE_PERMIT_STR),
			Pair.of(0b00000000000000100000000000000000, false));
		ep.put(StringUtils.upperCase(IDfACL.DF_XPERMIT_CHANGE_OWNER_STR),
			Pair.of(0b00000000000001000000000000000000, false));
		ep.put(StringUtils.upperCase(IDfACL.DF_XPERMIT_DELETE_OBJECT_STR),
			Pair.of(0b00000000000010000000000000000000, false));
		ep.put(StringUtils.upperCase(IDfACL.DF_XPERMIT_CHANGE_FOLDER_LINKS_STR),
			Pair.of(0b00000000000100000000000000000000, false));
		EXTENDED_PERMISSIONS = Tools.freezeMap(ep);
	}

	public static Collection<String> decodeExtendedPermission(int xpermit) {
		Collection<String> ep = new ArrayList<>();
		DfcUtils.EXTENDED_PERMISSIONS.forEach((label, pair) -> {
			int mask = pair.getLeft();
			boolean negated = pair.getRight();
			boolean set = ((xpermit & mask) != 0);
			if (negated) {
				set = !set;
			}
			if (set) {
				ep.add(label);
			}
		});
		return ep;
	}

	public static int decodeExtendedPermission(Collection<String> extendedPermissions) {
		final AtomicInteger ret = new AtomicInteger(3);
		if ((extendedPermissions != null) && !extendedPermissions.isEmpty()) {
			extendedPermissions.forEach((p) -> {
				Pair<Integer, Boolean> pair = DfcUtils.EXTENDED_PERMISSIONS.get(StringUtils.upperCase(p));
				if (pair != null) {
					int mask = pair.getLeft();
					int current = ret.get();
					if (pair.getRight()) {
						// Negated, so don't or the flag, and it
						ret.set(current & (~mask));
					} else {
						ret.set(current | mask);
					}
				}
			});
		}
		return ret.get();
	}

	public static String decodeAccessPermission(int permission) {
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
				throw new IllegalArgumentException(
					String.format("Unknown permissions value [%d] detected", permission));
		}
	}

	public static int decodeAccessPermission(String permission) {
		if (permission == null) { throw new IllegalArgumentException("Must provide a permission to map"); }
		Integer ret = DfcUtils.PERMISSIONS_STR_MAP.get(permission);
		if (ret == null) {
			throw new IllegalArgumentException(String.format("Unknown permissions value [%s] detected", permission));
		}
		return ret;
	}

	public static String decodePermitType(int permitType) {
		// We do it the "hardcoded" way here because it's MUCH faster than maps...
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
			default:
				throw new IllegalArgumentException(String.format("Unknown permit type [%d] detected", permitType));
		}
	}

	public static int decodePermitType(String permitType) {
		if (permitType == null) { throw new IllegalArgumentException("Must provide a permit type to map"); }
		Integer ret = DfcUtils.PERMIT_TYPES_STR_MAP.get(permitType);
		if (ret == null) {
			throw new IllegalArgumentException(String.format("Unknown permit type value [%s] detected", permitType));
		}
		return ret;
	}

	public static IDfStore getStore(IDfSession session, String name) throws DfException {
		if (session == null) { throw new IllegalArgumentException("Must provide a session to seek the store with"); }
		if (name == null) { throw new IllegalArgumentException("Must provide a store name to look for"); }
		return IDfStore.class.cast(
			session.getObjectByQualification(String.format("dm_store where name = %s", DfcUtils.quoteString(name))));
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
		String qualification = String.format("dm_job where object_name = %s", DfcUtils.quoteString(jobName));
		IDfSysObject oJob = IDfSysObject.class.cast(dctmSession.getObjectByQualification(qualification));
		DfcUtils.lockObject(DfcUtils.LOG, oJob);
		oJob.setBoolean(DfcConstant.RUN_NOW, true);
		oJob.save();
	}

	public static <T extends IDfPersistentObject> T lockObject(T obj) throws DfException {
		return DfcUtils.lockObject(null, obj, DfcUtils.DEFAULT_LOCK_FETCH);
	}

	public static <T extends IDfPersistentObject> T lockObject(T obj, boolean fetch) throws DfException {
		return DfcUtils.lockObject(null, obj, fetch);
	}

	public static <T extends IDfPersistentObject> T lockObject(Logger log, T obj) throws DfException {
		return DfcUtils.lockObject(log, obj, DfcUtils.DEFAULT_LOCK_FETCH);
	}

	public static <T extends IDfPersistentObject> T lockObject(Logger log, T obj, boolean fetch) throws DfException {
		if (obj == null) { return null; }
		log = Tools.coalesce(log, DfcUtils.LOG);
		boolean ok = false;
		final String objectId = obj.getObjectId().getId();
		final String objectClass = obj.getClass().getSimpleName();
		if (fetch && obj.fetch(null)) {
			log.trace("FETCHED A FRESH VERSION FOR LOCKING OF [{}]({})", objectId, objectClass);
		}
		try {
			if (obj.getSession().isTransactionActive()) {
				log.trace("LOCKING OBJECT [{}]({})", objectId, objectClass);
				obj.lockEx(true);
				log.trace("SUCCESSFULLY LOCKED OBJECT [{}]({})", objectId, objectClass);
			}
			ok = true;
			return obj;
		} finally {
			if (!ok) {
				log.error("ERROR LOCKING OBJECT WITH ID [{}]({})", objectId, objectClass);
			}
		}
	}

	public static String quoteString(String str) {
		if (str == null) { return null; }
		return String.format("'%s'", str.replace("'", "''"));
	}

	public static String quoteStringForSql(String str) {
		if (str == null) { return null; }
		return String.format("''%s''", str.replace("'", "''''"));
	}

	/**
	 * <p>
	 * Returns the same value as invoking
	 * {@link #runRetryable(IDfSession, boolean, DfOperation, Predicate) runRetryable(session,
	 * false, op, null)}.
	 * </p>
	 */
	public static DfException runRetryable(IDfSession session, DfOperation op) throws DfException {
		return DfcUtils.runRetryable(session, false, op, null);
	}

	/**
	 * <p>
	 * Returns the same value as invoking
	 * {@link #runRetryable(IDfSession, boolean, DfOperation, Predicate) runRetryable(session,
	 * false, op, filter)}.
	 * </p>
	 */
	public static DfException runRetryable(IDfSession session, DfOperation op, Predicate<DfException> filter)
		throws DfException {
		return DfcUtils.runRetryable(session, false, op, filter);
	}

	/**
	 * <p>
	 * Returns the same value as invoking
	 * {@link #runRetryable(IDfSession, boolean, DfOperation, Predicate) runRetryable(session,
	 * openTransaction, op, null)}.
	 * </p>
	 */
	public static DfException runRetryable(IDfSession session, boolean openTransaction, DfOperation op)
		throws DfException {
		return DfcUtils.runRetryable(session, openTransaction, op, null);
	}

	/**
	 * <p>
	 * Execute the given operation such that it can fail without invalidating any open transactions,
	 * and the caller can control which exceptions it tolerates as recoverable without a large,
	 * cumbersome try-catch construct, or the repetitive code for transaction management.
	 * </p>
	 * <p>
	 * If the {@code openTransaction} parameter is {@code false}, then a top-level transaction will
	 * not be initiated if one isn't already active (determined by calling
	 * {@link IDfSession#isTransactionActive()}). If the {@code openTransaction} parameter is
	 * {@code true}, then a top-level transaction will be initiated if one is not already active.
	 * Regardless of the value for {@code openTransaction}, if a top-level transaction is already
	 * active in the given {@code session}, then a local transaction will be initiated via
	 * {@link IDfSession#beginTransEx()}.
	 * </p>
	 * <p>
	 * If the operation raises an exception, and no filter is given (i.e. {@code filter} is
	 * {@code null}), then the exception is caught and returned by this method. If the operation
	 * raises an exception, and a non-{@code null} filter is given, then the exception is caught and
	 * handed to the filter's {@link Predicate#test(Object) test} method, which will determine what
	 * happens next: if the method returns {@code true}, then the exception is deemed recoverable by
	 * the caller and it'll be returned by the method. Otherwise, the exception is non-recoverable
	 * and will be raised by the method as if no special management were being performed. In either
	 * case, any overarching transaction will be protected - either because there was none active,
	 * and none was requested to be opened, or because there was one active and a local transaction
	 * was opened to attempt the call, and this local transaction was then aborted. If the operation
	 * is successful (i.e. no exceptions are raised), any transactions opened for protection will be
	 * committed.
	 * </p>
	 * <p>
	 * The upshot of this behavior is that if there's any overarching transaction active when the
	 * caller invokes this method, that transaction will remain valid almost regardless of what
	 * happens within the operation. Since this method only handles {@link DfException} instances,
	 * instances of {@link RuntimeException} or {@link Error} may still wreak havoc with your
	 * application.
	 * </p>
	 *
	 * @param session
	 * @param op
	 * @return the exception raised by the operation, or {@code null} if none was raised
	 * @throws DfException
	 *             raised if there's a problem determining if a
	 *             {@link IDfSession#isTransactionActive() transaction is active in the current
	 *             session}, or if there's a problem opening a {@link IDfSession#beginTransEx()
	 *             local transaction within the current session}
	 */
	public static DfException runRetryable(IDfSession session, boolean openTransaction, DfOperation op,
		Predicate<DfException> filter) throws DfException {
		if (op == null) { return null; }
		Objects.requireNonNull(session, "Must provide a non-null session to run the operation against");
		DfException ret = null;
		if (!openTransaction && !session.isTransactionActive()) {
			try {
				op.execute(session);
			} catch (final DfException e) {
				if ((filter != null) && !filter.test(e)) { throw e; }
				ret = e;
			}
		} else {
			final IDfLocalTransaction tx = DfcUtils.openTransaction(session);
			try {
				op.execute(session);
				DfcUtils.commitTransaction(session, tx);
			} catch (DfException e) {
				DfcUtils.abortTransaction(session, tx);
				if ((filter != null) && !filter.test(e)) { throw e; }
				ret = e;
			}
		}
		return ret;
	}

	public static Collection<IDfPermit> getPermissionsWithFallback(final IDfSysObject object) throws DfException {
		return DfcUtils.getPermissionsWithFallback(object, null);
	}

	public static Collection<IDfPermit> getPermissionsWithFallback(final IDfSysObject object,
		Predicate<DfException> exceptionFilter) throws DfException {
		final IDfSession session = Objects.requireNonNull(object, "Must provide a non-null IDfSysObject instance")
			.getSession();
		return DfcUtils.getPermissionsWithFallback(session, object, exceptionFilter);
	}

	public static Collection<IDfPermit> getPermissionsWithFallback(IDfSession session, final IDfSysObject object)
		throws DfException {
		return DfcUtils.getPermissionsWithFallback(session, object, null);
	}

	public static Collection<IDfPermit> getPermissionsWithFallback(IDfSession session, final IDfSysObject object,
		Predicate<DfException> exceptionFilter) throws DfException {
		Objects.requireNonNull(object, "Must provide a non-null IDfSysObject instance");
		if (session == null) {
			session = object.getSession();
		}

		final Collection<IDfPermit> ret = new ArrayList<>();
		DfException raised = null;

		ret.clear();
		raised = DfcUtils.runRetryable(session, (s) -> {
			IDfList list = object.getPermissions();
			final int count = list.getCount();
			for (int i = 0; i < count; i++) {
				ret.add(IDfPermit.class.cast(list.get(i)));
			}
		});
		if (raised == null) { return ret; }
		if ((exceptionFilter != null) && exceptionFilter.test(raised)) { throw raised; }

		return DfcUtils.getPermissionsWithFallback(session, object.getACL(), exceptionFilter);
	}

	public static Collection<IDfPermit> getPermissionsWithFallback(final IDfACL acl) throws DfException {
		return DfcUtils.getPermissionsWithFallback(null, acl, null);
	}

	public static Collection<IDfPermit> getPermissionsWithFallback(final IDfACL acl,
		Predicate<DfException> exceptionFilter) throws DfException {
		return DfcUtils.getPermissionsWithFallback(null, acl, exceptionFilter);
	}

	public static Collection<IDfPermit> getPermissionsWithFallback(IDfSession session, final IDfACL acl)
		throws DfException {
		return DfcUtils.getPermissionsWithFallback(session, acl, null);
	}

	public static Collection<IDfPermit> getPermissionsWithFallback(IDfSession session, final IDfACL acl,
		Predicate<DfException> exceptionFilter) throws DfException {
		Objects.requireNonNull(acl, "Must provide a non-null IDfACL instance");
		if (session == null) {
			session = acl.getSession();
		}

		final Collection<IDfPermit> ret = new ArrayList<>();
		DfException raised = null;

		// The previous strategy failed, so we move onto the next attempt which is getting the
		// object's ACL and getting the permissions from there
		ret.clear();
		raised = DfcUtils.runRetryable(session, (s) -> {
			IDfList list = acl.getPermissions();
			final int count = list.getCount();
			for (int i = 0; i < count; i++) {
				ret.add(IDfPermit.class.cast(list.get(i)));
			}
		});
		if (raised == null) { return ret; }
		if ((exceptionFilter != null) && exceptionFilter.test(raised)) { throw raised; }

		// Last chance - reconstruct the data from the ACL's attribute values...
		ret.clear();
		raised = DfcUtils.runRetryable(session, (s) -> {
			final int count = acl.getValueCount(IDfACL.ACCESSOR_NAME);
			Collection<String> valueStrings = new ArrayList<>();
			for (int i = 0; i < count; i++) {
				valueStrings.clear();
				final String accessor = acl.getRepeatingString(IDfACL.ACCESSOR_NAME, i);
				int type = acl.getRepeatingInt(IDfACL.PERMIT_TYPE, i);

				// final boolean group = acl.getRepeatingBoolean(IDfACL.IS_GROUP, i);

				boolean restriction = true;
				boolean findExtended = false;
				switch (type) {
					case IDfPermit.DF_ACCESS_PERMIT:
						restriction = false;
						// Fall-through
					case IDfPermit.DF_ACCESS_RESTRICTION:
						int permit = acl.getRepeatingInt(IDfACL.ACCESSOR_PERMIT, i);
						if (permit < 1) {
							permit = 1;
						}
						valueStrings.add(DfcUtils.decodeAccessPermission(permit));
						findExtended = true;
						break;

					case IDfPermit.DF_APPLICATION_PERMIT:
					case IDfPermit.DF_APPLICATION_RESTRICTION:
						valueStrings.add(acl.getRepeatingString(IDfACL.APPLICATION_PERMIT, i));
						break;

					case IDfPermit.DF_REQUIRED_GROUP:
					case IDfPermit.DF_REQUIRED_GROUP_SET:
						valueStrings.add(null);
						break;
				}

				for (String v : valueStrings) {
					IDfPermit result = new DfPermit();
					result.setAccessorName(accessor);
					result.setPermitType(type);
					// result.setPermitTypeString(typeString);
					if (v != null) {
						result.setPermitValue(v);
					}
					ret.add(result);
				}

				if (findExtended) {
					type = (restriction ? IDfPermit.DF_EXTENDED_RESTRICTION : IDfPermit.DF_EXTENDED_PERMIT);
					for (String v : DfcUtils
						.decodeExtendedPermission(acl.getRepeatingInt(IDfACL.ACCESSOR_XPERMIT, i))) {
						IDfPermit result = new DfPermit();
						result.setAccessorName(accessor);
						result.setPermitType(type);
						// result.setPermitTypeString(typeString);
						if (v != null) {
							result.setPermitValue(v);
						}
						ret.add(result);
					}
				}
			}
		});
		if (raised == null) { return ret; }
		if ((exceptionFilter != null) && exceptionFilter.test(raised)) { throw raised; }

		ret.clear();
		return ret;
	}

	public static String getDocbasePrefix(IDfSession session) throws DfException {
		return DfcUtils.getDocbasePrefix(session.getDocbaseId());
	}

	public static String getDocbasePrefix(String docbaseId) {
		return DfcUtils.getDocbasePrefix(Long.parseLong(docbaseId));
	}

	public static String getDocbasePrefix(long docbaseId) {
		return String.format("%08x", docbaseId);
	}

	public static String decodeDataTicket(long dataTicket) {
		return DfcUtils.decodeDataTicketImpl(null, dataTicket, null);
	}

	public static String decodeDataTicket(long dataTicket, char sep) {
		return DfcUtils.decodeDataTicketImpl(null, dataTicket, sep);
	}

	public static String decodeDataTicket(String prefix, long dataTicket) {
		return DfcUtils.decodeDataTicketImpl(prefix, dataTicket, null);
	}

	public static String decodeDataTicket(String prefix, long dataTicket, char sep) {
		return DfcUtils.decodeDataTicketImpl(prefix, dataTicket, sep);
	}

	private static String decodeDataTicketImpl(String prefix, long dataTicket, Character sep) {
		sep = Tools.coalesce(sep, File.separatorChar);
		StringBuilder result = new StringBuilder();
		if (!StringUtils.isEmpty(prefix)) {
			result.append(prefix).append(sep);
		}
		String dataTicketHex = Long.toHexString(dataTicket + 0x100000000L);
		for (int i = 0; i < dataTicketHex.length(); i += 2) {
			(i > 0 ? result.append(sep) : result).append(dataTicketHex.substring(i, i + 2));
		}
		return result.toString();
	}

	public static String getExtension(IDfSession session, IDfId format) throws DfException {
		if ((format == null) || format.isNull() || !format.isObjectId()) { return null; }
		IDfPersistentObject obj = session.getObject(format);
		if (!obj.isInstanceOf("dm_format")) { return null; }
		IDfFormat f = IDfFormat.class.cast(obj);
		return f.getDOSExtension();
	}

	public static String getFileStoreRoot(IDfSession session, IDfContent content) throws DfException {
		try {
			IDfPersistentObject obj = session.getObject(content.getStorageId());
			String root = obj.getString("root");
			if (!StringUtils.isBlank(root)) {
				obj = session.getObjectByQualification(
					String.format("dm_location where object_name = %s", DfcUtils.quoteString(root)));
				if ((obj != null) && obj.hasAttr("file_system_path")) { return obj.getString("file_system_path"); }
			}
			return String.format("(no-path-for-store-%s)", content.getStorageId());
		} catch (DfIdNotFoundException e) {
			return String.format("(store-%s-not-found)", content.getStorageId());
		}
	}

	public static String getContentLocation(IDfSession session, IDfContent content) throws DfException {
		final String prefix = DfcUtils.getDocbasePrefix(session);
		final char sep = Platform.decode(session).sep;
		String streamPath = DfcUtils.decodeDataTicket(prefix, content.getDataTicket(), sep);
		String extension = DfcUtils.getExtension(session, content.getFormatId());
		if (StringUtils.isBlank(extension)) {
			extension = StringUtils.EMPTY;
		} else {
			// Faster than String.format()
			extension = "." + extension;
		}
		String pathPrefix = DfcUtils.getFileStoreRoot(session, content);
		// This is more efficient than String.format()
		return pathPrefix + sep + streamPath + extension;
	}
}