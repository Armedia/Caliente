package com.delta.cmsmf.mainEngine;

import java.io.IOException;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import com.delta.cmsmf.cmsobjects.DctmACL;
import com.delta.cmsmf.cmsobjects.DctmDocument;
import com.delta.cmsmf.cmsobjects.DctmFolder;
import com.delta.cmsmf.cmsobjects.DctmFormat;
import com.delta.cmsmf.cmsobjects.DctmGroup;
import com.delta.cmsmf.cmsobjects.DctmObject;
import com.delta.cmsmf.cmsobjects.DctmType;
import com.delta.cmsmf.cmsobjects.DctmUser;
import com.delta.cmsmf.constants.DctmTypeConstants;
import com.delta.cmsmf.exception.CMSMFException;
import com.delta.cmsmf.properties.CMSMFProperties;
import com.delta.cmsmf.serialization.DctmObjectWriter;
import com.documentum.fc.client.IDfACL;
import com.documentum.fc.client.IDfFolder;
import com.documentum.fc.client.IDfFormat;
import com.documentum.fc.client.IDfGroup;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.client.IDfSysObject;
import com.documentum.fc.client.IDfType;
import com.documentum.fc.client.IDfUser;
import com.documentum.fc.common.DfException;
import com.documentum.fc.common.DfId;

/**
 * The Class DctmObjectExportHelper is a helper class used extensively during export operation
 * in CMSMF application. This class contains various static methods to serialize several types
 * of objects from documentum repository.
 *
 * @author Shridev Makim 6/15/2010
 */
public class DctmObjectExportHelper {

	/** The logger object used for logging. */
	static Logger logger = Logger.getLogger(DctmObjectExportHelper.class);

	/**
	 * Serializes a folder by given folder path from CMS.
	 *
	 * @param dctmSession
	 *            the existing documentum session
	 * @param fldrPath
	 *            the folder path
	 * @throws CMSMFException
	 *             the cMSMF exception
	 */
	public static void serializeFolderByPath(IDfSession dctmSession, String fldrPath) throws CMSMFException {
		if (DctmObjectExportHelper.logger.isEnabledFor(Level.INFO)) {
			DctmObjectExportHelper.logger.info("Started serializing folder by path: " + fldrPath);
		}
		try {
			// check if folder exists by path provided
			IDfFolder folder = dctmSession.getFolderByPath(fldrPath);
			if (folder != null) {
				DctmObjectExportHelper.serializeFolder(dctmSession, folder);
			} else {
				DctmObjectExportHelper.logger.warn("Folder by path: " + fldrPath + " does not exist in cms.");
				throw (new CMSMFException("Folder does not exist in cms with path: " + fldrPath));
			}
		} catch (DfException e) {
			throw (new CMSMFException("Couldn't retrieve a folder with path: " + fldrPath, e));
		}
		if (DctmObjectExportHelper.logger.isEnabledFor(Level.INFO)) {
			DctmObjectExportHelper.logger.info("Finished serializing folder by path: " + fldrPath);
		}
	}

	/**
	 * Serializes a folder by given DFC folder object from CMS.
	 *
	 * @param dctmSession
	 *            the the existing documentum session
	 * @param folder
	 *            the DFC IDfFolder folder object
	 * @throws CMSMFException
	 *             the cMSMF exception
	 */
	public static void serializeFolder(IDfSession dctmSession, IDfFolder folder) throws CMSMFException {
		if (DctmObjectExportHelper.logger.isEnabledFor(Level.INFO)) {
			DctmObjectExportHelper.logger.info("Started serializing folder.");
		}
		// check to see if user exists
		String folderName = "";
		try {
			DctmObject exportObject = null;
			if (folder != null) {
				folderName = folder.getObjectName();
				// get the folder and serialize it
				DctmObject dctmFldrObject = new DctmFolder(dctmSession);
				exportObject = dctmFldrObject.getFromCMS(folder);
				DctmObjectWriter.writeBinaryObject(exportObject);
				if ((exportObject != null) & DctmObjectExportHelper.logger.isEnabledFor(Level.DEBUG)) {
					DctmObjectExportHelper.logger.debug("Folder object written to filesystem!");
				}
			}
		} catch (DfException e) {
			throw (new CMSMFException("Couldn't retrieve a folder with name: " + folderName, e));
		} catch (IOException e) {
			throw (new CMSMFException("Couldn't serialize a folder with name: " + folderName, e));
		}
		if (DctmObjectExportHelper.logger.isEnabledFor(Level.INFO)) {
			DctmObjectExportHelper.logger.info("Finished serializing folder with name: " + folderName);
		}
	}

