using Armedia.CMSMF.SharePoint.Common;
using log4net;
using Microsoft.SharePoint.Client;
using System;
using System.Collections.Generic;
using System.Xml;
using System.Xml.Linq;

namespace Armedia.CMSMF.SharePoint.Import
{
    public class ImportedContentType
    {
        private readonly ContentTypeId ParentId;

        public readonly ContentTypeImporter Importer;
        public readonly ContentTypeId Id;
        public readonly string Name;
        public readonly ContentType Type;

        public ImportedContentType Parent
        {
            get
            {
                // Parents are ALWAYS site content types
                return (this.ParentId != null ? Importer.ResolveContentType(this.ParentId) : null);
            }

            private set
            {
            }
        }

        public ImportedContentType(ContentTypeImporter importer, ContentType type) : this(importer, type, type.Parent.Id)
        {
        }

        public ImportedContentType(ContentTypeImporter importer, ContentType type, ContentTypeId parentId)
        {
            this.Importer = importer;
            this.Id = type.Id;
            this.Name = type.Name;
            this.Type = type;
            this.Fields = new Dictionary<string, ImportedContentTypeField>();
            if (parentId.StringValue == this.Id.StringValue) parentId = null;
            this.ParentId = parentId;
        }

        private readonly Dictionary<string, ImportedContentTypeField> Fields;

        public void AddField(ImportedContentTypeField field)
        {
            this.Fields[field.Name] = field;
        }

        public ImportedContentTypeField GetField(string name)
        {
            if (!this.Fields.ContainsKey(name))
            {
                // Resolve via inheritance if necessary
                ImportedContentType parent = this.Parent;
                return (parent != null ? parent.GetField(name) : null);
            }
            return Fields[name];
        }
    }

    public class ImportedContentTypeField
    {
        public ImportedContentTypeField(Field field, string name, string finalName, string sourceName, bool repeating)
        {
            this.Field = field;
            this.Name = name;
            this.FinalName = finalName;
            this.SourceName = sourceName;
            this.Repeating = repeating;
        }

        public ImportedContentTypeField(Guid guid, string name, string finalName, string sourceName, FieldType type, bool repeating)
        {
            this.Field = null;
            this.Guid = guid;
            this.Name = name;
            this.FinalName = finalName;
            this.SourceName = sourceName;
            this.Type = type;
            this.Repeating = repeating;
        }
        public readonly Field Field;

        private Guid guid;
        public Guid Guid {
            get
            {
                return (this.Field != null ? this.Field.Id : this.guid);
            }

            private set
            {
                this.guid = value;
            }
        }
        public readonly string Name;
        public readonly string FinalName;
        public readonly string SourceName;

        private FieldType type;
        public FieldType Type
        {
            get
            {
                return (this.Field != null ? this.Field.FieldTypeKind : this.type);
            }
            private set
            {
                this.type = value;
            }
        }
        public readonly bool Repeating;
    }

    public class ContentTypeImporter : BaseImporter
    {
        private const string TYPE_GROUP = "Documentum Content Types";
        private const string FIELD_GROUP = "Documentum Columns";

        public static readonly ILog LOG = LogManager.GetLogger(typeof(ContentTypeImporter));
        private readonly Dictionary<string, ImportedContentType> SiteContentTypes;
        private readonly Dictionary<string, ImportedContentType> LibraryContentTypes;
        private readonly Dictionary<string, ImportedContentType> ContentTypesById;
        public readonly string TypeGroup;
        public readonly string FieldGroup;

        public ContentTypeImporter(ImportContext importContext, string documentLibraryName) : this(importContext, documentLibraryName, false)
        {
        }

