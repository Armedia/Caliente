package com.delta.cmsmf.cmsobjects;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import com.delta.cmsmf.constants.DctmAttrNameConstants;
import com.delta.cmsmf.datastore.DataObject;
import com.delta.cmsmf.datastore.DataProperty;
import com.delta.cmsmf.datastore.DataType;
import com.delta.cmsmf.exception.CMSMFException;
import com.delta.cmsmf.mainEngine.AbstractCMSMFMain;
import com.delta.cmsmf.mainEngine.DctmObjectExportHelper;
import com.delta.cmsmf.runtime.DctmConnectionPool;
import com.documentum.fc.client.IDfACL;
import com.documentum.fc.client.IDfDocument;
import com.documentum.fc.client.IDfFolder;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.client.IDfSysObject;
import com.documentum.fc.client.distributed.impl.IReference;
import com.documentum.fc.client.distributed.impl.ReferenceFinder;
import com.documentum.fc.common.DfException;
import com.documentum.fc.common.DfId;
import com.documentum.fc.common.DfValue;
import com.documentum.fc.common.IDfId;

/**
 * The DctmDocument class contains methods to export/import dm_document type (or its subtype) of
 * objects
 * from/to Documentum CMS. It also contains methods to export any supporting objects that are needed
 * to
 * replicate a dm_document object in target repository.
 * <p>
 * All versions of a document and all content files of each version are stored in lists which are
 * the field of this class. This allows to serialize/deserialize all of it in one shot and maintains
 * it in one enclosed object. It also helps during building the version tree in target repository
 * during import step.
 * <p>
 * <b> NOTE: Virtual documents are not handled currently.</b>
 *
 * @author Shridev Makim 6/15/2010
 */
public class DctmReferenceDocument extends DctmDocument {

	/** The Constant serialVersionUID. */
	private static final long serialVersionUID = 1L;

	// Static variables used to see how many documents were created, skipped, updated
	/** Keeps track of nbr of reference documents read from file during import process. */
	private static AtomicInteger ref_docs_read = new AtomicInteger(0);
	/** Keeps track of nbr of reference documents skipped due to duplicates during import process. */
	private static AtomicInteger ref_docs_skipped = new AtomicInteger(0);
	/** Keeps track of nbr of reference documents updated in CMS during import process. */
	private static AtomicInteger ref_docs_updated = new AtomicInteger(0);
	/** Keeps track of nbr of reference documents created in CMS during import process. */
	private static AtomicInteger ref_docs_created = new AtomicInteger(0);

	/** The logger object used for logging. */
	private static Logger logger = Logger.getLogger(DctmReferenceDocument.class);

	/**
	 * The isThisATest is used for testing purposes. The value for this attribute is set from
	 * properties file.
	 * If the value is true, the documents and folders are created in /Replications cabinet.
	 */
	private static final boolean isThisATest = AbstractCMSMFMain.getInstance().isTestMode();

	/** The binding condition. */
	private String bindingCondition;

	/** The binding label. */
	private String bindingLabel;

	/** The reference repository name. */
	private String referenceDbName;

	/** The id of the referenced object in remote repository. */
	private String referenceById;

	/**
	 * Instantiates a new dctm document.
	 */
	public DctmReferenceDocument() {
		super(DctmObjectType.DCTM_REFERENCE_DOCUMENT);
	}

	/**
	 * Gets the binding condition.
	 *
	 * @return the binding condition
	 */
	public String getBindingCondition() {
		return this.bindingCondition;
	}

	/**
	 * Gets the binding label.
	 *
	 * @return the binding label
	 */
	public String getBindingLabel() {
		return this.bindingLabel;
	}

	/**
	 * Gets the reference repository name.
	 *
	 * @return the reference repository name
	 */
	public String getReferenceDbName() {
		return this.referenceDbName;
	}

