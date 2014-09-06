/**
 *
 */

package com.delta.cmsmf.datastore.cms;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import com.armedia.commons.utilities.Tools;
import com.delta.cmsmf.constants.CMSMFAppConstants;
import com.delta.cmsmf.constants.DctmAttrNameConstants;
import com.delta.cmsmf.datastore.DataAttribute;
import com.delta.cmsmf.datastore.DataProperty;
import com.delta.cmsmf.datastore.DataStore;
import com.delta.cmsmf.datastore.DataType;
import com.delta.cmsmf.datastore.cms.CmsAttributeHandlers.AttributeHandler;
import com.delta.cmsmf.datastore.cms.CmsCounter.Result;
import com.delta.cmsmf.exception.CMSMFException;
import com.documentum.com.DfClientX;
import com.documentum.fc.client.IDfCollection;
import com.documentum.fc.client.IDfLocalTransaction;
import com.documentum.fc.client.IDfPersistentObject;
import com.documentum.fc.client.IDfQuery;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.common.DfException;
import com.documentum.fc.common.IDfAttr;
import com.documentum.fc.common.IDfValue;

/**
 * @author diego
 *
 */
public abstract class CmsObject<T extends IDfPersistentObject> {

	protected final Logger logger = Logger.getLogger(getClass());

	private final CmsObjectType type;
	private final Class<T> dfClass;

	private String id = null;
	private final Map<String, DataAttribute> attributes = new HashMap<String, DataAttribute>();
	private final Map<String, DataProperty> properties = new HashMap<String, DataProperty>();
	private String contentPath = null;

	protected CmsObject(CmsObjectType type, Class<T> dfClass) {
		if (type == null) { throw new IllegalArgumentException("Must provide an object type"); }
		if (dfClass == null) { throw new IllegalArgumentException("Must provde a DF class"); }
		if (type.getDfClass() != dfClass) { throw new IllegalArgumentException(String.format(
			"Class mismatch: type is tied to class [%s], but was given class [%s]", type.getDfClass()
				.getCanonicalName(), dfClass.getCanonicalName())); }
		this.type = type;
		this.dfClass = dfClass;
	}

	public final CmsObjectType getType() {
		return this.type;
	}

	public final Class<T> getDfClass() {
		return this.dfClass;
	}

	public final int getAttributeCount() {
		return this.attributes.size();
	}

	public final Set<String> getAttributeNames() {
		return Collections.unmodifiableSet(this.attributes.keySet());
	}

	public final DataAttribute getAttribute(String name) {
		if (name == null) { throw new IllegalArgumentException("Must provide an attribute name to retrieve"); }
		return this.attributes.get(name);
	}

	public final DataAttribute setAttribute(DataAttribute attribute) {
		if (attribute == null) { throw new IllegalArgumentException("Must provide an attribute to set"); }
		return this.attributes.put(attribute.getName(), attribute);
	}

	public final DataAttribute removeAttribute(String name) {
		if (name == null) { throw new IllegalArgumentException("Must provide an attribute name to remove"); }
		return this.attributes.remove(name);
	}

	public final Collection<DataAttribute> getAllAttributes() {
		return Collections.unmodifiableCollection(this.attributes.values());
	}

	public final int getPropertyCount() {
		return this.properties.size();
	}

	public final Set<String> getPropertyNames() {
		return Collections.unmodifiableSet(this.properties.keySet());
	}

	public final DataProperty getProperty(String name) {
		if (name == null) { throw new IllegalArgumentException("Must provide a property name to retrieve"); }
		return this.properties.get(name);
	}

	public final DataProperty setProperty(DataProperty property) {
		if (property == null) { throw new IllegalArgumentException("Must provide a property to set"); }
		return this.properties.put(property.getName(), property);
	}

	public final DataProperty removeProperty(String name) {
		if (name == null) { throw new IllegalArgumentException("Must provide a property name to remove"); }
		return this.properties.remove(name);
	}

	public final Collection<DataProperty> getAllProperties() {
		return Collections.unmodifiableCollection(this.properties.values());
	}

