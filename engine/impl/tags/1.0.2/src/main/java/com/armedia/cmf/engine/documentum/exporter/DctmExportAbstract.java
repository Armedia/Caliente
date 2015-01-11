package com.armedia.cmf.engine.documentum.exporter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.armedia.cmf.engine.documentum.DctmAttributeHandlers;
import com.armedia.cmf.engine.documentum.DctmAttributeHandlers.AttributeHandler;
import com.armedia.cmf.engine.documentum.DctmDataType;
import com.armedia.cmf.engine.documentum.DctmDelegateBase;
import com.armedia.cmf.engine.documentum.DctmObjectType;
import com.armedia.cmf.engine.documentum.UnsupportedDctmObjectTypeException;
import com.armedia.cmf.engine.exporter.ExportContext;
import com.armedia.cmf.engine.exporter.ExportException;
import com.armedia.cmf.engine.exporter.ExportTarget;
import com.armedia.cmf.storage.ContentStore;
import com.armedia.cmf.storage.ContentStore.Handle;
import com.armedia.cmf.storage.StoredAttribute;
import com.armedia.cmf.storage.StoredObject;
import com.armedia.cmf.storage.StoredProperty;
import com.documentum.fc.client.IDfPersistentObject;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.common.DfException;
import com.documentum.fc.common.IDfAttr;
import com.documentum.fc.common.IDfValue;

public class DctmExportAbstract<T extends IDfPersistentObject> extends DctmDelegateBase<T, DctmExportEngine> {

	protected DctmExportAbstract(DctmExportEngine engine, DctmObjectType type) {
		super(engine, type);
	}

	public final Collection<IDfPersistentObject> identifyRequirements(IDfSession session,
		StoredObject<IDfValue> marshaled, IDfPersistentObject object, DctmExportContext ctx) throws Exception {
		return findRequirements(session, marshaled, castObject(object), ctx);
	}

	protected Collection<IDfPersistentObject> findRequirements(IDfSession session, StoredObject<IDfValue> marshaled,
		T object, DctmExportContext ctx) throws Exception {
		return new ArrayList<IDfPersistentObject>();
	}

	public final Collection<IDfPersistentObject> identifyDependents(IDfSession session,
		StoredObject<IDfValue> marshaled, IDfPersistentObject object,
		ExportContext<IDfSession, IDfPersistentObject, IDfValue> ctx) throws Exception {
		return findDependents(session, marshaled, castObject(object), ctx);
	}

	protected Collection<IDfPersistentObject> findDependents(IDfSession session, StoredObject<IDfValue> marshaled,
		T object, ExportContext<IDfSession, IDfPersistentObject, IDfValue> ctx) throws Exception {
		return new ArrayList<IDfPersistentObject>();
	}

	protected final StoredObject<IDfValue> marshal(DctmExportContext ctx, IDfSession session, IDfPersistentObject object)
		throws DfException, ExportException, UnsupportedDctmObjectTypeException {
		final String id = object.getObjectId().getId();
		final String subtype = object.getType().getName();
		final T typedObject = castObject(object);

		final String batchId = calculateBatchId(session, typedObject);
		final String label = calculateLabel(session, typedObject);
		final StoredObject<IDfValue> storedObject = new StoredObject<IDfValue>(getDctmType().getStoredObjectType(), id,
			batchId, label, subtype);

		// First, the attributes
		final int attCount = object.getAttrCount();
		for (int i = 0; i < attCount; i++) {
			final IDfAttr attr = object.getAttr(i);
			final AttributeHandler handler = DctmAttributeHandlers.getAttributeHandler(getDctmType(), attr);
			// Get the attribute handler
			if (handler.includeInExport(object, attr)) {
				StoredAttribute<IDfValue> attribute = new StoredAttribute<IDfValue>(attr.getName(), DctmDataType
					.fromAttribute(attr).getStoredType(), attr.getId(), attr.getLength(), attr.isRepeating(),
					attr.isQualifiable(), handler.getExportableValues(object, attr));
				storedObject.setAttribute(attribute);
			}
		}

		// Properties are different from attributes in that they require special handling. For
		// instance, a property would only be settable via direct SQL, or via an explicit method
		// call, etc., because setting it directly as an attribute would cmsImportResult in an error
		// from DFC, and therefore specialized code is required to handle it
		List<StoredProperty<IDfValue>> properties = new ArrayList<StoredProperty<IDfValue>>();
		getDataProperties(ctx, properties, typedObject);
		for (StoredProperty<IDfValue> property : properties) {
			// This mechanism overwrites properties, and intentionally so
			storedObject.setProperty(property);
		}
		return storedObject;
	}

	protected void getDataProperties(DctmExportContext ctx, Collection<StoredProperty<IDfValue>> properties, T object)
		throws DfException, ExportException {
	}

	protected String calculateBatchId(IDfSession session, T object) throws DfException {
		return null;
	}

	protected String calculateLabel(IDfSession session, T object) throws DfException {
		return String.format("%s[%s]", getDctmType().name(), object.getObjectId().getId());
	}

	protected final String calculateLabel(IDfPersistentObject object) throws DfException {
		return calculateLabel(object.getSession(), castObject(object));
	}

	public final Handle storeContent(IDfSession session, ExportTarget referrent, IDfPersistentObject object,
		ContentStore streamStore) throws Exception {
		return doStoreContent(session, referrent, castObject(object), streamStore);
	}

	protected Handle doStoreContent(IDfSession session, ExportTarget referrent, T object, ContentStore streamStore)
		throws Exception {
		return null;
	}
}