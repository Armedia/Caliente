package com.armedia.caliente.engine.local.exporter;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.AclEntry;
import java.nio.file.attribute.AclEntryFlag;
import java.nio.file.attribute.AclEntryPermission;
import java.nio.file.attribute.AclEntryType;
import java.nio.file.attribute.AclFileAttributeView;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.DosFileAttributeView;
import java.nio.file.attribute.DosFileAttributes;
import java.nio.file.attribute.FileAttributeView;
import java.nio.file.attribute.FileOwnerAttributeView;
import java.nio.file.attribute.GroupPrincipal;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFileAttributes;
import java.nio.file.attribute.UserDefinedFileAttributeView;
import java.nio.file.attribute.UserPrincipal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.activation.MimeType;

import com.armedia.caliente.engine.converter.IntermediateAttribute;
import com.armedia.caliente.engine.converter.IntermediateProperty;
import com.armedia.caliente.engine.exporter.ExportException;
import com.armedia.caliente.engine.exporter.ExportTarget;
import com.armedia.caliente.engine.local.common.LocalFile;
import com.armedia.caliente.engine.local.common.LocalRoot;
import com.armedia.caliente.store.CmfAttribute;
import com.armedia.caliente.store.CmfAttributeTranslator;
import com.armedia.caliente.store.CmfContentStore;
import com.armedia.caliente.store.CmfContentStream;
import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.store.CmfObjectRef;
import com.armedia.caliente.store.CmfProperty;
import com.armedia.caliente.store.CmfValue;
import com.armedia.caliente.store.tools.MimeTools;

public class LocalFileExportDelegate extends LocalExportDelegate<LocalFile> {

	private static final String EXT_ATT_PREFIX = "cmfext";
	private static final String EXT_ATT_FORMAT = String.format("%s:%%s", LocalFileExportDelegate.EXT_ATT_PREFIX);
	private static final String DOS_ATT_PREFIX = "cmfdos";
	private static final String DOS_ATT_FORMAT = String.format("%s:%%s", LocalFileExportDelegate.DOS_ATT_PREFIX);

	private final Map<Class<? extends FileAttributeView>, FileAttributeView> attributeViews = new HashMap<>();

	protected LocalFileExportDelegate(LocalExportDelegateFactory factory, LocalRoot root, LocalFile object)
		throws Exception {
		super(factory, root, LocalFile.class, object);
	}

	protected <T extends FileAttributeView> T getFileAttributeView(Path path, Class<T> klazz) {
		if (this.attributeViews.containsKey(klazz)) { return klazz.cast(this.attributeViews.get(klazz)); }
		T view = null;
		if (this.object.isSymbolicLink()) {
			view = Files.getFileAttributeView(path, klazz, LinkOption.NOFOLLOW_LINKS);
		} else {
			view = Files.getFileAttributeView(path, klazz);
		}

		if (view != null) {
			this.attributeViews.put(klazz, view);
		}
		return view;
	}

	@Override
	protected Collection<CmfObjectRef> calculateParentIds(LocalRoot session, LocalFile object) throws Exception {
		String parentId = object.getParentId();
		if (parentId == null) { return Collections.emptyList(); }
		return Collections.singleton(new CmfObjectRef(CmfObject.Archetype.FOLDER, parentId));
	}

	protected UserPrincipal getOwner(Path path) {
		FileOwnerAttributeView owner = getFileAttributeView(path, FileOwnerAttributeView.class);
		if (owner != null) {
			try {
				return owner.getOwner();
			} catch (IOException e) {
				this.log.warn("Unexpected exception reading ownership information from [{}]", path, e);
			}
		}
		return null;
	}

