using Armedia.CMSMF.SharePoint.Common;
using log4net;
using Microsoft.SharePoint.Client;
using System;
using System.Collections.Generic;
using System.DirectoryServices;
using System.Linq;
using System.Text;
using System.Xml;
using System.Xml.Linq;

// Reference: http://stackoverflow.com/questions/10168240/encrypting-decrypting-a-string-in-c-sharp
namespace Armedia.CMSMF.SharePoint.Import
{
    public class UserGroupImporter : BaseImporter
    {
        private const int PAGE_SIZE = 1000;
        public const string DM_WORLD = "dm_world";
        public const string DM_OWNER = "dm_owner";
        private const string DM_GROUP = "dm_group";

        private const string EVERYONE_NAME = "Everyone";
        private const string EVERYONE_ID = "c:o(.s|true";

        private static readonly TimeSpan MAX_AGE = new TimeSpan(6, 0, 0);

        private class ImportedPrincipalInfo
        {
            protected readonly ILog Log;

            public readonly string Name;
            public readonly string Domain;
            public readonly string LoginName;
            public readonly string KerberosId;
            public readonly string Guid;
            public readonly bool Enabled;

            public readonly string FullLogin;

            private ImportedPrincipalInfo()
            {
                this.Log = LogManager.GetLogger(GetType());
            }

            public ImportedPrincipalInfo(string name, string domain, string loginName) : this()
            {
                this.Name = name;
                this.Domain = domain;
                this.LoginName = loginName.ToLower();
                this.KerberosId = null;
                this.Guid = null;
                this.Enabled = false;

                if (!string.IsNullOrEmpty(this.Domain))
                {
                    this.FullLogin = string.Format("{0}\\{1}", this.Domain, this.LoginName);
                }
                else
                {
                    this.FullLogin = this.LoginName;
                }
            }

            public ImportedPrincipalInfo(XElement xml) : this()
            {
                XNamespace ns = xml.GetDefaultNamespace();
                this.Name = (string)xml.Element(ns + "name");
                this.Domain = (string)xml.Element(ns + "domain");
                this.LoginName = (string)xml.Element(ns + "loginName");
                this.KerberosId = (string)xml.Element(ns + "kerberosId");
                this.Enabled = XmlConvert.ToBoolean((string)xml.Element(ns + "enabled"));
                this.Guid = (string)xml.Element(ns + "guid");

                if (!string.IsNullOrEmpty(this.Domain))
                {
                    this.FullLogin = string.Format("{0}\\{1}", this.Domain, this.LoginName);
                }
                else
                {
                    this.FullLogin = this.LoginName;
                }
            }

            public ImportedPrincipalInfo(SearchResult r, string domain) : this()
            {
                this.Name = (string)r.Properties["cn"][0];
                this.Domain = domain;
                this.LoginName = ((string)r.Properties["samaccountname"][0]).ToLower();
                this.KerberosId = (r.Properties.Contains("userprincipalname") ? (string)r.Properties["userprincipalname"][0] : null);

                if (r.Properties.Contains("useraccountcontrol"))
                {
                    this.Enabled = !Convert.ToBoolean((int)r.Properties["useraccountcontrol"][0] & 0x0002);
                }
                else
                {
                    this.Enabled = true;
                }
                byte[] guid = (byte[])r.Properties["objectguid"][0];
                this.Guid = (guid != null ? encodeHex(guid) : null);

                if (!string.IsNullOrEmpty(this.Domain))
                {
                    this.FullLogin = string.Format("{0}\\{1}", this.Domain, this.LoginName);
                }
                else
                {
                    this.FullLogin = this.LoginName;
                }
            }

            private static string encodeHex(byte[] arr)
            {
                if (arr == null || arr.Length == 0) return String.Empty;
                return BitConverter.ToString(arr).Replace("-", "").ToLower();
            }

            public Principal Resolve(ClientContext clientContext)
            {
                if (clientContext == null) throw new ArgumentNullException("clientContext");
                if (Log.IsDebugEnabled)
                {
                    Log.Debug(string.Format("Resolving Principal [{0}]...", this.FullLogin));
                }
                Principal principal = clientContext.Web.EnsureUser(this.FullLogin);
                clientContext.Load(principal, p => p.Id, p => p.LoginName, p => p.PrincipalType);
                return principal;
            }

