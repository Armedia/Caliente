/**
 *
 */

package com.armedia.cmf.engine.documentum.exporter;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
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
import com.armedia.cmf.engine.documentum.UnsupportedDctmObjectTypeException;
import com.armedia.cmf.engine.documentum.common.DctmCommon;
import com.armedia.cmf.engine.documentum.common.Setting;
import com.armedia.cmf.engine.exporter.ExportEngine;
import com.armedia.cmf.engine.exporter.ExportException;
import com.armedia.cmf.engine.exporter.ExportTarget;
import com.armedia.cmf.storage.ContentStore;
import com.armedia.cmf.storage.ContentStore.Handle;
import com.armedia.cmf.storage.ObjectStorageTranslator;
import com.armedia.cmf.storage.StoredDataType;
import com.armedia.cmf.storage.StoredObject;
import com.armedia.cmf.storage.StoredObjectType;
import com.armedia.commons.utilities.CfgTools;
import com.documentum.fc.client.IDfPersistentObject;
import com.documentum.fc.client.IDfQuery;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.common.DfException;
import com.documentum.fc.common.DfId;
import com.documentum.fc.common.IDfValue;

/**
 * @author diego
 *
 */
public class DctmExportEngine extends
	ExportEngine<IDfSession, DctmSessionWrapper, IDfPersistentObject, IDfValue, DctmExportContext> {

	private static final Set<String> TARGETS = Collections.singleton(DctmCommon.TARGET_NAME);
	private final Map<DctmObjectType, DctmExportAbstract<?>> delegates;

	public DctmExportEngine() {
		Map<DctmObjectType, DctmExportAbstract<?>> m = new EnumMap<DctmObjectType, DctmExportAbstract<?>>(
			DctmObjectType.class);
		m.put(DctmObjectType.ACL, new DctmExportACL(this));
		m.put(DctmObjectType.CONTENT, new DctmExportContent(this));
		m.put(DctmObjectType.DOCUMENT, new DctmExportDocument(this));
		m.put(DctmObjectType.STORE, new DctmExportStore(this));
		m.put(DctmObjectType.FOLDER, new DctmExportFolder(this));
		m.put(DctmObjectType.FORMAT, new DctmExportFormat(this));
		m.put(DctmObjectType.GROUP, new DctmExportGroup(this));
		m.put(DctmObjectType.TYPE, new DctmExportType(this));
		m.put(DctmObjectType.USER, new DctmExportUser(this));
		this.delegates = Collections.unmodifiableMap(m);
	}

	private DctmExportAbstract<?> getExportDelegate(IDfPersistentObject object) throws DfException,
	UnsupportedDctmObjectTypeException {
		DctmObjectType type = DctmObjectType.decodeType(object);
		DctmExportAbstract<?> delegate = this.delegates.get(type);
		if (delegate == null) { throw new IllegalStateException(String.format(
			"Failed to find a delegate for type [%s]", type.name())); }
		return delegate;
	}

	@Override
	protected final String getObjectId(IDfPersistentObject object) {
		if (object == null) { throw new IllegalArgumentException("Must provide an object whose ID to retrieve"); }
		try {
			return object.getObjectId().getId();
		} catch (DfException e) {
			return String.format("(id-unavailable: %s)", e.getMessage());
		}
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
	protected IDfPersistentObject getObject(IDfSession session, StoredObjectType type, String id) throws Exception {
		if (session == null) { throw new IllegalArgumentException(
			"Must provide a session through which to retrieve the object"); }
		if (id == null) { throw new IllegalArgumentException("Must provide an object ID to retrieve"); }

		// For Documentum, the type is not used for the search. We do, however, use it to validate
		// the returned object...
		IDfPersistentObject ret = session.getObject(new DfId(id));
		DctmObjectType dctmType = DctmObjectType.decodeType(ret);
		if ((type != null) && (type != dctmType.getStoredObjectType())) { throw new Exception(String.format(
			"Type mismatch - expected [%s] but got [%s] for object [%s]", type, dctmType.getStoredObjectType(), id)); }
		return ret;
	}

	@Override
	protected Collection<IDfPersistentObject> identifyRequirements(IDfSession session,
		StoredObject<IDfValue> marshaled, IDfPersistentObject object, DctmExportContext ctx) throws Exception {
		if (session == null) { throw new IllegalArgumentException(
			"Must provide a session through which to retrieve the requirements"); }
		if (object == null) { throw new IllegalArgumentException(
			"Must provide an object whose requirements to identify"); }
		return getExportDelegate(object).identifyRequirements(session, marshaled, object, ctx);
	}

	@Override
	protected Collection<IDfPersistentObject> identifyDependents(IDfSession session, StoredObject<IDfValue> marshaled,
		IDfPersistentObject object, DctmExportContext ctx) throws Exception {
		if (session == null) { throw new IllegalArgumentException(
			"Must provide a session through which to retrieve the dependents"); }
		if (object == null) { throw new IllegalArgumentException("Must provide an object whose dependents to identify"); }
		return getExportDelegate(object).identifyDependents(session, marshaled, object, ctx);
	}

	@Override
	protected StoredObject<IDfValue> marshal(DctmExportContext ctx, IDfSession session, IDfPersistentObject object)
		throws ExportException {
		if (session == null) { throw new IllegalArgumentException(
			"Must provide a session through which to marshal the object"); }
		if (object == null) { throw new IllegalArgumentException("Must provide an object to marshal"); }
		try {
			return getExportDelegate(object).marshal(ctx, session, object);
		} catch (DfException e) {
			throw new ExportException(String.format("Exception raised while marshaling object [%s]",
				getObjectId(object)), e);
		} catch (UnsupportedDctmObjectTypeException e) {
			throw new ExportException(String.format("Exception raised while marshaling object [%s]",
				getObjectId(object)), e);
		}
	}

	@Override
	protected Handle storeContent(IDfSession session, StoredObject<IDfValue> marshaled, ExportTarget referrent,
		IDfPersistentObject object, ContentStore streamStore) throws Exception {
		if (session == null) { throw new IllegalArgumentException(
			"Must provide a session through which to store the contents"); }
		if (marshaled == null) { throw new IllegalArgumentException("Must provide an object whose contents to store"); }
		if (object == null) { throw new IllegalArgumentException("Must provide an object whose contents to store"); }
		return getExportDelegate(object).storeContent(session, marshaled, referrent, object, streamStore);
	}

	@Override
	public ObjectStorageTranslator<IDfPersistentObject, IDfValue> getTranslator() {
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
	protected String calculateLabel(IDfPersistentObject object) throws Exception {
		if (object == null) { throw new IllegalArgumentException("Must provide an object whose contents to analyze"); }
		return getExportDelegate(object).calculateLabel(object);
	}

	@Override
	protected ExportTarget getExportTarget(IDfPersistentObject object) throws ExportException {
		try {
			return DfUtils.getExportTarget(object);
		} catch (DfException e) {
			throw new ExportException("Failed to generate the export target", e);
		} catch (UnsupportedDctmObjectTypeException e) {
			throw new ExportException("Failed to generate the export target", e);
		}
	}

	@Override
	protected IDfValue getValue(StoredDataType type, Object value) {
		return DfValueFactory.newValue(type, value);
	}

	public static ExportEngine<?, ?, ?, ?, ?> getExportEngine() {
		return ExportEngine.getExportEngine(DctmCommon.TARGET_NAME);
	}
}