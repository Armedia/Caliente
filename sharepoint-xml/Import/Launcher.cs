using Caliente.SharePoint.Common;
using CommandLine;
using CommandLine.Text;
using log4net;
using log4net.Config;
using Microsoft.Extensions.DependencyInjection;
using Microsoft.SharePoint.Client;
using System;
using System.Collections.Generic;
using System.DirectoryServices;
using System.IO;
using System.Net;
using System.Reflection;
using System.Runtime.Remoting.Messaging;
using System.Security;
using System.Xml.Linq;

namespace Caliente.SharePoint.Import
{
    class Launcher
    {
        private static readonly Crypt CRYPT = new Crypt();
        private static ILog LOG = null;


        public class Configuration
        {
            private class Settings
            {

                [OptionAttribute('c', "cfg", Required = false, HelpText = "The configuration file to use for default values")]
                public string cfg { get; set; }

                [OptionAttribute("siteUrl", Required = false, HelpText = "The SharePoint site to import into")]
                public string siteUrl { get; set; }

                [OptionAttribute("user", Required = false, HelpText = "The user with which to log into SharePoint")]
                public string user { get; set; }

                [OptionAttribute("domain", Required = false, HelpText = "The user's authentication domain for SharePoint")]
                public string domain { get; set; }

                [OptionAttribute("password", Required = false, HelpText = "The password with which to log into SharePoint")]
                public string password { get; set; }

                [OptionAttribute("applicationId", Required = false, HelpText = "The application ID to use for SharePoint Online")]
                public string applicationId { get; set; }

                [OptionAttribute("certificateKey", Required = false, HelpText = "The certificate key to use for SharePoint Online")]
                public string certificateKey { get; set; }

                [OptionAttribute("certificatePass", Required = false, HelpText = "The certificate key's password to use for SharePoint Online")]
                public string certificatePass { get; set; }

                [OptionAttribute("ldapSyncDomain", Required = false, HelpText = "The name of the Documentum LDAP Sync configuration that matches the target AD instance")]
                public string ldapSyncDomain { get; set; }

                [OptionAttribute("library", Required = false, HelpText = "The Document Library to import files into")]
                public string library { get; set; }

                [OptionAttribute("data", Required = false, HelpText = "The root directory where all the Caliente data is located (default = ./caliente)")]
                public string data { get; set; }

                [OptionAttribute("streams", Required = false, HelpText = "The location on the filesystem for the import content streams (default = ${data}/streams)")]
                public string streams { get; set; }

                [OptionAttribute("metadata", Required = false, HelpText = "The location on the filesystem for the import objects' metadata (default = ${data}/xml-metadata)")]
                public string metadata { get; set; }

                [OptionAttribute("progress", Required = false, HelpText = "The location on the filesystem for the progress tracker files (default = ${data}/sharepoint-progress)")]
                public string progress { get; set; }

                [OptionAttribute("caches", Required = false, HelpText = "The location on the filesystem for the work-in-progress data caches (default = ${data}/sharepoint-caches)")]
                public string caches { get; set; }

                [OptionAttribute("logs", Required = false, HelpText = "The location on the filesystem for the log files (default = ${data}/logs)")]
                public string logs { get; set; }

                [OptionAttribute("ldapUrl", Required = false, HelpText = "The LDAP directory to synchronize with")]
                public string ldapUrl { get; set; }

                [OptionAttribute("ldapBindDn", Required = false, HelpText = "The DN with which to bind to LDAP")]
                public string ldapBindDn { get; set; }

                [OptionAttribute("ldapBindPw", Required = false, HelpText = "The password with which to bind to LDAP")]
                public string ldapBindPw { get; set; }

                [OptionAttribute("fallbackUser", Required = false, HelpText = "The user to map to when a user can't be resolved in LDAP")]
                public string fallbackUser { get; set; }

                [OptionAttribute("internalUser", Required = false, HelpText = "The user to map internal documentum users to")]
                public string internalUser { get; set; }

