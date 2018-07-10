package com.armedia.caliente.cli.caliente.launcher.xml;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import com.armedia.caliente.cli.OptionSchemeExtender;
import com.armedia.caliente.cli.OptionSchemeExtensionSupport;
import com.armedia.caliente.cli.OptionValues;
import com.armedia.caliente.cli.caliente.launcher.EngineInterface;
import com.armedia.caliente.cli.caliente.options.CLIGroup;
import com.armedia.caliente.cli.exception.CommandLineExtensionException;
import com.armedia.caliente.cli.launcher.LaunchClasspathHelper;
import com.armedia.caliente.cli.token.Token;
import com.armedia.caliente.engine.exporter.ExportEngine;
import com.armedia.caliente.engine.importer.ImportEngine;
import com.armedia.caliente.engine.xml.importer.XmlImportEngine;

public class XmlEngineInterface extends EngineInterface implements OptionSchemeExtensionSupport {

	public XmlEngineInterface() {
	}

	@Override
	public String getName() {
		return "xml";
	}

	@Override
	public Set<String> getAliases() {
		return Collections.emptySet();
	}

	@Override
	protected ExportEngine<?, ?, ?, ?, ?, ?> getExportEngine() {
		return null;
	}

	@Override
	protected ImportEngine<?, ?, ?, ?, ?, ?> getImportEngine() {
		return XmlImportEngine.getImportEngine();
	}

	@Override
	protected XmlImporter newImporter(ImportEngine<?, ?, ?, ?, ?, ?> engine) {
		return new XmlImporter(engine);
	}

	@Override
	public Collection<? extends LaunchClasspathHelper> getClasspathHelpers() {
		return Collections.emptyList();
	}

	@Override
	public void extendScheme(int currentNumber, OptionValues baseValues, String currentCommand,
		OptionValues commandValues, Token currentToken, OptionSchemeExtender extender)
		throws CommandLineExtensionException {

		extender //
			.addGroup(CLIGroup.STORE) //
			.addGroup(CLIGroup.MAIL) //
		;
	}

}