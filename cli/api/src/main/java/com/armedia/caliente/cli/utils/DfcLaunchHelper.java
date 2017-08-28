package com.armedia.caliente.cli.utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.io.FileUtils;

import com.armedia.caliente.cli.Option;
import com.armedia.caliente.cli.OptionImpl;
import com.armedia.caliente.cli.OptionValues;
import com.armedia.caliente.cli.launcher.LaunchClasspathHelper;
import com.armedia.caliente.cli.launcher.LaunchOptionSet;
import com.armedia.commons.utilities.Tools;

public final class DfcLaunchHelper implements LaunchClasspathHelper, LaunchOptionSet {
	private static final String DFC_PROPERTIES_PROP = "dfc.properties.file";
	private static final String DEFAULT_DFC_PROPERTIES = "dfc.properties";
	private static final String ENV_DOCUMENTUM_SHARED = "DOCUMENTUM_SHARED";
	private static final String ENV_DOCUMENTUM = "DOCUMENTUM";
	private static final String DCTM_JAR = "dctm.jar";
	private static final String DFC_TEST_CLASS = "com.documentum.fc.client.IDfFolder";

	private static final Option DFC_LOCATION = new OptionImpl() //
		.setLongOpt("dfc") //
		.setMinValueCount(1) //
		.setMaxValueCount(1) //
		.setValueName("dfc install location") //
		.setDescription("The path where DFC is installed (i.e. instead of DOCUMENTUM_SHARED)");
	private static final Option DFC_DOCUMENTUM = new OptionImpl() //
		.setLongOpt("dctm") //
		.setMinValueCount(1) //
		.setMaxValueCount(1) //
		.setValueName("directory") //
		.setDescription("The user's local Documentum path (i.e. instead of DOCUMENTUM)");
	private static final Option DFC_PROPERTIES = new OptionImpl() //
		.setLongOpt("dfc-prop") //
		.setMinValueCount(1) //
		.setMaxValueCount(1) //
		.setValueName("dfc.properties location") //
		.setDescription("The dfc.properties file to use instead of the default");
	private static final Option DFC_DOCBASE = new OptionImpl() //
		.setLongOpt("docbase") //
		.setRequired(true) //
		.setMinValueCount(1) //
		.setMaxValueCount(1) //
		.setValueName("docbase") //
		.setDescription("The Documentum repostory name to connect to");
	private static final Option DFC_USER = new OptionImpl() //
		.setLongOpt("dctm-user") //
		.setRequired(true) //
		.setMinValueCount(1) //
		.setMaxValueCount(1) //
		.setValueName("username") //
		.setDescription("The username to connect to Documentum with");
	private static final Option DFC_PASSWORD = new OptionImpl() //
		.setLongOpt("dctm-pass") //
		.setMinValueCount(1) //
		.setMaxValueCount(1) //
		.setValueName("password") //
		.setDescription("The password to connect to Documentum with");

	private final Option paramDfc;
	private final Option paramDctm;
	private final Option paramDfcProp;

	private final boolean includesConnectionInfo;
	private final Option paramDocbase;
	private final Option paramUser;
	private final Option paramPassword;

	public DfcLaunchHelper(boolean includesConnectionInfo) {
		this.paramDfc = DfcLaunchHelper.DFC_LOCATION;
		this.paramDctm = DfcLaunchHelper.DFC_DOCUMENTUM;
		this.paramDfcProp = DfcLaunchHelper.DFC_PROPERTIES;
		this.includesConnectionInfo = includesConnectionInfo;
		if (includesConnectionInfo) {
			this.paramDocbase = DfcLaunchHelper.DFC_DOCBASE;
			this.paramUser = DfcLaunchHelper.DFC_USER;
			this.paramPassword = DfcLaunchHelper.DFC_PASSWORD;
		} else {
			this.paramDocbase = null;
			this.paramUser = null;
			this.paramPassword = null;
		}
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

		String dctmUser = getDfcDocbase(cli);
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
	public Collection<? extends Option> getOptions() {
		ArrayList<Option> ret = new ArrayList<>();
		ret.add(this.paramDfcProp);
		ret.add(this.paramDfc);
		ret.add(this.paramDctm);
		if (this.includesConnectionInfo) {
			ret.add(this.paramDocbase);
			ret.add(this.paramUser);
			ret.add(this.paramPassword);
		}
		return ret;
	}

	@Override
	public Collection<URL> getClasspathPatchesPre(OptionValues cli) {
		final boolean dfcFound = checkForDfc();
		List<URL> ret = new ArrayList<>(3);
		try {

			// Even if not configured, if there's a dfc.properties in our current working directory,
			// we try to use it
			String var = cli.getString(this.paramDfcProp, DfcLaunchHelper.DEFAULT_DFC_PROPERTIES);
			if (var != null) {
				File f = CliUtils.newFileObject(var);
				String error = null;
				if ((error == null) && !f.exists()) {
					error = "does not exist";
				}
				if ((error == null) && !f.isFile()) {
					error = "is not a regular file";
				}
				if ((error == null) && !f.canRead()) {
					error = "cannot be read";
				}
				if (error == null) {
					System.setProperty(DfcLaunchHelper.DFC_PROPERTIES_PROP, f.getAbsolutePath());
					/*
					} else {
						this.log.warn("The DFC properties file [{}] {} - will continue using DFC defaults",
							f.getAbsolutePath(), error);
					*/
				}
			}

			// Next, add ${DOCUMENTUM}/config to the classpath
			var = cli.getString(this.paramDctm, System.getenv(DfcLaunchHelper.ENV_DOCUMENTUM));
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
				if (!f.isDirectory()) { throw new FileNotFoundException(
					String.format("Could not find the directory [%s]", f.getAbsolutePath())); }

				ret.add(CliUtils.newFileObject(f, "config").toURI().toURL());
			}

			// Next, identify the DOCUMENTUM_SHARED location, and if paramDctm.jar is in there
			var = cli.getString(this.paramDfc, System.getenv(DfcLaunchHelper.ENV_DOCUMENTUM_SHARED));
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
				if (!f.isDirectory()) { throw new FileNotFoundException(
					String.format("Could not find the [%s] directory [%s]", DfcLaunchHelper.ENV_DOCUMENTUM_SHARED,
						f.getAbsolutePath())); }

				// Next, does paramDctm.jar exist in there?
				if (!dfcFound) {
					File tgt = CliUtils.newFileObject(f, DfcLaunchHelper.DCTM_JAR);
					if (!tgt.isFile()) { throw new FileNotFoundException(
						String.format("Could not find the JAR file [%s]", tgt.getAbsolutePath())); }

					// Next, to the classpath
					ret.add(tgt.toURI().toURL());
				}
			}
		} catch (IOException e) {
			throw new RuntimeException("Failed to configure the dynamic DFC classpath", e);
		}
		return ret;
	}

	@Override
	public Collection<URL> getClasspathPatchesPost(OptionValues commandLine) {
		return null;
	}
}