                [OptionAttribute("fallbackGroup", Required = false, HelpText = "The group to map to when a group can't be resolved in LDAP")]
                public string fallbackGroup { get; set; }

                [OptionAttribute("internalGroup", Required = false, HelpText = "The group to map internal documentum groups to")]
                public string internalGroup { get; set; }

                [OptionAttribute("fallbackDocumentType", Required = false, HelpText = "The content type to use when a document's content type can't be resolved")]
                public string fallbackDocumentType { get; set; }

                [OptionAttribute("fallbackFolderType", Required = false, HelpText = "The content type to use when a folder's content type can't be resolved")]
                public string fallbackFolderType { get; set; }

                [OptionAttribute("threads", Required = false, HelpText = "The number of threads to use in parallel import (min 1, max 32)")]
                public int? threads { get; set; }

                [OptionAttribute("useQueryRetry", Required = false, HelpText = "Use ExecuteQueryRetry() instead of ExecuteQuery() (default = use ExecuteQuery())")]
                public bool? useQueryRetry { get; set; }

                [OptionAttribute("retries", Required = false, HelpText = "The number of times to retry the document import failures when there is no success between attempts (min 0, max 3)")]
                public int? retries { get; set; }

                [OptionAttribute("useRetryWrapper", Required = false, HelpText = "Use the PnP.Framework's HttpClientWebRequestExecutorFactory as a wrapper for HTTP requests")]
                public bool? useRetryWrapper { get; set; }

                [OptionAttribute("orphanAclInherit", Required = false, HelpText = "Define whether parentless folders (from the source) or inherit their destination parent's ACL, or get their own (i.e. break inheritance)")]
                public bool? orphanAclInherit{ get; set; }

                [OptionAttribute("reuseCount", Required = false, HelpText = "The number of times to reuse each ClientContext instance (SharePoint session) before they get discarded (< 0 = forever, min = 1)")]
                public int? reuseCount { get; set; }

                [OptionAttribute("cleanTypes", Required = false, HelpText = "Attempt to clean out existing content types before creating the new types")]
                public bool? cleanTypes { get; set; }

                [OptionAttribute("simulationMode", Required = false, HelpText = "Choose the mode for simulating the content: SHORT = simulate with (small) semi-random data, FULL = simulate with same-sized semi-random data, NONE = use the actual content, MISSING = use the content, or switch to FULL simulation if it's missing")]
                public DocumentImporter.SimulationMode? simulationMode { get; set; }

                [OptionAttribute("autoPublish", Required = false, HelpText = "Choose whether to automatically publish each imported document upon completion")]
                public bool? autoPublish { get; set; }

                [OptionAttribute("locationMode", Required = false, HelpText = "Choose which location to use when importing: the FIRST (default), the LAST as per their version history, or the CURRENT one")]
                public DocumentImporter.LocationMode? locationMode { get; set; }

                [OptionAttribute("fixExtensions", Required = false, HelpText = "Choose whether or not to automatically repair 'invalid' extensions by appending one based on the document's format")]
                public bool? fixExtensions { get; set; }

                [OptionAttribute("uploadSegmentSize", Required = false, HelpText = "The maximum size to attempt a direct upload for. Files larger than this will be uploaded in multiple segments of at most this size (in MB, max 255)")]
                public int? uploadSegmentSize { get; set; }

                [OptionAttribute('?', "help", Required = false, HelpText = "Show this help message")]
                public bool help { get; set; }

                // These are the options intended for internal use only
                public bool? indexOnly { get; set; }

                public static HelpText RenderHelp(ParserResult<Settings> result)
                {
                    return HelpText.AutoBuild(result, //
                            (HelpText ht) => HelpText.DefaultParsingErrorsHandler(result, ht), //
                            e => e //
                        );
                }

                public Settings()
                {

                }

