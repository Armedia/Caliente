/**
 *
 */

package com.armedia.cmf.engine.documentum.importer;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.lang3.StringUtils;

import com.armedia.cmf.engine.documentum.DctmAttributeHandlers;
import com.armedia.cmf.engine.documentum.DctmAttributeHandlers.AttributeHandler;
import com.armedia.cmf.engine.documentum.DctmAttributes;
import com.armedia.cmf.engine.documentum.DctmDataType;
import com.armedia.cmf.engine.documentum.DctmDelegateBase;
import com.armedia.cmf.engine.documentum.DctmObjectType;
import com.armedia.cmf.engine.documentum.DctmTranslator;
import com.armedia.cmf.engine.documentum.DfUtils;
import com.armedia.cmf.engine.documentum.DfValueFactory;
import com.armedia.cmf.engine.documentum.UnsupportedDctmObjectTypeException;
import com.armedia.cmf.engine.documentum.common.DctmSysObject;
import com.armedia.cmf.engine.importer.ImportException;
import com.armedia.cmf.engine.importer.ImportOutcome;
import com.armedia.cmf.engine.importer.ImportResult;
import com.armedia.cmf.storage.StorageException;
import com.armedia.cmf.storage.StoredAttribute;
import com.armedia.cmf.storage.StoredAttributeMapper.Mapping;
import com.armedia.cmf.storage.StoredObject;
import com.armedia.cmf.storage.StoredObjectHandler;
import com.armedia.cmf.storage.StoredProperty;
import com.armedia.commons.utilities.Tools;
import com.documentum.fc.client.IDfCollection;
import com.documentum.fc.client.IDfLocalTransaction;
import com.documentum.fc.client.IDfPersistentObject;
import com.documentum.fc.client.IDfQuery;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.client.IDfType;
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
public abstract class DctmImportDelegate<T extends IDfPersistentObject> extends DctmDelegateBase<T, DctmImportEngine> {

	private static final IDfValue CURRENT_VERSION_LABEL = DfValueFactory.newStringValue("CURRENT");
	public static final String NULL_BATCH_ID = "[NO BATCHING]";

	protected final StoredObject<IDfValue> storedObject;

	protected DctmImportDelegate(DctmImportEngine engine, DctmObjectType expectedType,
		StoredObject<IDfValue> storedObject) {
		super(engine, storedObject.getType());
		if (expectedType != getDctmType()) { throw new IllegalArgumentException(String.format(
			"This delegate is meant for [%s], but the given object is of type [%s] (%s)", expectedType, getDctmType(),
			storedObject.getType())); }
		this.storedObject = storedObject;
	}

	protected abstract String calculateLabel(T object) throws DfException, ImportException;

	protected String calculateBatchId(T object) throws DfException {
		// We use this trick to avoid requiring subclasses to implement this method, but also
		// because we've structured our code to ensure that it's only called for subclasses
		// that really should implement it
		throw new AbstractMethodError("calculateBatchId() must be overridden by subclasses that support batching");
	}

	protected final StoredAttribute<IDfValue> newStoredAttribute(IDfAttr attr, IDfValue... values) {
		if (values == null) {
			return newStoredAttribute(attr, (Collection<IDfValue>) null);
		} else {
			return newStoredAttribute(attr, Arrays.asList(values));
		}
	}

	protected final StoredAttribute<IDfValue> newStoredAttribute(IDfAttr attr, Collection<IDfValue> values) {
		return new StoredAttribute<IDfValue>(attr.getName(), DctmDataType.fromAttribute(attr).getStoredType(),
			attr.isRepeating(), values);
	}

	protected boolean isTransitoryObject(T object) throws DfException, ImportException {
		return false;
	}

