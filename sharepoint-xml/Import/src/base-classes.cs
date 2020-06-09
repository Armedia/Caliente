﻿using Armedia.CMSMF.SharePoint.Common;
using log4net;
using Microsoft.SharePoint.Client;
using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Security.Cryptography;
using System.Text;
using System.Threading;
using System.Xml;
using System.Xml.Linq;

namespace Armedia.CMSMF.SharePoint.Import
{
    public abstract class FSObjectInfo
    {
        public readonly string Id;
        public readonly string Acl;
        public readonly string Path;
        public readonly string SafePath;
        public readonly string Name;
        public readonly string SafeName;
        public readonly string FullPath;
        public readonly string SafeFullPath;
        public readonly string Type;
        public readonly string Location;
        public readonly DateTime CreationDate;
        public readonly string Creator;
        public readonly DateTime ModificationDate;
        public readonly string Modifier;

        protected FSObjectInfo(string location)
        {
            XElement xml = XElement.Load(location);
            XNamespace ns = xml.GetDefaultNamespace();
            this.Location = location;
            this.Id = (string)xml.Element(ns + "id");
            this.Acl = (string)xml.Element(ns + "acl");
            this.Path = (string)xml.Element(ns + "sourcePath");
            this.SafePath = Tools.MakeSafePath(this.Path);
            this.Name = Tools.SanitizeSingleLineString((string)xml.Element(ns + "name"));
            this.SafeName = Tools.MakeSafeFolderName(this.Name, null);
            this.FullPath = string.Format("{0}/{1}", (this.Path == "/" ? "" : this.Path), this.Name);
            this.SafeFullPath = string.Format("{0}/{1}", this.SafePath, this.SafeName);
            this.CreationDate = Tools.ParseXmlDate(xml.Element(ns + "creationDate"));
            this.Creator = (string)xml.Element(ns + "creator");
            this.ModificationDate = Tools.ParseXmlDate(xml.Element(ns + "modificationDate"));
            this.Modifier = (string)xml.Element(ns + "modifier");
            this.Type = (string)xml.Element(ns + "type");
        }
    }

    public class ContentSimulator
    {
        private const int BLOCK_SIZE = (100 * 1024);

        private static readonly object BufferLock = new object();
        private static byte[] SampleBuffer = null;

        private class SimulationStream : Stream
        {
            private long Pos = 0;
            private readonly long Size;
            private readonly byte[] Buffer;

            public SimulationStream(long size, byte[] buf)
            {
                this.Buffer = buf;
                this.Size = size;
            }

            public override bool CanRead
            {
                get { return true; }
            }
            public override bool CanSeek
            {
                get { return true; }
            }
            public override bool CanWrite
            {
                get { return false; }
            }
            public override long Length
            {
                get { return this.Size; }
            }

            public override long Position
            {
                get { return this.Pos; }
                set { this.Pos = value; }
            }

            public override void Flush()
            {
            }

            public override int Read(byte[] buffer, int offset, int count)
            {
                if (Pos >= Size) return 0;
                long remaining = (Size - Pos);
                if (remaining < count) count = (int)remaining;
                for (long i = 0 ; i < count ; i++)
                {
                    buffer[offset + i] = Buffer[(Pos + i) % Buffer.Length];
                }
                Pos += count;
                return count;
            }

            public override long Seek(long offset, SeekOrigin origin)
            {
                long newPos = Pos;
                switch (origin)
                {
                    case SeekOrigin.Begin:
                        newPos = offset;
                        break;
                    case SeekOrigin.End:
                        newPos = this.Size - offset;
                        break;
                    case SeekOrigin.Current:
                        newPos += offset;
                        break;
                }
                if (newPos < 0 || newPos > Size)
                {
                    throw new IndexOutOfRangeException();
                }
                this.Pos = newPos;
                return this.Pos;
            }

            public override void SetLength(long value)
            {
                throw new NotImplementedException();
            }

            public override void Write(byte[] buffer, int offset, int count)
            {
                throw new NotImplementedException();
            }
        }

        private static byte[] initialize()
        {
            lock (BufferLock)
            {
                if (SampleBuffer == null)
                {
                    SampleBuffer = new byte[BLOCK_SIZE];
                    new Random().NextBytes(SampleBuffer);
                }
            }
            return SampleBuffer;
        }

