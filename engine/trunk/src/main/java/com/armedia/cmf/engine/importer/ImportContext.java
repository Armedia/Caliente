package com.armedia.cmf.engine.importer;

import org.slf4j.Logger;

import com.armedia.cmf.engine.TransferContext;
import com.armedia.cmf.engine.exporter.ExportListener;
import com.armedia.cmf.storage.ContentStreamStore;
import com.armedia.cmf.storage.ObjectStorageTranslator;
import com.armedia.cmf.storage.ObjectStore;

public class ImportContext<S, T, V> extends TransferContext<S, T, V> {

	ImportContext(ObjectStorageTranslator<T, V> translator, String rootId, S session, ObjectStore<?, ?> objectStore,
		ContentStreamStore fileSystem, Logger output, ExportListener listener) {
		super(translator, rootId, session, objectStore, fileSystem, output);
	}
}