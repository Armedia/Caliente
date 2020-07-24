using System;
using System.CodeDom.Compiler;
using System.Diagnostics;
using System.Xml.Serialization;

namespace Armedia.CMSMF.SharePoint.Common.Xml
{
    public sealed class Constants
    {
        public const string Namespace = "http://www.armedia.com/ns/cmf/engine/xml";
        public const string XsdVersion = "2.0.50727.3038";
    }

    [GeneratedCodeAttribute("xsd", Constants.XsdVersion)]
    [SerializableAttribute()]
    [DebuggerStepThroughAttribute()]
    [XmlTypeAttribute(TypeName = "types.t", Namespace = Constants.Namespace)]
    [XmlRootAttribute("types", Namespace = Constants.Namespace, IsNullable = false)]
    public class ContentTypesT
    {

        private ContentTypeT[] typeField;

        [XmlElementAttribute("type")]
        public ContentTypeT[] type
        {
            get
            {
                return this.typeField;
            }
            set
            {
                this.typeField = value;
            }
        }
    }

    [GeneratedCodeAttribute("xsd", Constants.XsdVersion)]
    [SerializableAttribute()]
    [DebuggerStepThroughAttribute()]
    [XmlTypeAttribute(TypeName = "type.t", Namespace = Constants.Namespace)]
    public class ContentTypeT
    {

        private string nameField;

        private string superTypeField;

        private AttributeDefT[] attributesField;

        private PropertyT[] propertiesField;

        public string name
        {
            get
            {
                return this.nameField;
            }
            set
            {
                this.nameField = value;
            }
        }

        public string superType
        {
            get
            {
                return this.superTypeField;
            }
            set
            {
                this.superTypeField = value;
            }
        }

        [XmlArrayItemAttribute("attribute", IsNullable = false)]
        public AttributeDefT[] attributes
        {
            get
            {
                return this.attributesField;
            }
            set
            {
                this.attributesField = value;
            }
        }

        [XmlArrayItemAttribute("property", IsNullable = false)]
        public PropertyT[] properties
        {
            get
            {
                return this.propertiesField;
            }
            set
            {
                this.propertiesField = value;
            }
        }
    }

    [GeneratedCodeAttribute("xsd", Constants.XsdVersion)]
    [SerializableAttribute()]
    [DebuggerStepThroughAttribute()]
    [XmlTypeAttribute(TypeName = "attributeDef.t", Namespace = Constants.Namespace)]
    public class AttributeDefT : AttributeBaseT
    {

        private int lengthField;

        private bool lengthFieldSpecified;

        private bool repeatingField;

        private bool repeatingFieldSpecified;

        private bool inheritedField;

        private string sourceNameField;

        [XmlAttributeAttribute()]
        public int length
        {
            get
            {
                return this.lengthField;
            }
            set
            {
                this.lengthField = value;
            }
        }

        [XmlIgnoreAttribute()]
        public bool lengthSpecified
        {
            get
            {
                return this.lengthFieldSpecified;
            }
            set
            {
                this.lengthFieldSpecified = value;
            }
        }

        [XmlAttributeAttribute()]
        public bool repeating
        {
            get
            {
                return this.repeatingField;
            }
            set
            {
                this.repeatingField = value;
            }
        }

        [XmlIgnoreAttribute()]
        public bool repeatingSpecified
        {
            get
            {
                return this.repeatingFieldSpecified;
            }
            set
            {
                this.repeatingFieldSpecified = value;
            }
        }

        [XmlAttributeAttribute()]
        public bool inherited
        {
            get
            {
                return this.inheritedField;
            }
            set
            {
                this.inheritedField = value;
            }
        }

        [XmlAttributeAttribute()]
        public string sourceName
        {
            get
            {
                return this.sourceNameField;
            }
            set
            {
                this.sourceNameField = value;
            }
        }
    }

    [XmlIncludeAttribute(typeof(PropertyT))]
    [XmlIncludeAttribute(typeof(AttributeT))]
    [XmlIncludeAttribute(typeof(AttributeDefT))]
    [GeneratedCodeAttribute("xsd", Constants.XsdVersion)]
    [SerializableAttribute()]
    [DebuggerStepThroughAttribute()]
    [XmlTypeAttribute(TypeName = "attributeBase.t", Namespace = Constants.Namespace)]
    public class AttributeBaseT
    {

