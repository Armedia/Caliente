using Caliente.SharePoint.Common;
using log4net;
using Microsoft.IdentityModel.Tokens;
using Microsoft.SharePoint.Client;
using System;
using System.Collections.Generic;
using System.Threading.Tasks.Dataflow;
using System.Xml;
using System.Xml.Linq;

namespace Caliente.SharePoint.Import
{
    public class FolderInfo : FSObjectInfo
    {
        public readonly HashSet<string> Files;
        public string Url;
        public Folder SPObj;
        public bool Modified = false;
        public bool Exists = false;

        public FolderInfo(string location, string relativeLocation) : base(location, relativeLocation)
        {
            this.Files = new HashSet<string>();
        }
    }

    public class FolderImporter : FSObjectImporter
    {
        protected static readonly ILog LOG = LogManager.GetLogger(typeof(FolderImporter));

        private readonly bool OrphanAclInherit;

        private void ProcessAccumulatedFolders(ImportContext importContext, SharePointSession session, Dictionary<string, List<FolderInfo>> parentFolders)
        {
            // This allows for the case where we're only creating indexes, and don't want to touch the repository
            if (session == null) return;
            ClientContext clientContext = session.ClientContext;
            Folder rootFolder = session.RootFolder;

            // We know all of these folders are at the same depth, so for all of these we only need
            // to find their parent folder(s), and add the new folders
            Web web = clientContext.Web;
            // Step 1: gather all the parent folders in this batch
            Dictionary<string, Folder> parentSPObjects = new Dictionary<string, Folder>();
            // Split the fetching of the parent folders into batches of 50, to be safe
            int parentBatch = 0;
            foreach (string parent in parentFolders.Keys)
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
                parentSPObjects[parent] = f;

                if (++parentBatch >= 50)
                {
                    // Now get all the accumulated parent folders in a single query.
                    Log.InfoFormat("Fetching the next {0} parent folders for this level", parentBatch);
                    session.ExecuteQuery();
                    parentBatch = 0;
                }
            }

            if (parentBatch > 0)
            {
                // We have parents to fetch, so fetch them
                Log.InfoFormat("Fetching the last {0} parent folders for this level", parentBatch);
                session.ExecuteQuery();
            }

            // Step 2: go through all the parent folders, and create their children
            Dictionary<string, ProgressTracker> trackers = new Dictionary<string, ProgressTracker>();
            List<List<FolderInfo>> batches = new List<List<FolderInfo>>();
            Action<List<FolderInfo>> processBatch = (batch) =>
            {
                if ((batch == null) || batch.IsNullOrEmpty()) return;
                Log.InfoFormat("Executing the query for the creation of the last {0} folders", batch.Count);
                session.ExecuteQuery();
                batches.Add(batch);
            };

            List<FolderInfo> processed = new List<FolderInfo>();
            List<FolderInfo> currentBatch = null;
            foreach (string parent in parentFolders.Keys)
            {
                Folder parentFolder = parentSPObjects[parent];
                foreach (FolderInfo f in parentFolders[parent])
                {
                    if (currentBatch == null) currentBatch = new List<FolderInfo>();
                    ProgressTracker tracker = new ProgressTracker(importContext.FormatMetadataLocation(f.RelativeLocation), importContext.FormatProgressLocation(f.RelativeLocation), this.Log);
                    trackers[f.Id] = tracker;
                    if (tracker.Completed)
                    {
                        f.SPObj = parentFolder.Folders.GetByUrl(f.SafeName);
                        f.Exists = true;
                        Log.InfoFormat("Loaded existing folder [{0}] from [{1}]", f.FullPath, f.SafeFullPath);
                    }
                    else
                    {
                        f.SPObj = parentFolder.Folders.Add(f.SafeName);
                        f.Modified = true;
                        Log.InfoFormat("Added folder [{0}] for creation as [{1}]", f.FullPath, f.SafeFullPath);
                    }
                    clientContext.Load(f.SPObj, r => r.ServerRelativeUrl);
                    currentBatch.Add(f);
                    processed.Add(f);

                    // Do them in bunches of 50 to reduce the number of queries
                    if (currentBatch.Count >= 50)
                    {
                        processBatch(currentBatch);
                        currentBatch = null;
                    }
                }
            }

            // This is the remainder in the last batch, so handle it
            processBatch(currentBatch);

            // Update the url
            processed.ForEach(f => f.Url = f.SPObj.ServerRelativeUrl);

            // TODO: Decide if this is the right time and place to ApplyMetadata, using the captured batches for
            // this depth level since we know that a) all parents already must exist, and b) the folders are empty.
            //
            // This might be important in the event that ACLs need to be split out b/c there's a limitation associated
            // with how many objects a folder tree contains: can't split the ACL if the tree contains more than 100K objects,
            // so if we need to split the ACL, we should do it ASAP - doubly so when the folders are freshly created
            // and thus empty.
            //
            // TODO: Not yet enabled b/c we're not sure it's the right thing to do just yet
            /*
            foreach (List<FolderInfo> batch in batches)
            {
                foreach (FolderInfo folder in batch)
                {
                    ApplyMetadata(folder, trackers[folder.Id], false);
                }
                session.ExecuteQuery();
            }
            */
        }

        private readonly Dictionary<string, FolderInfo> Folders;
        private readonly ContentType FallbackType;

        public FolderImporter(ContentTypeImporter contentTypeImporter, PermissionsImporter permissionsImporter, string fallbackType, bool orphanAclInherit) : this(contentTypeImporter?.ImportContext ?? permissionsImporter.ImportContext, contentTypeImporter, permissionsImporter, fallbackType, orphanAclInherit)
        {
        }

