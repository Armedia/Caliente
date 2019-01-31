package com.armedia.caliente.engine.sharepoint;

import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.Proxy;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;

import org.apache.http.HttpHost;
import org.apache.http.auth.Credentials;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.conn.HttpClientConnectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.commons.utilities.FileNameTools;
import com.armedia.commons.utilities.Tools;
import com.independentsoft.share.Attachment;
import com.independentsoft.share.BasePermission;
import com.independentsoft.share.Change;
import com.independentsoft.share.ChangeLogItemQuery;
import com.independentsoft.share.ChangeQuery;
import com.independentsoft.share.CheckInType;
import com.independentsoft.share.ContentType;
import com.independentsoft.share.ContextInfo;
import com.independentsoft.share.ControlMode;
import com.independentsoft.share.EventReceiver;
import com.independentsoft.share.Field;
import com.independentsoft.share.FieldCreationInfo;
import com.independentsoft.share.FieldLink;
import com.independentsoft.share.FieldSchemaXml;
import com.independentsoft.share.FieldValue;
import com.independentsoft.share.File;
import com.independentsoft.share.FileVersion;
import com.independentsoft.share.Folder;
import com.independentsoft.share.Form;
import com.independentsoft.share.Group;
import com.independentsoft.share.LimitedWebPartManager;
import com.independentsoft.share.List;
import com.independentsoft.share.ListItem;
import com.independentsoft.share.ListTemplate;
import com.independentsoft.share.ListTemplateType;
import com.independentsoft.share.Locale;
import com.independentsoft.share.MoveOperation;
import com.independentsoft.share.NavigationNode;
import com.independentsoft.share.PersonalizationScope;
import com.independentsoft.share.RecycleBinItem;
import com.independentsoft.share.RegionalSettings;
import com.independentsoft.share.Role;
import com.independentsoft.share.RoleType;
import com.independentsoft.share.SearchQuery;
import com.independentsoft.share.SearchQuerySuggestion;
import com.independentsoft.share.SearchResult;
import com.independentsoft.share.ServerSettings;
import com.independentsoft.share.Service;
import com.independentsoft.share.ServiceException;
import com.independentsoft.share.Site;
import com.independentsoft.share.SiteCreationInfo;
import com.independentsoft.share.SiteInfo;
import com.independentsoft.share.SiteTemplate;
import com.independentsoft.share.SuggestResult;
import com.independentsoft.share.TemplateFileType;
import com.independentsoft.share.ThemeInfo;
import com.independentsoft.share.TimeZone;
import com.independentsoft.share.User;
import com.independentsoft.share.View;
import com.independentsoft.share.WorkflowTemplate;
import com.independentsoft.share.queryoptions.IQueryOption;

public class ShptSession {

	private static final String URL_ENCODING = "UTF-8";

	private final Logger log = LoggerFactory.getLogger(getClass());

	private final URL url;
	private final String user;
	private final String password;
	private final String domain;

	private Service service = null;

	public ShptSession(URL url, String user, String password, String domain) {
		this.url = url;
		this.user = user;
		this.password = password;
		this.domain = domain;
		this.service = newService();
	}

	private ShptSessionException processException(ServiceException e, boolean silent) {
		boolean replaceService = false;
		String errorString = e.getErrorString();
		if (errorString != null) {
			errorString = errorString.toLowerCase();
			if (errorString.startsWith("400 ") || errorString.startsWith("404 ")) {
				replaceService = true;
			}
		} else {
			replaceService = false;
		}

		if (replaceService == true) {
			if (!silent) {
				if (this.log.isTraceEnabled()) {
					this.log.warn(
						String.format("Exception raised for URL [%s] resulted in a new Service instance being created",
							e.getRequestUrl()),
						e);
				} else {
					this.log.warn(String.format(
						"Exception raised for URL [%s] resulted in a new Service instance being created - %s",
						e.getRequestUrl(), e.getErrorString()));
				}
			}
			this.service = newService();
		}
		return new ShptSessionException(
			String.format(
				"ServiceException caught - %s, message = [%s], errorString = [%s], requestUrl = [%s], newService = %s",
				e.getClass().getCanonicalName(), e.getMessage(), e.getErrorString(), e.getRequestUrl(), replaceService),
			e);
	}

	private ShptSessionException processException(ServiceException e) {
		return processException(e, false);
	}

	protected Service newService() {
		return new Service(this.url.toString(), this.user, this.password, this.domain);
	}

	public Field addDependentLookupField(String displayName, String primaryLookupFieldId, String showField,
		String listId) throws ShptSessionException {
		try {
			return this.service.addDependentLookupField(displayName, primaryLookupFieldId, showField, listId);
		} catch (ServiceException e) {
			throw processException(e);
		}
	}

	public boolean addRoleAssignment(int principalId, int roleId) throws ShptSessionException {
		try {
			return this.service.addRoleAssignment(principalId, roleId);
		} catch (ServiceException e) {
			throw processException(e);
		}
	}

	public boolean applySiteTemplate(String name) throws ShptSessionException {
		try {
			return this.service.applySiteTemplate(name);
		} catch (ServiceException e) {
			throw processException(e);
		}
	}

	public boolean applyTheme(String colorPaletteUrl, boolean shareGenerated) throws ShptSessionException {
		try {
			return this.service.applyTheme(colorPaletteUrl, shareGenerated);
		} catch (ServiceException e) {
			throw processException(e);
		}
	}

	public boolean applyTheme(String colorPaletteUrl, String fontSchemeUrl, boolean shareGenerated)
		throws ShptSessionException {
		try {
			return this.service.applyTheme(colorPaletteUrl, fontSchemeUrl, shareGenerated);
		} catch (ServiceException e) {
			throw processException(e);
		}
	}

	public boolean applyTheme(String colorPaletteUrl, String fontSchemeUrl, String backgroundImageUrl,
		boolean shareGenerated) throws ShptSessionException {
		try {
			return this.service.applyTheme(colorPaletteUrl, fontSchemeUrl, backgroundImageUrl, shareGenerated);
		} catch (ServiceException e) {
			throw processException(e);
		}
	}

	public boolean applyTheme(String colorPaletteUrl, String fontSchemeUrl, String backgroundImageUrl)
		throws ShptSessionException {
		try {
			return this.service.applyTheme(colorPaletteUrl, fontSchemeUrl, backgroundImageUrl);
		} catch (ServiceException e) {
			throw processException(e);
		}
	}

	public boolean applyTheme(String colorPaletteUrl, String fontSchemeUrl) throws ShptSessionException {
		try {
			return this.service.applyTheme(colorPaletteUrl, fontSchemeUrl);
		} catch (ServiceException e) {
			throw processException(e);
		}
	}

	public boolean applyTheme(String colorPaletteUrl) throws ShptSessionException {
		try {
			return this.service.applyTheme(colorPaletteUrl);
		} catch (ServiceException e) {
			throw processException(e);
		}
	}

	public boolean approve(String filePath, String comment) throws ShptSessionException {
		try {
			return this.service.approve(filePath, comment);
		} catch (ServiceException e) {
			throw processException(e);
		}
	}

	public boolean approve(String filePath) throws ShptSessionException {
		try {
			return this.service.approve(filePath);
		} catch (ServiceException e) {
			throw processException(e);
		}
	}

	public boolean breakRoleInheritance() throws ShptSessionException {
		try {
			return this.service.breakRoleInheritance();
		} catch (ServiceException e) {
			throw processException(e);
		}
	}

	public boolean breakRoleInheritance(boolean copyRoleAssignments, boolean clearSubscopes)
		throws ShptSessionException {
		try {
			return this.service.breakRoleInheritance(copyRoleAssignments, clearSubscopes);
		} catch (ServiceException e) {
			throw processException(e);
		}
	}

	public boolean breakRoleInheritance(boolean copyRoleAssignments) throws ShptSessionException {
		try {
			return this.service.breakRoleInheritance(copyRoleAssignments);
		} catch (ServiceException e) {
			throw processException(e);
		}
	}

	public boolean breakRoleInheritance(String listId, boolean copyRoleAssignments, boolean clearSubscopes)
		throws ShptSessionException {
		try {
			return this.service.breakRoleInheritance(listId, copyRoleAssignments, clearSubscopes);
		} catch (ServiceException e) {
			throw processException(e);
		}
	}

	public boolean breakRoleInheritance(String listId, boolean copyRoleAssignments) throws ShptSessionException {
		try {
			return this.service.breakRoleInheritance(listId, copyRoleAssignments);
		} catch (ServiceException e) {
			throw processException(e);
		}
	}

