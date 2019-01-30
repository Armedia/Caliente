package com.armedia.caliente.engine.exporter;

import java.io.File;
import java.lang.reflect.InvocationHandler;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.concurrent.ConcurrentInitializer;
import org.apache.commons.lang3.concurrent.ConcurrentUtils;
import org.slf4j.Logger;

import com.armedia.caliente.engine.SessionFactory;
import com.armedia.caliente.engine.SessionFactoryException;
import com.armedia.caliente.engine.SessionWrapper;
import com.armedia.caliente.engine.TransferContextFactory;
import com.armedia.caliente.engine.TransferEngine;
import com.armedia.caliente.engine.TransferEngineSetting;
import com.armedia.caliente.engine.TransferException;
import com.armedia.caliente.engine.TransferSetting;
import com.armedia.caliente.engine.WarningTracker;
import com.armedia.caliente.engine.dynamic.filter.ObjectFilter;
import com.armedia.caliente.engine.dynamic.filter.ObjectFilterException;
import com.armedia.caliente.engine.dynamic.transformer.Transformer;
import com.armedia.caliente.engine.dynamic.transformer.TransformerException;
import com.armedia.caliente.store.CmfContentStore;
import com.armedia.caliente.store.CmfContentStream;
import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.store.CmfObjectCounter;
import com.armedia.caliente.store.CmfObjectRef;
import com.armedia.caliente.store.CmfObjectStore;
import com.armedia.caliente.store.CmfObjectStore.LockStatus;
import com.armedia.caliente.store.CmfObjectStore.StoreStatus;
import com.armedia.caliente.store.CmfStorageException;
import com.armedia.caliente.store.CmfValue;
import com.armedia.commons.utilities.CfgTools;
import com.armedia.commons.utilities.PooledWorkers;
import com.armedia.commons.utilities.PooledWorkersLogic;
import com.armedia.commons.utilities.Tools;
import com.armedia.commons.utilities.line.LineScanner;

public abstract class ExportEngine<//
	SESSION, //
	SESSION_WRAPPER extends SessionWrapper<SESSION>, //
	VALUE, //
	CONTEXT extends ExportContext<SESSION, VALUE, CONTEXT_FACTORY>, //
	CONTEXT_FACTORY extends ExportContextFactory<SESSION, SESSION_WRAPPER, VALUE, CONTEXT, ?>, //
	DELEGATE_FACTORY extends ExportDelegateFactory<SESSION, SESSION_WRAPPER, VALUE, CONTEXT, ?>, //
	ENGINE_FACTORY extends ExportEngineFactory<SESSION, VALUE, CONTEXT, CONTEXT_FACTORY, DELEGATE_FACTORY, ?> //
