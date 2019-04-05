package com.armedia.caliente.engine.dfc;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.text.StringTokenizer;

import com.armedia.caliente.store.CmfAttribute;
import com.armedia.caliente.tools.dfc.DfValueFactory;
import com.armedia.commons.utilities.CfgTools;
import com.documentum.fc.client.IDfPersistentObject;
import com.documentum.fc.common.DfException;
import com.documentum.fc.common.IDfAttr;
import com.documentum.fc.common.IDfValue;

public class DctmAttributeHandlers {

	public static class AttributeHandler {

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
		public Collection<IDfValue> getImportableValues(IDfPersistentObject object, CmfAttribute<IDfValue> attribute)
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
		public boolean includeInImport(IDfPersistentObject object, CmfAttribute<IDfValue> attribute)
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

	public static final AttributeHandler SESSION_CONFIG_USER_HANDLER = new AttributeHandler() {
		@Override
		public Collection<IDfValue> getExportableValues(IDfPersistentObject object, IDfAttr attr) throws DfException {
			return DctmMappingUtils.substituteMappableUsers(object, attr);
		}

		@Override
		public Collection<IDfValue> getImportableValues(IDfPersistentObject object, CmfAttribute<IDfValue> attribute)
			throws DfException {
			return DctmMappingUtils.resolveMappableUsers(object, attribute);
		}
	};

	public static final AttributeHandler DEFAULT_HANDLER = new AttributeHandler();
	public static final AttributeHandler NO_IMPORT_HANDLER = new AttributeHandler() {
		@Override
		public boolean includeInImport(IDfPersistentObject object, CmfAttribute<IDfValue> attribute)
			throws DfException {
			return false;
		}
	};

	public static final AttributeHandler USER_NAME_HANDLER = new AttributeHandler() {
		@Override
		public Collection<IDfValue> getExportableValues(IDfPersistentObject object, IDfAttr attr) throws DfException {
			return DctmAttributeHandlers.SESSION_CONFIG_USER_HANDLER.getExportableValues(object, attr);
		}

		@Override
		public Collection<IDfValue> getImportableValues(IDfPersistentObject object, CmfAttribute<IDfValue> attribute)
			throws DfException {
			return DctmAttributeHandlers.SESSION_CONFIG_USER_HANDLER.getImportableValues(object, attribute);
		}

		@Override
		public boolean includeInImport(IDfPersistentObject object, CmfAttribute<IDfValue> attribute)
			throws DfException {
			return false;
		}
	};

	private static final Map<DctmObjectType, Map<DctmDataType, Map<String, AttributeHandler>>> PER_TYPE;
	private static final Map<DctmDataType, Map<String, AttributeHandler>> GLOBAL;
	private static boolean OPERATORS_INITIALIZED = false;