                public Settings(string path)
                {
                    if (!System.IO.File.Exists(path)) return;

                    // Apply the defaults from the configuration
                    Console.Out.WriteLine($"Loading configuration from [{path}]...");
                    XElement cfg = XElement.Load(path);
                    XNamespace ns = cfg.GetDefaultNamespace();
                    object[] parameters = new object[1];
                    foreach (PropertyInfo p in GetType().GetProperties())
                    {
                        OptionAttribute opt = Attribute.GetCustomAttribute(p, typeof(OptionAttribute)) as OptionAttribute;
                        string propertyName = opt?.LongName ?? $"cmsmf.{p.Name}";

                        // Not null? Get its XML value
                        string value = (string)cfg.Element(ns + propertyName);
                        if (value == null) continue;

                        parameters[0] = value;

                        // We have a value from XML...now we determine if the option was read or not... how?
                        Type t = p.PropertyType;
                        if (t != typeof(string))
                        {
                            if (t.IsGenericType && t.GetGenericTypeDefinition() == typeof(Nullable<>))
                            {
                                // This is a nullable object, so its true type is...
                                t = Nullable.GetUnderlyingType(t);
                            }
                            // If the type has a Parse() method that accepts a string, then invoke it
                            if (t.IsEnum)
                            {
                                try
                                {
                                    parameters[0] = Enum.Parse(t, value, true);
                                }
                                catch (ArgumentException e)
                                {
                                    throw new Exception($"The string [{value}] is not a valid member of enum {t.FullName}", e);
                                }
                            }
                            else
                            {
                                MethodInfo parser = t.GetMethod("Parse", new Type[] { typeof(string) });
                                if (parser == null || !parser.IsStatic)
                                {
                                    // No parser...can't do jack...
                                    continue;
                                }
                                parameters[0] = parser.Invoke(null, parameters);
                            }
                        }
                        p.SetValue(this, parameters[0]);
                    }
                }
            }

            private const int MIN_THREADS = 1;
            private static readonly int DEFAULT_THREADS = ((Environment.ProcessorCount * 3) / 4);
            private const int MAX_THREADS = 32;

            private const string DEFAULT_DATA_DIR = "caliente";
            private const string DEFAULT_STREAMS_DIR = "streams";
            private const string DEFAULT_XML_METADATA_DIR = "xml-metadata";
            private const string DEFAULT_PROGRESS_DIR = "sharepoint-progress";
            private const string DEFAULT_CACHES_DIR = "sharepoint-caches";
            private const string DEFAULT_LOGS_DIR = "logs";

            private const bool DEFAULT_USE_QUERY_RETRY = true;

            private const int MIN_RETRIES = 0;
            private const int DEFAULT_RETRIES = 5;
            private const int MAX_RETRIES = 10;

            private const bool DEFAULT_USE_RETRY_WRAPPER = false;

            private const int DEFAULT_REUSE_COUNT = 10;
            private const bool DEFAULT_CLEAN_TYPES = false;
            private const DocumentImporter.SimulationMode DEFAULT_SIMULATE_CONTENT = DocumentImporter.SimulationMode.NONE;
            private const bool DEFAULT_AUTO_PUBLISH = false;
            private const DocumentImporter.LocationMode DEFAULT_USE_LAST_LOCATION = DocumentImporter.LocationMode.FIRST;
            private const bool DEFAULT_FIX_EXTENSIONS = false;

            private const string DEFAULT_LIBRARY = "Documents";
            private const string DEFAULT_FALLBACK_USER = "dm_fb_user";
            private const string DEFAULT_INTERNAL_USER = "dm_int_user";
            private const string DEFAULT_FALLBACK_GROUP = "dm_fb_group";
            private const string DEFAULT_INTERNAL_GROUP = "dm_int_group";

            private const int MIN_UPLOAD_SEGMENT_SIZE = 1;
            private const int MAX_UPLOAD_SEGMENT_SIZE = 255;
            private const int DEFAULT_UPLOAD_SEGMENT_SIZE = 10;
            private const bool DEFAULT_ORPHAN_ACL_INHERIT = true;

