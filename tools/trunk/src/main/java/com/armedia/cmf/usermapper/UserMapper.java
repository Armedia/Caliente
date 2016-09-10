package com.armedia.cmf.usermapper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLSocketFactory;
import javax.xml.bind.DatatypeConverter;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.cmf.usermapper.tools.DfUtils;
import com.armedia.commons.dfc.pool.DfcSessionPool;
import com.armedia.commons.utilities.Tools;
import com.documentum.fc.client.IDfCollection;
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
	private static final Random RANDOM = new Random(System.currentTimeMillis());

	private static final String NEWLINE = String.format("%n");

	private static final Map<String, String> USER_HEADINGS;
	private static final Map<String, String> GROUP_HEADINGS;

	private static final String FIRST_NAME = UUID.randomUUID().toString();
	private static final String LAST_NAME = UUID.randomUUID().toString();
	private static final String PASSWORD = UUID.randomUUID().toString();

	private static final String GROUP_NAME = UUID.randomUUID().toString();
	private static final String USER_MEMBERS = UUID.randomUUID().toString();
	private static final String GROUP_MEMBERS = UUID.randomUUID().toString();

	private static final Appendable NULL = new Appendable() {
		@Override
		public Appendable append(CharSequence csq) throws IOException {
			return this;
		}

		@Override
		public Appendable append(CharSequence csq, int start, int end) throws IOException {
			return this;
		}

		@Override
		public Appendable append(char c) throws IOException {
			return this;
		}
	};

	static {
		Map<String, String> m = new LinkedHashMap<String, String>();
		final String[][] userHeadings = {
			{
				"User Name", "user_login_name"
			}, {
				"First Name", UserMapper.FIRST_NAME
			}, {
				"Last Name", UserMapper.LAST_NAME
			}, {
				"E-mail Address", "user_address"
			}, {
				""
			}, {
				"Password", UserMapper.PASSWORD
			}, {
				"Company", "JSAP"
			}, {
				"Job Title",
			}, {
				"Location"
			}, {
				"Telephone"
			}, {
				"Mobile"
			}, {
				"Skype", "DCTM_HISTORIC"
			}, {
				"IM", "user_source"
			}, {
				"Google User Name"
			}, {
				"Address"
			}, {
				"Address Line 2"
			}, {
				"Address Line 3"
			}, {
				"Post Code"
			}, {
				"Telephone"
			}, {
				"Fax"
			}, {
				"Email", "user_address"
			},
		};
		for (String[] h : userHeadings) {
			m.put(h[0], (h.length > 1 ? h[1] : null));
		}
		USER_HEADINGS = Tools.freezeMap(m);

		m = new LinkedHashMap<String, String>();
		final String[][] groupHeadings = {
			{
				"Group Source", "group_source"
			}, {
				"Group Name", UserMapper.GROUP_NAME
			}, {
				"Group Display Name", "group_display_name"
			}, {
				"User Members", UserMapper.USER_MEMBERS
			}, {
				"Group Members", UserMapper.GROUP_MEMBERS
			},
		};
		for (String[] h : groupHeadings) {
			m.put(h[0], (h.length > 1 ? h[1] : null));
		}
		GROUP_HEADINGS = Tools.freezeMap(m);
	}

	public static final void main(String... args) {
		System.exit(UserMapper.runMain(args));
	}

	private static void outputUser(IDfSession session, DctmUser user, CSVPrinter userRecords) throws Exception {
		IDfUser u = session.getUser(user.getName());
		if (u == null) {
			// WTF?!?!?
			UserMapper.log
				.warn(String.format("User [%s] disappeared on us...can't generate the record", user.getName()));
			return;
		}

		UserMapper.log.info("Retrieved user {} ({})", u.getUserName(), u.getUserSourceAsString());
		if (userRecords == null) { return; }
		for (String s : UserMapper.USER_HEADINGS.keySet()) {
			String v = UserMapper.USER_HEADINGS.get(s);
			if (!StringUtils.isEmpty(v)) {
				if (u.hasAttr(v)) {
					v = u.getString(v);
				} else if (v == UserMapper.FIRST_NAME) {
					// The first name is everything but the last word
					String[] n = StringUtils.split(u.getUserName());
					if (n.length > 1) {
						v = StringUtils.join(n, ' ', 0, n.length - 1);
					} else {
						v = n[0];
					}
				} else if (v == UserMapper.LAST_NAME) {
					// The last name is only the last word...if it's only one word,
					// then it's nothing
					String[] n = StringUtils.split(u.getUserName());
					if (n.length > 1) {
						v = n[n.length - 1];
					} else {
						v = null;
					}
				} else if (v == UserMapper.PASSWORD) {
					byte[] b = new byte[64];
					UserMapper.RANDOM.nextBytes(b);
					v = DatatypeConverter.printBase64Binary(b);
				} else {
					// No attribute, and it's not a special value, so we simply leave it
					// as-is assuming it's a constant
				}

			}
			userRecords.print(Tools.coalesce(v, ""));
		}
		userRecords.println();
		userRecords.flush();
	}

	private static void outputGroup(IDfSession session, DctmGroup group, Properties userMappings,
		Properties groupMappings, CSVPrinter groupRecords) throws Exception {
		IDfGroup g = session.getGroup(group.getName());
		if (g == null) {
			// WTF?!?!?
			UserMapper.log
				.warn(String.format("Group [%s] disappeared on us...can't generate the record", group.getName()));
			return;
		}
		UserMapper.log.info("Retrieved group {} ({})", g.getGroupName(), g.getGroupSource());
		if (groupRecords == null) { return; }
		for (String s : UserMapper.GROUP_HEADINGS.keySet()) {
			String v = UserMapper.GROUP_HEADINGS.get(s);
			if (!StringUtils.isEmpty(v)) {
				if (g.hasAttr(v)) {
					v = g.getString(v);
				} else if (v == UserMapper.GROUP_NAME) {
					// The group name is to be folded to uppercase
					v = g.getGroupName().toUpperCase();
				} else if (v == UserMapper.USER_MEMBERS) {
					// Encode the user members into a single value
					IDfCollection c = g.getUsersNames();
					List<String> users = new ArrayList<String>(g.getUsersNamesCount());
					try {
						while (c.next()) {
							String n = c.getString("users_names");
							users.add(Tools.coalesce(userMappings.getProperty(n), n));
						}
					} finally {
						DfUtils.closeQuietly(c);
					}
					v = StringUtils.join(users, ',');
				} else if (v == UserMapper.GROUP_MEMBERS) {
					// Encode the group members into a single value
					IDfCollection c = g.getGroupsNames();
					List<String> groups = new ArrayList<String>(g.getGroupsNamesCount());
					try {
						while (c.next()) {
							String n = c.getString("groups_names");
							groups.add(Tools.coalesce(groupMappings.getProperty(n), n.toUpperCase()));
						}
					} finally {
						DfUtils.closeQuietly(c);
					}
					v = StringUtils.join(groups, ',');
				} else {
					// No attribute, and it's not a special value, so we simply leave it
					// as-is assuming it's a constant
				}
			}
			groupRecords.print(Tools.coalesce(v, ""));
		}
		groupRecords.println();
		groupRecords.flush();
	}

	private static CSVPrinter getCSVPrinter(String name, Set<String> headings, Map<String, CSVPrinter> records,
		String source) {
		if (StringUtils.isEmpty(source)) {
			source = "INTERNAL";
		}
		source = source.toUpperCase();
		source = source.replaceAll("\\s", "_");
		CSVPrinter ret = records.get(source);
		if (ret == null) {
			CSVFormat format = CSVFormat.DEFAULT.withRecordSeparator(UserMapper.NEWLINE);
			File f = new File(String.format("new_%s.%s.csv", name, source.toUpperCase())).getAbsoluteFile();
			try {
				try {
					f = f.getCanonicalFile();
				} catch (IOException e) {
					// Do nothing
				}
				UserMapper.log.info("Creating a new CSV file at [{}]...", f.getAbsolutePath());
				ret = new CSVPrinter(new FileWriter(f), format);
			} catch (IOException e) {
				UserMapper.log.warn(String.format("Error creating CSV file at [%s]...will use a grounded out printer",
					f.getAbsolutePath()), e);
				try {
					ret = new CSVPrinter(UserMapper.NULL, format);
				} catch (IOException e2) {
					// Impossible not writing at all
					throw new RuntimeException("Unexpected exception - no I/O happening", e2);
				}
			}
			try {
				for (String s : headings) {
					ret.print(s);
				}
				ret.println();
				ret.flush();
			} catch (IOException e) {
				throw new RuntimeException(
					String.format("Failed to initialize the new records file [%s]", f.getAbsolutePath()), e);
			}
			records.put(source, ret);
		}
		return ret;
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

			final Date startMarker = new Date();
			final String startMarkerString = DateFormatUtils.ISO_DATETIME_TIME_ZONE_FORMAT.format(startMarker);

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

			final Map<String, CSVPrinter> userRecords = new HashMap<String, CSVPrinter>();
			final Map<String, CSVPrinter> groupRecords = new HashMap<String, CSVPrinter>();

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

				groupMapping.setProperty(group.getName(), group.getName().toUpperCase());
				newGroups.add(g);
			}

			File f = null;
			FileOutputStream out = null;

			f = new File("usermap.xml").getAbsoluteFile();
			try {
				try {
					f = f.getCanonicalFile();
				} catch (IOException e) {
					// Do nothing...
				}
				out = new FileOutputStream(f);
				UserMapper.log.info("Writing out user mappings to [{}]...", f.getAbsolutePath());
				userMapping.storeToXML(out, String.format("User mappings as of %s", startMarkerString));
			} catch (IOException e) {
				f.deleteOnExit();
				UserMapper.log
					.error(String.format("Failed to write out the user mappings to [%s]", f.getAbsolutePath()), e);
				return 1;
			} finally {
				IOUtils.closeQuietly(out);
			}

			f = new File("groupmap.xml").getAbsoluteFile();
			try {
				try {
					f = f.getCanonicalFile();
				} catch (IOException e) {
					// Do nothing...
				}
				UserMapper.log.info("Writing out group mappings to [{}]...", f.getAbsolutePath());
				out = new FileOutputStream(f);
				groupMapping.storeToXML(out, String.format("Group mappings as of %s", startMarkerString));
			} catch (IOException e) {
				f.deleteOnExit();
				UserMapper.log
					.error(String.format("Failed to write out the group mappings to [%s]", f.getAbsolutePath()), e);
				return 1;
			} finally {
				IOUtils.closeQuietly(out);
			}

			IDfSession session;
			try {
				session = dfcPool.acquireSession();
			} catch (Exception e) {
				UserMapper.log.error("Failed to open a Documentum session to handle the user/group generation", e);
				return 1;
			}
			try {
				for (String u : newUsers) {
					final DctmUser user = dctmUsers.get(u);
					CSVPrinter p = UserMapper.getCSVPrinter("users", UserMapper.USER_HEADINGS.keySet(), userRecords,
						user.getSource());
					// The user isn't there, so we output a new record...but first we must pull
					// all the user's data from Documentum so we can do that...
					try {
						UserMapper.outputUser(session, user, p);
					} catch (Exception e) {
						throw new RuntimeException(String.format("Failed to write the record for user %s", user), e);
					}
				}
				for (String g : newGroups) {
					final DctmGroup group = dctmGroups.get(g);
					CSVPrinter p = UserMapper.getCSVPrinter("groups", UserMapper.GROUP_HEADINGS.keySet(), groupRecords,
						group.getSource());
					// The group isn't there, so we output a new record...but first we must pull
					// all the group's data from Documentum so we can do that...
					try {
						UserMapper.outputGroup(session, group, userMapping, groupMapping, p);
					} catch (Exception e) {
						throw new RuntimeException(String.format("Failed to write the record for group %s", group), e);
					}
				}
			} finally {
				dfcPool.releaseSession(session);

				for (CSVPrinter p : userRecords.values()) {
					IOUtils.closeQuietly(p);
				}

				for (CSVPrinter p : groupRecords.values()) {
					IOUtils.closeQuietly(p);
				}
			}
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