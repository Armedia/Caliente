/**
 *
 */

package com.armedia.cmf.documentum.engine.exporter;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;

import com.armedia.cmf.documentum.engine.DctmObjectType;
import com.armedia.cmf.documentum.engine.DctmSessionFactory;
import com.armedia.cmf.documentum.engine.DctmSessionWrapper;
import com.armedia.cmf.documentum.engine.DctmTranslator;
import com.armedia.cmf.documentum.engine.DfUtils;
import com.armedia.cmf.documentum.engine.UnsupportedObjectTypeException;
import com.armedia.cmf.engine.SessionFactory;
import com.armedia.cmf.engine.exporter.ExportEngine;
import com.armedia.cmf.engine.exporter.ExportException;
import com.armedia.cmf.engine.exporter.ExportTarget;
import com.armedia.cmf.storage.ContentStreamStore;
import com.armedia.cmf.storage.ObjectStorageTranslator;
import com.armedia.cmf.storage.StoredObject;
import com.armedia.cmf.storage.StoredObjectType;
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

	private static final Set<String> TARGETS = Collections.singleton("dctm");
	private static final Map<DctmObjectType, DctmExporter<?>> DELEGATES;

	static {
		Map<DctmObjectType, DctmExporter<?>> m = new EnumMap<DctmObjectType, DctmExporter<?>>(
			DctmObjectType.class);
		m.put(DctmObjectType.ACL, new DctmACLExporter());
		m.put(DctmObjectType.CONTENT, new DctmContentExporter());
		m.put(DctmObjectType.DOCUMENT, new DctmDocumentExporter());
		m.put(DctmObjectType.FOLDER, new DctmFolderExporter());
		m.put(DctmObjectType.FORMAT, new DctmFormatExporter());
		m.put(DctmObjectType.GROUP, new DctmGroupExporter());
		m.put(DctmObjectType.TYPE, new DctmTypeExporter());
		m.put(DctmObjectType.USER, new DctmUserExporter());
		DELEGATES = Collections.unmodifiableMap(m);
	}

	private static final String DCTM_DQL = "dql";

	private DctmExporter<?> getExportDelegate(IDfPersistentObject object) throws DfException,
		UnsupportedObjectTypeException {
		DctmObjectType type = DctmObjectType.decodeType(object);
		DctmExporter<?> delegate = DctmExportEngine.DELEGATES.get(type);
		if (delegate == null) { throw new IllegalStateException(String.format(
			"Failed to find a delegate for type [%s]", type.name())); }
		return delegate;
	}

	private final String getObjectId(IDfPersistentObject object) {
		if (object == null) { throw new IllegalArgumentException("Must provide an object whose ID to retrieve"); }
		try {
			return object.getObjectId().getId();
		} catch (DfException e) {
			return String.format("(id-unavailable: %s)", e.getMessage());
		}
	}

	@Override
	protected Iterator<ExportTarget> findExportResults(IDfSession session, Map<String, Object> settings)
		throws Exception {
		if (session == null) { throw new IllegalArgumentException(
			"Must provide a session through which to retrieve the results"); }
		if (settings == null) {
			settings = Collections.emptyMap();
		}
		Object dql = settings.get(DctmExportEngine.DCTM_DQL);
		if (dql == null) { throw new Exception(String.format("Must provide the DQL to query with", dql)); }
		return new DctmExportTargetIterator(DfUtils.executeQuery(session, dql.toString(), IDfQuery.DF_EXECREAD_QUERY));
	}

	@Override
	protected IDfPersistentObject getObject(IDfSession session, StoredObjectType type, String id) throws Exception {
		if (session == null) { throw new IllegalArgumentException(
			"Must provide a session through which to retrieve the object"); }
		if (type == null) { throw new IllegalArgumentException("Must provide an object type to retrieve"); }
		if (id == null) { throw new IllegalArgumentException("Must provide an object ID to retrieve"); }

		// For Documentum, the type is not used for the search. We do, however, use it to validate
		// the returned object...
		IDfPersistentObject ret = session.getObject(new DfId(id));
		DctmObjectType dctmType = DctmObjectType.decodeType(ret);
		if (type != dctmType.getStoredObjectType()) { throw new Exception(String.format(
			"Type mismatch - expected [%s] but got [%s] for object [%s]", type, dctmType.getStoredObjectType(), id)); }
		return ret;
	}

	@Override
	protected Collection<IDfPersistentObject> identifyRequirements(IDfSession session, IDfPersistentObject object,
		DctmExportContext ctx) throws Exception {
		if (session == null) { throw new IllegalArgumentException(
			"Must provide a session through which to retrieve the requirements"); }
		if (object == null) { throw new IllegalArgumentException(
			"Must provide an object whose requirements to identify"); }
		return getExportDelegate(object).identifyRequirements(session, object, ctx);
	}

	@Override
	protected Collection<IDfPersistentObject> identifyDependents(IDfSession session, IDfPersistentObject object,
		DctmExportContext ctx) throws Exception {
		if (session == null) { throw new IllegalArgumentException(
			"Must provide a session through which to retrieve the dependents"); }
		if (object == null) { throw new IllegalArgumentException("Must provide an object whose dependents to identify"); }
		return getExportDelegate(object).identifyDependents(session, object, ctx);
	}

	@Override
	protected StoredObject<IDfValue> marshal(IDfSession session, IDfPersistentObject object) throws ExportException {
		if (session == null) { throw new IllegalArgumentException(
			"Must provide a session through which to marshal the object"); }
		if (object == null) { throw new IllegalArgumentException("Must provide an object to marshal"); }
		try {
			return getExportDelegate(object).marshal(session, object);
		} catch (DfException | UnsupportedObjectTypeException e) {
			throw new ExportException(String.format("Exception raised while marshaling object [%s]",
				getObjectId(object)), e);
		}
	}

	@Override
	protected void storeContent(IDfSession session, IDfPersistentObject object, ContentStreamStore streamStore)
		throws Exception {
		if (session == null) { throw new IllegalArgumentException(
			"Must provide a session through which to store the contents"); }
		if (object == null) { throw new IllegalArgumentException("Must provide an object whose contents to store"); }
		if (streamStore == null) { throw new IllegalArgumentException(
			"Must provide a ContentStreamStore in which to store the contents"); }
		getExportDelegate(object).storeContent(session, object, streamStore);
	}

	@Override
	public ObjectStorageTranslator<IDfPersistentObject, IDfValue> getTranslator() {
		return DctmTranslator.INSTANCE;
	}

	@Override
	protected SessionFactory<IDfSession> newSessionFactory() {
		return new DctmSessionFactory();
	}

	@Override
	protected DctmExportContext newContext(String rootId, IDfSession session, Logger output) {
		return new DctmExportContext(rootId, session, output);
	}

	@Override
	protected Set<String> getTargetNames() {
		return DctmExportEngine.TARGETS;
	}
}