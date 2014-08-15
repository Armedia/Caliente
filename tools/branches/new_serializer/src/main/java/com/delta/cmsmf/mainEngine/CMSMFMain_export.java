package com.delta.cmsmf.mainEngine;

import java.io.IOException;

import org.apache.log4j.Level;

import com.delta.cmsmf.cmsobjects.DctmObject;
import com.delta.cmsmf.constants.CMSMFAppConstants;
import com.delta.cmsmf.constants.CMSMFProperties;
import com.delta.cmsmf.exception.CMSMFException;
import com.delta.cmsmf.exception.CMSMFIOException;
import com.delta.cmsmf.filestreams.FileStreamsManager;
import com.delta.cmsmf.properties.PropertiesManager;
import com.delta.cmsmf.runtime.AppCounter;
import com.delta.cmsmf.serialization.DctmObjectWriter;
import com.documentum.com.DfClientX;
import com.documentum.fc.client.IDfCollection;
import com.documentum.fc.client.IDfPersistentObject;
import com.documentum.fc.client.IDfQuery;
import com.documentum.fc.common.DfException;
import com.documentum.fc.common.DfId;

/**
 * The main method of this class is an entry point for the cmsmf application.
 * 
 * @author Shridev Makim 6/15/2010
 */
public class CMSMFMain_export extends CMSMFMain {

	CMSMFMain_export() throws Throwable {
		super();
	}

	/**
	 * Starts exporting objects from source directory. It reads up the query from
	 * properties file and executes it against the source repository. It retrieves
	 * objects from the repository and exports it out.
	 * 
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	@Override
	protected void run() throws IOException {
		if (this.logger.isEnabledFor(Level.INFO)) {
			this.logger.info("##### Export Process Started #####");
		}

		// Load source repository configuration information
		RepositoryConfiguration srcRepoConfig = RepositoryConfiguration.getRepositoryConfiguration();
		try {
			srcRepoConfig.loadRepositoryConfiguration(this.dctmSession);
		} catch (DfException e1) {
			this.logger.fatal("Couldn't retrieve repository configuration information", e1);
		}

		// reset the counters that keeps track of how many objects are exported
		AppCounter.getObjectCounter().resetCounters();

		// First set the directory path where all of the files will be created
		FileStreamsManager fsm = FileStreamsManager.getFileStreamManager();
		fsm.setStreamsDirectoryPath(this.streamFilesDirectoryLocation);
		fsm.setContentDirectoryPath(this.contentFilesDirectoryLocation);

		// Delete existing cmsmf stream files before export process
		fsm.deleteStreamFiles();

		// Build the query that will determine what objects will be exported
		String selectClause = CMSMFAppConstants.EXPORT_QUERY_SELECT_CLAUSE;
		String fromWhereClause = PropertiesManager.getProperty(CMSMFProperties.EXPORT_QUERY_PREDICATE, "");

		IDfQuery dqlQry = new DfClientX().getQuery();
		dqlQry.setDQL(selectClause + fromWhereClause);
		dqlQry.setBatchSize(20000);
		if (this.logger.isEnabledFor(Level.DEBUG)) {
			this.logger.debug("Export DQL Query is: " + selectClause + fromWhereClause);
		}

		try {
			IDfCollection resultCol = dqlQry.execute(this.dctmSession, IDfQuery.READ_QUERY);
			while (resultCol.next()) {
				String objID = resultCol.getId("r_object_id").getId();
				// get the object
				IDfPersistentObject prsstntObj = null;
				try {
					prsstntObj = this.dctmSession.getObject(new DfId(objID));
				} catch (DfException e) {
					this.logger.error("Couldn't retrieve object by ID: " + objID, e);
				}

				DctmObjectRetriever dctmObjRetriever = new DctmObjectRetriever(this.dctmSession);
				DctmObject dctmObj;
				try {
					dctmObj = dctmObjRetriever.retrieveObject(prsstntObj);
					DctmObjectWriter.writeBinaryObject(dctmObj);
					// dctmObjWriter.writeXMLObject(xmlEncoder);
				} catch (CMSMFException e) {
					// If for some reason object is not retrieved from the system, or written to the
					// filesystem,
					// write to an error log and continue on
					this.logger.error("Couldn't retrieve object information from repository for id: " + objID, e);
				} catch (IOException e) {
					// If there is IOException, log the error and exit out
					this.logger.fatal("Couldn't serialize an object to the filesystem for id: " + objID, e);
					// close all of the file streams
					try {
						fsm.closeAllStreams();
					} catch (CMSMFIOException e2) {
						this.logger.error("Couldn't close all of the filestreams", e2);
					}
					throw (e);
				}
			} // while (resultCol.next())
			resultCol.close();

			// Export source repository configuration information
			try {
				DctmObjectWriter.writeBinaryObject(srcRepoConfig);
			} catch (CMSMFException e) {
				this.logger.error("Couldn't retrieve source repository configuration information.", e);
			}
		} catch (DfException e) {
			// If there is a DfException while running the export query, log the error and exit out
			this.logger.fatal("Export Query failed to run. Query is:  " + selectClause + fromWhereClause, e);
		} finally {
			// close all of the file streams
			try {
				fsm.closeAllStreams();
			} catch (CMSMFIOException e) {
				this.logger.error("Couldn't close all of the filestreams", e);
			}
		}

		// print the counters to see how many objects were processed
		AppCounter.getObjectCounter().printCounters();

		if (this.logger.isEnabledFor(Level.INFO)) {
			this.logger.info("##### Export Process Finished #####");
		}
	}
}
