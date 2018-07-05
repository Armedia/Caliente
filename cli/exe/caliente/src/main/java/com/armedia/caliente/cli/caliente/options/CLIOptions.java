package com.armedia.caliente.cli.caliente.options;

import java.net.InetAddress;
import java.net.UnknownHostException;

import com.armedia.caliente.cli.EnumValueFilter;
import com.armedia.caliente.cli.IntegerValueFilter;
import com.armedia.caliente.cli.Option;
import com.armedia.caliente.cli.OptionImpl;
import com.armedia.caliente.cli.OptionValueFilter;
import com.armedia.caliente.cli.RegexValueFilter;
import com.armedia.caliente.cli.caliente.utils.SmtpServer.SslMode;
import com.armedia.caliente.cli.utils.LibLaunchHelper;
import com.armedia.caliente.cli.utils.ThreadsLaunchHelper;
import com.armedia.caliente.store.CmfType;

public class CLIOptions {

	private static final OptionValueFilter EMAIL_FILTER = new RegexValueFilter(false, null);
	private static final OptionValueFilter INET_ADDX_FILTER = new OptionValueFilter() {

		@Override
		protected boolean checkValue(String value) {
			try {
				InetAddress.getByName(value);
				return true;
			} catch (UnknownHostException e) {
			}
			return false;
		}

		@Override
		public String getDefinition() {
			return "Dotted-IP-address or a valid hostname";
		}

	};

	/*
	private static final OptionValueFilter URI_FILTER = new OptionValueFilter() {
	
		@Override
		protected boolean checkValue(String value) {
			try {
				new URI(value);
				return true;
			} catch (URISyntaxException e) {
			}
			return false;
		}
	
		@Override
		public String getDefinition() {
			return "A URI (as per java.net.URI)";
		}
	
	};
	
	private static final OptionValueFilter URL_FILTER = new OptionValueFilter() {
	
		@Override
		protected boolean checkValue(String value) {
			try {
				new URL(value);
				return true;
			} catch (MalformedURLException e) {
			}
			return false;
		}
	
		@Override
		public String getDefinition() {
			return "A URL (as per java.net.URL)";
		}
	
	};
	*/

	public static final Option HELP = new OptionImpl() //
		.setShortOpt('h') //
		.setLongOpt("help") //
		.setDescription("This help message") //
	;

	public static final Option LIB = LibLaunchHelper.LIB;

	public static final Option LOG = new OptionImpl() //
		.setLongOpt("log-name") //
		.setArgumentLimits(1) //
		.setDescription("The base name of the log file to use (${logName}).") //
		.setDefault(CalienteBaseOptions.DEFAULT_LOG_FORMAT) //
		.setArgumentName("log-name-template") //
	;

	public static final Option LOG_CFG = new OptionImpl() //
		.setLongOpt("log-cfg") //
		.setArgumentLimits(1) //
		.setDescription(
			"The Log4j configuration (XML format) to use instead of the default (can reference ${logName} from --log)") //
		.setArgumentName("configuration") //
	;

	public static final Option ENGINE = new OptionImpl() //
		.setShortOpt('e') //
		.setArgumentLimits(1) //
		.setRequired(true) //
		.setDescription("The ECM engine to use") //
		.setArgumentName("engine") //
	;

	public static final Option THREADS = ThreadsLaunchHelper.THREADS;

	public static final Option NO_RENDITIONS = new OptionImpl() //
		.setLongOpt("no-renditions") //
		.setDescription("Disable renditions processing") //
	;

	public static final Option NO_VERSIONS = new OptionImpl() //
		.setLongOpt("no-versions") //
		.setDescription("Only operate on the objects' current versions") //
	;

	public static final Option SKIP_CONTENT = new OptionImpl() //
		.setLongOpt("skip-content") //
		.setDescription("Don't process the actual content streams") //
	;

	public static final Option EXCLUDE_TYPES = new OptionImpl() //
		.setLongOpt("exclude-types") //
		.setArgumentName("object-type(s)") //
		.setValueFilter(new EnumValueFilter<>(false, CmfType.class)) //
		.setMinArguments(1) //
		.setDescription("Disable renditions processing") //
	;

