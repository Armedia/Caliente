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
import java.io.FileNotFoundException;
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
		String error = null;
		if ((error == null) && !f.exists()) { return "does not exist"; }
		if ((error == null) && !f.isFile()) { return "is not a regular file"; }
		if ((error == null) && !f.canRead()) { return "cannot be read"; }
		return null;
	}

	@Override
	public Collection<URL> getClasspathPatches(OptionValues baseValues, OptionValues commandValues) {
		final boolean dfcFound = checkForDfc();
		List<URL> ret = new ArrayList<>(3);
		try {

			File dfcProps = null;

			// First try the parameter value ... if it's set, then it MUST exist or
			// it's an error
			String var = commandValues.getString(this.paramDfcProp, StringUtils.EMPTY);
			if (!StringUtils.isEmpty(var)) {
				dfcProps = CliUtils.newFileObject(var);
				String error = checkExistingFile(dfcProps);
				if (error != null) {
					throw new RuntimeException(String.format("The DFC Properties file at [%s] %s", dfcProps, error));
				}
			} else {
				// No parameter given, try the one on the current directory ...
				dfcProps = CliUtils.newFileObject(DfcLaunchHelper.DEFAULT_DFC_PROPERTIES);
				String error = checkExistingFile(dfcProps);
				if (error != null) {
					dfcProps = null;
				}
			}

			// We found a DFC Properties file ... use it! Otherwise the DFC will use its defaults
			if (dfcProps != null) {
				System.setProperty(DfcLaunchHelper.DFC_PROPERTIES_PROP, dfcProps.getAbsolutePath());
				this.log.info("Will use the DFC Properties file at [{}]", dfcProps.getAbsolutePath());
			}

			// Next, add ${DOCUMENTUM}/config to the classpath
			var = commandValues.getString(this.paramDctm, System.getenv(DfcLaunchHelper.ENV_DOCUMENTUM));
			// Go with the environment
			if (var == null) {
				String msg = String.format("The environment variable [%s] is not set", DfcLaunchHelper.ENV_DOCUMENTUM);
				if (!dfcFound) { throw new RuntimeException(msg); }
				// this.log.warn("{}, integrated DFC may encounter errors", msg);
			} else {
				File f = CliUtils.newFileObject(var);
				if (!f.exists()) {
					FileUtils.forceMkdir(f);
				}
				if (!f.isDirectory()) {
					throw new FileNotFoundException(
						String.format("Could not find the directory [%s]", f.getAbsolutePath()));
				}

				ret.add(CliUtils.newFileObject(f, "config").toURI().toURL());
			}

			// Next, identify the DOCUMENTUM_SHARED location, and if dctm.jar is in there
			var = commandValues.getString(this.paramDfc, System.getenv(DfcLaunchHelper.ENV_DOCUMENTUM_SHARED));
			// Go with the environment
			if (var == null) {
				String msg = String.format("The environment variable [%s] is not set",
					DfcLaunchHelper.ENV_DOCUMENTUM_SHARED);
				if (!dfcFound) { throw new RuntimeException(msg); }
				// this.log.warn("{}, integrated DFC may encounter errors", msg);
			}

			if (var != null) {
				// Next, is it a directory?
				File f = CliUtils.newFileObject(var);
				if (!f.isDirectory()) {
					throw new FileNotFoundException(String.format("Could not find the [%s] directory [%s]",
						DfcLaunchHelper.ENV_DOCUMENTUM_SHARED, f.getAbsolutePath()));
				}

				// Next, does dctm.jar exist in there?
				if (!dfcFound) {
					File tgt = CliUtils.newFileObject(f, DfcLaunchHelper.DCTM_JAR);
					if (!tgt.isFile()) {
						throw new FileNotFoundException(
							String.format("Could not find the JAR file [%s]", tgt.getAbsolutePath()));
					}

					// Next, to the classpath
					ret.add(tgt.toURI().toURL());
				}
			}
		} catch (IOException e) {
			throw new RuntimeException("Failed to configure the dynamic DFC classpath", e);
		}
		return ret;
	}
}
