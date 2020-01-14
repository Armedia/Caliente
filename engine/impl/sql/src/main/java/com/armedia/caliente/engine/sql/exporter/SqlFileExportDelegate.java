/*******************************************************************************
 * #%L
 * Armedia Caliente
 * %%
 * Copyright (C) 2013 - 2019 Armedia, LLC
 * %%
 * This file is part of the Caliente software.
 *
 * If the software was purchased under a paid Caliente license, the terms of
 * the paid license agreement will prevail.  Otherwise, the software is
 * provided under the following open source license terms:
 *
 * Caliente is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Caliente is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Caliente. If not, see <http://www.gnu.org/licenses/>.
 * #L%
 *******************************************************************************/
package com.armedia.caliente.engine.sql.exporter;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
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
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.activation.MimeType;

import org.apache.commons.lang3.StringUtils;

import com.armedia.caliente.engine.TransferSetting;
import com.armedia.caliente.engine.converter.IntermediateAttribute;
import com.armedia.caliente.engine.converter.IntermediateProperty;
import com.armedia.caliente.engine.exporter.ExportException;
import com.armedia.caliente.engine.exporter.ExportTarget;
import com.armedia.caliente.engine.sql.common.SqlCommon;
import com.armedia.caliente.engine.sql.common.SqlFile;
import com.armedia.caliente.engine.sql.common.SqlRoot;
import com.armedia.caliente.store.CmfAttribute;
import com.armedia.caliente.store.CmfAttributeTranslator;
import com.armedia.caliente.store.CmfContentStore;
import com.armedia.caliente.store.CmfContentStream;
import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.store.CmfObjectRef;
import com.armedia.caliente.store.CmfProperty;
import com.armedia.caliente.store.CmfValue;
import com.armedia.caliente.store.tools.MimeTools;
import com.armedia.commons.utilities.FileNameTools;

public class SqlFileExportDelegate extends SqlExportDelegate<SqlFile> {

	private static final String EXT_ATT_PREFIX = "cmfext";
	private static final String EXT_ATT_FORMAT = String.format("%s:%%s", SqlFileExportDelegate.EXT_ATT_PREFIX);
	private static final String DOS_ATT_PREFIX = "cmfdos";
	private static final String DOS_ATT_FORMAT = String.format("%s:%%s", SqlFileExportDelegate.DOS_ATT_PREFIX);

	private final Map<Class<? extends FileAttributeView>, FileAttributeView> attributeViews = new HashMap<>();

