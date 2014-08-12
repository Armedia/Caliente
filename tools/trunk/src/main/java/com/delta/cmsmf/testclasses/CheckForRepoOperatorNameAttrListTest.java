package com.delta.cmsmf.testclasses;

import java.io.IOException;
import java.util.List;

import org.apache.commons.configuration.ConfigurationException;

import com.delta.cmsmf.constants.CMSMFAppConstants;
import com.delta.cmsmf.exception.CMSMFException;
import com.delta.cmsmf.properties.PropertiesManager;
import com.delta.cmsmf.runtime.RunTimeProperties;
import com.documentum.fc.common.DfException;

public class CheckForRepoOperatorNameAttrListTest {

	/**
	 * The main method.
	 * 
	 * @param args
	 *            the arguments
	 * @throws DfException
	 *             Signals that Dctm Server error has occurred.
	 * @throws CMSMFException
	 * @throws ConfigurationException
	 * @throws IOException
	 */
	public static void main(String[] args) throws DfException, CMSMFException, ConfigurationException, IOException {
		PropertiesManager.addPropertySource(CMSMFAppConstants.FULLY_QUALIFIED_CONFIG_FILE_NAME);
		PropertiesManager.init();

		List<String> attrsToCheckForRepoOperatorName = RunTimeProperties.getRunTimePropertiesInstance()
			.getAttrsToCheckForRepoOperatorName();

		System.out.println(attrsToCheckForRepoOperatorName);

		System.out.println("Done!");

	}

}
