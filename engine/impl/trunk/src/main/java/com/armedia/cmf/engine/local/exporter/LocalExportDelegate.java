package com.armedia.cmf.engine.local.exporter;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.AclFileAttributeView;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileOwnerAttributeView;
import java.nio.file.attribute.GroupPrincipal;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFileAttributes;
import java.nio.file.attribute.UserPrincipal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import javax.activation.MimeType;

import org.apache.commons.io.IOUtils;

import com.armedia.cmf.engine.converter.IntermediateAttribute;
import com.armedia.cmf.engine.converter.IntermediateProperty;
import com.armedia.cmf.engine.exporter.ExportDelegate;
import com.armedia.cmf.engine.exporter.ExportException;
import com.armedia.cmf.engine.exporter.ExportTarget;
import com.armedia.cmf.engine.local.common.LocalFile;
import com.armedia.cmf.engine.local.common.LocalRoot;
import com.armedia.cmf.engine.local.common.LocalSessionWrapper;
import com.armedia.cmf.storage.CmfAttribute;
import com.armedia.cmf.storage.CmfAttributeTranslator;
import com.armedia.cmf.storage.CmfContentInfo;
import com.armedia.cmf.storage.CmfContentStore;
import com.armedia.cmf.storage.CmfDataType;
import com.armedia.cmf.storage.CmfObject;
import com.armedia.cmf.storage.CmfProperty;
import com.armedia.cmf.storage.CmfType;
import com.armedia.cmf.storage.CmfValue;
import com.armedia.cmf.storage.tools.MimeTools;

