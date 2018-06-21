package com.armedia.caliente.cli.caliente.newlauncher.dctm;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.chemistry.opencmis.commons.impl.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.text.StrSubstitutor;
import org.apache.commons.lang3.time.DateFormatUtils;

import com.armedia.caliente.cli.caliente.cfg.CLIParam;
import com.armedia.caliente.cli.caliente.cfg.Setting;
import com.armedia.caliente.cli.caliente.exception.CalienteException;
import com.armedia.caliente.cli.caliente.launcher.AbstractCalienteModule_export;
import com.armedia.caliente.engine.dfc.DctmSetting;
import com.armedia.caliente.engine.dfc.DocumentumOrganizationStrategy;
import com.armedia.caliente.engine.dfc.exporter.DctmExportEngine;
import com.armedia.caliente.engine.exporter.ExportEngineListener;
import com.armedia.commons.dfc.pool.DfcSessionPool;
import com.armedia.commons.dfc.util.DfUtils;
import com.armedia.commons.utilities.ConfigurationSetting;
import com.documentum.fc.client.IDfCollection;
import com.documentum.fc.client.IDfDocument;
import com.documentum.fc.client.IDfFolder;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.client.IDfUser;
import com.documentum.fc.common.DfException;
import com.documentum.fc.common.IDfTime;

public class Caliente_export extends AbstractCalienteModule_export implements ExportEngineListener {

	private static final String DATE_FORMAT = "yyyy/MM/dd HH:mm:ss";
	private static final String DATE_FORMAT_DQL = IDfTime.DF_TIME_PATTERN26; // DQL-friendly syntax
	private static final String DATE_FORMAT_UTC = String.format("%s 'UTC'", Caliente_export.DATE_FORMAT);

	private static final String JOB_EXTENSION = "cmf.xml";
	private static final Pattern OBJECT_TYPE_FINDER = Pattern.compile("(?:\\bselect\\s+\\w+\\s+from\\s+(\\w+)\\b)",
		Pattern.CASE_INSENSITIVE);
	private static final Pattern ORDER_BY_FINDER = Pattern.compile("(?:\\border\\s+by\\b)", Pattern.CASE_INSENSITIVE);
	private static final Pattern ENABLE_FINDER = Pattern.compile("(?:\\benable\\b)", Pattern.CASE_INSENSITIVE);
	private static final String FIXED_PREDICATE_6_6 = "select CMF_WRAP_${objectType}.r_object_id from ${objectType} CMF_WRAP_${objectType}, ( ${baseDql} ) CMF_SUBQ_${objectType} where CMF_WRAP_${objectType}.r_object_id = CMF_SUBQ_${objectType}.r_object_id and CMF_WRAP_${objectType}.${dateColumn} >= DATE(${dateValue}, ${dateFormat})";
	private static final String FIXED_PREDICATE_6 = "select r_object_id from ${objectType} where r_object_id in ( ${baseDql} ) and ${dateColumn} >= DATE(${dateValue}, ${dateFormat})";
	private static final String DATE_CHECK_DQL = "select date(now) from dm_server_config";
	private static final long SECONDS = (60 * 1000);

	/**
	 * The from and where clause of the export query that runs periodically. The application will
	 * combine the select clause listed above with this from and where clauses to build the complete
	 * dql query. Please note that this clause will be ignored when the export is running in the
	 * adhoc mode. In that case the from and where clauses are specified in the properties file.
	 */
	private static final String DEFAULT_PREDICATE = "dm_sysobject where (TYPE(\"dm_folder\") or TYPE(\"dm_document\")) "
		+ "and not folder('/System', descend) and not folder('/Temp', descend) ";

	private DfcSessionPool pool = null;
	private IDfSession session = null;

	public Caliente_export() throws Throwable {
		super(DctmExportEngine.getExportEngine());
	}

	@Override
	protected void customizeSettings(Map<String, Object> settings) throws CalienteException {
		if (this.server != null) {
			settings.put(DctmSetting.DOCBASE.getLabel(), this.server);
		}
	}

