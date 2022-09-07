using log4net;
using Microsoft.SharePoint.Client;
using System;
using System.Collections.Generic;
using System.Net;
using System.Security;
using System.Threading;
using System.Xml.Linq;

namespace Armedia.CMSMF.SharePoint.Common
{

    public class SharePointSessionInfo : IDisposable
    {
        const string DEFAULT_LIBRARY_NAME = "Documents";

        public readonly string Url;
        public readonly string UserName;
        public readonly SecureString Password;
        public readonly string Domain;
        public readonly string Library;
        public readonly int MaxBorrowCount;
        public readonly string ApplicationId;
        public readonly string CertificateKey;
        public readonly string CertificatePass;
        public readonly bool UseQueryRetry;
        public readonly int RetryCount;
        public readonly int RetryDelay;

        public SharePointSessionInfo(string url, string userName, SecureString password, string domain, string applicationId, string certificateKey, string certificatePass, string library, int maxBorrowCount, bool useQueryRetry, int retryCount, int retryDelay)
        {
            this.Url = url.TrimEnd('/');
            this.UserName = userName;
            this.Password = password;
            this.Domain = domain;
            this.ApplicationId = applicationId;
            this.CertificateKey = certificateKey;
            this.CertificatePass = certificatePass;
            this.Library = (!string.IsNullOrWhiteSpace(library) ? library : DEFAULT_LIBRARY_NAME);
            if (maxBorrowCount == 0) maxBorrowCount = 1;
            this.MaxBorrowCount = maxBorrowCount;
            this.RetryCount = retryCount;
            this.RetryDelay = retryDelay;
            this.UseQueryRetry = useQueryRetry;
        }

        public void Dispose()
        {
            this.Password?.Dispose();
        }

        ~SharePointSessionInfo()
        {
            Dispose();
        }
    }

    public interface IThrottleHandler
    {
        void ApplyThrottling();

        void RetryAfter(WebException e);
    }

    public class SimpleThrottleHandler : IThrottleHandler
    {
        const int FALLBACK_THROTTLE_SECS = 30;
        private readonly ILog Log;
        private readonly ReaderWriterLockSlim Lock = new ReaderWriterLockSlim();
        private readonly DateTime Boot;
        private long Border = Environment.TickCount;

        public SimpleThrottleHandler()
        {
            this.Log = LogManager.GetLogger(GetType());

            // This isn't perfect, but it's close enough for our purposes
            this.Boot = DateTime.UtcNow - TimeSpan.FromMilliseconds(Environment.TickCount);
        }

        public void ApplyThrottling()
        {
            Lock.EnterReadLock();
            try
            {
                while (true)
                {
                    long now = Environment.TickCount;
                    long remaining = (Interlocked.Read(ref Border) - now);
                    if (remaining <= 0) return;
                    // First, let go of all locks ...
                    Lock.ExitReadLock();
                    try
                    {
                        // Sleep for "remaining" milliseconds
                        this.Log.Info(string.Format("Throttling before the next request by sleeping for {0}ms", remaining));
                        Thread.Sleep((int)remaining);
                    }
                    finally
                    {
                        // Now, reacquire the locks so we can release them cleanly
                        Lock.EnterReadLock();
                    }
                }
            }
            finally
            {
                Lock.ExitReadLock();
            }
        }

