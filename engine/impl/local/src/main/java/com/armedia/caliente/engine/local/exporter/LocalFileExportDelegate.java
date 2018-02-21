package com.armedia.caliente.engine.local.exporter;

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

import com.armedia.caliente.engine.converter.IntermediateAttribute;
import com.armedia.caliente.engine.converter.IntermediateProperty;
import com.armedia.caliente.engine.exporter.ExportException;
import com.armedia.caliente.engine.exporter.ExportTarget;
import com.armedia.caliente.engine.local.common.LocalFile;
import com.armedia.caliente.engine.local.common.LocalRoot;
import com.armedia.caliente.store.CmfAttribute;
import com.armedia.caliente.store.CmfAttributeTranslator;
import com.armedia.caliente.store.CmfContentStream;
import com.armedia.caliente.store.CmfContentStore;
import com.armedia.caliente.store.CmfDataType;
import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.store.CmfProperty;
import com.armedia.caliente.store.CmfType;
import com.armedia.caliente.store.CmfValue;
import com.armedia.caliente.store.tools.MimeTools;

public class LocalFileExportDelegate extends LocalExportDelegate<LocalFile> {

	protected LocalFileExportDelegate(LocalExportDelegateFactory factory, LocalRoot root, LocalFile object)
		throws Exception {
		super(factory, root, LocalFile.class, object);
	}

	protected UserPrincipal getOwner(Path path) {
		FileOwnerAttributeView owner = Files.getFileAttributeView(path, FileOwnerAttributeView.class);
		if (owner != null) {
			try {
				return owner.getOwner();
			} catch (IOException e) {
				this.log.warn("Unexpected exception reading ownership information from [{}]", path, e);
			}
		}
		return null;
	}

	@Override
	protected Collection<LocalExportDelegate<?>> identifyRequirements(CmfObject<CmfValue> marshalled,
		LocalExportContext ctx) throws Exception {
		Collection<LocalExportDelegate<?>> ret = new ArrayList<>();

		LocalFile abs = this.object;
		File f = abs.getAbsolute();
		String p = f.getParent();
		if (p != null) {
			File parent = new File(p);
			if (!parent.equals(this.factory.getRoot().getFile())) {
				ret.add(new LocalFileExportDelegate(this.factory, ctx.getSession(),
					new LocalFile(this.factory.getRoot(), p)));
			}
		}

		Path path = this.object.getAbsolute().toPath();
		PosixFileAttributeView posix = Files.getFileAttributeView(path, PosixFileAttributeView.class);

		UserPrincipal owner = getOwner(path);
		if (owner != null) {
			ret.add(new LocalPrincipalExportDelegate(this.factory, ctx.getSession(), owner));
		}

		if (posix != null) {
			// For now, we won't support groups for two reasons:
			// 1) in O/S land, users and groups can have identical names, whereas in CMS-land this
			// can be a problem
			// 2) we can't enumerate the group's members, and therefore there's little sense to
			// migrating them in since we're not yet able to migrate the ACLs over, which is where
			// the group names would be useful
			/*
			ret.add(new LocalPrincipalExportDelegate(this.factory, posix.readAttributes().group()));
			*/
		}

		return ret;
	}

	@Override
	protected int calculateDependencyTier(LocalRoot root, LocalFile file) throws Exception {
		if (file.isFolder()) { return file.getPathCount() - 1; }
		// TODO: Symbolic links should be handled properly here
		return 0;
	}

