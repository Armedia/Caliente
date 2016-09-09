package com.armedia.cmf.usermapper;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.security.GeneralSecurityException;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLSocketFactory;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.commons.dfc.pool.DfcSessionPool;
import com.documentum.fc.client.IDfGroup;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.client.IDfUser;
import com.documentum.fc.common.DfException;
import com.unboundid.ldap.sdk.LDAPConnection;
import com.unboundid.ldap.sdk.LDAPConnectionPool;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.LDAPURL;
import com.unboundid.util.ssl.SSLUtil;

public class UserMapper {
	protected static final String DFC_PROPERTIES_PROP = "dfc.properties.file";
	private static final Logger log = LoggerFactory.getLogger(UserMapper.class);

	public static final void main(String... args) {
		System.exit(UserMapper.runMain(args));
	}

	private static void outputUser(IDfSession session, DctmUser user, PrintWriter userRecords) throws Exception {
		IDfUser u = session.getUser(user.getName());
		if (u == null) {
			// WTF?!?!?
			UserMapper.log
				.warn(String.format("User [%s] disappeared on us...can't generate the record", user.getName()));
			return;
		}
		// TODO: Output the user's CSV record
		UserMapper.log.info("Retrieved user {}", u.getUserName());
	}

	private static void outputGroup(IDfSession session, DctmGroup group, Properties userMappings,
		Properties groupMappings, PrintWriter groupRecords) throws Exception {
		IDfGroup g = session.getGroup(group.getName());
		if (g == null) {
			// WTF?!?!?
			UserMapper.log
				.warn(String.format("Group [%s] disappeared on us...can't generate the record", group.getName()));
			return;
		}
		// TODO: Output the group's CSV record ... make sure to fold group names to uppercase
		UserMapper.log.info("Retrieved group {}", g.getGroupName());
	}

