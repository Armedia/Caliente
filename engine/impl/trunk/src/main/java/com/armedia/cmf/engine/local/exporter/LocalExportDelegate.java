package com.armedia.cmf.engine.local.exporter;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import javax.activation.MimeType;

import org.apache.chemistry.opencmis.commons.PropertyIds;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import com.armedia.cmf.engine.ContentInfo;
import com.armedia.cmf.engine.exporter.ExportDelegate;
import com.armedia.cmf.engine.exporter.ExportException;
import com.armedia.cmf.engine.exporter.ExportTarget;
import com.armedia.cmf.engine.local.common.LocalSessionWrapper;
import com.armedia.cmf.engine.tools.MimeTools;
import com.armedia.cmf.storage.ContentStore;
import com.armedia.cmf.storage.ContentStore.Handle;
import com.armedia.cmf.storage.StoredAttribute;
import com.armedia.cmf.storage.StoredDataType;
import com.armedia.cmf.storage.StoredObject;
import com.armedia.cmf.storage.StoredObjectType;
import com.armedia.cmf.storage.StoredValue;

public class LocalExportDelegate
	extends
	ExportDelegate<File, File, LocalSessionWrapper, StoredValue, LocalExportContext, LocalExportDelegateFactory, LocalExportEngine> {

	protected LocalExportDelegate(LocalExportDelegateFactory factory, File object) throws Exception {
		super(factory, File.class, new File(LocalExportDelegate.calculateRelativePath(factory.getRoot(), object)));
	}

	@Override
	protected Collection<LocalExportDelegate> identifyRequirements(StoredObject<StoredValue> marshalled,
		LocalExportContext ctx) throws Exception {
		Collection<LocalExportDelegate> ret = new ArrayList<LocalExportDelegate>();
		File abs = new File(this.factory.getRoot(), this.object.getPath()).getAbsoluteFile();
		File parent = abs.getParentFile();
		if ((parent != null) && !parent.equals(this.factory.getRoot())) {
			ret.add(new LocalExportDelegate(this.factory, parent));
		}
		return ret;
	}

	@Override
	protected void marshal(LocalExportContext ctx, StoredObject<StoredValue> object) throws ExportException {
		StoredAttribute<StoredValue> att = null;
		att = new StoredAttribute<StoredValue>(PropertyIds.NAME, StoredDataType.STRING, false);
		att.setValue(new StoredValue(this.object.getName()));
		object.setAttribute(att);

		att = new StoredAttribute<StoredValue>(PropertyIds.OBJECT_ID, StoredDataType.ID, false);
		att.setValue(new StoredValue(getObjectId()));
		object.setAttribute(att);

		att = new StoredAttribute<StoredValue>(PropertyIds.LAST_MODIFICATION_DATE, StoredDataType.DATETIME, false);
		att.setValue(new StoredValue(new Date(this.object.lastModified())));
		object.setAttribute(att);

		if (getType() == StoredObjectType.DOCUMENT) {
			att = new StoredAttribute<StoredValue>(PropertyIds.CONTENT_STREAM_LENGTH, StoredDataType.DOUBLE, false);
			att.setValue(new StoredValue(this.object.length()));
			object.setAttribute(att);
		}
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
		ContentInfo info = new ContentInfo("");
		File src = new File(this.factory.getRoot(), this.object.getPath());
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
	protected StoredObjectType calculateType(File f) throws Exception {
		File F = new File(this.factory.getRoot(), f.getPath());
		if (F.isFile()) { return StoredObjectType.DOCUMENT; }
		if (F.isDirectory()) { return StoredObjectType.FOLDER; }
		throw new ExportException(String.format("Filesystem object [%s] is of an unknown type or doesn't exist", F));
	}

	@Override
	protected String calculateLabel(File object) throws Exception {
		return object.getPath();
	}

	@Override
	protected String calculateObjectId(File object) throws Exception {
		return LocalExportDelegate.calculateCanonicalPathHash(this.factory.getRoot(), object);
	}

	@Override
	protected String calculateSearchKey(File object) throws Exception {
		return LocalExportDelegate.calculateRelativePath(this.factory.getRoot(), object);
	}

	static String calculateCanonicalPathHash(File root, File object) throws IOException {
		File canonical = new File(root, object.getPath()).getCanonicalFile();
		return String.format("%08X", canonical.hashCode());
	}

	static String calculateRelativePath(File root, File object) throws IOException {
		root = root.getCanonicalFile();
		String path = null;
		if (object.isAbsolute()) {
			path = object.getPath();
		} else {
			path = new File(root, object.getPath()).getAbsolutePath();
		}
		String rootPath = String.format("%s%s", root.getAbsolutePath(), File.separator);
		if (!path.startsWith(rootPath)) { throw new IOException(String.format(
			"The given path can't be empty: root = [%s], path = [%s]", rootPath, path)); }
		path = path.substring(rootPath.length());
		if (StringUtils.isBlank(path)) { throw new IOException(String.format(
			"The resulting path can't be empty: root = [%s], path = [%s]", rootPath, path)); }
		return path;
	}
}