	public final void loadFromCMS(T object) throws DfException {
		if (object == null) { throw new IllegalArgumentException("Must provide an object to populate from"); }
		CmsObjectType type = CmsObjectType.decodeType(object);
		if (type != this.type) { throw new IllegalArgumentException(String.format(
			"Expected an object of type %s, but got one of type %s", this.type, type)); }
		if (!this.dfClass.isAssignableFrom(object.getClass())) { throw new IllegalArgumentException(String.format(
			"Expected an object of class %s, but got one of class %s", this.dfClass.getCanonicalName(), object
				.getClass().getCanonicalName())); }

		this.id = object.getObjectId().getId();
		this.contentPath = calculateContentPath(object);

		// First, the attributes
		this.attributes.clear();
		final int attCount = object.getAttrCount();
		for (int i = 0; i < attCount; i++) {
			final IDfAttr attr = object.getAttr(i);
			final String name = attr.getName();
			AttributeHandler handler = CmsAttributeHandlers.getAttributeHandler(object, attr);
			// Get the attribute handler
			if (handler.includeInExport(object, attr)) {
				DataAttribute attribute = new DataAttribute(attr, handler.getExportableValues(object, attr));
				this.attributes.put(name, attribute);
			}
		}

		// Properties are different from attributes in that they require special handling. For
		// instance, a property would only be settable via direct SQL, or via an explicit method
		// call, etc., because setting it directly as an attribute would result in an error from
		// DFC, and therefore specialized code is required to handle it
		this.properties.clear();
		List<DataProperty> properties = new ArrayList<DataProperty>();
		getDataProperties(properties, object);
		for (DataProperty property : properties) {
			// This mechanism overwrites properties, and intentionally so
			this.properties.put(property.getName(), property);
		}
	}

	public final Result saveToCMS(IDfSession session) throws DfException, CMSMFException, SQLException {
		if (session == null) { throw new IllegalArgumentException("Must provide a session to save the object"); }
		CmsCounter.incrementCounter(this, Result.READ);
		boolean transOpen = false;
		boolean ok = false;

		// We assume the worst, out of the gate
		IDfLocalTransaction localTx = null;
		try {
			if (session.isTransactionActive()) {
				localTx = session.beginTransEx();
			} else {
				session.beginTrans();
			}
			transOpen = true;
			if (skipImport(session)) { return Result.SKIPPED; }

			Result result = Result.SKIPPED;

			T object = locateInCms(session);
			final boolean isUpdate = (object != null);
			final boolean updateVersionLabels = isVersionable(object);
			if (object == null) {
				// Create a new object
				object = newObject(session);
				result = Result.CREATED;
			} else {
				if (!isSameObject(object)) { return Result.SKIPPED; }
				result = Result.UPDATED;
			}
			DataStore.setIdMapping(this.id, object.getObjectId().getId());

			prepareForConstruction(object, !isUpdate);

			if (isUpdate) {
				// If an existing object is being updated, clear out all of its attributes that are
				// not part of our attribute set
				// NOTE Only clear non internal and non system attributes
				Set<String> attributesBeingUpdated = getAttributeNames();
				final int attributeCount = object.getAttrCount();
				for (int i = 0; i < attributeCount; i++) {
					final IDfAttr attr = object.getAttr(i);
					final String name = attr.getName();
					if (!name.startsWith("r_") && !name.startsWith("i_") && !attributesBeingUpdated.contains(name)) {
						clearAttributeFromObject(name, object);
					}
				}
			}

			// We remove the version labels as well
			if (updateVersionLabels) {
				object.removeAll(DctmAttrNameConstants.R_VERSION_LABEL);
			}

			// Set "default" attributes
			for (DataAttribute attribute : getAllAttributes()) {
				// TODO check to see if we need to set any internal or system attributes of various
				// types
				final String name = attribute.getName();
				final AttributeHandler handler = getAttributeHandler(attribute);

				// for now ignore setting internal and system attributes
				boolean doSet = (!name.startsWith("r_") && !name.startsWith("i_"));
				// but process r_version_lebel
				doSet |= (name.equals(DctmAttrNameConstants.R_VERSION_LABEL) && updateVersionLabels);
				// allow for a last-minute interception...
				doSet &= handler.includeInImport(object, attribute);

				if (doSet) {
					copyAttributeToObject(attribute, object);
				}
			}

			finalizeConstruction(object, !isUpdate);
			updateModifyDate(object);
			ok = true;
			return result;
		} finally {
			if (transOpen) {
				if (ok) {
					if (localTx != null) {
						session.commitTransEx(localTx);
					} else {
						session.commitTrans();
					}
				} else {
					// Clear the mapping
					DataStore.clearIdMapping(this.id);
					if (localTx != null) {
						session.abortTransEx(localTx);
					} else {
						session.abortTrans();
					}
				}
			}
		}
	}

