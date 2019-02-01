/**
 *
 */

package com.armedia.caliente.engine.dfc.importer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.lang3.StringUtils;

import com.armedia.caliente.engine.dfc.DctmAttributeHandlers;
import com.armedia.caliente.engine.dfc.DctmAttributeHandlers.AttributeHandler;
import com.armedia.caliente.engine.dfc.DctmAttributes;
import com.armedia.caliente.engine.dfc.DctmDataType;
import com.armedia.caliente.engine.dfc.DctmObjectType;
import com.armedia.caliente.engine.dfc.DctmSessionWrapper;
import com.armedia.caliente.engine.dfc.DctmTranslator;
import com.armedia.caliente.engine.importer.ImportDelegate;
import com.armedia.caliente.engine.importer.ImportException;
import com.armedia.caliente.engine.importer.ImportOutcome;
import com.armedia.caliente.engine.importer.ImportReplaceMode;
import com.armedia.caliente.engine.importer.ImportResult;
import com.armedia.caliente.store.CmfAttribute;
import com.armedia.caliente.store.CmfAttributeTranslator;
import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.store.CmfStorageException;
import com.armedia.commons.dfc.util.DfUtils;
import com.armedia.commons.dfc.util.DfValueFactory;
import com.armedia.commons.utilities.Tools;
import com.documentum.fc.client.DfDocument;
import com.documentum.fc.client.IDfCollection;
import com.documentum.fc.client.IDfLocalTransaction;
import com.documentum.fc.client.IDfPersistentObject;
import com.documentum.fc.client.IDfQuery;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.client.IDfType;
import com.documentum.fc.client.aspect.IDfAspects;
import com.documentum.fc.client.aspect.IDfAttachAspectCallback;
import com.documentum.fc.client.aspect.IDfDetachAspectCallback;
import com.documentum.fc.common.DfException;
import com.documentum.fc.common.IDfAttr;
import com.documentum.fc.common.IDfId;
import com.documentum.fc.common.IDfValue;
import com.documentum.fc.common.admin.DfAdminException;

/**
 * @author Diego Rivera &lt;diego.rivera@armedia.com&gt;
 *
 * @param <T>
 */