	/**
	 * Serializes a user or a group by given name. This method first tries to locate a group by
	 * given name
	 * and then if it can't find the group, it will try to locate a user by given name.
	 *
	 * @param dctmSession
	 *            the existing documentum session
	 * @param userGroupName
	 *            the user or group name that needs to be serialized
	 * @throws CMSMFException
	 *             the cMSMF exception
	 */
	public static void serializeUserOrGroupByName(IDfSession dctmSession, String userGroupName) throws CMSMFException {
		if (DctmObjectExportHelper.logger.isEnabledFor(Level.INFO)) {
			DctmObjectExportHelper.logger.info("Started serializing user or group by name: " + userGroupName);
		}
		// NOTE First check to see if a group exists by the name provided and if not check to see
		// if a user exists. In Documentum CMS, when you create a group object, a dm_user
		// object with user_name same as group name is created. Hence check for group first.
		try {
			// check if group exists by name provided
			IDfGroup group = dctmSession.getGroup(userGroupName);
			if (group != null) {
				// export the group
				DctmObjectExportHelper.serializeGroup(dctmSession, group);
			} else {
				// check if user exists by name provided
				IDfUser user = dctmSession.getUser(userGroupName);
				if (user != null) {
					// export the user
					DctmObjectExportHelper.serializeUser(dctmSession, user);
				} else {
					// something wrong, raise the exception
					throw (new CMSMFException("Expected user or group with name: " + userGroupName
						+ " was not found in cms."));
				}
			}
		} catch (DfException e) {
			throw (new CMSMFException("Couldn't retrieve a user or group in cms with name: " + userGroupName, e));
		}
		if (DctmObjectExportHelper.logger.isEnabledFor(Level.INFO)) {
			DctmObjectExportHelper.logger.info("Finished serializing user or group by name: " + userGroupName);
		}
	}

	/**
	 * Serializes a user by given user name.
	 *
	 * @param dctmSession
	 *            the existing documentum session
	 * @param userName
	 *            the user name
	 * @throws CMSMFException
	 *             the cMSMF exception
	 */
	public static void serializeUserByName(IDfSession dctmSession, String userName) throws CMSMFException {
		if (DctmObjectExportHelper.logger.isEnabledFor(Level.INFO)) {
			DctmObjectExportHelper.logger.info("Started serializing user by name: " + userName);
		}
		try {
			// First check if the property is set to export the users
			if (!CMSMFProperties.SKIP_USERS.getBoolean()) {
				// check if user exists by name provided
				IDfUser user = dctmSession.getUser(userName);
				if (user != null) {
					DctmObjectExportHelper.serializeUser(dctmSession, user);
				} else {
					DctmObjectExportHelper.logger.warn("User by name: " + userName + " does not exist in cms.");
					throw (new CMSMFException("User does not exist in cms with name: " + userName));
				}
			} else {
				if (DctmObjectExportHelper.logger.isEnabledFor(Level.DEBUG)) {
					DctmObjectExportHelper.logger.debug("Serializing a user skipped based on the configuration");
				}
			}
		} catch (DfException e) {
			throw (new CMSMFException("Couldn't retrieve a user with name: " + userName, e));
		}
		if (DctmObjectExportHelper.logger.isEnabledFor(Level.INFO)) {
			DctmObjectExportHelper.logger.info("Finished serializing user by name: " + userName);
		}
	}