	@Override
	protected boolean marshal(LocalExportContext ctx, CmfObject<CmfValue> object) throws ExportException {
		final File file = this.object.getAbsolute();
		CmfAttribute<CmfValue> att = null;
		att = new CmfAttribute<>(IntermediateAttribute.NAME, CmfDataType.STRING, false);
		att.setValue(new CmfValue(file.getName()));
		object.setAttribute(att);

		att = new CmfAttribute<>(IntermediateAttribute.OBJECT_ID, CmfDataType.ID, false);
		att.setValue(new CmfValue(getObjectId()));
		object.setAttribute(att);

		Path path = file.toPath();
		BasicFileAttributeView basic = Files.getFileAttributeView(path, BasicFileAttributeView.class);
		AclFileAttributeView acl = Files.getFileAttributeView(path, AclFileAttributeView.class);
		PosixFileAttributeView posix = Files.getFileAttributeView(path, PosixFileAttributeView.class);

		// Ok... we have the attribute views, export the information
		try {
			BasicFileAttributes basicAtts = basic.readAttributes();

			att = new CmfAttribute<>(IntermediateAttribute.CREATION_DATE, CmfDataType.DATETIME, false);
			att.setValue(new CmfValue(new Date(basicAtts.creationTime().toMillis())));
			object.setAttribute(att);

			att = new CmfAttribute<>(IntermediateAttribute.LAST_MODIFICATION_DATE, CmfDataType.DATETIME, false);
			att.setValue(new CmfValue(new Date(basicAtts.lastModifiedTime().toMillis())));
			object.setAttribute(att);

			att = new CmfAttribute<>(IntermediateAttribute.LAST_ACCESS_DATE, CmfDataType.DATETIME, false);
			att.setValue(new CmfValue(new Date(basicAtts.lastAccessTime().toMillis())));
			object.setAttribute(att);

			if (getType() == CmfType.DOCUMENT) {
				att = new CmfAttribute<>(IntermediateAttribute.CONTENT_STREAM_LENGTH, CmfDataType.DOUBLE, false);
				att.setValue(new CmfValue(basicAtts.size()));
				object.setAttribute(att);

				// All documents are roots...
				CmfProperty<CmfValue> versionTreeRoot = new CmfProperty<>(IntermediateProperty.VERSION_TREE_ROOT,
					CmfDataType.BOOLEAN, false);
				versionTreeRoot.setValue(new CmfValue(true));
				object.setProperty(versionTreeRoot);
			}

			UserPrincipal owner = getOwner(path);
			if (owner != null) {
				att = new CmfAttribute<>(IntermediateAttribute.CREATED_BY, CmfDataType.STRING, false);
				att.setValue(new CmfValue(owner.getName()));
				object.setAttribute(att);

				att = new CmfAttribute<>(IntermediateAttribute.OWNER, CmfDataType.STRING, false);
				att.setValue(new CmfValue(owner.getName()));
				object.setAttribute(att);
			}

			if (posix != null) {
				PosixFileAttributes posixAtts = posix.readAttributes();
				GroupPrincipal ownerGroup = posixAtts.group();
				att = new CmfAttribute<>(IntermediateAttribute.GROUP, CmfDataType.STRING, false);
				att.setValue(new CmfValue(ownerGroup.getName()));
				object.setAttribute(att);
			}

			if (acl != null) {
				// TODO: Before we can do this, we have to come up with a neutral, portable
				// mechanism to describe an ACL such that it works for ALL CMS engines...and this is
				// quite the conundrum to say the least...
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

		// The parent is always the parent folder
		CmfProperty<CmfValue> prop = null;

		String parentId = this.object.getParentId();
		prop = new CmfProperty<>(IntermediateProperty.PARENT_ID, CmfDataType.ID, true);
		att = new CmfAttribute<>(IntermediateAttribute.PARENT_ID, CmfDataType.ID, true);
		if (parentId != null) {
			att.setValue(new CmfValue(parentId));
			prop.setValue(att.getValue());
		}
		object.setAttribute(att);
		object.setProperty(prop);

		prop = new CmfProperty<>(IntermediateProperty.PATH, CmfDataType.STRING, true);
		prop.setValue(new CmfValue(this.object.getPortableParentPath()));
		object.setProperty(prop);

		if (this.object.isFolder()) {
			// If this is a folder, the path is set to its full, relative path
			att = new CmfAttribute<>(IntermediateAttribute.PATH, CmfDataType.STRING, true);
			att.setValue(new CmfValue(this.object.getPortableFullPath()));
			object.setAttribute(att);
		}

		return true;
	}

	@Override
	protected Collection<LocalFileExportDelegate> identifyDependents(CmfObject<CmfValue> marshalled,
		LocalExportContext ctx) throws Exception {
		return null;
	}

	@Override
	protected List<CmfContentStream> storeContent(LocalExportContext ctx, CmfAttributeTranslator<CmfValue> translator,
		CmfObject<CmfValue> marshalled, ExportTarget referrent, CmfContentStore<?, ?, ?> streamStore,
		boolean includeRenditions) throws Exception {
		if (getType() != CmfType.DOCUMENT) { return null; }

		List<CmfContentStream> ret = new ArrayList<>(1);
		CmfContentStream info = new CmfContentStream("");
		File src = this.object.getAbsolute();
		MimeType type = null;
		try {
			type = MimeTools.determineMimeType(src);
		} catch (IOException e) {
			type = MimeTools.DEFAULT_MIME_TYPE;
		}

		CmfAttribute<CmfValue> typeAtt = new CmfAttribute<>(IntermediateAttribute.CONTENT_STREAM_MIME_TYPE,
			CmfDataType.STRING, false);
		typeAtt.setValue(new CmfValue(type.getBaseType()));
		marshalled.setAttribute(typeAtt);

		// TODO: add the attributes...
		info.setMimeType(MimeTools.resolveMimeType(type.getBaseType()));
		info.setLength(src.length());
		info.setFileName(src.getName());
		ret.add(info);

		if (this.factory.isCopyContent()) {
			CmfContentStore<?, ?, ?>.Handle h = streamStore.getHandle(translator, marshalled, info);
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
	protected CmfType calculateType(LocalRoot root, LocalFile f) throws Exception {
		File F = f.getAbsolute();
		if (F.isFile()) { return CmfType.DOCUMENT; }
		if (F.isDirectory()) { return CmfType.FOLDER; }
		throw new ExportException(String.format("Filesystem object [%s] is of an unknown type or doesn't exist", F));
	}

	@Override
	protected String calculateLabel(LocalRoot root, LocalFile object) throws Exception {
		return object.getPortableFullPath();
	}

	@Override
	protected String calculateObjectId(LocalRoot root, LocalFile object) throws Exception {
		return object.getId();
	}

	@Override
	protected String calculateSearchKey(LocalRoot root, LocalFile object) throws Exception {
		return object.getSafePath();
	}

	@Override
	protected String calculateName(LocalRoot root, LocalFile object) throws Exception {
		return object.getName();
	}

	@Override
	protected boolean calculateHistoryCurrent(LocalRoot root, LocalFile object) throws Exception {
		// Always true
		return true;
	}
}