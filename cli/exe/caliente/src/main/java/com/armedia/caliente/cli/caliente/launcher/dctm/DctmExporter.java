package com.armedia.caliente.cli.caliente.launcher.dctm;

import java.util.Map;

import com.armedia.caliente.cli.Option;
import com.armedia.caliente.cli.OptionGroup;
import com.armedia.caliente.cli.OptionGroupImpl;
import com.armedia.caliente.cli.OptionImpl;
import com.armedia.caliente.cli.OptionScheme;
import com.armedia.caliente.cli.OptionValues;
import com.armedia.caliente.cli.caliente.cfg.CalienteState;
import com.armedia.caliente.cli.caliente.command.ExportCommandModule;
import com.armedia.caliente.cli.caliente.exception.CalienteException;
import com.armedia.caliente.cli.caliente.launcher.DynamicOptions;
import com.armedia.caliente.cli.caliente.options.CLIGroup;
import com.armedia.caliente.cli.caliente.options.CLIParam;
import com.armedia.caliente.engine.dfc.common.Setting;
import com.armedia.caliente.engine.exporter.ExportEngine;
import com.armedia.commons.dfc.pool.DfcSessionPool;
import com.documentum.fc.client.IDfSession;

class DctmExporter extends ExportCommandModule implements DynamicOptions {
	/**
	 * The from and where clause of the export query that runs periodically. The application will
	 * combine the select clause listed above with this from and where clauses to build the complete
	 * dql query. Please note that this clause will be ignored when the export is running in the
	 * adhoc mode. In that case the from and where clauses are specified in the properties file.
	 */
	private static final String DEFAULT_PREDICATE = "dm_sysobject where (TYPE(\"dm_folder\") or TYPE(\"dm_document\")) "
		+ "and not folder('/System', descend) and not folder('/Temp', descend) ";

	private static final Option BATCH_SIZE = new OptionImpl() //
		.setLongOpt("batch-size") //
		.setArgumentLimits(1) //
		.setArgumentName("batch-size") //
		.setDescription("The batch size to use when exporting objects from Documentum") //
	;

	private static final Option OWNER_ATTRIBUTES = new OptionImpl() //
		.setLongOpt("owner-attributes") //
		.setArgumentLimits(1, -1) //
		.setArgumentName("attribute-name") //
		.setDescription("The owner_attributes to check for") //
	;

	private static final Option SPECIAL_GROUPS = new OptionImpl() //
		.setLongOpt("special-groups") //
		.setArgumentLimits(1, -1) //
		.setArgumentName("group") //
		.setDescription("The special users that should not be imported into the target instance") //
		.setValueSep(',') //
	;

	private static final Option SPECIAL_TYPES = new OptionImpl() //
		.setLongOpt("special-types") //
		.setArgumentLimits(1, -1) //
		.setArgumentName("type") //
		.setDescription("The special types that should not be imported into the target instance") //
		.setValueSep(',') //
	;

	private static final Option SPECIAL_USERS = new OptionImpl() //
		.setLongOpt("special-users") //
		.setArgumentLimits(1, -1) //
		.setArgumentName("user") //
		.setDescription("The special users that should not be imported into the target instance") //
		.setValueSep(',') //
	;

	private static final OptionGroup OPTIONS = new OptionGroupImpl("DFC Export Options") //
		.add(DctmExporter.BATCH_SIZE) //
		.add(DctmExporter.OWNER_ATTRIBUTES) //
		.add(DctmExporter.SPECIAL_GROUPS) //
		.add(DctmExporter.SPECIAL_TYPES) //
		.add(DctmExporter.SPECIAL_USERS) //
	;

	private DfcSessionPool pool = null;
	private IDfSession session = null;

	DctmExporter(ExportEngine<?, ?, ?, ?, ?, ?> engine) {
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
		if (!DctmEngineInterface.commonConfigure(commandValues, settings)) { return false; }

		try {
			this.pool = new DfcSessionPool(settings);
			this.session = this.pool.acquireSession();
		} catch (Exception e) {
			throw new CalienteException("Failed to initialize the connection pool or get the primary session", e);
		}

		String dql = DctmExporter.DEFAULT_PREDICATE;
		if (commandValues.isPresent(CLIParam.source)) {
			dql = commandValues.getString(CLIParam.source);
		}
		settings.put(Setting.DQL.getLabel(), dql);

		// TODO: process the other parameters

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
	public void close() throws Exception {
		if (this.session != null) {
			this.pool.releaseSession(this.session);
		}
		if (this.pool != null) {
			this.pool.close();
		}
	}

	@Override
	public void getDynamicOptions(OptionScheme command) {
		command //
			.addGroup(CLIGroup.EXPORT_COMMON) //
			.addGroup(DctmExporter.OPTIONS) //
		;
	}
}