	public boolean breakRoleInheritance(String listId, int itemId, boolean copyRoleAssignments, boolean clearSubscopes)
		throws ShptSessionException {
		try {
			return this.service.breakRoleInheritance(listId, itemId, copyRoleAssignments, clearSubscopes);
		} catch (ServiceException e) {
			throw processException(e);
		}
	}

	public boolean breakRoleInheritance(String listId, int itemId, boolean copyRoleAssignments)
		throws ShptSessionException {
		try {
			return this.service.breakRoleInheritance(listId, itemId, copyRoleAssignments);
		} catch (ServiceException e) {
			throw processException(e);
		}
	}

	public boolean breakRoleInheritance(String listId, int itemId) throws ShptSessionException {
		try {
			return this.service.breakRoleInheritance(listId, itemId);
		} catch (ServiceException e) {
			throw processException(e);
		}
	}

	public boolean breakRoleInheritance(String listId) throws ShptSessionException {
		try {
			return this.service.breakRoleInheritance(listId);
		} catch (ServiceException e) {
			throw processException(e);
		}
	}

	public boolean checkIn(String filePath, CheckInType type, String comment) throws ShptSessionException {
		try {
			return this.service.checkIn(filePath, type, comment);
		} catch (ServiceException e) {
			throw processException(e);
		}
	}

	public boolean checkIn(String filePath, CheckInType type) throws ShptSessionException {
		try {
			return this.service.checkIn(filePath, type);
		} catch (ServiceException e) {
			throw processException(e);
		}
	}

	public boolean checkIn(String filePath, String comment) throws ShptSessionException {
		try {
			return this.service.checkIn(filePath, comment);
		} catch (ServiceException e) {
			throw processException(e);
		}
	}

	public boolean checkIn(String filePath) throws ShptSessionException {
		try {
			return this.service.checkIn(filePath);
		} catch (ServiceException e) {
			throw processException(e);
		}
	}

	public boolean checkOut(String filePath) throws ShptSessionException {
		try {
			return this.service.checkOut(filePath);
		} catch (ServiceException e) {
			throw processException(e);
		}
	}

	public User checkedOutByUser(String filePath) throws ShptSessionException {
		try {
			return this.service.checkedOutByUser(filePath);
		} catch (ServiceException e) {
			throw processException(e);
		}
	}

	public boolean copyFile(String sourceFilePath, String destinationFilePath, boolean overwrite)
		throws ShptSessionException {
		try {
			return this.service.copyFile(sourceFilePath, destinationFilePath, overwrite);
		} catch (ServiceException e) {
			throw processException(e);
		}
	}

	public boolean copyFile(String sourceFilePath, String destinationFilePath) throws ShptSessionException {
		try {
			return this.service.copyFile(sourceFilePath, destinationFilePath);
		} catch (ServiceException e) {
			throw processException(e);
		}
	}

	public Field createField(Field field, String listId) throws ShptSessionException {
		try {
			return this.service.createField(field, listId);
		} catch (ServiceException e) {
			throw processException(e);
		}
	}

	public Field createField(Field field) throws ShptSessionException {
		try {
			return this.service.createField(field);
		} catch (ServiceException e) {
			throw processException(e);
		}
	}

	public Field createField(FieldCreationInfo field, String listId) throws ShptSessionException {
		try {
			return this.service.createField(field, listId);
		} catch (ServiceException e) {
			throw processException(e);
		}
	}

	public Field createField(FieldSchemaXml field, String listId) throws ShptSessionException {
		try {
			return this.service.createField(field, listId);
		} catch (ServiceException e) {
			throw processException(e);
		}
	}

	public Field createField(FieldSchemaXml field) throws ShptSessionException {
		try {
			return this.service.createField(field);
		} catch (ServiceException e) {
			throw processException(e);
		}
	}

	public File createFile(String filePath, byte[] buffer, boolean overwrite) throws ShptSessionException {
		try {
			return this.service.createFile(filePath, buffer, overwrite);
		} catch (ServiceException e) {
			throw processException(e);
		}
	}

	public File createFile(String filePath, byte[] buffer) throws ShptSessionException {
		try {
			return this.service.createFile(filePath, buffer);
		} catch (ServiceException e) {
			throw processException(e);
		}
	}

	public File createFile(String filePath, InputStream stream, boolean overwrite) throws ShptSessionException {
		try {
			return this.service.createFile(filePath, stream, overwrite);
		} catch (ServiceException e) {
			throw processException(e);
		}
	}

	public File createFile(String filePath, InputStream stream) throws ShptSessionException {
		try {
			return this.service.createFile(filePath, stream);
		} catch (ServiceException e) {
			throw processException(e);
		}
	}

	public Folder createFolder(String folderPath) throws ShptSessionException {
		try {
			return this.service.createFolder(folderPath);
		} catch (ServiceException e) {
			throw processException(e);
		}
	}

	public Group createGroup(Group group) throws ShptSessionException {
		try {
			return this.service.createGroup(group);
		} catch (ServiceException e) {
			throw processException(e);
		}
	}

	public List createList(List list) throws ShptSessionException {
		try {
			return this.service.createList(list);
		} catch (ServiceException e) {
			throw processException(e);
		}
	}

	public ListItem createListItem(String listId, ListItem listItem) throws ShptSessionException {
		try {
			return this.service.createListItem(listId, listItem);
		} catch (ServiceException e) {
			throw processException(e);
		}
	}

	public Role createRole(Role role) throws ShptSessionException {
		try {
			return this.service.createRole(role);
		} catch (ServiceException e) {
			throw processException(e);
		}
	}

	public Site createSite(SiteCreationInfo siteInfo) throws ShptSessionException {
		try {
			return this.service.createSite(siteInfo);
		} catch (ServiceException e) {
			throw processException(e);
		}
	}

	public File createTemplateFile(String filePath, TemplateFileType type) throws ShptSessionException {
		try {
			return this.service.createTemplateFile(filePath, type);
		} catch (ServiceException e) {
			throw processException(e);
		}
	}

	public File createTemplateFile(String filePath) throws ShptSessionException {
		try {
			return this.service.createTemplateFile(filePath);
		} catch (ServiceException e) {
			throw processException(e);
		}
	}

	public User createUser(User user, int groupId) throws ShptSessionException {
		try {
			return this.service.createUser(user, groupId);
		} catch (ServiceException e) {
			throw processException(e);
		}
	}

	public View createView(String listId, View view) throws ShptSessionException {
		try {
			return this.service.createView(listId, view);
		} catch (ServiceException e) {
			throw processException(e);
		}
	}

	public boolean createViewField(String listId, String viewId, String fieldName) throws ShptSessionException {
		try {
			return this.service.createViewField(listId, viewId, fieldName);
		} catch (ServiceException e) {
			throw processException(e);
		}
	}

	public boolean deleteAllFileVersions(String filePath) throws ShptSessionException {
		try {
			return this.service.deleteAllFileVersions(filePath);
		} catch (ServiceException e) {
			throw processException(e);
		}
	}

	public boolean deleteAllViewFields(String listId, String viewId) throws ShptSessionException {
		try {
			return this.service.deleteAllViewFields(listId, viewId);
		} catch (ServiceException e) {
			throw processException(e);
		}
	}

	public void deleteField(String id, String listId) throws ShptSessionException {
		try {
			this.service.deleteField(id, listId);
		} catch (ServiceException e) {
			throw processException(e);
		}
	}

	public void deleteFile(String filePath) throws ShptSessionException {
		try {
			this.service.deleteFile(filePath);
		} catch (ServiceException e) {
			throw processException(e);
		}
	}

	public boolean deleteFileVersion(String filePath, int versionId) throws ShptSessionException {
		try {
			return this.service.deleteFileVersion(filePath, versionId);
		} catch (ServiceException e) {
			throw processException(e);
		}
	}

	public boolean deleteFileVersion(String filePath, String versionLabel) throws ShptSessionException {
		try {
			return this.service.deleteFileVersion(filePath, versionLabel);
		} catch (ServiceException e) {
			throw processException(e);
		}
	}

	public void deleteFolder(String folderPath) throws ShptSessionException {
		try {
			this.service.deleteFolder(folderPath);
		} catch (ServiceException e) {
			throw processException(e);
		}
	}

	public boolean deleteGroup(int groupId) throws ShptSessionException {
		try {
			return this.service.deleteGroup(groupId);
		} catch (ServiceException e) {
			throw processException(e);
		}
	}