        public void RetryAfter(WebException e)
        {
            HttpWebResponse rsp = e.Response as HttpWebResponse;
            if (rsp.StatusCode == (HttpStatusCode)429 || rsp.StatusCode == (HttpStatusCode)503)
            {
                // We got throttled!! Get the Retry-After header
                int retrySeconds = 0;
                string retryAfterHeader = System.Net.HttpResponseHeader.RetryAfter.ToString();
                string retryAfter = rsp.Headers.Get(retryAfterHeader);
                if (!string.IsNullOrEmpty(retryAfter))
                {
                    this.Log.Info(string.Format("Got the {0} header with the value [{1}]", retryAfterHeader, retryAfter));
                    try
                    {
                        retrySeconds = Int32.Parse(retryAfter);
                    }
                    catch (FormatException fe)
                    {
                        retrySeconds = FALLBACK_THROTTLE_SECS;
                        this.Log.Warn(string.Format("Received an HTTP response requiring throttling {0}, but the {1} header contained the value [{2}] which could not be parsed - will instead throttle for {3} seconds", rsp.StatusCode.GetTypeCode(), retryAfterHeader, retryAfter, retrySeconds, fe));
                    }
                }

                if (retrySeconds <= 0) return;

                // The new border will be "now" plus (retrySeconds + 1) seconds in the future
                long newBorder = Environment.TickCount + ((retrySeconds + 1) * 1000);
                Lock.EnterWriteLock();
                try
                {
                    long oldBorder = Interlocked.Read(ref this.Border);
                    // If the border moves forward, then do so ... otherwise, nothing to do here
                    if (newBorder > oldBorder)
                    {
                        this.Log.Info(string.Format("Setting the new retry interval to {0}", this.Boot + TimeSpan.FromMilliseconds(newBorder)));
                        Interlocked.Exchange(ref this.Border, newBorder);
                    }
                }
                finally
                {
                    Lock.ExitWriteLock();
                }
            }
        }
    }

    public sealed class SharePointSession : IDisposable
    {
        const string USER_AGENT = "Armedia-Caliente-SP-Ingestor";

        public static readonly TimeSpan TIME_OUT = new TimeSpan(1, 0, 0);

        private static long counter = 0;

        private readonly IThrottleHandler ThrottleHandler;
        public readonly ClientContext ClientContext;
        public readonly List DocumentLibrary;
        public readonly string BaseUrl;
        public readonly Folder RootFolder;
        public readonly string Id;
        private long BorrowCount = 0;
        private readonly bool UseQueryRetry;
        private readonly int RetryCount;
        private readonly int RetryDelay;

        private class ExecuteQueryException : Exception
        {
            public ExecuteQueryException(string msg, Exception e) : base(msg, e)
            {

            }
        }

        public SharePointSession(OfficeDevPnP.Core.AuthenticationManager authManager, SharePointSessionInfo info, IThrottleHandler throttleHandler)
        {
            this.UseQueryRetry = info.UseQueryRetry;
            this.RetryCount = info.RetryCount;
            this.RetryDelay = info.RetryDelay;
            this.ThrottleHandler = throttleHandler;

            // If we're using an application ID, we use that and forget everything else...
            if (!string.IsNullOrWhiteSpace(info.ApplicationId))
            {
                this.ClientContext = authManager.GetAzureADAppOnlyAuthenticatedContext(info.Url, info.ApplicationId, info.Domain, info.CertificateKey, info.CertificatePass);
                // this.ClientContext =authManager.GetAppOnlyAuthenticatedContext(info.Url, info.ApplicationId, info.CertificatePass);

                // this.ClientContext = authManager.GetSharePointOnlineAuthenticatedContextTenant(info.Url, info.UserName + "@" + info.Domain, info.Password);

                // this.ClientContext.Credentials = new SharePointOnlineCredentials(info.UserName + "@" + info.Domain, info.Password);
            }
            else
            // If we were given written auth parameters, we use those. This will fail if MFA is needed
            if (!string.IsNullOrWhiteSpace(info.UserName))
            {
                 this.ClientContext = authManager.GetSharePointOnlineAuthenticatedContextTenant(info.Url, info.UserName, info.Password);
            }
            else
            // We're not even being given auth details, so we ask ...
            {
                // This one pops up the authentication window where one can log in with MFA
                this.ClientContext = authManager.GetWebLoginClientContext(info.Url);
            }
            this.DocumentLibrary = this.ClientContext.Web.Lists.GetByTitle(info.Library);
            this.ClientContext.Load(this.DocumentLibrary, r => r.ForceCheckout, r => r.EnableVersioning, r => r.EnableMinorVersions, r => r.Title, r => r.ContentTypesEnabled, r => r.ContentTypes);
            this.ClientContext.Load(this.DocumentLibrary.RootFolder, f => f.ServerRelativeUrl, f => f.Name);
            this.ClientContext.Load(this.ClientContext.Web, w => w.Title, w => w.RoleDefinitions);
            ExecuteQuery();
            this.RootFolder = this.DocumentLibrary.RootFolder;
            this.BaseUrl = this.RootFolder.ServerRelativeUrl;
            string user = info.UserName;
            if (user == null)
            {
                user = "Guest";
            }
            if (info.Domain != null)
            {
                user = string.Format("{0}\\{1}", info.Domain, user);
            }
            this.Id = string.Format("SHPT[{0}@{1}#{2}]", user, this.ClientContext.Url, Interlocked.Increment(ref counter));
        }

