package com.delta.cmsmf.cmsobjects;

import java.io.IOException;
import java.util.List;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import com.delta.cmsmf.constants.DctmAttrNameConstants;
import com.delta.cmsmf.exception.CMSMFException;
import com.delta.cmsmf.mainEngine.DctmObjectExportHelper;
import com.delta.cmsmf.runtime.DuplicateChecker;
import com.documentum.com.DfClientX;
import com.documentum.fc.client.IDfCollection;
import com.documentum.fc.client.IDfPersistentObject;
import com.documentum.fc.client.IDfQuery;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.client.IDfType;
import com.documentum.fc.common.DfException;
import com.documentum.fc.common.IDfAttr;
import com.documentum.fc.common.IDfId;

/**
 * The DctmType class contains methods to export/import dm_type type of objects from/to Documentum
 * CMS.
 * <p>
 * <b> NOTE: Any type names that start with dm_ will not be imported in the target repository. These
 * type names are treated as documentum internal types. </b>
 * <p>
 * <b> NOTE: Only basic type definition is created. We are not handling value assistance or any
 * attribute constraints (eg not null, unique key, primary key etc). </b>
 * <p>
 * <b> NOTE: Type object updates can not be performed at moment because it would be extremely
 * difficult to see what had changed in either repository (source or target). It would take
 * considerable amount of effort to come up with the Alter Table dql necessary.</b>
 * 
 * @author Shridev Makim 6/15/2010
 */
public class DctmType extends DctmObject {

	/** The Constant serialVersionUID. */
	private static final long serialVersionUID = 1L;

	// NOTE We can not create a type within an explicit transaction. If you try to do so, the
// following
	// error is thrown
	// [DM_DATA_DICT_E_COMMIT_EXTERNAL_TRANSACTION]error: "Committing the Data Dictionary changes
// for type %s
	// would commit an explicit open transaction."

	// Static variables used to see how many Types were created, skipped, updated
	/** Keeps track of nbr of type objects read from file during import process. */
	public static int types_read = 0;
	/** Keeps track of nbr of type objects skipped due to duplicates during import process. */
	public static int types_skipped = 0;
	/** Keeps track of nbr of type objects updated in CMS during import process. */
	public static int types_updated = 0;
	/** Keeps track of nbr of type objects created in CMS during import process. */
	public static int types_created = 0;

	/** The logger object used for logging. */
	static Logger logger = Logger.getLogger(DctmType.class);

	/**
	 * Instantiates a new DctmType object.
	 */
	public DctmType() {
		super();
		// set dctmObjectType to dctm_type
		this.dctmObjectType = DctmObjectTypesEnum.DCTM_TYPE;
	}

