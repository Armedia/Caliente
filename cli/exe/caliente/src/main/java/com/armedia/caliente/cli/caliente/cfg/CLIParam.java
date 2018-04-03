package com.armedia.caliente.cli.caliente.cfg;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

import com.armedia.caliente.cli.EnumValueFilter;
import com.armedia.caliente.cli.IntegerValueFilter;
import com.armedia.caliente.cli.Option;
import com.armedia.caliente.cli.OptionImpl;
import com.armedia.caliente.cli.OptionValue;
import com.armedia.caliente.cli.OptionValues;
import com.armedia.caliente.cli.OptionWrapper;
import com.armedia.caliente.cli.StringValueFilter;
import com.armedia.caliente.cli.utils.DfcLaunchHelper;
import com.armedia.caliente.cli.utils.LibLaunchHelper;
import com.armedia.caliente.cli.utils.ThreadsLaunchHelper;
import com.armedia.caliente.engine.exporter.ExportResult;
import com.armedia.caliente.store.CmfType;

public enum CLIParam implements OptionWrapper {
	//

	//
	// First pass...
	//
	help( //
		CalienteBaseOptions.HELP //
	), //
	lib( //
		LibLaunchHelper.LIB //
	), //
	log( //
		CalienteBaseOptions.LOG //
	), //
	log_cfg( //
		CalienteBaseOptions.LOG_CFG //
	), //
	engine( //
		new OptionImpl() //
			.setShortOpt('e') //
			.setArgumentLimits(1) //
			.setValueFilter( // TODO: Find a way to make this list dynamic
				new StringValueFilter( //
					false, // Case-insensitive
					"dctm", //
					"alfresco", //
					"cmis", //
					"sharepoint", //
					"ucm", //
					"xml", //
					"local" //
				) //
			) //
			.setRequired(true) //
			.setDescription("The mode of operation") //
			.setArgumentName("engine") //
	), //
	db( //
		Setting.DB_DIRECTORY, //
		new OptionImpl() //
			.setShortOpt('d') //
			.setArgumentLimits(1) //
			.setArgumentName("metadata-directory-or-config") //
			.setRequired(true) //
			.setDescription(
				"The directory into which the metadata database will be stored, or the XML file that describes the store configuration") //
			.setDefault("caliente") //
	), //
	content(
		Setting.CONTENT_DIRECTORY, //
		new OptionImpl() //
			.setShortOpt('c') //
			.setArgumentLimits(1) //
			.setArgumentName("content-directory-or-config") //
			.setRequired(true) //
			.setDescription(
				"The directory into which the content streams will be stored (if omitted, it will be placed in the 'content' subdirectory of the Database directory), or the XML file that describes the store configuration") //
	), //