        public static Stream SimulateStream(long size, byte[] buf = null)
        {
            if (buf == null) buf = initialize();
            return new SimulationStream(size, buf);
        }
    }

    public class ImportContext
    {
        public readonly SharePointSessionFactory SessionFactory;
        private readonly string ContentLocation;
        private readonly XmlReaderSettings XmlSettings;

        public ImportContext(SharePointSessionFactory sessionFactory, string contentLocation)
        {
            this.SessionFactory = sessionFactory;
            this.ContentLocation = contentLocation;
            this.XmlSettings = new XmlReaderSettings();
            this.XmlSettings.DtdProcessing = DtdProcessing.Parse;
            this.XmlSettings.MaxCharactersFromEntities = 1024;
        }

        public XmlReader LoadIndex(string name)
        {
            return XmlReader.Create(string.Format("{0}/{1}", this.ContentLocation, name), this.XmlSettings);
        }

        public XmlReader LoadOptionalIndex(string name)
        {
            try
            {
                return LoadIndex(name);
            }
            catch (FileNotFoundException)
            {
                // We only catch this one, because it's only OK if it doesn't exist.
                // So for instance, if it exists and isn't readable, or is badly-formatted, we still want to explode then
                return null;
            }
        }

        public XmlWriter CreateIndex(string name, string rootElement)
        {
            XmlTextWriter w = new XmlFile(string.Format("{0}/{1}", this.ContentLocation, name), UTF8Encoding.UTF8);
            w.WriteStartDocument();
            w.WriteDocType(rootElement, null, null, null);
            w.WriteStartElement(rootElement);
            w.WriteAttributeString("date", XmlConvert.ToString(DateTime.UtcNow, XmlDateTimeSerializationMode.Utc));

            return w;
        }

        public XmlReader LoadDescriptor(string location)
        {
            return XmlReader.Create(FormatContentLocation(location), this.XmlSettings);
        }

        public string FormatContentLocation(string location)
        {
            return string.Format("{0}/content/{1}", this.ContentLocation, location);
        }
    }

    public abstract class BaseImporter
    {
        private const string DEFAULT_LABEL = "OBJECTS";

        public readonly ImportContext ImportContext;
        protected readonly ILog Log;
        private long TotalProgress = 0;
        private long LastProgress = Environment.TickCount;
        private long ProgressCounter = 0;
        protected readonly string Label;

        private bool _Abort = false;

        public bool Abort
        {
            get
            {
                return this._Abort;
            }

            set
            {
                this._Abort |= value;
            }
        }

        protected BaseImporter(string label, BaseImporter baseImporter) : this(label, baseImporter?.ImportContext)
        {
        }

        protected BaseImporter(string label, ImportContext importContext)
        {
            this.ImportContext = importContext;
            this.Log = LogManager.GetLogger(GetType());
            this.Label = (string.IsNullOrWhiteSpace(label) ? DEFAULT_LABEL : label.ToUpper());
        }

        protected void ResetProgress(long totalProgress)
        {
            if (totalProgress <= 0) throw new ArgumentException("Progress must be > 0");
            this.ProgressCounter = 0;
            this.TotalProgress = totalProgress;
            this.LastProgress = Environment.TickCount;
        }

        protected bool IncreaseProgress(bool quiet = false)
        {
            long count = Interlocked.Increment(ref this.ProgressCounter);
            if (quiet) return false;
            return ShowProgress(count);
        }

        private bool ShowProgress(long aggregateCurrent)
        {
            bool milestone = (aggregateCurrent == this.TotalProgress);
            long now = Environment.TickCount;
            long last = Interlocked.Read(ref this.LastProgress);

            bool shouldDisplay = (milestone || ((now - last) >= 5000));
            bool shown = false;
            if (shouldDisplay && (last == Interlocked.CompareExchange(ref this.LastProgress, now, last)))
            {
                double percentage = ((double)aggregateCurrent / (double)this.TotalProgress);
                StringWriter w = new StringWriter();
                w.WriteLine();
                w.WriteLine("PROGRESS REPORT{0}", (Abort ? " (ABORTED, awaiting normal termination)" : ""));
                w.WriteLine("\tProcessed {0}/{1} {2} ({3:P})", aggregateCurrent, this.TotalProgress, this.Label, percentage);
                w.WriteLine();
                GetSupplementalProgress(w, "\t");
                Log.Info(w.ToString());
                shown = true;
            }
            return shown;
        }