        public ContentTypeImporter(ImportContext importContext, String documentLibraryName, bool clearFirst) : base("content types", importContext)
        {
            this.Log.Info(string.Format("Mapping the object types to content types and columns to the site and the library [{0}]...", documentLibraryName));

            // TODO: Make these two configurable
            this.TypeGroup = TYPE_GROUP;
            this.FieldGroup = FIELD_GROUP;

            using (ObjectPool<SharePointSession>.Ref sessionRef = this.ImportContext.SessionFactory.GetSession())
            {
                SharePointSession session = sessionRef.Target;
                ClientContext clientContext = session.ClientContext;
                List documentLibrary = clientContext.Web.Lists.GetByTitle(documentLibraryName);
                if (clearFirst)
                {
                    try
                    {
                        Log.Warn("Cleaning out document library content types...");
                        CleanContentTypes(sessionRef.Target, documentLibrary.ContentTypes);
                        Log.Warn("Cleaning out document library fields...");
                        CleanFields(sessionRef.Target, documentLibrary.Fields);
                        Log.Warn("Cleaning out site content types...");
                        CleanContentTypes(sessionRef.Target, clientContext.Web.ContentTypes);
                        Log.Warn("Cleaning out site fields...");
                        CleanFields(sessionRef.Target, clientContext.Web.Fields);
                        Log.Warn("Fields and content types cleared!");
                    }
                    catch (Exception e)
                    {
                        Log.Warn("Tried to remove the existing content types, but failed", e);
                    }
                }

                ContentTypeCollection contentTypeCollection = clientContext.Web.ContentTypes;
                clientContext.Load(contentTypeCollection, c => c.Include(t => t.Id, t => t.Name, t => t.Group, t => t.Parent, t => t.FieldLinks));
                clientContext.Load(documentLibrary.ContentTypes, c => c.Include(t => t.Id, t => t.Name, t => t.Parent));
                clientContext.Load(clientContext.Web, w => w.Fields, w => w.AvailableFields);
                session.ExecuteQuery();

                // First we gather up whatever's already there
                Dictionary<string, ImportedContentType> siteContentTypes = new Dictionary<string, ImportedContentType>();
                Dictionary<string, ImportedContentType> libraryContentTypes = new Dictionary<string, ImportedContentType>();
                Dictionary<string, ImportedContentType> contentTypesById = new Dictionary<string, ImportedContentType>();
                Dictionary<string, ContentType> allTypes = new Dictionary<string, ContentType>();
                foreach (ContentType type in contentTypeCollection)
                {
                    ImportedContentType ct = new ImportedContentType(this, type);
                    siteContentTypes[type.Name] = ct;
                    contentTypesById[type.Id.StringValue] = ct;
                }

                Dictionary<string, Field> existingFields = new Dictionary<string, Field>();
                foreach (Field f in clientContext.Web.Fields)
                {
                    existingFields[f.StaticName] = f;
                }

                HashSet<string> documentLibraryTypes = new HashSet<string>();
                foreach (ContentType ct in documentLibrary.ContentTypes)
                {
                    documentLibraryTypes.Add(ct.Name);
                    ImportedContentType ict = new ImportedContentType(this, ct);
                    libraryContentTypes[ct.Name] = ict;
                    contentTypesById[ct.Id.StringValue] = ict;
                }

                // Now we go over the XML declarations
                HashSet<string> newTypes = new HashSet<string>();
                XElement types = XElement.Load(this.ImportContext.LoadIndex("types"));
                XNamespace ns = types.GetDefaultNamespace();
                foreach (XElement type in types.Elements(ns + "type"))
                {
                    string typeName = (string)type.Element(ns + "name");
                    ImportedContentType finalType = null;
                    HashSet<string> linkNames = new HashSet<string>();
                    bool skipInherited = true;
                    bool patchFields = false;
                    bool versionableType = false;
                    bool containerType = false;
                    if (siteContentTypes.ContainsKey(typeName))
                    {
                        finalType = siteContentTypes[typeName];
                        foreach (FieldLink link in finalType.Type.FieldLinks)
                        {
                            linkNames.Add(link.Name);
                        }
                        if (typeName == "dm_sysobject" || typeName == "dm_folder")
                        {
                            patchFields = true;
                            versionableType = (typeName == "dm_sysobject");
                            containerType = (typeName == "dm_folder");
                        }
                    }
                    else
                    {
                        // New type...create it
                        ImportedContentType superType = null;
                        XElement superTypeElement = type.Element(ns + "superType");
                        if ((superTypeElement != null) && (typeName != "dm_folder"))
                        {
                            string stName = (string)superTypeElement;
                            if (siteContentTypes.ContainsKey(stName))
                            {
                                superType = siteContentTypes[stName];
                            }
                        }

                        if (superType == null)
                        {
                            switch (typeName)
                            {
                                case "dm_sysobject":
                                    superType = siteContentTypes["Document"];
                                    // skipInherited = false;
                                    patchFields = true;
                                    versionableType = true;
                                    break;
                                case "dm_folder":
                                    superType = siteContentTypes["Folder"];
                                    // skipInherited = false;
                                    patchFields = true;
                                    containerType = true;
                                    break;
                                default:
                                    // This isn't a type we're intereseted in, so we skip it
                                    continue;
                            }
                        }

                        Log.Info(string.Format("Creating content type {0} (descended from [{1}]])", typeName, superType.Name));
                        ContentTypeCreationInformation ctInfo = new ContentTypeCreationInformation();
                        ctInfo.Description = string.Format("Documentum Type {0}", typeName);
                        ctInfo.Name = typeName;
                        ctInfo.ParentContentType = (superType != null ? superType.Type : null);
                        ctInfo.Group = this.TypeGroup;

                        ContentType contentTypeObj = contentTypeCollection.Add(ctInfo);
                        clientContext.Load(contentTypeObj, t => t.Id);
                        session.ExecuteQuery();

                        finalType = new ImportedContentType(this, contentTypeObj, superType.Id);
                        siteContentTypes[typeName] = finalType;
                        contentTypesById[finalType.Id.StringValue] = finalType;
                    }

                    // Now we link the type to its fields, as needed
                    int updateCount = 0;

                    XElement attributeContainer = type.Element(ns + "attributes");
                    if (patchFields)
                    {
                        XElement versionAtt = new XElement(ns + "attribute");
                        versionAtt.SetAttributeValue("length", "32");
                        versionAtt.SetAttributeValue("repeating", "false");
                        versionAtt.SetAttributeValue("inherited", "false");
                        versionAtt.SetAttributeValue("sourceName", "version");
                        versionAtt.SetAttributeValue("name", "cmf:version");
                        versionAtt.SetAttributeValue("dataType", "STRING");
                        attributeContainer.AddFirst(versionAtt);

                        versionAtt = new XElement(ns + "attribute");
                        versionAtt.SetAttributeValue("length", "400");
                        versionAtt.SetAttributeValue("repeating", "false");
                        versionAtt.SetAttributeValue("inherited", "false");
                        versionAtt.SetAttributeValue("sourceName", "title");
                        versionAtt.SetAttributeValue("name", "cmis:description");
                        versionAtt.SetAttributeValue("dataType", "STRING");
                        attributeContainer.AddFirst(versionAtt);

                        versionAtt = new XElement(ns + "attribute");
                        versionAtt.SetAttributeValue("length", "128");
                        versionAtt.SetAttributeValue("repeating", "false");
                        versionAtt.SetAttributeValue("inherited", "false");
                        versionAtt.SetAttributeValue("sourceName", "author_name");
                        versionAtt.SetAttributeValue("name", "shpt:authorName");
                        versionAtt.SetAttributeValue("dataType", "STRING");
                        attributeContainer.AddFirst(versionAtt);

                        versionAtt = new XElement(ns + "attribute");
                        versionAtt.SetAttributeValue("length", "0");
                        versionAtt.SetAttributeValue("repeating", "false");
                        versionAtt.SetAttributeValue("inherited", "false");
                        versionAtt.SetAttributeValue("sourceName", "author");
                        versionAtt.SetAttributeValue("name", "shpt:author");
                        versionAtt.SetAttributeValue("dataType", "USER");
                        attributeContainer.AddFirst(versionAtt);

                        versionAtt = new XElement(ns + "attribute");
                        versionAtt.SetAttributeValue("length", "128");
                        versionAtt.SetAttributeValue("repeating", "false");
                        versionAtt.SetAttributeValue("inherited", "false");
                        versionAtt.SetAttributeValue("sourceName", "editor_name");
                        versionAtt.SetAttributeValue("name", "shpt:editorName");
                        versionAtt.SetAttributeValue("dataType", "STRING");
                        attributeContainer.AddFirst(versionAtt);

                        versionAtt = new XElement(ns + "attribute");
                        versionAtt.SetAttributeValue("length", "0");
                        versionAtt.SetAttributeValue("repeating", "false");
                        versionAtt.SetAttributeValue("inherited", "false");
                        versionAtt.SetAttributeValue("sourceName", "editor");
                        versionAtt.SetAttributeValue("name", "shpt:editor");
                        versionAtt.SetAttributeValue("dataType", "USER");
                        attributeContainer.AddFirst(versionAtt);

                        versionAtt = new XElement(ns + "attribute");
                        versionAtt.SetAttributeValue("length", "16");
                        versionAtt.SetAttributeValue("repeating", "false");
                        versionAtt.SetAttributeValue("inherited", "false");
                        versionAtt.SetAttributeValue("sourceName", "object_id");
                        versionAtt.SetAttributeValue("name", "cmis:objectId");
                        versionAtt.SetAttributeValue("dataType", "STRING");
                        attributeContainer.AddFirst(versionAtt);

                        versionAtt = new XElement(ns + "attribute");
                        versionAtt.SetAttributeValue("length", "256");
                        versionAtt.SetAttributeValue("repeating", "false");
                        versionAtt.SetAttributeValue("inherited", "false");
                        versionAtt.SetAttributeValue("sourceName", "location");
                        versionAtt.SetAttributeValue("name", "cmf:location");
                        versionAtt.SetAttributeValue("dataType", "STRING");
                        attributeContainer.AddFirst(versionAtt);

                        versionAtt = new XElement(ns + "attribute");
                        versionAtt.SetAttributeValue("length", "256");
                        versionAtt.SetAttributeValue("repeating", "false");
                        versionAtt.SetAttributeValue("inherited", "false");
                        versionAtt.SetAttributeValue("sourceName", "path");
                        versionAtt.SetAttributeValue("name", "cmis:path");
                        versionAtt.SetAttributeValue("dataType", "STRING");
                        attributeContainer.AddFirst(versionAtt);

                        versionAtt = new XElement(ns + "attribute");
                        versionAtt.SetAttributeValue("length", "256");
                        versionAtt.SetAttributeValue("repeating", "false");
                        versionAtt.SetAttributeValue("inherited", "false");
                        versionAtt.SetAttributeValue("sourceName", "name");
                        versionAtt.SetAttributeValue("name", "cmis:name");
                        versionAtt.SetAttributeValue("dataType", "STRING");
                        attributeContainer.AddFirst(versionAtt);

                        versionAtt = new XElement(ns + "attribute");
                        versionAtt.SetAttributeValue("length", "1024");
                        versionAtt.SetAttributeValue("repeating", "true");
                        versionAtt.SetAttributeValue("inherited", "false");
                        versionAtt.SetAttributeValue("sourceName", "keywords");
                        versionAtt.SetAttributeValue("name", "caliente:keywords");
                        versionAtt.SetAttributeValue("dataType", "STRING");
                        attributeContainer.AddFirst(versionAtt);

                        versionAtt = new XElement(ns + "attribute");
                        versionAtt.SetAttributeValue("length", "16");
                        versionAtt.SetAttributeValue("repeating", "false");
                        versionAtt.SetAttributeValue("inherited", "false");
                        versionAtt.SetAttributeValue("sourceName", "acl_id");
                        versionAtt.SetAttributeValue("name", "caliente:acl_id");
                        versionAtt.SetAttributeValue("dataType", "STRING");
                        attributeContainer.AddFirst(versionAtt);

                        if (versionableType)
                        {
                            versionAtt = new XElement(ns + "attribute");
                            versionAtt.SetAttributeValue("length", "16");
                            versionAtt.SetAttributeValue("repeating", "false");
                            versionAtt.SetAttributeValue("inherited", "false");
                            versionAtt.SetAttributeValue("sourceName", "history_id");
                            versionAtt.SetAttributeValue("name", "cmis:versionSeriesId");
                            versionAtt.SetAttributeValue("dataType", "STRING");
                            attributeContainer.AddFirst(versionAtt);

                            versionAtt = new XElement(ns + "attribute");
                            versionAtt.SetAttributeValue("length", "16");
                            versionAtt.SetAttributeValue("repeating", "false");
                            versionAtt.SetAttributeValue("inherited", "false");
                            versionAtt.SetAttributeValue("sourceName", "antecedent_id");
                            versionAtt.SetAttributeValue("name", "cmf:version_antecedent_id");
                            versionAtt.SetAttributeValue("dataType", "STRING");
                            attributeContainer.AddFirst(versionAtt);

                            versionAtt = new XElement(ns + "attribute");
                            versionAtt.SetAttributeValue("length", "0");
                            versionAtt.SetAttributeValue("repeating", "false");
                            versionAtt.SetAttributeValue("inherited", "false");
                            versionAtt.SetAttributeValue("sourceName", "current");
                            versionAtt.SetAttributeValue("name", "cmis:isLatestVersion");
                            versionAtt.SetAttributeValue("dataType", "BOOLEAN");
                            attributeContainer.AddFirst(versionAtt);
                        }
                    }

                    foreach (XElement att in attributeContainer.Elements(ns + "attribute"))
                    {
                        // The attribute is either not inherited or its inheritance is ignored, so add it to the content type's declaration
                        string attName = att.Attribute("name").Value;
                        string attSourceName = att.Attribute("sourceName").Value;
                        string finalName = string.Format("caliente_{0}", attSourceName);
                        // Special case for folder attributes inherited from dm_sysobject
                        bool inherited = XmlConvert.ToBoolean(att.Attribute("inherited").Value) && (typeName != "dm_folder");
                        bool repeating = XmlConvert.ToBoolean(att.Attribute("repeating").Value);

                        // If this is an inherited attribute, we won't add it because we're not interested in it
                        if (inherited && skipInherited) continue;

                        ImportedContentTypeField finalField = null;
                        if (linkNames.Contains(finalName) || (inherited && skipInherited))
                        {
                            // Existing or inherited link...
                            finalField = new ImportedContentTypeField(existingFields[finalName], attName, finalName, attSourceName, repeating);
                        }
                        else
                        {
                            FieldLinkCreationInformation fieldLink = new FieldLinkCreationInformation();
                            if (existingFields.ContainsKey(finalName))
                            {
                                finalField = new ImportedContentTypeField(existingFields[finalName], attName, finalName, attSourceName, repeating);
                                fieldLink.Field = finalField.Field;
                            }
                            else
                            {
                                Log.Info(string.Format("Creating field {0} (first declared by {1})", finalName, typeName));
                                FieldType attType = Tools.DecodeFieldType(att.Attribute("dataType").Value);
                                if (repeating)
                                {
                                    // Default repeating fields to strings, since they'll be concatenated
                                    attType = FieldType.Note;
                                }
                                int length = XmlConvert.ToInt32(att.Attribute("length").Value);
                                if (length > 255)
                                {
                                    attType = FieldType.Note;
                                }
                                Guid guid = Guid.NewGuid();


                                string fieldXml = string.Format("<Field DisplayName='{0}' Name='{1}' ID='{2}' Group='{3}' Type='{4}' />", finalName, finalName, guid.ToString(), this.FieldGroup, attType);
                                fieldLink.Field = clientContext.Web.Fields.AddFieldAsXml(fieldXml, false, AddFieldOptions.AddFieldInternalNameHint);
                                clientContext.Load(fieldLink.Field, f => f.Id, f => f.FieldTypeKind, f => f.StaticName, f => f.Group);
                                existingFields[finalName] = fieldLink.Field;
                                finalField = new ImportedContentTypeField(existingFields[finalName], attName, finalName, attSourceName, repeating);
                            }
                            finalType.Type.FieldLinks.Add(fieldLink);
                            linkNames.Add(finalName);
                            updateCount++;
                        }
                        finalType.AddField(finalField);
                    }
                    if (updateCount > 0)
                    {
                        finalType.Type.Update(true);
                        session.ExecuteQuery();
                    }
                    newTypes.Add(typeName);
                }

                // Now, make sure this type exists in the document library
                List<ContentType> newContentTypes = new List<ContentType>();
                foreach (string typeName in newTypes)
                {
                    if (!documentLibraryTypes.Contains(typeName))
                    {
                        ContentType ct = documentLibrary.ContentTypes.AddExistingContentType(siteContentTypes[typeName].Type);
                        newContentTypes.Add(ct);
                        clientContext.Load(ct);
                        clientContext.Load(ct, c => c.Parent);
                    }
                }
                if (newContentTypes.Count > 0)
                {
                    session.ExecuteQuery();
                    foreach (ContentType ct in newContentTypes)
                    {
                        ImportedContentType newCt = new ImportedContentType(this, ct);
                        contentTypesById[ct.Id.StringValue] = newCt;
                        libraryContentTypes[ct.Name] = newCt;
                    }
                }

                this.SiteContentTypes = siteContentTypes;
                this.LibraryContentTypes = libraryContentTypes;
                this.ContentTypesById = contentTypesById;
            }
        }

