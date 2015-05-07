package com.armedia.cmf.engine.local.exporter;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.activation.MimeType;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import com.armedia.cmf.engine.ContentInfo;
import com.armedia.cmf.engine.exporter.ExportDelegate;
import com.armedia.cmf.engine.exporter.ExportException;
import com.armedia.cmf.engine.exporter.ExportTarget;
import com.armedia.cmf.engine.local.common.FilePointer;
import com.armedia.cmf.engine.local.common.LocalSessionWrapper;
import com.armedia.cmf.engine.tools.MimeTools;
import com.armedia.cmf.storage.ContentStore;
import com.armedia.cmf.storage.ContentStore.Handle;
import com.armedia.cmf.storage.StoredObject;
import com.armedia.cmf.storage.StoredObjectType;
import com.armedia.cmf.storage.StoredValue;

public class LocalExportDelegate
	extends
	ExportDelegate<FilePointer, File, LocalSessionWrapper, StoredValue, LocalExportContext, LocalExportDelegateFactory, LocalExportEngine> {

	protected LocalExportDelegate(LocalExportDelegateFactory factory, FilePointer object) throws Exception {
		super(factory, FilePointer.class, object);
	}

	@Override
	protected Collection<LocalExportDelegate> identifyRequirements(StoredObject<StoredValue> marshalled,
		LocalExportContext ctx) throws Exception {
		return null;
	}

	@Override
	protected void marshal(LocalExportContext ctx, StoredObject<StoredValue> object) throws ExportException {
	}

	@Override
	protected Collection<LocalExportDelegate> identifyDependents(StoredObject<StoredValue> marshalled,
		LocalExportContext ctx) throws Exception {
		return null;
	}

	@Override
	protected List<ContentInfo> storeContent(File session, StoredObject<StoredValue> marshalled,
		ExportTarget referrent, ContentStore streamStore) throws Exception {
		if (getType() != StoredObjectType.DOCUMENT) { return null; }

		List<ContentInfo> ret = new ArrayList<ContentInfo>(1);
		ContentInfo info = new ContentInfo(null);
		File src = this.object.getFile();
		MimeType type = null;
		try {
			type = MimeTools.determineMimeType(src);
		} catch (IOException e) {
			type = MimeTools.DEFAULT_MIME_TYPE;
		}
		// TODO: add the attributes...
		info.setProperty("mimeType", type.getBaseType());
		info.setProperty("size", String.valueOf(src.length()));
		info.setProperty("fileName", src.getName());

		Handle h = streamStore.getHandle(marshalled, info.getQualifier());
		File tgt = h.getFile();
		if (tgt != null) {
			FileUtils.copyFile(src, tgt);
		} else {
			InputStream in = new FileInputStream(src);
			OutputStream out = h.openOutput();
			try {
				IOUtils.copy(in, out);
			} finally {
				IOUtils.closeQuietly(in);
				IOUtils.closeQuietly(out);
			}
		}
		ret.add(info);
		return ret;
	}

	@Override
	protected StoredObjectType calculateType(FilePointer fp) throws Exception {
		File f = fp.getFile();
		if (f.isFile()) { return StoredObjectType.DOCUMENT; }
		if (f.isDirectory()) { return StoredObjectType.FOLDER; }
		throw new ExportException(String.format("Filesystem object [%s] is of an unknown type", this.object));
	}

	@Override
	protected String calculateLabel(FilePointer object) throws Exception {
		return object.getPath();
	}

	@Override
	protected String calculateObjectId(FilePointer object) throws Exception {
		return String.format("%08X", object.getFile().hashCode());
	}

	@Override
	protected String calculateSearchKey(FilePointer object) throws Exception {
		return object.getPath();
	}
}