	public boolean deleteGroup(String loginName) throws ShptSessionException {
		try {
			return this.service.deleteGroup(loginName);
		} catch (ServiceException e) {
			throw processException(e);
		}
	}

	public void deleteList(String listId) throws ShptSessionException {
		try {
			this.service.deleteList(listId);
		} catch (ServiceException e) {
			throw processException(e);
		}
	}

	public void deleteListItem(String listId, int itemId) throws ShptSessionException {
		try {
			this.service.deleteListItem(listId, itemId);
		} catch (ServiceException e) {
			throw processException(e);
		}
	}

	public void deleteRole(int roleId) throws ShptSessionException {
		try {
			this.service.deleteRole(roleId);
		} catch (ServiceException e) {
			throw processException(e);
		}
	}

	public void deleteSite() throws ShptSessionException {
		try {
			this.service.deleteSite();
		} catch (ServiceException e) {
			throw processException(e);
		}
	}

	public boolean deleteUser(int userId, int groupId) throws ShptSessionException {
		try {
			return this.service.deleteUser(userId, groupId);
		} catch (ServiceException e) {
			throw processException(e);
		}
	}

	public boolean deleteUser(String loginName, int groupId) throws ShptSessionException {
		try {
			return this.service.deleteUser(loginName, groupId);
		} catch (ServiceException e) {
			throw processException(e);
		}
	}

	public void deleteView(String listId, String viewId) throws ShptSessionException {
		try {
			this.service.deleteView(listId, viewId);
		} catch (ServiceException e) {
			throw processException(e);
		}
	}

	public boolean deleteViewField(String listId, String viewId, String fieldName) throws ShptSessionException {
		try {
			return this.service.deleteViewField(listId, viewId, fieldName);
		} catch (ServiceException e) {
			throw processException(e);
		}
	}

	public boolean deny(String filePath, String comment) throws ShptSessionException {
		try {
			return this.service.deny(filePath, comment);
		} catch (ServiceException e) {
			throw processException(e);
		}
	}

	public boolean deny(String filePath) throws ShptSessionException {
		try {
			return this.service.deny(filePath);
		} catch (ServiceException e) {
			throw processException(e);
		}
	}

	public User ensureUser(String loginName) throws ShptSessionException {
		try {
			return this.service.ensureUser(loginName);
		} catch (ServiceException e) {
			throw processException(e);
		}
	}

	@Override
	public boolean equals(Object obj) {
		if (!Tools.baseEquals(this, obj)) { return false; }
		ShptSession other = ShptSession.class.cast(obj);
		if (!Tools.equals(this.url, other.url)) { return false; }
		if (!Tools.equals(this.domain, other.domain)) { return false; }
		if (!Tools.equals(this.user, other.user)) { return false; }
		if (!Tools.equals(this.password, other.password)) { return false; }
		return true;
	}

	public Group getAssociatedMemberGroup() throws ShptSessionException {
		try {
			return this.service.getAssociatedMemberGroup();
		} catch (ServiceException e) {
			throw processException(e);
		}
	}

	public Group getAssociatedOwnerGroup() throws ShptSessionException {
		try {
			return this.service.getAssociatedOwnerGroup();
		} catch (ServiceException e) {
			throw processException(e);
		}
	}

	public Group getAssociatedVisitorGroup() throws ShptSessionException {
		try {
			return this.service.getAssociatedVisitorGroup();
		} catch (ServiceException e) {
			throw processException(e);
		}
	}

	public java.util.List<ContentType> getAvailableContentTypes() throws ShptSessionException {
		try {
			return this.service.getAvailableContentTypes();
		} catch (ServiceException e) {
			throw processException(e);
		}
	}

	public java.util.List<ContentType> getAvailableContentTypes(java.util.List<IQueryOption> queryOptions)
		throws ShptSessionException {
		try {
			return this.service.getAvailableContentTypes(queryOptions);
		} catch (ServiceException e) {
			throw processException(e);
		}
	}

	public List getCatalog(ListTemplateType listTemplate) throws ShptSessionException {
		try {
			return this.service.getCatalog(listTemplate);
		} catch (ServiceException e) {
			throw processException(e);
		}
	}

	public java.util.List<Change> getChanges(ChangeQuery query, java.util.List<IQueryOption> queryOptions)
		throws ShptSessionException {
		try {
			return this.service.getChanges(query, queryOptions);
		} catch (ServiceException e) {
			throw processException(e);
		}
	}

	public java.util.List<Change> getChanges(ChangeQuery query, String listId,
		java.util.List<IQueryOption> queryOptions) throws ShptSessionException {
		try {
			return this.service.getChanges(query, listId, queryOptions);
		} catch (ServiceException e) {
			throw processException(e);
		}
	}

	public java.util.List<Change> getChanges(ChangeQuery query, String listId) throws ShptSessionException {
		try {
			return this.service.getChanges(query, listId);
		} catch (ServiceException e) {
			throw processException(e);
		}
	}

	public java.util.List<Change> getChanges(ChangeQuery query) throws ShptSessionException {
		try {
			return this.service.getChanges(query);
		} catch (ServiceException e) {
			throw processException(e);
		}
	}

	public HttpClientConnectionManager getClientConnectionManager() {
		return this.service.getClientConnectionManager();
	}

	public int getConnectTimeout() {
		return this.service.getConnectTimeout();
	}

	public ContentType getContentType(String contentTypeId) throws ShptSessionException {
		try {
			return this.service.getContentType(contentTypeId);
		} catch (ServiceException e) {
			throw processException(e);
		}
	}

	public java.util.List<FieldLink> getContentTypeFieldLinks(String contentTypeId,
		java.util.List<IQueryOption> queryOptions) throws ShptSessionException {
		try {
			return this.service.getContentTypeFieldLinks(contentTypeId, queryOptions);
		} catch (ServiceException e) {
			throw processException(e);
		}
	}

	public java.util.List<FieldLink> getContentTypeFieldLinks(String contentTypeId) throws ShptSessionException {
		try {
			return this.service.getContentTypeFieldLinks(contentTypeId);
		} catch (ServiceException e) {
			throw processException(e);
		}
	}

	public java.util.List<Field> getContentTypeFields(String contentTypeId, java.util.List<IQueryOption> queryOptions)
		throws ShptSessionException {
		try {
			return this.service.getContentTypeFields(contentTypeId, queryOptions);
		} catch (ServiceException e) {
			throw processException(e);
		}
	}

	public java.util.List<Field> getContentTypeFields(String contentTypeId) throws ShptSessionException {
		try {
			return this.service.getContentTypeFields(contentTypeId);
		} catch (ServiceException e) {
			throw processException(e);
		}
	}

	public java.util.List<ContentType> getContentTypes() throws ShptSessionException {
		try {
			return this.service.getContentTypes();
		} catch (ServiceException e) {
			throw processException(e);
		}
	}

	public java.util.List<ContentType> getContentTypes(java.util.List<IQueryOption> queryOptions)
		throws ShptSessionException {
		try {
			return this.service.getContentTypes(queryOptions);
		} catch (ServiceException e) {
			throw processException(e);
		}
	}

	public ContextInfo getContextInfo() throws ShptSessionException {
		try {
			return this.service.getContextInfo();
		} catch (ServiceException e) {
			throw processException(e);
		}
	}

	public User getCreatedByUser(String filePath, int versionId) throws ShptSessionException {
		try {
			return this.service.getCreatedByUser(filePath, versionId);
		} catch (ServiceException e) {
			throw processException(e);
		}
	}

	public User getCurrentUser() throws ShptSessionException {
		try {
			return this.service.getCurrentUser();
		} catch (ServiceException e) {
			throw processException(e);
		}
	}

	public java.util.List<ListTemplate> getCustomListTemplates() throws ShptSessionException {
		try {
			return this.service.getCustomListTemplates();
		} catch (ServiceException e) {
			throw processException(e);
		}
	}

	public java.util.List<ListTemplate> getCustomListTemplates(java.util.List<IQueryOption> queryOptions)
		throws ShptSessionException {
		try {
			return this.service.getCustomListTemplates(queryOptions);
		} catch (ServiceException e) {
			throw processException(e);
		}
	}

	public String getDomain() {
		return this.service.getDomain();
	}

	public java.util.List<BasePermission> getEffectiveBasePermissions() throws ShptSessionException {
		try {
			return this.service.getEffectiveBasePermissions();
		} catch (ServiceException e) {
			throw processException(e);
		}
	}

