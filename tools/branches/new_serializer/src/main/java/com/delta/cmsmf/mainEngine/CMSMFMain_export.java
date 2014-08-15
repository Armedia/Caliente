package com.delta.cmsmf.mainEngine;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.IOUtils;
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
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.common.DfException;
import com.documentum.fc.common.DfId;

/**
 * The main method of this class is an entry point for the cmsmf application.
 * 
 * @author Shridev Makim 6/15/2010
 */
public class CMSMFMain_export extends CMSMFMain {

	private class Worker implements Runnable {

		private final String objectId;
		private final FileStreamsManager fsm;

		private Worker(FileStreamsManager fsm, String objectId) {
			this.objectId = objectId;
			this.fsm = fsm;
		}

		@Override
		public void run() {
			// get the object
			final IDfSession session;
			try {
				session = getSession();
			} catch (Throwable t) {
				throw new RuntimeException("Failed to get a session", t);
			}
			try {
				IDfPersistentObject prsstntObj = null;
				try {
					prsstntObj = session.getObject(new DfId(this.objectId));
				} catch (DfException e) {
					CMSMFMain_export.this.logger.error("Couldn't retrieve object by ID: " + this.objectId, e);
				}

				DctmObjectRetriever dctmObjRetriever = new DctmObjectRetriever(session);
				DctmObject dctmObj;
				try {
					dctmObj = dctmObjRetriever.retrieveObject(prsstntObj);

					// First, get the location where the object will be stored
					File targetDir = dctmObj.dctmObjectType.getBaseFolder();
					String targetId = dctmObj.dctmObjectType.getNextId();
					final String targetName = String.format("%s_%s.zip", targetId, dctmObj.getSrcObjectID());

					for (int i = 0; i < 3; i++) {
						int n = i * 2;
						targetDir = new File(targetDir, targetId.substring(n, n + 2));
						if (!targetDir.exists() && !targetDir.mkdirs()) {
							throw new IOException(
								String.format("Failed to create the target directory [%s]", targetDir));
						} else if (!targetDir.isDirectory()) { throw new IOException(String.format(
							"Path [%s] is not a directory", targetDir)); }
					}

					// Next, create the zip file that will contain the object description, as well
					// as the object content stream(s)
					File targetFile = new File(targetDir, targetName);
					FileOutputStream fos = new FileOutputStream(targetFile);
					ZipOutputStream zip = new ZipOutputStream(fos);
					try {
						// Now, write out the object to the zip output stream
						ZipEntry metadata = new ZipEntry("metadata.xml");
						zip.putNextEntry(metadata);
						DctmObjectWriter.writeBinaryObject(dctmObj, zip);
						zip.flush();

						// Next, write out the content
						ZipEntry content = new ZipEntry("content.dat");
						zip.putNextEntry(content);
						// zip.write(contentBytes);
						zip.flush();
					} catch (IOException e) {
						CMSMFMain_export.this.logger.fatal(
							String.format("IOException caught while writing out object [%s]", this.objectId), e);
						targetFile.deleteOnExit();
					} finally {
						IOUtils.closeQuietly(zip);
						IOUtils.closeQuietly(fos);
					}

				} catch (CMSMFException e) {
					// If for some reason object is not retrieved from the system, or written to the
					// filesystem, write to an error log and continue on
					CMSMFMain_export.this.logger.error("Couldn't retrieve object information from repository for id: "
						+ this.objectId, e);
				} catch (IOException e) {
					// If there is IOException, log the error and exit out
					CMSMFMain_export.this.logger.fatal("Couldn't serialize an object to the filesystem for id: "
						+ this.objectId, e);
					throw new RuntimeException("IOException caught", e);
				}
			} catch (Throwable t) {
				CMSMFMain_export.this.logger.fatal(
					String.format("Exception caught while processing object [%s]", this.objectId), t);
			} finally {
				closeSession(session);
			}
		}
	}

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

		IDfSession session;
		try {
			session = getSession();
		} catch (Throwable t) {
			throw new RuntimeException("Failed to get the query session", t);
		}

		// Load source repository configuration information
		RepositoryConfiguration srcRepoConfig = RepositoryConfiguration.getRepositoryConfiguration();
		try {
			srcRepoConfig.loadRepositoryConfiguration(session);
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
		String strBatchSize = CMSMFLauncher.getParameter(CLIParam.batch);
		int batchSize = CMSMFAppConstants.BATCH_SIZE;
		if (strBatchSize != null) {
			try {
				batchSize = Integer.valueOf(strBatchSize);
				if (batchSize <= 0) {
					this.logger.warn(String.format("Illegal batch size [%d] - using the default value", batchSize));
					batchSize = CMSMFAppConstants.BATCH_SIZE;
				}
			} catch (NumberFormatException e) {
				this.logger.warn(String.format("Illegal batch size [%s] - using the default value", strBatchSize));
				batchSize = CMSMFAppConstants.BATCH_SIZE;
			}
		}

		IDfQuery dqlQry = new DfClientX().getQuery();
		dqlQry.setDQL(selectClause + fromWhereClause);
		dqlQry.setBatchSize(batchSize);
		if (this.logger.isEnabledFor(Level.DEBUG)) {
			this.logger.debug("Export DQL Query is: " + selectClause + fromWhereClause);
		}

		try {
			IDfCollection resultCol = dqlQry.execute(session, IDfQuery.READ_QUERY);
			while (resultCol.next()) {
				queueWork(new Worker(fsm, resultCol.getId("r_object_id").getId()));
			}
			resultCol.close();
			closeSession(session);

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
