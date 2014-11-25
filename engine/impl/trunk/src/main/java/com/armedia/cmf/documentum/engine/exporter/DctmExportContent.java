/**
 *
 */

package com.armedia.cmf.documentum.engine.exporter;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import com.armedia.cmf.documentum.engine.DctmAttributes;
import com.armedia.cmf.documentum.engine.DctmObjectType;
import com.armedia.cmf.engine.exporter.ExportException;
import com.armedia.cmf.engine.exporter.ExportTarget;
import com.armedia.cmf.storage.ContentStore;
import com.armedia.cmf.storage.ContentStore.Handle;
import com.armedia.cmf.storage.StoredObjectType;
import com.documentum.fc.client.DfIdNotFoundException;
import com.documentum.fc.client.IDfPersistentObject;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.client.IDfSysObject;
import com.documentum.fc.client.content.IDfContent;
import com.documentum.fc.common.DfId;

/**
 * @author diego
 *
 */
public class DctmExportContent extends DctmExportAbstract<IDfContent> {

	protected DctmExportContent(DctmExportEngine engine) {
		super(engine, DctmObjectType.CONTENT);
	}

	@Override
	protected Handle doStoreContent(IDfSession session, ExportTarget referrent, IDfContent content,
		ContentStore streamStore) throws Exception {
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
		final IDfSysObject sysObject = IDfSysObject.class.cast(sourceObject);

		String format = content.getString(DctmAttributes.FULL_FORMAT);
		int pageNumber = content.getInt(DctmAttributes.PAGE);
		String pageModifier = content.getString(DctmAttributes.PAGE_MODIFIER);
		String qualifier = (!StringUtils.isBlank(pageModifier) ? String.format("[%04d]_%s.%s", pageNumber,
			pageModifier, format) : String.format("[%04d].%s", pageNumber, format));

		// Store the content in the filesystem
		Handle contentHandle = streamStore.getHandle(StoredObjectType.CONTENT, contentId, qualifier);
		final File targetFile = contentHandle.getFile();
		if (targetFile != null) {
			final File parent = targetFile.getParentFile();
			// Deal with a race condition with multiple threads trying to export to the same folder
			if (!parent.exists()) {
				IOException caught = null;
				for (int i = 0; (i < 3); i++) {
					if (i > 0) {
						// Only sleep if this is a retry
						try {
							Thread.sleep(333);
						} catch (InterruptedException e2) {
							// Ignore...
						}
					}

					try {
						caught = null;
						FileUtils.forceMkdir(parent);
						break;
					} catch (IOException e) {
						// Something went wrong...
						caught = e;
					}
				}
				if (caught != null) { throw new ExportException(String.format(
					"Failed to create the parent content directory [%s]", parent.getAbsolutePath()), caught); }
			}

			if (!parent.isDirectory()) { throw new ExportException(String.format(
				"The parent location [%s] is not a directory", parent.getAbsoluteFile())); }

			sysObject.getFileEx2(targetFile.getAbsolutePath(), format, pageNumber, pageModifier, false);
		} else {
			// Doesn't support file-level, so we (sadly) use stream-level transfers
			InputStream in = null;
			OutputStream out = contentHandle.openOutput();
			try {
				// Don't pull the content until we're sure we can put it somewhere...
				in = sysObject.getContentEx3(format, pageNumber, pageModifier, false);
				IOUtils.copy(in, out);
			} finally {
				IOUtils.closeQuietly(in);
				IOUtils.closeQuietly(out);
			}
		}
		return contentHandle;
	}
}