	private static int runMain(String... args) {
		if (!CLIParam.parse(args)) {
			// If the parameters didn't parse, we fail.
			return 1;
		}

		// final boolean debug = CLIParam.debug.isPresent();

		if (CLIParam.dfc_prop.isPresent()) {
			File f = new File(CLIParam.dfc_prop.getString("dfc.properties"));
			try {
				f = f.getCanonicalFile();
			} catch (IOException e) {
				// Do nothing...stay with the non-canonical path
				f = f.getAbsoluteFile();
			}
			String error = null;
			if ((error == null) && !f.exists()) {
				error = "does not exist";
			}
			if ((error == null) && !f.isFile()) {
				error = "is not a regular file";
			}
			if ((error == null) && !f.canRead()) {
				error = "cannot be read";
			}
			if (error == null) {
				System.setProperty(UserMapper.DFC_PROPERTIES_PROP, f.getAbsolutePath());
			} else {
				UserMapper.log.warn("The DFC properties file [{}] {} - will continue using DFC defaults",
					f.getAbsolutePath(), error);
			}
		}

		DfcSessionPool dfcPool = null;
		LDAPConnectionPool ldapPool = null;
		ExecutorService executor = null;
		try {
			final String docbase = CLIParam.docbase.getString();
			final String dctmUser = CLIParam.dctm_user.getString();
			final String dctmPass = CLIParam.dctm_pass.getString();

			try {
				dfcPool = new DfcSessionPool(docbase, dctmUser, dctmPass);
			} catch (DfException e) {
				UserMapper.log.error(
					String.format("Failed to open the session pool to docbase [%s] as [%s]", docbase, dctmUser), e);
				return 1;
			}

			Callable<LdapUserDb> ldapUserCallable = null;
			Callable<LdapGroupDb> ldapGroupCallable = null;
			if (CLIParam.ldap_url.isPresent()) {
				final String ldapUrlString = CLIParam.ldap_url.getString("ldap://");
				LDAPURL ldapUrl;
				try {
					ldapUrl = new LDAPURL(ldapUrlString);
				} catch (LDAPException e) {
					UserMapper.log.error(String.format("Failed to parse the LDAP URL [%s]", ldapUrlString), e);
					return 1;
				}

				final String bindDn = CLIParam.ldap_binddn.getString();
				final String bindPass = CLIParam.ldap_pass.getString();
				final boolean ldapOnDemand = CLIParam.ldap_on_demand.isPresent();

				final SSLSocketFactory sslSocketFactory;
				if (StringUtils.equalsIgnoreCase("ldaps", ldapUrl.getScheme())) {
					final SSLUtil sslUtil = new SSLUtil();
					try {
						sslSocketFactory = sslUtil.createSSLSocketFactory();
					} catch (GeneralSecurityException e) {
						UserMapper.log.error("Failed to obtain an SSL socket factory", e);
						return 1;
					}
				} else {
					sslSocketFactory = null;
				}

				try {
					ldapPool = new LDAPConnectionPool(
						new LDAPConnection(sslSocketFactory, ldapUrl.getHost(), ldapUrl.getPort(), bindDn, bindPass),
						2);
				} catch (LDAPException e) {
					UserMapper.log.error("Failed to connect to LDAP", e);
					return 1;
				}

				final LDAPConnectionPool pool = ldapPool;
				ldapUserCallable = new Callable<LdapUserDb>() {
					@Override
					public LdapUserDb call() throws Exception {
						return new LdapUserDb(pool, ldapOnDemand,
							CLIParam.ldap_user_basedn.getString(CLIParam.ldap_basedn.getString()));
					}
				};
				ldapGroupCallable = new Callable<LdapGroupDb>() {
					@Override
					public LdapGroupDb call() throws Exception {
						return new LdapGroupDb(pool, ldapOnDemand,
							CLIParam.ldap_group_basedn.getString(CLIParam.ldap_basedn.getString()));
					}
				};
			}

			executor = Executors.newCachedThreadPool();
			final Future<Map<String, DctmUser>> dctmUserFuture = executor.submit(DctmUser.getUserLoader(dfcPool));
			final Future<Map<String, DctmGroup>> dctmGroupFuture = executor.submit(DctmGroup.getGroupLoader(dfcPool));
			final Future<LdapUserDb> ldapUserFuture;
			if (ldapUserCallable != null) {
				ldapUserFuture = executor.submit(ldapUserCallable);
			} else {
				ldapUserFuture = null;
			}
			final Future<LdapGroupDb> ldapGroupFuture;
			if (ldapGroupCallable != null) {
				ldapGroupFuture = executor.submit(ldapGroupCallable);
			} else {
				ldapGroupFuture = null;
			}

			UserMapper.log.info("Waiting for background threads to finish");
			try {
				executor.shutdown();
				while (true) {
					try {
						if (executor.awaitTermination(5, TimeUnit.SECONDS)) {
							break;
						}
					} catch (InterruptedException e) {
						UserMapper.log.error("Interrupted waiting for job termination", e);
						return -1;
					}
				}
			} finally {
				executor = null;
			}

			Future<?>[] futures = {
				dctmUserFuture, dctmGroupFuture, ldapUserFuture, ldapGroupFuture
			};
			Throwable[] thrown = new Throwable[futures.length];
			int i = 0;
			boolean exceptionRaised = false;
			for (Future<?> f : futures) {
				if (f == null) {
					continue;
				}
				try {
					f.get();
				} catch (Exception e) {
					exceptionRaised = true;
					thrown[i] = e.getCause();
				} finally {
					i++;
				}
			}

			if (exceptionRaised) {
				for (i = 0; i < thrown.length; i++) {
					final Throwable t = thrown[i];
					if (t == null) {
						continue;
					}
					UserMapper.log.error(String.format("Exception raised while downloading databases (#%d)", i + 1), t);
				}
				return 1;
			}

			final Map<String, DctmUser> dctmUsers;
			final LdapUserDb ldapUserDb;
			final Map<String, DctmGroup> dctmGroups;
			final LdapGroupDb ldapGroupDb;
			try {
				dctmUsers = dctmUserFuture.get();
				ldapUserDb = ((ldapUserFuture != null) ? ldapUserFuture.get() : new LdapUserDb());
				dctmGroups = dctmGroupFuture.get();
				ldapGroupDb = ((ldapGroupFuture != null) ? ldapGroupFuture.get() : new LdapGroupDb());
			} catch (Exception e) {
				// This is impossible - we've already got the exceptions
				UserMapper.log.error("Impossible exception", e);
				return 1;
			}

			final PrintWriter userRecords = null;
			final PrintWriter groupRecords = null;

			final Properties userMapping = new Properties();
			final Set<String> newUsers = new LinkedHashSet<String>();
			for (String u : dctmUsers.keySet()) {
				DctmUser user = dctmUsers.get(u);
				// Look the user up by login in LDAP. If it's there, then
				// we're fine and we simply output the mapping
				LdapUser ldap = null;
				try {
					ldap = ldapUserDb.getByName(user.getLogin());
					if (ldap == null) {
						ldap = ldapUserDb.getByGuid(user.getGuid());
					}
				} catch (LDAPException e) {
					if (!ldapUserDb.isOnDemand()) {
						// This is impossible...WTF?
						throw new RuntimeException(
							"Caught an impossible exception since we're not hitting LDAP on demand", e);
					}
				}

				if (ldap != null) {
					// Output the mapping
					userMapping.setProperty(user.getName(), ldap.getLogin());
					continue;
				}

				userMapping.setProperty(user.getName(), user.getLogin());
				newUsers.add(u);
			}

			final Properties groupMapping = new Properties();
			final Set<String> newGroups = new LinkedHashSet<String>();
			for (String g : dctmGroups.keySet()) {
				DctmGroup group = dctmGroups.get(g);
				// Look the user up by login in LDAP. If it's there, then
				// we're fine and we simply output the mapping
				LdapGroup ldap = null;
				try {
					ldap = ldapGroupDb.getByName(group.getName());
					if (ldap == null) {
						ldap = ldapGroupDb.getByGuid(group.getGuid());
					}
				} catch (LDAPException e) {
					if (!ldapGroupDb.isOnDemand()) {
						// This is impossible...WTF?
						throw new RuntimeException(
							"Caught an impossible exception since we're not hitting LDAP on demand", e);
					}
				}

				if (ldap != null) {
					// We output the mapping to make sure we match the source case
					groupMapping.setProperty(group.getName(), ldap.getName());
					continue;
				}

				newGroups.add(g);
			}

			IDfSession session;
			try {
				session = dfcPool.acquireSession();
			} catch (Exception e) {
				UserMapper.log.error("Failed to open a Documentum session to handle the user/group generation", e);
				return -1;
			}
			try {
				for (String u : newUsers) {
					final DctmUser user = dctmUsers.get(u);
					// The user isn't there, so we output a new record...but first we must pull
					// all the user's data from Documentum so we can do that...
					try {
						UserMapper.outputUser(session, user, userRecords);
					} catch (Exception e) {
						throw new RuntimeException(String.format("Failed to write the record for user %s", user), e);
					}
				}
				for (String g : newGroups) {
					final DctmGroup group = dctmGroups.get(g);
					// The group isn't there, so we output a new record...but first we must pull
					// all the group's data from Documentum so we can do that...
					try {
						UserMapper.outputGroup(session, group, userMapping, groupMapping, groupRecords);
					} catch (Exception e) {
						throw new RuntimeException(String.format("Failed to write the record for group %s", group), e);
					}
				}
			} finally {
				dfcPool.releaseSession(session);
			}

			// Ok...so...write out the mapping files

			return 0;
		} finally {
			if (executor != null) {
				executor.shutdownNow();
			}
			if (dfcPool != null) {
				dfcPool.close();
			}
			if (ldapPool != null) {
				ldapPool.close();
			}
		}
	}
}