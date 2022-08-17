using Armedia.CMSMF.SharePoint.Common;
using log4net;
using Microsoft.SharePoint.Client;
using System;
using System.Collections.Generic;
using System.Threading.Tasks.Dataflow;
using System.Xml;
using System.Xml.Linq;

namespace Armedia.CMSMF.SharePoint.Import
{
    public class FolderInfo : FSObjectInfo
    {
        public readonly HashSet<string> Files;
        public string Url;
        public Folder SPObj;
        public bool Modified = false;
        public bool Exists = false;

        public FolderInfo(string location) : base(location)
        {
            this.Files = new HashSet<string>();
        }
    }

    public class FolderImporter : FSObjectImporter
    {
        protected static readonly ILog LOG = LogManager.GetLogger(typeof(FolderImporter));

        private void ProcessAccumulatedFolders(SharePointSession session, Dictionary<string, List<FolderInfo>> folders)
        {
            // This allows for the case where we're only creating indexes, and don't want to touch the repository
            if (session == null) return;
            ClientContext clientContext = session.ClientContext;
            Folder rootFolder = session.RootFolder;

            // We know all of these folders are at the same depth, so for all of these we only need
            // to find their parent folder(s), and add the new folders
            Web web = clientContext.Web;
            // Step 1: gather all the parent folders in this batch
            Dictionary<string, Folder> spFolders = new Dictionary<string, Folder>();
            List<FolderInfo> processed = new List<FolderInfo>();
            foreach (string parent in folders.Keys)
            {
                Folder f = null;
                if (parent == "")
                {
                    f = rootFolder;
                }
                else
                {
                    f = web.GetFolderByServerRelativeUrl(rootFolder.ServerRelativeUrl + parent);
                }
                clientContext.Load(f, r => r.ServerRelativeUrl);
                spFolders[parent] = f;
            }
            // Now get all the parent folders in a single query
            session.ExecuteQuery();

            // Step 2: go through all the parent folders, and create their children
            int batch = 0;
            foreach (string parent in folders.Keys)
            {
                Folder parentFolder = spFolders[parent];
                foreach (FolderInfo f in folders[parent])
                {
                    ProgressTracker tracker = new ProgressTracker(f.Location, this.Log);
                    if (tracker.Completed)
                    {
                        f.SPObj = parentFolder.Folders.GetByUrl(f.SafeName);
                        f.Exists = true;
                        Log.Info(string.Format("Loaded existing folder [{0}] from [{1}]", f.FullPath, f.SafeFullPath));
                    }
                    else
                    {
                        f.SPObj = parentFolder.Folders.Add(f.SafeName);
                        f.Modified = true;
                        Log.Info(string.Format("Added folder [{0}] for creation as [{1}]", f.FullPath, f.SafeFullPath));
                    }
                    clientContext.Load(f.SPObj, r => r.ServerRelativeUrl);
                    processed.Add(f);
                    // Do them in bunches of 50
                    if (++batch >= 50)
                    {
                        Log.Info(string.Format("Executing the creation of the last {0} folders", batch));
                        session.ExecuteQuery();
                        batch = 0;
                    }
                }
            }
            // Now, create all the children in a single query
            if (batch > 0)
            {
                Log.Info(string.Format("Executing the creation of the last {0} folders", batch));
                session.ExecuteQuery();
            }
            foreach (FolderInfo f in processed)
            {
                // Update the server relative URLs for each of them
                f.Url = f.SPObj.ServerRelativeUrl;
            }
        }

        private readonly Dictionary<string, FolderInfo> Folders;

        public FolderImporter(ContentTypeImporter contentTypeImporter, PermissionsImporter permissionsImporter) : this(contentTypeImporter?.ImportContext ?? permissionsImporter.ImportContext, contentTypeImporter, permissionsImporter)
        {
        }
        public FolderImporter(ImportContext importContext) : this(importContext, null, null)
        {
        }