	/**
	 * Instantiates a new DctmType object with new CMS session.
	 * 
	 * @param dctmSession
	 *            the existing documentum CMS session
	 */
	public DctmType(IDfSession dctmSession) {
		super(dctmSession);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.delta.cmsmf.cmsobjects.DctmObject#createInCMS()
	 */
	@Override
	public void createInCMS() throws DfException, IOException {
		DctmType.types_read++;

		if (DctmType.logger.isEnabledFor(Level.INFO)) {
			DctmType.logger.info("Started creating dctm dm_type in repository");
		}

		try {
			IDfPersistentObject prsstntObj = null;
			// First check to see if the type already exist; if it does, check to see if we need to
// update it
			String typeName = getStrSingleAttrValue(DctmAttrNameConstants.NAME);

			// Skip messing with documentum internal types (types starting with dm_)
			if (typeName.startsWith("dm_")) {
				if (DctmType.logger.isEnabledFor(Level.INFO)) {
					DctmType.logger.info("The type " + typeName
						+ " was not created in cms. It appears to be a system type");
				}
				// exit out
				DctmType.types_skipped++;
				return;
			}

			IDfType type = this.dctmSession.getType(typeName);
			if (type != null) { // we found existing type
				int versionStamp = type.getVStamp();
				if (versionStamp != getIntSingleAttrValue(DctmAttrNameConstants.I_VSTAMP)) {
					// NOTE We need to update the type but we can't yet, so raise the error
					DctmType.logger.error("Type by name " + typeName
						+ " already exist in target repository but needs to be updated.");
				} else { // Identical type exists in the target repository, abort the transaction
// and quit
					if (DctmType.logger.isEnabledFor(Level.INFO)) {
						DctmType.logger.info("Identical format " + typeName + " already exists in target repository.");
					}
					DctmType.types_skipped++;
					return;
				}
			} else { // type doesn't exist in repo, create one
				if (DctmType.logger.isEnabledFor(Level.INFO)) {
					DctmType.logger.info("Creating type " + typeName + " in target repository.");
				}
				DctmType.types_created++;
				CreateTypeInCMS();

				// update vStamp of the type object that was just created
				prsstntObj = this.dctmSession.getType(typeName);
				updateVStamp(prsstntObj, this);
			}

			if (DctmType.logger.isEnabledFor(Level.INFO)) {
				DctmType.logger.info("Finished creating dctm dm_type in repository with name: " + typeName);
			}
		} catch (DfException e) {
			throw (e);
		}
	}

	/**
	 * Creates the dm_type object in cms that corresponds to this object. This method builds
	 * required
	 * "Create Type" dql statement and executes it in order to create appropriate dm_type object.
	 * 
	 * @throws DfException
	 *             the df exception
	 */
	private void CreateTypeInCMS() throws DfException {

		// Build the DQL string needed to create the type object
		StringBuffer dqlString = new StringBuffer(32);
		int attrCount = getIntSingleAttrValue(DctmAttrNameConstants.ATTR_COUNT);
		int startPosition = getIntSingleAttrValue(DctmAttrNameConstants.START_POS);
		String typeName = getStrSingleAttrValue(DctmAttrNameConstants.NAME);
		String superTypeName = getStrSingleAttrValue(DctmAttrNameConstants.SUPER_NAME);
		List<Object> attrNames = findAttribute(DctmAttrNameConstants.ATTR_NAME).getRepeatingValues();
		List<Object> attrTypes = findAttribute(DctmAttrNameConstants.ATTR_TYPE).getRepeatingValues();
		List<Object> attrLengths = findAttribute(DctmAttrNameConstants.ATTR_LENGTH).getRepeatingValues();
		List<Object> attrRepeating = findAttribute(DctmAttrNameConstants.ATTR_REPEATING).getRepeatingValues();
		dqlString = dqlString.append("Create Type \"").append(typeName).append("\"( ");

		// Iterate through only the custom attributes of the type object and add them to the dql
// string
		for (int iIndex = startPosition; iIndex < attrCount; ++iIndex) {
			String attrName = (String) attrNames.get(iIndex);
			dqlString.append(attrName).append(" ");
			int attrType = (Integer) attrTypes.get(iIndex);
			switch (attrType) {
				case IDfAttr.DM_BOOLEAN:
					dqlString.append("boolean");
					break;
				case IDfAttr.DM_ID:
					dqlString.append("ID");
					break;
				case IDfAttr.DM_INTEGER:
					dqlString.append("Integer");
					break;
				case IDfAttr.DM_DOUBLE:
					dqlString.append("double");
					break;
				case IDfAttr.DM_STRING:
					int attrLength = (Integer) attrLengths.get(iIndex);
					dqlString.append("String(").append(attrLength).append(")");
					break;
				case IDfAttr.DM_TIME:
					dqlString.append("Date");
					break;
				case IDfAttr.DM_UNDEFINED:
					dqlString.append("Undefined");
					break;
				default:
					break;
			}
			Boolean isRepeating = (Boolean) attrRepeating.get(iIndex);
			if (isRepeating) {
				dqlString.append(" Repeating");
			}

			if (iIndex != (attrCount - 1)) {
				dqlString.append(", ");
			}
		}
		System.out.println(dqlString);

		// Add the supertype phrase if needed
		superTypeName = (superTypeName.length() > 0) ? superTypeName : "Null ";
		dqlString.append(")");
		dqlString.append(" With SuperType ").append(superTypeName).append(" Publish");

		if (DctmType.logger.isEnabledFor(Level.INFO)) {
			DctmType.logger.info("The DQL for creating type is: " + dqlString);
		}

		IDfQuery dqlQry = new DfClientX().getQuery();
		try {
			dqlQry.setDQL(dqlString.toString());
			IDfCollection resultCol = dqlQry.execute(this.dctmSession, IDfQuery.DF_EXECREAD_QUERY);
			while (resultCol.next()) {
				IDfId newTypeId = resultCol.getId(DctmAttrNameConstants.NEW_OBJECT_ID);
				if (DctmType.logger.isEnabledFor(Level.DEBUG)) {
					DctmType.logger.debug("Object id of new type object is: " + newTypeId.getId());
				}
			}
			resultCol.close();
		} catch (DfException e) {
			DctmType.logger.error("Couldn't create type with name: " + typeName, e);
			throw (e);
		}
		if (DctmType.logger.isEnabledFor(Level.INFO)) {
			DctmType.logger.info("Finished creating type " + typeName + " in target repository.");
		}
	}

	/**
	 * Prints the import report detailing how many type objects were read, updated, created, skipped
	 * during
	 * the import process.
	 */
	public static void printImportReport() {
		DctmType.logger.info("No. of type objects read from file: " + DctmType.types_read);
		DctmType.logger.info("No. of type objects skipped due to duplicates: " + DctmType.types_skipped);
		DctmType.logger.info("No. of type objects updated: " + DctmType.types_updated);
		DctmType.logger.info("No. of type objects created: " + DctmType.types_created);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.delta.cmsmf.cmsobjects.DctmObject#getFromCMS(com.documentum.fc.client.IDfPersistentObject)
	 */
	@Override
	public DctmObject getFromCMS(IDfPersistentObject prsstntObj) throws CMSMFException {
		if (DctmType.logger.isEnabledFor(Level.INFO)) {
			DctmType.logger.info("Started getting dctm dm_type from repository");
		}
		String typeID = "";
		try {
			typeID = prsstntObj.getObjectId().getId();
			String typeName = prsstntObj.getString(DctmAttrNameConstants.NAME);
			// Check if this type has already been exported, if not, add to processed list
			if (!DuplicateChecker.getDuplicateChecker().isTypeProcessed(typeID)) {

				// First export the supertypes
				exportSuperTypes((IDfType) prsstntObj);

				DctmType dctmType = new DctmType();
				getAllAttributesFromCMS(dctmType, prsstntObj, typeID);

				return dctmType;
			} else {
				if (DctmType.logger.isEnabledFor(Level.INFO)) {
					DctmType.logger.info("Type " + typeName + " already has been or is being exported!");
				}
			}
		} catch (DfException e) {
			throw (new CMSMFException("Error retrieving type in repository with id: " + typeID, e));
		}
		if (DctmType.logger.isEnabledFor(Level.INFO)) {
			DctmType.logger.info("Finished getting dctm dm_type from repository with id: " + typeID);
		}

		return null;
	}

	/**
	 * Exports super type objects of given type object.
	 * This method calls itself recursively in order to export
	 * all of its supertypes first before exporting itself.
	 * 
	 * @param typeObj
	 *            the DFC IDfType object
	 * @throws CMSMFException
	 *             the cMSMF exception
	 */
	private void exportSuperTypes(IDfType typeObj) throws CMSMFException {
		try {
			if (DctmType.logger.isEnabledFor(Level.INFO)) {
				DctmType.logger.info("Started serializing super types for dm_type object with name: "
					+ typeObj.getName());
			}

			// First process the supertypes and then process itself
			IDfType superType = typeObj.getSuperType();
			if (superType != null) {
				exportSuperTypes(superType);
			}

			// Export the type object
			DctmObjectExportHelper.serializeType(this.dctmSession, typeObj);
			if (DctmType.logger.isEnabledFor(Level.INFO)) {
				DctmType.logger.info("Finished serializing super types for dm_type object with name: "
					+ typeObj.getName());
			}
		} catch (DfException e) {
			throw (new CMSMFException("Couldn't retrieve all super types for dctm type", e));
		}
	}

}
