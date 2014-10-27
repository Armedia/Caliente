/**
 *
 */

package com.armedia.cmf.storage;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.cmf.storage.StoredAttributeMapper.Mapping;

/**
 * @author Diego Rivera &lt;diego.rivera@armedia.com&gt;
 *
 */
public abstract class ObjectStore {

	private class Mapper extends StoredAttributeMapper {

		private Mapping constructMapping(StoredObjectType type, String name, String source, String target) {
			return super.newMapping(type, name, source, target);
		}

		@Override
		protected Mapping createMapping(StoredObjectType type, String name, String source, String target) {
			try {
				ObjectStore.this.createMapping(type, name, source, target);
			} catch (StorageException e) {
				throw new RuntimeException(String.format("Failed to create the mapping for [%s::%s(%s->%s)]", type,
					name, source, target), e);
			}
			return constructMapping(type, name, source, target);
		}

		@Override
		public Mapping getTargetMapping(StoredObjectType type, String name, String source) {
			try {
				return ObjectStore.this.getTargetMapping(type, name, source);
			} catch (StorageException e) {
				throw new RuntimeException(String.format("Failed to retrieve the target mapping for [%s::%s(%s->?)]",
					type, name, source), e);
			}
		}

		@Override
		public Mapping getSourceMapping(StoredObjectType type, String name, String target) {
			try {
				return ObjectStore.this.getSourceMapping(type, name, target);
			} catch (StorageException e) {
				throw new RuntimeException(String.format("Failed to create the source mapping for [%s::%s(?->%s)]",
					type, name, target), e);
			}
		}

		@Override
		public Map<StoredObjectType, Set<String>> getAvailableMappings() {
			try {
				return ObjectStore.this.getAvailableMappings();
			} catch (StorageException e) {
				throw new RuntimeException("Failed to retrieve the mapping names in the system", e);
			}
		}

		@Override
		public Set<String> getAvailableMappings(StoredObjectType type) {
			try {
				return ObjectStore.this.getAvailableMappings(type);
			} catch (StorageException e) {
				throw new RuntimeException(String.format("Failed to retrieve the mapping names in the system for [%s]",
					type), e);
			}
		}

		@Override
		public Map<String, String> getMappings(StoredObjectType type, String name) {
			try {
				return ObjectStore.this.getMappings(type, name);
			} catch (StorageException e) {
				throw new RuntimeException(String.format("Failed to retrieves the mappings in the system for [%s::%s]",
					type, name), e);
			}
		}
	}

	protected final Logger log = LoggerFactory.getLogger(getClass());
	private final Mapper mapper = new Mapper();
	private final ReadWriteLock openLock = new ReentrantReadWriteLock();
	private boolean open = false;

	protected ObjectStore() throws StorageException {
		this(false);
	}

	protected ObjectStore(boolean openState) throws StorageException {
		this.open = openState;
	}

	protected final Lock getReadLock() {
		return this.openLock.readLock();
	}

	protected final Lock getWriteLock() {
		return this.openLock.writeLock();
	}

	public final boolean isOpen() {
		getReadLock().lock();
		try {
			return this.open;
		} finally {
			getReadLock().unlock();
		}
	}

	protected final void assertOpen() throws StorageException {
		if (!isOpen()) { throw new StorageException("This objectstore is not open"); }
	}

	public final boolean init(Map<String, String> settings) throws StorageException {
		getWriteLock().lock();
		try {
			// Do nothing - this is for subclasses to override
			if (this.open) { return false; }
			doInit(settings);
			this.open = true;
			return this.open;
		} finally {
			getWriteLock().unlock();
		}
	}

	protected void doInit(Map<String, String> settings) throws StorageException {
	}

	public final <V> Long storeObject(StoredObject<V> object, ObjectStorageTranslator<V> translator)
		throws StorageException, StoredValueEncoderException {
		getReadLock().lock();
		try {
			assertOpen();
			return doStoreObject(object, translator);
		} finally {
			getReadLock().unlock();
		}
	}

	protected abstract <V> Long doStoreObject(StoredObject<V> object, ObjectStorageTranslator<V> translator)
		throws StorageException, StoredValueEncoderException;

