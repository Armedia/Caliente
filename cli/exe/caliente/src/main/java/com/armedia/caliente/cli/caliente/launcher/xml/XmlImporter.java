package com.armedia.caliente.cli.caliente.launcher.xml;

import java.io.File;
import java.util.Map;

import com.armedia.caliente.cli.OptionSchemeExtender;
import com.armedia.caliente.cli.OptionSchemeExtensionSupport;
import com.armedia.caliente.cli.OptionValues;
import com.armedia.caliente.cli.caliente.cfg.CalienteState;
import com.armedia.caliente.cli.caliente.command.ImportCommandModule;
import com.armedia.caliente.cli.caliente.exception.CalienteException;
import com.armedia.caliente.cli.caliente.options.CLIGroup;
import com.armedia.caliente.cli.caliente.options.CLIParam;
import com.armedia.caliente.cli.exception.CommandLineExtensionException;
import com.armedia.caliente.cli.token.Token;
import com.armedia.caliente.engine.importer.ImportEngine;
import com.armedia.caliente.engine.xml.common.XmlSetting;
import com.armedia.commons.utilities.Tools;

class XmlImporter extends ImportCommandModule implements OptionSchemeExtensionSupport {
	XmlImporter(ImportEngine<?, ?, ?, ?, ?, ?> engine) {
		super(engine);
	}

	@Override
	protected boolean preInitialize(CalienteState state, Map<String, Object> settings) {
		return super.preInitialize(state, settings);
	}

	@Override
	protected boolean doInitialize(CalienteState state, Map<String, Object> settings) {
		return super.doInitialize(state, settings);
	}

	@Override
	protected boolean postInitialize(CalienteState state, Map<String, Object> settings) {
		return super.postInitialize(state, settings);
	}

	@Override
	protected void preValidateSettings(CalienteState state, Map<String, Object> settings) throws CalienteException {
		super.preValidateSettings(state, settings);
	}

	@Override
	protected boolean preConfigure(CalienteState state, OptionValues commandValues, Map<String, Object> settings)
		throws CalienteException {
		return super.preConfigure(state, commandValues, settings);
	}

	@Override
	protected boolean doConfigure(CalienteState state, OptionValues commandValues, Map<String, Object> settings)
		throws CalienteException {
		if (!super.doConfigure(state, commandValues, settings)) { return false; }

		String target = commandValues.getString(CLIParam.source);
		if (target == null) {
			target = ".";
		}
		final File targetDir = Tools.canonicalize(new File(target));
		targetDir.mkdirs();
		if (!targetDir.exists()) { throw new CalienteException(
			String.format("The target directory [%s] does not exist, and could not be created", targetDir)); }
		if (!targetDir.isDirectory()) { throw new CalienteException(
			String.format("A non-directory already exists at the location [%s] - can't continue", targetDir)); }

		settings.put(XmlSetting.ROOT.getLabel(), targetDir.getAbsolutePath());
		settings.put(XmlSetting.DB.getLabel(), state.getObjectStoreLocation().toString());
		settings.put(XmlSetting.CONTENT.getLabel(), state.getContentStoreLocation().toString());

		return true;
	}

	@Override
	protected void postConfigure(CalienteState state, OptionValues commandValues, Map<String, Object> settings)
		throws CalienteException {
		super.postConfigure(state, commandValues, settings);
	}

	@Override
	protected void postValidateSettings(CalienteState state, Map<String, Object> settings) throws CalienteException {
		super.postValidateSettings(state, settings);
	}

	@Override
	public void extendScheme(int currentNumber, OptionValues baseValues, String currentCommand,
		OptionValues commandValues, Token currentToken, OptionSchemeExtender extender)
		throws CommandLineExtensionException {
		extender //
			.addGroup(CLIGroup.IMPORT_COMMON) //
		;
	}
}