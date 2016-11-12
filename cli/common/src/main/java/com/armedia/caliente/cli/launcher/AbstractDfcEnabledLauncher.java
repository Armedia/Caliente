package com.armedia.caliente.cli.launcher;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.apache.commons.io.FileUtils;

import com.armedia.caliente.cli.parser.CommandLineValues;
import com.armedia.caliente.cli.parser.MutableParameterDefinition;
import com.armedia.caliente.cli.parser.ParameterDefinition;

public abstract class AbstractDfcEnabledLauncher extends AbstractLauncher {
	protected static final String DFC_PROPERTIES_PROP = "dfc.properties.file";
	protected static final String DEFAULT_DFC_PROPERTIES = "dfc.properties";
	protected static final String ENV_DOCUMENTUM_SHARED = "DOCUMENTUM_SHARED";
	protected static final String ENV_DOCUMENTUM = "DOCUMENTUM";
	protected static final String DCTM_JAR = "dctm.jar";
	protected static final String DFC_TEST_CLASS = "com.documentum.fc.client.IDfFolder";

	protected final ParameterDefinition paramDfc;
	protected final ParameterDefinition paramDctm;
	protected final ParameterDefinition paramDfcProp;
	protected final ParameterDefinition paramDocbase;
	protected final ParameterDefinition paramUser;
	protected final ParameterDefinition paramPassword;

	protected AbstractDfcEnabledLauncher(boolean includeConnection) {
		this.paramDfc = new MutableParameterDefinition() //
			.setLongOpt("paramDfc") //
			.setValueCount(1) //
			.setValueOptional(false) //
			.setValueName("paramDfc install location") //
			.setDescription("The path where DFC is installed (i.e. instead of DOCUMENTUM_SHARED)");
		this.paramDctm = new MutableParameterDefinition() //
			.setLongOpt("paramDctm") //
			.setValueCount(1) //
			.setValueOptional(false) //
			.setValueName("directory") //
			.setDescription("The user's local Documentum path (i.e. instead of DOCUMENTUM)");
		this.paramDfcProp = new MutableParameterDefinition() //
			.setLongOpt("paramDfc-prop") //
			.setValueCount(1) //
			.setValueOptional(false) //
			.setValueName("paramDfc.properties location") //
			.setDescription("The paramDfc.properties file to use instead of the default");
		if (includeConnection) {
			this.paramDocbase = new MutableParameterDefinition() //
				.setRequired(true) //
				.setValueCount(1) //
				.setValueOptional(false) //
				.setValueName("docbase") //
				.setDescription("The Documentum repostory name to connect to");
			this.paramUser = new MutableParameterDefinition() //
				.setRequired(true) //
				.setValueCount(1) //
				.setValueOptional(false) //
				.setValueName("username") //
				.setDescription("The username to connect to Documentum with");
			this.paramPassword = new MutableParameterDefinition() //
				.setValueCount(1) //
				.setValueOptional(false) //
				.setValueName("password") //
				.setDescription("The password to connect to Documentum with");
		} else {
			this.paramDocbase = null;
			this.paramUser = null;
			this.paramPassword = null;
		}
	}

	protected final File newFileObject(String path) {
		return newFileObject(null, path);
	}

	protected final File newFileObject(File parent, String path) {
		File f = (parent != null ? new File(parent, path) : new File(path));
		try {
			f = f.getCanonicalFile();
		} catch (IOException e) {
			// this.log.warn(String.format("Failed to canonicalize the path for [%s]",
			// f.getAbsolutePath()), e);
			// Do nothing, for now
		} finally {
			f = f.getAbsoluteFile();
		}
		return f;
	}

	protected final Collection<ParameterDefinition> getDfcParameters() {
		if (this.paramUser == null) {
			return Arrays.asList(this.paramDfcProp, this.paramDfc, this.paramDctm);
		} else {
			return Arrays.asList(this.paramDfcProp, this.paramDfc, this.paramDctm, this.paramDocbase, this.paramUser,
				this.paramPassword);
		}
	}

	protected final boolean checkForDfc() {
		try {
			Class.forName(AbstractDfcEnabledLauncher.DFC_TEST_CLASS);
			return true;
		} catch (Exception e) {
			// ignore the exception, for now
			return false;
		}
	}

	@Override
	protected Collection<URL> getClasspathPatchesPre(CommandLineValues cli) {
		final boolean dfcFound = checkForDfc();
		List<URL> ret = new ArrayList<>(3);
		try {

			// Even if not configured, if there's a dfc.properties in our current working directory,
			// we try to use it
			String var = cli.getString(this.paramDfcProp, AbstractDfcEnabledLauncher.DEFAULT_DFC_PROPERTIES);
			if (var != null) {
				File f = newFileObject(var);
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
					System.setProperty(AbstractDfcEnabledLauncher.DFC_PROPERTIES_PROP, f.getAbsolutePath());
					/*
					} else {
						this.log.warn("The DFC properties file [{}] {} - will continue using DFC defaults",
							f.getAbsolutePath(), error);
					*/
				}
			}

			// Next, add ${DOCUMENTUM}/config to the classpath
			var = cli.getString(this.paramDctm, System.getenv(AbstractDfcEnabledLauncher.ENV_DOCUMENTUM));
			// Go with the environment
			if (var == null) {
				String msg = String.format("The environment variable [%s] is not set",
					AbstractDfcEnabledLauncher.ENV_DOCUMENTUM);
				if (!dfcFound) { throw new RuntimeException(msg); }
				// this.log.warn("{}, integrated DFC may encounter errors", msg);
			} else {
				File f = newFileObject(var);
				if (!f.exists()) {
					FileUtils.forceMkdir(f);
				}
				if (!f.isDirectory()) { throw new FileNotFoundException(
					String.format("Could not find the directory [%s]", f.getAbsolutePath())); }

				ret.add(newFileObject(f, "config").toURI().toURL());
			}

			// Next, identify the DOCUMENTUM_SHARED location, and if paramDctm.jar is in there
			var = cli.getString(this.paramDfc, System.getenv(AbstractDfcEnabledLauncher.ENV_DOCUMENTUM_SHARED));
			// Go with the environment
			if (var == null) {
				String msg = String.format("The environment variable [%s] is not set",
					AbstractDfcEnabledLauncher.ENV_DOCUMENTUM_SHARED);
				if (!dfcFound) { throw new RuntimeException(msg); }
				// this.log.warn("{}, integrated DFC may encounter errors", msg);
			}

			if (var != null) {
				// Next, is it a directory?
				File f = newFileObject(var);
				if (!f.isDirectory()) { throw new FileNotFoundException(
					String.format("Could not find the [%s] directory [%s]",
						AbstractDfcEnabledLauncher.ENV_DOCUMENTUM_SHARED, f.getAbsolutePath())); }

				// Next, does paramDctm.jar exist in there?
				if (!dfcFound) {
					File tgt = newFileObject(f, AbstractDfcEnabledLauncher.DCTM_JAR);
					if (!tgt.isFile()) { throw new FileNotFoundException(
						String.format("Could not find the JAR file [%s]", tgt.getAbsolutePath())); }

					// Next, to the classpath
					ret.add(tgt.toURI().toURL());
				}
			}
		} catch (IOException e) {
			throw new RuntimeException("Failed to configure the dynamic classpath", e);
		}
		return ret;
	}
}