	protected void prepareOperation(T object, boolean newObject) throws DfException, ImportException {
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

	public final ImportOutcome importObject(DctmImportContext context) throws DfException, ImportException {
		if (context == null) { throw new IllegalArgumentException("Must provide a context to save the object"); }
		final IDfSession session = context.getSession();
		boolean ok = false;
		final IDfLocalTransaction localTx;
		if (session.isTransactionActive()) {
			localTx = session.beginTransEx();
		} else {
			localTx = null;
			session.beginTrans();
		}
		try {
			ImportOutcome ret = doImportObject(context);
			this.log.debug(String.format("Committing the transaction for [%s](%s)", this.storedObject.getLabel(),
				this.storedObject.getId()));
			if (localTx != null) {
				session.commitTransEx(localTx);
			} else {
				session.commitTrans();
			}
			ok = true;
			return ret;
		} finally {
			if (!ok) {
				this.log.warn(String.format("Aborting the transaction for [%s](%s)", this.storedObject.getLabel(),
					this.storedObject.getId()));
				try {
					if (localTx != null) {
						session.abortTransEx(localTx);
					} else {
						session.abortTrans();
					}
				} catch (DfException e) {
					// We log this here and don't raise it because if we're aborting, it means
					// that there's another exception already bubbling up, so we don't want
					// to intercept that
					this.log.error(String.format("Failed to roll back the transaction for [%s](%s)",
						this.storedObject.getLabel(), this.storedObject.getId()), e);
				}
			}
		}
	}

	protected ImportOutcome doImportObject(DctmImportContext context) throws DfException, ImportException {
		if (context == null) { throw new IllegalArgumentException("Must provide a context to save the object"); }

		boolean ok = false;

		// We assume the worst, out of the gate
		final IDfSession session = context.getSession();
		T object = null;
		try {
			if (skipImport(context)) { return new ImportOutcome(ImportResult.SKIPPED); }

			object = locateInCms(context);
			final boolean isNew = (object == null);
			final boolean updateVersionLabels = isVersionable(object);
			final ImportResult cmsImportResult;
			String newLabel = null;
			if (isNew) {
				// Create a new object
				if (this.log.isDebugEnabled()) {
					this.log.debug(String.format("Creating a new object for [%s](%s)", this.storedObject.getLabel(),
						this.storedObject.getId()));
				}
				object = newObject(context);
				cmsImportResult = ImportResult.CREATED;
				if (!isTransitoryObject(object)) {
					// DO NOT override mappings...we don't know the new object's ID until checkin is
					// completed
					context.getAttributeMapper().setMapping(getDctmType().getStoredObjectType(),
						DctmAttributes.R_OBJECT_ID, this.storedObject.getId(), object.getObjectId().getId());
				}
			} else {
				// Is this correct?
				newLabel = calculateLabel(object);
				this.log.info(String.format("Acquiring lock on %s [%s](%s)", getDctmType().name(),
					this.storedObject.getLabel(), this.storedObject.getId()));
				DfUtils.lockObject(this.log, object);
				object.fetch(null);
				this.log.info(String.format("Acquired lock on %s [%s](%s)", getDctmType().name(),
					this.storedObject.getLabel(), this.storedObject.getId()));
				context.getAttributeMapper().setMapping(getDctmType().getStoredObjectType(),
					DctmAttributes.R_OBJECT_ID, this.storedObject.getId(), object.getObjectId().getId());

				if (isSameObject(object)) {
					ok = true;
					return new ImportOutcome(ImportResult.DUPLICATE, newLabel, object.getObjectId().getId());
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
				this.log.info(String.format("Completed saving %s to CMS with result [%s] for [%s](%s)->[%s](%s)",
					getDctmType(), cmsImportResult, this.storedObject.getLabel(), this.storedObject.getId(), newLabel,
					object.getObjectId().getId()));

				return new ImportOutcome(cmsImportResult, newLabel, object.getObjectId().getId());
			}

			prepareOperation(object, isNew);
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

			// We remove the version labels as well
			// Set "default" attributes
			for (StoredAttribute<IDfValue> attribute : this.storedObject.getAttributes()) {
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

			if (updateVersionLabels) {
				StoredAttribute<IDfValue> versionLabel = this.storedObject.getAttribute(DctmAttributes.R_VERSION_LABEL);
				StoredProperty<IDfValue> current = this.storedObject.getProperty(DctmSysObject.CURRENT_VERSION);
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
			this.log.info(String.format("Completed saving %s to CMS with result [%s] for [%s](%s)->[%s](%s)",
				getDctmType(), cmsImportResult, this.storedObject.getLabel(), this.storedObject.getId(), newLabel,
				object.getObjectId().getId()));

			ImportOutcome ret = new ImportOutcome(cmsImportResult, object.getObjectId().getId(), newLabel);
			ok = true;
			return ret;
		} finally {
			if (ok) {
				try {
					finalizeOperation(object);
				} catch (DfException e) {
					ok = false;
					this.log
						.error(
							String
								.format(
									"Caught an exception while trying to finalize the import for [%s](%s) - aborting the transaction",
									this.storedObject.getLabel(), this.storedObject.getId()), e);
				}
				// This has to be the last thing that happens, else some of the attributes won't
				// take. There is no need to save() the object for this, as this is a direct
				// modification
				if (this.log.isTraceEnabled()) {
					this.log.trace(String.format("Updating the system attributes for [%s](%s)",
						this.storedObject.getLabel(), this.storedObject.getId()));
				}

				if (!updateSystemAttributes(this.storedObject, object, context)) {
					this.log.warn(String.format("Failed to update the system attributes for [%s](%s)",
						this.storedObject.getLabel(), this.storedObject.getId()));
				}
			} else {
				// Clear the mapping
				context.getAttributeMapper().clearSourceMapping(getDctmType().getStoredObjectType(),
					DctmAttributes.R_OBJECT_ID, this.storedObject.getId());
			}
		}
	}

	protected final AttributeHandler getAttributeHandler(IDfAttr attr) {
		return DctmAttributeHandlers.getAttributeHandler(getDctmType(), attr);
	}

	protected final AttributeHandler getAttributeHandler(StoredAttribute<IDfValue> attr) {
		return DctmAttributeHandlers.getAttributeHandler(getDctmType(), attr);
	}

	protected final AttributeHandler getAttributeHandler(DctmDataType dataType, String name) {
		return DctmAttributeHandlers.getAttributeHandler(getDctmType(), dataType, name);
	}

	protected boolean isVersionable(T object) throws DfException {
		return false;
	}

	protected boolean skipImport(DctmImportContext ctx) throws DfException {
		return false;
	}

	protected boolean isSameObject(T object) throws DfException {
		StoredAttribute<IDfValue> thisDate = this.storedObject.getAttribute(DctmAttributes.R_MODIFY_DATE);
		if (thisDate == null) { return false; }
		IDfValue objectDate = object.getValue(DctmAttributes.R_MODIFY_DATE);
		if (objectDate == null) { return false; }
		DctmDataType type = DctmTranslator.translateType(thisDate.getType());
		return Tools.equals(type.getValue(objectDate), type.getValue(thisDate.getValue()));
	}

	protected T newObject(DctmImportContext ctx) throws DfException, ImportException {
		IDfType type = DctmTranslator.translateType(ctx.getSession(), this.storedObject);
		if (type == null) { throw new ImportException(String.format("Unsupported type [%s::%s]",
			this.storedObject.getType(), this.storedObject.getSubtype())); }
		return castObject(ctx.getSession().newObject(type.getName()));
	}

	protected abstract T locateInCms(DctmImportContext context) throws ImportException, DfException;

	/**
	 * <p>
	 * Apply specific processing to the given object prior to the automatically-copied attributes
	 * having been applied to it. That means that the object may still have its old attributes, and
	 * thus only this instance's {@link StoredAttribute} values should be trusted. The
	 * {@code newObject} parameter indicates if this object was newly-created, or if it is an
	 * already-existing object.
	 * </p>
	 *
	 * @param object
	 * @throws DfException
	 */
	protected void prepareForConstruction(T object, boolean newObject, DctmImportContext context) throws DfException,
		ImportException {
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
	protected void finalizeConstruction(T object, boolean newObject, DctmImportContext context) throws DfException,
		ImportException {
	}

	protected boolean postConstruction(T object, boolean newObject, DctmImportContext context) throws DfException,
		ImportException {
		return false;
	}

	protected boolean cleanupAfterSave(T object, boolean newObject, DctmImportContext context) throws DfException,
		ImportException {
		return false;
	}

	protected void updateReferenced(T object, DctmImportContext context) throws DfException, ImportException {
	}

	protected final boolean copyAttributeToObject(String attrName, T object) throws DfException {
		if (attrName == null) { throw new IllegalArgumentException(
			"Must provide an attribute name to set on the object"); }
		StoredAttribute<IDfValue> attribute = this.storedObject.getAttribute(attrName);
		if (attribute == null) { return false; }
		return copyAttributeToObject(attribute, object);
	}

	protected final boolean copyAttributeToObject(StoredAttribute<IDfValue> attribute, T object) throws DfException {
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
		if (attrName == null) { throw new IllegalArgumentException(
			"Must provide an attribute name to set on the object"); }
		StoredAttribute<IDfValue> dataAttr = this.storedObject.getAttribute(attrName);
		if (dataAttr != null) { return setAttributeOnObject(dataAttr, values, object); }

		int idx = object.findAttrIndex(attrName);
		IDfAttr attribute = object.getAttr(idx);
		DctmDataType dataType = DctmDataType.fromAttribute(attribute);
		return setAttributeOnObject(attrName, dataType, attribute.isRepeating(), values, object);
	}

	protected final boolean setAttributeOnObject(StoredAttribute<IDfValue> attribute, IDfValue value, T object)
		throws DfException {
		return setAttributeOnObject(attribute, Collections.singleton(value), object);
	}

	protected final boolean setAttributeOnObject(StoredAttribute<IDfValue> attribute, Collection<IDfValue> values,
		T object) throws DfException {
		if (object == null) { throw new IllegalArgumentException("Must provide an object to set the attributes to"); }
		if (attribute == null) { throw new IllegalArgumentException("Must provide an attribute to set on the object"); }
		return setAttributeOnObject(attribute.getName(), DctmTranslator.translateType(attribute.getType()),
			attribute.isRepeating(), values, object);
	}

	private final boolean setAttributeOnObject(String attrName, DctmDataType dataType, boolean repeating,
		Collection<IDfValue> values, T object) throws DfException {
		if (object == null) { throw new IllegalArgumentException("Must provide an object to set the attributes to"); }
		if (attrName == null) { throw new IllegalArgumentException(
			"Must provide an attribute name to set on the object"); }
		if (!object.hasAttr(attrName)) { return false; }
		if (values == null) {
			values = Collections.emptyList();
		}
		// If an existing object is being updated, first clear repeating values if the attribute
		// being set is repeating type.
		clearAttributeFromObject(attrName, object);
		final int truncateLength = (dataType == DctmDataType.DF_STRING ? object.getAttr(object.findAttrIndex(attrName))
			.getLength() : 0);
		int i = 0;
		for (IDfValue value : values) {
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
					object.appendValue(attrName, value);
				} else {
					// I wonder if appendValue() can also be used for non-repeating attributes...
					object.setValue(attrName, value);
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
						msg = String
							.format(
								"Failed to set the value in position %d for the repeating %s attribute [%s] to [%s](%s) on %s [%s](%s)",
								i, dataType, attrName, value.asString(), valueTypeLabel, this.storedObject.getType(),
								this.storedObject.getLabel(), this.storedObject.getId());
					} else {
						msg = String.format(
							"Failed to set the value for the %s attribute [%s] to [%s](%s) on %s [%s](%s)", dataType,
							attrName, value.asString(), valueTypeLabel, this.storedObject.getType(),
							this.storedObject.getLabel(), this.storedObject.getId());
					}
					this.log.warn(msg);
				}
			}
		}
		return true;
	}

	protected final void clearAttributeFromObject(String attr, T object) throws DfException {
		if (object == null) { throw new IllegalArgumentException("Must provide an object to clear the attribute from"); }
		StoredAttribute<IDfValue> dataAttr = this.storedObject.getAttribute(attr);
		if (dataAttr != null) {
			clearAttributeFromObject(dataAttr, object);
		} else {
			clearAttributeFromObject(object.getAttr(object.findAttrIndex(attr)), object);
		}
	}

	protected final void clearAttributeFromObject(StoredAttribute<IDfValue> attribute, T object) throws DfException {
		if (attribute == null) { throw new IllegalArgumentException(
			"Must provide an attribute to clear from the object"); }
		clearAttributeFromObject(attribute.getName(), DctmTranslator.translateType(attribute.getType()),
			attribute.isRepeating(), object);
	}

	protected final void clearAttributeFromObject(IDfAttr attribute, T object) throws DfException {
		if (attribute == null) { throw new IllegalArgumentException(
			"Must provide an attribute to clear from the object"); }
		clearAttributeFromObject(attribute.getName(), DctmDataType.fromAttribute(attribute), attribute.isRepeating(),
			object);
	}

	protected final void clearAttributeFromObject(String attrName, DctmDataType dataType, boolean repeating, T object)
		throws DfException {
		if (object == null) { throw new IllegalArgumentException("Must provide an object to clear the attribute from"); }
		if (attrName == null) { throw new IllegalArgumentException(
			"Must provide an attribute name to clear from object"); }
		if (!object.hasAttr(attrName)) { return; }
		if (repeating) {
			object.removeAll(attrName);
		} else {
			if (dataType == null) { throw new IllegalArgumentException(
				"Must provide the data type for the attribute being cleared"); }
			try {
				object.setValue(attrName, dataType.getNull());
			} catch (DfAdminException e) {
				// If it's not the kind of thing we're defending against, then rethrow it
				if (!e.getMessageId().startsWith("DM_SET_")) { throw e; }
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
		final DctmObjectType targetType;
		try {
			targetType = DctmObjectType.decodeType(targetObject);
		} catch (UnsupportedDctmObjectTypeException e) {
			throw new ImportException(String.format("Failed to decode the object type for [%s]", targetObject.getType()
				.getName()), e);
		}
		Mapping m = context.getAttributeMapper().getSourceMapping(targetType.getStoredObjectType(),
			DctmAttributes.R_OBJECT_ID, objectId);
		if (m == null) { return false; }

		Set<String> s = Collections.singleton(m.getSourceValue());
		final AtomicReference<StoredObject<IDfValue>> loaded = new AtomicReference<StoredObject<IDfValue>>(null);
		final StoredObjectHandler<IDfValue> handler = new StoredObjectHandler<IDfValue>() {

			@Override
			public boolean newBatch(String batchId) throws StorageException {
				return true;
			}

			@Override
			public boolean handleObject(StoredObject<IDfValue> dataObject) throws StorageException {
				loaded.set(dataObject);
				return false;
			}

			@Override
			public boolean closeBatch(boolean ok) throws StorageException {
				return true;
			}

			@Override
			public boolean handleException(SQLException e) {
				return true;
			}
		};
		try {
			context.loadObjects(targetType.getStoredObjectType(), s, handler);
		} catch (Exception e) {
			throw new ImportException(String.format("Exception caught attempting to load %s [%s](%s)",
				targetType.getStoredObjectType(), this.storedObject.getLabel(), this.storedObject.getId()), e);
		}
		if (loaded.get() == null) { return false; }
		try {
			return updateSystemAttributes(loaded.get(), targetObject, context);
		} catch (DfException e) {
			throw new ImportException(String.format("Failed to update the system attributes for %s object [%s](%s)",
				this.storedObject.getType().name(), this.storedObject.getLabel(), objectId), e);
		}
	}

	protected String generateSystemAttributesSQL(StoredObject<IDfValue> stored, IDfPersistentObject object,
		DctmImportContext ctx) throws DfException {

		final String objType = object.getType().getName();
		StoredAttribute<IDfValue> attribute = stored.getAttribute(DctmAttributes.R_MODIFY_DATE);
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
			DfUtils.sqlQuoteString(object.getObjectId().getId()));
	}

	/**
	 * Updates modify date attribute of an persistent object using execsql.
	 *
	 * @param object
	 *            the DFC persistentObject representing an object in repository
	 * @throws DfException
	 *             Signals that Dctm Server error has occurred.
	 */
	private boolean updateSystemAttributes(StoredObject<IDfValue> stored, IDfPersistentObject object,
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
			this.storedObject.getSubtype(), getDfClass().getSimpleName(), this.storedObject.getId(),
			this.storedObject.getLabel());
	}
}