package com.armedia.cmf.engine.documentum.exporter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.armedia.cmf.engine.documentum.DctmAttributeHandlers;
import com.armedia.cmf.engine.documentum.DctmAttributeHandlers.AttributeHandler;
import com.armedia.cmf.engine.documentum.DctmDataType;
import com.armedia.cmf.engine.documentum.DctmObjectType;
import com.armedia.cmf.engine.documentum.DctmSessionWrapper;
import com.armedia.cmf.engine.exporter.ExportDelegate;
import com.armedia.cmf.engine.exporter.ExportException;
import com.armedia.cmf.engine.exporter.ExportTarget;
import com.armedia.cmf.storage.CmfAttribute;
import com.armedia.cmf.storage.CmfAttributeTranslator;
import com.armedia.cmf.storage.CmfContentInfo;
import com.armedia.cmf.storage.CmfContentStore;
import com.armedia.cmf.storage.CmfObject;
import com.armedia.cmf.storage.CmfProperty;
import com.armedia.cmf.storage.CmfType;
import com.documentum.fc.client.IDfPersistentObject;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.common.DfException;
import com.documentum.fc.common.IDfAttr;
import com.documentum.fc.common.IDfValue;

public abstract class DctmExportDelegate<T extends IDfPersistentObject> extends
	ExportDelegate<T, IDfSession, DctmSessionWrapper, IDfValue, DctmExportContext, DctmExportDelegateFactory, DctmExportEngine> {

	private final DctmObjectType dctmType;

	protected DctmExportDelegate(DctmExportDelegateFactory factory, Class<T> objectClass, T object) throws Exception {
		super(factory, objectClass, object);
		this.dctmType = DctmObjectType.decodeType(object);
	}

	protected final DctmObjectType getDctmType() {
		return this.dctmType;
	}

	@Override
	protected final void prepareForStorage(DctmExportContext ctx, CmfObject<IDfValue> marshaled)
		throws ExportException {
		super.prepareForStorage(ctx, marshaled);
		try {
			prepareForStorage(ctx, marshaled, this.object);
		} catch (DfException e) {
			throw new ExportException(String.format("Failed to prepare the %s [%s](%s) for storage",
				marshaled.getType(), marshaled.getLabel(), marshaled.getId()), e);
		}
	}

	protected void prepareForStorage(DctmExportContext ctx, CmfObject<IDfValue> marshaled, T object)
		throws ExportException, DfException {
		// By default, do nothing
	}

	@Override
	protected final CmfType calculateType(T object) throws Exception {
		return DctmObjectType.decodeType(object).getStoredObjectType();
	}

	@Override
	protected final String calculateObjectId(T object) throws Exception {
		return object.getObjectId().getId();
	}

	@Override
	protected String calculateLabel(T object) throws Exception {
		return String.format("%s[%s]", getDctmType().name(), getObjectId());
	}

	@Override
	protected final String calculateSearchKey(T object) throws Exception {
		return calculateObjectId(object);
	}

	@Override
	protected String calculateBatchId(T object) throws Exception {
		return null;
	}

	@Override
	protected final String calculateSubType(CmfType type, T object) throws Exception {
		return object.getType().getName();
	}

	@Override
	protected final Collection<DctmExportDelegate<?>> identifyRequirements(CmfObject<IDfValue> marshaled,
		DctmExportContext ctx) throws Exception {
		return findRequirements(ctx.getSession(), marshaled, castObject(this.object), ctx);
	}

	protected Collection<DctmExportDelegate<?>> findRequirements(IDfSession session, CmfObject<IDfValue> marshaled,
		T object, DctmExportContext ctx) throws Exception {
		Collection<DctmExportDelegate<?>> ret = new ArrayList<DctmExportDelegate<?>>();
		ret.add(this.factory.newExportDelegate(object.getType()));
		return ret;
	}

	@Override
	protected final Collection<DctmExportDelegate<?>> identifyDependents(CmfObject<IDfValue> marshaled,
		DctmExportContext ctx) throws Exception {
		return findDependents(ctx.getSession(), marshaled, castObject(this.object), ctx);
	}

	protected Collection<DctmExportDelegate<?>> findDependents(IDfSession session, CmfObject<IDfValue> marshaled,
		T object, DctmExportContext ctx) throws Exception {
		return new ArrayList<DctmExportDelegate<?>>();
	}

	@Override
	protected final boolean marshal(DctmExportContext ctx, CmfObject<IDfValue> object) throws ExportException {
		try {
			final T typedObject = castObject(this.object);
			// First, the attributes
			final int attCount = this.object.getAttrCount();
			for (int i = 0; i < attCount; i++) {
				final IDfAttr attr = this.object.getAttr(i);
				final AttributeHandler handler = DctmAttributeHandlers.getAttributeHandler(getDctmType(), attr);
				// Get the attribute handler
				if (handler.includeInExport(this.object, attr)) {
					CmfAttribute<IDfValue> attribute = new CmfAttribute<IDfValue>(attr.getName(),
						DctmDataType.fromAttribute(attr).getStoredType(), attr.isRepeating(),
						handler.getExportableValues(this.object, attr));
					object.setAttribute(attribute);
				}
			}

			// Properties are different from attributes in that they require special handling. For
			// instance, a property would only be settable via direct SQL, or via an explicit method
			// call, etc., because setting it directly as an attribute would cmsImportResult in an
			// error from DFC, and therefore specialized code is required to handle it
			List<CmfProperty<IDfValue>> properties = new ArrayList<CmfProperty<IDfValue>>();
			getDataProperties(ctx, properties, typedObject);
			for (CmfProperty<IDfValue> property : properties) {
				// This mechanism overwrites properties, and intentionally so
				object.setProperty(property);
			}

			return true;
		} catch (DfException e) {
			throw new ExportException(String.format("Failed to export %s %s", getType(), getObjectId()), e);
		}
	}

	protected void getDataProperties(DctmExportContext ctx, Collection<CmfProperty<IDfValue>> properties, T object)
		throws DfException, ExportException {
	}

	@Override
	protected final List<CmfContentInfo> storeContent(DctmExportContext ctx,
		CmfAttributeTranslator<IDfValue> translator, CmfObject<IDfValue> marshaled, ExportTarget referrent,
		CmfContentStore<?, ?, ?> streamStore) throws Exception {
		return doStoreContent(ctx, translator, marshaled, referrent, castObject(this.object), streamStore);
	}

	protected List<CmfContentInfo> doStoreContent(DctmExportContext ctx, CmfAttributeTranslator<IDfValue> translator,
		CmfObject<IDfValue> marshaled, ExportTarget referrent, T object, CmfContentStore<?, ?, ?> streamStore)
		throws Exception {
		return null;
	}

	protected static <T extends IDfPersistentObject> T staticCast(Class<T> klazz, IDfPersistentObject p)
		throws ClassCastException {
		if (klazz == null) { throw new IllegalArgumentException("Must provide a class to cast to"); }
		if (p == null) { return null; }
		if (!klazz.isInstance(p)) { throw new ClassCastException(String.format("Can't convert a [%s] into a [%s]",
			p.getClass().getCanonicalName(), klazz.getCanonicalName())); }
		return klazz.cast(p);
	}
}