            public void WriteXml(XmlWriter w)
            {
                w.WriteStartElement("entry");

                w.WriteStartElement("name");
                w.WriteString(this.Name);
                w.WriteEndElement();

                if (this.Domain != null)
                {
                    w.WriteStartElement("domain");
                    w.WriteString(this.Domain);
                    w.WriteEndElement();
                }

                w.WriteStartElement("loginName");
                w.WriteString(this.LoginName);
                w.WriteEndElement();

                if (this.KerberosId != null)
                {
                    w.WriteStartElement("kerberosId");
                    w.WriteString(this.KerberosId);
                    w.WriteEndElement();
                }


                if (this.Guid != null)
                {
                    w.WriteStartElement("guid");
                    w.WriteString(this.Guid);
                    w.WriteEndElement();
                }

                w.WriteStartElement("enabled");
                w.WriteString(XmlConvert.ToString(this.Enabled));
                w.WriteEndElement();

                w.WriteEndElement();
            }
        }

        private System.IO.FileInfo GetCacheFile(DirectoryEntry ldapDirectory, string type)
        {
            string path = ldapDirectory.Path;
            string domain = ((string)ldapDirectory.Properties["name"][0]).ToUpper();
            string fileName = string.Format("cache.{0}.{1}@{2}.xml", type, domain, path);
            fileName = Tools.MakeSafeFileName(fileName);
            fileName = this.ImportContext.FormatCacheLocation(fileName);
            return new System.IO.FileInfo(fileName);
        }

        private XmlReader LoadCache(DirectoryEntry ldapDirectory, string type)
        {
            System.IO.FileInfo file = GetCacheFile(ldapDirectory, type);
            if (!file.Exists) return null;
            XmlReaderSettings settings = new XmlReaderSettings();
            settings.DtdProcessing = DtdProcessing.Parse;
            settings.MaxCharactersFromEntities = 1024;
            System.IO.StreamReader stream = file.OpenText();
            XmlReader r = null;
            bool ok = false;
            try
            {
                r = XmlReader.Create(stream, settings);
                ok = true;
            }
            finally
            {
                // This defends against parsing errors, causing the cache to be completely trashed
                if (!ok)
                {
                    stream.Close();
                    file.Delete();
                }
            }
            if (!r.ReadToFollowing(type)) return null;
            DateTime date = file.LastWriteTimeUtc;
            try
            {
                if (r.HasAttributes && r.MoveToAttribute("date"))
                {
                    date = XmlConvert.ToDateTime(r.Value, XmlDateTimeSerializationMode.Utc);
                }
                else
                {
                    Log.Warn(string.Format("Cache for {0} has no date stamp, will use the file's modification time instead", type));
                }
            }
            catch (Exception e)
            {
                // Not a valid date in XML, use the file's modification date
                if (Log.IsDebugEnabled)
                {
                    Log.Warn(string.Format("Failed to read the {0} cache's date stamp, will use the file's modification time instead", type), e);
                }
                else
                {
                    Log.Warn(string.Format("Failed to read the {0} cache's date stamp, will use the file's modification time instead", type));
                }
            }

            if ((DateTime.UtcNow - date) >= MAX_AGE)
            {
                Log.Warn(string.Format("Cache for {0} is too old, discarding it (MAX_AGE = {1})", type, MAX_AGE));
                r.Close();
                return null;
            }
            Log.Warn(string.Format("Cache for {0} is valid, will obtain entries from it (created on {1})", type, date));
            return r;
        }

        private void StoreCache(DirectoryEntry ldapDirectory, string type, ICollection<ImportedPrincipalInfo> principals)
        {
            System.IO.FileInfo file = GetCacheFile(ldapDirectory, type);
            using (XmlTextWriter w = new XmlFile(file, UTF8Encoding.UTF8, false))
            {
                w.WriteStartDocument();
                w.WriteDocType(type, null, null, null);
                w.WriteStartElement(type);
                w.WriteAttributeString("date", XmlConvert.ToString(DateTime.UtcNow, XmlDateTimeSerializationMode.Utc));
                foreach (ImportedPrincipalInfo info in principals)
                {
                    info.WriteXml(w);
                    w.Flush();
                }
                w.WriteEndDocument();
                w.Flush();
            }
        }