public class LocalExportDelegate extends
	ExportDelegate<LocalFile, LocalRoot, LocalSessionWrapper, CmfValue, LocalExportContext, LocalExportDelegateFactory, LocalExportEngine> {

	protected LocalExportDelegate(LocalExportDelegateFactory factory, LocalFile object) throws Exception {
		super(factory, LocalFile.class, object);
	}

	@Override
	protected Collection<LocalExportDelegate> identifyRequirements(CmfObject<CmfValue> marshalled,
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
	protected boolean marshal(LocalExportContext ctx, CmfObject<CmfValue> object) throws ExportException {
		final File file = this.object.getAbsolute();
		CmfAttribute<CmfValue> att = null;
		att = new CmfAttribute<CmfValue>(IntermediateAttribute.NAME, CmfDataType.STRING, false);
		att.setValue(new CmfValue(file.getName()));
		object.setAttribute(att);

		att = new CmfAttribute<CmfValue>(IntermediateAttribute.OBJECT_ID, CmfDataType.ID, false);
		att.setValue(new CmfValue(getObjectId()));
		object.setAttribute(att);

		Path path = file.toPath();
		BasicFileAttributeView basic = Files.getFileAttributeView(path, BasicFileAttributeView.class);
		AclFileAttributeView acl = Files.getFileAttributeView(path, AclFileAttributeView.class);
		PosixFileAttributeView posix = Files.getFileAttributeView(path, PosixFileAttributeView.class);
		FileOwnerAttributeView owner = posix;
		if (owner == null) {
			owner = Files.getFileAttributeView(path, FileOwnerAttributeView.class);
		}

		// Ok... we have the attribute views, export the information
		try {
			BasicFileAttributes basicAtts = basic.readAttributes();

			att = new CmfAttribute<CmfValue>(IntermediateAttribute.CREATION_DATE, CmfDataType.DATETIME, false);
			att.setValue(new CmfValue(new Date(basicAtts.creationTime().toMillis())));
			object.setAttribute(att);

			att = new CmfAttribute<CmfValue>(IntermediateAttribute.LAST_MODIFICATION_DATE, CmfDataType.DATETIME, false);
			att.setValue(new CmfValue(new Date(basicAtts.lastModifiedTime().toMillis())));
			object.setAttribute(att);

			att = new CmfAttribute<CmfValue>(IntermediateAttribute.LAST_ACCESS_DATE, CmfDataType.DATETIME, false);
			att.setValue(new CmfValue(new Date(basicAtts.lastAccessTime().toMillis())));
			object.setAttribute(att);

			if (getType() == CmfType.DOCUMENT) {
				att = new CmfAttribute<CmfValue>(IntermediateAttribute.CONTENT_STREAM_LENGTH, CmfDataType.DOUBLE,
					false);
				att.setValue(new CmfValue(basicAtts.size()));
				object.setAttribute(att);
			}

			if (owner != null) {
				UserPrincipal ownerUser = owner.getOwner();
				att = new CmfAttribute<CmfValue>(IntermediateAttribute.CREATED_BY, CmfDataType.STRING, false);
				att.setValue(new CmfValue(ownerUser.getName()));
				object.setAttribute(att);

				att = new CmfAttribute<CmfValue>(IntermediateAttribute.OWNER, CmfDataType.STRING, false);
				att.setValue(new CmfValue(ownerUser.getName()));
				object.setAttribute(att);
			}

			if (posix != null) {
				PosixFileAttributes posixAtts = posix.readAttributes();
				GroupPrincipal ownerGroup = posixAtts.group();
				att = new CmfAttribute<CmfValue>(IntermediateAttribute.GROUP, CmfDataType.STRING, false);
				att.setValue(new CmfValue(ownerGroup.getName()));
				object.setAttribute(att);
			}

			if (acl != null) {
				/*
				for (AclEntry e : acl.getAcl()) {
					UserPrincipal principal = e.principal();
					for (AclEntryFlag f : e.flags()) {
					}
					for (AclEntryPermission p : e.permissions()) {
					}
				}
				*/
			}
		} catch (IOException e) {
			throw new ExportException(String.format("Failed to collect the attribute information for [%s]", file), e);
		}

		CmfProperty<CmfValue> parents = new CmfProperty<CmfValue>(IntermediateProperty.PARENT_ID, CmfDataType.ID, true);
		CmfProperty<CmfValue> paths = new CmfProperty<CmfValue>(IntermediateProperty.PATH, CmfDataType.STRING, true);
		if (this.object.getPathCount() > 1) {
			paths.setValue(new CmfValue(this.object.getPortablePath()));
			String parentPath = this.object.getPath();
			try {
				parents.setValue(new CmfValue(new LocalFile(this.object.getRootPath(), parentPath).getPathHash()));
			} catch (IOException e) {
				throw new ExportException(String.format("Failed to calculate the parent's ID for [%s] (parent = [%s])",
					this.object.getRelative(), parentPath));
			}
		}
		object.setProperty(paths);
		object.setProperty(parents);

		att = new CmfAttribute<CmfValue>(IntermediateAttribute.PATH, CmfDataType.STRING, true);
		att.setValue(new CmfValue(this.object.getPortablePath()));
		object.setAttribute(att);
		return true;
	}

	@Override
	protected Collection<LocalExportDelegate> identifyDependents(CmfObject<CmfValue> marshalled, LocalExportContext ctx)
		throws Exception {
		return null;
	}

	@Override
	protected List<CmfContentInfo> storeContent(LocalExportContext ctx, CmfAttributeTranslator<CmfValue> translator,
		CmfObject<CmfValue> marshalled, ExportTarget referrent, CmfContentStore<?, ?, ?> streamStore) throws Exception {
		if (getType() != CmfType.DOCUMENT) { return null; }

		List<CmfContentInfo> ret = new ArrayList<CmfContentInfo>(1);
		CmfContentInfo info = new CmfContentInfo("");
		File src = this.object.getAbsolute();
		MimeType type = null;
		try {
			type = MimeTools.determineMimeType(src);
		} catch (IOException e) {
			type = MimeTools.DEFAULT_MIME_TYPE;
		}

		CmfAttribute<CmfValue> typeAtt = new CmfAttribute<CmfValue>(IntermediateAttribute.CONTENT_STREAM_MIME_TYPE,
			CmfDataType.STRING, false);
		typeAtt.setValue(new CmfValue(type.getBaseType()));
		marshalled.setAttribute(typeAtt);

		// TODO: add the attributes...
		info.setMimeType(MimeTools.resolveMimeType(type.getBaseType()));
		info.setLength(src.length());
		info.setFileName(src.getName());
		ret.add(info);

		if (this.factory.isCopyContent()) {
			CmfContentStore<?, ?, ?>.Handle h = streamStore.getHandle(translator, marshalled, info.getQualifier());
			File tgt = h.getFile(true);
			if (tgt != null) {
				if (this.log.isDebugEnabled()) {
					this.log.debug(String.format("Copying %d bytes from [%s] into [%s]", src.length(), src, tgt));
				}
				Files.copy(src.toPath(), tgt.toPath(), StandardCopyOption.REPLACE_EXISTING);
			} else {
				InputStream in = new FileInputStream(src);
				try {
					h.setContents(in);
				} finally {
					IOUtils.closeQuietly(in);
				}
			}
		}
		return ret;
	}

	@Override
	protected CmfType calculateType(LocalFile f) throws Exception {
		File F = f.getAbsolute();
		if (F.isFile()) { return CmfType.DOCUMENT; }
		if (F.isDirectory()) { return CmfType.FOLDER; }
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