        public void ExecuteQuery()
        {
            // Read into: https://docs.microsoft.com/en-us/sharepoint/dev/solution-guidance/security-apponly-azuread
            this.ClientContext.RequestTimeout = (int)TIME_OUT.TotalMilliseconds;
            this.ClientContext.PendingRequest.RequestExecutor.RequestKeepAlive = true;
            this.ClientContext.PendingRequest.RequestExecutor.WebRequest.KeepAlive = true;
            this.ClientContext.PendingRequest.RequestExecutor.WebRequest.UserAgent = USER_AGENT;
            this.ClientContext.PendingRequest.RequestExecutor.WebRequest.Timeout = (int)TIME_OUT.TotalMilliseconds;
            // this.ClientContext.PendingRequest.RequestExecutor.WebRequest.ReadWriteTimeout = (int)TIME_OUT.TotalMilliseconds;
            long start = Environment.TickCount;
            try
            {
                this.ThrottleHandler.ApplyThrottling();
                if (this.UseQueryRetry)
                {
                    this.ClientContext.ExecuteQueryRetry(this.RetryCount, this.RetryDelay, USER_AGENT);
                }
                else
                {
                    this.ClientContext.ExecuteQuery();
                }
            }
            catch (ServerException e)
            {
                long duration = (Environment.TickCount - start);
                throw new ExecuteQueryException(string.Format("{1} ({2}.{3:000}s){0}\tHRESULT=[0x{4:X8}]{0}\tServerErrorCode=[0x{5:X8}]{0}\tServerErrorDetails=[{6}]{0}\tServerErrorTraceCorrelationId=[{7}]{0}\tServerErrorTypeName=[{8}]{0}\tServerErrorValue=[{9}]{0}\tServerStackTrace=[{10}]{0}", Environment.NewLine, e.Message, duration / 1000, duration % 1000, e.HResult, e.ServerErrorCode, e.ServerErrorDetails, e.ServerErrorTraceCorrelationId, e.ServerErrorTypeName, e.ServerErrorValue, e.ServerStackTrace), e);
            }
            catch (WebException e)
            {
                long duration = (Environment.TickCount - start);
                this.ThrottleHandler.RetryAfter(e);
                // Whatever happened above, we still puke out ...
                throw new ExecuteQueryException(string.Format("{1} ({2}.{3:000}s HRESULT=[0x{4:X8}] STATUS=[{5}] DATA=[{6}])", Environment.NewLine, e.Message, duration / 1000, duration % 1000, e.HResult, e.Status, e.Data), e);
            }
            catch (Exception e)
            {
                long duration = (Environment.TickCount - start);
                throw new ExecuteQueryException(string.Format("{1} ({2}.{3:000}s HRESULT=[0x{4:X8}] DATA=[{5}])", Environment.NewLine, e.Message, duration / 1000, duration % 1000, e.HResult, e.Data), e);
            }
        }

        public string GetServerRelativeUrl(string path)
        {
            if (path == null) throw new ArgumentNullException("path");
            if (path == "" || path == "/") return this.BaseUrl;
            string prepend = (path.StartsWith("/") ? "" : "/");
            return string.Format("{0}{1}{2}", this.BaseUrl, prepend, path);
        }

        private ListItemCollection GetItemByUniqueIdentifier(string fieldName, string value)
        {
            if (fieldName == null) throw new ArgumentNullException("fieldName");
            if (value == null) throw new ArgumentNullException("value");
            CamlQuery itemById = CamlQuery.CreateAllItemsQuery(1, "FileRef", "FileDirRef");
            XElement view = XElement.Parse(itemById.ViewXml);
            view.Add(
                new XElement("Query",
                    new XElement("Where",
                        new XElement("Eq",
                            new XElement("FieldRef", new XAttribute("Name", fieldName)),
                            new XElement("Value", new XAttribute("Type", "Guid"), value)
                        )
                    )
                )
            );
            itemById.ViewXml = view.ToString();
            ListItemCollection item = this.DocumentLibrary.GetItems(itemById);
            this.ClientContext.Load(item);
            ExecuteQuery();
            return item;
        }

