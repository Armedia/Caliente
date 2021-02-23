using Armedia.CMSMF.SharePoint.Common;
using log4net;
using Microsoft.SharePoint.Client;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Threading.Tasks.Dataflow;
using System.Text.RegularExpressions;
using System.Xml;
using System.Xml.Linq;

namespace Armedia.CMSMF.SharePoint.Import
{

    public class UnsupportedDocumentException : Exception
    {
        public UnsupportedDocumentException(string msg, Exception e) : base(msg, e)
        {

        }
        public UnsupportedDocumentException(string msg) : base(msg)
        {

        }
    }

    public class DocumentImporter : FSObjectImporter
    {
        // This is a SharePoint boundary, and cannot be exceeded
        private const int MAX_UPLOAD_SIZE = (2047 * 1024 * 1024);

        public static readonly Regex EXTENSION_EXTRACTOR = new Regex("\\.([a-z0-9][a-z0-9_~-]*)$", RegexOptions.IgnoreCase);
        private static readonly byte[] CONTENT_FILLER = new byte[0];

        private const string LOCATION_INDEX_NAME = "cmfstate.document-locations.xml";

        public readonly FolderImporter FolderImporter;
        public readonly FormatResolver FormatResolver;
        private readonly Dictionary<string, DocumentLocation> Filenames;
        public enum SimulationMode
        {
            NONE,
            MISSING,
            SHORT,
            FULL
        }
        public enum LocationMode
        {
            FIRST,
            LAST,
            CURRENT
        }

        protected class DocumentInfo
        {
            public readonly string HistoryId;
            public readonly string XmlLocation;
            public readonly string Path;
            public readonly string Name;
            public readonly string SourcePath;
            public readonly ProgressTracker Tracker;

            public DocumentInfo(ILog log, string historyId, string path, string name, string xmlLocation)
            {
                this.HistoryId = historyId;
                this.Path = path;
                this.Name = name;
                this.SourcePath = string.Format("{0}/{1}", (path == "/" ? "" : path), name);
                this.XmlLocation = xmlLocation;
                this.Tracker = new ProgressTracker(xmlLocation, log);
            }
        }

        protected class DocumentLocation : IComparable<DocumentLocation>
        {
            public static string DEFAULT_ELEMENT_NAME = "document-location";
            public readonly string CurrentFullPath;
            public readonly string HistoryId;
            public readonly string Version;
            public readonly long VersionCount;
            public readonly string SourcePath;
            public readonly string SourceName;
            public readonly string Path;
            public readonly string Name;
            public readonly string FullPath;

            public bool Removed = false;

            private readonly FolderInfo _Parent;

            public bool HasParent
            {
                get
                {
                    return (this._Parent != null);
                }
                private set
                {
                    // Do nothing
                }
            }

            public FolderInfo Parent
            {
                get
                {
                    if (!this.HasParent) throw new Exception(string.Format("Did not resolve parent folder [{0}] during the initialization phase.  This is a bug.", this.SourcePath));
                    return this._Parent;
                }

                private set
                {
                    // Do nothing
                }
            }

            public DocumentLocation(string historyId, long totalVersions, string version, FolderInfo parent, string sourceName, string safeName, DocumentLocation current)
            {
                if (parent == null) throw new ArgumentNullException("parent");
                if (string.IsNullOrWhiteSpace(historyId)) throw new ArgumentException("historyId");
                if (string.IsNullOrWhiteSpace(sourceName)) throw new ArgumentException("sourceName");
                if (string.IsNullOrWhiteSpace(safeName)) throw new ArgumentException("safeName");
                if (string.IsNullOrWhiteSpace(version)) throw new ArgumentException("version");
                this.CurrentFullPath = current?.FullPath;
                this.HistoryId = historyId;
                this._Parent = parent;
                this.SourcePath = parent.FullPath;
                this.SourceName = sourceName;
                this.Path = parent.SafeFullPath;
                this.Name = safeName;
                this.Version = version;
                this.VersionCount = totalVersions;
                this.FullPath = string.Format("{0}/{1}", this.Path, this.Name);
            }

            public DocumentLocation(XmlReader xml, FolderImporter folderImporter) : this(XElement.Load(xml), folderImporter)
            {
            }

            public DocumentLocation(XElement xml, FolderImporter folderImporter)
            {
                if (xml == null) throw new ArgumentNullException("xml");
                if (folderImporter == null) throw new ArgumentNullException("folderImporter");
                XNamespace ns = xml.GetDefaultNamespace();
                this.HistoryId = (string)xml.Element(ns + "historyId");
                this.Version = (string)xml.Element(ns + "version");
                string vc = (string)xml.Element(ns + "versionCount");
                // If there is no counter (OK for older indexes), then we simply default to -1, since this "old"
                // version count won't be used - only the true count from the main document index will be used
                this.VersionCount = vc != null ? Convert.ToInt64(vc) : -1;
                this.SourcePath = (string)xml.Element(ns + "sourcePath");
                this.SourceName = (string)xml.Element(ns + "sourceName");
                this.Path = (string)xml.Element(ns + "path");
                this.Name = (string)xml.Element(ns + "name");
                this.FullPath = (string)xml.Element(ns + "fullPath");
                this._Parent = folderImporter.ResolveFolder(this.SourcePath);
            }