        protected virtual void GetSupplementalProgress(TextWriter w, string lineLead = "")
        {
        }

        protected virtual bool ShowProgress()
        {
            return ShowProgress(Interlocked.Read(ref this.ProgressCounter));
        }
    }

    public abstract class FSObjectImporter : BaseImporter
    {
        public readonly ContentTypeImporter ContentTypeImporter;
        public readonly PermissionsImporter PermissionsImporter;
        public readonly UserGroupImporter UserGroupImporter;

        private int CounterSkipped = 0;
        private int CounterCompleted = 0;
        private int CounterFailed = 0;

        public enum Result
        {
            Skipped,
            Completed,
            Failed
        }

        protected class ProgressTracker
        {
            private readonly FileInfo CompletedMarker;
            private readonly FileInfo FailedMarker;
            private readonly FileInfo IgnoreMarker;
            private readonly string DescriptorLocation;
            private readonly ILog Log;
            private readonly System.Collections.Generic.List<string> Progress;

            public ProgressTracker(string xmlLocation, ILog log)
            {
                this.Progress = new System.Collections.Generic.List<string>();
                this.Log = log;
                this.DescriptorLocation = xmlLocation;
                this.CompletedMarker = new FileInfo(string.Format("{0}.completed", this.DescriptorLocation));
                this.FailedMarker = new FileInfo(string.Format("{0}.failed", this.DescriptorLocation));
                this.IgnoreMarker = new FileInfo(string.Format("{0}.ignore", this.DescriptorLocation));
            }

            public void TrackProgress(string format, params object[] args)
            {
                if (this.Progress.Count == 0)
                {
                    this.Progress.Add(string.Format("{0:O} BEGIN ACTION REPORT FOR [{1}]", DateTime.Now, this.DescriptorLocation));
                }
                string msg = string.Format(format, args);
                this.Log.Info(msg);
                Progress.Add(string.Format("{0:O} {1}", DateTime.Now, msg));
            }

            public void SaveOutcomeMarker(Result r, Exception e, bool markIgnored = false)
            {
                if (r == Result.Skipped) return;
                FileInfo fi = null;
                switch (r)
                {
                    case Result.Completed: fi = this.CompletedMarker; break;
                    case Result.Failed: fi = this.FailedMarker; break;
                }

                bool appending = fi.Exists;
                using (StreamWriter sw = (appending ? fi.AppendText() : fi.CreateText()))
                {
                    if (appending) sw.WriteLine();
                    foreach (string str in this.Progress)
                    {
                        sw.WriteLine(str);
                    }
                    if (e != null) sw.WriteLine("{0:O} {1}", DateTime.Now, e.ToString());
                    sw.WriteLine("{0:O} {1}", DateTime.Now, r);
                    sw.Flush();
                }
                if (markIgnored)
                {
                    this.IgnoreMarker.AppendText().Close();
                }
            }

            public void DeleteOutcomeMarker()
            {
                this.CompletedMarker.Delete();
                this.FailedMarker.Delete();
            }

            public bool Completed
            {
                get { return this.CompletedMarker.Exists; }
                private set { }
            }

            public bool Failed
            {
                get { return this.FailedMarker.Exists; }
                private set { }
            }

            public bool Ignored
            {
                get { return this.IgnoreMarker.Exists; }
                private set { }
            }
        }

        protected FSObjectImporter(string label, ImportContext importContext) : this(label, importContext, null, null)
        {
        }

        protected FSObjectImporter(string label, FSObjectImporter importer) : this(label, importer.ImportContext, importer.ContentTypeImporter, importer.PermissionsImporter)
        {
        }

        protected FSObjectImporter(string label, ContentTypeImporter contentTypeImporter, PermissionsImporter permissionsImporter) : this(label, contentTypeImporter?.ImportContext ?? permissionsImporter.ImportContext, contentTypeImporter, permissionsImporter)
        {
        }

        protected FSObjectImporter(string label, ImportContext importContext, ContentTypeImporter contentTypeImporter, PermissionsImporter permissionsImporter) : base(label, importContext)
        {
            this.ContentTypeImporter = contentTypeImporter;
            this.PermissionsImporter = permissionsImporter;
            this.UserGroupImporter = permissionsImporter?.UserGroupImporter;
        }

