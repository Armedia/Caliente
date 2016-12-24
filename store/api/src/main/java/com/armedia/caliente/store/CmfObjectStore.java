/**
 *
 */

package com.armedia.caliente.store;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.caliente.store.CmfAttributeMapper.Mapping;
import com.armedia.caliente.store.tools.CollectionObjectHandler;
import com.armedia.commons.utilities.Tools;

/**
 * @author Diego Rivera &lt;diego.rivera@armedia.com&gt;
 *
 */
public abstract class CmfObjectStore<C, O extends CmfStoreOperation<C>> extends CmfStore<C, O> {

	public static enum LockStatus {
		//
		LOCK_ACQUIRED, // Lock was acquired by the current thread
		LOCK_CONCURRENT, // Lock is concurrent, but the object's storage outcome is unknown
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

	private class Mapper extends CmfAttributeMapper {

		private final O operation;

		private Mapper() {
			this.operation = null;
		}

		private Mapper(O operation) {
			this.operation = operation;
		}

		private Mapping constructMapping(CmfType type, String name, String source, String target) {
			return super.newMapping(type, name, source, target);
		}

		@Override
		protected Mapping createMapping(CmfType type, String name, String source, String target) {
			if (type == null) { throw new IllegalArgumentException("Must provide an object type to map for"); }
			if (name == null) { throw new IllegalArgumentException("Must provide a mapping name to map for"); }
			if ((source == null) && (target == null)) { throw new IllegalArgumentException(
				"Must provide either a source or a target value for the mapping"); }
			try {
				if (this.operation == null) { return CmfObjectStore.this.createMapping(type, name, source, target); }
				CmfObjectStore.this.createMapping(this.operation, type, name, source, target);
				Mapping ret = null;
				if ((source != null) && (target != null)) {
					ret = constructMapping(type, name, source, target);
				}
				return ret;
			} catch (CmfStorageException e) {
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
	private final Class<O> operationClass;
	private boolean open = false;

	protected CmfObjectStore(Class<O> operationClass) throws CmfStorageException {
		this(operationClass, false);
	}

	protected CmfObjectStore(Class<O> operationClass, boolean openState) throws CmfStorageException {
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

	protected final O castOperation(CmfStoreOperation<?> operation) {
		if (operation == null) { throw new IllegalArgumentException("Must provide a valid operation"); }
		return this.operationClass.cast(operation);
	}

	public final <V> Long storeObject(CmfObject<V> object, CmfAttributeTranslator<V> translator)
		throws CmfStorageException {
		if (object == null) { throw new IllegalArgumentException("Must provide an object to store"); }
		if (translator == null) { throw new IllegalArgumentException(
			"Must provide a translator for storing object values"); }
		O operation = beginConcurrentInvocation();
		try {
			final boolean tx = operation.begin();
			boolean ok = false;
			try {
				Long ret = storeObject(operation, object, translator);
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
						this.log.warn(String.format("Failed to rollback the transaction for %s [%s](%s)",
							object.getType(), object.getLabel(), object.getId()), e);
					}
				}
			}
		} finally {
			endConcurrentInvocation(operation);
		}
	}

	protected abstract <V> Long storeObject(O operation, CmfObject<V> object, CmfAttributeTranslator<V> translator)
		throws CmfStorageException;

	public final <V> boolean markStoreStatus(CmfObjectRef target, StoreStatus status) throws CmfStorageException {
		return markStoreStatus(target, status, null);
	}

	public final <V> boolean markStoreStatus(CmfObjectRef target, StoreStatus status, String message)
		throws CmfStorageException {
		if (target == null) { throw new IllegalArgumentException("Must provide an object target"); }
		if (status == null) { throw new IllegalArgumentException("Must provide a status to mark the object with"); }
		O operation = beginConcurrentInvocation();
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
			endConcurrentInvocation(operation);
		}
	}

	protected abstract <V> boolean markStoreStatus(O operation, CmfObjectRef target, StoreStatus status, String message)
		throws CmfStorageException;

