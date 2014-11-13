package com.armedia.cmf.documentum.engine.exporter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.log4j.Logger;

import com.armedia.cmf.documentum.engine.DctmAttributeHandlers;
import com.armedia.cmf.documentum.engine.DctmAttributeHandlers.AttributeHandler;
import com.armedia.cmf.documentum.engine.DctmDataType;
import com.armedia.cmf.documentum.engine.DctmObjectType;
import com.armedia.cmf.documentum.engine.UnsupportedDctmObjectTypeException;
import com.armedia.cmf.engine.exporter.ExportContext;
import com.armedia.cmf.engine.exporter.ExportException;
import com.armedia.cmf.engine.exporter.ExportTarget;
import com.armedia.cmf.storage.ContentStore;
import com.armedia.cmf.storage.StoredAttribute;
import com.armedia.cmf.storage.StoredObject;
import com.armedia.cmf.storage.StoredProperty;
import com.documentum.fc.client.IDfPersistentObject;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.common.DfException;
import com.documentum.fc.common.IDfAttr;
import com.documentum.fc.common.IDfValue;

public class DctmExportAbstract<T extends IDfPersistentObject> {
	protected final Logger log = Logger.getLogger(getClass());

	private final Class<T> dfClass;
	private final DctmObjectType type;
	private final DctmExportEngine engine;

	protected DctmExportAbstract(DctmExportEngine engine, DctmObjectType type) {
		this.engine = engine;
		this.type = type;
		@SuppressWarnings("unchecked")
		Class<T> c = (Class<T>) type.getDfClass();
		this.dfClass = c;
	}

	protected final DctmExportEngine getEngine() {
		return this.engine;
	}

	protected final DctmObjectType getDctmType() {
		return this.type;
	}

	protected final T castObject(IDfPersistentObject object) throws DfException {
		if (object == null) { return null; }
		if (!this.dfClass.isAssignableFrom(object.getClass())) { throw new DfException(String.format(
			"Expected an object of class %s, but got one of class %s", this.dfClass.getCanonicalName(), object
			.getClass().getCanonicalName())); }
		return this.dfClass.cast(object);
	}

	public final Collection<IDfPersistentObject> identifyRequirements(IDfSession session,
		StoredObject<IDfValue> marshaled, IDfPersistentObject object,
		ExportContext<IDfSession, IDfPersistentObject, IDfValue> ctx) throws Exception {
		return findRequirements(session, marshaled, castObject(object), ctx);
	}

	protected Collection<IDfPersistentObject> findRequirements(IDfSession session, StoredObject<IDfValue> marshaled,
		T object, ExportContext<IDfSession, IDfPersistentObject, IDfValue> ctx) throws Exception {
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

	protected final StoredObject<IDfValue> marshal(IDfSession session, IDfPersistentObject object) throws DfException,
	ExportException, UnsupportedDctmObjectTypeException {
		final String id = object.getObjectId().getId();
		final String subtype = object.getType().getName();
		final T typedObject = castObject(object);

		final String batchId = calculateBatchId(session, typedObject);
		final String label = calculateLabel(session, typedObject);
		final StoredObject<IDfValue> storedObject = new StoredObject<IDfValue>(this.type.getStoredObjectType(), id,
			batchId, label, subtype);

		// First, the attributes
		final int attCount = object.getAttrCount();
		for (int i = 0; i < attCount; i++) {
			final IDfAttr attr = object.getAttr(i);
			final AttributeHandler handler = DctmAttributeHandlers.getAttributeHandler(this.type, attr);
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
		getDataProperties(properties, typedObject);
		for (StoredProperty<IDfValue> property : properties) {
			// This mechanism overwrites properties, and intentionally so
			storedObject.setProperty(property);
		}
		return storedObject;
	}

	protected void getDataProperties(Collection<StoredProperty<IDfValue>> properties, T object) throws DfException,
	ExportException {
	}

	protected String calculateBatchId(IDfSession session, T object) throws DfException {
		return null;
	}

	protected String calculateLabel(IDfSession session, T object) throws DfException {
		return String.format("%s[%s]", this.type.name(), object.getObjectId().getId());
	}

	protected final String calculateLabel(IDfPersistentObject object) throws DfException {
		return calculateLabel(object.getSession(), castObject(object));
	}

	public final String storeContent(IDfSession session, ExportTarget referrent, IDfPersistentObject object,
		ContentStore streamStore) throws Exception {
		return doStoreContent(session, referrent, castObject(object), streamStore);
	}

	protected String doStoreContent(IDfSession session, ExportTarget referrent, T object, ContentStore streamStore)
		throws Exception {
		return null;
	}
}