            private readonly Settings CommandLine;
            private readonly ParserResult<Settings> ParserResult;
            private readonly Settings ConfigurationFile;

            public string cfg { get; private set; }
            public string siteUrl { get; private set; }
            public string user { get; private set; }
            public string domain { get; private set; }
            public string password { get; private set; }
            public string applicationId { get; private set; }
            public string applicationSecret { get; private set; }
            public string certificateKey { get; private set; }
            public string certificatePass { get; private set; }
            public string ldapSyncDomain { get; private set; }
            public string library { get; private set; }
            public string data { get; private set; }
            public string streams { get; private set; }
            public string metadata { get; private set; }
            public string progress { get; private set; }
            public string caches { get; private set; }
            public string logs { get; private set; }
            public string ldapUrl { get; private set; }
            public string ldapBindDn { get; private set; }
            public string ldapBindPw { get; private set; }
            public string fallbackUser { get; private set; }
            public string internalUser { get; private set; }
            public string fallbackGroup { get; private set; }
            public string internalGroup { get; private set; }
            public string fallbackDocumentType { get; private set; }
            public string fallbackFolderType { get; private set; }
            public int threads { get; private set; }
            public bool useQueryRetry { get; private set; }
            public int retries { get; private set; }
            public bool useRetryWrapper { get; private set; }
            public int reuseCount { get; private set; }
            public bool cleanTypes { get; private set; }
            public DocumentImporter.SimulationMode simulationMode { get; private set; }
            public bool autoPublish { get; private set; }
            public DocumentImporter.LocationMode locationMode { get; private set; }
            public bool fixExtensions { get; private set; }
            public bool indexOnly { get; set; }
            public bool help { get; private set; }
            public string baseDir { get; private set; }
            public int uploadSegmentSize { get; private set; }
            public bool orphanAclInherit { get; private set; }

            public Configuration(string baseDir, params string[] args)
            {
                this.baseDir = baseDir;

                int exitCode = 0;
                this.ParserResult = Parser.Default.ParseArguments<Settings>(args);

                this.ParserResult.WithNotParsed<Settings>(errs =>
                {
                    Console.WriteLine(Settings.RenderHelp(this.ParserResult));
                    exitCode = 1;
                });

                // Explode, if required...
                if (exitCode != 0) Environment.Exit(1);

                this.CommandLine = this.ParserResult.Value;

                string cfgFile = (this.CommandLine.cfg != null ? this.CommandLine.cfg : $"{this.baseDir}\\config.xml");
                this.ConfigurationFile = new Settings(cfgFile);

                object[] parameters = new object[1];
                this.threads = DEFAULT_THREADS;
                this.retries = DEFAULT_RETRIES;
                this.reuseCount = DEFAULT_REUSE_COUNT;
                this.cleanTypes = DEFAULT_CLEAN_TYPES;
                this.simulationMode = DEFAULT_SIMULATE_CONTENT;
                this.autoPublish = DEFAULT_AUTO_PUBLISH;
                this.locationMode = DEFAULT_USE_LAST_LOCATION;
                this.fixExtensions = DEFAULT_FIX_EXTENSIONS;
                this.uploadSegmentSize = DEFAULT_UPLOAD_SEGMENT_SIZE;
                this.useQueryRetry = DEFAULT_USE_QUERY_RETRY;
                this.useRetryWrapper = DEFAULT_USE_RETRY_WRAPPER;
                this.orphanAclInherit = DEFAULT_ORPHAN_ACL_INHERIT;
                foreach (PropertyInfo src in typeof(Settings).GetProperties())
                {
                    PropertyInfo tgt = GetType().GetProperty(src.Name);
                    if (tgt == null) continue;

                    // Validate the type...
                    Type srcType = src.PropertyType;
                    Type tgtType = tgt.PropertyType;
                    if (srcType.IsGenericType && srcType.GetGenericTypeDefinition() == typeof(Nullable<>))
                    {
                        srcType = Nullable.GetUnderlyingType(srcType);
                    }
                    if (tgtType.IsGenericType && tgtType.GetGenericTypeDefinition() == typeof(Nullable<>))
                    {
                        tgtType = Nullable.GetUnderlyingType(tgtType);
                    }

                    if (!tgtType.IsAssignableFrom(srcType)) continue;

                    // First from the command-line
                    object value = src.GetValue(this.CommandLine);
                    // If the command-line failed, try the configuration file
                    if ((value == null) && (this.ConfigurationFile != null)) value = src.GetValue(this.ConfigurationFile);
                    // Only set the value if it's non-null, just in case
                    if (value != null) tgt.SetValue(this, value);
                }

                this.cfg = cfgFile;
            }

