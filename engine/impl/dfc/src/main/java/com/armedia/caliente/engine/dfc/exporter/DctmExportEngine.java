/**
 *
 */

package com.armedia.caliente.engine.dfc.exporter;

import java.io.File;
import java.util.Collections;
import java.util.stream.Stream;

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
import com.armedia.caliente.store.CmfObjectStore;
import com.armedia.caliente.store.CmfValue;
import com.armedia.caliente.tools.CmfCrypt;
import com.armedia.commons.dfc.util.DctmQuery;
import com.armedia.commons.dfc.util.DfUtils;
import com.armedia.commons.dfc.util.DfValueFactory;
import com.armedia.commons.utilities.CfgTools;
import com.documentum.fc.client.DfObjectNotFoundException;
import com.documentum.fc.client.IDfPersistentObject;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.client.IDfTypedObject;
import com.documentum.fc.common.DfId;
import com.documentum.fc.common.IDfId;
import com.documentum.fc.common.IDfValue;

/**
 * @author diego
 *
 */
public class DctmExportEngine extends
	ExportEngine<IDfSession, DctmSessionWrapper, IDfValue, DctmExportContext, DctmExportContextFactory, DctmExportDelegateFactory, DctmExportEngineFactory> {

	public DctmExportEngine(DctmExportEngineFactory factory, Logger output, WarningTracker warningTracker,
		File baseData, CmfObjectStore<?> objectStore, CmfContentStore<?, ?> contentStore, CfgTools settings) {
		super(factory, output, warningTracker, baseData, objectStore, contentStore, settings, true);
	}

	@Override
	protected Stream<ExportTarget> findExportTargetsBySearchKey(IDfSession session, CfgTools configuration,
		DctmExportDelegateFactory factory, String searchKey) throws Exception {
		// The searchKey is an r_object_id value, so treat it as such...
		IDfId id = new DfId(searchKey);
		if (id.isNull() || !id.isObjectId()) {
			throw new Exception(String.format("Invalid object ID [%s]", searchKey));
		}

		final IDfPersistentObject obj;
		try {
			obj = session.getObject(id);
		} catch (DfObjectNotFoundException e) {
			// ID goes nowhere...
			return null;
		}

		if (!obj.isInstanceOf("dm_folder")) {
			// Not a folder, so no recursion!
			return Collections
				.singleton(
					new ExportTarget(DctmObjectType.decodeType(obj).getStoredObjectType(), id.getId(), id.getId()))
				.stream();
		}

		// If it's a folder, we morph into a query-based recursion.
		return findExportTargetsByQuery(session, configuration, factory,
			String.format("dm_sysobject where folder(id(%s), DESCEND)", DfUtils.quoteString(id.getId())));
	}

	@Override
	protected Stream<ExportTarget> findExportTargetsByPath(IDfSession session, CfgTools configuration,
		DctmExportDelegateFactory factory, String path) throws Exception {
		IDfPersistentObject obj = session.getObjectByPath(path);
		if (obj == null) { return null; }

		final IDfId id = obj.getObjectId();
		if (!obj.isInstanceOf("dm_folder")) {
			// Not a folder, so no recursion!
			return Collections
				.singleton(
					new ExportTarget(DctmObjectType.decodeType(obj).getStoredObjectType(), id.getId(), id.getId()))
				.stream();
		}

		// If it's a folder, we morph into a query-based recursion.
		return findExportTargetsByQuery(session, configuration, factory,
			String.format("dm_sysobject where folder(id(%s), DESCEND)", DfUtils.quoteString(id.getId())));
	}

	@Override
	protected Stream<ExportTarget> findExportTargetsByQuery(IDfSession session, CfgTools configuration,
		DctmExportDelegateFactory factory, String query) throws Exception {
		if (session == null) {
			throw new IllegalArgumentException("Must provide a session through which to retrieve the results");
		}

		// Turn the predicate it into a DQL query
		query = String.format("select r_object_id from %s", StringUtils.strip(query));

		final int batchSize = configuration.getInteger(Setting.EXPORT_BATCH_SIZE);

		@SuppressWarnings("resource")
		DctmQuery dctmQuery = new DctmQuery(session, query, DctmQuery.Type.DF_EXECREAD_QUERY, batchSize);
		return dctmQuery.stream().map(this::getExportTarget);
	}

	private ExportTarget getExportTarget(IDfTypedObject t) {
		if (t == null) { return null; }
		try {
			return DctmExportTools.getExportTarget(t);
		} catch (Exception e) {
			throw new RuntimeException("Failed to construct an ExportTarget instance", e);
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
		CmfObjectStore<?> objectStore, CmfContentStore<?, ?> streamStore, Transformer transformer, Logger output,
		WarningTracker warningTracker) throws Exception {
		return new DctmExportContextFactory(this, cfg, session, objectStore, streamStore, output, warningTracker);
	}

	@Override
	protected DctmExportDelegateFactory newDelegateFactory(IDfSession session, CfgTools cfg) throws Exception {
		return new DctmExportDelegateFactory(this, cfg);
	}

	@Override
	protected IDfValue getValue(CmfValue.Type type, Object value) {
		return DfValueFactory.newValue(DctmTranslator.translateType(type).getDfConstant(), value);
	}
}