        private string nameField;

        private DataTypeT dataTypeField;

        [XmlAttributeAttribute()]
        public string name
        {
            get
            {
                return this.nameField;
            }
            set
            {
                this.nameField = value;
            }
        }

        [XmlAttributeAttribute()]
        public DataTypeT dataType
        {
            get
            {
                return this.dataTypeField;
            }
            set
            {
                this.dataTypeField = value;
            }
        }
    }

    [GeneratedCodeAttribute("xsd", Constants.XsdVersion)]
    [SerializableAttribute()]
    [XmlTypeAttribute(TypeName = "dataType.t", Namespace = Constants.Namespace)]
    public enum DataTypeT
    {
        BOOLEAN,
        INTEGER,
        DOUBLE,
        STRING,
        ID,
        DATETIME,
        URI,
        HTML,
        OTHER,
    }

    [XmlIncludeAttribute(typeof(DocumentIndexEntryT))]
    [GeneratedCodeAttribute("xsd", Constants.XsdVersion)]
    [SerializableAttribute()]
    [DebuggerStepThroughAttribute()]
    [XmlTypeAttribute(TypeName = "folderIndexEntry.t", Namespace = Constants.Namespace)]
    public class FolderIndexEntryT
    {

        private string idField;

        private string pathField;

        private string nameField;

        private string locationField;

        private string typeField;

        public string id
        {
            get
            {
                return this.idField;
            }
            set
            {
                this.idField = value;
            }
        }

        public string path
        {
            get
            {
                return this.pathField;
            }
            set
            {
                this.pathField = value;
            }
        }

        public string name
        {
            get
            {
                return this.nameField;
            }
            set
            {
                this.nameField = value;
            }
        }

        public string location
        {
            get
            {
                return this.locationField;
            }
            set
            {
                this.locationField = value;
            }
        }

        public string type
        {
            get
            {
                return this.typeField;
            }
            set
            {
                this.typeField = value;
            }
        }
    }

    [GeneratedCodeAttribute("xsd", Constants.XsdVersion)]
    [SerializableAttribute()]
    [DebuggerStepThroughAttribute()]
    [XmlTypeAttribute(TypeName = "documentIndexEntry.t", Namespace = Constants.Namespace)]
    public class DocumentIndexEntryT : FolderIndexEntryT
    {

        private string historyIdField;

        private string versionField;

        private bool currentField;

        private long sizeField;

        public string historyId
        {
            get
            {
                return this.historyIdField;
            }
            set
            {
                this.historyIdField = value;
            }
        }

        public string version
        {
            get
            {
                return this.versionField;
            }
            set
            {
                this.versionField = value;
            }
        }

        public bool current
        {
            get
            {
                return this.currentField;
            }
            set
            {
                this.currentField = value;
            }
        }

        public long size
        {
            get
            {
                return this.sizeField;
            }
            set
            {
                this.sizeField = value;
            }
        }
    }

    [GeneratedCodeAttribute("xsd", Constants.XsdVersion)]
    [SerializableAttribute()]
    [DebuggerStepThroughAttribute()]
    [XmlTypeAttribute(TypeName = "contentStreamProperty.t", Namespace = Constants.Namespace)]
    public class ContentStreamPropertyT
    {

        private string nameField;

        private string valueField;

        [XmlAttributeAttribute()]
        public string name
        {
            get
            {
                return this.nameField;
            }
            set
            {
                this.nameField = value;
            }
        }

        [XmlAttributeAttribute()]
        public string value
        {
            get
            {
                return this.valueField;
            }
            set
            {
                this.valueField = value;
            }
        }
    }

    [GeneratedCodeAttribute("xsd", Constants.XsdVersion)]
    [SerializableAttribute()]
    [DebuggerStepThroughAttribute()]
    [XmlTypeAttribute(TypeName = "contentStream.t", Namespace = Constants.Namespace)]
    public class ContentStreamT
    {

        private string qualifierField;

        private long sizeField;

        private byte[] hashField;

        private string locationField;

        private string fileNameField;

        private string mimeTypeField;

        private ContentStreamPropertyT[] propertiesField;

        public string qualifier
        {
            get
            {
                return this.qualifierField;
            }
            set
            {
                this.qualifierField = value;
            }
        }

        public long size
        {
            get
            {
                return this.sizeField;
            }
            set
            {
                this.sizeField = value;
            }
        }