	static {
		Map<DctmObjectType, Map<DctmDataType, Map<String, AttributeHandler>>> perObjectType = new EnumMap<>(
			DctmObjectType.class);
		for (DctmObjectType objectType : DctmObjectType.values()) {
			Map<DctmDataType, Map<String, AttributeHandler>> perCmsDataType = new EnumMap<>(DctmDataType.class);
			for (DctmDataType dataType : DctmDataType.values()) {
				perCmsDataType.put(dataType, new ConcurrentHashMap<String, AttributeHandler>());
			}
			perObjectType.put(objectType, Collections.unmodifiableMap(perCmsDataType));
		}
		PER_TYPE = Collections.unmodifiableMap(perObjectType);

		Map<DctmDataType, Map<String, AttributeHandler>> global = new EnumMap<>(DctmDataType.class);
		for (DctmDataType dataType : DctmDataType.values()) {
			global.put(dataType, new ConcurrentHashMap<String, AttributeHandler>());
		}
		GLOBAL = Collections.unmodifiableMap(global);

		// Now, add the basic interceptors

		//
		// Next...
		//
		DctmAttributeHandlers.setAttributeHandler(null, DctmDataType.DF_BOOLEAN, DctmAttributes.R_IMMUTABLE_FLAG,
			DctmAttributeHandlers.NO_IMPORT_HANDLER);
		DctmAttributeHandlers.setAttributeHandler(null, DctmDataType.DF_BOOLEAN, DctmAttributes.R_FROZEN_FLAG,
			DctmAttributeHandlers.NO_IMPORT_HANDLER);

		// These are the attributes that require special handling on import

		//
		// ACL
		//
		DctmAttributeHandlers.setAttributeHandler(DctmObjectType.ACL, DctmDataType.DF_STRING, DctmAttributes.OWNER_NAME,
			DctmAttributeHandlers.USER_NAME_HANDLER);
		DctmAttributeHandlers.setAttributeHandler(DctmObjectType.ACL, DctmDataType.DF_STRING,
			DctmAttributes.OBJECT_NAME, DctmAttributeHandlers.NO_IMPORT_HANDLER);

		DctmAttributeHandlers.setAttributeHandler(DctmObjectType.ACL, DctmDataType.DF_STRING,
			DctmAttributes.R_ACCESSOR_NAME, DctmAttributeHandlers.NO_IMPORT_HANDLER);
		DctmAttributeHandlers.setAttributeHandler(DctmObjectType.ACL, DctmDataType.DF_STRING,
			DctmAttributes.R_ACCESSOR_PERMIT, DctmAttributeHandlers.NO_IMPORT_HANDLER);
		DctmAttributeHandlers.setAttributeHandler(DctmObjectType.ACL, DctmDataType.DF_STRING, DctmAttributes.R_IS_GROUP,
			DctmAttributeHandlers.NO_IMPORT_HANDLER);
		DctmAttributeHandlers.setAttributeHandler(DctmObjectType.ACL, DctmDataType.DF_STRING,
			DctmAttributes.R_ACCESSOR_XPERMIT, DctmAttributeHandlers.NO_IMPORT_HANDLER);

		//
		// Document
		//
		DctmAttributeHandlers.setAttributeHandler(DctmObjectType.DOCUMENT, DctmDataType.DF_ID,
			DctmAttributes.I_FOLDER_ID, DctmAttributeHandlers.NO_IMPORT_HANDLER);
		DctmAttributeHandlers.setAttributeHandler(DctmObjectType.DOCUMENT, DctmDataType.DF_ID,
			DctmAttributes.I_ANTECEDENT_ID, DctmAttributeHandlers.NO_IMPORT_HANDLER);
		DctmAttributeHandlers.setAttributeHandler(DctmObjectType.DOCUMENT, DctmDataType.DF_ID,
			DctmAttributes.I_CHRONICLE_ID, DctmAttributeHandlers.NO_IMPORT_HANDLER);

		DctmAttributeHandlers.setAttributeHandler(DctmObjectType.DOCUMENT, DctmDataType.DF_STRING,
			DctmAttributes.OWNER_NAME, DctmAttributeHandlers.USER_NAME_HANDLER);
		DctmAttributeHandlers.setAttributeHandler(DctmObjectType.DOCUMENT, DctmDataType.DF_INTEGER,
			DctmAttributes.OWNER_PERMIT, DctmAttributeHandlers.NO_IMPORT_HANDLER);
		DctmAttributeHandlers.setAttributeHandler(DctmObjectType.DOCUMENT, DctmDataType.DF_STRING,
			DctmAttributes.GROUP_NAME, DctmAttributeHandlers.NO_IMPORT_HANDLER);
		DctmAttributeHandlers.setAttributeHandler(DctmObjectType.DOCUMENT, DctmDataType.DF_INTEGER,
			DctmAttributes.GROUP_PERMIT, DctmAttributeHandlers.NO_IMPORT_HANDLER);
		DctmAttributeHandlers.setAttributeHandler(DctmObjectType.DOCUMENT, DctmDataType.DF_INTEGER,
			DctmAttributes.WORLD_PERMIT, DctmAttributeHandlers.NO_IMPORT_HANDLER);
		DctmAttributeHandlers.setAttributeHandler(DctmObjectType.DOCUMENT, DctmDataType.DF_STRING,
			DctmAttributes.ACL_DOMAIN, DctmAttributeHandlers.USER_NAME_HANDLER);
		DctmAttributeHandlers.setAttributeHandler(DctmObjectType.DOCUMENT, DctmDataType.DF_STRING,
			DctmAttributes.ACL_NAME, DctmAttributeHandlers.NO_IMPORT_HANDLER);

		DctmAttributeHandlers.setAttributeHandler(DctmObjectType.DOCUMENT, DctmDataType.DF_ID,
			DctmAttributes.BINDING_CONDITION, DctmAttributeHandlers.NO_IMPORT_HANDLER);
		DctmAttributeHandlers.setAttributeHandler(DctmObjectType.DOCUMENT, DctmDataType.DF_ID,
			DctmAttributes.BINDING_LABEL, DctmAttributeHandlers.NO_IMPORT_HANDLER);

		// We don't use these, but we should keep them from being copied over
		DctmAttributeHandlers.setAttributeHandler(DctmObjectType.DOCUMENT, DctmDataType.DF_ID,
			DctmAttributes.LOCAL_FOLDER_LINK, DctmAttributeHandlers.NO_IMPORT_HANDLER);
		DctmAttributeHandlers.setAttributeHandler(DctmObjectType.DOCUMENT, DctmDataType.DF_ID,
			DctmAttributes.REFERENCE_DB_NAME, DctmAttributeHandlers.NO_IMPORT_HANDLER);
		DctmAttributeHandlers.setAttributeHandler(DctmObjectType.DOCUMENT, DctmDataType.DF_ID,
			DctmAttributes.REFERENCE_BY_ID, DctmAttributeHandlers.NO_IMPORT_HANDLER);
		DctmAttributeHandlers.setAttributeHandler(DctmObjectType.DOCUMENT, DctmDataType.DF_ID,
			DctmAttributes.REFERENCE_BY_NAME, DctmAttributeHandlers.NO_IMPORT_HANDLER);
		DctmAttributeHandlers.setAttributeHandler(DctmObjectType.DOCUMENT, DctmDataType.DF_ID,
			DctmAttributes.REFRESH_INTERVAL, DctmAttributeHandlers.NO_IMPORT_HANDLER);
		DctmAttributeHandlers.setAttributeHandler(DctmObjectType.DOCUMENT, DctmDataType.DF_STRING,
			DctmAttributes.A_STORAGE_TYPE, DctmAttributeHandlers.NO_IMPORT_HANDLER);

		//
		// Folder
		//
		DctmAttributeHandlers.setAttributeHandler(DctmObjectType.FOLDER, DctmDataType.DF_STRING,
			DctmAttributes.R_FOLDER_PATH, DctmAttributeHandlers.NO_IMPORT_HANDLER);
		DctmAttributeHandlers.setAttributeHandler(DctmObjectType.FOLDER, DctmDataType.DF_STRING,
			DctmAttributes.OBJECT_NAME, DctmAttributeHandlers.NO_IMPORT_HANDLER);

		DctmAttributeHandlers.setAttributeHandler(DctmObjectType.FOLDER, DctmDataType.DF_STRING,
			DctmAttributes.OWNER_NAME, DctmAttributeHandlers.USER_NAME_HANDLER);
		DctmAttributeHandlers.setAttributeHandler(DctmObjectType.FOLDER, DctmDataType.DF_INTEGER,
			DctmAttributes.OWNER_PERMIT, DctmAttributeHandlers.NO_IMPORT_HANDLER);
		DctmAttributeHandlers.setAttributeHandler(DctmObjectType.FOLDER, DctmDataType.DF_STRING,
			DctmAttributes.GROUP_NAME, DctmAttributeHandlers.NO_IMPORT_HANDLER);
		DctmAttributeHandlers.setAttributeHandler(DctmObjectType.FOLDER, DctmDataType.DF_INTEGER,
			DctmAttributes.GROUP_PERMIT, DctmAttributeHandlers.NO_IMPORT_HANDLER);
		DctmAttributeHandlers.setAttributeHandler(DctmObjectType.FOLDER, DctmDataType.DF_INTEGER,
			DctmAttributes.WORLD_PERMIT, DctmAttributeHandlers.NO_IMPORT_HANDLER);
		DctmAttributeHandlers.setAttributeHandler(DctmObjectType.FOLDER, DctmDataType.DF_STRING,
			DctmAttributes.ACL_DOMAIN, DctmAttributeHandlers.USER_NAME_HANDLER);
		DctmAttributeHandlers.setAttributeHandler(DctmObjectType.FOLDER, DctmDataType.DF_STRING,
			DctmAttributes.ACL_NAME, DctmAttributeHandlers.NO_IMPORT_HANDLER);
		DctmAttributeHandlers.setAttributeHandler(DctmObjectType.FOLDER, DctmDataType.DF_STRING,
			DctmAttributes.A_STORAGE_TYPE, DctmAttributeHandlers.NO_IMPORT_HANDLER);

		//
		// Format
		//
		DctmAttributeHandlers.setAttributeHandler(DctmObjectType.FORMAT, DctmDataType.DF_STRING, DctmAttributes.NAME,
			DctmAttributeHandlers.NO_IMPORT_HANDLER);

		//
		// Group
		//
		DctmAttributeHandlers.setAttributeHandler(DctmObjectType.GROUP, DctmDataType.DF_STRING,
			DctmAttributes.GROUP_ADMIN, DctmAttributeHandlers.USER_NAME_HANDLER);
		DctmAttributeHandlers.setAttributeHandler(DctmObjectType.GROUP, DctmDataType.DF_STRING,
			DctmAttributes.OWNER_NAME, DctmAttributeHandlers.USER_NAME_HANDLER);
		DctmAttributeHandlers.setAttributeHandler(DctmObjectType.GROUP, DctmDataType.DF_STRING,
			DctmAttributes.GROUP_NAME, DctmAttributeHandlers.NO_IMPORT_HANDLER);
		DctmAttributeHandlers.setAttributeHandler(DctmObjectType.GROUP, DctmDataType.DF_STRING,
			DctmAttributes.GROUPS_NAMES, DctmAttributeHandlers.NO_IMPORT_HANDLER);
		DctmAttributeHandlers.setAttributeHandler(DctmObjectType.GROUP, DctmDataType.DF_STRING,
			DctmAttributes.GROUP_GLOBAL_UNIQUE_ID, DctmAttributeHandlers.NO_IMPORT_HANDLER);
		DctmAttributeHandlers.setAttributeHandler(DctmObjectType.GROUP, DctmDataType.DF_STRING,
			DctmAttributes.USERS_NAMES, new AttributeHandler() {
				@Override
				public boolean includeInImport(IDfPersistentObject object, CmfAttribute<IDfValue> attribute)
					throws DfException {
					return false;
				}

				@Override
				public Collection<IDfValue> getExportableValues(IDfPersistentObject object, IDfAttr attr)
					throws DfException {
					return DctmMappingUtils.substituteMappableUsers(object, attr);
				}

			});

		//
		// Type
		//
		DctmAttributeHandlers.setAttributeHandler(DctmObjectType.TYPE, DctmDataType.DF_STRING,
			DctmAttributes.ATTR_COUNT, DctmAttributeHandlers.NO_IMPORT_HANDLER);
		DctmAttributeHandlers.setAttributeHandler(DctmObjectType.TYPE, DctmDataType.DF_STRING,
			DctmAttributes.ATTR_COUNT, DctmAttributeHandlers.NO_IMPORT_HANDLER);
		DctmAttributeHandlers.setAttributeHandler(DctmObjectType.TYPE, DctmDataType.DF_STRING, DctmAttributes.START_POS,
			DctmAttributeHandlers.NO_IMPORT_HANDLER);
		DctmAttributeHandlers.setAttributeHandler(DctmObjectType.TYPE, DctmDataType.DF_STRING, DctmAttributes.NAME,
			DctmAttributeHandlers.NO_IMPORT_HANDLER);
		DctmAttributeHandlers.setAttributeHandler(DctmObjectType.TYPE, DctmDataType.DF_STRING,
			DctmAttributes.SUPER_NAME, DctmAttributeHandlers.NO_IMPORT_HANDLER);
		DctmAttributeHandlers.setAttributeHandler(DctmObjectType.TYPE, DctmDataType.DF_STRING, DctmAttributes.ATTR_NAME,
			DctmAttributeHandlers.NO_IMPORT_HANDLER);
		DctmAttributeHandlers.setAttributeHandler(DctmObjectType.TYPE, DctmDataType.DF_STRING, DctmAttributes.ATTR_TYPE,
			DctmAttributeHandlers.NO_IMPORT_HANDLER);
		DctmAttributeHandlers.setAttributeHandler(DctmObjectType.TYPE, DctmDataType.DF_STRING,
			DctmAttributes.ATTR_LENGTH, DctmAttributeHandlers.NO_IMPORT_HANDLER);
		DctmAttributeHandlers.setAttributeHandler(DctmObjectType.TYPE, DctmDataType.DF_STRING,
			DctmAttributes.ATTR_REPEATING, DctmAttributeHandlers.NO_IMPORT_HANDLER);

		//
		// User
		//
		DctmAttributeHandlers.setAttributeHandler(DctmObjectType.USER, DctmDataType.DF_STRING,
			DctmAttributes.USER_PASSWORD, DctmAttributeHandlers.NO_IMPORT_HANDLER);
		DctmAttributeHandlers.setAttributeHandler(DctmObjectType.USER, DctmDataType.DF_STRING,
			DctmAttributes.USER_LOGIN_DOMAIN, DctmAttributeHandlers.NO_IMPORT_HANDLER);
		DctmAttributeHandlers.setAttributeHandler(DctmObjectType.USER, DctmDataType.DF_STRING,
			DctmAttributes.USER_LOGIN_NAME, DctmAttributeHandlers.NO_IMPORT_HANDLER);
		DctmAttributeHandlers.setAttributeHandler(DctmObjectType.USER, DctmDataType.DF_STRING,
			DctmAttributes.HOME_DOCBASE, DctmAttributeHandlers.NO_IMPORT_HANDLER);

		// We avoid storing these because it'll be the job of other classes to link back
		// to the users to which they're related. This is CRITICAL to allow us to do a one-pass
		// import without having to circle back to resolve circular dependencies, or getting
		// ahead of ourselves in the object creation phase.

		// The default ACL will be linked back when the ACL's are imported.
		DctmAttributeHandlers.setAttributeHandler(DctmObjectType.USER, DctmDataType.DF_STRING,
			DctmAttributes.ACL_DOMAIN, DctmAttributeHandlers.USER_NAME_HANDLER);
		DctmAttributeHandlers.setAttributeHandler(DctmObjectType.USER, DctmDataType.DF_STRING, DctmAttributes.ACL_NAME,
			DctmAttributeHandlers.NO_IMPORT_HANDLER);

		// The default group will be linked back when the groups are imported
		DctmAttributeHandlers.setAttributeHandler(DctmObjectType.USER, DctmDataType.DF_STRING,
			DctmAttributes.USER_GROUP_NAME, DctmAttributeHandlers.NO_IMPORT_HANDLER);

		// The default folder will be linked back when the folders are imported
		DctmAttributeHandlers.setAttributeHandler(DctmObjectType.USER, DctmDataType.DF_STRING,
			DctmAttributes.DEFAULT_FOLDER, DctmAttributeHandlers.NO_IMPORT_HANDLER);

		// This will help intercept user names that need to be mapped to "dynamic" names on the
		// target DB, taken from the session config
		DctmAttributeHandlers.setAttributeHandler(DctmObjectType.USER, DctmDataType.DF_STRING, DctmAttributes.USER_NAME,
			DctmAttributeHandlers.USER_NAME_HANDLER);
	}

