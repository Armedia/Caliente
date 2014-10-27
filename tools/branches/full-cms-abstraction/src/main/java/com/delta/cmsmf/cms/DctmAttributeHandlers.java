package com.delta.cmsmf.cms;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang3.text.StrTokenizer;

import com.armedia.cmf.storage.StoredAttribute;
import com.delta.cmsmf.cfg.Setting;
import com.documentum.fc.client.IDfPersistentObject;
import com.documentum.fc.common.DfException;
import com.documentum.fc.common.IDfAttr;
import com.documentum.fc.common.IDfValue;

class DctmAttributeHandlers {

	static class AttributeHandler {

		/**
		 * <p>
		 * Calculate alternate import values to be written out to the CMS for the given attribute as
		 * per the application requirements.
		 * </p>
		 *
		 * @return the {@link Collection} of {@link IDfValue} instances to use, as extracted (and
		 *         possibly modified)
		 * @throws DfException
		 */
		public Collection<IDfValue> getImportableValues(IDfPersistentObject object, StoredAttribute<IDfValue> attribute)
			throws DfException {
			return attribute.getValues();
		}

		/**
		 * <p>
		 * Indicate whether this attribute should be included on the import ({@code true}) or not (
		 * {@code false}).
		 * </p>
		 *
		 * @param object
		 * @param attribute
		 * @return whether this attribute should be included on the import ({@code true}) or not (
		 *         {@code false})
		 * @throws DfException
		 */
		public boolean includeInImport(IDfPersistentObject object, StoredAttribute<IDfValue> attribute)
			throws DfException {
			return true;
		}

		/**
		 * <p>
		 * Calculate alternate export values to be written out to SQL for the given attribute as per
		 * the application requirements.
		 * </p>
		 *
		 * @return the {@link Collection} of {@link IDfValue} instances to use, as extracted (and
		 *         possibly modified) from the CMS.
		 * @throws DfException
		 */
		public Collection<IDfValue> getExportableValues(IDfPersistentObject object, IDfAttr attr) throws DfException {
			return DfValueFactory.getAllRepeatingValues(attr, object);
		}

		/**
		 * <p>
		 * Indicate whether this attribute should be included on the export ({@code true}) or not (
		 * {@code false}).
		 * </p>
		 *
		 * @param object
		 * @param attr
		 * @return whether this attribute should be included on the export ({@code true}) or not (
		 *         {@code false})
		 * @throws DfException
		 */
		public boolean includeInExport(IDfPersistentObject object, IDfAttr attr) throws DfException {
			return true;
		}
	}

	static final AttributeHandler SESSION_CONFIG_USER_HANDLER = new AttributeHandler() {
		@Override
		public Collection<IDfValue> getExportableValues(IDfPersistentObject object, IDfAttr attr) throws DfException {
			return DctmMappingUtils.substituteMappableUsers(object, attr);
		}

		@Override
		public Collection<IDfValue> getImportableValues(IDfPersistentObject object, StoredAttribute<IDfValue> attribute)
			throws DfException {
			return DctmMappingUtils.resolveMappableUsers(object, attribute);
		}
	};

	static final AttributeHandler DEFAULT_HANDLER = new AttributeHandler();
	static final AttributeHandler NO_IMPORT_HANDLER = new AttributeHandler() {
		@Override
		public boolean includeInImport(IDfPersistentObject object, StoredAttribute<IDfValue> attribute)
			throws DfException {
			return false;
		}
	};

	private static final Map<DctmObjectType, Map<DctmDataType, Map<String, AttributeHandler>>> PER_TYPE;
	private static final Map<DctmDataType, Map<String, AttributeHandler>> GLOBAL;