	//
	// Second pass
	//
	dfc( //
		DfcLaunchHelper.DFC_LOCATION //
	), //
	dfc_prop( //
		DfcLaunchHelper.DFC_PROPERTIES //
	), //
	dctm( //
		DfcLaunchHelper.DFC_DOCUMENTUM //
	), //
	threads( //
		Setting.THREADS, //
		ThreadsLaunchHelper.THREADS //
	), //
	non_recursive( //
		new OptionImpl() //
			.setDescription("Turn off counter recursion (i.e. to count a single folder without descending)") //
	), //
	count_empty( //
		new OptionImpl() //
			.setDescription("Enable reporting of empty folders (i.e. folders with 0 non-folder children)") //
	), //
	count_private( //
		new OptionImpl() //
			.setArgumentLimits(1) //
			.setDescription(
				"For any cabinets encountered, evaluate their private status and include or exclude the private ones. The special value 'any' (default) causes the status to not be taken into account") //
			.setDefault("any") //
			.setValueFilter(new StringValueFilter( //
				false, // Case-insensitive
				"true", "yes", "on", "1", "false", "off", "no", "0", "any" //
		)) //
	), //
	count_hidden( //
		new OptionImpl() //
			.setArgumentLimits(1) //
			.setDescription(
				"For any folders encountered, evaluate their hidden status and include or exclude the hidden ones. The special value 'any' (default) causes the status to not be taken into account") //
			.setDefault("any") //
			.setValueFilter(new StringValueFilter( //
				false, // Case-insensitive
				"true", "yes", "on", "1", "false", "off", "no", "0", "any" //
		)) //
	), //
	count_include( //
		new OptionImpl() //
			.setArgumentLimits(1, -1) //
			.setDescription(
				"Include the folder in the count (defaults to only these, may be specified multiple times) - path or object ID is valid") //
			.setArgumentName("path-or-id") //
	), //
	count_exclude( //
		new OptionImpl() //
			.setArgumentLimits(1, -1) //
			.setDescription(
				"Exclude the folder in the count (defaults to ALL except these, may be specified multiple times) - path or object ID is valid") //
			.setValueSep(',') //
			.setArgumentName("path-or-id") //
	), //
	exclude_types(
		Setting.CMF_EXCLUDE_TYPES, //
		new OptionImpl() //
			.setArgumentLimits(1, -1) //
			.setDescription("The list of object types to be ignored during the operation (comma-separated)") //
			.setValueFilter(new EnumValueFilter<>(false, CmfType.class)) //
			.setValueSep(',') //
			.setArgumentName("type") //
	), //
	cmf_import_target_location(
		Setting.CMF_IMPORT_TARGET_LOCATION, //
		new OptionImpl() //
			.setArgumentLimits(1) //
			.setArgumentName("target-path") //
			.setDescription("The path location into which to import the contents") //
	), //
	cmf_import_trim_prefix(
		Setting.CMF_IMPORT_TRIM_PREFIX, //
		new OptionImpl() //
			.setArgumentLimits(0, 1) //
			.setArgumentName("number") //
			.setDescription("The number of leading path components to trim from the content being imported") //
	), //
	special_users(
		Setting.SPECIAL_USERS, //
		new OptionImpl() //
			.setArgumentLimits(1, -1) //
			.setArgumentName("user") //
			.setDescription("The special users that should not be imported into the target instance") //
			.setValueSep(',') //
	), //
	special_groups(
		Setting.SPECIAL_GROUPS, //
		new OptionImpl() //
			.setArgumentLimits(1, -1) //
			.setArgumentName("group") //
			.setDescription("The special users that should not be imported into the target instance") //
			.setValueSep(',') //
	), //
	special_types(
		Setting.SPECIAL_TYPES, //
		new OptionImpl() //
			.setArgumentLimits(1, -1) //
			.setArgumentName("type") //
			.setDescription("The special types that should not be imported into the target instance") //
			.setValueSep(',') //
	), //
	batch_size( //
		Setting.EXPORT_BATCH_SIZE, //
		new OptionImpl() //
			.setArgumentLimits(1) //
			.setArgumentName("batch-size") //
			.setDescription("The batch size to use when exporting objects from Documentum") //
	), //
	post_process( //
		Setting.POST_PROCESS_IMPORT, //
		new OptionImpl() //
			.setDescription("Whether to post-process the imported content") //
	), //
	source( //
		Setting.EXPORT_PREDICATE, //
		new OptionImpl() //
			.setArgumentLimits(1) //
			.setArgumentName("source-spec") //
			.setDescription("The DQL 'from-where' predicate, or the name of the Sharepoint site, to use for exporting") //
	), //
	shpt_source_prefix(
		Setting.SHPT_SOURCE_PREFIX, //
		new OptionImpl() //
			.setArgumentLimits(1) //
			.setArgumentName("prefix") //
			.setDescription("The prefix to pre-pend to Sharepoint source paths (i.e. /sites is the default)") //
			.setDefault("/sites") //
	), //
	content_strategy(
		Setting.CONTENT_ORGANIZATION, //
		new OptionImpl() //
			.setShortOpt('o') //
			.setArgumentLimits(1) //
			.setArgumentName("organization") //
			.setDescription(
				"The name of the organization strategy to use in the Content directory (specific engines may override with their own defaults if they require it)") //
	), //
	owner_attributes(
		Setting.OWNER_ATTRIBUTES, //
		new OptionImpl() //
			.setArgumentLimits(1) //
			.setArgumentName("attribute-name") //
			.setDescription("The owner_attributes to check for") //
	), //
	errorCount(
		Setting.IMPORT_MAX_ERRORS, //
		new OptionImpl() //
			.setArgumentLimits(1) //
			.setArgumentName("number") //
			.setDescription("The number of errors to accept before aborting an import") //
	), //
	default_password(
		Setting.DEFAULT_USER_PASSWORD, //
		new OptionImpl() //
			.setArgumentLimits(1) //
			.setArgumentName("password") //
			.setDescription(
				"The default password to use for users being copied over (the default is to use the same login name)") //
	), //
	manifest_types(
		Setting.MANIFEST_TYPES, //
		new OptionImpl() //
			.setArgumentLimits(1) //
			.setArgumentName("types") //
			.setValueFilter(new EnumValueFilter<>(false, CmfType.class)) //
			.setDescription("The object types to include in the manifest (not specified = all types)") //
	), //
	manifest_outcomes(
		Setting.MANIFEST_OUTCOMES,
		new OptionImpl() //
			.setArgumentLimits(1) //
			.setArgumentName("outcomes") //
			// TODO: This parameter changes depending on the mode of operation
			.setValueFilter(new EnumValueFilter<>(false, ExportResult.class)) //
			// .setValueFilter(new EnumValueFilter<>(false, ImportResult.class)) //
			.setDescription("The outcomes to include in the manifest (not specified = all outcomes)") //
	), //
	mail_to(
		Setting.MAIL_TO,
		new OptionImpl() //
			.setArgumentLimits(1) //
			.setArgumentName("recipient") //
			// .setValueFilter(new EmailValueFilter()) //
			.setDescription("The primary recipient(s) for the status e-mails") //
	), //
	mail_cc(
		Setting.MAIL_CC,
		new OptionImpl() //
			.setArgumentLimits(1) //
			.setArgumentName("recipient") //
			// .setValueFilter(new EmailValueFilter()) //
			.setDescription("The recipient(s) to CC in the status e-mails") //
	), //
	mail_bcc(
		Setting.MAIL_CC,
		new OptionImpl() //
			.setArgumentLimits(1) //
			.setArgumentName("recipient") //
			// .setValueFilter(new EmailValueFilter()) //
			.setDescription("The recipient(s) to BCC in the status e-mails") //
	), //
	mail_from(
		Setting.MAIL_FROM_ADDX, //
		new OptionImpl() //
			.setArgumentLimits(1) //
			.setArgumentName("sender") //
			// .setValueFilter(new EmailValueFilter()) //
			.setDescription("The e-mail address to use as the sender for the status e-mails") //
	), //
	mail_host(
		Setting.MAIL_SMTP_HOST, //
		new OptionImpl() //
			.setArgumentLimits(1) //
			.setArgumentName("hostname-or-ip") //
			// .setValueFilter(new InetAddressValueFilter()) //
			.setDescription("The SMTP server through which e-mail must be sent") //
	), //
	mail_port(
		Setting.MAIL_SMTP_HOST, //
		new OptionImpl() //
			.setArgumentLimits(1) //
			.setArgumentName("port") //
			.setValueFilter(new IntegerValueFilter(1, true, 65535, true)) //
			.setDescription("The port SMTP server is listening on") //
	), //
	skip_users(
		Setting.SKIP_USERS, //
		new OptionImpl() //
			.setDescription("Skip exporting users") //
	), //
	skip_groups(
		Setting.SKIP_GROUPS, //
		new OptionImpl() //
			.setDescription("Skip exporting groups") //
	), //
	skip_acls(
		Setting.SKIP_ACLS, //
		new OptionImpl() //
			.setDescription("Skip exporting acls") //
	), //
	skip_content( //
		new OptionImpl() //
			.setDescription("Skip importing document contents (only create \"empty\" documents)") //
	), //
	direct_fs( //
		new OptionImpl() //
			.setDescription("Export files to local FS duplicating the CMS's path") //
	), //
	no_renditions( //
		new OptionImpl() //
			.setDescription(
				"Only include the main content stream for documents (and only the first page where multipage is supported)") //
	), //
	no_versions( //
		new OptionImpl() //
			.setDescription("Only include the latest (current) version of documents") //
	), //
	job_name( //
		new OptionImpl() //
			.setArgumentLimits(1) //
			.setDescription("The name of the synchronization job this export is to define") //
	), //
	reset_job( //
		new OptionImpl() //
			.setDescription("Reset any existing synchronization job settings with this export's") //
	), //
	content_model( //
		new OptionImpl() //
			.setArgumentName("content-model-file") //
			.setArgumentLimits(0, -1) //
			.setDescription("The XML files that make up the Alfresco content model to use on import") //
	), //
	user_map( //
		Setting.USER_MAP, //
		new OptionImpl() //
			.setArgumentName("user-map-file") //
			.setDefault("usermap.xml") //
			.setArgumentLimits(1) //
			.setDescription("The Properties (XML) file that contains the user name mappings to apply") //
	), //
	group_map( //
		Setting.GROUP_MAP, //
		new OptionImpl() //
			.setArgumentName("group-map-file") //
			.setDefault("groupmap.xml") //
			.setArgumentLimits(1) //
			.setDescription("The Properties (XML) file that contains the group name mappings to apply") //
	), //
	role_map( //
		Setting.ROLE_MAP,
		new OptionImpl() //
			.setArgumentName("role-map-file") //
			.setDefault("rolemap.xml") //
			.setArgumentLimits(1) //
			.setDescription("The Properties (XML) file that contains the role name mappings to apply") //
	), //
	no_filename_map( //
		new OptionImpl() //
			.setDescription("Disable the use of the filename map (even if the default map exists)") //
	), //
	validate_requirements( //
		new OptionImpl() //
			.setDescription(
				"Activate the validation of an object's requirements' import success during object import (object is skipped if any of its requirements fails to import properly)") //
	), //
	filename_map(
		Setting.FILENAME_MAP, //
		new OptionImpl() //
			.setArgumentName("filename-map-file") //
			.setDefault("filenamemap.xml") //
			.setArgumentLimits(1) //
			.setDescription("The Properties (XML) file that contains the static filename mappings to be applied") //

	), //
	object_store_config( //
		new OptionImpl() //
			.setArgumentName("config-file") //
			.setArgumentLimits(1) //
			.setDescription("The properties file to use for Object Store DB configuration") //
	), //
	content_store_config( //
		new OptionImpl() //
			.setArgumentName("config-file") //
			.setArgumentLimits(1) //
			.setDescription("The properties file to use for Content Store DB configuration") //
	), //
	copy_content( //
		new OptionImpl() //
			.setDescription("Enable the copying of content for the Local engine") //
	), //
	ignore_empty_folders( //
		new OptionImpl() //
			.setDescription("Enable the copying of content for the Local engine") //
	), //
	transformations( //
		new OptionImpl() //
			.setArgumentName("transformations-file") //
			.setArgumentLimits(1) //
			.setDescription("The object transformations descriptor file") //
	), //
	filters( //
		new OptionImpl() //
			.setArgumentName("filters-file") //
			.setArgumentLimits(1) //
			.setDescription("The object filters descriptor file") //
	), //
	external_metadata( //
		new OptionImpl() //
			.setArgumentName("external-metadata-file") //
			.setArgumentLimits(1) //
			.setDescription("The external metadata descriptor file") //
	), //
		//
	;