	void initOperatorNames(CfgTools cfg) {
		synchronized (DctmAttributeHandlers.class) {
			if (DctmAttributeHandlers.OPERATORS_INITIALIZED) { return; }
			String attrsToCheck = cfg.getString("owner.attributes", "");
			StringTokenizer strTokenizer = StringTokenizer.getCSVInstance(attrsToCheck);
			List<String> l = strTokenizer.getTokenList();
			for (String att : new HashSet<>(l)) {
				DctmAttributeHandlers.setAttributeHandler(null, DctmDataType.DF_STRING, att,
					DctmAttributeHandlers.SESSION_CONFIG_USER_HANDLER);
			}
			DctmAttributeHandlers.OPERATORS_INITIALIZED = true;
			DctmAttributeHandlers.class.notify();
		}
	}

	private DctmAttributeHandlers() {
	}

	private static Map<String, AttributeHandler> getAttributeHandlerMap(DctmObjectType objectType,
		DctmDataType dataType) {
		if (dataType == null) {
			throw new IllegalArgumentException("Must provide a data type to retrieve the interceptor for");
		}
		if (dataType == DctmDataType.DF_UNDEFINED) {
			throw new IllegalArgumentException("DF_UNDEFINED is not supported");
		}
		return (objectType == null ? DctmAttributeHandlers.GLOBAL.get(dataType)
			: DctmAttributeHandlers.PER_TYPE.get(objectType).get(dataType));
	}

