/**
 *
 */

package com.armedia.cmf.engine.importer;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
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
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

import com.armedia.cmf.engine.CmfCrypt;
import com.armedia.cmf.engine.SessionFactory;
import com.armedia.cmf.engine.SessionWrapper;
import com.armedia.cmf.engine.TransferEngine;
import com.armedia.cmf.engine.TransferEngineSetting;
import com.armedia.cmf.engine.importer.ImportStrategy.BatchItemStrategy;
import com.armedia.cmf.engine.tools.MappingTools;
import com.armedia.cmf.storage.CmfAttributeTranslator;
import com.armedia.cmf.storage.CmfContentStore;
import com.armedia.cmf.storage.CmfNameFixer;
import com.armedia.cmf.storage.CmfObject;
import com.armedia.cmf.storage.CmfObjectCounter;
import com.armedia.cmf.storage.CmfObjectHandler;
import com.armedia.cmf.storage.CmfObjectStore;
import com.armedia.cmf.storage.CmfStorageException;
import com.armedia.cmf.storage.CmfType;
import com.armedia.cmf.storage.CmfTypeMapper;
import com.armedia.commons.utilities.CfgTools;
import com.armedia.commons.utilities.Tools;

/**
 * @author diego
 *
 */
public abstract class ImportEngine<S, W extends SessionWrapper<S>, V, C extends ImportContext<S, V, CF>, CF extends ImportContextFactory<S, W, V, C, ?, ?>, DF extends ImportDelegateFactory<S, W, V, C, ?>>
	extends TransferEngine<S, V, C, CF, DF, ImportEngineListener> {

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
		private Throwable thrown = null;

		private Batch() {
			this(null, null, null, null);
		}

		private Batch(CmfType type, String id, Collection<CmfObject<V>> contents, ImportStrategy strategy) {
			this.type = type;
			this.id = id;
			this.contents = contents;
			this.strategy = strategy;
		}

		private synchronized void markCompleted() {
			if (this.status == BatchStatus.PENDING) {
				this.status = BatchStatus.PROCESSED;
				notify();
			}
		}

		private synchronized void markAborted(Throwable thrown) {
			if (this.status == BatchStatus.PENDING) {
				this.status = BatchStatus.ABORTED;
				this.thrown = thrown;
				notify();
			}
		}

		@Override
		public String toString() {
			return String.format(
				"Batch [type=%s, id=%s, status=%s, strategy.parallel=%s, strategy.batchingSupported=%s, strategy.batchingStrategy=%s, strategy.failRemainder=%s, contents=%s]",
				this.type, this.id, this.status, this.strategy.isParallelCapable(), this.strategy.isBatchingSupported(),
				this.strategy.getBatchItemStrategy(), this.strategy.isBatchFailRemainder(), this.contents);
		}
	}

	private class ImportListenerDelegator extends ListenerDelegator<ImportResult> implements ImportEngineListener {

		private final Collection<ImportEngineListener> listeners = getListeners();

		private ImportListenerDelegator(CmfObjectCounter<ImportResult> counter) {
			super(counter);
		}

		@Override
		public void importStarted(UUID jobId, Map<CmfType, Integer> summary) {
			for (ImportEngineListener l : this.listeners) {
				try {
					l.importStarted(jobId, summary);
				} catch (Exception e) {
					if (this.log.isDebugEnabled()) {
						this.log.error("Exception caught during listener propagation", e);
					}
				}
			}
		}

		@Override
		public void objectTypeImportStarted(UUID jobId, CmfType objectType, int totalObjects) {
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
		public void objectTypeImportFinished(UUID jobId, CmfType objectType, Map<ImportResult, Integer> counters) {
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
		public void importFinished(UUID jobId, Map<ImportResult, Integer> counters) {
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

		@Override
		public void objectBatchImportStarted(UUID jobId, CmfType objectType, String batchId, int count) {
			for (ImportEngineListener l : this.listeners) {
				try {
					l.objectBatchImportStarted(jobId, objectType, batchId, count);
				} catch (Exception e) {
					if (this.log.isDebugEnabled()) {
						this.log.error("Exception caught during listener propagation", e);
					}
				}
			}
		}

		@Override
		public void objectBatchImportFinished(UUID jobId, CmfType objectType, String batchId,
			Map<String, Collection<ImportOutcome>> outcomes, boolean failed) {
			for (ImportEngineListener l : this.listeners) {
				try {
					l.objectBatchImportFinished(jobId, objectType, batchId, outcomes, failed);
				} catch (Exception e) {
					if (this.log.isDebugEnabled()) {
						this.log.error("Exception caught during listener propagation", e);
					}
				}
			}
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

		Map<String, Object> m = new HashMap<String, Object>();
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

		ImportState importState = new ImportState(output, objectStore, streamStore, configuration);

		prepareImport(importState);
		boolean ok = false;
		try {

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
						throw new ImportException("Failed to configure the delegate factory to carry out the import",
							e);
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

					CmfObjectCounter<ImportResult> result = runImportImpl(importState, sessionFactory, counter,
						contextFactory, delegateFactory, typeMapper);
					ok = true;
					return result;
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
		} finally {
			if (ok) {
				importFinalized(importState);
			} else {
				importFailed(importState);
			}
		}
	}

	private final void renameObjectsWithMap(final Logger output, Properties p, CmfObjectStore<?, ?> objectStore,
		final String verb) throws ImportException, CmfStorageException {
		// Things happen differently here... since we have a limited scope in which
		// objects require fixing, we don't sweep the whole table, but instead submit
		// the IDs that we want fixed.

		Map<CmfType, Map<String, String>> idMap = new EnumMap<CmfType, Map<String, String>>(CmfType.class);
		for (Object k : p.keySet()) {
			final String key = k.toString();
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
				m = new TreeMap<String, String>();
				idMap.put(t, m);
			}
			m.put(id, fixedName);
		}

		if (idMap.isEmpty()) {
			output.info("Static name fix map is empty, object names will be kept as-is");
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
					// DOCUMENTS will use the batchId instead of the ID, while all
					// others will use the ID
					String id = dataObject.getId();
					if (dataObject.getType() == CmfType.DOCUMENT) {
						id = dataObject.getBatchId();
					}
					return mappings.get(id);
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

	private final CmfObjectCounter<ImportResult> runImportImpl(ImportState importState,
		final SessionFactory<S> sessionFactory, CmfObjectCounter<ImportResult> counter,
		final ImportContextFactory<S, W, V, C, ?, ?> contextFactory,
		final ImportDelegateFactory<S, W, V, C, ?> delegateFactory, final CmfTypeMapper typeMapper)
		throws ImportException, CmfStorageException {
		final UUID jobId = importState.jobId;
		final Logger output = importState.output;
		final CmfObjectStore<?, ?> objectStore = importState.objectStore;
		final CmfContentStore<?, ?, ?> streamStore = importState.streamStore;
		final CfgTools settings = importState.cfg;

		final int threadCount;
		final int backlogSize;
		synchronized (this) {
			threadCount = getThreadCount(settings);
			backlogSize = getBacklogSize(settings);
		}

		final AtomicInteger activeCounter = new AtomicInteger(0);
		final AtomicLong batchCounter = new AtomicLong(0);
		final Object workerSynchronizer = new Object();
		final Batch exitValue = new Batch();
		final BlockingQueue<Batch> workQueue = new ArrayBlockingQueue<Batch>(backlogSize);
		final ExecutorService executor = newExecutor(threadCount);

		if (counter == null) {
			counter = new CmfObjectCounter<ImportResult>(ImportResult.class);
		}
		final ImportListenerDelegator listenerDelegator = new ImportListenerDelegator(counter);

		// First things first, validate that valid strategies are returned for every object
		// type that will be imported
		Runnable worker = new Runnable() {
			private final Logger log = ImportEngine.this.log;

			@Override
			public void run() {
				activeCounter.incrementAndGet();
				synchronized (workerSynchronizer) {
					workerSynchronizer.notify();
				}
				SessionWrapper<S> session = null;
				try {
					while (!Thread.interrupted()) {
						if (this.log.isDebugEnabled()) {
							this.log.debug("Polling the queue...");
						}
						// increase the waiter count
						final Batch batch;
						try {
							batch = workQueue.take();
							if (batch == null) {
								// These are impossible, but this will shut FindBugs up :)
								continue;
							}
						} catch (InterruptedException e) {
							Thread.currentThread().interrupt();
							return;
						}

						if (batch == exitValue) {
							this.log.info("Exiting the export polling loop");
							return;
						}

						boolean failBatch = false;
						Map<String, Collection<ImportOutcome>> outcomes = new LinkedHashMap<String, Collection<ImportOutcome>>(
							batch.contents.size());
						try {
							if (batch.contents.isEmpty()) {
								// Shouldn't happen, but still
								this.log.warn(
									String.format("An invalid value made it into the work queue somehow: %s", batch));
								batch.markCompleted();
								continue;
							}

							if (this.log.isDebugEnabled()) {
								this.log.debug(String.format("Polled a batch with %d items", batch.contents.size()));
							}
							try {
								session = sessionFactory.acquireSession();
							} catch (Exception e) {
								this.log.error("Failed to obtain a worker session", e);
								batch.markAborted(e);
								return;
							}

							if (this.log.isDebugEnabled()) {
								this.log.debug(String.format("Got session [%s]", session.getId()));
							}

							listenerDelegator.objectBatchImportStarted(jobId, batch.type, batch.id,
								batch.contents.size());
							int i = 0;
							for (CmfObject<V> next : batch.contents) {
								if (failBatch) {
									this.log
										.error(String.format("Batch has been failed - will not process [%s](%s) (%s)",
											next.getLabel(), next.getId(), ImportResult.SKIPPED.name()));
									listenerDelegator.objectImportCompleted(jobId, next, ImportOutcome.SKIPPED);
									continue;
								}

								final C ctx = contextFactory.newContext(next.getId(), next.getType(),
									session.getWrapped(), output, objectStore, streamStore, typeMapper, i);
								try {
									initContext(ctx);
									final CmfType storedType = next.getType();
									final boolean useTx = getImportStrategy(storedType).isSupportsTransactions();
									if (useTx) {
										session.begin();
									}
									try {
										listenerDelegator.objectImportStarted(jobId, next);
										// TODO: Transform the loaded object from the
										// intermediate format into the target format
										ImportDelegate<?, S, W, V, C, ?, ?> delegate = delegateFactory
											.newImportDelegate(next);
										final Collection<ImportOutcome> outcome = delegate.importObject(getTranslator(),
											ctx);
										outcomes.put(next.getId(), outcome);
										for (ImportOutcome o : outcome) {
											listenerDelegator.objectImportCompleted(jobId, next, o);
											if (this.log.isDebugEnabled()) {
												String msg = null;
												switch (o.getResult()) {
													case CREATED:
													case UPDATED:
														msg = String.format("Persisted (%s) %s as [%s](%s)",
															o.getResult(), next, o.getNewId(), o.getNewLabel());
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
										listenerDelegator.objectImportFailed(jobId, next, t);
										// Log the error, move on
										this.log.error(String.format("Exception caught processing %s", next), t);
										if (batch.strategy.isBatchFailRemainder()) {
											// If we're supposed to kill the batch, fail all
											// the other objects
											failBatch = true;
											this.log.debug(String.format(
												"Objects of type [%s] require that the remainder of the batch fail if an object fails",
												storedType));
											batch.markAborted(t);
											continue;
										}
									}
								} finally {
									ctx.close();
								}
							}
						} finally {
							batch.markCompleted();
							listenerDelegator.objectBatchImportFinished(jobId, batch.type, batch.id, outcomes,
								failBatch);
							if (session != null) {
								session.close();
								session = null;
							}
							batchCounter.incrementAndGet();
							synchronized (workerSynchronizer) {
								workerSynchronizer.notify();
							}
						}
					}
				} finally {
					this.log.debug("Worker exiting...");
					activeCounter.decrementAndGet();
					synchronized (workerSynchronizer) {
						workerSynchronizer.notify();
					}
					// Just in case
					if (session != null) {
						session.close();
					}
				}
			}
		};

		try {
			Map<CmfType, Integer> containedTypes;
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

			List<Future<?>> futures = new ArrayList<Future<?>>();
			List<Batch> remaining = new ArrayList<Batch>();
			objectStore.clearAttributeMappings();
			listenerDelegator.importStarted(jobId, containedTypes);

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
			output.info("Resetting object names to their original values...");
			objectStore.resetAltNames();
			output.info("Original object names successfully reset");

			{
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

			if (!settings.getBoolean(ImportSetting.NO_NAME_FIX)) {
				CmfNameFixer<V> nameFixer = getNameFixer();
				if (nameFixer != null) {
					output.info("Fixing all object names dynamically...");
					final int fixes = objectStore.fixObjectNames(getTranslator(), nameFixer);
					output.info("Fixed the names of {} objects", fixes);
				} else {
					output.info("Object names will be kept as-is");
				}
			}

			if (!isSupportsDuplicateFileNames() && !settings.getBoolean(ImportSetting.NO_DEDUP)) {
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
							id = object.getBatchId();
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

			final CmfAttributeTranslator<V> translator = getTranslator();
			for (final CmfType type : CmfType.values()) {
				final Integer total = containedTypes.get(type);
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
				futures.clear();
				// If we don't support parallelization at any level, then we simply use a
				// single worker to do everything. Otherwise, the rest of the strategy will
				// dictate how the parallelism will work (i.e. batches are parallel and
				// their contents serialized, or batches' contents are parallel and batches
				// are serialized).
				final int workerCount = (strategy.isParallelCapable() ? Math.min(total, threadCount) : 1);
				for (int i = 0; i < workerCount; i++) {
					futures.add(executor.submit(worker));
				}

				this.log.info(String.format("%d %s objects available, starting deserialization", total, type.name()));
				try {
					objectStore.loadObjects(typeMapper, translator, type, new CmfObjectHandler<V>() {
						private final Logger log = ImportEngine.this.log;

						private String batchId = null;
						private List<CmfObject<V>> contents = null;

						@Override
						public boolean newBatch(String batchId) throws CmfStorageException {
							this.contents = new LinkedList<CmfObject<V>>();
							this.batchId = batchId;
							return true;
						}

						@Override
						public boolean handleObject(CmfObject<V> dataObject) {
							if (this.contents == null) {
								ImportStrategy strategy = getImportStrategy(dataObject.getType());
								Collection<CmfObject<V>> c = new ArrayList<CmfObject<V>>(1);
								c.add(dataObject);
								try {
									workQueue
										.put(new Batch(dataObject.getType(), dataObject.getBatchId(), c, strategy));
								} catch (InterruptedException e) {
									Thread.currentThread().interrupt();
									String msg = String.format(
										"Thread interrupted while trying to submit the batch %s containing [%s]",
										this.batchId, dataObject);
									if (this.log.isDebugEnabled()) {
										this.log.warn(msg, e);
									} else {
										this.log.warn(msg);
									}
									return false;
								}
							} else {
								this.contents.add(dataObject);
							}
							return true;
						}

						@Override
						public boolean closeBatch(boolean ok) throws CmfStorageException {
							if ((this.contents == null) || this.contents.isEmpty()) { return true; }
							CmfObject<?> sample = this.contents.get(0);
							CmfType storedType = sample.getType();
							ImportStrategy strategy = getImportStrategy(storedType);
							// We will have already validated that a valid strategy is provided
							// for all stored types
							if (!strategy.isParallelCapable()
								|| (strategy.getBatchItemStrategy() == BatchItemStrategy.ITEMS_SERIALIZED)) {
								// If we're not parallelizing AT ALL, or if we're processing
								// batch contents serially (but whole batches in parallel), then
								// we submit batches as a group, and don't wait
								try {
									workQueue.put(new Batch(storedType, this.batchId, this.contents, strategy));
								} catch (InterruptedException e) {
									Thread.currentThread().interrupt();
									String msg = String.format(
										"Thread interrupted while trying to submit the batch %s containing [%s]",
										this.batchId, this.contents);
									if (this.log.isDebugEnabled()) {
										this.log.warn(msg, e);
									} else {
										this.log.warn(msg);
									}
									return false;
								} finally {
									this.contents = null;
									this.batchId = null;
								}
							} else if ((strategy.getBatchItemStrategy() == null)
								|| (strategy.getBatchItemStrategy() == BatchItemStrategy.ITEMS_CONCURRENT)) {
								// Batch items are to be run in parallel. If the strategy is
								// null, then we don't wait and simply fire every item off in
								// its own individual batch. Otherwise, batch items are to be
								// run in parallel, but batches themselves are to be serialized,
								// so we have to wait until each batch is concluded before we
								// return
								batchCounter.set(0);
								final int contentSize = this.contents.size();
								List<Batch> batches = new ArrayList<Batch>(this.contents.size());
								for (CmfObject<V> o : this.contents) {
									List<CmfObject<V>> l = new ArrayList<CmfObject<V>>(1);
									l.add(o);
									try {
										Batch batch = new Batch(o.getType(), this.batchId, l, strategy);
										batches.add(batch);
										workQueue.put(batch);
									} catch (InterruptedException e) {
										Thread.currentThread().interrupt();
										String msg = String.format(
											"Thread interrupted while trying to submit the batch %s containing [%s]",
											this.batchId, this.contents);
										if (this.log.isDebugEnabled()) {
											this.log.warn(msg, e);
										} else {
											this.log.warn(msg);
										}
										// Thread is interrupted, take that as a sign to
										// terminate
										return false;
									}
								}

								if (strategy.getBatchItemStrategy() != null) {
									// We need to wait until the batch has been processed in its
									// entirety, or no more workers waiting...
									try {
										synchronized (workerSynchronizer) {
											while ((activeCounter.get() > 0) && (batchCounter.get() < contentSize)) {
												workerSynchronizer.wait();
											}
											workerSynchronizer.notify();
										}
										// Check the result
										boolean hasError = false;
										for (Batch batch : batches) {
											if ((batch.thrown != null) || (batch.status == BatchStatus.ABORTED)) {
												hasError = true;
												break;
											}
										}
										if (hasError) {
											StringBuilder b = new StringBuilder();
											for (Batch batch : batches) {
												if (b.length() > 0) {
													b.append(String.format("%n"));
												}
												b.append(String.format("Batch [%s] %s with contents: %s", batch.id,
													batch.status, batch.contents));
												if (batch.thrown != null) {
													hasError = true;
													StringWriter sw = new StringWriter();
													PrintWriter pw = new PrintWriter(sw);
													batch.thrown.printStackTrace(pw);
													b.append(String.format("%n%s", sw.toString()));
												}
											}
											this.log.warn(String.format("Workers exited early%n%s", b.toString()));
											return false;
										}
									} catch (InterruptedException e) {
										Thread.currentThread().interrupt();
										String msg = String.format(
											"Thread interrupted while waiting for work to complete for batch %s containing [%s]",
											this.batchId, this.contents);
										if (this.log.isDebugEnabled()) {
											this.log.warn(msg, e);
										} else {
											this.log.warn(msg);
										}
										// Thread is interrupted, take that as a sign to
										// terminate
										return false;
									}
								}
							}
							// TODO: Perhaps check the error threshold
							return true;
						}

						@Override
						public boolean handleException(Exception e) {
							return true;
						}
					}, strategy.isBatchingSupported());
				} catch (Exception e) {
					throw new ImportException(
						String.format("Exception raised while loading objects of type [%s]", type), e);
				} finally {
					try {
						// Ask the workers to exit civilly after the entire workload is
						// submitted
						this.log.info(String.format("Signaling work completion for the %s workers", type.name()));
						for (int i = 0; i < workerCount; i++) {
							try {
								workQueue.put(exitValue);
							} catch (InterruptedException e) {
								// Here we have a problem: we're timing out while adding the
								// exit values...
								this.log.warn("Interrupted while attempting to request executor thread termination", e);
								Thread.currentThread().interrupt();
								break;
							}
						}

						// Here, we wait for all the workers to conclude
						this.log.info(String.format("Waiting for the %s workers to exit...", type.name()));
						for (Future<?> future : futures) {
							try {
								future.get();
							} catch (InterruptedException e) {
								this.log.warn("Interrupted while waiting for an executor thread to exit", e);
								Thread.currentThread().interrupt();
								break;
							} catch (ExecutionException e) {
								this.log.warn("An executor thread raised an exception", e);
							} catch (CancellationException e) {
								this.log.warn("An executor thread was canceled!", e);
							}
						}
						this.log.info(String.format("All the %s workers have exited", type.name()));
					} finally {
						listenerDelegator.objectTypeImportFinished(jobId, type);
						workQueue.drainTo(remaining);
						for (Batch v : remaining) {
							if (v == exitValue) {
								continue;
							}
							this.log.error(String.format("WORK LEFT PENDING IN THE QUEUE: %s", v));
						}
						remaining.clear();
					}
				}

				// Finally, decide what to do if errors were encountered
				final Map<ImportResult, Integer> results = listenerDelegator.getCounters(type);
				final int errorCount = results.get(ImportResult.FAILED);
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
			executor.shutdown();

			// If there are still pending workers, then wait for them to finish for up to 5
			// minutes
			int pending = activeCounter.get();
			if (pending > 0) {
				this.log.info(String.format(
					"Waiting for pending workers to terminate (maximum 5 minutes, %d pending workers)", pending));
				try {
					executor.awaitTermination(5, TimeUnit.MINUTES);
				} catch (InterruptedException e) {
					this.log.warn("Interrupted while waiting for normal executor termination", e);
					Thread.currentThread().interrupt();
				}
			}
			return listenerDelegator.getStoredObjectCounter();
		} finally {
			executor.shutdownNow();
			int pending = activeCounter.get();
			if (pending > 0) {
				try {
					this.log.info(String.format(
						"Waiting an additional 60 seconds for worker termination as a contingency (%d pending workers)",
						pending));
					executor.awaitTermination(1, TimeUnit.MINUTES);
				} catch (InterruptedException e) {
					this.log.warn("Interrupted while waiting for immediate executor termination", e);
					Thread.currentThread().interrupt();
				}
			}
			listenerDelegator.importFinished(jobId);
		}
	}

	protected boolean abortImport(CmfType type, int errors) {
		return false;
	}

	protected CmfNameFixer<V> getNameFixer() {
		return null;
	}

	public static ImportEngine<?, ?, ?, ?, ?, ?> getImportEngine(String targetName) {
		return TransferEngine.getTransferEngine(ImportEngine.class, targetName);
	}

	protected void initContext(C ctx) {
	}

	protected void prepareImport(ImportState importState) throws CmfStorageException, ImportException {
		// In case we wish to do something before the import process runs...
	}

	protected void importFinalized(ImportState importState) throws CmfStorageException, ImportException {
		// In case we need to do some cleanup
	}

	protected void importFailed(ImportState importState) throws CmfStorageException, ImportException {
		// In case we need to do some cleanup
	}

	@Override
	protected void getSupportedSettings(Collection<TransferEngineSetting> settings) {
		for (ImportSetting s : ImportSetting.values()) {
			settings.add(s);
		}
	}
}