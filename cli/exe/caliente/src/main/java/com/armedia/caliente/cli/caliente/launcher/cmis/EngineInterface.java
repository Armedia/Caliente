package com.armedia.caliente.cli.caliente.launcher.cmis;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import com.armedia.caliente.cli.OptionScheme;
import com.armedia.caliente.cli.caliente.command.CalienteCommand;
import com.armedia.caliente.cli.caliente.launcher.AbstractEngineInterface;
import com.armedia.caliente.cli.caliente.launcher.DynamicEngineOptions;
import com.armedia.caliente.cli.caliente.options.CLIGroup;
import com.armedia.caliente.cli.launcher.LaunchClasspathHelper;
import com.armedia.caliente.engine.cmis.CmisCommon;
import com.armedia.caliente.engine.cmis.exporter.CmisExportEngineFactory;
import com.armedia.caliente.engine.cmis.importer.CmisImportEngineFactory;
import com.armedia.caliente.engine.exporter.ExportEngineFactory;
import com.armedia.caliente.engine.importer.ImportEngineFactory;

public class EngineInterface extends AbstractEngineInterface implements DynamicEngineOptions {

	static final String ID_PREFIX = "id:";

	private final CmisExportEngineFactory exportFactory = new CmisExportEngineFactory();
	private final CmisImportEngineFactory importFactory = new CmisImportEngineFactory();

	public EngineInterface() {
	}

	@Override
	public String getName() {
		return CmisCommon.TARGET_NAME;
	}

	@Override
	public Set<String> getAliases() {
		return Collections.emptySet();
	}

	@Override
	protected CmisExportEngineFactory getExportEngine() {
		return this.exportFactory;
	}

	@Override
	protected Exporter newExporter(ExportEngineFactory<?, ?, ?, ?, ?, ?> engine) {
		return new Exporter(engine);
	}

	@Override
	protected CmisImportEngineFactory getImportEngine() {
		return this.importFactory;
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
				.addGroup(CLIGroup.DOMAIN_CONNECTION) //
			;
		}
	}

}