        [XmlElementAttribute(DataType = "base64Binary")]
        public byte[] hash
        {
            get
            {
                return this.hashField;
            }
            set
            {
                this.hashField = value;
            }
        }

        public string location
        {
            get
            {
                return this.locationField;
            }
            set
            {
                this.locationField = value;
            }
        }

        public string fileName
        {
            get
            {
                return this.fileNameField;
            }
            set
            {
                this.fileNameField = value;
            }
        }

        public string mimeType
        {
            get
            {
                return this.mimeTypeField;
            }
            set
            {
                this.mimeTypeField = value;
            }
        }

        [XmlArrayItemAttribute("property", IsNullable = false)]
        public ContentStreamPropertyT[] properties
        {
            get
            {
                return this.propertiesField;
            }
            set
            {
                this.propertiesField = value;
            }
        }
    }

    [XmlIncludeAttribute(typeof(DocumentVersionT))]
    [XmlIncludeAttribute(typeof(FolderT))]
    [GeneratedCodeAttribute("xsd", Constants.XsdVersion)]
    [SerializableAttribute()]
    [DebuggerStepThroughAttribute()]
    [System.ComponentModel.DesignerCategoryAttribute("code")]
    [XmlTypeAttribute(TypeName = "sysObject.t", Namespace = Constants.Namespace)]
    public class SysObjectT
    {

        private string idField;

        private string parentIdField;

        private string nameField;

        private string typeField;

        private string sourcePathField;

        private System.DateTime creationDateField;

        private string creatorField;

        private System.DateTime modificationDateField;

        private string modifierField;

        private string aclField;

        private AttributeT[] attributesField;

        private PropertyT[] propertiesField;

        public string id
        {
            get
            {
                return this.idField;
            }
            set
            {
                this.idField = value;
            }
        }

        public string parentId
        {
            get
            {
                return this.parentIdField;
            }
            set
            {
                this.parentIdField = value;
            }
        }

        public string name
        {
            get
            {
                return this.nameField;
            }
            set
            {
                this.nameField = value;
            }
        }

        public string type
        {
            get
            {
                return this.typeField;
            }
            set
            {
                this.typeField = value;
            }
        }

        public string sourcePath
        {
            get
            {
                return this.sourcePathField;
            }
            set
            {
                this.sourcePathField = value;
            }
        }

        public System.DateTime creationDate
        {
            get
            {
                return this.creationDateField;
            }
            set
            {
                this.creationDateField = value;
            }
        }

        public string creator
        {
            get
            {
                return this.creatorField;
            }
            set
            {
                this.creatorField = value;
            }
        }

        public System.DateTime modificationDate
        {
            get
            {
                return this.modificationDateField;
            }
            set
            {
                this.modificationDateField = value;
            }
        }

        public string modifier
        {
            get
            {
                return this.modifierField;
            }
            set
            {
                this.modifierField = value;
            }
        }

        public string acl
        {
            get
            {
                return this.aclField;
            }
            set
            {
                this.aclField = value;
            }
        }

        [XmlArrayItemAttribute("attribute", IsNullable = false)]
        public AttributeT[] attributes
        {
            get
            {
                return this.attributesField;
            }
            set
            {
                this.attributesField = value;
            }
        }

        [XmlArrayItemAttribute("property", IsNullable = false)]
        public PropertyT[] properties
        {
            get
            {
                return this.propertiesField;
            }
            set
            {
                this.propertiesField = value;
            }
        }
    }

    [GeneratedCodeAttribute("xsd", Constants.XsdVersion)]
    [SerializableAttribute()]
    [DebuggerStepThroughAttribute()]
    [XmlTypeAttribute(TypeName = "attribute.t", Namespace = Constants.Namespace)]
    public class AttributeT : AttributeBaseT
    {

        private string[] valueField;

        [XmlElementAttribute("value")]
        public string[] value
        {
            get
            {
                return this.valueField;
            }
            set
            {
                this.valueField = value;
            }
        }
    }

    [GeneratedCodeAttribute("xsd", Constants.XsdVersion)]
    [SerializableAttribute()]
    [DebuggerStepThroughAttribute()]
    [XmlTypeAttribute(TypeName = "property.t", Namespace = Constants.Namespace)]
    public class PropertyT : AttributeBaseT
    {

