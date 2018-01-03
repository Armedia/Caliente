package com.armedia.caliente.cli.caliente.launcher;

import java.util.Set;

import com.armedia.caliente.cli.OptionSchemeExtensionSupport;

public abstract class CalienteLauncherModule {

	public abstract Set<String> getSupportedECM();

	public abstract Set<String> getSupportedModes(String ecm);

	public abstract OptionSchemeExtensionSupport getOptionSchemeExtender(String ecm, String mode);

}