            public void ToXml(XmlWriter w, string elementName)
            {
                w.WriteStartElement(elementName);
                w.WriteStartElement("historyId");
                w.WriteString(this.HistoryId);
                w.WriteEndElement();
                w.WriteStartElement("versionCount");
                w.WriteString(Convert.ToString(this.VersionCount));
                w.WriteEndElement();
                w.WriteStartElement("version");
                w.WriteString(this.Version);
                w.WriteEndElement();
                w.WriteStartElement("sourcePath");
                w.WriteString(this.SourcePath);
                w.WriteEndElement();
                w.WriteStartElement("sourceName");
                w.WriteString(this.SourceName);
                w.WriteEndElement();
                w.WriteStartElement("path");
                w.WriteString(this.Path);
                w.WriteEndElement();
                w.WriteStartElement("name");
                w.WriteString(this.Name);
                w.WriteEndElement();
                w.WriteStartElement("fullPath");
                w.WriteString(this.FullPath);
                w.WriteEndElement();
                w.WriteEndElement();
                w.Flush();
            }

            public void ToXml(XmlWriter w)
            {
                ToXml(w, DEFAULT_ELEMENT_NAME);
            }

            public override string ToString()
            {
                return string.Format("[{0}:{1}]{2}", this.HistoryId, this.Version, this.FullPath);
            }

            public int CompareTo(DocumentLocation other)
            {
                if (other == null) return 1;
                if (ReferenceEquals(this, other)) return 0;
                int r = this.FullPath.CompareTo(other.FullPath);
                if (r != 0) return r;
                r = this.HistoryId.CompareTo(other.HistoryId);
                if (r != 0) return r;
                return 0;
            }
        }

        protected Dictionary<string, DocumentLocation> LoadNameMappings(FolderImporter folderImporter)
        {
            Dictionary<string, DocumentLocation> filenames = new Dictionary<string, DocumentLocation>();
            XmlReader documents = this.ImportContext.LoadOptionalIndex(LOCATION_INDEX_NAME);
            if (documents != null)
            {
                using (documents)
                {
                    while (documents.ReadToFollowing(DocumentLocation.DEFAULT_ELEMENT_NAME))
                    {
                        DocumentLocation location = new DocumentLocation(documents.ReadSubtree(), folderImporter);
                        filenames[location.HistoryId] = location;
                        // This is important: we need to preserve the list of files in each directory in order to
                        // properly defend against collisions
                        if (location.HasParent) location.Parent.Files.Add(location.Name);
                    }
                }
            }
            return filenames;
        }

        protected int SelectLocationInfo(XmlReader documentXml, ref string path, ref string name, ref string version, ref bool currentVersion, ref Format format, LocationMode locationMode)
        {
            int count = 0;
            while (documentXml.ReadToFollowing("version"))
            {
                using (XmlReader versionXml = documentXml.ReadSubtree())
                {
                    if (!versionXml.ReadToFollowing("path"))
                    {
                        continue;
                    }
                    path = "/" + versionXml.ReadElementContentAsString();

                    if (!versionXml.ReadToFollowing("name"))
                    {
                        continue;
                    }
                    name = versionXml.ReadElementContentAsString();

                    if (!versionXml.ReadToFollowing("version"))
                    {
                        continue;
                    }
                    version = versionXml.ReadElementContentAsString();

                    if (!versionXml.ReadToFollowing("current"))
                    {
                        continue;
                    }
                    currentVersion = XmlConvert.ToBoolean(versionXml.ReadElementContentAsString());

                    if (versionXml.ReadToFollowing("format"))
                    {
                        format = FormatResolver.ResolveFormat(versionXml.ReadElementContentAsString());
                    }
                }
                // If we're ok with using the first location, then we break now.  Otherwise, we keep going since we're
                // looking for the last one
                count++;
                bool breakLoop = false;
                switch (locationMode)
                {
                    case LocationMode.FIRST: breakLoop = true; break;
                    case LocationMode.CURRENT: breakLoop = currentVersion; break;
                    case LocationMode.LAST: breakLoop = false; break;
                }
                if (breakLoop) break;
            }
            return count;
        }