	public EventReceiver getEventReceiver(String receiverId) throws ShptSessionException {
		try {
			return this.service.getEventReceiver(receiverId);
		} catch (ServiceException e) {
			throw processException(e);
		}
	}

	public java.util.List<EventReceiver> getEventReceivers() throws ShptSessionException {
		try {
			return this.service.getEventReceivers();
		} catch (ServiceException e) {
			throw processException(e);
		}
	}

	public java.util.List<EventReceiver> getEventReceivers(java.util.List<IQueryOption> queryOptions)
		throws ShptSessionException {
		try {
			return this.service.getEventReceivers(queryOptions);
		} catch (ServiceException e) {
			throw processException(e);
		}
	}

	public java.util.List<String> getFeatures() throws ShptSessionException {
		try {
			return this.service.getFeatures();
		} catch (ServiceException e) {
			throw processException(e);
		}
	}

	public java.util.List<String> getFeatures(java.util.List<IQueryOption> queryOptions) throws ShptSessionException {
		try {
			return this.service.getFeatures(queryOptions);
		} catch (ServiceException e) {
			throw processException(e);
		}
	}

	public Field getField(String id, String listId) throws ShptSessionException {
		try {
			return this.service.getField(id, listId);
		} catch (ServiceException e) {
			throw processException(e);
		}
	}

	public Field getField(String id) throws ShptSessionException {
		try {
			return this.service.getField(id);
		} catch (ServiceException e) {
			throw processException(e);
		}
	}

	public Field getFieldByInternalNameOrTitle(String name, String listId) throws ShptSessionException {
		try {
			return this.service.getFieldByInternalNameOrTitle(name, listId);
		} catch (ServiceException e) {
			throw processException(e);
		}
	}

	public Field getFieldByInternalNameOrTitle(String name) throws ShptSessionException {
		try {
			return this.service.getFieldByInternalNameOrTitle(name);
		} catch (ServiceException e) {
			throw processException(e);
		}
	}

	public Field getFieldByTitle(String title, String listId) throws ShptSessionException {
		try {
			return this.service.getFieldByTitle(title, listId);
		} catch (ServiceException e) {
			throw processException(e);
		}
	}

	public Field getFieldByTitle(String title) throws ShptSessionException {
		try {
			return this.service.getFieldByTitle(title);
		} catch (ServiceException e) {
			throw processException(e);
		}
	}

	public java.util.List<FieldValue> getFieldValues(String listId, int itemId,
		java.util.List<IQueryOption> queryOptions) throws ShptSessionException {
		try {
			return this.service.getFieldValues(listId, itemId, queryOptions);
		} catch (ServiceException e) {
			throw processException(e);
		}
	}

	public java.util.List<FieldValue> getFieldValues(String listId, int itemId) throws ShptSessionException {
		try {
			return this.service.getFieldValues(listId, itemId);
		} catch (ServiceException e) {
			throw processException(e);
		}
	}

	public java.util.List<Field> getFields() throws ShptSessionException {
		try {
			return this.service.getFields();
		} catch (ServiceException e) {
			throw processException(e);
		}
	}

	public java.util.List<Field> getFields(java.util.List<IQueryOption> queryOptions) throws ShptSessionException {
		try {
			return this.service.getFields(queryOptions);
		} catch (ServiceException e) {
			throw processException(e);
		}
	}

	public File getFile(String filePath) throws ShptSessionException {
		try {
			return this.service.getFile(filePath);
		} catch (ServiceException e) {
			throw processException(e);
		}
	}

	public User getFileAuthor(String filePath) throws ShptSessionException {
		try {
			return this.service.getFileAuthor(filePath);
		} catch (ServiceException e) {
			throw processException(e);
		}
	}

	public byte[] getFileContent(String filePath) throws ShptSessionException {
		try {
			return this.service.getFileContent(filePath);
		} catch (ServiceException e) {
			throw processException(e);
		}
	}

	public InputStream getFileStream(String filePath) throws ShptSessionException {
		try {
			return this.service.getFileStream(filePath);
		} catch (ServiceException e) {
			throw processException(e);
		}
	}

	public FileVersion getFileVersion(String filePath, int versionId) throws ShptSessionException {
		try {
			return this.service.getFileVersion(filePath, versionId);
		} catch (ServiceException e) {
			throw processException(e);
		}
	}

	public java.util.List<FileVersion> getFileVersions(String filePath, java.util.List<IQueryOption> queryOptions)
		throws ShptSessionException {
		try {
			return this.service.getFileVersions(filePath, queryOptions);
		} catch (ServiceException e) {
			throw processException(e);
		}
	}

	public java.util.List<FileVersion> getFileVersions(String filePath) throws ShptSessionException {
		try {
			return this.service.getFileVersions(filePath);
		} catch (ServiceException e) {
			throw processException(e);
		}
	}

	public java.util.List<File> getFiles(String folderPath, java.util.List<IQueryOption> queryOptions)
		throws ShptSessionException {
		try {
			return this.service.getFiles(folderPath, queryOptions);
		} catch (ServiceException e) {
			throw processException(e);
		}
	}

	public java.util.List<File> getFiles(String folderPath) throws ShptSessionException {
		try {
			return this.service.getFiles(folderPath);
		} catch (ServiceException e) {
			throw processException(e);
		}
	}

	public Folder getFolder(String folderPath) throws ShptSessionException {
		try {
			return this.service.getFolder(folderPath);
		} catch (ServiceException e) {
			throw processException(e);
		}
	}

	public java.util.List<Folder> getFolders(String parentFolder, java.util.List<IQueryOption> queryOptions)
		throws ShptSessionException {
		try {
			return this.service.getFolders(parentFolder, queryOptions);
		} catch (ServiceException e) {
			throw processException(e);
		}
	}

	public java.util.List<Folder> getFolders(String parentFolder) throws ShptSessionException {
		try {
			return this.service.getFolders(parentFolder);
		} catch (ServiceException e) {
			throw processException(e);
		}
	}

	public Group getGroup(int groupId) throws ShptSessionException {
		try {
			return this.service.getGroup(groupId);
		} catch (ServiceException e) {
			throw processException(e);
		}
	}

	public Group getGroup(String loginName) throws ShptSessionException {
		try {
			return this.service.getGroup(loginName);
		} catch (ServiceException e) {
			throw processException(e);
		}
	}

	public User getGroupOwner(int groupId) throws ShptSessionException {
		try {
			return this.service.getGroupOwner(groupId);
		} catch (ServiceException e) {
			throw processException(e);
		}
	}

	public User getGroupOwner(String loginName) throws ShptSessionException {
		try {
			return this.service.getGroupOwner(loginName);
		} catch (ServiceException e) {
			throw processException(e);
		}
	}

	public java.util.List<User> getGroupUsers(int groupId, java.util.List<IQueryOption> queryOptions)
		throws ShptSessionException {
		try {
			return this.service.getGroupUsers(groupId, queryOptions);
		} catch (ServiceException e) {
			throw processException(e);
		}
	}

	public java.util.List<User> getGroupUsers(int groupId) throws ShptSessionException {
		try {
			return this.service.getGroupUsers(groupId);
		} catch (ServiceException e) {
			throw processException(e);
		}
	}

	public java.util.List<User> getGroupUsers(String loginName, java.util.List<IQueryOption> queryOptions)
		throws ShptSessionException {
		try {
			return this.service.getGroupUsers(loginName, queryOptions);
		} catch (ServiceException e) {
			throw processException(e);
		}
	}

	public java.util.List<User> getGroupUsers(String loginName) throws ShptSessionException {
		try {
			return this.service.getGroupUsers(loginName);
		} catch (ServiceException e) {
			throw processException(e);
		}
	}

	public java.util.List<Group> getGroups() throws ShptSessionException {
		try {
			return this.service.getGroups();
		} catch (ServiceException e) {
			throw processException(e);
		}
	}

	public java.util.List<Group> getGroups(java.util.List<IQueryOption> queryOptions) throws ShptSessionException {
		try {
			return this.service.getGroups(queryOptions);
		} catch (ServiceException e) {
			throw processException(e);
		}
	}

	public Proxy getHttpURLConnectionProxy() {
		return this.service.getHttpURLConnectionProxy();
	}