	/**
	 * Serializes a user by given user object from CMS.
	 *
	 * @param dctmSession
	 *            the existing documentum session
	 * @param user
	 *            the DFC user object
	 * @throws CMSMFException
	 *             the cMSMF exception
	 */
	public static void serializeUser(IDfSession dctmSession, IDfUser user) throws CMSMFException {
		if (DctmObjectExportHelper.logger.isEnabledFor(Level.INFO)) {
			DctmObjectExportHelper.logger.info("Started serializing user.");
		}
		// check to see if user exists
		String userName = "";
		try {
			DctmObject exportObject = null;
			if ((user != null) && !CMSMFProperties.SKIP_USERS.getBoolean()) {
				userName = user.getUserName();
				// get the user and serialize it
				DctmUser dctmUser = new DctmUser(dctmSession);
				exportObject = dctmUser.getFromCMS(user);
				DctmObjectWriter.writeBinaryObject(exportObject);
				if (DctmObjectExportHelper.logger.isEnabledFor(Level.DEBUG)) {
					DctmObjectExportHelper.logger.debug("Finished exporting user: " + userName);
				}
			}
		} catch (DfException e) {
			throw (new CMSMFException("Couldn't retrieve a user with name: " + userName, e));
		} catch (IOException e) {
			throw (new CMSMFException("Couldn't serialize a user with name: " + userName, e));
		}
		if (DctmObjectExportHelper.logger.isEnabledFor(Level.INFO)) {
			DctmObjectExportHelper.logger.info("Finished serializing user with name: " + userName);
		}
	}

	/**
	 * Serializes a group by given group name.
	 *
	 * @param dctmSession
	 *            the existing documentum session
	 * @param groupName
	 *            the group name
	 * @throws CMSMFException
	 *             the cMSMF exception
	 */
	public static void serializeGroupByName(IDfSession dctmSession, String groupName) throws CMSMFException {
		if (DctmObjectExportHelper.logger.isEnabledFor(Level.INFO)) {
			DctmObjectExportHelper.logger.info("Started serializing group by name: " + groupName);
		}
		try {
			// First check if the property is set to export the groups
			if (!CMSMFProperties.SKIP_GROUPS.getBoolean()) {
				// check if group exists by name provided
				IDfGroup group = dctmSession.getGroup(groupName);
				if (group != null) {
					DctmObjectExportHelper.serializeGroup(dctmSession, group);
				} else {
					DctmObjectExportHelper.logger.warn("Group by name: " + groupName + " does not exist in cms.");
					throw (new CMSMFException("Group does not exist in cms with name: " + groupName));
				}
			} else {
				if (DctmObjectExportHelper.logger.isEnabledFor(Level.DEBUG)) {
					DctmObjectExportHelper.logger.debug("Serializing a group skipped based on the configuration");
				}
			}
		} catch (DfException e) {
			throw (new CMSMFException("Couldn't retrieve a group with name: " + groupName, e));
		}
		if (DctmObjectExportHelper.logger.isEnabledFor(Level.INFO)) {
			DctmObjectExportHelper.logger.info("Finished serializing group by name: " + groupName);
		}
	}

	/**
	 * Serializes a group by given DFC group object from CMS.
	 *
	 * @param dctmSession
	 *            the existing documentum session
	 * @param group
	 *            the DFC group object
	 * @throws CMSMFException
	 *             the cMSMF exception
	 */
	public static void serializeGroup(IDfSession dctmSession, IDfGroup group) throws CMSMFException {
		if (DctmObjectExportHelper.logger.isEnabledFor(Level.INFO)) {
			DctmObjectExportHelper.logger.info("Started serializing group.");
		}
		// check to see if group exists
		String groupName = "";
		try {
			DctmObject exportObject = null;
			if ((group != null) && !CMSMFProperties.SKIP_GROUPS.getBoolean()) {
				groupName = group.getGroupName();
				// get the group and serialize it
				DctmGroup dctmGroup = new DctmGroup(dctmSession);
				exportObject = dctmGroup.getFromCMS(group);
				DctmObjectWriter.writeBinaryObject(exportObject);
				if (DctmObjectExportHelper.logger.isEnabledFor(Level.DEBUG)) {
					DctmObjectExportHelper.logger.debug("Finished exporting group: " + groupName);
				}
			}
		} catch (DfException e) {
			throw (new CMSMFException("Couldn't retrieve a group with name: " + groupName, e));
		} catch (IOException e) {
			throw (new CMSMFException("Couldn't serialize a group with name: " + groupName, e));
		}
		if (DctmObjectExportHelper.logger.isEnabledFor(Level.INFO)) {
			DctmObjectExportHelper.logger.info("Finished serializing group with name: " + groupName);
		}
	}

