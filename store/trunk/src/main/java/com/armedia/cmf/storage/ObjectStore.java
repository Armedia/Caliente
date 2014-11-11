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
public abstract class ObjectStore<C, O extends ObjectStoreOperation<C>> {

	private class Mapper extends StoredAttributeMapper {

		private final O operation;

		private Mapper() {
			this.operation = null;
		}

		private Mapper(O operation) {
			this.operation = operation;
		}

		private Mapping constructMapping(StoredObjectType type, String name, String source, String target) {
			return super.newMapping(type, name, source, target);
		}

		@Override
		protected Mapping createMapping(StoredObjectType type, String name, String source, String target) {
			O o = this.operation;
			boolean newOperation = false;
			if (o == null) {
				newOperation = true;
				try {
					o = newOperation();
				} catch (StorageException e) {
					throw new RuntimeException("Failed to initialize an operation to create the mapping");
				}
			}

			try {
				ObjectStore.this.createMapping(o, type, name, source, target);
			} catch (StorageException e) {
				throw new RuntimeException(String.format("Failed to create the mapping for [%s::%s(%s->%s)]", type,
					name, source, target), e);
			} finally {
				if (newOperation) {
					try {
						o.close();
					} catch (StorageException e) {
						throw new RuntimeException("Failed to complete the operation", e);
					}
				}
			}
			return constructMapping(type, name, source, target);
		}

		@Override
		public Mapping getTargetMapping(StoredObjectType type, String name, String source) {
			O o = this.operation;
			boolean newOperation = false;
			if (o == null) {
				newOperation = true;
				try {
					o = newOperation();
				} catch (StorageException e) {
					throw new RuntimeException("Failed to initialize an operation to create the mapping");
				}
			}
			try {
				return ObjectStore.this.getTargetMapping(o, type, name, source);
			} catch (StorageException e) {
				throw new RuntimeException(String.format("Failed to retrieve the target mapping for [%s::%s(%s->?)]",
					type, name, source), e);
			} finally {
				if (newOperation) {
					try {
						o.close();
					} catch (StorageException e) {
						throw new RuntimeException("Failed to complete the operation", e);
					}
				}
			}
		}

		@Override
		public Mapping getSourceMapping(StoredObjectType type, String name, String target) {
			O o = this.operation;
			boolean newOperation = false;
			if (o == null) {
				newOperation = true;
				try {
					o = newOperation();
				} catch (StorageException e) {
					throw new RuntimeException("Failed to initialize an operation to create the mapping");
				}
			}
			try {
				return ObjectStore.this.getSourceMapping(o, type, name, target);
			} catch (StorageException e) {
				throw new RuntimeException(String.format("Failed to create the source mapping for [%s::%s(?->%s)]",
					type, name, target), e);
			} finally {
				if (newOperation) {
					try {
						o.close();
					} catch (StorageException e) {
						throw new RuntimeException("Failed to complete the operation", e);
					}
				}
			}
		}

		@Override
		public Map<StoredObjectType, Set<String>> getAvailableMappings() {
			O o = this.operation;
			boolean newOperation = false;
			if (o == null) {
				newOperation = true;
				try {
					o = newOperation();
				} catch (StorageException e) {
					throw new RuntimeException("Failed to initialize an operation to create the mapping");
				}
			}
			try {
				return ObjectStore.this.getAvailableMappings(o);
			} catch (StorageException e) {
				throw new RuntimeException("Failed to retrieve the mapping names in the system", e);
			} finally {
				if (newOperation) {
					try {
						o.close();
					} catch (StorageException e) {
						throw new RuntimeException("Failed to complete the operation", e);
					}
				}
			}
		}

		@Override
		public Set<String> getAvailableMappings(StoredObjectType type) {
			O o = this.operation;
			boolean newOperation = false;
			if (o == null) {
				newOperation = true;
				try {
					o = newOperation();
				} catch (StorageException e) {
					throw new RuntimeException("Failed to initialize an operation to create the mapping");
				}
			}
			try {
				return ObjectStore.this.getAvailableMappings(o, type);
			} catch (StorageException e) {
				throw new RuntimeException(String.format("Failed to retrieve the mapping names in the system for [%s]",
					type), e);
			} finally {
				if (newOperation) {
					try {
						o.close();
					} catch (StorageException e) {
						throw new RuntimeException("Failed to complete the operation", e);
					}
				}
			}
		}