        public ListItemCollection GetItemByUniqueId(string guid)
        {
            return GetItemByUniqueIdentifier("UniqueId", guid);
        }

        public ListItemCollection GetItemByUniqueId(Guid guid)
        {
            return GetItemByUniqueId(guid.ToString());
        }

        public ListItemCollection GetItemByGUID(string guid)
        {
            return GetItemByUniqueIdentifier("GUID", guid);
        }

        public ListItemCollection GetItemByGUID(Guid guid)
        {
            return GetItemByGUID(guid.ToString());
        }

        public long GetBorrowCount()
        {
            return this.BorrowCount;
        }

        public long IncrementBorrowCount()
        {
            return ++BorrowCount;
        }

        public override string ToString()
        {
            return string.Format("SHPT[{0}#{1}]", this.ClientContext.Url, this.Id);
        }

        public void Dispose()
        {
            this.ClientContext.Dispose();
        }
    }

    public sealed class SharePointSessionFactory : IDisposable
    {
        private class SharePointSessionGenerator : PoolableObjectFactory<SharePointSession>
        {
            private OfficeDevPnP.Core.AuthenticationManager AuthManager;
            private SharePointSessionInfo Info;
            private readonly IThrottleHandler ThrottleHandler = new SimpleThrottleHandler();

            public SharePointSessionGenerator(SharePointSessionInfo info)
            {
                this.Info = info;
                this.AuthManager = new OfficeDevPnP.Core.AuthenticationManager();
            }

            SharePointSession PoolableObjectFactory<SharePointSession>.Create()
            {
                return new SharePointSession(this.AuthManager, this.Info, this.ThrottleHandler);
            }

            void PoolableObjectFactory<SharePointSession>.Destroy(SharePointSession t)
            {
                t.Dispose();
            }

            public bool Validate(SharePointSession t)
            {
                if (this.Info.MaxBorrowCount < 0) return true;
                return (t.GetBorrowCount() < this.Info.MaxBorrowCount);
            }

            public void Activate(SharePointSession t)
            {
                t.IncrementBorrowCount();
            }

            public void Passivate(SharePointSession t)
            {
            }

            public void Dispose()
            {
                try
                {
                    this.AuthManager.Dispose();
                }
                finally
                {
                    this.Info.Dispose();
                }
            }

            ~SharePointSessionGenerator()
            {
                Dispose();
            }
        }

        private SharePointSessionGenerator Generator;
        private ObjectPool<SharePointSession> ObjectPool;

        public SharePointSessionFactory(SharePointSessionInfo info) : this(info, 0)
        {
        }

        public SharePointSessionFactory(SharePointSessionInfo info, int initCount)
        {
            this.Generator = new SharePointSessionGenerator(info);
            this.ObjectPool = new ObjectPool<SharePointSession>(this.Generator);
            if (initCount > 0)
            {
                List<ObjectPool<SharePointSession>.Ref> cache = new List<ObjectPool<SharePointSession>.Ref>();
                // Pre-cache the items...
                for (int i = 0 ; i < initCount ; i++)
                {
                    ObjectPool<SharePointSession>.Ref r = this.ObjectPool.GetObject();
                    cache.Add(r);
                }
                // Now return them to the pool
                foreach (ObjectPool<SharePointSession>.Ref r in cache)
                {
                    r.Dispose();
                }
            }
        }

        public ObjectPool<SharePointSession>.Ref GetSession()
        {
            return this.ObjectPool.GetObject();
        }

        public void Dispose()
        {
            try
            {
                this.ObjectPool.Dispose();
            }
            finally
            {
                this.Generator.Dispose();
            }
        }

        public void Close()
        {
            Dispose();
        }

        ~SharePointSessionFactory()
        {
            Dispose();
        }
    }
}