/*******************************************************************************
 * #%L
 * Armedia Caliente
 * %%
 * Copyright (C) 2013 - 2019 Armedia, LLC
 * %%
 * This file is part of the Caliente software.
 *
 * If the software was purchased under a paid Caliente license, the terms of
 * the paid license agreement will prevail.  Otherwise, the software is
 * provided under the following open source license terms:
 *
 * Caliente is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Caliente is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Caliente. If not, see <http://www.gnu.org/licenses/>.
 * #L%
 *******************************************************************************/
package com.armedia.caliente.engine.importer;

import java.io.File;
import java.lang.reflect.InvocationHandler;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.caliente.engine.SessionFactory;
import com.armedia.caliente.engine.SessionFactoryException;
import com.armedia.caliente.engine.SessionWrapper;
import com.armedia.caliente.engine.TransferEngine;
import com.armedia.caliente.engine.TransferEngineSetting;
import com.armedia.caliente.engine.TransferException;
import com.armedia.caliente.engine.TransferSetting;
import com.armedia.caliente.engine.WarningTracker;
import com.armedia.caliente.engine.dynamic.filter.ObjectFilter;
import com.armedia.caliente.engine.dynamic.filter.ObjectFilterException;
import com.armedia.caliente.engine.dynamic.transformer.Transformer;
import com.armedia.caliente.engine.dynamic.transformer.TransformerException;
import com.armedia.caliente.engine.dynamic.transformer.mapper.AttributeMapper;
import com.armedia.caliente.engine.dynamic.transformer.mapper.schema.SchemaService;
import com.armedia.caliente.engine.dynamic.transformer.mapper.schema.SchemaServiceException;
import com.armedia.caliente.engine.tools.DefaultNameFixer;
import com.armedia.caliente.engine.tools.MappingTools;
import com.armedia.caliente.store.CmfAttributeTranslator;
import com.armedia.caliente.store.CmfContentStore;
import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.store.CmfObjectCounter;
import com.armedia.caliente.store.CmfObjectHandler;
import com.armedia.caliente.store.CmfObjectRef;
import com.armedia.caliente.store.CmfObjectStore;
import com.armedia.caliente.store.CmfRequirementInfo;
import com.armedia.caliente.store.CmfStorageException;
import com.armedia.caliente.store.CmfValue;
import com.armedia.caliente.tools.Closer;
import com.armedia.commons.utilities.CfgTools;
import com.armedia.commons.utilities.SynchronizedCounter;
import com.armedia.commons.utilities.Tools;
import com.armedia.commons.utilities.line.LineIterator;
import com.armedia.commons.utilities.line.LineScanner;

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
						this.log.warn("An invalid value made it into the work queue somehow: {}", this.batch);
						this.batch.markCompleted();
						return null;
					}

					if (this.log.isDebugEnabled()) {
						this.log.debug("Polled a batch with {} items", this.batch.contents.size());
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
							this.log.debug("Got session [{}]", session.getId());
						}

						this.listenerDelegator.objectHistoryImportStarted(this.importState.jobId, this.batch.type,
							this.batch.id, this.batch.contents.size());
						int i = 0;
						batch: for (CmfObject<VALUE> next : this.batch.contents) {
							if (failBatch) {
								this.log.error("Batch has been failed - will not process {} ({})",
									next.getDescription(), ImportResult.SKIPPED.name());
								this.listenerDelegator.objectImportStarted(this.importState.jobId, next);
								this.listenerDelegator.objectImportCompleted(this.importState.jobId, next,
									ImportOutcome.SKIPPED);
								continue batch;
							}

							final CONTEXT ctx = this.contextFactory.newContext(next.getId(), next.getType(),
								session.get(), i);
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
												this.log.error("The required {} for {} has not been imported yet",
													req.getShortLabel(), next.getDescription());
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
													this.log.error(
														"The required {} for {} was {}, can't import the object (extra info = {})",
														req.getShortLabel(), next.getDescription(),
														req.getStatus().name(), req.getData());
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

								final CmfObject.Archetype storedType = next.getType();
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
									if (this.batch.strategy.isFailBatchOnError()) {
										// If we're supposed to kill the batch, fail all
										// the other objects
										failBatch = true;
										this.log.debug(
											"Objects of type [{}] require that the remainder of the batch fail if an object fails",
											storedType);
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
					this.log.error("Uncaught exception while processing batch [{}::{}]", this.batch.type.name(),
						this.batch.id, e);
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

	private static enum BatchStatus {
		//
		PENDING, PROCESSED, ABORTED;
	}

	private class Batch {
		private final CmfObject.Archetype type;
		private final String id;
		private final Collection<CmfObject<VALUE>> contents;
		private final ImportStrategy strategy;
		private BatchStatus status = BatchStatus.PENDING;

		private Batch() {
			this(null, null, null, null);
		}

		private Batch(CmfObject.Archetype type, String id, Collection<CmfObject<VALUE>> contents,
			ImportStrategy strategy) {
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
				"Batch [type=%s, id=%s, status=%s, strategy.parallel=%s, strategy.failBatchOnError=%s, contents=%s]",
				this.type, this.id, this.status, this.strategy.isParallelCapable(), this.strategy.isFailBatchOnError(),
				this.contents);
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

		private void objectTypeImportFinished(UUID jobId, CmfObject.Archetype objectType) {
			this.listenerProxy.objectTypeImportFinished(jobId, objectType,
				getStoredObjectCounter().getCounters(objectType));
		}

		private void importFinished(UUID jobId) {
			this.listenerProxy.importFinished(jobId, getStoredObjectCounter().getCummulative());
		}
	}

	protected ImportEngine(ENGINE_FACTORY factory, Logger output, WarningTracker warningTracker, File baseData,
		CmfObjectStore<?> objectStore, CmfContentStore<?, ?> contentStore, CfgTools settings) {
		super(factory, ImportResult.class, output, warningTracker, baseData, objectStore, contentStore, settings,
			"import");
	}

	protected abstract ImportStrategy getImportStrategy(CmfObject.Archetype type);

	protected final ExecutorService newExecutor(int threadCount) {
		return new ThreadPoolExecutor(threadCount, threadCount, 30, TimeUnit.SECONDS,
			new LinkedBlockingQueue<Runnable>());
	}

	protected final AttributeMapper getAttributeMapper(CfgTools cfg, SchemaService schemaService) throws Exception {
		if (schemaService == null) { return null; }

		String mapperDefault = String.format("%s%s", this.cfgNamePrefix, AttributeMapper.getDefaultLocation());
		String mapper = cfg.getString(ImportSetting.ATTRIBUTE_MAPPING);
		String residualsPrefix = cfg.getString(ImportSetting.RESIDUALS_PREFIX);

		return AttributeMapper.getAttributeMapper(schemaService, Tools.coalesce(mapper, mapperDefault), residualsPrefix,
			false);
	}

	private Stream<CmfObjectRef> getObjectRestrictions(CfgTools cfg) throws ImportException {
		Collection<String> source = cfg.getStrings(ImportSetting.RESTRICT_TO);
		if ((source == null) || source.isEmpty()) { return null; }

		LineIterator it = new LineScanner().iterator(source);
		return it.stream().filter(StringUtils::isNotEmpty).map(ImportRestriction::parseQuiet).filter(Objects::nonNull);
	}

	@Override
	protected final void work(CmfObjectCounter<ImportResult> counter) throws ImportException, CmfStorageException {

		// First things first...we should only do this if the target repo ID
		// is not the same as the previous target repo - we can tell this by
		// looking at the target mappings.
		// this.log.info("Clearing all previous mappings");
		// objectStore.clearAllMappings();

		final CfgTools configuration = this.settings;
		final ImportState importState = new ImportState(this.output, this.baseData, this.objectStore, this.contentStore,
			configuration);

		try (final SessionFactory<SESSION> sessionFactory = constructSessionFactory(configuration, this.crypto)) {
			SessionWrapper<SESSION> baseSession = null;
			try {
				// We do it like this instead of via try-with-resources because we may want
				// to release this session early rather than late, and we can only do that
				// if we manage the closing manually
				baseSession = sessionFactory.acquireSession();
			} catch (SessionFactoryException e) {
				throw new ImportException("Failed to obtain the import initialization session", e);
			}

			AttributeMapper attributeMapper = null;
			Transformer transformer = null;
			ObjectFilter filter = null;
			ImportContextFactory<SESSION, SESSION_WRAPPER, VALUE, CONTEXT, ?, ?> contextFactory = null;
			ImportDelegateFactory<SESSION, SESSION_WRAPPER, VALUE, CONTEXT, ?> delegateFactory = null;
			try {
				SchemaService schemaService = null;
				try {
					schemaService = newSchemaService(baseSession.get());
				} catch (SchemaServiceException e) {
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
					contextFactory = newContextFactory(baseSession.get(), configuration, this.objectStore,
						this.contentStore, transformer, this.output, this.warningTracker);
					final String fmt = "caliente.import.product.%s";
					this.objectStore.setProperty(String.format(fmt, "name"),
						new CmfValue(contextFactory.getProductName()));
					this.objectStore.setProperty(String.format(fmt, "version"),
						new CmfValue(contextFactory.getProductVersion()));
				} catch (Exception e) {
					throw new ImportException("Failed to configure the context factory to carry out the import", e);
				}

				try {
					delegateFactory = newDelegateFactory(baseSession.get(), configuration);
				} catch (Exception e) {
					throw new ImportException("Failed to configure the delegate factory to carry out the import", e);
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
				if (delegateFactory != null) {
					delegateFactory.close();
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
			}
		} catch (SessionFactoryException e) {
			throw new ImportException("Failed to configure the session factory to carry out the import", e);
		}
	}

	private final void renameObjectsWithMap(final Logger output, Properties p, CmfObjectStore<?> objectStore,
		final String verb) throws ImportException, CmfStorageException {
		// Things happen differently here... since we have a limited scope in which
		// objects require fixing, we don't sweep the whole table, but instead submit
		// the IDs that we want fixed.

		DefaultNameFixer<CmfValue> nameFixer = new DefaultNameFixer<>(output, p);
		if (nameFixer.isEmpty()) {
			output.info("Static name fix map is empty, will not {} any object names", verb);
			return;
		}

		for (final CmfObject.Archetype t : CmfObject.Archetype.values()) {
			Map<String, String> mappings = nameFixer.getMappings(t);
			if ((mappings == null) || mappings.isEmpty()) {
				continue;
			}
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
		final CmfObjectStore<?> objectStore = importState.objectStore;
		final CfgTools settings = importState.cfg;

		final int threadCount = getThreadCount(settings);

		final SynchronizedCounter workerCounter = new SynchronizedCounter();
		final ExecutorService parallelExecutor = newExecutor(threadCount);
		final ExecutorService singleExecutor = Executors.newSingleThreadExecutor();

		if (counter == null) {
			counter = new CmfObjectCounter<>(ImportResult.class);
		}
		final ImportListenerPropagator listenerDelegator = new ImportListenerPropagator(counter);
		final ImportEngineListener listener = listenerDelegator.getListenerProxy();

		try {
			Map<CmfObject.Archetype, Long> containedTypes;
			try {
				containedTypes = objectStore.getStoredObjectTypes();
			} catch (CmfStorageException e) {
				throw new ImportException("Exception raised getting the stored object counts", e);
			}

			// Make sure we have a valid import strategy for every item
			for (CmfObject.Archetype t : containedTypes.keySet()) {
				// If the type is supported, and we have no strategy, it's a problem...
				if (contextFactory.isSupported(t) && (getImportStrategy(t) == null)) {
					throw new ImportException(
						String.format("No import strategy provided for available object type [%s]", t.name()));
				}
			}

			objectStore.clearAttributeMappings();
			try {
				loadPrincipalMappings(objectStore.getValueMapper(), settings);
			} catch (TransferException e) {
				throw new ImportException(e.getMessage(), e.getCause());
			}
			// Ensure the target path exists
			{
				try (SessionWrapper<SESSION> rootSession = sessionFactory.acquireSession()) {
					try {
						rootSession.begin();
						contextFactory.ensureTargetPath(rootSession.get());
						rootSession.commit();
					} catch (Exception e) {
						rootSession.rollback();
						throw new ImportException("Failed to ensure the target path", e);
					}
				} catch (SessionFactoryException e) {
					throw new ImportException("Failed to obtain the root session to ensure the target path", e);
				}
			}

			// Reset all alternate names, to ensure we're not using already-processed names
			output.info("Resetting object names to their base values...");
			objectStore.resetAltNames();
			output.info("Object names successfully reset");

			output.info("Clearing the import plan");
			objectStore.clearImportPlan();
			output.info("Cleared the import plan");

			output.info("Loading the object restriction list");
			try (Stream<CmfObjectRef> objectRestrictions = getObjectRestrictions(importState.cfg)) {
				if (objectRestrictions != null) {
					output.info("Loaded the object restriction list");
					output.info("Applying the object restriction list");
					containedTypes = objectStore.setBulkObjectLoaderFilter(objectRestrictions.iterator());
					output.info("Applied the object restriction list");
				} else {
					output.info("No object restriction list loaded, will import all available objects");
				}
			}

			if (!settings.getBoolean(TransferSetting.NO_FILENAME_MAP)) {
				final Properties p = new Properties();
				final boolean loaded;
				try {
					loaded = MappingTools.loadMap(this.log, settings, TransferSetting.FILENAME_MAP, p);
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
			for (final CmfObject.Archetype type : CmfObject.Archetype.values()) {
				final Long total = containedTypes.get(type);
				if (total == null) {
					this.log.warn("No {} objects are contained in the export", type.name());
					continue;
				}

				if (total < 1) {
					this.log.warn("No {} objects available", type.name());
					continue;
				}

				if (!contextFactory.isSupported(type)) {
					this.log.debug("Ignoring {} objects of type {} due to engine configuration", total, type);
					continue;
				}

				final ImportStrategy strategy = getImportStrategy(type);
				if (strategy.isIgnored()) {
					if (this.log.isDebugEnabled()) {
						this.log.debug(
							"Skipping {} objects of type {} because it's marked as ignored by the given strategy",
							total, type.name());
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

				this.log.info("{} {} objects available, starting deserialization", total, type.name());

				try (final SessionWrapper<SESSION> loaderSession = sessionFactory.acquireSession()) {
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
								SchemaService schemaService = null;
								try {
									schemaService = newSchemaService(loaderSession.get());
								} catch (SchemaServiceException e) {
									throw new CmfStorageException(String.format(
										"Failed to initialize the required schema service while processing %s",
										dataObject.getDescription()), e);
								}

								try {
									"".length();
									dataObject = transformer.transform(objectStore.getValueMapper(),
										translator.getAttributeNameMapper(), schemaService, dataObject);
								} catch (TransformerException e) {
									throw new CmfStorageException(
										String.format("Failed to transform %s", dataObject.getDescription()), e);
								} finally {
									Closer.closeQuietly(schemaService);
								}
							}

							this.contents.add(translator.decodeObject(dataObject));
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
							CmfObject.Archetype storedType = sample.getType();
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
						this.log.info("Waiting for the {} workers to exit...", type.name());
						try {
							workerCounter.waitUntil(0);
						} catch (InterruptedException e) {
							this.log.warn("Interrupted while waiting for an executor thread to exit", e);
							Thread.currentThread().interrupt();
							break;
						}
						this.log.info("All the {} workers have exited", type.name());
					} finally {
						listenerDelegator.objectTypeImportFinished(jobId, type);
					}
				}

				// Finally, decide what to do if errors were encountered
				final Map<ImportResult, Long> results = listenerDelegator.getCounters(type);
				final long errorCount = results.get(ImportResult.FAILED);
				if (abortImport(type, errorCount)) {
					this.log.info("Import aborted due to {} errors detected while importing objects of type {}",
						errorCount, type.name());
					break;
				}
				this.log.info("Work on {} objects completed, continuing with the next object type...", type.name());
			}

			// Shut down the executor
			parallelExecutor.shutdown();
			singleExecutor.shutdown();

			// If there are still pending workers, then wait for them to finish for up to 5
			// minutes
			long pending = workerCounter.get();
			if (pending > 0) {
				this.log.info("Waiting for pending workers to terminate (maximum 5 minutes, {} pending workers)",
					pending);
				try {
					workerCounter.waitUntil(0, 5, TimeUnit.MINUTES);
				} catch (InterruptedException e) {
					this.log.warn("Interrupted while waiting for normal executor termination", e);
					Thread.currentThread().interrupt();
				}
			}
			return listenerDelegator.getStoredObjectCounter();
		} finally

		{
			parallelExecutor.shutdownNow();
			singleExecutor.shutdownNow();

			long pending = workerCounter.get();
			if (pending > 0) {
				try {
					this.log.info(
						"Waiting an additional 60 seconds for worker termination as a contingency ({} pending workers)",
						pending);
					workerCounter.waitUntil(0, 1, TimeUnit.MINUTES);
				} catch (InterruptedException e) {
					this.log.warn("Interrupted while waiting for immediate executor termination", e);
					Thread.currentThread().interrupt();
				}
			}
			listenerDelegator.importFinished(jobId);
		}
	}

	protected boolean abortImport(CmfObject.Archetype type, long errors) {
		return false;
	}

	protected abstract SchemaService newSchemaService(SESSION session) throws SchemaServiceException;

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