		@Override
		public Map<String, String> getMappings(StoredObjectType type, String name) {
			O o = this.operation;
			boolean newOperation = false;
			if (o == null) {
				newOperation = true;
				try {
					o = newOperation();
				} catch (StorageException e) {
					throw new RuntimeException("Failed to initialize an operation to create the mapping");
				}
			}
			try {
				return ObjectStore.this.getMappings(o, type, name);
			} catch (StorageException e) {
				throw new RuntimeException(String.format("Failed to retrieves the mappings in the system for [%s::%s]",
					type, name), e);
			} finally {
				if (newOperation) {
					try {
						o.close();
					} catch (StorageException e) {
						throw new RuntimeException("Failed to complete the operation", e);
					}
				}
			}
		}
	}

	protected final Logger log = LoggerFactory.getLogger(getClass());
	private final ReadWriteLock openLock = new ReentrantReadWriteLock();
	private final Mapper mapper = new Mapper();
	private final Class<O> operationClass;
	private boolean open = false;

	protected ObjectStore(Class<O> operationClass) throws StorageException {
		this(operationClass, false);
	}

	protected ObjectStore(Class<O> operationClass, boolean openState) throws StorageException {
		if (operationClass == null) { throw new IllegalArgumentException("Must provide the operation class"); }
		this.operationClass = operationClass;
		this.open = openState;
	}

	protected abstract O newOperation() throws StorageException;

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

	protected final O castOperation(ObjectStoreOperation<?> operation) {
		if (operation == null) { throw new IllegalArgumentException("Must provide a valid operation"); }
		return this.operationClass.cast(operation);
	}

	public final <T, V> Long storeObject(StoredObject<V> object, ObjectStorageTranslator<T, V> translator)
		throws StorageException, StoredValueEncoderException {
		O operation = newOperation();
		boolean ok = false;
		try {
			Long ret = storeObject(operation, object, translator);
			ok = true;
			return ret;
		} finally {
			operation.close(ok);
		}
	}

	public final <T, V> Long storeObject(ObjectStoreOperation<?> operation, StoredObject<V> object,
		ObjectStorageTranslator<T, V> translator) throws StorageException, StoredValueEncoderException {
		O o = castOperation(operation);
		if (object == null) { throw new IllegalArgumentException("Must provide an object to store"); }
		if (translator == null) { throw new IllegalArgumentException(
			"Must provide a translator for storing object values"); }
		getReadLock().lock();
		try {
			assertOpen();
			return doStoreObject(o, object, translator);
		} finally {
			getReadLock().unlock();
		}
	}

	protected abstract <T, V> Long doStoreObject(O operation, StoredObject<V> object,
		ObjectStorageTranslator<T, V> translator) throws StorageException, StoredValueEncoderException;

	public final boolean isStored(StoredObjectType type, String objectId) throws StorageException {
		assertOpen();
		O operation = newOperation();
		try {
			return isStored(operation, type, objectId);
		} finally {
			operation.close();
		}
	}

	public final boolean isStored(ObjectStoreOperation<?> operation, StoredObjectType type, String objectId)
		throws StorageException {
		if (type == null) { throw new IllegalArgumentException("Must provide an object type to check for"); }
		if (objectId == null) { throw new IllegalArgumentException("Must provide an object id to check for"); }
		O o = castOperation(operation);
		getReadLock().lock();
		try {
			assertOpen();
			return doIsStored(o, type, objectId);
		} finally {
			getReadLock().unlock();
		}
	}

	protected abstract boolean doIsStored(O operation, StoredObjectType type, String objectId) throws StorageException;

	public final boolean lockForStorage(StoredObjectType type, String objectId) throws StorageException {
		assertOpen();
		O operation = newOperation();
		boolean ok = false;
		try {
			boolean ret = lockForStorage(operation, type, objectId);
			ok = true;
			return ret;
		} finally {
			operation.close(ok);
		}
	}