	public final <V> void setContentInfo(CmfObject<V> object, Collection<CmfContentInfo> content)
		throws CmfStorageException {
		if (object == null) { throw new IllegalArgumentException("Must provide an object to store"); }
		if ((content == null) || content.isEmpty()) { return; }
		O operation = beginConcurrentInvocation();
		try {
			final boolean tx = operation.begin();
			boolean ok = false;
			try {
				setContentInfo(operation, object, content);
				if (tx) {
					operation.commit();
				}
				ok = true;
			} finally {
				if (tx && !ok) {
					try {
						operation.rollback();
					} catch (CmfStorageException e) {
						this.log.warn(String.format("Failed to rollback the transaction for %s [%s](%s)",
							object.getType(), object.getLabel(), object.getId()), e);
					}
				}
			}
		} finally {
			endConcurrentInvocation(operation);
		}
	}

	protected abstract <V> void setContentInfo(O operation, CmfObject<V> object, Collection<CmfContentInfo> content)
		throws CmfStorageException;

	public final <V> List<CmfContentInfo> getContentInfo(CmfObject<V> object) throws CmfStorageException {
		if (object == null) { throw new IllegalArgumentException("Must provide an object to store"); }
		O operation = beginConcurrentInvocation();
		try {
			final boolean tx = operation.begin();
			try {
				return getContentInfo(operation, object);
			} finally {
				if (tx) {
					try {
						operation.rollback();
					} catch (CmfStorageException e) {
						this.log.warn(String.format("Failed to rollback the transaction for %s [%s](%s)",
							object.getType(), object.getLabel(), object.getId()), e);
					}
				}
			}
		} finally {
			endConcurrentInvocation(operation);
		}
	}

	protected abstract <V> List<CmfContentInfo> getContentInfo(O operation, CmfObject<V> object)
		throws CmfStorageException;

	public final StoreStatus getStoreStatus(CmfObjectRef target) throws CmfStorageException {
		if (target == null) { throw new IllegalArgumentException("Must provide an object spec to check for"); }
		O operation = beginConcurrentInvocation();
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
			endConcurrentInvocation(operation);
		}
	}

	protected abstract StoreStatus getStoreStatus(O operation, CmfObjectRef target) throws CmfStorageException;