        private Dictionary<string, ImportedPrincipalInfo> generateUserMappings(DirectoryEntry ldapDirectory, string ldapSyncAlias, string fallbackName, string internalName)
        {
            Dictionary<string, ImportedPrincipalInfo> users = new Dictionary<string, ImportedPrincipalInfo>();
            // Step 1: Get all the AD users in one pass - this is quicker
            string domain = ((string)ldapDirectory.Properties["name"][0]).ToUpper();
            Log.Info(string.Format("Loading LDAP Users from {0} (domain = {1})", ldapDirectory.Path, domain));
            Dictionary<string, ImportedPrincipalInfo> usersByGUID = new Dictionary<string, ImportedPrincipalInfo>();
            Dictionary<string, ImportedPrincipalInfo> usersByLogin = new Dictionary<string, ImportedPrincipalInfo>();
            Dictionary<string, ImportedPrincipalInfo> usersByKerberosId = new Dictionary<string, ImportedPrincipalInfo>();

            XmlReader cache = LoadCache(ldapDirectory, "users");
            bool cacheLoaded = false;
            if (cache != null)
            {
                try
                {
                    using (cache)
                    {
                        int c = 0;
                        Log.Info("Loading users from the cache...");
                        while (cache.ReadToFollowing("entry"))
                        {
                            ImportedPrincipalInfo info = new ImportedPrincipalInfo(XElement.Load(cache.ReadSubtree()));
                            if (info.Enabled)
                            {
                                usersByGUID[info.Guid] = info;
                                usersByLogin[info.LoginName] = info;
                                if (info.KerberosId != null) usersByKerberosId[info.KerberosId] = info;
                            }
                            if ((++c % PAGE_SIZE) == 0) Log.Info(string.Format("Loaded {0} users from the cache", c));
                        }
                        cacheLoaded = true;
                    }
                }
                catch (Exception e)
                {
                    usersByGUID.Clear();
                    usersByLogin.Clear();
                    usersByKerberosId.Clear();
                    Log.Warn(string.Format("Failed to load data from the user cache for LDAP directory [{0}] (for {1})", ldapDirectory.Path, domain), e);
                }
                finally
                {
                    cache = null;
                }
            }

            if (!cacheLoaded)
            {
                using (DirectorySearcher ldapSearch = new DirectorySearcher(ldapDirectory))
                {
                    ldapSearch.Filter = "(&(objectClass=user)(objectCategory=person))";
                    ldapSearch.PageSize = PAGE_SIZE;
                    using (SearchResultCollection ldapResults = ldapSearch.FindAll())
                    {
                        int c = 0;
                        foreach (SearchResult r in ldapResults)
                        {
                            ImportedPrincipalInfo info = new ImportedPrincipalInfo(r, domain);
                            if (info.Enabled)
                            {
                                usersByGUID[info.Guid] = info;
                                usersByLogin[info.LoginName] = info;
                                if (info.KerberosId != null) usersByKerberosId[info.KerberosId] = info;
                            }
                            if ((++c % ldapSearch.PageSize) == 0) Log.Info(string.Format("Loaded {0} users", c));
                        }
                    }
                }

                List<string> logins = new List<string>(usersByLogin.Keys);
                List<ImportedPrincipalInfo> items = new List<ImportedPrincipalInfo>(usersByLogin.Count);
                logins.Sort();
                foreach (string l in logins)
                {
                    items.Add(usersByLogin[l]);
                }
                try
                {
                    StoreCache(ldapDirectory, "users", items);
                }
                catch (Exception e)
                {
                    Log.Warn(string.Format("Failed to write the user cache for the LDAP directory at [{0}] (domain {1})", ldapDirectory.Path, ldapDirectory.Name), e);
                }
            }

            // Step 2: sanitize the fallback users.
            if ((fallbackName != null) && !usersByLogin.ContainsKey(fallbackName)) fallbackName = null;
            if ((internalName != null) && !usersByLogin.ContainsKey(internalName)) internalName = null;

            // Step 3: Scan through the XML file generating the mappings for the users being referenced
            using (XmlReader usersXml = this.ImportContext.LoadIndex("users"))
            {
                Log.Info(string.Format("Loaded {0} LDAP users, resolving the users in XML...", usersByGUID.Count));
                while (usersXml.ReadToFollowing("user"))
                {
                    using (XmlReader userXml = usersXml.ReadSubtree())
                    {
                        XElement user = XElement.Load(userXml);
                        XNamespace ns = user.GetDefaultNamespace();
                        string name = (string)user.Element(ns + "name");
                        string source = (string)user.Element(ns + "source");
                        string loginName = (string)user.Element(ns + "loginName");
                        string osName = (string)user.Element(ns + "osName");

                        IEnumerable<XElement> attributes = user.Element(ns + "attributes").Elements(ns + "attribute");
                        ImportedPrincipalInfo info = null;
                        switch (source)
                        {
                            case "LDAP":
                                string ldapGuid = ((string)attributes.FirstOrDefault(a => a.Attribute("name").Value == "caliente:user_global_unique_id")).ToLower().Trim();
                                // ldapGuid will be of the form DIRECTORY:hexGuid, so we have to parse the directory name.  If it's the same directory
                                // as our domain uses, then we can search by guid directly.  Otherwise, we have to search by samaccountname
                                string[] data = ldapGuid.Split(':');
                                if (data[0] == ldapSyncAlias && usersByGUID.ContainsKey(data[1]))
                                {
                                    info = usersByGUID[data[1]];
                                }

                                if (info == null)
                                {
                                    // Either not the same domain, or couldn't find the GUID in question, so...
                                    if (usersByLogin.ContainsKey(loginName))
                                    {
                                        info = usersByLogin[loginName];
                                    }
                                    else
                                    if (usersByLogin.ContainsKey(osName))
                                    {
                                        info = usersByLogin[osName];
                                    }
                                    else
                                    if (fallbackName != null)
                                    {
                                        info = usersByLogin[fallbackName];
                                    }
                                }
                                break;
                            case "dm_krb":
                                // We do things differently here...
                                if (usersByKerberosId.ContainsKey(osName))
                                {
                                    info = usersByKerberosId[osName];
                                }
                                else
                                if (usersByLogin.ContainsKey(loginName))
                                {
                                    info = usersByLogin[loginName];
                                }
                                else
                                if (fallbackName != null)
                                {
                                    info = usersByLogin[fallbackName];
                                }
                                break;
                            default:
                                if (name.StartsWith("dm_"))
                                {
                                    if (internalName != null)
                                    {
                                        info = usersByLogin[internalName];
                                    }
                                }
                                else
                                {
                                    if (usersByLogin.ContainsKey(loginName))
                                    {
                                        info = usersByLogin[loginName];
                                    }
                                    else
                                    if (usersByLogin.ContainsKey(osName))
                                    {
                                        info = usersByLogin[osName];
                                    }
                                    else
                                    {
                                        string key = (name.StartsWith("${") ? internalName : fallbackName);
                                        if (key != null)
                                        {
                                            info = usersByLogin[key];
                                        }
                                    }
                                }
                                break;
                        }

                        if (info != null)
                        {
                            users[name] = info;
                            if (name.StartsWith("${"))
                            {
                                users[loginName] = info;
                            }
                        }
                    }
                }
                // Make sure the fallbacks are in place
                if (fallbackName != null && !users.ContainsKey(fallbackName)) users[fallbackName] = usersByLogin[fallbackName];
                if (internalName != null && !users.ContainsKey(internalName)) users[internalName] = usersByLogin[internalName];

                // Add the DM_WORLD mapping
                users[DM_WORLD] = new ImportedPrincipalInfo(EVERYONE_NAME, null, EVERYONE_ID);
            }
            return users;
        }

