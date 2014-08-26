package com.delta.cmsmf.runtime;

import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.text.StrTokenizer;

import com.delta.cmsmf.constants.DctmAttrNameConstants;
import com.delta.cmsmf.properties.CMSMFProperties;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.common.DfException;

/**
 * The Class RunTimeProperties. This class implements singleton design pattern to maintain runtime
 * properties
 * during the execution of cmsmf application. These properties are used internally by the
 * application and not
 * set by the user in properties file.
 *
 * @author Shridev Makim 6/15/2010
 */
public class RunTimeProperties {

	/**
	 * Instantiates a new run time properties. Private constructor to prevent
	 * new instances being created.
	 */
	private RunTimeProperties() {
		// no code here; this is a singleton class so private constructor
	}

	/**
	 * Gets the singleton instance of the run time properties class.
	 *
	 * @return the run time properties instance
	 */
	public static synchronized RunTimeProperties getRunTimePropertiesInstance() {
		if (RunTimeProperties.singletonInstance == null) {
			// we can call this private constructor
			RunTimeProperties.singletonInstance = new RunTimeProperties();
		}
		return RunTimeProperties.singletonInstance;
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
	private static RunTimeProperties singletonInstance;

	/** The target repository operator name. */
	private String targetRepoOperatorName = "";

	/**
	 * Gets the target repository operator name.
	 *
	 * @param dctmSession
	 *            the existing docbase session
	 * @return the target repository operator name
	 * @throws DfException
	 *             the df exception
	 */
	public String getTargetRepoOperatorName(IDfSession dctmSession) throws DfException {
		if (StringUtils.isBlank(this.targetRepoOperatorName)) {
			// read the repository operator name from server config object
			this.targetRepoOperatorName = dctmSession.getServerConfig().getString(DctmAttrNameConstants.OPERATOR_NAME);
		}
		return this.targetRepoOperatorName;
	}

	/** The list of attribute names whose value should be checked for repository operator name. */
	public List<String> attrsToCheckForRepoOperatorName = null;

	/**
	 * Gets the list of attribute names to check for repository operator name.
	 *
	 * @return the list of attribute names to check for repository operator name
	 */
	@SuppressWarnings("unchecked")
	public List<String> getAttrsToCheckForRepoOperatorName() {
		if (this.attrsToCheckForRepoOperatorName == null) {
			String attrsToCheck = CMSMFProperties.OWNER_ATTRIBUTES.getString();
			StrTokenizer strTokenizer = StrTokenizer.getCSVInstance(attrsToCheck);
			this.attrsToCheckForRepoOperatorName = strTokenizer.getTokenList();
		}
		return this.attrsToCheckForRepoOperatorName;
	}

	/** The import process error count. */
	private int importProcessErrorCount = 0;

	/**
	 * Gets the current import process error count.
	 *
	 * @return the import process error count
	 */
	public int getImportProcessErrorCount() {
		return this.importProcessErrorCount;
	}

	/**
	 * Increments import process error count by 1.
	 */
	public void incrementImportProcessErrorCount() {
		this.importProcessErrorCount++;
	}

}