        private string[] valueField;

        private bool repeatingField;

        private bool repeatingFieldSpecified;

        [XmlElementAttribute("value")]
        public string[] value
        {
            get
            {
                return this.valueField;
            }
            set
            {
                this.valueField = value;
            }
        }

        [XmlAttributeAttribute()]
        public bool repeating
        {
            get
            {
                return this.repeatingField;
            }
            set
            {
                this.repeatingField = value;
            }
        }

        [XmlIgnoreAttribute()]
        public bool repeatingSpecified
        {
            get
            {
                return this.repeatingFieldSpecified;
            }
            set
            {
                this.repeatingFieldSpecified = value;
            }
        }
    }

    [GeneratedCodeAttribute("xsd", Constants.XsdVersion)]
    [SerializableAttribute()]
    [DebuggerStepThroughAttribute()]
    [XmlTypeAttribute(TypeName = "documentVersion.t", Namespace = Constants.Namespace)]
    public class DocumentVersionT : SysObjectT
    {

        private System.DateTime lastAccessDateField;

        private bool lastAccessDateFieldSpecified;

        private string lastAccessorField;

        private string historyIdField;

        private string versionField;

        private bool currentField;

        private string antecedentIdField;

        private ContentStreamT[] contentsField;

        public System.DateTime lastAccessDate
        {
            get
            {
                return this.lastAccessDateField;
            }
            set
            {
                this.lastAccessDateField = value;
            }
        }

        [XmlIgnoreAttribute()]
        public bool lastAccessDateSpecified
        {
            get
            {
                return this.lastAccessDateFieldSpecified;
            }
            set
            {
                this.lastAccessDateFieldSpecified = value;
            }
        }

        public string lastAccessor
        {
            get
            {
                return this.lastAccessorField;
            }
            set
            {
                this.lastAccessorField = value;
            }
        }

        public string historyId
        {
            get
            {
                return this.historyIdField;
            }
            set
            {
                this.historyIdField = value;
            }
        }

        public string version
        {
            get
            {
                return this.versionField;
            }
            set
            {
                this.versionField = value;
            }
        }

        public bool current
        {
            get
            {
                return this.currentField;
            }
            set
            {
                this.currentField = value;
            }
        }

        public string antecedentId
        {
            get
            {
                return this.antecedentIdField;
            }
            set
            {
                this.antecedentIdField = value;
            }
        }

        [XmlArrayItemAttribute("content", IsNullable = false)]
        public ContentStreamT[] contents
        {
            get
            {
                return this.contentsField;
            }
            set
            {
                this.contentsField = value;
            }
        }
    }

    [GeneratedCodeAttribute("xsd", Constants.XsdVersion)]
    [SerializableAttribute()]
    [DebuggerStepThroughAttribute()]
    [XmlTypeAttribute(TypeName = "format.t", Namespace = Constants.Namespace)]
    public class FormatT
    {

        private string nameField;

        private string descriptionField;

        private AttributeT[] attributesField;

        private PropertyT[] propertiesField;

        public string name
        {
            get
            {
                return this.nameField;
            }
            set
            {
                this.nameField = value;
            }
        }

        public string description
        {
            get
            {
                return this.descriptionField;
            }
            set
            {
                this.descriptionField = value;
            }
        }

        [XmlArrayItemAttribute("attribute", IsNullable = false)]
        public AttributeT[] attributes
        {
            get
            {
                return this.attributesField;
            }
            set
            {
                this.attributesField = value;
            }
        }

        [XmlArrayItemAttribute("property", IsNullable = false)]
        public PropertyT[] properties
        {
            get
            {
                return this.propertiesField;
            }
            set
            {
                this.propertiesField = value;
            }
        }
    }

    [GeneratedCodeAttribute("xsd", Constants.XsdVersion)]
    [SerializableAttribute()]
    [DebuggerStepThroughAttribute()]
    [XmlTypeAttribute(TypeName = "group.t", Namespace = Constants.Namespace)]
    public class GroupT
    {

        private string nameField;

        private string typeField;

        private string emailField;

        private string sourceField;

        private string administratorField;

        private string displayNameField;

        private string[] usersField;

        private string[] groupsField;

        private AttributeT[] attributesField;

        private PropertyT[] propertiesField;

        public string name
        {
            get
            {
                return this.nameField;
            }
            set
            {
                this.nameField = value;
            }
        }