        private ImportedContentType ResolveNamedContentType(Dictionary<string, ImportedContentType> dict, string name)
        {
            if (dict.ContainsKey(name))
            {
                return dict[name];
            }
            return null;
        }

        public ImportedContentType ResolveSiteContentType(string name)
        {
            return ResolveNamedContentType(this.SiteContentTypes, name);
        }

        public ImportedContentType ResolveLibraryContentType(string name)
        {
            return ResolveNamedContentType(this.LibraryContentTypes, name);
        }

        public ImportedContentType ResolveContentType(ContentTypeId id)
        {
            if (this.ContentTypesById.ContainsKey(id.StringValue))
            {
                return this.ContentTypesById[id.StringValue];
            }
            return null;
        }

        private void CleanContentTypes(SharePointSession session, ContentTypeCollection contentTypes)
        {
            if (this.TypeGroup == null) return;
            ClientContext clientContext = session.ClientContext;
            clientContext.Load(contentTypes, c => c.Include(t => t.Id, t => t.Name, t => t.Group, t => t.Parent, t => t.Parent.Name, t => t.Parent.Group));
            session.ExecuteQuery();

            Dictionary<string, ContentType> tbdTypes = new Dictionary<string, ContentType>();
            foreach (ContentType type in contentTypes)
            {
                if (type.Group == this.TypeGroup)
                {
                    tbdTypes[type.Name] = type;
                }
            }
            if (tbdTypes.Count > 0)
            {
                // TODO: we have an inheritance issue here, in that parent types can't be
                // deleted prior to their subtypes...so build the tree...
                Dictionary<int, List<ContentType>> tree = new Dictionary<int, List<ContentType>>();
                int maxDepth = -1;
                foreach (ContentType type in tbdTypes.Values)
                {
                    // Calculate the depth for this item
                    int depth = 0;
                    ContentType current = type;
                    while (current.Parent != null && current.Parent.Name != current.Name && current.Parent.Group == this.TypeGroup)
                    {
                        depth++;
                        if (!tbdTypes.ContainsKey(current.Parent.Name)) break;
                        current = tbdTypes[current.Parent.Name];
                    }
                    List<ContentType> l = null;
                    if (tree.ContainsKey(depth))
                    {
                        l = tree[depth];
                    }
                    else
                    {
                        l = new List<ContentType>();
                        tree[depth] = l;
                    }
                    l.Add(type);
                    if (depth > maxDepth) maxDepth = depth;
                }
                for (int d = maxDepth ; d >= 0 ; d--)
                {
                    foreach (ContentType type in tree[d])
                    {
                        type.DeleteObject();
                    }
                }
                session.ExecuteQuery();
            }
        }

        private void CleanFields(SharePointSession session, FieldCollection fields)
        {
            if (this.FieldGroup == null) return;
            ClientContext clientContext = session.ClientContext;
            clientContext.Load(fields, c => c.Include(t => t.Group, t => t.StaticName));
            session.ExecuteQuery();

            List<Field> tbdFields = new List<Field>();
            foreach (Field f in fields)
            {
                if (f.Group == this.FieldGroup)
                {
                    tbdFields.Add(f);
                }
            }
            if (tbdFields.Count > 0)
            {
                foreach (Field f in tbdFields)
                {
                    f.DeleteObject();
                }
                session.ExecuteQuery();
            }
        }
    }
}
