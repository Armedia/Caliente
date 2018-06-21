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

	private static DfcSessionPool pool = null;
	private static IDfSession session = null;

	static boolean commonConfigure(OptionValues commandValues, Map<String, Object> settings) throws CalienteException {
		try {
			DctmEngineProxy.pool = new DfcSessionPool(settings);
			DctmEngineProxy.session = DctmEngineProxy.pool.acquireSession();
		} catch (Exception e) {
			throw new CalienteException("Failed to initialize the connection pool or get the primary session", e);
		}
		return true;
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
	protected ExportEngine<?, ?, ?, ?, ?, ?> getExportEngine() {
		return DctmExportEngine.getExportEngine();
	}

	@Override
	protected DctmExporter newExporter(ExportEngine<?, ?, ?, ?, ?, ?> engine) {
		return new DctmExporter(engine);
	}

	@Override
	protected ImportEngine<?, ?, ?, ?, ?, ?> getImportEngine() {
		return DctmImportEngine.getImportEngine();
	}

	@Override
	protected DctmImporter newImporter(ImportEngine<?, ?, ?, ?, ?, ?> engine) {
		return new DctmImporter(engine);
	}

	@Override
	public Collection<? extends LaunchClasspathHelper> getClasspathHelpers() {
		return Collections.singleton(new DctmClasspathPatcher());
	}

	@Override
	public void close() throws Exception {
		if (DctmEngineProxy.session != null) {
			DctmEngineProxy.pool.releaseSession(DctmEngineProxy.session);
		}
		if (DctmEngineProxy.pool != null) {
			DctmEngineProxy.pool.close();
		}
	}
}