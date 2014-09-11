package com.delta.cmsmf.filestreams;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.SuffixFileFilter;
import org.apache.log4j.Logger;

import com.delta.cmsmf.cmsobjects.DctmObjectTypesEnum;
import com.delta.cmsmf.exception.CMSMFFileNotFoundException;
import com.delta.cmsmf.exception.CMSMFIOException;
import com.delta.cmsmf.properties.CMSMFProperties;

/**
 * The Class FileStreamsManager is facilitates access to various file system
 * stream files to serialize/deserialize java objects. This class implements
 * a singleton design pattern. It contains methods to create outputstreams so
 * that CMSMF application write CMS objects to these streams. Likewise, it
 * contains methods to create inputstreams so that application can read CMS
 * objects during import process. This class maintains one filestream for each object
 * type.
 *
 * @author Shridev Makim 6/15/2010
 */
public class FileStreamsManager {

	/** The logger object used for logging. */
	static Logger logger = Logger.getLogger(FileStreamsManager.class);

	/** The Constant DOCUMENTS_FILE_NAME. */
	private static final String DOCUMENTS_FILE_NAME = "DCTMDocsFile.cmsmf";

	/** The Constant FOLDERS_FILE_NAME. */
	private static final String FOLDERS_FILE_NAME = "DCTMFldrsFile.cmsmf";

	/** The Constant USERS_FILE_NAME. */
	private static final String USERS_FILE_NAME = "DCTMUsrsFile.cmsmf";

	/** The Constant GROUPS_FILE_NAME. */
	private static final String GROUPS_FILE_NAME = "DCTMGrpsFile.cmsmf";

	/** The Constant ACLS_FILE_NAME. */
	private static final String ACLS_FILE_NAME = "DCTMACLsFile.cmsmf";

	/** The Constant TYPES_FILE_NAME. */
	private static final String TYPES_FILE_NAME = "DCTMTypsFile.cmsmf";

	/** The Constant FORMATS_FILE_NAME. */
	private static final String FORMATS_FILE_NAME = "DCTMFrmtsFile.cmsmf";

	/** The Constant SRC_REPO_CONFIG_FILE_NAME. */
	private static final String SRC_REPO_CONFIG_FILE_NAME = "SrcRepoConfigFile.cmsmf";

	// output streams
	/** The dctm docs file os. */
	private OutputStream dctmDocsFileOS = null;

	/** The dctm fldrs file os. */
	private OutputStream dctmFldrsFileOS = null;

	/** The dctm usrs file os. */
	private OutputStream dctmUsrsFileOS = null;

	/** The dctm grps file os. */
	private OutputStream dctmGrpsFileOS = null;

	/** The dctm acls file os. */
	private OutputStream dctmACLsFileOS = null;

	/** The dctm typs file os. */
	private OutputStream dctmTypsFileOS = null;

	/** The dctm frmts file os. */
	private OutputStream dctmFrmtsFileOS = null;

	/** The src repo config file os. */
	private OutputStream srcRepoConfigFileOS = null;

	// input streams
	/** The dctm docs file is. */
	private InputStream dctmDocsFileIS = null;

	/** The dctm fldrs file is. */
	private InputStream dctmFldrsFileIS = null;

	/** The dctm Usrs file is. */
	private InputStream dctmUsrsFileIS = null;

	/** The dctm Grps file is. */
	private InputStream dctmGrpsFileIS = null;

	/** The dctm ACLs file is. */
	private InputStream dctmACLsFileIS = null;

	/** The dctm typs file is. */
	private InputStream dctmTypsFileIS = null;

	/** The dctm frmts file is. */
	private InputStream dctmFrmtsFileIS = null;

	/** The src repo config file is. */
	private InputStream srcRepoConfigFileIS = null;

	private final boolean compressStreams;

	/**
	 * Instantiates a new file streams manager.
	 */
	private FileStreamsManager() {
		// no code here; this is a singleton class so private constructor
		this.compressStreams = CMSMFProperties.COMPRESSDATA_FLAG.getBoolean();
	}

