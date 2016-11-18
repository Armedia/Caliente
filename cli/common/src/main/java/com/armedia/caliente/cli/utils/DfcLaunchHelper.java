package com.armedia.caliente.cli.utils;

import java.io.Console;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.io.FileUtils;

import com.armedia.caliente.cli.launcher.LaunchClasspathHelper;
import com.armedia.caliente.cli.launcher.LaunchParameterSet;
import com.armedia.caliente.cli.parser.CommandLineValues;
import com.armedia.caliente.cli.parser.MutableParameter;
import com.armedia.caliente.cli.parser.Parameter;
import com.armedia.commons.utilities.Tools;

public final class DfcLaunchHelper implements LaunchClasspathHelper, LaunchParameterSet {
	private static final String DFC_PROPERTIES_PROP = "dfc.properties.file";
	private static final String DEFAULT_DFC_PROPERTIES = "dfc.properties";
	private static final String ENV_DOCUMENTUM_SHARED = "DOCUMENTUM_SHARED";
	private static final String ENV_DOCUMENTUM = "DOCUMENTUM";
	private static final String DCTM_JAR = "dctm.jar";
	private static final String DFC_TEST_CLASS = "com.documentum.fc.client.IDfFolder";

	private static final Parameter DFC_LOCATION = new MutableParameter() //
		.setLongOpt("paramDfc") //
		.setMinValueCount(1) //
		.setMaxValueCount(1) //
		.setValueName("paramDfc install location") //
		.setDescription("The path where DFC is installed (i.e. instead of DOCUMENTUM_SHARED)") //
		.freezeCopy();;
	private static final Parameter DFC_DOCUMENTUM = new MutableParameter() //
		.setLongOpt("paramDctm") //
		.setMinValueCount(1) //
		.setMaxValueCount(1) //
		.setValueName("directory") //
		.setDescription("The user's local Documentum path (i.e. instead of DOCUMENTUM)") //
		.freezeCopy();;
	private static final Parameter DFC_PROPERTIES = new MutableParameter() //
		.setLongOpt("paramDfc-prop") //
		.setMinValueCount(1) //
		.setMaxValueCount(1) //
		.setValueName("paramDfc.properties location") //
		.setDescription("The paramDfc.properties file to use instead of the default") //
		.freezeCopy();;
	private static final Parameter DFC_DOCBASE = new MutableParameter() //
		.setRequired(true) //
		.setMinValueCount(1) //
		.setMaxValueCount(1) //
		.setValueName("docbase") //
		.setDescription("The Documentum repostory name to connect to") //
		.freezeCopy();;
	private static final Parameter DFC_USER = new MutableParameter() //
		.setRequired(true) //
		.setMinValueCount(1) //
		.setMaxValueCount(1) //
		.setValueName("username") //
		.setDescription("The username to connect to Documentum with") //
		.freezeCopy();
	private static final Parameter DFC_PASSWORD = new MutableParameter() //
		.setMinValueCount(1) //
		.setMaxValueCount(1) //
		.setValueName("password") //
		.setDescription("The password to connect to Documentum with")//
		.freezeCopy();

	private final Parameter paramDfc;
	private final Parameter paramDctm;
	private final Parameter paramDfcProp;

	private final boolean includesConnectionInfo;
	private final Parameter paramDocbase;
	private final Parameter paramUser;
	private final Parameter paramPassword;

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

	public String getDfcUser(CommandLineValues cli) {
		if (!this.includesConnectionInfo) { return null; }
		return cli.getString(this.paramUser);
	}

	public String getDfcDocbase(CommandLineValues cli) {
		if (!this.includesConnectionInfo) { return null; }
		return cli.getString(this.paramDocbase);
	}

	public String getDfcPassword(CommandLineValues cli) {
		if (!this.includesConnectionInfo) { return null; }

		if (cli.isPresent(this.paramPassword)) { return cli.getString(this.paramPassword); }

		final Console console = System.console();
		if (console == null) { return null; }
		String dctmUser = getDfcDocbase(cli);
		String docbase = getDfcDocbase(cli);
		char[] pass = console.readPassword(String.format("Please enter the Password for user [%s] in Docbase %s: ",
			Tools.coalesce(dctmUser, ""), docbase));
		if (pass != null) { return new String(pass); }
		return null;
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
	public Collection<? extends Parameter> getParameters(CommandLineValues commandLine) {
		ArrayList<Parameter> ret = new ArrayList<>();
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
	public Collection<URL> getClasspathPatchesPre(CommandLineValues cli) {
		final boolean dfcFound = checkForDfc();
		List<URL> ret = new ArrayList<>(3);
		try {

			// Even if not configured, if there's a dfc.properties in our current working directory,
			// we try to use it
			String var = cli.getString(this.paramDfcProp, DfcLaunchHelper.DEFAULT_DFC_PROPERTIES);
			if (var != null) {
				File f = Utils.newFileObject(var);
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
				File f = Utils.newFileObject(var);
				if (!f.exists()) {
					FileUtils.forceMkdir(f);
				}
				if (!f.isDirectory()) { throw new FileNotFoundException(
					String.format("Could not find the directory [%s]", f.getAbsolutePath())); }

				ret.add(Utils.newFileObject(f, "config").toURI().toURL());
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
				File f = Utils.newFileObject(var);
				if (!f.isDirectory()) { throw new FileNotFoundException(
					String.format("Could not find the [%s] directory [%s]", DfcLaunchHelper.ENV_DOCUMENTUM_SHARED,
						f.getAbsolutePath())); }

				// Next, does paramDctm.jar exist in there?
				if (!dfcFound) {
					File tgt = Utils.newFileObject(f, DfcLaunchHelper.DCTM_JAR);
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
	public Collection<URL> getClasspathPatchesPost(CommandLineValues commandLine) {
		return null;
	}
}