	public InputStream getInputStream(String url) throws ShptSessionException {
		try {
			return this.service.getInputStream(url);
		} catch (ServiceException e) {
			ShptSessionException rethrowable = processException(e, true);
			String errorString = Tools.coalesce(e.getErrorString(), "");
			errorString = errorString.toLowerCase();
			if (!errorString.startsWith("400 ") && !errorString.startsWith("404 ")) {
				//
				throw rethrowable;
			}

			// Ok...so...let's try to reprocess, as a last resort...
			if (this.log.isTraceEnabled()) {
				this.log.trace(String.format("Will reprocess URL [%s] for getInputStream() invocation", url));
			}
			java.util.List<String> items = new ArrayList<>();
			for (String s : FileNameTools.tokenize(url, '/')) {
				try {
					String S = URLEncoder.encode(s, ShptSession.URL_ENCODING);
					S = S.replaceAll("\\+", "%20");
					items.add(S);
				} catch (UnsupportedEncodingException e2) {
					throw new ShptSessionException(String.format("%s encoding is not supported (while encoding [%s])",
						ShptSession.URL_ENCODING, s), e2);
				}
			}
			final String newUrl = FileNameTools.reconstitute(items, url.startsWith("/"), url.endsWith("/"), '/');
			if (this.log.isTraceEnabled()) {
				this.log
					.trace(String.format("URL reprocessing of [%s] resulted in [%s] - invoking getInputStream(\"%s\")",
						url, newUrl, newUrl));
			}
			try {
				return this.service.getInputStream(newUrl);
			} catch (ServiceException se) {
				throw processException(se);
			}
		}
	}

	public LimitedWebPartManager getLimitedWebPartManager(String filePath, PersonalizationScope scope)
		throws ShptSessionException {
		try {
			return this.service.getLimitedWebPartManager(filePath, scope);
		} catch (ServiceException e) {
			throw processException(e);
		}
	}

	public LimitedWebPartManager getLimitedWebPartManager(String filePath) throws ShptSessionException {
		try {
			return this.service.getLimitedWebPartManager(filePath);
		} catch (ServiceException e) {
			throw processException(e);
		}
	}

	public List getList(String listId) throws ShptSessionException {
		try {
			return this.service.getList(listId);
		} catch (ServiceException e) {
			throw processException(e);
		}
	}

	public List getListByTitle(String title) throws ShptSessionException {
		try {
			return this.service.getListByTitle(title);
		} catch (ServiceException e) {
			throw processException(e);
		}
	}

	public java.util.List<ContentType> getListContentTypes(String listId, java.util.List<IQueryOption> queryOptions)
		throws ShptSessionException {
		try {
			return this.service.getListContentTypes(listId, queryOptions);
		} catch (ServiceException e) {
			throw processException(e);
		}
	}

	public java.util.List<ContentType> getListContentTypes(String listId) throws ShptSessionException {
		try {
			return this.service.getListContentTypes(listId);
		} catch (ServiceException e) {
			throw processException(e);
		}
	}

	public View getListDefaultView(String listId) throws ShptSessionException {
		try {
			return this.service.getListDefaultView(listId);
		} catch (ServiceException e) {
			throw processException(e);
		}
	}

	public java.util.List<EventReceiver> getListEventReceivers(String listId, java.util.List<IQueryOption> queryOptions)
		throws ShptSessionException {
		try {
			return this.service.getListEventReceivers(listId, queryOptions);
		} catch (ServiceException e) {
			throw processException(e);
		}
	}

	public java.util.List<EventReceiver> getListEventReceivers(String listId) throws ShptSessionException {
		try {
			return this.service.getListEventReceivers(listId);
		} catch (ServiceException e) {
			throw processException(e);
		}
	}

	public java.util.List<Field> getListFields(String listId, java.util.List<IQueryOption> queryOptions)
		throws ShptSessionException {
		try {
			return this.service.getListFields(listId, queryOptions);
		} catch (ServiceException e) {
			throw processException(e);
		}
	}

	public java.util.List<Field> getListFields(String listId) throws ShptSessionException {
		try {
			return this.service.getListFields(listId);
		} catch (ServiceException e) {
			throw processException(e);
		}
	}

	public Form getListForm(String listId, String formId) throws ShptSessionException {
		try {
			return this.service.getListForm(listId, formId);
		} catch (ServiceException e) {
			throw processException(e);
		}
	}

	public java.util.List<Form> getListForms(String listId, java.util.List<IQueryOption> queryOptions)
		throws ShptSessionException {
		try {
			return this.service.getListForms(listId, queryOptions);
		} catch (ServiceException e) {
			throw processException(e);
		}
	}

	public java.util.List<Form> getListForms(String listId) throws ShptSessionException {
		try {
			return this.service.getListForms(listId);
		} catch (ServiceException e) {
			throw processException(e);
		}
	}

	public ListItem getListItem(String listId, int itemId) throws ShptSessionException {
		try {
			return this.service.getListItem(listId, itemId);
		} catch (ServiceException e) {
			throw processException(e);
		}
	}

	public java.util.List<Attachment> getListItemAttachments(String listId, int itemId,
		java.util.List<IQueryOption> queryOptions) throws ShptSessionException {
		try {
			return this.service.getListItemAttachments(listId, itemId, queryOptions);
		} catch (ServiceException e) {
			throw processException(e);
		}
	}

	public java.util.List<Attachment> getListItemAttachments(String listId, int itemId) throws ShptSessionException {
		try {
			return this.service.getListItemAttachments(listId, itemId);
		} catch (ServiceException e) {
			throw processException(e);
		}
	}

	public java.util.List<Change> getListItemChanges(String listId, ChangeLogItemQuery query,
		java.util.List<IQueryOption> queryOptions) throws ShptSessionException {
		try {
			return this.service.getListItemChanges(listId, query, queryOptions);
		} catch (ServiceException e) {
			throw processException(e);
		}
	}

	public java.util.List<Change> getListItemChanges(String listId, ChangeLogItemQuery query)
		throws ShptSessionException {
		try {
			return this.service.getListItemChanges(listId, query);
		} catch (ServiceException e) {
			throw processException(e);
		}
	}

	public ContentType getListItemContentType(String listId, int itemId) throws ShptSessionException {
		try {
			return this.service.getListItemContentType(listId, itemId);
		} catch (ServiceException e) {
			throw processException(e);
		}
	}

	public java.util.List<ListItem> getListItems(String listId, java.util.List<IQueryOption> queryOptions)
		throws ShptSessionException {
		try {
			return this.service.getListItems(listId, queryOptions);
		} catch (ServiceException e) {
			throw processException(e);
		}
	}

	public java.util.List<ListItem> getListItems(String listId) throws ShptSessionException {
		try {
			return this.service.getListItems(listId);
		} catch (ServiceException e) {
			throw processException(e);
		}
	}

	public RegionalSettings getListRegionalSettings(String listId) throws ShptSessionException {
		try {
			return this.service.getListRegionalSettings(listId);
		} catch (ServiceException e) {
			throw processException(e);
		}
	}

	public String getListSchemaXml(String listId) throws ShptSessionException {
		try {
			return this.service.getListSchemaXml(listId);
		} catch (ServiceException e) {
			throw processException(e);
		}
	}

	public ServerSettings getListServerSettings(String listId) throws ShptSessionException {
		try {
			return this.service.getListServerSettings(listId);
		} catch (ServiceException e) {
			throw processException(e);
		}
	}

	public ListTemplate getListTemplate(String name) throws ShptSessionException {
		try {
			return this.service.getListTemplate(name);
		} catch (ServiceException e) {
			throw processException(e);
		}
	}

	public java.util.List<ListTemplate> getListTemplates() throws ShptSessionException {
		try {
			return this.service.getListTemplates();
		} catch (ServiceException e) {
			throw processException(e);
		}
	}

	public java.util.List<ListTemplate> getListTemplates(java.util.List<IQueryOption> queryOptions)
		throws ShptSessionException {
		try {
			return this.service.getListTemplates(queryOptions);
		} catch (ServiceException e) {
			throw processException(e);
		}
	}

	public java.util.List<List> getLists() throws ShptSessionException {
		try {
			return this.service.getLists();
		} catch (ServiceException e) {
			throw processException(e);
		}
	}

	public java.util.List<List> getLists(java.util.List<IQueryOption> queryOptions) throws ShptSessionException {
		try {
			return this.service.getLists(queryOptions);
		} catch (ServiceException e) {
			throw processException(e);
		}
	}

	public User getLockedByUser(String filePath) throws ShptSessionException {
		try {
			return this.service.getLockedByUser(filePath);
		} catch (ServiceException e) {
			throw processException(e);
		}
	}

	public User getModifiedByUser(String filePath) throws ShptSessionException {
		try {
			return this.service.getModifiedByUser(filePath);
		} catch (ServiceException e) {
			throw processException(e);
		}
	}

