package com.armedia.caliente.cli.launcher;

import java.net.URL;
import java.util.Collection;

import com.armedia.caliente.cli.OptionValues;

public interface LaunchClasspathHelper {

	public Collection<URL> getClasspathPatchesPre(OptionValues values);

	public Collection<URL> getClasspathPatchesPost(OptionValues values);

}