            public string GetUsage()
            {
                return Settings.RenderHelp(this.ParserResult);
            }

            private string ComputePath(string value, Func<string> fallback)
            {
                string ret = value;
                if (string.IsNullOrEmpty(value) && (fallback != null)) ret = fallback();
                if (!string.IsNullOrEmpty(ret)) ret = Path.GetFullPath(ret).Replace('\\', '/');
                return ret;
            }

            public List<string> ValidateConfiguration()
            {
                List<string> errors = new List<string>();
                if (string.IsNullOrWhiteSpace(this.siteUrl)) errors.Add("Must provide a URL with which to connect to Sharepoint (siteUrl)");

                // TODO: This may not need to be provided if we're using app authentication
                /*
                if (string.IsNullOrWhiteSpace(this.user) && string.IsNullOrWhiteSpace(this.applicationId))
                {
                    errors.Add("Must provide either a user name or an application ID with which to connect to Sharepoint (user)");
                }
                */

                if (!string.IsNullOrWhiteSpace(this.ldapUrl))
                {
                    if (string.IsNullOrWhiteSpace(this.ldapSyncDomain)) errors.Add("Must provide the name of the LDAP Sync configuration which maps to the primary user domain (ldapSyncDomain)");
                }

                if (!string.IsNullOrWhiteSpace(this.applicationId))
                {
                    if (string.IsNullOrWhiteSpace(this.certificateKey)) this.certificateKey = $"{this.baseDir}\\Caliente.pfx";
                    if (string.IsNullOrWhiteSpace(this.domain)) errors.Add("Must provide the domain the application ID is valid for (domain)");
                }
                if (errors.Count > 0) return errors;

                this.data = ComputePath(this.data, () => $"${this.baseDir}/{DEFAULT_DATA_DIR}");
                if (!Directory.Exists(this.data))
                {
                    errors.Add($"The data directory [{this.data}] does not exist");
                }

                this.streams = ComputePath(this.streams, () => $"{this.data}/{DEFAULT_STREAMS_DIR}");
                if (!Directory.Exists(this.streams))
                {
                    errors.Add($"The streams directory [{this.streams}] does not exist");
                }

                this.metadata = ComputePath(this.metadata, () => $"{this.data}/{DEFAULT_XML_METADATA_DIR}");
                if (!Directory.Exists(this.metadata))
                {
                    errors.Add($"The metadata directory [{this.metadata}] does not exist");
                }

                this.progress = ComputePath(this.progress, () => $"{this.data}/{DEFAULT_PROGRESS_DIR}");
                this.caches = ComputePath(this.caches, () => $"{this.data}/{DEFAULT_CACHES_DIR}");
                this.logs = ComputePath(this.logs, () => $"{this.data}/{DEFAULT_LOGS_DIR}");

                if (string.IsNullOrWhiteSpace(this.ldapBindDn)) this.ldapBindDn = "";
                if (string.IsNullOrEmpty(this.ldapBindPw)) this.ldapBindPw = "";

                if (string.IsNullOrWhiteSpace(this.library)) this.library = DEFAULT_LIBRARY;
                if (string.IsNullOrWhiteSpace(this.fallbackUser)) this.fallbackUser = DEFAULT_FALLBACK_USER;
                if (string.IsNullOrWhiteSpace(this.internalUser)) this.internalUser = DEFAULT_INTERNAL_USER;
                if (string.IsNullOrWhiteSpace(this.fallbackGroup)) this.fallbackGroup = DEFAULT_FALLBACK_GROUP;
                if (string.IsNullOrWhiteSpace(this.internalGroup)) this.internalGroup = DEFAULT_INTERNAL_GROUP;

                if (this.threads < MIN_THREADS) this.threads = MIN_THREADS;
                if (this.threads > MAX_THREADS) this.threads = MAX_THREADS;
                if (this.reuseCount < 0) this.reuseCount = -1;
                if (this.reuseCount == 0) this.reuseCount = 1;
                if (this.retries < MIN_RETRIES) this.retries = MIN_RETRIES;
                if (this.retries > MAX_RETRIES) this.retries = MAX_RETRIES;
                if (this.uploadSegmentSize < MIN_UPLOAD_SEGMENT_SIZE) this.uploadSegmentSize = MIN_UPLOAD_SEGMENT_SIZE;
                if (this.uploadSegmentSize > MAX_UPLOAD_SEGMENT_SIZE) this.uploadSegmentSize = MAX_UPLOAD_SEGMENT_SIZE;
                return errors;
            }
        }