	static {
		Map<DctmObjectType, Map<DctmDataType, Map<String, AttributeHandler>>> perObjectType = new EnumMap<DctmObjectType, Map<DctmDataType, Map<String, AttributeHandler>>>(
			DctmObjectType.class);
		for (DctmObjectType objectType : DctmObjectType.values()) {
			Map<DctmDataType, Map<String, AttributeHandler>> perCmsDataType = new EnumMap<DctmDataType, Map<String, AttributeHandler>>(
				DctmDataType.class);
			for (DctmDataType dataType : DctmDataType.values()) {
				perCmsDataType.put(dataType, new ConcurrentHashMap<String, AttributeHandler>());
			}
			perObjectType.put(objectType, Collections.unmodifiableMap(perCmsDataType));
		}
		PER_TYPE = Collections.unmodifiableMap(perObjectType);

		Map<DctmDataType, Map<String, AttributeHandler>> global = new EnumMap<DctmDataType, Map<String, AttributeHandler>>(
			DctmDataType.class);
		for (DctmDataType dataType : DctmDataType.values()) {
			global.put(dataType, new ConcurrentHashMap<String, AttributeHandler>());
		}
		GLOBAL = Collections.unmodifiableMap(global);

		// Now, add the basic interceptors

		//
		// First, the operator names
		//
		String attrsToCheck = Setting.OWNER_ATTRIBUTES.getString();
		StrTokenizer strTokenizer = StrTokenizer.getCSVInstance(attrsToCheck);
		List<String> l = strTokenizer.getTokenList();
		for (String att : new HashSet<String>(l)) {
			DctmAttributeHandlers.setAttributeHandler(null, DctmDataType.DF_STRING, att,
				DctmAttributeHandlers.SESSION_CONFIG_USER_HANDLER);
		}

		//
		// Next...
		//
		DctmAttributeHandlers.setAttributeHandler(null, DctmDataType.DF_BOOLEAN, DctmAttributes.R_IMMUTABLE_FLAG,
			DctmAttributeHandlers.NO_IMPORT_HANDLER);
		DctmAttributeHandlers.setAttributeHandler(null, DctmDataType.DF_BOOLEAN, DctmAttributes.R_FROZEN_FLAG,
			DctmAttributeHandlers.NO_IMPORT_HANDLER);
	}

	private DctmAttributeHandlers() {
	}

	private static Map<String, AttributeHandler> getAttributeHandlerMap(DctmObjectType objectType, DctmDataType dataType) {
		if (dataType == null) { throw new IllegalArgumentException(
			"Must provide a data type to retrieve the interceptor for"); }
		if (dataType == DctmDataType.DF_UNDEFINED) { throw new IllegalArgumentException("DF_UNDEFINED is not supported"); }
		return (objectType == null ? DctmAttributeHandlers.GLOBAL.get(dataType) : DctmAttributeHandlers.PER_TYPE.get(
			objectType).get(dataType));
	}

	static AttributeHandler removeAttributeHandler(DctmObjectType objectType, DctmDataType dataType,
		String attributeName) {
		return DctmAttributeHandlers.setAttributeHandler(objectType, dataType, attributeName, null);
	}

	static AttributeHandler setAttributeHandler(DctmObjectType objectType, IDfAttr attribute,
		AttributeHandler interceptor) {
		if (attribute == null) { throw new IllegalArgumentException("Must provide an attribute to intercept"); }
		return DctmAttributeHandlers.setAttributeHandler(objectType, DctmDataType.fromAttribute(attribute),
			attribute.getName(), interceptor);
	}

	static AttributeHandler setAttributeHandler(DctmObjectType objectType, DctmDataType dataType, String attributeName,
		AttributeHandler interceptor) {
		if (attributeName == null) { throw new IllegalArgumentException("Must provide an attribute to intercept"); }
		Map<String, AttributeHandler> m = DctmAttributeHandlers.getAttributeHandlerMap(objectType, dataType);
		return (interceptor != null ? m.put(attributeName, interceptor) : m.remove(attributeName));
	}

	static AttributeHandler getAttributeHandler(DctmObjectType objectType, StoredAttribute<IDfValue> attribute) {
		if (attribute == null) { throw new IllegalArgumentException("Must provide an attribute to intercept"); }
		return DctmAttributeHandlers.getAttributeHandler(objectType, DctmTranslator.translateType(attribute.getType()),
			attribute.getName());
	}

	static AttributeHandler getAttributeHandler(DctmObjectType objectType, IDfAttr attribute) {
		if (attribute == null) { throw new IllegalArgumentException("Must provide an attribute to intercept"); }
		final DctmDataType dataType = DctmDataType.fromAttribute(attribute);
		return DctmAttributeHandlers.getAttributeHandler(objectType, dataType, attribute.getName());
	}

	static AttributeHandler getAttributeHandler(IDfPersistentObject object, IDfAttr attribute) throws DfException,
		UnsupportedObjectTypeException {
		if (object == null) { throw new IllegalArgumentException(
			"Must provide an object to identify the attribute handler for"); }
		final DctmObjectType objectType = DctmObjectType.decodeType(object);
		return DctmAttributeHandlers.getAttributeHandler(objectType, DctmDataType.fromAttribute(attribute),
			attribute.getName());
	}

	static AttributeHandler getAttributeHandler(DctmObjectType objectType, DctmDataType dataType, String attributeName) {
		if (attributeName == null) { throw new IllegalArgumentException("Must provide an attribute name to intercept"); }
		AttributeHandler ret = DctmAttributeHandlers.getAttributeHandlerMap(objectType, dataType).get(attributeName);
		if (ret == null) {
			// Nothing, so try for the global one
			ret = DctmAttributeHandlers.getAttributeHandlerMap(null, dataType).get(attributeName);
		}
		return (ret == null ? DctmAttributeHandlers.DEFAULT_HANDLER : ret);
	}
}