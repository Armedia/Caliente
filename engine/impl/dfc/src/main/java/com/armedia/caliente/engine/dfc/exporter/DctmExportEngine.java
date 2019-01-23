/**
 *
 */

package com.armedia.caliente.engine.dfc.exporter;

import java.io.File;
import java.util.function.Consumer;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

import com.armedia.caliente.engine.SessionFactory;
import com.armedia.caliente.engine.WarningTracker;
import com.armedia.caliente.engine.dfc.DctmObjectType;
import com.armedia.caliente.engine.dfc.DctmSessionFactory;
import com.armedia.caliente.engine.dfc.DctmSessionWrapper;
import com.armedia.caliente.engine.dfc.DctmTranslator;
import com.armedia.caliente.engine.dfc.common.Setting;
import com.armedia.caliente.engine.dynamic.transformer.Transformer;
import com.armedia.caliente.engine.exporter.ExportEngine;
import com.armedia.caliente.engine.exporter.ExportTarget;
import com.armedia.caliente.store.CmfAttributeTranslator;
import com.armedia.caliente.store.CmfContentStore;
import com.armedia.caliente.store.CmfDataType;
import com.armedia.caliente.store.CmfObjectStore;
import com.armedia.caliente.tools.CmfCrypt;
import com.armedia.commons.dfc.util.DfUtils;
import com.armedia.commons.dfc.util.DfValueFactory;
import com.armedia.commons.utilities.CfgTools;
import com.armedia.commons.utilities.CloseableIterator;
import com.documentum.fc.client.IDfPersistentObject;
import com.documentum.fc.client.IDfQuery;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.common.IDfId;
import com.documentum.fc.common.IDfValue;

/**
 * @author diego
 *
 */
public class DctmExportEngine extends
	ExportEngine<IDfSession, DctmSessionWrapper, IDfValue, DctmExportContext, DctmExportContextFactory, DctmExportDelegateFactory, DctmExportEngineFactory> {

	public DctmExportEngine(DctmExportEngineFactory factory, Logger output, WarningTracker warningTracker,
		File baseData, CmfObjectStore<?, ?> objectStore, CmfContentStore<?, ?, ?> contentStore, CfgTools settings) {
		super(factory, output, warningTracker, baseData, objectStore, contentStore, settings, true);
	}

	@Override
	protected ExportTarget findExportTarget(IDfSession session, String searchKey) throws Exception {
		// The searchKey is an r_object_id value, so treat it as such
		return null;
	}

	@Override
	protected void findExportTargetsByPath(IDfSession session, CfgTools configuration,
		DctmExportDelegateFactory factory, Consumer<ExportTarget> handler, String path) throws Exception {
		IDfPersistentObject obj = session.getObjectByPath(path);
		if (obj == null) { return; }

		final IDfId id = obj.getObjectId();
		if (!obj.isInstanceOf("dm_folder")) {
			// Not a folder, so no recursion!
			handler
				.accept(new ExportTarget(DctmObjectType.decodeType(obj).getStoredObjectType(), id.getId(), id.getId()));
		}

		// If it's a folder, we morph into a query-based recursion.
		findExportTargetsByQuery(session, configuration, factory, handler,
			String.format("dm_sysobject where folder(id(%s), DESCEND)", DfUtils.quoteString(id.getId())));
	}

	@Override
	protected void findExportTargetsByQuery(IDfSession session, CfgTools configuration,
		DctmExportDelegateFactory factory, Consumer<ExportTarget> handler, String source) throws Exception {
		if (session == null) {
			throw new IllegalArgumentException("Must provide a session through which to retrieve the results");
		}

		// Turn the predicate it into a DQL query
		source = String.format("select r_object_id from %s", StringUtils.strip(source));

		final int batchSize = configuration.getInteger(Setting.EXPORT_BATCH_SIZE);
		try (CloseableIterator<ExportTarget> it = new DctmExportTargetIterator(
			DfUtils.executeQuery(session, source.toString(), IDfQuery.DF_EXECREAD_QUERY, batchSize))) {
			while (it.hasNext()) {
				handler.accept(it.next());
			}
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
		CmfObjectStore<?, ?> objectStore, CmfContentStore<?, ?, ?> streamStore, Transformer transformer, Logger output,
		WarningTracker warningTracker) throws Exception {
		return new DctmExportContextFactory(this, cfg, session, objectStore, streamStore, output, warningTracker);
	}

	@Override
	protected DctmExportDelegateFactory newDelegateFactory(IDfSession session, CfgTools cfg) throws Exception {
		return new DctmExportDelegateFactory(this, cfg);
	}

	@Override
	protected IDfValue getValue(CmfDataType type, Object value) {
		return DfValueFactory.newValue(DctmTranslator.translateType(type).getDfConstant(), value);
	}
}