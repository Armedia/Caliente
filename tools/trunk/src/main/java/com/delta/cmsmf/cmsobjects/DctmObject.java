package com.delta.cmsmf.cmsobjects;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import com.delta.cmsmf.cms.CmsObjectType;
import com.delta.cmsmf.constants.CMSMFAppConstants;
import com.delta.cmsmf.constants.DctmAttrNameConstants;
import com.delta.cmsmf.datastore.DataAttribute;
import com.delta.cmsmf.datastore.DataObject;
import com.delta.cmsmf.exception.CMSMFException;
import com.delta.cmsmf.mainEngine.RepositoryConfiguration;
import com.delta.cmsmf.runtime.RunTimeProperties;
import com.documentum.com.DfClientX;
import com.documentum.fc.client.IDfCollection;
import com.documentum.fc.client.IDfPersistentObject;
import com.documentum.fc.client.IDfQuery;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.client.IDfSysObject;
import com.documentum.fc.common.DfException;
import com.documentum.fc.common.DfId;
import com.documentum.fc.common.DfTime;
import com.documentum.fc.common.IDfAttr;
import com.documentum.fc.common.IDfId;
import com.documentum.fc.common.IDfTime;

/**
 * The DctmObject class is an abstract base class for other object types.
 *
 * This class is equivalent to IDfPersistentObject of DFC. This class contains 2 abstract methods
 * that has to be implemented by all extending classes. The method getFromCMS() retrieves an object
 * from the repository and method createInCMS() creates a object in the repository.
 * <p>
 * This class contains attributes map that contains attribute name, value pair. Attribute value can
 * be a single type or repeating type. This class also contains several methods to get attributes
 * from repository and set attributes in the repository. It also contains several utility methods to
 * get attribute values from attributes map.
 *
 * @author Shridev Makim 6/15/2010
 */
public abstract class DctmObject<T extends IDfPersistentObject> implements Serializable {

	/** The Constant serialVersionUID. */
	private static final long serialVersionUID = 1L;

	/** The logger object used for logging. */
	protected static final Logger logger = Logger.getLogger(DctmObject.class);

	/**
	 * The dctm object type enumeration.
	 * This type will be set by individual object types during initialization
	 * within the class constructors.
	 *
	 * @see CmsObjectType
	 */
	private final DctmObjectType dctmObjectType;

	private final Class<T> dctmObjectClass;

	/** The attributes map. */
	private final Map<String, DctmAttribute> attrMap = new HashMap<String, DctmAttribute>();

	/** The object id of the object in source repository. It is populated during the export step. */
	private String srcObjectID;

	private transient DataObject dataObject;

	/**
	 * Instantiates a new dctm object.
	 */
	public DctmObject(DctmObjectType type, Class<T> dctmObjectClass) {
		this.dctmObjectType = type;
		this.dctmObjectClass = dctmObjectClass;
	}

	public final DctmObjectType getObjectType() {
		return this.dctmObjectType;
	}

	public final void loadFrom(DataObject dataObject) {
		if (dataObject == null) { throw new IllegalArgumentException(); }
		if (dataObject.getType() != this.dctmObjectType) { throw new IllegalArgumentException(String.format(
			"Expected a DataObject of type %s but got type %s", this.dctmObjectType, dataObject.getType())); }
		this.dataObject = dataObject;
		this.srcObjectID = dataObject.getId();
		for (DataAttribute attribute : dataObject.getAttributes()) {
			this.attrMap.put(attribute.getName(), new DctmAttribute(attribute));
		}
	}

	protected void doLoadFrom(DataObject dataObject) {
		// Do any additional work
	}

	/**
	 * Gets the attributes map.
	 *
	 * @return the attributes map
	 */
	public Map<String, DctmAttribute> getAttrMap() {
		return this.attrMap;
	}

	/**
	 * Adds the attribute to the attributes map.
	 *
	 * @param attrName
	 *            the attribute name
	 * @param dctmAttr
	 *            the instance of DctmAttribute class representing an attribute in repository.
	 * @see DctmAttribute
	 */
	public void addAttribute(String attrName, DctmAttribute dctmAttr) {
		this.attrMap.put(attrName, dctmAttr);
	}

	/**
	 * Finds attribute by name from attributes map.
	 *
	 * @param attrName
	 *            the attr name
	 * @return the CMSMF DctmAttribute that stores attribute information and value.
	 * @see DctmAttribute
	 */
	public DctmAttribute findAttribute(String attrName) {
		DctmAttribute returnAttribute = null;
		returnAttribute = this.attrMap.get(attrName);
		return returnAttribute;
	}

	/**
	 * Checks if an attribute exist in the attributes map.
	 *
	 * @param attrName
	 *            the attribute name
	 * @return true, if attributes map contains the attribute
	 */
	public boolean doesAttributeExist(String attrName) {
		return this.attrMap.containsKey(attrName);
	}

	/**
	 * Removes the attribute from the attributes map.
	 *
	 * @param attrName
	 *            the attribute name
	 */
	public void removeAttribute(String attrName) {
		this.attrMap.remove(attrName);
	}

	/**
	 * Removes the repeating attribute value for an attribute.
	 *
	 * @param attrName
	 *            the attribute name
	 * @param attrValue
	 *            the attribute value that needs to be removed
	 * @return true, if successful
	 */
	public boolean removeRepeatingAttrValue(String attrName, String attrValue) {
		DctmAttribute repeatingAttr = this.attrMap.get(attrName);
		return repeatingAttr.removeRepeatingAttrValue(attrValue);
	}

	/**
	 * Gets the value of single value type attribute, whose data type is String.
	 *
	 * @param attrName
	 *            the attribute name
	 * @return the string value of the attribute
	 */
	public String getStrSingleAttrValue(String attrName) {
		String returnVal = "";
		DctmAttribute singleAttr = this.attrMap.get(attrName);
		if (singleAttr != null) {
			returnVal = singleAttr.getSingleValue().toString();
		}
		return returnVal;
	}

