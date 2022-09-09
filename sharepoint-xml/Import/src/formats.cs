using Caliente.SharePoint.Common;
using log4net;
using Microsoft.SharePoint.Client;
using System;
using System.Collections.Generic;
using System.Xml;
using System.Xml.Linq;

namespace Caliente.SharePoint.Import
{
    public class Format
    {
        public readonly string Name;
        public readonly string DosExtension;
        public readonly string MimeType;
        public readonly string Description;

        public Format(string name, string dosExtension, string mimeType, string description)
        {
            this.Name = name;
            this.DosExtension = dosExtension;
            this.MimeType = mimeType;
            this.Description = description;
        }

        public override string ToString()
        {
            return string.Format("Format{name={0}, ext={1}, type={2}, desc={3} }", this.Name, this.DosExtension, this.MimeType, this.Description);
        }
    }

    public class FormatResolver : BaseImporter
    {
        public static readonly ILog LOG = LogManager.GetLogger(typeof(FormatResolver));

        private readonly Dictionary<string, Format> Formats;

        public FormatResolver(ImportContext importContext) : base("formats", importContext)
        {
            this.Log.Info("Loading the format information");
            this.Formats = new Dictionary<string, Format>();
            XmlReader formatsXml = this.ImportContext.LoadIndex("formats");
            if (formatsXml == null) return;
            using (formatsXml)
            {
                while (formatsXml.ReadToFollowing("format"))
                {
                    XElement formatXml = XElement.Load(formatsXml.ReadSubtree());
                    XNamespace ns = formatXml.GetDefaultNamespace();

                    string name = (string)formatXml.Element(ns + "name");
                    string description = (string)formatXml.Element(ns + "description");
                    string dosExtension = XmlTools.GetAttributeValue(formatXml, "caliente:dos_extension");
                    string mimeType = XmlTools.GetAttributeValue(formatXml, "caliente:mime_type");
                    Format format = new Format(name, dosExtension, mimeType, description);
                    if (!this.Formats.ContainsKey(name))
                    {
                        this.Formats[name] = format;
                    }
                    else
                    {
                        // Log a warning...and discard the new format
                        this.Log.Warn(string.Format("Duplicate format name [{0}] - mapped to {1} and {2}", name, this.Formats[name], format));
                    }
                }
            }
        }

        public Format ResolveFormat(string name)
        {
            if (name == null) return null;
            return (this.Formats.ContainsKey(name) ? this.Formats[name] : null);
        }
    }
}