	protected final AttributeHandler getAttributeHandler(IDfAttr attr) {
		return CmsAttributeHandlers.getAttributeHandler(this.type, attr);
	}

	protected final AttributeHandler getAttributeHandler(DataAttribute attr) {
		return CmsAttributeHandlers.getAttributeHandler(this.type, attr);
	}

	protected final AttributeHandler getAttributeHandler(DataType dataType, String name) {
		return CmsAttributeHandlers.getAttributeHandler(this.type, dataType, name);
	}

	protected boolean isVersionable(T object) throws DfException {
		return false;
	}

	protected boolean skipImport(IDfSession session) throws DfException {
		return false;
	}

	protected boolean isSameObject(T object) throws DfException {
		DataAttribute thisDate = getAttribute(DctmAttrNameConstants.R_MODIFY_DATE);
		if (thisDate == null) { return false; }
		IDfValue objectDate = object.getValue(DctmAttrNameConstants.R_MODIFY_DATE);
		if (objectDate == null) { return false; }
		DataType type = thisDate.getType(); // Should be DF_TIME, but this is the safe way to do it
		return Tools.equals(type.getValue(objectDate), type.getValue(thisDate.getValue()));
	}

	protected final T newObject(IDfSession session) throws DfException {
		return castObject(session.newObject(this.type.getDocumentumType()));
	}

	protected final T castObject(IDfPersistentObject object) throws DfException {
		if (object == null) { return null; }
		if (!this.dfClass.isAssignableFrom(object.getClass())) { throw new DfException(String.format(
			"Expected an object of class %s, but got one of class %s", this.dfClass.getCanonicalName(), object
				.getClass().getCanonicalName())); }
		return this.dfClass.cast(object);
	}

	protected abstract T locateInCms(IDfSession session) throws DfException;

	public final String getId() {
		return this.id;
	}

	public final String getContentPath() {
		return this.contentPath;
	}

	protected String calculateContentPath(T object) {
		return null;
	}

	protected void getDataProperties(Collection<DataProperty> properties, T object) throws DfException {
	}

	/**
	 * <p>
	 * Apply specific processing to the given object prior to the automatically-copied attributes
	 * having been applied to it. That means that the object may still have its old attributes, and
	 * thus only this instance's {@link DataAttribute} values should be trusted. The
	 * {@code newObject} parameter indicates if this object was newly-created, or if it is an
	 * already-existing object.
	 * </p>
	 *
	 * @param object
	 * @throws DfException
	 */
	protected void prepareForConstruction(T object, boolean newObject) throws DfException {
	}

	/**
	 * <p>
	 * Apply specific processing to the given object after the automatically-copied attributes have
	 * been applied to it. That means that the object should now reflect its intended new state,
	 * pending only whatever changes this method will apply.
	 * </p>
	 *
	 * @param object
	 * @throws DfException
	 */
	protected void finalizeConstruction(T object, boolean newObject) throws DfException {
	}

	protected final boolean copyAttributeToObject(String attrName, T object) throws DfException {
		return copyAttributeToObject(getAttribute(attrName), object);
	}

	protected final boolean copyAttributeToObject(DataAttribute attribute, T object) throws DfException {
		if (object == null) { throw new IllegalArgumentException("Must provide an object to set the attributes to"); }
		if (attribute == null) { throw new IllegalArgumentException("Must provide an attribute to set on the object"); }
		final AttributeHandler handler = getAttributeHandler(attribute);
		return setAttributeOnObject(attribute, handler.getImportableValues(object, attribute), object);
	}

