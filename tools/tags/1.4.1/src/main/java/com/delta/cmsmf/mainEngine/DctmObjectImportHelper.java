package com.delta.cmsmf.mainEngine;

import java.util.StringTokenizer;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import com.delta.cmsmf.constants.DctmTypeConstants;
import com.documentum.fc.client.IDfFolder;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.common.DfException;
import com.documentum.fc.common.IDfId;

/**
 * The Class DctmObjectImportHelper contains several utility methods that are used
 * during import step.
 *
 * @author Shridev Makim 6/15/2010
 */
public class DctmObjectImportHelper {

	/** The logger object used for logging. */
	static Logger logger = Logger.getLogger(DctmObjectImportHelper.class);

	/**
	 * Creates the folder in a given repository. This method creates the cabinets and
	 * other parent folders if they do not exist in the repository.
	 *
	 * @param dctmSession
	 *            the existing documentum repository session
	 * @param fldrPath
	 *            the folder path
	 * @throws DfException
	 *             raised from the DFC API
	 */
	public static void createFolderByPath(IDfSession dctmSession, String fldrPath) throws DfException {
		if (DctmObjectImportHelper.logger.isEnabledFor(Level.INFO)) {
			DctmObjectImportHelper.logger.info("Started creating folder by path: " + fldrPath);
		}

		// Check to see if folder already exists
		IDfFolder fldrObj = dctmSession.getFolderByPath(fldrPath);
		if (fldrObj == null) {
			String pathSep = "/";
			IDfId currentId = null;
			StringBuffer bufFldrPath = new StringBuffer(48);

			StringTokenizer tokFldrNames = new StringTokenizer(fldrPath, pathSep);

			String cabinetName = tokFldrNames.nextToken();
			StringBuffer cabQual = new StringBuffer(50);
			cabQual.append("dm_cabinet where object_name='").append(cabinetName).append("'");

			currentId = dctmSession.getIdByQualification(cabQual.toString());
			if (currentId.isNull()) {
				// need to create cabinet.
				IDfFolder cab = (IDfFolder) dctmSession.newObject("dm_cabinet");
				cab.setObjectName(cabinetName);
				cab.save();
				currentId = cab.getObjectId();
			}
			bufFldrPath.append(pathSep).append(cabinetName);

			// now create all folders beyond the cabinet
			while (tokFldrNames.hasMoreTokens()) {
				String parentPath = bufFldrPath.toString();

				String fldrName = tokFldrNames.nextToken();
				bufFldrPath.append(pathSep).append(fldrName);
				// by this point the buffer should contain the new expected path

				currentId = DctmObjectImportHelper.getIdByPath(dctmSession, bufFldrPath.toString());
				if (currentId.isNull()) {
					// looks like the new folder in the path does not exist.
					IDfFolder newFldr = (IDfFolder) dctmSession.newObject(DctmTypeConstants.DM_FOLDER);
					newFldr.setObjectName(fldrName);
					newFldr.link(parentPath);
					newFldr.save();
					currentId = newFldr.getObjectId();
				}
				// by this point currentId should point to next folder in path
			}// while(all folder names)

		} else {
			if (DctmObjectImportHelper.logger.isEnabledFor(Level.DEBUG)) {
				DctmObjectImportHelper.logger.debug("Folder already exists!");
			}
		}

		if (DctmObjectImportHelper.logger.isEnabledFor(Level.INFO)) {
			DctmObjectImportHelper.logger.info("Finished creating folder by path: " + fldrPath);
		}

	}

	/**
	 * Gets the object id of the sysobject for given folder path. This
	 * method runs a dql query to get the object id. It can return object id
	 * of folders as well as cabinets in documentum reposotiry.
	 *
	 * @param dctmSession
	 *            the existing documentum repository session
	 * @param fldrPath
	 *            the folder path
	 * @return the object id of the folder/cabinet object
	 * @throws DfException
	 *             Signals that Dctm Server error has occurred.
	 */
	private static IDfId getIdByPath(IDfSession dctmSession, String fldrPath) throws DfException {

		int pathSepIndex = fldrPath.lastIndexOf('/');
		if (pathSepIndex == -1) { return null; }

		StringBuffer bufQual = new StringBuffer(32);
		if (pathSepIndex == 0) {
			// its a cabinet path
			bufQual.append(" dm_cabinet where object_name='");
			bufQual.append(fldrPath.substring(1));
			bufQual.append("'");
		} else {
			bufQual.append(" dm_sysobject where FOLDER('");
			bufQual.append(fldrPath.substring(0, pathSepIndex));
			bufQual.append("') ");
			bufQual.append(" and object_name='");
			bufQual.append(fldrPath.substring(pathSepIndex + 1));
			bufQual.append("'");
		}

		String strQual = bufQual.toString();
		IDfId id = dctmSession.getIdByQualification(strQual);
		return id;
	}
}
