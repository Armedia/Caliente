/**
 *
 */

package com.armedia.cmf.storage;

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

import com.armedia.cmf.storage.CmfAttributeMapper.Mapping;
import com.armedia.cmf.storage.tools.CollectionObjectHandler;
import com.armedia.commons.utilities.Tools;

/**
 * @author Diego Rivera &lt;diego.rivera@armedia.com&gt;
 *
 */
public abstract class CmfObjectStore<C, O extends CmfStoreOperation<C>> extends CmfStore<C, O> {

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

	public final boolean isStored(CmfType type, String objectId) throws CmfStorageException {
		if (type == null) { throw new IllegalArgumentException("Must provide an object type to check for"); }
		if (objectId == null) { throw new IllegalArgumentException("Must provide an object id to check for"); }
		O operation = beginConcurrentInvocation();
		try {
			final boolean tx = operation.begin();
			try {
				return isStored(operation, type, objectId);
			} finally {
				if (tx) {
					try {
						operation.rollback();
					} catch (CmfStorageException e) {
						this.log.warn(String.format("Failed to rollback the transaction for %s (%s)", type, objectId),
							e);
					}
				}
			}
		} finally {
			endConcurrentInvocation(operation);
		}
	}

	protected abstract boolean isStored(O operation, CmfType type, String objectId) throws CmfStorageException;

	public final boolean lockForStorage(CmfType type, String objectId) throws CmfStorageException {
		if (type == null) { throw new IllegalArgumentException("Must provide an object type to check for"); }
		if (objectId == null) { throw new IllegalArgumentException("Must provide an object id to check for"); }
		O operation = beginConcurrentInvocation();
		try {
			final boolean tx = operation.begin();
			boolean ok = false;
			try {
				boolean ret = lockForStorage(operation, type, objectId);
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
						this.log.warn(String.format("Failed to rollback the transaction for %s (%s)", type, objectId),
							e);
					}
				}
			}
		} finally {
			endConcurrentInvocation(operation);
		}
	}

	protected abstract boolean lockForStorage(O operation, CmfType type, String objectId) throws CmfStorageException;

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

	public final <V> Collection<CmfObject<V>> loadObjects(final CmfTypeMapper typeMapper,
		CmfAttributeTranslator<V> translator, CmfType type, boolean batching, String... ids)
		throws CmfStorageException {
		return loadObjects(typeMapper, translator, type, (ids != null ? Arrays.asList(ids) : null), batching);
	}

	public final <V> Collection<CmfObject<V>> loadObjects(final CmfTypeMapper typeMapper,
		final CmfAttributeTranslator<V> translator, final CmfType type, Collection<String> ids, boolean batching)
		throws CmfStorageException {
		O operation = beginConcurrentInvocation();
		try {
			final boolean tx = operation.begin();
			try {
				return loadObjects(operation, typeMapper, translator, type, ids, batching);
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
		final CmfAttributeTranslator<V> translator, final CmfType type, Collection<String> ids, boolean batching)
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
			final List<CmfObject<V>> ret = new ArrayList<CmfObject<V>>(ids.size());
			if (ids.isEmpty()) { return ret; }
			actualIds = new HashSet<String>();
			for (String s : ids) {
				if (s == null) {
					continue;
				}
				actualIds.add(s);
			}
			final CmfObjectHandler<V> h = new CollectionObjectHandler<V>(ret);
			loadObjects(operation, translator, type, actualIds, new CmfObjectHandler<V>() {
				@Override
				public boolean handleObject(CmfObject<V> dataObject) throws CmfStorageException {
					return h.handleObject(adjustLoadedObject(dataObject, typeMapper, translator));
				}

				@Override
				public boolean newBatch(String batchId) throws CmfStorageException {
					return h.newBatch(batchId);
				}

				@Override
				public boolean handleException(Exception e) {
					return h.handleException(e);
				}

				@Override
				public boolean closeBatch(boolean ok) throws CmfStorageException {
					return h.closeBatch(ok);
				}
			}, batching);
			return ret;
		} finally {
			getReadLock().unlock();
		}
	}

	public final <V> int loadObjects(final CmfTypeMapper typeMapper, CmfAttributeTranslator<V> translator,
		final CmfType type, CmfObjectHandler<V> handler, boolean batching) throws CmfStorageException {
		return loadObjects(typeMapper, translator, type, null, handler, batching);
	}

	public final <V> int loadObjects(final CmfTypeMapper typeMapper, final CmfAttributeTranslator<V> translator,
		final CmfType type, Collection<String> ids, final CmfObjectHandler<V> handler, boolean batching)
		throws CmfStorageException {
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
				}, batching);
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
		Collection<String> ids, CmfObjectHandler<V> handler, boolean batching) throws CmfStorageException;

	public final <V> Collection<CmfObject<V>> getObjectsWithFileNameCollisions(
		final CmfAttributeTranslator<V> translator) throws CmfStorageException {
		if (translator == null) { throw new IllegalArgumentException("Must provide a translator for the conversions"); }
		O operation = beginConcurrentInvocation();
		try {
			final boolean tx = operation.begin();
			try {
				return getObjectsWithFileNameCollisions(operation, translator);
			} finally {
				if (tx) {
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

	protected abstract <V> Collection<CmfObject<V>> getObjectsWithFileNameCollisions(O operation,
		final CmfAttributeTranslator<V> translator) throws CmfStorageException;

	public final <V> int fixObjectNames(final CmfAttributeTranslator<V> translator, final CmfNameFixer<V> nameFixer)
		throws CmfStorageException {
		if (translator == null) { throw new IllegalArgumentException("Must provide a translator for the conversions"); }
		if (nameFixer == null) { throw new IllegalArgumentException(
			"Must provide name fixer to fix the object names"); }
		O operation = beginConcurrentInvocation();
		boolean ok = false;
		try {
			final boolean tx = operation.begin();
			try {
				int ret = fixObjectNames(operation, translator, nameFixer);
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
		CmfNameFixer<V> nameFixer) throws CmfStorageException;

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

	public final Map<CmfType, Integer> getStoredObjectTypes() throws CmfStorageException {
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

	protected abstract Map<CmfType, Integer> getStoredObjectTypes(O operation) throws CmfStorageException;

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

	public final <V> void renameObject(final CmfObject<V> object, final String newName) throws CmfStorageException {
		if (object == null) { throw new IllegalArgumentException("Must provide an object to rename"); }
		if (newName == null) { throw new IllegalArgumentException("Must provide new name for the object"); }

		// Shortcut - do nothing if there's no name change
		if (Tools.equals(newName, object.getName())) { return; }

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
}