	public final Setting property;
	public final OptionImpl option;

	private CLIParam(OptionImpl option) {
		this(null, option);
	}

	private CLIParam(Setting property, OptionImpl option) {
		if (option.getLongOpt() == null) {
			option.setLongOpt(name().replace('_', '-'));
		}
		this.option = option;
		this.property = property;
	}

	private CLIParam(Option option) {
		this(null, option);
	}

	private CLIParam(Setting property, Option option) {
		this.option = new OptionImpl(option);
		this.property = property;
	}

	@Override
	public Option getOption() {
		return this.option;
	}

	private void assertValuesValid(OptionValues values) {
		Objects.requireNonNull(values, "Must provide an OptionValues instance");
	}

	public boolean isDefined(OptionValues values) {
		assertValuesValid(values);
		return values.isDefined(this.option);
	}

	public OptionValue getOption(OptionValues values) {
		assertValuesValid(values);
		return values.getOption(this.option);
	}

	public Boolean getBoolean(OptionValues values) {
		assertValuesValid(values);
		return values.getBoolean(this.option);
	}

	public Boolean getBoolean(OptionValues values, Boolean def) {
		assertValuesValid(values);
		return values.getBoolean(this.option, def);
	}

	public List<Boolean> getAllBooleans(OptionValues values) {
		assertValuesValid(values);
		return values.getAllBooleans(this.option);
	}

