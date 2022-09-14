using log4net;
using Microsoft.SharePoint.Client;
using PnP.Framework.Diagnostics;
using PnP.Framework.Http;
using PnP.Framework.Utilities;
using System;
using System.Collections.Generic;
using System.Net;
using System.Security;
using System.ServiceModel.Channels;
using System.Threading;
using System.Xml.Linq;

namespace Caliente.SharePoint.Common
{

    public class ThrottleSimulatorExecutorFactory : WebRequestExecutorFactory
    {
        private static readonly string PARAMETER = "test429=true";
        private static long SIMULATE_429 = 0;

        private readonly WebRequestExecutorFactory Delegator;
        private readonly ILog Log;

        public static void Simulate429(bool value)
        {
            Interlocked.Exchange(ref SIMULATE_429, (value ? 1L : 0L));
        }

        private static bool Simulate429()
        {
            // Only return true every 20th invocation
            long val = Interlocked.Increment(ref SIMULATE_429);
            return ((val % 20) == 0);
        }

        public ThrottleSimulatorExecutorFactory(WebRequestExecutorFactory delegator)
        {
            this.Delegator = delegator;
            this.Log = LogManager.GetLogger(typeof(ThrottleSimulatorExecutorFactory));
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
                Log.Warn("********** ISSUING A RETRY TEST **********");
            }
            WebRequestExecutor result = Delegator.CreateWebRequestExecutor(context, b.Uri.ToString());
            return result;
        }
    }

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
        public readonly bool UseRetryWrapper;

        public SharePointSessionInfo(string url, string userName, SecureString password, string domain, string applicationId, string certificateKey, string certificatePass, string library, int maxBorrowCount, bool useQueryRetry, int retryCount, bool useRetryWrapper)
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
            this.UseQueryRetry = useQueryRetry;
            this.RetryCount = retryCount;
            this.UseRetryWrapper = useRetryWrapper;
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

    public sealed class SharePointSession : IDisposable
    {
        const string USER_AGENT = "NONISV|Armedia|Caliente-SP-Ingestor/1.0";

        public static readonly TimeSpan TIME_OUT = new TimeSpan(1, 0, 0);

        private static long counter = 0;

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

        // PnP.Framework.AuthenticationManager authManager
        public SharePointSession(Func<ClientContext> authenticator, SharePointSessionInfo info, Action<ClientContext, string> executeQueryAction)
        {
            this.ExecuteQueryAction = executeQueryAction;

            this.ClientContext = authenticator();

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
            if (info.UseRetryWrapper)
            {
                this.ClientContext.WebRequestExecutorFactory = new HttpClientWebRequestExecutorFactory(PnPHttpClient.Instance.GetHttpClient());
            }

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
            if (!string.IsNullOrEmpty(info.Domain))
            {
                user = $"{user}@{info.Domain}";
            }
            this.Id = $"SHPT[{user}::{this.ClientContext.Url}#{Interlocked.Increment(ref counter)}]";
        }

        public void ExecuteQuery()
        {
            // Read into: https://docs.microsoft.com/en-us/sharepoint/dev/solution-guidance/security-apponly-azuread
            this.ClientContext.RequestTimeout = (int)TIME_OUT.TotalMilliseconds;
            long start = Environment.TickCount;
            try
            {
                this.ExecuteQueryAction(this.ClientContext, USER_AGENT);
            }
            catch (WebException e)
            {
                long duration = (Environment.TickCount - start);
                throw new ExecuteQueryException(string.Format("{1} ({2}.{3:000}s HRESULT=[0x{4:X8}] STATUS=[{5}] DATA=[{6}])", Environment.NewLine, e.Message, duration / 1000, duration % 1000, e.HResult, e.Status, e.Data), e);
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
            return $"{this.BaseUrl}{prepend}{path}";
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
            return $"SHPT[{this.ClientContext.Url}#{this.Id}]";
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
            private readonly Func<ClientContext> Authenticator;
            // private PnP.Framework.AuthenticationManager AuthManager;
            private SharePointSessionInfo Info;
            private readonly Action<ClientContext, string> ExecuteQuery;

            public SharePointSessionGenerator(SharePointSessionInfo info)
            {
                this.Info = info;
                // Important reading: https://github.com/pnp/pnpframework/blob/dev/docs/MigratingFromPnPSitesCore.md
                // If we're using an application ID, we use that and forget everything else...
                if (!string.IsNullOrWhiteSpace(this.Info.ApplicationId))
                {
                    // WAS:
                    // authManager.GetAzureADAppOnlyAuthenticatedContext(info.Url, info.ApplicationId, info.Domain, info.CertificateKey, info.CertificatePass);
                    // this.AuthManager = new PnP.Framework.AuthenticationManager(this.Info.ApplicationId, this.Info.CertificateKey, this.Info.CertificatePass, this.Info.Domain);
                    this.Authenticator = () =>
                        new PnP.Framework.AuthenticationManager(this.Info.ApplicationId, this.Info.CertificateKey, this.Info.CertificatePass, this.Info.Domain) //
                           .GetContext(this.Info.Url) //
                    ;
                }
                else
                // If we were given written auth parameters, we use those. This will fail if MFA is needed
                if (!string.IsNullOrWhiteSpace(this.Info.UserName))
                {
                    if (!string.IsNullOrEmpty(this.Info.Domain))
                    {
                        // this.AuthManager = new PnP.Framework.AuthenticationManager(this.Info.Domain, this.Info.UserName, this.Info.Password);
                        this.Authenticator = () =>
                        {
                            ClientContext ctx = new ClientContext(this.Info.Url);
                            ctx.Credentials = new SharePointOnlineCredentials(this.Info.UserName + "@" + this.Info.Domain, this.Info.Password);
                            return ctx;
                        };
                    }
                    else
                    {
                        // Requires Admin authorization per:
                        // https://www.sharepointdiary.com/2021/08/fix-connect-pnponline-aadsts65001-user-or-administrator-has-not-consented-to-use-the-application.html
                        // this.AuthManager = new PnP.Framework.AuthenticationManager(this.Info.UserName, this.Info.Password);
                        this.Authenticator = () =>
                        {
                            ClientContext ctx = new ClientContext(this.Info.Url);
                            ctx.Credentials = new SharePointOnlineCredentials(this.Info.UserName, this.Info.Password);
                            return ctx;
                        };
                    }
                }
                else
                {
                    // TODO: There is no longer support for interactive/MFA authentication ... would have to copy it from:
                    // https://github.com/pnp/powershell/blob/dev/src/Commands/Utilities/BrowserHelper.cs
                    throw new Exception("Insufficient authentication information is given - cannot continue");
                }

                if (info.UseQueryRetry)
                {
                    this.ExecuteQuery = (clientContext, userAgent) => clientContext.ExecuteQueryRetry(info.RetryCount, userAgent);
                }
                else
                {
                    this.ExecuteQuery = (clientContext, userAgent) => clientContext.ExecuteQuery();
                }

            }

            SharePointSession PoolableObjectFactory<SharePointSession>.Create()
            {
                return new SharePointSession(this.Authenticator, this.Info, this.ExecuteQuery);
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
                    // this.AuthManager?.Dispose();
                }
                finally
                {
                    this.Info?.Dispose();
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
                this.ObjectPool?.Dispose();
            }
            finally
            {
                this.Generator?.Dispose();
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