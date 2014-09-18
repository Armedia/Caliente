package com.delta.cmsmf.cms;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang3.text.StrTokenizer;

import com.delta.cmsmf.cfg.Setting;
import com.documentum.fc.client.IDfPersistentObject;
import com.documentum.fc.common.DfException;
import com.documentum.fc.common.IDfAttr;
import com.documentum.fc.common.IDfValue;

class CmsAttributeHandlers {

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
		public Collection<IDfValue> getImportableValues(IDfPersistentObject object, CmsAttribute attribute)
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
		public boolean includeInImport(IDfPersistentObject object, CmsAttribute attribute) throws DfException {
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
			return CmsMappingUtils.substituteSpecialUsers(object, attr);
		}

		@Override
		public Collection<IDfValue> getImportableValues(IDfPersistentObject object, CmsAttribute attribute)
			throws DfException {
			return CmsMappingUtils.resolveSpecialUsers(object, attribute);
		}
	};

	static final AttributeHandler DEFAULT_HANDLER = new AttributeHandler();
	static final AttributeHandler NO_IMPORT_HANDLER = new AttributeHandler() {
		@Override
		public boolean includeInImport(IDfPersistentObject object, CmsAttribute attribute) throws DfException {
			return false;
		}
	};

	private static final Map<CmsObjectType, Map<CmsDataType, Map<String, AttributeHandler>>> PER_TYPE;
	private static final Map<CmsDataType, Map<String, AttributeHandler>> GLOBAL;

	static {
		Map<CmsObjectType, Map<CmsDataType, Map<String, AttributeHandler>>> perObjectType = new EnumMap<CmsObjectType, Map<CmsDataType, Map<String, AttributeHandler>>>(
			CmsObjectType.class);
		for (CmsObjectType objectType : CmsObjectType.values()) {
			Map<CmsDataType, Map<String, AttributeHandler>> perCmsDataType = new EnumMap<CmsDataType, Map<String, AttributeHandler>>(
				CmsDataType.class);
			for (CmsDataType dataType : CmsDataType.values()) {
				perCmsDataType.put(dataType, new ConcurrentHashMap<String, AttributeHandler>());
			}
			perObjectType.put(objectType, Collections.unmodifiableMap(perCmsDataType));
		}
		PER_TYPE = Collections.unmodifiableMap(perObjectType);

		Map<CmsDataType, Map<String, AttributeHandler>> global = new EnumMap<CmsDataType, Map<String, AttributeHandler>>(
			CmsDataType.class);
		for (CmsDataType dataType : CmsDataType.values()) {
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
			CmsAttributeHandlers.setAttributeHandler(null, CmsDataType.DF_STRING, att,
				CmsAttributeHandlers.SESSION_CONFIG_USER_HANDLER);
		}

		//
		// Next...
		//
		CmsAttributeHandlers.setAttributeHandler(null, CmsDataType.DF_BOOLEAN, CmsAttributes.R_IMMUTABLE_FLAG,
			CmsAttributeHandlers.NO_IMPORT_HANDLER);
		CmsAttributeHandlers.setAttributeHandler(null, CmsDataType.DF_BOOLEAN, CmsAttributes.R_FROZEN_FLAG,
			CmsAttributeHandlers.NO_IMPORT_HANDLER);
	}

	private CmsAttributeHandlers() {
	}

	private static Map<String, AttributeHandler> getAttributeHandlerMap(CmsObjectType objectType, CmsDataType dataType) {
		if (dataType == null) { throw new IllegalArgumentException(
			"Must provide a data type to retrieve the interceptor for"); }
		if (dataType == CmsDataType.DF_UNDEFINED) { throw new IllegalArgumentException("DF_UNDEFINED is not supported"); }
		return (objectType == null ? CmsAttributeHandlers.GLOBAL.get(dataType) : CmsAttributeHandlers.PER_TYPE.get(
			objectType).get(dataType));
	}

	static AttributeHandler removeAttributeHandler(CmsObjectType objectType, CmsDataType dataType, String attributeName) {
		return CmsAttributeHandlers.setAttributeHandler(objectType, dataType, attributeName, null);
	}

	static AttributeHandler setAttributeHandler(CmsObjectType objectType, IDfAttr attribute,
		AttributeHandler interceptor) {
		if (attribute == null) { throw new IllegalArgumentException("Must provide an attribute to intercept"); }
		return CmsAttributeHandlers.setAttributeHandler(objectType, CmsDataType.fromAttribute(attribute),
			attribute.getName(), interceptor);
	}

	static AttributeHandler setAttributeHandler(CmsObjectType objectType, CmsDataType dataType, String attributeName,
		AttributeHandler interceptor) {
		if (attributeName == null) { throw new IllegalArgumentException("Must provide an attribute to intercept"); }
		Map<String, AttributeHandler> m = CmsAttributeHandlers.getAttributeHandlerMap(objectType, dataType);
		return (interceptor != null ? m.put(attributeName, interceptor) : m.remove(attributeName));
	}

	static AttributeHandler getAttributeHandler(CmsObjectType objectType, CmsAttribute attribute) {
		if (attribute == null) { throw new IllegalArgumentException("Must provide an attribute to intercept"); }
		return CmsAttributeHandlers.getAttributeHandler(objectType, attribute.getType(), attribute.getName());
	}

	static AttributeHandler getAttributeHandler(CmsObjectType objectType, IDfAttr attribute) {
		if (attribute == null) { throw new IllegalArgumentException("Must provide an attribute to intercept"); }
		final CmsDataType dataType = CmsDataType.fromAttribute(attribute);
		return CmsAttributeHandlers.getAttributeHandler(objectType, dataType, attribute.getName());
	}

	static AttributeHandler getAttributeHandler(IDfPersistentObject object, IDfAttr attribute) throws DfException,
	UnsupportedObjectTypeException {
		if (object == null) { throw new IllegalArgumentException(
			"Must provide an object to identify the attribute handler for"); }
		final CmsObjectType objectType = CmsObjectType.decodeType(object);
		return CmsAttributeHandlers.getAttributeHandler(objectType, CmsDataType.fromAttribute(attribute),
			attribute.getName());
	}

	static AttributeHandler getAttributeHandler(CmsObjectType objectType, CmsDataType dataType, String attributeName) {
		if (attributeName == null) { throw new IllegalArgumentException("Must provide an attribute name to intercept"); }
		AttributeHandler ret = CmsAttributeHandlers.getAttributeHandlerMap(objectType, dataType).get(attributeName);
		if (ret == null) {
			// Nothing, so try for the global one
			ret = CmsAttributeHandlers.getAttributeHandlerMap(null, dataType).get(attributeName);
		}
		return (ret == null ? CmsAttributeHandlers.DEFAULT_HANDLER : ret);
	}
}