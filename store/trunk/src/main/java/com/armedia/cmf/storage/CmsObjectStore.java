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

import com.armedia.cmf.storage.CmsAttributeMapper.Mapping;

/**
 * @author Diego Rivera &lt;diego.rivera@armedia.com&gt;
 *
 */
public abstract class CmsObjectStore {

	public static interface StoredObjectHandler {

		/**
		 * <p>
		 * Signal the beginning of a new batch, with the given ID. Returns {@code true} if the batch
		 * should be processed, {@code false} if it should be skipped. If the batch is skipped,
		 * Neither {@link #closeBatch(boolean)} nor {@link #handleObject(CmsStoredObject)} will be
		 * invoked.
		 * </p>
		 *
		 * @param batchId
		 * @return {@code true} if the batch should be processed, {@code false} if it should be
		 *         skipped
		 * @throws CmsStorageException
		 */
		public boolean newBatch(String batchId) throws CmsStorageException;

		/**
		 * <p>
		 * Handle the given object instance in the context of the currently-open batch. This method
		 * should return {@code true} if the loop is to be continued, or {@code false} if no further
		 * attempt should be made to obtain objects.
		 * </p>
		 *
		 * @param dataObject
		 * @throws CmsStorageException
		 * @return {@code true} if more objects should be loaded, or {@code false} if this should be
		 *         the last object load attempted.
		 */
		public boolean handleObject(CmsStoredObject dataObject) throws CmsStorageException;

		/**
		 * <p>
		 * Indicate that the load attempt failed for the object with the given ID, and provides the
		 * exception that describes the failure. It should return {@code true} if the code is
		 * expected to continue attempting to load objects, or {@code false} if the load attempt
		 * should be aborted.
		 * </p>
		 *
		 * @param e
		 * @return {@code true} if the load process should continue, {@code false} if it should be
		 *         aborted.
		 */
		public boolean handleException(SQLException e);

		/**
		 * <p>
		 * Close the current batch, returning {@code true} if processing should continue with the
		 * next batch, or {@code false} otherwise.
		 * </p>
		 *
		 * @param ok
		 *            {@code true} if processing should continue with the next batch, or
		 *            {@code false} otherwise
		 * @return {@code true} if processing should continue with the next batch, or {@code false}
		 *         otherwise
		 * @throws CmsStorageException
		 */
		public boolean closeBatch(boolean ok) throws CmsStorageException;
	}

	private class Mapper extends CmsAttributeMapper {

		private Mapping constructMapping(CmsObjectType type, String name, String source, String target) {
			return super.newMapping(type, name, source, target);
		}

		@Override
		protected Mapping createMapping(CmsObjectType type, String name, String source, String target) {
			try {
				CmsObjectStore.this.createMapping(type, name, source, target);
			} catch (CmsStorageException e) {
				throw new RuntimeException(String.format("Failed to create the mapping for [%s::%s(%s->%s)]", type,
					name, source, target), e);
			}
			return constructMapping(type, name, source, target);
		}

		@Override
		public Mapping getTargetMapping(CmsObjectType type, String name, String source) {
			try {
				return CmsObjectStore.this.getTargetMapping(type, name, source);
			} catch (CmsStorageException e) {
				throw new RuntimeException(String.format("Failed to retrieve the target mapping for [%s::%s(%s->?)]",
					type, name, source), e);
			}
		}

		@Override
		public Mapping getSourceMapping(CmsObjectType type, String name, String target) {
			try {
				return CmsObjectStore.this.getSourceMapping(type, name, target);
			} catch (CmsStorageException e) {
				throw new RuntimeException(String.format("Failed to create the source mapping for [%s::%s(?->%s)]",
					type, name, target), e);
			}
		}

		@Override
		public Map<CmsObjectType, Set<String>> getAvailableMappings() {
			try {
				return CmsObjectStore.this.getAvailableMappings();
			} catch (CmsStorageException e) {
				throw new RuntimeException("Failed to retrieve the mapping names in the system", e);
			}
		}

		@Override
		public Set<String> getAvailableMappings(CmsObjectType type) {
			try {
				return CmsObjectStore.this.getAvailableMappings(type);
			} catch (CmsStorageException e) {
				throw new RuntimeException(String.format("Failed to retrieve the mapping names in the system for [%s]",
					type), e);
			}
		}

		@Override
		public Map<String, String> getMappings(CmsObjectType type, String name) {
			try {
				return CmsObjectStore.this.getMappings(type, name);
			} catch (CmsStorageException e) {
				throw new RuntimeException(String.format("Failed to retrieves the mappings in the system for [%s::%s]",
					type, name), e);
			}
		}
	}

	protected final Logger log = LoggerFactory.getLogger(getClass());
	private final Mapper mapper = new Mapper();
	private final ReadWriteLock openLock = new ReentrantReadWriteLock();
	private boolean open = false;

	protected CmsObjectStore() throws CmsStorageException {
		this(false);
	}

