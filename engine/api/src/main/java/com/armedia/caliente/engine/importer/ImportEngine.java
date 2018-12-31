package com.armedia.caliente.engine.importer;

import java.io.File;
import java.lang.reflect.InvocationHandler;
import java.util.Collection;
import java.util.EnumMap;
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.caliente.engine.SessionFactory;
import com.armedia.caliente.engine.SessionWrapper;
import com.armedia.caliente.engine.TransferEngine;
import com.armedia.caliente.engine.TransferEngineSetting;
import com.armedia.caliente.engine.TransferException;
import com.armedia.caliente.engine.WarningTracker;
import com.armedia.caliente.engine.dynamic.filter.ObjectFilter;
import com.armedia.caliente.engine.dynamic.filter.ObjectFilterException;
import com.armedia.caliente.engine.dynamic.mapper.AttributeMapper;
import com.armedia.caliente.engine.dynamic.mapper.schema.SchemaService;
import com.armedia.caliente.engine.dynamic.transformer.Transformer;
import com.armedia.caliente.engine.dynamic.transformer.TransformerException;
import com.armedia.caliente.engine.tools.MappingTools;
import com.armedia.caliente.store.CmfAttributeTranslator;
import com.armedia.caliente.store.CmfContentStore;
import com.armedia.caliente.store.CmfNameFixer;
import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.store.CmfObjectCounter;
import com.armedia.caliente.store.CmfObjectHandler;
import com.armedia.caliente.store.CmfObjectRef;
import com.armedia.caliente.store.CmfObjectStore;
import com.armedia.caliente.store.CmfRequirementInfo;
import com.armedia.caliente.store.CmfStorageException;
import com.armedia.caliente.store.CmfType;
import com.armedia.caliente.store.CmfValue;
import com.armedia.caliente.tools.Closer;
import com.armedia.commons.utilities.CfgTools;
import com.armedia.commons.utilities.SynchronizedCounter;
import com.armedia.commons.utilities.Tools;

public abstract class ImportEngine<//
	SESSION, //
	SESSION_WRAPPER extends SessionWrapper<SESSION>, //
	VALUE, //
	CONTEXT extends ImportContext<SESSION, VALUE, CONTEXT_FACTORY>, //
	CONTEXT_FACTORY extends ImportContextFactory<SESSION, SESSION_WRAPPER, VALUE, CONTEXT, ?, ?>, //
	DELEGATE_FACTORY extends ImportDelegateFactory<SESSION, SESSION_WRAPPER, VALUE, CONTEXT, ?>, //
	ENGINE_FACTORY extends ImportEngineFactory<SESSION, VALUE, CONTEXT, CONTEXT_FACTORY, DELEGATE_FACTORY, ?> //
