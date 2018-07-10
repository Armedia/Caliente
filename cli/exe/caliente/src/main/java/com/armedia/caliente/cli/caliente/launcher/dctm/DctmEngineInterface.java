package com.armedia.caliente.cli.caliente.launcher.dctm;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import com.armedia.caliente.cli.OptionGroup;
import com.armedia.caliente.cli.OptionGroupImpl;
import com.armedia.caliente.cli.OptionSchemeExtender;
import com.armedia.caliente.cli.OptionSchemeExtensionSupport;
import com.armedia.caliente.cli.OptionValues;
import com.armedia.caliente.cli.caliente.exception.CalienteException;
import com.armedia.caliente.cli.caliente.launcher.EngineInterface;
import com.armedia.caliente.cli.caliente.options.CLIGroup;
import com.armedia.caliente.cli.caliente.options.CLIParam;
import com.armedia.caliente.cli.exception.CommandLineExtensionException;
import com.armedia.caliente.cli.launcher.LaunchClasspathHelper;
import com.armedia.caliente.cli.token.Token;
import com.armedia.caliente.cli.utils.DfcLaunchHelper;
import com.armedia.caliente.engine.dfc.exporter.DctmExportEngine;
import com.armedia.caliente.engine.dfc.importer.DctmImportEngine;
import com.armedia.caliente.engine.exporter.ExportEngine;
import com.armedia.caliente.engine.importer.ImportEngine;
import com.armedia.commons.dfc.pool.DfcSessionFactory;

public class DctmEngineInterface extends EngineInterface implements OptionSchemeExtensionSupport {

	static final DfcLaunchHelper DFC_HELPER = new DfcLaunchHelper(true);

	private static final OptionGroup DFC_OPTIONS = new OptionGroupImpl("DFC Options") //
		.add(DfcLaunchHelper.DFC_DOCUMENTUM) //
		.add(DfcLaunchHelper.DFC_LOCATION) //
		.add(DfcLaunchHelper.DFC_PROPERTIES) //
		.add(DfcLaunchHelper.DFC_UNIFIED) //
	;

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
		return Collections.singleton(DctmEngineInterface.DFC_HELPER);
	}

	@Override
	public void extendScheme(int currentNumber, OptionValues baseValues, String currentCommand,
		OptionValues commandValues, Token currentToken, OptionSchemeExtender extender)
		throws CommandLineExtensionException {
		extender //
			.addGroup(CLIGroup.STORE) //
			.addGroup(CLIGroup.MAIL) //
			.addGroup(CLIGroup.CONNECTION) //
			.addGroup(DctmEngineInterface.DFC_OPTIONS) //
		;
	}

}