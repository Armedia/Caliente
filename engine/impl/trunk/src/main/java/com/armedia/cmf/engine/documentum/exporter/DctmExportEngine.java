/**
 *
 */

package com.armedia.cmf.engine.documentum.exporter;

import java.util.Collections;
import java.util.Iterator;
import java.util.Set;

import com.armedia.cmf.engine.SessionFactory;
import com.armedia.cmf.engine.documentum.DctmSessionFactory;
import com.armedia.cmf.engine.documentum.DctmSessionWrapper;
import com.armedia.cmf.engine.documentum.DctmTranslator;
import com.armedia.cmf.engine.documentum.DfUtils;
import com.armedia.cmf.engine.documentum.DfValueFactory;
import com.armedia.cmf.engine.documentum.common.DctmCommon;
import com.armedia.cmf.engine.documentum.common.Setting;
import com.armedia.cmf.engine.exporter.ExportEngine;
import com.armedia.cmf.engine.exporter.ExportTarget;
import com.armedia.cmf.storage.ObjectStorageTranslator;
import com.armedia.cmf.storage.StoredDataType;
import com.armedia.commons.utilities.CfgTools;
import com.documentum.fc.client.IDfQuery;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.common.IDfValue;

/**
 * @author diego
 *
 */
public class DctmExportEngine extends ExportEngine<IDfSession, DctmSessionWrapper, IDfValue, DctmExportContext> {

	private static final Set<String> TARGETS = Collections.singleton(DctmCommon.TARGET_NAME);

	public DctmExportEngine() {
	}

	@Override
	protected Iterator<ExportTarget> findExportResults(IDfSession session, CfgTools configuration) throws Exception {
		if (session == null) { throw new IllegalArgumentException(
			"Must provide a session through which to retrieve the results"); }
		String dql = configuration.getString(Setting.DQL);
		if (dql == null) { throw new Exception(String.format("Must provide the DQL to query with", dql)); }
		final int batchSize = configuration.getInteger(Setting.EXPORT_BATCH_SIZE);
		return new DctmExportTargetIterator(DfUtils.executeQuery(session, dql.toString(), IDfQuery.DF_EXECREAD_QUERY,
			batchSize));
	}

	@Override
	public ObjectStorageTranslator<IDfValue> getTranslator() {
		return DctmTranslator.INSTANCE;
	}

	@Override
	protected SessionFactory<IDfSession> newSessionFactory(CfgTools config) throws Exception {
		return new DctmSessionFactory(config);
	}

	@Override
	protected DctmExportContextFactory newContextFactory(CfgTools config) {
		return new DctmExportContextFactory(this, config);
	}

	@Override
	protected DctmExportDelegateFactory newDelegateFactory(CfgTools cfg) throws Exception {
		return new DctmExportDelegateFactory(this, cfg);
	}

	@Override
	protected Set<String> getTargetNames() {
		return DctmExportEngine.TARGETS;
	}

	@Override
	protected IDfValue getValue(StoredDataType type, Object value) {
		return DfValueFactory.newValue(type, value);
	}

	public static ExportEngine<?, ?, ?, ?> getExportEngine() {
		return ExportEngine.getExportEngine(DctmCommon.TARGET_NAME);
	}
}