	public final <V> Collection<StoredObject<V>> loadObjects(ObjectStorageTranslator<V> translator,
		final StoredObjectType type, String... ids) throws StorageException, StoredValueDecoderException {
		getReadLock().lock();
		try {
			assertOpen();
			return loadObjects(translator, type, (ids != null ? Arrays.asList(ids) : null));
		} finally {
			getReadLock().unlock();
		}
	}

	public final <V> Collection<StoredObject<V>> loadObjects(ObjectStorageTranslator<V> translator,
		final StoredObjectType type, Collection<String> ids) throws StorageException, StoredValueDecoderException {
		getReadLock().lock();
		try {
			assertOpen();
			final List<StoredObject<V>> ret = new ArrayList<StoredObject<V>>(ids.size());
			Set<String> actualIds = null;
			if (ids != null) {
				if (ids.isEmpty()) { return ret; }
				actualIds = new HashSet<String>();
				for (String s : ids) {
					if (s == null) {
						continue;
					}
					actualIds.add(s);
				}
			}
			loadObjects(translator, type, actualIds, new StoredObjectHandler<V>() {
				@Override
				public boolean newBatch(String batchId) throws StorageException {
					return true;
				}

				@Override
				public boolean handleObject(StoredObject<V> dataObject) throws StorageException {
					ret.add(dataObject);
					return true;
				}

				@Override
				public boolean handleException(SQLException e) {
					return false;
				}

				@Override
				public boolean closeBatch(boolean ok) throws StorageException {
					return true;
				}
			});
			return ret;
		} finally {
			getReadLock().unlock();
		}
	}

	public final <V> int loadObjects(ObjectStorageTranslator<V> translator, final StoredObjectType type,
		StoredObjectHandler<V> handler) throws StorageException, StoredValueDecoderException {
		return loadObjects(translator, type, null, handler);
	}

	public final <V> int loadObjects(ObjectStorageTranslator<V> translator, final StoredObjectType type,
		Collection<String> ids, StoredObjectHandler<V> handler) throws StorageException, StoredValueDecoderException {
		if (type == null) { throw new IllegalArgumentException("Must provide an object type to load"); }
		if (handler == null) { throw new IllegalArgumentException(
			"Must provide an object handler to handle the deserialized objects"); }
		getReadLock().lock();
		try {
			assertOpen();
			return doLoadObjects(translator, type, ids, handler);
		} finally {
			getReadLock().unlock();
		}
	}

	protected abstract <V> int doLoadObjects(ObjectStorageTranslator<V> translator, StoredObjectType type,
		Collection<String> ids, StoredObjectHandler<V> handler) throws StorageException, StoredValueDecoderException;

	public final boolean isStored(StoredObjectType type, String objectId) throws StorageException {
		if (type == null) { throw new IllegalArgumentException("Must provide an object type to check for"); }
		if (objectId == null) { throw new IllegalArgumentException("Must provide an object id to check for"); }
		getReadLock().lock();
		try {
			assertOpen();
			return doIsStored(type, objectId);
		} finally {
			getReadLock().unlock();
		}
	}

	protected abstract boolean doIsStored(StoredObjectType type, String objectId) throws StorageException;

	private void createMapping(StoredObjectType type, String name, String source, String target)
		throws StorageException {
		if (type == null) { throw new IllegalArgumentException("Must provide an object type to map for"); }
		if (name == null) { throw new IllegalArgumentException("Must provide a mapping name to map for"); }
		if (source == null) { throw new IllegalArgumentException(
			"Must provide a source value to link to the target value"); }
		if (target == null) { throw new IllegalArgumentException(
			"Must provide a target value to link to the source value"); }
		getReadLock().lock();
		try {
			assertOpen();
			doCreateMappedValue(type, name, source, target);
		} finally {
			getReadLock().unlock();
		}
	}

	protected abstract void doCreateMappedValue(StoredObjectType type, String name, String source, String target)
		throws StorageException;

	protected final String getMappedValue(boolean source, StoredObjectType type, String name, String value)
		throws StorageException {
		getReadLock().lock();
		try {
			assertOpen();
			return doGetMappedValue(source, type, name, value);
		} finally {
			getReadLock().unlock();
		}
	}

