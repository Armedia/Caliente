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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.cmf.storage.CmfAttributeMapper.Mapping;

/**
 * @author Diego Rivera &lt;diego.rivera@armedia.com&gt;
 *
 */
public abstract class CmfObjectStore<C, O extends CmfStoreOperation<C>> extends CmfStore {

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

			O o = this.operation;
			boolean newOperation = false;
			if (o == null) {
				newOperation = true;
				try {
					o = newOperation();
				} catch (CmfStorageException e) {
					throw new RuntimeException("Failed to initialize an operation to create the mapping");
				}
			}

			boolean ok = false;
			try {
				CmfObjectStore.this.createMapping(o, type, name, source, target);
				ok = true;
			} catch (CmfStorageException e) {
				throw new RuntimeException(String.format("Failed to create the mapping for [%s::%s(%s->%s)]", type,
					name, source, target), e);
			} finally {
				if (newOperation) {
					try {
						o.close(ok);
					} catch (CmfStorageException e) {
						throw new RuntimeException("Failed to complete the operation", e);
					}
				}
			}
			if ((source == null) || (target == null)) { return null; }
			return constructMapping(type, name, source, target);
		}

		@Override
		public Mapping getTargetMapping(CmfType type, String name, String source) {
			if (type == null) { throw new IllegalArgumentException("Must provide an object type to map for"); }
			if (name == null) { throw new IllegalArgumentException("Must provide a mapping name to map for"); }
			if (source == null) { throw new IllegalArgumentException("Must provide a source value for the mapping"); }
			O o = this.operation;
			boolean newOperation = false;
			if (o == null) {
				newOperation = true;
				try {
					o = newOperation();
				} catch (CmfStorageException e) {
					throw new RuntimeException("Failed to initialize an operation to create the mapping");
				}
			}
			try {
				return CmfObjectStore.this.getTargetMapping(o, type, name, source);
			} catch (CmfStorageException e) {
				throw new RuntimeException(String.format("Failed to retrieve the target mapping for [%s::%s(%s->?)]",
					type, name, source), e);
			} finally {
				if (newOperation) {
					try {
						o.close();
					} catch (CmfStorageException e) {
						throw new RuntimeException("Failed to complete the operation", e);
					}
				}
			}
		}

		@Override
		public Mapping getSourceMapping(CmfType type, String name, String target) {
			if (type == null) { throw new IllegalArgumentException("Must provide an object type to map for"); }
			if (name == null) { throw new IllegalArgumentException("Must provide a mapping name to map for"); }
			if (target == null) { throw new IllegalArgumentException("Must provide a target value for the mapping"); }
			O o = this.operation;
			boolean newOperation = false;
			if (o == null) {
				newOperation = true;
				try {
					o = newOperation();
				} catch (CmfStorageException e) {
					throw new RuntimeException("Failed to initialize an operation to create the mapping");
				}
			}
			try {
				return CmfObjectStore.this.getSourceMapping(o, type, name, target);
			} catch (CmfStorageException e) {
				throw new RuntimeException(String.format("Failed to create the source mapping for [%s::%s(?->%s)]",
					type, name, target), e);
			} finally {
				if (newOperation) {
					try {
						o.close();
					} catch (CmfStorageException e) {
						throw new RuntimeException("Failed to complete the operation", e);
					}
				}
			}
		}

		@Override
		public Map<CmfType, Set<String>> getAvailableMappings() {
			O o = this.operation;
			boolean newOperation = false;
			if (o == null) {
				newOperation = true;
				try {
					o = newOperation();
				} catch (CmfStorageException e) {
					throw new RuntimeException("Failed to initialize an operation to create the mapping");
				}
			}
			try {
				return CmfObjectStore.this.getAvailableMappings(o);
			} catch (CmfStorageException e) {
				throw new RuntimeException("Failed to retrieve the mapping names in the system", e);
			} finally {
				if (newOperation) {
					try {
						o.close();
					} catch (CmfStorageException e) {
						throw new RuntimeException("Failed to complete the operation", e);
					}
				}
			}
		}

		@Override
		public Set<String> getAvailableMappings(CmfType type) {
			if (type == null) { throw new IllegalArgumentException(
				"Must provide an object type to find the mappings for"); }
			O o = this.operation;
			boolean newOperation = false;
			if (o == null) {
				newOperation = true;
				try {
					o = newOperation();
				} catch (CmfStorageException e) {
					throw new RuntimeException("Failed to initialize an operation to create the mapping");
				}
			}
			try {
				return CmfObjectStore.this.getAvailableMappings(o, type);
			} catch (CmfStorageException e) {
				throw new RuntimeException(String.format("Failed to retrieve the mapping names in the system for [%s]",
					type), e);
			} finally {
				if (newOperation) {
					try {
						o.close();
					} catch (CmfStorageException e) {
						throw new RuntimeException("Failed to complete the operation", e);
					}
				}
			}
		}

		@Override
		public Map<String, String> getMappings(CmfType type, String name) {
			if (type == null) { throw new IllegalArgumentException(
				"Must provide an object type to find the mappings for"); }
			if (name == null) { throw new IllegalArgumentException(
				"Must provide a mapping name to find the mappings for"); }
			O o = this.operation;
			boolean newOperation = false;
			if (o == null) {
				newOperation = true;
				try {
					o = newOperation();
				} catch (CmfStorageException e) {
					throw new RuntimeException("Failed to initialize an operation to create the mapping");
				}
			}
			try {
				return CmfObjectStore.this.getMappings(o, type, name);
			} catch (CmfStorageException e) {
				throw new RuntimeException(String.format("Failed to retrieves the mappings in the system for [%s::%s]",
					type, name), e);
			} finally {
				if (newOperation) {
					try {
						o.close();
					} catch (CmfStorageException e) {
						throw new RuntimeException("Failed to complete the operation", e);
					}
				}
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

	protected abstract O newOperation() throws CmfStorageException;

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
		throws CmfStorageException, CmfValueEncoderException {
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

	public final <V> Long storeObject(CmfStoreOperation<?> operation, CmfObject<V> object,
		CmfAttributeTranslator<V> translator) throws CmfStorageException, CmfValueEncoderException {
		O o = castOperation(operation);
		if (object == null) { throw new IllegalArgumentException("Must provide an object to store"); }
		if (translator == null) { throw new IllegalArgumentException(
			"Must provide a translator for storing object values"); }
		getReadLock().lock();
		try {
			assertOpen();
			return doStoreObject(o, translator.encodeObject(object), translator);
		} finally {
			getReadLock().unlock();
		}
	}

	protected abstract <V> Long doStoreObject(O operation, CmfObject<V> object, CmfAttributeTranslator<V> translator)
		throws CmfStorageException, CmfValueEncoderException;

	public final boolean isStored(CmfType type, String objectId) throws CmfStorageException {
		assertOpen();
		O operation = newOperation();
		try {
			return isStored(operation, type, objectId);
		} finally {
			operation.close();
		}
	}

	public final boolean isStored(CmfStoreOperation<?> operation, CmfType type, String objectId)
		throws CmfStorageException {
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

	protected abstract boolean doIsStored(O operation, CmfType type, String objectId) throws CmfStorageException;

	public final boolean lockForStorage(CmfType type, String objectId) throws CmfStorageException {
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

	public final boolean lockForStorage(CmfStoreOperation<?> operation, CmfType type, String objectId)
		throws CmfStorageException {
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

	protected abstract boolean doLockForStorage(O operation, CmfType type, String objectId)
		throws CmfStorageException;

	public final <V> Collection<CmfObject<V>> loadObjects(CmfAttributeTranslator<V> translator,
		final CmfType type, String... ids) throws CmfStorageException, CmfValueDecoderException {
		assertOpen();
		O operation = newOperation();
		boolean ok = false;
		try {
			Collection<CmfObject<V>> ret = loadObjects(operation, translator, type, ids);
			ok = true;
			return ret;
		} finally {
			operation.close(ok);
		}
	}

	public final <V> Collection<CmfObject<V>> loadObjects(CmfStoreOperation<?> operation,
		CmfAttributeTranslator<V> translator, final CmfType type, String... ids) throws CmfStorageException,
		CmfValueDecoderException {
		getReadLock().lock();
		try {
			assertOpen();
			return loadObjects(operation, translator, type, (ids != null ? Arrays.asList(ids) : null));
		} finally {
			getReadLock().unlock();
		}
	}

	public final <V> Collection<CmfObject<V>> loadObjects(CmfAttributeTranslator<V> translator,
		final CmfType type, Collection<String> ids) throws CmfStorageException, CmfValueDecoderException {
		assertOpen();
		O operation = newOperation();
		boolean ok = false;
		try {
			Collection<CmfObject<V>> ret = loadObjects(operation, translator, type, ids);
			ok = true;
			return ret;
		} finally {
			operation.close(ok);
		}
	}

	public final <V> Collection<CmfObject<V>> loadObjects(CmfStoreOperation<?> operation,
		final CmfAttributeTranslator<V> translator, final CmfType type, Collection<String> ids)
		throws CmfStorageException, CmfValueDecoderException {
		if (operation == null) { throw new IllegalArgumentException("Must proved an operation to work under"); }
		if (type == null) { throw new IllegalArgumentException("Must provide an object type to retrieve"); }
		if (translator == null) { throw new IllegalArgumentException(
			"Must provide a translator for storing object values"); }
		getReadLock().lock();
		try {
			assertOpen();
			final List<CmfObject<V>> ret = new ArrayList<CmfObject<V>>(ids.size());
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
			final CmfObjectHandler<V> handler = new CmfObjectHandler<V>() {
				@Override
				public boolean newBatch(String batchId) throws CmfStorageException {
					return true;
				}

				@Override
				public boolean handleObject(CmfObject<V> dataObject) throws CmfStorageException {
					ret.add(translator.decodeObject(dataObject));
					return true;
				}

				@Override
				public boolean handleException(SQLException e) {
					return false;
				}

				@Override
				public boolean closeBatch(boolean ok) throws CmfStorageException {
					return true;
				}
			};
			loadObjects(operation, translator, type, actualIds, handler);
			return ret;
		} finally {
			getReadLock().unlock();
		}
	}

	public final <V> int loadObjects(CmfAttributeTranslator<V> translator, final CmfType type,
		CmfObjectHandler<V> handler) throws CmfStorageException, CmfValueDecoderException {
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

	public final <V> int loadObjects(CmfStoreOperation<?> operation, CmfAttributeTranslator<V> translator,
		final CmfType type, CmfObjectHandler<V> handler) throws CmfStorageException,
		CmfValueDecoderException {
		return loadObjects(operation, translator, type, null, handler);
	}

	public final <V> int loadObjects(CmfAttributeTranslator<V> translator, final CmfType type,
		Collection<String> ids, CmfObjectHandler<V> handler) throws CmfStorageException, CmfValueDecoderException {
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

	public final <V> int loadObjects(CmfStoreOperation<?> operation, CmfAttributeTranslator<V> translator,
		final CmfType type, Collection<String> ids, CmfObjectHandler<V> handler) throws CmfStorageException,
		CmfValueDecoderException {
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

	protected abstract <V> int doLoadObjects(O operation, CmfAttributeTranslator<V> translator, CmfType type,
		Collection<String> ids, CmfObjectHandler<V> handler) throws CmfStorageException, CmfValueDecoderException;

	private void createMapping(CmfStoreOperation<?> operation, CmfType type, String name, String source,
		String target) throws CmfStorageException {
		if (type == null) { throw new IllegalArgumentException("Must provide an object type to map for"); }
		if (name == null) { throw new IllegalArgumentException("Must provide a mapping name to map for"); }
		if ((source == null) && (target == null)) { throw new IllegalArgumentException(
			"Must provide either a source or a target value for the mapping"); }
		O o = castOperation(operation);
		getReadLock().lock();
		try {
			assertOpen();
			doCreateMappedValue(o, type, name, source, target);
		} finally {
			getReadLock().unlock();
		}
	}

	protected abstract void doCreateMappedValue(O operation, CmfType type, String name, String source,
		String target) throws CmfStorageException;

	protected final String getMappedValue(CmfStoreOperation<?> operation, boolean source, CmfType type,
		String name, String value) throws CmfStorageException {
		O o = castOperation(operation);
		getReadLock().lock();
		try {
			assertOpen();
			return doGetMappedValue(o, source, type, name, value);
		} finally {
			getReadLock().unlock();
		}
	}

	protected abstract String doGetMappedValue(O operation, boolean source, CmfType type, String name,
		String value) throws CmfStorageException;

	public final Mapping getTargetMapping(CmfType type, String name, String target) throws CmfStorageException {
		assertOpen();
		O operation = newOperation();
		try {
			return getTargetMapping(operation, type, name, target);
		} finally {
			operation.close();
		}
	}

	public final Mapping getTargetMapping(CmfStoreOperation<?> operation, CmfType type, String name,
		String source) throws CmfStorageException {
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

	public final Mapping getSourceMapping(CmfType type, String name, String target) throws CmfStorageException {
		assertOpen();
		O operation = newOperation();
		try {
			return getSourceMapping(operation, type, name, target);
		} finally {
			operation.close();
		}
	}

	public final Mapping getSourceMapping(CmfStoreOperation<?> operation, CmfType type, String name,
		String target) throws CmfStorageException {
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

	public final Map<CmfType, Integer> getStoredObjectTypes() throws CmfStorageException {
		assertOpen();
		O operation = newOperation();
		try {
			return getStoredObjectTypes(operation);
		} finally {
			operation.close();
		}
	}

	public final Map<CmfType, Integer> getStoredObjectTypes(CmfStoreOperation<?> operation)
		throws CmfStorageException {
		getReadLock().lock();
		try {
			assertOpen();
			return doGetStoredObjectTypes(castOperation(operation));
		} finally {
			getReadLock().unlock();
		}

	}

	protected abstract Map<CmfType, Integer> doGetStoredObjectTypes(O operation) throws CmfStorageException;

	public final CmfAttributeMapper getAttributeMapper() {
		return this.mapper;
	}

	public final CmfAttributeMapper getAttributeMapper(CmfStoreOperation<?> operation) {
		return new Mapper(castOperation(operation));
	}

	public final int clearAttributeMappings() throws CmfStorageException {
		assertOpen();
		O operation = newOperation();
		try {
			return clearAttributeMappings(operation);
		} finally {
			operation.close();
		}
	}

	public final int clearAttributeMappings(CmfStoreOperation<?> operation) throws CmfStorageException {
		O o = castOperation(operation);
		getReadLock().lock();
		try {
			assertOpen();
			return doClearAttributeMappings(o);
		} finally {
			getReadLock().unlock();
		}
	}

	protected abstract int doClearAttributeMappings(O operation) throws CmfStorageException;

	public final Map<CmfType, Set<String>> getAvailableMappings() throws CmfStorageException {
		assertOpen();
		O operation = newOperation();
		try {
			return getAvailableMappings(operation);
		} finally {
			operation.close();
		}
	}

	public final Map<CmfType, Set<String>> getAvailableMappings(CmfStoreOperation<?> operation)
		throws CmfStorageException {
		O o = castOperation(operation);
		getReadLock().lock();
		try {
			assertOpen();
			return doGetAvailableMappings(o);
		} finally {
			getReadLock().unlock();
		}
	}

	protected abstract Map<CmfType, Set<String>> doGetAvailableMappings(O operation) throws CmfStorageException;

	public final Set<String> getAvailableMappings(CmfType type) throws CmfStorageException {
		assertOpen();
		O operation = newOperation();
		try {
			return getAvailableMappings(operation, type);
		} finally {
			operation.close();
		}
	}

	public final Set<String> getAvailableMappings(CmfStoreOperation<?> operation, CmfType type)
		throws CmfStorageException {
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

	protected abstract Set<String> doGetAvailableMappings(O operation, CmfType type) throws CmfStorageException;

	public final Map<String, String> getMappings(CmfType type, String name) throws CmfStorageException {
		assertOpen();
		O operation = newOperation();
		try {
			return getMappings(operation, type, name);
		} finally {
			operation.close();
		}
	}

	public final Map<String, String> getMappings(CmfStoreOperation<?> operation, CmfType type, String name)
		throws CmfStorageException {
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

	protected abstract Map<String, String> doGetMappings(O operation, CmfType type, String name)
		throws CmfStorageException;

	public final void clearAllObjects() throws CmfStorageException {
		assertOpen();
		O operation = newOperation();
		try {
			clearAllObjects(operation);
		} finally {
			operation.commit();
		}
	}

	public final void clearAllObjects(CmfStoreOperation<?> operation) throws CmfStorageException {
		O o = castOperation(operation);
		getReadLock().lock();
		try {
			assertOpen();
			doClearAllObjects(o);
		} finally {
			getReadLock().unlock();
		}
	}

	protected abstract void doClearAllObjects(O operation) throws CmfStorageException;

	@Override
	public final void clearProperties() throws CmfStorageException {
		assertOpen();
		O operation = newOperation();
		try {
			doClearProperties(operation);
		} finally {
			operation.commit();
		}
	}

	public final void clearProperties(CmfStoreOperation<?> operation) throws CmfStorageException {
		O o = castOperation(operation);
		getReadLock().lock();
		try {
			assertOpen();
			doClearProperties(o);
		} finally {
			getReadLock().unlock();
		}
	}

	protected abstract void doClearProperties(O operation) throws CmfStorageException;

	public final CmfValue getProperty(CmfStoreOperation<?> operation, String property) throws CmfStorageException {
		O o = castOperation(operation);
		getReadLock().lock();
		try {
			assertOpen();
			return doGetProperty(o, property);
		} finally {
			getReadLock().unlock();
		}
	}

	@Override
	protected final CmfValue doGetProperty(String property) throws CmfStorageException {
		assertOpen();
		O operation = newOperation();
		try {
			return doGetProperty(operation, property);
		} finally {
			operation.close();
		}
	}

	protected abstract CmfValue doGetProperty(O operation, String property) throws CmfStorageException;

	public final CmfValue setProperty(CmfStoreOperation<?> operation, String property, CmfValue value)
		throws CmfStorageException {
		O o = castOperation(operation);
		getReadLock().lock();
		try {
			assertOpen();
			return doSetProperty(o, property, value);
		} finally {
			getReadLock().unlock();
		}
	}

	@Override
	protected final CmfValue doSetProperty(String property, CmfValue value) throws CmfStorageException {
		assertOpen();
		O operation = newOperation();
		try {
			return doSetProperty(operation, property, value);
		} finally {
			operation.close();
		}
	}

	protected abstract CmfValue doSetProperty(O operation, String property, CmfValue value)
		throws CmfStorageException;

	@Override
	public final Set<String> getPropertyNames() throws CmfStorageException {
		assertOpen();
		O operation = newOperation();
		try {
			return doGetPropertyNames(operation);
		} finally {
			operation.close();
		}
	}

	public final Set<String> getPropertyNames(CmfStoreOperation<?> operation) throws CmfStorageException {
		O o = castOperation(operation);
		getReadLock().lock();
		try {
			assertOpen();
			return doGetPropertyNames(o);
		} finally {
			getReadLock().unlock();
		}
	}

	protected abstract Set<String> doGetPropertyNames(O operation) throws CmfStorageException;

	public final CmfValue clearProperty(CmfStoreOperation<?> operation, String property) throws CmfStorageException {
		O o = castOperation(operation);
		getReadLock().lock();
		try {
			assertOpen();
			return doClearProperty(o, property);
		} finally {
			getReadLock().unlock();
		}
	}

	@Override
	protected final CmfValue doClearProperty(String property) throws CmfStorageException {
		assertOpen();
		O operation = newOperation();
		try {
			return doClearProperty(operation, property);
		} finally {
			operation.close();
		}
	}

	protected abstract CmfValue doClearProperty(O operation, String property) throws CmfStorageException;
}