/**
 *
 */

package com.armedia.cmf.documentum.engine.exporter;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;

import com.armedia.cmf.documentum.engine.DctmObjectType;
import com.armedia.cmf.documentum.engine.DctmSessionFactory;
import com.armedia.cmf.documentum.engine.UnsupportedObjectTypeException;
import com.armedia.cmf.engine.SessionFactory;
import com.armedia.cmf.engine.exporter.ExportException;
import com.armedia.cmf.engine.exporter.ExportTarget;
import com.armedia.cmf.engine.exporter.Exporter;
import com.armedia.cmf.storage.ContentStreamStore;
import com.armedia.cmf.storage.ObjectStorageTranslator;
import com.armedia.cmf.storage.StoredObject;
import com.armedia.cmf.storage.StoredObjectType;
import com.documentum.fc.client.IDfPersistentObject;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.common.DfException;
import com.documentum.fc.common.DfId;
import com.documentum.fc.common.IDfValue;

/**
 * @author diego
 *
 */
public class DctmExporter implements Exporter<IDfSession, IDfPersistentObject, IDfValue> {

	@Override
	public Iterator<ExportTarget> findExportResults(IDfSession session, Map<String, Object> settings) throws Exception {
		return null;
	}

	@Override
	public IDfPersistentObject getObject(IDfSession session, StoredObjectType type, String id) throws Exception {
		return session.getObject(new DfId(id));
	}

	@Override
	public Collection<IDfPersistentObject> identifyRequirements(IDfSession session, IDfPersistentObject object)
		throws Exception {
		// This will vary based on the object type...this is where we use the documentum-specific
		// delegates
		return Collections.emptyList();
	}

	@Override
	public Collection<IDfPersistentObject> identifyDependents(IDfSession session, IDfPersistentObject object)
		throws Exception {
		// This will vary based on the object type...this is where we use the documentum-specific
		// delegates
		return Collections.emptyList();
	}

	@Override
	public StoredObject<IDfValue> marshal(IDfSession session, IDfPersistentObject object) throws ExportException {
		try {
			return doMarshal(session, object);
		} catch (Exception e) {
			throw new ExportException("Exception caught attempting to marshal an object", e);
		}
	}

	private StoredObject<IDfValue> doMarshal(IDfSession session, IDfPersistentObject object) throws DfException,
		UnsupportedObjectTypeException {
		final DctmObjectType type = DctmObjectType.decodeType(object);
		final String id = object.getObjectId().getId();
		final String subtype = object.getType().getName();
		final String batchId = null; // TODO: calculate
		final String label = null; // TODO: calculate
		StoredObject<IDfValue> ret = new StoredObject<IDfValue>(type.getStoredObjectType(), id, batchId, label, subtype);
		return ret;
	}

	@Override
	public void storeContent(IDfSession session, IDfPersistentObject object, ContentStreamStore streamStore)
		throws Exception {
	}

	@Override
	public ObjectStorageTranslator<IDfPersistentObject, IDfValue> getTranslator() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public SessionFactory<IDfSession> getSessionFactory() {
		return new DctmSessionFactory();
	}
}