	public Integer getInteger(OptionValues values) {
		assertValuesValid(values);
		return values.getInteger(this.option);
	}

	public Integer getInteger(OptionValues values, Integer def) {
		assertValuesValid(values);
		return values.getInteger(this.option, def);
	}

	public List<Integer> getAllIntegers(OptionValues values) {
		assertValuesValid(values);
		return values.getAllIntegers(this.option);
	}

	public Long getLong(OptionValues values) {
		assertValuesValid(values);
		return values.getLong(this.option);
	}

	public Long getLong(OptionValues values, Long def) {
		assertValuesValid(values);
		return values.getLong(this.option, def);
	}

	public List<Long> getAllLongs(OptionValues values) {
		assertValuesValid(values);
		return values.getAllLongs(this.option);
	}

	public Float getFloat(OptionValues values) {
		assertValuesValid(values);
		return values.getFloat(this.option);
	}

	public Float getFloat(OptionValues values, Float def) {
		assertValuesValid(values);
		return values.getFloat(this.option, def);
	}

	public List<Float> getAllFloats(OptionValues values) {
		assertValuesValid(values);
		return values.getAllFloats(this.option);
	}

	public Double getDouble(OptionValues values) {
		assertValuesValid(values);
		return values.getDouble(this.option);
	}

	public Double getDouble(OptionValues values, Double def) {
		assertValuesValid(values);
		return values.getDouble(this.option, def);
	}