        public DocumentImporter(FolderImporter folderImporter, FormatResolver formatResolver, LocationMode locationMode, bool fixExtensions) : base("documents", folderImporter)
        {
            this.FolderImporter = folderImporter;
            this.FormatResolver = formatResolver;
            Dictionary<string, DocumentLocation> finalNames = LoadNameMappings(folderImporter);

            // We need to do two passes...

            // First pass: identify the incoming stuff, and remove them from their current parent folders
            if (finalNames.Count > 0)
            {
                using (XmlReader documentsXml = this.ImportContext.LoadIndex("documents"))
                {
                    while (documentsXml.ReadToFollowing("document"))
                    {
                        using (XmlReader documentXml = documentsXml.ReadSubtree())
                        {
                            if (!documentXml.ReadToFollowing("historyId"))
                            {
                                continue;
                            }
                            string historyId = documentXml.ReadElementContentAsString();

                            if (!documentXml.ReadToFollowing("count"))
                            {
                                continue;
                            }
                            long total = documentXml.ReadElementContentAsLong();

                            // If this is a new object, we simply skip it since we don't need to remove it
                            if (!finalNames.ContainsKey(historyId)) continue;
                            DocumentLocation current = finalNames[historyId];

                            // We have an object, so we have to identify the name and path that it will carry
                            // and compare that the old one, to determine if it needs to be moved or renamed
                            string path = null;
                            string name = null;
                            string version = null;
                            bool currentVersion = false;
                            Format format = null;
                            long count = SelectLocationInfo(documentXml, ref path, ref name, ref version, ref currentVersion, ref format, locationMode);
                            // If we've harvested no versions, then we simply loop back up
                            if (count < 1) continue;

                            // If we have fixing to do, we do it
                            if (fixExtensions)
                            {
                                // Does it already have a valid extension? The extension is everything after the last dot...
                                if ((format != null) && !string.IsNullOrWhiteSpace(format.DosExtension) && string.IsNullOrWhiteSpace(EXTENSION_EXTRACTOR.Match(name).Groups[1].Value))
                                {
                                    name = string.Format("{0}.{1}", name, format.DosExtension);
                                }
                            }

                            // Now we need to identify if its current location and its new location match... if they do, then there's no need
                            // to touch it
                            if (current.SourcePath == path && current.SourceName == name) continue;

                            // We have a change, so we must remove this file from its parent since we don't (yet) know what the final state of things
                            // should be
                            current.Parent.Files.Remove(current.Name);
                            current.Removed = true;
                        }
                    }
                }
            }

            // Second pass: at this stage, the parent folders have been cleansed of files that are about
            // to be re-ingested with differing names, so any new filename collisions will be calculated correctly,
            // and files that are being re-ingested but didn't change location will be preserved in the same place
            using (XmlReader documentsXml = this.ImportContext.LoadIndex("documents"))
            {
                while (documentsXml.ReadToFollowing("document"))
                {
                    using (XmlReader documentXml = documentsXml.ReadSubtree())
                    {
                        if (!documentXml.ReadToFollowing("historyId"))
                        {
                            continue;
                        }
                        string historyId = documentXml.ReadElementContentAsString();

                        if (!documentXml.ReadToFollowing("count"))
                        {
                            continue;
                        }
                        long total = documentXml.ReadElementContentAsLong();

                        string path = null;
                        string name = null;
                        string version = null;
                        bool currentVersion = false;
                        Format format = null;
                        long count = SelectLocationInfo(documentXml, ref path, ref name, ref version, ref currentVersion, ref format, locationMode);
                        // If we've harvested no versions, then we simply loop back up
                        if (count < 1) continue;

                        DocumentLocation current = null;
                        if (finalNames.ContainsKey(historyId)) current = finalNames[historyId];

                        // If we have fixing to do, we do it
                        if (fixExtensions)
                        {
                            // Does it already have a valid extension? The extension is everything after the last dot...
                            if ((format != null) && !string.IsNullOrWhiteSpace(format.DosExtension) && string.IsNullOrWhiteSpace(EXTENSION_EXTRACTOR.Match(name).Groups[1].Value))
                            {
                                name = string.Format("{0}.{1}", name, format.DosExtension);
                            }
                        }

                        FolderInfo parent = folderImporter.ResolveFolder(path);
                        string safeName = null;
                        if (current == null || current.Removed)
                        {
                            // If there's no current location (a new file), or the location has changed (marked as such previously), then
                            // we create a new location object
                            safeName = Tools.MakeSafeFileName(name, null);
                            if (string.IsNullOrWhiteSpace(safeName) || !parent.Files.Add(safeName))
                            {
                                // Duplicate filename, so we modify the name and make sure it's used later
                                safeName = Tools.MakeSafeFileName(name, historyId); // new safe name
                                name = safeName;
                                parent.Files.Add(safeName);
                            }
                        }
                        else
                        {
                            // The name remains the same, and the file wasn't removed, so no need to add it to the parent
                            safeName = current.Name;
                        }
                        finalNames[historyId] = new DocumentLocation(historyId, total, version, parent, name, safeName, current);
                    }
                }
            }
            this.Filenames = finalNames;
        }

