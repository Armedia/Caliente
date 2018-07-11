package com.armedia.caliente.cli.caliente.launcher.dctm;

import java.util.Map;

import com.armedia.caliente.cli.IntegerValueFilter;
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
		.setDefault(1000) //
		.setValueFilter(new IntegerValueFilter(100, 100000)) //
		.setDescription("The batch size to use when exporting objects from Documentum") //
	;

	private static final Option OWNER_ATTRIBUTES = new OptionImpl() //
		.setLongOpt("owner-attributes") //
		.setArgumentLimits(1, -1) //
		.setArgumentName("attribute-name") //
		.setDescription("The owner_attributes to check for") //
		.setDefaults("group_admin", "owner_name", "users_names", "owner", "r_creator_name", "r_modifier_name",
			"acl_domain") //
	;

	private static final Option SPECIAL_GROUPS = new OptionImpl() //
		.setLongOpt("special-groups") //
		.setArgumentLimits(1, -1) //
		.setArgumentName("group") //
		.setDescription("The special users that should not be imported into the target instance") //
		.setValueSep(',') //
		.setDefaults("admingroup", "dm_assume_user", "dm_assume_user_role", "dm_browse_all", "dm_browse_all_dynamic",
			"dm_create_cabinet", "dm_create_group", "dm_create_table", "dm_create_type", "dm_create_user",
			"dm_datefield_override", "dm_datefield_override_role", "dm_delete_table", "dm_escalated_allow_save_on_lock",
			"dm_escalated_delete", "dm_escalated_full_control", "dm_escalated_owner_control", "dm_escalated_read",
			"dm_escalated_relate", "dm_escalated_version", "dm_escalated_write", "dm_fulltext_admin",
			"dm_internal_attrib_override", "dm_internal_attrib_override_role", "dm_read_all", "dm_read_all_dynamic",
			"dm_retention_managers", "dm_retention_users", "dm_superusers", "dm_superusers_dynamic", "dm_sysadmin",
			"dm_sysadmin_role", "dm_user_identity_override", "dm_user_identity_override_role",
			"dm_workflow_task_supervisor", "dmc_wdk_presets_coordinator", "docu", "express_user",
			"process_report_admin", "queue_admin", "queue_advance_processor", "queue_manager", "queue_processor") //
	;

	private static final Option SPECIAL_TYPES = new OptionImpl() //
		.setLongOpt("special-types") //
		.setArgumentLimits(1, -1) //
		.setArgumentName("type") //
		.setDescription("The special types that should not be imported into the target instance") //
		.setValueSep(',') //
		.setDefaults("dm_acl", "dm_acs_config", "dm_activity", "dm_aggr_domain", "dm_alias_set", "dm_app_ref",
			"dm_application", "dm_assembly", "dm_atmos_store", "dm_attachments_folder", "dm_audit_policy",
			"dm_audittrail", "dm_audittrail_acl", "dm_audittrail_group", "dm_blobstore", "dm_bocs_config",
			"dm_builtin_expr", "dm_business_pro", "dm_ca_store", "dm_cabinet", "dm_cache_config", "dm_category",
			"dm_category_assign", "dm_category_class", "dm_ci_config", "dm_client_registration", "dm_client_rights",
			"dm_client_rights_domain", "dm_component", "dm_cond_expr", "dm_cond_id_expr", "dm_cont_transfer_config",
			"dm_cryptographic_key", "dm_dd_info", "dm_decision", "dm_display_config", "dm_distributedstore",
			"dm_dms_config", "dm_docbase_config", "dm_docbaseid_map", "dm_docset", "dm_docset_run", "dm_document",
			"dm_domain", "dm_dump_record", "dm_email_message", "dm_esign_template", "dm_expression", "dm_extern_file",
			"dm_extern_free", "dm_extern_store", "dm_extern_url", "dm_federation", "dm_filestore", "dm_folder",
			"dm_foreign_key", "dm_format", "dm_format_preferences", "dm_ftengine_config", "dm_ftfilter_config",
			"dm_ftindex_agent_config", "dm_ftquery_subscription", "dm_ftwatermark", "dm_fulltext_collection",
			"dm_fulltext_index", "dm_func_expr", "dm_group", "dm_java", "dm_jms_config", "dm_job", "dm_job_request",
			"dm_job_sequence", "dm_key", "dm_krb_util_config", "dm_ldap_config", "dm_linkedstore", "dm_literal_expr",
			"dm_load_record", "dm_location", "dm_locator", "dm_media_profile", "dm_menu_system", "dm_message_archive",
			"dm_message_container", "dm_message_route_user_data", "dm_method", "dm_migrate_rule", "dm_mount_point",
			"dm_network_location_map", "dm_nls_dd_info", "dm_note", "dm_opticalstore", "dm_outputdevice",
			"dm_partition_scheme", "dm_plugin", "dm_policy", "dm_procedure", "dm_process", "dm_public_key_certificate",
			"dm_qual_comp", "dm_query", "dm_reference", "dm_registered", "dm_relation", "dm_relation_ssa_policy",
			"dm_relation_type", "dm_retainer", "dm_router", "dm_scope_config", "dm_script", "dm_server_config",
			"dm_shmeconfig", "dm_smart_list", "dm_ssa_policy", "dm_staged", "dm_state_extension", "dm_state_type",
			"dm_store", "dm_sync_list_relation", "dm_sysobject", "dm_sysprocess_config", "dm_taxonomy", "dm_type",
			"dm_user", "dm_validation_descriptor", "dm_value_assist", "dm_value_func", "dm_value_list",
			"dm_value_query", "dm_webc_config", "dm_webc_target", "dm_workflow", "dm_xml_application", "dm_xml_config",
			"dm_xml_custom_code", "dm_xml_style_sheet", "dm_xml_zone", "dmc_act_group_instance", "dmc_aspect_relation",
			"dmc_aspect_type", "dmc_class", "dmc_completed_workflow", "dmc_completed_workitem",
			"dmc_composite_predicate", "dmc_config_scope_relation", "dmc_constraint_set", "dmc_dar", "dmc_jar",
			"dmc_java_library", "dmc_metamodel", "dmc_module", "dmc_module_config", "dmc_preset_info",
			"dmc_preset_package", "dmc_process_correlation_set", "dmc_process_parameter", "dmc_relationship_def",
			"dmc_routecase_condition", "dmc_search_template", "dmc_tcf_activity", "dmc_tcf_activity_template",
			"dmc_transition_condition", "dmc_validation_module", "dmc_validation_relation", "dmc_wf_package_schema",
			"dmc_wf_package_skill", "dmc_wf_package_type_info", "dmc_wfsd_element", "dmc_wfsd_element_boolean",
			"dmc_wfsd_element_date", "dmc_wfsd_element_double", "dmc_wfsd_element_integer", "dmc_wfsd_element_parent",
			"dmc_wfsd_element_string", "dmc_wfsd_type_info", "dmc_wfsdrp_boolean", "dmc_wfsdrp_date",
			"dmc_wfsdrp_double", "dmc_wfsdrp_integer", "dmc_wfsdrp_parent", "dmc_wfsdrp_string", "dmc_workqueue",
			"dmc_workqueue_category", "dmc_workqueue_doc_profile", "dmc_workqueue_policy", "dmc_workqueue_user_profile",
			"dmc_wpr_parent", "dmc_wq_skill_info", "dmc_wq_task_skill", "dmc_wq_user_skill", "dmi_audittrail_asp_attrs",
			"dmi_audittrail_attrs", "dmi_change_record", "dmi_dd_attr_info", "dmi_dd_common_info", "dmi_dd_type_info",
			"dmi_dist_comp_record", "dmi_dump_object_record", "dmi_expr_code", "dmi_index", "dmi_linkrecord",
			"dmi_load_object_record", "dmi_otherfile", "dmi_package", "dmi_queue_item", "dmi_recovery", "dmi_registry",
			"dmi_replica_record", "dmi_sequence", "dmi_session", "dmi_subcontent", "dmi_transactionlog",
			"dmi_type_info", "dmi_vstamp", "dmi_wf_attachment", "dmi_wf_timer", "dmi_workitem", "dmr_containment",
			"dmr_content") //
	;

	private static final Option SPECIAL_USERS = new OptionImpl() //
		.setLongOpt("special-users") //
		.setArgumentLimits(1, -1) //
		.setArgumentName("user") //
		.setDescription("The special users that should not be imported into the target instance") //
		.setValueSep(',') //
		.setDefaults("admingroup", "dm_assume_user", "dm_assume_user_role", "dm_autorender_mac", "dm_autorender_win31",
			"dm_bof_registry", "dm_browse_all", "dm_browse_all_dynamic", "dm_create_cabinet", "dm_create_group",
			"dm_create_table", "dm_create_type", "dm_create_user", "dmc_wdk_preferences_owner",
			"dmc_wdk_presets_coordinator", "dmc_wdk_presets_owner", "dm_datefield_override",
			"dm_datefield_override_role", "dm_delete_table", "dm_escalated_allow_save_on_lock", "dm_escalated_delete",
			"dm_escalated_full_control", "dm_escalated_owner_control", "dm_escalated_read", "dm_escalated_relate",
			"dm_escalated_version", "dm_escalated_write", "dm_fulltext_admin", "dm_fulltext_index_user",
			"dm_internal_attrib_override", "dm_internal_attrib_override_role", "dm_mediaserver", "dm_read_all",
			"dm_read_all_dynamic", "dm_report_user", "dm_retention_managers", "dm_retention_users", "dm_superusers",
			"dm_superusers_dynamic", "dm_sysadmin", "dm_sysadmin_role", "dm_user_identity_override",
			"dm_user_identity_override_role", "dm_workflow_task_supervisor", "docu", "express_user",
			"process_report_admin", "queue_admin", "queue_advance_processor", "queue_manager", "queue_processor") //
	;

	private static final OptionGroup OPTIONS = new OptionGroupImpl("DFC Export") //
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