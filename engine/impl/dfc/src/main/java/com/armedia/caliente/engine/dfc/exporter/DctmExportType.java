/**
 *
 */

package com.armedia.caliente.engine.dfc.exporter;

import java.util.Collection;

import org.apache.commons.lang3.StringUtils;

import com.armedia.caliente.engine.converter.IntermediateProperty;
import com.armedia.caliente.engine.dfc.DctmAttributes;
import com.armedia.caliente.engine.dfc.DctmObjectType;
import com.armedia.caliente.engine.dfc.UnsupportedDctmObjectTypeException;
import com.armedia.caliente.engine.exporter.ExportException;
import com.armedia.caliente.store.CmfAttributeNameMapper;
import com.armedia.caliente.store.CmfAttributeTranslator;
import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.store.CmfProperty;
import com.armedia.caliente.store.CmfValue;
import com.armedia.caliente.tools.dfc.DfUtils;
import com.armedia.caliente.tools.dfc.DfValueFactory;
import com.documentum.fc.client.DfObjectNotFoundException;
import com.documentum.fc.client.IDfPersistentObject;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.client.IDfType;
import com.documentum.fc.client.IDfValidator;
import com.documentum.fc.client.IDfValueAssistance;
import com.documentum.fc.client.content.IDfStore;
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

	protected DctmExportType(DctmExportDelegateFactory factory, IDfSession session, IDfType type) throws Exception {
		super(factory, session, IDfType.class, type);
	}

	DctmExportType(DctmExportDelegateFactory factory, IDfSession session, IDfPersistentObject type) throws Exception {
		this(factory, session, DctmExportDelegate.staticCast(IDfType.class, type));
	}

	@Override
	protected String calculateLabel(IDfSession session, IDfType type) throws Exception {
		String superName = type.getSuperName();
		if ((superName != null) && (superName.length() > 0)) {
			superName = String.format(" (extends %s)", superName);
		} else {
			superName = "";
		}
		return String.format("%s%s", type.getName(), superName);
	}

	@Override
	protected int calculateDependencyTier(IDfSession session, IDfType type) throws Exception {
		return calculateDepth(session, type);
	}

	private int calculateDepth(IDfSession session, IDfType type) throws DfException {
		if (type == null) { return -1; }
		return calculateDepth(session, type.getSuperType()) + 1;
	}

	@SuppressWarnings("unused")
	protected boolean getExtraProperties(DctmExportContext ctx, Collection<CmfProperty<IDfValue>> properties,
		IDfType type) throws DfException, ExportException {

		// First, as much as we can get from the type info
		IDfPersistentObject typeInfo = ctx.getSession().getObjectByQualification(
			String.format("dmi_type_info where r_type_id = %s", DfUtils.quoteString(type.getObjectId().getId())));
		if (typeInfo != null) {
			String aclDom = typeInfo.getString(DctmAttributes.ACL_DOMAIN);
			String aclName = typeInfo.getString(DctmAttributes.ACL_NAME);
			if (!StringUtils.isEmpty(aclDom) && !StringUtils.isEmpty(aclName)) {
				properties.add(new CmfProperty<>(IntermediateProperty.DEFAULT_ACL, CmfValue.Type.STRING, false,
					DfValueFactory.newStringValue(String.format("%s:%s", aclDom, aclName))));
			}

			String defaultStorage = typeInfo.getString(DctmAttributes.DEFAULT_STORAGE);
			if (!StringUtils.isEmpty(defaultStorage)) {
				try {
					IDfStore store = IDfStore.class.cast(ctx.getSession().getObject(new DfId(defaultStorage)));
					properties.add(new CmfProperty<>(IntermediateProperty.DEFAULT_STORAGE, CmfValue.Type.STRING, false,
						DfValueFactory.newStringValue(store.getName())));
				} catch (DfObjectNotFoundException e) {
					throw new ExportException(
						String.format("Type [%s] references a nonexistent default object store with ID [%s]",
							type.getName(), defaultStorage),
						e);
				}
			}

			properties.add(new CmfProperty<>(IntermediateProperty.DEFAULT_ASPECTS, CmfValue.Type.STRING, true,
				DfValueFactory.getAllRepeatingValues(DctmAttributes.DEFAULT_ASPECTS, typeInfo)));
		}

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
					this.log.warn(
						"Attribute {}::{} has an incorrect count for ValueAssist values ({}) vs Labels ({}), can't continue storing VA information for it",
						type.getName(), name, vaValueCount, vaLabelCount);
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
		IDfType dfType = ctx.getSession().getType(typeName);
		// If there's no dfType, then we clearly have an issue
		if (dfType == null) {
			throw new ExportException(String.format(
				"Could not locate the type object for [%s] even though its definition is being exported", typeName));
		}
		// getExtraProperties(ctx, properties, dfType);
		DctmObjectType dctmTypeObjectType;
		boolean ret = false;
		try {
			dctmTypeObjectType = DctmObjectType.decodeType(dfType);

			final int attCount = type.getValueCount(DctmAttributes.ATTR_NAME);
			// We map the name for every attribute, just to be safe
			final CmfAttributeTranslator<IDfValue> translator = this.factory.getTranslator();
			CmfProperty<IDfValue> orig = new CmfProperty<>(IntermediateProperty.ORIG_ATTR_NAME, CmfValue.Type.STRING);
			CmfProperty<IDfValue> mapped = new CmfProperty<>(IntermediateProperty.MAPPED_ATTR_NAME,
				CmfValue.Type.STRING);
			CmfAttributeNameMapper nameMapper = translator.getAttributeNameMapper();
			for (int i = 0; i < attCount; i++) {
				IDfValue o = type.getRepeatingValue(DctmAttributes.ATTR_NAME, i);
				IDfValue m = DfValueFactory.newStringValue(
					nameMapper.encodeAttributeName(dctmTypeObjectType.getStoredObjectType(), o.asString()));
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
				this.log.warn("Will not export special type [{}] (supertype of [{}])", superType.getName(),
					type.getName());
			} else {
				ret.add(this.factory.newExportDelegate(session, superType));
			}
		}
		return ret;
	}

	@Override
	protected String calculateName(IDfSession session, IDfType type) throws Exception {
		return type.getName();
	}
}