        private static DirectoryEntry BindToLDAP(Configuration options)
        {
            if (string.IsNullOrWhiteSpace(options.ldapUrl)) return null;
            if (string.IsNullOrWhiteSpace(options.ldapBindDn)) return new DirectoryEntry(options.ldapUrl);

            string ldapBindPw = options.ldapBindPw;
            if (string.IsNullOrEmpty(ldapBindPw))
            {
                Console.Write($"Enter LDAP password for DN=[{options.ldapBindDn}] @ [{options.ldapUrl}]: ");
                SecureString password = Tools.ReadPassword();
                ldapBindPw = password.ToString();
            }
            else
            {
                ldapBindPw = CRYPT.Decrypt(options.ldapBindPw);
            }
            return new DirectoryEntry(options.ldapUrl, options.ldapBindDn, ldapBindPw);
        }

        private static string GetVersionInfo()
        {
            Assembly assembly = Assembly.GetExecutingAssembly();
            Version version = assembly.GetName().Version;
            String title = assembly.GetCustomAttribute<AssemblyTitleAttribute>().Title;
            DateTime buildDate = new DateTime(2000, 1, 1).Add(new TimeSpan(TimeSpan.TicksPerDay * version.Build + TimeSpan.TicksPerSecond * 2 * version.Revision)).ToUniversalTime();
            return $"{title} v{version} (built at {buildDate} UTC)";
        }

        private static string GetExeLocation()
        {
            return Path.GetDirectoryName(Environment.GetCommandLineArgs()[0]);
        }

        private static string GetExeName()
        {
            return Path.GetFileNameWithoutExtension(Environment.GetCommandLineArgs()[0]);
        }


        public static void Main(string[] args)
        {
            int ret = 0;
            try
            {
                ret = MainLoop(args);
            }
            catch (Exception e)
            {
                ret = 1;
                if (LOG != null)
                {
                    LOG.Error("Uncaught exception caused a program crash", e);
                }
                else
                {
                    Console.Error.WriteLine(GetVersionInfo());
                    Console.Error.WriteLine("Uncaught exception caused a program crash: {0}", e);
                    Console.Error.WriteLine(e.StackTrace);
                    Console.Error.WriteLine("Press any key to exit...");
                    Console.ReadKey();
                }
            }
            finally
            {
                Environment.Exit(ret);
            }
        }

        private static string FindLogConfiguration(string dir, string fileName)
        {
            string config = $"{dir}\\{fileName}";
            if (!System.IO.File.Exists(config)) return null;
            return config;
        }