> extends
	TransferEngine<ExportEngineListener, ExportResult, ExportException, SESSION, VALUE, CONTEXT, CONTEXT_FACTORY, DELEGATE_FACTORY, ENGINE_FACTORY> {

	protected static enum SearchType {
		//
		PATH, //
		KEY, //
		QUERY, //
		//
		;
	}

	private class Result {
		private final Long objectNumber;
		private final CmfObject<VALUE> object;
		private final ExportSkipReason skipReason;
		private final String extraInfo;

		public Result(Long objectNumber, CmfObject<VALUE> marshaled) {
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

	private final Map<LockStatus, Result> staticLockResults;
	private final boolean supportsMultipleSources;
	private final Set<SearchType> supportedSearches;
	private final Result unsupportedResult = new Result(ExportSkipReason.UNSUPPORTED);

	private class ExportListenerPropagator extends ListenerPropagator<ExportResult> implements InvocationHandler {

		private ExportListenerPropagator(CmfObjectCounter<ExportResult> counter) {
			super(counter, getListeners(), ExportEngineListener.class);
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
				case "exportStarted":
					getStoredObjectCounter().reset();
					break;
				case "objectExportCompleted":
					getStoredObjectCounter().increment(object.getType(), ExportResult.EXPORTED);
					break;
				case "objectSkipped":
					getStoredObjectCounter().increment(object.getType(), ExportResult.SKIPPED);
					break;
				case "objectExportFailed":
					getStoredObjectCounter().increment(object.getType(), ExportResult.FAILED);
					break;
			}
		}
	}

	protected ExportEngine(ENGINE_FACTORY factory, Logger output, WarningTracker warningTracker, File baseData,
		CmfObjectStore<?, ?> objectStore, CmfContentStore<?, ?, ?> contentStore, CfgTools settings,
		boolean supportsMultipleSources, SearchType... searchTypes) {
		super(factory, ExportResult.class, output, warningTracker, baseData, objectStore, contentStore, settings,
			"export");
		Map<LockStatus, Result> staticLockResults = new EnumMap<>(LockStatus.class);
		for (ExportSkipReason reason : ExportSkipReason.values()) {
			if (reason.lockStatus != null) {
				staticLockResults.put(reason.lockStatus, new Result(reason));
			}
		}
		this.staticLockResults = Tools.freezeMap(staticLockResults);
		Set<SearchType> specSet = EnumSet.noneOf(SearchType.class);
		for (SearchType type : searchTypes) {
			if (type != null) {
				specSet.add(type);
			}
		}

		if (specSet.isEmpty()) {
			specSet = EnumSet.allOf(SearchType.class);
		}

		this.supportedSearches = Tools.freezeSet(specSet);
		this.supportsMultipleSources = supportsMultipleSources;
	}

	private ExportOperation getOrCreateExportOperation(ExportTarget target,
		final ConcurrentMap<ExportTarget, ExportOperation> statusMap) {
		return ConcurrentUtils.createIfAbsentUnchecked(statusMap, target, new ConcurrentInitializer<ExportOperation>() {
			@Override
			public ExportOperation get() {
				return new ExportOperation(target);
			}
		});
	}

	private Result exportObject(ExportState exportState, final Transformer transformer, final ObjectFilter filter,
		final ExportTarget referrent, final ExportTarget target,
		final ExportDelegate<?, SESSION, SESSION_WRAPPER, VALUE, CONTEXT, ?, ?> sourceObject, final CONTEXT ctx,
		final ExportListener listener, final ConcurrentMap<ExportTarget, ExportOperation> statusMap)
		throws ExportException, CmfStorageException {
		try {
			if (!ctx.isSupported(target.getType())) { return this.unsupportedResult; }

			final CmfObject.Archetype type = target.getType();
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

					case ALREADY_LOCKED:
						// If the object is already locked, then we HAVE to have this created, so we
						// do this early on.
						getOrCreateExportOperation(target, statusMap);
						// fall-through
					case ALREADY_FAILED:
					case ALREADY_STORED:
						this.log.trace("{} result = {}", logLabel, locked);
						return this.staticLockResults.get(locked);
				}
			} catch (CmfStorageException e) {
				throw new ExportException(
					String.format("Exception caught attempting to lock a %s for storage", logLabel), e);
			}

			listener.objectExportStarted(exportState.jobId, target, referrent);

			final Result result = doExportObject(exportState, transformer, filter, referrent, target, sourceObject, ctx,
				listener, statusMap);
			if ((result.objectNumber != null) && (result.object != null)) {
				listener.objectExportCompleted(exportState.jobId, result.object, result.objectNumber);
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
							listener.objectSkipped(exportState.jobId, target, result.skipReason, result.extraInfo);
						}
						break;
				}
			}
			return result;
		} catch (Exception e) {
			try {
				listener.objectExportFailed(exportState.jobId, target, e);
			} finally {
				exportState.objectStore.markStoreStatus(target, StoreStatus.FAILED, Tools.dumpStackTrace(e));
			}
			if (e instanceof ExportException) { throw ExportException.class.cast(e); }
			if (e instanceof CmfStorageException) { throw CmfStorageException.class.cast(e); }
			throw RuntimeException.class.cast(e);
		}
	}

	private Result doExportObject(ExportState exportState, final Transformer transformer, final ObjectFilter filter,
		final ExportTarget referrent, final ExportTarget target,
		final ExportDelegate<?, SESSION, SESSION_WRAPPER, VALUE, CONTEXT, ?, ?> sourceObject, final CONTEXT ctx,
		final ExportListener listener, final ConcurrentMap<ExportTarget, ExportOperation> statusMap)
		throws ExportException, CmfStorageException {
		if (target == null) { throw new IllegalArgumentException("Must provide the original export target"); }
		if (sourceObject == null) { throw new IllegalArgumentException("Must provide the original object to export"); }
		if (ctx == null) { throw new IllegalArgumentException("Must provide a context to operate in"); }

		final CmfObject.Archetype type = target.getType();
		final String id = target.getId();
		final String objectLabel = sourceObject.getLabel();
		final String logLabel = String.format("%s [%s](%s)", type, objectLabel, id);

		if (this.log.isTraceEnabled()) {
			this.log.trace(String.format(
				"Attemp	public ExportEngineWorker() {\n" + "		super(ExportResult.class);\n"
					+ "		// TODO Auto-generated constructor stub\n" + "	}\n" + "\n" + "ting export of %s",
				logLabel));
		}

		final CmfObjectStore<?, ?> objectStore = exportState.objectStore;
		final CmfContentStore<?, ?, ?> streamStore = exportState.streamStore;

		// To make sure other threads don't work on this same object
		final ExportOperation thisStatus = getOrCreateExportOperation(target, statusMap);

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

			final CmfObject<VALUE> marshaled = sourceObject.marshal(ctx, referrent);
			if (marshaled == null) { return new Result(ExportSkipReason.SKIPPED); }
			// For now, only filter "leaf" objects (i.e. with no referrent, which means they were
			// explicitly requested). In the (near) future, we'll add an option to allow filtering
			// of any object in the graph, including dependencies, such that the filtering of a
			// dependency causes the export "failure" of its dependent object tree.
			if ((filter != null) && (referrent == null)) {
				try {
					if (!filter.acceptRaw(marshaled, objectStore.getValueMapper())) {
						return new Result(ExportSkipReason.SKIPPED, "Excluded by filtering logic");
					}
				} catch (ObjectFilterException e) {
					throw new ExportException(String.format("Filtering logic exception while processing %s", logLabel),
						e);
				}
			}

			Collection<? extends ExportDelegate<?, SESSION, SESSION_WRAPPER, VALUE, CONTEXT, ?, ?>> referenced;
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
			for (ExportDelegate<?, SESSION, SESSION_WRAPPER, VALUE, CONTEXT, ?, ?> requirement : referenced) {
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
						requirement, ctx, listener, statusMap);
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
					if (status == null) {
						throw new ExportException(
							String.format("No export status found for requirement [%s] of %s", requirement, logLabel));
					}
					try {
						ctx.printf("Waiting for [%s] from %s (#%d created by %s)", requirement, logLabel,
							status.getObjectNumber(), status.getCreatorThread().getName());
						long waitTime = status.waitUntilCompleted();
						ctx.printf("Waiting for [%s] from %s for %d ms", requirement, logLabel, waitTime);
					} catch (InterruptedException e) {
						Thread.interrupted();
						throw new ExportException(String.format(
							"Thread interrupted waiting on the export of [%s] by %s", requirement, logLabel), e);
					}
					if (!status.isSuccessful()) {
						return new Result(ExportSkipReason.DEPENDENCY_FAILED,
							String.format("A required object [%s] failed to serialize for %s", requirement, logLabel));
					}
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
				for (ExportDelegate<?, SESSION, SESSION_WRAPPER, VALUE, CONTEXT, ?, ?> antecedent : referenced) {
					try {
						exportObject(exportState, transformer, filter, target, antecedent.getExportTarget(), antecedent,
							ctx, listener, statusMap);
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
					encoded = transformer.transform(objectStore.getValueMapper(),
						getTranslator().getAttributeNameMapper(), null, encoded);
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
				List<CmfContentStream> contentStreams = sourceObject.storeContent(ctx, getTranslator(), marshaled,
					referrent, streamStore, includeRenditions);
				if ((contentStreams != null) && !contentStreams.isEmpty()) {
					objectStore.setContentStreams(marshaled, contentStreams);
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
				for (ExportDelegate<?, SESSION, SESSION_WRAPPER, VALUE, CONTEXT, ?, ?> successor : referenced) {
					try {
						exportObject(exportState, transformer, filter, target, successor.getExportTarget(), successor,
							ctx, listener, statusMap);
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
			for (ExportDelegate<?, SESSION, SESSION_WRAPPER, VALUE, CONTEXT, ?, ?> dependent : referenced) {
				try {
					exportObject(exportState, transformer, filter, target, dependent.getExportTarget(), dependent, ctx,
						listener, statusMap);
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

	private Stream<ExportTarget> getExportTargets(SESSION session, String source, DELEGATE_FACTORY delegateFactory)
		throws Exception {
		Stream<ExportTarget> ret = null;
		if (source.startsWith("%")) {
			if (!this.supportedSearches.contains(SearchType.KEY)) {
				throw new ExportException(
					String.format("This engine doesn't support searches by key - found [%s] as the source", source));
			}

			// SearchKey!
			final String searchKey = StringUtils.strip(source.substring(1));
			if (StringUtils.isEmpty(searchKey)) {
				throw new ExportException(
					String.format("Invalid search key [%s] - no object can be found with an empty key"));
			}
			ret = findExportTargetsBySearchKey(session, this.settings, delegateFactory, searchKey);
		} else //
		if (source.startsWith("/")) {
			if (!this.supportedSearches.contains(SearchType.PATH)) {
				throw new ExportException(
					String.format("This engine doesn't support searches by path - found [%s] as the source", source));
			}
			// CMS Path!
			ret = findExportTargetsByPath(session, this.settings, delegateFactory, source);
		} else {
			if (!this.supportedSearches.contains(SearchType.QUERY)) {
				throw new ExportException(
					String.format("This engine doesn't support searches by query - found [%s] as the source", source));
			}
			// Query string!
			ret = findExportTargetsByQuery(session, this.settings, delegateFactory, source);
		}
		if (ret != null) {
			if (ret.isParallel()) {
				// Switch to sequential mode - we're doing our own parallelism here
				ret = ret.sequential();
			}
		} else {
			ret = Stream.empty();
		}
		return ret;
	}

	private CmfObjectCounter<ExportResult> runExportImpl(final ExportState exportState,
		CmfObjectCounter<ExportResult> objectCounter, final SessionFactory<SESSION> sessionFactory,
		final SessionWrapper<SESSION> baseSession,
		final TransferContextFactory<SESSION, VALUE, CONTEXT, ?> contextFactory, final DELEGATE_FACTORY delegateFactory,
		final Transformer transformer, final ObjectFilter filter) throws ExportException, CmfStorageException {
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
		final ExportListenerPropagator listenerDelegator = new ExportListenerPropagator(objectCounter);
		final ExportEngineListener listener = listenerDelegator.getListenerProxy();
		final ConcurrentMap<ExportTarget, ExportOperation> statusMap = new ConcurrentHashMap<>();

		final PooledWorkersLogic<SessionWrapper<SESSION>, ExportTarget> logic = new PooledWorkersLogic<SessionWrapper<SESSION>, ExportTarget>() {

			@Override
			public SessionWrapper<SESSION> initialize() throws Exception {
				try {
					final SessionWrapper<SESSION> s = sessionFactory.acquireSession();
					ExportEngine.this.log.info("Worker ready with session [{}]", s.getId());
					return s;
				} catch (SessionFactoryException e) {
					throw new ExportException("Failed to obtain a worker session", e);
				}
			}

			@Override
			public void process(SessionWrapper<SESSION> session, ExportTarget target) throws Exception {
				final SESSION s = session.getWrapped();

				CmfObject.Archetype nextType = target.getType();
				final String nextId = target.getId();
				final String nextKey = target.getSearchKey();

				if (ExportEngine.this.log.isDebugEnabled()) {
					ExportEngine.this.log.debug("Polled {}", target);
				}
				ExportEngine.this.log.info("Worker thread polled {}", target);

				final boolean tx = session.begin();
				boolean ok = false;
				try {
					// Begin transaction

					final ExportDelegate<?, SESSION, SESSION_WRAPPER, VALUE, CONTEXT, ?, ?> exportDelegate = delegateFactory
						.newExportDelegate(s, nextType, nextKey);
					if (exportDelegate == null) {
						// No object found with that ID...
						ExportEngine.this.log.warn("No {} object found with searchKey[{}]",
							(nextType != null ? nextType.name() : "globally unique"), nextKey);
						return;
					}
					// This allows for object substitutions to take place
					target = exportDelegate.getExportTarget();
					nextType = target.getType();
					if (nextType == null) {
						ExportEngine.this.log.error(
							"Failed to determine the object type for target with ID[{}] and searchKey[{}]", nextId,
							nextKey);
						return;
					}

					if (ExportEngine.this.log.isDebugEnabled()) {
						ExportEngine.this.log.debug("Exporting the {} object with ID[{}]", nextType, nextId);
					}

					// The type mapper parameter is null here because it's only useful
					// for imports
					final CONTEXT ctx = contextFactory.newContext(nextId, nextType, s, 0);
					try {
						initContext(ctx);
						Result result = null;
						try {
							result = exportObject(exportState, transformer, filter, null, target, exportDelegate, ctx,
								listener, statusMap);
						} catch (Exception e) {
							// Any and all Exceptions have already been processed in exportObject,
							// so we safely absorb them here. We leave all other Throwables intact
							// so they can be caught in the worker's handler
							result = null;
						}
						if (result != null) {
							if (ExportEngine.this.log.isDebugEnabled()) {
								if (result.skipReason != null) {
									ExportEngine.this.log.debug("Skipped {} [{}]({}) : {}", target.getType(),
										target.getSearchKey(), target.getId(), result.skipReason);
								} else {
									ExportEngine.this.log.debug("Exported {} in position {}",
										result.object.getDescription(), result.objectNumber);
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
							listener.objectExportFailed(exportState.jobId, target, t);
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
			public void handleFailure(SessionWrapper<SESSION> state, ExportTarget target, Exception thrown) {
				ExportEngine.this.log.error("Failed to process {}", target, thrown);
			}

			@Override
			public void cleanup(SessionWrapper<SESSION> session) {
				session.close();
			}
		};

		final PooledWorkers<SessionWrapper<SESSION>, ExportTarget> worker = new PooledWorkers<>();

		this.log.debug("Locating export results...");
		try {
			// Fire off the workers
			listener.exportStarted(exportState);
			worker.start(logic, threadCount, "Exporter", true);
			try {
				output.info("Retrieving the results");
				final int reportCount = 1000;
				final AtomicLong targetCounter = new AtomicLong(0);
				boolean ok = false;
				try {

					final Consumer<ExportTarget> submitter = (target) -> {
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
							throw new RuntimeException(String.format("Interrupted while trying to queue up %s", target),
								e);
						}
					};

					LineScanner scanner = new LineScanner();
					final SESSION session = baseSession.getWrapped();
					Collection<String> sources = exportState.cfg.getStrings(ExportSetting.FROM);

					// Does it support multiple sources?
					if (sources.size() > 1) {
						if (!this.supportsMultipleSources) {
							throw new ExportException(String.format(
								"This engine doesn't support multiple export sources, please use only one (found %d)",
								sources.size()));
						}

						scanner.iterator(sources).forEachRemaining((line) -> {
							try (Stream<ExportTarget> s = getExportTargets(session, line, delegateFactory)) {
								s.forEachOrdered(submitter);
							} catch (Exception e) {
								this.log.warn("Failed to find the export target(s) as per [{}]", line, e);
							}
						});
					} else {
						try (Stream<ExportTarget> s = getExportTargets(session, sources.iterator().next(),
							delegateFactory)) {
							s.forEachOrdered(submitter);
						}
					}
					ok = true;
				} catch (Exception e) {
					throw new ExportException("Failed to retrieve the objects to export", e);
				} finally {
					output.info("Retrieved a total of {} objects{}", targetCounter.get(),
						ok ? "" : " (the load was incomplete)");
				}
			} finally {
				List<ExportTarget> l = worker.waitForCompletion();
				if (!l.isEmpty()) {
					output.warn("{} items left unprocessed", l.size());
					for (ExportTarget t : l) {
						output.warn("Unprocessed item: {}", t);
					}
				}
			}

			setExportProperties(objectStore);
			return listenerDelegator.getStoredObjectCounter();
		} finally {
			baseSession.close(false);

			Map<CmfObject.Archetype, Long> summary = Collections.emptyMap();
			try {
				summary = objectStore.getStoredObjectTypes();
			} catch (CmfStorageException e) {
				this.log.warn("Exception caught attempting to get the work summary", e);
			}
			listener.exportFinished(exportState.jobId, summary);
		}
	}

	protected void initContext(CONTEXT ctx) {
	}

	protected void setExportProperties(CmfObjectStore<?, ?> store) {
	}

	protected void validateEngine(SESSION session) throws ExportException {
		// By default, do nothing...
	}

	protected abstract Stream<ExportTarget> findExportTargetsByQuery(SESSION session, CfgTools configuration,
		DELEGATE_FACTORY factory, String query) throws Exception;

	protected abstract Stream<ExportTarget> findExportTargetsByPath(SESSION session, CfgTools configuration,
		DELEGATE_FACTORY factory, String path) throws Exception;

	protected abstract Stream<ExportTarget> findExportTargetsBySearchKey(SESSION session, CfgTools configuration,
		DELEGATE_FACTORY factory, String searchKey) throws Exception;

	@Override
	protected void getSupportedSettings(Collection<TransferEngineSetting> settings) {
		for (ExportSetting s : ExportSetting.values()) {
			settings.add(s);
		}
	}

	@Override
	protected ExportException newException(String message, Throwable cause) {
		return new ExportException(message, cause);
	}

	@Override
	protected final void work(CmfObjectCounter<ExportResult> counter) throws ExportException, CmfStorageException {
		// We get this at the very top because if this fails, there's no point in continuing.

		final CfgTools configuration = getSettings();
		getObjectStore().clearAttributeMappings();
		try {
			loadPrincipalMappings(getObjectStore().getValueMapper(), configuration);
		} catch (TransferException e) {
			throw new ExportException(e.getMessage(), e.getCause());
		}
		final ExportState exportState = new ExportState(getOutput(), getBaseData(), getObjectStore(), getContentStore(),
			configuration);

		try (final SessionFactory<SESSION> sessionFactory = constructSessionFactory(configuration, this.crypto)) {
			TransferContextFactory<SESSION, VALUE, CONTEXT, ?> contextFactory = null;
			DELEGATE_FACTORY delegateFactory = null;
			Transformer transformer = null;
			ObjectFilter filter = null;
			try (final SessionWrapper<SESSION> baseSession = sessionFactory.acquireSession()) {
				validateEngine(baseSession.getWrapped());
				try {
					transformer = getTransformer(configuration, null);
				} catch (Exception e) {
					throw new ExportException("Failed to initialize the configured object transformations", e);
				}

				try {
					filter = getFilter(configuration);
				} catch (Exception e) {
					throw new ExportException("Failed to initialize the configured object filters", e);
				}

				try {
					contextFactory = newContextFactory(baseSession.getWrapped(), configuration, getObjectStore(),
						getContentStore(), transformer, getOutput(), getWarningTracker());

					final String fmt = "caliente.export.product.%s";
					this.objectStore.setProperty(String.format(fmt, "name"),
						new CmfValue(contextFactory.getProductName()));
					this.objectStore.setProperty(String.format(fmt, "version"),
						new CmfValue(contextFactory.getProductVersion()));
				} catch (Exception e) {
					throw new ExportException("Failed to configure the context factory to carry out the export", e);
				}

				try {
					delegateFactory = newDelegateFactory(baseSession.getWrapped(), configuration);
				} catch (Exception e) {
					throw new ExportException("Failed to configure the delegate factory to carry out the export", e);
				}

				runExportImpl(exportState, counter, sessionFactory, baseSession, contextFactory, delegateFactory,
					transformer, filter);
			} catch (SessionFactoryException e) {
				throw new ExportException("Failed to obtain the main export session", e);
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
			}
		} catch (SessionFactoryException e) {
			throw new ExportException("Failed to configure the session factory to carry out the export", e);
		}
	}
}