/*******************************************************************************
 * #%L
 * Armedia Caliente
 * %%
 * Copyright (C) 2013 - 2019 Armedia, LLC
 * %%
 * This file is part of the Caliente software.
 *
 * If the software was purchased under a paid Caliente license, the terms of
 * the paid license agreement will prevail.  Otherwise, the software is
 * provided under the following open source license terms:
 *
 * Caliente is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Caliente is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Caliente. If not, see <http://www.gnu.org/licenses/>.
 * #L%
 *******************************************************************************/
package com.armedia.caliente.engine.dfc.exporter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.armedia.caliente.engine.converter.IntermediateAttribute;
import com.armedia.caliente.engine.dfc.DctmAttributeHandlers;
import com.armedia.caliente.engine.dfc.DctmAttributeHandlers.AttributeHandler;
import com.armedia.caliente.engine.dfc.DctmDataType;
import com.armedia.caliente.engine.dfc.DctmObjectType;
import com.armedia.caliente.engine.dfc.DctmSessionWrapper;
import com.armedia.caliente.engine.exporter.ExportDelegate;
import com.armedia.caliente.engine.exporter.ExportException;
import com.armedia.caliente.store.CmfAttribute;
import com.armedia.caliente.store.CmfAttributeTranslator;
import com.armedia.caliente.store.CmfContentStore;
import com.armedia.caliente.store.CmfContentStream;
import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.store.CmfProperty;
import com.documentum.fc.client.IDfPersistentObject;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.common.DfException;
import com.documentum.fc.common.IDfAttr;
import com.documentum.fc.common.IDfValue;

