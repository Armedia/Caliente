package com.delta.cmsmf.datastore.cms;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.delta.cmsmf.constants.CMSMFAppConstants;
import com.delta.cmsmf.datastore.DataAttribute;
import com.delta.cmsmf.datastore.DataType;
import com.delta.cmsmf.datastore.DfValueFactory;
import com.delta.cmsmf.runtime.RunTimeProperties;
import com.documentum.fc.client.IDfPersistentObject;
import com.documentum.fc.common.DfException;
import com.documentum.fc.common.IDfAttr;
import com.documentum.fc.common.IDfValue;

public class CmsAttributeAdjuster {

	public static interface AttributeInterceptor {

		/**
		 * <p>
		 * Calculate alternate import values to be written out to the CMS for the given attribute as
		 * per the application requirements. Will return {@code null} if there are no alternates.
		 * </p>
		 *
		 * @return the {@link Collection} of {@link IDfValue} instances to use, or {@code null} if
		 *         the original values should be left intact.
		 * @throws DfException
		 */
		public Collection<IDfValue> getAlternateImportValues(IDfPersistentObject object, DataAttribute attribute)
			throws DfException;

		/**
		 * <p>
		 * Calculate alternate export values to be written out to SQL for the given attribute as per
		 * the application requirements. Will return {@code null} if there are no alternates.
		 * </p>
		 *
		 * @return the {@link Collection} of {@link IDfValue} instances to use, or {@code null} if
		 *         the original values should be left intact.
		 * @throws DfException
		 */
		public Collection<IDfValue> getAlternateExportValues(IDfPersistentObject object, IDfAttr attr)
			throws DfException;
	}

	private static AttributeInterceptor DBO_INTERCEPTOR = new AttributeInterceptor() {

		@Override
		public Collection<IDfValue> getAlternateImportValues(IDfPersistentObject object, DataAttribute attribute)
			throws DfException {
			if (!attribute.isRepeating()) {
				// Is this an operator attribute that needs interception?
				if (CMSMFAppConstants.DM_DBO.equals(attribute.getSingleValue().asString())) {
					String alternate = RunTimeProperties.getRunTimePropertiesInstance().getTargetRepoOperatorName(
						object.getSession());
					return Collections.singletonList(DfValueFactory.newStringValue(alternate));
				}
			}
			return null;
		}

		@Override
		public Collection<IDfValue> getAlternateExportValues(IDfPersistentObject object, IDfAttr attr)
			throws DfException {
			return Collections.singletonList(DfValueFactory.newStringValue(CMSMFAppConstants.DM_DBO));
		}
	};

	private static final Map<CmsObjectType, Map<DataType, Map<String, AttributeInterceptor>>> PER_TYPE;
	private static final Map<DataType, Map<String, AttributeInterceptor>> GLOBAL;

	static {
		Map<CmsObjectType, Map<DataType, Map<String, AttributeInterceptor>>> interceptors = new EnumMap<CmsObjectType, Map<DataType, Map<String, AttributeInterceptor>>>(
			CmsObjectType.class);
		for (CmsObjectType objectType : CmsObjectType.values()) {
			Map<DataType, Map<String, AttributeInterceptor>> outer = new EnumMap<DataType, Map<String, AttributeInterceptor>>(
				DataType.class);
			for (DataType dataType : DataType.values()) {
				outer.put(dataType, new ConcurrentHashMap<String, AttributeInterceptor>());
			}
			interceptors.put(objectType, outer);
		}
		PER_TYPE = Collections.unmodifiableMap(interceptors);

		Map<DataType, Map<String, AttributeInterceptor>> global = new EnumMap<DataType, Map<String, AttributeInterceptor>>(
			DataType.class);
		for (DataType dataType : DataType.values()) {
			global.put(dataType, new ConcurrentHashMap<String, AttributeInterceptor>());
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
				CmsAttributeAdjuster.setAttributeInterceptor(null, DataType.DF_STRING, att,
					CmsAttributeAdjuster.DBO_INTERCEPTOR);
			}
		}

