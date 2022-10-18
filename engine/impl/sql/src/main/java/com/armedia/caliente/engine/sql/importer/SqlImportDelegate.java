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
package com.armedia.caliente.engine.sql.importer;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.AclFileAttributeView;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileOwnerAttributeView;
import java.nio.file.attribute.FileTime;
import java.nio.file.attribute.GroupPrincipal;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.UserPrincipal;
import java.nio.file.attribute.UserPrincipalLookupService;
import java.nio.file.attribute.UserPrincipalNotFoundException;
import java.text.ParseException;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

import org.apache.commons.lang3.SystemUtils;

import com.armedia.caliente.engine.converter.IntermediateAttribute;
import com.armedia.caliente.engine.importer.ImportDelegate;
import com.armedia.caliente.engine.importer.ImportException;
import com.armedia.caliente.engine.importer.ImportOutcome;
import com.armedia.caliente.engine.sql.common.SqlRoot;
import com.armedia.caliente.engine.sql.common.SqlSessionWrapper;
import com.armedia.caliente.store.CmfAttribute;
import com.armedia.caliente.store.CmfAttributeNameMapper;
import com.armedia.caliente.store.CmfAttributeTranslator;
import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.store.CmfStorageException;
import com.armedia.caliente.store.CmfValue;
import com.armedia.caliente.store.tools.FilenameEncoder;
import com.armedia.commons.utilities.FileNameTools;

