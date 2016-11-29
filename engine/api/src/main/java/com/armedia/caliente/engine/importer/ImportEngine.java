/**
 *
 */

package com.armedia.caliente.engine.importer;

import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.caliente.engine.CmfCrypt;
import com.armedia.caliente.engine.SessionFactory;
import com.armedia.caliente.engine.SessionWrapper;
import com.armedia.caliente.engine.TransferEngine;
import com.armedia.caliente.engine.TransferEngineSetting;
import com.armedia.caliente.engine.tools.MappingTools;
import com.armedia.caliente.store.CmfAttributeTranslator;
import com.armedia.caliente.store.CmfContentStore;
import com.armedia.caliente.store.CmfNameFixer;
import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.store.CmfObjectCounter;
import com.armedia.caliente.store.CmfObjectHandler;
import com.armedia.caliente.store.CmfObjectStore;
import com.armedia.caliente.store.CmfStorageException;
import com.armedia.caliente.store.CmfType;
import com.armedia.caliente.store.CmfTypeMapper;
import com.armedia.commons.utilities.CfgTools;
import com.armedia.commons.utilities.SynchronizedCounter;
import com.armedia.commons.utilities.Tools;

/**
 * @author diego
 *
 */
public abstract class ImportEngine<S, W extends SessionWrapper<S>, V, C extends ImportContext<S, V, CF>, CF extends ImportContextFactory<S, W, V, C, ?, ?>, DF extends ImportDelegateFactory<S, W, V, C, ?>>
	extends TransferEngine<S, V, C, CF, DF, ImportEngineListener> {

	private class BatchWorker implements Callable<Map<String, Collection<ImportOutcome>>> {

		private final SynchronizedCounter workerCounter;

		private final Logger log = LoggerFactory.getLogger(getClass());
		private final SessionFactory<S> sessionFactory;
		private final ImportEngineListener listenerDelegator;
		private final ImportState importState;
		private final Batch batch;
		private final ImportContextFactory<S, W, V, C, ?, ?> contextFactory;
		private final ImportDelegateFactory<S, W, V, C, ?> delegateFactory;
		private final CmfTypeMapper typeMapper;

		private BatchWorker(Batch batch, SynchronizedCounter synchronizedCounter, SessionFactory<S> sessionFactory,
			ImportEngineListener listenerDelegator, ImportState importState,
			final ImportContextFactory<S, W, V, C, ?, ?> contextFactory,
			final ImportDelegateFactory<S, W, V, C, ?> delegateFactory, final CmfTypeMapper typeMapper) {
			this.sessionFactory = sessionFactory;
			this.listenerDelegator = listenerDelegator;
			this.importState = importState;
			this.batch = batch;
			this.contextFactory = contextFactory;
			this.delegateFactory = delegateFactory;
			this.typeMapper = typeMapper;
			this.workerCounter = synchronizedCounter;
			this.workerCounter.increment();
		}

		@Override
		public Map<String, Collection<ImportOutcome>> call() throws Exception {
			try {
				boolean failBatch = false;
				final Map<String, Collection<ImportOutcome>> outcomes = new LinkedHashMap<>(this.batch.contents.size());
				try {
					if (this.batch.contents.isEmpty()) {
						// Shouldn't happen, but still
						this.log.warn(
							String.format("An invalid value made it into the work queue somehow: %s", this.batch));
						this.batch.markCompleted();
						return null;
					}

					if (this.log.isDebugEnabled()) {
						this.log.debug(String.format("Polled a batch with %d items", this.batch.contents.size()));
					}

					final SessionWrapper<S> session;
					try {
						session = this.sessionFactory.acquireSession();
					} catch (Exception e) {
						this.log.error("Failed to obtain a worker session", e);
						this.batch.markAborted(e);
						return null;
					}

					try {
						if (this.log.isDebugEnabled()) {
							this.log.debug(String.format("Got session [%s]", session.getId()));
						}

						this.listenerDelegator.objectHistoryImportStarted(this.importState.jobId, this.batch.type,
							this.batch.id, this.batch.contents.size());
						int i = 0;
						for (CmfObject<V> next : this.batch.contents) {
							if (failBatch) {
								this.log.error(String.format("Batch has been failed - will not process [%s](%s) (%s)",
									next.getLabel(), next.getId(), ImportResult.SKIPPED.name()));
								this.listenerDelegator.objectImportCompleted(this.importState.jobId, next,
									ImportOutcome.SKIPPED);
								continue;
							}

							final C ctx = this.contextFactory.newContext(next.getId(), next.getType(),
								session.getWrapped(), this.importState.output, this.importState.objectStore,
								this.importState.streamStore, this.typeMapper, i);
							try {
								initContext(ctx);
								final CmfType storedType = next.getType();
								final boolean useTx = getImportStrategy(storedType).isSupportsTransactions();
								if (useTx) {
									session.begin();
								}
								try {
									this.listenerDelegator.objectImportStarted(this.importState.jobId, next);
									// TODO: Transform the loaded object from the intermediate
									// format into the target format
									ImportDelegate<?, S, W, V, C, ?, ?> delegate = this.delegateFactory
										.newImportDelegate(next);
									final Collection<ImportOutcome> outcome = delegate.importObject(getTranslator(),
										ctx);
									outcomes.put(next.getId(), outcome);
									for (ImportOutcome o : outcome) {
										this.listenerDelegator.objectImportCompleted(this.importState.jobId, next, o);
										if (this.log.isDebugEnabled()) {
											String msg = null;
											switch (o.getResult()) {
												case CREATED:
												case UPDATED:
													msg = String.format("Persisted (%s) %s as [%s](%s)", o.getResult(),
														next, o.getNewId(), o.getNewLabel());
													break;

												case DUPLICATE:
													msg = String.format("Found a duplicate of %s as [%s](%s)", next,
														o.getNewId(), o.getNewLabel());
													break;

												default:
													msg = String.format("Persisted (%s) %s", o.getResult(), next);
													break;
											}
											this.log.debug(msg);
										}
									}
									if (useTx) {
										session.commit();
									}
									i++;
								} catch (Throwable t) {
									if (useTx) {
										session.rollback();
									}
									this.listenerDelegator.objectImportFailed(this.importState.jobId, next, t);
									// Log the error, move on
									this.log.error(String.format("Exception caught processing %s", next), t);
									if (this.batch.strategy.isBatchFailRemainder()) {
										// If we're supposed to kill the batch, fail all
										// the other objects
										failBatch = true;
										this.log.debug(String.format(
											"Objects of type [%s] require that the remainder of the batch fail if an object fails",
											storedType));
										this.batch.markAborted(t);
										continue;
									}
								}
							} finally {
								ctx.close();
							}
						}
						return outcomes;
					} finally {
						session.close();
					}
				} catch (Exception e) {
					this.log.error(String.format("Uncaught exception while processing batch [%s::%s]",
						this.batch.type.name(), this.batch.id), e);
					throw e;
				} finally {
					this.batch.markCompleted();
					this.listenerDelegator.objectHistoryImportFinished(this.importState.jobId, this.batch.type,
						this.batch.id, outcomes, failBatch);
				}
			} finally {
				this.log.debug("Worker exiting...");
				this.workerCounter.decrement();
			}
		}
	}

	public static final String TYPE_MAPPER_PREFIX = "cmfTypeMapper.";
	public static final String TYPE_MAPPER_SELECTOR = "cmfTypeMapperName";

	private static final Pattern MAP_KEY_PARSER = Pattern.compile("^\\s*([^#\\s]+)\\s*#\\s*(.+)\\s*$");

	private static final CmfTypeMapper DEFAULT_TYPE_MAPPER = new CmfTypeMapper() {

		@Override
		protected String getMapping(String sourceType) {
			return null;
		}

	};

	private static enum BatchStatus {
		//
		PENDING, PROCESSED, ABORTED;
	}

	private class Batch {
		private final CmfType type;
		private final String id;
		private final Collection<CmfObject<V>> contents;
		private final ImportStrategy strategy;
		private BatchStatus status = BatchStatus.PENDING;
		// private Throwable thrown = null;

		private Batch() {
			this(null, null, null, null);
		}

		private Batch(CmfType type, String id, Collection<CmfObject<V>> contents, ImportStrategy strategy) {
			this.type = type;
			this.id = id;
			this.contents = contents;
			this.strategy = strategy;
		}

		private void markCompleted() {
			if (this.status == BatchStatus.PENDING) {
				this.status = BatchStatus.PROCESSED;
				notify();
			}
		}

		private void markAborted(Throwable thrown) {
			if (this.status == BatchStatus.PENDING) {
				this.status = BatchStatus.ABORTED;
				// this.thrown = thrown;
				notify();
			}
		}

		@Override
		public String toString() {
			return String.format(
				"Batch [type=%s, id=%s, status=%s, strategy.parallel=%s, strategy.failRemainder=%s, contents=%s]",
				this.type, this.id, this.status, this.strategy.isParallelCapable(),
				this.strategy.isBatchFailRemainder(), this.contents);
		}
	}

	private class ImportListenerDelegator extends ListenerDelegator<ImportResult> implements ImportEngineListener {

		private final Collection<ImportEngineListener> listeners = getListeners();

		private ImportListenerDelegator(CmfObjectCounter<ImportResult> counter) {
			super(counter);
		}

		@Override
		public void importStarted(ImportState importState, Map<CmfType, Long> summary) {
			for (ImportEngineListener l : this.listeners) {
				try {
					l.importStarted(importState, summary);
				} catch (Exception e) {
					if (this.log.isDebugEnabled()) {
						this.log.error("Exception caught during listener propagation", e);
					}
				}
			}
		}

		@Override
		public void objectTypeImportStarted(UUID jobId, CmfType objectType, long totalObjects) {
			for (ImportEngineListener l : this.listeners) {
				try {
					l.objectTypeImportStarted(jobId, objectType, totalObjects);
				} catch (Exception e) {
					if (this.log.isDebugEnabled()) {
						this.log.error("Exception caught during listener propagation", e);
					}
				}
			}
		}

		@Override
		public void objectTierImportStarted(UUID jobId, CmfType objectType, int tier) {
			for (ImportEngineListener l : this.listeners) {
				try {
					l.objectTierImportStarted(jobId, objectType, tier);
				} catch (Exception e) {
					if (this.log.isDebugEnabled()) {
						this.log.error("Exception caught during listener propagation", e);
					}
				}
			}
		}

		@Override
		public void objectHistoryImportStarted(UUID jobId, CmfType objectType, String batchId, int count) {
			for (ImportEngineListener l : this.listeners) {
				try {
					l.objectHistoryImportStarted(jobId, objectType, batchId, count);
				} catch (Exception e) {
					if (this.log.isDebugEnabled()) {
						this.log.error("Exception caught during listener propagation", e);
					}
				}
			}
		}

		@Override
		public void objectImportStarted(UUID jobId, CmfObject<?> object) {
			for (ImportEngineListener l : this.listeners) {
				try {
					l.objectImportStarted(jobId, object);
				} catch (Exception e) {
					if (this.log.isDebugEnabled()) {
						this.log.error("Exception caught during listener propagation", e);
					}
				}
			}
		}

		@Override
		public void objectImportCompleted(UUID jobId, CmfObject<?> object, ImportOutcome outcome) {
			getStoredObjectCounter().increment(object.getType(), outcome.getResult());
			for (ImportEngineListener l : this.listeners) {
				try {
					l.objectImportCompleted(jobId, object, outcome);
				} catch (Exception e) {
					if (this.log.isDebugEnabled()) {
						this.log.error("Exception caught during listener propagation", e);
					}
				}
			}
		}

		@Override
		public void objectImportFailed(UUID jobId, CmfObject<?> object, Throwable thrown) {
			getStoredObjectCounter().increment(object.getType(), ImportResult.FAILED);
			for (ImportEngineListener l : this.listeners) {
				try {
					l.objectImportFailed(jobId, object, thrown);
				} catch (Exception e) {
					if (this.log.isDebugEnabled()) {
						this.log.error("Exception caught during listener propagation", e);
					}
				}
			}
		}

		@Override
		public void objectHistoryImportFinished(UUID jobId, CmfType objectType, String batchId,
			Map<String, Collection<ImportOutcome>> outcomes, boolean failed) {
			for (ImportEngineListener l : this.listeners) {
				try {
					l.objectHistoryImportFinished(jobId, objectType, batchId, outcomes, failed);
				} catch (Exception e) {
					if (this.log.isDebugEnabled()) {
						this.log.error("Exception caught during listener propagation", e);
					}
				}
			}
		}

		@Override
		public void objectTierImportFinished(UUID jobId, CmfType objectType, int tier, boolean failed) {
			for (ImportEngineListener l : this.listeners) {
				try {
					l.objectTierImportFinished(jobId, objectType, tier, failed);
				} catch (Exception e) {
					if (this.log.isDebugEnabled()) {
						this.log.error("Exception caught during listener propagation", e);
					}
				}
			}
		}

		@Override
		public void objectTypeImportFinished(UUID jobId, CmfType objectType, Map<ImportResult, Long> counters) {
			for (ImportEngineListener l : this.listeners) {
				try {
					l.objectTypeImportFinished(jobId, objectType, counters);
				} catch (Exception e) {
					if (this.log.isDebugEnabled()) {
						this.log.error("Exception caught during listener propagation", e);
					}
				}
			}
		}

		@Override
		public void importFinished(UUID jobId, Map<ImportResult, Long> counters) {
			for (ImportEngineListener l : this.listeners) {
				try {
					l.importFinished(jobId, counters);
				} catch (Exception e) {
					if (this.log.isDebugEnabled()) {
						this.log.error("Exception caught during listener propagation", e);
					}
				}
			}
		}

		private void objectTypeImportFinished(UUID jobId, CmfType objectType) {
			objectTypeImportFinished(jobId, objectType, getStoredObjectCounter().getCounters(objectType));
		}

		private void importFinished(UUID jobId) {
			importFinished(jobId, getStoredObjectCounter().getCummulative());
		}

	}

	protected ImportEngine(CmfCrypt crypto) {
		super(crypto);
	}

	protected ImportEngine(CmfCrypt crypto, boolean supportsDuplicateNames) {
		super(crypto, supportsDuplicateNames);
	}

	protected abstract ImportStrategy getImportStrategy(CmfType type);

	protected final CmfTypeMapper getTypeMapper(S session, CfgTools cfg) throws Exception {
		final String typeMapperName = cfg.getString(ImportEngine.TYPE_MAPPER_SELECTOR);

		Map<String, Object> m = new HashMap<>();
		for (String str : cfg.getSettings()) {
			if (str.startsWith(ImportEngine.TYPE_MAPPER_PREFIX)) {
				str = str.substring(ImportEngine.TYPE_MAPPER_PREFIX.length());
				m.put(str, cfg.getObject(str));
			}
		}
		cfg = new CfgTools(m);
		CmfTypeMapper mapper = CmfTypeMapper.getTypeMapper(typeMapperName, cfg);
		if (mapper == null) {
			mapper = ImportEngine.DEFAULT_TYPE_MAPPER;
		}
		return mapper;
	}

	protected final ExecutorService newExecutor(int threadCount) {
		return new ThreadPoolExecutor(threadCount, threadCount, 30, TimeUnit.SECONDS,
			new LinkedBlockingQueue<Runnable>());
	}

	public final CmfObjectCounter<ImportResult> runImport(final Logger output, final CmfObjectStore<?, ?> objectStore,
		final CmfContentStore<?, ?, ?> streamStore, Map<String, ?> settings)
		throws ImportException, CmfStorageException {
		return runImport(output, objectStore, streamStore, settings, null);
	}

	public final CmfObjectCounter<ImportResult> runImport(final Logger output, final CmfObjectStore<?, ?> objectStore,
		final CmfContentStore<?, ?, ?> streamStore, Map<String, ?> settings, CmfObjectCounter<ImportResult> counter)
		throws ImportException, CmfStorageException {

		// First things first...we should only do this if the target repo ID
		// is not the same as the previous target repo - we can tell this by
		// looking at the target mappings.
		// this.log.info("Clearing all previous mappings");
		// objectStore.clearAllMappings();

		final CfgTools configuration = new CfgTools(settings);

		final ImportState importState = new ImportState(output, objectStore, streamStore, configuration);
		final SessionFactory<S> sessionFactory;
		try {
			sessionFactory = newSessionFactory(configuration, this.crypto);
		} catch (Exception e) {
			throw new ImportException("Failed to configure the session factory to carry out the import", e);
		}

		try {
			SessionWrapper<S> baseSession = null;
			try {
				baseSession = sessionFactory.acquireSession();
			} catch (Exception e) {
				throw new ImportException("Failed to obtain the import initialization session", e);
			}

			ImportContextFactory<S, W, V, C, ?, ?> contextFactory = null;
			ImportDelegateFactory<S, W, V, C, ?> delegateFactory = null;
			CmfTypeMapper typeMapper = null;
			try {
				try {
					contextFactory = newContextFactory(baseSession.getWrapped(), configuration);
				} catch (Exception e) {
					throw new ImportException("Failed to configure the context factory to carry out the import", e);
				}
				try {
					delegateFactory = newDelegateFactory(baseSession.getWrapped(), configuration);
				} catch (Exception e) {
					throw new ImportException("Failed to configure the delegate factory to carry out the import", e);
				}

				try {
					typeMapper = getTypeMapper(baseSession.getWrapped(), configuration);
				} catch (Exception e) {
					throw new ImportException("Failed to configure the required type mapper", e);
				}

				try {
					baseSession.close();
				} finally {
					baseSession = null;
				}

				return runImportImpl(importState, sessionFactory, counter, contextFactory, delegateFactory, typeMapper);
			} finally {
				if (baseSession != null) {
					baseSession.close();
				}
				if (typeMapper != null) {
					typeMapper.close();
				}
				if (delegateFactory != null) {
					delegateFactory.close();
				}
				if (contextFactory != null) {
					contextFactory.close();
				}
			}
		} finally {
			sessionFactory.close();
		}
	}

	private final void renameObjectsWithMap(final Logger output, Properties p, CmfObjectStore<?, ?> objectStore,
		final String verb) throws ImportException, CmfStorageException {
		// Things happen differently here... since we have a limited scope in which
		// objects require fixing, we don't sweep the whole table, but instead submit
		// the IDs that we want fixed.

		Map<CmfType, Map<String, String>> idMap = new EnumMap<>(CmfType.class);
		for (String key : p.stringPropertyNames()) {
			final String fixedName = p.getProperty(key);
			Matcher matcher = ImportEngine.MAP_KEY_PARSER.matcher(key);
			if (!matcher.matches()) {
				continue;
			}
			final String T = matcher.group(1);
			final CmfType t;
			try {
				t = CmfType.valueOf(T);
			} catch (Exception e) {
				this.log.warn(
					String.format("Unsupported object type found [%s] in key [%s] (value = [%s])", T, key, fixedName),
					e);
				continue;
			}
			final String id = matcher.group(2);
			Map<String, String> m = idMap.get(t);
			if (m == null) {
				m = new TreeMap<>();
				idMap.put(t, m);
			}
			m.put(id, fixedName);
		}

		if (idMap.isEmpty()) {
			output.info("Static name fix map is empty, will not {} any object names", verb);
			return;
		}

		for (final CmfType t : idMap.keySet()) {
			final Map<String, String> mappings = idMap.get(t);
			CmfNameFixer<V> nameFixer = new CmfNameFixer<V>() {

				@Override
				public boolean supportsType(CmfType type) {
					return (type == t);
				}

				@Override
				public String fixName(CmfObject<V> dataObject) throws CmfStorageException {
					return mappings.get(dataObject.getId());
				}

				@Override
				public boolean handleException(Exception e) {
					return false;
				}

				@Override
				public void nameFixed(CmfObject<V> dataObject, String oldName, String newName) {
					output.info("Renamed {} with ID[{}] from [{}] to [{}]", dataObject.getType(), dataObject.getId(),
						oldName, newName);
				}
			};
			output.info("Trying to {} {} {} names...", verb, mappings.size(), t.name());
			final int fixes = objectStore.fixObjectNames(getTranslator(), nameFixer, t, mappings.keySet());
			output.info("Modified {} {} objects", fixes, t.name());
		}
	}

	private final CmfObjectCounter<ImportResult> runImportImpl(final ImportState importState,
		final SessionFactory<S> sessionFactory, CmfObjectCounter<ImportResult> counter,
		final ImportContextFactory<S, W, V, C, ?, ?> contextFactory,
		final ImportDelegateFactory<S, W, V, C, ?> delegateFactory, final CmfTypeMapper typeMapper)
		throws ImportException, CmfStorageException {
		final UUID jobId = importState.jobId;
		final Logger output = importState.output;
		final CmfObjectStore<?, ?> objectStore = importState.objectStore;
		final CfgTools settings = importState.cfg;

		final int threadCount;
		synchronized (this) {
			threadCount = getThreadCount(settings);
		}

		final SynchronizedCounter workerCounter = new SynchronizedCounter();
		final ExecutorService parallelExecutor = newExecutor(threadCount);
		final ExecutorService singleExecutor = Executors.newSingleThreadExecutor();

		if (counter == null) {
			counter = new CmfObjectCounter<>(ImportResult.class);
		}
		final ImportListenerDelegator listenerDelegator = new ImportListenerDelegator(counter);

		try {
			Map<CmfType, Long> containedTypes;
			try {
				containedTypes = objectStore.getStoredObjectTypes();
			} catch (CmfStorageException e) {
				throw new ImportException("Exception raised getting the stored object counts", e);
			}

			// Make sure we have a valid import strategy for every item
			for (CmfType t : containedTypes.keySet()) {
				// If the type is supported, and we have no strategy, it's a problem...
				if (contextFactory.isSupported(t) && (getImportStrategy(t) == null)) { throw new ImportException(
					String.format("No import strategy provided for available object type [%s]", t.name())); }
			}

			objectStore.clearAttributeMappings();
			// Ensure the target path exists
			{
				final SessionWrapper<S> rootSession;
				try {
					rootSession = sessionFactory.acquireSession();
				} catch (Exception e) {
					throw new ImportException("Failed to obtain the root session to ensure the target path", e);
				}
				try {
					rootSession.begin();
					contextFactory.ensureTargetPath(rootSession.getWrapped());
					rootSession.commit();
				} catch (Exception e) {
					rootSession.rollback();
					throw new ImportException("Failed to ensure the target path", e);
				} finally {
					rootSession.close();
				}
			}

			// Reset all alternate names, to ensure we're not using already-processed names
			output.info("Resetting object names to their base values...");
			objectStore.resetAltNames();
			output.info("Object names successfully reset");

			if (!settings.getBoolean(ImportSetting.NO_FILENAME_MAP)) {
				final Properties p = new Properties();
				if (MappingTools.loadMap(this.log, settings.getString(ImportSetting.FILENAME_MAP), p)) {
					// Things happen differently here... since we have a limited scope in which
					// objects require fixing, we don't sweep the whole table, but instead submit
					// the IDs that we want fixed.
					output.info("Applying static object renames for {} objects...", p.size());
					renameObjectsWithMap(output, p, objectStore, "statically transform");
					output.info("Static renames completed");
				}
			}

			if (settings.getBoolean(ImportSetting.NAME_FIX)) {
				CmfNameFixer<V> nameFixer = getNameFixer(output);
				if (nameFixer != null) {
					output.info("Fixing all object names dynamically...");
					final int fixes = objectStore.fixObjectNames(getTranslator(), nameFixer);
					output.info("Fixed the names of {} objects", fixes);
				}
			}

			if (!isSupportsDuplicateFileNames() && settings.getBoolean(ImportSetting.DEDUP)) {
				// Handle deduplication globally as well, since we may have cross-type collisions
				int pass = 0;
				outer: for (;;) {
					output.info("Checking for filename collisions (pass # {})", ++pass);
					Collection<CmfObject<V>> collidingObjects = objectStore
						.getObjectsWithFileNameCollisions(getTranslator());
					if (collidingObjects.isEmpty()) {
						if (pass > 1) {
							output.info("No name collisions left to resolve (after {} passes)", pass - 1);
						} else {
							output.info("No name collisions were found");
						}
						break;
					}
					output.info("Resolving the next filename collision ({} total remaining, pass # {})",
						collidingObjects.size(), pass);
					for (CmfObject<V> object : collidingObjects) {
						String newName = object.getName();
						String id = object.getId();
						String ext = "";
						if (object.getType() == CmfType.DOCUMENT) {
							id = object.getHistoryId();
							ext = Tools.coalesce(FilenameUtils.getExtension(newName), ext);
							if (!StringUtils.isEmpty(ext)) {
								ext = String.format(".%s", ext);
								newName = newName.substring(0, newName.length() - ext.length());
							}
						}
						newName = String.format("%s_%s%s", newName, id, ext);
						output.info("Fixing name collisions for {} [{}]({}), from [{}] to [{}]", object.getType(),
							object.getLabel(), object.getId(), object.getName(), newName);
						objectStore.renameObject(object, newName);
						continue outer;
					}
					break outer;
				}
			}

			listenerDelegator.importStarted(importState, containedTypes);
			final CmfAttributeTranslator<V> translator = getTranslator();
			for (final CmfType type : CmfType.values()) {
				final Long total = containedTypes.get(type);
				if (total == null) {
					this.log.warn(String.format("No %s objects are contained in the export", type.name()));
					continue;
				}

				if (total < 1) {
					this.log.warn(String.format("No %s objects available", type.name()));
					continue;
				}

				if (!contextFactory.isSupported(type)) {
					this.log.debug(
						String.format("Ignoring %d objects of type %s due to engine configuration", total, type));
					continue;
				}

				final ImportStrategy strategy = getImportStrategy(type);
				if (strategy.isIgnored()) {
					if (this.log.isDebugEnabled()) {
						this.log.debug(String.format(
							"Skipping %d objects of type %s because it's marked as ignored by the given strategy",
							total, type.name()));
					}
					continue;
				}
				listenerDelegator.objectTypeImportStarted(jobId, type, total);

				// Start the workers
				// If we don't support parallelization at any level, then we simply use a
				// single worker to do everything. Otherwise, the rest of the strategy will
				// dictate how the parallelism will work (i.e. batches are parallel and
				// their contents serialized, or batches' contents are parallel and batches
				// are serialized).
				final ExecutorService executor = (strategy.isParallelCapable() ? parallelExecutor : singleExecutor);

				this.log.info(String.format("%d %s objects available, starting deserialization", total, type.name()));
				try {
					objectStore.loadObjects(typeMapper, translator, type, new CmfObjectHandler<V>() {
						private List<CmfObject<V>> contents = null;

						@Override
						public boolean newTier(int tier) throws CmfStorageException {
							output.info("Processing {} tier {}", type.name(), tier);
							return true;
						}

						@Override
						public boolean newHistory(String historyId) throws CmfStorageException {
							this.contents = new LinkedList<>();
							return true;
						}

						@Override
						public boolean handleObject(CmfObject<V> dataObject) {
							this.contents.add(dataObject);
							return true;
						}

						@Override
						public boolean endHistory(String historyId, boolean ok) throws CmfStorageException {
							if ((this.contents == null) || this.contents.isEmpty()) { return true; }
							CmfObject<?> sample = this.contents.get(0);
							CmfType storedType = sample.getType();
							ImportStrategy strategy = getImportStrategy(storedType);
							// We will have already validated that a valid strategy is provided
							// for all stored types
							try {
								executor
									.submit(new BatchWorker(new Batch(storedType, historyId, this.contents, strategy),
										workerCounter, sessionFactory, listenerDelegator, importState, contextFactory,
										delegateFactory, typeMapper));
							} finally {
								this.contents = null;
							}
							return true;
						}

						@Override
						public boolean endTier(int tierId, boolean ok) throws CmfStorageException {
							// TODO: Wait for the currently-running tier to complete.
							// i.e. wait until the workers are idle
							try {
								workerCounter.waitUntil(0);
							} catch (InterruptedException e) {
								throw new CmfStorageException(
									String.format("Thread interrupted while waiting for tier [%d] to complete", tierId),
									e);
							} finally {
								output.info("Completed {} tier {}", type.name(), tierId);
							}
							return ok;
						}

						@Override
						public boolean handleException(Exception e) {
							return true;
						}
					});
				} catch (Exception e) {
					throw new ImportException(
						String.format("Exception raised while loading objects of type [%s]", type), e);
				} finally {
					try {
						// Here, we wait for all the workers to conclude
						this.log.info(String.format("Waiting for the %s workers to exit...", type.name()));
						try {
							workerCounter.waitUntil(0);
						} catch (InterruptedException e) {
							this.log.warn("Interrupted while waiting for an executor thread to exit", e);
							Thread.currentThread().interrupt();
							break;
						}
						this.log.info(String.format("All the %s workers have exited", type.name()));
					} finally {
						listenerDelegator.objectTypeImportFinished(jobId, type);
					}
				}

				// Finally, decide what to do if errors were encountered
				final Map<ImportResult, Long> results = listenerDelegator.getCounters(type);
				final long errorCount = results.get(ImportResult.FAILED);
				if (abortImport(type, errorCount)) {
					this.log.info(
						String.format("Import aborted due to %d errors detected while importing objects of type %s",
							errorCount, type.name()));
					break;
				}
				this.log.info(String.format("Work on %s objects completed, continuing with the next object type...",
					type.name()));
			}

			// Shut down the executor
			parallelExecutor.shutdown();
			singleExecutor.shutdown();

			// If there are still pending workers, then wait for them to finish for up to 5
			// minutes
			long pending = workerCounter.get();
			if (pending > 0) {
				this.log.info(String.format(
					"Waiting for pending workers to terminate (maximum 5 minutes, %d pending workers)", pending));
				try {
					workerCounter.waitUntil(0, 5, TimeUnit.MINUTES);
				} catch (InterruptedException e) {
					this.log.warn("Interrupted while waiting for normal executor termination", e);
					Thread.currentThread().interrupt();
				}
			}
			return listenerDelegator.getStoredObjectCounter();
		} finally {
			parallelExecutor.shutdownNow();
			singleExecutor.shutdownNow();

			long pending = workerCounter.get();
			if (pending > 0) {
				try {
					this.log.info(String.format(
						"Waiting an additional 60 seconds for worker termination as a contingency (%d pending workers)",
						pending));
					workerCounter.waitUntil(0, 1, TimeUnit.MINUTES);
				} catch (InterruptedException e) {
					this.log.warn("Interrupted while waiting for immediate executor termination", e);
					Thread.currentThread().interrupt();
				}
			}
			listenerDelegator.importFinished(jobId);
		}
	}

	protected boolean abortImport(CmfType type, long errors) {
		return false;
	}

	protected CmfNameFixer<V> getNameFixer(Logger output) {
		return null;
	}

	public static ImportEngine<?, ?, ?, ?, ?, ?> getImportEngine(String targetName) {
		return TransferEngine.getTransferEngine(ImportEngine.class, targetName);
	}

	protected void initContext(C ctx) {
	}

	@Override
	protected void getSupportedSettings(Collection<TransferEngineSetting> settings) {
		for (ImportSetting s : ImportSetting.values()) {
			settings.add(s);
		}
	}
}