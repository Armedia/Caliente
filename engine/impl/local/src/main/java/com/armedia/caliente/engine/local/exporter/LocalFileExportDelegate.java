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
package com.armedia.caliente.engine.local.exporter;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
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
import java.util.Objects;
import java.util.function.Consumer;

import javax.activation.MimeType;

import org.apache.commons.lang3.StringUtils;

import com.armedia.caliente.engine.TransferSetting;
import com.armedia.caliente.engine.converter.IntermediateAttribute;
import com.armedia.caliente.engine.converter.IntermediateProperty;
import com.armedia.caliente.engine.converter.PathIdHelper;
import com.armedia.caliente.engine.exporter.ExportDelegate;
import com.armedia.caliente.engine.exporter.ExportException;
import com.armedia.caliente.engine.local.common.LocalCommon;
import com.armedia.caliente.engine.local.common.LocalRoot;
import com.armedia.caliente.engine.local.common.LocalSessionWrapper;
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
		Path path = abs.getAbsolute().toPath();
		if (path != null) {
			Path parent = path.getParent();
			if (!Files.isSameFile(parent, this.factory.getRoot().getPath())) {
				ret.add(new LocalFileExportDelegate(this.factory, ctx.getSession(),
					this.factory.getLocalFile(parent.toString())));
			}
		}

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

	protected void applyBasicFileAttributes(Path path, CmfObject<CmfValue> object) throws ExportException {
		try {
			CmfAttribute<CmfValue> att = null;
			final BasicFileAttributeView basic = getFileAttributeView(path, BasicFileAttributeView.class);
			BasicFileAttributes basicAtts = basic.readAttributes();

			att = new CmfAttribute<>(IntermediateAttribute.CREATION_DATE, IntermediateAttribute.CREATION_DATE.type,
				CmfValue.of(new Date(basicAtts.creationTime().toMillis())));
			object.setAttribute(att);

			att = new CmfAttribute<>(IntermediateAttribute.LAST_MODIFICATION_DATE,
				IntermediateAttribute.LAST_MODIFICATION_DATE.type,
				CmfValue.of(new Date(basicAtts.lastModifiedTime().toMillis())));
			object.setAttribute(att);

			att = new CmfAttribute<>(IntermediateAttribute.LAST_ACCESS_DATE,
				IntermediateAttribute.LAST_ACCESS_DATE.type,
				CmfValue.of(new Date(basicAtts.lastAccessTime().toMillis())));
			object.setAttribute(att);

			if (getType() == CmfObject.Archetype.DOCUMENT) {
				att = new CmfAttribute<>(IntermediateAttribute.CONTENT_STREAM_LENGTH,
					IntermediateAttribute.CONTENT_STREAM_LENGTH.type, false);
				att.setValue(CmfValue.of(basicAtts.size()));
				object.setAttribute(att);

				// All documents are roots...
				CmfProperty<CmfValue> versionTreeRoot = new CmfProperty<>(IntermediateProperty.VERSION_TREE_ROOT,
					IntermediateProperty.VERSION_TREE_ROOT.type, false);
				versionTreeRoot.setValue(CmfValue.of(true));
				object.setProperty(versionTreeRoot);
			}
		} catch (IOException e) {
			throw new ExportException(String.format("Failed to collect the basic attribute information for [%s]", path),
				e);
		}
	}

	protected void applyUserDefinedAttributes(Path path, CmfObject<CmfValue> object) {
		final UserDefinedFileAttributeView extendedAtts = getFileAttributeView(path,
			UserDefinedFileAttributeView.class);
		if (extendedAtts == null) { return; }
		CmfAttribute<CmfValue> att = null;
		try {
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
				att.setValue(CmfValue.of(data));
				object.setAttribute(att);
			}
		} catch (Exception e) {
			// Do nothing
		}
	}

	protected void applyDosFileAttributes(Path path, CmfObject<CmfValue> object) {
		final DosFileAttributeView dos = getFileAttributeView(path, DosFileAttributeView.class);
		if (dos == null) { return; }
		CmfAttribute<CmfValue> att = null;
		try {
			DosFileAttributes atts = dos.readAttributes();
			att = new CmfAttribute<>(String.format(LocalFileExportDelegate.DOS_ATT_FORMAT, "hidden"),
				CmfValue.Type.BOOLEAN, false);
			att.setValue(CmfValue.of(atts.isHidden()));
			object.setAttribute(att);

			att = new CmfAttribute<>(String.format(LocalFileExportDelegate.DOS_ATT_FORMAT, "system"),
				CmfValue.Type.BOOLEAN, false);
			att.setValue(CmfValue.of(atts.isSystem()));
			object.setAttribute(att);

			att = new CmfAttribute<>(String.format(LocalFileExportDelegate.DOS_ATT_FORMAT, "archive"),
				CmfValue.Type.BOOLEAN, false);
			att.setValue(CmfValue.of(atts.isArchive()));
			object.setAttribute(att);

			att = new CmfAttribute<>(String.format(LocalFileExportDelegate.DOS_ATT_FORMAT, "readonly"),
				CmfValue.Type.BOOLEAN, false);
			att.setValue(CmfValue.of(atts.isReadOnly()));
			object.setAttribute(att);
		} catch (Exception e) {
			// do nothing...
		}
	}

	protected void applyPosixFileAttributes(Path path, CmfObject<CmfValue> object) {
		final PosixFileAttributeView posix = getFileAttributeView(path, PosixFileAttributeView.class);
		if (posix == null) { return; }
		CmfAttribute<CmfValue> att = null;
		try {
			PosixFileAttributes posixAtts = posix.readAttributes();
			GroupPrincipal ownerGroup = posixAtts.group();
			att = new CmfAttribute<>(IntermediateAttribute.GROUP, IntermediateAttribute.GROUP.type, false);
			att.setValue(CmfValue.of(ownerGroup.getName()));
			object.setAttribute(att);
		} catch (Exception e) {
			// Do nothing...
		}
	}

	@Override
	protected boolean baseMarshal(LocalExportContext ctx, CmfObject<CmfValue> encoded) throws ExportException {
		final Path path = this.object.getAbsolute().toPath();
		CmfAttribute<CmfValue> att = null;
		att = new CmfAttribute<>(IntermediateAttribute.NAME, IntermediateAttribute.NAME.type, false);
		att.setValue(CmfValue.of(path.getFileName().toString()));
		encoded.setAttribute(att);

		att = new CmfAttribute<>(IntermediateAttribute.OBJECT_ID, IntermediateAttribute.OBJECT_ID.type, false);
		att.setValue(CmfValue.of(getObjectId()));
		encoded.setAttribute(att);

		// Ok... we have the attribute views, export the information
		applyBasicFileAttributes(path, encoded);
		applyUserDefinedAttributes(path, encoded);
		applyDosFileAttributes(path, encoded);
		applyPosixFileAttributes(path, encoded);

		UserPrincipal owner = getOwner(path);
		if (owner != null) {
			att = new CmfAttribute<>(IntermediateAttribute.CREATED_BY, IntermediateAttribute.CREATED_BY.type, false);
			att.setValue(CmfValue.of(owner.getName()));
			encoded.setAttribute(att);

			att = new CmfAttribute<>(IntermediateAttribute.LAST_MODIFIED_BY,
				IntermediateAttribute.LAST_MODIFIED_BY.type, false);
			att.setValue(CmfValue.of(owner.getName()));
			encoded.setAttribute(att);

			att = new CmfAttribute<>(IntermediateAttribute.OWNER, IntermediateAttribute.OWNER.type, false);
			att.setValue(CmfValue.of(owner.getName()));
			encoded.setAttribute(att);
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
		prop = new CmfProperty<>(IntermediateProperty.PARENT_ID, IntermediateProperty.PARENT_ID.type, true);
		att = new CmfAttribute<>(IntermediateAttribute.PARENT_ID, IntermediateAttribute.PARENT_ID.type, true);
		if (parentId != null) {
			att.setValue(CmfValue.of(parentId));
			prop.setValue(att.getValue());
		}
		encoded.setAttribute(att);
		encoded.setProperty(prop);

		try {
			encoded.setProperty(new CmfProperty<>(IntermediateProperty.PARENT_TREE_IDS,
				IntermediateProperty.PARENT_TREE_IDS.type, calculateParentTreeIds(path)));
		} catch (IOException e) {
			throw new ExportException(String.format("Failed to calculate the parent path IDs for [%s]", path), e);
		}

		encoded.setProperty(new CmfProperty<>(IntermediateProperty.PATH, IntermediateProperty.PATH.type,
			CmfValue.of(this.object.getPortableParentPath())));

		encoded.setProperty(new CmfProperty<>(IntermediateProperty.FULL_PATH, IntermediateProperty.FULL_PATH.type,
			CmfValue.of(this.object.getPortableFullPath())));
		encoded.setProperty(new CmfProperty<>(IntermediateProperty.PRESERVED_NAME,
			IntermediateProperty.PRESERVED_NAME.type, CmfValue.of(this.object.getName())));

		if (this.object.isFolder()) {
			// If this is a folder, the path is set to its full, relative path
			att = new CmfAttribute<>(IntermediateAttribute.PATH, CmfValue.Type.STRING, true);
			att.setValue(CmfValue.of(this.object.getPortableFullPath()));
			encoded.setAttribute(att);
		}

		LocalVersionHistory history = this.factory.getEngine().getHistory(this.object);
		encoded.setProperty(new CmfProperty<>(IntermediateProperty.HEAD_NAME, IntermediateProperty.HEAD_NAME.type,
			CmfValue.of(FileNameTools.basename(history.getCurrentVersion().getHistoryRadix(), '/'))));
		encoded.setProperty(new CmfProperty<>(IntermediateProperty.VERSION_COUNT,
			IntermediateProperty.VERSION_COUNT.type, CmfValue.of(history.size())));
		Integer historyIndex = history.getIndexFor(this.object.getVersionTag());
		encoded.setProperty(new CmfProperty<>(IntermediateProperty.VERSION_INDEX,
			IntermediateProperty.VERSION_INDEX.type, CmfValue.of(historyIndex.intValue() + 1)));
		historyIndex = history.getCurrentIndex();
		encoded.setProperty(new CmfProperty<>(IntermediateProperty.VERSION_HEAD_INDEX,
			IntermediateProperty.VERSION_HEAD_INDEX.type, CmfValue.of(historyIndex.intValue() + 1)));

		return true;
	}

	protected CmfValue calculateParentTreeIds(Path p) throws IOException {
		p = this.root.relativize(p);
		List<String> parents = new LinkedList<>();
		while (true) {
			p = p.getParent();
			if ((p == null) || StringUtils.isEmpty(p.getFileName().toString())) {
				break;
			}
			parents.add(0, LocalCommon.calculateId(p));
		}
		return CmfValue.of(PathIdHelper.encodePaths(parents));
	}

	@Override
	protected Collection<LocalFileExportDelegate> identifyDependents(CmfObject<CmfValue> marshalled,
		LocalExportContext ctx) throws Exception {
		return null;
	}

	@Override
	protected Collection<? extends ExportDelegate<?, LocalRoot, LocalSessionWrapper, CmfValue, LocalExportContext, LocalExportDelegateFactory, ?>> identifyAntecedents(
		CmfObject<CmfValue> marshalled, LocalExportContext ctx) throws Exception {
		if (this.object.isFolder()) { return super.identifyAntecedents(marshalled, ctx); }

		if (this.object == null) {
			throw new IllegalArgumentException("Must provide an object whose versions to analyze");
		}

		// Ok...so...we have to find all the history
		Collection<LocalFileExportDelegate> antecedents = new LinkedList<>();
		for (LocalFile file : this.factory.getEngine().getHistory(this.object)) {
			if (Objects.equals(file.getId(), this.object.getId())) {
				break;
			}
			antecedents.add(new LocalFileExportDelegate(this.factory, this.root, file));
		}

		return antecedents;
	}

	@Override
	protected Collection<? extends ExportDelegate<?, LocalRoot, LocalSessionWrapper, CmfValue, LocalExportContext, LocalExportDelegateFactory, ?>> identifySuccessors(
		CmfObject<CmfValue> marshalled, LocalExportContext ctx) throws Exception {
		if (this.object.isFolder()) { return super.identifySuccessors(marshalled, ctx); }

		if (this.object == null) {
			throw new IllegalArgumentException("Must provide an object whose versions to analyze");
		}

		// Ok...so...we have to find all the history
		Collection<LocalFileExportDelegate> successors = new LinkedList<>();
		Consumer<LocalFileExportDelegate> consumer = null;
		for (LocalFile file : this.factory.getEngine().getHistory(this.object)) {
			if (Objects.equals(file.getId(), this.object.getId())) {
				consumer = successors::add;
				continue;
			}
			if (consumer != null) {
				consumer.accept(new LocalFileExportDelegate(this.factory, this.root, file));
			}
		}

		return successors;
	}

	@Override
	protected List<CmfContentStream> storeContent(LocalExportContext ctx, CmfAttributeTranslator<CmfValue> translator,
		CmfObject<CmfValue> marshalled, CmfContentStore<?, ?> streamStore, boolean includeRenditions) {
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

		CmfAttribute<CmfValue> typeAtt = new CmfAttribute<>(IntermediateAttribute.CONTENT_STREAM_MIME_TYPE,
			CmfValue.Type.STRING, false);
		typeAtt.setValue(CmfValue.of(type.getBaseType()));
		marshalled.setAttribute(typeAtt);

		// TODO: add the attributes...
		info.setMimeType(MimeTools.resolveMimeType(type.getBaseType()));
		info.setLength(src.length());
		info.setFileName(src.getName());
		info.setProperty(IntermediateProperty.FULL_PATH, this.object.getPortableFullPath());
		final CmfContentStore<?, ?>.Handle h = streamStore.addContentStream(translator, marshalled, info);
		ret.add(info);
		boolean skipContent = ctx.getSettings().getBoolean(TransferSetting.IGNORE_CONTENT);
		if (this.factory.isCopyContent() && !skipContent) {
			try {
				File tgt = h.getFile(true);
				if (tgt != null) {
					if (this.log.isDebugEnabled()) {
						this.log.debug("Copying {} bytes from [{}] into [{}]", src.length(), src, tgt);
					}
					Files.copy(src.toPath(), tgt.toPath(), StandardCopyOption.REPLACE_EXISTING);
				} else {
					try (FileChannel in = FileChannel.open(src.toPath(), StandardOpenOption.READ)) {
						h.store(in);
					}
				}
			} catch (Exception e) {
				this.log.error("Failed to copy the source file [{}] into the content store", src, e);
			}
		}
		return ret;
	}

	@Override
	protected CmfObject.Archetype calculateType(LocalRoot root, LocalFile f) throws Exception {
		File F = f.getAbsolute();
		if (!F.exists()) { throw new ExportException(String.format("Filesystem object [%s] does not exist", F)); }
		if (F.isFile()) { return CmfObject.Archetype.DOCUMENT; }
		if (F.isDirectory()) { return CmfObject.Archetype.FOLDER; }
		throw new ExportException(String.format("Filesystem object [%s] is of an unknown type", F));
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
	protected String calculateHistoryId(LocalRoot session, LocalFile object) throws Exception {
		return object.getHistoryId();
	}

	@Override
	protected boolean calculateHistoryCurrent(LocalRoot root, LocalFile object) throws Exception {
		return object.isHeadRevision();
	}
}