        public string type
        {
            get
            {
                return this.typeField;
            }
            set
            {
                this.typeField = value;
            }
        }

        public string email
        {
            get
            {
                return this.emailField;
            }
            set
            {
                this.emailField = value;
            }
        }

        public string source
        {
            get
            {
                return this.sourceField;
            }
            set
            {
                this.sourceField = value;
            }
        }

        public string administrator
        {
            get
            {
                return this.administratorField;
            }
            set
            {
                this.administratorField = value;
            }
        }

        public string displayName
        {
            get
            {
                return this.displayNameField;
            }
            set
            {
                this.displayNameField = value;
            }
        }

        [XmlArrayItemAttribute("user", IsNullable = false)]
        public string[] users
        {
            get
            {
                return this.usersField;
            }
            set
            {
                this.usersField = value;
            }
        }

        [XmlArrayItemAttribute("group", IsNullable = false)]
        public string[] groups
        {
            get
            {
                return this.groupsField;
            }
            set
            {
                this.groupsField = value;
            }
        }

        [XmlArrayItemAttribute("attribute", IsNullable = false)]
        public AttributeT[] attributes
        {
            get
            {
                return this.attributesField;
            }
            set
            {
                this.attributesField = value;
            }
        }

        [XmlArrayItemAttribute("property", IsNullable = false)]
        public PropertyT[] properties
        {
            get
            {
                return this.propertiesField;
            }
            set
            {
                this.propertiesField = value;
            }
        }
    }

    [GeneratedCodeAttribute("xsd", Constants.XsdVersion)]
    [SerializableAttribute()]
    [DebuggerStepThroughAttribute()]
    [XmlTypeAttribute(TypeName = "user.t", Namespace = Constants.Namespace)]
    public class UserT
    {

        private string nameField;

        private string defaultFolderField;

        private string descriptionField;

        private string emailField;

        private string sourceField;

        private string loginNameField;

        private string loginDomainField;

        private string osNameField;

        private string osDomainField;

        private AttributeT[] attributesField;

        private PropertyT[] propertiesField;

        public string name
        {
            get
            {
                return this.nameField;
            }
            set
            {
                this.nameField = value;
            }
        }

        public string defaultFolder
        {
            get
            {
                return this.defaultFolderField;
            }
            set
            {
                this.defaultFolderField = value;
            }
        }

        public string description
        {
            get
            {
                return this.descriptionField;
            }
            set
            {
                this.descriptionField = value;
            }
        }

        public string email
        {
            get
            {
                return this.emailField;
            }
            set
            {
                this.emailField = value;
            }
        }

        public string source
        {
            get
            {
                return this.sourceField;
            }
            set
            {
                this.sourceField = value;
            }
        }

        public string loginName
        {
            get
            {
                return this.loginNameField;
            }
            set
            {
                this.loginNameField = value;
            }
        }

        public string loginDomain
        {
            get
            {
                return this.loginDomainField;
            }
            set
            {
                this.loginDomainField = value;
            }
        }

        public string osName
        {
            get
            {
                return this.osNameField;
            }
            set
            {
                this.osNameField = value;
            }
        }

        public string osDomain
        {
            get
            {
                return this.osDomainField;
            }
            set
            {
                this.osDomainField = value;
            }
        }

        [XmlArrayItemAttribute("attribute", IsNullable = false)]
        public AttributeT[] attributes
        {
            get
            {
                return this.attributesField;
            }
            set
            {
                this.attributesField = value;
            }
        }

        [XmlArrayItemAttribute("property", IsNullable = false)]
        public PropertyT[] properties
        {
            get
            {
                return this.propertiesField;
            }
            set
            {
                this.propertiesField = value;
            }
        }
    }

    [GeneratedCodeAttribute("xsd", Constants.XsdVersion)]
    [SerializableAttribute()]
    [DebuggerStepThroughAttribute()]
    [XmlTypeAttribute(TypeName = "aclPermit.t", Namespace = Constants.Namespace)]
    public class AclPermitT
    {

        private PermitTypeT typeField;

        private string nameField;

        private int levelField;

        private string extendedField;

        [XmlAttributeAttribute()]
        public PermitTypeT type
        {
            get
            {
                return this.typeField;
            }
            set
            {
                this.typeField = value;
            }
        }

