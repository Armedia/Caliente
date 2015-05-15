package com.armedia.cmf.engine.local.importer;

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
import java.util.EnumMap;
import java.util.Map;

import com.armedia.cmf.engine.converter.IntermediateAttribute;
import com.armedia.cmf.engine.converter.IntermediateProperty;
import com.armedia.cmf.engine.importer.ImportDelegate;
import com.armedia.cmf.engine.local.common.LocalRoot;
import com.armedia.cmf.engine.local.common.LocalSessionWrapper;
import com.armedia.cmf.storage.ObjectStorageTranslator;
import com.armedia.cmf.storage.StoredAttribute;
import com.armedia.cmf.storage.StoredObject;
import com.armedia.cmf.storage.StoredProperty;
import com.armedia.cmf.storage.StoredValue;
import com.armedia.commons.utilities.Tools;

public abstract class LocalImportDelegate
extends
ImportDelegate<File, LocalRoot, LocalSessionWrapper, StoredValue, LocalImportContext, LocalImportDelegateFactory, LocalImportEngine> {

	protected final File targetFile;
	protected final Path targetPath;
	protected final String newId;

	protected LocalImportDelegate(LocalImportDelegateFactory factory, StoredObject<StoredValue> storedObject)
		throws Exception {
		super(factory, File.class, storedObject);
		StoredProperty<StoredValue> pathProp = this.storedObject
			.getProperty(IntermediateProperty.DEFAULT_PATH.encode());
		File root = this.factory.getRoot().getFile();
		File tgt = root;
		if (pathProp != null) {
			for (StoredValue v : pathProp) {
				// TODO: potentially fix the target path
				tgt = new File(tgt, v.asString());
			}
		} else {
			pathProp = this.storedObject.getProperty(IntermediateProperty.PATH.encode());
			tgt = new File(root, pathProp.getValue().asString());
		}
		this.targetFile = tgt.getCanonicalFile();
		this.targetPath = this.targetFile.toPath();
		this.newId = String.format("%08X", this.targetFile.hashCode());
	}

	protected boolean isSameDatesAndOwners(ObjectStorageTranslator<StoredValue> translator) throws IOException,
	ParseException {
		final UserPrincipalLookupService userSvc = this.targetPath.getFileSystem().getUserPrincipalLookupService();
		final BasicFileAttributeView basicView = Files.getFileAttributeView(this.targetPath,
			BasicFileAttributeView.class);
		final PosixFileAttributeView posixView = Files.getFileAttributeView(this.targetPath,
			PosixFileAttributeView.class);
		final FileOwnerAttributeView ownerView;
		if (posixView != null) {
			ownerView = posixView;
		} else {
			ownerView = Files.getFileAttributeView(this.targetPath, FileOwnerAttributeView.class);
		}

		final BasicFileAttributes basic = basicView.readAttributes();
		Map<IntermediateAttribute, FileTime> dates = new EnumMap<IntermediateAttribute, FileTime>(
			IntermediateAttribute.class);
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

			StoredAttribute<StoredValue> v = this.storedObject.getAttribute(translator.decodeAttributeName(
				this.storedObject.getType(), att.encode()));
			if ((v == null) || !v.hasValues()) {
				continue;
			}
			FileTime remote = FileTime.fromMillis(v.getValue().asTime().getTime());
			if (!Tools.equals(local, remote)) { return false; }
		}

		if (ownerView != null) {
			UserPrincipal local = ownerView.getOwner();
			if (local != null) {
				StoredAttribute<StoredValue> v = this.storedObject.getAttribute(translator.decodeAttributeName(
					this.storedObject.getType(), IntermediateAttribute.GROUP.encode()));
				if ((v != null) && v.hasValues()) {
					try {
						UserPrincipal remote = userSvc.lookupPrincipalByName(v.getValue().asString());
						if (!Tools.equals(local, remote)) { return false; }
					} catch (UserPrincipalNotFoundException e) {
						// Ignore...
					}
				}
			}
		}

		if (posixView != null) {
			GroupPrincipal local = posixView.readAttributes().group();
			if (local != null) {
				StoredAttribute<StoredValue> v = this.storedObject.getAttribute(translator.decodeAttributeName(
					this.storedObject.getType(), IntermediateAttribute.GROUP.encode()));
				if ((v != null) && v.hasValues()) {
					try {
						GroupPrincipal remote = userSvc.lookupPrincipalByGroupName(v.getValue().asString());
						if (!Tools.equals(local, remote)) { return false; }
					} catch (UserPrincipalNotFoundException e) {
						// Ignore...
					}
				}
			}
		}

		return true;
	}

	protected void applyAttributes(ObjectStorageTranslator<StoredValue> translator) throws IOException, ParseException {
		final UserPrincipalLookupService userSvc = this.targetPath.getFileSystem().getUserPrincipalLookupService();

		final BasicFileAttributeView basicView = Files.getFileAttributeView(this.targetPath,
			BasicFileAttributeView.class);
		final PosixFileAttributeView posixView = Files.getFileAttributeView(this.targetPath,
			PosixFileAttributeView.class);
		final AclFileAttributeView aclView = Files.getFileAttributeView(this.targetPath, AclFileAttributeView.class);
		final FileOwnerAttributeView ownerView;
		if (posixView != null) {
			ownerView = posixView;
		} else {
			ownerView = Files.getFileAttributeView(this.targetPath, FileOwnerAttributeView.class);
		}

		final BasicFileAttributes basic = basicView.readAttributes();
		FileTime created = basic.creationTime();
		boolean createdChanged = false;
		FileTime modified = basic.lastModifiedTime();
		boolean modifiedChanged = false;
		FileTime accessed = basic.lastAccessTime();
		boolean accessedChanged = false;

		StoredAttribute<StoredValue> v = null;

		// Now we ensure that the dates are consistent with whatever
		// comes from the CMS. If the data that comes from the CMS
		// is borked, we replicate it locally (sadly). However,
		// if not all the data came from the CMS, then we fill in
		// the blanks assuming the logical order of C<=M<=A (C = creation
		// date, M = modification date, A = last access date).
		v = this.storedObject.getAttribute(translator.decodeAttributeName(this.storedObject.getType(),
			IntermediateAttribute.LAST_ACCESS_DATE.encode()));
		if ((v != null) && v.hasValues()) {
			accessed = FileTime.fromMillis(v.getValue().asTime().getTime());
			accessedChanged = true;
		}

		v = this.storedObject.getAttribute(translator.decodeAttributeName(this.storedObject.getType(),
			IntermediateAttribute.LAST_MODIFICATION_DATE.encode()));
		if ((v != null) && v.hasValues()) {
			modified = FileTime.fromMillis(v.getValue().asTime().getTime());
			modifiedChanged = true;
		}

		v = this.storedObject.getAttribute(translator.decodeAttributeName(this.storedObject.getType(),
			IntermediateAttribute.CREATION_DATE.encode()));
		if ((v != null) && v.hasValues()) {
			created = FileTime.fromMillis(v.getValue().asTime().getTime());
			createdChanged = true;
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
				created = accessed;
				modified = accessed;
				break;

			case 6: // Only accessed was unchanged
				accessed = modified;
				// Fall-through
			case 2: // only modified was set
				created = modified;
				break;

			case 3: // only created was unchanged
				created = modified;
				break;

			case 5: // only modified was unchanged
				modified = created;
				// fall-through
			case 4: // only created was set
				accessed = created;
				break;

		}

		basicView.setTimes(modified, accessed, created);

		if (posixView != null) {
			// Set the group
			v = this.storedObject.getAttribute(translator.decodeAttributeName(this.storedObject.getType(),
				IntermediateAttribute.GROUP.encode()));
			if ((v != null) && v.hasValues()) {
				try {
					GroupPrincipal group = userSvc.lookupPrincipalByGroupName(v.getValue().asString());
					posixView.setGroup(group);
				} catch (UserPrincipalNotFoundException e) {
					// No such group...
				}
			}
		}

		if (ownerView != null) {
			// Set the owner
			v = this.storedObject.getAttribute(translator.decodeAttributeName(this.storedObject.getType(),
				IntermediateAttribute.OWNER.encode()));
			if ((v != null) && v.hasValues()) {
				try {
					UserPrincipal owner = userSvc.lookupPrincipalByGroupName(v.getValue().asString());
					ownerView.setOwner(owner);
				} catch (UserPrincipalNotFoundException e) {
					// No such user...
				}
			}
		}

		if (aclView != null) {
			// TODO: Restore the ACL if possible
			// TODO: How to map ACL entries from a CMS to an O/S ACL?
			aclView.getAcl();
		}
	}
}