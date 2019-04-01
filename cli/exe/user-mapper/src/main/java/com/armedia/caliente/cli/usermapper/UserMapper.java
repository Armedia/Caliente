package com.armedia.caliente.cli.usermapper;

import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
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
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.caliente.cli.OptionValues;
import com.armedia.caliente.cli.utils.CliValuePrompt;
import com.armedia.caliente.cli.utils.DfcLaunchHelper;
import com.armedia.caliente.tools.CmfCrypt;
import com.armedia.caliente.tools.dfc.DctmCrypto;
import com.armedia.commons.dfc.pool.DfcSessionPool;
import com.armedia.commons.dfc.util.DfUtils;
import com.armedia.commons.utilities.Tools;
import com.documentum.fc.client.IDfCollection;
import com.documentum.fc.client.IDfGroup;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.client.IDfUser;
import com.documentum.fc.common.DfException;
import com.documentum.fc.common.IDfAttr;
import com.unboundid.ldap.sdk.LDAPConnection;
import com.unboundid.ldap.sdk.LDAPConnectionPool;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.LDAPURL;
import com.unboundid.util.ssl.SSLUtil;

public class UserMapper {

	private static final Logger log = LoggerFactory.getLogger(UserMapper.class);

	private static final Random RANDOM = new Random(System.currentTimeMillis());

	private static final List<String> DEFAULT_DCTM_SAM_ATTRIBUTES;
	static {
		List<String> l = new ArrayList<>();
		l.add("user_login_name");
		l.add("user_os_name");
		DEFAULT_DCTM_SAM_ATTRIBUTES = Tools.freezeList(l);
	}

	private static final String NEWLINE = String.format("%n");
	private static final String NO_FIRST_NAME = "DummyFirstName";
	private static final String NO_LAST_NAME = "DummyLastName";
	private static final String NO_EMAIL = "dummy@email.com";

	private static final Map<String, String> USER_HEADINGS;
	private static final Map<String, String> GROUP_HEADINGS;

	private static final String FIRST_NAME = UUID.randomUUID().toString();
	private static final String LAST_NAME = UUID.randomUUID().toString();
	private static final String EMAIL = UUID.randomUUID().toString();
	private static final String PASSWORD = UUID.randomUUID().toString();

	private static final String GROUP_NAME = UUID.randomUUID().toString();
	private static final String USER_MEMBERS = UUID.randomUUID().toString();
	private static final String GROUP_MEMBERS = UUID.randomUUID().toString();

