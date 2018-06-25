package com.armedia.caliente.cli.caliente.newlauncher.dctm;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import com.armedia.caliente.cli.OptionValues;
import com.armedia.caliente.cli.caliente.exception.CalienteException;
import com.armedia.caliente.cli.caliente.newlauncher.EngineInterface;
import com.armedia.caliente.cli.launcher.LaunchClasspathHelper;
import com.armedia.caliente.cli.utils.DfcLaunchHelper;
import com.armedia.caliente.engine.dfc.exporter.DctmExportEngine;
import com.armedia.caliente.engine.dfc.importer.DctmImportEngine;
import com.armedia.caliente.engine.exporter.ExportEngine;
import com.armedia.caliente.engine.importer.ImportEngine;
import com.armedia.commons.dfc.pool.DfcSessionFactory;

public class DctmEngineInterface extends EngineInterface {

	public DctmEngineInterface() {
	}

	@Override
	public String getName() {
		return "dctm";
	}

	@Override
	public Set<String> getAliases() {
		return Collections.emptySet();
	}

	static boolean commonConfigure(OptionValues commandValues, Map<String, Object> settings) throws CalienteException {
		String server = commandValues.getString(DfcLaunchHelper.DFC_DOCBASE);
		String user = commandValues.getString(DfcLaunchHelper.DFC_USER);
		String password = commandValues.getString(DfcLaunchHelper.DFC_PASSWORD);

		if (!StringUtils.isEmpty(server)) {
			settings.put(DfcSessionFactory.DOCBASE, server);
		}
		if (!StringUtils.isEmpty(user)) {
			settings.put(DfcSessionFactory.USERNAME, user);
		}
		if (!StringUtils.isEmpty(password)) {
			settings.put(DfcSessionFactory.PASSWORD, password);
		}
		return true;
	}

	@Override
	protected ExportEngine<?, ?, ?, ?, ?, ?> getExportEngine() {
		return DctmExportEngine.getExportEngine();
	}

	@Override
	protected DctmExporter newExporter(ExportEngine<?, ?, ?, ?, ?, ?> engine) {
		return new DctmExporter(engine);
	}

	@Override
	protected ImportEngine<?, ?, ?, ?, ?, ?> getImportEngine() {
		return DctmImportEngine.getImportEngine();
	}

	@Override
	protected DctmImporter newImporter(ImportEngine<?, ?, ?, ?, ?, ?> engine) {
		return new DctmImporter(engine);
	}

	@Override
	public Collection<? extends LaunchClasspathHelper> getClasspathHelpers() {
		return Collections.singleton(new DctmClasspathPatcher());
	}

}