	public static AttributeHandler removeAttributeHandler(DctmObjectType objectType, DctmDataType dataType,
		String attributeName) {
		return DctmAttributeHandlers.setAttributeHandler(objectType, dataType, attributeName, null);
	}

	public static AttributeHandler setAttributeHandler(DctmObjectType objectType, IDfAttr attribute,
		AttributeHandler interceptor) {
		if (attribute == null) { throw new IllegalArgumentException("Must provide an attribute to intercept"); }
		return DctmAttributeHandlers.setAttributeHandler(objectType, DctmDataType.fromAttribute(attribute),
			attribute.getName(), interceptor);
	}

	public static AttributeHandler setAttributeHandler(DctmObjectType objectType, DctmDataType dataType,
		String attributeName, AttributeHandler interceptor) {
		if (attributeName == null) { throw new IllegalArgumentException("Must provide an attribute to intercept"); }
		Map<String, AttributeHandler> m = DctmAttributeHandlers.getAttributeHandlerMap(objectType, dataType);
		return (interceptor != null ? m.put(attributeName, interceptor) : m.remove(attributeName));
	}

	public static AttributeHandler getAttributeHandler(DctmObjectType objectType, CmfAttribute<IDfValue> attribute) {
		if (attribute == null) { throw new IllegalArgumentException("Must provide an attribute to intercept"); }
		return DctmAttributeHandlers.getAttributeHandler(objectType, DctmTranslator.translateType(attribute.getType()),
			attribute.getName());
	}