public abstract class DctmExportDelegate<T extends IDfPersistentObject> extends
	ExportDelegate<T, IDfSession, DctmSessionWrapper, IDfValue, DctmExportContext, DctmExportDelegateFactory, DctmExportEngine> {

	private final DctmObjectType dctmType;

	protected DctmExportDelegate(DctmExportDelegateFactory factory, IDfSession session, Class<T> objectClass, T object)
		throws Exception {
		super(factory, session, objectClass, object);
		this.dctmType = DctmObjectType.decodeType(object);
	}

	protected final DctmObjectType getDctmType() {
		return this.dctmType;
	}

	@Override
	protected final void prepareForStorage(DctmExportContext ctx, CmfObject<IDfValue> marshaled) throws Exception {
		super.prepareForStorage(ctx, marshaled);
		prepareForStorage(ctx, marshaled, this.object);
	}

	protected void prepareForStorage(DctmExportContext ctx, CmfObject<IDfValue> marshaled, T object)
		throws ExportException, DfException {
		// By default, do nothing
	}

	@Override
	protected final CmfObject.Archetype calculateType(IDfSession session, T object) throws Exception {
		return DctmObjectType.decodeType(object).getStoredObjectType();
	}

	@Override
	protected final String calculateObjectId(IDfSession session, T object) throws Exception {
		return object.getObjectId().getId();
	}

	@Override
	protected String calculateLabel(IDfSession session, T object) throws Exception {
		return String.format("%s[%s]", getDctmType().name(), getObjectId());
	}

	@Override
	protected final String calculateSearchKey(IDfSession session, T object) throws Exception {
		return calculateObjectId(session, object);
	}

	@Override
	protected String calculateHistoryId(IDfSession session, T object) throws Exception {
		return object.getObjectId().getId();
	}

	@Override
	protected final String calculateSubType(IDfSession session, CmfObject.Archetype type, T object) throws Exception {
		return object.getType().getName();
	}

	@Override
	protected final Collection<DctmExportDelegate<?>> identifyRequirements(CmfObject<IDfValue> marshaled,
		DctmExportContext ctx) throws Exception {
		return findRequirements(ctx.getSession(), marshaled, castObject(this.object), ctx);
	}

	protected Collection<DctmExportDelegate<?>> findRequirements(IDfSession session, CmfObject<IDfValue> marshaled,
		T object, DctmExportContext ctx) throws Exception {
		Collection<DctmExportDelegate<?>> ret = new ArrayList<>();
		ret.add(this.factory.newExportDelegate(session, object.getType()));
		return ret;
	}

	@Override
	protected final Collection<DctmExportDelegate<?>> identifyAntecedents(CmfObject<IDfValue> marshaled,
		DctmExportContext ctx) throws Exception {
		return findAntecedents(ctx.getSession(), marshaled, castObject(this.object), ctx);
	}

	protected Collection<DctmExportDelegate<?>> findAntecedents(IDfSession session, CmfObject<IDfValue> marshaled,
		T object, DctmExportContext ctx) throws Exception {
		return new ArrayList<>();
	}

	@Override
	protected final Collection<DctmExportDelegate<?>> identifySuccessors(CmfObject<IDfValue> marshaled,
		DctmExportContext ctx) throws Exception {
		return findSuccessors(ctx.getSession(), marshaled, castObject(this.object), ctx);
	}

	protected Collection<DctmExportDelegate<?>> findSuccessors(IDfSession session, CmfObject<IDfValue> marshaled,
		T object, DctmExportContext ctx) throws Exception {
		return new ArrayList<>();
	}

	@Override
	protected final Collection<DctmExportDelegate<?>> identifyDependents(CmfObject<IDfValue> marshaled,
		DctmExportContext ctx) throws Exception {
		return findDependents(ctx.getSession(), marshaled, castObject(this.object), ctx);
	}

	protected Collection<DctmExportDelegate<?>> findDependents(IDfSession session, CmfObject<IDfValue> marshaled,
		T object, DctmExportContext ctx) throws Exception {
		return new ArrayList<>();
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
					CmfAttribute<IDfValue> attribute = new CmfAttribute<>(attr.getName(),
						DctmDataType.fromAttribute(attr).getStoredType(), attr.isRepeating(),
						handler.getExportableValues(this.object, attr));
					object.setAttribute(attribute);
				}
			}

			// Then, the CMIS (Intermediate) attributes
			for (IntermediateAttribute att : IntermediateAttribute.values()) {
				final String tgtName = att.get();
				// If the attribute is already encoded, skip it
				if (object.hasAttribute(tgtName)) {
					continue;
				}

				// Identify the source object's attribute for this intermediate attribtue
				final String srcName = this.factory.getEngine().getTranslator().getAttributeNameMapper()
					.decodeAttributeName(getType(), tgtName);
				if (!this.object.hasAttr(srcName)) {
					continue;
				}

				// Copy srcName into a new attribute called cmisName if it doesn't already exist
				final int idx = this.object.findAttrIndex(srcName);
				final IDfAttr attr = this.object.getAttr(idx);
				final AttributeHandler handler = DctmAttributeHandlers.getAttributeHandler(getDctmType(), attr);
				if (handler.includeInExport(this.object, attr)) {
					CmfAttribute<IDfValue> attribute = new CmfAttribute<>(attr.getName(),
						DctmDataType.fromAttribute(attr).getStoredType(), attr.isRepeating(),
						handler.getExportableValues(this.object, attr));
					object.setAttribute(attribute);
				}
			}

			// Properties are different from attributes in that they require special handling. For
			// instance, a property would only be settable via direct SQL, or via an explicit method
			// call, etc., because setting it directly as an attribute would cmsImportResult in an
			// error from DFC, and therefore specialized code is required to handle it
			List<CmfProperty<IDfValue>> properties = new ArrayList<>();
			getDataProperties(ctx, properties, typedObject);
			// This mechanism overwrites properties, and intentionally so
			properties.forEach(object::setProperty);

			return true;
		} catch (DfException e) {
			throw new ExportException(String.format("Failed to export %s %s", getType(), getObjectId()), e);
		}
	}

	protected boolean getDataProperties(DctmExportContext ctx, Collection<CmfProperty<IDfValue>> properties, T object)
		throws DfException, ExportException {
		return true;
	}

	@Override
	protected final List<CmfContentStream> storeContent(DctmExportContext ctx,
		CmfAttributeTranslator<IDfValue> translator, CmfObject<IDfValue> marshaled, CmfContentStore<?, ?> streamStore,
		boolean includeRenditions) {
		try {
			return doStoreContent(ctx, translator, marshaled, castObject(this.object), streamStore, includeRenditions);
		} catch (DfException e) {
			this.log.error("Failed to store the content streams for {}", marshaled.getDescription(), e);
			return new ArrayList<>();
		}
	}

	protected List<CmfContentStream> doStoreContent(DctmExportContext ctx, CmfAttributeTranslator<IDfValue> translator,
		CmfObject<IDfValue> marshaled, T object, CmfContentStore<?, ?> streamStore, boolean includeRenditions)
		throws DfException {
		return new ArrayList<>();
	}

	protected static <T extends IDfPersistentObject> T staticCast(Class<T> klazz, IDfPersistentObject p)
		throws ClassCastException {
		if (klazz == null) { throw new IllegalArgumentException("Must provide a class to cast to"); }
		if (p == null) { return null; }
		if (!klazz.isInstance(p)) {
			throw new ClassCastException(String.format("Can't convert a [%s] into a [%s]",
				p.getClass().getCanonicalName(), klazz.getCanonicalName()));
		}
		return klazz.cast(p);
	}
}