	public final LockStatus lockForStorage(CmfObjectRef target, CmfObjectRef referrent) throws CmfStorageException {
		if (target == null) { throw new IllegalArgumentException("Must provide an object spec to check for"); }
		O operation = beginConcurrentInvocation();
		try {
			final boolean tx = operation.begin();
			boolean ok = false;
			try {
				boolean locked = lockForStorage(operation, target, referrent);
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
						ret = LockStatus.LOCK_CONCURRENT;
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
			endConcurrentInvocation(operation);
		}
	}

	protected abstract boolean lockForStorage(O operation, CmfObjectRef target, CmfObjectRef referrent)
		throws CmfStorageException;

	protected final <V> CmfObject<V> adjustLoadedObject(CmfObject<V> dataObject, CmfTypeMapper typeMapper,
		CmfAttributeTranslator<V> translator) {
		// Ensure type mapping takes place, and ensure that translation also takes place
		// TODO: This should only happen if "necessary" (i.e. target CMS is different from the
		// source)
		String altType = typeMapper.mapType(dataObject.getSubtype());
		if (altType != null) {
			dataObject = new CmfObject<>(dataObject, altType);
		}
		return translator.decodeObject(dataObject);
	}

	public final <V> CmfObject<V> loadHeadObject(final CmfTypeMapper typeMapper, CmfAttributeTranslator<V> translator,
		CmfObject<V> sample) throws CmfStorageException {
		if (typeMapper == null) { throw new IllegalArgumentException("Must provde a type mapper"); }
		if (translator == null) { throw new IllegalArgumentException(
			"Must provide a translator for storing object values"); }
		if (sample == null) { throw new IllegalArgumentException("Must provide a sample to work with"); }
		if (sample.isHistoryCurrent()) { return sample; }

		O operation = beginConcurrentInvocation();
		try {
			final boolean tx = operation.begin();
			try {
				return loadHeadObject(operation, typeMapper, translator, sample);
			} finally {
				if (tx) {
					try {
						operation.rollback();
					} catch (CmfStorageException e) {
						this.log.warn(String.format(
							"Failed to rollback the transaction for loading the head object for %s", sample), e);
					}
				}
			}
		} finally {
			endConcurrentInvocation(operation);
		}
	}

	protected abstract <V> CmfObject<V> loadHeadObject(O operation, CmfTypeMapper typeMapper,
		CmfAttributeTranslator<V> translator, CmfObject<V> sample) throws CmfStorageException;

	public final <V> Collection<CmfObject<V>> loadObjects(final CmfTypeMapper typeMapper,
		CmfAttributeTranslator<V> translator, CmfType type, String... ids) throws CmfStorageException {
		return loadObjects(typeMapper, translator, type, (ids != null ? Arrays.asList(ids) : null));
	}

	public final <V> Collection<CmfObject<V>> loadObjects(final CmfTypeMapper typeMapper,
		final CmfAttributeTranslator<V> translator, final CmfType type, Collection<String> ids)
		throws CmfStorageException {
		O operation = beginConcurrentInvocation();
		try {
			final boolean tx = operation.begin();
			try {
				return loadObjects(operation, typeMapper, translator, type, ids);
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
			endConcurrentInvocation(operation);
		}
	}

	protected final <V> Collection<CmfObject<V>> loadObjects(final O operation, final CmfTypeMapper typeMapper,
		final CmfAttributeTranslator<V> translator, final CmfType type, Collection<String> ids)
		throws CmfStorageException {
		if (operation == null) { throw new IllegalArgumentException("Must provide an operation to work with"); }
		if (translator == null) { throw new IllegalArgumentException(
			"Must provide a translator for storing object values"); }
		if (type == null) { throw new IllegalArgumentException("Must provide an object type to retrieve"); }
		getReadLock().lock();
		try {
			if (ids == null) {
				ids = Collections.emptyList();
			}
			Set<String> actualIds = null;
			final List<CmfObject<V>> ret = new ArrayList<>(ids.size());
			if (ids.isEmpty()) { return ret; }
			actualIds = new HashSet<>();
			for (String s : ids) {
				if (s == null) {
					continue;
				}
				actualIds.add(s);
			}
			final CmfObjectHandler<V> h = new CollectionObjectHandler<>(ret);
			loadObjects(operation, translator, type, actualIds, new CmfObjectHandler<V>() {
				@Override
				public boolean newTier(int tierNumber) throws CmfStorageException {
					return h.newTier(tierNumber);
				}

				@Override
				public boolean handleObject(CmfObject<V> dataObject) throws CmfStorageException {
					return h.handleObject(adjustLoadedObject(dataObject, typeMapper, translator));
				}

				@Override
				public boolean newHistory(String historyId) throws CmfStorageException {
					return h.newHistory(historyId);
				}

				@Override
				public boolean handleException(Exception e) {
					return h.handleException(e);
				}

				@Override
				public boolean endHistory(String historyId, boolean ok) throws CmfStorageException {
					return h.endHistory(historyId, ok);
				}

				@Override
				public boolean endTier(int tierNumber, boolean ok) throws CmfStorageException {
					return h.endTier(tierNumber, ok);
				}
			});
			return ret;
		} finally {
			getReadLock().unlock();
		}
	}

	public final <V> int loadObjects(final CmfTypeMapper typeMapper, CmfAttributeTranslator<V> translator,
		final CmfType type, CmfObjectHandler<V> handler) throws CmfStorageException {
		return loadObjects(typeMapper, translator, type, null, handler);
	}

	public final <V> int loadObjects(final CmfTypeMapper typeMapper, final CmfAttributeTranslator<V> translator,
		final CmfType type, Collection<String> ids, final CmfObjectHandler<V> handler) throws CmfStorageException {
		if (translator == null) { throw new IllegalArgumentException("Must provide a translator for the conversions"); }
		if (type == null) { throw new IllegalArgumentException("Must provide an object type to load"); }
		if (handler == null) { throw new IllegalArgumentException(
			"Must provide an object handler to handle the deserialized objects"); }
		O operation = beginConcurrentInvocation();
		try {
			final boolean tx = operation.begin();
			try {
				return loadObjects(operation, translator, type, ids, new CmfObjectHandler<V>() {
					@Override
					public boolean newTier(int tierNumber) throws CmfStorageException {
						return handler.newTier(tierNumber);
					}

					@Override
					public boolean handleObject(CmfObject<V> dataObject) throws CmfStorageException {
						return handler.handleObject(adjustLoadedObject(dataObject, typeMapper, translator));
					}

					@Override
					public boolean newHistory(String historyId) throws CmfStorageException {
						return handler.newHistory(historyId);
					}

					@Override
					public boolean handleException(Exception e) {
						return handler.handleException(e);
					}

					@Override
					public boolean endHistory(String historyId, boolean ok) throws CmfStorageException {
						return handler.endHistory(historyId, ok);
					}

					@Override
					public boolean endTier(int tierNumber, boolean ok) throws CmfStorageException {
						return handler.endTier(tierNumber, ok);
					}
				});
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
			endConcurrentInvocation(operation);
		}
	}

	protected abstract <V> int loadObjects(O operation, CmfAttributeTranslator<V> translator, CmfType type,
		Collection<String> ids, CmfObjectHandler<V> handler) throws CmfStorageException;

	public final <V> int fixObjectNames(final CmfAttributeTranslator<V> translator, final CmfNameFixer<V> nameFixer)
		throws CmfStorageException {
		return fixObjectNames(translator, nameFixer, null, null);
	}

	public final <V> int fixObjectNames(final CmfAttributeTranslator<V> translator, final CmfNameFixer<V> nameFixer,
		final CmfType type) throws CmfStorageException {
		return fixObjectNames(translator, nameFixer, type, null);
	}

	public final <V> int fixObjectNames(final CmfAttributeTranslator<V> translator, final CmfNameFixer<V> nameFixer,
		final CmfType type, Set<String> ids) throws CmfStorageException {
		if (translator == null) { throw new IllegalArgumentException("Must provide a translator for the conversions"); }
		if (nameFixer == null) { throw new IllegalArgumentException(
			"Must provide name fixer to fix the object names"); }
		if (ids != null) {
			if (type == null) { throw new CmfStorageException(
				"Submitted a set of IDs without an object type - this is not supported"); }
			// Short-circuit - avoid doing anything if there's nothing to do
			if (ids.isEmpty()) { return 0; }
		}

		O operation = beginConcurrentInvocation();
		boolean ok = false;
		try {
			final boolean tx = operation.begin();
			try {
				int ret = fixObjectNames(operation, translator, nameFixer, type, ids);
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
			endConcurrentInvocation(operation);
		}
	}

	protected abstract <V> int fixObjectNames(O operation, CmfAttributeTranslator<V> translator,
		CmfNameFixer<V> nameFixer, CmfType type, Set<String> ids) throws CmfStorageException;

	public final void scanObjectTree(final CmfTreeScanner scanner) throws CmfStorageException {
		if (scanner == null) { throw new IllegalArgumentException("Must provide scanner to process the object tree"); }
		O operation = beginConcurrentInvocation();
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
			endConcurrentInvocation(operation);
		}
	}

	protected abstract void scanObjectTree(final O operation, final CmfTreeScanner scanner) throws CmfStorageException;

	private Mapping createMapping(CmfType type, String name, String source, String target) throws CmfStorageException {
		if (type == null) { throw new IllegalArgumentException("Must provide an object type to map for"); }
		if (name == null) { throw new IllegalArgumentException("Must provide a mapping name to map for"); }
		if ((source == null) && (target == null)) { throw new IllegalArgumentException(
			"Must provide either a source or a target value for the mapping"); }
		O operation = beginConcurrentInvocation();
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
			endConcurrentInvocation(operation);
		}
	}

	protected abstract void createMapping(O operation, CmfType type, String name, String source, String target)
		throws CmfStorageException;

	protected abstract String getMapping(O operation, boolean source, CmfType type, String name, String value)
		throws CmfStorageException;

	public final Mapping getTargetMapping(CmfType type, String name, String source) throws CmfStorageException {
		O operation = beginConcurrentInvocation();
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
			endConcurrentInvocation(operation);
		}
	}

	protected final Mapping getTargetMapping(O operation, CmfType type, String name, String source)
		throws CmfStorageException {
		if (operation == null) { throw new IllegalArgumentException("Must provide an operation to work with"); }
		if (type == null) { throw new IllegalArgumentException("Must provide an object type to search against"); }
		if (name == null) { throw new IllegalArgumentException("Must provide a mapping name to search for"); }
		if (source == null) { throw new IllegalArgumentException(
			"Must provide a source value to find the target mapping for"); }
		String target = getMapping(operation, true, type, name, source);
		if (target == null) { return null; }
		return this.mapper.constructMapping(type, name, source, target);
	}

	public final Mapping getSourceMapping(CmfType type, String name, String target) throws CmfStorageException {
		O operation = beginConcurrentInvocation();
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
			endConcurrentInvocation(operation);
		}
	}