	public static AttributeHandler getAttributeHandler(DctmObjectType objectType, IDfAttr attribute) {
		if (attribute == null) { throw new IllegalArgumentException("Must provide an attribute to intercept"); }
		final DctmDataType dataType = DctmDataType.fromAttribute(attribute);
		return DctmAttributeHandlers.getAttributeHandler(objectType, dataType, attribute.getName());
	}

	public static AttributeHandler getAttributeHandler(IDfPersistentObject object, IDfAttr attribute)
		throws DfException, UnsupportedDctmObjectTypeException {
		if (object == null) {
			throw new IllegalArgumentException("Must provide an object to identify the attribute handler for");
		}
		final DctmObjectType objectType = DctmObjectType.decodeType(object);
		return DctmAttributeHandlers.getAttributeHandler(objectType, DctmDataType.fromAttribute(attribute),
			attribute.getName());
	}

	public static AttributeHandler getAttributeHandler(DctmObjectType objectType, DctmDataType dataType,
		String attributeName) {
		if (attributeName == null) {
			throw new IllegalArgumentException("Must provide an attribute name to intercept");
		}
		AttributeHandler ret = DctmAttributeHandlers.getAttributeHandlerMap(objectType, dataType).get(attributeName);
		if (ret == null) {
			// Nothing, so try for the global one
			ret = DctmAttributeHandlers.getAttributeHandlerMap(null, dataType).get(attributeName);
		}
		return (ret == null ? DctmAttributeHandlers.DEFAULT_HANDLER : ret);
	}
}