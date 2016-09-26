/**
 *
 */

package com.armedia.cmf.engine.exporter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;

import org.slf4j.Logger;

import com.armedia.cmf.engine.CmfCrypt;
import com.armedia.cmf.engine.ContextFactory;
import com.armedia.cmf.engine.SessionFactory;
import com.armedia.cmf.engine.SessionWrapper;
import com.armedia.cmf.engine.TransferEngine;
import com.armedia.cmf.engine.TransferEngineSetting;
import com.armedia.cmf.engine.TransferSetting;
import com.armedia.cmf.storage.CmfContentInfo;
import com.armedia.cmf.storage.CmfContentStore;
import com.armedia.cmf.storage.CmfObject;
import com.armedia.cmf.storage.CmfObjectCounter;
import com.armedia.cmf.storage.CmfObjectSpec;
import com.armedia.cmf.storage.CmfObjectStore;
import com.armedia.cmf.storage.CmfStorageException;
import com.armedia.cmf.storage.CmfType;
import com.armedia.commons.utilities.CfgTools;
import com.armedia.commons.utilities.CloseableIterator;
import com.armedia.commons.utilities.PooledWorkers;

/**
 * @author diego
 *
 */
public abstract class ExportEngine<S, W extends SessionWrapper<S>, V, C extends ExportContext<S, V, CF>, CF extends ExportContextFactory<S, W, V, C, ?>, DF extends ExportDelegateFactory<S, W, V, C, ?>>
	extends TransferEngine<S, V, C, CF, DF, ExportEngineListener> {

	private class Result {
		private final Long objectNumber;
		private final CmfObject<V> marshaled;
		private final ExportSkipReason message;
		private final String extraInfo;

		public Result(Long objectNumber, CmfObject<V> marshaled) {
			this.objectNumber = objectNumber;
			this.marshaled = marshaled;
			this.message = null;
			this.extraInfo = null;
		}

		public Result(ExportSkipReason message) {
			this(message, null);
		}

		public Result(ExportSkipReason message, String extraInfo) {
			this.objectNumber = null;
			this.marshaled = null;
			this.message = message;
			this.extraInfo = null;
		}
	}

	private class ExportListenerDelegator extends ListenerDelegator<ExportResult> implements ExportEngineListener {

		private final Collection<ExportEngineListener> listeners = getListeners();

		private ExportListenerDelegator(CmfObjectCounter<ExportResult> counter) {
			super(counter);
		}

		@Override
		public void exportStarted(ExportState exportState) {
			getStoredObjectCounter().reset();
			for (ExportEngineListener l : this.listeners) {
				try {
					l.exportStarted(exportState);
				} catch (Exception e) {
					if (this.log.isDebugEnabled()) {
						this.log.error("Exception caught during listener propagation", e);
					}
				}
			}
		}

		@Override
		public void objectExportStarted(UUID jobId, CmfType objectType, String objectId) {
			for (ExportEngineListener l : this.listeners) {
				try {
					l.objectExportStarted(jobId, objectType, objectId);
				} catch (Exception e) {
					if (this.log.isDebugEnabled()) {
						this.log.error("Exception caught during listener propagation", e);
					}
				}
			}
		}

		@Override
		public void objectExportCompleted(UUID jobId, CmfObject<?> object, Long objectNumber) {
			getStoredObjectCounter().increment(object.getType(), ExportResult.EXPORTED);
			for (ExportEngineListener l : this.listeners) {
				try {
					l.objectExportCompleted(jobId, object, objectNumber);
				} catch (Exception e) {
					if (this.log.isDebugEnabled()) {
						this.log.error("Exception caught during listener propagation", e);
					}
				}
			}
		}

		@Override
		public void objectSkipped(UUID jobId, CmfType objectType, String objectId, ExportSkipReason reason,
			String extraInfo) {
			getStoredObjectCounter().increment(objectType, ExportResult.SKIPPED);
			for (ExportEngineListener l : this.listeners) {
				try {
					l.objectSkipped(jobId, objectType, objectId, reason, extraInfo);
				} catch (Exception e) {
					if (this.log.isDebugEnabled()) {
						this.log.error("Exception caught during listener propagation", e);
					}
				}
			}
		}

		@Override
		public void objectExportFailed(UUID jobId, CmfType objectType, String objectId, Throwable thrown) {
			getStoredObjectCounter().increment(objectType, ExportResult.FAILED);
			for (ExportEngineListener l : this.listeners) {
				try {
					l.objectExportFailed(jobId, objectType, objectId, thrown);
				} catch (Exception e) {
					if (this.log.isDebugEnabled()) {
						this.log.error("Exception caught during listener propagation", e);
					}
				}
			}
		}

		@Override
		public void exportFinished(ExportState exportState, Map<CmfType, Integer> summary) {
			for (ExportEngineListener l : this.listeners) {
				try {
					l.exportFinished(exportState, summary);
				} catch (Exception e) {
					if (this.log.isDebugEnabled()) {
						this.log.error("Exception caught during listener propagation", e);
					}
				}
			}
		}
	}

	protected ExportEngine(CmfCrypt crypto) {
		super(crypto);
	}

	protected ExportEngine(CmfCrypt crypto, boolean supportsDuplicateNames) {
		super(crypto, supportsDuplicateNames);
	}

	private Result exportObject(ExportState exportState, final ExportTarget referrent, final ExportTarget target,
		final ExportDelegate<?, S, W, V, C, ?, ?> sourceObject, final C ctx,
		final ExportListenerDelegator listenerDelegator, final Map<ExportTarget, ExportStatus> statusMap)
		throws ExportException, CmfStorageException {
		try {
			listenerDelegator.objectExportStarted(exportState.jobId, target.getType(), target.getId());
			Result result = null;
			if (ctx.isSupported(target.getType())) {
				result = doExportObject(exportState, referrent, target, sourceObject, ctx, listenerDelegator,
					statusMap);
			} else {
				result = new Result(ExportSkipReason.UNSUPPORTED);
			}
			if ((result.objectNumber != null) && (result.marshaled != null)) {
				listenerDelegator.objectExportCompleted(exportState.jobId, result.marshaled, result.objectNumber);
			} else {
				listenerDelegator.objectSkipped(exportState.jobId, target.getType(), target.getId(), result.message,
					result.extraInfo);
			}
			return result;
		} catch (Exception e) {
			listenerDelegator.objectExportFailed(exportState.jobId, target.getType(), target.getId(), e);
			if (e instanceof ExportException) { throw ExportException.class.cast(e); }
			if (e instanceof CmfStorageException) { throw CmfStorageException.class.cast(e); }
			throw RuntimeException.class.cast(e);
		}
	}

	private Result doExportObject(ExportState exportState, final ExportTarget referrent, final ExportTarget target,
		final ExportDelegate<?, S, W, V, C, ?, ?> sourceObject, final C ctx,
		final ExportListenerDelegator listenerDelegator, final Map<ExportTarget, ExportStatus> statusMap)
		throws ExportException, CmfStorageException {
		if (target == null) { throw new IllegalArgumentException("Must provide the original export target"); }
		if (sourceObject == null) { throw new IllegalArgumentException("Must provide the original object to export"); }
		if (ctx == null) { throw new IllegalArgumentException("Must provide a context to operate in"); }
		final CmfType type = target.getType();
		final String id = target.getId();
		String objectLabel = null;
		try {
			objectLabel = sourceObject.getLabel();
		} catch (Exception e) {
			this.log.warn(String.format("Failed to calculate object label for %s (%s)", type, id), e);
			objectLabel = String.format("{Failed to calculate object label: %s}", e.getMessage());
		}
		final String label = String.format("%s [%s](%s)", type, objectLabel, id);

		if (this.log.isTraceEnabled()) {
			this.log.trace(String.format("Attempting export of %s", label));
		}

		final CmfObjectStore<?, ?> objectStore = exportState.objectStore;
		final CmfContentStore<?, ?, ?> streamStore = exportState.streamStore;

		// First, make sure other threads don't work on this same object
		ExportStatus thisStatus = null;
		boolean locked = false;
		try {
			locked = objectStore.lockForStorage(type, id);
			if (locked) {
				// We got the lock, which means we create the locker object
				thisStatus = new ExportStatus(target);
				ExportStatus old = statusMap.put(target, thisStatus);
				if (old != null) { throw new ExportException(String.format(
					"Duplicate export status for [%s] - this should be impossible! This means DB lock markers are broken!",
					target)); }
				if (this.log.isTraceEnabled()) {
					this.log.trace(String.format("Locked %s for storage", label));
				}
			}
		} catch (CmfStorageException e) {
			throw new ExportException(
				String.format("Exception caught attempting to lock a %s for storage [%s](%s)", type, label, id), e);
		}

		if (!locked) {
			if (this.log.isTraceEnabled()) {
				this.log.trace(String.format("%s is already locked for storage, skipping it", label));
			}
			return new Result(ExportSkipReason.ALREADY_LOCKED);
		}

		// Make sure the object hasn't already been exported
		if (objectStore.isStored(type, id)) {
			// Should be impossible, but still guard against it
			if (this.log.isTraceEnabled()) {
				this.log
					.trace(String.format("%s was locked for storage by this thread, but is already stored...", label));
			}
			// Just in case...
			thisStatus.markExported(true);
			return new Result(ExportSkipReason.ALREADY_STORED);
		}

		/*
		try {
		
		} finally {
			try {
				objectStore.unlockForStorage(type, id);
			} catch (CmfStorageException e) {
				if (this.log.isTraceEnabled()) {
					this.log.error(String.format("Failed to unlock the object [%s::%s]", type.name(), id), e);
				}
			} finally {
				statusMap.remove(target);
			}
		}
		*/

		boolean success = false;
		if (referrent != null) {
			ctx.pushReferrent(referrent);
		}
		try {
			if (this.log.isDebugEnabled()) {
				if (referrent != null) {
					this.log.debug(String.format("Exporting %s (referenced by %s)", label, referrent));
				} else {
					this.log.debug(String.format("Exporting %s (from the main search)", label));
				}
			}

			final CmfObject<V> marshaled = sourceObject.marshal(ctx, referrent);
			if (marshaled == null) { return new Result(ExportSkipReason.SKIPPED); }

			Collection<? extends ExportDelegate<?, S, W, V, C, ?, ?>> referenced;
			try {
				referenced = sourceObject.identifyRequirements(marshaled, ctx);
				if (referenced == null) {
					referenced = Collections.emptyList();
				}
			} catch (Exception e) {
				throw new ExportException(String.format("Failed to identify the requirements for %s", label), e);
			}

			if (this.log.isDebugEnabled()) {
				this.log
					.debug(String.format("%s requires %d objects for successful storage", label, referenced.size()));
			}
			// We use a TreeSet to ensure that all our targets are always waited upon in the same
			// order, to avoid deadlocks.
			Collection<ExportTarget> waitTargets = new TreeSet<ExportTarget>();
			for (ExportDelegate<?, S, W, V, C, ?, ?> requirement : referenced) {
				if (requirement.getExportTarget().equals(target)) {
					continue;
				}
				Result r = exportObject(exportState, target, requirement.getExportTarget(), requirement, ctx,
					listenerDelegator, statusMap);
				// If there is no message, the result is success
				if (r.message == null) {
					continue;
				}
				switch (r.message) {
					case ALREADY_LOCKED:
						if (!ctx.isReferrentLoop(requirement.exportTarget)
							&& ctx.shouldWaitForRequirement(target.getType(), requirement.getType())) {
							// Locked, but not stored...we should wait for it to be stored...
							waitTargets.add(requirement.getExportTarget());
						}
						break;
					default:
						// Already stored, skipped, or is unsupported... we need not wait
						break;
				}
			}

			thisStatus.startWait();
			try {
				for (ExportTarget requirement : waitTargets) {
					// We need to wait for each of these to be stored...so find their lock object
					// and listen on it...?
					ExportStatus status = statusMap.get(requirement);
					if (status == null) { throw new ExportException(
						String.format("No export status found for requirement [%s] of %s", requirement, label)); }
					try {
						ctx.printf("Waiting for [%s] from %s (#%d created by %s)", requirement, label,
							status.getObjectNumber(), status.getCreatorThread());
						long waitTime = status.waitUntilExported();
						ctx.printf("Waiting for [%s] from %s for %d ms", requirement, label, waitTime);
					} catch (InterruptedException e) {
						Thread.interrupted();
						throw new ExportException(
							String.format("Thread interrupted waiting on the export of [%s] by %s", requirement, label),
							e);
					}
					if (!status.isSuccessful()) { return new Result(ExportSkipReason.DEPENDENCY_FAILED,
						String.format("A required object [%s] failed to serialize for %s", requirement, label)); }
				}
			} finally {
				thisStatus.endWait();
			}

			try {
				sourceObject.requirementsExported(marshaled, ctx);
			} catch (Exception e) {
				this.log.error(String.format("Failed to run the post-requirements callback for %s", label), e);
			}

			final boolean latestOnly = ctx.getSettings().getBoolean(TransferSetting.LATEST_ONLY);
			if (!latestOnly) {
				try {
					referenced = sourceObject.identifyAntecedents(marshaled, ctx);
					if (referenced == null) {
						referenced = Collections.emptyList();
					}
				} catch (Exception e) {
					throw new ExportException(String.format("Failed to identify the antecedent versions for %s", label),
						e);
				}

				if (this.log.isDebugEnabled()) {
					this.log.debug(String.format("%s requires %d antecedent versions for successful storage", label,
						referenced.size()));
				}
				for (ExportDelegate<?, S, W, V, C, ?, ?> antecedent : referenced) {
					exportObject(exportState, target, antecedent.getExportTarget(), antecedent, ctx, listenerDelegator,
						statusMap);
				}

				try {
					sourceObject.antecedentsExported(marshaled, ctx);
				} catch (Exception e) {
					this.log.error(String.format("Failed to run the post-antecedents callback for %s", label), e);
				}
			}

			// Are there any last-minute properties/attributes to calculate prior to
			// storing the object for posterity?
			try {
				sourceObject.prepareForStorage(ctx, marshaled);
			} catch (Exception e) {
				throw new ExportException(String.format("Failed to prepare the object for storage for %s", label), e);
			}

			final Long ret = objectStore.storeObject(marshaled, getTranslator());
			if (ret == null) {
				// Should be impossible, but still guard against it
				if (this.log.isTraceEnabled()) {
					this.log.trace(String.format("%s was stored by another thread", label));
				}
				return new Result(ExportSkipReason.ALREADY_STORED);
			}

			if (this.log.isDebugEnabled()) {
				this.log.debug(String.format("Executing supplemental storage for %s", label));
			}
			try {
				final boolean includeRenditions = !ctx.getSettings().getBoolean(TransferSetting.NO_RENDITIONS);
				List<CmfContentInfo> cmfContentInfo = sourceObject.storeContent(ctx, getTranslator(), marshaled,
					referrent, streamStore, includeRenditions);
				if ((cmfContentInfo != null) && !cmfContentInfo.isEmpty()) {
					objectStore.setContentInfo(marshaled, cmfContentInfo);
				}
			} catch (Exception e) {
				throw new ExportException(String.format("Failed to execute the content storage for %s", label), e);
			}

			if (this.log.isDebugEnabled()) {
				this.log.debug(String.format("Successfully stored %s as object # %d", label, ret));
			}

			if (!latestOnly) {
				try {
					referenced = sourceObject.identifySuccessors(marshaled, ctx);
					if (referenced == null) {
						referenced = Collections.emptyList();
					}
				} catch (Exception e) {
					throw new ExportException(String.format("Failed to identify the successor versions for %s", label),
						e);
				}

				if (this.log.isDebugEnabled()) {
					this.log.debug(String.format("%s is succeeded by %d additional versions for successful storage",
						label, referenced.size()));
				}
				for (ExportDelegate<?, S, W, V, C, ?, ?> successor : referenced) {
					exportObject(exportState, target, successor.getExportTarget(), successor, ctx, listenerDelegator,
						statusMap);
				}

				try {
					sourceObject.successorsExported(marshaled, ctx);
				} catch (Exception e) {
					this.log.error(String.format("Failed to run the post-successors callback for %s", label), e);
				}
			}

			try {
				referenced = sourceObject.identifyDependents(marshaled, ctx);
				if (referenced == null) {
					referenced = Collections.emptyList();
				}
			} catch (Exception e) {
				throw new ExportException(String.format("Failed to identify the dependents for %s", label), e);
			}

			if (this.log.isDebugEnabled()) {
				this.log.debug(String.format("%s has %d dependent objects to store", label, referenced.size()));
			}
			for (ExportDelegate<?, S, W, V, C, ?, ?> dependent : referenced) {
				exportObject(exportState, target, dependent.getExportTarget(), dependent, ctx, listenerDelegator,
					statusMap);
			}

			try {
				sourceObject.dependentsExported(marshaled, ctx);
			} catch (Exception e) {
				this.log.error(String.format("Failed to run the post-dependents callback for %s", label), e);
			}

			Result result = new Result(ret, marshaled);
			success = true;
			return result;
		} finally {
			thisStatus.markExported(success);
			if (referrent != null) {
				ctx.popReferrent();
			}
		}
	}

	public final CmfObjectCounter<ExportResult> runExport(final Logger output, final CmfObjectStore<?, ?> objectStore,
		final CmfContentStore<?, ?, ?> contentStore, Map<String, ?> settings)
		throws ExportException, CmfStorageException {
		return runExport(output, objectStore, contentStore, settings, null);
	}

	public final CmfObjectCounter<ExportResult> runExport(final Logger output, final CmfObjectStore<?, ?> objectStore,
		final CmfContentStore<?, ?, ?> contentStore, Map<String, ?> settings, CmfObjectCounter<ExportResult> counter)
		throws ExportException, CmfStorageException {
		// We get this at the very top because if this fails, there's no point in continuing.

		final CfgTools configuration = new CfgTools(settings);

		final ExportState exportState = new ExportState(output, objectStore, contentStore, configuration);

		final SessionFactory<S> sessionFactory;
		try {
			sessionFactory = newSessionFactory(configuration, this.crypto);
		} catch (Exception e) {
			throw new ExportException("Failed to configure the session factory to carry out the export", e);
		}

		try {
			SessionWrapper<S> baseSession = null;
			try {
				baseSession = sessionFactory.acquireSession();
			} catch (Exception e) {
				throw new ExportException("Failed to obtain the main export session", e);
			}

			ContextFactory<S, V, C, ?> contextFactory = null;
			DF delegateFactory = null;
			try {
				try {
					contextFactory = newContextFactory(baseSession.getWrapped(), configuration);
				} catch (Exception e) {
					throw new ExportException("Failed to configure the context factory to carry out the export", e);
				}

				try {
					delegateFactory = newDelegateFactory(baseSession.getWrapped(), configuration);
				} catch (Exception e) {
					throw new ExportException("Failed to configure the delegate factory to carry out the export", e);
				}

				return runExportImpl(exportState, counter, sessionFactory, baseSession, contextFactory,
					delegateFactory);
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
			}
		} finally {
			sessionFactory.close();
		}
	}

	private CmfObjectCounter<ExportResult> runExportImpl(final ExportState exportState,
		CmfObjectCounter<ExportResult> counter, final SessionFactory<S> sessionFactory,
		final SessionWrapper<S> baseSession, final ContextFactory<S, V, C, ?> contextFactory, final DF delegateFactory)
		throws ExportException, CmfStorageException {
		final Logger output = exportState.output;
		final CmfObjectStore<?, ?> objectStore = exportState.objectStore;
		final CmfContentStore<?, ?, ?> contentStore = exportState.streamStore;
		final CfgTools settings = exportState.cfg;
		final int threadCount;
		final int backlogSize;
		// Ensure nobody changes this under our feet
		synchronized (this) {
			threadCount = getThreadCount(settings);
			backlogSize = getBacklogSize(settings);
		}
		String msg = String.format("Will pull a maximum of %d items at a time, and process them using %d threads",
			backlogSize, threadCount);
		this.log.info(msg);
		if (output != null) {
			output.info(msg);
		}

		if (counter == null) {
			counter = new CmfObjectCounter<ExportResult>(ExportResult.class);
		}
		final ExportListenerDelegator listenerDelegator = new ExportListenerDelegator(counter);
		final Map<ExportTarget, ExportStatus> statusMap = new ConcurrentHashMap<ExportTarget, ExportStatus>();

		PooledWorkers<SessionWrapper<S>, ExportTarget> worker = new PooledWorkers<SessionWrapper<S>, ExportTarget>(
			backlogSize) {

			@Override
			protected SessionWrapper<S> prepare() throws Exception {
				final SessionWrapper<S> s;
				try {
					s = sessionFactory.acquireSession();
				} catch (Exception e) {
					this.log.error("Failed to obtain a worker session", e);
					return null;
				}
				this.log.info(String.format("Worker ready with session [%s]", s.getId()));
				return s;
			}

			@Override
			protected void process(SessionWrapper<S> session, ExportTarget next) throws Exception {
				final S s = session.getWrapped();

				CmfType nextType = next.getType();
				final String nextId = next.getId();
				final String nextKey = next.getSearchKey();

				if (this.log.isDebugEnabled()) {
					this.log.debug(String.format("Polled %s", next));
				}

				boolean tx = false;
				boolean ok = false;
				try {
					// Begin transaction
					tx = session.begin();
					final ExportDelegate<?, S, W, V, C, ?, ?> exportDelegate = delegateFactory.newExportDelegate(s,
						nextType, nextKey);
					if (exportDelegate == null) {
						// No object found with that ID...
						this.log.warn(String.format("No %s object found with searchKey[%s]",
							(nextType != null ? nextType.name() : "globally unique"), nextKey));
						return;
					}
					// This allows for object substitutions to take place
					next = exportDelegate.getExportTarget();
					nextType = next.getType();
					if (nextType == null) {
						this.log.error(String.format(
							"Failed to determine the object type for target with ID[%s] and searchKey[%s]", nextId,
							nextKey));
						return;
					}

					if (this.log.isDebugEnabled()) {
						this.log.debug(String.format("Exporting the %s object with ID[%s]", nextType, nextId));
					}

					// The type mapper parameter is null here because it's only useful
					// for imports
					final C ctx = contextFactory.newContext(nextId, nextType, s, output, objectStore, contentStore,
						null, 0);
					try {
						initContext(ctx);
						Result result = exportObject(exportState, null, next, exportDelegate, ctx, listenerDelegator,
							statusMap);
						if (result != null) {
							if (this.log.isDebugEnabled()) {
								this.log.debug(
									String.format("Exported %s [%s](%s) in position %d", result.marshaled.getType(),
										result.marshaled.getLabel(), result.marshaled.getId(), result.objectNumber));
							}
						}
						ok = true;
					} finally {
						ctx.close();
					}
					if (tx) {
						session.commit();
					}
				} catch (Throwable t) {
					this.log.error(String.format("Failed to export %s object with ID[%s]", nextType, nextId), t);
					if (tx && !ok) {
						session.rollback();
					}
				}
			}

			@Override
			protected void cleanup(SessionWrapper<S> session) {
				session.close();
			}
		};

		final CloseableIterator<ExportTarget> results;
		this.log.debug("Locating export results...");
		try {
			this.log.debug("Results will be cached");
			output.info("Caching export results...");
			results = cacheExportResults(baseSession.getWrapped(), settings, delegateFactory, objectStore, output);
		} catch (Exception e) {
			throw new ExportException(String.format("Failed to obtain the export results with settings: %s", settings),
				e);
		}

		try {
			// Fire off the workers
			worker.start(threadCount, new ExportTarget(), true);

			int c = 1;
			// 1: run the query for the given predicate
			listenerDelegator.exportStarted(exportState);
			// 2: iterate over the results, gathering up the object IDs
			try {
				this.log.debug("Processing the located results...");
				while ((results != null) && results.hasNext()) {
					final ExportTarget target = results.next();
					msg = String.format("Queueing item #%d: %s", c, target);
					this.log.info(msg);
					if (output != null) {
						output.info(msg);
					}
					try {
						worker.addWorkItem(target);
						msg = String.format("Queued item #%d: %s", c, target);
						this.log.info(msg);
						if (output != null) {
							output.info(msg);
						}
					} catch (InterruptedException e) {
						Thread.currentThread().interrupt();
						msg = String.format("Thread interrupted while queueing target #%d: %s", c, target);
						if (this.log.isDebugEnabled()) {
							this.log.warn(msg, e);
						} else {
							this.log.warn(msg);
						}
						break;
					} finally {
						c++;
					}
				}
				this.log.info(String.format("Submitted the entire export workload (%d objects)", c - 1));
				if (output != null) {
					output.info(String.format("Submitted the entire export workload (%d objects)", c - 1));
				}
			} finally {
				worker.waitForCompletion();
			}

			setExportProperties(objectStore);
			return listenerDelegator.getStoredObjectCounter();
		} finally {
			results.close();
			baseSession.close(false);

			Map<CmfType, Integer> summary = Collections.emptyMap();
			try {
				summary = objectStore.getStoredObjectTypes();
			} catch (CmfStorageException e) {
				this.log.warn("Exception caught attempting to get the work summary", e);
			}
			listenerDelegator.exportFinished(exportState, summary);
		}
	}

	protected void initContext(C ctx) {
	}

	protected void setExportProperties(CmfObjectStore<?, ?> store) {
	}

	private final CloseableIterator<ExportTarget> cacheExportResults(final S session, final CfgTools configuration,
		final DF factory, final CmfObjectStore<?, ?> store, final Logger output) throws Exception {

		final List<CmfObjectSpec> end = Collections.emptyList();
		final BlockingQueue<Collection<CmfObjectSpec>> queue = new LinkedBlockingQueue<Collection<CmfObjectSpec>>();
		ExecutorService executor = Executors.newSingleThreadExecutor();
		final Future<Long> cachingTask = executor.submit(new Callable<Long>() {
			@Override
			public Long call() throws Exception {
				long cached = 0;
				store.clearTargetCache();
				while (true) {
					Collection<CmfObjectSpec> c = queue.take();
					if (c.isEmpty()) {
						// We're done
						return cached;
					}

					try {
						output.info("Caching {} targets ({} total so far)", c.size(), cached);
						store.cacheTargets(c);
						cached += c.size();
					} finally {
						// Help the GC along...
						c.clear();
					}
				}
			}

		});
		// No more tasks needed here
		executor.shutdown();

		boolean ok = false;
		try {
			output.info("Retrieving the results");
			CloseableIterator<ExportTarget> it = findExportResults(session, configuration, factory);
			try {
				final int segmentSize = 1000;
				List<CmfObjectSpec> temp = new ArrayList<CmfObjectSpec>(segmentSize);
				// It's OK to ask if the thread is finished up front because the only way it could
				// happen is if it ran into an error, in which case we need to fail short.
				while (!cachingTask.isDone() && it.hasNext()) {
					if (temp.size() == segmentSize) {
						try {
							queue.put(temp);
						} finally {
							temp = new ArrayList<CmfObjectSpec>(segmentSize);
						}
					}
					temp.add(it.next().toObjectSpec());
				}
				queue.put(temp);
				queue.put(end);

				output.info("Cached a total of {} objects", cachingTask.get());
				ok = true;
			} finally {
				it.close();
			}
		} finally {
			// If the caching thread isn't finished, then...
			if (!ok && !cachingTask.isDone()) {
				queue.put(end);
				cachingTask.cancel(true);
				try {
					cachingTask.get();
				} catch (CancellationException e) {
					// Do nothing...we're OK with this
				} catch (ExecutionException e) {
					// Do nothing...we're OK with this
				} catch (InterruptedException e) {
					// Do nothing...we're OK with this
				}
			}
		}

		return new CloseableIterator<ExportTarget>() {

			private final CloseableIterator<CmfObjectSpec> it = store.getCachedTargets();

			@Override
			protected boolean checkNext() {
				return this.it.hasNext();
			}

			@Override
			protected ExportTarget getNext() throws Exception {
				return new ExportTarget(this.it.next());
			}

			@Override
			protected void doClose() {
				this.it.close();
			}
		};
	}

	protected abstract CloseableIterator<ExportTarget> findExportResults(S session, CfgTools configuration, DF factory)
		throws Exception;

	public static ExportEngine<?, ?, ?, ?, ?, ?> getExportEngine(String targetName) {
		return TransferEngine.getTransferEngine(ExportEngine.class, targetName);
	}

	@Override
	protected void getSupportedSettings(Collection<TransferEngineSetting> settings) {
		for (ExportSetting s : ExportSetting.values()) {
			settings.add(s);
		}
	}
}