	protected final Mapping getSourceMapping(O operation, CmfType type, String name, String target)
		throws CmfStorageException {
		if (operation == null) { throw new IllegalArgumentException("Must provide an operation to work with"); }
		if (type == null) { throw new IllegalArgumentException("Must provide an object type to search against"); }
		if (name == null) { throw new IllegalArgumentException("Must provide a mapping name to search for"); }
		if (target == null) { throw new IllegalArgumentException(
			"Must provide a target value to find the source mapping for"); }
		String source = getMapping(operation, false, type, name, target);
		if (source == null) { return null; }
		return this.mapper.constructMapping(type, name, source, target);
	}

	public final Map<CmfType, Long> getStoredObjectTypes() throws CmfStorageException {
		O operation = beginConcurrentInvocation();
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
			endConcurrentInvocation(operation);
		}
	}

	protected abstract Map<CmfType, Long> getStoredObjectTypes(O operation) throws CmfStorageException;

	public final CmfAttributeMapper getAttributeMapper() {
		return this.mapper;
	}

	protected final CmfAttributeMapper getAttributeMapper(O operation) {
		return new Mapper(operation);
	}

	public final void resetAltNames() throws CmfStorageException {
		O operation = beginExclusiveInvocation();
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
			endExclusiveInvocation(operation);
		}
	}

	protected abstract void resetAltNames(O operation) throws CmfStorageException;

	public final <V> boolean renameObject(final CmfObject<V> object, final String newName) throws CmfStorageException {
		if (object == null) { throw new IllegalArgumentException("Must provide an object to rename"); }
		if (newName == null) { throw new IllegalArgumentException("Must provide new name for the object"); }

		// Shortcut - do nothing if there's no name change
		if (Tools.equals(newName, object.getName())) { return false; }

		O operation = beginExclusiveInvocation();
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
			endExclusiveInvocation(operation);
		}
	}

	protected abstract <V> void renameObject(final O operation, final CmfObject<V> object, final String newName)
		throws CmfStorageException;

	public final int clearAttributeMappings() throws CmfStorageException {
		O operation = beginExclusiveInvocation();
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
			endExclusiveInvocation(operation);
		}
	}

	protected abstract int clearAttributeMappings(O operation) throws CmfStorageException;

	public final Map<CmfType, Set<String>> getAvailableMappings() throws CmfStorageException {
		O operation = beginConcurrentInvocation();
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
			endConcurrentInvocation(operation);
		}
	}

	protected abstract Map<CmfType, Set<String>> getAvailableMappings(O operation) throws CmfStorageException;

	public final Set<String> getAvailableMappings(CmfType type) throws CmfStorageException {
		if (type == null) { throw new IllegalArgumentException("Must provide an object type to search against"); }
		O operation = beginConcurrentInvocation();
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
			endConcurrentInvocation(operation);
		}
	}

	protected abstract Set<String> getAvailableMappings(O operation, CmfType type) throws CmfStorageException;

	public final Map<String, String> getMappings(CmfType type, String name) throws CmfStorageException {
		if (type == null) { throw new IllegalArgumentException("Must provide an object type to search against"); }
		if (name == null) { throw new IllegalArgumentException("Must provide a mapping name to search for"); }
		O operation = beginConcurrentInvocation();
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
			endConcurrentInvocation(operation);
		}
	}

	protected abstract Map<String, String> getMappings(O operation, CmfType type, String name)
		throws CmfStorageException;

	public final void clearAllObjects() throws CmfStorageException {
		O operation = beginExclusiveInvocation();
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
			endExclusiveInvocation(operation);
		}
	}

	protected abstract void clearAllObjects(O operation) throws CmfStorageException;

	public final Map<CmfType, Map<String, String>> getRenameMappings() throws CmfStorageException {
		O operation = beginConcurrentInvocation();
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
			endConcurrentInvocation(operation);
		}
	}

	protected abstract Map<CmfType, Map<String, String>> getRenameMappings(O operation) throws CmfStorageException;

	public final Collection<CmfObjectRef> getContainers(CmfObjectRef object) throws CmfStorageException {
		if (object == null) { throw new IllegalArgumentException("Must provide an object to check for"); }
		if (object.isNull()) { throw new IllegalArgumentException("Null object references are not allowed"); }
		O operation = beginConcurrentInvocation();
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
			endConcurrentInvocation(operation);
		}
	}

	protected abstract Collection<CmfObjectRef> getContainers(O operation, CmfObjectRef object)
		throws CmfStorageException;

	public final Collection<CmfObjectRef> getContainedObjects(CmfObjectRef object) throws CmfStorageException {
		if (object == null) { throw new IllegalArgumentException("Must provide an object to check for"); }
		if (object.isNull()) { throw new IllegalArgumentException("Null object references are not allowed"); }
		O operation = beginConcurrentInvocation();
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
			endConcurrentInvocation(operation);
		}
	}

	protected abstract Collection<CmfObjectRef> getContainedObjects(O operation, CmfObjectRef object)
		throws CmfStorageException;

	public final boolean addRequirement(CmfObjectRef object, CmfObjectRef requirement) throws CmfStorageException {
		if (object == null) { throw new IllegalArgumentException("Must provide an object to check for"); }
		if (object.isNull()) { throw new IllegalArgumentException("Null object references are not allowed"); }
		if (requirement == null) { throw new IllegalArgumentException(
			"Must provide a requirement to associate to the base object"); }
		if (requirement.isNull()) { throw new IllegalArgumentException("Null requirement references are not allowed"); }
		O operation = beginConcurrentInvocation();
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
			endConcurrentInvocation(operation);
		}
	}

	protected abstract boolean addRequirement(O operation, CmfObjectRef object, CmfObjectRef requirement)
		throws CmfStorageException;

	public final <T extends Enum<T>> CmfRequirementInfo<T> setImportStatus(CmfObjectRef object, T status,
		String info) throws CmfStorageException {
		if (object == null) { throw new IllegalArgumentException("Must provide an object to check for"); }
		if (object.isNull()) { throw new IllegalArgumentException("Null object references are not allowed"); }
		if (status == null) { throw new IllegalArgumentException("Must provide a non-null status"); }
		O operation = beginConcurrentInvocation();
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
			endConcurrentInvocation(operation);
		}
	}

	protected abstract <T extends Enum<T>> CmfRequirementInfo<T> setImportStatus(O operation, CmfObjectRef object,
		T status, String info) throws CmfStorageException;

	public final <T extends Enum<T>> Collection<CmfRequirementInfo<T>> getRequirementInfo(Class<T> statusClass,
		CmfObjectRef object) throws CmfStorageException {
		if (object == null) { throw new IllegalArgumentException("Must provide an object to check for"); }
		if (object.isNull()) { throw new IllegalArgumentException("Null object references are not allowed"); }
		O operation = beginConcurrentInvocation();
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
			endConcurrentInvocation(operation);
		}
	}

	protected abstract <T extends Enum<T>> Collection<CmfRequirementInfo<T>> getRequirementInfo(O operation,
		Class<T> statusClass, CmfObjectRef object) throws CmfStorageException;

	public final void clearImportPlan() throws CmfStorageException {
		O operation = beginConcurrentInvocation();
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
						this.log.warn("Failed to rollback the transaction for clearing the requirement status", e);
					}
				}
			}
		} finally {
			endConcurrentInvocation(operation);
		}
	}

	protected abstract void clearImportPlan(O operation) throws CmfStorageException;
}