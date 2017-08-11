/**
 *
 */

package com.armedia.caliente.engine.documentum.exporter;

import java.util.Collections;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;

import com.armedia.caliente.engine.SessionFactory;
import com.armedia.caliente.engine.WarningTracker;
import com.armedia.caliente.engine.documentum.DctmSessionFactory;
import com.armedia.caliente.engine.documentum.DctmSessionWrapper;
import com.armedia.caliente.engine.documentum.DctmTranslator;
import com.armedia.caliente.engine.documentum.common.DctmCommon;
import com.armedia.caliente.engine.documentum.common.Setting;
import com.armedia.caliente.engine.exporter.ExportEngine;
import com.armedia.caliente.engine.exporter.ExportTarget;
import com.armedia.caliente.store.CmfAttributeTranslator;
import com.armedia.caliente.store.CmfContentStore;
import com.armedia.caliente.store.CmfDataType;
import com.armedia.caliente.store.CmfObjectStore;
import com.armedia.caliente.store.CmfTypeMapper;
import com.armedia.caliente.tools.CmfCrypt;
import com.armedia.caliente.tools.dfc.DctmCrypto;
import com.armedia.commons.dfc.util.DfUtils;
import com.armedia.commons.dfc.util.DfValueFactory;
import com.armedia.commons.utilities.CfgTools;
import com.armedia.commons.utilities.CloseableIterator;
import com.documentum.fc.client.IDfQuery;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.common.IDfValue;

/**
 * @author diego
 *
 */
public class DctmExportEngine extends
	ExportEngine<IDfSession, DctmSessionWrapper, IDfValue, DctmExportContext, DctmExportContextFactory, DctmExportDelegateFactory> {

	private static final Set<String> TARGETS = Collections.singleton(DctmCommon.TARGET_NAME);

	public DctmExportEngine() {
		super(new DctmCrypto(), true);
	}

	@Override
	protected void findExportResults(IDfSession session, CfgTools configuration, DctmExportDelegateFactory factory,
		TargetSubmitter submitter) throws Exception {
		if (session == null) { throw new IllegalArgumentException(
			"Must provide a session through which to retrieve the results"); }
		String dql = configuration.getString(Setting.DQL);
		if (dql == null) { throw new Exception("Must provide the DQL to query with"); }
		final int batchSize = configuration.getInteger(Setting.EXPORT_BATCH_SIZE);
		CloseableIterator<ExportTarget> it = new DctmExportTargetIterator(
			DfUtils.executeQuery(session, dql.toString(), IDfQuery.DF_EXECREAD_QUERY, batchSize));
		try {
			while (it.hasNext()) {
				submitter.submit(it.next());
			}
		} finally {
			IOUtils.closeQuietly(it);
		}
	}

	@Override
	public CmfAttributeTranslator<IDfValue> getTranslator() {
		return DctmTranslator.INSTANCE;
	}

	@Override
	protected SessionFactory<IDfSession> newSessionFactory(CfgTools config, CmfCrypt crypto) throws Exception {
		return new DctmSessionFactory(config, crypto);
	}

	@Override
	protected DctmExportContextFactory newContextFactory(IDfSession session, CfgTools cfg,
		CmfObjectStore<?, ?> objectStore, CmfContentStore<?, ?, ?> streamStore, CmfTypeMapper typeMapper, Logger output,
		WarningTracker warningTracker) throws Exception {
		return new DctmExportContextFactory(this, cfg, session, objectStore, streamStore, output, warningTracker);
	}

	@Override
	protected DctmExportDelegateFactory newDelegateFactory(IDfSession session, CfgTools cfg) throws Exception {
		return new DctmExportDelegateFactory(this, cfg);
	}

	@Override
	protected Set<String> getTargetNames() {
		return DctmExportEngine.TARGETS;
	}

	@Override
	protected IDfValue getValue(CmfDataType type, Object value) {
		return DfValueFactory.newValue(DctmTranslator.translateType(type).getDfConstant(), value);
	}

	public static ExportEngine<?, ?, ?, ?, ?, ?> getExportEngine() {
		return ExportEngine.getExportEngine(DctmCommon.TARGET_NAME);
	}
}