		//
		// Next...
		//
	}

	private CmsAttributeAdjuster() {
	}

	private static Map<String, AttributeInterceptor> getAttributeInterceptorMap(CmsObjectType objectType,
		DataType dataType) {
		if (dataType == null) { throw new IllegalArgumentException(
			"Must provide a data type to retrieve the interceptor for"); }
		if (dataType == DataType.DF_UNDEFINED) { throw new IllegalArgumentException("DF_UNDEFINED is not supported"); }
		return (objectType == null ? CmsAttributeAdjuster.GLOBAL.get(dataType) : CmsAttributeAdjuster.PER_TYPE.get(
			objectType).get(dataType));
	}

	static AttributeInterceptor setAttributeInterceptor(CmsObjectType objectType, DataType dataType, String attribute,
		AttributeInterceptor interceptor) {
		if (attribute == null) { throw new IllegalArgumentException("Must provide an attribute name to intercept"); }
		Map<String, AttributeInterceptor> m = CmsAttributeAdjuster.getAttributeInterceptorMap(objectType, dataType);
		return (interceptor != null ? m.put(attribute, interceptor) : m.remove(attribute));
	}

	static AttributeInterceptor setAttributeInterceptor(CmsObjectType objectType, int dataType, String attribute,
		AttributeInterceptor interceptor) {
		return CmsAttributeAdjuster.setAttributeInterceptor(objectType, DataType.fromDfConstant(dataType), attribute,
			interceptor);
	}

	static AttributeInterceptor removeAttributeInterceptor(CmsObjectType objectType, DataType dataType, String attribute) {
		return CmsAttributeAdjuster.setAttributeInterceptor(objectType, dataType, attribute, null);
	}

	public static AttributeInterceptor getAttributeInterceptor(CmsObjectType objectType, DataType dataType,
		String attribute) {
		if (attribute == null) { throw new IllegalArgumentException("Must provide an attribute name to intercept"); }
		return CmsAttributeAdjuster.getAttributeInterceptorMap(objectType, dataType).get(attribute);
	}

	public static AttributeInterceptor getAttributeInterceptor(CmsObjectType objectType, DataAttribute attribute) {
		if (attribute == null) { throw new IllegalArgumentException("Must provide an attribute to intercept"); }
		return CmsAttributeAdjuster.getAttributeInterceptor(objectType, attribute.getType(), attribute.getName());
	}

	/**
	 * <p>
	 * Calculate alternate import values to be written out to the CMS for the given attribute as per
	 * the application requirements. Will return {@code null} if there are no alternates.
	 * </p>
	 *
	 * @return the {@link Collection} of {@link IDfValue} instances to use, or {@code null} if the
	 *         original values should be left intact.
	 * @throws DfException
	 */
	public static Collection<IDfValue> getAlternateImportValues(IDfPersistentObject object, DataAttribute attribute)
		throws DfException {
		if (object == null) { throw new IllegalArgumentException(
			"Must provide an object to calculate the alternative for"); }
		if (attribute == null) { throw new IllegalArgumentException(
			"Must provide an attribute to calculate the alternative for"); }
		AttributeInterceptor interceptor = CmsAttributeAdjuster.getAttributeInterceptor(
			CmsObjectType.decodeType(object), attribute);
		return (interceptor != null ? interceptor.getAlternateImportValues(object, attribute) : null);
	}

	/**
	 * <p>
	 * Calculate alternate export values to be written out to SQL for the given attribute as per the
	 * application requirements. Will return {@code null} if there are no alternates.
	 * </p>
	 *
	 * @return the {@link Collection} of {@link IDfValue} instances to use, or {@code null} if the
	 *         original values should be left intact.
	 * @throws DfException
	 */
	public static Collection<IDfValue> getAlternateExportValues(IDfPersistentObject object, IDfAttr attribute)
		throws DfException {
		if (object == null) { throw new IllegalArgumentException(
			"Must provide an object to calculate the alternative for"); }
		if (attribute == null) { throw new IllegalArgumentException(
			"Must provide an attribute to calculate the alternative for"); }
		final DataType dataType = DataType.fromDfConstant(attribute.getDataType());
		final CmsObjectType objectType = CmsObjectType.decodeType(object);
		AttributeInterceptor interceptor = CmsAttributeAdjuster.getAttributeInterceptor(objectType, dataType,
			attribute.getName());
		return (interceptor != null ? interceptor.getAlternateExportValues(object, attribute) : null);
	}
}