/**
 *
 */

package com.armedia.cmf.storage;

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
import com.armedia.cmf.storage.tools.CollectionObjectHandler;

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
			try {
				if (this.operation == null) { return CmfObjectStore.this.createMapping(type, name, source, target); }
				O operation = CmfObjectStore.this.beginInvocation();
				try {
					CmfObjectStore.this.createMapping(this.operation, type, name, source, target);
					Mapping ret = null;
					if ((source != null) && (target != null)) {
						ret = constructMapping(type, name, source, target);
					}
					operation.commit();
					return ret;
				} finally {
					CmfObjectStore.this.endInvocation(operation);
				}
			} catch (CmfStorageException e) {
				throw new RuntimeException(String.format(
					"Exception caught attempting to get the target mapping for [%s/%s/%s]", type, name, source), e);
			}
		}

		@Override
		public Mapping getTargetMapping(CmfType type, String name, String source) {
			if (type == null) { throw new IllegalArgumentException("Must provide an object type to map for"); }
			if (name == null) { throw new IllegalArgumentException("Must provide a mapping name to map for"); }
			if (source == null) { throw new IllegalArgumentException("Must provide a source value for the mapping"); }
			try {
				if (this.operation == null) { return CmfObjectStore.this.getTargetMapping(type, name, source); }
				O operation = CmfObjectStore.this.beginInvocation();
				try {
					String target = CmfObjectStore.this.getMapping(this.operation, false, type, name, source);
					Mapping ret = null;
					if (source != null) {
						ret = constructMapping(type, name, source, target);
					}
					operation.commit();
					return ret;
				} finally {
					CmfObjectStore.this.endInvocation(operation);
				}
			} catch (CmfStorageException e) {
				throw new RuntimeException(String.format(
					"Exception caught attempting to get the target mapping for [%s/%s/%s]", type, name, source), e);
			}
		}

		@Override
		public Mapping getSourceMapping(CmfType type, String name, String target) {
			if (type == null) { throw new IllegalArgumentException("Must provide an object type to map for"); }
			if (name == null) { throw new IllegalArgumentException("Must provide a mapping name to map for"); }
			if (target == null) { throw new IllegalArgumentException("Must provide a target value for the mapping"); }
			try {
				if (this.operation == null) { return CmfObjectStore.this.getSourceMapping(type, name, target); }
				O operation = CmfObjectStore.this.beginInvocation();
				try {
					String source = CmfObjectStore.this.getMapping(this.operation, true, type, name, target);
					Mapping ret = null;
					if (source != null) {
						ret = constructMapping(type, name, source, target);
					}
					operation.commit();
					return ret;
				} finally {
					CmfObjectStore.this.endInvocation(operation);
				}
			} catch (CmfStorageException e) {
				throw new RuntimeException(String.format(
					"Exception caught attempting to get the source mapping for [%s/%s/%s]", type, name, target), e);
			}
		}

		@Override
		public Map<CmfType, Set<String>> getAvailableMappings() {
			try {
				if (this.operation == null) { return CmfObjectStore.this.getAvailableMappings(); }
				O operation = CmfObjectStore.this.beginInvocation();
				try {
					Map<CmfType, Set<String>> ret = CmfObjectStore.this.getAvailableMappings(this.operation);
					operation.commit();
					return ret;
				} finally {
					CmfObjectStore.this.endInvocation(operation);
				}
			} catch (CmfStorageException e) {
				throw new RuntimeException("Exception caught attempting to get available mappings", e);
			}
		}

		@Override
		public Set<String> getAvailableMappings(CmfType type) {
			try {
				if (this.operation == null) { return CmfObjectStore.this.getAvailableMappings(type); }
				O operation = CmfObjectStore.this.beginInvocation();
				try {
					Set<String> ret = CmfObjectStore.this.getAvailableMappings(this.operation, type);
					operation.commit();
					return ret;
				} finally {
					CmfObjectStore.this.endInvocation(operation);
				}
			} catch (CmfStorageException e) {
				throw new RuntimeException("Exception caught attempting to get available mappings", e);
			}
		}

		@Override
		public Map<String, String> getMappings(CmfType type, String name) {
			if (type == null) { throw new IllegalArgumentException(
				"Must provide an object type to find the mappings for"); }
			if (name == null) { throw new IllegalArgumentException(
				"Must provide a mapping name to find the mappings for"); }
			try {
				if (this.operation == null) { return CmfObjectStore.this.getMappings(type, name); }
				O operation = CmfObjectStore.this.beginInvocation();
				try {
					Map<String, String> ret = CmfObjectStore.this.getMappings(this.operation, type, name);
					operation.commit();
					return ret;
				} finally {
					CmfObjectStore.this.endInvocation(operation);
				}
			} catch (CmfStorageException e) {
				throw new RuntimeException(String.format(
					"Exception caught attempting to get available mappings for [%s/%s]", type, name), e);
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

	private O beginInvocation() throws CmfStorageException {
		boolean ok = true;
		try {
			getReadLock().lock();
			assertOpen();
			O ret = newOperation();
			ok = true;
			return ret;
		} finally {
			if (!ok) {
				getReadLock().unlock();
			}
		}
	}

	private void endInvocation(O operation) throws CmfStorageException {
		operation.close();
		getReadLock().unlock();
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
		if (object == null) { throw new IllegalArgumentException("Must provide an object to store"); }
		if (translator == null) { throw new IllegalArgumentException(
			"Must provide a translator for storing object values"); }
		O operation = beginInvocation();
		try {
			Long ret = storeObject(operation, object, translator);
			operation.commit();
			return ret;
		} finally {
			endInvocation(operation);
		}
	}

	protected abstract <V> Long storeObject(O operation, CmfObject<V> object, CmfAttributeTranslator<V> translator)
		throws CmfStorageException, CmfValueEncoderException;

	public final boolean isStored(CmfType type, String objectId) throws CmfStorageException {
		if (type == null) { throw new IllegalArgumentException("Must provide an object type to check for"); }
		if (objectId == null) { throw new IllegalArgumentException("Must provide an object id to check for"); }
		O operation = beginInvocation();
		try {
			boolean ret = isStored(operation, type, objectId);
			operation.commit();
			return ret;
		} finally {
			endInvocation(operation);
		}
	}

	protected abstract boolean isStored(O operation, CmfType type, String objectId) throws CmfStorageException;

	public final boolean lockForStorage(CmfType type, String objectId) throws CmfStorageException {
		if (type == null) { throw new IllegalArgumentException("Must provide an object type to check for"); }
		if (objectId == null) { throw new IllegalArgumentException("Must provide an object id to check for"); }
		O operation = beginInvocation();
		try {
			boolean ret = lockForStorage(operation, type, objectId);
			operation.commit();
			return ret;
		} finally {
			endInvocation(operation);
		}
	}

	protected abstract boolean lockForStorage(O operation, CmfType type, String objectId) throws CmfStorageException;

	public final <V> Collection<CmfObject<V>> loadObjects(final CmfTypeMapper typeMapper,
		CmfAttributeTranslator<V> translator, CmfType type, String... ids) throws CmfStorageException,
		CmfValueDecoderException {
		return loadObjects(typeMapper, translator, type, (ids != null ? Arrays.asList(ids) : null));
	}

	public final <V> Collection<CmfObject<V>> loadObjects(final CmfTypeMapper typeMapper,
		final CmfAttributeTranslator<V> translator, final CmfType type, Collection<String> ids)
		throws CmfStorageException, CmfValueDecoderException {
		O operation = beginInvocation();
		try {
			Collection<CmfObject<V>> ret = loadObjects(operation, typeMapper, translator, type, ids);
			operation.commit();
			return ret;
		} finally {
			endInvocation(operation);
		}
	}

	protected final <V> CmfObject<V> adjustLoadedObject(CmfObject<V> dataObject, CmfTypeMapper typeMapper,
		CmfAttributeTranslator<V> translator) {
		// Ensure type mapping takes place, and ensure that translation also takes place
		// TODO: This should only happen if "necessary" (i.e. target CMS is different from the
		// source)
		String altType = typeMapper.mapType(dataObject.getSubtype());
		if (altType != null) {
			dataObject = new CmfObject<V>(dataObject, altType);
		}
		return translator.decodeObject(dataObject);
	}

	protected final <V> Collection<CmfObject<V>> loadObjects(final O operation, final CmfTypeMapper typeMapper,
		final CmfAttributeTranslator<V> translator, final CmfType type, Collection<String> ids)
		throws CmfStorageException, CmfValueDecoderException {
		if (operation == null) { throw new IllegalArgumentException("Must provide an operation to work with"); }
		if (translator == null) { throw new IllegalArgumentException(
			"Must provide a translator for storing object values"); }
		if (type == null) { throw new IllegalArgumentException("Must provide an object type to retrieve"); }
		getReadLock().lock();
		try {
			Set<String> actualIds = null;
			final List<CmfObject<V>> ret = new ArrayList<CmfObject<V>>(ids.size());
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
			loadObjects(operation, translator, type, actualIds, new CollectionObjectHandler<V>(ret));
			return ret;
		} finally {
			getReadLock().unlock();
		}
	}

	public final <V> int loadObjects(final CmfTypeMapper typeMapper, CmfAttributeTranslator<V> translator,
		final CmfType type, CmfObjectHandler<V> handler) throws CmfStorageException, CmfValueDecoderException {
		return loadObjects(typeMapper, translator, type, null, handler);
	}

	public final <V> int loadObjects(final CmfTypeMapper typeMapper, final CmfAttributeTranslator<V> translator,
		final CmfType type, Collection<String> ids, final CmfObjectHandler<V> handler) throws CmfStorageException,
		CmfValueDecoderException {
		if (translator == null) { throw new IllegalArgumentException("Must provide a translator for the conversions"); }
		if (type == null) { throw new IllegalArgumentException("Must provide an object type to load"); }
		if (handler == null) { throw new IllegalArgumentException(
			"Must provide an object handler to handle the deserialized objects"); }
		O operation = beginInvocation();
		try {
			final CmfObjectHandler<V> handlerWrapper = new CmfObjectHandler<V>() {
				@Override
				public boolean handleObject(CmfObject<V> dataObject) throws CmfStorageException {
					return handler.handleObject(adjustLoadedObject(dataObject, typeMapper, translator));
				}

				@Override
				public boolean newBatch(String batchId) throws CmfStorageException {
					return handler.newBatch(batchId);
				}

				@Override
				public boolean handleException(Exception e) {
					return handler.handleException(e);
				}

				@Override
				public boolean closeBatch(boolean ok) throws CmfStorageException {
					return handler.closeBatch(ok);
				}
			};
			int ret = loadObjects(operation, translator, type, ids, handlerWrapper);
			operation.commit();
			return ret;
		} finally {
			endInvocation(operation);
		}
	}

	protected abstract <V> int loadObjects(O operation, CmfAttributeTranslator<V> translator, CmfType type,
		Collection<String> ids, CmfObjectHandler<V> handler) throws CmfStorageException, CmfValueDecoderException;

	private Mapping createMapping(CmfType type, String name, String source, String target) throws CmfStorageException {
		if (type == null) { throw new IllegalArgumentException("Must provide an object type to map for"); }
		if (name == null) { throw new IllegalArgumentException("Must provide a mapping name to map for"); }
		if ((source == null) && (target == null)) { throw new IllegalArgumentException(
			"Must provide either a source or a target value for the mapping"); }
		O operation = beginInvocation();
		try {
			createMapping(operation, type, name, source, target);
			Mapping ret = null;
			if ((source != null) && (target != null)) {
				ret = this.mapper.constructMapping(type, name, source, target);
			}
			operation.commit();
			return ret;
		} finally {
			endInvocation(operation);
		}
	}

	protected abstract void createMapping(O operation, CmfType type, String name, String source, String target)
		throws CmfStorageException;

	protected abstract String getMapping(O operation, boolean source, CmfType type, String name, String value)
		throws CmfStorageException;

	public final Mapping getTargetMapping(CmfType type, String name, String source) throws CmfStorageException {
		if (type == null) { throw new IllegalArgumentException("Must provide an object type to search against"); }
		if (name == null) { throw new IllegalArgumentException("Must provide a mapping name to search for"); }
		if (source == null) { throw new IllegalArgumentException(
			"Must provide a source value to find the target mapping for"); }
		O operation = beginInvocation();
		try {
			String target = getMapping(operation, true, type, name, source);
			Mapping ret = null;
			if (target != null) {
				ret = this.mapper.constructMapping(type, name, source, target);
			}
			operation.commit();
			return ret;
		} finally {
			endInvocation(operation);
		}
	}

	public final Mapping getSourceMapping(CmfType type, String name, String target) throws CmfStorageException {
		if (type == null) { throw new IllegalArgumentException("Must provide an object type to search against"); }
		if (name == null) { throw new IllegalArgumentException("Must provide a mapping name to search for"); }
		if (target == null) { throw new IllegalArgumentException(
			"Must provide a target value to find the target mapping for"); }
		O operation = beginInvocation();
		try {
			String source = getMapping(operation, false, type, name, target);
			Mapping ret = null;
			if (target != null) {
				ret = this.mapper.constructMapping(type, name, source, target);
			}
			operation.commit();
			return ret;
		} finally {
			endInvocation(operation);
		}
	}

	public final Map<CmfType, Integer> getStoredObjectTypes() throws CmfStorageException {
		O operation = beginInvocation();
		try {
			Map<CmfType, Integer> ret = getStoredObjectTypes(operation);
			operation.commit();
			return ret;
		} finally {
			endInvocation(operation);
		}
	}

	protected abstract Map<CmfType, Integer> getStoredObjectTypes(O operation) throws CmfStorageException;

	public final CmfAttributeMapper getAttributeMapper() {
		return this.mapper;
	}

	protected final CmfAttributeMapper getAttributeMapper(O operation) {
		return new Mapper(operation);
	}

	public final int clearAttributeMappings() throws CmfStorageException {
		O operation = beginInvocation();
		try {
			int ret = clearAttributeMappings(operation);
			operation.commit();
			return ret;
		} finally {
			endInvocation(operation);
		}
	}

	protected abstract int clearAttributeMappings(O operation) throws CmfStorageException;

	public final Map<CmfType, Set<String>> getAvailableMappings() throws CmfStorageException {
		O operation = beginInvocation();
		try {
			Map<CmfType, Set<String>> ret = getAvailableMappings(operation);
			operation.commit();
			return ret;
		} finally {
			endInvocation(operation);
		}
	}

	protected abstract Map<CmfType, Set<String>> getAvailableMappings(O operation) throws CmfStorageException;

	public final Set<String> getAvailableMappings(CmfType type) throws CmfStorageException {
		if (type == null) { throw new IllegalArgumentException("Must provide an object type to search against"); }
		O operation = beginInvocation();
		try {
			Set<String> ret = getAvailableMappings(operation, type);
			operation.commit();
			return ret;
		} finally {
			endInvocation(operation);
		}
	}

	protected abstract Set<String> getAvailableMappings(O operation, CmfType type) throws CmfStorageException;

	public final Map<String, String> getMappings(CmfType type, String name) throws CmfStorageException {
		if (type == null) { throw new IllegalArgumentException("Must provide an object type to search against"); }
		if (name == null) { throw new IllegalArgumentException("Must provide a mapping name to search for"); }
		O operation = beginInvocation();
		try {
			Map<String, String> ret = getMappings(operation, type, name);
			operation.commit();
			return ret;
		} finally {
			endInvocation(operation);
		}
	}

	protected abstract Map<String, String> getMappings(O operation, CmfType type, String name)
		throws CmfStorageException;

	public final void clearAllObjects() throws CmfStorageException {
		O operation = beginInvocation();
		try {
			clearAllObjects(operation);
			operation.commit();
		} finally {
			endInvocation(operation);
		}
	}

	protected abstract void clearAllObjects(O operation) throws CmfStorageException;

	@Override
	public final void clearProperties() throws CmfStorageException {
		O operation = beginInvocation();
		try {
			clearProperties(operation);
			operation.commit();
		} finally {
			endInvocation(operation);
		}
	}

	protected abstract void clearProperties(O operation) throws CmfStorageException;

	@Override
	protected final CmfValue doGetProperty(String property) throws CmfStorageException {
		O operation = beginInvocation();
		try {
			CmfValue ret = getProperty(operation, property);
			operation.commit();
			return ret;
		} finally {
			endInvocation(operation);
		}
	}

	protected abstract CmfValue getProperty(O operation, String property) throws CmfStorageException;

	@Override
	protected final CmfValue doSetProperty(String property, CmfValue value) throws CmfStorageException {
		O operation = beginInvocation();
		try {
			CmfValue ret = setProperty(operation, property, value);
			operation.commit();
			return ret;
		} finally {
			endInvocation(operation);
		}
	}

	protected abstract CmfValue setProperty(O operation, String property, CmfValue value) throws CmfStorageException;

	@Override
	public final Set<String> getPropertyNames() throws CmfStorageException {
		O operation = beginInvocation();
		try {
			Set<String> ret = getPropertyNames(operation);
			operation.commit();
			return ret;
		} finally {
			endInvocation(operation);
		}
	}

	protected abstract Set<String> getPropertyNames(O operation) throws CmfStorageException;

	@Override
	protected final CmfValue doClearProperty(String property) throws CmfStorageException {
		O operation = beginInvocation();
		try {
			CmfValue ret = clearProperty(operation, property);
			operation.commit();
			return ret;
		} finally {
			endInvocation(operation);
		}
	}

	protected abstract CmfValue clearProperty(O operation, String property) throws CmfStorageException;
}