package com.armedia.caliente.cli.caliente.launcher.cmis;

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
import com.armedia.caliente.engine.cmis.exporter.CmisExportEngine;
import com.armedia.caliente.engine.cmis.importer.CmisImportEngine;
import com.armedia.caliente.engine.exporter.ExportEngine;
import com.armedia.caliente.engine.importer.ImportEngine;

public class CmisEngineInterface extends EngineInterface implements OptionSchemeExtensionSupport {

	static final String ID_PREFIX = "id:";

	public CmisEngineInterface() {
	}

	@Override
	public String getName() {
		return "cmis";
	}

	@Override
	public Set<String> getAliases() {
		return Collections.emptySet();
	}

	@Override
	protected ExportEngine<?, ?, ?, ?, ?, ?> getExportEngine() {
		return CmisExportEngine.getExportEngine();
	}

	@Override
	protected CmisExporter newExporter(ExportEngine<?, ?, ?, ?, ?, ?> engine) {
		return new CmisExporter(engine);
	}

	@Override
	protected ImportEngine<?, ?, ?, ?, ?, ?> getImportEngine() {
		return CmisImportEngine.getImportEngine();
	}

	@Override
	protected CmisImporter newImporter(ImportEngine<?, ?, ?, ?, ?, ?> engine) {
		return new CmisImporter(engine);
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
			.addGroup(CLIGroup.DOMAIN_CONNECTION) //
		;
	}

}