	public NavigationNode getNavigationNode(int id) throws ShptSessionException {
		try {
			return this.service.getNavigationNode(id);
		} catch (ServiceException e) {
			throw processException(e);
		}
	}

	public java.util.List<NavigationNode> getNavigationNodeChildren(int id, java.util.List<IQueryOption> queryOptions)
		throws ShptSessionException {
		try {
			return this.service.getNavigationNodeChildren(id, queryOptions);
		} catch (ServiceException e) {
			throw processException(e);
		}
	}

	public java.util.List<NavigationNode> getNavigationNodeChildren(int id) throws ShptSessionException {
		try {
			return this.service.getNavigationNodeChildren(id);
		} catch (ServiceException e) {
			throw processException(e);
		}
	}

	public String getPassword() {
		return this.service.getPassword();
	}

	public HttpHost getProxy() {
		return this.service.getProxy();
	}

	public Credentials getProxyCredentials() {
		return this.service.getProxyCredentials();
	}

	public java.util.List<NavigationNode> getQuickLaunch() throws ShptSessionException {
		try {
			return this.service.getQuickLaunch();
		} catch (ServiceException e) {
			throw processException(e);
		}
	}

	public java.util.List<NavigationNode> getQuickLaunch(java.util.List<IQueryOption> queryOptions)
		throws ShptSessionException {
		try {
			return this.service.getQuickLaunch(queryOptions);
		} catch (ServiceException e) {
			throw processException(e);
		}
	}

	public int getReadTimeout() {
		return this.service.getReadTimeout();
	}

	public RecycleBinItem getRecycleBinItem(String id) throws ShptSessionException {
		try {
			return this.service.getRecycleBinItem(id);
		} catch (ServiceException e) {
			throw processException(e);
		}
	}

	public User getRecycleBinItemAuthor(String id) throws ShptSessionException {
		try {
			return this.service.getRecycleBinItemAuthor(id);
		} catch (ServiceException e) {
			throw processException(e);
		}
	}

	public User getRecycleBinItemDeletedBy(String id) throws ShptSessionException {
		try {
			return this.service.getRecycleBinItemDeletedBy(id);
		} catch (ServiceException e) {
			throw processException(e);
		}
	}

	public java.util.List<RecycleBinItem> getRecycleBinItems() throws ShptSessionException {
		try {
			return this.service.getRecycleBinItems();
		} catch (ServiceException e) {
			throw processException(e);
		}
	}

	public java.util.List<RecycleBinItem> getRecycleBinItems(java.util.List<IQueryOption> queryOptions)
		throws ShptSessionException {
		try {
			return this.service.getRecycleBinItems(queryOptions);
		} catch (ServiceException e) {
			throw processException(e);
		}
	}

	public RegionalSettings getRegionalSettings() throws ShptSessionException {
		try {
			return this.service.getRegionalSettings();
		} catch (ServiceException e) {
			throw processException(e);
		}
	}

	public java.util.List<Field> getRelatedFields(String listId, java.util.List<IQueryOption> queryOptions)
		throws ShptSessionException {
		try {
			return this.service.getRelatedFields(listId, queryOptions);
		} catch (ServiceException e) {
			throw processException(e);
		}
	}

	public java.util.List<Field> getRelatedFields(String listId) throws ShptSessionException {
		try {
			return this.service.getRelatedFields(listId);
		} catch (ServiceException e) {
			throw processException(e);
		}
	}

	public RequestConfig getRequestConfig() {
		return this.service.getRequestConfig();
	}

	public Role getRole(int roleId) throws ShptSessionException {
		try {
			return this.service.getRole(roleId);
		} catch (ServiceException e) {
			throw processException(e);
		}
	}

	public Role getRole(RoleType type) throws ShptSessionException {
		try {
			return this.service.getRole(type);
		} catch (ServiceException e) {
			throw processException(e);
		}
	}

	public Role getRole(String name) throws ShptSessionException {
		try {
			return this.service.getRole(name);
		} catch (ServiceException e) {
			throw processException(e);
		}
	}

	public java.util.List<Integer> getRoleAssignments() throws ShptSessionException {
		try {
			return this.service.getRoleAssignments();
		} catch (ServiceException e) {
			throw processException(e);
		}
	}

	public java.util.List<Integer> getRoleAssignments(int principalId) throws ShptSessionException {
		try {
			return this.service.getRoleAssignments(principalId);
		} catch (ServiceException e) {
			throw processException(e);
		}
	}

	public java.util.List<Integer> getRoleAssignments(java.util.List<IQueryOption> queryOptions)
		throws ShptSessionException {
		try {
			return this.service.getRoleAssignments(queryOptions);
		} catch (ServiceException e) {
			throw processException(e);
		}
	}

	public java.util.List<Role> getRoles() throws ShptSessionException {
		try {
			return this.service.getRoles();
		} catch (ServiceException e) {
			throw processException(e);
		}
	}

	public java.util.List<Role> getRoles(java.util.List<IQueryOption> queryOptions) throws ShptSessionException {
		try {
			return this.service.getRoles(queryOptions);
		} catch (ServiceException e) {
			throw processException(e);
		}
	}

	public Folder getRootFolder() throws ShptSessionException {
		try {
			return this.service.getRootFolder();
		} catch (ServiceException e) {
			throw processException(e);
		}
	}

	public Site getSite() throws ShptSessionException {
		try {
			return this.service.getSite();
		} catch (ServiceException e) {
			throw processException(e);
		}
	}

	public java.util.List<SiteInfo> getSiteInfos() throws ShptSessionException {
		try {
			return this.service.getSiteInfos();
		} catch (ServiceException e) {
			throw processException(e);
		}
	}

	public java.util.List<SiteInfo> getSiteInfos(java.util.List<IQueryOption> queryOptions)
		throws ShptSessionException {
		try {
			return this.service.getSiteInfos(queryOptions);
		} catch (ServiceException e) {
			throw processException(e);
		}
	}

	public java.util.List<SiteTemplate> getSiteTemplates(Locale locale, boolean includeCrossLanguage,
		java.util.List<IQueryOption> queryOptions) throws ShptSessionException {
		try {
			return this.service.getSiteTemplates(locale, includeCrossLanguage, queryOptions);
		} catch (ServiceException e) {
			throw processException(e);
		}
	}

	public java.util.List<SiteTemplate> getSiteTemplates(Locale locale, boolean includeCrossLanguage)
		throws ShptSessionException {
		try {
			return this.service.getSiteTemplates(locale, includeCrossLanguage);
		} catch (ServiceException e) {
			throw processException(e);
		}
	}

	public java.util.List<SiteTemplate> getSiteTemplates(Locale locale, java.util.List<IQueryOption> queryOptions)
		throws ShptSessionException {
		try {
			return this.service.getSiteTemplates(locale, queryOptions);
		} catch (ServiceException e) {
			throw processException(e);
		}
	}

	public java.util.List<SiteTemplate> getSiteTemplates(Locale locale) throws ShptSessionException {
		try {
			return this.service.getSiteTemplates(locale);
		} catch (ServiceException e) {
			throw processException(e);
		}
	}

	public String getSiteUrl() {
		return this.service.getSiteUrl();
	}

	public java.util.List<Site> getSites() throws ShptSessionException {
		try {
			return this.service.getSites();
		} catch (ServiceException e) {
			throw processException(e);
		}
	}

	public java.util.List<Site> getSites(java.util.List<IQueryOption> queryOptions) throws ShptSessionException {
		try {
			return this.service.getSites(queryOptions);
		} catch (ServiceException e) {
			throw processException(e);
		}
	}

	public ThemeInfo getThemeInfo() throws ShptSessionException {
		try {
			return this.service.getThemeInfo();
		} catch (ServiceException e) {
			throw processException(e);
		}
	}

	public TimeZone getTimeZone() throws ShptSessionException {
		try {
			return this.service.getTimeZone();
		} catch (ServiceException e) {
			throw processException(e);
		}
	}

	public java.util.List<TimeZone> getTimeZones() throws ShptSessionException {
		try {
			return this.service.getTimeZones();
		} catch (ServiceException e) {
			throw processException(e);
		}
	}

	public java.util.List<TimeZone> getTimeZones(java.util.List<IQueryOption> queryOptions)
		throws ShptSessionException {
		try {
			return this.service.getTimeZones(queryOptions);
		} catch (ServiceException e) {
			throw processException(e);
		}
	}

	public java.util.List<NavigationNode> getTopNavigationBar() throws ShptSessionException {
		try {
			return this.service.getTopNavigationBar();
		} catch (ServiceException e) {
			throw processException(e);
		}
	}

