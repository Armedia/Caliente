/**
 *
 */

package com.armedia.cmf.engine.documentum.exporter;

import java.util.Collection;

import com.armedia.cmf.engine.converter.IntermediateProperty;
import com.armedia.cmf.engine.documentum.DctmAttributes;
import com.armedia.cmf.engine.documentum.DctmDataType;
import com.armedia.cmf.engine.documentum.DctmObjectType;
import com.armedia.cmf.engine.documentum.DfValueFactory;
import com.armedia.cmf.engine.documentum.UnsupportedDctmObjectTypeException;
import com.armedia.cmf.engine.exporter.ExportException;
import com.armedia.cmf.storage.CmfAttributeTranslator;
import com.armedia.cmf.storage.CmfDataType;
import com.armedia.cmf.storage.CmfObject;
import com.armedia.cmf.storage.CmfProperty;
import com.documentum.fc.client.IDfPersistentObject;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.client.IDfType;
import com.documentum.fc.client.IDfValidator;
import com.documentum.fc.client.IDfValueAssistance;
import com.documentum.fc.common.DfException;
import com.documentum.fc.common.DfId;
import com.documentum.fc.common.IDfAttr;
import com.documentum.fc.common.IDfList;
import com.documentum.fc.common.IDfValue;

/**
 * @author diego
 *
 */
public class DctmExportType extends DctmExportDelegate<IDfType> {

	protected DctmExportType(DctmExportDelegateFactory factory, IDfType type) throws Exception {
		super(factory, IDfType.class, type);
	}

	DctmExportType(DctmExportDelegateFactory factory, IDfPersistentObject type) throws Exception {
		this(factory, DctmExportDelegate.staticCast(IDfType.class, type));
	}

	@Override
	protected String calculateLabel(IDfType type) throws Exception {
		String superName = type.getSuperName();
		if ((superName != null) && (superName.length() > 0)) {
			superName = String.format(" (extends %s)", superName);
		} else {
			superName = "";
		}
		return String.format("%s%s", type.getName(), superName);
	}

	@Override
	protected String calculateBatchId(IDfType type) throws DfException {
		// Calculate the maximum depth that this folder resides in, from its parents.
		// Keep track of visited nodes, and explode on a loop.
		// We return it in zero-padded hex to allow for large numbers (up to 2^64
		// depth), and also maintain consistent sorting
		return String.format("%016x", calculateDepth(type.getSession(), type));
	}

	private int calculateDepth(IDfSession session, IDfType type) throws DfException {
		if (type == null) { return -1; }
		return calculateDepth(session, type.getSuperType()) + 1;
	}

	@SuppressWarnings("unused")
	protected boolean getExtraProperties(DctmExportContext ctx, Collection<CmfProperty<IDfValue>> properties,
		IDfType type) throws DfException, ExportException {

		// Now for the value assistance
		final IDfPersistentObject obj = ctx.getSession().getObject(new DfId(ctx.getReferrent().getId()));
		final int attCount = type.getTypeAttrCount();
		final IDfValidator validator = obj.getValidator();
		for (int i = type.getInt(DctmAttributes.START_POS); i < attCount; i++) {
			IDfAttr attr = type.getTypeAttr(i);
			final String name = attr.getName();
			final boolean repeating = attr.isRepeating();
			final int dataType = attr.getDataType();
			final int length = attr.getLength();
			final String desc = type.getTypeAttrDescription(name);

			ctx.printf("TYPEEXPORT: %s::%s (%s/%s/%d/%s)", type.getName(), name, repeating ? "R" : "S",
				DctmDataType.fromDataType(dataType), length, desc);

			final IDfValueAssistance va;
			if (obj.hasAttr(name)) {
				va = validator.getValueAssistance(name, null);
			} else {
				va = null;
			}

			if (va != null) {
				IDfList vaValues = va.getActualValues();
				IDfList vaLabels = va.getDisplayValues();
				final int vaValueCount = vaValues.getCount();
				final int vaLabelCount = vaLabels.getCount();
				if (vaValueCount != vaLabels.getCount()) {
					// The count doesn't match, so can't use this
					this.log.warn(String.format(
						"Attribute %s::%s has an incorrect count for ValueAssist values (%d) vs Labels (%d), can't continue storing VA information for it",
						type.getName(), name, vaValueCount, vaLabelCount));
					// TODO: store empty values
					continue;
				}

				for (int v = 0; v < vaValueCount; v++) {
					IDfValue value = vaValues.getValue(i);
					String label = vaLabels.getString(i);
					// TODO: Export this label and value
				}
			}
		}
		return true;
	}

	@Override
	protected boolean getDataProperties(DctmExportContext ctx, Collection<CmfProperty<IDfValue>> properties,
		IDfType type) throws DfException, ExportException {
		if (!super.getDataProperties(ctx, properties, type)) { return false; }
		String typeName = type.getString(DctmAttributes.NAME);
		IDfType dfType = type.getSession().getType(typeName);
		// If there's no dfType, then we clearly have an issue
		if (dfType == null) { throw new ExportException(String.format(
			"Could not locate the type object for [%s] even though its definition is being exported", typeName)); }
		// getExtraProperties(ctx, properties, dfType);
		DctmObjectType dctmTypeObjectType;
		boolean ret = false;
		try {
			dctmTypeObjectType = DctmObjectType.decodeType(dfType);

			final int attCount = type.getValueCount(DctmAttributes.ATTR_NAME);
			// We map the name for every attribute, just to be safe
			final CmfAttributeTranslator<IDfValue> translator = this.factory.getTranslator();
			CmfProperty<IDfValue> orig = new CmfProperty<IDfValue>(IntermediateProperty.ORIG_ATTR_NAME,
				CmfDataType.STRING);
			CmfProperty<IDfValue> mapped = new CmfProperty<IDfValue>(IntermediateProperty.MAPPED_ATTR_NAME,
				CmfDataType.STRING);
			for (int i = 0; i < attCount; i++) {
				IDfValue o = type.getRepeatingValue(DctmAttributes.ATTR_NAME, i);
				IDfValue m = DfValueFactory.newStringValue(
					translator.encodeAttributeName(dctmTypeObjectType.getStoredObjectType(), o.asString()));
				orig.addValue(o);
				mapped.addValue(m);
			}
			properties.add(orig);
			properties.add(mapped);
			ret = true;
		} catch (UnsupportedDctmObjectTypeException e) {
			// if this isn't a type we support (because it's a supertype that doesn't map to
			// a concrete type), then we simply skip adding the "target" marker property
		}
		return ret;
	}

	@Override
	protected Collection<DctmExportDelegate<?>> findRequirements(IDfSession session, CmfObject<IDfValue> marshaled,
		IDfType type, DctmExportContext ctx) throws Exception {
		Collection<DctmExportDelegate<?>> ret = super.findRequirements(session, marshaled, type, ctx);
		IDfType superType = type.getSuperType();
		if (superType != null) {
			if (ctx.isSpecialType(superType.getName())) {
				this.log.warn(String.format("Will not export special type [%s] (supertype of [%s])",
					superType.getName(), type.getName()));
			} else {
				ret.add(this.factory.newExportDelegate(superType));
			}
		}
		return ret;
	}
}