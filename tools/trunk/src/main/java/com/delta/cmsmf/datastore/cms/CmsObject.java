/**
 *
 */

package com.delta.cmsmf.datastore.cms;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import com.delta.cmsmf.cmsobjects.DctmAttribute;
import com.delta.cmsmf.cmsobjects.DctmFormat;
import com.delta.cmsmf.cmsobjects.DctmGroup;
import com.delta.cmsmf.cmsobjects.DctmUser;
import com.delta.cmsmf.constants.CMSMFAppConstants;
import com.delta.cmsmf.constants.DctmAttrNameConstants;
import com.delta.cmsmf.datastore.DataAttribute;
import com.delta.cmsmf.datastore.DataProperty;
import com.delta.cmsmf.datastore.DataType;
import com.documentum.com.DfClientX;
import com.documentum.fc.client.IDfCollection;
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

	protected static enum AttributeMode {
		//
		COPY, // straight copy, no mod
		FILTER_TO_SQL, // apply a filter to the value on the way to SQL
		FILTER_TO_CMS, // apply a filter to the value on the way to CMS
		IGNORE, // ignore the value - do not export as an attribute
	}

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
			switch (getAttributeMode(name)) {
				case IGNORE:
					// Do nothing
					continue;

				case FILTER_TO_SQL:
					// Apply the outgoing filter
					this.attributes.put(name, getFilteredAttribute(false, object, attr));
					break;

				case COPY:
				case FILTER_TO_CMS: // this filter isn't applied here
				default:
					this.attributes.put(name, new DataAttribute(object, attr));
					break;
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
			this.properties.put(property.getName(), property);
		}
	}

	public final void saveToCMS(IDfSession session) throws DfException {
	}

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

	protected DataAttribute getFilteredAttribute(boolean toCms, IDfPersistentObject object, IDfAttr attribute)
		throws DfException {
		return new DataAttribute(object, attribute);
	}

	protected AttributeMode getAttributeMode(String attributeName) {
		return AttributeMode.COPY;
	}

	/**
	 * Sets all attributes of an object in CMS.
	 *
	 * @param object
	 *            the DFC persistent object in CMS
	 * @param updateVersionLabels
	 *            if true, version labels are updated of an object
	 * @param isUpdate
	 *            true if existing object is being updated
	 * @throws DfException
	 *             Signals that Dctm Server error has occurred.
	 */
	protected void setAllAttributesInCMS(T object, boolean updateVersionLabels, boolean isUpdate) throws DfException {
		if (this.logger.isEnabledFor(Level.INFO)) {
			this.logger.info("Started setting attributes of persistent object");
		}

		// read the attributes
		if (isUpdate) {
			// If an existing object is being updated, clear out all of its attributes that are not
			// part of attribute map
			// NOTE Only clear non internal and not system attributes
			// NOTE Do not clear group_name attribute for dm_group object
			// NOTE Do not clear home_docbase attribute for dm_user object
			// NOTE Do not clear user_name attribute for dm_user object
			// NOTE Do not clear name attribute for dm_format object
			Set<String> attributesBeingUpdated = getAttributeNames();
			final int attributeCount = object.getAttrCount();
			for (int i = 0; i < attributeCount; i++) {
				final IDfAttr attr = object.getAttr(i);
				final String name = attr.getName();

				// Skip clearing system and internal attributess
				// Skip clearing group_name attribute if dealing with group object
				// Skip clearing user_name attribute if dealing with user object
				// Skip clearing home_docbase attribute if dealing with user object
				// Skip clearing name attribute if dealing with format object
				if (!name.startsWith("r_") && !name.startsWith("i_")
					&& !(name.equals(DctmAttrNameConstants.GROUP_NAME) && (object instanceof DctmGroup))
					&& !(name.equals(DctmAttrNameConstants.USER_NAME) && (object instanceof DctmUser))
					&& !(name.equals(DctmAttrNameConstants.HOME_DOCBASE) && (object instanceof DctmUser))
					&& !(name.equals(DctmAttrNameConstants.NAME) && (object instanceof DctmFormat))
					&& !attributesBeingUpdated.contains(name)) {
					clearAttributeInCMS(object, attr);
				}
			}
		}

		if (updateVersionLabels) {
			// First remove version label attributes
			object.removeAll("r_version_label");
		}

		// Set attributes
		for (final DataAttribute attribute : getAllAttributes()) {
			// TODO check to see if we need to set any internal or system attributes of various
			// types
			final String name = attribute.getName();

			// for now ignore setting internal and system attributes
			boolean doSet = (!name.startsWith("r_") && !name.startsWith("i_"));
			// but process r_version_lebel
			doSet |= (name.equals("r_version_label") && updateVersionLabels);

			if (doSet) {
				setAttributeInCMS(object, attribute, isUpdate);
			}
		}

		if (this.logger.isEnabledFor(Level.INFO)) {
			this.logger.info("Finished setting attributes of persistent object.");
		}
	}

	/**
	 * Sets the attribute in cms.
	 *
	 * @param object
	 *            the DFC persistentObject for which the attribute is being updated
	 * @param attribute
	 *            the attribute
	 * @param isUpdate
	 *            true if an existing object is being updated
	 * @throws DfException
	 *             Signals that Dctm Server error has occurred.
	 * @see DctmAttribute
	 */
	protected void setAttributeInCMS(T object, DataAttribute attribute, boolean isUpdate) throws DfException {
		// If an existing object is being updated, first clear repeating values if the attribute
		// being set is repeating type.
		final String attrName = attribute.getName();
		if (isUpdate) {
			object.removeAll(attrName);
		}

		// TODO: String values that are in the repository operator name should be filtered
		/*
					String strVal = (String) dctmAttribute.getSingleValue();
					if (strVal.equals(CMSMFAppConstants.DM_DBO)
						&& RunTimeProperties.getRunTimePropertiesInstance().getAttrsToCheckForRepoOperatorName()
						.contains(attrName)) {
						strVal = RunTimeProperties.getRunTimePropertiesInstance().getTargetRepoOperatorName(session);
						if (this.logger.isEnabledFor(Level.INFO)) {
							this.logger.info("Updated " + attrName
								+ " attribute of object to repository operator name.");
						}
					}
					object.setString(attrName, strVal);

		 */
		final IDfAttr attr = object.getAttr(object.findAttrIndex(attrName));
		for (IDfValue value : attribute) {
			clearAttributeInCMS(object, attr);
			if (attribute.isRepeating()) {
				object.appendValue(attrName, value);
			} else {
				// I wonder if appendValue() can also be used for non-repeating attributes
				object.setValue(attrName, value);
			}
		}
	}

	/**
	 * Clears the value of an attribute in cms.
	 *
	 * @param object
	 *            the persistent object
	 * @param attr
	 *            the attribute
	 * @throws DfException
	 *             the DfException
	 */
	protected void clearAttributeInCMS(T object, IDfAttr attr) throws DfException {
		final String attrName = attr.getName();
		if (attr.isRepeating()) {
			object.removeAll(attrName);
		} else {
			// I wonder what happens if we apply removeAll() to non-repeating attributes
			// object.removeAll(attrName);
			DataType dataType = DataType.fromDfConstant(attr.getDataType());
			object.setValue(attrName, dataType.getClearingValue());
		}
	}

	/**
	 * Clears the value of an attribute in cms.
	 *
	 * @param object
	 *            the persistent object
	 * @param attr
	 *            the attribute
	 * @throws DfException
	 *             the DfException
	 */
	protected void clearAttributeInCMS(T object, String attr) throws DfException {
		clearAttributeInCMS(object, object.getAttr(object.findAttrIndex(attr)));
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
		String objType = object.getType().getName();
		IDfValue modifyDate = getAttribute(DctmAttrNameConstants.R_MODIFY_DATE).getSingleValue();
		IDfValue vStamp = getAttribute(DctmAttrNameConstants.I_VSTAMP).getSingleValue();

		final String sqlStr = String.format(
			"UPDATE %s_s SET r_modify_date = TO_DATE(''%s'', ''%s''), i_vstamp = %d WHERE r_object_id = ''%s''",
			objType, modifyDate.asTime().asString(CMSMFAppConstants.DCTM_DATETIME_PATTERN),
			CMSMFAppConstants.DCTM_DATETIME_PATTERN, vStamp.asInteger(), object.getObjectId().getId());
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
	protected final void updateVStamp(T obj, int vStamp) throws DfException {
		updateVStamp(obj, String.format("%s_s", obj.getType().getName()), vStamp);
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
	protected final void updateVStamp(T obj, String tableName, int vStamp) throws DfException {
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