	protected GroupPrincipal getGroup(Path path) {
		PosixFileAttributeView posix = getFileAttributeView(path, PosixFileAttributeView.class);
		if (posix != null) {
			try {
				return posix.readAttributes().group();
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
		PosixFileAttributeView posix = getFileAttributeView(path, PosixFileAttributeView.class);

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
		int ret = 0;
		if (file.isFolder()) {
			ret = file.getPathCount() - 1;
		}
		if (file.isSymbolicLink()) {
			ret++;
		}
		return ret;
	}

	@Override
	protected boolean marshal(LocalExportContext ctx, CmfObject<CmfValue> object) throws ExportException {
		final File file = this.object.getAbsolute();
		CmfAttribute<CmfValue> att = null;
		att = new CmfAttribute<>(IntermediateAttribute.NAME, CmfValue.Type.STRING, false);
		att.setValue(new CmfValue(file.getName()));
		object.setAttribute(att);

		att = new CmfAttribute<>(IntermediateAttribute.OBJECT_ID, CmfValue.Type.ID, false);
		att.setValue(new CmfValue(getObjectId()));
		object.setAttribute(att);

		Path path = file.toPath();
		final BasicFileAttributeView basic = getFileAttributeView(path, BasicFileAttributeView.class);
		final DosFileAttributeView dos = getFileAttributeView(path, DosFileAttributeView.class);
		final AclFileAttributeView acl = getFileAttributeView(path, AclFileAttributeView.class);
		final PosixFileAttributeView posix = getFileAttributeView(path, PosixFileAttributeView.class);
		final UserDefinedFileAttributeView extendedAtts = getFileAttributeView(path,
			UserDefinedFileAttributeView.class);

		// Ok... we have the attribute views, export the information
		try {
			BasicFileAttributes basicAtts = basic.readAttributes();

			att = new CmfAttribute<>(IntermediateAttribute.CREATION_DATE, CmfValue.Type.DATETIME, false);
			att.setValue(new CmfValue(new Date(basicAtts.creationTime().toMillis())));
			object.setAttribute(att);

			att = new CmfAttribute<>(IntermediateAttribute.LAST_MODIFICATION_DATE, CmfValue.Type.DATETIME, false);
			att.setValue(new CmfValue(new Date(basicAtts.lastModifiedTime().toMillis())));
			object.setAttribute(att);

			att = new CmfAttribute<>(IntermediateAttribute.LAST_ACCESS_DATE, CmfValue.Type.DATETIME, false);
			att.setValue(new CmfValue(new Date(basicAtts.lastAccessTime().toMillis())));
			object.setAttribute(att);

			if (extendedAtts != null) {
				for (String name : extendedAtts.list()) {
					int bytes = extendedAtts.size(name);
					if (bytes == 0) {
						continue;
					}

					ByteBuffer buf = ByteBuffer.allocate(bytes);
					extendedAtts.read(name, buf);
					buf.flip();
					att = new CmfAttribute<>(String.format(LocalFileExportDelegate.EXT_ATT_FORMAT, name),
						CmfValue.Type.BASE64_BINARY, false);
					byte[] data = null;
					if (buf.hasArray()) {
						data = buf.array();
					} else {
						data = new byte[bytes];
						buf.get(data);
					}
					att.setValue(new CmfValue(data));
					object.setAttribute(att);
				}
			}

			if (dos != null) {
				DosFileAttributes atts = dos.readAttributes();
				att = new CmfAttribute<>(String.format(LocalFileExportDelegate.DOS_ATT_FORMAT, "hidden"),
					CmfValue.Type.BOOLEAN, false);
				att.setValue(new CmfValue(atts.isHidden()));
				object.setAttribute(att);

				att = new CmfAttribute<>(String.format(LocalFileExportDelegate.DOS_ATT_FORMAT, "system"),
					CmfValue.Type.BOOLEAN, false);
				att.setValue(new CmfValue(atts.isSystem()));
				object.setAttribute(att);

				att = new CmfAttribute<>(String.format(LocalFileExportDelegate.DOS_ATT_FORMAT, "archive"),
					CmfValue.Type.BOOLEAN, false);
				att.setValue(new CmfValue(atts.isArchive()));
				object.setAttribute(att);

				att = new CmfAttribute<>(String.format(LocalFileExportDelegate.DOS_ATT_FORMAT, "readonly"),
					CmfValue.Type.BOOLEAN, false);
				att.setValue(new CmfValue(atts.isReadOnly()));
				object.setAttribute(att);
			}

			if (getType() == CmfObject.Archetype.DOCUMENT) {
				att = new CmfAttribute<>(IntermediateAttribute.CONTENT_STREAM_LENGTH, CmfValue.Type.DOUBLE, false);
				att.setValue(new CmfValue((double) basicAtts.size()));
				object.setAttribute(att);

				// All documents are roots...
				CmfProperty<CmfValue> versionTreeRoot = new CmfProperty<>(IntermediateProperty.VERSION_TREE_ROOT,
					CmfValue.Type.BOOLEAN, false);
				versionTreeRoot.setValue(new CmfValue(true));
				object.setProperty(versionTreeRoot);
			}

			UserPrincipal owner = getOwner(path);
			if (owner != null) {
				att = new CmfAttribute<>(IntermediateAttribute.CREATED_BY, CmfValue.Type.STRING, false);
				att.setValue(new CmfValue(owner.getName()));
				object.setAttribute(att);

				att = new CmfAttribute<>(IntermediateAttribute.OWNER, CmfValue.Type.STRING, false);
				att.setValue(new CmfValue(owner.getName()));
				object.setAttribute(att);
			}

			if (posix != null) {
				PosixFileAttributes posixAtts = posix.readAttributes();
				GroupPrincipal ownerGroup = posixAtts.group();
				att = new CmfAttribute<>(IntermediateAttribute.GROUP, CmfValue.Type.STRING, false);
				att.setValue(new CmfValue(ownerGroup.getName()));
				object.setAttribute(att);
			}

			if (acl != null) {
				// TODO: Before we can do this, we have to come up with a neutral, portable
				// mechanism to describe an ACL such that it works for ALL CMS engines...and this is
				// quite the conundrum to say the least...
				for (AclEntry e : acl.getAcl()) {
					AclEntryType type = e.type();
					UserPrincipal principal = e.principal();
					for (AclEntryFlag f : e.flags()) {
					}
					for (AclEntryPermission p : e.permissions()) {
					}
				}
			}
		} catch (IOException e) {
			throw new ExportException(String.format("Failed to collect the attribute information for [%s]", file), e);
		}

		// The parent is always the parent folder
		CmfProperty<CmfValue> prop = null;

		String parentId = this.object.getParentId();
		prop = new CmfProperty<>(IntermediateProperty.PARENT_ID, CmfValue.Type.ID, true);
		att = new CmfAttribute<>(IntermediateAttribute.PARENT_ID, CmfValue.Type.ID, true);
		if (parentId != null) {
			att.setValue(new CmfValue(parentId));
			prop.setValue(att.getValue());
		}
		object.setAttribute(att);
		object.setProperty(prop);

		prop = new CmfProperty<>(IntermediateProperty.PATH, CmfValue.Type.STRING, true);
		prop.setValue(new CmfValue(this.object.getPortableParentPath()));
		object.setProperty(prop);

		if (this.object.isFolder()) {
			// If this is a folder, the path is set to its full, relative path
			att = new CmfAttribute<>(IntermediateAttribute.PATH, CmfValue.Type.STRING, true);
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
		if (getType() != CmfObject.Archetype.DOCUMENT) { return null; }

		List<CmfContentStream> ret = new ArrayList<>(1);
		CmfContentStream info = new CmfContentStream(0, "");
		File src = this.object.getAbsolute();
		MimeType type = null;
		try {
			type = MimeTools.determineMimeType(src);
		} catch (IOException e) {
			type = MimeTools.DEFAULT_MIME_TYPE;
		}

		CmfAttribute<CmfValue> typeAtt = new CmfAttribute<>(IntermediateAttribute.CONTENT_STREAM_MIME_TYPE,
			CmfValue.Type.STRING, false);
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
					this.log.debug("Copying {} bytes from [{}] into [{}]", src.length(), src, tgt);
				}
				Files.copy(src.toPath(), tgt.toPath(), StandardCopyOption.REPLACE_EXISTING);
			} else {
				try (InputStream in = new FileInputStream(src)) {
					h.setContents(in);
				}
			}
		}
		return ret;
	}

	@Override
	protected CmfObject.Archetype calculateType(LocalRoot root, LocalFile f) throws Exception {
		File F = f.getAbsolute();
		if (F.isFile()) { return CmfObject.Archetype.DOCUMENT; }
		if (F.isDirectory()) { return CmfObject.Archetype.FOLDER; }
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