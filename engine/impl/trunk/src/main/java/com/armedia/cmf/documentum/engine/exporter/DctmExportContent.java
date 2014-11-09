/**
 *
 */

package com.armedia.cmf.documentum.engine.exporter;

import java.io.File;
import java.util.Collection;
import java.util.LinkedList;
import java.util.UUID;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import com.armedia.cmf.documentum.engine.DctmAttributes;
import com.armedia.cmf.documentum.engine.DctmDataType;
import com.armedia.cmf.documentum.engine.DctmObjectType;
import com.armedia.cmf.documentum.engine.DfUtils;
import com.armedia.cmf.documentum.engine.DfValueFactory;
import com.armedia.cmf.engine.exporter.ExportTarget;
import com.armedia.cmf.storage.ContentStreamStore;
import com.armedia.cmf.storage.StoredProperty;
import com.documentum.fc.client.DfIdNotFoundException;
import com.documentum.fc.client.IDfPersistentObject;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.client.IDfSysObject;
import com.documentum.fc.client.content.IDfContent;
import com.documentum.fc.common.DfException;
import com.documentum.fc.common.DfId;
import com.documentum.fc.common.IDfValue;

/**
 * @author diego
 *
 */
public class DctmExportContent extends DctmExportAbstract<IDfContent> {

	static String FS_PATH = "fileSystemPath";
	static String DOCUMENT_ID = UUID.randomUUID().toString();

	protected DctmExportContent(DctmExportEngine engine) {
		super(engine, DctmObjectType.CONTENT);
	}

	@Override
	protected void getDataProperties(Collection<StoredProperty<IDfValue>> properties, IDfContent content)
		throws DfException {
		String contentId = content.getObjectId().getId();
		String format = content.getString(DctmAttributes.FULL_FORMAT);
		int pageNumber = content.getInt(DctmAttributes.PAGE);
		String pageModifier = content.getString(DctmAttributes.PAGE_MODIFIER);

		File location = DfUtils.getContentDirectory(contentId);
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
		StoredProperty<IDfValue> paths = new StoredProperty<IDfValue>(DctmExportContent.FS_PATH,
			DctmDataType.DF_STRING.getStoredType(), true, components);
		properties.add(paths);

		// We need to stow the file such that when it's retrieved, it doesn't matter the OS
		// in which it's retrieved, it makes sense. So, we'll use a URL format for it
		// We use a manager so we don't have to mess with
	}

	@Override
	protected String doStoreContent(IDfSession session, ExportTarget referrent, IDfContent content,
		ContentStreamStore streamStore) throws Exception {
		final String contentId = content.getObjectId().getId();
		if (referrent == null) { throw new Exception(String.format(
			"Could not locate the referrent document for which content [%s] was to be exported", contentId)); }

		final String documentId = referrent.getId();
		final IDfPersistentObject sourceObject;
		try {
			sourceObject = session.getObject(new DfId(documentId));
		} catch (DfIdNotFoundException e) {
			throw new Exception(String.format("Failed to locate document with id [%s] for content [%s]", documentId,
				contentId));
		}

		if (!(sourceObject instanceof IDfSysObject)) { throw new Exception(String.format(
			"Document with id [%s] for content [%s] is not a dm_sysobject: %s (%s)", documentId, contentId,
			sourceObject.getType().getName(), sourceObject.getClass().getCanonicalName())); }

		String format = content.getString(DctmAttributes.FULL_FORMAT);
		int pageNumber = content.getInt(DctmAttributes.PAGE);
		String pageModifier = content.getString(DctmAttributes.PAGE_MODIFIER);

		// Store the content in the filesystem
		File targetFile = streamStore.getStreamLocation(marshaled);
		File parent = targetFile.getParentFile();
		FileUtils.forceMkdir(parent);

		IDfSysObject.class.cast(sourceObject).getFileEx2(targetFile.getCanonicalPath(), format, pageNumber,
			pageModifier, false);
		return null;
	}
}