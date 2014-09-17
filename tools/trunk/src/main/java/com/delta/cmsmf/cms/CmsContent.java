/**
 *
 */

package com.delta.cmsmf.cms;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.UUID;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import com.delta.cmsmf.exception.CMSMFException;
import com.delta.cmsmf.utils.CMSMFUtils;
import com.documentum.fc.client.IDfPersistentObject;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.client.IDfSysObject;
import com.documentum.fc.client.content.IDfContent;
import com.documentum.fc.common.DfException;
import com.documentum.fc.common.IDfValue;

/**
 * @author Diego Rivera &lt;diego.rivera@armedia.com&gt;
 *
 */
public class CmsContent extends CmsObject<IDfContent> {

	static String FS_PATH = "fileSystemPath";
	static String DOCUMENT_ID = UUID.randomUUID().toString();

	public CmsContent() {
		super(IDfContent.class);
	}

	@Override
	protected String calculateLabel(IDfContent content) throws DfException {
		return content.getObjectId().getId();
	}

	@Override
	protected boolean skipImport(CmsTransferContext ctx) throws DfException {
		return true;
	}

	@Override
	protected void getDataProperties(Collection<CmsProperty> properties, IDfContent content) throws DfException {
		String contentId = content.getObjectId().getId();
		String format = content.getString(CmsAttributes.FULL_FORMAT);
		int pageNumber = content.getInt(CmsAttributes.PAGE);
		String pageModifier = content.getString(CmsAttributes.PAGE_MODIFIER);

		File location = CMSMFUtils.getContentDirectory(contentId);
		String fileName = String.format("%s[%04d].%s", contentId, pageNumber, format);
		if (StringUtils.isNotBlank(pageModifier)) {
			fileName = String.format("%s_%s", fileName, pageModifier);
		}
		location = new File(location, fileName);
		LinkedList<IDfValue> components = new LinkedList<IDfValue>();
		while (location != null) {
			components.addFirst(DfValueFactory.newStringValue(location.getName()));
			location = location.getParentFile();
		}
		CmsProperty paths = new CmsProperty(CmsContent.FS_PATH, CmsDataType.DF_STRING, true, components);
		properties.add(paths);

		// We need to stow the file such that when it's retrieved, it doesn't matter the OS
		// in which it's retrieved, it makes sense. So, we'll use a URL format for it
		// We use a manager so we don't have to mess with
	}

	public File getFsPath() {
		CmsProperty path = getProperty(CmsContent.FS_PATH);
		if (path == null) { return null; }
		File target = null;
		for (IDfValue v : path) {
			if (target != null) {
				target = new File(target, v.asString());
			} else {
				target = new File(v.asString());
			}
		}
		return target;
	}

	@Override
	protected void doPersistDependents(IDfContent content, CmsTransferContext ctx,
		CmsDependencyManager dependencyManager) throws DfException, CMSMFException {
		final String contentId = content.getObjectId().getId();
		IDfValue documentId = ctx.getValue(CmsContent.DOCUMENT_ID);
		if (documentId == null) { throw new CMSMFException(String.format(
			"Could not locate the document ID in the context, for which content [%s] is to be exported", contentId)); }
		final IDfSession session = content.getSession();
		final IDfPersistentObject document = session.getObject(documentId.asId());
		if (document == null) { throw new CMSMFException(String.format(
			"Failed to locate document with id [%s] for content [%s]", documentId, contentId)); }
		if (!(document instanceof IDfSysObject)) { throw new CMSMFException(String.format(
			"Document with id [%s] for content [%s] is not a dm_sysobject: %s (%s)", documentId, contentId, document
			.getType().getName(), document.getClass().getCanonicalName())); }

		String format = content.getString(CmsAttributes.FULL_FORMAT);
		int pageNumber = content.getInt(CmsAttributes.PAGE);
		String pageModifier = content.getString(CmsAttributes.PAGE_MODIFIER);
		// Store the content in the filesystem
		File fsPath = getFsPath();
		try {
			File targetFile = ctx.getFileSystem().getContentFile(fsPath);
			File parent = targetFile.getParentFile();
			FileUtils.forceMkdir(parent);
			IDfSysObject.class.cast(document).getFileEx2(targetFile.getCanonicalPath(), format, pageNumber,
				pageModifier, false);
		} catch (IOException e) {
			throw new CMSMFException(String.format("IOException attempting to persist content to ${contentDir}/%s",
				fsPath.getPath()), e);
		}
	}

	@Override
	protected IDfContent locateInCms(CmsTransferContext ctx) throws DfException, CMSMFException {
		throw new CMSMFException("Content should not be handled directly");
	}
}