	/**
	 * Gets the value of single value type attribute, whose data type is Integer.
	 *
	 * @param attrName
	 *            the attribute name
	 * @return the int value of the attribute
	 */
	public int getIntSingleAttrValue(String attrName) {
		int returnVal = 0;
		DctmAttribute singleAttr = this.attrMap.get(attrName);
		if (singleAttr != null) {
			returnVal = (Integer) singleAttr.getSingleValue();
		}
		return returnVal;
	}

	/**
	 * Gets the value of single value type attribute, whose data type is Boolean.
	 *
	 * @param attrName
	 *            the attribute name
	 * @return the boolean value of the attribute
	 */
	public boolean getBoolSingleAttrValue(String attrName) {
		boolean returnVal = false;
		DctmAttribute singleAttr = this.attrMap.get(attrName);
		if (singleAttr != null) {
			returnVal = (Boolean) singleAttr.getSingleValue();
		}
		return returnVal;
	}

	/**
	 * Gets the value of single value type attribute, whose data type is Date/Time.
	 *
	 * @param attrName
	 *            the attribute name
	 * @return the date value of the attribute
	 */
	public Date getDateSingleAttrValue(String attrName) {
		DctmAttribute singleAttr = this.attrMap.get(attrName);
		return (Date) singleAttr.getSingleValue();
	}

	/**
	 * Gets the source repository object id.
	 *
	 * @return the source repository object id
	 */
	public String getSrcObjectID() {
		return this.srcObjectID;
	}

	/**
	 * Sets the source repository object id.
	 *
	 * @param srcObjectID
	 *            the source repository object id
	 */
	public void setSrcObjectID(String srcObjectID) {
		this.srcObjectID = srcObjectID;
	}

	/**
	 * Creates the object in target repository.
	 *
	 * @throws DfException
	 *             Signals that Dctm Server error has occurred.
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	public abstract void createInCMS(IDfSession session) throws DfException, IOException;

	/**
	 * Updates system attributes of an object using execsql. This method is used to
	 * update various system and internal attributes of an object during the import step.
	 *
	 * @param sysObject
	 *            the DFC sysObject representing an object in repository.
	 * @param dctmObj
	 *            the CMSMF DctmObject that has internal/system attributes.
	 * @throws DfException
	 *             Signals that Dctm Server error has occurred.
	 */
	protected void updateSystemAttributes(IDfSysObject sysObject, DctmObject<T> dctmObj) throws DfException {
		IDfSession session = sysObject.getSession();
		if (DctmObject.logger.isEnabledFor(Level.INFO)) {
			DctmObject.logger.info("Started updating system attributes of object with name: "
				+ sysObject.getObjectName());
		}
		// prepare sql to be executed
		IDfTime modifyDate = new DfTime(dctmObj.getDateSingleAttrValue(DctmAttrNameConstants.R_MODIFY_DATE));
		IDfTime creationDate = new DfTime(dctmObj.getDateSingleAttrValue(DctmAttrNameConstants.R_CREATION_DATE));
		int vStamp = dctmObj.getIntSingleAttrValue(DctmAttrNameConstants.I_VSTAMP);

		// if a value in acl_domain or r_creator_name or r_modifier attributes
		// of the system object is "dm_dbo", change it to target repository operator name
		String aclDomain = dctmObj.getStrSingleAttrValue(DctmAttrNameConstants.ACL_DOMAIN);
		String creatorName = dctmObj.getStrSingleAttrValue(DctmAttrNameConstants.R_CREATOR_NAME);
		// If creator name contains single quote in its name, to escape it, replace it with 4 single
		// quotes.
		creatorName = creatorName.replaceAll("'", "''''");
		String modifier = dctmObj.getStrSingleAttrValue(DctmAttrNameConstants.R_MODIFIER);
		// If modifier contains single quote in its name, to escape it, replace it with 4 single
		// quotes.
		modifier = modifier.replaceAll("'", "''''");
		if (aclDomain.equals(CMSMFAppConstants.DM_DBO) || creatorName.equals(CMSMFAppConstants.DM_DBO)
			|| modifier.equals(CMSMFAppConstants.DM_DBO)) {
			String targetRepoOperatorName = RunTimeProperties.getRunTimePropertiesInstance().getTargetRepoOperatorName(
				session);
			if (aclDomain.equals(CMSMFAppConstants.DM_DBO)) {
				aclDomain = targetRepoOperatorName;
			}
			if (creatorName.equals(CMSMFAppConstants.DM_DBO)) {
				creatorName = targetRepoOperatorName;
			}
			if (modifier.equals(CMSMFAppConstants.DM_DBO)) {
				modifier = targetRepoOperatorName;
			}
		}

		String sqlStr = "UPDATE dm_sysobject_s " + "SET r_modify_date = TO_DATE(''"
			+ modifyDate.asString(CMSMFAppConstants.DCTM_DATETIME_PATTERN) + "'', ''"
			+ CMSMFAppConstants.ORACLE_DATETIME_PATTERN + "''), r_creation_date = TO_DATE(''"
			+ creationDate.asString(CMSMFAppConstants.DCTM_DATETIME_PATTERN) + "'', ''"
			+ CMSMFAppConstants.ORACLE_DATETIME_PATTERN + "''), r_creator_name = ''" + creatorName
			+ "'', r_modifier = ''" + modifier + "'', i_vstamp = " + vStamp + ", acl_name = ''"
			+ dctmObj.getStrSingleAttrValue(DctmAttrNameConstants.ACL_NAME) + "'', acl_domain = ''" + aclDomain
			+ "'' WHERE r_object_id = ''" + sysObject.getObjectId().getId() + "''";

		DctmObject.runExecSQL(sysObject.getSession(), sqlStr);

		if (DctmObject.logger.isEnabledFor(Level.INFO)) {
			DctmObject.logger.info("Finished updating system attributes of object with name: "
				+ sysObject.getObjectName());
		}
	}