public abstract class SqlImportDelegate extends
	ImportDelegate<File, SqlRoot, SqlSessionWrapper, CmfValue, SqlImportContext, SqlImportDelegateFactory, SqlImportEngine> {

	protected SqlImportDelegate(SqlImportDelegateFactory factory, CmfObject<CmfValue> storedObject) throws Exception {
		super(factory, File.class, storedObject);
	}

	protected final String getNewId(File f) {
		return String.format("%08X", f.hashCode());
	}

	@Override
	protected final Collection<ImportOutcome> importObject(CmfAttributeTranslator<CmfValue> translator,
		SqlImportContext ctx) throws ImportException, CmfStorageException {

		CmfAttribute<CmfValue> att = this.cmfObject.getAttribute(IntermediateAttribute.IS_LATEST_VERSION);
		if ((att != null) && att.hasValues()) {
			CmfValue v = att.getValue();
			if (!v.isNull() && !v.asBoolean() && !this.factory.isIncludeAllVersions()) {
				// If this isn't the last version, we bork out if we're configured to only deal
				// with the last version
				if (this.log.isDebugEnabled()) {
					this.log.warn("Skipping non-final version for {}", this.cmfObject.getDescription());
				}
				return Collections.singleton(ImportOutcome.SKIPPED);
			}
		}

		return doImportObject(translator, ctx);
	}

	protected abstract Collection<ImportOutcome> doImportObject(CmfAttributeTranslator<CmfValue> translator,
		SqlImportContext ctx) throws ImportException, CmfStorageException;

	protected final File getTargetFile(SqlImportContext ctx) throws ImportException, IOException {
		// TODO: We must also determine if the target FS requires "windows mode".. for instance
		// for NTFS on Linux, windows restrictions must be observed... but there's no "clean"
		// way to figure that out from Java...
		final boolean windowsMode = SystemUtils.IS_OS_WINDOWS;

		File tgt = ctx.getSession().getFile();
		String p = getFixedPath(ctx);
		if (p == null) { return null; }

		for (String s : FileNameTools.tokenize(p, '/')) {
			tgt = new File(tgt, FilenameEncoder.safeEncode(s, windowsMode));
		}

		// We always fix the file's name, since it's not part of the path and may also need fixing.
		// Same dilemma as above, though - need to know "when" to use windows mode...
		String name = FilenameEncoder.safeEncode(this.cmfObject.getName(), windowsMode);
		return new File(tgt, name).getCanonicalFile();
	}

	protected boolean isSameDatesAndOwners(File targetFile, CmfAttributeTranslator<CmfValue> translator)
		throws IOException, ParseException {
		Path targetPath = targetFile.toPath();
		final CmfAttributeNameMapper nameMapper = translator.getAttributeNameMapper();
		final UserPrincipalLookupService userSvc = targetPath.getFileSystem().getUserPrincipalLookupService();
		final BasicFileAttributeView basicView = Files.getFileAttributeView(targetPath, BasicFileAttributeView.class);
		final PosixFileAttributeView posixView = Files.getFileAttributeView(targetPath, PosixFileAttributeView.class);
		final FileOwnerAttributeView ownerView;
		if (posixView != null) {
			ownerView = posixView;
		} else {
			ownerView = Files.getFileAttributeView(targetPath, FileOwnerAttributeView.class);
		}

		final BasicFileAttributes basic = basicView.readAttributes();
		Map<IntermediateAttribute, FileTime> dates = new EnumMap<>(IntermediateAttribute.class);
		dates.put(IntermediateAttribute.CREATION_DATE, basic.creationTime());
		dates.put(IntermediateAttribute.LAST_MODIFICATION_DATE, basic.lastModifiedTime());
		dates.put(IntermediateAttribute.LAST_ACCESS_DATE, basic.lastAccessTime());

		IntermediateAttribute[] atts = {
			IntermediateAttribute.LAST_ACCESS_DATE, IntermediateAttribute.LAST_MODIFICATION_DATE,
			IntermediateAttribute.CREATION_DATE
		};
		for (IntermediateAttribute att : atts) {
			FileTime local = dates.get(att);
			if (local == null) {
				continue;
			}

			CmfAttribute<CmfValue> v = this.cmfObject
				.getAttribute(nameMapper.decodeAttributeName(this.cmfObject.getType(), att));
			if ((v == null) || !v.hasValues()) {
				continue;
			}
			CmfValue sv = v.getValue();
			if (sv.isNull()) {
				continue;
			}
			FileTime remote = FileTime.fromMillis(sv.asTime().getTime());
			if (!Objects.equals(local, remote)) { return false; }
		}

		if (ownerView != null) {
			UserPrincipal local = ownerView.getOwner();
			if (local != null) {
				CmfAttribute<CmfValue> v = this.cmfObject.getAttribute(
					nameMapper.decodeAttributeName(this.cmfObject.getType(), IntermediateAttribute.GROUP));
				if ((v != null) && v.hasValues()) {
					CmfValue sv = v.getValue();
					if (!sv.isNull()) {
						try {
							UserPrincipal remote = userSvc.lookupPrincipalByName(v.getValue().asString());
							if (!Objects.equals(local, remote)) { return false; }
						} catch (UserPrincipalNotFoundException e) {
							// Ignore...
						}
					}
				}
			}
		}

		if (posixView != null) {
			GroupPrincipal local = posixView.readAttributes().group();
			if (local != null) {
				CmfAttribute<CmfValue> v = this.cmfObject.getAttribute(
					nameMapper.decodeAttributeName(this.cmfObject.getType(), IntermediateAttribute.GROUP));
				if ((v != null) && v.hasValues()) {
					CmfValue sv = v.getValue();
					if (!sv.isNull()) {
						try {
							GroupPrincipal remote = userSvc.lookupPrincipalByGroupName(v.getValue().asString());
							if (!Objects.equals(local, remote)) { return false; }
						} catch (UserPrincipalNotFoundException e) {
							// Ignore...
						}
					}
				}
			}
		}

		return true;
	}

	protected void applyAttributes(File targetFile, CmfAttributeTranslator<CmfValue> translator)
		throws IOException, ParseException {
		Path targetPath = targetFile.toPath();
		final UserPrincipalLookupService userSvc = targetPath.getFileSystem().getUserPrincipalLookupService();
		final CmfAttributeNameMapper nameMapper = translator.getAttributeNameMapper();
		final BasicFileAttributeView basicView = Files.getFileAttributeView(targetPath, BasicFileAttributeView.class);
		final PosixFileAttributeView posixView = Files.getFileAttributeView(targetPath, PosixFileAttributeView.class);
		final AclFileAttributeView aclView = Files.getFileAttributeView(targetPath, AclFileAttributeView.class);
		final FileOwnerAttributeView ownerView;
		if (posixView != null) {
			ownerView = posixView;
		} else {
			ownerView = Files.getFileAttributeView(targetPath, FileOwnerAttributeView.class);
		}

		final BasicFileAttributes basic = basicView.readAttributes();
		FileTime created = basic.creationTime();
		boolean createdChanged = false;
		FileTime modified = basic.lastModifiedTime();
		boolean modifiedChanged = false;
		FileTime accessed = basic.lastAccessTime();
		boolean accessedChanged = false;

		CmfAttribute<CmfValue> v = null;

		// Now we ensure that the dates are consistent with whatever
		// comes from the CMS. If the data that comes from the CMS
		// is borked, we replicate it locally (sadly). However,
		// if not all the data came from the CMS, then we fill in
		// the blanks assuming the logical order of C<=M<=A (C = creation
		// date, M = modification date, A = last access date).
		v = this.cmfObject.getAttribute(
			nameMapper.decodeAttributeName(this.cmfObject.getType(), IntermediateAttribute.LAST_ACCESS_DATE));
		if ((v != null) && v.hasValues()) {
			CmfValue sv = v.getValue();
			if (!sv.isNull()) {
				accessed = FileTime.fromMillis(sv.asTime().getTime());
				accessedChanged = true;
			}
		}

		v = this.cmfObject.getAttribute(
			nameMapper.decodeAttributeName(this.cmfObject.getType(), IntermediateAttribute.LAST_MODIFICATION_DATE));
		if ((v != null) && v.hasValues()) {
			CmfValue sv = v.getValue();
			if (!sv.isNull()) {
				modified = FileTime.fromMillis(sv.asTime().getTime());
				modifiedChanged = true;
			}
		}

		v = this.cmfObject.getAttribute(
			nameMapper.decodeAttributeName(this.cmfObject.getType(), IntermediateAttribute.CREATION_DATE));
		if ((v != null) && v.hasValues()) {
			CmfValue sv = v.getValue();
			if (!sv.isNull()) {
				created = FileTime.fromMillis(sv.asTime().getTime());
				createdChanged = true;
			}
		}

		int changes = 0;
		changes |= (createdChanged ? 0b0100 : 0);
		changes |= (modifiedChanged ? 0b0010 : 0);
		changes |= (accessedChanged ? 0b0001 : 0);
		switch (changes) {
			case 0:
			case 7:
				// No changes or all changed, fix nothing...
				break;

			case 1: // only accessed was set
				modified = accessed;
				created = accessed;
				break;

			case 2: // only modified was set
				accessed = modified;
				created = modified;
				break;

			case 3: // only created was unchanged
				created = modified;
				break;

			case 5: // only modified was unchanged
				modified = created;
				break;

			case 6: // Only accessed was unchanged
				accessed = modified;
				break;

			case 4: // only created was set
				accessed = created;
				modified = created;
				break;

		}

		basicView.setTimes(modified, accessed, created);

		if (posixView != null) {
			// Set the group
			v = this.cmfObject
				.getAttribute(nameMapper.decodeAttributeName(this.cmfObject.getType(), IntermediateAttribute.GROUP));
			if ((v != null) && v.hasValues()) {
				CmfValue sv = v.getValue();
				if (!sv.isNull()) {
					try {
						GroupPrincipal group = userSvc.lookupPrincipalByGroupName(sv.asString());
						posixView.setGroup(group);
					} catch (UserPrincipalNotFoundException e) {
						// No such group...
					}
				}
			}
		}

		if (ownerView != null) {
			// Set the owner
			v = this.cmfObject
				.getAttribute(nameMapper.decodeAttributeName(this.cmfObject.getType(), IntermediateAttribute.OWNER));
			if ((v != null) && v.hasValues()) {
				CmfValue sv = v.getValue();
				if (!sv.isNull()) {
					try {
						UserPrincipal owner = userSvc.lookupPrincipalByGroupName(sv.asString());
						ownerView.setOwner(owner);
					} catch (UserPrincipalNotFoundException e) {
						// No such user...
					}
				}
			}
		}

		if (aclView != null) {
			// TODO: Restore the ACL if possible
			// TODO: How to map ACL entries from a CMS to an O/S ACL?
			aclView.getAcl();
		}
	}

	/*
	protected void saveCompanionMetadata() {
		try {
			CmfContentStream metadata = new CmfContentStream(0, "$metadata$", 0, null);
			metadata.setExtension("xml");
			metadata.setProperty(CmfContentStream.BASENAME, "metadata");
			CmfContentStore<?, ?>.Handle h = streamStore.getHandle(getTranslator(), marshaled, metadata);
			try (OutputStream out = h.getOutputStream()) {
				if (out != null) {
					XmlBase.storeToXML(new MetadataT(encoded), out);
				}
			}
		} catch (Exception e) {
			throw new ExportException(
				String.format("Failed to marshal the XML-formatted metadata for %s", marshaled.getDescription()), e);
		}
		break;
	}
	*/
}