        [XmlAttributeAttribute()]
        public string name
        {
            get
            {
                return this.nameField;
            }
            set
            {
                this.nameField = value;
            }
        }

        [XmlAttributeAttribute()]
        public int level
        {
            get
            {
                return this.levelField;
            }
            set
            {
                this.levelField = value;
            }
        }

        [XmlAttributeAttribute()]
        public string extended
        {
            get
            {
                return this.extendedField;
            }
            set
            {
                this.extendedField = value;
            }
        }
    }

    [GeneratedCodeAttribute("xsd", Constants.XsdVersion)]
    [SerializableAttribute()]
    [XmlTypeAttribute(TypeName = "permitType.t", Namespace = Constants.Namespace)]
    public enum PermitTypeT
    {

        ACCESS,

        EXTENDED,

        APPLICATION,

        ACCESS_RESTRICTION,

        EXTENDED_RESTRICTION,

        APPLICATION_RESTRICTION,

        REQUIRED_GROUP,

        REQUIRED_GROUP_SET,
    }

    [GeneratedCodeAttribute("xsd", Constants.XsdVersion)]
    [SerializableAttribute()]
    [DebuggerStepThroughAttribute()]
    [XmlTypeAttribute(TypeName = "acl.t", Namespace = Constants.Namespace)]
    public class AclT
    {

        private string idField;

        private string descriptionField;

        private AclPermitT[] usersField;

        private AclPermitT[] groupsField;

        private AttributeT[] attributesField;

        private PropertyT[] propertiesField;

        public string id
        {
            get
            {
                return this.idField;
            }
            set
            {
                this.idField = value;
            }
        }

        public string description
        {
            get
            {
                return this.descriptionField;
            }
            set
            {
                this.descriptionField = value;
            }
        }

        [XmlArrayItemAttribute("permit", IsNullable = false)]
        public AclPermitT[] users
        {
            get
            {
                return this.usersField;
            }
            set
            {
                this.usersField = value;
            }
        }

        [XmlArrayItemAttribute("permit", IsNullable = false)]
        public AclPermitT[] groups
        {
            get
            {
                return this.groupsField;
            }
            set
            {
                this.groupsField = value;
            }
        }

        [XmlArrayItemAttribute("attribute", IsNullable = false)]
        public AttributeT[] attributes
        {
            get
            {
                return this.attributesField;
            }
            set
            {
                this.attributesField = value;
            }
        }

        [XmlArrayItemAttribute("property", IsNullable = false)]
        public PropertyT[] properties
        {
            get
            {
                return this.propertiesField;
            }
            set
            {
                this.propertiesField = value;
            }
        }
    }

    [GeneratedCodeAttribute("xsd", Constants.XsdVersion)]
    [SerializableAttribute()]
    [DebuggerStepThroughAttribute()]
    [XmlTypeAttribute(TypeName = "acls.t", Namespace = Constants.Namespace)]
    [XmlRootAttribute("acls", Namespace = Constants.Namespace, IsNullable = false)]
    public class AclsT
    {

        private AclT[] aclField;

        [XmlElementAttribute("acl")]
        public AclT[] acl
        {
            get
            {
                return this.aclField;
            }
            set
            {
                this.aclField = value;
            }
        }
    }

    [GeneratedCodeAttribute("xsd", Constants.XsdVersion)]
    [SerializableAttribute()]
    [DebuggerStepThroughAttribute()]
    [XmlTypeAttribute(TypeName = "users.t", Namespace = Constants.Namespace)]
    [XmlRootAttribute("users", Namespace = Constants.Namespace, IsNullable = false)]
    public class UsersT
    {

        private UserT[] userField;

        [XmlElementAttribute("user")]
        public UserT[] user
        {
            get
            {
                return this.userField;
            }
            set
            {
                this.userField = value;
            }
        }
    }

    [GeneratedCodeAttribute("xsd", Constants.XsdVersion)]
    [SerializableAttribute()]
    [DebuggerStepThroughAttribute()]
    [XmlTypeAttribute(TypeName = "groups.t", Namespace = Constants.Namespace)]
    [XmlRootAttribute("groups", Namespace = Constants.Namespace, IsNullable = false)]
    public class GroupsT
    {

        private GroupT[] groupField;

        [XmlElementAttribute("group")]
        public GroupT[] group
        {
            get
            {
                return this.groupField;
            }
            set
            {
                this.groupField = value;
            }
        }
    }

