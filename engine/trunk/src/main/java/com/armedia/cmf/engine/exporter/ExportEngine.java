/**
 *
 */

package com.armedia.cmf.engine.exporter;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;

import com.armedia.cmf.engine.CmfCrypt;
import com.armedia.cmf.engine.ContextFactory;
import com.armedia.cmf.engine.PooledWorkers;
import com.armedia.cmf.engine.SessionFactory;
import com.armedia.cmf.engine.SessionWrapper;
import com.armedia.cmf.engine.TransferEngine;
import com.armedia.cmf.engine.TransferSetting;
import com.armedia.cmf.storage.CmfContentInfo;
import com.armedia.cmf.storage.CmfContentStore;
import com.armedia.cmf.storage.CmfObject;
import com.armedia.cmf.storage.CmfObjectCounter;
import com.armedia.cmf.storage.CmfObjectStore;
import com.armedia.cmf.storage.CmfStorageException;
import com.armedia.cmf.storage.CmfType;
import com.armedia.commons.utilities.CfgTools;

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

		public Result(Long objectNumber, CmfObject<V> marshaled) {
			this.objectNumber = objectNumber;
			this.marshaled = marshaled;
			this.message = null;
		}

		public Result(ExportSkipReason message) {
			this.objectNumber = null;
			this.marshaled = null;
			this.message = message;
		}
	}

	private class ExportListenerDelegator extends ListenerDelegator<ExportResult> implements ExportEngineListener {

		private final Collection<ExportEngineListener> listeners = getListeners();

		private ExportListenerDelegator(CmfObjectCounter<ExportResult> counter) {
			super(counter);
		}

		@Override
		public void exportStarted(CfgTools configuration) {
			getStoredObjectCounter().reset();
			for (ExportEngineListener l : this.listeners) {
				try {
					l.exportStarted(configuration);
				} catch (Exception e) {
					if (this.log.isDebugEnabled()) {
						this.log.error("Exception caught during listener propagation", e);
					}
				}
			}
		}

		@Override
		public void objectExportStarted(CmfType objectType, String objectId) {
			for (ExportEngineListener l : this.listeners) {
				try {
					l.objectExportStarted(objectType, objectId);
				} catch (Exception e) {
					if (this.log.isDebugEnabled()) {
						this.log.error("Exception caught during listener propagation", e);
					}
				}
			}
		}

		@Override
		public void objectExportCompleted(CmfObject<?> object, Long objectNumber) {
			getStoredObjectCounter().increment(object.getType(), ExportResult.EXPORTED);
			for (ExportEngineListener l : this.listeners) {
				try {
					l.objectExportCompleted(object, objectNumber);
				} catch (Exception e) {
					if (this.log.isDebugEnabled()) {
						this.log.error("Exception caught during listener propagation", e);
					}
				}
			}
		}

		@Override
		public void objectSkipped(CmfType objectType, String objectId, ExportSkipReason reason) {
			getStoredObjectCounter().increment(objectType, ExportResult.SKIPPED);
			for (ExportEngineListener l : this.listeners) {
				try {
					l.objectSkipped(objectType, objectId, reason);
				} catch (Exception e) {
					if (this.log.isDebugEnabled()) {
						this.log.error("Exception caught during listener propagation", e);
					}
				}
			}
		}

		@Override
		public void objectExportFailed(CmfType objectType, String objectId, Throwable thrown) {
			getStoredObjectCounter().increment(objectType, ExportResult.FAILED);
			for (ExportEngineListener l : this.listeners) {
				try {
					l.objectExportFailed(objectType, objectId, thrown);
				} catch (Exception e) {
					if (this.log.isDebugEnabled()) {
						this.log.error("Exception caught during listener propagation", e);
					}
				}
			}
		}

		@Override
		public void exportFinished(Map<CmfType, Integer> summary) {
			for (ExportEngineListener l : this.listeners) {
				try {
					l.exportFinished(summary);
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

	private Result exportObject(final CmfObjectStore<?, ?> objectStore, final CmfContentStore<?, ?, ?> streamStore,
		final ExportTarget referrent, final ExportTarget target, final ExportDelegate<?, S, W, V, C, ?, ?> sourceObject,
		final C ctx, final ExportListenerDelegator listenerDelegator, final Map<ExportTarget, ExportStatus> statusMap)
		throws ExportException, CmfStorageException {
		try {
			listenerDelegator.objectExportStarted(target.getType(), target.getId());
			Result result = null;
			if (ctx.isSupported(target.getType())) {
				result = doExportObject(objectStore, streamStore, referrent, target, sourceObject, ctx,
					listenerDelegator, statusMap);
			} else {
				result = new Result(ExportSkipReason.UNSUPPORTED);
			}
			if ((result.objectNumber != null) && (result.marshaled != null)) {
				listenerDelegator.objectExportCompleted(result.marshaled, result.objectNumber);
			} else {
				listenerDelegator.objectSkipped(target.getType(), target.getId(), result.message);
			}
			return result;
		} catch (Exception e) {
			listenerDelegator.objectExportFailed(target.getType(), target.getId(), e);
			if (e instanceof ExportException) { throw ExportException.class.cast(e); }
			if (e instanceof CmfStorageException) { throw CmfStorageException.class.cast(e); }
			throw RuntimeException.class.cast(e);
		}
	}

	private Result doExportObject(final CmfObjectStore<?, ?> objectStore, final CmfContentStore<?, ?, ?> streamStore,
		final ExportTarget referrent, final ExportTarget target, final ExportDelegate<?, S, W, V, C, ?, ?> sourceObject,
		final C ctx, final ExportListenerDelegator listenerDelegator, final Map<ExportTarget, ExportStatus> statusMap)
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

		if (this.log.isTraceEnabled()) {
			this.log.trace(String.format("Locked %s for storage", label));
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
				Result r = exportObject(objectStore, streamStore, target, requirement.getExportTarget(), requirement,
					ctx, listenerDelegator, statusMap);
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
					if (!status.isSuccessful()) { throw new ExportException(
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
					exportObject(objectStore, streamStore, target, antecedent.getExportTarget(), antecedent, ctx,
						listenerDelegator, statusMap);
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
					exportObject(objectStore, streamStore, target, successor.getExportTarget(), successor, ctx,
						listenerDelegator, statusMap);
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
			for (ExportDelegate<?, S, W, V, C, ?, ?> dependent : referenced) {
				exportObject(objectStore, streamStore, target, dependent.getExportTarget(), dependent, ctx,
					listenerDelegator, statusMap);
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

				return runExportImpl(output, objectStore, contentStore, settings, counter, configuration,
					sessionFactory, baseSession, contextFactory, delegateFactory);
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

	private CmfObjectCounter<ExportResult> runExportImpl(final Logger output, final CmfObjectStore<?, ?> objectStore,
		final CmfContentStore<?, ?, ?> contentStore, Map<String, ?> settings, CmfObjectCounter<ExportResult> counter,
		final CfgTools configuration, final SessionFactory<S> sessionFactory, final SessionWrapper<S> baseSession,
		final ContextFactory<S, V, C, ?> contextFactory, final DF delegateFactory)
		throws ExportException, CmfStorageException {
		final int threadCount;
		final int backlogSize;
		// Ensure nobody changes this under our feet
		synchronized (this) {
			threadCount = getThreadCount(settings);
			backlogSize = getBacklogSize(settings);
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
				if (this.log.isDebugEnabled()) {
					this.log.debug(String.format("Got session [%s]", s.getId()));
				}
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
						Result result = exportObject(objectStore, contentStore, null, next, exportDelegate, ctx,
							listenerDelegator, statusMap);
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
					listenerDelegator.objectExportFailed(nextType, nextId, t);
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

		final Iterator<ExportTarget> results;
		this.log.debug("Locating export results...");
		try {
			results = findExportResults(baseSession.getWrapped(), configuration, delegateFactory);
		} catch (Exception e) {
			throw new ExportException(String.format("Failed to obtain the export results with settings: %s", settings),
				e);
		}

		try {
			// Fire off the workers
			worker.start(threadCount, new ExportTarget(), true);

			int c = 0;
			// 1: run the query for the given predicate
			listenerDelegator.exportStarted(configuration);
			// 2: iterate over the results, gathering up the object IDs
			try {
				this.log.debug("Processing the located results...");
				while ((results != null) && results.hasNext()) {
					final ExportTarget target = results.next();
					String msg = String.format("Processing item %s", target);
					this.log.info(msg);
					if (output != null) {
						output.info(msg);
					}
					try {
						worker.addWorkItem(target);
					} catch (InterruptedException e) {
						Thread.currentThread().interrupt();
						if (this.log.isDebugEnabled()) {
							this.log.warn(String.format("Thread interrupted after reading %d object targets", c), e);
						} else {
							this.log.warn(String.format("Thread interrupted after reading %d objects targets", c));
						}
						break;
					} finally {
						c++;
					}
				}
				this.log.info(String.format("Submitted the entire export workload (%d objects)", c));
				if (output != null) {
					output.info(String.format("Submitted the entire export workload (%d objects)", c));
				}
			} finally {
				worker.waitForCompletion();
			}

			setExportProperties(objectStore);
			return listenerDelegator.getStoredObjectCounter();
		} finally {
			baseSession.close(false);

			Map<CmfType, Integer> summary = Collections.emptyMap();
			try {
				summary = objectStore.getStoredObjectTypes();
			} catch (CmfStorageException e) {
				this.log.warn("Exception caught attempting to get the work summary", e);
			}
			listenerDelegator.exportFinished(summary);
		}
	}

	protected void initContext(C ctx) {
	}

	protected void setExportProperties(CmfObjectStore<?, ?> store) {
	}

	protected abstract Iterator<ExportTarget> findExportResults(S session, CfgTools configuration, DF factory)
		throws Exception;

	/*
	protected abstract ExportDelegate<?, S, W, V, C, ?> getExportDelegate(S session, CmfType type,
		String searchKey, CfgTools configuration) throws Exception;
	 */

	public static ExportEngine<?, ?, ?, ?, ?, ?> getExportEngine(String targetName) {
		return TransferEngine.getTransferEngine(ExportEngine.class, targetName);
	}
}