        public void StoreLocationIndex()
        {
            if (this.Filenames.Count > 0)
            {
                using (XmlWriter w = this.ImportContext.CreateIndex(LOCATION_INDEX_NAME, "document-locations"))
                {
                    foreach (DocumentLocation location in this.Filenames.Values)
                    {
                        location.ToXml(w);
                    }
                    w.Flush();
                }
            }
        }

        private void StoreDocument(string documentLocation, ProgressTracker tracker, SimulationMode simulationMode, LocationMode locationMode, bool autoPublish)
        {
            using (ObjectPool<SharePointSession>.Ref sessionRef = this.ImportContext.SessionFactory.GetSession())
            {
                SharePointSession session = sessionRef.Target;
                tracker.TrackProgress("Session [{0}] processing [{1}]...", session.Id, documentLocation);
                ClientContext clientContext = session.ClientContext;
                int minorCount = 0;
                const int maxMinor = 250;
                using (XmlReader document = XmlReader.Create(documentLocation))
                {
                    File newVersion = null;
                    DocumentLocation location = null;
                    string previousAcl = null;
                    ContentType contentType = null;
                    int versionCount = 0;
                    string comment = null;
                    string restoreSpVersionLabel = null;
                    string restoreVersionNumber = null;
                    string versionNumber = null;
                    string safeFullPath = null;
                    while (document.ReadToFollowing("version") && !this.Abort)
                    {
                        // Ok...so now we have the document version.
                        XElement version = XElement.Load(document.ReadSubtree());
                        XNamespace ns = version.GetDefaultNamespace();

                        string id = (string)version.Element(ns + "id");
                        string objectType = (string)version.Element(ns + "type");
                        string aclId = (string)version.Element(ns + "acl");

                        if (location == null)
                        {
                            string historyId = (string)version.Element(ns + "historyId");
                            location = this.Filenames[historyId];
                            previousAcl = location.Parent.Acl;
                            location.Parent.Modified = true;
                        }

                        string safeName = location.Name;
                        versionNumber = (string)version.Element(ns + "version");
                        string path = (string)version.Element(ns + "sourcePath");
                        string name = Tools.SanitizeSingleLineString((string)version.Element(ns + "name"));
                        string fullName = string.Format("{0}/{1}", path, name);

                        tracker.TrackProgress("Processing version [{0}] for document [{1}] (#{2:N0} of {3:N0})", versionNumber, location.FullPath, ++versionCount, location.VersionCount);

                        // Step 1: if this is a branch - skip it!!
                        if (Tools.CountChars(versionNumber, '.') > 1)
                        {
                            // BRANCH!! Must be skipped
                            tracker.TrackProgress("Skipping branch version [{0}] for document [{1}]", versionNumber, fullName);
                            continue;
                        }

                        // Prepare the content stream
                        XElement allContents = version.Element(ns + "contents");
                        XElement firstContent = (allContents != null ? allContents.Element(ns + "content") : null);
                        System.IO.Stream stream = null;
                        long contentStreamSize = 0;
                        switch (simulationMode)
                        {
                            case SimulationMode.NONE:
                            case SimulationMode.MISSING:
                                if (firstContent == null)
                                {
                                    // Allow objects with no content streams to be uploaded as such
                                    stream = new System.IO.MemoryStream(CONTENT_FILLER);
                                    break;
                                }
                                string contentStreamLocation = this.ImportContext.FormatContentStreamLocation((string)firstContent.Element(ns + "location"));
                                contentStreamSize = XmlConvert.ToInt64((string)firstContent.Element(ns + "size"));
                                if (contentStreamSize == 0)
                                {
                                    // Short-circuit the case of the empty stream
                                    stream = new System.IO.MemoryStream(CONTENT_FILLER);
                                    break;
                                }
                                System.IO.FileInfo streamInfo = new System.IO.FileInfo(contentStreamLocation);
                                if (streamInfo.Exists)
                                {
                                    if (streamInfo.Length != contentStreamSize)
                                    {
                                        Log.Warn(string.Format("Stream size mismatch for [{0}] v{1} - expected {2:N0} bytes, but the actual stream is {3:N0} bytes", fullName, versionNumber, contentStreamSize, streamInfo.Length));
                                        contentStreamSize = streamInfo.Length;
                                    }

                                    if (contentStreamSize > MAX_UPLOAD_SIZE)
                                    {
                                        throw new UnsupportedDocumentException(string.Format("Maximum upload size for SharePoint is {0:N0} bytes, but the stream for [{1}] v{2} is {3:N0} bytes long ({4:N0} bytes too big) (described by [{5}])", MAX_UPLOAD_SIZE, fullName, versionNumber, contentStreamSize, contentStreamSize - MAX_UPLOAD_SIZE, documentLocation));
                                    }

                                    stream = streamInfo.OpenRead();
                                    if (stream != null) break;
                                }
                                if (simulationMode == SimulationMode.MISSING)
                                {
                                    // We're in "MISSING" mode, so we HAVE to switch over to FULL mode because we found no stream, but we need to simulate the content anyhow
                                    simulationMode = SimulationMode.FULL;
                                    break;
                                }
                                throw new Exception(string.Format("Could not locate the content stream at [{0}] for document [{1}] version [{2}] (described by [{3}])", contentStreamLocation, fullName, versionNumber, documentLocation));
                        }

                        // We split the switch into two, instead of just a single big one, because the simulation mode may change
                        // depending on the circumstances...
                        switch (simulationMode)
                        {
                            case SimulationMode.FULL:
                            case SimulationMode.SHORT:
                                if (contentStreamSize > MAX_UPLOAD_SIZE)
                                {
                                    // If the stream is too big, and we're simulating, we simply bow out gracefully, and treat it as completed
                                    throw new UnsupportedDocumentException(string.Format("Maximum upload size for SharePoint is {0:N0} bytes, but the stream for [{1}] v{2} is {3:N0} bytes long ({4:N0} bytes too big) (described by [{5}])", MAX_UPLOAD_SIZE, fullName, versionNumber, contentStreamSize, contentStreamSize - MAX_UPLOAD_SIZE, documentLocation));
                                }
                                if (contentStreamSize == 0)
                                {
                                    // Short-circuit the case of the empty stream
                                    stream = new System.IO.MemoryStream(CONTENT_FILLER);
                                    break;
                                }
                                using (System.IO.MemoryStream ms = new System.IO.MemoryStream())
                                {
                                    System.IO.StreamWriter sw = new System.IO.StreamWriter(ms);
                                    // Ok...start dumping out keywords, titles, attributes, etc.
                                    sw.WriteLine(name);
                                    sw.WriteLine(path);
                                    sw.WriteLine(versionNumber);
                                    foreach (string kw in XmlTools.GetAttributeValues(version, "dctm:keywords"))
                                    {
                                        sw.WriteLine(kw);
                                    }
                                    sw.WriteLine();
                                    sw.Write(version.ToString());
                                    sw.WriteLine();
                                    byte[] randomData = new byte[100 * 1024];
                                    new Random(Environment.TickCount).NextBytes(randomData);
                                    // We do it like this because we need it to continue to be textual data, else it won't be parsed
                                    sw.WriteLine(Convert.ToBase64String(randomData));
                                    sw.WriteLine();
                                    sw.WriteLine();
                                    sw.Flush();
                                    ms.Flush();

                                    // We do this instead of using the memory stream directly because we don't want to necessarily
                                    // have to generate the entire size in bytes-worth of simulated content.  This way we just
                                    // generate a "seed" that then gets repeated and rehashed and whatnot (as well as mixed with random
                                    // data) in order to complete the requisite size in bytes.
                                    if (simulationMode == SimulationMode.SHORT) contentStreamSize = Math.Min(contentStreamSize, ms.Length);
                                    stream = ContentSimulator.SimulateStream(contentStreamSize, ms.ToArray());
                                }
                                break;
                        }

                        if (newVersion != null)
                        {
                            if ((++minorCount % maxMinor) == 0)
                            {
                                tracker.TrackProgress("Publishing a new major version ({0} minor versions checked in) for document [{1}] (from [{2}])", minorCount, fullName, documentLocation);
                                newVersion.Publish("");
                                session.ExecuteQuery();
                            }

                            // ALWAYS execute this load
                            clientContext.Load(newVersion, r => r.UIVersionLabel, r => r.MajorVersion, r => r.MinorVersion, r => r.CheckOutType, r => r.ListItemAllFields);
                            session.ExecuteQuery();
                            ShowProgress();
                        }

                        // Step 2: Obtain the parent folder

                        // Step 3: Identify if this is the root version
                        bool checkedOut = false;
                        FileCreationInformation fileCreationInfo = null;
                        FileSaveBinaryInformation fileSaveBinaryInfo = null;
                        if (newVersion == null)
                        {
                            // TODO: Handle the case when the root object doesn't start at version 0.1, which is
                            // where Sharepoint MUST start.  Thus, if there's a difference, we need to account for it
                            // by deducing how many major/minor versions we must pad with until we can commit the first
                            // version

                            // This is the root!
                            fileCreationInfo = new FileCreationInformation();
                        }
                        else
                        {
                            // This isn't the root, so we must check out the previous version
                            fileSaveBinaryInfo = new FileSaveBinaryInformation();
                            newVersion.CheckOut();
                            checkedOut = true;
                        }

                        // Finally, if this is a checked out version, do the checkin
                        comment = XmlTools.GetAttributeValue(version, "cmis:checkinComment") ?? "";
                        safeFullPath = location.FullPath;
                        string sourcePath = string.Format("{0}/{1}", path, name);
                        string targetUrl = session.GetServerRelativeUrl(location.FullPath);

                        string acl = (string)version.Element(ns + "acl");
                        bool restoreAcl = (acl != previousAcl && acl == location.Parent.Acl);
                        bool uniqueAcl = (acl != previousAcl && acl != location.Parent.Acl);
                        bool breakRoleInheritance = (uniqueAcl && previousAcl == location.Parent.Acl);

                        using (stream)
                        {
                            if (checkedOut)
                            {
                                fileSaveBinaryInfo.ContentStream = stream;
                                newVersion.SaveBinary(fileSaveBinaryInfo);
                                newVersion.CheckIn(comment, CheckinType.MinorCheckIn);
                                tracker.TrackProgress("Checking in new version [{0}] for document [{1}] (at [{2}]) - {3:N0} bytes", versionNumber, sourcePath, safeFullPath, contentStreamSize);
                                session.ExecuteQuery();
                                ShowProgress();

                                // This load operation is required in order for the metadata creation to work
                                clientContext.Load(newVersion, r => r.UIVersionLabel, r => r.MajorVersion, r => r.MinorVersion, r => r.CheckOutType, r => r.ListItemAllFields, r => r.ListItemAllFields.RoleAssignments);
                            }
                            else
                            {
                                fileCreationInfo.Url = safeName;
                                fileCreationInfo.Overwrite = true;
                                fileCreationInfo.ContentStream = stream;

                                string currentUrl = targetUrl;
                                if (location.CurrentFullPath != null)
                                {
                                    currentUrl = session.GetServerRelativeUrl(location.CurrentFullPath);
                                    if (currentUrl != targetUrl) clientContext.Web.GetFileByServerRelativeUrl(currentUrl).DeleteObject();
                                }
                                clientContext.Web.GetFileByServerRelativeUrl(targetUrl).DeleteObject();
                                tracker.TrackProgress("Clearing out the existing document at [{0}] to make way for [{1}]", currentUrl, targetUrl);
                                session.ExecuteQuery();

                                Folder f = clientContext.Web.GetFolderByServerRelativeUrl(location.Parent.Url);
                                newVersion = f.Files.Add(fileCreationInfo);
                                clientContext.Load(newVersion, r => r.UIVersionLabel, r => r.MajorVersion, r => r.MinorVersion, r => r.CheckOutType, r => r.ListItemAllFields, r => r.ListItemAllFields.RoleAssignments);
                                tracker.TrackProgress("Creating document [{0}] as [{1}] - {2:N0} bytes", sourcePath, safeFullPath, contentStreamSize);
                            }

                            session.ExecuteQuery();
                            ShowProgress();

                            if (newVersion.CheckOutType == CheckOutType.None)
                            {
                                newVersion.CheckOut();
                            }
                            if (contentType == null)
                            {
                                contentType = ResolveContentType(objectType);
                                tracker.TrackProgress("Assigning content type [{0}] (id={1}) to [{2}] (GUID=[{3}] UniqueId=[{4}])...", contentType.Name, contentType.Id, safeFullPath, newVersion.ListItemAllFields["UniqueId"], newVersion.ListItemAllFields["GUID"]);
                            }
                            newVersion.ListItemAllFields["ContentTypeId"] = contentType.Id;
                            ApplyMetadata(newVersion.ListItemAllFields, version);

                            string aclResult = "inherited from its parent's";
                            if (restoreAcl)
                            {
                                newVersion.ListItemAllFields.ResetRoleInheritance();
                                aclResult = "re-inherited from its parent's";
                            }
                            else
                            if (uniqueAcl)
                            {
                                if (breakRoleInheritance)
                                {
                                    newVersion.ListItemAllFields.BreakRoleInheritance(false, false);
                                    aclResult = "independent from its parent's";
                                }
                                else
                                {
                                    aclResult = "independent from its previous version's and from its parent's";
                                    ClearPermissions(newVersion.ListItemAllFields);
                                }
                                ApplyPermissions(newVersion.ListItemAllFields, version);
                            }
                            else
                            if (previousAcl != location.Parent.Acl)
                            {
                                aclResult = "preserved from the previous version";
                            }
                            tracker.TrackProgress("The ACL for [{0}] v{1} will be {2}", safeFullPath, versionNumber, aclResult);

                            newVersion.ListItemAllFields["dctm_antecedent_id"] = (string)version.Element(ns + "antecedentId");
                            newVersion.ListItemAllFields["dctm_history_id"] = location.HistoryId;
                            newVersion.ListItemAllFields["dctm_version"] = versionNumber;
                            newVersion.ListItemAllFields["dctm_current"] = XmlConvert.ToBoolean((string)version.Element(ns + "current"));
                            newVersion.ListItemAllFields.Update();
                            newVersion.CheckIn(comment, CheckinType.OverwriteCheckIn);
                            session.ExecuteQuery();
                            ShowProgress();

                            clientContext.Load(newVersion);
                            clientContext.Load(newVersion.ListItemAllFields);
                            session.ExecuteQuery();
                            ShowProgress();

                            newVersion.CheckOut();
                            SetAuthorAndEditor(newVersion.ListItemAllFields, version);
                            if (uniqueAcl) ApplyOwnerPermission(newVersion.ListItemAllFields, version);

                            newVersion.ListItemAllFields.Update();
                            newVersion.CheckIn(comment, CheckinType.OverwriteCheckIn);
                            newVersion.RefreshLoad();
                            clientContext.Load(newVersion.Versions);
                            session.ExecuteQuery();
                            ShowProgress();
                            previousAcl = acl;

                            if (XmlConvert.ToBoolean((string)version.Element(ns + "current")))
                            {
                                // This is the current version, so mark it
                                restoreVersionNumber = versionNumber;
                                restoreSpVersionLabel = newVersion.UIVersionLabel;
                            }
                        }
                    }

                    if (!this.Abort && newVersion != null)
                    {
                        // If we have a version number to restore that isn't the last version checked in, then we restore it to that
                        // version
                        if (locationMode == LocationMode.CURRENT && (restoreVersionNumber != null) && (restoreVersionNumber != versionNumber))
                        {
                            newVersion.CheckOut();
                            newVersion.Versions.RestoreByLabel(restoreSpVersionLabel);
                            newVersion.CheckIn(string.Format("Restored to version {0}", restoreVersionNumber), CheckinType.MinorCheckIn);
                            session.ExecuteQuery();
                            tracker.TrackProgress("Restored document [{0}] to older version [{1}] (sp version {2})", safeFullPath, restoreVersionNumber, restoreSpVersionLabel);
                        }

                        // We only do two attempts
                        for (int i = 0 ; i < 2 ; i++)
                        {
                            clientContext.Load(newVersion);
                            clientContext.Load(newVersion.ListItemAllFields);
                            session.ExecuteQuery();

                            ContentTypeId finalId = newVersion.ListItemAllFields["ContentTypeId"] as ContentTypeId;
                            if (finalId.StringValue == contentType.Id.StringValue)
                            {
                                tracker.TrackProgress("Confirmed content type [{0}] is set for [{1}] upon import completion", contentType.Name, safeFullPath);
                                break;
                            }
                            else if (i > 0)
                            {
                                // If on the second check we failed, we don't try to set it again, and we simply explode accordingly
                                ContentType actual = this.ContentTypeImporter.ResolveContentType(finalId).Type;
                                throw new Exception(string.Format("Failed to re-set the content type for [{0}] - expected to set [{1}] with ID={2} but was actually set as [{3}] with ID={4} (GUID=[{5}] UniqueId=[{6}])", safeFullPath, contentType.Name, contentType.Id, actual.Name, finalId));
                            }

                            // One last attempt...try to set the type...
                            if (newVersion.CheckOutType == CheckOutType.None) newVersion.CheckOut();
                            newVersion.ListItemAllFields["ContentTypeId"] = contentType.Id;
                            newVersion.ListItemAllFields.Update();
                            newVersion.CheckIn(comment, CheckinType.OverwriteCheckIn);
                            session.ExecuteQuery();
                            ShowProgress();
                        }

                        if (autoPublish)
                        {
                            newVersion.Publish(comment);
                            session.ExecuteQuery();
                            ShowProgress();
                            tracker.TrackProgress("Automatically published document [{0}]", safeFullPath);
                        }
                    }
                }
            }
        }

