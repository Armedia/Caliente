package com.delta.cmsmf.runtime;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.text.StrTokenizer;

import com.delta.cmsmf.cfg.Setting;
import com.delta.cmsmf.constants.DctmAttrNameConstants;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.common.DfException;

/**
 * The Class RunTimeProperties. This class implements singleton design pattern to maintain runtime
 * properties during the execution of cmsmf application. These properties are used internally by the
 * application and not set by the user in properties file.
 *
 * @author Shridev Makim 6/15/2010
 */
public class RunTimeProperties {

	/** The singleton instance. */
	private static RunTimeProperties singletonInstance;

	/** The target repository operator name. */
	private String targetRepoOperatorName = "";

	/** The list of attribute names whose value should be checked for repository operator name. */
	public Set<String> attrsToCheckForRepoOperatorName = null;

	/** The import process error count. */
	private AtomicInteger importProcessErrorCount = new AtomicInteger(0);

	/**
	 * Instantiates a new run time properties. Private constructor to prevent new instances being
	 * created.
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

	/**
	 * Gets the list of attribute names to check for repository operator name.
	 *
	 * @return the list of attribute names to check for repository operator name
	 */
	@SuppressWarnings("unchecked")
	public Set<String> getAttrsToCheckForRepoOperatorName() {
		if (this.attrsToCheckForRepoOperatorName == null) {
			String attrsToCheck = Setting.OWNER_ATTRIBUTES.getString();
			StrTokenizer strTokenizer = StrTokenizer.getCSVInstance(attrsToCheck);
			this.attrsToCheckForRepoOperatorName = new HashSet<String>(strTokenizer.getTokenList());
		}
		return this.attrsToCheckForRepoOperatorName;
	}

	/**
	 * Gets the current import process error count.
	 *
	 * @return the import process error count
	 */
	public int getImportProcessErrorCount() {
		return this.importProcessErrorCount.get();
	}

	/**
	 * Increments import process error count by 1.
	 */
	public int incrementImportProcessErrorCount() {
		return this.importProcessErrorCount.incrementAndGet();
	}

}