    [GeneratedCodeAttribute("xsd", Constants.XsdVersion)]
    [SerializableAttribute()]
    [DebuggerStepThroughAttribute()]
    [XmlTypeAttribute(TypeName = "formats.t", Namespace = Constants.Namespace)]
    [XmlRootAttribute("formats", Namespace = Constants.Namespace, IsNullable = false)]
    public class FormatsT
    {

        private FormatT[] formatField;

        [XmlElementAttribute("format")]
        public FormatT[] format
        {
            get
            {
                return this.formatField;
            }
            set
            {
                this.formatField = value;
            }
        }
    }

    [GeneratedCodeAttribute("xsd", Constants.XsdVersion)]
    [SerializableAttribute()]
    [DebuggerStepThroughAttribute()]
    [XmlTypeAttribute(TypeName = "folder.t", Namespace = Constants.Namespace)]
    [XmlRootAttribute("folder", Namespace = Constants.Namespace, IsNullable = false)]
    public class FolderT : SysObjectT
    {
    }

    [GeneratedCodeAttribute("xsd", Constants.XsdVersion)]
    [SerializableAttribute()]
    [DebuggerStepThroughAttribute()]
    [XmlTypeAttribute(TypeName = "folders.t", Namespace = Constants.Namespace)]
    [XmlRootAttribute("folders", Namespace = Constants.Namespace, IsNullable = false)]
    public class folderst
    {

        private FolderT[] folderField;

        [XmlElementAttribute("folder")]
        public FolderT[] folder
        {
            get
            {
                return this.folderField;
            }
            set
            {
                this.folderField = value;
            }
        }
    }

    [GeneratedCodeAttribute("xsd", Constants.XsdVersion)]
    [SerializableAttribute()]
    [DebuggerStepThroughAttribute()]
    [XmlTypeAttribute(TypeName = "folderIndex.t", Namespace = Constants.Namespace)]
    [XmlRootAttribute("folderIndex", Namespace = Constants.Namespace, IsNullable = false)]
    public class FolderIndexT
    {

        private FolderIndexEntryT[] folderField;

        [XmlElementAttribute("folder")]
        public FolderIndexEntryT[] folder
        {
            get
            {
                return this.folderField;
            }
            set
            {
                this.folderField = value;
            }
        }
    }

    [GeneratedCodeAttribute("xsd", Constants.XsdVersion)]
    [SerializableAttribute()]
    [DebuggerStepThroughAttribute()]
    [XmlTypeAttribute(TypeName = "document.t", Namespace = Constants.Namespace)]
    [XmlRootAttribute("document", Namespace = Constants.Namespace, IsNullable = false)]
    public class DocumentT
    {

        private DocumentVersionT[] versionField;

        [XmlElementAttribute("version")]
        public DocumentVersionT[] version
        {
            get
            {
                return this.versionField;
            }
            set
            {
                this.versionField = value;
            }
        }
    }

    [GeneratedCodeAttribute("xsd", Constants.XsdVersion)]
    [SerializableAttribute()]
    [DebuggerStepThroughAttribute()]
    [XmlTypeAttribute(TypeName = "documents.t", Namespace = Constants.Namespace)]
    [XmlRootAttribute("documents", Namespace = Constants.Namespace, IsNullable = false)]
    public class DocumentsT
    {

        private DocumentVersionT[][] documentField;

        [XmlArrayItemAttribute("version", typeof(DocumentVersionT), IsNullable = false)]
        public DocumentVersionT[][] document
        {
            get
            {
                return this.documentField;
            }
            set
            {
                this.documentField = value;
            }
        }
    }

    [GeneratedCodeAttribute("xsd", Constants.XsdVersion)]
    [SerializableAttribute()]
    [DebuggerStepThroughAttribute()]
    [XmlTypeAttribute(TypeName = "documentIndex.t", Namespace = Constants.Namespace)]
    [XmlRootAttribute("documentIndex", Namespace = Constants.Namespace, IsNullable = false)]
    public class DocumentIndexT
    {

        private DocumentIndexEntryT[] documentField;

        [XmlElementAttribute("document")]
        public DocumentIndexEntryT[] document
        {
            get
            {
                return this.documentField;
            }
            set
            {
                this.documentField = value;
            }
        }
    }
}
