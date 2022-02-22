/*******************************************************************************
 * #%L
 * Armedia Caliente
 * %%
 * Copyright (C) 2013 - 2022 Armedia, LLC
 * %%
 * This file is part of the Caliente software.
 *
 * If the software was purchased under a paid Caliente license, the terms of
 * the paid license agreement will prevail.  Otherwise, the software is
 * provided under the following open source license terms:
 *
 * Caliente is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Caliente is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Caliente. If not, see <http://www.gnu.org/licenses/>.
 * #L%
 *******************************************************************************/
package com.armedia.caliente.tools.dfc.cli;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.commons.utilities.Tools;
import com.armedia.commons.utilities.cli.Option;
import com.armedia.commons.utilities.cli.OptionGroup;
import com.armedia.commons.utilities.cli.OptionGroupImpl;
import com.armedia.commons.utilities.cli.OptionImpl;
import com.armedia.commons.utilities.cli.OptionValues;
import com.armedia.commons.utilities.cli.Options;
import com.armedia.commons.utilities.cli.launcher.LaunchClasspathHelper;
import com.armedia.commons.utilities.cli.utils.CliUtils;
import com.armedia.commons.utilities.cli.utils.CliValuePrompt;

public final class DfcLaunchHelper extends Options implements LaunchClasspathHelper {

	private static final String DFC_PROPERTIES_PROP = "dfc.properties.file";
	private static final String DEFAULT_DFC_PROPERTIES = "dfc.properties";
	private static final String ENV_DOCUMENTUM_SHARED = "DOCUMENTUM_SHARED";
	private static final String ENV_DOCUMENTUM = "DOCUMENTUM";
	private static final String DCTM_JAR = "dctm.jar";
	private static final String DFC_TEST_CLASS = "com.documentum.fc.client.IDfFolder";

	public static final Option DFC_LOCATION = new OptionImpl() //
		.setLongOpt("dfc") //
		.setArgumentLimits(1) //
		.setArgumentName("dfc-install-location") //
		.setDescription("The path where DFC is installed (i.e. instead of DOCUMENTUM_SHARED)") //
	;
	public static final Option DFC_DOCUMENTUM = new OptionImpl() //
		.setLongOpt("dctm") //
		.setArgumentLimits(1) //
		.setArgumentName("directory") //
		.setDescription("The user's local Documentum path (i.e. instead of DOCUMENTUM)") //
	;
	public static final Option DFC_PROPERTIES = new OptionImpl() //
		.setLongOpt("dfc-prop") //
		.setArgumentLimits(1) //
		.setArgumentName("dfc.properties-location") //
		.setDescription("The dfc.properties file to use instead of the default") //
	;
	public static final Option DFC_DOCBASE = new OptionImpl() //
		.setLongOpt("docbase") //
		.setRequired(true) //
		.setArgumentLimits(1) //
		.setArgumentName("docbase") //
		.setDescription("The Documentum repostory name to connect to") //
	;
	public static final Option DFC_USER = new OptionImpl() //
		.setLongOpt("dctm-user") //
		.setRequired(true) //
		.setArgumentLimits(1) //
		.setArgumentName("username") //
		.setDescription("The username to connect to Documentum with") //
	;
	public static final Option DFC_PASSWORD = new OptionImpl() //
		.setLongOpt("dctm-pass") //
		.setArgumentLimits(1) //
		.setArgumentName("password") //
		.setDescription("The password to connect to Documentum with") //
	;
	public static final Option DFC_UNIFIED = new OptionImpl() //
		.setLongOpt("dctm-unified") //
		.setDescription("Utilize unified login mode") //
	;

	private final Logger log = LoggerFactory.getLogger(getClass());

	private final Option paramDfc;
	private final Option paramDctm;
	private final Option paramDfcProp;

	private final boolean includesConnectionInfo;
	private final Option paramDocbase;
	private final Option paramUser;
	private final Option paramPassword;
	private final Option paramUnified;

	private final OptionGroupImpl group;

	public DfcLaunchHelper(boolean includesConnectionInfo) {
		this.paramDfc = DfcLaunchHelper.DFC_LOCATION;
		this.paramDctm = DfcLaunchHelper.DFC_DOCUMENTUM;
		this.paramDfcProp = DfcLaunchHelper.DFC_PROPERTIES;
		this.includesConnectionInfo = includesConnectionInfo;

		OptionGroupImpl group = new OptionGroupImpl(
			String.format("DFC%s", includesConnectionInfo ? " connectivity" : "")) //
				.add(this.paramDfcProp) //
				.add(this.paramDfc) //
				.add(this.paramDctm) //
		;

		if (includesConnectionInfo) {
			this.paramDocbase = DfcLaunchHelper.DFC_DOCBASE;
			this.paramUser = DfcLaunchHelper.DFC_USER;
			this.paramPassword = DfcLaunchHelper.DFC_PASSWORD;
			this.paramUnified = DfcLaunchHelper.DFC_UNIFIED;
		} else {
			this.paramDocbase = null;
			this.paramUser = null;
			this.paramPassword = null;
			this.paramUnified = null;
		}

		if (this.includesConnectionInfo) {
			group //
				.add(this.paramDocbase) //
				.add(this.paramUser) //
				.add(this.paramPassword) //
				.add(this.paramUnified) //
			;
		}
		this.group = group;
	}

	public String getDfcUser(OptionValues cli) {
		if (!this.includesConnectionInfo) { return null; }
		return cli.getString(this.paramUser);
	}

	public String getDfcDocbase(OptionValues cli) {
		if (!this.includesConnectionInfo) { return null; }
		return cli.getString(this.paramDocbase);
	}

