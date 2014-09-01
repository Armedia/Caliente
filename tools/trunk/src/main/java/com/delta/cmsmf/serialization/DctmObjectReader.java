package com.delta.cmsmf.serialization;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;

import org.apache.log4j.Logger;

import com.delta.cmsmf.exception.CMSMFException;
import com.delta.cmsmf.exception.CMSMFFileNotFoundException;
import com.delta.cmsmf.filestreams.FileStreamsManager;
import com.delta.cmsmf.mainEngine.RepositoryConfiguration;

/**
 * The Class DctmObjectReader contains methods to read various types of objects
 * from file during import process.
 *
 * @author Shridev Makim 6/15/2010
 */
public class DctmObjectReader {

	/** The logger object used for logging. */
	static Logger logger = Logger.getLogger(DctmObjectReader.class);

	/**
	 * Reads an object of given type from filesystem.
	 *
	 * @param dctmObjType
	 *            the dctm obj type
	 * @return the object
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 * @throws CMSMFException
	 *             the cMSMF exception
	 */
	/*
	public static DctmObject readObject(DctmObjectType dctmObjType) throws IOException, CMSMFException {
		DctmObject returnObject = null;
		try {
			DataObject dataObject = DataStore.deserializeNextObject(dctmObjType);
			if (dataObject != null) {
				returnObject = dctmObjType.newInstance(dataObject);
			} else {
				DctmObjectReader.logger.info(String.format("Loaded the last object of type %s", dctmObjType));
			}
		} catch (InstantiationException e) {
			throw new CMSMFException(String.format("Failed to deserialize an object of type %s", dctmObjType), e);
		} catch (IllegalAccessException e) {
			throw new CMSMFException(String.format("Failed to deserialize an object of type %s", dctmObjType), e);
		} catch (InvocationTargetException e) {
			throw new CMSMFException(String.format("Failed to deserialize an object of type %s", dctmObjType), e);
		}
		return returnObject;
	}
	 */

	/**
	 * Reads repository configuration information from file system and returns it.
	 *
	 * @return the repository configuration
	 * @throws CMSMFException
	 *             the cMSMF exception
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	public static RepositoryConfiguration readSrcRepoConfig() throws CMSMFException, IOException {
		RepositoryConfiguration srcRepoConfig = null;
		try {
			InputStream is = FileStreamsManager.getFileStreamManager().getSrcRepoConfigIS();
			srcRepoConfig = (RepositoryConfiguration) ((ObjectInputStream) is).readObject();
		} catch (CMSMFFileNotFoundException e) {
			// If ACLs stream file is not found, record it in the log
			DctmObjectReader.logger.warn("The Source Repo Config file does not exist. Process can not continue.");
		} catch (EOFException e) {
			// Do nothing if you get a end of file exception
			DctmObjectReader.logger.info("Reached the end of the file for Source Repo Config file.");
		} catch (ClassNotFoundException e) {
			throw (new CMSMFException("Couldn't read Repo Config object from filesystem.", e));
		}
		return srcRepoConfig;
	}
}
