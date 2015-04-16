/**
 *
 */

package com.armedia.cmf.engine.documentum.exporter;

import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.armedia.cmf.engine.SessionFactory;
import com.armedia.cmf.engine.documentum.DctmObjectType;
import com.armedia.cmf.engine.documentum.DctmSessionFactory;
import com.armedia.cmf.engine.documentum.DctmSessionWrapper;
import com.armedia.cmf.engine.documentum.DctmTranslator;
import com.armedia.cmf.engine.documentum.DfUtils;
import com.armedia.cmf.engine.documentum.DfValueFactory;
import com.armedia.cmf.engine.documentum.common.DctmCommon;
import com.armedia.cmf.engine.documentum.common.Setting;
import com.armedia.cmf.engine.exporter.ExportEngine;
import com.armedia.cmf.engine.exporter.ExportException;
import com.armedia.cmf.engine.exporter.ExportTarget;
import com.armedia.cmf.storage.ObjectStorageTranslator;
import com.armedia.cmf.storage.StoredDataType;
import com.armedia.cmf.storage.StoredObjectType;
import com.armedia.commons.utilities.CfgTools;
import com.documentum.fc.client.IDfPersistentObject;
import com.documentum.fc.client.IDfQuery;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.common.DfId;
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
	protected Iterator<ExportTarget> findExportResults(IDfSession session, Map<String, ?> settings) throws Exception {
		if (session == null) { throw new IllegalArgumentException(
			"Must provide a session through which to retrieve the results"); }
		if (settings == null) {
			settings = Collections.emptyMap();
		}
		Object dql = settings.get(Setting.DQL.getLabel());
		if (dql == null) { throw new Exception(String.format("Must provide the DQL to query with", dql)); }
		final int batchSize = CfgTools.decodeInteger(Setting.EXPORT_BATCH_SIZE.getLabel(), settings, 0);
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

	@Override
	protected DctmExportDelegate<?> getExportDelegate(IDfSession session, StoredObjectType type, String searchKey)
		throws Exception {
		if (session == null) { throw new IllegalArgumentException(
			"Must provide a session through which to retrieve the object"); }
		if (searchKey == null) { throw new IllegalArgumentException("Must provide an object ID to retrieve"); }

		// For Documentum, the type is not used for the search. We do, however, use it to validate
		// the returned object...
		final DctmObjectType dctmType = DctmObjectType.decodeType(type);
		if (dctmType == null) { throw new ExportException(String.format(
			"Unsupported object type [%s] (search key = [%s])", type, searchKey)); }

		IDfPersistentObject object = session.getObject(new DfId(searchKey));
		Class<? extends IDfPersistentObject> requiredClass = dctmType.getDfClass();
		if (requiredClass.isInstance(object)) {
			DctmExportDelegate<?> delegate = null;
			switch (dctmType) {
				case STORE:
					delegate = new DctmExportStore(this, object);
					break;
				case USER:
					delegate = new DctmExportUser(this, object);
					break;
				case GROUP:
					delegate = new DctmExportGroup(this, object);
					break;
				case ACL:
					delegate = new DctmExportACL(this, object);
					break;
				case TYPE:
					delegate = new DctmExportType(this, object);
					break;
				case FORMAT:
					delegate = new DctmExportFormat(this, object);
					break;
				case FOLDER:
					delegate = new DctmExportFolder(this, object);
					break;
				case DOCUMENT:
					delegate = new DctmExportDocument(this, object);
					break;
				default:
					break;
			}
			return delegate;
		}
		this.log.warn(String.format("Type [%s] is not supported - no delegate created for search key [%s]", type,
			searchKey));
		return null;
	}
}