	/**
	 * Gets the file stream manager singleton instance.
	 *
	 * @return the file stream manager
	 */
	public static synchronized FileStreamsManager getFileStreamManager() {
		if (FileStreamsManager.singletonInstance == null) {
			// we can call this private constructor
			FileStreamsManager.singletonInstance = new FileStreamsManager();
		}
		return FileStreamsManager.singletonInstance;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see java.lang.Object#clone()
	 */
	@Override
	public Object clone() throws CloneNotSupportedException {
		throw new CloneNotSupportedException();
		// prevent generation of a clone
	}

	/** The singleton instance of this class. */
	private static FileStreamsManager singletonInstance;

	/** The strems diretory path where files are created. */
	private File streamsDirectoryPath;

	public void setStreamsDirectoryPath(String directoryPath) {
		setStreamsDirectoryPath(new File(directoryPath));
	}

	/**
	 * Sets the strems diretory path where the files will be created.
	 *
	 * @param streamsDirectoryPath
	 *            the new strems diretory path
	 */
	public void setStreamsDirectoryPath(File streamsDirectoryPath) {
		this.streamsDirectoryPath = streamsDirectoryPath;
		createStreamsDirectory();
	}

	/** The content directory path where content files are created. */
	private File contentDirectoryPath;

	public void setContentDirectoryPath(String directoryPath) {
		setContentDirectoryPath(new File(directoryPath));
	}

	/**
	 * Sets the content directory path where the content files will be created.
	 *
	 * @param contentDirectoryPath
	 *            the new content directory path
	 */
	public void setContentDirectoryPath(File contentDirectoryPath) {
		this.contentDirectoryPath = contentDirectoryPath;
	}

	/**
	 * Creates the streams directory if it does not exist.
	 */
	private void createStreamsDirectory() {
		// Create the directory if it doesn't exist
		if (!this.streamsDirectoryPath.exists()) {
			this.streamsDirectoryPath.mkdirs();
		}
	}

	/**
	 * Returns the OutputStream for given object type.
	 *
	 * @param dctmObjectType
	 *            the dctm object type
	 * @return the OutputStream corresponding to the object type
	 * @throws CMSMFIOException
	 *             the cMSMFIO exception
	 */
	public OutputStream getOutputStreamForType(DctmObjectTypesEnum dctmObjectType) throws CMSMFIOException {

		OutputStream returnOS = null;

		try {
			switch (dctmObjectType) {
				case DCTM_DOCUMENT:
					if (this.dctmDocsFileOS == null) {
						this.dctmDocsFileOS = createObjectOutputStream(FileStreamsManager.DOCUMENTS_FILE_NAME);
					}
					returnOS = this.dctmDocsFileOS;
					break;
				case DCTM_FOLDER:
					if (this.dctmFldrsFileOS == null) {
						this.dctmFldrsFileOS = createObjectOutputStream(FileStreamsManager.FOLDERS_FILE_NAME);
					}
					returnOS = this.dctmFldrsFileOS;
					break;
				case DCTM_USER:
					if (this.dctmUsrsFileOS == null) {
						this.dctmUsrsFileOS = createObjectOutputStream(FileStreamsManager.USERS_FILE_NAME);
					}
					returnOS = this.dctmUsrsFileOS;
					break;
				case DCTM_GROUP:
					if (this.dctmGrpsFileOS == null) {
						this.dctmGrpsFileOS = createObjectOutputStream(FileStreamsManager.GROUPS_FILE_NAME);
					}
					returnOS = this.dctmGrpsFileOS;
					break;
				case DCTM_ACL:
					if (this.dctmACLsFileOS == null) {
						this.dctmACLsFileOS = createObjectOutputStream(FileStreamsManager.ACLS_FILE_NAME);
					}
					returnOS = this.dctmACLsFileOS;
					break;
				case DCTM_TYPE:
					if (this.dctmTypsFileOS == null) {
						this.dctmTypsFileOS = createObjectOutputStream(FileStreamsManager.TYPES_FILE_NAME);
					}
					returnOS = this.dctmTypsFileOS;
					break;
				case DCTM_FORMAT:
					if (this.dctmFrmtsFileOS == null) {
						this.dctmFrmtsFileOS = createObjectOutputStream(FileStreamsManager.FORMATS_FILE_NAME);
					}
					returnOS = this.dctmFrmtsFileOS;
					break;
				default:
					FileStreamsManager.logger.warn("Trying to get output stream for invalid object type!");
			}
		} catch (CMSMFIOException e) {
			throw (new CMSMFIOException("Couldn't create object output stream for " + dctmObjectType + " file.", e));
		}

		return returnOS;
	}

	/**
	 * Returns the InputStream for given object type.
	 *
	 * @param dctmObjectType
	 *            the dctm object type
	 * @return the InputStream corresponding to the object type
	 * @throws CMSMFIOException
	 *             in the event of any I/O error
	 * @throws CMSMFFileNotFoundException
	 *             in the event of files that does not exist are being opened
	 */
	public InputStream getInputStreamForType(DctmObjectTypesEnum dctmObjectType) throws CMSMFIOException,
		CMSMFFileNotFoundException {

		InputStream returnIS = null;

		try {
			switch (dctmObjectType) {
				case DCTM_DOCUMENT:
					if (this.dctmDocsFileIS == null) {
						this.dctmDocsFileIS = createObjectInputStream(FileStreamsManager.DOCUMENTS_FILE_NAME);
					}
					returnIS = this.dctmDocsFileIS;
					break;
				case DCTM_FOLDER:
					if (this.dctmFldrsFileIS == null) {
						this.dctmFldrsFileIS = createObjectInputStream(FileStreamsManager.FOLDERS_FILE_NAME);
					}
					returnIS = this.dctmFldrsFileIS;
					break;
				case DCTM_USER:
					if (this.dctmUsrsFileIS == null) {
						this.dctmUsrsFileIS = createObjectInputStream(FileStreamsManager.USERS_FILE_NAME);
					}
					returnIS = this.dctmUsrsFileIS;
					break;
				case DCTM_GROUP:
					if (this.dctmGrpsFileIS == null) {
						this.dctmGrpsFileIS = createObjectInputStream(FileStreamsManager.GROUPS_FILE_NAME);
					}
					returnIS = this.dctmGrpsFileIS;
					break;
				case DCTM_ACL:
					if (this.dctmACLsFileIS == null) {
						this.dctmACLsFileIS = createObjectInputStream(FileStreamsManager.ACLS_FILE_NAME);
					}
					returnIS = this.dctmACLsFileIS;
					break;
				case DCTM_TYPE:
					if (this.dctmTypsFileIS == null) {
						this.dctmTypsFileIS = createObjectInputStream(FileStreamsManager.TYPES_FILE_NAME);
					}
					returnIS = this.dctmTypsFileIS;
					break;
				case DCTM_FORMAT:
					if (this.dctmFrmtsFileIS == null) {
						this.dctmFrmtsFileIS = createObjectInputStream(FileStreamsManager.FORMATS_FILE_NAME);
					}
					returnIS = this.dctmFrmtsFileIS;
					break;
				default:
					FileStreamsManager.logger.warn("Trying to get input stream for invalid object type!");
					break;
			}
		} catch (CMSMFIOException e) {
			throw (new CMSMFIOException("Couldn't create object output stream for " + dctmObjectType + " file.", e));
		}
		return returnIS;
	}

	/**
	 * Returns the OutputStream to export source repository information.
	 *
	 * @return the OutputStream for source repository configuration
	 * @throws CMSMFIOException
	 *             the cMSMFIO exception
	 */
	public OutputStream getSrcRepoConfigOS() throws CMSMFIOException {

		if (this.srcRepoConfigFileOS == null) {
			try {
				this.srcRepoConfigFileOS = createObjectOutputStream(FileStreamsManager.SRC_REPO_CONFIG_FILE_NAME);
			} catch (CMSMFIOException e) {
				throw (new CMSMFIOException("Couldn't create object output stream for Src Repo Config file.", e));
			}
		}
		return this.srcRepoConfigFileOS;
	}

	/**
	 * Creates the object output stream for a given filename. It checks the compress data flag in
	 * properties file to see if the data exported to the file system needs to be compressed or not.
	 *
	 * @param fileName
	 *            the file name
	 * @return the object output stream
	 * @throws CMSMFIOException
	 *             the CMSMFIO exception
	 */
	private OutputStream createObjectOutputStream(String fileName) throws CMSMFIOException {

		OutputStream os;
		// Make sure the streams directory exists
		createStreamsDirectory();
		// See if objects file exists
		// open the obj file
		File objFile = new File(this.streamsDirectoryPath, fileName);
		if (!objFile.exists()) {
			try {
				objFile.createNewFile();
			} catch (IOException e) {
				throw (new CMSMFIOException("Couldn't create file.", e));
			}
		}
		try {
			if (this.compressStreams) {
				os = new ObjectOutputStream(new GZIPOutputStream(new FileOutputStream(objFile)));
			} else {
				os = new ObjectOutputStream(new FileOutputStream(objFile));
			}
		} catch (FileNotFoundException e) {
			throw (new CMSMFIOException("Couldn't locate file.", e));
		} catch (IOException e) {
			throw (new CMSMFIOException("Couldn't create object output stream.", e));
		}

		return os;
	}

	/**
	 * Returns the InputStream from which to import source repository information.
	 *
	 * @return the InputStream for source repository configuration
	 * @throws CMSMFIOException
	 *             the cMSMFIO exception
	 * @throws CMSMFFileNotFoundException
	 *             the cMSMF file not found exception
	 */
	public InputStream getSrcRepoConfigIS() throws CMSMFIOException, CMSMFFileNotFoundException {
		if (this.srcRepoConfigFileIS == null) {
			try {
				this.srcRepoConfigFileIS = createObjectInputStream(FileStreamsManager.SRC_REPO_CONFIG_FILE_NAME);
			} catch (CMSMFIOException e) {
				throw (new CMSMFIOException("Couldn't create object input stream for src repo config file.", e));
			}
		}
		return this.srcRepoConfigFileIS;
	}

	/**
	 * Creates the input stream on a given file name and returns it. It checks the compress data
	 * flag in
	 * properties file to see if the data stored in the file system is compressed or not.
	 *
	 * @param fileName
	 *            the file name excluding the path
	 * @return the input stream
	 * @throws CMSMFIOException
	 *             the CMSMFIO exception
	 * @throws CMSMFFileNotFoundException
	 *             the cMSMF file not found exception
	 */
	private InputStream createObjectInputStream(String fileName) throws CMSMFIOException, CMSMFFileNotFoundException {
		InputStream is;
		// open the obj file
		File objFile = new File(this.streamsDirectoryPath, fileName);
		try {
			if (this.compressStreams) {
				is = new ObjectInputStream(new GZIPInputStream(new FileInputStream(objFile)));
			} else {
				is = new ObjectInputStream(new FileInputStream(objFile));
			}
		} catch (FileNotFoundException e) {
			throw (new CMSMFFileNotFoundException("Couldn't locate cmsmf stream file."));
		} catch (IOException e) {
			throw (new CMSMFIOException("Couldn't create object input stream.", e));
		}

		return is;
	}

	/**
	 * Closes all streams that may be open during application execution.
	 *
	 * @throws CMSMFIOException
	 *             the CMSMFIO exception
	 */
	public void closeAllStreams() throws CMSMFIOException {
		try {
			closeOutputStream(this.dctmDocsFileOS);
			closeOutputStream(this.dctmFldrsFileOS);
			closeOutputStream(this.dctmUsrsFileOS);
			closeOutputStream(this.dctmGrpsFileOS);
			closeOutputStream(this.dctmACLsFileOS);
			closeOutputStream(this.dctmTypsFileOS);
			closeOutputStream(this.dctmFrmtsFileOS);
			closeOutputStream(this.srcRepoConfigFileOS);
			closeInputStream(this.dctmDocsFileIS);
			closeInputStream(this.dctmFldrsFileIS);
			closeInputStream(this.dctmUsrsFileIS);
			closeInputStream(this.dctmGrpsFileIS);
			closeInputStream(this.dctmACLsFileIS);
			closeInputStream(this.dctmTypsFileIS);
			closeInputStream(this.dctmFrmtsFileIS);
			closeInputStream(this.srcRepoConfigFileIS);
		} catch (CMSMFIOException e) {
			throw (new CMSMFIOException("Couldn't close some of the object output stream files.", e));
		}
	}

	/**
	 * Closes output stream.
	 *
	 * @param os
	 *            the os
	 * @throws CMSMFIOException
	 *             the cMSMFIO exception
	 */
	private void closeOutputStream(OutputStream os) throws CMSMFIOException {
		if (os != null) {
			try {
				os.close();
			} catch (IOException e) {
				throw (new CMSMFIOException("Couldn't close object output stream file.", e));
			}
		}
	}

	/**
	 * Closes input stream.
	 *
	 * @param is
	 *            the is
	 * @throws CMSMFIOException
	 *             the CMSMFIO exception
	 */
	private void closeInputStream(InputStream is) throws CMSMFIOException {
		if (is != null) {
			try {
				is.close();
			} catch (IOException e) {
				throw (new CMSMFIOException("Couldn't close object input stream file.", e));
			}
		}

	}

	/**
	 * Deletes all stream files from the streams directory. This method only deletes
	 * files that may be used by cmsmf application (files with .cmsmf extension).
	 */
	public void deleteStreamFiles() {

		File directory = this.streamsDirectoryPath;

		if (directory.exists() && directory.isDirectory()) {
			// clean the directory
			FilenameFilter cmsmfExtension = new SuffixFileFilter(".cmsmf");
			File[] cmsmfFiles = directory.listFiles(cmsmfExtension);
			for (int i = 0; i < cmsmfFiles.length; i++) {
				File cmsmfFile = cmsmfFiles[i];
				cmsmfFile.delete();
			}

			// Delete the content files directory
			File contentDirectory = this.contentDirectoryPath;
			if (contentDirectory.exists() && contentDirectory.isDirectory()) {
				try {
					FileUtils.deleteDirectory(contentDirectory);
				} catch (IOException e) {
					FileStreamsManager.logger.error("Error deleting content files directory", e);
				}
			}
		}
	}

	/**
	 * Exports given object to a given output stream.
	 *
	 * @param os
	 *            the OutputStream where the object will be written
	 * @param object
	 *            the object that is being written
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	public void exportObject(OutputStream os, Object object) throws IOException {
		if (os != null) {
			if (os instanceof ObjectOutputStream) {
				((ObjectOutputStream) os).writeObject(object);
// ((ObjectOutputStream) os).writeUnshared(object);
				((ObjectOutputStream) os).reset();

			}
		} else {
			throw (new CMSMFIOException("OutputStream is null. Could not write object to outputstream."));
		}
	}

	/**
	 * Imports an object from a given input stream.
	 *
	 * @param is
	 *            the inputstream from which the object is read
	 * @return the object
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 * @throws ClassNotFoundException
	 *             the class not found exception
	 */
	public Object importObject(InputStream is) throws IOException, ClassNotFoundException {
		Object returnObject = null;
		if (is instanceof ObjectInputStream) {
			returnObject = ((ObjectInputStream) is).readObject();
		}
		return returnObject;
	}
}
