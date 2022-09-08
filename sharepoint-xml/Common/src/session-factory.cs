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

        bool CheckRetry(WebException e);

        TimeSpan ApplyRetry(string retryAfter);
    }

    internal class SimpleThrottleHandler : IThrottleHandler
    {
        const string RETRY_AFTER = "Retry-After";
        const int FALLBACK_THROTTLE_SECS = 30;

        private readonly ILog Log;
        private readonly ReaderWriterLockSlim Lock = new ReaderWriterLockSlim();
        private readonly DateTime Boot;
        private long Border = Environment.TickCount;

        public SimpleThrottleHandler()
        {
            this.Log = LogManager.GetLogger(GetType());

            // This isn't perfect, but it's close enough for our purposes
            this.Boot = DateTime.Now - TimeSpan.FromMilliseconds(Environment.TickCount);
        }

        public void ApplyThrottling()
        {
            Lock.EnterReadLock();
            try
            {
                while (true)
                {
                    long remaining = (Interlocked.Read(ref Border) - Environment.TickCount);
                    if (remaining <= 0) return;
                    // First, let go of all locks ...
                    Lock.ExitReadLock();
                    try
                    {
                        // Sleep for "remaining" milliseconds
                        this.Log.InfoFormat("Throttling before the next request by sleeping for {0}", TimeSpan.FromMilliseconds(remaining));
                        Thread.Sleep((int)remaining);
                        this.Log.Info("Throttle threshold reached, will resume processing");
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

        public bool CheckRetry(WebException e)
        {
            HttpWebResponse rsp = e.Response as HttpWebResponse;
            if (rsp == null) return false;

            int statusCode = (int)rsp.StatusCode;
            switch (statusCode)
            {
                case 429:
                case 503:
                case 504:
                    // We got throttled... keep going!
                    break;

                default:
                    // Something else ... don't return
                    return false;
            }

            // We got throttled!! Get the Retry-After header
            // MORE INFO : https://docs.microsoft.com/en-us/answers/questions/318901/the-remote-server-returned-an-error-429.html?page=2&pageSize=10&sort=oldest
            string retryAfter = rsp.Headers.Get(RETRY_AFTER);
            TimeSpan retry = ApplyRetry(retryAfter);
            this.Log.InfoFormat("Found the {0} header asking for a retry interval of {1} seconds (will throttle for {2})", RETRY_AFTER, retryAfter, retry);
            return true;
        }

        public TimeSpan ApplyRetry(string retryAfter)
        {
            int retrySeconds = 0;
            if (!string.IsNullOrEmpty(retryAfter))
            {
                this.Log.InfoFormat("Got the {0} header with the value [{1}]", RETRY_AFTER, retryAfter);
                if (!Int32.TryParse(retryAfter, out retrySeconds))
                {
                    retrySeconds = FALLBACK_THROTTLE_SECS;
                    this.Log.WarnFormat("The requested retry value [{0}] could not be parsed - will instead throttle for {1} seconds", retryAfter, retrySeconds);
                }
            }
            else
            {
                retrySeconds = FALLBACK_THROTTLE_SECS;
            }

            if (retrySeconds < 0)
            {
                retrySeconds = FALLBACK_THROTTLE_SECS;
            }

            long newBorder = Environment.TickCount + (retrySeconds * 1000);
            Lock.EnterWriteLock();
            try
            {
                long oldBorder = Interlocked.Read(ref this.Border);
                // If the border moves forward, then do so ... otherwise, nothing to do here
                if (newBorder > oldBorder)
                {
                    Interlocked.Exchange(ref this.Border, newBorder);
                    this.Log.InfoFormat("Set the new retry interval to {0}", this.Boot + TimeSpan.FromMilliseconds(newBorder));
                }
            }
            finally
            {
                Lock.ExitWriteLock();
            }

            return TimeSpan.FromSeconds(retrySeconds);
        }
    }

    public class ThrottleSimulatorExecutorFactory : WebRequestExecutorFactory
    {
        private static readonly string PARAMETER = "test429=true";
        private static long SIMULATE_429 = 0;

        private readonly WebRequestExecutorFactory Delegator;

        public static void Simulate429(bool value)
        {
            Interlocked.Exchange(ref SIMULATE_429, (value ? 1L : 0L));
        }

        public static bool Simulate429()
        {
            long val = Interlocked.Read(ref SIMULATE_429);
            return (val != 0);
        }

        public ThrottleSimulatorExecutorFactory(WebRequestExecutorFactory delegator)
        {
            this.Delegator = delegator;
        }

        public override WebRequestExecutor CreateWebRequestExecutor(ClientRuntimeContext context, string requestUrl)
        {
            string newUrl = requestUrl;
            UriBuilder b = new UriBuilder(requestUrl);
            if (Simulate429() && !string.IsNullOrEmpty(b.Path) && b.Path.EndsWith("/ProcessQuery"))
            {
                // If the query already has parameters, tack it onto the end
                if (!string.IsNullOrEmpty(b.Query))
                {
                    b.Query += "&";
                }
                b.Query += PARAMETER;
            }
            WebRequestExecutor result = Delegator.CreateWebRequestExecutor(context, b.Uri.ToString());
            return result;
        }
    }

    internal class RetryAwareWebRequestExecutor : WebRequestExecutor
    {
        private readonly IThrottleHandler ThrottleHandler;
        private readonly WebRequestExecutor Executor;

        public RetryAwareWebRequestExecutor(IThrottleHandler throttleHandler, WebRequestExecutor executor)
        {
            this.ThrottleHandler = throttleHandler;
            this.Executor = executor;
        }

        public override string RequestContentType {
            get
            {
                return this.Executor.RequestContentType;
            }
            set
            {
                this.Executor.RequestContentType = value;
            }
        }

        public override WebHeaderCollection RequestHeaders
        {
            get
            {
                return this.Executor.RequestHeaders;
            }
        }

        public override bool RequestKeepAlive
        {
            get
            {
                return this.Executor.RequestKeepAlive;
            }
            set
            {
                this.Executor.RequestKeepAlive = value;
            }
        }

        public override string RequestMethod
        {
            get
            {
                return this.Executor.RequestMethod;
            }
            set
            {
                this.Executor.RequestMethod = value;
            }
        }

        public override string ResponseContentType
        {
            get
            {
                return this.Executor.ResponseContentType;
            }
        }

        public override WebHeaderCollection ResponseHeaders
        {
            get
            {
                return this.Executor.ResponseHeaders;
            }
        }

        public override HttpStatusCode StatusCode
        {
            get
            {
                return this.Executor.StatusCode;
            }
        }

        public override HttpWebRequest WebRequest
        {
            get
            {
                return this.Executor.WebRequest;
            }
        }

        public override void Dispose()
        {
            this.Executor.Dispose();
        }

        public override void Execute()
        {
            // TODO: Implement the retry logic here to stow away a copy
            // of the request to be executed so it can be retried later. Read
            // the code in the PnP package to see how they do it ...
            this.Executor.Execute();
        }

        public override System.Threading.Tasks.Task ExecuteAsync()
        {
            return this.Executor.ExecuteAsync();
        }

        public override System.IO.Stream GetRequestStream()
        {
            return this.Executor.GetRequestStream();
        }

        public override System.IO.Stream GetResponseStream()
        {
            return this.Executor.GetResponseStream();
        }
    }

    internal class RetryAwareWebRequestExecutorFactory : WebRequestExecutorFactory
    {
        private readonly IThrottleHandler ThrottleHandler;
        private readonly WebRequestExecutorFactory Factory;

        public RetryAwareWebRequestExecutorFactory(IThrottleHandler throttleHandler, WebRequestExecutorFactory factory)
        {
            this.ThrottleHandler = throttleHandler;
            this.Factory = factory;
        }

        public override WebRequestExecutor CreateWebRequestExecutor(ClientRuntimeContext context, string requestUrl)
        {
            return new RetryAwareWebRequestExecutor(this.ThrottleHandler, this.Factory.CreateWebRequestExecutor(context, requestUrl));
        }
    }

        public sealed class SharePointSession : IDisposable
    {
        const string USER_AGENT = "NONISV|Armedia|Caliente-SP-Ingestor/1.0";

        public static readonly TimeSpan TIME_OUT = new TimeSpan(1, 0, 0);

        private static long counter = 0;

        private readonly IThrottleHandler ThrottleHandler;
        public readonly ClientContext ClientContext;
        public readonly List DocumentLibrary;
        public readonly string BaseUrl;
        public readonly Folder RootFolder;
        public readonly string Id;
        private long BorrowCount = 0;
        private readonly Action<ClientContext, string> ExecuteQueryAction;

        private class ExecuteQueryException : Exception
        {
            public ExecuteQueryException(string msg, Exception e) : base(msg, e)
            {

            }
        }

        public SharePointSession(OfficeDevPnP.Core.AuthenticationManager authManager, SharePointSessionInfo info, IThrottleHandler throttleHandler, Action<ClientContext, string> executeQueryAction)
        {
            this.ThrottleHandler = throttleHandler;
            this.ExecuteQueryAction = executeQueryAction;

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
            // Make sure we ALWAYS include the USER_AGENT
            this.ClientContext.ExecutingWebRequest += delegate (object sender, WebRequestEventArgs e)
            {
                e.WebRequestExecutor.RequestKeepAlive = true;
                e.WebRequestExecutor.WebRequest.KeepAlive = true;
                e.WebRequestExecutor.WebRequest.UserAgent = USER_AGENT;
                e.WebRequestExecutor.WebRequest.Timeout = (int)TIME_OUT.TotalMilliseconds;
                // e.WebRequestExecutor.WebRequest.ReadWriteTimeout = (int)TIME_OUT.TotalMilliseconds;
            };

            // Use a custom request factory to support handling retry requests, since ExecuteQueryRetry() doesn't
            // seem to do it properly
            this.ClientContext.WebRequestExecutorFactory = new RetryAwareWebRequestExecutorFactory(throttleHandler, this.ClientContext.WebRequestExecutorFactory);

            // Use a custom request factory to simulate throttling
            // this.ClientContext.WebRequestExecutorFactory = new ThrottleSimulatorExecutorFactory(this.ClientContext.WebRequestExecutorFactory);

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
            long start = Environment.TickCount;
            try
            {
                while (true)
                {
                    this.ThrottleHandler.ApplyThrottling();
                    try
                    {
                        this.ExecuteQueryAction(this.ClientContext, USER_AGENT);
                        break;
                    }
                    catch (WebException e)
                    {
                        long duration = (Environment.TickCount - start);

                        // First things first: are we retrying? If so, then do so...
                        // TODO: Is this the correct approach? What if we're not using ExecuteQueryRetry?
                        this.ThrottleHandler.CheckRetry(e);

                        // We're not retrying, so we puke out ...
                        throw new ExecuteQueryException(string.Format("{1} ({2}.{3:000}s HRESULT=[0x{4:X8}] STATUS=[{5}] DATA=[{6}])", Environment.NewLine, e.Message, duration / 1000, duration % 1000, e.HResult, e.Status, e.Data), e);
                    }
                }
            }
            catch (ServerException e)
            {
                long duration = (Environment.TickCount - start);
                throw new ExecuteQueryException(string.Format("{1} ({2}.{3:000}s){0}\tHRESULT=[0x{4:X8}]{0}\tServerErrorCode=[0x{5:X8}]{0}\tServerErrorDetails=[{6}]{0}\tServerErrorTraceCorrelationId=[{7}]{0}\tServerErrorTypeName=[{8}]{0}\tServerErrorValue=[{9}]{0}\tServerStackTrace=[{10}]{0}", Environment.NewLine, e.Message, duration / 1000, duration % 1000, e.HResult, e.ServerErrorCode, e.ServerErrorDetails, e.ServerErrorTraceCorrelationId, e.ServerErrorTypeName, e.ServerErrorValue, e.ServerStackTrace), e);
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
            private readonly Action<ClientContext, string> ExecuteQuery;

            public SharePointSessionGenerator(SharePointSessionInfo info)
            {
                this.Info = info;
                this.AuthManager = new OfficeDevPnP.Core.AuthenticationManager();

                if (info.UseQueryRetry)
                {
                    this.ExecuteQuery = (clientContext, userAgent) => clientContext.ExecuteQueryRetry(info.RetryCount, info.RetryDelay, userAgent);
                }
                else
                {
                    this.ExecuteQuery = (clientContext, userAgent) => clientContext.ExecuteQuery();
                }

            }

            SharePointSession PoolableObjectFactory<SharePointSession>.Create()
            {
                return new SharePointSession(this.AuthManager, this.Info, this.ThrottleHandler, this.ExecuteQuery);
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