	static {
		Map<String, String> m = new LinkedHashMap<>();
		final String[][] userHeadings = {
			{
				"User Name", "user_login_name"
			}, {
				"First Name", UserMapper.FIRST_NAME
			}, {
				"Last Name", UserMapper.LAST_NAME
			}, {
				"E-mail Address", UserMapper.EMAIL
			}, {
				""
			}, {
				"Password", UserMapper.PASSWORD
			}, {
				"Company", "DummyCompanyName"
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
				"Email", UserMapper.EMAIL
			},
		};
		for (String[] h : userHeadings) {
			m.put(h[0], (h.length > 1 ? h[1] : null));
		}
		USER_HEADINGS = Tools.freezeMap(m);

		m = new LinkedHashMap<>();
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

	private final DfcLaunchHelper dfcLaunchHelper;

	UserMapper(DfcLaunchHelper dfcLaunchHelper) {
		this.dfcLaunchHelper = dfcLaunchHelper;
	}

	private void outputUser(IDfSession session, DctmUser user, CSVPrinter userRecords) throws DfException, IOException {
		IDfUser u = session.getUser(user.getName());
		if (u == null) {
			// WTF?!?!?
			UserMapper.log.warn("User [{}] disappeared on us...can't generate the record", user.getName());
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
					String userName = u.getUserName();
					if (StringUtils.isEmpty(userName)) {
						v = null;
					} else {
						String[] n = StringUtils.split(userName);
						if (n.length > 1) {
							v = StringUtils.join(n, ' ', 0, n.length - 1);
						} else {
							v = n[0];
						}
					}
					if (StringUtils.isEmpty(v)) {
						v = UserMapper.NO_FIRST_NAME;
					}
				} else if (v == UserMapper.LAST_NAME) {
					// The last name is only the last word...if it's only one word,
					// then it's nothing
					String userName = u.getUserName();
					if (StringUtils.isEmpty(userName)) {
						v = null;
					} else {
						String[] n = StringUtils.split(userName);
						if (n.length > 1) {
							v = n[n.length - 1];
						} else {
							v = null;
						}
					}
					if (StringUtils.isEmpty(v)) {
						v = UserMapper.NO_LAST_NAME;
					}
				} else if (v == UserMapper.EMAIL) {
					// Email can't be empty...
					v = u.getUserAddress();
					if (StringUtils.isEmpty(v)) {
						v = UserMapper.NO_EMAIL;
					}
				} else if (v == UserMapper.PASSWORD) {
					byte[] b = new byte[24];
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

	private void outputGroup(IDfSession session, DctmGroup group, Properties userMappings, Properties groupMappings,
		CSVPrinter groupRecords) throws DfException, IOException {
		IDfGroup g = session.getGroup(group.getName());
		if (g == null) {
			// WTF?!?!?
			UserMapper.log.warn("Group [{}] disappeared on us...can't generate the record", group.getName());
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
					v = convertGroupName(groupMappings, g.getGroupName());
				} else if (v == UserMapper.USER_MEMBERS) {
					// Encode the user members into a single value
					IDfCollection c = g.getUsersNames();
					List<String> users = new ArrayList<>(g.getUsersNamesCount());
					try {
						while (c.next()) {
							String n = c.getString("users_names");
							users.add(Tools.coalesce(userMappings.getProperty(n), n));
						}
					} finally {
						DfUtils.closeQuietly(c);
					}
					v = StringUtils.join(users, '|');
				} else if (v == UserMapper.GROUP_MEMBERS) {
					// Encode the group members into a single value
					IDfCollection c = g.getGroupsNames();
					List<String> groups = new ArrayList<>(g.getGroupsNamesCount());
					try {
						while (c.next()) {
							String n = c.getString("groups_names");
							groups.add(convertGroupName(groupMappings, n));
						}
					} finally {
						DfUtils.closeQuietly(c);
					}
					v = StringUtils.join(groups, '|');
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

	private String getDocbaseSuffix(OptionValues cli, String docbase) {
		if (!cli.isPresent(CLIParam.add_docbase)) { return ""; }
		if (StringUtils.isBlank(docbase)) { return ""; }
		return String.format(".%s", docbase.toLowerCase());
	}

	private String convertGroupName(Properties groupMappings, String groupName) {
		String newName = (groupMappings != null ? groupMappings.getProperty(groupName) : null);
		if (newName == null) {
			newName = String.format("GROUP_%s", groupName.toUpperCase());
		}
		return newName;
	}

	private CSVPrinter newCSVPrinter(OptionValues cli, String name, String docbase, Set<String> headings, String source)
		throws IOException {
		if (StringUtils.isEmpty(source)) {
			source = "INTERNAL";
		}
		docbase = getDocbaseSuffix(cli, docbase);
		source = source.toUpperCase();
		source = source.replaceAll("\\s", "_");
		CSVFormat format = CSVFormat.DEFAULT.withRecordSeparator(UserMapper.NEWLINE);
		File f = new File(String.format("new_%s%s.%s.csv", name, docbase, source)).getAbsoluteFile();
		try {
			f = f.getCanonicalFile();
		} catch (IOException e) {
			// Do nothing
		}
		UserMapper.log.info("Creating a new CSV file at [{}]...", f.getAbsolutePath());
		CSVPrinter ret = new CSVPrinter(new FileWriter(f), format);
		for (String s : headings) {
			ret.print(s);
		}
		ret.println();
		ret.flush();
		return ret;
	}

	private int writeMappings(OptionValues cli, String startMarkerString, String docbase, Properties userMapping,
		Properties groupMapping) {
		File mapFile = null;
		;
		docbase = getDocbaseSuffix(cli, docbase);
		mapFile = Tools.canonicalize(new File(String.format("usermap%s.xml", docbase)));
		try (FileOutputStream out = new FileOutputStream(mapFile)) {
			UserMapper.log.info("Writing out user mappings to [{}]...", mapFile.getAbsolutePath());
			userMapping.storeToXML(out, String.format("User mappings as of %s", startMarkerString));
			UserMapper.log.info("User mappings written out to [{}]...", mapFile.getAbsolutePath());
		} catch (IOException e) {
			mapFile.deleteOnExit();
			UserMapper.log.error("Failed to write out the user mappings to [{}]", mapFile.getAbsolutePath(), e);
			return 1;
		}

		mapFile = Tools.canonicalize(new File(String.format("groupmap%s.xml", docbase)));
		try (FileOutputStream out = new FileOutputStream(mapFile)) {
			UserMapper.log.info("Writing out group mappings to [{}]...", mapFile.getAbsolutePath());
			;
			groupMapping.storeToXML(out, String.format("Group mappings as of %s", startMarkerString));
			UserMapper.log.info("Group mappings written out to [{}]...", mapFile.getAbsolutePath());
		} catch (IOException e) {
			mapFile.deleteOnExit();
			UserMapper.log.error("Failed to write out the group mappings to [{}]", mapFile.getAbsolutePath(), e);
			return 1;
		}

		return 0;
	}

	private abstract class RecordWorker<P extends DctmPrincipal> implements Runnable {
		protected final P principal;
		protected final CSVPrinter printer;
		protected final DfcSessionPool pool;

		protected RecordWorker(P principal, CSVPrinter printer, DfcSessionPool pool) {
			this.principal = principal;
			this.printer = printer;
			this.pool = pool;
		}

		@Override
		public final void run() {
			// The user isn't there, so we output a new record...but first we must pull
			// all the user's data from Documentum so we can do that...
			IDfSession session = null;
			try {
				session = this.pool.acquireSession();
				synchronized (this.printer) {
					// Make sure we block out the printer while we work.
					doWork(session);
				}
			} catch (Exception e) {
				UserMapper.log.error("Exception caught attempting to store {}", this.principal, e);
				return;
			} finally {
				if (session != null) {
					this.pool.releaseSession(session);
				}
			}
		}

		protected abstract void doWork(IDfSession session) throws Exception;
	}

	private class UserWorker extends RecordWorker<DctmUser> {

		protected UserWorker(DctmUser principal, CSVPrinter printer, DfcSessionPool pool) {
			super(principal, printer, pool);
		}

		@Override
		protected void doWork(IDfSession session) throws Exception {
			outputUser(session, this.principal, this.printer);
		}
	}

	private class GroupWorker extends RecordWorker<DctmGroup> {

		private final Properties userMapping;
		private final Properties groupMapping;

		protected GroupWorker(DctmGroup principal, CSVPrinter printer, DfcSessionPool pool, Properties userMapping,
			Properties groupMapping) {
			super(principal, printer, pool);
			this.userMapping = userMapping;
			this.groupMapping = groupMapping;
		}

		@Override
		protected void doWork(IDfSession session) throws Exception {
			outputGroup(session, this.principal, this.userMapping, this.groupMapping, this.printer);
		}
	}

	private Set<String> getMappingAttributes(OptionValues cli, DfcSessionPool pool) throws DfException {
		List<String> attributes = cli.getStrings(CLIParam.dctm_sam, UserMapper.DEFAULT_DCTM_SAM_ATTRIBUTES);
		// Shortcut - if there's nothing to validate, don't bother validating...
		if (attributes.isEmpty()) { return Collections.emptySet(); }

		Set<String> candidates = new LinkedHashSet<>(attributes);
		Set<String> finalAttributes = new LinkedHashSet<>();
		IDfSession session = pool.acquireSession();
		try {
			// Who is the current user? Use that as a validation point...
			IDfUser user = session.getUser(session.getLoginUserName());
			if (user == null) {
				throw new DfException(String.format("Failed to locate the current session's user object [%s]",
					session.getLoginUserName()));
			}
			for (String attributeName : candidates) {
				if (StringUtils.isEmpty(attributeName)) {
					UserMapper.log.warn("Blank attribute name detected, ignoring it");
					continue;
				}
				if (!user.hasAttr(attributeName)) {
					UserMapper.log.warn("The attribute [{}] is not part of the user object type, ignoring it",
						attributeName);
					continue;
				}
				if (user.isAttrRepeating(attributeName)) {
					UserMapper.log.warn("The attribute [{}] is a repeating attribute, ignoring it", attributeName);
					continue;
				}
				switch (user.getAttrDataType(attributeName)) {
					case IDfAttr.DM_STRING:
					case IDfAttr.DM_ID:
						break;
					default:
						UserMapper.log.warn("The attribute [{}] is not a string-equivalent attribute, ignoring it",
							attributeName);
						continue;
				}
				finalAttributes.add(attributeName);
			}
		} finally {
			pool.releaseSession(session);
		}
		return Tools.freezeSet(finalAttributes);
	}

	protected int run(final OptionValues cli) throws Exception {
		DfcSessionPool dfcPool = null;
		LDAPConnectionPool ldapPool = null;
		ExecutorService executor = null;
		try {
			final String docbase = this.dfcLaunchHelper.getDfcDocbase(cli);
			final String dctmUser = this.dfcLaunchHelper.getDfcUser(cli);
			final String dctmPass = this.dfcLaunchHelper.getDfcPassword(cli);

			try {
				dfcPool = new DfcSessionPool(docbase, dctmUser, new DctmCrypto().decrypt(dctmPass));
			} catch (DfException e) {
				UserMapper.log.error("Failed to open the session pool to docbase [{}] as [{}]", docbase, dctmUser, e);
				return 1;
			}

			// First things first: validate the list of attributes provided against those available
			// in the user type. They must be string or ID-valued, and non-repeating
			final Set<String> mappingAttributes;
			try {
				mappingAttributes = getMappingAttributes(cli, dfcPool);
			} catch (Exception e) {
				UserMapper.log.error("Failed to validate the mapping attributes provided", e);
				return 1;
			}
			if (mappingAttributes.isEmpty()) {
				UserMapper.log.error("No mapping attributes specified, cannot continue");
				return 1;
			}

			UserMapper.log.info("Will use the following attributes (in order) to map to sAMAccountName: {}",
				mappingAttributes);

			Callable<LdapUserDb> ldapUserCallable = null;
			Callable<LdapGroupDb> ldapGroupCallable = null;
			if (cli.isPresent(CLIParam.ldap_url)) {
				final String ldapUrlString = cli.getString(CLIParam.ldap_url, "ldap://");
				LDAPURL ldapUrl;
				try {
					ldapUrl = new LDAPURL(ldapUrlString);
				} catch (LDAPException e) {
					UserMapper.log.error("Failed to parse the LDAP URL [{}]", ldapUrlString, e);
					return 1;
				}

				final String bindDn = cli.getString(CLIParam.ldap_binddn);
				final String bindPass = CliValuePrompt.getPasswordString(cli, CLIParam.ldap_pass,
					"Please enter the LDAP Password for DN [%s] at %s: ", Tools.coalesce(bindDn, ""),
					ldapUrl.toString());

				final boolean ldapOnDemand = cli.isPresent(CLIParam.ldap_on_demand);

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
					ldapPool = new LDAPConnectionPool(new LDAPConnection(sslSocketFactory, ldapUrl.getHost(),
						ldapUrl.getPort(), bindDn, new CmfCrypt().decrypt(bindPass)), 2);
				} catch (LDAPException e) {
					UserMapper.log.error("Failed to connect to LDAP", e);
					return 1;
				}

				final LDAPConnectionPool pool = ldapPool;
				ldapUserCallable = new Callable<LdapUserDb>() {
					@Override
					public LdapUserDb call() throws Exception {
						return new LdapUserDb(pool, ldapOnDemand,
							cli.getString(CLIParam.ldap_user_basedn, cli.getString(CLIParam.ldap_basedn)));
					}
				};
				ldapGroupCallable = new Callable<LdapGroupDb>() {
					@Override
					public LdapGroupDb call() throws Exception {
						return new LdapGroupDb(pool, ldapOnDemand,
							cli.getString(CLIParam.ldap_group_basedn, cli.getString(CLIParam.ldap_basedn)));
					}
				};
			}

			executor = Executors.newCachedThreadPool();

			final Date startMarker = new Date();
			final String startMarkerString = DateFormatUtils.ISO_8601_EXTENDED_DATETIME_TIME_ZONE_FORMAT
				.format(startMarker);

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

			{
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
						UserMapper.log.error("Exception raised while downloading databases (#{})", i + 1, t);
					}
					return 1;
				}
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

			final Set<String> userSources = new TreeSet<>();
			final Set<String> groupSources = new TreeSet<>();

			final Properties userMapping = new Properties();
			final Set<String> newUsers = new LinkedHashSet<>();
			for (String u : dctmUsers.keySet()) {
				DctmUser user = dctmUsers.get(u);
				// Look the user up by login in LDAP. If it's there, then
				// we're fine and we simply output the mapping
				LdapUser ldap = null;
				try {
					for (String attribute : mappingAttributes) {
						final String value = user.getAttribute(attribute);
						ldap = ldapUserDb.getByName(value);
						if (ldap != null) {
							break;
						}
					}

					// If we have no match by any of the other attributes, we attempt to match
					// by GUID...
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
				userSources.add(user.getSource());
			}

			final Properties groupMapping = new Properties();
			final Set<String> newGroups = new LinkedHashSet<>();
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

				groupMapping.setProperty(group.getName(), convertGroupName(null, group.getName()));
				newGroups.add(g);
				groupSources.add(group.getSource());
			}

			int ret = writeMappings(cli, startMarkerString, docbase, userMapping, groupMapping);
			if (ret != 0) { return ret; }

			final Map<String, CSVPrinter> userRecords = new HashMap<>();
			final Map<String, CSVPrinter> groupRecords = new HashMap<>();
			try {
				for (String source : userSources) {
					userRecords.put(source,
						newCSVPrinter(cli, "users", docbase, UserMapper.USER_HEADINGS.keySet(), source));
				}
				for (String source : groupSources) {
					groupRecords.put(source,
						newCSVPrinter(cli, "groups", docbase, UserMapper.GROUP_HEADINGS.keySet(), source));
				}
			} catch (IOException e) {
				UserMapper.log.error("Failed to initialize the CSV files", e);
				return 1;
			}

			try {
				executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
				List<Future<?>> futures = new ArrayList<>(newUsers.size() + newGroups.size());
				List<DctmPrincipal> principals = new ArrayList<>(newUsers.size() + newGroups.size());
				for (String u : newUsers) {
					DctmUser user = dctmUsers.get(u);
					futures.add(executor.submit(new UserWorker(user, userRecords.get(user.getSource()), dfcPool)));
					principals.add(user);
				}
				for (String g : newGroups) {
					final DctmGroup group = dctmGroups.get(g);
					futures.add(executor.submit(new GroupWorker(group, groupRecords.get(group.getSource()), dfcPool,
						userMapping, groupMapping)));
					principals.add(group);
				}

				UserMapper.log.info("Waiting for background tasks to finish");
				executor.shutdown();
				while (true) {
					try {
						if (executor.awaitTermination(5, TimeUnit.SECONDS)) {
							break;
						}
					} catch (InterruptedException e) {
						UserMapper.log.error("Interrupted waiting for job termination", e);
						executor.shutdownNow();
						return 1;
					}
				}

				Map<Integer, Throwable> thrown = new TreeMap<>();
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
						thrown.put(i, e);
					} finally {
						i++;
					}
				}

				if (exceptionRaised) {
					for (Integer key : thrown.keySet()) {
						final Throwable t = thrown.get(key);
						if (t == null) {
							continue;
						}
						int k = key.intValue();
						if ((k < 0) || (k >= principals.size())) {
							// ?!?!? Out of bounds
							UserMapper.log.error("Unidentified exception caught ({})", i, t);
							continue;
						}
						DctmPrincipal p = principals.get(key.intValue());
						UserMapper.log.error("Exception raised while processing {}", p, t);
					}
					return 1;
				}
			} finally {
				for (CSVPrinter p : userRecords.values()) {
					UserMapper.closeQuietly(p);
				}

				for (CSVPrinter p : groupRecords.values()) {
					UserMapper.closeQuietly(p);
				}
			}
			UserMapper.log.info("File generation completed");
			return 0;
		} finally

		{
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

	private static void closeQuietly(Closeable c) {
		if (c != null) {
			try {
				c.close();
			} catch (Exception e) {
				// Do nothing...
			}
		}
	}
}