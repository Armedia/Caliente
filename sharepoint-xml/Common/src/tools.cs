using log4net;
using Microsoft.SharePoint.Client;
using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Security;
using System.Text;
using System.Text.RegularExpressions;
using System.Xml;
using System.Xml.Linq;

namespace Armedia.CMSMF.SharePoint.Common
{
    public sealed class Tools
    {
        private Tools()
        {
        }

        // This has been extended to include all unicode control characters, as they too are unsafe
        private const string UnsafeChars = "~#%&*{}\\:<>?/|\"" +
            "\u0000\u0001\u0002\u0003\u0004\u0005\u0006\u0007\u0008\u0009\u000A\u000B\u000C\u000D\u000E\u000F" +
            "\u0010\u0011\u0012\u0013\u0014\u0015\u0016\u0017\u0018\u0019\u001A\u001B\u001C\u001D\u001E\u001F" +
            "\u007F" +
            "\u0080\u0081\u0082\u0083\u0084\u0085\u0086\u0087\u0088\u0089\u008A\u008B\u008C\u008D\u008E\u008F" +
            "\u0090\u0091\u0092\u0093\u0094\u0095\u0096\u0097\u0098\u0099\u009A\u009B\u009C\u009D\u009E\u009F";

        private static readonly Regex PeriodConsolidator = new Regex("(\\.)\\1+");

        private static readonly Regex ReplaceVTI = new Regex("_vti_");

        private static readonly Regex ReplaceHidden = new Regex("^_");

        private static readonly Regex ReplaceLineTerm = new Regex("[\r\n]");

        private static readonly string[] BlankArray = { };

        private static readonly string[] BadFilenameEnd =
        {
            ".files",
            "_files",
            "-Dateien",
            "_fichiers",
            "_bestanden",
            "_file",
            "_archivos",
            "-filer",
            "_tiedostot",
            "_pliki",
            "_soubory",
            "_elemei",
            "_ficheiros",
            "_arquivos",
            "_dosyalar",
            "_datoteke",
            "_fitxers",
            "_failid",
            "_fails",
            "_bylos",
            "_fajlovi",
            "_fitxategiak"
        };

        public static FieldType DecodeFieldType(string type)
        {
            FieldType fieldType = FieldType.Invalid;
            switch (type)
            {
                case "BOOLEAN": fieldType = FieldType.Boolean; break;
                case "INTEGER": fieldType = FieldType.Integer; break;
                case "DOUBLE": fieldType = FieldType.Number; break;
                case "STRING": fieldType = FieldType.Text; break;
                case "ID": fieldType = FieldType.Text; break;
                case "DATETIME": fieldType = FieldType.DateTime; break;
                case "URI": fieldType = FieldType.URL; break;
                case "HTML": // fall-through
                case "NOTE": fieldType = FieldType.Note; break;

                // This one is special, just for Sharepoint
                case "USER": fieldType = FieldType.User; break;
            }
            return fieldType;
        }
        public static string MakeSafePath(string path)
        {
            string safePath = "";
            foreach (string s in path.Split("/".ToCharArray(), StringSplitOptions.RemoveEmptyEntries))
            {
                safePath = safePath + "/" + MakeSafeFolderName(s, null);
            }
            return safePath;
        }
        public static string SanitizeSingleLineString(string name)
        {
            return ReplaceLineTerm.Replace(name, "");
        }

        public static int CountChars(string str, char c)
        {
            int count = 0;
            foreach (char x in str) if (x == c) count++;
            return count;
        }

        public static DateTime ParseXmlDate(XElement element)
        {
            return ParseXmlDate((string)element);
        }

        public static DateTime ParseXmlDate(string date)
        {
            return XmlConvert.ToDateTime(date, XmlDateTimeSerializationMode.Utc);
        }

        public static string MakeSafeFileName(string fileName, string objectId = null)
        {
            // TODO: May have to use the bad filenames here
            return MakeSafeFolderName(fileName, objectId, BlankArray);
        }

        public static string MakeSafeFolderName(string folderName, string objectId = null)
        {
            return MakeSafeFolderName(folderName, objectId, BlankArray);
        }

        public static string MakeSafeFolderName(string folderName, string objectId, string[] disallowedTerminators)
        {
            string oldName = folderName;
            // We only do a maximum of 10 attempts, because we don't want to get stuck in an infinite loop
            for (int i = 0 ; i < 5 ; i++)
            {
                string newName = FixFolderName(oldName, objectId, disallowedTerminators);
                if (oldName == newName) break;
                oldName = newName;
                objectId = null; // We don't need to append it over and over again
            }
            return oldName;
        }

