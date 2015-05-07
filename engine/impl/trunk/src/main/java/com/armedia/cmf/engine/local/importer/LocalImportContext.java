package com.armedia.cmf.engine.local.importer;

import java.net.URL;

import org.slf4j.Logger;

import com.armedia.cmf.engine.importer.ImportContext;
import com.armedia.cmf.storage.ContentStore;
import com.armedia.cmf.storage.ObjectStorageTranslator;
import com.armedia.cmf.storage.ObjectStore;
import com.armedia.cmf.storage.StoredObjectType;
import com.armedia.cmf.storage.StoredValue;
import com.armedia.commons.utilities.CfgTools;

public class LocalImportContext extends ImportContext<URL, StoredValue> {

	public LocalImportContext(LocalImportContextFactory factory, CfgTools settings, String rootId,
		StoredObjectType rootType, URL session, Logger output, ObjectStorageTranslator<StoredValue> translator,
		ObjectStore<?, ?> objectStore, ContentStore streamStore) {
		super(factory, settings, rootId, rootType, session, output, translator, objectStore, streamStore);
	}
}