        private static bool ConfigureLogging(string baseDir)
        {
            string[] nameOptions = { $"{GetExeName()}.log.xml", "log4net.xml" };
            string[] directoryOptions = { baseDir, GetExeLocation() };
            foreach (string directory in directoryOptions)
            {
                foreach (string name in nameOptions)
                {
                    string config = FindLogConfiguration(directory, name);
                    if (!string.IsNullOrEmpty(config))
                    {
                        Console.Out.WriteLine($"Initializing logging from [{config}]...");
                        using (Stream stream = System.IO.File.OpenRead(config))
                        {
                            XmlConfigurator.Configure(stream);
                        }
                        return true;
                    }
                }
            }
            return false;
        }

        public static int MainLoop(string[] args)
        {
            string version = GetVersionInfo();

            ILog log = null;
            string baseDir = Directory.GetCurrentDirectory();
            // Initialize log4j
            Configuration options = new Configuration(baseDir, args);

            if (options.help)
            {
                Console.Error.WriteLine(version);
                Console.Error.WriteLine(options.GetUsage());
                return 1;
            }
            List<string> errors = options.ValidateConfiguration();
            if (errors.Count > 0)
            {
                Console.Error.WriteLine(version);
                Console.Error.WriteLine($"{errors.Count} Configuration Errors detected:");
                foreach (string e in errors) Console.Error.WriteLine($"\t* {e}");
                return 2;
            }

            System.IO.Directory.CreateDirectory(options.caches);
            System.IO.Directory.CreateDirectory(options.logs);

            Environment.SetEnvironmentVariable("CMF_LOGDATE", $"{DateTime.Now:yyyyMMdd-HHmmss}");
            Environment.SetEnvironmentVariable("CMF_LOGDIR", options.logs);

            ConfigureLogging(baseDir);
            LOG = log = LogManager.GetLogger(typeof(Launcher));
            log.Info("Initializing Application");
            log.Info(version);

            if (options.indexOnly)
            {
                ImportContext importContext = new ImportContext(null, options.streams, options.metadata, options.progress, options.caches);
                FormatResolver formatResolver = new FormatResolver(importContext);
                new DocumentImporter(new FolderImporter(importContext, options.fallbackFolderType, options.orphanAclInherit), formatResolver, options.locationMode, options.fixExtensions, options.fallbackDocumentType, options.uploadSegmentSize).StoreLocationIndex();
                return 0;
            }

            ServicePointManager.MaxServicePointIdleTime = (int)SharePointSession.TIME_OUT.TotalMilliseconds;
            ServicePointManager.SetTcpKeepAlive(true, (int)SharePointSession.TIME_OUT.TotalMilliseconds, 60000);
            ServicePointManager.DefaultConnectionLimit = options.threads * 10;

            using (DirectoryEntry ldapDirectory = BindToLDAP(options))
            {
                log.Info($"Using SharePoint at [{options.siteUrl}]");

                string userString = options.user;
                if (!string.IsNullOrWhiteSpace(options.domain))
                {
                    userString = $"{userString}@{options.domain}";
                }

                SecureString userPassword = null;
                if (!string.IsNullOrWhiteSpace(options.user)) {
                    if (options.password == null)
                    {
                        Console.Write($"Enter The Sharepoint Password for [{userString}]: ");
                        userPassword = Tools.ReadPassword();
                    }
                    else
                    {
                        String pass = CRYPT.Decrypt(options.password);
                        pass = CRYPT.Encrypt(pass);
                        log.Info($"Using stored credentials for [{userString}] = [{pass}]");
                        userPassword = new SecureString();
                        foreach (char c in CRYPT.Decrypt(pass))
                        {
                            userPassword.AppendChar(c);
                        }
                    }
                }

                using (SharePointSessionFactory sessionFactory = new SharePointSessionFactory(new SharePointSessionInfo(options.siteUrl, options.user, userPassword, options.domain, options.applicationId, options.certificateKey, options.certificatePass, options.library, options.reuseCount, options.useQueryRetry, options.retries, options.useRetryWrapper)))
                {
                    ImportContext importContext = new ImportContext(sessionFactory, options.streams, options.metadata, options.progress, options.caches);
                    using (ObjectPool<SharePointSession>.Ref sessionRef = sessionFactory.GetSession())
                    {
                        SharePointSession session = sessionRef.Target;
                        ClientContext clientContext = session.ClientContext;
                        List documentLibrary = sessionRef.Target.DocumentLibrary;
                        // We configure the document library as required
                        documentLibrary.EnableVersioning = true;
                        documentLibrary.EnableMinorVersions = true;
                        documentLibrary.ForceCheckout = false;
                        documentLibrary.ContentTypesEnabled = true;
                        // documentLibrary.MajorVersionLimit = 50000;
                        // documentLibrary.MajorWithMinorVersionsLimit = 50000;
                        documentLibrary.Update();
                        session.ExecuteQuery();
                    }

                    FormatResolver formatResolver = new FormatResolver(importContext);

                    ContentTypeImporter contentTypeImporter = null;
                    for (int i = 0 ; i <= options.retries ; i++)
                    {
                        try
                        {
                            contentTypeImporter = new ContentTypeImporter(importContext, options.library, options.cleanTypes);
                            break;
                        }
                        catch (Exception e)
                        {
                            contentTypeImporter = null;
                            log.Warn("WARNING: ContentTypeImporter failed to initialize due to an exception", e);
                        }
                    }
                    if (contentTypeImporter == null)
                    {
                        log.ErrorFormat("ContentTypeImporter failed to initialize after {0} attempts", options.retries + 1);
                        return 3;
                    }

                    UserGroupImporter userGroupImporter = null;
                    for (int i = 0 ; i <= options.retries ; i++)
                    {
                        try
                        {
                            userGroupImporter = new UserGroupImporter(importContext, ldapDirectory, options.ldapSyncDomain, options.fallbackUser, options.internalUser, options.fallbackGroup, options.internalGroup);
                            break;
                        }
                        catch (Exception e)
                        {
                            userGroupImporter = null;
                            log.Warn("WARNING: UserGroupImporter failed to initialize due to an exception", e);
                        }
                    }
                    if (userGroupImporter == null)
                    {
                        log.ErrorFormat("UserGroupImporter failed to initialize after {0} attempts", options.retries + 1);
                        return 4;
                    }

                    PermissionsImporter permissionsImporter = new PermissionsImporter(userGroupImporter);
                    FolderImporter folderImporter = new FolderImporter(contentTypeImporter, permissionsImporter, options.fallbackFolderType, options.orphanAclInherit);
                    DocumentImporter documentImporter = new DocumentImporter(folderImporter, formatResolver, options.locationMode, options.fixExtensions, options.fallbackDocumentType, options.uploadSegmentSize);
                    bool aborted = false;

                    Console.CancelKeyPress += delegate (object sender, ConsoleCancelEventArgs e)
                    {
                        if (aborted) return;
                        aborted = true;
                        e.Cancel = true;
                        documentImporter.Abort = true;
                        folderImporter.Abort = true;
                        string msg = "Program Interrupted (hit Ctrl-C again to terminate immediately)";
                        if (log == null)
                        {
                            Console.WriteLine(msg);
                        }
                        else
                        {
                            log.Warn(msg);
                        }
                    };

                    try
                    {
                        documentImporter.StoreDocuments(options.threads, options.simulationMode, options.locationMode, options.autoPublish, options.retries);
                        folderImporter.FinalizeFolders(importContext, options.threads, options.retries);
                        documentImporter.StoreLocationIndex();
                    }
                    finally
                    {
                        log.Info(documentImporter.ProcessingReport);
                        log.Info(folderImporter.ProcessingReport);
                    }
                    return (aborted ? 1 : 0);
                }
            }
        }
    }
}