	protected final boolean setAttributeOnObject(String attrName, IDfValue value, T object) throws DfException {
		return setAttributeOnObject(attrName, Collections.singleton(value), object);
	}

	protected final boolean setAttributeOnObject(String attrName, Collection<IDfValue> values, T object)
		throws DfException {
		if (object == null) { throw new IllegalArgumentException("Must provide an object to set the attributes to"); }
		if (attrName == null) { throw new IllegalArgumentException(
			"Must provide an attribute name to set on the object"); }
		DataAttribute dataAttr = getAttribute(attrName);
		if (dataAttr != null) { return setAttributeOnObject(dataAttr, values, object); }

		int idx = object.findAttrIndex(attrName);
		IDfAttr attribute = object.getAttr(idx);
		DataType dataType = DataType.fromAttribute(attribute);
		return setAttributeOnObject(attrName, dataType, attribute.isRepeating(), values, object);
	}

	protected final boolean setAttributeOnObject(DataAttribute attribute, IDfValue value, T object) throws DfException {
		return setAttributeOnObject(attribute, Collections.singleton(value), object);
	}

	protected final boolean setAttributeOnObject(DataAttribute attribute, Collection<IDfValue> values, T object)
		throws DfException {
		if (object == null) { throw new IllegalArgumentException("Must provide an object to set the attributes to"); }
		if (attribute == null) { throw new IllegalArgumentException("Must provide an attribute to set on the object"); }
		return setAttributeOnObject(attribute.getName(), attribute.getType(), attribute.isRepeating(), values, object);
	}

	private final boolean setAttributeOnObject(String attrName, DataType dataType, boolean repeating,
		Collection<IDfValue> values, T object) throws DfException {
		if (object == null) { throw new IllegalArgumentException("Must provide an object to set the attributes to"); }
		if (attrName == null) { throw new IllegalArgumentException(
			"Must provide an attribute name to set on the object"); }
		if (values == null) {
			values = Collections.emptyList();
		}
		// If an existing object is being updated, first clear repeating values if the attribute
		// being set is repeating type.
		clearAttributeFromObject(attrName, object);
		for (IDfValue value : values) {
			if (value == null) {
				value = dataType.getNullValue();
			}
			if (repeating) {
				object.appendValue(attrName, value);
			} else {
				// I wonder if appendValue() can also be used for non-repeating attributes...
				object.setValue(attrName, value);
			}
		}
		return true;
	}

	protected final void clearAttributeFromObject(String attr, T object) throws DfException {
		if (object == null) { throw new IllegalArgumentException("Must provide an object to clear the attribute from"); }
		DataAttribute dataAttr = getAttribute(attr);
		if (dataAttr != null) {
			clearAttributeFromObject(dataAttr, object);
		} else {
			clearAttributeFromObject(object.getAttr(object.findAttrIndex(attr)), object);
		}
	}

	protected final void clearAttributeFromObject(DataAttribute attribute, T object) throws DfException {
		if (attribute == null) { throw new IllegalArgumentException(
			"Must provide an attribute to clear from the object"); }
		clearAttributeFromObject(attribute.getName(), attribute.getType(), attribute.isRepeating(), object);
	}

	protected final void clearAttributeFromObject(IDfAttr attribute, T object) throws DfException {
		if (attribute == null) { throw new IllegalArgumentException(
			"Must provide an attribute to clear from the object"); }
		clearAttributeFromObject(attribute.getName(), DataType.fromAttribute(attribute), attribute.isRepeating(),
			object);
	}

	protected final void clearAttributeFromObject(String attrName, DataType dataType, boolean repeating, T object)
		throws DfException {
		if (object == null) { throw new IllegalArgumentException("Must provide an object to clear the attribute from"); }
		if (attrName == null) { throw new IllegalArgumentException(
			"Must provide an attribute name to clear from object"); }
		if (repeating) {
			object.removeAll(attrName);
		} else {
			if (dataType == null) { throw new IllegalArgumentException(
				"Must provide the data type for the attribute being cleared"); }
			object.setValue(attrName, dataType.getNullValue());
		}
	}