	/**
	 * Serializes an acl by given acl name.
	 *
	 * @param dctmSession
	 *            the existing documentum session
	 * @param aclName
	 *            the acl name
	 * @param aclDomain
	 *            the acl domain name
	 * @throws CMSMFException
	 *             the cMSMF exception
	 */
	public static void serializeACLByName(IDfSession dctmSession, String aclName, String aclDomain)
		throws CMSMFException {
		if (DctmObjectExportHelper.logger.isEnabledFor(Level.INFO)) {
			DctmObjectExportHelper.logger.info("Started serializing acl by name: " + aclName);
		}
		try {
			// check if acl exists by name provided
			// First check if the property is set to export the groups
			if (!CMSMFProperties.SKIP_ACLS.getBoolean()) {
				IDfACL acl = dctmSession.getACL(aclDomain, aclName);
				if (acl != null) {
					DctmObjectExportHelper.serializeACL(dctmSession, acl);
				} else {
					DctmObjectExportHelper.logger.warn("ACL by name: " + aclName + " with domain: " + aclDomain
						+ " does not exist in cms.");
					throw (new CMSMFException("ACL does not exist in cms with name: " + aclName + " and domain: "
						+ aclDomain));
				}
			} else {
				if (DctmObjectExportHelper.logger.isEnabledFor(Level.DEBUG)) {
					DctmObjectExportHelper.logger.debug("Serializing a acl skipped based on the properties set");
				}
			}
		} catch (DfException e) {
			throw (new CMSMFException("Couldn't retrieve an ACL with name: " + aclName + " and domain: " + aclDomain, e));
		}
		if (DctmObjectExportHelper.logger.isEnabledFor(Level.INFO)) {
			DctmObjectExportHelper.logger.info("Finished serializing ACL by name: " + aclName);
		}
	}

	/**
	 * Serializes an acl by given DFC acl object from CMS.
	 *
	 * @param dctmSession
	 *            the existing documentum session
	 * @param acl
	 *            the DFC acl object
	 * @throws CMSMFException
	 *             the cMSMF exception
	 */
	public static void serializeACL(IDfSession dctmSession, IDfACL acl) throws CMSMFException {
		if (DctmObjectExportHelper.logger.isEnabledFor(Level.INFO)) {
			DctmObjectExportHelper.logger.info("Started serializing ACL.");
		}
		// check to see if acl exists
		String aclName = "";
		try {
			DctmObject exportObject = null;
			if (acl != null) {
				aclName = acl.getObjectName();
				// get the acl and serialize it
				DctmACL dctmACL = new DctmACL(dctmSession);
				exportObject = dctmACL.getFromCMS(acl);
				DctmObjectWriter.writeBinaryObject(exportObject);
				if (DctmObjectExportHelper.logger.isEnabledFor(Level.DEBUG)) {
					DctmObjectExportHelper.logger.debug("Finished exporting acl: " + aclName);
				}
			}
		} catch (DfException e) {
			throw (new CMSMFException("Couldn't retrieve an acl with name: " + aclName, e));
		} catch (IOException e) {
			throw (new CMSMFException("Couldn't serialize an acl with name: " + aclName, e));
		}
		if (DctmObjectExportHelper.logger.isEnabledFor(Level.INFO)) {
			DctmObjectExportHelper.logger.info("Finished serializing ACL with name: " + aclName);
		}
	}