public abstract class DctmImportDelegate<T extends IDfPersistentObject> extends
	ImportDelegate<T, IDfSession, DctmSessionWrapper, IDfValue, DctmImportContext, DctmImportDelegateFactory, DctmImportEngine> {

	private static final IDfValue CURRENT_VERSION_LABEL = DfValueFactory
		.newStringValue(DfDocument.CURRENT_VERSION_LABEL);

	private final class AspectHelper implements IDfAttachAspectCallback, IDfDetachAspectCallback {
		private final AtomicReference<T> ref;
		private final Set<String> defaultAspects;

		private AspectHelper(T object) throws DfException {
			this.ref = new AtomicReference<>(object);
			IDfSession session = object.getSession();
			IDfType type = object.getType();
			IDfPersistentObject info = session.getObjectByQualification(
				String.format("dmi_type_info where r_type_id = %s", DfUtils.quoteString(type.getObjectId().getId())));
			Set<String> defaultAspects = Collections.emptySet();
			if ((info != null) && info.hasAttr(DctmAttributes.DEFAULT_ASPECTS)) {
				final int c = info.getValueCount(DctmAttributes.DEFAULT_ASPECTS);
				defaultAspects = new LinkedHashSet<>();
				for (int i = 0; i < c; i++) {
					defaultAspects.add(info.getRepeatingString(DctmAttributes.DEFAULT_ASPECTS, i));
				}
			}
			this.defaultAspects = Tools.freezeSet(defaultAspects, true);
		}

		private T attachAspects(Set<String> aspects) throws DfException {
			for (String a : aspects) {
				// We make sure we don't attach aspects that are part of the type
				if (this.defaultAspects.contains(a)) {
					continue;
				}
				IDfAspects.class.cast(this.ref.get()).attachAspect(a, this);
			}
			return this.ref.get();
		}

		@Override
		public void doPostAttach(IDfPersistentObject object) throws Exception {
			this.ref.set(castObject(object));
		}

		private T detachAspects(Set<String> aspects) throws DfException {
			for (String a : aspects) {
				// We make sure we don't detach aspects that are part of the type
				if (this.defaultAspects.contains(a)) {
					continue;
				}
				IDfAspects.class.cast(this.ref.get()).detachAspect(a, this);
			}
			return this.ref.get();
		}

		@Override
		public void doPostDetach(IDfPersistentObject object) throws Exception {
			this.ref.set(castObject(object));
		}

		public T get() {
			return this.ref.get();
		}
	}

	private final DctmObjectType dctmType;

	protected DctmImportDelegate(DctmImportDelegateFactory factory, Class<T> objectClass, DctmObjectType expectedType,
		CmfObject<IDfValue> storedObject) throws Exception {
		super(factory, objectClass, storedObject);
		this.dctmType = expectedType;
		if (expectedType.getStoredObjectType() != storedObject.getType()) {
			throw new IllegalArgumentException(
				String.format("This delegate is meant for [%s], but the given object is of type [%s]",
					expectedType.getStoredObjectType(), storedObject.getType()));
		}
	}

	protected final T castObject(IDfPersistentObject object) throws DfException {
		if (object == null) { return null; }
		Class<T> dfClass = getObjectClass();
		if (!dfClass.isInstance(object)) {
			throw new DfException(String.format("Expected an object of class %s, but got one of class %s",
				dfClass.getCanonicalName(), object.getClass().getCanonicalName()));
		}
		return dfClass.cast(object);
	}

	public final DctmObjectType getDctmType() {
		return this.dctmType;
	}

	protected abstract String calculateLabel(T object) throws DfException, ImportException;

	protected final CmfAttribute<IDfValue> newStoredAttribute(IDfAttr attr, IDfValue... values) {
		if (values == null) {
			return newStoredAttribute(attr, (Collection<IDfValue>) null);
		} else {
			return newStoredAttribute(attr, Arrays.asList(values));
		}
	}

	protected final CmfAttribute<IDfValue> newStoredAttribute(IDfAttr attr, Collection<IDfValue> values) {
		return new CmfAttribute<>(attr.getName(), DctmDataType.fromAttribute(attr).getStoredType(), attr.isRepeating(),
			values);
	}

	protected boolean isTransitoryObject(T object) throws DfException, ImportException {
		return false;
	}

	protected void prepareOperation(T object, boolean newObject, DctmImportContext context)
		throws DfException, ImportException {
	}

	protected IDfId persistChanges(T object, DctmImportContext context) throws DfException, ImportException {
		IDfId newId = object.getObjectId();
		object.save();
		return newId;
	}

	protected void finalizeOperation(T sysObject) throws DfException, ImportException {
	}

	protected boolean isShortConstructionCycle() {
		return false;
	}

	@Override
	protected final Collection<ImportOutcome> importObject(CmfAttributeTranslator<IDfValue> translator,
		DctmImportContext context) throws ImportException, CmfStorageException {
		if (context == null) { throw new IllegalArgumentException("Must provide a context to save the object"); }
		try {
			List<ImportOutcome> ret = new ArrayList<>(1);
			ret.add(doImportObject(context));
			return ret;
		} catch (DfException e) {
			throw new ImportException(
				String.format("Documentum Exception caught while processing %s", this.cmfObject.getDescription()), e);
		}
	}

	protected void copyBaseAttributes(T object) throws DfException {
		// We remove the version labels as well
		// Set "default" attributes
		for (CmfAttribute<IDfValue> attribute : this.cmfObject.getAttributes()) {
			// TODO check to see if we need to set any internal or system attributes of various
			// types
			final String name = attribute.getName();
			final AttributeHandler handler = getAttributeHandler(attribute);

			// for now ignore setting internal and system attributes
			boolean doSet = (!name.startsWith("r_") && !name.startsWith("i_"));
			// allow for a last-minute interception...
			doSet &= handler.includeInImport(object, attribute);

			if (doSet) {
				copyAttributeToObject(attribute, object);
			}
		}
	}

	protected final boolean deleteExisting(T object) throws DfException {
		final IDfSession session = object.getSession();
		final IDfLocalTransaction tx = DfUtils.openTransaction(session);
		this.log.debug("Deleting existing copy of {} [{}]({})", this.cmfObject.getType(), this.cmfObject.getLabel(),
			this.cmfObject.getId());
		try {
			doDeleteExisting(object);
			DfUtils.commitTransaction(session, tx);
			this.log.debug("Existing copy of {} [{}]({}) was successfully deleted", this.cmfObject.getType(),
				this.cmfObject.getLabel(), this.cmfObject.getId());
			return true;
		} catch (DfException e) {
			this.log.warn(String.format("Failed to delete the existing copy of %s [%s](%s)", this.cmfObject.getType(),
				this.cmfObject.getLabel(), this.cmfObject.getId()), e);
			DfUtils.abortTransaction(session, tx);
		}
		return false;
	}

	protected void doDeleteExisting(T object) throws DfException {
		object.destroy();
	}

	protected ImportReplaceMode getReplaceMode(DctmImportContext context) throws ImportException {
		// Only sysobjects should change this
		return ImportReplaceMode.UPDATE;
	}

	protected ImportOutcome doImportObject(DctmImportContext context) throws DfException, ImportException {
		if (context == null) { throw new IllegalArgumentException("Must provide a context to save the object"); }

		boolean ok = false;
		final ImportReplaceMode replaceMode = getReplaceMode(context);

		// We assume the worst, out of the gate
		final IDfSession session = context.getSession();
		T object = null;
		try {
			if (skipImport(context)) { return ImportOutcome.SKIPPED; }

			// TODO: Make sure that in the current replace mode, we always return an existing one...
			object = locateInCms(context);
			if ((object != null) && (context.getHistoryPosition() == 0)) {
				// There's an existing object, and this is the first object in its history...let's
				// see what we're supposed to do with regards to the replacement strategy...
				switch (replaceMode) {
					case SKIP:
						// The object should be skipped...so skip it!
						ok = true;
						return new ImportOutcome(ImportResult.SKIPPED, object.getObjectId().getId(),
							calculateLabel(object));

					case REPLACE:
						// The object should be replaced...so delete the existing object
						if (deleteExisting(object)) {
							this.log.info("Deleted the existing history for {} [{}]({}) - object ID = {}",
								this.cmfObject.getType().name(), this.cmfObject.getLabel(), this.cmfObject.getId(),
								object.getObjectId());
						} else {
							this.log.warn("Failed to delete the existing copy of {} [{}]({}) - object ID = {}",
								this.cmfObject.getType().name(), this.cmfObject.getLabel(), this.cmfObject.getId(),
								object.getObjectId());
						}
						// Setting this to null makes the rest of the code behave as if this is a
						// brand new object, which is exactly what we want.
						object = null;
						break;

					case UPDATE:
						// Do nothing, proceed as usual
						break;
				}
			}

			final boolean isNew = (object == null);
			final boolean updateVersionLabels = isVersionable(object);
			final ImportResult cmsImportResult;
			String newLabel = null;
			if (isNew) {
				// Create a new object
				this.log.info("Creating {}", this.cmfObject.getDescription());
				object = newObject(context);
				// Apply the aspects right away?
				object = applyAspects(object, context);
				cmsImportResult = ImportResult.CREATED;
				if (!isTransitoryObject(object)) {
					// DO NOT override mappings...we don't know the new object's ID until checkin is
					// completed
					context.getValueMapper().setMapping(getDctmType().getStoredObjectType(), DctmAttributes.R_OBJECT_ID,
						this.cmfObject.getId(), object.getObjectId().getId());
				}
			} else {
				// Is this correct?
				newLabel = calculateLabel(object);
				this.log.info("Acquiring lock on existing {}", this.cmfObject.getDescription());
				DfUtils.lockObject(this.log, object);
				object.fetch(null);
				this.log.info("Acquired lock on {}", this.cmfObject.getDescription());
				// First, store the mapping for the object's exact ID
				context.getValueMapper().setMapping(getDctmType().getStoredObjectType(), DctmAttributes.R_OBJECT_ID,
					this.cmfObject.getId(), object.getObjectId().getId());
				// Now, if necessary, store the mapping for the object's chronicle ID
				if (object.hasAttr(DctmAttributes.I_CHRONICLE_ID)) {
					final String attName = DctmAttributes.I_CHRONICLE_ID;
					final String sourceHistoryId = this.cmfObject.getHistoryId();
					final CmfObject.Archetype type = getDctmType().getStoredObjectType();
					if (context.getValueMapper().getTargetMapping(type, attName, sourceHistoryId) == null) {
						context.getValueMapper().setMapping(type, attName, sourceHistoryId,
							object.getId(attName).getId());
					}
				}

				if (isSameObject(object, context)) {
					ok = true;
					return new ImportOutcome(ImportResult.DUPLICATE, object.getObjectId().getId(), newLabel);
				}
				cmsImportResult = ImportResult.UPDATED;
			}

			if (isShortConstructionCycle()) {
				finalizeConstruction(object, isNew, context);
				final IDfId newId = persistChanges(object, context);
				if (!Tools.equals(object.getObjectId().getId(), newId.getId())) {
					// The object has changed... so we pull the newly-persisted object
					object = castObject(session.getObject(newId));
				}
				if (newLabel == null) {
					newLabel = calculateLabel(object);
				}
				ok = true;
				this.log.info("Completed saving {} to CMS with result [{}] for {} -> [{}]({})", getDctmType(),
					cmsImportResult, this.cmfObject.getDescription(), newLabel, object.getObjectId().getId());

				return new ImportOutcome(cmsImportResult, object.getObjectId().getId(), newLabel);
			}

			prepareOperation(object, isNew, context);
			prepareForConstruction(object, isNew, context);

			/*
			if (!isNew) {
				// If an existing object is being updated, clear out all of its attributes that are
				// not part of our attribute set
				// NOTE Only clear non internal and non system attributes
				Set<String> attributesBeingUpdated = this.storedObject.getAttributeNames();
				final int attributeCount = object.getAttrCount();
				for (int i = 0; i < attributeCount; i++) {
					final IDfAttr attr = object.getAttr(i);
					final String name = attr.getName();
					if (!name.startsWith("r_") && !name.startsWith("i_") && !attributesBeingUpdated.contains(name)) {
						clearAttributeFromObject(name, object);
					}
				}
			}
			 */

			copyBaseAttributes(object);

			if (updateVersionLabels) {
				CmfAttribute<IDfValue> versionLabel = this.cmfObject.getAttribute(DctmAttributes.R_VERSION_LABEL);
				CmfAttribute<IDfValue> current = this.cmfObject.getAttribute(DctmAttributes.I_HAS_FOLDER);
				if ((current != null) && current.getValue().asBoolean()) {
					// This is the current version...so double-check that it's not already in the
					// attribute. If it isn't there, add it
					boolean add = true;
					for (IDfValue v : versionLabel) {
						if (StringUtils.equalsIgnoreCase(v.asString(),
							DctmImportDelegate.CURRENT_VERSION_LABEL.asString())) {
							add = false;
							break;
						}
					}
					if (add) {
						if (!versionLabel.isMultivalued()) {
							// If the incoming attribute doesn't support multiple values, we
							// need to change that...
							versionLabel = new CmfAttribute<>(versionLabel.getName(), versionLabel.getType(), true,
								versionLabel.getValues());
							this.cmfObject.setAttribute(versionLabel);
						}
						versionLabel.addValue(DctmImportDelegate.CURRENT_VERSION_LABEL);
					}
				}
				copyAttributeToObject(DctmAttributes.R_VERSION_LABEL, object);
			}

			finalizeConstruction(object, isNew, context);
			final IDfId newId = persistChanges(object, context);

			if (!Tools.equals(object.getObjectId().getId(), newId.getId())) {
				// The object has changed... so we pull the newly-persisted object
				object = castObject(session.getObject(newId));
				newLabel = calculateLabel(object);
			}

			if (postConstruction(object, isNew, context)) {
				object.save();
			}

			if (cleanupAfterSave(object, isNew, context)) {
				object.save();
			}

			updateReferenced(object, context);

			if (newLabel == null) {
				newLabel = calculateLabel(object);
			}
			this.log.info("Completed saving {} to CMS with result [{}] for {}->[{}]({})", getDctmType(),
				cmsImportResult, this.cmfObject.getDescription(), newLabel, object.getObjectId().getId());

			ImportOutcome ret = new ImportOutcome(cmsImportResult, object.getObjectId().getId(), newLabel);
			ok = true;
			return ret;
		} finally {
			if (ok) {
				try {
					finalizeOperation(object);
				} catch (DfException e) {
					ok = false;
					this.log.error(
						"Caught an exception while trying to finalize the import for {} - aborting the transaction",
						this.cmfObject.getDescription(), e);
				}
				// This has to be the last thing that happens, else some of the attributes won't
				// take. There is no need to save() the object for this, as this is a direct
				// modification
				if (this.log.isTraceEnabled()) {
					this.log.trace("Updating the system attributes for {}", this.cmfObject.getDescription());
				}

				if (!updateSystemAttributes(this.cmfObject, object, context)) {
					this.log.warn("Failed to update the system attributes for {}", this.cmfObject.getDescription());
				}
			} else {
				// Clear the mapping
				context.getValueMapper().clearSourceMapping(getDctmType().getStoredObjectType(),
					DctmAttributes.R_OBJECT_ID, this.cmfObject.getId());
			}
		}
	}

	private T applyAspects(T object, DctmImportContext ctx) throws DfException, ImportException {
		// Is this an aspect-able class?
		if (!IDfAspects.class.isInstance(object)) { return object; }

		// First things first: which aspects does it already have?
		Set<String> currentAspects = new LinkedHashSet<>();
		int aspectCount = object.getValueCount(DctmAttributes.R_ASPECT_NAME);
		for (int i = 0; i < aspectCount; i++) {
			currentAspects.add(object.getRepeatingString(DctmAttributes.R_ASPECT_NAME, i));
		}

		// Next... which aspects does the incoming object have?
		Set<String> newAspects = new LinkedHashSet<>(this.cmfObject.getSecondarySubtypes());

		final AspectHelper helper = new AspectHelper(object);

		// Detach only those aspects we won't have to re-add later
		Set<String> oldAspects = new LinkedHashSet<>(currentAspects);
		oldAspects.removeAll(newAspects);
		helper.detachAspects(oldAspects);

		// Attach only those aspects we don't already have
		newAspects.removeAll(currentAspects);
		helper.attachAspects(newAspects);

		return helper.get();
	}

	protected final AttributeHandler getAttributeHandler(IDfAttr attr) {
		return DctmAttributeHandlers.getAttributeHandler(getDctmType(), attr);
	}

	protected final AttributeHandler getAttributeHandler(CmfAttribute<IDfValue> attr) {
		return DctmAttributeHandlers.getAttributeHandler(getDctmType(), attr);
	}

	protected final AttributeHandler getAttributeHandler(DctmDataType dataType, String name) {
		return DctmAttributeHandlers.getAttributeHandler(getDctmType(), dataType, name);
	}

	protected boolean isVersionable(T object) throws DfException {
		return false;
	}

	protected boolean skipImport(DctmImportContext ctx) throws DfException, ImportException {
		return false;
	}

	protected final boolean isAttributeEquals(final T existingObject, final String attributeName)
		throws DfException, ImportException {
		final CmfAttribute<IDfValue> thisAtt = this.cmfObject.getAttribute(attributeName);
		// We don't have this attribute but the existing object does...
		if ((thisAtt == null) && existingObject.hasAttr(attributeName)) { return false; }

		// Both have the attribute, let's get the definition
		final IDfAttr existingAtt = existingObject.getAttr(existingObject.findAttrIndex(attributeName));

		// Are they the same type?
		DctmDataType thisType = DctmTranslator.translateType(thisAtt.getType());
		DctmDataType existingType = DctmDataType.fromAttribute(existingAtt);
		if (thisType != existingType) { return false; }

		// Do the attribute counts mismatch?
		if (thisAtt.getValueCount() != existingObject.getValueCount(attributeName)) { return false; }

		// We don't check for repeating or not because at this point we don't much care since
		// we know they both have the same number of values
		for (int i = 0; i < thisAtt.getValueCount(); i++) {
			IDfValue thisValue = thisAtt.getValue(i);
			IDfValue existingValue = existingObject.getRepeatingValue(attributeName, i);

			if (!Tools.equals(thisType.getValue(thisValue), thisType.getValue(existingValue))) { return false; }
		}
		// At this point we know that all attribute values are equal and in the same order
		return true;
	}

	protected boolean isSameObject(T existingObject, DctmImportContext ctx) throws DfException, ImportException {
		// Same object type?
		return StringUtils.equals(this.cmfObject.getSubtype(), existingObject.getType().getName());
	}

	protected T newObject(DctmImportContext ctx) throws DfException, ImportException {
		IDfType type = DctmTranslator.translateType(ctx, this.cmfObject);
		if (type == null) {
			throw new ImportException(
				String.format("Unsupported type [%s::%s]", this.cmfObject.getType(), this.cmfObject.getSubtype()));
		}
		return castObject(ctx.getSession().newObject(type.getName()));
	}

	protected abstract T locateInCms(DctmImportContext context) throws ImportException, DfException;

	/**
	 * <p>
	 * Apply specific processing to the given object prior to the automatically-copied attributes
	 * having been applied to it. That means that the object may still have its old attributes, and
	 * thus only this instance's {@link CmfAttribute} values should be trusted. The
	 * {@code newObject} parameter indicates if this object was newly-created, or if it is an
	 * already-existing object.
	 * </p>
	 *
	 * @param object
	 * @throws DfException
	 */
	protected void prepareForConstruction(T object, boolean newObject, DctmImportContext context)
		throws DfException, ImportException {
	}

	/**
	 * <p>
	 * Apply specific processing to the given object after the automatically-copied attributes have
	 * been applied to it. That means that the object should now reflect its intended new state,
	 * pending only whatever changes this method will apply.
	 * </p>
	 *
	 * @param object
	 * @throws DfException
	 */
	protected void finalizeConstruction(T object, boolean newObject, DctmImportContext context)
		throws DfException, ImportException {
	}

	protected boolean postConstruction(T object, boolean newObject, DctmImportContext context)
		throws DfException, ImportException {
		return false;
	}

	protected boolean cleanupAfterSave(T object, boolean newObject, DctmImportContext context)
		throws DfException, ImportException {
		return false;
	}

	protected void updateReferenced(T object, DctmImportContext context) throws DfException, ImportException {
	}

	protected final boolean copyAttributeToObject(String attrName, T object) throws DfException {
		if (attrName == null) {
			throw new IllegalArgumentException("Must provide an attribute name to set on the object");
		}
		CmfAttribute<IDfValue> attribute = this.cmfObject.getAttribute(attrName);
		if (attribute == null) { return false; }
		return copyAttributeToObject(attribute, object);
	}

	protected final boolean copyAttributeToObject(CmfAttribute<IDfValue> attribute, T object) throws DfException {
		if (object == null) { throw new IllegalArgumentException("Must provide an object to set the attributes to"); }
		if (attribute == null) { throw new IllegalArgumentException("Must provide an attribute to set on the object"); }
		final AttributeHandler handler = getAttributeHandler(attribute);
		return setAttributeOnObject(attribute, handler.getImportableValues(object, attribute), object);
	}

	protected final boolean setAttributeOnObject(String attrName, IDfValue value, T object) throws DfException {
		return setAttributeOnObject(attrName, Collections.singleton(value), object);
	}

	protected final boolean setAttributeOnObject(String attrName, Collection<IDfValue> values, T object)
		throws DfException {
		if (object == null) { throw new IllegalArgumentException("Must provide an object to set the attributes to"); }
		if (attrName == null) {
			throw new IllegalArgumentException("Must provide an attribute name to set on the object");
		}
		CmfAttribute<IDfValue> dataAttr = this.cmfObject.getAttribute(attrName);
		if (dataAttr != null) { return setAttributeOnObject(dataAttr, values, object); }

		int idx = object.findAttrIndex(attrName);
		IDfAttr attribute = object.getAttr(idx);
		DctmDataType dataType = DctmDataType.fromAttribute(attribute);
		return setAttributeOnObject(attrName, dataType, attribute.isRepeating(), values, object);
	}

	protected final boolean setAttributeOnObject(CmfAttribute<IDfValue> attribute, IDfValue value, T object)
		throws DfException {
		return setAttributeOnObject(attribute, Collections.singleton(value), object);
	}

	protected final boolean setAttributeOnObject(CmfAttribute<IDfValue> attribute, Collection<IDfValue> values,
		T object) throws DfException {
		if (object == null) { throw new IllegalArgumentException("Must provide an object to set the attributes to"); }
		if (attribute == null) { throw new IllegalArgumentException("Must provide an attribute to set on the object"); }
		return setAttributeOnObject(attribute.getName(), DctmTranslator.translateType(attribute.getType()),
			attribute.isMultivalued(), values, object);
	}

	private final boolean setAttributeOnObject(final String attrName, final DctmDataType dataType,
		final boolean repeating, Collection<IDfValue> values, T object) throws DfException {
		if (object == null) { throw new IllegalArgumentException("Must provide an object to set the attributes to"); }
		if (attrName == null) {
			throw new IllegalArgumentException("Must provide an attribute name to set on the object");
		}
		if (!object.hasAttr(attrName)) { return false; }
		if (values == null) {
			values = Collections.emptyList();
		}
		// If an existing object is being updated, first clear repeating values if the attribute
		// being set is repeating type.
		clearAttributeFromObject(attrName, object);
		final int truncateLength = (dataType == DctmDataType.DF_STRING
			? object.getAttr(object.findAttrIndex(attrName)).getLength()
			: 0);
		int i = 0;
		for (IDfValue value : values) {
			if ((i > 0) && !repeating) {
				// Make sure we only take the first value
				break;
			}

			if (value == null) {
				value = dataType.getNull();
			}
			// Ensure the value's length is always consistent
			if ((truncateLength > 0) && (value.asString().length() > truncateLength)) {
				value = DfValueFactory.newStringValue(value.asString().substring(0, truncateLength));
			}
			boolean ok = false;
			try {
				if (repeating) {
					dataType.appendValue(value, object, attrName);
				} else {
					dataType.setValue(value, object, attrName);
				}
				i++;
				ok = true;
			} finally {
				// Here we EXPLICITLY don't catch the exception because we don't want to interfere
				// with the natural exception's lifecycle. We simply note that there has been
				// a problem, and move on.
				if (!ok) {
					String msg = null;
					String valueTypeLabel = null;
					try {
						valueTypeLabel = DctmDataType.fromDataType(value.getDataType()).name();
					} catch (IllegalArgumentException e) {
						valueTypeLabel = String.format("UNK-%d", value.getDataType());
					}
					if (repeating) {
						msg = String.format(
							"Failed to set the value in position %d for the repeating %s attribute [%s] to [%s](%s) on %s",
							i, dataType, attrName, value.asString(), valueTypeLabel, this.cmfObject.getDescription());
					} else {
						msg = String.format("Failed to set the value for the %s attribute [%s] to [%s](%s) on %s",
							dataType, attrName, value.asString(), valueTypeLabel, this.cmfObject.getDescription());
					}
					this.log.warn(msg);
				}
			}
		}
		return true;
	}

	protected final void clearAttributeFromObject(String attr, T object) throws DfException {
		if (object == null) {
			throw new IllegalArgumentException("Must provide an object to clear the attribute from");
		}
		CmfAttribute<IDfValue> dataAttr = this.cmfObject.getAttribute(attr);
		if (dataAttr != null) {
			clearAttributeFromObject(dataAttr, object);
		} else {
			clearAttributeFromObject(object.getAttr(object.findAttrIndex(attr)), object);
		}
	}

	protected final void clearAttributeFromObject(CmfAttribute<IDfValue> attribute, T object) throws DfException {
		if (attribute == null) {
			throw new IllegalArgumentException("Must provide an attribute to clear from the object");
		}
		clearAttributeFromObject(attribute.getName(), DctmTranslator.translateType(attribute.getType()),
			attribute.isMultivalued(), object);
	}

	protected final void clearAttributeFromObject(IDfAttr attribute, T object) throws DfException {
		if (attribute == null) {
			throw new IllegalArgumentException("Must provide an attribute to clear from the object");
		}
		clearAttributeFromObject(attribute.getName(), DctmDataType.fromAttribute(attribute), attribute.isRepeating(),
			object);
	}

	protected final void clearAttributeFromObject(String attrName, DctmDataType dataType, boolean repeating, T object)
		throws DfException {
		if (object == null) {
			throw new IllegalArgumentException("Must provide an object to clear the attribute from");
		}
		if (attrName == null) {
			throw new IllegalArgumentException("Must provide an attribute name to clear from object");
		}
		if (!object.hasAttr(attrName)) { return; }
		if (repeating) {
			object.removeAll(attrName);
		} else {
			if (dataType == null) {
				throw new IllegalArgumentException("Must provide the data type for the attribute being cleared");
			}
			try {
				object.setValue(attrName, dataType.getNull());
			} catch (DfAdminException e) {
				// If it's not the kind of thing we're defending against, then rethrow it
				if (!StringUtils.startsWithIgnoreCase(e.getMessageId(), "DM_SET_")) { throw e; }
				// This is raised when the attribute shouldn't be set (cleared) in this manner...
				// it's safe to ignore it
			}
		}
	}

	protected final boolean updateSystemAttributes(final IDfPersistentObject targetObject, DctmImportContext context)
		throws DfException, ImportException {
		if (targetObject == null) { throw new IllegalArgumentException("Must provide a target object to update"); }
		if (context == null) { throw new IllegalArgumentException("Must provide a context to update with"); }

		// Update the system attributes, if we can
		final String objectId = targetObject.getObjectId().getId();
		try {
			return updateSystemAttributes(this.cmfObject, targetObject, context);
		} catch (DfException e) {
			throw new ImportException(String.format("Failed to update the system attributes for %s object [%s](%s)",
				this.cmfObject.getType().name(), this.cmfObject.getLabel(), objectId), e);
		}
	}

	protected String generateSystemAttributesSQL(CmfObject<IDfValue> stored, IDfPersistentObject object,
		DctmImportContext ctx) throws DfException {

		final String objType = object.getType().getName();
		CmfAttribute<IDfValue> attribute = stored.getAttribute(DctmAttributes.R_MODIFY_DATE);
		if (attribute == null) { return null; }

		final IDfValue modifyDate = attribute.getValue();
		String sql = "" //
			+ "UPDATE %s_s SET " //
			+ "       r_modify_date = %s " //
			+ "       %s " //
			+ " WHERE r_object_id = %s";
		String vstampFlag = "";
		// TODO: For now we don't touch the i_vstamp b/c we don't think it necessary
		// (Setting.SKIP_VSTAMP.getBoolean() ? "" : String.format(", i_vstamp = %d",
		// dctmObj.getIntSingleAttrValue(DctmAttributes.I_VSTAMP)));

		return String.format(sql, objType,
			DfUtils.generateSqlDateClause(modifyDate.asTime().getDate(), object.getSession()), vstampFlag,
			DfUtils.quoteStringForSql(object.getObjectId().getId()));
	}

	/**
	 * Updates modify date attribute of an persistent object using execsql.
	 *
	 * @param object
	 *            the DFC persistentObject representing an object in repository
	 * @throws DfException
	 *             Signals that Dctm Server error has occurred.
	 */
	private boolean updateSystemAttributes(CmfObject<IDfValue> stored, IDfPersistentObject object,
		DctmImportContext ctx) throws DfException {
		final String sqlStr = generateSystemAttributesSQL(stored, object, ctx);
		if (sqlStr == null) { return true; }
		return runExecSQL(object.getSession(), sqlStr);
	}

	/**
	 * Updates vStamp attribute of an persistent object using execsql.
	 *
	 * @param obj
	 *            the DFC persistentObject representing an object in repository
	 * @throws DfException
	 *             Signals that Dctm Server error has occurred.
	 */
	protected final void updateVStamp(int vStamp, T obj) throws DfException {
		updateVStamp(String.format("%s_s", obj.getType().getName()), vStamp, obj);
	}

	/**
	 * Updates vStamp attribute of an persistent object using execsql.
	 *
	 * @param obj
	 *            the DFC persistentObject representing an object in repository
	 * @param tableName
	 *            the table name to be used in update clause
	 * @throws DfException
	 *             Signals that Dctm Server error has occurred.
	 */
	protected final void updateVStamp(String tableName, int vStamp, T obj) throws DfException {
		final String objId = obj.getObjectId().getId();
		runExecSQL(obj.getSession(),
			String.format("UPDATE %s SET i_vstamp = %d WHERE r_object_id = ''%s''", tableName, vStamp, objId));
	}

	/**
	 * Runs execsql query that can be used to update various system/internal attributes.
	 *
	 * @param session
	 *            the repository session
	 * @param sql
	 *            the sql query string
	 * @throws DfException
	 *             Signals that Dctm Server error has occurred.
	 */
	protected final boolean runExecSQL(IDfSession session, String sql) throws DfException {
		IDfCollection resultCol = DfUtils.executeQuery(session, String.format("EXECUTE exec_sql WITH query='%s'", sql),
			IDfQuery.DF_QUERY);
		boolean ok = false;
		try {
			if (resultCol.next()) {
				final IDfValue ret = resultCol.getValueAt(0);
				DfUtils.closeQuietly(resultCol);
				final String outcome;
				if (ret.toString().equalsIgnoreCase("F")) {
					ok = false;
					outcome = "rollback";
				} else {
					ok = true;
					outcome = "commit";
				}
				resultCol = DfUtils.executeQuery(session, String.format("EXECUTE exec_sql with query='%s';", outcome),
					IDfQuery.DF_QUERY);
			}
			return ok;
		} finally {
			DfUtils.closeQuietly(resultCol);
		}
	}

	@Override
	public String toString() {
		return String.format("CmsObject [type=%s, subtype=%s, dfClass=%s, id=%s, label=%s]", getDctmType(),
			this.cmfObject.getSubtype(), getObjectClass().getSimpleName(), this.cmfObject.getId(),
			this.cmfObject.getLabel());
	}
}