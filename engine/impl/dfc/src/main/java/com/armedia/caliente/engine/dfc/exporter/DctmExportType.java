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
/**
 *
 */

package com.armedia.caliente.engine.dfc.exporter;

import java.math.BigInteger;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.chemistry.opencmis.commons.definitions.MutablePropertyDefinition;
import org.apache.chemistry.opencmis.commons.definitions.MutablePropertyStringDefinition;
import org.apache.chemistry.opencmis.commons.definitions.MutableTypeDefinition;
import org.apache.chemistry.opencmis.commons.definitions.PropertyDefinition;
import org.apache.chemistry.opencmis.commons.definitions.TypeDefinition;
import org.apache.chemistry.opencmis.commons.enums.BaseTypeId;
import org.apache.chemistry.opencmis.commons.enums.Cardinality;
import org.apache.chemistry.opencmis.commons.enums.ContentStreamAllowed;
import org.apache.chemistry.opencmis.commons.enums.PropertyType;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.DocumentTypeDefinitionImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.FolderTypeDefinitionImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.ItemTypeDefinitionImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.SecondaryTypeDefinitionImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.TypeMutabilityImpl;
import org.apache.commons.lang3.StringUtils;

import com.armedia.caliente.engine.common.TypeDefinitionEncoder;
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
import com.armedia.caliente.tools.dfc.DfValueFactory;
import com.armedia.caliente.tools.dfc.DfcUtils;
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
 *
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
			String.format("dmi_type_info where r_type_id = %s", DfcUtils.quoteString(type.getObjectId().getId())));
		if (typeInfo != null) {
			String aclDom = typeInfo.getString(DctmAttributes.ACL_DOMAIN);
			String aclName = typeInfo.getString(DctmAttributes.ACL_NAME);
			if (!StringUtils.isEmpty(aclDom) && !StringUtils.isEmpty(aclName)) {
				properties.add(new CmfProperty<>(IntermediateProperty.DEFAULT_ACL, CmfValue.Type.STRING, false,
					DfValueFactory.of(String.format("%s:%s", aclDom, aclName))));
			}

			String defaultStorage = typeInfo.getString(DctmAttributes.DEFAULT_STORAGE);
			if (!StringUtils.isEmpty(defaultStorage)) {
				try {
					IDfStore store = IDfStore.class.cast(ctx.getSession().getObject(new DfId(defaultStorage)));
					properties.add(new CmfProperty<>(IntermediateProperty.DEFAULT_STORAGE, CmfValue.Type.STRING, false,
						DfValueFactory.of(store.getName())));
				} catch (DfObjectNotFoundException e) {
					throw new ExportException(
						String.format("Type [%s] references a nonexistent default object store with ID [%s]",
							type.getName(), defaultStorage),
						e);
				}
			}

			properties.add(new CmfProperty<>(IntermediateProperty.DEFAULT_ASPECTS, CmfValue.Type.STRING, true,
				DfValueFactory.getAllValues(DctmAttributes.DEFAULT_ASPECTS, typeInfo)));
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
				IDfValue m = DfValueFactory
					.of(nameMapper.encodeAttributeName(dctmTypeObjectType.getStoredObjectType(), o.asString()));
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

		TypeDefinitionEncoder.encode(createTypeDefinition(type), properties::add, DfValueFactory::of);
		return ret;
	}

	protected TypeDefinition createTypeDefinition(IDfType type) throws DfException {
		Map<String, PropertyDefinition<?>> definitions = new LinkedHashMap<>();
		final int attCount = type.getTypeAttrCount();
		final int firstAtt = type.getInt(DctmAttributes.START_POS);
		for (int i = firstAtt; i < attCount; i++) {
			IDfAttr attr = type.getTypeAttr(i);
			final String name = attr.getName();
			final boolean repeating = attr.isRepeating();
			final int dataType = attr.getDataType();
			final int length = attr.getLength();
			final String desc = type.getTypeAttrDescription(name);

			PropertyType propertyType = null;
			switch (dataType) {
				case IDfValue.DF_BOOLEAN:
					propertyType = PropertyType.BOOLEAN;
					break;

				case IDfValue.DF_DOUBLE:
					propertyType = PropertyType.DECIMAL;
					break;

				case IDfValue.DF_ID:
					propertyType = PropertyType.ID;
					break;

				case IDfValue.DF_INTEGER:
					propertyType = PropertyType.INTEGER;
					break;

				case IDfValue.DF_TIME:
					propertyType = PropertyType.DATETIME;
					break;

				case IDfValue.DF_STRING:
				case IDfValue.DF_UNDEFINED:
				default:
					propertyType = PropertyType.STRING;
					break;

			}

			MutablePropertyDefinition<?> def = TypeDefinitionEncoder.constructDefinition(propertyType);
			def.setLocalName(name);
			def.setLocalNamespace("dctm:" + type.getName());
			def.setId(type.getName() + ":" + name);
			def.setCardinality(repeating ? Cardinality.MULTI : Cardinality.SINGLE);
			def.setIsInherited(false);
			def.setIsQueryable(true);
			def.setQueryName(name);
			def.setIsRequired(false); // TODO: What here?
			def.setDescription(desc);

			if (propertyType == PropertyType.STRING) {
				MutablePropertyStringDefinition.class.cast(def).setMaxLength(BigInteger.valueOf(length));
			}

			definitions.put(def.getId(), def);
		}

		MutableTypeDefinition typeDef = buildTypeDefinition(type);
		boolean fileable = typeDef.isFileable();
		typeDef.setDescription(type.getDescription());
		typeDef.setDisplayName(type.getName());
		typeDef.setId("dctm:" + type.getName());
		typeDef.setIsControllableAcl(fileable);
		typeDef.setIsControllablePolicy(fileable);
		typeDef.setIsCreatable(true);
		typeDef.setIsFileable(fileable);
		typeDef.setIsFulltextIndexed(false); // TODO: How to tell?
		typeDef.setIsIncludedInSupertypeQuery(false);
		typeDef.setIsQueryable(true);
		typeDef.setLocalName(type.getName());
		typeDef.setLocalNamespace("dctm");
		typeDef.setParentTypeId("dctm:" + type.getSuperName());
		typeDef.setQueryName(type.getName());
		TypeMutabilityImpl typeMutability = new TypeMutabilityImpl();
		typeMutability.setCanCreate(true); // TODO: HOW TO TELL?
		typeMutability.setCanDelete(true); // TODO: HOW TO TELL?
		typeMutability.setCanUpdate(true); // TODO: HOW TO TELL?
		typeDef.setTypeMutability(typeMutability);
		definitions.values().forEach(typeDef::addPropertyDefinition);
		return typeDef;
	}

	private MutableTypeDefinition buildTypeDefinition(IDfType type) throws DfException {
		if (type.isSubTypeOf("dm_folder")) {
			FolderTypeDefinitionImpl def = new FolderTypeDefinitionImpl();
			def.setBaseTypeId(BaseTypeId.CMIS_FOLDER);
			def.setIsFileable(true);
			return def;
		}

		if (type.isSubTypeOf("dm_sysobject")) {
			DocumentTypeDefinitionImpl def = new DocumentTypeDefinitionImpl();
			def.setIsVersionable(true);
			def.setIsFileable(true);
			def.setContentStreamAllowed(ContentStreamAllowed.ALLOWED);
			def.setBaseTypeId(BaseTypeId.CMIS_DOCUMENT);
			return def;
		}

		int typeCategory = type.getInt(DctmAttributes.TYPE_CATEGORY);
		if (typeCategory == 1) {
			SecondaryTypeDefinitionImpl def = new SecondaryTypeDefinitionImpl();
			def.setBaseTypeId(BaseTypeId.CMIS_SECONDARY);
			def.setIsFileable(false);
			return def;
		}

		ItemTypeDefinitionImpl def = new ItemTypeDefinitionImpl();
		def.setBaseTypeId(BaseTypeId.CMIS_ITEM);
		def.setIsFileable(false);
		return def;
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