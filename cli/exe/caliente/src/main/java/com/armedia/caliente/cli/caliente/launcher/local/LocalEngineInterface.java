package com.armedia.caliente.cli.caliente.launcher.local;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import com.armedia.caliente.cli.OptionSchemeExtender;
import com.armedia.caliente.cli.OptionSchemeExtensionSupport;
import com.armedia.caliente.cli.OptionValues;
import com.armedia.caliente.cli.caliente.exception.CalienteException;
import com.armedia.caliente.cli.caliente.launcher.EngineInterface;
import com.armedia.caliente.cli.caliente.options.CLIGroup;
import com.armedia.caliente.cli.exception.CommandLineExtensionException;
import com.armedia.caliente.cli.launcher.LaunchClasspathHelper;
import com.armedia.caliente.cli.token.Token;
import com.armedia.caliente.engine.exporter.ExportEngine;
import com.armedia.caliente.engine.importer.ImportEngine;
import com.armedia.caliente.engine.local.exporter.LocalExportEngine;
import com.armedia.caliente.engine.local.importer.LocalImportEngine;

public class LocalEngineInterface extends EngineInterface implements OptionSchemeExtensionSupport {

	static boolean commonConfigure(OptionValues commandValues, Map<String, Object> settings) throws CalienteException {

		return true;
	}

	public LocalEngineInterface() {
	}

	@Override
	public String getName() {
		return "local";
	}

	@Override
	public Set<String> getAliases() {
		return Collections.emptySet();
	}

	@Override
	protected ExportEngine<?, ?, ?, ?, ?, ?> getExportEngine() {
		return LocalExportEngine.getExportEngine();
	}

	@Override
	protected LocalExporter newExporter(ExportEngine<?, ?, ?, ?, ?, ?> engine) {
		return new LocalExporter(engine);
	}

	@Override
	protected ImportEngine<?, ?, ?, ?, ?, ?> getImportEngine() {
		return LocalImportEngine.getImportEngine();
	}

	@Override
	protected LocalImporter newImporter(ImportEngine<?, ?, ?, ?, ?, ?> engine) {
		return new LocalImporter(engine);
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