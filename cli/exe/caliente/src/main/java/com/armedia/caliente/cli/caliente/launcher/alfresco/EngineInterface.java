package com.armedia.caliente.cli.caliente.launcher.alfresco;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import com.armedia.caliente.cli.OptionScheme;
import com.armedia.caliente.cli.caliente.command.CalienteCommand;
import com.armedia.caliente.cli.caliente.launcher.AbstractEngineInterface;
import com.armedia.caliente.cli.caliente.launcher.DynamicEngineOptions;
import com.armedia.caliente.cli.caliente.options.CLIGroup;
import com.armedia.caliente.cli.launcher.LaunchClasspathHelper;
import com.armedia.caliente.engine.alfresco.bi.AlfCommon;
import com.armedia.caliente.engine.alfresco.bi.importer.AlfImportEngineFactory;
import com.armedia.caliente.engine.exporter.ExportEngineFactory;
import com.armedia.caliente.engine.importer.ImportEngineFactory;

public class EngineInterface extends AbstractEngineInterface implements DynamicEngineOptions {

	public EngineInterface() {
	}

	@Override
	public String getName() {
		return AlfCommon.TARGET_NAME;
	}

	@Override
	public Set<String> getAliases() {
		return Collections.emptySet();
	}

	@Override
	protected ExportEngineFactory<?, ?, ?, ?, ?, ?> getExportEngineFactory() {
		return null;
	}

	@Override
	protected AlfImportEngineFactory getImportEngineFactory() {
		return new AlfImportEngineFactory();
	}

	@Override
	protected Importer newImporter(ImportEngineFactory<?, ?, ?, ?, ?, ?> engine) {
		return new Importer(engine);
	}

	@Override
	public Collection<? extends LaunchClasspathHelper> getClasspathHelpers() {
		return Collections.emptyList();
	}

	@Override
	public void getDynamicOptions(CalienteCommand command, OptionScheme scheme) {
		if (command.isRequiresStorage()) {
			scheme //
				.addGroup(CLIGroup.STORE) //
				.addGroup(CLIGroup.MAIL) //
			;
		}
	}

}