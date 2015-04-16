package com.armedia.cmf.engine.documentum.exporter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.armedia.cmf.engine.ContentInfo;
import com.armedia.cmf.engine.documentum.DctmAttributeHandlers;
import com.armedia.cmf.engine.documentum.DctmAttributeHandlers.AttributeHandler;
import com.armedia.cmf.engine.documentum.DctmDataType;
import com.armedia.cmf.engine.documentum.UnsupportedDctmObjectTypeException;
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

public class DctmExportAbstract<T extends IDfPersistentObject> extends DctmExportDelegate<T> {

	protected DctmExportAbstract(DctmExportEngine engine, Class<T> objectClass, T object) throws Exception {
		super(engine, objectClass, object);
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
		StoredObject<IDfValue> marshaled, IDfPersistentObject object, DctmExportContext ctx) throws Exception {
		return findDependents(session, marshaled, castObject(object), ctx);
	}

	protected Collection<IDfPersistentObject> findDependents(IDfSession session, StoredObject<IDfValue> marshaled,
		T object, DctmExportContext ctx) throws Exception {
		return new ArrayList<IDfPersistentObject>();
	}

	@Override
	protected void marshal(DctmExportContext ctx, StoredObject<IDfValue> object) throws ExportException {
		// TODO Auto-generated method stub
		try {
			doMarshal(ctx, object);
		} catch (Exception e) {
			throw new ExportException(String.format("Failed to export %s %s", getType(), getObjectId()), e);
		}
	}

	protected final void doMarshal(DctmExportContext ctx, StoredObject<IDfValue> storedObject) throws DfException,
	ExportException, UnsupportedDctmObjectTypeException {
		final T typedObject = castObject(this.object);
		// First, the attributes
		final int attCount = this.object.getAttrCount();
		for (int i = 0; i < attCount; i++) {
			final IDfAttr attr = this.object.getAttr(i);
			final AttributeHandler handler = DctmAttributeHandlers.getAttributeHandler(getDctmType(), attr);
			// Get the attribute handler
			if (handler.includeInExport(this.object, attr)) {
				StoredAttribute<IDfValue> attribute = new StoredAttribute<IDfValue>(attr.getName(), DctmDataType
					.fromAttribute(attr).getStoredType(), attr.isRepeating(), handler.getExportableValues(this.object,
						attr));
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
	}

	protected void getDataProperties(DctmExportContext ctx, Collection<StoredProperty<IDfValue>> properties, T object)
		throws DfException, ExportException {
	}

	public final List<ContentInfo> storeContent(IDfSession session, StoredObject<IDfValue> marshaled,
		ExportTarget referrent, IDfPersistentObject object, ContentStore streamStore) throws Exception {
		return doStoreContent(session, marshaled, referrent, castObject(object), streamStore);
	}

	protected List<ContentInfo> doStoreContent(IDfSession session, StoredObject<IDfValue> marshaled,
		ExportTarget referrent, T object, ContentStore streamStore) throws Exception {
		return null;
	}

	protected static <T extends IDfPersistentObject> T staticCast(Class<T> klazz, IDfPersistentObject p)
		throws ClassCastException {
		if (klazz == null) { throw new IllegalArgumentException("Must provide a class to cast to"); }
		if (p == null) { return null; }
		if (!klazz.isInstance(p)) { throw new ClassCastException(String.format("Can't convert a [%s] into a [%s]", p
			.getClass().getCanonicalName(), klazz.getCanonicalName())); }
		return klazz.cast(p);
	}
}