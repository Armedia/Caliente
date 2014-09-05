package com.delta.cmsmf.datastore.cms;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.delta.cmsmf.constants.CMSMFAppConstants;
import com.delta.cmsmf.datastore.DataAttribute;
import com.delta.cmsmf.datastore.DataStore;
import com.delta.cmsmf.datastore.DataType;
import com.delta.cmsmf.datastore.DfValueFactory;
import com.delta.cmsmf.runtime.RunTimeProperties;
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
		 *         possibly modified) from the {@link DataStore}.
		 * @throws DfException
		 */
		public Collection<IDfValue> getImportableValues(IDfPersistentObject object, DataAttribute attribute)
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
		public boolean includeInImport(IDfPersistentObject object, DataAttribute attribute) throws DfException {
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

	static final AttributeHandler DEFAULT_HANDLER = new AttributeHandler();

	private static final AttributeHandler DBO_HANDLER = new AttributeHandler() {

		@Override
		public Collection<IDfValue> getImportableValues(IDfPersistentObject object, DataAttribute attribute)
			throws DfException {
			if (!attribute.isRepeating()) {
				// Is this an operator attribute that needs interception?
				if (CMSMFAppConstants.DM_DBO.equals(attribute.getValue().asString())) {
					String alternate = RunTimeProperties.getRunTimePropertiesInstance().getTargetRepoOperatorName(
						object.getSession());
					return Collections.singletonList(DfValueFactory.newStringValue(alternate));
				}
			}
			return null;
		}

		@Override
		public Collection<IDfValue> getExportableValues(IDfPersistentObject object, IDfAttr attr) throws DfException {
			return Collections.singletonList(DfValueFactory.newStringValue(CMSMFAppConstants.DM_DBO));
		}
	};

	private static final Map<CmsObjectType, Map<DataType, Map<String, AttributeHandler>>> PER_TYPE;
	private static final Map<DataType, Map<String, AttributeHandler>> GLOBAL;

	static {
		Map<CmsObjectType, Map<DataType, Map<String, AttributeHandler>>> perObjectType = new EnumMap<CmsObjectType, Map<DataType, Map<String, AttributeHandler>>>(
			CmsObjectType.class);
		for (CmsObjectType objectType : CmsObjectType.values()) {
			Map<DataType, Map<String, AttributeHandler>> perDataType = new EnumMap<DataType, Map<String, AttributeHandler>>(
				DataType.class);
			for (DataType dataType : DataType.values()) {
				perDataType.put(dataType, new ConcurrentHashMap<String, AttributeHandler>());
			}
			perObjectType.put(objectType, Collections.unmodifiableMap(perDataType));
		}
		PER_TYPE = Collections.unmodifiableMap(perObjectType);

		Map<DataType, Map<String, AttributeHandler>> global = new EnumMap<DataType, Map<String, AttributeHandler>>(
			DataType.class);
		for (DataType dataType : DataType.values()) {
			global.put(dataType, new ConcurrentHashMap<String, AttributeHandler>());
		}
		GLOBAL = Collections.unmodifiableMap(global);

		// Now, add the basic interceptors

		//
		// First, the operator names
		//
		Set<String> operatorNameAttributes = RunTimeProperties.getRunTimePropertiesInstance()
			.getAttrsToCheckForRepoOperatorName();
		if (operatorNameAttributes != null) {
			for (String att : operatorNameAttributes) {
				CmsAttributeHandlers.setAttributeHandler(null, DataType.DF_STRING, att,
					CmsAttributeHandlers.DBO_HANDLER);
			}
		}

		//
		// Next...
		//
	}

	private CmsAttributeHandlers() {
	}

	private static Map<String, AttributeHandler> getAttributeHandlerMap(CmsObjectType objectType, DataType dataType) {
		if (dataType == null) { throw new IllegalArgumentException(
			"Must provide a data type to retrieve the interceptor for"); }
		if (dataType == DataType.DF_UNDEFINED) { throw new IllegalArgumentException("DF_UNDEFINED is not supported"); }
		return (objectType == null ? CmsAttributeHandlers.GLOBAL.get(dataType) : CmsAttributeHandlers.PER_TYPE.get(
			objectType).get(dataType));
	}

	static AttributeHandler setAttributeHandler(CmsObjectType objectType, DataType dataType, String attribute,
		AttributeHandler interceptor) {
		if (attribute == null) { throw new IllegalArgumentException("Must provide an attribute name to intercept"); }
		Map<String, AttributeHandler> m = CmsAttributeHandlers.getAttributeHandlerMap(objectType, dataType);
		return (interceptor != null ? m.put(attribute, interceptor) : m.remove(attribute));
	}

	static AttributeHandler setAttributeHandler(CmsObjectType objectType, int dataType, String attribute,
		AttributeHandler interceptor) {
		return CmsAttributeHandlers.setAttributeHandler(objectType, DataType.fromDfConstant(dataType), attribute,
			interceptor);
	}

	static AttributeHandler removeAttributeHandler(CmsObjectType objectType, DataType dataType, String attribute) {
		return CmsAttributeHandlers.setAttributeHandler(objectType, dataType, attribute, null);
	}

	static AttributeHandler getAttributeHandler(CmsObjectType objectType, DataType dataType, String attribute) {
		if (attribute == null) { throw new IllegalArgumentException("Must provide an attribute name to intercept"); }
		AttributeHandler ret = CmsAttributeHandlers.getAttributeHandlerMap(objectType, dataType).get(attribute);
		if (ret == null) {
			// Nothing, so try for the global one
			ret = CmsAttributeHandlers.getAttributeHandlerMap(null, dataType).get(attribute);
		}
		return (ret == null ? CmsAttributeHandlers.DEFAULT_HANDLER : ret);
	}

	static AttributeHandler getAttributeHandler(CmsObjectType objectType, DataAttribute attribute) {
		if (attribute == null) { throw new IllegalArgumentException("Must provide an attribute to intercept"); }
		return CmsAttributeHandlers.getAttributeHandler(objectType, attribute.getType(), attribute.getName());
	}

	static AttributeHandler getAttributeHandler(CmsObjectType objectType, IDfAttr attr) {
		if (attr == null) { throw new IllegalArgumentException("Must provide an attribute to intercept"); }
		final DataType dataType = DataType.fromDfConstant(attr.getDataType());
		return CmsAttributeHandlers.getAttributeHandler(objectType, dataType, attr.getName());
	}

	static AttributeHandler getAttributeHandler(IDfPersistentObject object, IDfAttr attr) throws DfException {
		if (object == null) { throw new IllegalArgumentException(
			"Must provide an object to identify the attribute handler for"); }
		final CmsObjectType objectType = CmsObjectType.decodeType(object);
		return CmsAttributeHandlers.getAttributeHandler(objectType, DataType.fromDfConstant(attr.getDataType()),
			attr.getName());
	}
}