	/**
	 * Updates modify date attribute of an persistent object using execsql.
	 *
	 * @param prsstntObject
	 *            the DFC persistentObject representing an object in repository
	 * @param dctmObj
	 *            the CMSMF DctmObject that has internal/system attributes.
	 * @throws DfException
	 *             Signals that Dctm Server error has occurred.
	 */
	protected void updateModifyDate(T prsstntObject, DctmObject<T> dctmObj) throws DfException {
		if (DctmObject.logger.isEnabledFor(Level.INFO)) {
			DctmObject.logger.info("Started updating modify date of object");
		}

		String objType = prsstntObject.getType().getName();
		// prepare sql to be executed
		IDfTime modifyDate = new DfTime(dctmObj.getDateSingleAttrValue(DctmAttrNameConstants.R_MODIFY_DATE));
		int vStamp = dctmObj.getIntSingleAttrValue(DctmAttrNameConstants.I_VSTAMP);

		String sqlStr = "UPDATE " + objType + "_s " + "SET r_modify_date = TO_DATE(''"
			+ modifyDate.asString(CMSMFAppConstants.DCTM_DATETIME_PATTERN) + "'', ''"
			+ CMSMFAppConstants.ORACLE_DATETIME_PATTERN + "''), i_vstamp = " + vStamp + " WHERE r_object_id = ''"
			+ prsstntObject.getObjectId().getId() + "''";

		DctmObject.runExecSQL(prsstntObject.getSession(), sqlStr);

		if (DctmObject.logger.isEnabledFor(Level.INFO)) {
			DctmObject.logger.info("Finished updating modify date of object");
		}
	}

	/**
	 * Update set_file, set_client and set_time attributes for all of the associated content
	 * objects.
	 *
	 * @param prsstntObject
	 *            the DFC persistentObject representing an object in repository
	 * @param dctmObj
	 *            the CMSMF DctmObject that has internal/system attributes.
	 * @throws DfException
	 *             Signals that Dctm Server error has occurred.
	 */
	protected void updateContentAttributes(T prsstntObject, DctmObject<T> dctmObj) throws DfException {
		if (DctmObject.logger.isEnabledFor(Level.INFO)) {
			DctmObject.logger.info("Started updating attributes of content objects");
		}

		String parentID = prsstntObject.getObjectId().getId();
		List<DctmContent> contentList = ((DctmDocument) dctmObj).getContentList();
		for (DctmContent dctmContent : contentList) {
			String setFile = dctmContent.getStrSingleAttrValue(DctmAttrNameConstants.SET_FILE);
			if (StringUtils.isBlank(setFile)) {
				setFile = " ";
			}
			// If setFile contains single quote in its contents, to escape it, replace it with 4
			// single quotes.
			setFile = setFile.replaceAll("'", "''''");
			String setClient = dctmContent.getStrSingleAttrValue(DctmAttrNameConstants.SET_CLIENT);
			if (StringUtils.isBlank(setClient)) {
				setClient = " ";
			}
			IDfTime setTime = new DfTime(dctmContent.getDateSingleAttrValue(DctmAttrNameConstants.SET_TIME));

			String pageModifierStr = "";
			if (!StringUtils.isBlank(dctmContent.getPageModifier())) {
				pageModifierStr = " and dcr.page_modifier = ''" + dctmContent.getPageModifier() + "''";
			}

			// Prepare the sql to be executed
			String sqlStr = "UPDATE dmr_content_s SET set_file = ''" + setFile + "'', set_client = ''" + setClient
				+ "'', set_time = TO_DATE(''" + setTime.asString(CMSMFAppConstants.DCTM_DATETIME_PATTERN) + "'', ''"
				+ CMSMFAppConstants.ORACLE_DATETIME_PATTERN
				+ "'') WHERE r_object_id = (select dcs.r_object_id from dmr_content_s dcs, dmr_content_r dcr "
				+ "where dcr.parent_id = ''" + parentID + "'' " + " and dcs.r_object_id = dcr.r_object_id "
				+ "and dcs.rendition = " + dctmContent.getIntSingleAttrValue(DctmAttrNameConstants.RENDITION)
				+ pageModifierStr + " and dcr.page = " + dctmContent.getPageNbr() + " and dcs.full_format = ''"
				+ dctmContent.getStrSingleAttrValue(DctmAttrNameConstants.FULL_FORMAT) + "'')";

			// Run the exec sql
			DctmObject.runExecSQL(prsstntObject.getSession(), sqlStr);
		}

		if (DctmObject.logger.isEnabledFor(Level.INFO)) {
			DctmObject.logger.info("Finished updating attributes of content objects");
		}
	}

	/**
	 * Update i_is_deleted attribute of objects.
	 *
	 * @param updateIsDeletedObjects
	 *            the list of objects whose i_is_deleted needs to be set to true
	 * @throws DfException
	 *             the df exception
	 */
	protected void updateIsDeletedAttribute(IDfSession session, List<String> updateIsDeletedObjects) throws DfException {
		if (DctmObject.logger.isEnabledFor(Level.INFO)) {
			DctmObject.logger.info("Started updating i_is_deleted attribute of objects");
		}

		// Prepare list of object ids for IN clause
		String objectIDList = "";
		for (String objectID : updateIsDeletedObjects) {
			objectIDList = objectIDList + "''" + objectID + "'',";
		}
		// Remove last comma ',' character
		objectIDList = StringUtils.removeEnd(objectIDList, ",");

		// prepare sql to be executed
		String sqlStr = "UPDATE dm_sysobject_s SET i_is_deleted = 1 WHERE r_object_id IN (" + objectIDList + ")";

		DctmObject.runExecSQL(session, sqlStr);

		if (DctmObject.logger.isEnabledFor(Level.INFO)) {
			DctmObject.logger.info("Finished updating i_is_deleted attribute of objects");
		}
	}

