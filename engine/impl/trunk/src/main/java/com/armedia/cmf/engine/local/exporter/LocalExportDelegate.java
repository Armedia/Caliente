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

import com.armedia.cmf.engine.ContentInfo;
import com.armedia.cmf.engine.converter.IntermediateAttribute;
import com.armedia.cmf.engine.converter.IntermediateProperty;
import com.armedia.cmf.engine.exporter.ExportDelegate;
import com.armedia.cmf.engine.exporter.ExportException;
import com.armedia.cmf.engine.exporter.ExportTarget;
import com.armedia.cmf.engine.local.common.LocalFile;
import com.armedia.cmf.engine.local.common.LocalRoot;
import com.armedia.cmf.engine.local.common.LocalSessionWrapper;
import com.armedia.cmf.engine.tools.MimeTools;
import com.armedia.cmf.storage.ContentStore;
import com.armedia.cmf.storage.ObjectStorageTranslator;
import com.armedia.cmf.storage.StoredAttribute;
import com.armedia.cmf.storage.StoredDataType;
import com.armedia.cmf.storage.StoredObject;
import com.armedia.cmf.storage.StoredObjectType;
import com.armedia.cmf.storage.StoredProperty;
import com.armedia.cmf.storage.StoredValue;

public class LocalExportDelegate
extends
ExportDelegate<LocalFile, LocalRoot, LocalSessionWrapper, StoredValue, LocalExportContext, LocalExportDelegateFactory, LocalExportEngine> {

	protected LocalExportDelegate(LocalExportDelegateFactory factory, LocalFile object) throws Exception {
		super(factory, LocalFile.class, object);
	}

	@Override
	protected Collection<LocalExportDelegate> identifyRequirements(StoredObject<StoredValue> marshalled,
		LocalExportContext ctx) throws Exception {
		Collection<LocalExportDelegate> ret = new ArrayList<LocalExportDelegate>();

		LocalFile abs = this.object;
		File f = abs.getAbsolute();
		String p = f.getParent();
		if (p != null) {
			File parent = new File(f.getParent());
			if (!parent.equals(this.factory.getRoot().getFile())) {
				ret.add(new LocalExportDelegate(this.factory, new LocalFile(this.factory.getRoot(), p)));
			}
		}
		return ret;
	}

	@Override
	protected void marshal(LocalExportContext ctx, StoredObject<StoredValue> object) throws ExportException {
		final File file = this.object.getAbsolute();
		StoredAttribute<StoredValue> att = null;
		att = new StoredAttribute<StoredValue>(PropertyIds.NAME, StoredDataType.STRING, false);
		att.setValue(new StoredValue(file.getName()));
		object.setAttribute(att);

		att = new StoredAttribute<StoredValue>(PropertyIds.OBJECT_ID, StoredDataType.ID, false);
		att.setValue(new StoredValue(getObjectId()));
		object.setAttribute(att);

		att = new StoredAttribute<StoredValue>(PropertyIds.LAST_MODIFICATION_DATE, StoredDataType.DATETIME, false);
		att.setValue(new StoredValue(new Date(file.lastModified())));
		object.setAttribute(att);

		StoredProperty<StoredValue> parents = new StoredProperty<StoredValue>(IntermediateProperty.PARENT_ID.encode(),
			StoredDataType.ID, true);
		StoredProperty<StoredValue> paths = new StoredProperty<StoredValue>(IntermediateProperty.PATH.encode(),
			StoredDataType.STRING, true);
		if (this.object.getPathCount() > 1) {
			paths.setValue(new StoredValue(this.object.getPortablePath()));
			String parentPath = this.object.getPath();
			try {
				parents.setValue(new StoredValue(new LocalFile(this.object.getRootPath(), parentPath).getPathHash()));
			} catch (IOException e) {
				throw new ExportException(String.format("Failed to calculate the parent's ID for [%s] (parent = [%s])",
					this.object.getRelative(), parentPath));
			}
		}
		object.setProperty(paths);
		object.setProperty(parents);

		att = new StoredAttribute<StoredValue>(PropertyIds.PATH, StoredDataType.STRING, true);
		att.setValue(new StoredValue(this.object.getPortablePath()));
		object.setAttribute(att);

		if (getType() == StoredObjectType.DOCUMENT) {
			att = new StoredAttribute<StoredValue>(PropertyIds.CONTENT_STREAM_LENGTH, StoredDataType.DOUBLE, false);
			att.setValue(new StoredValue(file.length()));
			object.setAttribute(att);
		}
	}

	@Override
	protected Collection<LocalExportDelegate> identifyDependents(StoredObject<StoredValue> marshalled,
		LocalExportContext ctx) throws Exception {
		return null;
	}

	@Override
	protected List<ContentInfo> storeContent(LocalRoot session, ObjectStorageTranslator<StoredValue> translator,
		StoredObject<StoredValue> marshalled, ExportTarget referrent, ContentStore<?> streamStore) throws Exception {
		if (getType() != StoredObjectType.DOCUMENT) { return null; }

		List<ContentInfo> ret = new ArrayList<ContentInfo>(1);
		ContentInfo info = new ContentInfo("");
		File src = this.object.getAbsolute();
		MimeType type = null;
		try {
			type = MimeTools.determineMimeType(src);
		} catch (IOException e) {
			type = MimeTools.DEFAULT_MIME_TYPE;
		}

		StoredAttribute<StoredValue> typeAtt = new StoredAttribute<StoredValue>(
			IntermediateAttribute.CONTENT_STREAM_MIME_TYPE.encode(), StoredDataType.STRING, false);
		typeAtt.setValue(new StoredValue(type.getBaseType()));
		marshalled.setAttribute(typeAtt);

		// TODO: add the attributes...
		info.setProperty("mimeType", type.getBaseType());
		info.setProperty("size", String.valueOf(src.length()));
		info.setProperty("fileName", src.getName());
		ret.add(info);

		if (this.factory.isCopyContent()) {
			ContentStore<?>.Handle h = streamStore.getHandle(translator, marshalled, info.getQualifier());
			File tgt = h.getFile();
			if (tgt != null) {
				if (this.log.isDebugEnabled()) {
					this.log.debug(String.format("Copying %d bytes from [%s] into [%s]", src.length(), src, tgt));
				}
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
		}
		return ret;
	}

	@Override
	protected StoredObjectType calculateType(LocalFile f) throws Exception {
		File F = f.getAbsolute();
		if (F.isFile()) { return StoredObjectType.DOCUMENT; }
		if (F.isDirectory()) { return StoredObjectType.FOLDER; }
		throw new ExportException(String.format("Filesystem object [%s] is of an unknown type or doesn't exist", F));
	}

	@Override
	protected String calculateLabel(LocalFile object) throws Exception {
		String path = object.getPortablePath();
		if (path != null) { return String.format("%s/%s", path, object.getName()); }
		return object.getName();
	}

	@Override
	protected String calculateObjectId(LocalFile object) throws Exception {
		return object.getPathHash();
	}

	@Override
	protected String calculateBatchId(LocalFile object) throws Exception {
		if (object.getAbsolute().isDirectory()) { return String.format("%08X", object.getPathCount()); }
		return super.calculateBatchId(object);
	}

	@Override
	protected String calculateSearchKey(LocalFile object) throws Exception {
		return object.getSafePath();
	}
}