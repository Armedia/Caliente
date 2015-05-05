package com.armedia.cmf.engine.documentum.exporter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.armedia.cmf.engine.ContentInfo;
import com.armedia.cmf.engine.documentum.DctmAttributeHandlers;
import com.armedia.cmf.engine.documentum.DctmAttributeHandlers.AttributeHandler;
import com.armedia.cmf.engine.documentum.DctmDataType;
import com.armedia.cmf.engine.documentum.DctmObjectType;
import com.armedia.cmf.engine.documentum.DctmSessionWrapper;
import com.armedia.cmf.engine.exporter.ExportDelegate;
import com.armedia.cmf.engine.exporter.ExportException;
import com.armedia.cmf.engine.exporter.ExportTarget;
import com.armedia.cmf.storage.ContentStore;
import com.armedia.cmf.storage.StoredAttribute;
import com.armedia.cmf.storage.StoredObject;
import com.armedia.cmf.storage.StoredObjectType;
import com.armedia.cmf.storage.StoredProperty;
import com.armedia.commons.utilities.CfgTools;
import com.documentum.fc.client.IDfPersistentObject;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.common.DfException;
import com.documentum.fc.common.IDfAttr;
import com.documentum.fc.common.IDfValue;

public abstract class DctmExportDelegate<T extends IDfPersistentObject> extends
	ExportDelegate<T, IDfSession, DctmSessionWrapper, IDfValue, DctmExportContext, DctmExportEngine> {

	private final DctmObjectType dctmType;

	protected DctmExportDelegate(DctmExportEngine engine, Class<T> objectClass, T object, CfgTools configuration)
		throws Exception {
		super(engine, objectClass, object, configuration);
		this.dctmType = DctmObjectType.decodeType(object);
	}

	protected final DctmObjectType getDctmType() {
		return this.dctmType;
	}

	@Override
	protected final StoredObjectType calculateType(T object, CfgTools configuration) throws Exception {
		return DctmObjectType.decodeType(object).getStoredObjectType();
	}

	@Override
	protected final String calculateObjectId(T object, CfgTools configuration) throws Exception {
		return object.getObjectId().getId();
	}

	@Override
	protected String calculateLabel(T object, CfgTools configuration) throws Exception {
		return String.format("%s[%s]", getDctmType().name(), getObjectId());
	}

	@Override
	protected final String calculateSearchKey(T object, CfgTools configuration) throws Exception {
		return calculateObjectId(object, configuration);
	}

	@Override
	protected String calculateBatchId(T object, CfgTools configuration) throws Exception {
		return null;
	}

	@Override
	protected final Collection<DctmExportDelegate<?>> identifyRequirements(StoredObject<IDfValue> marshaled,
		DctmExportContext ctx) throws Exception {
		return findRequirements(ctx.getSession(), marshaled, castObject(this.object), ctx);
	}

	protected Collection<DctmExportDelegate<?>> findRequirements(IDfSession session, StoredObject<IDfValue> marshaled,
		T object, DctmExportContext ctx) throws Exception {
		return new ArrayList<DctmExportDelegate<?>>();
	}

	@Override
	protected final Collection<DctmExportDelegate<?>> identifyDependents(StoredObject<IDfValue> marshaled,
		DctmExportContext ctx) throws Exception {
		return findDependents(ctx.getSession(), marshaled, castObject(this.object), ctx);
	}

	protected Collection<DctmExportDelegate<?>> findDependents(IDfSession session, StoredObject<IDfValue> marshaled,
		T object, DctmExportContext ctx) throws Exception {
		return new ArrayList<DctmExportDelegate<?>>();
	}

	@Override
	protected final void marshal(DctmExportContext ctx, StoredObject<IDfValue> object) throws ExportException {
		try {
			final T typedObject = castObject(this.object);
			// First, the attributes
			final int attCount = this.object.getAttrCount();
			for (int i = 0; i < attCount; i++) {
				final IDfAttr attr = this.object.getAttr(i);
				final AttributeHandler handler = DctmAttributeHandlers.getAttributeHandler(getDctmType(), attr);
				// Get the attribute handler
				if (handler.includeInExport(this.object, attr)) {
					StoredAttribute<IDfValue> attribute = new StoredAttribute<IDfValue>(attr.getName(), DctmDataType
						.fromAttribute(attr).getStoredType(), attr.isRepeating(), handler.getExportableValues(
						this.object, attr));
					object.setAttribute(attribute);
				}
			}

			// Properties are different from attributes in that they require special handling. For
			// instance, a property would only be settable via direct SQL, or via an explicit method
			// call, etc., because setting it directly as an attribute would cmsImportResult in an
			// error from DFC, and therefore specialized code is required to handle it
			List<StoredProperty<IDfValue>> properties = new ArrayList<StoredProperty<IDfValue>>();
			getDataProperties(ctx, properties, typedObject);
			for (StoredProperty<IDfValue> property : properties) {
				// This mechanism overwrites properties, and intentionally so
				object.setProperty(property);
			}
		} catch (DfException e) {
			throw new ExportException(String.format("Failed to export %s %s", getType(), getObjectId()), e);
		}
	}

	protected void getDataProperties(DctmExportContext ctx, Collection<StoredProperty<IDfValue>> properties, T object)
		throws DfException, ExportException {
	}

	@Override
	protected final List<ContentInfo> storeContent(IDfSession session, StoredObject<IDfValue> marshaled,
		ExportTarget referrent, ContentStore streamStore) throws Exception {
		return doStoreContent(session, marshaled, referrent, castObject(this.object), streamStore);
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