	/**
	 * Serializes a type by given type name.
	 *
	 * @param dctmSession
	 *            the existing documentum session
	 * @param typeName
	 *            the type name
	 * @throws CMSMFException
	 *             the cMSMF exception
	 */
	public static void serializeTypeByName(IDfSession dctmSession, String typeName) throws CMSMFException {
		if (DctmObjectExportHelper.logger.isEnabledFor(Level.INFO)) {
			DctmObjectExportHelper.logger.info("Started serializing type by name: " + typeName);
		}
		try {
			// check if type exists by name provided
			IDfType type = dctmSession.getType(typeName);
			if (type != null) {
				DctmObjectExportHelper.serializeType(dctmSession, type);
			} else {
				DctmObjectExportHelper.logger.warn("Type by name: " + typeName + " does not exist in cms.");
				throw (new CMSMFException("Type does not exist in cms with name: " + typeName));
			}
		} catch (DfException e) {
			throw (new CMSMFException("Couldn't retrieve a type with name: " + typeName, e));
		}
		if (DctmObjectExportHelper.logger.isEnabledFor(Level.INFO)) {
			DctmObjectExportHelper.logger.info("Finished serializing type by name: " + typeName);
		}
	}

	/**
	 * Serializes a type by given DFC type object from CMS.
	 *
	 * @param dctmSession
	 *            the existing documentum session
	 * @param type
	 *            the DFC type object
	 * @throws CMSMFException
	 *             the cMSMF exception
	 */
	public static void serializeType(IDfSession dctmSession, IDfType type) throws CMSMFException {
		if (DctmObjectExportHelper.logger.isEnabledFor(Level.INFO)) {
			DctmObjectExportHelper.logger.info("Started serializing type.");
		}
		// check to see if type exists
		String typeName = "";
		try {
			DctmObject exportObject = null;
			if (type != null) {
				typeName = type.getName();
				// get the type and serialize it
				DctmType dctmType = new DctmType(dctmSession);
				exportObject = dctmType.getFromCMS(type);
				DctmObjectWriter.writeBinaryObject(exportObject);
				if (DctmObjectExportHelper.logger.isEnabledFor(Level.DEBUG)) {
					DctmObjectExportHelper.logger.debug("Finished exporting type: " + typeName);
				}
			}
		} catch (DfException e) {
			throw (new CMSMFException("Couldn't retrieve a type with name: " + typeName, e));
		} catch (IOException e) {
			throw (new CMSMFException("Couldn't serialize a type with name: " + typeName, e));
		}
		if (DctmObjectExportHelper.logger.isEnabledFor(Level.INFO)) {
			DctmObjectExportHelper.logger.info("Finished serializing type with name: " + typeName);
		}
	}

	/**
	 * Serializes a format by given format name.
	 *
	 * @param dctmSession
	 *            the existing documentum session
	 * @param formatName
	 *            the format name
	 * @throws CMSMFException
	 *             the cMSMF exception
	 */
	public static void serializeFormatByName(IDfSession dctmSession, String formatName) throws CMSMFException {
		if (DctmObjectExportHelper.logger.isEnabledFor(Level.INFO)) {
			DctmObjectExportHelper.logger.info("Started serializing acl by name: " + formatName);
		}
		try {
			// check if format exists by name provided
			IDfFormat format = dctmSession.getFormat(formatName);
			if (format != null) {
				DctmObjectExportHelper.serializeFormat(dctmSession, format);
			} else {
				DctmObjectExportHelper.logger.warn("Format by name: " + formatName + " does not exist in cms.");
				throw (new CMSMFException("Format does not exist in cms with name: " + formatName));
			}
		} catch (DfException e) {
			throw (new CMSMFException("Couldn't retrieve an Format with name: " + formatName, e));
		}
		if (DctmObjectExportHelper.logger.isEnabledFor(Level.INFO)) {
			DctmObjectExportHelper.logger.info("Finished serializing Format by name: " + formatName);
		}
	}

