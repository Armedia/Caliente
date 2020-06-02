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
package com.armedia.caliente.engine.exporter;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.InvocationHandler;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.stream.Stream;

import javax.xml.bind.JAXBException;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

import com.armedia.caliente.engine.TransferContextFactory;
import com.armedia.caliente.engine.TransferEngine;
import com.armedia.caliente.engine.TransferEngineSetting;
import com.armedia.caliente.engine.TransferException;
import com.armedia.caliente.engine.TransferSetting;
import com.armedia.caliente.engine.WarningTracker;
import com.armedia.caliente.engine.common.SessionFactory;
import com.armedia.caliente.engine.common.SessionFactoryException;
import com.armedia.caliente.engine.common.SessionWrapper;
import com.armedia.caliente.engine.converter.IntermediateProperty;
import com.armedia.caliente.engine.dynamic.filter.ObjectFilter;
import com.armedia.caliente.engine.dynamic.filter.ObjectFilterException;
import com.armedia.caliente.engine.dynamic.transformer.Transformer;
import com.armedia.caliente.engine.dynamic.transformer.TransformerException;
import com.armedia.caliente.engine.tools.xml.MetadataT;
import com.armedia.caliente.engine.tools.xml.XmlBase;
import com.armedia.caliente.store.CmfAttributeTranslator;
import com.armedia.caliente.store.CmfContentStore;
import com.armedia.caliente.store.CmfContentStream;
import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.store.CmfObjectCounter;
import com.armedia.caliente.store.CmfObjectRef;
import com.armedia.caliente.store.CmfObjectStore;
import com.armedia.caliente.store.CmfObjectStore.LockStatus;
import com.armedia.caliente.store.CmfObjectStore.StoreStatus;
import com.armedia.caliente.store.CmfProperty;
import com.armedia.caliente.store.CmfStorageException;
import com.armedia.caliente.store.CmfValue;
import com.armedia.caliente.store.CmfValue.Type;
import com.armedia.commons.utilities.CfgTools;
import com.armedia.commons.utilities.DelayedSupplier;
import com.armedia.commons.utilities.FileNameTools;
import com.armedia.commons.utilities.PooledWorkers;
import com.armedia.commons.utilities.PooledWorkersLogic;
import com.armedia.commons.utilities.Tools;
import com.armedia.commons.utilities.concurrent.ConcurrentTools;
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
	private final ConcurrentMap<String, String> idPathFixes = new ConcurrentHashMap<>();
	private final ConcurrentMap<String, String> idFolderNames = new ConcurrentHashMap<>();

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
		CmfObjectStore<?> objectStore, CmfContentStore<?, ?> contentStore, CfgTools settings,
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

	private Result exportObject(ExportState exportState, final Transformer transformer, final ObjectFilter filter,
		final ExportDelegate<?, SESSION, SESSION_WRAPPER, VALUE, CONTEXT, ?, ?> referrent,
		final ExportDelegate<?, SESSION, SESSION_WRAPPER, VALUE, CONTEXT, ?, ?> sourceObject, final CONTEXT ctx,
		final ExportListener listener, final ConcurrentMap<ExportTarget, DelayedSupplier<ExportOperation>> statusMap)
		throws ExportException, CmfStorageException {
		final ExportTarget target = sourceObject.getExportTarget();
		try {
			if (!ctx.isSupported(target.getType())) { return this.unsupportedResult; }

			final ExportTarget referrentTarget;
			final String referrentLogLabel;
			if (referrent != null) {
				referrentTarget = (referrent != null ? referrent.getExportTarget() : null);
				referrentLogLabel = String.format("%s [%s](%s)", referrentTarget.getType(), referrent.getLabel(),
					referrentTarget.getId());
			} else {
				referrentTarget = null;
				referrentLogLabel = "direct search";
			}

			final CmfObject.Archetype type = target.getType();
			final String id = target.getId();
			final String objectLabel = sourceObject.getLabel();
			final String targetLogLabel = String.format("%s [%s](%s)", type, objectLabel, id);
			final LockStatus locked;
			final ExportOperation thisStatus;
			try {
				locked = exportState.objectStore.lockForStorage(target, referrentTarget,
					Tools.coalesce(sourceObject.getHistoryId(), sourceObject.getObjectId()), ctx.getId());
				switch (locked) {
					case LOCK_ACQUIRED:
						// We got the lock, which means we create the locker object
						this.log.trace("Locked {} for storage", targetLogLabel);
						thisStatus = new ExportOperation(target, targetLogLabel, referrentTarget, referrentLogLabel);
						ConcurrentTools.createIfAbsent(statusMap, target, (t) -> new DelayedSupplier<>())
							.set(thisStatus);
						break;

					case ALREADY_LOCKED: // fall-through
					case ALREADY_FAILED: // fall-through
					case ALREADY_STORED: // fall-through
					default:
						this.log.trace("{} result = {}", targetLogLabel, locked);
						return this.staticLockResults.get(locked);
				}
			} catch (CmfStorageException e) {
				throw new ExportException(
					String.format("Exception caught attempting to lock a %s for storage", targetLogLabel), e);
			}

			listener.objectExportStarted(exportState.jobId, target, referrentTarget);

			final Result result = doExportObject(exportState, transformer, filter, referrent, target, sourceObject, ctx,
				listener, statusMap, thisStatus);
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

	protected String getFixedFolderName(CONTEXT ctx, CmfObject<VALUE> object, String folderId, Object ecmObject)
		throws ExportException {
		try {
			return ConcurrentTools.createIfAbsent(this.idFolderNames, folderId, (id) -> {
				String name = ctx.getFixedName(CmfObject.Archetype.FOLDER, id, id);
				if (!StringUtils.isBlank(name)) { return name; }
				name = findFolderName(ctx.getSession(), id, ecmObject);
				if (!StringUtils.isBlank(name)) { return name; }
				throw new Exception(String.format("Could not find the folder with ID [%s] to retrieve its name", id));
			});
		} catch (Exception e) {
			throw new ExportException(
				String.format("Can't calculate the folder name for folder with ID [%s] - please fix this!", folderId),
				e);
		}
	}

	protected abstract String findFolderName(SESSION session, String folderId, Object ecmObject);

	protected Collection<VALUE> calculateFixedPath(CONTEXT ctx, CmfObject<VALUE> object, Object ecmObject)
		throws ExportException {
		CmfProperty<VALUE> parentPathIds = object.getProperty(IntermediateProperty.PARENT_TREE_IDS);
		if ((parentPathIds == null) || !parentPathIds.hasValues()) { return Collections.emptyList(); }

		Collection<VALUE> values = new ArrayList<>(parentPathIds.getValueCount());
		for (VALUE v : parentPathIds) {
			final String fixed = ConcurrentTools.createIfAbsent(this.idPathFixes, v.toString(), (idPath) -> {
				List<String> ids = FileNameTools.tokenize(idPath, '/');
				List<String> names = new ArrayList<>(ids.size());
				for (String id : ids) {
					String name = getFixedFolderName(ctx, object, id, ecmObject);
					if (name == null) {
						throw new ExportException(
							String.format("Did not cache the folder name for the folder with ID [%s]", id));
					}
					names.add(name);
				}
				return FileNameTools.reconstitute(names, true, false, '/');
			});
			try {
				values.add(object.getTranslator().getValue(Type.STRING, fixed));
			} catch (ParseException e) {
				// Should never happen...but still
				throw new ExportException(String.format("Failed to encode the String value [%s] as a STRING for %s",
					fixed, object.getDescription()), e);
			}
		}
		return values;
	}

	protected boolean shouldFetchHistory(
		final ExportDelegate<?, SESSION, SESSION_WRAPPER, VALUE, CONTEXT, ?, ?> referrent, CmfObject<VALUE> marshaled) {
		if (referrent == null) { return true; }
		return !Objects.equals(referrent.getHistoryId(), marshaled.getHistoryId());
	}

	private Result doExportObject(ExportState exportState, final Transformer transformer, final ObjectFilter filter,
		final ExportDelegate<?, SESSION, SESSION_WRAPPER, VALUE, CONTEXT, ?, ?> referrent, final ExportTarget target,
		final ExportDelegate<?, SESSION, SESSION_WRAPPER, VALUE, CONTEXT, ?, ?> sourceObject, final CONTEXT ctx,
		final ExportListener listener, final ConcurrentMap<ExportTarget, DelayedSupplier<ExportOperation>> statusMap,
		final ExportOperation thisStatus) throws ExportException, CmfStorageException {

		boolean success = false;
		boolean pushed = false;
		try {
			if (target == null) { throw new IllegalArgumentException("Must provide the original export target"); }
			if (sourceObject == null) {
				throw new IllegalArgumentException("Must provide the original object to export");
			}
			if (ctx == null) { throw new IllegalArgumentException("Must provide a context to operate in"); }

			final ExportTarget referrentTarget = (referrent != null ? referrent.getExportTarget() : null);
			if (referrent != null) {
				ctx.pushReferrent(referrentTarget);
				pushed = true;
			}
			final CmfObjectStore<?> objectStore = exportState.objectStore;
			final CmfContentStore<?, ?> streamStore = exportState.streamStore;

			final CmfObject.Archetype type = target.getType();
			final String id = target.getId();
			final String objectLabel = sourceObject.getLabel();
			final String logLabel = String.format("%s [%s](%s)", type, objectLabel, id);

			if (this.log.isDebugEnabled()) {
				if (referrent != null) {
					this.log.debug("Exporting {} (referenced by {})", logLabel, referrent);
				} else {
					this.log.debug("Exporting {} (from the main search)", logLabel);
				}
			}
			CmfObject<VALUE> marshaled = sourceObject.marshal(ctx, referrentTarget);
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
				this.log.debug("{} requires {} objects for successful storage", logLabel, referenced.size());
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
				ConcurrentTools.createIfAbsent(statusMap, requirement.getExportTarget(),
					(k) -> new DelayedSupplier<>());
				final Result r;
				try {
					r = exportObject(exportState, transformer, filter, sourceObject, requirement, ctx, listener,
						statusMap);
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
					DelayedSupplier<ExportOperation> synchronizer = statusMap.get(requirement);
					ExportOperation status;
					try {
						status = synchronizer.get(5, TimeUnit.SECONDS);
					} catch (TimeoutException e) {
						throw new ExportException(String.format(
							"Timed out waiting for the synchronizer to be added for requirement [%s] of %s",
							requirement, logLabel), e);
					} catch (InterruptedException e) {
						throw new ExportException(String.format(
							"Interrupted waiting for the synchronizer to be added for requirement [%s] of %s",
							requirement, logLabel), e);
					}
					if (status == null) {
						throw new ExportException(
							String.format("No export status found for requirement [%s] of %s", requirement, logLabel));
					}
					try {
						long waitTime = status.waitUntilCompleted(
							() -> ctx.printf("Waiting on a dependency:%n\tFOR  : %s%n\tFROM : %s%n\tOWNER: %s for %s",
								status.getTargetLabel(), logLabel, status.getCreatorThread().getName(),
								status.getReferrentLabel()));
						ctx.printf("Waited for %s from %s for %d ms", status.getTargetLabel(), logLabel, waitTime);
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
			if (!latestOnly && shouldFetchHistory(referrent, marshaled)) {
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
					this.log.debug("{} requires {} antecedent versions for successful storage", logLabel,
						referenced.size());
				}
				CmfObjectRef prev = null;
				for (ExportDelegate<?, SESSION, SESSION_WRAPPER, VALUE, CONTEXT, ?, ?> antecedent : referenced) {
					try {
						exportObject(exportState, transformer, filter, sourceObject, antecedent, ctx, listener,
							statusMap);
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

			final CmfAttributeTranslator<VALUE> translator = getTranslator();
			String finalName = ctx.getFixedName(marshaled);
			if (StringUtils.isEmpty(finalName)) {
				finalName = marshaled.getName();
			} else {
				try {
					marshaled.setProperty(new CmfProperty<>(IntermediateProperty.FIXED_NAME, CmfValue.Type.STRING,
						false, translator.getValue(Type.STRING, finalName)));
				} catch (ParseException e) {
					throw new ExportException(String.format("Failed to encode the String value [%s] as a STRING for %s",
						finalName, marshaled.getDescription()));
				}
			}
			marshaled.setProperty(new CmfProperty<>(IntermediateProperty.FIXED_PATH, CmfValue.Type.STRING, true,
				calculateFixedPath(ctx, marshaled, sourceObject.getEcmObject())));

			if (marshaled.getType() == CmfObject.Archetype.FOLDER) {
				// Let's be smart here - cache the final name for folders as we scan through them
				final String str = finalName; // B/c it's needed for the lambda
				ConcurrentTools.createIfAbsent(this.idFolderNames, marshaled.getId(), (folderId) -> str);
			}

			if (transformer != null) {
				try {
					marshaled = transformer.transform(objectStore.getValueMapper(),
						getTranslator().getAttributeNameMapper(), null, marshaled);
				} catch (TransformerException e) {
					throw new ExportException(String.format("Transformation failed for %s", marshaled.getDescription()),
						e);
				}
			}

			CmfObject<CmfValue> encoded = translator.encodeObject(marshaled);
			final Long ret = objectStore.storeObject(encoded);
			marshaled.copyNumber(encoded);

			if (ret == null) {
				// Should be impossible, but still guard against it
				if (this.log.isTraceEnabled()) {
					this.log.trace("{} was stored by another thread", logLabel);
				}
				return new Result(ExportSkipReason.ALREADY_STORED);
			}

			if (this.log.isDebugEnabled()) {
				this.log.debug("Executing supplemental storage for {}", logLabel);
			}

			if (ctx.isSupportsCompanionMetadata(type)) {
				CmfContentStream md = new CmfContentStream(marshaled, 0, "metadata", 1);
				md.setProperty(CmfContentStream.BASENAME, "metadata." + type.name());
				md.setExtension("xml");
				CmfContentStore<?, ?>.Handle h = streamStore
					.addContentStream(CmfAttributeTranslator.CMFVALUE_TRANSLATOR, encoded, md);
				try (OutputStream out = h.createStream()) {
					XmlBase.storeToXML(new MetadataT(encoded), out);
				} catch (JAXBException e) {
					this.log.warn("Failed to construct the XML companion metadata for {}", marshaled.getDescription(),
						e);
				} catch (IOException e) {
					this.log.warn("Failed to write out the XML companion metadata for {}", marshaled.getDescription(),
						e);
				}
			}

			try {
				final boolean includeRenditions = !ctx.getSettings().getBoolean(TransferSetting.NO_RENDITIONS);
				List<CmfContentStream> contentStreams = sourceObject.storeContent(ctx, getTranslator(), marshaled,
					referrentTarget, streamStore, includeRenditions);
				if ((contentStreams != null) && !contentStreams.isEmpty()) {
					objectStore.setContentStreams(marshaled, contentStreams);
				}
			} catch (Exception e) {
				if (CmfStorageException.class.isInstance(e) || CmfStorageException.class.isInstance(e.getCause())) {
					throw new ExportException(String.format("Failed to execute the content storage for %s", logLabel),
						e);
				}
				// Otherwise, just log it and keep moving
				this.log.warn("Failed to store the content streams for {}", logLabel, e);
			}

			if (this.log.isDebugEnabled()) {
				this.log.debug("Successfully stored {} as object # {}", logLabel, ret);
			}

			if (!latestOnly && shouldFetchHistory(referrent, marshaled)) {
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
					this.log.debug("{} is succeeded by {} additional versions for successful storage", logLabel,
						referenced.size());
				}
				CmfObjectRef prev = marshaled;
				for (ExportDelegate<?, SESSION, SESSION_WRAPPER, VALUE, CONTEXT, ?, ?> successor : referenced) {
					try {
						exportObject(exportState, transformer, filter, sourceObject, successor, ctx, listener,
							statusMap);
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
				this.log.debug("{} has {} dependent objects to store", logLabel, referenced.size());
			}
			int dependentsExported = 0;
			for (ExportDelegate<?, SESSION, SESSION_WRAPPER, VALUE, CONTEXT, ?, ?> dependent : referenced) {
				try {
					exportObject(exportState, transformer, filter, sourceObject, dependent, ctx, listener, statusMap);
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
			if ((referrent != null) && pushed) {
				ctx.popReferrent();
			}
		}
	}

	/**
	 * Detect the type of search to conduct, or {@code null} if this search pattern isn't supported.
	 *
	 * @return the type of search to conduct, or {@code null} if this search pattern isn't
	 *         supported.
	 */
	protected SearchType detectSearchType(String source) {
		if (source == null) { return null; }
		if (source.startsWith("%")) { return SearchType.KEY; }
		if (source.startsWith("/")) { return SearchType.PATH; }
		return SearchType.QUERY;
	}

	private Stream<ExportTarget> getExportTargets(SESSION session, String source, DELEGATE_FACTORY delegateFactory)
		throws Exception {
		final SearchType searchType = detectSearchType(source);
		if (searchType == null) {
			throw new ExportException(
				String.format("This engine doesn't know how to search for exportable objects using [%s]", source));
		}

		if (!this.supportedSearches.contains(searchType)) {
			throw new ExportException(String.format("This engine doesn't support searches by %s (from the source [%s])",
				searchType.name().toLowerCase(), source));
		}

		Stream<ExportTarget> ret = null;
		switch (searchType) {
			case KEY:
				// SearchKey!
				final String searchKey = StringUtils.strip(source.substring(1));
				if (StringUtils.isEmpty(searchKey)) {
					throw new ExportException(
						String.format("Invalid search key [%s] - no object can be found with an empty key"));
				}
				ret = findExportTargetsBySearchKey(session, this.settings, delegateFactory, searchKey);
				break;
			case PATH:
				// CMS Path!
				ret = findExportTargetsByPath(session, this.settings, delegateFactory, source);
				break;
			case QUERY:
				// Query string!
				ret = findExportTargetsByQuery(session, this.settings, delegateFactory, source);
			default:
				break;
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
		final CmfObjectStore<?> objectStore = exportState.objectStore;
		final CfgTools settings = exportState.cfg;
		final int threadCount = getThreadCount(settings);
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
		final ConcurrentMap<ExportTarget, DelayedSupplier<ExportOperation>> statusMap = new ConcurrentHashMap<>();

		final PooledWorkersLogic<SessionWrapper<SESSION>, ExportTarget, Exception> logic = new PooledWorkersLogic<SessionWrapper<SESSION>, ExportTarget, Exception>() {

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
				final SESSION s = session.get();

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
							result = exportObject(exportState, transformer, filter, null, exportDelegate, ctx, listener,
								statusMap);
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
				final int reportCount = 1000;
				final AtomicReference<String> currentSource = new AtomicReference<>(null);
				final AtomicLong sourceCounter = new AtomicLong(0);
				final AtomicLong totalCounter = new AtomicLong(0);
				final AtomicReference<Exception> thrown = new AtomicReference<>(null);

				final Consumer<ExportTarget> submitter = (target) -> {
					if (target == null) { return; }
					if (target.isNull()) {
						ExportEngine.this.log.warn("Skipping a null target: {}", target);
						return;
					}

					final long cTotal = totalCounter.incrementAndGet();
					final long cSource = sourceCounter.incrementAndGet();
					if ((cSource % reportCount) == 0) {
						listener.sourceSearchMilestone(currentSource.get(), cSource, cTotal);
					}
					try {
						worker.addWorkItem(target);
					} catch (InterruptedException e) {
						throw new RuntimeException(String.format("Interrupted while trying to queue up %s", target), e);
					}
				};

				LineScanner scanner = new LineScanner();
				final SESSION session = baseSession.get();
				Collection<String> sources = exportState.cfg.getStrings(ExportSetting.FROM);

				Stream<String> baseStream = scanner.iterator(sources).stream();
				if (!this.supportsMultipleSources) {
					this.log.warn(
						"This engine doesn't support multiple export sources, this export will only cover the first non-recurring source encountered");
					baseStream = baseStream.limit(1);
				}
				try (Stream<String> stream = baseStream) {
					stream.forEach((line) -> {
						sourceCounter.set(0);
						currentSource.set(line);
						listener.sourceSearchStarted(line);
						try (Stream<ExportTarget> s = getExportTargets(session, line, delegateFactory)) {
							s.forEach(submitter);
						} catch (Exception e) {
							thrown.set(e);
						} finally {
							try {
								if (thrown.get() == null) {
									listener.sourceSearchCompleted(line, sourceCounter.get(), totalCounter.get());
								} else {
									listener.sourceSearchFailed(line, sourceCounter.get(), totalCounter.get(),
										thrown.get());
								}
							} finally {
								thrown.set(null);
							}
						}
					});
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

	protected void setExportProperties(CmfObjectStore<?> store) {
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
				validateEngine(baseSession.get());
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
					contextFactory = newContextFactory(baseSession.get(), configuration, getObjectStore(),
						getContentStore(), transformer, getOutput(), getWarningTracker());

					final String fmt = "caliente.export.product.%s";
					this.objectStore.setProperty(String.format(fmt, "name"),
						CmfValue.of(contextFactory.getProductName()));
					this.objectStore.setProperty(String.format(fmt, "version"),
						CmfValue.of(contextFactory.getProductVersion()));
				} catch (Exception e) {
					throw new ExportException("Failed to configure the context factory to carry out the export", e);
				}

				try {
					delegateFactory = newDelegateFactory(baseSession.get(), configuration);
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