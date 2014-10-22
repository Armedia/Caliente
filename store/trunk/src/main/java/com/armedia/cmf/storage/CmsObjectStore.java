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

import com.armedia.cmf.storage.CmsAttributeMapper.Mapping;

/**
 * @author Diego Rivera &lt;diego.rivera@armedia.com&gt;
 *
 */
public abstract class CmsObjectStore {

	public static interface ObjectHandler {

		/**
		 * <p>
		 * Signal the beginning of a new batch, with the given ID. Returns {@code true} if the batch
		 * should be processed, {@code false} if it should be skipped. If the batch is skipped,
		 * Neither {@link #closeBatch(boolean)} nor {@link #handleObject(CmsObject)} will be
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
		public boolean handleObject(CmsObject dataObject) throws CmsStorageException;

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

	private final Mapper mapper = new Mapper();
	private final boolean writeMode;

	protected CmsObjectStore(boolean writeMode) throws CmsStorageException {
		this.writeMode = writeMode;
	}

	public final boolean isWriteMode() {
		return this.writeMode;
	}

	protected void init(Map<String, String> settings) throws CmsStorageException {
		// Do nothing - this is for subclasses to override
	}

	public abstract Long storeObject(CmsObject object) throws CmsStorageException;

	public final Collection<CmsObject> loadObjects(final CmsObjectType type, String... ids) throws CmsStorageException {
		return loadObjects(type, (ids != null ? Arrays.asList(ids) : null));
	}

	public final Collection<CmsObject> loadObjects(final CmsObjectType type, Collection<String> ids)
		throws CmsStorageException {
		final List<CmsObject> ret = new ArrayList<CmsObject>(ids.size());
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
		loadObjects(type, actualIds, new ObjectHandler() {
			@Override
			public boolean newBatch(String batchId) throws CmsStorageException {
				return true;
			}

			@Override
			public boolean handleObject(CmsObject dataObject) throws CmsStorageException {
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
	}

	public final int loadObjects(final CmsObjectType type, ObjectHandler handler) throws CmsStorageException {
		return loadObjects(type, null, handler);
	}

	public final int loadObjects(final CmsObjectType type, Collection<String> ids, ObjectHandler handler)
		throws CmsStorageException {
		if (type == null) { throw new IllegalArgumentException("Must provide an object type to load"); }
		if (handler == null) { throw new IllegalArgumentException(
			"Must provide an object handler to handle the deserialized objects"); }
		return doLoadObjects(type, ids, handler);
	}

	protected abstract int doLoadObjects(final CmsObjectType type, Collection<String> ids, ObjectHandler handler)
		throws CmsStorageException;

	public final boolean isStored(CmsObjectType type, String objectId) throws CmsStorageException {
		if (type == null) { throw new IllegalArgumentException("Must provide an object type to check for"); }
		if (objectId == null) { throw new IllegalArgumentException("Must provide an object id to check for"); }
		return doIsStored(type, objectId);
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
		doCreateMappedValue(type, name, source, target);
	}

	protected abstract void doCreateMappedValue(CmsObjectType type, String name, String source, String target)
		throws CmsStorageException;

	protected abstract String getMappedValue(boolean source, CmsObjectType type, String name, String value)
		throws CmsStorageException;

	private final Mapping getTargetMapping(CmsObjectType type, String name, String source) throws CmsStorageException {
		if (type == null) { throw new IllegalArgumentException("Must provide an object type to search against"); }
		if (name == null) { throw new IllegalArgumentException("Must provide a mapping name to search for"); }
		if (source == null) { throw new IllegalArgumentException(
			"Must provide a source value to find the target mapping for"); }
		String target = getMappedValue(true, type, name, source);
		if (target == null) { return null; }
		return this.mapper.constructMapping(type, name, source, target);
	}

	private final Mapping getSourceMapping(CmsObjectType type, String name, String target) throws CmsStorageException {
		if (type == null) { throw new IllegalArgumentException("Must provide an object type to search against"); }
		if (name == null) { throw new IllegalArgumentException("Must provide a mapping name to search for"); }
		if (target == null) { throw new IllegalArgumentException(
			"Must provide a target value to find the source mapping for"); }
		String source = getMappedValue(false, type, name, target);
		if (source == null) { return null; }
		return this.mapper.constructMapping(type, name, source, target);
	}

	public abstract Map<CmsObjectType, Integer> getStoredObjectTypes() throws CmsStorageException;

	public final CmsAttributeMapper getAttributeMapper() {
		return this.mapper;
	}

	public abstract int clearAttributeMappings() throws CmsStorageException;

	public abstract Map<CmsObjectType, Set<String>> getAvailableMappings() throws CmsStorageException;

	public final Set<String> getAvailableMappings(CmsObjectType type) throws CmsStorageException {
		if (type == null) { throw new IllegalArgumentException("Must provide an object type to search against"); }
		return doGetAvailableMappings(type);
	}

	protected abstract Set<String> doGetAvailableMappings(CmsObjectType type) throws CmsStorageException;

	public final Map<String, String> getMappings(CmsObjectType type, String name) throws CmsStorageException {
		if (type == null) { throw new IllegalArgumentException("Must provide an object type to search against"); }
		if (name == null) { throw new IllegalArgumentException("Must provide a mapping name to search for"); }
		return doGetMappings(type, name);
	}

	protected abstract Map<String, String> doGetMappings(CmsObjectType type, String name) throws CmsStorageException;
}