	public final boolean lockForStorage(ObjectStoreOperation<?> operation, StoredObjectType type, String objectId)
		throws StorageException {
		if (type == null) { throw new IllegalArgumentException("Must provide an object type to check for"); }
		if (objectId == null) { throw new IllegalArgumentException("Must provide an object id to check for"); }
		O o = castOperation(operation);
		getReadLock().lock();
		try {
			assertOpen();
			return doLockForStorage(o, type, objectId);
		} finally {
			getReadLock().unlock();
		}
	}

	protected abstract boolean doLockForStorage(O operation, StoredObjectType type, String objectId)
		throws StorageException;

	public final <T, V> Collection<StoredObject<V>> loadObjects(ObjectStorageTranslator<T, V> translator,
		final StoredObjectType type, String... ids) throws StorageException, StoredValueDecoderException {
		assertOpen();
		O operation = newOperation();
		boolean ok = false;
		try {
			Collection<StoredObject<V>> ret = loadObjects(operation, translator, type, ids);
			ok = true;
			return ret;
		} finally {
			operation.close(ok);
		}
	}

	public final <T, V> Collection<StoredObject<V>> loadObjects(ObjectStoreOperation<?> operation,
		ObjectStorageTranslator<T, V> translator, final StoredObjectType type, String... ids) throws StorageException,
		StoredValueDecoderException {
		getReadLock().lock();
		try {
			assertOpen();
			return loadObjects(operation, translator, type, (ids != null ? Arrays.asList(ids) : null));
		} finally {
			getReadLock().unlock();
		}
	}

	public final <T, V> Collection<StoredObject<V>> loadObjects(ObjectStorageTranslator<T, V> translator,
		final StoredObjectType type, Collection<String> ids) throws StorageException, StoredValueDecoderException {
		assertOpen();
		O operation = newOperation();
		boolean ok = false;
		try {
			Collection<StoredObject<V>> ret = loadObjects(operation, translator, type, ids);
			ok = true;
			return ret;
		} finally {
			operation.close(ok);
		}
	}