        private ICollection<DocumentInfo> IngestDocuments(ICollection<DocumentInfo> documents, int threads, SimulationMode simulationMode, LocationMode locationMode, bool autoPublish)
        {
            // Reference: http://blog.repsaj.nl/index.php/2015/09/o365-parallel-processing-and-csom-friends-or-foes/
            List<DocumentInfo> failures = new List<DocumentInfo>();
            ActionBlock<DocumentInfo> ingestor = new ActionBlock<DocumentInfo>(docInfo =>
            {
                if (this.Abort) return;
                Result r = Result.Failed;
                try
                {
                    r = Result.Failed;
                    Exception exc = null;
                    bool markIgnored = false;
                    try
                    {
                        StoreDocument(docInfo.XmlLocation, docInfo.Tracker, simulationMode, locationMode, autoPublish);
                        docInfo.Tracker.DeleteOutcomeMarker();
                        r = Result.Completed;
                    }
                    catch (Exception e)
                    {
                        if (e is UnsupportedDocumentException)
                        {
                            markIgnored = true;
                        }
                        else
                        {
                            lock (failures)
                            {
                                failures.Add(docInfo);
                            }
                        }
                        exc = e;
                        Log.Error(string.Format("Failed to import the document history for [{0}] described by [{1}]", docInfo.SourcePath, docInfo.XmlLocation), e);
                    }
                    finally
                    {
                        docInfo.Tracker.SaveOutcomeMarker(r, exc, markIgnored);
                        IncreaseProgress();
                    }
                }
                finally
                {
                    IncrementCounter(r);
                }
            }, new ExecutionDataflowBlockOptions
            {
                // MaxDegreeOfParallelism sets the maximum number of parallel processes
                MaxDegreeOfParallelism = threads,
                // We're constrained to a single producer thread, so set this to true
                SingleProducerConstrained = true,
                // Accept these many in the queue before bouncing/blocking?
                // BoundedCapacity = 1000,
            });

            try
            {
                ResetProgress(documents.Count);
                foreach (DocumentInfo docInfo in documents)
                {
                    if (this.Abort) break;
                    ingestor.Post(docInfo);
                }
                return failures;
            }
            finally
            {
                ingestor.Complete();
                ingestor.Completion.Wait();
                Log.Info(this.ProcessingReport);
            }
        }