	protected CmsObjectStore(boolean openState) throws CmsStorageException {
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

	protected final void assertOpen() throws CmsStorageException {
		if (!isOpen()) { throw new CmsStorageException("This objectstore is not open"); }
	}

	public final boolean init(Map<String, String> settings) throws CmsStorageException {
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

	protected void doInit(Map<String, String> settings) throws CmsStorageException {
	}

	public final Long storeObject(CmsStoredObject object) throws CmsStorageException {
		getReadLock().lock();
		try {
			assertOpen();
			return doStoreObject(object);
		} finally {
			getReadLock().unlock();
		}
	}

	protected abstract Long doStoreObject(CmsStoredObject object) throws CmsStorageException;

	public final Collection<CmsStoredObject> loadObjects(final CmsObjectType type, String... ids) throws CmsStorageException {
		getReadLock().lock();
		try {
			assertOpen();
			return loadObjects(type, (ids != null ? Arrays.asList(ids) : null));
		} finally {
			getReadLock().unlock();
		}
	}

	public final Collection<CmsStoredObject> loadObjects(final CmsObjectType type, Collection<String> ids)
		throws CmsStorageException {
		getReadLock().lock();
		try {
			assertOpen();
			final List<CmsStoredObject> ret = new ArrayList<CmsStoredObject>(ids.size());
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
			loadObjects(type, actualIds, new StoredObjectHandler() {
				@Override
				public boolean newBatch(String batchId) throws CmsStorageException {
					return true;
				}

				@Override
				public boolean handleObject(CmsStoredObject dataObject) throws CmsStorageException {
					ret.add(dataObject);
					return true;
				}

				@Override
				public boolean handleException(SQLException e) {
					return false;
				}

				@Override
				public boolean closeBatch(boolean ok) throws CmsStorageException {
					return true;
				}
			});
			return ret;
		} finally {
			getReadLock().unlock();
		}
	}

	public final int loadObjects(final CmsObjectType type, StoredObjectHandler handler) throws CmsStorageException {
		return loadObjects(type, null, handler);
	}

	public final int loadObjects(final CmsObjectType type, Collection<String> ids, StoredObjectHandler handler)
		throws CmsStorageException {
		if (type == null) { throw new IllegalArgumentException("Must provide an object type to load"); }
		if (handler == null) { throw new IllegalArgumentException(
			"Must provide an object handler to handle the deserialized objects"); }
		getReadLock().lock();
		try {
			assertOpen();
			return doLoadObjects(type, ids, handler);
		} finally {
			getReadLock().unlock();
		}
	}

	protected abstract int doLoadObjects(final CmsObjectType type, Collection<String> ids, StoredObjectHandler handler)
		throws CmsStorageException;

	public final boolean isStored(CmsObjectType type, String objectId) throws CmsStorageException {
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

	protected abstract boolean doIsStored(CmsObjectType type, String objectId) throws CmsStorageException;

	private void createMapping(CmsObjectType type, String name, String source, String target)
		throws CmsStorageException {
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

	protected abstract void doCreateMappedValue(CmsObjectType type, String name, String source, String target)
		throws CmsStorageException;

	protected final String getMappedValue(boolean source, CmsObjectType type, String name, String value)
		throws CmsStorageException {
		getReadLock().lock();
		try {
			assertOpen();
			return doGetMappedValue(source, type, name, value);
		} finally {
			getReadLock().unlock();
		}
	}

	protected abstract String doGetMappedValue(boolean source, CmsObjectType type, String name, String value)
		throws CmsStorageException;

	private final Mapping getTargetMapping(CmsObjectType type, String name, String source) throws CmsStorageException {
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

	private final Mapping getSourceMapping(CmsObjectType type, String name, String target) throws CmsStorageException {
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

	public final Map<CmsObjectType, Integer> getStoredObjectTypes() throws CmsStorageException {
		getReadLock().lock();
		try {
			assertOpen();
			return doGetStoredObjectTypes();
		} finally {
			getReadLock().unlock();
		}
	}

	protected abstract Map<CmsObjectType, Integer> doGetStoredObjectTypes() throws CmsStorageException;

	public final CmsAttributeMapper getAttributeMapper() {
		return this.mapper;
	}

	public final int clearAttributeMappings() throws CmsStorageException {
		getReadLock().lock();
		try {
			assertOpen();
			return doClearAttributeMappings();
		} finally {
			getReadLock().unlock();
		}
	}

	protected abstract int doClearAttributeMappings() throws CmsStorageException;

	public final Map<CmsObjectType, Set<String>> getAvailableMappings() throws CmsStorageException {
		getReadLock().lock();
		try {
			assertOpen();
			return doGetAvailableMappings();
		} finally {
			getReadLock().unlock();
		}
	}

	protected abstract Map<CmsObjectType, Set<String>> doGetAvailableMappings() throws CmsStorageException;

	public final Set<String> getAvailableMappings(CmsObjectType type) throws CmsStorageException {
		if (type == null) { throw new IllegalArgumentException("Must provide an object type to search against"); }
		getReadLock().lock();
		try {
			assertOpen();
			return doGetAvailableMappings(type);
		} finally {
			getReadLock().unlock();
		}
	}

	protected abstract Set<String> doGetAvailableMappings(CmsObjectType type) throws CmsStorageException;

	public final Map<String, String> getMappings(CmsObjectType type, String name) throws CmsStorageException {
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

	protected abstract Map<String, String> doGetMappings(CmsObjectType type, String name) throws CmsStorageException;

	public final void clearAllObjects() throws CmsStorageException {
		getReadLock().lock();
		try {
			assertOpen();
			doClearAllObjects();
		} finally {
			getReadLock().unlock();
		}
	}

	protected abstract void doClearAllObjects() throws CmsStorageException;

	public final boolean close() throws CmsStorageException {
		getWriteLock().lock();
		try {
			if (!this.open) { return false; }
			return doClose();
		} finally {
			this.open = false;
			getWriteLock().unlock();
		}
	}

	protected boolean doClose() throws CmsStorageException {
		return this.open;
	}
}