        private FolderImporter(ImportContext importContext, ContentTypeImporter contentTypeImporter, PermissionsImporter permissionsImporter) : base("folders", importContext, contentTypeImporter, permissionsImporter)
        {
            this.Folders = new Dictionary<string, FolderInfo>();
            XmlReader foldersXml = this.ImportContext.LoadIndex("folders");
            if (foldersXml == null) return;
            using (foldersXml)
            {
                int currentDepth = 0;
                Dictionary<string, List<FolderInfo>> accumulated = new Dictionary<string, List<FolderInfo>>();
                int accumulatedCount = 0;

                using (ObjectPool<SharePointSession>.Ref sessionRef = this.ImportContext.SessionFactory?.GetSession())
                {
                    SharePointSession session = sessionRef?.Target;
                    outer: while (foldersXml.ReadToFollowing("folder"))
                    {
                        string location = null;
                        string path = null;
                        using (XmlReader folderXml = foldersXml.ReadSubtree())
                        {
                            // We're only interested in the <location> and <path> elements, so if they're not there
                            // we simply move on to the next folder
                            if (!folderXml.ReadToFollowing("path"))
                            {
                                goto outer;
                            }
                            path = "/" + folderXml.ReadElementContentAsString();
                            if (!folderXml.ReadToFollowing("location"))
                            {
                                goto outer;
                            }
                            location = folderXml.ReadElementContentAsString();
                        }

                        int thisDepth = (path == "/" ? 0 : thisDepth = Tools.CountChars(path, '/'));

                        // If we've changed depths, we process what we've accumulated so far
                        if (thisDepth > currentDepth)
                        {
                            if (session != null)
                            {
                                Log.Info(string.Format("Creating {0} folders in the target environment, depth {1}", accumulatedCount, thisDepth));
                                try
                                {
                                    ProcessAccumulatedFolders(session, accumulated);
                                }
                                catch (Exception e)
                                {
                                    Log.Error("Failed to process the current accumulated folder batch");
                                    throw e;
                                }
                            }
                            accumulated.Clear();
                            accumulatedCount = 0;
                            currentDepth = thisDepth;
                        }

                        // A new folder to handle...
                        FolderInfo f = new FolderInfo(this.ImportContext.FormatMetadataLocation(location));

                        List<FolderInfo> l = null;
                        if (!accumulated.ContainsKey(f.SafePath))
                        {
                            l = new List<FolderInfo>();
                            accumulated[f.SafePath] = l;
                        }
                        else
                        {
                            l = accumulated[f.SafePath];
                        }

                        /*
                        if (thisDepth == 0)
                        {
                            // Check to see if this is a cabinet we want to avoid
                            if (XmlConvert.ToBoolean(XmlTools.GetAttributeValue(xml, "caliente:is_private")))
                            {
                                Log.Info(string.Format("Skipping private cabinet [{0}]", f.FullPath));
                                continue;
                            }
                        }
                        */
                        l.Add(f);
                        accumulatedCount++;
                        this.Folders[f.FullPath] = f;
                    }
                    if ((session != null) && accumulatedCount > 0)
                    {
                        Log.Info(string.Format("Creating {0} folders in the target environment, depth {1}", accumulated.Count, currentDepth + 1));
                        try
                        {
                            ProcessAccumulatedFolders(session, accumulated);
                        }
                        catch (Exception e)
                        {
                            Log.Error("Failed to process the last accumulated folder batch");
                            throw e;
                        }
                    }
                }
            }
        }

        public FolderInfo ResolveFolder(string path)
        {
            if (this.Folders.ContainsKey(path))
            {
                return this.Folders[path];
            }
            return null;
        }

        private void ApplyMetadata(FolderInfo folder, ProgressTracker tracker)
        {
            // Apply the actual metadata to the folder, including permissions, attributes, etc.
            using (ObjectPool<SharePointSession>.Ref sessionRef = this.ImportContext.SessionFactory.GetSession())
            {
                SharePointSession session = sessionRef.Target;
                ClientContext clientContext = session.ClientContext;
                XElement xml = XElement.Load(folder.Location);
                XNamespace ns = xml.GetDefaultNamespace();
                Folder f = clientContext.Web.GetFolderByServerRelativeUrl(folder.Url);
                tracker.TrackProgress("Gathering metadata and permissions for folder at [{0}]", folder.SafeFullPath);
                ContentType contentType = ResolveContentType(folder.Type);
                if (contentType != null) f.ListItemAllFields["ContentTypeId"] = contentType.Id;

                FolderInfo parent = ResolveFolder(folder.Path);
                bool individualAcl = (parent == null || (folder.Acl != parent.Acl));
                ApplyMetadata(f.ListItemAllFields, xml);
                string aclResult = "inherited";
                if (individualAcl)
                {
                    aclResult = "independent";
                    f.ListItemAllFields.BreakRoleInheritance(false, false);
                    ApplyPermissions(f.ListItemAllFields, xml);
                }
                tracker.TrackProgress("The ACL for [{0}] will be {1} from its parent's", folder.SafeFullPath, aclResult);
                SetAuthorAndEditor(f.ListItemAllFields, xml);
                if (individualAcl) ApplyOwnerPermission(f.ListItemAllFields, xml);
                f.ListItemAllFields.Update();
                f.Update();
                tracker.TrackProgress("Applying metadata and permissions on folder at [{0}]", folder.SafeFullPath);
                session.ExecuteQuery();
                ShowProgress();
            }
        }

