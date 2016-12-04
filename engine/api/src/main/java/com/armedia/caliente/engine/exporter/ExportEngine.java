/**
 *
 */

package com.armedia.caliente.engine.exporter;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.commons.lang3.concurrent.ConcurrentInitializer;
import org.apache.commons.lang3.concurrent.ConcurrentUtils;
import org.slf4j.Logger;

import com.armedia.caliente.engine.CmfCrypt;
import com.armedia.caliente.engine.ContextFactory;
import com.armedia.caliente.engine.SessionFactory;
import com.armedia.caliente.engine.SessionWrapper;
import com.armedia.caliente.engine.TransferEngine;
import com.armedia.caliente.engine.TransferEngineSetting;
import com.armedia.caliente.engine.TransferSetting;
import com.armedia.caliente.store.CmfContentInfo;
import com.armedia.caliente.store.CmfContentStore;
import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.store.CmfObjectCounter;
import com.armedia.caliente.store.CmfObjectStore;
import com.armedia.caliente.store.CmfObjectStore.LockStatus;
import com.armedia.caliente.store.CmfObjectStore.StoreStatus;
import com.armedia.caliente.store.CmfStorageException;
import com.armedia.caliente.store.CmfType;
import com.armedia.commons.utilities.CfgTools;
import com.armedia.commons.utilities.CloseableIterator;
import com.armedia.commons.utilities.PooledWorkers;
import com.armedia.commons.utilities.Tools;

/**
 * @author diego
 *
 */