        private Dictionary<string, ImportedPrincipalInfo> generateGroupMappings(DirectoryEntry ldapDirectory, string ldapSyncAlias, string fallbackName, string internalName)
        {
            Dictionary<string, ImportedPrincipalInfo> groups = new Dictionary<string, ImportedPrincipalInfo>();
            string domain = ((string)ldapDirectory.Properties["name"][0]).ToUpper();
            Log.Info(string.Format("Loading LDAP Groups from {0}", ldapDirectory.Path));
            Dictionary<string, ImportedPrincipalInfo> groupsByGUID = new Dictionary<string, ImportedPrincipalInfo>();
            Dictionary<string, ImportedPrincipalInfo> groupsByLogin = new Dictionary<string, ImportedPrincipalInfo>();

            XmlReader cache = LoadCache(ldapDirectory, "groups");
            bool cacheLoaded = false;
            if (cache != null)
            {
                try
                {
                    using (cache)
                    {
                        int c = 0;
                        Log.Info("Loading groups from the cache...");
                        while (cache.ReadToFollowing("entry"))
                        {
                            ImportedPrincipalInfo info = new ImportedPrincipalInfo(XElement.Load(cache.ReadSubtree()));
                            groupsByGUID[info.Guid] = info;
                            groupsByLogin[info.LoginName] = info;
                            if ((++c % PAGE_SIZE) == 0) Log.Info(string.Format("Loaded {0} users from the cache", c));
                        }
                        cacheLoaded = true;
                    }
                }
                catch (Exception e)
                {
                    groupsByGUID.Clear();
                    groupsByLogin.Clear();
                    Log.Warn(string.Format("Failed to load data from the group cache for LDAP directory [{0}] (for {1})", ldapDirectory.Path, domain), e);
                }
                finally
                {
                    cache = null;
                }
            }

            if (!cacheLoaded)
            {
                using (DirectorySearcher ldapSearch = new DirectorySearcher(ldapDirectory))
                {
                    ldapSearch.Filter = "(objectClass=group)";
                    ldapSearch.PageSize = PAGE_SIZE;
                    using (SearchResultCollection ldapResults = ldapSearch.FindAll())
                    {
                        int c = 0;
                        foreach (SearchResult r in ldapResults)
                        {
                            ImportedPrincipalInfo info = new ImportedPrincipalInfo(r, domain);
                            groupsByGUID[info.Guid] = info;
                            groupsByLogin[info.LoginName] = info;
                            if ((++c % ldapSearch.PageSize) == 0) Log.Info(string.Format("Loaded {0} groups", c));
                        }
                    }
                }

                List<string> logins = new List<string>(groupsByLogin.Keys);
                List<ImportedPrincipalInfo> items = new List<ImportedPrincipalInfo>(groupsByLogin.Count);
                logins.Sort();
                foreach (string l in logins)
                {
                    items.Add(groupsByLogin[l]);
                }
                try
                {
                    StoreCache(ldapDirectory, "groups", items);
                }
                catch (Exception e)
                {
                    Log.Warn(string.Format("Failed to write the group cache for the LDAP directory at [{0}] (domain {1})", ldapDirectory.Path, ldapDirectory.Name), e);
                }
            }

            // Step 2: sanitize the fallback groups.  Their non-existance is an error
            if ((fallbackName != null) && !groupsByLogin.ContainsKey(fallbackName)) fallbackName = null;
            if ((internalName != null) && !groupsByLogin.ContainsKey(internalName)) internalName = null;

            // Step 3: Scan through the XML file generating the mappings for the groups being referenced
            using (XmlReader groupsXml = this.ImportContext.LoadIndex("groups"))
            {
                Log.Info(string.Format("Loaded {0} LDAP groups, resolving the groups in XML...", groupsByGUID.Count));
                while (groupsXml.ReadToFollowing("group"))
                {
                    using (XmlReader groupxml = groupsXml.ReadSubtree())
                    {
                        XElement group = XElement.Load(groupxml);
                        XNamespace ns = group.GetDefaultNamespace();
                        string name = (string)group.Element(ns + "name");
                        string source = (string)group.Element(ns + "source");
                        string type = (string)group.Element(ns + "type");

                        IEnumerable<XElement> attributes = group.Element(ns + "attributes").Elements(ns + "attribute");
                        ImportedPrincipalInfo info = null;
                        switch (source)
                        {
                            case "LDAP":
                                string ldapGuid = ((string)attributes.FirstOrDefault(a => a.Attribute("name").Value == "caliente:group_global_unique_id")).ToLower().Trim();
                                // ldapGuid will be of the form DIRECTORY:hexGuid, so we have to parse the directory name.  If it's the same directory
                                // as our domain uses, then we can search by guid directly.  Otherwise, we have to search by samaccountname
                                string[] data = ldapGuid.Split(':');
                                if (data[0] == ldapSyncAlias && groupsByGUID.ContainsKey(data[1]))
                                {
                                    info = groupsByGUID[data[1]];
                                }

                                if (info == null)
                                {
                                    // Either not the same domain, or couldn't find the GUID in question, so...
                                    if (groupsByLogin.ContainsKey(name))
                                    {
                                        info = groupsByLogin[name];
                                    }
                                    else
                                    if (fallbackName != null)
                                    {
                                        info = groupsByLogin[fallbackName];
                                    }
                                }
                                break;
                            default:
                                string key = (name.StartsWith("dm_") || name.StartsWith("${") ? internalName : fallbackName);
                                if (key != null)
                                {
                                    info = groupsByLogin[key];
                                }
                                break;
                        }

                        if (info != null)
                        {
                            groups[name] = info;
                        }
                    }
                }
                // Make sure the fallbacks are in place
                if (fallbackName != null && !groups.ContainsKey(fallbackName)) groups[fallbackName] = groupsByLogin[fallbackName];
                if (internalName != null && !groups.ContainsKey(internalName)) groups[internalName] = groupsByLogin[internalName];
            }
            return groups;
        }

