package com.delta.cmsmf.mainEngine;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import com.delta.cmsmf.constants.DctmAttrNameConstants;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.client.IDfTypedObject;
import com.documentum.fc.common.DfException;

// TODO: Auto-generated Javadoc
/**
 * The Class RepositoryConfiguration is a utility class that contains methods to
 * retrieve various configuration parameters of the documentum repository. This class
 * implements singleton design pattern to maintain a single instance of this class
 * during application execution.
 * <p>
 * It also contains a list field called fileStores to store all of the file store names encountered
 * during the export step. The singleton instance of this class is serialized towards the end of the
 * export step. During import step, this instance is read from the file system and the application
 * checks to see if all of the file stores that were exported during export step exists in the
 * target repository. If they do exist then only the import process continues.
 *
 * @author Shridev Makim 6/15/2010
 */
public class RepositoryConfiguration implements Serializable {

	/** The Constant serialVersionUID. */
	private static final long serialVersionUID = 1L;

	/** The logger object used for logging. */
	static Logger logger = Logger.getLogger(RepositoryConfiguration.class);

	/**
	 * Instantiates a new repository configuration. Private constructor to prevent
	 * new instances being created.
	 */
	private RepositoryConfiguration() {
		// no code here; this is a singleton class so private constructor
	}

	/**
	 * Gets the singleton instance of the repository configuration class.
	 *
	 * @return the repository configuration instance
	 */
	public static synchronized RepositoryConfiguration getRepositoryConfiguration() {
// if (singletonInstance == null)
// // we can call this private constructor
// singletonInstance = new RepositoryConfiguration();
		return RepositoryConfiguration.singletonInstance;
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

	/** The singleton instance. */
	private static RepositoryConfiguration singletonInstance = new RepositoryConfiguration();;

	// Info from server configuration
	/** The operator name. */
	private String operatorName;

	/**
	 * Gets the operator name.
	 *
	 * @return the operator name
	 */
	public String getOperatorName() {
		return this.operatorName;
	}

	/**
	 * Sets the operator name.
	 *
	 * @param operatorName
	 *            the new operator name
	 */
	public void setOperatorName(String operatorName) {
		this.operatorName = operatorName;
	}

	// Info from server configuration
	/** The owner name. */
	private String ownerName;

	/**
	 * Gets the owner name.
	 *
	 * @return the owner name
	 */
	public String getOwnerName() {
		return this.ownerName;
	}

	/**
	 * Sets the owner name.
	 *
	 * @param ownerName
	 *            the new operator name
	 */
	public void setOwnerName(String ownerName) {
		this.ownerName = ownerName;
	}

	/** The install owner. */
	private String installOwner;

	/**
	 * Gets the install owner.
	 *
	 * @return the install owner
	 */
	public String getInstallOwner() {
		return this.installOwner;
	}

	/**
	 * Sets the install owner.
	 *
	 * @param installOwner
	 *            the new install owner
	 */
	public void setInstallOwner(String installOwner) {
		this.installOwner = installOwner;
	}

	/** The server version. */
	private String serverVersion;

	/**
	 * Gets the server version.
	 *
	 * @return the server version
	 */
	public String getServerVersion() {
		return this.serverVersion;
	}

	/**
	 * Sets the server version.
	 *
	 * @param serverVersion
	 *            the new server version
	 */
	public void setServerVersion(String serverVersion) {
		this.serverVersion = serverVersion;
	}

	/** The host name. */
	private String hostName;

	/**
	 * Gets the host name.
	 *
	 * @return the host name
	 */
	public String getHostName() {
		return this.hostName;
	}

	/**
	 * Sets the host name.
	 *
	 * @param hostName
	 *            the new host name
	 */
	public void setHostName(String hostName) {
		this.hostName = hostName;
	}

	// From docbase config
	/** The database name. */
	private String dbmsName;

	/**
	 * Gets the database name.
	 *
	 * @return the database name
	 */
	public String getDbmsName() {
		return this.dbmsName;
	}

	/**
	 * Sets the database name.
	 *
	 * @param dbmsName
	 *            the new database name
	 */
	public void setDbmsName(String dbmsName) {
		this.dbmsName = dbmsName;
	}

	/** The security mode. */
	private String securityMode;

	/**
	 * Gets the security mode.
	 *
	 * @return the security mode
	 */
	public String getSecurityMode() {
		return this.securityMode;
	}

	/**
	 * Sets the security mode.
	 *
	 * @param securityMode
	 *            the new security mode
	 */
	public void setSecurityMode(String securityMode) {
		this.securityMode = securityMode;
	}

	/** The docbase id. */
	private String docbaseID;

	/**
	 * Gets the docbase id.
	 *
	 * @return the docbase id
	 */
	public String getDocbaseID() {
		return this.docbaseID;
	}

	/**
	 * Sets the docbase id.
	 *
	 * @param docbaseID
	 *            the new docbase id
	 */
	public void setDocbaseID(String docbaseID) {
		this.docbaseID = docbaseID;
	}

	// From connection config
	/** The docbase name. */
	private String docbaseName;

	/**
	 * Gets the docbase name.
	 *
	 * @return the docbase name
	 */
	public String getDocbaseName() {
		return this.docbaseName;
	}

	/**
	 * Sets the docbase name.
	 *
	 * @param docbaseName
	 *            the new docbase name
	 */
	public void setDocbaseName(String docbaseName) {
		this.docbaseName = docbaseName;
	}

	/**
	 * The list of file stores that are used by the objects exported during
	 * export step.
	 */
	private List<String> fileStores = new ArrayList<String>();

	/**
	 * Gets the list of file stores.
	 *
	 * @return the file stores
	 */
	public List<String> getFileStores() {
		return this.fileStores;
	}

	/**
	 * Sets the file stores.
	 *
	 * @param fileStores
	 *            the new file stores
	 */
	public void setFileStores(List<String> fileStores) {
		this.fileStores = fileStores;
	}

	/**
	 * Adds the file store.
	 *
	 * @param fileStoreName
	 *            the file store name
	 */
	public void addFileStore(String fileStoreName) {
		if (!this.fileStores.contains(fileStoreName)) {
			this.fileStores.add(fileStoreName);
		}
	}

	/**
	 * Loads various repository configuration parameters using given docbase session.
	 *
	 * @param dctmSession
	 *            the dctm session
	 * @throws DfException
	 *             the df exception
	 */
	public void loadRepositoryConfiguration(IDfSession dctmSession) throws DfException {

		if (RepositoryConfiguration.logger.isEnabledFor(Level.INFO)) {
			RepositoryConfiguration.logger.info("Started loading repository configuration");
		}

		// Get Server Config Attributes
		IDfTypedObject serverConfig = dctmSession.getServerConfig();
		this.operatorName = serverConfig.getString(DctmAttrNameConstants.OPERATOR_NAME);
		this.ownerName = serverConfig.getString(DctmAttrNameConstants.OWNER_NAME);
		this.installOwner = serverConfig.getString(DctmAttrNameConstants.R_INSTALL_OWNER);
		this.serverVersion = serverConfig.getString(DctmAttrNameConstants.R_SERVER_VERSION);
		this.hostName = serverConfig.getString(DctmAttrNameConstants.R_HOST_NAME);

		// Get Docbase Config Attributes
		IDfTypedObject docbaseConfig = dctmSession.getDocbaseConfig();
		this.dbmsName = docbaseConfig.getString(DctmAttrNameConstants.R_DBMS_NAME);
		this.securityMode = docbaseConfig.getString(DctmAttrNameConstants.SECURITY_MODE);
		this.docbaseID = docbaseConfig.getString(DctmAttrNameConstants.R_DOCBASE_ID);

		// Get Connection Config Attributes
		IDfTypedObject connectionConfig = dctmSession.getConnectionConfig();
		this.docbaseName = connectionConfig.getString(DctmAttrNameConstants.R_DOCBASE_NAME);

		// Print repository configuration
		if (RepositoryConfiguration.logger.isEnabledFor(Level.DEBUG)) {
			RepositoryConfiguration.logger.debug(printRepositoryConfiguration());
		}

		if (RepositoryConfiguration.logger.isEnabledFor(Level.INFO)) {
			RepositoryConfiguration.logger.info("Finished loading repository configuration");
		}
	}

	/**
	 * Returns the repository configuration as a String.
	 *
	 * @return the string
	 */
	public String printRepositoryConfiguration() {

		String resultStr = "";
		String newLine = System.getProperty("line.separator");
		StringBuffer sb = new StringBuffer();
		sb.append(newLine);
		sb.append("*** Server Configuration Attributes ***");
		sb.append(newLine);
		sb.append("Operator Name: " + this.operatorName);
		sb.append(newLine);
		sb.append("Owner Name: " + this.ownerName);
		sb.append(newLine);
		sb.append("Install Owner: " + this.installOwner);
		sb.append(newLine);
		sb.append("Server Version: " + this.serverVersion);
		sb.append(newLine);
		sb.append("Host Name: " + this.hostName);
		sb.append(newLine);
		sb.append("*** Docbase Configuration Attributes ***");
		sb.append(newLine);
		sb.append("DBMS Name: " + this.dbmsName);
		sb.append(newLine);
		sb.append("Security Mode: " + this.securityMode);
		sb.append(newLine);
		sb.append("Docbase ID: " + this.docbaseID);
		sb.append(newLine);
		sb.append("*** Connection Configuration Attributes ***");
		sb.append(newLine);
		sb.append("Docbase Name: " + this.docbaseName);
		sb.append(newLine);

		resultStr = sb.toString();
		return resultStr;
	}

}