	@Override
	protected void prepareState(Map<String, Object> settings) throws CalienteException {
		try {
			this.pool = new DfcSessionPool(settings);
			this.session = this.pool.acquireSession();
		} catch (Exception e) {
			throw new CalienteException("Failed to initialize the connection pool or get the primary session", e);
		}
	}

	@Override
	protected void cleanupState() {
		if (this.session != null) {
			this.pool.releaseSession(this.session);
		}
		if (this.pool != null) {
			this.pool.close();
		}
	}

	private String getJobQualification(String jobName, IDfFolder exportFolder) throws DfException {
		final String fileName = String.format("%s.%s", jobName.toLowerCase(), Caliente_export.JOB_EXTENSION);
		return String.format("dm_document where object_name = %s and folder(ID(%s))", DfUtils.quoteString(fileName),
			DfUtils.quoteString(exportFolder.getObjectId().getId()));
	}

	@Override
	protected Map<String, Object> loadSettings(String jobName) throws CalienteException {

		try {
			try {
				this.session.beginTrans();

				IDfFolder exportFolder = getCalienteStateFolder(false);
				if (exportFolder == null) { return null; }

				final String qualification = getJobQualification(jobName, exportFolder);
				final IDfDocument doc = IDfDocument.class.cast(this.session.getObjectByQualification(qualification));
				Map<String, Object> settings = null;
				if (doc != null) {
					settings = new HashMap<>();
					InputStream in = doc.getContent();
					Properties props = new Properties();
					try {
						props.loadFromXML(in); // TODO Auto-generated method stub

					} finally {
						IOUtils.closeQuietly(in);
					}
					for (Object o : props.keySet()) {
						settings.put(o.toString(), props.get(o));
					}
				}
				return settings;
			} finally {
				try {
					this.session.abortTrans();
				} catch (DfException e) {
					this.log.error(String.format(
						"Exception caught while rolling back the transaction for loading the export metadata for job [%s]",
						jobName), e);
				}
			}
		} catch (Exception e) {
			throw new CalienteException(
				String.format("Exception caught loading the export settings for job [%s]", jobName), e);
		}
	}

	@Override
	protected Map<String, Object> loadDefaultSettings() throws CalienteException {
		Map<String, Object> settings = super.loadDefaultSettings();

		String predicate = Setting.EXPORT_PREDICATE.getString();
		if (StringUtils.isBlank(predicate)) {
			predicate = Caliente_export.DEFAULT_PREDICATE;
		}

		String dql = String.format("select r_object_id from %s", predicate);
		settings.put(AbstractCalienteModule_export.BASE_SELECTOR, dql);
		return settings;
	}

	@Override
	protected boolean storeSettings(String jobName, Map<String, Object> settings, Date exportStart, Date exportEnd)
		throws CalienteException {

		try {
			/**
			 * Now, we try to set the last export date
			 */
			boolean ok = false;
			this.session.beginTrans();
			try {
				final IDfFolder exportFolder = getCalienteStateFolder(true);
				final String qualification = getJobQualification(jobName, exportFolder);
				IDfDocument doc = IDfDocument.class.cast(this.session.getObjectByQualification(qualification));
				Map<String, Object> m = new HashMap<>();
				if ((settings != null) && !settings.isEmpty()) {
					m.putAll(settings);
				}

				m.put(AbstractCalienteModule_export.EXPORT_START,
					DateFormatUtils.formatUTC(exportStart, Caliente_export.DATE_FORMAT_UTC));
				m.put(AbstractCalienteModule_export.EXPORT_END,
					DateFormatUtils.formatUTC(exportEnd, Caliente_export.DATE_FORMAT_UTC));

				Properties p = new Properties();
				for (String s : m.keySet()) {
					if (s == null) {
						continue;
					}
					Object v = m.get(s);
					if (v == null) {
						continue;
					}
					p.put(s, v.toString());
				}

				ByteArrayOutputStream out = new ByteArrayOutputStream();
				p.storeToXML(out, String.format("Configurations for sync job [%s]", jobName), "UTF-8");
				IOUtils.closeQuietly(out);

				if (doc != null) {
					doc.checkout();
					doc.setContent(out);
					doc.checkin(false, null);
				} else {
					// Create a new one & save
					doc = IDfDocument.class.cast(this.session.newObject("dm_document"));
					doc.setObjectName(String.format("%s.%s", jobName, Caliente_export.JOB_EXTENSION));
					doc.setContentType("xml");
					doc.setContent(out);
					doc.link(exportFolder.getObjectId().getId());
					doc.save();
				}

				this.session.commitTrans();
				ok = true;
			} finally {
				try {
					if (!ok) {
						this.session.abortTrans();
					}
				} catch (DfException e) {
					this.log.error("Exception caught while rolling back the transaction for saving the export metadata",
						e);
				}
			}
			return true;
		} catch (Exception e) {
			throw new CalienteException("Exception caught storing the export metadata", e);
		} // TODO Auto-generated method stub

	}