        private readonly Dictionary<string, ImportedPrincipalInfo> Users;
        private readonly Dictionary<string, ImportedPrincipalInfo> Groups;

        public UserGroupImporter(ImportContext importContext, DirectoryEntry ldapDirectory, string ldapSyncName, string fallbackUser, string internalUser, string fallbackGroup, string internalGroup) : base("users & groups", importContext)
        {
            if (ldapDirectory == null)
            {
                this.Users = new Dictionary<string, ImportedPrincipalInfo>();
                this.Groups = new Dictionary<string, ImportedPrincipalInfo>();
            } else {
                ldapSyncName = ldapSyncName.ToLower();
                if (fallbackUser != null) fallbackUser = fallbackUser.ToLower();
                if (fallbackGroup != null) fallbackGroup = fallbackGroup.ToLower();
                if (internalUser != null) internalUser = internalUser.ToLower();
                if (internalGroup != null) internalGroup = internalGroup.ToLower();
                this.Users = generateUserMappings(ldapDirectory, ldapSyncName, fallbackUser, internalUser);
                this.Groups = generateGroupMappings(ldapDirectory, ldapSyncName, fallbackGroup, internalGroup);
            }
        }
        private ImportedPrincipalInfo ResolveInfo(string name, Dictionary<string, ImportedPrincipalInfo> map)
        {
            return (map.ContainsKey(name) ? map[name] : null);
        }
        public Principal ResolveUser(ClientContext ctx, string name)
        {
            // dm_group is ignored because there is no analog in SharePoint
            if (name == DM_GROUP)
            {
                return null;
            }
            ImportedPrincipalInfo info = ResolveInfo(name, this.Users);
            if (info == null) return null;
            return info.Resolve(ctx);
        }
        public string ResolveUserName(string name)
        {
            // The "special" names get...well..."specialed handling"
            switch (name)
            {
                case DM_OWNER:
                case DM_GROUP:
                case DM_WORLD:
                    return name;
                default:
                    break;
            }
            ImportedPrincipalInfo info = ResolveInfo(name, this.Users);
            if (info == null) return null;
            return info.Name;
        }
        public Principal ResolveGroup(ClientContext ctx, string name)
        {
            ImportedPrincipalInfo info = ResolveInfo(name, this.Groups);
            if (info == null) return null;
            return info.Resolve(ctx);
        }
        public string ResolveGroupName(string name)
        {
            ImportedPrincipalInfo info = ResolveInfo(name, this.Groups);
            if (info == null) return null;
            return info.LoginName;
        }
    }
}
