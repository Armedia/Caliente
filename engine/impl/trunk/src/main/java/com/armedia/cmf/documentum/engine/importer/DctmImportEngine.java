/**
 *
 */

package com.armedia.cmf.documentum.engine.importer;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;

import com.armedia.cmf.documentum.engine.DctmObjectType;
import com.armedia.cmf.documentum.engine.DctmSessionFactory;
import com.armedia.cmf.documentum.engine.DctmSessionWrapper;
import com.armedia.cmf.documentum.engine.DctmTranslator;
import com.armedia.cmf.documentum.engine.DfValueFactory;
import com.armedia.cmf.documentum.engine.UnsupportedDctmObjectTypeException;
import com.armedia.cmf.engine.SessionFactory;
import com.armedia.cmf.engine.importer.ImportEngine;
import com.armedia.cmf.engine.importer.ImportException;
import com.armedia.cmf.engine.importer.ImportOutcome;
import com.armedia.cmf.engine.importer.ImportStrategy;
import com.armedia.cmf.storage.ContentStore;
import com.armedia.cmf.storage.ObjectStorageTranslator;
import com.armedia.cmf.storage.ObjectStore;
import com.armedia.cmf.storage.StorageException;
import com.armedia.cmf.storage.StoredDataType;
import com.armedia.cmf.storage.StoredObject;
import com.armedia.cmf.storage.StoredObjectType;
import com.armedia.cmf.storage.StoredValueDecoderException;
import com.documentum.fc.client.IDfPersistentObject;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.common.DfException;
import com.documentum.fc.common.IDfValue;

/**
 * @author diego
 *
 */
public class DctmImportEngine extends
	ImportEngine<IDfSession, DctmSessionWrapper, IDfPersistentObject, IDfValue, DctmImportContext> {

	private static final Set<String> TARGETS = Collections.singleton("dctm");
	private final Map<DctmObjectType, DctmImportAbstract<?>> delegates;

	public DctmImportEngine() {
		Map<DctmObjectType, DctmImportAbstract<?>> m = new EnumMap<DctmObjectType, DctmImportAbstract<?>>(
			DctmObjectType.class);
		m.put(DctmObjectType.ACL, new DctmImportACL(this));
		m.put(DctmObjectType.CONTENT, new DctmImportContent(this));
		m.put(DctmObjectType.DOCUMENT, new DctmImportDocument(this));
		m.put(DctmObjectType.FOLDER, new DctmImportFolder(this));
		m.put(DctmObjectType.FORMAT, new DctmImportFormat(this));
		m.put(DctmObjectType.GROUP, new DctmImportGroup(this));
		m.put(DctmObjectType.TYPE, new DctmImportType(this));
		m.put(DctmObjectType.USER, new DctmImportUser(this));
		this.delegates = Collections.unmodifiableMap(m);
	}

	private DctmImportAbstract<?> getImportDelegate(StoredObject<?> marshaled)
		throws UnsupportedDctmObjectTypeException {
		DctmObjectType type = DctmTranslator.translateType(marshaled.getType());
		DctmImportAbstract<?> delegate = this.delegates.get(type);
		if (delegate == null) { throw new IllegalStateException(String.format(
			"Failed to find a delegate for type [%s]", type.name())); }
		return delegate;
	}

	@Override
	protected ImportStrategy getImportStrategy(StoredObjectType type) {
		return DctmTranslator.translateType(type).importStrategy;
	}

	@Override
	protected ImportOutcome importObject(StoredObject<?> marshaled,
		ObjectStorageTranslator<IDfPersistentObject, IDfValue> translator, DctmImportContext ctx)
			throws ImportException, StorageException, StoredValueDecoderException {
		try {
			return getImportDelegate(marshaled).importObject(marshaled, translator, ctx);
		} catch (DfException | UnsupportedDctmObjectTypeException e) {
			throw new ImportException(String.format("Exception raised while marshaling %s [%s](%s)",
				marshaled.getType(), marshaled.getLabel(), marshaled.getId()), e);
		}
	}

	@Override
	protected DctmImportContext newContext(String rootId, StoredObjectType rootType, IDfSession session, Logger output,
		ObjectStore<?, ?> objectStore, ContentStore streamStore) {
		return new DctmImportContext(this, rootId, rootType, session, output, objectStore, streamStore);
	}

	@Override
	protected IDfValue getValue(StoredDataType type, Object value) {
		return DfValueFactory.newValue(type, value);
	}

	@Override
	protected ObjectStorageTranslator<IDfPersistentObject, IDfValue> getTranslator() {
		return DctmTranslator.INSTANCE;
	}

	@Override
	protected SessionFactory<IDfSession> newSessionFactory() {
		return new DctmSessionFactory();
	}

	@Override
	protected Set<String> getTargetNames() {
		return DctmImportEngine.TARGETS;
	}
}