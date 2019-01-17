/**
 *
 */

package com.armedia.caliente.store;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.caliente.store.CmfValueMapper.Mapping;
import com.armedia.caliente.store.tools.CollectionObjectHandler;
import com.armedia.commons.utilities.Tools;

/**
 * @author Diego Rivera &lt;diego.rivera@armedia.com&gt;
 *
 */
public abstract class CmfObjectStore<CONNECTION, OPERATION extends CmfStoreOperation<CONNECTION>>
	extends CmfStore<CONNECTION, OPERATION> {

	public static enum LockStatus {
		//
		LOCK_ACQUIRED, // Lock was acquired by the current thread
		ALREADY_LOCKED, // Lock was not acquired, the object is locked by another thread
		ALREADY_STORED, // Lock was not acquired, but the object was stored successfully
		ALREADY_FAILED, // Lock was not acquired, but the object failed to be stored
		//
		;
	}

	public static enum StoreStatus {
		//
		STORED(LockStatus.ALREADY_STORED), // Object was stored successfully
		SKIPPED(LockStatus.ALREADY_FAILED), // Object was deliberately skipped
		FAILED(LockStatus.ALREADY_FAILED), // Object was not stored due to a failure
		//
		;

		private final LockStatus lockStatus;

		private StoreStatus(LockStatus lockStatus) {
			this.lockStatus = lockStatus;
		}
	}

	private class Mapper extends CmfValueMapper {

		private final OPERATION operation;

		private final Logger log = LoggerFactory.getLogger(getClass());

		private Mapper() {
			this.operation = null;
		}

		private Mapper(OPERATION operation) {
			this.operation = operation;
		}

		private Mapping constructMapping(CmfType type, String name, String source, String target) {
			return super.newMapping(type, name, source, target);
		}

		@Override
		protected Mapping createMapping(CmfType type, String name, String source, String target) {
			if (type == null) { throw new IllegalArgumentException("Must provide an object type to map for"); }
			if (name == null) { throw new IllegalArgumentException("Must provide a mapping name to map for"); }
			if ((source == null) && (target == null)) {
				throw new IllegalArgumentException("Must provide either a source or a target value for the mapping");
			}

			this.log.debug("Creating a {} mapping for attribute [{}] from [{}] to [{}]{}", type, name, source, target,
				(this.operation != null ? " within a transaction" : ""));
			try {
				Mapping ret = null;
				if (this.operation == null) {
					ret = CmfObjectStore.this.createMapping(type, name, source, target);
				} else {
					CmfObjectStore.this.createMapping(this.operation, type, name, source, target);
					if ((source != null) && (target != null)) {
						ret = constructMapping(type, name, source, target);
					}
				}
				if (ret != null) {
					this.log.debug("Created {}", ret);
				} else {
					this.log.debug("Deleted the {} mapping(s) for [{}] {} [{}] ", type, name,
						(source != null ? "from" : "to"), Tools.coalesce(source, target));
				}
				return ret;
			} catch (CmfStorageException e) {
				if (this.log.isDebugEnabled()) {
					this.log.error("Failed to create a {} mapping for attribute [{}] from [{}] to [{}]", type, name,
						source, target, e);
				}
				throw new RuntimeException(String.format(
					"Exception caught attempting to get the target mapping for [%s/%s/%s]", type, name, source), e);
			}
		}

		@Override
		public Mapping getTargetMapping(CmfType type, String name, String source) {
			try {
				if (this.operation == null) { return CmfObjectStore.this.getTargetMapping(type, name, source); }
				return CmfObjectStore.this.getTargetMapping(this.operation, type, name, source);
			} catch (CmfStorageException e) {
				throw new RuntimeException(String.format(
					"Exception caught attempting to get the target mapping for [%s/%s/%s]", type, name, source), e);
			}
		}

		@Override
		public Mapping getSourceMapping(CmfType type, String name, String target) {
			try {
				if (this.operation == null) { return CmfObjectStore.this.getSourceMapping(type, name, target); }
				return CmfObjectStore.this.getSourceMapping(this.operation, type, name, target);
			} catch (CmfStorageException e) {
				throw new RuntimeException(String.format(
					"Exception caught attempting to get the source mapping for [%s/%s/%s]", type, name, target), e);
			}
		}

		@Override
		public Map<CmfType, Set<String>> getAvailableMappings() {
			try {
				if (this.operation == null) { return CmfObjectStore.this.getAvailableMappings(); }
				return CmfObjectStore.this.getAvailableMappings(this.operation);
			} catch (CmfStorageException e) {
				throw new RuntimeException("Exception caught attempting to get available mappings", e);
			}
		}

		@Override
		public Set<String> getAvailableMappings(CmfType type) {
			if (type == null) { throw new IllegalArgumentException("Must provide an object type to search against"); }
			try {
				if (this.operation == null) { return CmfObjectStore.this.getAvailableMappings(type); }
				return CmfObjectStore.this.getAvailableMappings(this.operation, type);
			} catch (CmfStorageException e) {
				throw new RuntimeException("Exception caught attempting to get available mappings", e);
			}
		}

		@Override
		public Map<String, String> getMappings(CmfType type, String name) {
			if (type == null) { throw new IllegalArgumentException("Must provide an object type to search against"); }
			if (name == null) { throw new IllegalArgumentException("Must provide a mapping name to search for"); }
			try {
				if (this.operation == null) { return CmfObjectStore.this.getMappings(type, name); }
				return CmfObjectStore.this.getMappings(this.operation, type, name);
			} catch (CmfStorageException e) {
				throw new RuntimeException(
					String.format("Exception caught attempting to get available mappings for [%s/%s]", type, name), e);
			}
		}
	}

	protected final Logger log = LoggerFactory.getLogger(getClass());
	private final Mapper mapper = new Mapper();
	private final Class<OPERATION> operationClass;
	private boolean open = false;

	protected CmfObjectStore(Class<OPERATION> operationClass) throws CmfStorageException {
		this(operationClass, false);
	}

	protected CmfObjectStore(Class<OPERATION> operationClass, boolean openState) throws CmfStorageException {
		if (operationClass == null) { throw new IllegalArgumentException("Must provide the operation class"); }
		this.operationClass = operationClass;
		this.open = openState;
	}

	public final boolean init(Map<String, String> settings) throws CmfStorageException {
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

	protected void doInit(Map<String, String> settings) throws CmfStorageException {
	}

	protected final OPERATION castOperation(CmfStoreOperation<?> operation) {
		if (operation == null) { throw new IllegalArgumentException("Must provide a valid operation"); }
		return this.operationClass.cast(operation);
	}

	public final Long storeObject(CmfObject<CmfValue> object) throws CmfStorageException {
		if (object == null) { throw new IllegalArgumentException("Must provide an object to store"); }
		OPERATION operation = beginConcurrentInvocation();
		try {
			final boolean tx = operation.begin();
			boolean ok = false;
			try {
				Long ret = storeObject(operation, object);
				markStoreStatus(operation, object, StoreStatus.STORED, null);
				object.setNumber(ret);
				if (tx) {
					operation.commit();
				}
				ok = true;
				return ret;
			} finally {
				if (tx && !ok) {
					try {
						operation.rollback();
					} catch (CmfStorageException e) {
						this.log.warn(
							String.format("Failed to rollback the transaction for %s", object.getDescription()), e);
					}
				}
			}
		} finally {
			endInvocation(operation);
		}
	}

	protected abstract Long storeObject(OPERATION operation, CmfObject<CmfValue> object) throws CmfStorageException;

	public final boolean markStoreStatus(CmfObjectRef target, StoreStatus status) throws CmfStorageException {
		return markStoreStatus(target, status, null);
	}

	public final boolean markStoreStatus(CmfObjectRef target, StoreStatus status, String message)
		throws CmfStorageException {
		if (target == null) { throw new IllegalArgumentException("Must provide an object target"); }
		if (status == null) { throw new IllegalArgumentException("Must provide a status to mark the object with"); }
		OPERATION operation = beginConcurrentInvocation();
		try {
			final boolean tx = operation.begin();
			boolean ok = false;
			try {
				final boolean ret = markStoreStatus(operation, target, status, message);
				if (tx) {
					operation.commit();
				}
				ok = true;
				return ret;
			} finally {
				if (tx && !ok) {
					try {
						operation.rollback();
					} catch (CmfStorageException e) {
						this.log.warn(
							String.format("Failed to rollback the transaction for %s", target.getShortLabel()), e);
					}
				}
			}
		} finally {
			endInvocation(operation);
		}
	}

	protected abstract boolean markStoreStatus(OPERATION operation, CmfObjectRef target, StoreStatus status,
		String message) throws CmfStorageException;

	public final <VALUE> void setContentStreams(CmfObject<VALUE> object, Collection<CmfContentStream> content)
		throws CmfStorageException {
		if (object == null) { throw new IllegalArgumentException("Must provide an object to store"); }
		if ((content == null) || content.isEmpty()) { return; }
		OPERATION operation = beginConcurrentInvocation();
		try {
			final boolean tx = operation.begin();
			boolean ok = false;
			try {
				setContentStreams(operation, object, content);
				if (tx) {
					operation.commit();
				}
				ok = true;
			} finally {
				if (tx && !ok) {
					try {
						operation.rollback();
					} catch (CmfStorageException e) {
						this.log.warn(
							String.format("Failed to rollback the transaction for %s", object.getDescription()), e);
					}
				}
			}
		} finally {
			endInvocation(operation);
		}
	}

	protected abstract <VALUE> void setContentStreams(OPERATION operation, CmfObject<VALUE> object,
		Collection<CmfContentStream> content) throws CmfStorageException;

	public final <VALUE> List<CmfContentStream> getContentStreams(CmfObject<VALUE> object) throws CmfStorageException {
		if (object == null) { throw new IllegalArgumentException("Must provide an object to store"); }
		OPERATION operation = beginConcurrentInvocation();
		try {
			final boolean tx = operation.begin();
			try {
				return getContentStreams(operation, object);
			} finally {
				if (tx) {
					try {
						operation.rollback();
					} catch (CmfStorageException e) {
						this.log.warn(
							String.format("Failed to rollback the transaction for %s", object.getDescription()), e);
					}
				}
			}
		} finally {
			endInvocation(operation);
		}
	}

	protected abstract <VALUE> List<CmfContentStream> getContentStreams(OPERATION operation, CmfObject<VALUE> object)
		throws CmfStorageException;

	public final StoreStatus getStoreStatus(CmfObjectRef target) throws CmfStorageException {
		if (target == null) { throw new IllegalArgumentException("Must provide an object spec to check for"); }
		OPERATION operation = beginConcurrentInvocation();
		try {
			final boolean tx = operation.begin();
			try {
				return getStoreStatus(operation, target);
			} finally {
				if (tx) {
					try {
						operation.rollback();
					} catch (CmfStorageException e) {
						this.log.warn(String.format("Failed to rollback the transaction for %s (%s)",
							target.getType().name(), target.getId()), e);
					}
				}
			}
		} finally {
			endInvocation(operation);
		}
	}

	protected abstract StoreStatus getStoreStatus(OPERATION operation, CmfObjectRef target) throws CmfStorageException;

	public final LockStatus lockForStorage(CmfObjectRef target, CmfObjectRef referrent, String historyId, String lockId)
		throws CmfStorageException {
		if (target == null) { throw new IllegalArgumentException("Must provide an object spec to check for"); }
		OPERATION operation = beginConcurrentInvocation();
		try {
			final boolean tx = operation.begin();
			boolean ok = false;
			try {

				final boolean locked;

				// First things first - are we able to lock the history, or do we already own the
				// lock?
				if (lockHistory(operation, target.getType(), historyId, lockId)) {
					// We own the history, so try to lock the object itself!
					locked = lockForStorage(operation, target, referrent);
				} else {
					// We don't own the history, so by definition we can't own the object's lock,
					// but we still have to check its store status so we can report it upwards.
					locked = false;
				}

				final StoreStatus storeStatus = getStoreStatus(operation, target);
				final LockStatus ret;
				if (locked) {
					if (storeStatus != null) {
						// WTF? We have an issue here - we got the lock, but we also have a
						// non-null status??
						throw new CmfStorageException(
							String.format("Unexpected storage status [%s] detected for %s while acquiring storage lock",
								storeStatus.name(), target.getShortLabel()));
					}
					// We acquired the lock...and there is no existing status
					ret = LockStatus.LOCK_ACQUIRED;
				} else {
					// We didn't acquire the lock...so...what's the status?
					if (storeStatus == null) {
						// We didn't get the lock, but the object hasn't yet been fully stored by
						// someone else...
						ret = LockStatus.ALREADY_LOCKED;
					} else {
						// We didn't get the lock, but someone else already did their thing here
						ret = storeStatus.lockStatus;
					}
				}
				if (tx) {
					operation.commit();
				}
				ok = true;
				return ret;
			} finally {
				if (tx && !ok) {
					try {
						operation.rollback();
					} catch (CmfStorageException e) {
						this.log.warn(
							String.format("Failed to rollback the transaction for %s", target.getShortLabel()), e);
					}
				}
			}
		} finally {
			endInvocation(operation);
		}
	}

	/**
	 * Obtain a lock for the given object reference, returning {@code true} if the lock was
	 * obtained, and {@code false} otherwise. The lock is non-reentrant.
	 *
	 * @param operation
	 * @param target
	 * @param referrent
	 * @throws CmfStorageException
	 */
	protected abstract boolean lockForStorage(OPERATION operation, CmfObjectRef target, CmfObjectRef referrent)
		throws CmfStorageException;

	/**
	 * Obtain a re-entrant lock for the given history and object type, for the given lock ID. This
	 * method will return {@code true} if there was no existing lock for the given type or history
	 * ID, or if the existing lock's ID is the same as the given lock ID. If a lock exists but is
	 * for a different lock ID, this method returns {@code false}.
	 *
	 * @param operation
	 * @param type
	 * @param historyId
	 * @param lockId
	 * @throws CmfStorageException
	 */
	protected abstract boolean lockHistory(OPERATION operation, CmfType type, String historyId, String lockId)
		throws CmfStorageException;

	public final CmfObject<CmfValue> loadHeadObject(CmfType type, String historyId) throws CmfStorageException {
		if (type == null) { throw new IllegalArgumentException("Must provide an object type to work with"); }
		if (historyId == null) { throw new IllegalArgumentException("Must provide a history ID to work with"); }

		OPERATION operation = beginConcurrentInvocation();
		try {
			final boolean tx = operation.begin();
			try {
				return loadHeadObject(operation, type, historyId);
			} finally {
				if (tx) {
					try {
						operation.rollback();
					} catch (CmfStorageException e) {
						this.log.warn(
							String.format("Failed to rollback the transaction for loading the head object for %s %s",
								type, historyId),
							e);
					}
				}
			}
		} finally {
			endInvocation(operation);
		}
	}

	protected abstract CmfObject<CmfValue> loadHeadObject(OPERATION operation, CmfType type, String historyId)
		throws CmfStorageException;

	public final Collection<CmfObject<CmfValue>> loadObjects(CmfType type, String... ids) throws CmfStorageException {
		return loadObjects(type, (ids != null ? Arrays.asList(ids) : null));
	}

	public final Collection<CmfObject<CmfValue>> loadObjects(final CmfType type, Collection<String> ids)
		throws CmfStorageException {
		OPERATION operation = beginConcurrentInvocation();
		try {
			final boolean tx = operation.begin();
			try {
				return loadObjects(operation, type, ids);
			} finally {
				if (tx) {
					try {
						operation.rollback();
					} catch (CmfStorageException e) {
						this.log.warn(String.format(
							"Failed to rollback the transaction for loading objects of type %s: %s", type, ids), e);
					}
				}
			}
		} finally {
			endInvocation(operation);
		}
	}

	protected final Collection<CmfObject<CmfValue>> loadObjects(final OPERATION operation, final CmfType type,
		Collection<String> ids) throws CmfStorageException {
		if (operation == null) { throw new IllegalArgumentException("Must provide an operation to work with"); }
		if (type == null) { throw new IllegalArgumentException("Must provide an object type to retrieve"); }
		if (ids == null) {
			ids = Collections.emptyList();
		}
		Set<String> actualIds = null;
		final List<CmfObject<CmfValue>> ret = new ArrayList<>(ids.size());
		if (ids.isEmpty()) { return ret; }
		actualIds = new HashSet<>();
		for (String s : ids) {
			if (s == null) {
				continue;
			}
			actualIds.add(s);
		}
		final CmfObjectHandler<CmfValue> h = new CollectionObjectHandler<>(ret);
		loadObjects(operation, type, actualIds, h);
		return ret;
	}

	public final int loadObjects(final CmfType type, CmfObjectHandler<CmfValue> handler) throws CmfStorageException {
		return loadObjects(type, null, handler);
	}

	public final int loadObjects(final CmfType type, Collection<String> ids, final CmfObjectHandler<CmfValue> handler)
		throws CmfStorageException {
		if (type == null) { throw new IllegalArgumentException("Must provide an object type to load"); }
		if (handler == null) {
			throw new IllegalArgumentException("Must provide an object handler to handle the deserialized objects");
		}
		OPERATION operation = beginConcurrentInvocation();
		try {
			final boolean tx = operation.begin();
			try {
				return loadObjects(operation, type, ids, handler);
			} finally {
				if (tx) {
					try {
						operation.rollback();
					} catch (CmfStorageException e) {
						this.log.warn(
							String.format("Failed to rollback the transaction for loading %s : %s", type, ids), e);
					}
				}
			}
		} finally {
			endInvocation(operation);
		}
	}

	protected abstract int loadObjects(OPERATION operation, CmfType type, Collection<String> ids,
		CmfObjectHandler<CmfValue> handler) throws CmfStorageException;

	public final int fixObjectNames(final CmfNameFixer<CmfValue> nameFixer) throws CmfStorageException {
		return fixObjectNames(nameFixer, null, null);
	}

	public final int fixObjectNames(final CmfNameFixer<CmfValue> nameFixer, final CmfType type)
		throws CmfStorageException {
		return fixObjectNames(nameFixer, type, null);
	}

	public final int fixObjectNames(final CmfNameFixer<CmfValue> nameFixer, final CmfType type, Set<String> ids)
		throws CmfStorageException {
		if (nameFixer == null) {
			throw new IllegalArgumentException("Must provide name fixer to fix the object names");
		}
		if (ids != null) {
			if (type == null) {
				throw new CmfStorageException("Submitted a set of IDs without an object type - this is not supported");
			}
			// Short-circuit - avoid doing anything if there's nothing to do
			if (ids.isEmpty()) { return 0; }
		}

		OPERATION operation = beginConcurrentInvocation();
		boolean ok = false;
		try {
			final boolean tx = operation.begin();
			try {
				int ret = fixObjectNames(operation, nameFixer, type, ids);
				if (tx) {
					operation.commit();
				}
				ok = true;
				return ret;
			} finally {
				if (tx && !ok) {
					try {
						operation.rollback();
					} catch (CmfStorageException e) {
						this.log.warn("Failed to roll back the transaction for fixing object names", e);
					}
				}
			}
		} finally {
			endInvocation(operation);
		}
	}

	protected abstract int fixObjectNames(OPERATION operation, CmfNameFixer<CmfValue> nameFixer, CmfType type,
		Set<String> ids) throws CmfStorageException;

	public final void scanObjectTree(final CmfTreeScanner scanner) throws CmfStorageException {
		if (scanner == null) { throw new IllegalArgumentException("Must provide scanner to process the object tree"); }
		OPERATION operation = beginConcurrentInvocation();
		try {
			final boolean tx = operation.begin();
			try {
				scanObjectTree(operation, scanner);
			} finally {
				if (tx) {
					try {
						operation.rollback();
					} catch (CmfStorageException e) {
						this.log.warn("Failed to roll back the transaction for scanning the object tree", e);
					}
				}
			}
		} finally {
			endInvocation(operation);
		}
	}

	protected abstract void scanObjectTree(final OPERATION operation, final CmfTreeScanner scanner)
		throws CmfStorageException;

	private Mapping createMapping(CmfType type, String name, String source, String target) throws CmfStorageException {
		if (type == null) { throw new IllegalArgumentException("Must provide an object type to map for"); }
		if (name == null) { throw new IllegalArgumentException("Must provide a mapping name to map for"); }
		if ((source == null) && (target == null)) {
			throw new IllegalArgumentException("Must provide either a source or a target value for the mapping");
		}
		OPERATION operation = beginConcurrentInvocation();
		try {
			final boolean tx = operation.begin();
			boolean ok = false;
			try {
				createMapping(operation, type, name, source, target);
				Mapping ret = null;
				if ((source != null) && (target != null)) {
					ret = this.mapper.constructMapping(type, name, source, target);
				}
				if (tx) {
					operation.commit();
				}
				ok = true;
				return ret;
			} finally {
				if (tx && !ok) {
					try {
						operation.rollback();
					} catch (CmfStorageException e) {
						this.log.warn(String.format("Failed to rollback the transaction for mapping [%s::%s(%s->%s))",
							type, name, source, target), e);
					}
				}
			}
		} finally {
			endInvocation(operation);
		}
	}

	protected abstract void createMapping(OPERATION operation, CmfType type, String name, String source, String target)
		throws CmfStorageException;

	protected abstract String getMapping(OPERATION operation, boolean source, CmfType type, String name, String value)
		throws CmfStorageException;

	public final Mapping getTargetMapping(CmfType type, String name, String source) throws CmfStorageException {
		OPERATION operation = beginConcurrentInvocation();
		try {
			final boolean tx = operation.begin();
			try {
				return getTargetMapping(operation, type, name, source);
			} finally {
				if (tx) {
					try {
						operation.rollback();
					} catch (CmfStorageException e) {
						this.log.warn(String.format("Failed to rollback the transaction for mapping [%s::%s(%s->?))",
							type, name, source), e);
					}
				}
			}
		} finally {
			endInvocation(operation);
		}
	}

	protected final Mapping getTargetMapping(OPERATION operation, CmfType type, String name, String source)
		throws CmfStorageException {
		if (operation == null) { throw new IllegalArgumentException("Must provide an operation to work with"); }
		if (type == null) { throw new IllegalArgumentException("Must provide an object type to search against"); }
		if (name == null) { throw new IllegalArgumentException("Must provide a mapping name to search for"); }
		if (source == null) {
			throw new IllegalArgumentException("Must provide a source value to find the target mapping for");
		}
		String target = getMapping(operation, true, type, name, source);
		if (target == null) { return null; }
		return this.mapper.constructMapping(type, name, source, target);
	}

	public final Mapping getSourceMapping(CmfType type, String name, String target) throws CmfStorageException {
		OPERATION operation = beginConcurrentInvocation();
		try {
			final boolean tx = operation.begin();
			try {
				return getSourceMapping(operation, type, name, target);
			} finally {
				if (tx) {
					try {
						operation.rollback();
					} catch (CmfStorageException e) {
						this.log.warn(String.format("Failed to rollback the transaction for mapping [%s::%s(?->%s))",
							type, name, target), e);
					}
				}
			}
		} finally {
			endInvocation(operation);
		}
	}

	protected final Mapping getSourceMapping(OPERATION operation, CmfType type, String name, String target)
		throws CmfStorageException {
		if (operation == null) { throw new IllegalArgumentException("Must provide an operation to work with"); }
		if (type == null) { throw new IllegalArgumentException("Must provide an object type to search against"); }
		if (name == null) { throw new IllegalArgumentException("Must provide a mapping name to search for"); }
		if (target == null) {
			throw new IllegalArgumentException("Must provide a target value to find the source mapping for");
		}
		String source = getMapping(operation, false, type, name, target);
		if (source == null) { return null; }
		return this.mapper.constructMapping(type, name, source, target);
	}

	public final Map<CmfType, Long> getStoredObjectTypes() throws CmfStorageException {
		OPERATION operation = beginConcurrentInvocation();
		try {
			final boolean tx = operation.begin();
			try {
				return getStoredObjectTypes(operation);
			} finally {
				if (tx) {
					operation.rollback();
				}
			}
		} finally {
			endInvocation(operation);
		}
	}

	protected abstract Map<CmfType, Long> getStoredObjectTypes(OPERATION operation) throws CmfStorageException;

	public final CmfValueMapper getValueMapper() {
		return this.mapper;
	}

	protected final CmfValueMapper getValueMapper(OPERATION operation) {
		return new Mapper(operation);
	}

	public final void resetAltNames() throws CmfStorageException {
		OPERATION operation = beginExclusiveInvocation();
		try {
			final boolean tx = operation.begin();
			boolean ok = false;
			try {
				resetAltNames(operation);
				if (tx) {
					operation.commit();
				}
				ok = true;
			} finally {
				if (tx && !ok) {
					if (tx && !ok) {
						try {
							operation.rollback();
						} catch (CmfStorageException e) {
							this.log.warn("Failed to rollback the transaction for resetting the alternate names", e);
						}
					}
				}
			}
		} finally {
			endInvocation(operation);
		}
	}

	protected abstract void resetAltNames(OPERATION operation) throws CmfStorageException;

	public final <VALUE> boolean renameObject(final CmfObject<VALUE> object, final String newName)
		throws CmfStorageException {
		if (object == null) { throw new IllegalArgumentException("Must provide an object to rename"); }
		if (newName == null) { throw new IllegalArgumentException("Must provide new name for the object"); }

		// Shortcut - do nothing if there's no name change
		if (Tools.equals(newName, object.getName())) { return false; }

		OPERATION operation = beginExclusiveInvocation();
		try {
			final boolean tx = operation.begin();
			boolean ok = false;
			try {
				renameObject(operation, object, newName);
				if (tx) {
					operation.commit();
				}
				ok = true;
				return true;
			} finally {
				if (tx && !ok) {
					if (tx && !ok) {
						try {
							operation.rollback();
						} catch (CmfStorageException e) {
							this.log.warn("Failed to rollback the transaction for resetting the alternate names", e);
						}
					}
				}
			}
		} finally {
			endInvocation(operation);
		}
	}

	protected abstract <VALUE> void renameObject(final OPERATION operation, final CmfObject<VALUE> object,
		final String newName) throws CmfStorageException;

	public final int clearAttributeMappings() throws CmfStorageException {
		OPERATION operation = beginExclusiveInvocation();
		try {
			final boolean tx = operation.begin();
			boolean ok = false;
			try {
				int ret = clearAttributeMappings(operation);
				if (tx) {
					operation.commit();
				}
				ok = true;
				return ret;
			} finally {
				if (tx && !ok) {
					if (tx && !ok) {
						try {
							operation.rollback();
						} catch (CmfStorageException e) {
							this.log.warn("Failed to rollback the transaction for clearing all attribute mappings", e);
						}
					}
				}
			}
		} finally {
			endInvocation(operation);
		}
	}

	protected abstract int clearAttributeMappings(OPERATION operation) throws CmfStorageException;

	public final Map<CmfType, Set<String>> getAvailableMappings() throws CmfStorageException {
		OPERATION operation = beginConcurrentInvocation();
		try {
			final boolean tx = operation.begin();
			try {
				return getAvailableMappings(operation);
			} finally {
				if (tx) {
					if (tx) {
						try {
							operation.rollback();
						} catch (CmfStorageException e) {
							this.log.warn("Failed to rollback the transaction for getting all available mappings", e);
						}
					}
				}
			}
		} finally {
			endInvocation(operation);
		}
	}

	protected abstract Map<CmfType, Set<String>> getAvailableMappings(OPERATION operation) throws CmfStorageException;

	public final Set<String> getAvailableMappings(CmfType type) throws CmfStorageException {
		if (type == null) { throw new IllegalArgumentException("Must provide an object type to search against"); }
		OPERATION operation = beginConcurrentInvocation();
		try {
			final boolean tx = operation.begin();
			try {
				return getAvailableMappings(operation, type);
			} finally {
				if (tx) {
					try {
						operation.rollback();
					} catch (CmfStorageException e) {
						this.log.warn(String.format(
							"Failed to rollback the transaction for getting all available mappings for type %s", type),
							e);
					}
				}
			}
		} finally {
			endInvocation(operation);
		}
	}

	protected abstract Set<String> getAvailableMappings(OPERATION operation, CmfType type) throws CmfStorageException;

	public final Map<String, String> getMappings(CmfType type, String name) throws CmfStorageException {
		if (type == null) { throw new IllegalArgumentException("Must provide an object type to search against"); }
		if (name == null) { throw new IllegalArgumentException("Must provide a mapping name to search for"); }
		OPERATION operation = beginConcurrentInvocation();
		try {
			final boolean tx = operation.begin();
			try {
				return getMappings(operation, type, name);
			} finally {
				if (tx) {
					try {
						operation.rollback();
					} catch (CmfStorageException e) {
						this.log.warn(String.format(
							"Failed to rollback the transaction for getting all available [%s] mappings for type %s",
							name, type), e);
					}
				}
			}
		} finally {
			endInvocation(operation);
		}
	}

	protected abstract Map<String, String> getMappings(OPERATION operation, CmfType type, String name)
		throws CmfStorageException;

	public final void clearAllObjects() throws CmfStorageException {
		OPERATION operation = beginExclusiveInvocation();
		try {
			final boolean tx = operation.begin();
			boolean ok = false;
			try {
				clearAllObjects(operation);
				if (tx) {
					operation.commit();
				}
				ok = true;
			} finally {
				if (tx && !ok) {
					try {
						operation.rollback();
					} catch (CmfStorageException e) {
						this.log.warn("Failed to rollback the transaction for clearing all objects", e);
					}
				}
			}
		} finally {
			endInvocation(operation);
		}
	}

	protected abstract void clearAllObjects(OPERATION operation) throws CmfStorageException;

	public final Map<CmfType, Map<String, String>> getRenameMappings() throws CmfStorageException {
		OPERATION operation = beginConcurrentInvocation();
		try {
			final boolean tx = operation.begin();
			try {
				return getRenameMappings(operation);
			} finally {
				if (tx) {
					try {
						operation.rollback();
					} catch (CmfStorageException e) {
						this.log.warn("Failed to rollback the transaction for retrieving all rename mappings", e);
					}
				}
			}
		} finally {
			endInvocation(operation);
		}
	}

	protected abstract Map<CmfType, Map<String, String>> getRenameMappings(OPERATION operation)
		throws CmfStorageException;

	public final Map<CmfObjectRef, String> getObjectNames(Collection<CmfObjectRef> refs, boolean latest)
		throws CmfStorageException {
		if ((refs == null) || refs.isEmpty()) { return new HashMap<>(); }
		OPERATION operation = beginConcurrentInvocation();
		try {
			final boolean tx = operation.begin();
			try {
				return getObjectNames(operation, refs, latest);
			} finally {
				if (tx) {
					try {
						operation.rollback();
					} catch (CmfStorageException e) {
						this.log.warn("Failed to rollback the transaction for retrieving all rename mappings", e);
					}
				}
			}
		} finally {
			endInvocation(operation);
		}
	}

	protected abstract Map<CmfObjectRef, String> getObjectNames(OPERATION operation, Collection<CmfObjectRef> refs,
		boolean latest) throws CmfStorageException;

	public final Collection<CmfObjectRef> getContainers(CmfObjectRef object) throws CmfStorageException {
		if (object == null) { throw new IllegalArgumentException("Must provide an object to check for"); }
		if (object.isNull()) { throw new IllegalArgumentException("Null object references are not allowed"); }
		OPERATION operation = beginConcurrentInvocation();
		try {
			final boolean tx = operation.begin();
			try {
				return getContainers(operation, object);
			} finally {
				if (tx) {
					try {
						operation.rollback();
					} catch (CmfStorageException e) {
						this.log.warn("Failed to rollback the transaction for retrieving all containers", e);
					}
				}
			}
		} finally {
			endInvocation(operation);
		}
	}

	protected abstract Collection<CmfObjectRef> getContainers(OPERATION operation, CmfObjectRef object)
		throws CmfStorageException;

	public final Collection<CmfObjectRef> getContainedObjects(CmfObjectRef object) throws CmfStorageException {
		if (object == null) { throw new IllegalArgumentException("Must provide an object to check for"); }
		if (object.isNull()) { throw new IllegalArgumentException("Null object references are not allowed"); }
		OPERATION operation = beginConcurrentInvocation();
		try {
			final boolean tx = operation.begin();
			try {
				return getContainedObjects(operation, object);
			} finally {
				if (tx) {
					try {
						operation.rollback();
					} catch (CmfStorageException e) {
						this.log.warn("Failed to rollback the transaction for retrieving all contained objects", e);
					}
				}
			}
		} finally {
			endInvocation(operation);
		}
	}

	protected abstract Collection<CmfObjectRef> getContainedObjects(OPERATION operation, CmfObjectRef object)
		throws CmfStorageException;

	public final boolean addRequirement(CmfObjectRef object, CmfObjectRef requirement) throws CmfStorageException {
		if (object == null) { throw new IllegalArgumentException("Must provide an object to check for"); }
		if (object.isNull()) { throw new IllegalArgumentException("Null object references are not allowed"); }
		if (requirement == null) {
			throw new IllegalArgumentException("Must provide a requirement to associate to the base object");
		}
		if (requirement.isNull()) { throw new IllegalArgumentException("Null requirement references are not allowed"); }
		OPERATION operation = beginConcurrentInvocation();
		try {
			final boolean tx = operation.begin();
			try {
				final boolean ret = addRequirement(operation, object, requirement);
				if (tx) {
					operation.commit();
				}
				return ret;
			} finally {
				if (tx) {
					try {
						operation.rollback();
					} catch (CmfStorageException e) {
						this.log.warn("Failed to rollback the transaction for retrieving all contained objects", e);
					}
				}
			}
		} finally {
			endInvocation(operation);
		}
	}

	protected abstract boolean addRequirement(OPERATION operation, CmfObjectRef object, CmfObjectRef requirement)
		throws CmfStorageException;

	public final <T extends Enum<T>> CmfRequirementInfo<T> setImportStatus(CmfObjectRef object, T status, String info)
		throws CmfStorageException {
		if (object == null) { throw new IllegalArgumentException("Must provide an object to check for"); }
		if (object.isNull()) { throw new IllegalArgumentException("Null object references are not allowed"); }
		if (status == null) { throw new IllegalArgumentException("Must provide a non-null status"); }
		OPERATION operation = beginConcurrentInvocation();
		try {
			final boolean tx = operation.begin();
			try {
				CmfRequirementInfo<T> ret = setImportStatus(operation, object, status, info);
				if (tx) {
					operation.commit();
				}
				return ret;
			} finally {
				if (tx) {
					try {
						operation.rollback();
					} catch (CmfStorageException e) {
						this.log.warn(
							String.format("Failed to rollback the transaction for setting requirements info for %s",
								object.getShortLabel()),
							e);
					}
				}
			}
		} finally {
			endInvocation(operation);
		}
	}

	protected abstract <T extends Enum<T>> CmfRequirementInfo<T> setImportStatus(OPERATION operation,
		CmfObjectRef object, T status, String info) throws CmfStorageException;

	public final <T extends Enum<T>> Collection<CmfRequirementInfo<T>> getRequirementInfo(Class<T> statusClass,
		CmfObjectRef object) throws CmfStorageException {
		if (object == null) { throw new IllegalArgumentException("Must provide an object to check for"); }
		if (object.isNull()) { throw new IllegalArgumentException("Null object references are not allowed"); }
		OPERATION operation = beginConcurrentInvocation();
		try {
			final boolean tx = operation.begin();
			try {
				return getRequirementInfo(operation, statusClass, object);
			} finally {
				if (tx) {
					try {
						operation.rollback();
					} catch (CmfStorageException e) {
						this.log.warn("Failed to rollback the transaction for retrieving all contained objects", e);
					}
				}
			}
		} finally {
			endInvocation(operation);
		}
	}

	protected abstract <T extends Enum<T>> Collection<CmfRequirementInfo<T>> getRequirementInfo(OPERATION operation,
		Class<T> statusClass, CmfObjectRef object) throws CmfStorageException;

	public final void clearImportPlan() throws CmfStorageException {
		OPERATION operation = beginConcurrentInvocation();
		try {
			final boolean tx = operation.begin();
			try {
				clearImportPlan(operation);
				if (tx) {
					operation.commit();
				}
			} finally {
				if (tx) {
					try {
						operation.rollback();
					} catch (CmfStorageException e) {
						this.log.warn("Failed to rollback the transaction for clearing the import status", e);
					}
				}
			}
		} finally {
			endInvocation(operation);
		}
	}

	protected abstract void clearImportPlan(OPERATION operation) throws CmfStorageException;
}