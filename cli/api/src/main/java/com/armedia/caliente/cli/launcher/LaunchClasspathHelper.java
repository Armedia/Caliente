package com.armedia.caliente.cli.launcher;

import java.net.URL;
import java.util.Collection;

import com.armedia.caliente.cli.ParameterValues;

public interface LaunchClasspathHelper {

	public Collection<URL> getClasspathPatchesPre(ParameterValues values);

	public Collection<URL> getClasspathPatchesPost(ParameterValues values);

}