	@Override
	protected void processSettings(Map<String, Object> settings, boolean loaded, boolean resetJob)
		throws CalienteException {
		if (loaded) {
			// If there are previous settings, we need to look at BASE_SELECTOR and the dates given,
			// and based on that construct the new FINAL_SELECTOR (dql)
			// TODO: Eventually we may switch this out
			final String dateColumnName = "r_modify_date";
			// If we're resetting the job, we'll simply ignore the existing date markers
			Object startDate = (resetJob ? null : settings.get(AbstractCalienteModule_export.EXPORT_START));
			if (startDate != null) {
				// Ok...so some servers have broken configurations where their timezone info is
				// completely borked because they're configured to run in UTC time, even though
				// they're NOT on UTC time... so this is our fix for that...
				try {
					startDate = convertUTCDateToServerDate(startDate.toString());
				} catch (DfException e) {
					throw new CalienteException(
						"Failed to perform UTC date conversion (required to account for broken server configurations)",
						e);
				}

				// Find the first word - that'll be our object type
				String dql = String.valueOf(settings.get(AbstractCalienteModule_export.BASE_SELECTOR));
				Matcher m = Caliente_export.OBJECT_TYPE_FINDER.matcher(dql.toString());
				if (m.find()) {
					// If we were able to find it, then we can certainly modify the query as
					// required to safely apply the date filter
					String objectType = m.group(1);
					Map<String, Object> data = new HashMap<>();
					data.put("objectType", objectType);
					data.put("dateColumn", dateColumnName);

					// Make sure we remove any "order by" clauses that may bork up our stuff
					Matcher ob = Caliente_export.ORDER_BY_FINDER.matcher(dql);
					if (ob.find()) {
						this.log.warn(String.format(
							"Stored DQL contains an 'ORDER BY' clause, and will need to be sanitized: [%s]", dql));
						int obStart = ob.start();
						String lead = dql.substring(0, obStart);
						Matcher en = Caliente_export.ENABLE_FINDER.matcher(dql);
						if (en.find(obStart)) {
							dql = String.format("%s %s", lead, dql.substring(en.start()));
						} else {
							dql = lead;
						}
						this.log.warn(String.format("Sanitized DQL: [%s]", dql));
					}
					data.put("baseDql", dql);

					final String wrapperPattern;
					try {
						String[] version = StringUtils.split(this.session.getServerVersion());
						// Parse out the version number
						String[] numbers = StringUtils.split(version[0], '.');
						int major = Integer.valueOf(numbers[0]);
						int minor = Integer.valueOf(numbers[1]);
						if (((major == 6) && (minor >= 6)) || (major > 6)) {
							wrapperPattern = Caliente_export.FIXED_PREDICATE_6_6;
						} else {
							// We have to use IN() instead of a subquery join because it's not
							// supported in the older versions
							wrapperPattern = Caliente_export.FIXED_PREDICATE_6;
						}
					} catch (DfException e) {
						throw new CalienteException("Failed to determine the Documentum server version", e);
					}
					data.put("dateValue", DfUtils.quoteString(startDate.toString()));
					data.put("dateFormat", DfUtils.quoteString(Caliente_export.DATE_FORMAT_DQL));
					dql = StrSubstitutor.replace(wrapperPattern, data);
				}
				settings.put(AbstractCalienteModule_export.FINAL_SELECTOR, dql);
			} else {
				settings.put(AbstractCalienteModule_export.FINAL_SELECTOR,
					String.valueOf(settings.get(AbstractCalienteModule_export.BASE_SELECTOR)));
			}
		} else {
			Object dql = settings.get(AbstractCalienteModule_export.BASE_SELECTOR);
			if (dql == null) {
				dql = CLIParam.source.getString();
			}
			settings.put(AbstractCalienteModule_export.FINAL_SELECTOR, dql);
		}

		settings.put("dql", settings.get(AbstractCalienteModule_export.FINAL_SELECTOR));
	}