        protected ICollection<DocumentInfo> IdentifyPending(ICollection<DocumentInfo> failed)
        {
            ICollection<DocumentInfo> pending = new List<DocumentInfo>();
            using (XmlReader documents = this.ImportContext.LoadIndex("documents"))
            {
                HashSet<string> histories = new HashSet<string>();
                while (documents.ReadToFollowing("document") && !this.Abort)
                {
                    string path = null;
                    string name = null;
                    string location = null;
                    string historyId = null;
                    using (XmlReader document = documents.ReadSubtree())
                    {
                        if (!document.ReadToFollowing("path"))
                        {
                            continue;
                        }
                        path = document.ReadElementContentAsString();

                        if (!document.ReadToFollowing("name"))
                        {
                            continue;
                        }
                        name = document.ReadElementContentAsString();

                        if (!document.ReadToFollowing("location"))
                        {
                            continue;
                        }
                        location = document.ReadElementContentAsString();

                        if (!document.ReadToFollowing("historyId"))
                        {
                            continue;
                        }
                        historyId = document.ReadElementContentAsString();
                    }

                    if (!histories.Add(historyId))
                    {
                        // Already added
                        continue;
                    }

                    DocumentInfo docInfo = new DocumentInfo(this.Log, historyId, path, name, this.ImportContext.FormatMetadataLocation(location));
                    if (docInfo.Tracker.Completed)
                    {
                        Log.Debug(string.Format("Skipping file [{0}] - already completed", docInfo.SourcePath));
                        IncrementCounter(Result.Skipped);
                    }
                    else if (docInfo.Tracker.Ignored)
                    {
                        Log.Debug(string.Format("Skipping file [{0}] - marked as ignorable", docInfo.SourcePath));
                        IncrementCounter(Result.Skipped);
                    }
                    else
                    {
                        if (docInfo.Tracker.Failed)
                        {
                            failed.Add(docInfo);
                        }
                        pending.Add(docInfo);
                    }
                }
                histories.Clear();
            }
            return pending;
        }