        private static string FixFolderName(string folderName, string objectId, string[] disallowedTerminators)
        {
            string ret = SanitizeSingleLineString(folderName);
            if (!string.IsNullOrEmpty(objectId)) objectId = SanitizeSingleLineString(objectId);
            // replace disallowed characters
            ret = string.Join("_", ret.Split(UnsafeChars.ToCharArray()));
            // consolidate consecutive periods
            ret = PeriodConsolidator.Replace(ret, "$1");
            // remove leading and trailing periods
            ret = ret.Trim('.');

            // Replace every instance of "_vti_" with _v.t.i_)
            ret = ReplaceVTI.Replace(ret, "_v.t.i_");

            if (ret.StartsWith("_"))
            {
                // In SharePoint, a leading underscore means the file is hidden, so we replace it with a 
                // dash instead - similar enough, but won't add funkiness and won't hide files
                ret = ReplaceHidden.Replace(ret, "-");
            }

            if (disallowedTerminators != null && disallowedTerminators.Length > 0)
            {
                // TODO: What, exactly, is the correct solution here?
            }

            if (objectId != null)
            {
                ret = string.Format("{0}_{1}", objectId, ret);
            }
            if (ret.Length > 127)
            {
                // Keep the extension - everything after the last dot
                // take the length of the extension (including the dot), subtract it from 127
                // and that's how many characters need to be preserved from the filename
                int lastDot = ret.LastIndexOf('.');
                if (lastDot >= 0)
                {
                    string ext = ret.Substring(lastDot);
                    ret = string.Format("{0}{1}", ret.Substring(0, 127 - ext.Length), ext);
                }
                else
                {
                    // No extension, so just truncate it
                    ret = ret.Substring(0, 127);
                }
            }
            return ret;
        }

        // <summary>
        /// Like System.Console.ReadLine(), only with a mask.
        /// </summary>
        /// <param name="mask">a <c>char</c> representing your choice of console mask</param>
        /// <returns>the string the user typed in </returns>
        public static SecureString ReadPassword(char mask)
        {
            const int ENTER = 13, BACKSP = 8, CTRLBACKSP = 127;
            int[] FILTERED = { 0, 27, 9, 10 /*, 32 space, if you care */ }; // const

            // First, clear the buffer...
            while (Console.KeyAvailable) Console.ReadKey(true);

            char chr = (char)0;
            SecureString str = new SecureString();
            while ((chr = System.Console.ReadKey(true).KeyChar) != ENTER)
            {
                if (chr == BACKSP)
                {
                    if (str.Length > 0)
                    {
                        System.Console.Write("\b \b");
                        str.RemoveAt(str.Length - 1);
                    }
                }
                else if (chr == CTRLBACKSP)
                {
                    while (str.Length > 0)
                    {
                        System.Console.Write("\b \b");
                        str.RemoveAt(str.Length - 1);
                    }
                }
                else if (FILTERED.Count(x => chr == x) > 0) { }
                else
                {
                    str.AppendChar(chr);
                    System.Console.Write(mask);
                }
            }

            System.Console.WriteLine();

            return str;
        }

        /// <summary>
        /// Like System.Console.ReadLine(), only with a mask.
        /// </summary>
        /// <returns>the string the user typed in </returns>
        public static SecureString ReadPassword()
        {
            return ReadPassword('*');
        }
    }
    public class XmlFile : XmlTextWriter
    {
        private readonly ILog Log;
        private readonly FileInfo Info;
        private readonly bool Backup;
        private readonly FileInfo Target;

        private XmlFile(object marker, FileInfo info, Encoding encoding, bool backup = true) : base(info.Open(FileMode.Create), encoding)
        {
            this.Log = LogManager.GetLogger(GetType());
            this.Backup = backup;
            this.Info = info;
            this.Indentation = 1;
            this.Formatting = Formatting.Indented;
            this.IndentChar = '\t';
        }

        public XmlFile(string targetFilename, Encoding encoding, bool backup = true) : this(null, new FileInfo(Path.GetTempFileName()), encoding, backup)
        {
            this.Target = new FileInfo(targetFilename);
        }

        public XmlFile(FileInfo target, Encoding encoding, bool backup = true) : this(target.FullName, encoding, backup)
        {
        }

        public override void Close()
        {
            base.Close();
            if (this.Target.Exists)
            {
                if (this.Backup)
                {
                    try
                    {
                        Info.Replace(this.Target.FullName, string.Format("{0}.bak-{1:yyyyMMddHHmmss}", this.Target.FullName, DateTime.Now));
                        return;
                    }
                    catch (IOException e)
                    {
                        if (Log.IsDebugEnabled) Log.Debug(string.Format("Failed to create a backup for the XML file at [{0}]", this.Target.FullName), e);
                    }
                }
                this.Target.Delete();
            }
            Info.MoveTo(this.Target.FullName);
        }
    }

    public class XmlTools
    {
        public static string GetAttributeValue(XElement element, string name)
        {
            foreach (string s in GetAttributeValues(element, name))
            {
                return s;
            }
            return null;
        }

        public static IEnumerable<string> GetAttributeValues(XElement element, string name)
        {
            XNamespace ns = element.GetDefaultNamespace();
            XElement attributes = element;
            IEnumerable<XElement> values = Enumerable.Empty<XElement>();
            if (element.Name != (ns + "attributes"))
            {
                attributes = element.Element(ns + "attributes");
            }
            if (attributes != null)
            {
                values = attributes.Elements(ns + "attribute").FirstOrDefault(a => a.Attribute("name").Value == name).Descendants(ns + "value");
            }

            foreach (XElement kw in values)
            {
                yield return (string)kw;
            }
        }

    }
}