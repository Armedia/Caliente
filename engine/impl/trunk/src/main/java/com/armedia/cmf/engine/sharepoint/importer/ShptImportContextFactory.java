/**
 *
 */

package com.armedia.cmf.engine.sharepoint.importer;

import org.slf4j.Logger;

import com.armedia.cmf.engine.importer.ImportContextFactory;
import com.armedia.cmf.engine.sharepoint.ShptSession;
import com.armedia.cmf.engine.sharepoint.ShptSessionWrapper;
import com.armedia.cmf.engine.sharepoint.types.ShptFolder;
import com.armedia.cmf.storage.ContentStore;
import com.armedia.cmf.storage.ObjectStore;
import com.armedia.cmf.storage.StoredObjectType;
import com.armedia.cmf.storage.StoredValue;
import com.armedia.commons.utilities.CfgTools;
import com.independentsoft.share.Folder;

/**
 * @author diego
 *
 */
public class ShptImportContextFactory extends
	ImportContextFactory<ShptSession, ShptSessionWrapper, StoredValue, ShptImportContext, ShptImportEngine, Folder> {

	ShptImportContextFactory(ShptImportEngine engine, CfgTools settings) {
		super(engine, settings);
	}

	@Override
	protected ShptImportContext constructContext(String rootId, StoredObjectType rootType, ShptSession session,
		Logger output, ObjectStore<?, ?> objectStore, ContentStore contentStore) {
		return new ShptImportContext(this, getSettings(), rootId, rootType, session, output, null, objectStore,
			contentStore);
	}

	@Override
	protected Folder locateFolder(ShptSession session, String path) throws Exception {
		// TODO: does this raise a 404 exception? or return null?
		return session.getFolder(path);
	}

	@Override
	protected Folder createFolder(ShptSession session, Folder parent, String name) throws Exception {
		ShptFolder parentFolder = ShptFolder.class.cast(parent);
		return session.createFolder(String.format("%s/%s", parentFolder.getServerRelativeUrl(), name));
	}
}