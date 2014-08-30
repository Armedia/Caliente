package com.delta.cmsmf.serialization;

import java.io.IOException;
import java.io.OutputStream;
import java.sql.SQLException;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import com.delta.cmsmf.cmsobjects.DctmObject;
import com.delta.cmsmf.datastore.DataStore;
import com.delta.cmsmf.exception.CMSMFException;
import com.delta.cmsmf.filestreams.FileStreamsManager;
import com.delta.cmsmf.mainEngine.RepositoryConfiguration;
import com.delta.cmsmf.runtime.AppCounter;
import com.documentum.fc.common.DfException;

/**
 * The Class DctmObjectWriter contains methods to write various types of objects
 * to file system during export process.
 *
 * @author Shridev Makim 6/15/2010
 */
public class DctmObjectWriter {

	/** The logger object used for logging. */
	static Logger logger = Logger.getLogger(DctmObjectWriter.class);

	/**
	 * Writes binary object to file system by opening appropriate output stream.
	 *
	 * @param dctmObj
	 *            the dctm object that needs to be written
	 * @throws CMSMFException
	 *             the cMSMF exception
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	public static void writeBinaryObject(DctmObject dctmObj) throws CMSMFException, IOException {
		if (dctmObj != null) {
			if (DctmObjectWriter.logger.isEnabledFor(Level.INFO)) {
				DctmObjectWriter.logger
					.info("Started serializing the object to filesystem " + dctmObj.getSrcObjectID());
			}

			try {
				if (DataStore.serializeObject(dctmObj.getDataObject())) {
					DctmObjectWriter.logger.debug(String.format("Serialized object [%s] to the database",
						dctmObj.getSrcObjectID()));
				} else {
					DctmObjectWriter.logger.warn(String.format("Object [%s] already serialized in the database",
						dctmObj.getSrcObjectID()));
				}
			} catch (SQLException e) {
				throw new CMSMFException(String.format("Failed to serialize object [%s]", dctmObj.getSrcObjectID()), e);
			} catch (DfException e) {
				throw new CMSMFException(String.format("Failed to serialize object [%s]", dctmObj.getSrcObjectID()), e);
			}

			/*
			FileStreamsManager fsm = FileStreamsManager.getFileStreamManager();
			// Get appropriate outputstream that corresponds to object type
			OutputStream os = fsm.getOutputStreamForType(dctmObj.dctmObjectType);

			// Export the dctmobject to outputstream
			fsm.exportObject(os, dctmObj);
			 */

			// Update appropriate counter
			AppCounter.getObjectCounter().incrementCounter(dctmObj.dctmObjectType);

			if (DctmObjectWriter.logger.isEnabledFor(Level.INFO)) {
				DctmObjectWriter.logger.info("Finished serializing the object to filesystem "
					+ dctmObj.getSrcObjectID());
			}
		}
	}

	/**
	 * Writes repository configuration object to the file system.
	 *
	 * @param repoConfigObject
	 *            the repository configuration object that needs to be written
	 * @throws CMSMFException
	 *             the cMSMF exception
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	public static void writeBinaryObject(RepositoryConfiguration repoConfigObject) throws CMSMFException, IOException {
		if (repoConfigObject != null) {
			if (DctmObjectWriter.logger.isEnabledFor(Level.INFO)) {
				DctmObjectWriter.logger.info("Started serializing the repo configuration to filesystem for docbase"
					+ repoConfigObject.getDocbaseName());
			}

			// Get the repo configuration outputstream and write repo config object to it.
			OutputStream repoConfigFileOS = FileStreamsManager.getFileStreamManager().getSrcRepoConfigOS();
			FileStreamsManager.getFileStreamManager().exportObject(repoConfigFileOS, repoConfigObject);

			if (DctmObjectWriter.logger.isEnabledFor(Level.INFO)) {
				DctmObjectWriter.logger.info("Finished serializing the repo configuration to filesystem for docbase"
					+ repoConfigObject.getDocbaseName());
			}
		}
	}

	// public void writeXMLObject(XMLEncoder xEnc) {
	// xEnc.writeObject(dctmObject);
	// xEnc.flush();
	// }

}