	private String convertUTCDateToServerDate(String utcDate) throws DfException {
		// Query the server's current date...
		IDfCollection c = DfUtils.executeQuery(this.session, Caliente_export.DATE_CHECK_DQL);
		try {
			if (!c.next()) { throw new DfException("Failed to get the date check information from the server"); }
			final Calendar localDate = Calendar.getInstance();
			final long localOffset = localDate.getTimeZone().getRawOffset() / Caliente_export.SECONDS;

			final Calendar serverDate = Calendar.getInstance();
			serverDate.setTime(c.getValueAt(0).asTime().getDate());
			final long serverOffset = (localDate.getTimeInMillis() - serverDate.getTimeInMillis())
				/ Caliente_export.SECONDS;

			// Ok we know our offset, and the server's offset relative to us, so with that
			// we can deduce the server's true offset relative to UTC...
			final long adjustmentMinutes = localOffset + serverOffset;

			// And, with that, we make the adjustment
			Calendar adjusted = Calendar.getInstance();
			DateFormat df = new SimpleDateFormat(Caliente_export.DATE_FORMAT_UTC);
			try {
				adjusted.setTime(df.parse(utcDate));
			} catch (ParseException e) {
				throw new DfException(String.format("Failed to parse the given date of [%s] with the format [%s]",
					utcDate, Caliente_export.DATE_FORMAT_UTC), e);
			}

			// We must subtract the server's UTC offset from this date, and we get our target. Since
			// it's a subtraction, we must sign-flip the adjustmenDctmt value
			adjusted.add(Calendar.MINUTE, (int) adjustmentMinutes);
			return DateFormatUtils.format(adjusted, Caliente_export.DATE_FORMAT);
		} finally {
			DfUtils.closeQuietly(c);
		}
	}

	private IDfFolder getCalienteStateFolder(boolean createIfMissing) throws DfException, CalienteException {
		final String stateFolderName = Setting.STATE_FOLDER.getString();
		final IDfUser currentUser = this.session.getUser(this.session.getLoginUserName());

		final String prefix = currentUser.getDefaultFolder();
		final IDfFolder homeFolder = this.session.getFolderByPath(prefix);
		if (homeFolder == null) { throw new CalienteException(String.format(
			"Could not locate the home folder at [%s] for user [%s] - please make sure it exists and is writable by the user",
			prefix, currentUser.getUserName())); }

		final String folderPath = String.format("%s/%s", prefix, stateFolderName);
		IDfFolder lstExportFolder = IDfFolder.class.cast(this.session.getObjectByPath(folderPath));
		if ((lstExportFolder == null) && createIfMissing) {
			// Object does not exist, create one.
			// try to locate a folder for a target repository and create one if it doesn't exist
			// target folder does not exist, create one.

			// create a folder for a target repository in this cabinet.
			lstExportFolder = IDfFolder.class.cast(this.session.newObject("dm_folder"));
			lstExportFolder.setObjectName(stateFolderName);
			lstExportFolder.link(homeFolder.getObjectId().getId());
			lstExportFolder.save();
		}
		return lstExportFolder;
	}

	@Override
	protected String getContentStrategyName() {
		return DocumentumOrganizationStrategy.NAME;
	}

	@Override
	protected ConfigurationSetting getUserSetting() {
		return DctmSetting.USERNAME;
	}

	@Override
	protected ConfigurationSetting getPasswordSetting() {
		return DctmSetting.PASSWORD;
	}
}