	public java.util.List<NavigationNode> getTopNavigationBar(java.util.List<IQueryOption> queryOptions)
		throws ShptSessionException {
		try {
			return this.service.getTopNavigationBar(queryOptions);
		} catch (ServiceException e) {
			throw processException(e);
		}
	}

	public User getUser(int userId, int groupId) throws ShptSessionException {
		try {
			return this.service.getUser(userId, groupId);
		} catch (ServiceException e) {
			throw processException(e);
		}
	}

	public User getUser(int userId) throws ShptSessionException {
		try {
			return this.service.getUser(userId);
		} catch (ServiceException e) {
			throw processException(e);
		}
	}

	public User getUser(String loginName, int groupId) throws ShptSessionException {
		try {
			return this.service.getUser(loginName, groupId);
		} catch (ServiceException e) {
			throw processException(e);
		}
	}

	public User getUserByEmail(String email, int groupId) throws ShptSessionException {
		try {
			return this.service.getUserByEmail(email, groupId);
		} catch (ServiceException e) {
			throw processException(e);
		}
	}

	public java.util.List<Role> getUserCustomActions() throws ShptSessionException {
		try {
			return this.service.getUserCustomActions();
		} catch (ServiceException e) {
			throw processException(e);
		}
	}

	public java.util.List<Role> getUserCustomActions(java.util.List<IQueryOption> queryOptions)
		throws ShptSessionException {
		try {
			return this.service.getUserCustomActions(queryOptions);
		} catch (ServiceException e) {
			throw processException(e);
		}
	}

	public java.util.List<BasePermission> getUserEffectivePermissions(String listId, int itemId, String loginName)
		throws ShptSessionException {
		try {
			return this.service.getUserEffectivePermissions(listId, itemId, loginName);
		} catch (ServiceException e) {
			throw processException(e);
		}
	}

	public java.util.List<BasePermission> getUserEffectivePermissions(String listId, String loginName)
		throws ShptSessionException {
		try {
			return this.service.getUserEffectivePermissions(listId, loginName);
		} catch (ServiceException e) {
			throw processException(e);
		}
	}

	public java.util.List<Group> getUserGroups(int userId, java.util.List<IQueryOption> queryOptions)
		throws ShptSessionException {
		try {
			return this.service.getUserGroups(userId, queryOptions);
		} catch (ServiceException e) {
			throw processException(e);
		}
	}

	public java.util.List<Group> getUserGroups(int userId) throws ShptSessionException {
		try {
			return this.service.getUserGroups(userId);
		} catch (ServiceException e) {
			throw processException(e);
		}
	}

	public List getUserInfoList() throws ShptSessionException {
		try {
			return this.service.getUserInfoList();
		} catch (ServiceException e) {
			throw processException(e);
		}
	}

	public String getUsername() {
		return this.service.getUsername();
	}

	public java.util.List<User> getUsers() throws ShptSessionException {
		try {
			return this.service.getUsers();
		} catch (ServiceException e) {
			throw processException(e);
		}
	}

	public java.util.List<User> getUsers(int groupId, java.util.List<IQueryOption> queryOptions)
		throws ShptSessionException {
		try {
			return this.service.getUsers(groupId, queryOptions);
		} catch (ServiceException e) {
			throw processException(e);
		}
	}

	public java.util.List<User> getUsers(int groupId) throws ShptSessionException {
		try {
			return this.service.getUsers(groupId);
		} catch (ServiceException e) {
			throw processException(e);
		}
	}

	public java.util.List<User> getUsers(java.util.List<IQueryOption> queryOptions) throws ShptSessionException {
		try {
			return this.service.getUsers(queryOptions);
		} catch (ServiceException e) {
			throw processException(e);
		}
	}

	public View getView(String listId, String viewId) throws ShptSessionException {
		try {
			return this.service.getView(listId, viewId);
		} catch (ServiceException e) {
			throw processException(e);
		}
	}

	public View getViewByTitle(String listId, String title) throws ShptSessionException {
		try {
			return this.service.getViewByTitle(listId, title);
		} catch (ServiceException e) {
			throw processException(e);
		}
	}

	public java.util.List<String> getViewFields(String listId, String viewId, java.util.List<IQueryOption> queryOptions)
		throws ShptSessionException {
		try {
			return this.service.getViewFields(listId, viewId, queryOptions);
		} catch (ServiceException e) {
			throw processException(e);
		}
	}

	public java.util.List<String> getViewFields(String listId, String viewId) throws ShptSessionException {
		try {
			return this.service.getViewFields(listId, viewId);
		} catch (ServiceException e) {
			throw processException(e);
		}
	}

	public String getViewFieldsSchemaXml(String listId, String viewId) throws ShptSessionException {
		try {
			return this.service.getViewFieldsSchemaXml(listId, viewId);
		} catch (ServiceException e) {
			throw processException(e);
		}
	}

	public String getViewHtml(String listId, String viewId) throws ShptSessionException {
		try {
			return this.service.getViewHtml(listId, viewId);
		} catch (ServiceException e) {
			throw processException(e);
		}
	}

	public java.util.List<View> getViews(String listId, java.util.List<IQueryOption> queryOptions)
		throws ShptSessionException {
		try {
			return this.service.getViews(listId, queryOptions);
		} catch (ServiceException e) {
			throw processException(e);
		}
	}

	public java.util.List<View> getViews(String listId) throws ShptSessionException {
		try {
			return this.service.getViews(listId);
		} catch (ServiceException e) {
			throw processException(e);
		}
	}

	public java.util.List<WorkflowTemplate> getWorkflowTemplates() throws ShptSessionException {
		try {
			return this.service.getWorkflowTemplates();
		} catch (ServiceException e) {
			throw processException(e);
		}
	}

	public java.util.List<WorkflowTemplate> getWorkflowTemplates(java.util.List<IQueryOption> queryOptions)
		throws ShptSessionException {
		try {
			return this.service.getWorkflowTemplates(queryOptions);
		} catch (ServiceException e) {
			throw processException(e);
		}
	}

	@Override
	public int hashCode() {
		return this.service.hashCode();
	}

	public boolean isAcceptGzipEncoding() {
		return this.service.isAcceptGzipEncoding();
	}

	public boolean isSharedNavigation() throws ShptSessionException {
		try {
			return this.service.isSharedNavigation();
		} catch (ServiceException e) {
			throw processException(e);
		}
	}

	public boolean moveFile(String sourceFilePath, String destinationFilePath, MoveOperation operation)
		throws ShptSessionException {
		try {
			return this.service.moveFile(sourceFilePath, destinationFilePath, operation);
		} catch (ServiceException e) {
			throw processException(e);
		}
	}

	public boolean moveFile(String sourceFilePath, String destinationFilePath) throws ShptSessionException {
		try {
			return this.service.moveFile(sourceFilePath, destinationFilePath);
		} catch (ServiceException e) {
			throw processException(e);
		}
	}

	public boolean moveViewField(String listId, String viewId, String fieldName, int index)
		throws ShptSessionException {
		try {
			return this.service.moveViewField(listId, viewId, fieldName, index);
		} catch (ServiceException e) {
			throw processException(e);
		}
	}

	public boolean publish(String filePath, String comment) throws ShptSessionException {
		try {
			return this.service.publish(filePath, comment);
		} catch (ServiceException e) {
			throw processException(e);
		}
	}

	public boolean publish(String filePath) throws ShptSessionException {
		try {
			return this.service.publish(filePath);
		} catch (ServiceException e) {
			throw processException(e);
		}
	}

	public String recycleFile(String filePath) throws ShptSessionException {
		try {
			return this.service.recycleFile(filePath);
		} catch (ServiceException e) {
			throw processException(e);
		}
	}

	public String recycleFolder(String folderPath) throws ShptSessionException {
		try {
			return this.service.recycleFolder(folderPath);
		} catch (ServiceException e) {
			throw processException(e);
		}
	}

	public String recycleList(String listId) throws ShptSessionException {
		try {
			return this.service.recycleList(listId);
		} catch (ServiceException e) {
			throw processException(e);
		}
	}

	public String recycleListItem(String listId, int itemId) throws ShptSessionException {
		try {
			return this.service.recycleListItem(listId, itemId);
		} catch (ServiceException e) {
			throw processException(e);
		}
	}

	public boolean removeRoleAssignment(int principalId, int roleId) throws ShptSessionException {
		try {
			return this.service.removeRoleAssignment(principalId, roleId);
		} catch (ServiceException e) {
			throw processException(e);
		}
	}