	protected SqlFileExportDelegate(SqlExportDelegateFactory factory, SqlRoot root, SqlFile object) throws Exception {
		super(factory, root, SqlFile.class, object);
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
	protected Collection<CmfObjectRef> calculateParentIds(SqlRoot session, SqlFile object) throws Exception {
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
	protected Collection<SqlExportDelegate<?>> identifyRequirements(CmfObject<CmfValue> marshalled,
		SqlExportContext ctx) throws Exception {
		Collection<SqlExportDelegate<?>> ret = new ArrayList<>();

		SqlFile abs = this.object;
		File f = abs.getAbsolute();
		String p = f.getParent();
		if (p != null) {
			File parent = new File(p);
			if (!parent.equals(this.factory.getRoot().getFile())) {
				ret.add(
					new SqlFileExportDelegate(this.factory, ctx.getSession(), new SqlFile(this.factory.getRoot(), p)));
			}
		}

		Path path = this.object.getAbsolute().toPath();
		PosixFileAttributeView posix = getFileAttributeView(path, PosixFileAttributeView.class);

		UserPrincipal owner = getOwner(path);
		if (owner != null) {
			ret.add(new SqlPrincipalExportDelegate(this.factory, ctx.getSession(), owner));
		}

		if (posix != null) {
			// For now, we won't support groups for two reasons:
			// 1) in O/S land, users and groups can have identical names, whereas in CMS-land this
			// can be a problem
			// 2) we can't enumerate the group's members, and therefore there's little sense to
			// migrating them in since we're not yet able to migrate the ACLs over, which is where
			// the group names would be useful
			/*
			ret.add(new SqlPrincipalExportDelegate(this.factory, posix.readAttributes().group()));
			*/
		}

		return ret;
	}

	@Override
	protected int calculateDependencyTier(SqlRoot root, SqlFile file) throws Exception {
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
	protected boolean marshal(SqlExportContext ctx, CmfObject<CmfValue> object) throws ExportException {
		final File file = this.object.getAbsolute();
		CmfAttribute<CmfValue> att = null;
		att = new CmfAttribute<>(IntermediateAttribute.NAME, CmfValue.Type.STRING, false);
		att.setValue(new CmfValue(file.getName()));
		object.setAttribute(att);

		att = new CmfAttribute<>(IntermediateAttribute.OBJECT_ID, CmfValue.Type.ID, false);
		att.setValue(new CmfValue(getObjectId()));
		object.setAttribute(att);

		Path path = file.toPath();

		// Ok... we have the attribute views, export the information
		try {
			final BasicFileAttributeView basic = getFileAttributeView(path, BasicFileAttributeView.class);
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
		} catch (IOException e) {
			throw new ExportException(String.format("Failed to collect the basic attribute information for [%s]", file),
				e);
		}

		final UserDefinedFileAttributeView extendedAtts = getFileAttributeView(path,
			UserDefinedFileAttributeView.class);
		if (extendedAtts != null) {
			try {
				for (String name : extendedAtts.list()) {
					int bytes = extendedAtts.size(name);
					if (bytes == 0) {
						continue;
					}

					ByteBuffer buf = ByteBuffer.allocate(bytes);
					extendedAtts.read(name, buf);
					buf.flip();
					att = new CmfAttribute<>(String.format(SqlFileExportDelegate.EXT_ATT_FORMAT, name),
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
			} catch (Exception e) {
				// Do nothing
			}
		}

		final DosFileAttributeView dos = getFileAttributeView(path, DosFileAttributeView.class);
		if (dos != null) {
			try {
				DosFileAttributes atts = dos.readAttributes();
				att = new CmfAttribute<>(String.format(SqlFileExportDelegate.DOS_ATT_FORMAT, "hidden"),
					CmfValue.Type.BOOLEAN, false);
				att.setValue(new CmfValue(atts.isHidden()));
				object.setAttribute(att);

				att = new CmfAttribute<>(String.format(SqlFileExportDelegate.DOS_ATT_FORMAT, "system"),
					CmfValue.Type.BOOLEAN, false);
				att.setValue(new CmfValue(atts.isSystem()));
				object.setAttribute(att);

				att = new CmfAttribute<>(String.format(SqlFileExportDelegate.DOS_ATT_FORMAT, "archive"),
					CmfValue.Type.BOOLEAN, false);
				att.setValue(new CmfValue(atts.isArchive()));
				object.setAttribute(att);

				att = new CmfAttribute<>(String.format(SqlFileExportDelegate.DOS_ATT_FORMAT, "readonly"),
					CmfValue.Type.BOOLEAN, false);
				att.setValue(new CmfValue(atts.isReadOnly()));
				object.setAttribute(att);
			} catch (Exception e) {
				// do nothing...
			}
		}

		UserPrincipal owner = getOwner(path);
		if (owner != null) {
			att = new CmfAttribute<>(IntermediateAttribute.CREATED_BY, CmfValue.Type.STRING, false);
			att.setValue(new CmfValue(owner.getName()));
			object.setAttribute(att);

			att = new CmfAttribute<>(IntermediateAttribute.LAST_MODIFIED_BY, CmfValue.Type.STRING, false);
			att.setValue(new CmfValue(owner.getName()));
			object.setAttribute(att);

			att = new CmfAttribute<>(IntermediateAttribute.OWNER, CmfValue.Type.STRING, false);
			att.setValue(new CmfValue(owner.getName()));
			object.setAttribute(att);
		}

		final PosixFileAttributeView posix = getFileAttributeView(path, PosixFileAttributeView.class);
		if (posix != null) {
			try {
				PosixFileAttributes posixAtts = posix.readAttributes();
				GroupPrincipal ownerGroup = posixAtts.group();
				att = new CmfAttribute<>(IntermediateAttribute.GROUP, CmfValue.Type.STRING, false);
				att.setValue(new CmfValue(ownerGroup.getName()));
				object.setAttribute(att);
			} catch (Exception e) {
				// Do nothing...
			}
		}

		/*
		final AclFileAttributeView acl = getFileAttributeView(path, AclFileAttributeView.class);
		if (acl != null) {
			// TODO: Before we can do this, we have to come up with a neutral, portable
			// mechanism to describe an ACL such that it works for ALL CMS engines...and this is
			// quite the conundrum to say the least...
			try {
				for (AclEntry e : acl.getAcl()) {
					AclEntryType type = e.type();
					UserPrincipal principal = e.principal();
					for (AclEntryFlag f : e.flags()) {
					}
					for (AclEntryPermission p : e.permissions()) {
					}
				}
			} catch (Exception e) {
				// Do nothing...
			}
		}
		*/

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

		try {
			object.setProperty(new CmfProperty<>(IntermediateProperty.PARENT_TREE_IDS, CmfValue.Type.STRING,
				calculateParentTreeIds(file)));
		} catch (IOException e) {
			throw new ExportException(
				String.format("Failed to calculate the parent path IDs for [%s]", file.getAbsolutePath()), e);
		}

		object.setProperty(new CmfProperty<>(IntermediateProperty.PATH, CmfValue.Type.STRING,
			new CmfValue(this.object.getPortableParentPath())));

		object.setProperty(new CmfProperty<>(IntermediateProperty.FULL_PATH, CmfValue.Type.STRING,
			new CmfValue(this.object.getPortableFullPath())));

		if (this.object.isFolder()) {
			// If this is a folder, the path is set to its full, relative path
			att = new CmfAttribute<>(IntermediateAttribute.PATH, CmfValue.Type.STRING, true);
			att.setValue(new CmfValue(this.object.getPortableFullPath()));
			object.setAttribute(att);
		}

		return true;
	}

	protected CmfValue calculateParentTreeIds(File f) throws IOException {
		f = this.root.relativize(f);
		List<String> parents = new LinkedList<>();
		while (true) {
			f = f.getParentFile();
			if ((f == null) || StringUtils.isEmpty(f.getName())) {
				break;
			}
			String path = SqlCommon.getPortablePath(f.getPath());
			String id = SqlCommon.calculateId(path);
			parents.add(0, id);
		}
		return new CmfValue(FileNameTools.reconstitute(parents, false, false, '/'));
	}

	@Override
	protected Collection<SqlFileExportDelegate> identifyDependents(CmfObject<CmfValue> marshalled, SqlExportContext ctx)
		throws Exception {
		return null;
	}

	@Override
	protected List<CmfContentStream> storeContent(SqlExportContext ctx, CmfAttributeTranslator<CmfValue> translator,
		CmfObject<CmfValue> marshalled, ExportTarget referrent, CmfContentStore<?, ?> streamStore,
		boolean includeRenditions) {
		if (getType() != CmfObject.Archetype.DOCUMENT) { return null; }

		List<CmfContentStream> ret = new ArrayList<>(1);
		CmfContentStream info = new CmfContentStream(marshalled, 0);
		File src = this.object.getAbsolute();
		MimeType type = null;
		try {
			type = MimeTools.determineMimeType(src);
		} catch (IOException e) {
			type = MimeTools.DEFAULT_MIME_TYPE;
		}

		marshalled
			.setProperty(new CmfProperty<>(IntermediateProperty.VERSION_COUNT, CmfValue.Type.INTEGER, new CmfValue(1)));
		marshalled.setProperty(
			new CmfProperty<>(IntermediateProperty.VERSION_HEAD_INDEX, CmfValue.Type.INTEGER, new CmfValue(0)));
		marshalled
			.setProperty(new CmfProperty<>(IntermediateProperty.VERSION_INDEX, CmfValue.Type.INTEGER, new CmfValue(0)));

		CmfAttribute<CmfValue> typeAtt = new CmfAttribute<>(IntermediateAttribute.CONTENT_STREAM_MIME_TYPE,
			CmfValue.Type.STRING, false);
		typeAtt.setValue(new CmfValue(type.getBaseType()));
		marshalled.setAttribute(typeAtt);

		// TODO: add the attributes...
		info.setMimeType(MimeTools.resolveMimeType(type.getBaseType()));
		info.setLength(src.length());
		info.setFileName(src.getName());
		ret.add(info);
		boolean skipContent = ctx.getSettings().getBoolean(TransferSetting.IGNORE_CONTENT);
		if (this.factory.isCopyContent() && !skipContent) {
			try {
				CmfContentStore<?, ?>.Handle h = streamStore.createHandle(translator, marshalled, info);
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
			} catch (Exception e) {
				this.log.error("Failed to copy the source file [{}] into the content store", src, e);
			}
		}
		return ret;
	}

	@Override
	protected CmfObject.Archetype calculateType(SqlRoot root, SqlFile f) throws Exception {
		File F = f.getAbsolute();
		if (F.isFile()) { return CmfObject.Archetype.DOCUMENT; }
		if (F.isDirectory()) { return CmfObject.Archetype.FOLDER; }
		throw new ExportException(String.format("Filesystem object [%s] is of an unknown type or doesn't exist", F));
	}

	@Override
	protected String calculateLabel(SqlRoot root, SqlFile object) throws Exception {
		return object.getPortableFullPath();
	}

	@Override
	protected String calculateObjectId(SqlRoot root, SqlFile object) throws Exception {
		return object.getId();
	}

	@Override
	protected String calculateSearchKey(SqlRoot root, SqlFile object) throws Exception {
		return object.getSafePath();
	}

	@Override
	protected String calculateName(SqlRoot root, SqlFile object) throws Exception {
		return object.getName();
	}

	@Override
	protected boolean calculateHistoryCurrent(SqlRoot root, SqlFile object) throws Exception {
		// Always true
		return true;
	}
}