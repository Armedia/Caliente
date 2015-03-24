/**
 *
 */

package com.armedia.cmf.engine.sharepoint.importer;

import org.slf4j.Logger;

import com.armedia.cmf.engine.importer.ImportContext;
import com.armedia.cmf.engine.sharepoint.ShptSession;
import com.armedia.cmf.engine.sharepoint.ShptSessionException;
import com.armedia.cmf.engine.sharepoint.types.ShptFolder;
import com.armedia.cmf.engine.sharepoint.types.ShptObject;
import com.armedia.cmf.storage.ContentStore;
import com.armedia.cmf.storage.ObjectStorageTranslator;
import com.armedia.cmf.storage.ObjectStore;
import com.armedia.cmf.storage.StoredObjectType;
import com.armedia.cmf.storage.StoredValue;
import com.armedia.commons.utilities.CfgTools;
import com.independentsoft.share.Folder;

/**
 * @author diego
 *
 */
public class ShptImportContext extends ImportContext<ShptSession, ShptObject<?>, StoredValue> {

	public ShptImportContext(ShptImportContextFactory factory, CfgTools settings, String rootId,
		StoredObjectType rootType, ShptSession session, Logger output,
		ObjectStorageTranslator<ShptObject<?>, StoredValue> translator, ObjectStore<?, ?> objectStore,
		ContentStore streamStore) {
		super(factory, settings, rootId, rootType, session, output, translator, objectStore, streamStore);
	}

	@Override
	protected boolean isSurrogateType(StoredObjectType rootType, StoredObjectType target) {
		return super.isSurrogateType(rootType, target);
	}

	@Override
	protected ShptObject<?> locateOrCreatePath(String path) throws Exception {
		ShptSession session = getSession();
		// TODO: does this raise a 404 exception? or return null?
		Folder f = session.getFolder(path);
		if (f == null) {
			f = session.createFolder(path);
		}
		if (f == null) { throw new ShptSessionException(String.format("Failed to locate or create the path [%s]", path)); }
		return new ShptFolder(session, f);
	}
}