	public String renderListData(String listId, String viewXml) throws ShptSessionException {
		try {
			return this.service.renderListData(listId, viewXml);
		} catch (ServiceException e) {
			throw processException(e);
		}
	}

	public String renderListFormData(String listId, int itemId, String formId, ControlMode mode)
		throws ShptSessionException {
		try {
			return this.service.renderListFormData(listId, itemId, formId, mode);
		} catch (ServiceException e) {
			throw processException(e);
		}
	}

	public int reserveListItemId(String listId) throws ShptSessionException {
		try {
			return this.service.reserveListItemId(listId);
		} catch (ServiceException e) {
			throw processException(e);
		}
	}

	public boolean resetRoleInheritance(String listId, int itemId) throws ShptSessionException {
		try {
			return this.service.resetRoleInheritance(listId, itemId);
		} catch (ServiceException e) {
			throw processException(e);
		}
	}

	public boolean restoreFileVersion(String filePath, String versionLabel) throws ShptSessionException {
		try {
			return this.service.restoreFileVersion(filePath, versionLabel);
		} catch (ServiceException e) {
			throw processException(e);
		}
	}

	public SearchResult search(com.independentsoft.share.fql.IRestriction restriction) throws ShptSessionException {
		try {
			return this.service.search(restriction);
		} catch (ServiceException e) {
			throw processException(e);
		}
	}

	public SearchResult search(com.independentsoft.share.kql.IRestriction restriction) throws ShptSessionException {
		try {
			return this.service.search(restriction);
		} catch (ServiceException e) {
			throw processException(e);
		}
	}

	public SearchResult search(SearchQuery query) throws ShptSessionException {
		try {
			return this.service.search(query);
		} catch (ServiceException e) {
			throw processException(e);
		}
	}

	public SearchResult search(String query) throws ShptSessionException {
		try {
			return this.service.search(query);
		} catch (ServiceException e) {
			throw processException(e);
		}
	}

	public void setAcceptGzipEncoding(boolean acceptGzipEncoding) {
		this.service.setAcceptGzipEncoding(acceptGzipEncoding);
	}

	public void setClientConnectionManager(HttpClientConnectionManager connectionManager) {
		this.service.setClientConnectionManager(connectionManager);
	}

	public void setConnectTimeout(int connectTimeout) {
		this.service.setConnectTimeout(connectTimeout);
	}

	public void setDomain(String domain) {
		this.service.setDomain(domain);
	}

	public void setFieldValue(String listId, int listItemId, FieldValue fieldValue) throws ShptSessionException {
		try {
			this.service.setFieldValue(listId, listItemId, fieldValue);
		} catch (ServiceException e) {
			throw processException(e);
		}
	}

	public void setFieldValues(String listId, int listItemId, java.util.List<FieldValue> fieldValues)
		throws ShptSessionException {
		try {
			this.service.setFieldValues(listId, listItemId, fieldValues);
		} catch (ServiceException e) {
			throw processException(e);
		}
	}

	public void setHttpURLConnection(boolean useHttpURLConnection) {
		this.service.setHttpURLConnection(useHttpURLConnection);
	}

	public void setHttpURLConnectionProxy(Proxy httpURLConnectionProxy) {
		this.service.setHttpURLConnectionProxy(httpURLConnectionProxy);
	}

	public void setPassword(String password) {
		this.service.setPassword(password);
	}

	public void setProxy(HttpHost proxy) {
		this.service.setProxy(proxy);
	}

	public void setProxyCredentials(Credentials proxyCredentials) {
		this.service.setProxyCredentials(proxyCredentials);
	}

	public void setReadTimeout(int readTimeout) {
		this.service.setReadTimeout(readTimeout);
	}

	public void setRequestConfig(RequestConfig requestConfig) {
		this.service.setRequestConfig(requestConfig);
	}

	public boolean setShowInDisplayForm(String id, String listId, boolean show) throws ShptSessionException {
		try {
			return this.service.setShowInDisplayForm(id, listId, show);
		} catch (ServiceException e) {
			throw processException(e);
		}
	}

	public boolean setShowInDisplayForm(String id, String listId) throws ShptSessionException {
		try {
			return this.service.setShowInDisplayForm(id, listId);
		} catch (ServiceException e) {
			throw processException(e);
		}
	}

	public boolean setShowInEditForm(String id, String listId, boolean show) throws ShptSessionException {
		try {
			return this.service.setShowInEditForm(id, listId, show);
		} catch (ServiceException e) {
			throw processException(e);
		}
	}

	public boolean setShowInEditForm(String id, String listId) throws ShptSessionException {
		try {
			return this.service.setShowInEditForm(id, listId);
		} catch (ServiceException e) {
			throw processException(e);
		}
	}

	public boolean setShowInNewForm(String id, String listId, boolean show) throws ShptSessionException {
		try {
			return this.service.setShowInNewForm(id, listId, show);
		} catch (ServiceException e) {
			throw processException(e);
		}
	}

	public boolean setShowInNewForm(String id, String listId) throws ShptSessionException {
		try {
			return this.service.setShowInNewForm(id, listId);
		} catch (ServiceException e) {
			throw processException(e);
		}
	}

	public void setUsername(String username) {
		this.service.setUsername(username);
	}

	public void setsiteUrl(String siteUrl) {
		this.service.setsiteUrl(siteUrl);
	}

	public SuggestResult suggest(SearchQuerySuggestion suggestion) throws ShptSessionException {
		try {
			return this.service.suggest(suggestion);
		} catch (ServiceException e) {
			throw processException(e);
		}
	}

	public SuggestResult suggest(String query) throws ShptSessionException {
		try {
			return this.service.suggest(query);
		} catch (ServiceException e) {
			throw processException(e);
		}
	}

	@Override
	public String toString() {
		return this.service.toString();
	}

	public boolean undoCheckOut(String filePath) throws ShptSessionException {
		try {
			return this.service.undoCheckOut(filePath);
		} catch (ServiceException e) {
			throw processException(e);
		}
	}

	public boolean unpublish(String filePath, String comment) throws ShptSessionException {
		try {
			return this.service.unpublish(filePath, comment);
		} catch (ServiceException e) {
			throw processException(e);
		}
	}

	public boolean unpublish(String filePath) throws ShptSessionException {
		try {
			return this.service.unpublish(filePath);
		} catch (ServiceException e) {
			throw processException(e);
		}
	}

	public Field updateField(Field field, String listId) throws ShptSessionException {
		try {
			return this.service.updateField(field, listId);
		} catch (ServiceException e) {
			throw processException(e);
		}
	}

	public void updateFileContent(String filePath, byte[] buffer) throws ShptSessionException {
		try {
			this.service.updateFileContent(filePath, buffer);
		} catch (ServiceException e) {
			throw processException(e);
		}
	}

	public void updateFileContent(String filePath, InputStream stream) throws ShptSessionException {
		try {
			this.service.updateFileContent(filePath, stream);
		} catch (ServiceException e) {
			throw processException(e);
		}
	}

	public void updateFolder(Folder folder) throws ShptSessionException {
		try {
			this.service.updateFolder(folder);
		} catch (ServiceException e) {
			throw processException(e);
		}
	}

	public void updateGroup(Group group) throws ShptSessionException {
		try {
			this.service.updateGroup(group);
		} catch (ServiceException e) {
			throw processException(e);
		}
	}

	public void updateList(List list) throws ShptSessionException {
		try {
			this.service.updateList(list);
		} catch (ServiceException e) {
			throw processException(e);
		}
	}

	public void updateListItem(String listId, ListItem listItem) throws ShptSessionException {
		try {
			this.service.updateListItem(listId, listItem);
		} catch (ServiceException e) {
			throw processException(e);
		}
	}

	public void updateRole(Role role) throws ShptSessionException {
		try {
			this.service.updateRole(role);
		} catch (ServiceException e) {
			throw processException(e);
		}
	}

	public void updateSite(Site site) throws ShptSessionException {
		try {
			this.service.updateSite(site);
		} catch (ServiceException e) {
			throw processException(e);
		}
	}

	public User updateUser(User user, int groupId) throws ShptSessionException {
		try {
			return this.service.updateUser(user, groupId);
		} catch (ServiceException e) {
			throw processException(e);
		}
	}

	public void updateView(String listId, View view) throws ShptSessionException {
		try {
			this.service.updateView(listId, view);
		} catch (ServiceException e) {
			throw processException(e);
		}
	}
}