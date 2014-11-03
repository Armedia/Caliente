package com.armedia.cmf.documentum.engine.exporter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.armedia.cmf.documentum.engine.DctmAttributeHandlers;
import com.armedia.cmf.documentum.engine.DctmAttributeHandlers.AttributeHandler;
import com.armedia.cmf.documentum.engine.DctmDataType;
import com.armedia.cmf.documentum.engine.DctmObjectType;
import com.armedia.cmf.documentum.engine.UnsupportedObjectTypeException;
import com.armedia.cmf.engine.exporter.ExportException;
import com.armedia.cmf.storage.StoredAttribute;
import com.armedia.cmf.storage.StoredObject;
import com.armedia.cmf.storage.StoredProperty;
import com.documentum.fc.client.IDfPersistentObject;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.common.DfException;
import com.documentum.fc.common.IDfAttr;
import com.documentum.fc.common.IDfValue;

public class DctmMarshaller<T extends IDfPersistentObject> {

	private final Class<T> dfClass;
	private final DctmObjectType type;

	protected DctmMarshaller(DctmObjectType type) {
		this.type = type;
		@SuppressWarnings("unchecked")
		Class<T> c = (Class<T>) type.getDfClass();
		this.dfClass = c;
	}

	protected final T castObject(IDfPersistentObject object) throws DfException {
		if (object == null) { return null; }
		if (!this.dfClass.isAssignableFrom(object.getClass())) { throw new DfException(String.format(
			"Expected an object of class %s, but got one of class %s", this.dfClass.getCanonicalName(), object
				.getClass().getCanonicalName())); }
		return this.dfClass.cast(object);
	}

	protected final StoredObject<IDfValue> marshal(IDfSession session, IDfPersistentObject object) throws DfException,
		ExportException, UnsupportedObjectTypeException {
		final String id = object.getObjectId().getId();
		final String subtype = object.getType().getName();
		final T typedObject = castObject(object);

		final String batchId = calculateBatchId(session, typedObject);
		final String label = calculateLabel(session, typedObject);
		StoredObject<IDfValue> storedObject = new StoredObject<IDfValue>(this.type.getStoredObjectType(), id, batchId,
			label, subtype);

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
		return object.getObjectId().getId();
	}

	protected String calculateLabel(IDfSession session, T object) throws DfException {
		return String.format("%s[%s]", this.type.name(), object.getObjectId().getId());
	}
}