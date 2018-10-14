package com.armedia.caliente.cli.caliente.launcher.dctm;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import com.armedia.caliente.cli.OptionGroup;
import com.armedia.caliente.cli.OptionGroupImpl;
import com.armedia.caliente.cli.OptionScheme;
import com.armedia.caliente.cli.OptionValues;
import com.armedia.caliente.cli.caliente.command.CommandModule;
import com.armedia.caliente.cli.caliente.exception.CalienteException;
import com.armedia.caliente.cli.caliente.launcher.AbstractEngineInterface;
import com.armedia.caliente.cli.caliente.launcher.DynamicEngineOptions;
import com.armedia.caliente.cli.caliente.options.CLIGroup;
import com.armedia.caliente.cli.caliente.options.CLIParam;
import com.armedia.caliente.cli.launcher.LaunchClasspathHelper;
import com.armedia.caliente.cli.utils.DfcLaunchHelper;
import com.armedia.caliente.engine.dfc.exporter.DctmExportEngine;
import com.armedia.caliente.engine.dfc.importer.DctmImportEngine;
import com.armedia.caliente.engine.exporter.ExportEngine;
import com.armedia.caliente.engine.importer.ImportEngine;
import com.armedia.commons.dfc.pool.DfcSessionFactory;
import com.documentum.fc.common.DfLoggerDisabled;
import com.documentum.fc.common.impl.logging.LoggingConfigurator;

public class EngineInterface extends AbstractEngineInterface implements DynamicEngineOptions {

	static final DfcLaunchHelper DFC_HELPER = new DfcLaunchHelper(true);

	private static final OptionGroup DFC_OPTIONS = new OptionGroupImpl("DFC Configuration") //
		.add(DfcLaunchHelper.DFC_DOCUMENTUM) //
		.add(DfcLaunchHelper.DFC_LOCATION) //
	;

	private static final OptionGroup DFC_CONNECTION = new OptionGroupImpl("DFC Connection") //
		.addFrom(CLIGroup.CONNECTION) //
		.add(DfcLaunchHelper.DFC_UNIFIED) //
		.add(DfcLaunchHelper.DFC_PROPERTIES) //
	;

	public EngineInterface() {
		// Load the logging-related patch classes
		DfLoggerDisabled.class.hashCode();
		LoggingConfigurator.class.hashCode();
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
		String server = commandValues.getString(CLIParam.server);
		String user = commandValues.getString(CLIParam.user);
		String password = commandValues.getString(CLIParam.password);

		if (commandValues.isPresent(DfcLaunchHelper.DFC_UNIFIED)) {
			// We don't use users/passwords for unified login
			user = null;
			password = null;
		} else {
			// If unified login is not in use, then user information (and a password?) is required
			if (StringUtils.isBlank(user)) { throw new CalienteException(
				String.format("A non-blank username (specified via --%s) is required when not using unified login",
					CLIParam.user.getOption().getLongOpt())); }
		}

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
	protected Exporter newExporter(ExportEngine<?, ?, ?, ?, ?, ?> engine) {
		return new Exporter(engine);
	}

	@Override
	protected ImportEngine<?, ?, ?, ?, ?, ?> getImportEngine() {
		return DctmImportEngine.getImportEngine();
	}

	@Override
	protected Importer newImporter(ImportEngine<?, ?, ?, ?, ?, ?> engine) {
		return new Importer(engine);
	}

	@Override
	public Collection<? extends LaunchClasspathHelper> getClasspathHelpers() {
		return Collections.singleton(EngineInterface.DFC_HELPER);
	}

	@Override
	public void getDynamicOptions(CommandModule<?> command, OptionScheme scheme) {
		if (command.getDescriptor().isRequiresStorage()) {
			scheme //
				.addGroup(CLIGroup.STORE) //
				.addGroup(CLIGroup.MAIL) //
				.addGroup(EngineInterface.DFC_CONNECTION) //
			;
		}

		scheme //
			.addGroup(EngineInterface.DFC_OPTIONS) //
		;
	}

}