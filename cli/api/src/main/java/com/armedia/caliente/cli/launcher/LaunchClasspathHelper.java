package com.armedia.caliente.cli.launcher;

import java.net.URL;
import java.util.Collection;

import com.armedia.caliente.cli.CommandLineValues;

public interface LaunchClasspathHelper {

	public Collection<URL> getClasspathPatchesPre(CommandLineValues commandLine);

	public Collection<URL> getClasspathPatchesPost(CommandLineValues commandLine);

}