	public static final Option TRANSFORMATIONS = new OptionImpl() //
		.setLongOpt("transformation") //
		.setArgumentName("transformations-file") //
		.setArgumentLimits(1) //
		.setDescription("The object transformations descriptor file") //
	;

	public static final Option FILTERS = new OptionImpl() //
		.setLongOpt("filter") //
		.setArgumentName("filters-file") //
		.setArgumentLimits(1) //
		.setDescription("The object filters descriptor file") //
	;

	public static final Option EXTERNAL_METADATA = new OptionImpl() //
		.setLongOpt("external-metadata") //
		.setArgumentName("external-metadata-file") //
		.setArgumentLimits(1) //
		.setDescription("The external metadata descriptor file") //
	;

	public static final Option DIRECT_FS = new OptionImpl() //
		.setDescription("Export files to local FS duplicating the CMS's path") //
	;

	public static final Option SOURCE = new OptionImpl() //
		.setArgumentLimits(1) //
		.setRequired(true) //
		.setArgumentName("source-spec") //
		.setDescription("The source specification identifying which content to extract") //
	;

	public static final Option MAIL_FROM = new OptionImpl() //
		.setLongOpt("mail-from") //
		.setValueFilter(CLIOptions.EMAIL_FILTER) //
		.setArgumentLimits(1) //
		.setDescription("Sender for the status e-mail") //
	;

	public static final Option MAIL_TO = new OptionImpl() //
		.setLongOpt("mail-to") //
		.setValueFilter(CLIOptions.EMAIL_FILTER) //
		.setMinArguments(1) //
		.setDescription("Recipients for the status e-mail") //
	;

	public static final Option MAIL_CC = new OptionImpl() //
		.setLongOpt("mail-cc") //
		.setValueFilter(CLIOptions.EMAIL_FILTER) //
		.setMinArguments(1) //
		.setDescription("Carbon Copy Recipients for the status e-mail") //
	;

	public static final Option MAIL_BCC = new OptionImpl() //
		.setLongOpt("mail-bcc") //
		.setValueFilter(CLIOptions.EMAIL_FILTER) //
		.setMinArguments(1) //
		.setDescription("Blind Carbon Copy Recipients for the status e-mail") //
	;

	public static final Option MAIL_HOST = new OptionImpl() //
		.setLongOpt("mail-host") //
		.setValueFilter(CLIOptions.INET_ADDX_FILTER) //
		.setMinArguments(1) //
		.setDefault("127.0.0.1") //
		.setDescription("SMTP host to post the status e-mail to") //
	;

	public static final Option MAIL_PORT = new OptionImpl() //
		.setLongOpt("mail-port") //
		.setValueFilter(new IntegerValueFilter(1, 65535)) //
		.setMinArguments(1) //
		.setDefault("25") //
		.setDescription("The port at which the mail-host is listening") //
	;

	public static final Option MAIL_SSL = new OptionImpl() //
		.setLongOpt("mail-ssl") //
		.setValueFilter(new EnumValueFilter<>(false, SslMode.class)) //
		.setMinArguments(1) //
		.setDefault(SslMode.NONE.name()) //
		.setDescription("The SSL mode to use when connecting to the server") //
	;

	public static final Option MAIL_USER = new OptionImpl() //
		.setLongOpt("mail-user") //
		.setArgumentLimits(1) //
		.setDescription("The user with which to authenticate to the SMTP host") //
	;

	public static final Option MAIL_PASSWORD = new OptionImpl() //
		.setLongOpt("mail-user") //
		.setArgumentLimits(1) //
		.setDescription("The password with which to authenticate to the SMTP host") //
	;

	public static final Option MAIL_AUTH = new OptionImpl() //
		.setLongOpt("mail-auth") //
		.setArgumentLimits(1) //
		.setDescription("The authentication mode to use when connecting to the SMTP host") //
	;
}