	public List<Double> getAllDoubles(OptionValues values) {
		assertValuesValid(values);
		return values.getAllDoubles(this.option);
	}

	public String getString(OptionValues values) {
		assertValuesValid(values);
		return values.getString(this.option);
	}

	public String getString(OptionValues values, String def) {
		assertValuesValid(values);
		return values.getString(this.option, def);
	}

	public List<String> getAllStrings(OptionValues values) {
		assertValuesValid(values);
		return values.getAllStrings(this.option);
	}

	public List<String> getAllStrings(OptionValues values, List<String> def) {
		assertValuesValid(values);
		return values.getAllStrings(this.option, def);
	}

	public boolean isPresent(OptionValues values) {
		assertValuesValid(values);
		return values.isPresent(this.option);
	}

	public int getOccurrences(OptionValues values) {
		assertValuesValid(values);
		return values.getOccurrences(this.option);
	}

	public Collection<String> getOccurrenceValues(OptionValues values, int occurrence) {
		assertValuesValid(values);
		return values.getOccurrenceValues(this.option, occurrence);
	}

	public int getValueCount(OptionValues values) {
		assertValuesValid(values);
		return values.getValueCount(this.option);
	}

	public boolean hasValues(OptionValues values) {
		assertValuesValid(values);
		return values.hasValues(this.option);
	}
}