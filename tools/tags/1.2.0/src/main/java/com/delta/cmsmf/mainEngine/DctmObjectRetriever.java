package com.delta.cmsmf.mainEngine;

import org.apache.log4j.Logger;

import com.delta.cmsmf.cmsobjects.DctmACL;
import com.delta.cmsmf.cmsobjects.DctmDocument;
import com.delta.cmsmf.cmsobjects.DctmFolder;
import com.delta.cmsmf.cmsobjects.DctmGroup;
import com.delta.cmsmf.cmsobjects.DctmObject;
import com.delta.cmsmf.cmsobjects.DctmReferenceDocument;
import com.delta.cmsmf.cmsobjects.DctmUser;
import com.delta.cmsmf.constants.DctmTypeConstants;
import com.delta.cmsmf.exception.CMSMFException;
import com.documentum.fc.client.IDfPersistentObject;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.client.IDfSysObject;
import com.documentum.fc.common.DfException;

/**
 * The Class DctmObjectRetriever is a utility class that contains a method to
 * retrieve a persistent object from the documentum CMS.
 * 
 * @author Shridev Makim 6/15/2010
 */
public class DctmObjectRetriever {

	/** The logger object used for logging. */
	static Logger logger = Logger.getLogger(DctmObjectRetriever.class);

	/** The dctm session. */
	private IDfSession dctmSession = null;

	/**
	 * Sets the dctm session.
	 * 
	 * @param dctmSession
	 *            the new dctm session
	 */
	public void setDctmSession(IDfSession dctmSession) {
		this.dctmSession = dctmSession;
	}

	/**
	 * Instantiates a new dctm object retriever.
	 * 
	 * @param dctmSession
	 *            the existing dctm repository session
	 */
	public DctmObjectRetriever(IDfSession dctmSession) {
		super();
		this.dctmSession = dctmSession;
	}

	/**
	 * Retrieves an object from a documentum repository which can be serialized to the file system.
	 * It instantiates an instance of DctmObject based on the type of persistent object and then
	 * calls getFromCMS() method to retrieve the object. Method getFromCMS() may export any
	 * supporting
	 * objects referenced by given persistent object.
	 * 
	 * @param prsstntObj
	 *            the prsstnt obj
	 * @return the dctm object
	 * @throws CMSMFException
	 *             the cMSMF exception
	 */
	public DctmObject retrieveObject(IDfPersistentObject prsstntObj) throws CMSMFException {

		DctmObject dctmObject = null;
		DctmObject exportObject = null;

		try {
			String objTypeName = prsstntObj.getType().getName();
			if (objTypeName.equals(DctmTypeConstants.DM_DOCUMENT)
				|| prsstntObj.getType().isSubTypeOf(DctmTypeConstants.DM_DOCUMENT)) {
				if (((IDfSysObject) prsstntObj).isReference()) {
					// This is a reference object. Handle it differently
					dctmObject = new DctmReferenceDocument();
				} else {
					// This is a regular dm_document object
					dctmObject = new DctmDocument();
				}
			} else if (objTypeName.equals(DctmTypeConstants.DM_FOLDER)
				|| prsstntObj.getType().isSubTypeOf(DctmTypeConstants.DM_FOLDER)) {
				dctmObject = new DctmFolder();
			} else if (objTypeName.equals(DctmTypeConstants.DM_USER)
				|| prsstntObj.getType().isSubTypeOf(DctmTypeConstants.DM_USER)) {
				dctmObject = new DctmUser();
			} else if (objTypeName.equals(DctmTypeConstants.DM_GROUP)
				|| prsstntObj.getType().isSubTypeOf(DctmTypeConstants.DM_GROUP)) {
				dctmObject = new DctmGroup();
			} else if (objTypeName.equals(DctmTypeConstants.DM_ACL)
				|| prsstntObj.getType().isSubTypeOf(DctmTypeConstants.DM_ACL)) {
				dctmObject = new DctmACL();
			}

			if (dctmObject != null) {
				dctmObject.setDctmSession(this.dctmSession);
				exportObject = dctmObject.getFromCMS(prsstntObj);
			}
		} catch (DfException e) {
			String sysObjID = "";
			try {
				sysObjID = prsstntObj.getObjectId().getId();
			} catch (DfException e1) {
				DctmObjectRetriever.logger.error("Couldn't get object id for dm_sysobject", e1);
			}
			throw (new CMSMFException("Couldn't not determine type of dm_sysobject in repository with id: " + sysObjID,
				e));
		}

		return exportObject;
	}
}
