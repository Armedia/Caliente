using Microsoft.SharePoint.Client;
using System;
using System.Collections.Generic;
using System.Net;
using System.Security;
using System.Threading;
using System.Drawing;
using System.Xml.Linq;
using OfficeDevPnP.Core;

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

        public SharePointSessionInfo(string url, string userName, SecureString password, string domain, string applicationId, string certificateKey, string certificatePass, string library, int maxBorrowCount)
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
        public static readonly TimeSpan TIME_OUT = new TimeSpan(1, 0, 0);

        private static long counter = 0;

        public readonly ClientContext ClientContext;
        public readonly List DocumentLibrary;
        public readonly string BaseUrl;
        public readonly Folder RootFolder;
        public readonly string Id;
        private long BorrowCount = 0;

        private class ExecuteQueryException : Exception
        {
            public ExecuteQueryException(string msg, Exception e) : base(msg, e)
            {

            }
        }

        public SharePointSession(OfficeDevPnP.Core.AuthenticationManager authManager, SharePointSessionInfo info)
        {
            // If we're using an application ID, we use that and forget everything else...
            if (!string.IsNullOrWhiteSpace(info.ApplicationId))
            {
                this.ClientContext = authManager.GetAzureADAppOnlyAuthenticatedContext(info.Url, info.ApplicationId, info.Domain, info.CertificateKey, info.CertificatePass);
                // this.ClientContext = new OfficeDevPnP.Core.AuthenticationManager().GetAppOnlyAuthenticatedContext(info.Url, info.ApplicationId, info.CertificatePass);

                // this.ClientContext = new OfficeDevPnP.Core.AuthenticationManager().GetSharePointOnlineAuthenticatedContextTenant(info.Url, info.UserName + "@" + info.Domain, info.Password);

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
            this.ClientContext.PendingRequest.RequestExecutor.WebRequest.Timeout = (int)TIME_OUT.TotalMilliseconds;
            // this.ClientContext.PendingRequest.RequestExecutor.WebRequest.ReadWriteTimeout = (int)TIME_OUT.TotalMilliseconds;
            long start = Environment.TickCount;
            try
            {
                this.ClientContext.ExecuteQuery();
            }
            catch (ServerException e)
            {
                long duration = (Environment.TickCount - start);
                throw new ExecuteQueryException(string.Format("{1} ({2}.{3:000}s){0}\tHRESULT=[0x{4:X8}]{0}\tServerErrorCode=[0x{5:X8}]{0}\tServerErrorDetails=[{6}]{0}\tServerErrorTraceCorrelationId=[{7}]{0}\tServerErrorTypeName=[{8}]{0}\tServerErrorValue=[{9}]{0}\tServerStackTrace=[{10}]{0}", Environment.NewLine, e.Message, duration / 1000, duration % 1000, e.HResult, e.ServerErrorCode, e.ServerErrorDetails, e.ServerErrorTraceCorrelationId, e.ServerErrorTypeName, e.ServerErrorValue, e.ServerStackTrace), e);
            }
            catch (WebException e)
            {
                long duration = (Environment.TickCount - start);
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

            public SharePointSessionGenerator(SharePointSessionInfo info)
            {
                this.Info = info;
                this.AuthManager = new OfficeDevPnP.Core.AuthenticationManager();
            }

            SharePointSession PoolableObjectFactory<SharePointSession>.Create()
            {
                return new SharePointSession(this.AuthManager, this.Info);
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