	protected abstract String doGetMappedValue(boolean source, StoredObjectType type, String name, String value)
		throws StorageException;

	private final Mapping getTargetMapping(StoredObjectType type, String name, String source) throws StorageException {
		if (type == null) { throw new IllegalArgumentException("Must provide an object type to search against"); }
		if (name == null) { throw new IllegalArgumentException("Must provide a mapping name to search for"); }
		if (source == null) { throw new IllegalArgumentException(
			"Must provide a source value to find the target mapping for"); }
		getReadLock().lock();
		try {
			assertOpen();
			String target = getMappedValue(true, type, name, source);
			if (target == null) { return null; }
			return this.mapper.constructMapping(type, name, source, target);
		} finally {
			getReadLock().unlock();
		}
	}

	private final Mapping getSourceMapping(StoredObjectType type, String name, String target) throws StorageException {
		if (type == null) { throw new IllegalArgumentException("Must provide an object type to search against"); }
		if (name == null) { throw new IllegalArgumentException("Must provide a mapping name to search for"); }
		if (target == null) { throw new IllegalArgumentException(
			"Must provide a target value to find the source mapping for"); }
		getReadLock().lock();
		try {
			assertOpen();
			String source = getMappedValue(false, type, name, target);
			if (source == null) { return null; }
			return this.mapper.constructMapping(type, name, source, target);
		} finally {
			getReadLock().unlock();
		}
	}

	public final Map<StoredObjectType, Integer> getStoredObjectTypes() throws StorageException {
		getReadLock().lock();
		try {
			assertOpen();
			return doGetStoredObjectTypes();
		} finally {
			getReadLock().unlock();
		}
	}

	protected abstract Map<StoredObjectType, Integer> doGetStoredObjectTypes() throws StorageException;

	public final StoredAttributeMapper getAttributeMapper() {
		return this.mapper;
	}

	public final int clearAttributeMappings() throws StorageException {
		getReadLock().lock();
		try {
			assertOpen();
			return doClearAttributeMappings();
		} finally {
			getReadLock().unlock();
		}
	}

	protected abstract int doClearAttributeMappings() throws StorageException;

	public final Map<StoredObjectType, Set<String>> getAvailableMappings() throws StorageException {
		getReadLock().lock();
		try {
			assertOpen();
			return doGetAvailableMappings();
		} finally {
			getReadLock().unlock();
		}
	}

	protected abstract Map<StoredObjectType, Set<String>> doGetAvailableMappings() throws StorageException;

	public final Set<String> getAvailableMappings(StoredObjectType type) throws StorageException {
		if (type == null) { throw new IllegalArgumentException("Must provide an object type to search against"); }
		getReadLock().lock();
		try {
			assertOpen();
			return doGetAvailableMappings(type);
		} finally {
			getReadLock().unlock();
		}
	}

	protected abstract Set<String> doGetAvailableMappings(StoredObjectType type) throws StorageException;

	public final Map<String, String> getMappings(StoredObjectType type, String name) throws StorageException {
		if (type == null) { throw new IllegalArgumentException("Must provide an object type to search against"); }
		if (name == null) { throw new IllegalArgumentException("Must provide a mapping name to search for"); }
		getReadLock().lock();
		try {
			assertOpen();
			return doGetMappings(type, name);
		} finally {
			getReadLock().unlock();
		}
	}

	protected abstract Map<String, String> doGetMappings(StoredObjectType type, String name) throws StorageException;

	public final void clearAllObjects() throws StorageException {
		getReadLock().lock();
		try {
			assertOpen();
			doClearAllObjects();
		} finally {
			getReadLock().unlock();
		}
	}

	protected abstract void doClearAllObjects() throws StorageException;

	public final boolean close() throws StorageException {
		getWriteLock().lock();
		try {
			if (!this.open) { return false; }
			return doClose();
		} finally {
			this.open = false;
			getWriteLock().unlock();
		}
	}

	protected boolean doClose() throws StorageException {
		return this.open;
	}
}