	/**
	 * Gets the id of the referenced object in remote repository.
	 *
	 * @return the reference by id
	 */
	public String getReferenceById() {
		return this.referenceById;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.delta.cmsmf.repoSync.DctmObject#createInCMS()
	 */
	@Override
	public void createInCMS(IDfSession session) throws DfException, IOException {

		if (DctmReferenceDocument.logger.isEnabledFor(Level.INFO)) {
			DctmReferenceDocument.logger.info("Started creating dctm dm_document in repository");
		}

		// Create a mirror object in target repository
		createMirrorDocument(session);

		if (DctmReferenceDocument.logger.isEnabledFor(Level.INFO)) {
			DctmReferenceDocument.logger.info("Finished creating dctm dm_document in repository");
		}

	}

	/**
	 * Prints the import report detailing how many reference documents were read, updated, created,
	 * skipped
	 * during the import process.
	 */
	public static void printImportReport() {
		DctmReferenceDocument.logger.info("No. of reference documents read from file: "
			+ DctmReferenceDocument.ref_docs_read);
		DctmReferenceDocument.logger.info("No. of reference documents skipped due to duplicates: "
			+ DctmReferenceDocument.ref_docs_skipped);
		DctmReferenceDocument.logger.info("No. of reference documents updated: "
			+ DctmReferenceDocument.ref_docs_updated);
		DctmReferenceDocument.logger.info("No. of reference documents created: "
			+ DctmReferenceDocument.ref_docs_created);
	}

	public static String getDetailedReferenceDocumentImportReport() {
		StringBuffer importReport = new StringBuffer();
		importReport
			.append("\nNo. of reference documents read from file: " + DctmReferenceDocument.ref_docs_read + ".");
		importReport.append("\nNo. of reference documents skipped due to duplicates: "
			+ DctmReferenceDocument.ref_docs_skipped + ".");
		importReport.append("\nNo. of reference documents updated: " + DctmReferenceDocument.ref_docs_updated + ".");
		importReport.append("\nNo. of reference documents created: " + DctmReferenceDocument.ref_docs_created + ".");

		return importReport.toString();
	}

	/**
	 * Creates the mirror document in target repository.
	 *
	 * @throws DfException
	 *             Signals that Dctm Server error has occurred.
	 */
	private void createMirrorDocument(IDfSession session) throws DfException {
		if (DctmReferenceDocument.logger.isEnabledFor(Level.INFO)) {
			DctmReferenceDocument.logger.info("Started creating dctm dm_document with multiple versions in repository");
		}
		DctmReferenceDocument.ref_docs_read.incrementAndGet();

		// Begin transaction
		session.beginTrans();

		// before creating this document check to see if identical object already exist in CMS
		IDfDocument existingMirrorDoc = retrieveIdenticalObjectFromCMS(session, this);

		if (existingMirrorDoc == null) {
			// Matching mirror document does not exist in target repository
			if (DctmReferenceDocument.logger.isEnabledFor(Level.DEBUG)) {
				DctmReferenceDocument.logger.debug("Duplicate mirror object does not exist!");
			}

			// Create new mirror document
			// NOTE First try to locate the remote object using the target repository session, if
			// there is a dfexception thrown, try locating the remote object using a
			// session with source repository
			IDfSysObject remoteObj = null;
			try {
				remoteObj = (IDfSysObject) session.getObject(new DfId(getReferenceById()));
			} catch (DfException e) {
				DctmReferenceDocument.logger.warn(
					"Unable to locate a remote object while creating reference using target repository session.", e);
				// if that fails, try to locate the remote object using source repository session
				final IDfSession otherSession = DctmConnectionPool.acquireSession();
				try {
					remoteObj = (IDfSysObject) otherSession.getObject(new DfId(getReferenceById()));
				} catch (DfException e1) {
					DctmReferenceDocument.logger.warn(
						"Unable to locate a remote object while creating reference using source repository session.",
						e1);
					session.abortTrans();
				} finally {
					DctmConnectionPool.releaseSession(otherSession);
				}
			}
			if (remoteObj != null) {
				// Get the folder object id in target repository where we are adding the mirror
// object
				String parentFolderLocation = getFolderLocations().get(0);
				if (DctmReferenceDocument.isThisATest) {
					parentFolderLocation = "/Replications" + parentFolderLocation;
				}

				IDfSysObject parentFolder = session.getFolderByPath(parentFolderLocation);
				if (parentFolder != null) {
					// Add reference in the parent folder for a remote object
					IDfId referenceObjId = remoteObj.addReference(parentFolder.getObjectId(), getBindingCondition(),
						getBindingLabel());
					if (DctmReferenceDocument.logger.isEnabledFor(Level.DEBUG)) {
						DctmReferenceDocument.logger.debug("reference ID is: " + referenceObjId.getId());
					}
					DctmReferenceDocument.ref_docs_created.incrementAndGet();
					session.commitTrans();
				}
			} else {
				DctmReferenceDocument.logger.warn("Unable to locate a remote object while creating a mirror object");
				session.abortTrans();
			}
		} else {
			session.abortTrans();
			DctmReferenceDocument.ref_docs_skipped.incrementAndGet();
			if (DctmReferenceDocument.logger.isEnabledFor(Level.DEBUG)) {
				DctmReferenceDocument.logger.debug("Duplicate mirror object DOES exist!");
			}
		}
	}

	/**
	 * Tries to retrieve identical mirror object from cms.
	 *
	 * @param dctmRefDoc
	 *            the dctm reference doc
	 * @return the sysobject that is identical to object being imported
	 */
	private IDfDocument retrieveIdenticalObjectFromCMS(IDfSession session, DctmReferenceDocument dctmRefDoc) {

		if (DctmReferenceDocument.logger.isEnabledFor(Level.INFO)) {
			DctmReferenceDocument.logger.info("Started retrieving Identical mirror document from target cms.");
		}
		String objectName = dctmRefDoc.getStrSingleAttrValue(DctmAttrNameConstants.OBJECT_NAME);
		// If object name contains single quote, replace it with 2 single quotes for DQL
		objectName = objectName.replaceAll("'", "''");

		String objectType = dctmRefDoc.getStrSingleAttrValue(DctmAttrNameConstants.R_OBJECT_TYPE);
		String fldrLoc = "";
		if (dctmRefDoc.getFolderLocations().size() >= 1) {
			fldrLoc = dctmRefDoc.getFolderLocations().get(0);
		}
		if (DctmReferenceDocument.isThisATest) {
			fldrLoc = "/Replications" + fldrLoc;
		}
		// If folder location contains single quote, replace it with 2 single quotes for DQL
		fldrLoc = fldrLoc.replaceAll("'", "''");

		StringBuffer objLookUpQry = new StringBuffer(50);
		try {
			// Build a query for ex: " dm_document where object_name='xxx' and
			// folder('/xxx/xxx') and r_creation_date=DATE('xxxxxx')
			objLookUpQry.append(objectType);
			objLookUpQry.append(" (ALL) where object_name='");
			objLookUpQry.append(objectName);
			objLookUpQry.append("' and folder('");
			objLookUpQry.append(fldrLoc);
			objLookUpQry.append("') and i_is_reference = True");
			if (DctmReferenceDocument.logger.isEnabledFor(Level.DEBUG)) {
				DctmReferenceDocument.logger.debug("Query to lookup duplicate mirror document is: "
					+ objLookUpQry.toString());
			}
			// Retrieve the object using the query
			return castPersistentObject(session.getObjectByQualification(objLookUpQry.toString()));
		} catch (DfException e) {
			DctmReferenceDocument.logger.error("Lookup of object failed with query: " + objLookUpQry, e);
			return null;
		}
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.delta.cmsmf.cmsobjects.DctmObject#getFromCMS(com.documentum.fc.client.IDfPersistentObject)
	 */
	@Override
	protected DctmReferenceDocument doGetFromCMS(IDfDocument doc) throws CMSMFException {

		if (DctmReferenceDocument.logger.isEnabledFor(Level.INFO)) {
			DctmReferenceDocument.logger
				.info("Started exporting dctm mirror dm_document and supporting objects from repository");
		}

		// NOTE: Mirror objects do not have content associated with them. They merely point to
		// remote object. Mirror objects contains the same attributes as remote object except the
// acl name
		// and acl domain. Mirror objects also do not have any versions.

		DctmReferenceDocument dctmReferenceDocument = new DctmReferenceDocument();
		String srcObjID = null;
		try {
			srcObjID = doc.getObjectId().getId();

			// Get all of the attributes
			dctmReferenceDocument.getAllAttributesFromCMS(doc, srcObjID);

			// Get reference attributes
			dctmReferenceDocument.getReferenceAttributesFromCMS(doc, srcObjID);

			// Export other supporting objects
			// exportSupportingObjects((IDfSysObject) prsstntObj);

			// Process folders and write where this document is linked
			exportParentFolders(dctmReferenceDocument, doc, srcObjID);

		} catch (DfException e) {
			throw (new CMSMFException("Couldn't retrieve mirror object from cms with id: " + srcObjID, e));
		} catch (CMSMFException e) {
			throw (new CMSMFException("Couldn't retrieve mirror object from cms with id: " + srcObjID, e));
		}

		if (DctmReferenceDocument.logger.isEnabledFor(Level.INFO)) {
			DctmReferenceDocument.logger
				.info("Finished exporting dctm mirror dm_document and supporting objects from repository for ID: "
					+ srcObjID);
		}
		return dctmReferenceDocument;
	}

	@Override
	protected void doLoadFrom(DataObject dataObject) {
		super.doLoadFrom(dataObject);

		DataProperty prop = null;

		// Now, restore ("deserialize") the internal state
		prop = dataObject.getProperty("binding_condition");
		this.bindingCondition = prop.getSingleValue().asString();

		prop = dataObject.getProperty("binding_label");
		this.bindingLabel = prop.getSingleValue().asString();

		prop = dataObject.getProperty("reference_by_id");
		this.referenceById = prop.getSingleValue().asString();

		prop = dataObject.getProperty("reference_db_name");
		this.referenceDbName = prop.getSingleValue().asString();
	}

	/**
	 * Gets the reference document attributes from cms.
	 *
	 * @param dctmReferenceDocument
	 *            the dctm reference document
	 * @param prsstntObj
	 *            the prsstnt obj
	 * @param srcObjID
	 *            the src obj id
	 * @throws CMSMFException
	 *             the cMSMF exception
	 */
	private void getReferenceAttributesFromCMS(IDfDocument doc, String srcObjID) throws CMSMFException {
		if (DctmReferenceDocument.logger.isEnabledFor(Level.INFO)) {
			DctmReferenceDocument.logger
				.info("Started retrieving dctm mirror object attributes from repository for object with id: "
					+ srcObjID);
		}
		IDfSession session = doc.getSession();
		try {
			// Set object id
			setSrcObjectID(srcObjID);

			IReference ref = ReferenceFinder.getForMirrorId(new DfId(srcObjID), session);
			final DataType type = DataType.DF_STRING;
			DataObject dataObject = getDataObject();

			DataProperty prop = new DataProperty("binding_condition", type, false, new DfValue(
				this.bindingCondition = ref.getBindingCondition()));
			dataObject.setProperty(prop);

			prop = new DataProperty("binding_label", type, false,
				new DfValue(this.bindingLabel = ref.getBindingLabel()));
			dataObject.setProperty(prop);

			prop = new DataProperty("reference_by_id", type, false, new DfValue(this.referenceById = ref
				.getReferenceById().getId()));
			dataObject.setProperty(prop);

			prop = new DataProperty("reference_db_name", type, false, new DfValue(
				this.referenceDbName = ref.getReferenceDbName()));
			dataObject.setProperty(prop);
		} catch (DfException e) {
			throw (new CMSMFException("Couldn't read all attributes from dctm object with id: " + srcObjID, e));
		}
		if (DctmReferenceDocument.logger.isEnabledFor(Level.INFO)) {
			DctmReferenceDocument.logger
				.info("Finished retrieving dctm mirror object attributes from repository for object with id: "
					+ srcObjID);
		}

	}

	/**
	 * Exports supporting objects of this document. It exports document owner, acl, object type etc.
	 *
	 * @param sysObj
	 *            the sys obj
	 * @throws DfException
	 *             Signals that Dctm Server error has occurred.
	 * @throws CMSMFException
	 *             the cMSMF exception
	 */
	@SuppressWarnings("unused")
	private void exportSupportingObjects(IDfSysObject sysObj) throws DfException, CMSMFException {
		// NOTE: For reference documents, we do not need to export owner and group objects, type,
		// format etc. We only need to export the acl
		IDfSession session = sysObj.getSession();

		// Export the acl
		IDfACL acl = sysObj.getACL();
		DctmObjectExportHelper.serializeACL(session, acl);
	}

	/**
	 * Export parent folders where an document is linked.
	 *
	 * @param dctmDocument
	 *            the dctm document
	 * @param sysObj
	 *            the sys obj
	 * @param srcObjID
	 *            the src obj id
	 * @throws CMSMFException
	 *             the cMSMF exception
	 */
	private void exportParentFolders(DctmReferenceDocument dctmDocument, IDfSysObject sysObj, String srcObjID)
		throws CMSMFException {
		if (DctmReferenceDocument.logger.isEnabledFor(Level.INFO)) {
			DctmReferenceDocument.logger
				.info("Started retrieving parent folders from repository for document with id: " + srcObjID);
		}
		IDfSession session = sysObj.getSession();
		try {
			List<Object> folderIDs = dctmDocument.findAttribute(DctmAttrNameConstants.I_FOLDER_ID).getRepeatingValues();
			for (Object folderID : folderIDs) {
				IDfFolder folder = (IDfFolder) sysObj.getSession().getObject(new DfId((String) folderID));
				dctmDocument.addFolderLocation(folder.getFolderPath(0));

				// Export the folder object
				DctmObjectExportHelper.serializeFolder(session, folder);
			}
		} catch (DfException e) {
			throw (new CMSMFException("Couldn't retrieve related folder objects from repository for object with id: "
				+ srcObjID, e));
		}

		if (DctmReferenceDocument.logger.isEnabledFor(Level.INFO)) {
			DctmReferenceDocument.logger
				.info("Finished retrieving parent folders from repository for document with id: " + srcObjID);
		}
	}

}