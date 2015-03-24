/**
 *
 */

package com.armedia.cmf.engine.documentum.importer;

import org.slf4j.Logger;

import com.armedia.cmf.engine.documentum.DctmMappingUtils;
import com.armedia.cmf.engine.documentum.DctmObjectType;
import com.armedia.cmf.engine.documentum.DctmTranslator;
import com.armedia.cmf.engine.documentum.common.DctmSpecialValues;
import com.armedia.cmf.engine.importer.ImportContext;
import com.armedia.cmf.storage.ContentStore;
import com.armedia.cmf.storage.ObjectStore;
import com.armedia.cmf.storage.StoredObjectType;
import com.armedia.commons.utilities.FileNameTools;
import com.armedia.commons.utilities.Tools;
import com.documentum.fc.client.IDfFolder;
import com.documentum.fc.client.IDfPersistentObject;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.common.DfException;
import com.documentum.fc.common.IDfValue;

/**
 * @author diego
 *
 */
public class DctmImportContext extends ImportContext<IDfSession, IDfPersistentObject, IDfValue> {

	private final DctmSpecialValues specialValues;

	DctmImportContext(DctmImportContextFactory factory, String rootId, StoredObjectType rootType, IDfSession session,
		Logger output, ObjectStore<?, ?> objectStore, ContentStore streamStore) {
		super(factory, factory.getSettings(), rootId, rootType, session, output, DctmTranslator.INSTANCE, objectStore,
			streamStore);
		this.specialValues = factory.getSpecialValues();
	}

	public final boolean isSpecialGroup(String group) {
		return this.specialValues.isSpecialGroup(group);
	}

	public final boolean isSpecialUser(String user) {
		return this.specialValues.isSpecialUser(user);
	}

	public final boolean isSpecialType(String type) {
		return this.specialValues.isSpecialType(type);
	}

	public boolean isUntouchableUser(String user) throws DfException {
		return isSpecialUser(user) || DctmMappingUtils.isSubstitutionForMappableUser(user)
			|| DctmMappingUtils.isMappableUser(getSession(), user);
	}

	@Override
	protected boolean isSurrogateType(StoredObjectType rootType, StoredObjectType target) {
		DctmObjectType dctmRootType = DctmTranslator.translateType(rootType);
		if (dctmRootType == null) { return false; }
		DctmObjectType dctmTarget = DctmTranslator.translateType(target);
		if (dctmTarget == null) { return false; }
		return dctmTarget.getSurrogateOf().contains(dctmRootType) || super.isSurrogateType(rootType, target);
	}

	@Override
	protected IDfPersistentObject locateOrCreatePath(String path) throws Exception {
		return doEnsurePath(path);
	}

	public final IDfFolder ensurePath(String path) throws Exception {
		if (path == null) { throw new IllegalArgumentException("Must provide a path to ensure"); }
		if (!path.startsWith("/")) { throw new IllegalArgumentException(String.format("The path [%s] is not absolute",
			path)); }
		return doEnsurePath(FileNameTools.normalizePath(path, '/'));
	}

	private IDfFolder doEnsurePath(String path) throws Exception {
		final IDfSession session = getSession();
		IDfFolder f = session.getFolderByPath(path);
		if (f != null) { return f; }

		final String dirName = FileNameTools.dirname(path, '/');
		final boolean cabinet = Tools.equals("/", dirName);
		final String type = (cabinet ? "dm_cabinet" : DctmObjectType.FOLDER.getDmType());
		f = IDfFolder.class.cast(session.newObject(type));
		if (!cabinet) {
			final IDfFolder parent = doEnsurePath(dirName);
			f.link(parent.getObjectId().getId());
			parent.save();
		}
		f.save();
		return f;
	}
}