> extends
	TransferEngine<ImportEngineListener, ImportResult, ImportException, SESSION, VALUE, CONTEXT, CONTEXT_FACTORY, DELEGATE_FACTORY, ENGINE_FACTORY> {

	private class BatchWorker implements Callable<Map<String, Collection<ImportOutcome>>> {

		private final SynchronizedCounter workerCounter;

		private final Logger log = LoggerFactory.getLogger(getClass());
		private final SessionFactory<SESSION> sessionFactory;
		private final ImportEngineListener listenerDelegator;
		private final ImportState importState;
		private final Batch batch;
		private final ImportContextFactory<SESSION, SESSION_WRAPPER, VALUE, CONTEXT, ?, ?> contextFactory;
		private final ImportDelegateFactory<SESSION, SESSION_WRAPPER, VALUE, CONTEXT, ?> delegateFactory;

		private BatchWorker(Batch batch, SynchronizedCounter synchronizedCounter,
			SessionFactory<SESSION> sessionFactory, ImportEngineListener listenerDelegator, ImportState importState,
			final ImportContextFactory<SESSION, SESSION_WRAPPER, VALUE, CONTEXT, ?, ?> contextFactory,
			final ImportDelegateFactory<SESSION, SESSION_WRAPPER, VALUE, CONTEXT, ?> delegateFactory) {
			this.sessionFactory = sessionFactory;
			this.listenerDelegator = listenerDelegator;
			this.importState = importState;
			this.batch = batch;
			this.contextFactory = contextFactory;
			this.delegateFactory = delegateFactory;
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

					final SessionWrapper<SESSION> session;
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
						batch: for (CmfObject<VALUE> next : this.batch.contents) {
							if (failBatch) {
								this.log.error(String.format("Batch has been failed - will not process %s (%s)",
									next.getDescription(), ImportResult.SKIPPED.name()));
								this.listenerDelegator.objectImportStarted(this.importState.jobId, next);
								this.listenerDelegator.objectImportCompleted(this.importState.jobId, next,
									ImportOutcome.SKIPPED);
								continue batch;
							}

							final CONTEXT ctx = this.contextFactory.newContext(next.getId(), next.getType(),
								session.getWrapped(), i);
							ImportResult result = ImportResult.FAILED;
							String info = null;
							try {
								initContext(ctx);

								if (ctx.getSettings().getBoolean(ImportSetting.VALIDATE_REQUIREMENTS)) {
									// Check if its requirements have been imported. If not, skip it
									Collection<CmfRequirementInfo<ImportResult>> requirements = this.importState.objectStore
										.getRequirementInfo(ImportResult.class, next);
									if (requirements != null) {
										for (CmfRequirementInfo<ImportResult> req : requirements) {
											ImportResult status = req.getStatus();
											if (status == null) {
												failBatch = true;
												this.log.error(
													String.format("The required %s for %s has not been imported yet",
														req.getShortLabel(), next.getDescription()));
												this.listenerDelegator.objectImportStarted(this.importState.jobId,
													next);
												this.listenerDelegator.objectImportCompleted(this.importState.jobId,
													next, ImportOutcome.SKIPPED);
												continue batch;
											}

											switch (req.getStatus()) {
												case FAILED:
												case SKIPPED:
													// Can't continue... a requirement is missing
													failBatch = true;
													this.log.error(String.format(
														"The required %s for %s was %s, can't import the object (extra info = %s)",
														req.getShortLabel(), next.getDescription(),
														req.getStatus().name(), req.getData()));
													this.listenerDelegator.objectImportStarted(this.importState.jobId,
														next);
													this.listenerDelegator.objectImportCompleted(this.importState.jobId,
														next, ImportOutcome.SKIPPED);
													continue batch;
												default:
													break;
											}
										}
									}
								}

								final CmfType storedType = next.getType();
								final boolean useTx = getImportStrategy(storedType).isSupportsTransactions();
								if (useTx) {
									session.begin();
								}
								try {
									this.listenerDelegator.objectImportStarted(this.importState.jobId, next);
									ImportDelegate<?, SESSION, SESSION_WRAPPER, VALUE, CONTEXT, ?, ?> delegate = this.delegateFactory
										.newImportDelegate(next);
									final Collection<ImportOutcome> outcome = delegate.importObject(getTranslator(),
										ctx);
									if (outcome.isEmpty()) {
										result = ImportResult.SKIPPED;
									} else {
										result = null;
									}
									outcomes.put(next.getId(), outcome);
									for (ImportOutcome o : outcome) {
										this.listenerDelegator.objectImportCompleted(this.importState.jobId, next, o);
										if (result == null) {
											result = o.getResult();
										}

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
													msg = String.format("Outcome was (%s) for %s", o.getResult(), next);
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
									info = Tools.dumpStackTrace(t);
									if (useTx) {
										session.rollback();
									}
									this.listenerDelegator.objectImportFailed(this.importState.jobId, next, t);
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
								} finally {
									ctx.getObjectStore().setImportStatus(next, result, info);
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

	private static final Pattern MAP_KEY_PARSER = Pattern.compile("^\\s*([^#\\s]+)\\s*#\\s*(.+)\\s*$");

	private static enum BatchStatus {
		//
		PENDING, PROCESSED, ABORTED;
	}

	private class Batch {
		private final CmfType type;
		private final String id;
		private final Collection<CmfObject<VALUE>> contents;
		private final ImportStrategy strategy;
		private BatchStatus status = BatchStatus.PENDING;

		private Batch() {
			this(null, null, null, null);
		}

		private Batch(CmfType type, String id, Collection<CmfObject<VALUE>> contents, ImportStrategy strategy) {
			this.type = type;
			this.id = id;
			this.contents = contents;
			this.strategy = strategy;
		}

		private void markCompleted() {
			if (this.status == BatchStatus.PENDING) {
				this.status = BatchStatus.PROCESSED;
			}
		}

		private void markAborted(Throwable thrown) {
			if (this.status == BatchStatus.PENDING) {
				this.status = BatchStatus.ABORTED;
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

	private class ImportListenerPropagator extends ListenerPropagator<ImportResult> implements InvocationHandler {

		private ImportListenerPropagator(CmfObjectCounter<ImportResult> counter) {
			super(counter, getListeners(), ImportEngineListener.class);
		}

		@Override
		protected void handleMethod(String name, Object[] args) throws Throwable {
			CmfObjectRef object = null;
			for (Object o : args) {
				if (CmfObjectRef.class.isInstance(o)) {
					object = CmfObjectRef.class.cast(o);
					break;
				}
			}

			switch (name) {
				case "importStarted":
					getStoredObjectCounter().reset();
					break;
				case "objectImportCompleted":
					ImportOutcome outcome = null;
					for (Object o : args) {
						if (ImportOutcome.class.isInstance(o)) {
							outcome = ImportOutcome.class.cast(o);
							break;
						}
					}
					getStoredObjectCounter().increment(object.getType(), outcome.getResult());
					break;
				case "objectImportFailed":
					getStoredObjectCounter().increment(object.getType(), ImportResult.FAILED);
					break;
			}
		}

		private void objectTypeImportFinished(UUID jobId, CmfType objectType) {
			this.listenerProxy.objectTypeImportFinished(jobId, objectType,
				getStoredObjectCounter().getCounters(objectType));
		}

		private void importFinished(UUID jobId) {
			this.listenerProxy.importFinished(jobId, getStoredObjectCounter().getCummulative());
		}
	}

	protected ImportEngine(ENGINE_FACTORY factory, Logger output, WarningTracker warningTracker, File baseData,
		CmfObjectStore<?, ?> objectStore, CmfContentStore<?, ?, ?> contentStore, Map<String, ?> settings) {
		super(factory, ImportResult.class, output, warningTracker, baseData, objectStore, contentStore, settings,
			"import");
	}

	protected abstract ImportStrategy getImportStrategy(CmfType type);

	protected final ExecutorService newExecutor(int threadCount) {
		return new ThreadPoolExecutor(threadCount, threadCount, 30, TimeUnit.SECONDS,
			new LinkedBlockingQueue<Runnable>());
	}

	protected final AttributeMapper getAttributeMapper(CfgTools cfg, SchemaService schemaService) throws Exception {
		if (schemaService == null) { return null; }

		String mapperDefault = String.format("%s%s", this.cfgNamePrefix, AttributeMapper.getDefaultLocation());
		String mapper = cfg.getString(ImportSetting.ATTRIBUTE_MAPPING.getLabel());
		String residualsPrefix = cfg.getString(ImportSetting.RESIDUALS_PREFIX.getLabel());

		return AttributeMapper.getAttributeMapper(schemaService, Tools.coalesce(mapper, mapperDefault), residualsPrefix,
			false);
	}

	@Override
	protected final void work(CmfObjectCounter<ImportResult> counter) throws ImportException, CmfStorageException {

		// First things first...we should only do this if the target repo ID
		// is not the same as the previous target repo - we can tell this by
		// looking at the target mappings.
		// this.log.info("Clearing all previous mappings");
		// objectStore.clearAllMappings();Object

		final CfgTools configuration = this.settings;
		final ImportState importState = new ImportState(this.output, this.baseData, this.objectStore, this.contentStore,
			configuration);
		final SessionFactory<SESSION> sessionFactory;
		try {
			sessionFactory = newSessionFactory(configuration, this.crypto);
		} catch (Exception e) {
			throw new ImportException("Failed to configure the session factory to carry out the import", e);
		}

		try {
			SessionWrapper<SESSION> baseSession = null;
			try {
				baseSession = sessionFactory.acquireSession();
			} catch (Exception e) {
				throw new ImportException("Failed to obtain the import initialization session", e);
			}

			ImportDelegateFactory<SESSION, SESSION_WRAPPER, VALUE, CONTEXT, ?> delegateFactory = null;
			AttributeMapper attributeMapper = null;
			Transformer transformer = null;
			ObjectFilter filter = null;
			ImportContextFactory<SESSION, SESSION_WRAPPER, VALUE, CONTEXT, ?, ?> contextFactory = null;
			try {
				try {
					delegateFactory = newDelegateFactory(baseSession.getWrapped(), configuration);
				} catch (Exception e) {
					throw new ImportException("Failed to configure the delegate factory to carry out the import", e);
				}

				SchemaService schemaService = null;
				try {
					schemaService = delegateFactory.newSchemaService(baseSession.getWrapped());
				} catch (Exception e) {
					throw new ImportException("Failed to initialize the required schema service", e);
				}

				if (schemaService != null) {
					try {
						attributeMapper = getAttributeMapper(configuration, schemaService);
					} catch (Exception e) {
						throw new ImportException("Failed to initialize the configured attribute mappings", e);
					} finally {
						Closer.closeQuietly(schemaService);
					}
				}

				try {
					transformer = getTransformer(configuration, attributeMapper);
				} catch (Exception e) {
					throw new ImportException("Failed to initialize the configured object transformations", e);
				}

				try {
					filter = getFilter(configuration);
				} catch (Exception e) {
					throw new ImportException("Failed to configure the configured object filters", e);
				}

				try {
					contextFactory = newContextFactory(baseSession.getWrapped(), configuration, this.objectStore,
						this.contentStore, transformer, this.output, this.warningTracker);
				} catch (Exception e) {
					throw new ImportException("Failed to configure the context factory to carry out the import", e);
				}

				try {
					baseSession.close();
				} finally {
					baseSession = null;
				}

				runImportImpl(importState, sessionFactory, counter, contextFactory, delegateFactory, transformer,
					filter);
			} finally {
				if (baseSession != null) {
					baseSession.close();
				}

				if (contextFactory != null) {
					contextFactory.close();
				}
				if (filter != null) {
					filter.close();
				}
				if (transformer != null) {
					transformer.close();
				}
				if (delegateFactory != null) {
					delegateFactory.close();
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
			CmfNameFixer<CmfValue> nameFixer = new CmfNameFixer<CmfValue>() {

				@Override
				public boolean supportsType(CmfType type) {
					return (type == t);
				}

				@Override
				public String fixName(CmfObject<CmfValue> dataObject) throws CmfStorageException {
					return mappings.get(dataObject.getId());
				}

				@Override
				public boolean handleException(Exception e) {
					return false;
				}

				@Override
				public void nameFixed(CmfObject<CmfValue> dataObject, String oldName, String newName) {
					output.info("Renamed {} with ID[{}] from [{}] to [{}]", dataObject.getType(), dataObject.getId(),
						oldName, newName);
				}
			};
			output.info("Trying to {} {} {} names...", verb, mappings.size(), t.name());
			final int fixes = objectStore.fixObjectNames(nameFixer, t, mappings.keySet());
			output.info("Modified {} {} objects", fixes, t.name());
		}
	}

	private final CmfObjectCounter<ImportResult> runImportImpl(final ImportState importState,
		final SessionFactory<SESSION> sessionFactory, CmfObjectCounter<ImportResult> counter,
		final ImportContextFactory<SESSION, SESSION_WRAPPER, VALUE, CONTEXT, ?, ?> contextFactory,
		final ImportDelegateFactory<SESSION, SESSION_WRAPPER, VALUE, CONTEXT, ?> delegateFactory,
		final Transformer transformer, final ObjectFilter filter) throws ImportException, CmfStorageException {
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
		final ImportListenerPropagator listenerDelegator = new ImportListenerPropagator(counter);
		final ImportEngineListener listener = listenerDelegator.getListenerProxy();

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
			try {
				loadPrincipalMappings(objectStore.getValueMapper(), settings);
			} catch (TransferException e) {
				throw new ImportException(e.getMessage(), e.getCause());
			}
			// Ensure the target path exists
			{
				final SessionWrapper<SESSION> rootSession;
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

			output.info("Clearing the import plan");
			objectStore.clearImportPlan();
			output.info("Cleared the import plan");

			if (!settings.getBoolean(ImportSetting.NO_FILENAME_MAP)) {
				final Properties p = new Properties();
				final boolean loaded;
				try {
					loaded = MappingTools.loadMap(this.log, settings, ImportSetting.FILENAME_MAP, p);
				} catch (TransferException e) {
					throw new ImportException(e.getMessage(), e.getCause());
				}

				if (loaded) {
					// Things happen differently here... since we have a limited scope in which
					// objects require fixing, we don't sweep the whole table, but instead submit
					// the IDs that we want fixed.
					output.info("Applying static object renames for {} objects...", p.size());
					renameObjectsWithMap(output, p, objectStore, "statically transform");
					output.info("Static renames completed");
				}
			}

			listener.importStarted(importState, containedTypes);
			final CmfAttributeTranslator<VALUE> translator = getTranslator();
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
				listener.objectTypeImportStarted(jobId, type, total);

				// Start the workers
				// If we don't support parallelization at any level, then we simply use a
				// single worker to do everything. Otherwise, the rest of the strategy will
				// dictate how the parallelism will work (i.e. batches are parallel and
				// their contents serialized, or batches' contents are parallel and batches
				// are serialized).
				final ExecutorService executor = (strategy.isParallelCapable() ? parallelExecutor : singleExecutor);

				this.log.info(String.format("%d %s objects available, starting deserialization", total, type.name()));
				try {
					objectStore.loadObjects(type, new CmfObjectHandler<CmfValue>() {
						private boolean filtered = false;
						private List<CmfObject<VALUE>> contents = null;

						@Override
						public boolean newTier(int tier) throws CmfStorageException {
							output.info("Processing {} tier {}", type.name(), tier);
							return true;
						}

						@Override
						public boolean newHistory(String historyId) throws CmfStorageException {
							this.filtered = false;
							this.contents = new LinkedList<>();
							return true;
						}

						@Override
						public boolean handleObject(CmfObject<CmfValue> dataObject) throws CmfStorageException {
							// Shortcut... if we're already filtered, just blow past the next
							// objects
							if (this.filtered) { return true; }

							if (filter != null) {
								try {
									if (!filter.accept(dataObject, objectStore.getValueMapper())) {
										this.filtered = true;
										return true;
									}
								} catch (ObjectFilterException e) {
									throw new CmfStorageException(
										String.format("Filtering logic exception while processing %s",
											dataObject.getDescription()),
										e);
								}
							}

							if (transformer != null) {
								try {
									dataObject = transformer.transform(objectStore.getValueMapper(), dataObject);
								} catch (TransformerException e) {
									throw new CmfStorageException(
										String.format("Failed to transform %s", dataObject.getDescription()), e);
								}
							}

							CmfObject<VALUE> decoded = translator.decodeObject(dataObject);
							// TODO: Perform attribute mapping here?
							this.contents.add(decoded);
							return true;
						}

						@Override
						public boolean endHistory(String historyId, boolean ok) throws CmfStorageException {
							// Is there anything to process?
							if ((this.contents == null) || this.contents.isEmpty()) { return true; }

							// Was it filtered?
							if (this.filtered) {
								// Mark it as filtered in the import status!!
								for (CmfObject<VALUE> object : this.contents) {
									objectStore.setImportStatus(object, ImportResult.SKIPPED,
										"Excluded by filtering logic");
								}
								return true;
							}

							// Ok... we have stuff to process that wasn't filtered, process it!
							CmfObject<?> sample = this.contents.get(0);
							CmfType storedType = sample.getType();
							ImportStrategy strategy = getImportStrategy(storedType);
							// We will have already validated that a valid strategy is provided
							// for all stored types
							try {
								executor.submit(new BatchWorker(
									new Batch(storedType, historyId, this.contents, strategy), workerCounter,
									sessionFactory, listener, importState, contextFactory, delegateFactory));
							} finally {
								this.contents = null;
							}
							return true;
						}

						@Override
						public boolean endTier(int tierId, boolean ok) throws CmfStorageException {
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

	protected CmfNameFixer<VALUE> getNameFixer(Logger output) {
		return null;
	}

	protected void initContext(CONTEXT ctx) {
	}

	@Override
	protected void getSupportedSettings(Collection<TransferEngineSetting> settings) {
		for (ImportSetting s : ImportSetting.values()) {
			settings.add(s);
		}
	}

	@Override
	protected ImportException newException(String message, Throwable cause) {
		return new ImportException(message, cause);
	}
}