package com.delta.cmsmf.cmsobjects;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import com.delta.cmsmf.cfg.Constant;
import com.delta.cmsmf.cfg.Setting;
import com.delta.cmsmf.constants.DctmAttrNameConstants;
import com.delta.cmsmf.exception.CMSMFException;
import com.delta.cmsmf.mainEngine.AbstractCMSMFMain;
import com.delta.cmsmf.mainEngine.DctmObjectExportHelper;
import com.delta.cmsmf.mainEngine.RepositoryConfiguration;
import com.delta.cmsmf.utils.CMSMFUtils;
import com.documentum.com.DfClientX;
import com.documentum.fc.client.IDfACL;
import com.documentum.fc.client.IDfCollection;
import com.documentum.fc.client.IDfDocument;
import com.documentum.fc.client.IDfFolder;
import com.documentum.fc.client.IDfFormat;
import com.documentum.fc.client.IDfPersistentObject;
import com.documentum.fc.client.IDfQuery;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.client.IDfSysObject;
import com.documentum.fc.client.IDfType;
import com.documentum.fc.common.DfException;
import com.documentum.fc.common.DfId;
import com.documentum.fc.common.DfTime;
import com.documentum.fc.common.IDfId;
import com.documentum.fc.common.IDfTime;

/**
 * The DctmDocument class contains methods to export/import dm_document type (or its subtype) of
 * objects from/to Documentum CMS. It also contains methods to export any supporting objects that
 * are needed to replicate a dm_document object in target repository.
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
public class DctmDocument extends DctmObject<IDfDocument> {

	/** The Constant serialVersionUID. */
	private static final long serialVersionUID = 1L;

	// Static variables used to see how many documents were created, skipped, updated
	/** Keeps track of nbr of document versions read from file during import process. */
	private static AtomicInteger docs_read = new AtomicInteger(0);
	/** Keeps track of nbr of document versions skipped due to duplicates during import process. */
	private static AtomicInteger docs_skipped = new AtomicInteger(0);
	/** Keeps track of nbr of document versions updated in CMS during import process. */
	private static AtomicInteger docs_updated = new AtomicInteger(0);
	/** Keeps track of nbr of document versions created in CMS during import process. */
	private static AtomicInteger docs_created = new AtomicInteger(0);

	/** The logger object used for logging. */
	private static final Logger logger = Logger.getLogger(DctmDocument.class);

	/** An ArrayList that holds all of the content rendition files for this document. */
	private List<DctmContent> contentList = new ArrayList<DctmContent>();

	/** The list that contains folder paths where the document is linked. */
	protected List<String> folderLocations = new ArrayList<String>();

	/**
	 * The list that contains all of the version tree. It stores the version in the order from
	 * oldest to newest
	 */
	private List<DctmDocument> versionTree = new ArrayList<DctmDocument>();

	/**
	 * The isThisATest is used for testing purposes. The value for this attribute is set from
	 * properties file. If the value is true, the documents and folders are created in /Replications
	 * cabinet.
	 */
	private static final boolean isThisATest = AbstractCMSMFMain.getInstance().isTestMode();

	protected DctmDocument(DctmObjectType type) {
		super(type, IDfDocument.class);
	}

	/**
	 * Instantiates a new dctm document.
	 */
	public DctmDocument() {
		super(DctmObjectType.DCTM_DOCUMENT, IDfDocument.class);
	}

	/**
	 * Gets the list of content rendition files for this document.
	 *
	 * @return the list of all content renditions
	 */
	public List<DctmContent> getContentList() {
		return this.contentList;
	}

	/**
	 * Adds the content to the content list of this document.
	 *
	 * @param content
	 *            the content
	 */
	public void addContent(DctmContent content) {
		this.contentList.add(content);
	}

	/**
	 * Gets the list of folder locations.
	 *
	 * @return the folder locations
	 */
	protected List<String> getFolderLocations() {
		return this.folderLocations;
	}

	/**
	 * Adds the folder location to the folder location list.
	 *
	 * @param fldrLocation
	 *            the fldr location
	 */
	public void addFolderLocation(String fldrLocation) {
		this.folderLocations.add(fldrLocation);
	}

	/**
	 * Gets the list that contains version tree of this document.
	 *
	 * @return the version tree
	 */
	protected List<DctmDocument> getVersionTree() {
		return this.versionTree;
	}

	/**
	 * Adds the version to the version list of this document.
	 *
	 * @param dctmDoc
	 *            the dctm doc
	 */
	private void addVersion(DctmDocument dctmDoc) {
		this.versionTree.add(dctmDoc);
	}

	/** The implicit version label of this document object. */
	private String implicitVersionLabel;

	/**
	 * Gets the implicit version label.
	 *
	 * @return the implicit version label
	 */
	public String getImplicitVersionLabel() {
		return this.implicitVersionLabel;
	}

	/**
	 * Sets the implicit version label.
	 *
	 * @param implicitVersionLabel
	 *            the new implicit version label
	 */
	public void setImplicitVersionLabel(String implicitVersionLabel) {
		this.implicitVersionLabel = implicitVersionLabel;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.delta.cmsmf.repoSync.DctmObject#createInCMS()
	 */
	@Override
	public void createInCMS(IDfSession session) throws DfException, IOException {

		if (DctmDocument.logger.isEnabledFor(Level.INFO)) {
			DctmDocument.logger.info("Started creating dctm dm_document in repository");
		}

		// Create the version tree in CMS
		createMultiVersionDocument(session);
		if (DctmDocument.logger.isEnabledFor(Level.INFO)) {
			DctmDocument.logger.info("Finished creating dctm dm_document in repository");
		}

	}

	/**
	 * Prints the import report detailing how many document versions were read, updated, created,
	 * skipped during the import process.
	 */
	public static void printImportReport() {
		DctmDocument.logger.info("No. of document object versions read from file: " + DctmDocument.docs_read);
		DctmDocument.logger.info("No. of document object versions skipped due to duplicates: "
			+ DctmDocument.docs_skipped);
		DctmDocument.logger.info("No. of document object versions updated: " + DctmDocument.docs_updated);
		DctmDocument.logger.info("No. of document object versions created: " + DctmDocument.docs_created);
	}

	/**
	 * Gets the detailed document import report.
	 *
	 * @return the detailed document import report
	 */
	public static String getDetailedDocumentImportReport() {
		StringBuffer importReport = new StringBuffer();
		importReport.append("\nNo. of document object versions read from file: " + DctmDocument.docs_read + ".");
		importReport.append("\nNo. of document object versions skipped due to duplicates: " + DctmDocument.docs_skipped
			+ ".");
		importReport.append("\nNo. of document object versions updated: " + DctmDocument.docs_updated + ".");
		importReport.append("\nNo. of document object versions created: " + DctmDocument.docs_created + ".");

		return importReport.toString();
	}

	/**
	 * Creates the multi version document in the repository. It first checks to see if an identical
	 * document already exists in the repository, if not it also checks to see if out dated version
	 * exists and updates it if needed. It also checks to see if there is a need to branching. This
	 * method builds the complete version tree as it existed in the source repository.
	 *
	 * @throws DfException
	 *             Signals that Dctm Server error has occurred.
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	private void createMultiVersionDocument(IDfSession session) throws DfException, IOException {
		// isThisATest = true;
		if (DctmDocument.logger.isEnabledFor(Level.INFO)) {
			DctmDocument.logger.info("Started creating dctm dm_document with multiple versions in repository");
		}

		// Begin transaction
		session.beginTrans();

		// list of objects whose i_is_deleted attribute needs to be updated after
		// creating the version tree.
		List<String> updateIsDeletedObjects = new ArrayList<String>();
		List<String> linkedUnLinkedParentIDs = new ArrayList<String>();
		List<restoreOldACLInfo> restoreACLObjectList = new ArrayList<restoreOldACLInfo>();
		try {
			// This hash map is maintained throughout all of the versions to keep track of newly
// created
			// objects in target repo to corresponding objects in source repo. This information is
// used in
			// building version tree in target repo.
			Map<String, String> oldNewDocuments = new HashMap<String, String>();

			for (DctmDocument dctmVerDoc : getVersionTree()) {
				DctmDocument.docs_read.incrementAndGet();
				if (DctmDocument.logger.isEnabledFor(Level.DEBUG)) {
					DctmDocument.logger.debug("Object name is: "
						+ dctmVerDoc.getStrSingleAttrValue(DctmAttrNameConstants.OBJECT_NAME));
					DctmDocument.logger.debug("Object implicit version label is: "
						+ dctmVerDoc.getImplicitVersionLabel());
				}
				DctmAttribute iAntecedentID = dctmVerDoc.findAttribute(DctmAttrNameConstants.I_ANTECEDENT_ID);
				boolean isRootVersion = false;
				IDfDocument antecedentVersion = null;
				if (iAntecedentID == null) {
					isRootVersion = true;
				} else {
					// find the antecedent object in target repository
					String antecedentVersionID = oldNewDocuments.get(iAntecedentID.getSingleValue());
					if (DctmDocument.logger.isEnabledFor(Level.DEBUG)) {
						DctmDocument.logger.debug("Looking for antecedent id " + antecedentVersionID
							+ " from target repo for id: " + iAntecedentID.getSingleValue());
					}
					antecedentVersion = castPersistentObject(session.getObject(new DfId(antecedentVersionID)));
					if (DctmDocument.logger.isEnabledFor(Level.DEBUG)) {
						DctmDocument.logger.debug("Found antecedent id " + antecedentVersionID
							+ " from target repo for id: " + iAntecedentID.getSingleValue());
					}
				}

				boolean doesDuplicateObjectExists = false;
				boolean branchCreated = false;
				boolean doesExistingObjectNeedsUpdate = false;
				boolean permitChangedFlag = false;
				boolean isImmutableChangedFlag = false;
				int curPermit = 0;

				String objectType = dctmVerDoc.getStrSingleAttrValue(DctmAttrNameConstants.R_OBJECT_TYPE);

				// before creating this document check to see if identical object already exist in
				// CMS
				IDfDocument existingDoc = retrieveIdenticalObjectFromCMS(session, dctmVerDoc);

				if ((existingDoc == null) && (antecedentVersion != null)) {
					// NOTE: If the object name of the version is changed, it will not find the
					// matching object. Check the antecedent version to see if it has any
					// descendants. If it does, try to locate it. Make sure that it has same version
					// label.
					String doesAntecedentHasAnyDescendant = antecedentVersion.getDirectDescendant();
					if (doesAntecedentHasAnyDescendant.equals("T")) {
						// Try to locate the matching document in target repository
						existingDoc = retrieveSimilarDescendantFromCMS(dctmVerDoc, antecedentVersion);
					}
				}

				if (existingDoc == null) {
					// Matching document does not exist in target repository
					if (DctmDocument.logger.isEnabledFor(Level.DEBUG)) {
						DctmDocument.logger.debug("Duplicate object does not exist!");
					}
					// Check if we need to create a new object or checkout/branch old version
					if (isRootVersion) { // Create new object if it is a root version
						existingDoc = castPersistentObject(session.newObject(objectType));
					} else {
						// Checkout or branch the antecedent version
						// First make sure that you have version permissions.
						// If you don't, grant it. Reset it later on.
						int ancstrCurPermit = antecedentVersion.getPermit();
						if (ancstrCurPermit < IDfACL.DF_PERMIT_VERSION) {
							// Grant version permission and save the object
							session.flushCache(false);
							antecedentVersion.fetch(null);
							IDfACL parentFldrACL = antecedentVersion.getACL();
							String aclName = parentFldrACL.getObjectName();
							String aclDomain = parentFldrACL.getDomain();
							int vStamp = antecedentVersion.getVStamp();
							antecedentVersion.grant(session.getLoginUserName(), IDfACL.DF_PERMIT_VERSION, null);
							antecedentVersion.save();
							restoreACLObjectList.add(new restoreOldACLInfo(aclName, aclDomain, antecedentVersion
								.getObjectId().getId(), vStamp));
						}

						String antecedentVersionImplicitVersionLabel = antecedentVersion.getImplicitVersionLabel();
						String dctmVerDocImplicitVersionLabel = dctmVerDoc.getImplicitVersionLabel();

						// Check to see if we need to create a branch or not by checking nbr of '.'
// in the
						// implicit version label compared to the antecedent's implicit version
// label.
						// Branched version will have exactly 2 more dots ('.') in it and it will
// contain
						// antecedent's implicit version label.
						int dotsInAntecedentVersionLabel = StringUtils.countMatches(
							antecedentVersionImplicitVersionLabel, ".");
						int dotsInDctmDocVersionLabel = StringUtils.countMatches(dctmVerDocImplicitVersionLabel, ".");
						if ((dotsInDctmDocVersionLabel == (dotsInAntecedentVersionLabel + 2))
							&& dctmVerDocImplicitVersionLabel.startsWith(antecedentVersionImplicitVersionLabel)) {
							// branch
							if (DctmDocument.logger.isEnabledFor(Level.DEBUG)) {
								DctmDocument.logger.debug("Creating a branch version."
									+ "Antecedent Implicit VrsnLbl: " + antecedentVersionImplicitVersionLabel
									+ " vrsnLbl of current Object: " + dctmVerDocImplicitVersionLabel);
							}
							IDfId branchID = antecedentVersion.branch(antecedentVersionImplicitVersionLabel);
							antecedentVersion = castPersistentObject(session.getObject(branchID));
							// remove branch versionlabel from repeating attributes
							dctmVerDoc.removeRepeatingAttrValue(DctmAttrNameConstants.R_VERSION_LABEL,
								dctmVerDocImplicitVersionLabel);
							branchCreated = true;
							// If branching document object, make sure that you have write
							// permissions.
							// If you don't, grant it. The ACL will be updated later on.
							curPermit = antecedentVersion.getPermit();
							if (curPermit < IDfACL.DF_PERMIT_WRITE) {
								// Grant write permission and save the object
								antecedentVersion.grant(session.getLoginUserName(), IDfACL.DF_PERMIT_WRITE, null);
								antecedentVersion.save();
								// We don't need to change the permit later on since acl attributes
								// will be saved later on.
								// permitChangedFlag = true;
							}
						} else {
							if (DctmDocument.logger.isEnabledFor(Level.DEBUG)) {
								DctmDocument.logger.debug("Checking out a version." + "Antecedent Implicit VrsnLbl: "
									+ antecedentVersionImplicitVersionLabel + " vrsnLbl of current Object: "
									+ dctmVerDocImplicitVersionLabel);
							}
							// checkout
							antecedentVersion.checkout();
							// antecedentVersion.checkoutEx(antecedentVersionImplicitVersionLabel,
							// "", "");
						}
						existingDoc = antecedentVersion;
					}
				} // if (existingDoc == null)
				else {
					// Matching document exist in target repository. Compare modify dates to see if
					// we need to
					// update the document in repo or skip this document.
					if (DctmDocument.logger.isEnabledFor(Level.DEBUG)) {
						DctmDocument.logger.debug("Possible duplicate object DOES exist!");
					}
					// Check modify date to see if we need to update the document in CMS
					if (existingDoc.getModifyDate().getDate()
						.equals(dctmVerDoc.getDateSingleAttrValue(DctmAttrNameConstants.R_MODIFY_DATE))) {
						// duplicate document already exist in target CMS, add the object id in map
						// and return
						doesDuplicateObjectExists = true;
						DctmDocument.docs_skipped.incrementAndGet();
						if (DctmDocument.logger.isEnabledFor(Level.DEBUG)) {
							DctmDocument.logger
								.debug("Identical version of document already exist in target repo with id: "
									+ existingDoc.getObjectId().getId() + " and name: " + existingDoc.getObjectName());
						}
					} else {
						doesExistingObjectNeedsUpdate = true;
						// If updating an existing document object, make sure that you have write
						// permissions. If you don't, grant it. Reset it later on.
						curPermit = existingDoc.getPermit();
						if (curPermit < IDfACL.DF_PERMIT_WRITE) {
							// Grant write permission and save the object
							existingDoc.grant(session.getLoginUserName(), IDfACL.DF_PERMIT_WRITE, null);
							existingDoc.save();
							permitChangedFlag = true;
						}

						// Check the immutable flag of the document version object. Set to False if
						// it is not and revert it back later.
						if (existingDoc.isImmutable()) {
							existingDoc.setBoolean(DctmAttrNameConstants.R_IMMUTABLE_FLAG, false);
							existingDoc.save();
							isImmutableChangedFlag = true;
						}
					}
				}
				// update/create new document if duplicate doesn't exist
				if (!doesDuplicateObjectExists) {

					// Set content files
					DctmDocument.setContentFilesInCMS(existingDoc, dctmVerDoc);

					// Remove existing links and then Link the document appropriately
					// NOTE If we do not remove the existing links and try to link the document to
					// same location, you get dfc exception
					List<String> unLinkedParentIDs = DctmObject.removeAllLinks(existingDoc);
					for (String unlinkedParentID : unLinkedParentIDs) {
						if (!linkedUnLinkedParentIDs.contains(unlinkedParentID)) {
							linkedUnLinkedParentIDs.add(unlinkedParentID);
						}
					}
					List<String> folderLocations = dctmVerDoc.getFolderLocations();
					for (String fldrLoc : folderLocations) {
						if (DctmDocument.isThisATest) {
							fldrLoc = "/Replications" + fldrLoc;
						}
						if (DctmDocument.logger.isEnabledFor(Level.DEBUG)) {
							DctmDocument.logger.debug("Linking the document in folder " + fldrLoc);
						}
						// Try to link the document to parent, if the document is already linked to
// the
						// specified parent, DFC will throw DfException with MessageID
						// DM_SYSOBJECT_E_ALREADY_LINKED. If it does throw this error, catch it and
// ignore it,
						// if exception is other than that, throw it.
						try {
							existingDoc.link(fldrLoc);
							// Add the object id of parent folders where we are trying to link the
// object in
							// the list
							// which we need to check the permissions
							IDfFolder parentFldr = session.getFolderByPath(fldrLoc);
							if (!linkedUnLinkedParentIDs.contains(parentFldr.getObjectId().getId())) {
								linkedUnLinkedParentIDs.add(parentFldr.getObjectId().getId());
							}
						} catch (DfException dfe) {
							if (dfe.getMessageId().equals(Constant.DM_SYSOBJECT_E_ALREADY_LINKED_MESSAGE_ID)) {
								if (DctmDocument.logger.isEnabledFor(Level.DEBUG)) {
									DctmDocument.logger.debug("Sysobject already Linked error ignored");
								}
							} else {
								throw (dfe);
							}
						}
					}

					for (String parentFldrID : linkedUnLinkedParentIDs) {
						// Check the permit on all of the parent folders from where you are
// unlinking or
						// linking.
						IDfSysObject parentFldr = (IDfSysObject) session.getObject(new DfId(parentFldrID));

						// if permit is less than write, modify the acl temporarily.
						if (parentFldr.getPermit() < IDfACL.DF_PERMIT_WRITE) {
							// Flush and ReFatch the parent folder
							session.flush("persistentobjcache", null);
							session.flushObject(parentFldr.getObjectId());
							session.flushCache(false);
							parentFldr = (IDfSysObject) session.getObject(new DfId(parentFldrID));
							parentFldr.fetch(null);
							IDfACL parentFldrACL = parentFldr.getACL();
							String aclName = parentFldrACL.getObjectName();
							String aclDomain = parentFldrACL.getDomain();
							int vStamp = parentFldr.getVStamp();
							if (DctmDocument.logger.isEnabledFor(Level.DEBUG)) {
								DctmDocument.logger.debug("Permission for parent folder "
									+ parentFldr.getObjectId().getId()
									+ " needs to be modified. Current info about parent folder is: acl name: "
									+ aclName + " acl domain: " + aclDomain + " vStamp: " + vStamp);
							}
							parentFldr.grant(session.getLoginUserName(), IDfACL.DF_PERMIT_WRITE, null);
							parentFldr.save();
							restoreACLObjectList.add(new restoreOldACLInfo(aclName, aclDomain, parentFldr.getObjectId()
								.getId(), vStamp));
						}
					}

					// Set all of the attributes of the new document or existing document from a
// read object
					// If root version, update version label otherwise not
					if (isRootVersion) {
						setAllAttributesInCMS(existingDoc, dctmVerDoc, true, doesExistingObjectNeedsUpdate);
					} else {
						setAllAttributesInCMS(existingDoc, dctmVerDoc, false, doesExistingObjectNeedsUpdate);
					}

					// save or checkin document accordingly
					if (branchCreated || isRootVersion || doesExistingObjectNeedsUpdate) {
						existingDoc.save();
						// Revert back the r_immutable_flag if it was modified previously
						if (isImmutableChangedFlag) {
							existingDoc.setBoolean(DctmAttrNameConstants.R_IMMUTABLE_FLAG, true);
							existingDoc.save();
						}
						// Revert back the permission on the document if it was modified previously
						if (permitChangedFlag) {
							existingDoc.grant(session.getLoginUserName(), curPermit, null);
							existingDoc.save();
						}
						if (DctmDocument.logger.isEnabledFor(Level.DEBUG)) {
							DctmDocument.logger.debug("Saved the document with id: "
								+ existingDoc.getObjectId().getId() + " and name: " + existingDoc.getObjectName());
						}
					} else {
						// otherwise checkin

						// NOTE Before setting version label check to see if the implicit version is
// in
						// x.x.x.x...
						// format. If it is, remove it before checking in otherwise dfexception is
// thrown.
						String dctmVerDocImplicitVersionLabel = dctmVerDoc.getImplicitVersionLabel();
						int dotsInDctmDocVersionLabel = StringUtils.countMatches(dctmVerDocImplicitVersionLabel, ".");

						if (dotsInDctmDocVersionLabel >= 3) {
							dctmVerDoc.removeRepeatingAttrValue(DctmAttrNameConstants.R_VERSION_LABEL,
								dctmVerDocImplicitVersionLabel);

						}
						// Check in the new version
						String versionLabels = dctmVerDoc.findAttribute(DctmAttrNameConstants.R_VERSION_LABEL)
							.getRepeatingValuesAsCommaSeparatedString();
						if (DctmDocument.logger.isEnabledFor(Level.DEBUG)) {
							DctmDocument.logger.debug("Setting version labels to: " + versionLabels);
							DctmDocument.logger.debug("Checking in the document with id: "
								+ existingDoc.getObjectId().getId() + " and name: " + existingDoc.getObjectName());
						}
						IDfId newVersionID = existingDoc.checkin(false, versionLabels);
						existingDoc = castPersistentObject(session.getObject(newVersionID));
						if (DctmDocument.logger.isEnabledFor(Level.DEBUG)) {
							DctmDocument.logger.debug("The new version id is " + existingDoc.getObjectId().getId()
								+ " and name: " + existingDoc.getObjectName());
						}
					}

					if (doesExistingObjectNeedsUpdate) {
						DctmDocument.docs_updated.incrementAndGet();
					} else {
						DctmDocument.docs_created.incrementAndGet();
					}

					// Update internal attributes like creation/modify date and creators/modifiers
					updateSystemAttributes(existingDoc, dctmVerDoc);

					// Update set_file, set_client and set_time attributes of newly added content.
					updateContentAttributes(existingDoc, dctmVerDoc);
				} // if (!doesDuplicateObjectExists)

				// Check to see if we need to update i_is_deleted attribute to True in target repo
				if (dctmVerDoc.getBoolSingleAttrValue(DctmAttrNameConstants.I_IS_DELETED)) {
					// If the object in source repo is marked as deleted but not the corresponding
// one
					// in target, add target object id in the list of objects whose i_is_deleted
// will
					// updated to true later on.
					if (!existingDoc.isDeleted() && !updateIsDeletedObjects.contains(existingDoc.getObjectId().getId())) {
						updateIsDeletedObjects.add(existingDoc.getObjectId().getId());
						if (DctmDocument.logger.isEnabledFor(Level.DEBUG)) {
							DctmDocument.logger.debug("I_IS_DELETED need to be set to true in target repo with id: "
								+ existingDoc.getObjectId().getId() + " and name: " + existingDoc.getObjectName());
						}
					}
				}

				// Store source repo object id and target repo objectid in a map to build up version
// history
				// in target repo identical to source repo.
				oldNewDocuments.put(dctmVerDoc.getSrcObjectID(), existingDoc.getObjectId().getId());
				if (DctmDocument.logger.isEnabledFor(Level.INFO)) {
					DctmDocument.logger.info("Created/Located document with id " + existingDoc.getObjectId().getId()
						+ " in target repo for object id " + dctmVerDoc.getSrcObjectID() + " of source repo.");
				}

			} // for (DctmDocument dctmVerDoc : getVersionTree())

			// Restore ACL of any parent folders that was changed during the import process.
			restoreACLOfParentFolders(session, restoreACLObjectList);

			// Update i_is_deleted attribute of documents if needed.
			if (!updateIsDeletedObjects.isEmpty()) {
				updateIsDeletedAttribute(session, updateIsDeletedObjects);
			}

			if (DctmDocument.logger.isEnabledFor(Level.INFO)) {
				DctmDocument.logger.info("Finished creating dctm dm_document with multiple versions in repository");
			}
		} catch (DfException e) {
			// Abort the transaction in case of DfException
			session.abortTrans();
			throw (e);
		}

		// Commit the transaction
		session.commitTrans();
	}

	private IDfDocument retrieveSimilarDescendantFromCMS(DctmDocument dctmVerDoc, IDfSysObject antecedentVersion) {
		if (DctmDocument.logger.isEnabledFor(Level.INFO)) {
			DctmDocument.logger.info("Started retrieving similar descendant version document from target cms.");
		}
		IDfSession session = antecedentVersion.getSession();

		StringBuffer objLookUpQry = new StringBuffer(50);
		try {
			String antecendentVersionId = antecedentVersion.getObjectId().getId();
			String srcObjVersionLabel = dctmVerDoc.implicitVersionLabel;
			String objectType = dctmVerDoc.getStrSingleAttrValue(DctmAttrNameConstants.R_OBJECT_TYPE);
			// Date creationDate =
// dctmVerDoc.getDateSingleAttrValue(DctmAttrNameConstants.R_CREATION_DATE);
			// String dctmDateTimePattern = Constant.DCTM_DATETIME_PATTERN;
			// IDfTime createDate = new DfTime(creationDate);

			// NOTE: MODIFY THIS NOTE

			// Build a query for ex: " dm_document where i_antecedent_id='XXXX' and any
// r_version_label
			// ='XXXX';
			objLookUpQry.append(objectType);
			objLookUpQry.append(" (ALL) where i_antecedent_id='");
			objLookUpQry.append(antecendentVersionId);
			objLookUpQry.append("' and any r_version_label='");
			objLookUpQry.append(srcObjVersionLabel);
			objLookUpQry.append("'");
			// objLookUpQry.append("') and r_creation_date=DATE('");
			// objLookUpQry.append(createDate.asString(dctmDateTimePattern));
			// objLookUpQry.append("')");
			if (DctmDocument.logger.isEnabledFor(Level.DEBUG)) {
				DctmDocument.logger.debug("Query to lookup descendant version is: " + objLookUpQry.toString());
			}
			// Retrieve the object using the query
			return castPersistentObject(session.getObjectByQualification(objLookUpQry.toString()));
		} catch (DfException e) {
			DctmDocument.logger.error("Lookup of object failed with query: " + objLookUpQry, e);
			return null;
		}
	}

	/**
	 * Tries to retrieve identical object from cms.
	 *
	 * @param dctmVerDoc
	 *            the dctm ver doc
	 * @return the i df sys object
	 */
	private IDfDocument retrieveIdenticalObjectFromCMS(IDfSession session, DctmDocument dctmVerDoc) {

		if (DctmDocument.logger.isEnabledFor(Level.INFO)) {
			DctmDocument.logger.info("Started retrieving Identical version document from target cms.");
		}
		String objectName = dctmVerDoc.getStrSingleAttrValue(DctmAttrNameConstants.OBJECT_NAME);
		// If object name contains single quote, replace it with 2 single quotes for DQL
		objectName = objectName.replaceAll("'", "''");

		String objectType = dctmVerDoc.getStrSingleAttrValue(DctmAttrNameConstants.R_OBJECT_TYPE);
		Date creationDate = dctmVerDoc.getDateSingleAttrValue(DctmAttrNameConstants.R_CREATION_DATE);
		String dctmDateTimePattern = Constant.DCTM_DATETIME_PATTERN;
		IDfTime createDate = new DfTime(creationDate);
		String fldrLoc = "";
		if (dctmVerDoc.folderLocations.size() >= 1) {
			fldrLoc = dctmVerDoc.folderLocations.get(0);
		}
		if (DctmDocument.isThisATest) {
			fldrLoc = "/Replications" + fldrLoc;
		}
		// If folder location contains single quote, replace it with 2 single quotes for DQL
		fldrLoc = fldrLoc.replaceAll("'", "''");

		// NOTE: Check the i_is_deleted flag. If the object is marked as deleted, the dql will never
		// find the matching document. Marked as deleted objects need to be queried by going
// directly
		// against the dm_sysobject_s table. When you use dm_sysobject_s table in from clause, you
// can't
		// use folder predicate. Find out the r_object_id of the /Temp cabinet and use it in the
// query.

		boolean isDeleted = dctmVerDoc.getBoolSingleAttrValue(DctmAttrNameConstants.I_IS_DELETED);
		StringBuffer objLookUpQry = new StringBuffer(50);
		try {
			if (isDeleted) {
				if (DctmDocument.logger.isEnabledFor(Level.DEBUG)) {
					DctmDocument.logger.debug("Document is marked as deleted: " + objectName);
				}
				IDfFolder tempCabinet = session.getFolderByPath("/Temp");
				String tempCabinetId = tempCabinet.getObjectId().getId();
				objLookUpQry.append("dm_sysobject_s where object_name='");
				objLookUpQry.append(objectName);
				objLookUpQry.append("' and i_cabinet_id='");
				objLookUpQry.append(tempCabinetId);
				objLookUpQry.append("' and r_creation_date=DATE('");
				objLookUpQry.append(createDate.asString(dctmDateTimePattern));
				objLookUpQry.append("')");

			} else {
				// Build a query for ex: " dm_document where object_name='xxx' and
				// folder('/xxx/xxx') and r_creation_date=DATE('xxxxxx')
				objLookUpQry.append(objectType);
				objLookUpQry.append(" (ALL) where object_name='");
				objLookUpQry.append(objectName);
				objLookUpQry.append("' and folder('");
				objLookUpQry.append(fldrLoc);
				objLookUpQry.append("') and r_creation_date=DATE('");
				objLookUpQry.append(createDate.asString(dctmDateTimePattern));
				objLookUpQry.append("')");
			}
			if (DctmDocument.logger.isEnabledFor(Level.DEBUG)) {
				DctmDocument.logger.debug("Query to lookup duplicate document is: " + objLookUpQry.toString());
			}

			// Retrieve the object using the query
			return castPersistentObject(session.getObjectByQualification(objLookUpQry.toString()));
		} catch (DfException e) {
			DctmDocument.logger.error("Lookup of object failed with query: " + objLookUpQry, e);
			return null;
		}

	}

	/**
	 * Sets all content renditions of an sysobject in repository from content list of dctm document.
	 *
	 * @param sysObject
	 *            the sys object
	 * @param dctmDoc
	 *            the dctm doc
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 * @throws DfException
	 *             Signals that DFC Exception has occurred.
	 */
	private static void setContentFilesInCMS(IDfSysObject sysObject, DctmDocument dctmDoc) throws IOException,
		DfException {
		if (DctmDocument.logger.isEnabledFor(Level.INFO)) {
			DctmDocument.logger.info("Started setting content files of document with name: "
				+ sysObject.getObjectName());
		}

		String aContentType = dctmDoc.getStrSingleAttrValue(DctmAttrNameConstants.A_CONTENT_TYPE);

		List<DctmContent> contentList = dctmDoc.getContentList();
		File contentExportRootDir = AbstractCMSMFMain.getInstance().getContentFilesDirectory();
		for (DctmContent dctmContent : contentList) {
			File contentFullPath = new File(contentExportRootDir, dctmContent.getRelativeContentFileLocation());
			if (DctmDocument.logger.isEnabledFor(Level.DEBUG)) {
				DctmDocument.logger.debug("Content File Location in fileSystem is: "
					+ contentFullPath.getAbsolutePath());
			}
			if (dctmContent.getIntSingleAttrValue(DctmAttrNameConstants.RENDITION) == 0) {
				String dmrContentFullFormat = dctmContent.getStrSingleAttrValue(DctmAttrNameConstants.FULL_FORMAT);
				if (!(dmrContentFullFormat.equals(aContentType))) {
					DctmDocument.logger.warn("The value " + aContentType
						+ "  in a_content_type attr does not match with value " + dmrContentFullFormat
						+ " in full_format attr of corresponding contentObject");

					dmrContentFullFormat = aContentType;
					dctmContent.findAttribute(DctmAttrNameConstants.FULL_FORMAT).setSingleValue(dmrContentFullFormat);
				}
				sysObject.setFileEx(contentFullPath.getAbsolutePath(), dmrContentFullFormat, dctmContent.getPageNbr(),
					null);
			} else {
				sysObject.addRenditionEx2(contentFullPath.getAbsolutePath(),
					dctmContent.getStrSingleAttrValue(DctmAttrNameConstants.FULL_FORMAT), dctmContent.getPageNbr(),
					dctmContent.getPageModifier(), null, false, false, false);
			}
		}
		if (DctmDocument.logger.isEnabledFor(Level.INFO)) {
			DctmDocument.logger.info("Finished setting content files of document with name: "
				+ sysObject.getObjectName());
		}

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.delta.cmsmf.cmsobjects.DctmObject#getFromCMS(com.documentum.fc.client.IDfPersistentObject
	 * )
	 */
	@Override
	protected DctmDocument doGetFromCMS(IDfDocument doc) throws CMSMFException {
		return getDctmDocument(doc, false);
	}

	/**
	 * Gets the dctm document.
	 *
	 * @param prsstntObj
	 *            the sys obj
	 * @param isVersionBeingProcessed
	 *            the is version being processed
	 * @return the dctm document
	 * @throws CMSMFException
	 *             the cMSMF exception
	 */
	private DctmDocument getDctmDocument(IDfDocument doc, boolean isVersionBeingProcessed) throws CMSMFException {
		if (DctmDocument.logger.isEnabledFor(Level.INFO)) {
			DctmDocument.logger.info("Started exporting dctm dm_document and supporting objects from repository");
		}

		DctmDocument dctmDocument = new DctmDocument();
		String srcObjID = null;
		try {
			srcObjID = doc.getObjectId().getId();

			// Get all of the attributes
			dctmDocument.getAllAttributesFromCMS(doc, srcObjID);

			// Update ACL Domain attribute value if needed
			// No need to do this here anymore, it is handled in getAllAttributesFromCMS() itself.
			// updateACLDomainAttribute(dctmDocument);

			// Set implicit version label
			dctmDocument.setImplicitVersionLabel(doc.getImplicitVersionLabel());

			// Set content files
			// NOTE The content files should only be retrieved if you are processing versions. Not
			// when this method is called from prepareObject().
			if (isVersionBeingProcessed) {
				getContentFilesFromCMS(dctmDocument, doc, srcObjID);
			}

			// Export other supporting objects
			exportSupportingObjects(doc);

			// Process folders and write where this document is linked
			dctmDocument.exportParentFolders(doc, srcObjID);

			// If processing a version, do not call getVersions method otherwise it will end up in
			// never ending loop. You want to call this method only the 1st time the
			// prepareDctmDocument() method is called from prepareObject() method.
			if (!isVersionBeingProcessed) {
				// Set all of the versions of the document
				getVersions(dctmDocument, doc, srcObjID);
			}

		} catch (DfException e) {
			throw (new CMSMFException("Couldn't retrieve object from cms with id: " + srcObjID, e));
		}

		if (DctmDocument.logger.isEnabledFor(Level.INFO)) {
			DctmDocument.logger
				.info("Finished exporting dctm dm_document and supporting objects from repository for ID: " + srcObjID);
		}
		return dctmDocument;
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
	private void exportSupportingObjects(IDfSysObject sysObj) throws DfException, CMSMFException {
		IDfSession session = sysObj.getSession();
		// Export document owner
		String owner = sysObj.getOwnerName();
		DctmObjectExportHelper.serializeUserOrGroupByName(session, owner);

		// Export document group name
		String groupName = sysObj.getGroupName();
		DctmObjectExportHelper.serializeUserOrGroupByName(session, groupName);

		// Export the acl
		IDfACL acl = sysObj.getACL();
		DctmObjectExportHelper.serializeACL(session, acl);

		// Export the object type
		IDfType objType = sysObj.getType();
		DctmObjectExportHelper.serializeType(session, objType);

		// Export the format object
		IDfFormat format = sysObj.getFormat();
		DctmObjectExportHelper.serializeFormat(session, format);

		// Record the file store name in source repo configuration object
		String aStorageType = sysObj.getStorageType();
		if (StringUtils.isNotBlank(aStorageType)) {
			RepositoryConfiguration.getRepositoryConfiguration().addFileStore(aStorageType);
		}
	}

	/**
	 * Gets all of the versions of an document from repository and builds the version tree.
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
	private void getVersions(DctmDocument dctmDocument, IDfSysObject sysObj, String srcObjID) throws CMSMFException {
		if (DctmDocument.logger.isEnabledFor(Level.INFO)) {
			DctmDocument.logger.info("Started retrieving all versions from repository for document with id: "
				+ srcObjID);
		}
		IDfSession session = sysObj.getSession();
		IDfQuery dqlQry = new DfClientX().getQuery();
		try {
			String chronicalID = sysObj.getChronicleId().getId();
			StringBuffer versionsQry = new StringBuffer(
				"Select distinct r_object_id, r_creation_date from dm_sysobject_s where i_chronicle_id = '");
			versionsQry.append(chronicalID);
			// NOTE Previously the versions were retrieved in order by r_object_id but it was
			// discovered that in cobsi docbase we had some objects who had their chronicle ids
			// higher than their own object id. It is believed that Dump n Load may have created the
			// versioned objects in reverse order (from newer to older) instead of (older to newer).
			// Hence the query looks up in the order by creation date.
			// versionsQry.append("' order by r_object_id");
			versionsQry.append("' order by r_creation_date");
			if (DctmDocument.logger.isEnabledFor(Level.INFO)) {
				DctmDocument.logger.info("The versions query is: " + versionsQry);
			}
			dqlQry.setDQL(versionsQry.toString());
			IDfCollection resultCol = dqlQry.execute(sysObj.getSession(), IDfQuery.READ_QUERY);
			int versionCnt = 0;
			while (resultCol.next()) {
				String versionID = resultCol.getId("r_object_id").getId();
				if (DctmDocument.logger.isEnabledFor(Level.DEBUG)) {
					DctmDocument.logger.debug("Retrieving older version with id: " + versionID);
				}
				IDfDocument verObj = castPersistentObject(session.getObject(new DfId(versionID)));
				DctmDocument verDctmDoc = getDctmDocument(verObj, true);
				dctmDocument.addVersion(verDctmDoc);
				versionCnt++;
			}
			if (DctmDocument.logger.isEnabledFor(Level.DEBUG)) {
				DctmDocument.logger.debug("Versions query is: " + versionsQry + " No of versions found: " + versionCnt);
			}
			resultCol.close();
		} catch (DfException e) {
			throw (new CMSMFException("Couldn't retrieve version objects for document with ID: " + srcObjID, e));
		}
		if (DctmDocument.logger.isEnabledFor(Level.INFO)) {
			DctmDocument.logger.info("Finished retrieving all versions from repository for document with id: "
				+ srcObjID);
		}

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
	private void exportParentFolders(IDfSysObject sysObj, String srcObjID) throws CMSMFException {
		if (DctmDocument.logger.isEnabledFor(Level.INFO)) {
			DctmDocument.logger.info("Started retrieving parent folders from repository for document with id: "
				+ srcObjID);
		}
		IDfSession session = sysObj.getSession();
		try {
			List<Object> folderIDs = findAttribute(DctmAttrNameConstants.I_FOLDER_ID).getRepeatingValues();
			for (Object folderID : folderIDs) {
				IDfFolder folder = (IDfFolder) sysObj.getSession().getObject(new DfId((String) folderID));
				addFolderLocation(folder.getFolderPath(0));

				// Export the folder object
				DctmObjectExportHelper.serializeFolder(session, folder);
			}
		} catch (DfException e) {
			throw (new CMSMFException("Couldn't retrieve related folder objects from repository for object with id: "
				+ srcObjID, e));
		}

		if (DctmDocument.logger.isEnabledFor(Level.INFO)) {
			DctmDocument.logger.info("Finished retrieving parent folders from repository for document with id: "
				+ srcObjID);
		}
	}

	/**
	 * Gets the content files from repository for an object and sets as the content file list of an
	 * dctm object.
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
	@SuppressWarnings("unused")
	@Deprecated
	private void getContentFilesFromCMS_old(DctmDocument dctmDocument, IDfSysObject sysObj, String srcObjID)
		throws CMSMFException {
		if (DctmDocument.logger.isEnabledFor(Level.INFO)) {
			DctmDocument.logger.info("Started retrieving content files from repository for document with id: "
				+ srcObjID);
		}

		IDfSession session = sysObj.getSession();
		try {
			int pageCnt = sysObj.getPageCount();
			if (DctmDocument.logger.isEnabledFor(Level.DEBUG)) {
				DctmDocument.logger.debug("object with id " + srcObjID + " has page count: " + pageCnt);
			}
			for (int i = 0; i < pageCnt; i++) {
				// Run a query to find out all dmr_content objects linked to the sysobject
				StringBuffer contentDQLBuffer = new StringBuffer(
					"select dcs.r_object_id, dcr.parent_id, dcs.full_format, dcr.page, dcr.page_modifier, dcs.rendition, ");
				contentDQLBuffer
					.append("dcs.content_size, dcs.set_file, dcs.set_time, dcs.set_client, dcs.data_ticket ");
				contentDQLBuffer.append("from dmr_content_r  dcr, dmr_content_s dcs ");
				contentDQLBuffer.append("where dcr.parent_id = '" + sysObj.getObjectId().getId() + "' ");
				contentDQLBuffer.append("and dcr.r_object_id = dcs.r_object_id ");
				contentDQLBuffer.append("and page = ");
				contentDQLBuffer.append(i);
				contentDQLBuffer.append(" order by rendition");

				if (DctmDocument.logger.isEnabledFor(Level.DEBUG)) {
					DctmDocument.logger.debug("DQL Query to locate content is: " + contentDQLBuffer.toString());
				}
				IDfQuery contentQuery = new DfClientX().getQuery();
				contentQuery.setDQL(contentDQLBuffer.toString());
				IDfCollection contentColl = contentQuery.execute(session, IDfQuery.READ_QUERY);
				while (contentColl.next()) {
					DctmContent dctmContent = new DctmContent();
					// Set various attributes of dctmContent object
					// dctmContent.setContentFormat(contentColl.getString(DctmAttrNameConstants.FULL_FORMAT));
					dctmContent.setPageNbr(contentColl.getInt(DctmAttrNameConstants.PAGE));
					dctmContent.setPageModifier(contentColl.getString(DctmAttrNameConstants.PAGE_MODIFIER));
					// dctmContent.setSetFile(contentColl.getString(DctmAttrNameConstants.SET_FILE));
					// dctmContent.setSetClient(contentColl.getString(DctmAttrNameConstants.SET_CLIENT));
					// dctmContent.setSetTime(contentColl.getTime(DctmAttrNameConstants.SET_TIME).getDate());
					// dctmContent.setRenditionNbr(contentColl.getInt(DctmAttrNameConstants.RENDITION));
					// int contentDataTicket =
// contentColl.getInt(DctmAttrNameConstants.DATA_TICKET);
					// dctmContent.setRelativeContentFileLocation(getContent(sysObj,
					// contentColl.getId("r_object_id")
					// .getId(), dctmContent.getContentFormat(), dctmContent.getPageNbr(),
// dctmContent
					// .getPageModifier(), contentDataTicket));
					dctmDocument.addContent(dctmContent);
				}
				contentColl.close();
			}
		} catch (DfException e) {
			throw (new CMSMFException("Couldn't retrieve all content files from dctm document with id: " + srcObjID, e));
			// } catch (IOException e) {
			// throw (new
// CMSMFException("Couldn't read content file streams from dctm document with id: " +
			// srcObjID, e));
		}

		if (DctmDocument.logger.isEnabledFor(Level.INFO)) {
			DctmDocument.logger.info("Finished retrieving content files from repository for document with id: "
				+ srcObjID);
		}
	}

	/**
	 * Gets the content files from repository for an object and sets as the content file list of an
	 * dctm object.
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
	private void getContentFilesFromCMS(DctmDocument dctmDocument, IDfSysObject sysObj, String srcObjID)
		throws CMSMFException {
		if (DctmDocument.logger.isEnabledFor(Level.INFO)) {
			DctmDocument.logger.info("Started retrieving content files from repository for document with id: "
				+ srcObjID);
		}

		IDfSession session = sysObj.getSession();

		try {
			int pageCnt = sysObj.getPageCount();
			if (DctmDocument.logger.isEnabledFor(Level.DEBUG)) {
				DctmDocument.logger.debug("object with id " + srcObjID + " has page count: " + pageCnt);
			}
			for (int i = 0; i < pageCnt; i++) {
				// Run a query to find out all dmr_content objects linked to the sysobject for given
// page_cnt
				StringBuffer contentDQLBuffer = new StringBuffer(
					"select dcs.r_object_id, dcr.page, dcr.page_modifier from dmr_content_r  dcr, dmr_content_s dcs ");
				contentDQLBuffer.append("where dcr.parent_id = '" + sysObj.getObjectId().getId() + "' ");
				contentDQLBuffer.append("and dcr.r_object_id = dcs.r_object_id ");
				contentDQLBuffer.append("and page = ");
				contentDQLBuffer.append(i);
				contentDQLBuffer.append(" order by rendition");

				if (DctmDocument.logger.isEnabledFor(Level.DEBUG)) {
					DctmDocument.logger.debug("DQL Query to locate content is: " + contentDQLBuffer.toString());
				}
				IDfQuery contentQuery = new DfClientX().getQuery();
				contentQuery.setDQL(contentDQLBuffer.toString());
				IDfCollection contentColl = contentQuery.execute(session, IDfQuery.READ_QUERY);
				while (contentColl.next()) {
					IDfPersistentObject contentObject = session.getObject(contentColl
						.getId(DctmAttrNameConstants.R_OBJECT_ID));
					DctmContent dctmContent = new DctmContent();
					dctmContent = (DctmContent) dctmContent.getFromCMS(contentObject);
					dctmContent.setPageNbr(contentColl.getInt(DctmAttrNameConstants.PAGE));
					dctmContent.setPageModifier(contentColl.getString(DctmAttrNameConstants.PAGE_MODIFIER));
					dctmContent.setRelativeContentFileLocation(getContent(sysObj, dctmContent));
					dctmDocument.addContent(dctmContent);
				}
				contentColl.close();
			}
		} catch (DfException e) {
			throw (new CMSMFException("Couldn't retrieve all content files from dctm document with id: " + srcObjID, e));
		} catch (IOException e) {
			throw (new CMSMFException("Couldn't read content file streams from dctm document with id: " + srcObjID, e));
		}

		if (DctmDocument.logger.isEnabledFor(Level.INFO)) {
			DctmDocument.logger.info("Finished retrieving content files from repository for document with id: "
				+ srcObjID);
		}
	}

	/**
	 * Gets the content from cms using getfile.
	 *
	 * @param sysObj
	 *            the sys obj
	 * @param dctmContent
	 *            the dctm content object
	 * @return the content
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 * @throws DfException
	 *             the df exception
	 */
	private String getContent(IDfSysObject sysObj, DctmContent dctmContent) throws IOException, DfException {

		String contentObjID = dctmContent.getSrcObjectID();
		String contentFormat = dctmContent.getStrSingleAttrValue(DctmAttrNameConstants.FULL_FORMAT);
		int pageNbr = dctmContent.getPageNbr();
		String pageModifier = dctmContent.getPageModifier();
		if (DctmDocument.logger.isEnabledFor(Level.DEBUG)) {
			DctmDocument.logger
				.debug("Started getting content file to filesystem. <contentID, format, pageNbr, pageModifier, dataTicket> : <"
					+ contentObjID + ", " + contentFormat + ", " + pageNbr + ", " + pageModifier + ">");
		}
		// NOTE smakim: I tried using getContentEx2 method of IDfSysObject to get the
// ByteArrayInputStream
		// but ran into out of memory (OutOfMemoryError: Java heap space) for large documents. So,
// now
		// using getFileEx2 instead.
		@SuppressWarnings("unused")
		int bufferSize = Setting.CONTENT_READ_BUFFER_SIZE.getInt();
		String contentExportRootDir = Setting.CONTENT_DIRECTORY.getString();
		// Make sure the content export location exists
		FileUtils.forceMkdir(new File(contentExportRootDir));
		String relativeContentFileLocation = CMSMFUtils.getContentPathFromContentID(contentObjID);

		// Make sure that the content file folder location exists
		FileUtils.forceMkdir(new File(contentExportRootDir + File.separator + relativeContentFileLocation));

		// Prepare the file name for the content
		String contentFileName = contentObjID + "_" + contentFormat + "_" + pageNbr;
		if (StringUtils.isNotBlank(pageModifier)) {
			contentFileName = contentFileName + "_" + pageModifier;
		}

		// Prepare the full filesystem file name
		String fullFileSystemFileName = contentExportRootDir + File.separator + relativeContentFileLocation
			+ File.separator + contentFileName;

		// Get the content files
		sysObj.getFileEx2(fullFileSystemFileName, contentFormat, pageNbr, pageModifier, false);
		if (DctmDocument.logger.isEnabledFor(Level.DEBUG)) {
			DctmDocument.logger.debug("Finished getting content file to filesystem. Relative file location is: "
				+ relativeContentFileLocation + File.separator + contentFileName);
		}
		return relativeContentFileLocation + File.separator + contentFileName;
	}

	/**
	 * Gets the content.
	 *
	 * @param sysObj
	 *            the sys obj
	 * @param contentObjID
	 *            the content obj id
	 * @param contentFormat
	 *            the content format
	 * @param pageNbr
	 *            the page nbr
	 * @param pageModifier
	 *            the page modifier
	 * @param contentDataTicket
	 *            the content data ticket
	 * @return the content
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 * @throws DfException
	 *             the df exception
	 */
	@SuppressWarnings("unused")
	private String getContent2(IDfSysObject sysObj, String contentObjID, String contentFormat, int pageNbr,
		String pageModifier, int contentDataTicket) throws IOException, DfException {
		if (DctmDocument.logger.isEnabledFor(Level.DEBUG)) {
			DctmDocument.logger
				.debug("Started getting content file to filesystem. <contentID, format, pageNbr, pageModifier, dataTicket> : <"
					+ contentObjID
					+ ", "
					+ contentFormat
					+ ", "
					+ pageNbr
					+ ", "
					+ pageModifier
					+ ", "
					+ contentDataTicket + ">");
		}
		// NOTE smakim: I tried using getContentEx2 method of IDfSysObject to get the
// ByteArrayInputStream
		// but ran into out of memory (OutOfMemoryError: Java heap space) for large documents. So,
// now
		// using getFileEx2 instead.
/*
 * int bufferSize = SettingManager.getProperty("content_read_buffer_size",
 * Constant.CONTENT_READ_BUFFER_SIZE);
 */
		File contentExportRootDir = AbstractCMSMFMain.getInstance().getContentFilesDirectory();
		// Make sure the content export location exists
		FileUtils.forceMkdir(contentExportRootDir);
		String relativeContentFileLocation = CMSMFUtils.getContentPathFromContentID(contentObjID);

		// Make sure that the content file folder location exists
		FileUtils.forceMkdir(new File(contentExportRootDir, relativeContentFileLocation));

		String contentFileName = contentObjID + "_" + contentFormat + "_" + pageNbr;
		if (StringUtils.isNotBlank(pageModifier)) {
			contentFileName = contentFileName + "_" + pageModifier;
		}

		String fullFileSystemFileName = contentExportRootDir + File.separator + relativeContentFileLocation
			+ File.separator + contentFileName;

		// Get the content files
		sysObj.getFileEx2(fullFileSystemFileName, contentFormat, pageNbr, pageModifier, false);
		if (DctmDocument.logger.isEnabledFor(Level.DEBUG)) {
			DctmDocument.logger.debug("Finished getting content file to filesystem. Relative file location is: "
				+ relativeContentFileLocation + File.separator + contentFileName);
		}
		return relativeContentFileLocation + File.separator + contentFileName;
	}

	/**
	 * Reads the content from a byte array input stream and stores it into a byte array.
	 *
	 * @param contentStream
	 *            the content stream
	 * @return the byte data
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	@Deprecated
	protected byte[] getByteData(ByteArrayInputStream contentStream) throws IOException {
		if (DctmDocument.logger.isEnabledFor(Level.DEBUG)) {
			DctmDocument.logger.debug("Started converting content input stream to byte[]");
		}

		int bufferSize = Setting.CONTENT_READ_BUFFER_SIZE.getInt();
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream(bufferSize);
		byte[] bytes = new byte[bufferSize];

		// Read bytes from the input stream in bytes.length-sized chunks and
		// write them into the output stream
		int readBytes;
		while ((readBytes = contentStream.read(bytes)) > 0) {
			outputStream.write(bytes, 0, readBytes);
		}

		// Convert the contents of the output stream into a byte array
		byte[] byteData = outputStream.toByteArray();

		// Close the streams
		contentStream.close();
		outputStream.close();
		if (DctmDocument.logger.isEnabledFor(Level.DEBUG)) {
			DctmDocument.logger.debug("Finished converting content input stream to byte[]");
		}

		return byteData;
	}

}