public abstract class ExportEngine<S, W extends SessionWrapper<S>, V, C extends ExportContext<S, V, CF>, CF extends ExportContextFactory<S, W, V, C, ?>, DF extends ExportDelegateFactory<S, W, V, C, ?>>
	extends TransferEngine<S, V, C, CF, DF, ExportEngineListener> {

	private class Result {
		private final Long objectNumber;
		private final CmfObject<V> object;
		private final ExportSkipReason skipReason;
		private final String extraInfo;

		public Result(Long objectNumber, CmfObject<V> marshaled) {
			this.objectNumber = objectNumber;
			this.object = marshaled;
			this.skipReason = null;
			this.extraInfo = null;
		}

		public Result(ExportSkipReason message) {
			this(message, null);
		}

		public Result(ExportSkipReason message, String extraInfo) {
			this.objectNumber = null;
			this.object = null;
			this.skipReason = message;
			this.extraInfo = extraInfo;
		}
	}

	private final Result unsupportedResult = new Result(ExportSkipReason.UNSUPPORTED);

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
		public void exportFinished(UUID jobId, Map<CmfType, Long> summary) {
			for (ExportEngineListener l : this.listeners) {
				try {
					l.exportFinished(jobId, summary);
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
		final ExportListenerDelegator listenerDelegator, final ConcurrentMap<ExportTarget, ExportOperation> statusMap)
		throws ExportException, CmfStorageException {
		if (!ctx.isSupported(target.getType())) { return this.unsupportedResult; }
		try {
			listenerDelegator.objectExportStarted(exportState.jobId, target.getType(), target.getId());
			final Result result = doExportObject(exportState, referrent, target, sourceObject, ctx, listenerDelegator,
				statusMap);
			if ((result.objectNumber != null) && (result.object != null)) {
				listenerDelegator.objectExportCompleted(exportState.jobId, result.object, result.objectNumber);
			} else {
				switch (result.skipReason) {
					case ALREADY_STORED:
						// The object is already stored, so it can be safely and quietly skipped
						// fall-through...
					case ALREADY_FAILED:
						// The object is already failed, so it can be safely and quietly skipped
						// fall-through...
					case ALREADY_LOCKED:
						// We don't report anything for these, since the object
						// is being stored by another thread...
						break;

					case SKIPPED: // fall-through
					case DEPENDENCY_FAILED: // fall-through
					case UNSUPPORTED:
						if (exportState.objectStore.markStoreStatus(target, StoreStatus.SKIPPED, result.extraInfo)) {
							listenerDelegator.objectSkipped(exportState.jobId, target.getType(), target.getId(),
								result.skipReason, result.extraInfo);
						}
						break;
				}
			}
			return result;
		} catch (Exception e) {
			try {
				listenerDelegator.objectExportFailed(exportState.jobId, target.getType(), target.getId(), e);
			} finally {
				exportState.objectStore.markStoreStatus(target, StoreStatus.FAILED, Tools.dumpStackTrace(e));
			}
			if (e instanceof ExportException) { throw ExportException.class.cast(e); }
			if (e instanceof CmfStorageException) { throw CmfStorageException.class.cast(e); }
			throw RuntimeException.class.cast(e);
		}
	}

	private Result doExportObject(ExportState exportState, final ExportTarget referrent, final ExportTarget target,
		final ExportDelegate<?, S, W, V, C, ?, ?> sourceObject, final C ctx,
		final ExportListenerDelegator listenerDelegator, final ConcurrentMap<ExportTarget, ExportOperation> statusMap)
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
		final ExportOperation thisStatus = ConcurrentUtils.createIfAbsentUnchecked(statusMap, target,
			new ConcurrentInitializer<ExportOperation>() {
				@Override
				public ExportOperation get() {
					return new ExportOperation(target);
				}
			});

		final LockStatus locked;
		try {
			locked = objectStore.lockForStorage(target, referrent);
			switch (locked) {
				case LOCK_ACQUIRED:
					// We got the lock, which means we create the locker object
					if (this.log.isTraceEnabled()) {
						this.log.trace(String.format("Locked %s for storage", label));
					}
					break;

				case LOCK_CONCURRENT:
					if (this.log.isTraceEnabled()) {
						this.log.trace(
							String.format("%s is already locked for storage by another thread, skipping it", label));
					}
					return new Result(ExportSkipReason.ALREADY_LOCKED);

				case ALREADY_FAILED:
					String msg = String.format("%s was already failed, skipping it", label);
					if (this.log.isTraceEnabled()) {
						this.log.trace(msg);
					}
					return new Result(ExportSkipReason.ALREADY_FAILED, msg);

				case ALREADY_STORED:
					if (this.log.isTraceEnabled()) {
						this.log.trace(String.format("%s is already locked for storage, skipping it", label));
					}
					return new Result(ExportSkipReason.ALREADY_STORED);
			}
		} catch (CmfStorageException e) {
			throw new ExportException(
				String.format("Exception caught attempting to lock a %s for storage [%s](%s)", type, label, id), e);
		}

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
			Collection<ExportTarget> waitTargets = new TreeSet<>();
			for (ExportDelegate<?, S, W, V, C, ?, ?> requirement : referenced) {
				if (requirement.getExportTarget().equals(target)) {
					continue;
				}
				final Result r;
				try {
					r = exportObject(exportState, target, requirement.getExportTarget(), requirement, ctx,
						listenerDelegator, statusMap);
				} catch (Exception e) {
					// This exception will already be logged...so we simply accept the failure and
					// report it upwards, without bubbling up the exception to be reported 1000
					// times
					return new Result(ExportSkipReason.DEPENDENCY_FAILED, String
						.format("A required object [%s] failed to serialize for %s", requirement.exportTarget, label));
				}

				// If there is no message, the result is success
				if (r.skipReason == null) {
					continue;
				}
				switch (r.skipReason) {
					case ALREADY_FAILED: // fall-through
					case DEPENDENCY_FAILED:
						// A dependency has failed, we fail immediately...
						return new Result(ExportSkipReason.DEPENDENCY_FAILED, String.format(
							"A required object [%s] failed to serialize for %s", requirement.exportTarget, label));

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
					ExportOperation status = statusMap.get(requirement);
					if (status == null) { throw new ExportException(
						String.format("No export status found for requirement [%s] of %s", requirement, label)); }
					try {
						ctx.printf("Waiting for [%s] from %s (#%d created by %s)", requirement, label,
							status.getObjectNumber(), status.getCreatorThread());
						long waitTime = status.waitUntilCompleted();
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
				throw new ExportException(String.format("Failed to run the post-requirements callback for %s", label),
					e);
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
					try {
						exportObject(exportState, target, antecedent.getExportTarget(), antecedent, ctx,
							listenerDelegator, statusMap);
					} catch (Exception e) {
						// This exception will already be logged...so we simply accept the failure
						// and report it upwards, without bubbling up the exception to be reported
						// 1000 times
						return new Result(ExportSkipReason.DEPENDENCY_FAILED, String.format(
							"An antecedent object [%s] failed to serialize for %s", antecedent.exportTarget, label));
					}
				}
				try {
					sourceObject.antecedentsExported(marshaled, ctx);
				} catch (Exception e) {
					throw new ExportException(
						String.format("Failed to run the post-antecedents callback for %s", label), e);
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
					try {
						exportObject(exportState, target, successor.getExportTarget(), successor, ctx,
							listenerDelegator, statusMap);
					} catch (Exception e) {
						// This exception will already be logged...so we simply accept the failure
						// and report it upwards, without bubbling up the exception to be reported
						// 1000 times
						return new Result(ExportSkipReason.DEPENDENCY_FAILED, String.format(
							"A successor object [%s] failed to serialize for %s", successor.exportTarget, label));
					}
				}

				try {
					sourceObject.successorsExported(marshaled, ctx);
				} catch (Exception e) {
					throw new ExportException(String.format("Failed to run the post-successors callback for %s", label),
						e);
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
			int dependentsExported = 0;
			for (ExportDelegate<?, S, W, V, C, ?, ?> dependent : referenced) {
				try {
					exportObject(exportState, target, dependent.getExportTarget(), dependent, ctx, listenerDelegator,
						statusMap);
				} catch (Exception e) {
					// Contrary to previous cases, this isn't a failure because this doesn't
					// stop the object from being properly represented...
					dependentsExported++;
				}
			}
			if (dependentsExported != referenced.size()) {
				this.log.warn("Failed to store all dependent objects for {} - only exported {} of {}", label,
					dependentsExported, referenced.size());
			}

			try {
				sourceObject.dependentsExported(marshaled, ctx);
			} catch (Exception e) {
				throw new ExportException(String.format("Failed to run the post-dependents callback for %s", label), e);
			}

			Result result = new Result(ret, marshaled);
			success = true;
			return result;
		} finally {
			thisStatus.setCompleted(success);
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
					contextFactory = newContextFactory(baseSession.getWrapped(), configuration, objectStore,
						contentStore, null, output);
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
		CmfObjectCounter<ExportResult> objectCounter, final SessionFactory<S> sessionFactory,
		final SessionWrapper<S> baseSession, final ContextFactory<S, V, C, ?> contextFactory, final DF delegateFactory)
		throws ExportException, CmfStorageException {
		final Logger output = exportState.output;
		final CmfObjectStore<?, ?> objectStore = exportState.objectStore;
		final CfgTools settings = exportState.cfg;
		final int threadCount;
		// Ensure nobody changes this under our feet
		synchronized (this) {
			threadCount = getThreadCount(settings);
		}
		String msg = String.format("Will export items using %d threads", threadCount);
		this.log.info(msg);
		if (output != null) {
			output.info(msg);
		}

		if (objectCounter == null) {
			objectCounter = new CmfObjectCounter<>(ExportResult.class);
		}
		final ExportListenerDelegator listenerDelegator = new ExportListenerDelegator(objectCounter);
		final ConcurrentMap<ExportTarget, ExportOperation> statusMap = new ConcurrentHashMap<>();

		final PooledWorkers<SessionWrapper<S>, ExportTarget> worker = new PooledWorkers<SessionWrapper<S>, ExportTarget>() {

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
					final C ctx = contextFactory.newContext(nextId, nextType, s, 0);
					try {
						initContext(ctx);
						Result result = exportObject(exportState, null, next, exportDelegate, ctx, listenerDelegator,
							statusMap);
						if (result != null) {
							if (this.log.isDebugEnabled()) {
								this.log
									.debug(String.format("Exported %s [%s](%s) in position %d", result.object.getType(),
										result.object.getLabel(), result.object.getId(), result.objectNumber));
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

		this.log.debug("Locating export results...");
		try {
			// Fire off the workers
			final ExportTarget terminator = new ExportTarget();
			worker.start(threadCount, terminator, true);
			try {
				output.info("Retrieving the results");
				final CloseableIterator<ExportTarget> it;
				try {
					it = findExportResults(baseSession.getWrapped(), settings, delegateFactory);
				} catch (Exception e) {
					throw new ExportException("Failed to retrieve the objects to export", e);
				}
				output.info("Results ready, queueing the workload...");
				final int reportCount = 1000;
				long targetCounter = 0;
				try {
					// It's OK to ask if the thread is finished up front because the
					// only way it could happen is if it ran into an error, in which
					// case we need to stop immediately since there's no point in continuing
					while (it.hasNext()) {
						final ExportTarget next = it.next();
						if (next == null) {
							// Just in case...
							break;
						}
						if ((++targetCounter % reportCount) == 0) {
							output.info("Retrieved {} object references for export", targetCounter);
						}
						try {
							worker.addWorkItem(next);
						} catch (InterruptedException e) {
							throw new ExportException(
								String.format("Interrupted while trying to queue up element [%s]", next), e);
						}
					}
					output.info("Retrieved a total of {} objects", targetCounter);
				} finally {
					it.close();
				}
			} finally {
				worker.waitForCompletion();
			}

			setExportProperties(objectStore);
			return listenerDelegator.getStoredObjectCounter();
		} finally {
			baseSession.close(false);

			Map<CmfType, Long> summary = Collections.emptyMap();
			try {
				summary = objectStore.getStoredObjectTypes();
			} catch (CmfStorageException e) {
				this.log.warn("Exception caught attempting to get the work summary", e);
			}
			listenerDelegator.exportFinished(exportState.jobId, summary);
		}
	}

	protected void initContext(C ctx) {
	}

	protected void setExportProperties(CmfObjectStore<?, ?> store) {
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