        protected int ResetCounter(Result result)
        {
            int ret = -1;
            switch (result)
            {
                case Result.Skipped:
                    ret = Interlocked.Exchange(ref this.CounterSkipped, 0);
                    break;
                case Result.Completed:
                    ret = Interlocked.Exchange(ref this.CounterCompleted, 0);
                    break;
                case Result.Failed:
                    ret = Interlocked.Exchange(ref this.CounterFailed, 0);
                    break;
            }
            return ret;
        }

        protected void ResetCounters()
        {
            foreach (Result r in Enum.GetValues(typeof(Result)))
            {
                ResetCounter(r);
            }
        }

        protected int IncrementCounter(Result result)
        {
            int ret = -1;
            switch (result)
            {
                case Result.Skipped:
                    ret = Interlocked.Increment(ref this.CounterSkipped);
                    break;
                case Result.Completed:
                    ret = Interlocked.Increment(ref this.CounterCompleted);
                    break;
                case Result.Failed:
                    ret = Interlocked.Increment(ref this.CounterFailed);
                    break;
            }
            return ret;
        }

        public Dictionary<Result, int> ProgressCounters
        {
            get
            {
                Dictionary<Result, int> ret = new Dictionary<Result, int>();
                ret.Add(Result.Completed, this.CounterCompleted);
                ret.Add(Result.Failed, this.CounterFailed);
                ret.Add(Result.Skipped, this.CounterSkipped);
                return ret;
            }

            private set { }
        }

        protected override void GetSupplementalProgress(TextWriter w, string lineLead = "")
        {
            Dictionary<FSObjectImporter.Result, int> results = this.ProgressCounters;
            int total = 0;
            foreach (FSObjectImporter.Result r in results.Keys)
            {
                int v = results[r];
                total += v;
                w.WriteLine("{0}{1} {2} {3}", lineLead, v, Label.ToLower(), r);
            }
        }

        public string ProcessingReport
        {
            get
            {
                Dictionary<FSObjectImporter.Result, int> results = this.ProgressCounters;
                StringWriter w = new StringWriter();
                w.WriteLine("Processing report for {0}:", this.Label);
                GetSupplementalProgress(w, "\t");
                return w.ToString();
            }
            private set { }
        }

        protected void ApplyAttributes(ListItem li, XElement attributes, String objectType)
        {
            XNamespace ns = attributes.GetDefaultNamespace();
            ImportedContentType typeObject = this.ContentTypeImporter.ResolveLibraryContentType(objectType);
            foreach (XElement attribute in attributes.Elements(ns + "attribute"))
            {
                string fieldName = attribute.Attribute("name").Value;
                ImportedContentTypeField f = typeObject.GetField(fieldName);
                if (f == null) continue;
                // Ok...so...now we convert the value and slam it into the thing
                object value = null;
                if (f.Repeating)
                {
                    value = "|";
                    bool first = true;
                    foreach (XElement v in attribute.Elements(ns + "value"))
                    {
                        if (!first)
                        {
                            value += "|";
                        }
                        value += Tools.SanitizeSingleLineString((string)v);
                        first = false;
                    }
                    value += "|";
                }
                else
                {
                    FieldType type = Tools.DecodeFieldType(attribute.Attribute("dataType").Value);
                    string v = Tools.SanitizeSingleLineString((string)attribute.Elements(ns + "value").FirstOrDefault());
                    if (type == FieldType.Text)
                    {
                        value = v;
                    }
                    else if (v != null && v != string.Empty)
                    {
                        switch (type)
                        {
                            case FieldType.Boolean:
                                value = XmlConvert.ToBoolean(v);
                                break;
                            case FieldType.Integer:
                                value = XmlConvert.ToInt32(v);
                                break;
                            case FieldType.Number:
                                value = XmlConvert.ToDecimal(v);
                                break;
                            case FieldType.DateTime:
                                value = Tools.ParseXmlDate(v);
                                break;
                            default:
                                value = v;
                                break;
                        }
                    }
                }
                li[f.FinalName] = value;
            }
        }