        private ICollection<FolderInfo> FinalizeFolders(ICollection<FolderInfo> pending, int importThreads)
        {
            List<FolderInfo> failed = new List<FolderInfo>();
            ActionBlock<FolderInfo> processor = new ActionBlock<FolderInfo>(folderInfo =>
            {
                if (this.Abort) return;
                Result r = Result.Skipped;
                try
                {
                    ProgressTracker tracker = new ProgressTracker(folderInfo.Location, this.Log);
                    if (!folderInfo.Modified && folderInfo.Exists && !tracker.Failed)
                    {
                        if (Log.IsDebugEnabled)
                        {
                            Log.Debug(string.Format("Skipping folder [{0}] - already completed", folderInfo.FullPath));
                        }
                        IncreaseProgress();
                        return;
                    }

                    r = Result.Failed;
                    Exception exc = null;
                    try
                    {
                        ApplyMetadata(folderInfo, tracker);
                        r = Result.Completed;
                        tracker.DeleteOutcomeMarker();
                    }
                    catch (Exception e)
                    {
                        exc = e;
                        lock (failed)
                        {
                            failed.Add(folderInfo);
                        }
                        Log.Error(string.Format("Failed to apply the metadata to the folder at [{0}]", folderInfo.SafeFullPath), e);
                    }
                    finally
                    {
                        IncreaseProgress();
                        tracker.SaveOutcomeMarker(r, exc);
                    }
                }
                finally
                {
                    IncrementCounter(r);
                }
            }, new ExecutionDataflowBlockOptions
            {
                // MaxDegreeOfParallelism sets the maximum number of parallel processes
                MaxDegreeOfParallelism = importThreads,
                // We're constrained to a single producer thread, so set this to true
                SingleProducerConstrained = true,
                // Accept these many in the queue before bouncing/blocking?
                // BoundedCapacity = 1000,
            });

            try
            {
                ResetProgress(this.Folders.Count);
                foreach (FolderInfo folder in pending)
                {
                    if (this.Abort) break;
                    processor.Post(folder);
                }
                return failed;
            }
            finally
            {
                processor.Complete();
                processor.Completion.Wait();
                Log.Info(this.ProcessingReport);
            }
        }

        protected ICollection<FolderInfo> IdentifyPending(ICollection<FolderInfo> failed)
        {
            ICollection<FolderInfo> pending = new List<FolderInfo>();
            foreach (FolderInfo info in this.Folders.Values)
            {
                if (this.Abort) break;
                if (info.Modified)
                {
                    pending.Add(info);
                }
                else
                {
                    IncrementCounter(Result.Skipped);
                }
            }
            return pending;
        }

        public void FinalizeFolders(int threads, int retries)
        {
            ICollection<FolderInfo> failed = new List<FolderInfo>();
            Log.Info("Checking existing status to identify which folders need processing");
            ICollection<FolderInfo> pending = IdentifyPending(failed);

            if (this.Abort) return;

            if (pending.Count == 0)
            {
                Log.Info(string.Format("No folders are in need of processing"));
                return;
            }

            ResetCounters();
            int attempt = 0;
            while (pending.Count > 0 && (attempt < (retries + 1)) && !this.Abort)
            {
                ResetCounter(Result.Failed);
                attempt++;
                Log.Info(string.Format("Identified {0} folders in need of processing (of which {1} are retries) (attempt #{2}/{3})", pending.Count, failed.Count, attempt, retries + 1));
                int lastPending = pending.Count;
                pending = FinalizeFolders(pending, threads);
                failed = pending;
                if (pending.Count < lastPending)
                {
                    Log.Info(string.Format("{0} folders were successfully processed in this attempt, with {1} failures", lastPending - pending.Count, pending.Count));
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