	public final <T, V> Collection<StoredObject<V>> loadObjects(ObjectStoreOperation<?> operation,
		ObjectStorageTranslator<T, V> translator, final StoredObjectType type, Collection<String> ids)
			throws StorageException, StoredValueDecoderException {
		if (operation == null) { throw new IllegalArgumentException("Must proved an operation to work under"); }
		if (type == null) { throw new IllegalArgumentException("Must provide an object type to retrieve"); }
		if (translator == null) { throw new IllegalArgumentException(
			"Must provide a translator for storing object values"); }
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
			final StoredObjectHandler<V> handler = new StoredObjectHandler<V>() {
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
			};
			loadObjects(operation, translator, type, actualIds, handler);
			return ret;
		} finally {
			getReadLock().unlock();
		}
	}

	public final <T, V> int loadObjects(ObjectStorageTranslator<T, V> translator, final StoredObjectType type,
		StoredObjectHandler<V> handler) throws StorageException, StoredValueDecoderException {
		assertOpen();
		O operation = newOperation();
		boolean ok = false;
		try {
			int ret = loadObjects(operation, translator, type, handler);
			ok = true;
			return ret;
		} finally {
			operation.close(ok);
		}
	}

	public final <T, V> int loadObjects(ObjectStoreOperation<?> operation, ObjectStorageTranslator<T, V> translator,
		final StoredObjectType type, StoredObjectHandler<V> handler) throws StorageException,
		StoredValueDecoderException {
		return loadObjects(operation, translator, type, null, handler);
	}

	public final <T, V> int loadObjects(ObjectStorageTranslator<T, V> translator, final StoredObjectType type,
		Collection<String> ids, StoredObjectHandler<V> handler) throws StorageException, StoredValueDecoderException {
		assertOpen();
		O operation = newOperation();
		boolean ok = false;
		try {
			int ret = loadObjects(operation, translator, type, ids, handler);
			ok = true;
			return ret;
		} finally {
			operation.close(ok);
		}
	}

	public final <T, V> int loadObjects(ObjectStoreOperation<?> operation, ObjectStorageTranslator<T, V> translator,
		final StoredObjectType type, Collection<String> ids, StoredObjectHandler<V> handler) throws StorageException,
		StoredValueDecoderException {
		O o = castOperation(operation);
		if (type == null) { throw new IllegalArgumentException("Must provide an object type to load"); }
		if (handler == null) { throw new IllegalArgumentException(
			"Must provide an object handler to handle the deserialized objects"); }
		getReadLock().lock();
		try {
			assertOpen();
			return doLoadObjects(o, translator, type, ids, handler);
		} finally {
			getReadLock().unlock();
		}
	}

	protected abstract <T, V> int doLoadObjects(O operation, ObjectStorageTranslator<T, V> translator,
		StoredObjectType type, Collection<String> ids, StoredObjectHandler<V> handler) throws StorageException,
		StoredValueDecoderException;

	private void createMapping(ObjectStoreOperation<?> operation, StoredObjectType type, String name, String source,
		String target) throws StorageException {
		if (type == null) { throw new IllegalArgumentException("Must provide an object type to map for"); }
		if (name == null) { throw new IllegalArgumentException("Must provide a mapping name to map for"); }
		if (source == null) { throw new IllegalArgumentException(
			"Must provide a source value to link to the target value"); }
		if (target == null) { throw new IllegalArgumentException(
			"Must provide a target value to link to the source value"); }
		O o = castOperation(operation);
		getReadLock().lock();
		try {
			assertOpen();
			doCreateMappedValue(o, type, name, source, target);
		} finally {
			getReadLock().unlock();
		}
	}

	protected abstract void doCreateMappedValue(O operation, StoredObjectType type, String name, String source,
		String target) throws StorageException;

	protected final String getMappedValue(ObjectStoreOperation<?> operation, boolean source, StoredObjectType type,
		String name, String value) throws StorageException {
		O o = castOperation(operation);
		getReadLock().lock();
		try {
			assertOpen();
			return doGetMappedValue(o, source, type, name, value);
		} finally {
			getReadLock().unlock();
		}
	}

	protected abstract String doGetMappedValue(O operation, boolean source, StoredObjectType type, String name,
		String value) throws StorageException;

	public final Mapping getTargetMapping(StoredObjectType type, String name, String target) throws StorageException {
		assertOpen();
		O operation = newOperation();
		try {
			return getTargetMapping(operation, type, name, target);
		} finally {
			operation.close();
		}
	}

	public final Mapping getTargetMapping(ObjectStoreOperation<?> operation, StoredObjectType type, String name,
		String source) throws StorageException {
		if (type == null) { throw new IllegalArgumentException("Must provide an object type to search against"); }
		if (name == null) { throw new IllegalArgumentException("Must provide a mapping name to search for"); }
		if (source == null) { throw new IllegalArgumentException(
			"Must provide a source value to find the target mapping for"); }
		O o = castOperation(operation);
		getReadLock().lock();
		try {
			assertOpen();
			String target = getMappedValue(o, true, type, name, source);
			if (target == null) { return null; }
			return this.mapper.constructMapping(type, name, source, target);
		} finally {
			getReadLock().unlock();
		}
	}

	public final Mapping getSourceMapping(StoredObjectType type, String name, String target) throws StorageException {
		assertOpen();
		O operation = newOperation();
		try {
			return getSourceMapping(operation, type, name, target);
		} finally {
			operation.close();
		}
	}

	public final Mapping getSourceMapping(ObjectStoreOperation<?> operation, StoredObjectType type, String name,
		String target) throws StorageException {
		if (type == null) { throw new IllegalArgumentException("Must provide an object type to search against"); }
		if (name == null) { throw new IllegalArgumentException("Must provide a mapping name to search for"); }
		if (target == null) { throw new IllegalArgumentException(
			"Must provide a target value to find the source mapping for"); }
		O o = castOperation(operation);
		getReadLock().lock();
		try {
			assertOpen();
			String source = getMappedValue(o, false, type, name, target);
			if (source == null) { return null; }
			return this.mapper.constructMapping(type, name, source, target);
		} finally {
			getReadLock().unlock();
		}
	}

	public final Map<StoredObjectType, Integer> getStoredObjectTypes() throws StorageException {
		assertOpen();
		O operation = newOperation();
		try {
			return getStoredObjectTypes(operation);
		} finally {
			operation.close();
		}
	}

	public final Map<StoredObjectType, Integer> getStoredObjectTypes(ObjectStoreOperation<?> operation)
		throws StorageException {
		getReadLock().lock();
		try {
			assertOpen();
			return doGetStoredObjectTypes(castOperation(operation));
		} finally {
			getReadLock().unlock();
		}

	}

	protected abstract Map<StoredObjectType, Integer> doGetStoredObjectTypes(O operation) throws StorageException;

	public final StoredAttributeMapper getAttributeMapper() {
		return this.mapper;
	}

	public final StoredAttributeMapper getAttributeMapper(ObjectStoreOperation<?> operation) {
		return new Mapper(castOperation(operation));
	}

	public final int clearAttributeMappings() throws StorageException {
		assertOpen();
		O operation = newOperation();
		try {
			return clearAttributeMappings(operation);
		} finally {
			operation.close();
		}
	}

	public final int clearAttributeMappings(ObjectStoreOperation<?> operation) throws StorageException {
		O o = castOperation(operation);
		getReadLock().lock();
		try {
			assertOpen();
			return doClearAttributeMappings(o);
		} finally {
			getReadLock().unlock();
		}
	}

	protected abstract int doClearAttributeMappings(O operation) throws StorageException;

	public final Map<StoredObjectType, Set<String>> getAvailableMappings() throws StorageException {
		assertOpen();
		O operation = newOperation();
		try {
			return getAvailableMappings(operation);
		} finally {
			operation.close();
		}
	}

	public final Map<StoredObjectType, Set<String>> getAvailableMappings(ObjectStoreOperation<?> operation)
		throws StorageException {
		O o = castOperation(operation);
		getReadLock().lock();
		try {
			assertOpen();
			return doGetAvailableMappings(o);
		} finally {
			getReadLock().unlock();
		}
	}

	protected abstract Map<StoredObjectType, Set<String>> doGetAvailableMappings(O operation) throws StorageException;

	public final Set<String> getAvailableMappings(StoredObjectType type) throws StorageException {
		assertOpen();
		O operation = newOperation();
		try {
			return getAvailableMappings(operation, type);
		} finally {
			operation.close();
		}
	}

	public final Set<String> getAvailableMappings(ObjectStoreOperation<?> operation, StoredObjectType type)
		throws StorageException {
		if (type == null) { throw new IllegalArgumentException("Must provide an object type to search against"); }
		O o = castOperation(operation);
		getReadLock().lock();
		try {
			assertOpen();
			return doGetAvailableMappings(o, type);
		} finally {
			getReadLock().unlock();
		}
	}

	protected abstract Set<String> doGetAvailableMappings(O operation, StoredObjectType type) throws StorageException;

	public final Map<String, String> getMappings(StoredObjectType type, String name) throws StorageException {
		assertOpen();
		O operation = newOperation();
		try {
			return getMappings(operation, type, name);
		} finally {
			operation.close();
		}
	}

	public final Map<String, String> getMappings(ObjectStoreOperation<?> operation, StoredObjectType type, String name)
		throws StorageException {
		if (type == null) { throw new IllegalArgumentException("Must provide an object type to search against"); }
		if (name == null) { throw new IllegalArgumentException("Must provide a mapping name to search for"); }
		O o = castOperation(operation);
		getReadLock().lock();
		try {
			assertOpen();
			return doGetMappings(o, type, name);
		} finally {
			getReadLock().unlock();
		}
	}

	protected abstract Map<String, String> doGetMappings(O operation, StoredObjectType type, String name)
		throws StorageException;

	public final void clearAllObjects() throws StorageException {
		assertOpen();
		O operation = newOperation();
		try {
			clearAllObjects(operation);
		} finally {
			operation.commit();
		}
	}

	public final void clearAllObjects(ObjectStoreOperation<?> operation) throws StorageException {
		O o = castOperation(operation);
		getReadLock().lock();
		try {
			assertOpen();
			doClearAllObjects(o);
		} finally {
			getReadLock().unlock();
		}
	}

	protected abstract void doClearAllObjects(O operation) throws StorageException;

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