	/**
	 * Updates vStamp attribute of an persistent object using execsql.
	 *
	 * @param obj
	 *            the DFC persistentObject representing an object in repository
	 * @throws DfException
	 *             Signals that Dctm Server error has occurred.
	 */
	protected void updateVStamp(IDfPersistentObject obj, int vStamp) throws DfException {
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
	protected void updateVStamp(IDfPersistentObject obj, String tableName, int vStamp) throws DfException {
		final String objId = obj.getObjectId().getId();
		final int oldVstamp = obj.getVStamp();
		if (DctmObject.logger.isEnabledFor(Level.INFO)) {
			DctmObject.logger.info(String.format(
				"Started updating vStamp of a %s object with ID = [%s] from [%d] to [%d] on table [%s]", obj.getType()
				.getName(), objId, oldVstamp, vStamp, tableName));
		}

		String sqlStr = String.format("UPDATE %s SET i_vstamp = %d WHERE r_object_id = ''%s''", tableName, vStamp,
			objId);
		DctmObject.runExecSQL(obj.getSession(), sqlStr);

		if (DctmObject.logger.isEnabledFor(Level.INFO)) {
			DctmObject.logger.info(String.format("Finished updating vStamp of object with ID[%s]", objId));
		}
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
	private static void runExecSQL(IDfSession session, String sqlQueryString) throws DfException {
		IDfQuery dqlQry = new DfClientX().getQuery();
		if (DctmObject.logger.isEnabledFor(Level.DEBUG)) {
			DctmObject.logger.debug("Running exec_sql dql with query: " + sqlQueryString);
		}
		dqlQry.setDQL("EXECUTE exec_sql WITH query='" + sqlQueryString + "'");
		IDfCollection resultCol = dqlQry.execute(session, IDfQuery.EXEC_QUERY);
		if (resultCol.next()) {
			if (resultCol.getValueAt(0).toString().equalsIgnoreCase("F")) {
				if (DctmObject.logger.isEnabledFor(Level.DEBUG)) {
					DctmObject.logger.debug("Error running exec_sql, rolling back changes.");
				}
				dqlQry.setDQL("EXECUTE exec_sql with query='rollback';");
				resultCol.close();
				resultCol = dqlQry.execute(session, IDfQuery.EXEC_QUERY);
			} else {
				if (DctmObject.logger.isEnabledFor(Level.DEBUG)) {
					DctmObject.logger.debug("Success running exec_sql, committing changes.");
				}
				dqlQry.setDQL("EXECUTE exec_sql with query='commit';");
				resultCol.close();
				resultCol = dqlQry.execute(session, IDfQuery.EXEC_QUERY);
			}
		}
		resultCol.close();
	}

	/**
	 * Removes all links of an object in CMS.
	 *
	 * @param sysObj
	 *            the DFC sysObject who is being unlinked
	 * @throws DfException
	 *             Signals that Dctm Server error has occurred.
	 */
	protected static List<String> removeAllLinks(IDfSysObject sysObj) throws DfException {
		if (DctmObject.logger.isEnabledFor(Level.INFO)) {
			DctmObject.logger.info("Started removing all links for object with id: " + sysObj.getObjectId().getId());
		}
		List<String> unLinkedObjectIDList = new ArrayList<String>();
		int folderIdCount = sysObj.getFolderIdCount();
		for (int i = folderIdCount - 1; i >= 0; i--) {
			if (!unLinkedObjectIDList.contains(sysObj.getFolderId(i).getId())) {
				unLinkedObjectIDList.add(sysObj.getFolderId(i).getId());
			}
			sysObj.unlink(sysObj.getFolderId(i).getId());
		}
		if (DctmObject.logger.isEnabledFor(Level.INFO)) {
			DctmObject.logger.info("Finished removing all links for object with id: " + sysObj.getObjectId().getId());
		}
		return unLinkedObjectIDList;
	}

	/**
	 * Sets all attributes of an object in CMS.
	 *
	 * @param prsstntObj
	 *            the DFC persistent object in CMS
	 * @param dctmObj
	 *            the CMSMF DctmObject that contains attributes map
	 * @param updateVersionLabels
	 *            if true, version labels are updated of an object
	 * @param isUpdate
	 *            true if existing object is being updated
	 * @throws DfException
	 *             Signals that Dctm Server error has occurred.
	 */
	protected void setAllAttributesInCMS(T prsstntObj, DctmObject<T> dctmObj, boolean updateVersionLabels,
		boolean isUpdate) throws DfException {
		if (DctmObject.logger.isEnabledFor(Level.INFO)) {
			DctmObject.logger.info("Started setting attributes of persistent object");
		}
		// read the attributes
		Map<String, DctmAttribute> attrListR = dctmObj.getAttrMap();

		if (DctmObject.logger.isEnabledFor(Level.DEBUG)) {
			for (String attrName : attrListR.keySet()) {
				if (attrListR.get(attrName).getAttrValueType() == DctmAttributeTypesEnum.SINGLE_VALUE_TYPE_ATTRIBUTE) {
					DctmObject.logger.debug(attrName + "[S] : " + attrListR.get(attrName).getSingleValue());
				} else if (attrListR.get(attrName).getAttrValueType() == DctmAttributeTypesEnum.REPEATING_VALUE_TYPE_ATTRIBUTE) {
					DctmObject.logger.debug(attrName + "[R] : " + attrListR.get(attrName).getRepeatingValues());
				}
			}
		}

		if (isUpdate) {
			// If an existing object is being updated, clear out all of its attributes that are not
			// part of attribute map in dctmObj.
			// NOTE Only clear non internal and not system attributes
			// NOTE Do not clear group_name attribute for dm_group object
			// NOTE Do not clear home_docbase attribute for dm_user object
			// NOTE Do not clear user_name attribute for dm_user object
			// NOTE Do not clear name attribute for dm_format object
			Set<String> attributesThatAreBeingUpdated = dctmObj.getAttrMap().keySet();
			Set<String> attributesThatNeedsToBeCleared = new HashSet<String>(0);
			Enumeration<?> attrs = prsstntObj.enumAttrs();
			while (attrs.hasMoreElements()) {
				IDfAttr idfAttr = (IDfAttr) attrs.nextElement();
				// Skip clearing system and internal attributess
				// Skip clearing group_name attribute if dealing with group object
				// Skip clearing user_name attribute if dealing with user object
				// Skip clearing home_docbase attribute if dealing with user object
				// Skip clearing name attribute if dealing with format object
				if (!idfAttr.getName().startsWith("r_") && !idfAttr.getName().startsWith("i_")
					&& !(idfAttr.getName().equals(DctmAttrNameConstants.GROUP_NAME) && (dctmObj instanceof DctmGroup))
					&& !(idfAttr.getName().equals(DctmAttrNameConstants.USER_NAME) && (dctmObj instanceof DctmUser))
					&& !(idfAttr.getName().equals(DctmAttrNameConstants.HOME_DOCBASE) && (dctmObj instanceof DctmUser))
					&& !(idfAttr.getName().equals(DctmAttrNameConstants.NAME) && (dctmObj instanceof DctmFormat))) {
					attributesThatNeedsToBeCleared.add(idfAttr.getName());
				}
			}
			// Remove attributes that are being updated from all attributes set
			attributesThatNeedsToBeCleared.removeAll(attributesThatAreBeingUpdated);
			for (String attrName : attributesThatNeedsToBeCleared) {
				clearAttributeInCMS(prsstntObj, attrName);
			}
		}
		if (updateVersionLabels) {
			// First remove version label attributes
			prsstntObj.removeAll("r_version_label");
		}
		// Set attributes
		for (String attrName : dctmObj.getAttrMap().keySet()) {
			// for now ignore setting internal and system attributes
			// TODO check to see if we need to set any internal or system attributes of various
// types
			if (!attrName.startsWith("r_") && !attrName.startsWith("i_")) {
				setAttributeInCMS(prsstntObj, attrName, dctmObj.findAttribute(attrName), isUpdate);
			}
			// but process r_version_lebel
			if (attrName.equals("r_version_label") && updateVersionLabels) {
				setAttributeInCMS(prsstntObj, attrName, dctmObj.findAttribute(attrName), isUpdate);
			}
		}
		if (DctmObject.logger.isEnabledFor(Level.INFO)) {
			DctmObject.logger.info("Finished setting attributes of persistent object.");
		}
	}

	/**
	 * Clears the value of an attribute in cms.
	 *
	 * @param prsstntObj
	 *            the persistent object
	 * @param attrName
	 *            the attribute name
	 * @throws DfException
	 *             the DfException
	 */
	protected void clearAttributeInCMS(T prsstntObj, String attrName) throws DfException {

		if (DctmObject.logger.isEnabledFor(Level.DEBUG)) {
			DctmObject.logger.debug("Clearing attribute with name: " + attrName + " from object with id: "
				+ prsstntObj.getObjectId().getId());
		}

		// If attribute being cleared is repeating type, remove all attr values
		if (prsstntObj.isAttrRepeating(attrName)) {
			prsstntObj.removeAll(attrName);
		} else {
			// If attribute is single value type clear it by setting its value to null value or
// default value
			int attrDataType = prsstntObj.getAttrDataType(attrName);
			switch (attrDataType) {
				case IDfAttr.DM_BOOLEAN:
					prsstntObj.setBoolean(attrName, false);
					break;
				case IDfAttr.DM_ID:
					prsstntObj.setId(attrName, DfId.DF_NULLID);
					break;
				case IDfAttr.DM_INTEGER:
					prsstntObj.setInt(attrName, 0);
					break;
				case IDfAttr.DM_DOUBLE:
					prsstntObj.setDouble(attrName, 0);
					break;
				case IDfAttr.DM_STRING:
					prsstntObj.setString(attrName, "");
					break;
				case IDfAttr.DM_TIME:
					prsstntObj.setTime(attrName, DfTime.DF_NULLDATE);
					break;
				default:
					break;
			}
		}
	}

	/**
	 * Sets the attribute in cms.
	 *
	 * @param prsstntObj
	 *            the DFC persistentObject for which the attribute is being updated
	 * @param attrName
	 *            the attribute name
	 * @param dctmAttribute
	 *            the CMSMF DctmAttribute that holds value for single/repeating type of attribute
	 * @param isUpdate
	 *            true if an existing object is being updated
	 * @throws DfException
	 *             Signals that Dctm Server error has occurred.
	 * @see DctmAttribute
	 */
	protected void setAttributeInCMS(IDfPersistentObject prsstntObj, String attrName, DctmAttribute dctmAttribute,
		boolean isUpdate) throws DfException {
		final IDfSession session = prsstntObj.getSession();
		if (DctmObject.logger.isEnabledFor(Level.DEBUG)) {
			if (dctmAttribute.getAttrValueType() == DctmAttributeTypesEnum.SINGLE_VALUE_TYPE_ATTRIBUTE) {
				DctmObject.logger.debug("Attribute <name, value> pair is: <" + attrName + ", "
					+ dctmAttribute.getSingleValue().toString() + ">");
			} else if (dctmAttribute.getAttrValueType() == DctmAttributeTypesEnum.REPEATING_VALUE_TYPE_ATTRIBUTE) {
				DctmObject.logger.debug("Attribute <name, value> pair is: <" + attrName + ", "
					+ dctmAttribute.getRepeatingValues().toString() + ">");
			}
		}

		// If an existing object is being updated, first clear repeating values if the attribute
		// being
		// set is repeating type.
		if ((dctmAttribute.getAttrValueType() == DctmAttributeTypesEnum.REPEATING_VALUE_TYPE_ATTRIBUTE) && isUpdate) {
			prsstntObj.removeAll(attrName);
		}

		int attrDataType = prsstntObj.getAttrDataType(attrName);
		switch (attrDataType) {
			case IDfAttr.DM_BOOLEAN:
				if (dctmAttribute.getAttrValueType() == DctmAttributeTypesEnum.SINGLE_VALUE_TYPE_ATTRIBUTE) {
					prsstntObj.setBoolean(attrName, (Boolean) dctmAttribute.getSingleValue());
				} else if (dctmAttribute.getAttrValueType() == DctmAttributeTypesEnum.REPEATING_VALUE_TYPE_ATTRIBUTE) {
					List<Object> attrValues = dctmAttribute.getRepeatingValues();
					for (Object attrVal : attrValues) {
						prsstntObj.appendBoolean(attrName, (Boolean) attrVal);
					}
				}
				break;
			case IDfAttr.DM_ID:
				if (dctmAttribute.getAttrValueType() == DctmAttributeTypesEnum.SINGLE_VALUE_TYPE_ATTRIBUTE) {
					prsstntObj.setId(attrName, new DfId((String) dctmAttribute.getSingleValue()));
				} else if (dctmAttribute.getAttrValueType() == DctmAttributeTypesEnum.REPEATING_VALUE_TYPE_ATTRIBUTE) {
					List<Object> attrValues = dctmAttribute.getRepeatingValues();
					for (Object attrVal : attrValues) {
						prsstntObj.appendId(attrName, new DfId((String) attrVal));
					}
				}
				break;
			case IDfAttr.DM_INTEGER:
				if (dctmAttribute.getAttrValueType() == DctmAttributeTypesEnum.SINGLE_VALUE_TYPE_ATTRIBUTE) {
					prsstntObj.setInt(attrName, (Integer) dctmAttribute.getSingleValue());
				} else if (dctmAttribute.getAttrValueType() == DctmAttributeTypesEnum.REPEATING_VALUE_TYPE_ATTRIBUTE) {
					List<Object> attrValues = dctmAttribute.getRepeatingValues();
					for (Object attrVal : attrValues) {
						prsstntObj.appendInt(attrName, (Integer) attrVal);
					}
				}
				break;
			case IDfAttr.DM_DOUBLE:
				if (dctmAttribute.getAttrValueType() == DctmAttributeTypesEnum.SINGLE_VALUE_TYPE_ATTRIBUTE) {
					prsstntObj.setDouble(attrName, (Double) dctmAttribute.getSingleValue());
				} else if (dctmAttribute.getAttrValueType() == DctmAttributeTypesEnum.REPEATING_VALUE_TYPE_ATTRIBUTE) {
					List<Object> attrValues = dctmAttribute.getRepeatingValues();
					for (Object attrVal : attrValues) {
						prsstntObj.appendDouble(attrName, (Double) attrVal);
					}
				}
				break;
			case IDfAttr.DM_STRING:
				if (dctmAttribute.getAttrValueType() == DctmAttributeTypesEnum.SINGLE_VALUE_TYPE_ATTRIBUTE) {
					// Check to see if attribute name belongs to the list of attributes
					// to check for repository owner name. If it is in this list,
					// check the value of the attribute if it is "dm_dbo", replace it with
					// repository owner (operator) name.
					String strVal = (String) dctmAttribute.getSingleValue();
					if (strVal.equals(CMSMFAppConstants.DM_DBO)
						&& RunTimeProperties.getRunTimePropertiesInstance().getAttrsToCheckForRepoOperatorName()
						.contains(attrName)) {
						strVal = RunTimeProperties.getRunTimePropertiesInstance().getTargetRepoOperatorName(session);
						if (DctmObject.logger.isEnabledFor(Level.INFO)) {
							DctmObject.logger.info("Updated " + attrName
								+ " attribute of object to repository operator name.");
						}
					}
					prsstntObj.setString(attrName, strVal);
				} else if (dctmAttribute.getAttrValueType() == DctmAttributeTypesEnum.REPEATING_VALUE_TYPE_ATTRIBUTE) {
					List<Object> attrValues = dctmAttribute.getRepeatingValues();
					for (Object attrVal : attrValues) {
						// Check to see if attribute name belongs to the list of attributes
						// to check for repository owner name. If it is in this list,
						// check the value of the attribute if it is "dm_dbo", replace it with
						// repository owner (operator) name.
						String strVal = (String) attrVal;
						if (strVal.equals(CMSMFAppConstants.DM_DBO)
							&& RunTimeProperties.getRunTimePropertiesInstance().getAttrsToCheckForRepoOperatorName()
							.contains(attrName)) {
							strVal = RunTimeProperties.getRunTimePropertiesInstance()
								.getTargetRepoOperatorName(session);
							if (DctmObject.logger.isEnabledFor(Level.INFO)) {
								DctmObject.logger.info("Updated " + attrName
									+ " attribute of object to repository operator name.");
							}
						}
						prsstntObj.appendString(attrName, strVal);
					}
				}
				break;
			case IDfAttr.DM_TIME:
				if (dctmAttribute.getAttrValueType() == DctmAttributeTypesEnum.SINGLE_VALUE_TYPE_ATTRIBUTE) {
					prsstntObj.setTime(attrName, new DfTime((Date) dctmAttribute.getSingleValue()));
				} else if (dctmAttribute.getAttrValueType() == DctmAttributeTypesEnum.REPEATING_VALUE_TYPE_ATTRIBUTE) {
					List<Object> attrValues = dctmAttribute.getRepeatingValues();
					for (Object attrVal : attrValues) {
						prsstntObj.appendTime(attrName, new DfTime((Date) attrVal));
					}
				}
				break;
			default:
				break;
		}

	}

	protected final T castPersistentObject(IDfPersistentObject obj) {
		if (obj == null) { return null; }
		if (this.dctmObjectClass.isAssignableFrom(obj.getClass())) { return this.dctmObjectClass.cast(obj); }
		throw new ClassCastException(String.format("Cannot cast a %s as a %s", obj.getClass().getCanonicalName(),
			this.dctmObjectClass.getCanonicalName()));
	}

	/**
	 * Gets the object from CMS.
	 *
	 * @param prsstntObj
	 *            the DFC Persistent Object
	 * @return the DctmObject that represents a persistent object in CMS
	 * @throws CMSMFException
	 *             the cMSMF exception
	 */
	public final DctmObject<T> getFromCMS(IDfPersistentObject prsstntObj) throws CMSMFException {
		if (prsstntObj == null) { return null; }
		final T obj = castPersistentObject(prsstntObj);
		try {
			this.dataObject = new DataObject(obj);
		} catch (DfException e) {
			throw new CMSMFException("Failed to introspect the object", e);
		}
		return doGetFromCMS(obj);
	}

	protected abstract DctmObject<T> doGetFromCMS(T prsstntObj) throws CMSMFException;

	/**
	 * Gets all attributes from CMS and sets them in an attribute map of an instance of DctmObject
	 * class.
	 *
	 * @param prsstntObj
	 *            the DFC persistentObject whose attributes will be fetched
	 * @param srcObjID
	 *            the unique object id of the persistent object.
	 * @throws CMSMFException
	 *             the cMSMF exception
	 */
	protected final void getAllAttributesFromCMS(T prsstntObj, String srcObjID) throws CMSMFException {
		if (this.dataObject == null) {
			try {
				this.dataObject = new DataObject(prsstntObj);
			} catch (DfException e) {
				throw new CMSMFException("Failed to introspect the object into a DataObject instance", e);
			}
		}

		if (DctmObject.logger.isEnabledFor(Level.INFO)) {
			DctmObject.logger.info("Started retrieving dctm object attributes from repository for object with id: "
				+ srcObjID);
		}
		try {
			// Set object id
			setSrcObjectID(srcObjID);
			final int attCount = prsstntObj.getAttrCount();
			for (int i = 0; i < attCount; i++) {
				getAttributeFromCMS(prsstntObj, prsstntObj.getAttr(i));
			}
		} catch (DfException e) {
			throw (new CMSMFException("Couldn't read all attributes from dctm object with id: " + srcObjID, e));
		}
		if (DctmObject.logger.isEnabledFor(Level.INFO)) {
			DctmObject.logger.info("Finished retrieving dctm object attributes from repository for object with id: "
				+ srcObjID);
		}
	}

	/**
	 * Gets a single attribute from the CMS and adds to the attributes map of DctmObject.
	 * This method handles single value attribute as well as repeating attributes
	 *
	 * @param dctmObj
	 *            the CMSMF DctmObject to which the retrieved attribute will be added
	 * @param prsstntObj
	 *            the DFC persistentObject whose attribute is being fetched
	 * @param idfAttr
	 *            the DFC idfAttr object that has CMS attribute information
	 * @throws DfException
	 *             Signals that Dctm Server error has occurred.
	 */
	private void getAttributeFromCMS(IDfPersistentObject prsstntObj, IDfAttr idfAttr) throws DfException {
		if (!idfAttr.isRepeating()) {
			// handle single value attribute
			boolean isValueEmpty = true;
			DctmAttribute dctmAttr = new DctmAttribute();
			dctmAttr.setAttrValueType(DctmAttributeTypesEnum.SINGLE_VALUE_TYPE_ATTRIBUTE);
			switch (idfAttr.getDataType()) {
				case IDfAttr.DM_BOOLEAN:
					Boolean boolVal = prsstntObj.getBoolean(idfAttr.getName());
					if (boolVal != null) {
						dctmAttr.setSingleValue(boolVal);
						isValueEmpty = false;
					}
					break;
				case IDfAttr.DM_ID:
					IDfId idVal = prsstntObj.getId(idfAttr.getName());
					if ((idVal != null) && !idVal.isNull() && (idVal.getId().length() != 0)) {
						dctmAttr.setSingleValue(idVal.getId());
						isValueEmpty = false;
					}
					break;
				case IDfAttr.DM_INTEGER:
					Integer intVal = prsstntObj.getInt(idfAttr.getName());
					dctmAttr.setSingleValue(intVal);
					isValueEmpty = false;
					break;
				case IDfAttr.DM_DOUBLE:
					Double doubleVal = prsstntObj.getDouble(idfAttr.getName());
					if (doubleVal != 0) {
						dctmAttr.setSingleValue(doubleVal);
						isValueEmpty = false;
					}
					break;
				case IDfAttr.DM_STRING:
					// Check to see if attribute name belongs to the list of attributes
					// to check for repository owner name. If it is in this list,
					// check the value of the attribute and replace it with dm_dbo
					// if it is indeed equal to repository owner (operator) name.
					String strVal = prsstntObj.getString(idfAttr.getName());
					if (RunTimeProperties.getRunTimePropertiesInstance().getAttrsToCheckForRepoOperatorName()
						.contains(idfAttr.getName())
						&& strVal.equals(RepositoryConfiguration.getRepositoryConfiguration().getOperatorName())) {
						strVal = CMSMFAppConstants.DM_DBO;
						if (DctmObject.logger.isEnabledFor(Level.INFO)) {
							DctmObject.logger.info("Updated " + idfAttr.getName() + " attribute of object with id: "
								+ getSrcObjectID() + " to dm_dbo.");
						}
					}
					if ((strVal != null) && (strVal.length() != 0)) {
						dctmAttr.setSingleValue(strVal);
						isValueEmpty = false;
					}
					break;
				case IDfAttr.DM_TIME:
					IDfTime timeVal = prsstntObj.getTime(idfAttr.getName());
					if ((timeVal != null) && !timeVal.isNullDate() && timeVal.isValid()) {
						Date dateVal = timeVal.getDate();
						dctmAttr.setSingleValue(dateVal);
						isValueEmpty = false;
					}
					break;
				default:
					break;
			}
			// Store the attribute value in the object only if it is not empty
			if (!isValueEmpty) {
				addAttribute(idfAttr.getName(), dctmAttr);
				if (DctmObject.logger.isEnabledFor(Level.DEBUG)) {
					DctmObject.logger.debug("Attribute <name, value> pair is: <" + idfAttr.getName() + ", "
						+ dctmAttr.getSingleValue().toString() + ">");
				}
			}
			isValueEmpty = true;
		} else {
			// handle repeating attribute
			String attrName = idfAttr.getName();
			int repValueCount = prsstntObj.getValueCount(attrName);
			if (repValueCount > 0) {
				DctmAttribute dctmAttr = new DctmAttribute();
				dctmAttr.setAttrValueType(DctmAttributeTypesEnum.REPEATING_VALUE_TYPE_ATTRIBUTE);
				List<Object> attrValueList = new ArrayList<Object>();

				// loop through all of the repeating values
				for (int i = 0; i < repValueCount; i++) {
					switch (idfAttr.getDataType()) {
						case IDfAttr.DM_BOOLEAN:
							Boolean boolVal = prsstntObj.getRepeatingBoolean(attrName, i);
							if (boolVal != null) {
								attrValueList.add(boolVal);
							}
							break;
						case IDfAttr.DM_ID:
							IDfId idVal = prsstntObj.getRepeatingId(attrName, i);
							if ((idVal != null) && !idVal.isNull() && (idVal.getId().length() != 0)) {
								attrValueList.add(idVal.getId());
							}
							break;
						case IDfAttr.DM_INTEGER:
							Integer intVal = prsstntObj.getRepeatingInt(attrName, i);
							attrValueList.add(intVal);
							break;
						case IDfAttr.DM_DOUBLE:
							Double doubleVal = prsstntObj.getRepeatingDouble(attrName, i);
							attrValueList.add(doubleVal);
							break;
						case IDfAttr.DM_STRING:
							// Check to see if attribute name belongs to the list of attributes
							// to check for repository owner name. If it is in this list,
							// check the value of the attribute and replace it with dm_dbo
							// if it is indeed equal to repository owner (operator) name.
							String strVal = prsstntObj.getRepeatingString(idfAttr.getName(), i);
							if (RunTimeProperties.getRunTimePropertiesInstance().getAttrsToCheckForRepoOperatorName()
								.contains(idfAttr.getName())
								&& strVal
								.equals(RepositoryConfiguration.getRepositoryConfiguration().getOperatorName())) {
								strVal = CMSMFAppConstants.DM_DBO;
								if (DctmObject.logger.isEnabledFor(Level.INFO)) {
									DctmObject.logger.info("Updated " + idfAttr.getName()
										+ " attribute of object with id: " + getSrcObjectID() + " to dm_dbo.");
								}
							}
							if ((strVal != null) && (strVal.length() != 0)) {
								attrValueList.add(strVal);
							}
							break;
						case IDfAttr.DM_TIME:
							IDfTime timeVal = prsstntObj.getRepeatingTime(attrName, i);
							if ((timeVal != null) && !timeVal.isNullDate() && timeVal.isValid()) {
								Date dateVal = timeVal.getDate();
								attrValueList.add(dateVal);
							}
							break;
						default:
							break;
					}
				}
				dctmAttr.setRepeatingValues(attrValueList);
				addAttribute(attrName, dctmAttr);
				if (DctmObject.logger.isEnabledFor(Level.DEBUG)) {
					DctmObject.logger.debug("Attribute <name, value> pair is: <" + idfAttr.getName() + ", "
						+ dctmAttr.getRepeatingValues().toString() + ">");
				}
			}
		}
	}

	/**
	 * The static Inner Class restoreOldACLInfo.
	 */
	protected static class restoreOldACLInfo {
		private String aclName;
		private String aclDomain;
		private String objectID;
		private int vStamp;

		/**
		 * Instantiates a new restore old acl info.
		 *
		 * @param aclName
		 *            the acl name
		 * @param aclDomain
		 *            the acl domain
		 * @param objectID
		 *            the object id
		 * @param vStamp
		 *            the v stamp
		 */
		public restoreOldACLInfo(String aclName, String aclDomain, String objectID, int vStamp) {
			super();
			this.aclName = aclName;
			this.aclDomain = aclDomain;
			this.objectID = objectID;
			this.vStamp = vStamp;
		}
	}

	/**
	 * Restore acl of parent folders.
	 *
	 * @param restoreACLObjectList
	 *            the restore acl object list
	 * @throws DfException
	 *             the df exception
	 */
	protected void restoreACLOfParentFolders(IDfSession session, List<restoreOldACLInfo> restoreACLObjectList)
		throws DfException {
		for (restoreOldACLInfo aclInfo : restoreACLObjectList) {
			IDfSysObject parentFolder = (IDfSysObject) session.getObject(new DfId(aclInfo.objectID));
			if (parentFolder != null) {
				parentFolder.setACLName(aclInfo.aclName);
				parentFolder.setACLDomain(aclInfo.aclDomain);
				parentFolder.save();

				// Update the vStamp attribute of the parent folder
				updateVStamp(parentFolder, "dm_sysobject_s", aclInfo.vStamp);

				// Flush this object out
				session.flushObject(parentFolder.getObjectId());
			}
		}

		// Flush the persistent object cache to avoid version mismatch errors.
		session.flush("persistentobjcache", null);
		session.flushCache(false);
	}

	public DataObject getDataObject() {
		return this.dataObject;
	}
}