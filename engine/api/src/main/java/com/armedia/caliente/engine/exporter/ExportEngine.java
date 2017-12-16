/**
 *
 */

package com.armedia.caliente.engine.exporter;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.lang3.concurrent.ConcurrentInitializer;
import org.apache.commons.lang3.concurrent.ConcurrentUtils;
import org.slf4j.Logger;

import com.armedia.caliente.engine.SessionFactory;
import com.armedia.caliente.engine.SessionWrapper;
import com.armedia.caliente.engine.TransferContextFactory;
import com.armedia.caliente.engine.TransferEngine;
import com.armedia.caliente.engine.TransferEngineSetting;
import com.armedia.caliente.engine.TransferSetting;
import com.armedia.caliente.engine.WarningTracker;
import com.armedia.caliente.engine.dynamic.filter.ObjectFilter;
import com.armedia.caliente.engine.dynamic.filter.ObjectFilterException;
import com.armedia.caliente.engine.dynamic.transformer.Transformer;
import com.armedia.caliente.engine.dynamic.transformer.TransformerException;
import com.armedia.caliente.store.CmfContentInfo;
import com.armedia.caliente.store.CmfContentStore;
import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.store.CmfObjectCounter;
import com.armedia.caliente.store.CmfObjectRef;
import com.armedia.caliente.store.CmfObjectStore;
import com.armedia.caliente.store.CmfObjectStore.LockStatus;
import com.armedia.caliente.store.CmfObjectStore.StoreStatus;
import com.armedia.caliente.store.CmfStorageException;
import com.armedia.caliente.store.CmfType;
import com.armedia.caliente.store.CmfValue;
import com.armedia.caliente.tools.CmfCrypt;
import com.armedia.commons.utilities.CfgTools;
import com.armedia.commons.utilities.PooledWorkers;
import com.armedia.commons.utilities.Tools;

/**
 * @author diego
 *
 */
