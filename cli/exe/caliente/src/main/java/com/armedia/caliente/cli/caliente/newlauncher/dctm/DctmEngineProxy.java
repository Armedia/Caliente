package com.armedia.caliente.cli.caliente.newlauncher.dctm;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import com.armedia.caliente.cli.OptionValues;
import com.armedia.caliente.cli.caliente.exception.CalienteException;
import com.armedia.caliente.cli.caliente.newlauncher.EngineInterface;
import com.armedia.caliente.cli.launcher.LaunchClasspathHelper;
import com.armedia.caliente.engine.dfc.exporter.DctmExportEngine;
import com.armedia.caliente.engine.dfc.importer.DctmImportEngine;
import com.armedia.caliente.engine.exporter.ExportEngine;
import com.armedia.caliente.engine.importer.ImportEngine;
import com.armedia.caliente.tools.CmfCrypt;
import com.armedia.caliente.tools.dfc.DctmCrypto;
import com.armedia.commons.dfc.pool.DfcSessionFactory;
import com.armedia.commons.dfc.pool.DfcSessionPool;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.common.IDfTime;

public class DctmEngineProxy extends EngineInterface {

	private static final String DATE_FORMAT = "yyyy/MM/dd HH:mm:ss";
	private static final String DATE_FORMAT_DQL = IDfTime.DF_TIME_PATTERN26; // DQL-friendly syntax
	private static final String DATE_FORMAT_UTC = String.format("%s 'UTC'", DctmEngineProxy.DATE_FORMAT);

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

	private boolean commonConfigure(OptionValues commandValues, Map<String, Object> settings) throws CalienteException {
		try {
			this.pool = new DfcSessionPool(settings);
			this.session = this.pool.acquireSession();
		} catch (Exception e) {
			throw new CalienteException("Failed to initialize the connection pool or get the primary session", e);
		}
		return true;
	}

	private class DctmExporter extends Exporter {

		private DctmExporter(ExportEngine<?, ?, ?, ?, ?, ?> engine) {
			super(engine);
		}

		@Override
		protected boolean preInitialize(Map<String, Object> settings) {
			return super.preInitialize(settings);
		}

		@Override
		protected boolean doInitialize(Map<String, Object> settings) {
			return super.doInitialize(settings);
		}

		@Override
		protected boolean postInitialize(Map<String, Object> settings) {
			return super.postInitialize(settings);
		}

		@Override
		protected void preValidateSettings(Map<String, Object> settings) throws CalienteException {
			super.preValidateSettings(settings);
		}

		@Override
		protected boolean preConfigure(OptionValues commandValues, Map<String, Object> settings)
			throws CalienteException {
			return super.preConfigure(commandValues, settings);
		}

		@Override
		protected boolean doConfigure(OptionValues commandValues, Map<String, Object> settings)
			throws CalienteException {
			if (!super.doConfigure(commandValues, settings)) { return false; }
			if (!commonConfigure(commandValues, settings)) { return false; }
			return true;
		}

		@Override
		protected void postConfigure(OptionValues commandValues, Map<String, Object> settings)
			throws CalienteException {
			super.postConfigure(commandValues, settings);
		}

		@Override
		protected void postValidateSettings(Map<String, Object> settings) throws CalienteException {
			super.postValidateSettings(settings);
		}
	}

	private class DctmImporter extends Importer {
		private DctmImporter(ImportEngine<?, ?, ?, ?, ?, ?> engine) {
			super(engine);
		}

		@Override
		protected boolean preInitialize(Map<String, Object> settings) {
			return super.preInitialize(settings);
		}

		@Override
		protected boolean doInitialize(Map<String, Object> settings) {
			return super.doInitialize(settings);
		}

		@Override
		protected boolean postInitialize(Map<String, Object> settings) {
			return super.postInitialize(settings);
		}

		@Override
		protected void preValidateSettings(Map<String, Object> settings) throws CalienteException {
			super.preValidateSettings(settings);
		}

		@Override
		protected boolean preConfigure(OptionValues commandValues, Map<String, Object> settings)
			throws CalienteException {
			return super.preConfigure(commandValues, settings);
		}

		@Override
		protected boolean doConfigure(OptionValues commandValues, Map<String, Object> settings)
			throws CalienteException {
			if (!super.doConfigure(commandValues, settings)) { return false; }
			if (!commonConfigure(commandValues, settings)) { return false; }

			String server = null;
			String user = null;
			String password = null;

			if (server != null) {
				settings.put(DfcSessionFactory.DOCBASE, server);
			}
			if (user != null) {
				settings.put(DfcSessionFactory.USERNAME, user);
			}
			if (password != null) {
				settings.put(DfcSessionFactory.PASSWORD, password);
			}

			return true;
		}

		@Override
		protected void postConfigure(OptionValues commandValues, Map<String, Object> settings)
			throws CalienteException {
			super.postConfigure(commandValues, settings);
		}

		@Override
		protected void postValidateSettings(Map<String, Object> settings) throws CalienteException {
			super.postValidateSettings(settings);
		}
	}

	public DctmEngineProxy() {
	}

	@Override
	public String getName() {
		return "dctm";
	}

	@Override
	public Set<String> getAliases() {
		return Collections.emptySet();
	}

	@Override
	public CmfCrypt getCrypt() {
		return new DctmCrypto();
	}

	@Override
	protected ExportEngine<?, ?, ?, ?, ?, ?> getExportEngine() {
		return DctmExportEngine.getExportEngine();
	}

	@Override
	protected Exporter newExporter(ExportEngine<?, ?, ?, ?, ?, ?> engine) {
		return new DctmExporter(engine);
	}

	@Override
	protected ImportEngine<?, ?, ?, ?, ?, ?> getImportEngine() {
		return DctmImportEngine.getImportEngine();
	}

	@Override
	protected Importer newImporter(ImportEngine<?, ?, ?, ?, ?, ?> engine) {
		return new DctmImporter(engine);
	}

	@Override
	public Collection<? extends LaunchClasspathHelper> getClasspathHelpers() {
		return Collections.singleton(new DctmClasspathPatcher());
	}

	@Override
	public void close() throws Exception {
		if (this.session != null) {
			this.pool.releaseSession(this.session);
		}
		if (this.pool != null) {
			this.pool.close();
		}
	}
}