	/**
	 * Updates modify date attribute of an persistent object using execsql.
	 *
	 * @param object
	 *            the DFC persistentObject representing an object in repository
	 * @throws DfException
	 *             Signals that Dctm Server error has occurred.
	 */
	protected final void updateModifyDate(T object) throws DfException {
		final String objType = object.getType().getName();
		final IDfValue modifyDate = getAttribute(DctmAttrNameConstants.R_MODIFY_DATE).getValue();
		/*
		final IDfValue vStamp = getAttribute(DctmAttrNameConstants.I_VSTAMP).getSingleValue();
		final String sqlStr = String.format(
			"UPDATE %s_s SET r_modify_date = TO_DATE(''%s'', ''%s''), i_vstamp = %d WHERE r_object_id = ''%s''",
			objType, modifyDate.asTime().asString(CMSMFAppConstants.DCTM_DATETIME_PATTERN),
			CMSMFAppConstants.DCTM_DATETIME_PATTERN, vStamp.asInteger(), object.getObjectId().getId());
		 */

		// TODO: For now we don't touch the i_vstamp b/c we don't think it necessary
		final String sqlStr = String.format(
			"UPDATE %s_s SET r_modify_date = TO_DATE(''%s'', ''%s'') WHERE r_object_id = ''%s''", objType, modifyDate
			.asTime().asString(CMSMFAppConstants.DCTM_DATETIME_PATTERN), CMSMFAppConstants.DCTM_DATETIME_PATTERN,
			object.getObjectId().getId());

		runExecSQL(object.getSession(), sqlStr);
	}

	/**
	 * Updates vStamp attribute of an persistent object using execsql.
	 *
	 * @param obj
	 *            the DFC persistentObject representing an object in repository
	 * @throws DfException
	 *             Signals that Dctm Server error has occurred.
	 */
	protected final void updateVStamp(int vStamp, T obj) throws DfException {
		updateVStamp(String.format("%s_s", obj.getType().getName()), vStamp, obj);
	}

	/**
	 * Updates vStamp attribute of an persistent object using execsql.
	 *
	 * @param obj
	 *            the DFC persistentObject representing an object in repository
	 * @param tableName
	 *            the table name to be used in update clause
	 * @throws DfException
	 *             Signals that Dctm Server error has occurred.
	 */
	protected final void updateVStamp(String tableName, int vStamp, T obj) throws DfException {
		final String objId = obj.getObjectId().getId();
		runExecSQL(obj.getSession(),
			String.format("UPDATE %s SET i_vstamp = %d WHERE r_object_id = ''%s''", tableName, vStamp, objId));
	}

	/**
	 * Runs execsql query that can be used to update various system/internal attributes.
	 *
	 * @param session
	 *            the repository session
	 * @param sqlQueryString
	 *            the sql query string
	 * @throws DfException
	 *             Signals that Dctm Server error has occurred.
	 */
	private final void runExecSQL(IDfSession session, String sql) throws DfException {
		IDfQuery dqlQry = new DfClientX().getQuery();
		dqlQry.setDQL(String.format("EXECUTE exec_sql WITH query='%s'", sql));
		IDfCollection resultCol = dqlQry.execute(session, IDfQuery.EXEC_QUERY);
		try {
			if (resultCol.next()) {
				final IDfValue ret = resultCol.getValueAt(0);
				closeQuietly(resultCol);
				final String outcome;
				if (ret.toString().equalsIgnoreCase("F")) {
					outcome = "rollback";
				} else {
					outcome = "commit";
				}
				dqlQry.setDQL(String.format("EXECUTE exec_sql with query='%s';", outcome));
				resultCol = dqlQry.execute(session, IDfQuery.EXEC_QUERY);
			}
		} finally {
			closeQuietly(resultCol);
		}
	}

	protected final void closeQuietly(IDfCollection c) {
		if (c == null) { return; }
		try {
			c.close();
		} catch (DfException e) {
			// quietly swallowed
			if (this.logger.isTraceEnabled()) {
				this.logger.trace("Swallowing exception on close", e);
			}
		}
	}
}