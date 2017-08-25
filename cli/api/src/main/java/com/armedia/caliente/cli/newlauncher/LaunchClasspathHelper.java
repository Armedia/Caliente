package com.armedia.caliente.cli.newlauncher;

import java.net.URL;
import java.util.Collection;

import com.armedia.caliente.cli.CommandLineResult;

public interface LaunchClasspathHelper {

	public Collection<URL> getClasspathPatchesPre(CommandLineResult commandLine);

	public Collection<URL> getClasspathPatchesPost(CommandLineResult commandLine);

}