        // Reference: http://sharepoint.stackexchange.com/questions/130636/cannot-update-created-by-author-field-through-powershell
        protected void ApplyMetadata(ListItem li, XElement element)
        {
            XNamespace ns = element.GetDefaultNamespace();
            string path = (string)element.Element(ns + "sourcePath");
            string name = Tools.SanitizeSingleLineString((string)element.Element(ns + "name"));
            li["Title"] = name;
            li["dctm_path"] = path;
            li["dctm_name"] = name;
            li["dctm_location"] = string.Format("{0}/{1}", path == "/" ? "" : path, name);
            li["dctm_author_name"] = (string)element.Element(ns + "creator");
            li["dctm_author"] = this.UserGroupImporter.ResolveUser(li.Context as ClientContext, (string)li["dctm_author_name"]);
            li["dctm_editor_name"] = (string)element.Element(ns + "modifier");
            li["dctm_editor"] = this.UserGroupImporter.ResolveUser(li.Context as ClientContext, (string)li["dctm_editor_name"]);
            li["dctm_object_id"] = (string)element.Element(ns + "id");
            li["dctm_acl_id"] = (string)element.Element(ns + "acl");
            ApplyAttributes(li, element.Element(ns + "attributes"), (string)element.Element(ns + "type"));
        }

        protected void SetAuthorAndEditor(ListItem li, XElement element)
        {
            XNamespace ns = element.GetDefaultNamespace();
            li["Created"] = Tools.ParseXmlDate(element.Element(ns + "creationDate"));
            li["Modified"] = Tools.ParseXmlDate(element.Element(ns + "modificationDate"));
            li["Author"] = li["dctm_author"];
            li["Editor"] = li["dctm_editor"];
        }

        protected void ApplyPermissions(ListItem li, XElement element)
        {
            this.PermissionsImporter.ApplyPermissions(li, (string)element.Element(element.GetDefaultNamespace() + "acl"));
        }

        protected void ClearPermissions(ListItem li)
        {
            while (li.RoleAssignments.Count > 0)
            {
                li.RoleAssignments[0].DeleteObject();
            }
        }

        protected void ApplyOwnerPermission(ListItem li, XElement element)
        {
            XNamespace ns = element.GetDefaultNamespace();
            this.PermissionsImporter.ApplyOwnerPermission(li, (string)element.Element(ns + "acl"), (string)element.Element(ns + "creator"));
        }

        protected ContentType ResolveContentType(string contentType)
        {
            return this.ContentTypeImporter.ResolveLibraryContentType(contentType).Type;
        }

        protected ContentType ResolveContentType(ContentTypeId id)
        {
            return this.ContentTypeImporter.ResolveContentType(id).Type;
        }
    }
    public class Crypt
    {
        private const string DEFAULT_KEY = @"6RBjZgfVO+KhuPU0qSqmdQ==";
        protected static readonly Encoding ENCODING = UTF8Encoding.UTF8;

        protected readonly byte[] Key;

        public Crypt() : this(null)
        {
        }

        public Crypt(byte[] key)
        {
            if (key == null || (key.Length != 16 && key.Length != 32))
            {
                this.Key = Convert.FromBase64String(DEFAULT_KEY);
            }
            else
            {
                this.Key = (byte[])key.Clone();
            }
        }

        protected virtual SymmetricAlgorithm GetCryptoAlgorithm()
        {
            SymmetricAlgorithm algorithm = new AesManaged();
            algorithm.Mode = CipherMode.ECB;
            algorithm.Padding = PaddingMode.PKCS7;
            algorithm.KeySize = (this.Key.Length * 8);
            algorithm.Key = this.Key;
            algorithm.BlockSize = 128;
            return algorithm;
        }

        public string Decrypt(string value)
        {
            if (value == null) throw new ArgumentNullException("Must provide a value to decrypt");
            byte[] data = null;
            try
            {
                data = Convert.FromBase64String(value);
            }
            catch (FormatException)
            {
                // Not Base-64, so return it verbatim
                return value;
            }

            using (ICryptoTransform crypto = GetCryptoAlgorithm().CreateDecryptor())
            {
                try
                {
                    return ENCODING.GetString(crypto.TransformFinalBlock(data, 0, data.Length));
                }
                catch (CryptographicException)
                {
                    // Can't decrypt, return the base value
                    return value;
                }
            }
        }

        public string Encrypt(string value)
        {
            if (value == null) value = string.Empty;
            using (ICryptoTransform crypto = GetCryptoAlgorithm().CreateEncryptor())
            {
                byte[] data = ENCODING.GetBytes(value);
                return Convert.ToBase64String(crypto.TransformFinalBlock(data, 0, data.Length));
            }
        }
    }
}