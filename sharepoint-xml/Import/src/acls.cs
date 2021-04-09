using Microsoft.SharePoint.Client;
using System;
using System.Collections.Generic;
using System.Xml;
using System.Xml.Linq;

namespace Armedia.CMSMF.SharePoint.Import
{

    public class AccessControlList
    {
        private static Dictionary<string, RoleType> BuildRoleDefinitionsDirectory()
        {
            Dictionary<string, RoleType> roles = new Dictionary<string, RoleType>();
            RoleType roleDef;
            roleDef = RoleType.None;
            roles["1"] = roleDef;
            roleDef = RoleType.Reader;
            roles["2"] = roleDef;
            roles["3"] = roleDef;
            roleDef = RoleType.Editor;
            roles["4"] = roleDef;
            roles["5"] = roleDef;
            roles["6"] = roleDef;
            roleDef = RoleType.Administrator;
            roles["7"] = roleDef;
            return roles;
        }
        private static readonly Dictionary<string, RoleType> ROLE_DEFS = BuildRoleDefinitionsDirectory();
        private static RoleType ResolveRoleDefinition(string level)
        {
            if (ROLE_DEFS.ContainsKey(level))
            {
                return ROLE_DEFS[level];
            }
            // Default to NONE if the level requested isn't supported
            return ROLE_DEFS["1"];
        }


        private abstract class Permission
        {
            public readonly string Extended;
            public readonly string Level;
            public readonly string Name;
            public readonly string PermitType;
            public readonly RoleType RoleType;

            public Permission(string name, string type, string level, string extended)
            {
                this.Name = name;
                this.Level = level;
                this.Extended = extended;
                this.PermitType = type;
                this.RoleType = ResolveRoleDefinition(level);
            }

            protected abstract Principal ResolvePrincipal(ClientContext clientContext, UserGroupImporter userGroupImporter);

            public void Grant(ListItem item, UserGroupImporter userGroupImporter)
            {
                if (this.Name == UserGroupImporter.DM_OWNER) return;
                if (this.RoleType == RoleType.None) return;
                ClientContext clientContext = item.Context as ClientContext;
                Principal principal = ResolvePrincipal(clientContext, userGroupImporter);
                if (principal == null) return;
                RoleDefinitionBindingCollection roles = new RoleDefinitionBindingCollection(clientContext);
                roles.Add(clientContext.Web.RoleDefinitions.GetByType(this.RoleType));
                item.RoleAssignments.Add(principal, roles);
            }
        }

        private class UserPermission : Permission
        {
            public UserPermission(string name, string type, string level, string extended) : base(name, type, level, extended)
            {
            }

            protected override Principal ResolvePrincipal(ClientContext clientContext, UserGroupImporter userGroupImporter)
            {
                return userGroupImporter.ResolveUser(clientContext, this.Name);
            }
        }

        private class GroupPermission : Permission
        {
            public GroupPermission(string name, string type, string level, string extended) : base(name, type, level, extended)
            {
            }

            protected override Principal ResolvePrincipal(ClientContext clientContext, UserGroupImporter userGroupImporter)
            {
                return userGroupImporter.ResolveGroup(clientContext, this.Name);
            }
        }

        private readonly UserGroupImporter UserGroupImporter;
        public readonly string Id;
        public readonly string Description;
        private readonly Dictionary<string, UserPermission> Users;
        private readonly Dictionary<string, GroupPermission> Groups;

        public AccessControlList(XElement aclXml, UserGroupImporter userGroupImporter)
        {
            this.UserGroupImporter = userGroupImporter;
            XNamespace ns = aclXml.GetDefaultNamespace();
            this.Id = (string)aclXml.Element(ns + "id");
            this.Description = (string)aclXml.Element(ns + "description");
            this.Users = new Dictionary<string, UserPermission>();
            XElement users = aclXml.Element(ns + "users");
            if (users != null)
            {
                foreach (XElement p in users.Elements(ns + "permit"))
                {
                    string name = p.Attribute("name").Value;
                    string resolved = (name == UserGroupImporter.DM_OWNER ? name : this.UserGroupImporter.ResolveUserName(name));
                    if ((resolved != null) && !this.Users.ContainsKey(resolved))
                    {
                        this.Users[resolved] = new UserPermission(resolved, p.Attribute("type").Value, p.Attribute("level").Value, p.Attribute("extended").Value);
                    }
                }
            }
            this.Groups = new Dictionary<string, GroupPermission>();
            XElement groups = aclXml.Element(ns + "groups");
            if (groups != null)
            {
                foreach (XElement p in groups.Elements(ns + "permit"))
                {
                    string name = p.Attribute("name").Value;
                    string resolved = this.UserGroupImporter.ResolveGroupName(name);
                    if ((resolved != null) && !this.Groups.ContainsKey(resolved))
                    {
                        this.Groups[resolved] = new GroupPermission(resolved, p.Attribute("type").Value, p.Attribute("level").Value, p.Attribute("extended").Value);
                    }
                }
            }
        }

        public void Apply(ListItem item)
        {
            foreach (Permission p in this.Users.Values)
            {
                p.Grant(item, this.UserGroupImporter);
            }
            foreach (Permission p in this.Groups.Values)
            {
                p.Grant(item, this.UserGroupImporter);
            }
        }

        public void ApplyOwner(ListItem item, string owner)
        {
            if (!this.Users.ContainsKey(UserGroupImporter.DM_OWNER)) return;
            Permission p = this.Users[UserGroupImporter.DM_OWNER];
            string resolved = this.UserGroupImporter.ResolveUserName(owner);
            if (resolved == null) return;
            new UserPermission(resolved, p.PermitType, p.Level, p.Extended).Grant(item, this.UserGroupImporter);
        }
    }

    public class PermissionsImporter : BaseImporter
    {
        private readonly Dictionary<string, AccessControlList> Acls;
        public readonly UserGroupImporter UserGroupImporter;

        public PermissionsImporter(UserGroupImporter userGroupImporter) : base("permissions", userGroupImporter.ImportContext)
        {
            this.UserGroupImporter = userGroupImporter;
            Dictionary<string, AccessControlList> acls = new Dictionary<string, AccessControlList>();
            using (XmlReader aclsXml = this.ImportContext.LoadIndex("acls"))
            {
                while (aclsXml.ReadToFollowing("acl"))
                {
                    using (XmlReader aclXml = aclsXml.ReadSubtree())
                    {
                        AccessControlList a = new AccessControlList(XElement.Load(aclXml), userGroupImporter);
                        acls[a.Id] = a;
                    }
                }
            }
            this.Acls = acls;
        }

        public void ApplyPermissions(ListItem item, string id)
        {
            if (this.Acls.ContainsKey(id))
            {
                this.Acls[id].Apply(item);
            }
        }

        public void ApplyOwnerPermission(ListItem item, string id, string owner)
        {
            if (this.Acls.ContainsKey(id))
            {
                this.Acls[id].ApplyOwner(item, owner);
            }
        }
    }
}