        public FolderImporter(ImportContext importContext, string fallbackType, bool orphanAclInherit) : this(importContext, null, null, fallbackType, orphanAclInherit)
        {
        }

        private FolderImporter(ImportContext importContext, ContentTypeImporter contentTypeImporter, PermissionsImporter permissionsImporter, string fallbackType, bool orphanAclInherit) : base("folders", importContext, contentTypeImporter, permissionsImporter)
        {
            this.OrphanAclInherit = orphanAclInherit;
            this.Folders = new Dictionary<string, FolderInfo>();
            XmlReader foldersXml = this.ImportContext.LoadIndex("folders");
            if (!string.IsNullOrWhiteSpace(fallbackType))
            {
                this.FallbackType = ResolveContentType(fallbackType);
                if (this.FallbackType == null) throw new Exception($"Fallback folder type [{fallbackType}] could not be resolved");
            }
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
                                Log.InfoFormat("Creating {0} folders in the target environment, depth {1}", accumulatedCount, thisDepth);
                                bool ok = false;
                                try
                                {
                                    ProcessAccumulatedFolders(importContext, session, accumulated);
                                    ok = true;
                                }
                                finally
                                {
                                    if (!ok)
                                    {
                                        Log.Error("Failed to process the current accumulated folder batch");
                                    }
                                }
                            }
                            accumulated.Clear();
                            accumulatedCount = 0;
                            currentDepth = thisDepth;
                        }

                        // A new folder to handle...
                        FolderInfo f = new FolderInfo(this.ImportContext.FormatMetadataLocation(location), location);

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
                                Log.InfoFormat("Skipping private cabinet [{0}]", f.FullPath);
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
                        Log.InfoFormat("Creating {0} folders in the target environment, depth {1}", accumulated.Count, currentDepth + 1);
                        bool ok = false;
                        try
                        {
                            ProcessAccumulatedFolders(importContext, session, accumulated);
                            ok = true;
                        }
                        finally
                        {
                            if (!ok)
                            {
                                Log.Error("Failed to process the last accumulated folder batch");
                            }
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
            ApplyMetadata(folder, tracker, true);
        }

        private void ApplyMetadata(FolderInfo folder, ProgressTracker tracker, bool executeQuery)
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
                if (contentType == null) contentType = this.FallbackType;
                if (contentType != null) f.ListItemAllFields["ContentTypeId"] = contentType.Id;

                FolderInfo parent = ResolveFolder(folder.Path);
                bool individualAcl = (parent != null ? !string.Equals(folder.Acl, parent.Acl) : (!this.OrphanAclInherit || !string.IsNullOrEmpty(folder.Acl)));
                ApplyMetadata(f.ListItemAllFields, xml, contentType);
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
                if (executeQuery) session.ExecuteQuery();
                ShowProgress();
                folder.SPObj = f;
            }
        }

        private ICollection<FolderInfo> FinalizeFolders(ImportContext importContext, ICollection<FolderInfo> pending, int importThreads)
        {
            List<FolderInfo> failed = new List<FolderInfo>();
            ActionBlock<FolderInfo> processor = new ActionBlock<FolderInfo>(folderInfo =>
            {
                if (this.Abort) return;
                Result r = Result.Skipped;
                try
                {
                    ProgressTracker tracker = new ProgressTracker(importContext.FormatMetadataLocation(folderInfo.RelativeLocation), importContext.FormatProgressLocation(folderInfo.RelativeLocation), this.Log);
                    if (!folderInfo.Modified && folderInfo.Exists && !tracker.Failed)
                    {
                        Log.DebugFormat("Skipping folder [{0}] - already completed", folderInfo.FullPath);
                        IncreaseProgress();
                        return;
                    }

                    r = Result.Failed;
                    Exception exc = null;
                    try
                    {
                        ApplyMetadata(folderInfo, tracker);
                        tracker.DeleteOutcomeMarker();
                        r = Result.Completed;
                    }
                    catch (Exception e)
                    {
                        exc = e;
                        lock (failed)
                        {
                            failed.Add(folderInfo);
                        }
                        Log.Error($"Failed to apply the metadata to the folder at [{folderInfo.SafeFullPath}]", e);
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

        public void FinalizeFolders(ImportContext importContext, int threads, int retries)
        {
            ICollection<FolderInfo> failed = new List<FolderInfo>();
            Log.Info("Checking existing status to identify which folders need processing");
            ICollection<FolderInfo> pending = IdentifyPending(failed);

            if (this.Abort) return;

            if (pending.Count == 0)
            {
                Log.Info("No folders are in need of processing");
                return;
            }

            ResetCounters();
            int attempt = 0;
            while (pending.Count > 0 && (attempt < (retries + 1)) && !this.Abort)
            {
                ResetCounter(Result.Failed);
                attempt++;
                Log.InfoFormat("Identified {0} folders in need of processing (of which {1} are retries) (attempt #{2}/{3})", pending.Count, failed.Count, attempt, retries + 1);
                int lastPending = pending.Count;
                pending = FinalizeFolders(importContext, pending, threads);
                failed = pending;
                if (pending.Count < lastPending)
                {
                    Log.InfoFormat("{0} folders were successfully processed in this attempt, with {1} failures", lastPending - pending.Count, pending.Count);
                    attempt = 0;
                }
                else
                {
                    Log.InfoFormat("No change in the data set - {0} were pending, and {1} failed", lastPending, pending.Count);
                }
            }

            // Spit out a list of objects to retry, in CSV format
            foreach (FolderInfo info in pending)
            {
                LogFailure("FOLDER", info.Id, info.FullPath, info.Location);
            }
        }
    }
}