	public String getDfcPassword(OptionValues cli) {
		if (!this.includesConnectionInfo) { return null; }

		String dctmUser = getDfcUser(cli);
		String docbase = getDfcDocbase(cli);
		return CliValuePrompt.getPasswordString(cli, this.paramPassword,
			"Please enter the Password for user [%s] in Docbase %s: ", Tools.coalesce(dctmUser, ""), docbase);
	}

	public boolean checkForDfc() {
		try {
			Class.forName(DfcLaunchHelper.DFC_TEST_CLASS);
			return true;
		} catch (Exception e) {
			// ignore the exception, for now
			return false;
		}
	}

	@Override
	public OptionGroup asGroup(String name) {
		return this.group.getCopy(name);
	}

	private String checkExistingFile(File f) {
		if (!f.exists()) { return "does not exist"; }
		if (!f.isFile()) { return "is not a regular file"; }
		if (!f.canRead()) { return "cannot be read"; }
		return null;
	}

	private String checkExistingDirectory(File f) {
		if (!f.exists()) { return "does not exist"; }
		if (!f.isDirectory()) { return "is not a directory"; }
		if (!f.canRead()) { return "cannot be read"; }
		return null;
	}

	private File findDfcProperties(OptionValues commandValues) throws Exception {
		File dfcProps = null;

		// First try the parameter value ... if it's set, then it MUST exist or
		// it's an error
		String var = commandValues.getString(this.paramDfcProp);
		if (var != null) {
			if (StringUtils.isEmpty(var)) {
				// I don't think this is possible, but check it anyway
				throw new Exception(
					"The --" + this.paramDfcProp.getLongOpt() + " parameter must be given a non-empty string");
			}
			dfcProps = CliUtils.newFileObject(var);
			String error = checkExistingFile(dfcProps);
			if (error == null) { return dfcProps; }
			throw new Exception("The DFC Properties at [" + dfcProps + "] " + error);
		} else if (commandValues.isPresent(this.paramDfcProp)) {
			throw new Exception("The --" + this.paramDfcProp.getLongOpt() + " parameter must be given a string value");
		}

		// No parameter given, try the one on the current directory ...
		dfcProps = CliUtils.newFileObject(DfcLaunchHelper.DEFAULT_DFC_PROPERTIES);
		String error = checkExistingFile(dfcProps);
		if (error == null) { return dfcProps; }

		// If we reach this point, then we're telling the DFC to use whatever default it sees fit
		return null;
	}

	private File findParameterizedFolder(OptionValues commandValues, boolean mustExist, Option param, String envVar)
		throws Exception {
		if (param != null) {
			String var = commandValues.getString(param);
			if (var != null) {
				if (StringUtils.isEmpty(var)) {
					// I don't think this is possible, but check it anyway
					throw new IOException(
						"The --" + param.getLongOpt() + " parameter must be given a non-empty string");
				}
				File directory = CliUtils.newFileObject(var);
				String error = checkExistingDirectory(directory);
				if ((error == null) || !mustExist) { return directory; }
				throw new Exception("The directory at [" + directory + "] " + error);
			} else if (commandValues.isPresent(param)) {
				throw new Exception("The --" + param.getLongOpt() + " parameter must be given a string value");
			}
		}

		if (StringUtils.isNotEmpty(envVar)) {
			String var = System.getenv(envVar);
			if (var == null) {
				throw new Exception("The environment variable [" + envVar
					+ "] is not set, can't set the Documentum working directory without it or a parameter pointing to it (--"
					+ param.getLongOpt() + ")");
			}
			File directory = CliUtils.newFileObject(var);
			String error = checkExistingDirectory(directory);
			if ((error == null) || !mustExist) { return directory; }
			throw new Exception("The Documentum working directory at [" + directory + "] " + error);
		}

		return null;
	}

	@Override
	public Collection<URL> getClasspathPatches(OptionValues baseValues, OptionValues commandValues) {
		List<URL> ret = new ArrayList<>(3);
		try {

			File dfcProps = findDfcProperties(commandValues);
			// We found a DFC Properties file ... use it! Otherwise the DFC will use its defaults
			if (dfcProps != null) {
				System.setProperty(DfcLaunchHelper.DFC_PROPERTIES_PROP, dfcProps.getAbsolutePath());
				this.log.info("Will use the DFC Properties file at [{}]", dfcProps.getAbsolutePath());
			}

			// Next, add ${DOCUMENTUM}/config to the classpath
			File dfcWork = findParameterizedFolder(commandValues, false, this.paramDctm,
				DfcLaunchHelper.ENV_DOCUMENTUM);
			if (!dfcWork.exists()) {
				FileUtils.forceMkdir(dfcWork);
			}
			if (!dfcWork.isDirectory()) {
				throw new Exception("Could not find or create the directory [" + dfcWork + " ]");
			}

			ret.add(CliUtils.newFileObject(dfcWork, "config").toURI().toURL());

			// Next, identify the DOCUMENTUM_SHARED location, and if dctm.jar is in there
			File dfcLocation = findParameterizedFolder(commandValues, true, this.paramDfc,
				DfcLaunchHelper.ENV_DOCUMENTUM_SHARED);
			if (dfcLocation == null) {
				throw new Exception(
					"The DFC Location could not be discovered from parameters or environment variables");
			}

			File dctmJar = CliUtils.newFileObject(dfcLocation, DfcLaunchHelper.DCTM_JAR);
			String error = checkExistingFile(dctmJar);
			if (error != null) {
				throw new Exception(
					"Could not find the JAR file dctm.jar in the DFC location [" + dfcLocation + "]: " + error);
			}

			// Next, add dctm.jar to the classpath
			ret.add(dctmJar.toURI().toURL());
		} catch (Exception e) {
			throw new RuntimeException("Failed to configure the dynamic DFC classpath", e);
		}
		return ret;
	}
}