        public void StoreDocuments(int threads, SimulationMode simulationMode, LocationMode locationMode, bool autoPublish, int retries)
        {
            ICollection<DocumentInfo> failed = new List<DocumentInfo>();
            Log.Info("Checking existing status to identify which documents need processing");
            ICollection<DocumentInfo> pending = IdentifyPending(failed);

            if (this.Abort) return;

            if (pending.Count == 0)
            {
                Log.Info(string.Format("No documents are in need of processing"));
                return;
            }

            ResetCounters();
            int attempt = 0;
            while (pending.Count > 0 && (attempt < (retries + 1)) && !this.Abort)
            {
                ResetCounter(Result.Failed);
                attempt++;
                Log.Info(string.Format("Identified {0} documents in need of processing (of which {1} are retries) (attempt #{2}/{3})", pending.Count, failed.Count, attempt, retries + 1));
                int lastPending = pending.Count;
                pending = IngestDocuments(pending, threads, simulationMode, locationMode, autoPublish);
                failed = pending;
                if (pending.Count < lastPending)
                {
                    Log.Info(string.Format("{0} files were processed in this attempt, with {1} retryable failures", lastPending - pending.Count, pending.Count));
                    attempt = 0;
                }
                else
                {
                    Log.Info(string.Format("No change in the data set - {0} were pending, and {1} failed", lastPending, pending.Count));
                }
            }
        }
    }
}