public abstract class ExportEngine<S, W extends SessionWrapper<S>, V, C extends ExportContext<S, V, CF>, CF extends ExportContextFactory<S, W, V, C, ?>, DF extends ExportDelegateFactory<S, W, V, C, ?>>
	extends TransferEngine<S, V, C, CF, DF, ExportEngineListener> {

	protected static interface TargetSubmitter {
		public void submit(ExportTarget target) throws ExportException;
	}

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
		public void objectExportStarted(UUID jobId, CmfObjectRef object, CmfObjectRef referrent) {
			for (ExportEngineListener l : this.listeners) {
				try {
					l.objectExportStarted(jobId, object, referrent);
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
		public void objectSkipped(UUID jobId, CmfObjectRef object, ExportSkipReason reason, String extraInfo) {
			getStoredObjectCounter().increment(object.getType(), ExportResult.SKIPPED);
			for (ExportEngineListener l : this.listeners) {
				try {
					l.objectSkipped(jobId, object, reason, extraInfo);
				} catch (Exception e) {
					if (this.log.isDebugEnabled()) {
						this.log.error("Exception caught during listener propagation", e);
					}
				}
			}
		}

		@Override
		public void objectExportFailed(UUID jobId, CmfObjectRef object, Throwable thrown) {
			getStoredObjectCounter().increment(object.getType(), ExportResult.FAILED);
			for (ExportEngineListener l : this.listeners) {
				try {
					l.objectExportFailed(jobId, object, thrown);
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

		@Override
		public void consistencyWarning(UUID jobId, CmfObjectRef object, String fmt, Object... args) {
			for (ExportEngineListener l : this.listeners) {
				try {
					l.consistencyWarning(jobId, object, fmt, args);
				} catch (Exception e) {
					if (this.log.isDebugEnabled()) {
						this.log.error("Exception caught during listener propagation", e);
					}
				}
			}
		}
	}

	protected ExportEngine(CmfCrypt crypto) {
		super(crypto, "export");
	}

	protected ExportEngine(CmfCrypt crypto, boolean supportsDuplicateNames) {
		super(crypto, "export", supportsDuplicateNames);
	}

	private Result exportObject(ExportState exportState, final Transformer transformer, final ObjectFilter filter,
		final ExportTarget referrent, final ExportTarget target, final ExportDelegate<?, S, W, V, C, ?, ?> sourceObject,
		final C ctx, final ExportListenerDelegator listenerDelegator,
		final ConcurrentMap<ExportTarget, ExportOperation> statusMap) throws ExportException, CmfStorageException {
		try {
			if (!ctx.isSupported(target.getType())) { return this.unsupportedResult; }

			final CmfType type = target.getType();
			final String id = target.getId();
			final String objectLabel = sourceObject.getLabel();
			final String logLabel = String.format("%s [%s](%s)", type, objectLabel, id);
			final LockStatus locked;
			try {
				locked = exportState.objectStore.lockForStorage(target, referrent,
					Tools.coalesce(sourceObject.getHistoryId(), sourceObject.getObjectId()), ctx.getId());
				switch (locked) {
					case LOCK_ACQUIRED:
						// We got the lock, which means we create the locker object
						this.log.trace("Locked {} for storage", logLabel);
						break;

					case LOCK_CONCURRENT:
						this.log.trace("{} is already locked for storage by another thread", logLabel);
						return new Result(ExportSkipReason.ALREADY_LOCKED);

					case ALREADY_FAILED:
						String msg = String.format("%s was already failed, skipping it", logLabel);
						this.log.trace(msg);
						return new Result(ExportSkipReason.ALREADY_FAILED, msg);

					case ALREADY_STORED:
						this.log.trace("{} is already stored", logLabel);
						return new Result(ExportSkipReason.ALREADY_STORED);
				}
			} catch (CmfStorageException e) {
				throw new ExportException(
					String.format("Exception caught attempting to lock a %s for storage", logLabel), e);
			}

			listenerDelegator.objectExportStarted(exportState.jobId, target, referrent);

			final Result result = doExportObject(exportState, transformer, filter, referrent, target, sourceObject, ctx,
				listenerDelegator, statusMap);
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
							listenerDelegator.objectSkipped(exportState.jobId, target, result.skipReason,
								result.extraInfo);
						}
						break;
				}
			}
			return result;
		} catch (Exception e) {
			try {
				listenerDelegator.objectExportFailed(exportState.jobId, target, e);
			} finally {
				exportState.objectStore.markStoreStatus(target, StoreStatus.FAILED, Tools.dumpStackTrace(e));
			}
			if (e instanceof ExportException) { throw ExportException.class.cast(e); }
			if (e instanceof CmfStorageException) { throw CmfStorageException.class.cast(e); }
			throw RuntimeException.class.cast(e);
		}
	}

	private Result doExportObject(ExportState exportState, final Transformer transformer, final ObjectFilter filter,
		final ExportTarget referrent, final ExportTarget target, final ExportDelegate<?, S, W, V, C, ?, ?> sourceObject,
		final C ctx, final ExportListenerDelegator listenerDelegator,
		final ConcurrentMap<ExportTarget, ExportOperation> statusMap) throws ExportException, CmfStorageException {
		if (target == null) { throw new IllegalArgumentException("Must provide the original export target"); }
		if (sourceObject == null) { throw new IllegalArgumentException("Must provide the original object to export"); }
		if (ctx == null) { throw new IllegalArgumentException("Must provide a context to operate in"); }

		final CmfType type = target.getType();
		final String id = target.getId();
		final String objectLabel = sourceObject.getLabel();
		final String logLabel = String.format("%s [%s](%s)", type, objectLabel, id);

		if (this.log.isTraceEnabled()) {
			this.log.trace(String.format("Attempting export of %s", logLabel));
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

		boolean success = false;
		if (referrent != null) {
			ctx.pushReferrent(referrent);
		}
		try {
			if (this.log.isDebugEnabled()) {
				if (referrent != null) {
					this.log.debug(String.format("Exporting %s (referenced by %s)", logLabel, referrent));
				} else {
					this.log.debug(String.format("Exporting %s (from the main search)", logLabel));
				}
			}

			final CmfObject<V> marshaled = sourceObject.marshal(ctx, referrent);
			if (marshaled == null) { return new Result(ExportSkipReason.SKIPPED); }
			// For now, only filter "leaf" objects (i.e. with no referrent, which means they were
			// explicitly requested). In the (near) future, we'll add an option to allow filtering
			// of any object in the graph, including dependencies, such that the filtering of a
			// dependency causes the export "failure" of its dependent object tree.
			if ((filter != null) && (referrent == null)) {
				try {
					if (!filter.acceptRaw(marshaled, objectStore.getAttributeMapper())) { return new Result(
						ExportSkipReason.SKIPPED, "Excluded by filtering logic"); }
				} catch (ObjectFilterException e) {
					throw new ExportException(String.format("Filtering logic exception while processing %s", logLabel),
						e);
				}
			}

			Collection<? extends ExportDelegate<?, S, W, V, C, ?, ?>> referenced;
			try {
				referenced = sourceObject.identifyRequirements(marshaled, ctx);
				if (referenced == null) {
					referenced = Collections.emptyList();
				}
			} catch (Exception e) {
				throw new ExportException(String.format("Failed to identify the requirements for %s", logLabel), e);
			}

			if (this.log.isDebugEnabled()) {
				this.log
					.debug(String.format("%s requires %d objects for successful storage", logLabel, referenced.size()));
			}
			// We use a TreeSet to ensure that all our targets are always waited upon in the same
			// order, to avoid deadlocks.
			Collection<ExportTarget> waitTargets = new TreeSet<>();
			Set<CmfObjectRef> requirements = new LinkedHashSet<>();
			for (ExportDelegate<?, S, W, V, C, ?, ?> requirement : referenced) {
				if (requirement.getExportTarget().equals(target)) {
					// Loop - avoid it!
					continue;
				}
				if (!requirements.add(requirement.getExportTarget())) {
					// Duplicate requirement - don't export again
					continue;
				}
				final Result r;
				try {
					r = exportObject(exportState, transformer, filter, target, requirement.getExportTarget(),
						requirement, ctx, listenerDelegator, statusMap);
				} catch (Exception e) {
					// This exception will already be logged...so we simply accept the failure and
					// report it upwards, without bubbling up the exception to be reported 1000
					// times
					return new Result(ExportSkipReason.DEPENDENCY_FAILED, String.format(
						"A required object [%s] failed to serialize for %s", requirement.exportTarget, logLabel));
				}

				if (r.skipReason != ExportSkipReason.UNSUPPORTED) {
					exportState.objectStore.addRequirement(marshaled, requirement.getExportTarget());
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
							"A required object [%s] failed to serialize for %s", requirement.exportTarget, logLabel));

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
						String.format("No export status found for requirement [%s] of %s", requirement, logLabel)); }
					try {
						ctx.printf("Waiting for [%s] from %s (#%d created by %s)", requirement, logLabel,
							status.getObjectNumber(), status.getCreatorThread());
						long waitTime = status.waitUntilCompleted();
						ctx.printf("Waiting for [%s] from %s for %d ms", requirement, logLabel, waitTime);
					} catch (InterruptedException e) {
						Thread.interrupted();
						throw new ExportException(String.format(
							"Thread interrupted waiting on the export of [%s] by %s", requirement, logLabel), e);
					}
					if (!status.isSuccessful()) { return new Result(ExportSkipReason.DEPENDENCY_FAILED,
						String.format("A required object [%s] failed to serialize for %s", requirement, logLabel)); }
				}
			} finally {
				thisStatus.endWait();
			}

			try {
				sourceObject.requirementsExported(marshaled, ctx);
			} catch (Exception e) {
				throw new ExportException(
					String.format("Failed to run the post-requirements callback for %s", logLabel), e);
			}

			final boolean latestOnly = ctx.getSettings().getBoolean(TransferSetting.LATEST_ONLY);
			if (!latestOnly) {
				try {
					referenced = sourceObject.identifyAntecedents(marshaled, ctx);
					if (referenced == null) {
						referenced = Collections.emptyList();
					}
				} catch (Exception e) {
					throw new ExportException(
						String.format("Failed to identify the antecedent versions for %s", logLabel), e);
				}

				if (this.log.isDebugEnabled()) {
					this.log.debug(String.format("%s requires %d antecedent versions for successful storage", logLabel,
						referenced.size()));
				}
				CmfObjectRef prev = null;
				for (ExportDelegate<?, S, W, V, C, ?, ?> antecedent : referenced) {
					try {
						exportObject(exportState, transformer, filter, target, antecedent.getExportTarget(), antecedent,
							ctx, listenerDelegator, statusMap);
					} catch (Exception e) {
						// This exception will already be logged...so we simply accept the failure
						// and report it upwards, without bubbling up the exception to be reported
						// 1000 times
						return new Result(ExportSkipReason.DEPENDENCY_FAILED, String.format(
							"An antecedent object [%s] failed to serialize for %s", antecedent.exportTarget, logLabel));
					}
					if (prev != null) {
						exportState.objectStore.addRequirement(antecedent.getExportTarget(), prev);
					}
					prev = antecedent.getExportTarget();
				}

				if (prev != null) {
					exportState.objectStore.addRequirement(marshaled, prev);
				}

				try {
					sourceObject.antecedentsExported(marshaled, ctx);
				} catch (Exception e) {
					throw new ExportException(
						String.format("Failed to run the post-antecedents callback for %s", logLabel), e);
				}
			}

			// Are there any last-minute properties/attributes to calculate prior to
			// storing the object for posterity?
			try {
				sourceObject.prepareForStorage(ctx, marshaled);
			} catch (Exception e) {
				throw new ExportException(String.format("Failed to prepare the object for storage for %s", logLabel),
					e);
			}

			CmfObject<CmfValue> encoded = getTranslator().encodeObject(marshaled);
			if (transformer != null) {
				try {
					encoded = transformer.transform(objectStore.getAttributeMapper(), encoded);
				} catch (TransformerException e) {
					throw new ExportException(String.format("Transformation failed for %s", marshaled.getDescription()),
						e);
				}
			}

			final Long ret = objectStore.storeObject(encoded);
			marshaled.copyNumber(encoded); // PATCH: make sure the object number is always copied

			if (ret == null) {
				// Should be impossible, but still guard against it
				if (this.log.isTraceEnabled()) {
					this.log.trace(String.format("%s was stored by another thread", logLabel));
				}
				return new Result(ExportSkipReason.ALREADY_STORED);
			}

			if (this.log.isDebugEnabled()) {
				this.log.debug(String.format("Executing supplemental storage for %s", logLabel));
			}
			try {
				final boolean includeRenditions = !ctx.getSettings().getBoolean(TransferSetting.NO_RENDITIONS);
				List<CmfContentInfo> cmfContentInfo = sourceObject.storeContent(ctx, getTranslator(), marshaled,
					referrent, streamStore, includeRenditions);
				if ((cmfContentInfo != null) && !cmfContentInfo.isEmpty()) {
					objectStore.setContentInfo(marshaled, cmfContentInfo);
				}
			} catch (Exception e) {
				throw new ExportException(String.format("Failed to execute the content storage for %s", logLabel), e);
			}

			if (this.log.isDebugEnabled()) {
				this.log.debug(String.format("Successfully stored %s as object # %d", logLabel, ret));
			}

			if (!latestOnly) {
				try {
					referenced = sourceObject.identifySuccessors(marshaled, ctx);
					if (referenced == null) {
						referenced = Collections.emptyList();
					}
				} catch (Exception e) {
					throw new ExportException(
						String.format("Failed to identify the successor versions for %s", logLabel), e);
				}

				if (this.log.isDebugEnabled()) {
					this.log.debug(String.format("%s is succeeded by %d additional versions for successful storage",
						logLabel, referenced.size()));
				}
				CmfObjectRef prev = marshaled;
				for (ExportDelegate<?, S, W, V, C, ?, ?> successor : referenced) {
					try {
						exportObject(exportState, transformer, filter, target, successor.getExportTarget(), successor,
							ctx, listenerDelegator, statusMap);
					} catch (Exception e) {
						// This exception will already be logged...so we simply accept the failure
						// and report it upwards, without bubbling up the exception to be reported
						// 1000 times
						return new Result(ExportSkipReason.DEPENDENCY_FAILED, String.format(
							"A successor object [%s] failed to serialize for %s", successor.exportTarget, logLabel));
					}
					exportState.objectStore.addRequirement(successor.getExportTarget(), prev);
					prev = successor.getExportTarget();
				}

				try {
					sourceObject.successorsExported(marshaled, ctx);
				} catch (Exception e) {
					throw new ExportException(
						String.format("Failed to run the post-successors callback for %s", logLabel), e);
				}
			}

			try {
				referenced = sourceObject.identifyDependents(marshaled, ctx);
				if (referenced == null) {
					referenced = Collections.emptyList();
				}
			} catch (Exception e) {
				throw new ExportException(String.format("Failed to identify the dependents for %s", logLabel), e);
			}

			if (this.log.isDebugEnabled()) {
				this.log.debug(String.format("%s has %d dependent objects to store", logLabel, referenced.size()));
			}
			int dependentsExported = 0;
			for (ExportDelegate<?, S, W, V, C, ?, ?> dependent : referenced) {
				try {
					exportObject(exportState, transformer, filter, target, dependent.getExportTarget(), dependent, ctx,
						listenerDelegator, statusMap);
				} catch (Exception e) {
					// Contrary to previous cases, this isn't a failure because this doesn't
					// stop the object from being properly represented...
					dependentsExported++;
				}
			}
			if (dependentsExported != referenced.size()) {
				this.log.warn("Failed to store all dependent objects for {} - only exported {} of {}", logLabel,
					dependentsExported, referenced.size());
			}

			try {
				sourceObject.dependentsExported(marshaled, ctx);
			} catch (Exception e) {
				throw new ExportException(String.format("Failed to run the post-dependents callback for %s", logLabel),
					e);
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

	public final CmfObjectCounter<ExportResult> runExport(final Logger output, final WarningTracker warningTracker,
		final CmfObjectStore<?, ?> objectStore, final CmfContentStore<?, ?, ?> contentStore, Map<String, ?> settings)
		throws ExportException, CmfStorageException {
		return runExport(output, warningTracker, objectStore, contentStore, settings, null);
	}

	public final CmfObjectCounter<ExportResult> runExport(final Logger output, final WarningTracker warningTracker,
		final CmfObjectStore<?, ?> objectStore, final CmfContentStore<?, ?, ?> contentStore, Map<String, ?> settings,
		CmfObjectCounter<ExportResult> counter) throws ExportException, CmfStorageException {
		// We get this at the very top because if this fails, there's no point in continuing.

		final CfgTools configuration = new CfgTools(settings);
		objectStore.clearAttributeMappings();
		loadPrincipalMappings(objectStore.getAttributeMapper(), configuration);
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

			TransferContextFactory<S, V, C, ?> contextFactory = null;
			DF delegateFactory = null;
			Transformer transformer = null;
			ObjectFilter filter = null;
			try {

				validateEngine(baseSession.getWrapped());

				try {
					transformer = getTransformer(configuration);
				} catch (Exception e) {
					throw new ExportException("Failed to initialize the configured object transformations", e);
				}

				try {
					filter = getFilter(configuration);
				} catch (Exception e) {
					throw new ExportException("Failed to initialize the configured object filters", e);
				}

				try {
					contextFactory = newContextFactory(baseSession.getWrapped(), configuration, objectStore,
						contentStore, null, output, warningTracker);
				} catch (Exception e) {
					throw new ExportException("Failed to configure the context factory to carry out the export", e);
				}

				try {
					delegateFactory = newDelegateFactory(baseSession.getWrapped(), configuration);
				} catch (Exception e) {
					throw new ExportException("Failed to configure the delegate factory to carry out the export", e);
				}

				return runExportImpl(exportState, counter, sessionFactory, baseSession, contextFactory, delegateFactory,
					transformer, filter);
			} finally {
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
				if (baseSession != null) {
					baseSession.close();
				}
			}
		} finally {
			sessionFactory.close();
		}
	}

	private CmfObjectCounter<ExportResult> runExportImpl(final ExportState exportState,
		CmfObjectCounter<ExportResult> objectCounter, final SessionFactory<S> sessionFactory,
		final SessionWrapper<S> baseSession, final TransferContextFactory<S, V, C, ?> contextFactory,
		final DF delegateFactory, final Transformer transformer, final ObjectFilter filter)
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
			protected void process(SessionWrapper<S> session, ExportTarget target) throws Exception {
				final S s = session.getWrapped();

				CmfType nextType = target.getType();
				final String nextId = target.getId();
				final String nextKey = target.getSearchKey();

				if (this.log.isDebugEnabled()) {
					this.log.debug(String.format("Polled %s", target));
				}

				final boolean tx = session.begin();
				boolean ok = false;
				try {
					// Begin transaction

					final ExportDelegate<?, S, W, V, C, ?, ?> exportDelegate = delegateFactory.newExportDelegate(s,
						nextType, nextKey);
					if (exportDelegate == null) {
						// No object found with that ID...
						this.log.warn(String.format("No %s object found with searchKey[%s]",
							(nextType != null ? nextType.name() : "globally unique"), nextKey));
						return;
					}
					// This allows for object substitutions to take place
					target = exportDelegate.getExportTarget();
					nextType = target.getType();
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
						Result result = null;
						try {
							result = exportObject(exportState, transformer, filter, null, target, exportDelegate, ctx,
								listenerDelegator, statusMap);
						} catch (Exception e) {
							// Any and all Exceptions have already been processed in exportObject,
							// so we safely absorb them here. We leave all other Throwables intact
							// so they can be caught in the worker's handler
							result = null;
						}
						if (result != null) {
							if (this.log.isDebugEnabled()) {
								if (result.skipReason != null) {
									this.log.debug(String.format("Skipped %s [%s](%s) : %s", target.getType(),
										target.getSearchKey(), target.getId(), result.skipReason));
								} else {
									this.log.debug(String.format("Exported %s in position %d",
										result.object.getDescription(), result.objectNumber));
								}
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
					// Don't let these failures go unnoticed
					try {
						try {
							listenerDelegator.objectExportFailed(exportState.jobId, target, t);
						} finally {
							exportState.objectStore.markStoreStatus(target, StoreStatus.FAILED,
								Tools.dumpStackTrace(t));
						}
					} finally {
						if (tx && !ok) {
							session.rollback();
						}
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
			listenerDelegator.exportStarted(exportState);
			worker.start(threadCount, "Exporter", true);
			try {
				output.info("Retrieving the results");
				final int reportCount = 1000;
				final AtomicLong targetCounter = new AtomicLong(0);
				try {
					findExportResults(baseSession.getWrapped(), settings, delegateFactory, new TargetSubmitter() {
						@Override
						public void submit(ExportTarget target) throws ExportException {
							if (target == null) { return; }
							if (target.isNull()) {
								ExportEngine.this.log.warn("Skipping a null target: {}", target);
								return;
							}
							if ((targetCounter.incrementAndGet() % reportCount) == 0) {
								output.info("Retrieved {} object references for export", targetCounter.get());
							}
							try {
								worker.addWorkItem(target);
							} catch (InterruptedException e) {
								throw new ExportException(
									String.format("Interrupted while trying to queue up %s", target), e);
							}
						}
					});
				} catch (Exception e) {
					throw new ExportException("Failed to retrieve the objects to export", e);
				} finally {
					output.info("Retrieved a total of {} objects", targetCounter.get());
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

	protected void validateEngine(S session) throws ExportException {
		// By default, do nothing...
	}

	protected abstract void findExportResults(S session, CfgTools configuration, DF factory, TargetSubmitter handler)
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