	/**
	 * Serializes a format by given DFC format object from CMS.
	 *
	 * @param dctmSession
	 *            the existing documentum session
	 * @param format
	 *            the DFC format object
	 * @throws CMSMFException
	 *             the cMSMF exception
	 */
	public static void serializeFormat(IDfSession dctmSession, IDfFormat format) throws CMSMFException {
		if (DctmObjectExportHelper.logger.isEnabledFor(Level.INFO)) {
			DctmObjectExportHelper.logger.info("Started serializing Format.");
		}
		// check to see if format exists
		String formatName = "";
		try {
			DctmObject exportObject = null;
			if (format != null) {
				formatName = format.getName();
				// get the format and serialize it
				DctmFormat dctmFormat = new DctmFormat(dctmSession);
				exportObject = dctmFormat.getFromCMS(format);
				DctmObjectWriter.writeBinaryObject(exportObject);
				if (DctmObjectExportHelper.logger.isEnabledFor(Level.DEBUG)) {
					DctmObjectExportHelper.logger.debug("Finished exporting format: " + formatName);
				}
			}
		} catch (DfException e) {
			throw (new CMSMFException("Couldn't retrieve an format with name: " + formatName, e));
		} catch (IOException e) {
			throw (new CMSMFException("Couldn't serialize an format with name: " + formatName, e));
		}
		if (DctmObjectExportHelper.logger.isEnabledFor(Level.INFO)) {
			DctmObjectExportHelper.logger.info("Finished serializing Format with name: " + formatName);
		}
	}

	public static void serializeDocumentByID(IDfSession dctmSession, String docObjID) throws CMSMFException {
		if (DctmObjectExportHelper.logger.isEnabledFor(Level.INFO)) {
			DctmObjectExportHelper.logger.info("Started serializing Document by id: " + docObjID);
		}
		try {
			// check if document exists by ID provided
			if (StringUtils.isNotBlank(docObjID)) {
				IDfSysObject docObj = (IDfSysObject) dctmSession.getObject(new DfId(docObjID));
				if ((docObj != null)
					&& (docObj.getType().getName().equals(DctmTypeConstants.DM_DOCUMENT) || docObj.getType()
						.isSubTypeOf(DctmTypeConstants.DM_DOCUMENT))) {
					DctmObjectExportHelper.serializeDocument(dctmSession, docObj);
				} else {
					DctmObjectExportHelper.logger.warn("Document by id: " + docObjID + " does not exist in cms.");
					throw (new CMSMFException("Document does not exist in cms with id: " + docObjID));
				}
			}
		} catch (DfException e) {
			throw (new CMSMFException("Couldn't retrieve a document with id: " + docObjID, e));
		}
		if (DctmObjectExportHelper.logger.isEnabledFor(Level.INFO)) {
			DctmObjectExportHelper.logger.info("Finished serializing document by id: " + docObjID);
		}
	}

	/**
	 * Serializes a document by given DFC document object from CMS.
	 *
	 * @param dctmSession
	 *            the existing documentum session
	 * @param docObj
	 *            the DFC sysobject
	 * @throws CMSMFException
	 *             the cMSMF exception
	 */
	public static void serializeDocument(IDfSession dctmSession, IDfSysObject docObj) throws CMSMFException {
		if (DctmObjectExportHelper.logger.isEnabledFor(Level.INFO)) {
			DctmObjectExportHelper.logger.info("Started serializing Format.");
		}
		// check to see if document exists
		String documentName = "";
		try {
			DctmObject exportObject = null;
			if (docObj != null) {
				documentName = docObj.getObjectName();
				// get the format and serialize it
				DctmDocument dctmDocument = new DctmDocument(dctmSession);
				exportObject = dctmDocument.getFromCMS(docObj);
				DctmObjectWriter.writeBinaryObject(exportObject);
				if (DctmObjectExportHelper.logger.isEnabledFor(Level.DEBUG)) {
					DctmObjectExportHelper.logger.debug("Finished exporting format: " + documentName);
				}
			}
		} catch (DfException e) {
			throw (new CMSMFException("Couldn't retrieve a document with name: " + documentName, e));
		} catch (IOException e) {
			throw (new CMSMFException("Couldn't serialize a document with name: " + documentName, e));
		}
		if (DctmObjectExportHelper.logger.isEnabledFor(Level.INFO)) {
			DctmObjectExportHelper.logger.info("Finished serializing document with name: " + documentName);
		}

	}
}
