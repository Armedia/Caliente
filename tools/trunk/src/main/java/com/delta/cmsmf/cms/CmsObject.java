/**
 *
 */

package com.delta.cmsmf.cms;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.log4j.Logger;

import com.armedia.commons.utilities.Tools;
import com.delta.cmsmf.cms.CmsAttributeHandlers.AttributeHandler;
import com.delta.cmsmf.cms.CmsAttributeMapper.Mapping;
import com.delta.cmsmf.cms.storage.CmsObjectStore.ObjectHandler;
import com.delta.cmsmf.exception.CMSMFException;
import com.delta.cmsmf.utils.DfUtils;
import com.documentum.fc.client.IDfCollection;
import com.documentum.fc.client.IDfLocalTransaction;
import com.documentum.fc.client.IDfPersistentObject;
import com.documentum.fc.client.IDfQuery;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.common.DfException;
import com.documentum.fc.common.IDfAttr;
import com.documentum.fc.common.IDfId;
import com.documentum.fc.common.IDfValue;

/**
 * @author Diego Rivera &lt;diego.rivera@armedia.com&gt;
 *
 */
/**
 * @author Diego Rivera &lt;diego.rivera@armedia.com&gt;
 *
 * @param <T>
 */
public abstract class CmsObject<T extends IDfPersistentObject> {

	public static final String NULL_BATCH_ID = "[NO BATCHING]";

	public static final class SaveResult {
		private final CmsImportResult cmsImportResult;
		private final String objectLabel;
		private final String objectId;

		private SaveResult(CmsImportResult cmsImportResult, String objectLabel, String objectId) {
			this.cmsImportResult = cmsImportResult;
			this.objectLabel = objectLabel;
			this.objectId = objectId;
		}

		public CmsImportResult getResult() {
			return this.cmsImportResult;
		}

		public String getObjectLabel() {
			return this.objectLabel;
		}

		public String getObjectId() {
			return this.objectId;
		}

		@Override
		public int hashCode() {
			return Tools.hashTool(this, null, this.cmsImportResult, this.objectLabel, this.objectId);
		}

		@Override
		public boolean equals(Object obj) {
			if (!Tools.baseEquals(this, obj)) { return false; }
			SaveResult other = SaveResult.class.cast(obj);
			if (!Tools.equals(this.cmsImportResult, other.cmsImportResult)) { return false; }
			if (!Tools.equals(this.objectLabel, other.objectLabel)) { return false; }
			if (!Tools.equals(this.objectId, other.objectId)) { return false; }
			return true;
		}

		@Override
		public String toString() {
			return String.format("SaveResult [cmsImportResult=%s, objectLabel=%s, objectId=%s]", this.cmsImportResult,
				this.objectLabel, this.objectId);
		}
	}

	protected final Logger log = Logger.getLogger(getClass());

	private final CmsObjectType type;
	private final Class<T> dfClass;

	private String id = null;
	private String batchId = null;
	private String label = null;
	private String subtype = null;
	private final Map<String, CmsAttribute> attributes = new HashMap<String, CmsAttribute>();
	private final Map<String, CmsProperty> properties = new HashMap<String, CmsProperty>();

	protected CmsObject(Class<T> dfClass) {
		if (dfClass == null) { throw new IllegalArgumentException("Must provde a DF class"); }
		this.type = CmsObjectType.decodeFromClass(getClass());
		if (this.type.getDfClass() != dfClass) { throw new IllegalArgumentException(String.format(
			"Class mismatch: type is tied to class [%s], but was given class [%s]", this.type.getDfClass()
			.getCanonicalName(), dfClass.getCanonicalName())); }
		this.dfClass = dfClass;
	}

	public final void load(ResultSet rs) throws SQLException {
		this.id = rs.getString("object_id");
		this.batchId = rs.getString("batch_id");
		this.label = rs.getString("object_label");
		this.subtype = rs.getString("object_subtype");
		this.attributes.clear();
		this.properties.clear();
	}

	public final void loadAttributes(ResultSet rs) throws SQLException {
		boolean ok = false;
		try {
			this.attributes.clear();
			while (rs.next()) {
				CmsAttribute attribute = new CmsAttribute(rs);
				this.attributes.put(attribute.getName(), attribute);
			}
			ok = true;
		} finally {
			if (!ok) {
				this.attributes.clear();
			}
		}
	}

	public final void loadProperties(ResultSet rs) throws SQLException {
		boolean ok = false;
		try {
			this.properties.clear();
			while (rs.next()) {
				CmsProperty property = new CmsProperty(rs);
				this.properties.put(property.getName(), property);
			}
			ok = true;
		} finally {
			if (!ok) {
				this.properties.clear();
			}
		}
	}

	public void loadCompleted() throws CMSMFException {
	}

	public final CmsObjectType getType() {
		return this.type;
	}

	public final String getSubtype() {
		return this.subtype;
	}

	public final String getId() {
		return this.id;
	}

	public final String getBatchId() {
		return this.batchId;
	}

	public final String getLabel() {
		return this.label;
	}

	public final Class<T> getDfClass() {
		return this.dfClass;
	}

	public final int getAttributeCount() {
		return this.attributes.size();
	}

	public final Set<String> getAttributeNames() {
		return Collections.unmodifiableSet(this.attributes.keySet());
	}

	public final CmsAttribute getAttribute(String name) {
		if (name == null) { throw new IllegalArgumentException("Must provide an attribute name to retrieve"); }
		return this.attributes.get(name);
	}

	public final CmsAttribute setAttribute(CmsAttribute attribute) {
		if (attribute == null) { throw new IllegalArgumentException("Must provide an attribute to set"); }
		return this.attributes.put(attribute.getName(), attribute);
	}

	public final CmsAttribute removeAttribute(String name) {
		if (name == null) { throw new IllegalArgumentException("Must provide an attribute name to remove"); }
		return this.attributes.remove(name);
	}

	public final Collection<CmsAttribute> getAllAttributes() {
		return Collections.unmodifiableCollection(this.attributes.values());
	}

	public final int getPropertyCount() {
		return this.properties.size();
	}

	public final Set<String> getPropertyNames() {
		return Collections.unmodifiableSet(this.properties.keySet());
	}

	public final CmsProperty getProperty(String name) {
		if (name == null) { throw new IllegalArgumentException("Must provide a property name to retrieve"); }
		return this.properties.get(name);
	}

	public final CmsProperty setProperty(CmsProperty property) {
		if (property == null) { throw new IllegalArgumentException("Must provide a property to set"); }
		return this.properties.put(property.getName(), property);
	}

	public final CmsProperty removeProperty(String name) {
		if (name == null) { throw new IllegalArgumentException("Must provide a property name to remove"); }
		return this.properties.remove(name);
	}

	public final Collection<CmsProperty> getAllProperties() {
		return Collections.unmodifiableCollection(this.properties.values());
	}

	/**
	 * <p>
	 * Validate that the object should continue to be loaded by
	 * {@link #loadFromCMS(IDfPersistentObject, CmsTransferContext)}, or not.
	 * </p>
	 *
	 * @param object
	 * @return {@code true} if the object should continue to be loaded, {@code false} otherwise.
	 * @throws DfException
	 */
	protected boolean isValidForLoad(T object) throws DfException {
		return true;
	}

	protected abstract String calculateLabel(T object) throws DfException, CMSMFException;

	protected String calculateBatchId(T object) throws DfException {
		// We use this trick to avoid requiring subclasses to implement this method, but also
		// because we've structured our code to ensure that it's only called for subclasses
		// that really should implement it
		throw new AbstractMethodError("calculateBatchId() must be overridden by subclasses that support batching");
	}

	/**
	 * <p>
	 * Loads the object's attributes and properties from the given CMS object, and returns
	 * {@code true} if the load was successful or {@code false} if the object should not be
	 * processed at all.
	 * </p>
	 *
	 * @param object
	 * @return {@code true} if the load was successful or {@code false} if the object should not be
	 *         processed at all
	 * @throws DfException
	 * @throws CMSMFException
	 */
	public final boolean loadFromCMS(IDfPersistentObject object, CmsTransferContext ctx) throws DfException,
	CMSMFException {
		if (object == null) { throw new IllegalArgumentException("Must provide an object to populate from"); }
		if (ctx == null) { throw new IllegalArgumentException("Must provide transfer context"); }
		final T typedObject = castObject(object);
		final CmsObjectType type;
		try {
			type = CmsObjectType.decodeType(object);
		} catch (UnsupportedObjectTypeException e) {
			throw new CMSMFException("Unsupported object type", e);
		}
		if (type != this.type) { throw new IllegalArgumentException(String.format(
			"Expected an object of type %s, but got one of type %s", this.type, type)); }

		if (!isValidForLoad(typedObject)) {
			// This particular object isn't supported
			return false;
		}

		this.id = object.getObjectId().getId();
		this.subtype = object.getType().getName();
		this.label = calculateLabel(typedObject);
		this.batchId = (type.isBatchingSupported() ? calculateBatchId(typedObject) : CmsObject.NULL_BATCH_ID);

		// First, the attributes
		this.attributes.clear();
		final int attCount = object.getAttrCount();
		for (int i = 0; i < attCount; i++) {
			final IDfAttr attr = object.getAttr(i);
			final String name = attr.getName();
			final AttributeHandler handler = CmsAttributeHandlers.getAttributeHandler(type, attr);
			// Get the attribute handler
			if (handler.includeInExport(object, attr)) {
				CmsAttribute attribute = new CmsAttribute(attr, handler.getExportableValues(object, attr));
				this.attributes.put(name, attribute);
			}
		}

		// Properties are different from attributes in that they require special handling. For
		// instance, a property would only be settable via direct SQL, or via an explicit method
		// call, etc., because setting it directly as an attribute would cmsImportResult in an error
		// from DFC, and therefore specialized code is required to handle it
		this.properties.clear();
		List<CmsProperty> properties = new ArrayList<CmsProperty>();
		getDataProperties(properties, typedObject, ctx);
		for (CmsProperty property : properties) {
			// This mechanism overwrites properties, and intentionally so
			this.properties.put(property.getName(), property);
		}
		return true;
	}

	public final void persistRequirements(IDfPersistentObject object, CmsTransferContext ctx,
		CmsDependencyManager dependencyManager) throws DfException, CMSMFException {
		if (object == null) { throw new IllegalArgumentException(
			"Must provide the Documentum object from which to identify the requirements"); }
		if (ctx == null) { throw new IllegalArgumentException("Must provide a context"); }
		if (dependencyManager == null) { throw new IllegalArgumentException("Must provide a dependency manager"); }
		doPersistRequirements(castObject(object), ctx, dependencyManager);
	}

	protected void doPersistRequirements(T object, CmsTransferContext ctx, CmsDependencyManager dependencyManager)
		throws DfException, CMSMFException {
	}

	public final void persistDependents(IDfPersistentObject object, CmsTransferContext ctx,
		CmsDependencyManager dependencyManager) throws DfException, CMSMFException {
		if (object == null) { throw new IllegalArgumentException(
			"Must provide the Documentum object from which to identify the dependencies"); }
		if (ctx == null) { throw new IllegalArgumentException("Must provide a context"); }
		if (dependencyManager == null) { throw new IllegalArgumentException("Must provide a dependency manager"); }
		doPersistDependents(castObject(object), ctx, dependencyManager);
	}

	protected void doPersistDependents(T object, CmsTransferContext ctx, CmsDependencyManager dependencyManager)
		throws DfException, CMSMFException {
	}

	protected boolean isTransitoryObject(T object) throws DfException, CMSMFException {
		return false;
	}

	protected void prepareOperation(T object, boolean newObject) throws DfException, CMSMFException {
	}

	protected IDfId persistChanges(T object, CmsTransferContext context) throws DfException, CMSMFException {
		IDfId newId = object.getObjectId();
		object.save();
		return newId;
	}

	protected void finalizeOperation(T sysObject) throws DfException, CMSMFException {
	}

	protected boolean isShortConstructionCycle() {
		return false;
	}

	public final SaveResult saveToCMS(CmsTransferContext context) throws DfException, CMSMFException {
		if (context == null) { throw new IllegalArgumentException("Must provide a context to save the object"); }

		boolean transOpen = false;
		boolean ok = false;

		// We assume the worst, out of the gate
		IDfLocalTransaction localTx = null;
		final IDfSession session = context.getSession();
		T object = null;
		try {
			if (session.isTransactionActive()) {
				localTx = session.beginTransEx();
			} else {
				session.beginTrans();
			}
			transOpen = true;
			if (skipImport(context)) { return new SaveResult(CmsImportResult.SKIPPED, null, null); }

			object = locateInCms(context);
			final boolean isNew = (object == null);
			final boolean updateVersionLabels = isVersionable(object);
			final CmsImportResult cmsImportResult;
			String newLabel = null;
			if (isNew) {
				// Create a new object
				if (this.log.isDebugEnabled()) {
					this.log.debug(String.format("Creating a new object for [%s](%s)", getLabel(), getId()));
				}
				object = newObject(context);
				cmsImportResult = CmsImportResult.CREATED;
				if (!isTransitoryObject(object)) {
					// DO NOT override mappings...we don't know the new object's ID until checkin is
					// completed
					context.getAttributeMapper().setMapping(this.type, CmsAttributes.R_OBJECT_ID, this.id,
						object.getObjectId().getId());
				}
			} else {
				// Is this correct?
				newLabel = calculateLabel(object);
				this.log.info(String.format("Acquiring lock on %s [%s](%s)", this.type.name(), this.label, this.id));
				object.lock();
				object.fetch(null);
				this.log.info(String.format("Acquired lock on %s [%s](%s)", this.type.name(), this.label, this.id));
				context.getAttributeMapper().setMapping(this.type, CmsAttributes.R_OBJECT_ID, this.id,
					object.getObjectId().getId());

				if (isSameObject(object)) {
					ok = true;
					return new SaveResult(CmsImportResult.DUPLICATE, newLabel, object.getObjectId().getId());
				}
				cmsImportResult = CmsImportResult.UPDATED;
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
					this.type, cmsImportResult, this.label, this.id, newLabel, object.getObjectId().getId()));

				return new SaveResult(cmsImportResult, newLabel, object.getObjectId().getId());
			}

			prepareOperation(object, isNew);
			prepareForConstruction(object, isNew, context);

			if (!isNew) {
				// If an existing object is being updated, clear out all of its attributes that are
				// not part of our attribute set
				// NOTE Only clear non internal and non system attributes
				Set<String> attributesBeingUpdated = getAttributeNames();
				final int attributeCount = object.getAttrCount();
				for (int i = 0; i < attributeCount; i++) {
					final IDfAttr attr = object.getAttr(i);
					final String name = attr.getName();
					if (!name.startsWith("r_") && !name.startsWith("i_") && !attributesBeingUpdated.contains(name)) {
						clearAttributeFromObject(name, object);
					}
				}
			}

			// We remove the version labels as well
			if (updateVersionLabels) {
				object.removeAll(CmsAttributes.R_VERSION_LABEL);
			}

			// Set "default" attributes
			for (CmsAttribute attribute : getAllAttributes()) {
				// TODO check to see if we need to set any internal or system attributes of various
				// types
				final String name = attribute.getName();
				final AttributeHandler handler = getAttributeHandler(attribute);

				// for now ignore setting internal and system attributes
				boolean doSet = (!name.startsWith("r_") && !name.startsWith("i_"));
				// but process r_version_lebel
				doSet |= (name.equals(CmsAttributes.R_VERSION_LABEL) && updateVersionLabels);
				// allow for a last-minute interception...
				doSet &= handler.includeInImport(object, attribute);

				if (doSet) {
					copyAttributeToObject(attribute, object);
				}
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
			ok = true;
			this.log.info(String.format("Completed saving %s to CMS with result [%s] for [%s](%s)->[%s](%s)",
				this.type, cmsImportResult, this.label, this.id, newLabel, object.getObjectId().getId()));

			return new SaveResult(cmsImportResult, newLabel, object.getObjectId().getId());
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
							this.label, this.id), e);
				}
			}
			if (transOpen) {
				if (ok) {
					// This has to be the last thing that happens, else some of the attributes won't
					// take. There is no need to save() the object for this, as this is a direct
					// modification
					if (this.log.isTraceEnabled()) {
						this.log.trace(String
							.format("Updating the system attributes for [%s](%s)", this.label, this.id));
					}

					if (!updateSystemAttributes(object)) {
						this.log.warn(String.format("Failed to update the system attributes for [%s](%s)", this.label,
							this.id));
					}
					this.log.info(String.format("Committing the transaction for [%s](%s)", this.label, this.id));
					if (localTx != null) {
						session.commitTransEx(localTx);
					} else {
						session.commitTrans();
					}
				} else {
					this.log.warn(String.format("Aborting the transaction for [%s](%s)", this.label, this.id));
					// Clear the mapping
					context.getAttributeMapper().clearSourceMapping(this.type, CmsAttributes.R_OBJECT_ID, this.id);
					if (localTx != null) {
						session.abortTransEx(localTx);
					} else {
						session.abortTrans();
					}
				}
			}
		}
	}

	protected final AttributeHandler getAttributeHandler(IDfAttr attr) {
		return CmsAttributeHandlers.getAttributeHandler(this.type, attr);
	}

	protected final AttributeHandler getAttributeHandler(CmsAttribute attr) {
		return CmsAttributeHandlers.getAttributeHandler(this.type, attr);
	}

	protected final AttributeHandler getAttributeHandler(CmsDataType dataType, String name) {
		return CmsAttributeHandlers.getAttributeHandler(this.type, dataType, name);
	}

	protected boolean isVersionable(T object) throws DfException {
		return false;
	}

	protected boolean skipImport(CmsTransferContext ctx) throws DfException {
		return false;
	}

	protected boolean isSameObject(T object) throws DfException {
		CmsAttribute thisDate = getAttribute(CmsAttributes.R_MODIFY_DATE);
		if (thisDate == null) { return false; }
		IDfValue objectDate = object.getValue(CmsAttributes.R_MODIFY_DATE);
		if (objectDate == null) { return false; }
		CmsDataType type = thisDate.getType();
		return Tools.equals(type.getValue(objectDate), type.getValue(thisDate.getValue()));
	}

	protected final T castObject(IDfPersistentObject object) throws DfException {
		if (object == null) { return null; }
		if (!this.dfClass.isAssignableFrom(object.getClass())) { throw new DfException(String.format(
			"Expected an object of class %s, but got one of class %s", this.dfClass.getCanonicalName(), object
			.getClass().getCanonicalName())); }
		return this.dfClass.cast(object);
	}

	protected T newObject(CmsTransferContext ctx) throws DfException, CMSMFException {
		return castObject(ctx.getSession().newObject(this.subtype));
	}

	protected abstract T locateInCms(CmsTransferContext context) throws CMSMFException, DfException;

	protected void getDataProperties(Collection<CmsProperty> properties, T object, CmsTransferContext ctx)
		throws DfException, CMSMFException {
	}

	/**
	 * <p>
	 * Apply specific processing to the given object prior to the automatically-copied attributes
	 * having been applied to it. That means that the object may still have its old attributes, and
	 * thus only this instance's {@link CmsAttribute} values should be trusted. The
	 * {@code newObject} parameter indicates if this object was newly-created, or if it is an
	 * already-existing object.
	 * </p>
	 *
	 * @param object
	 * @throws DfException
	 */
	protected void prepareForConstruction(T object, boolean newObject, CmsTransferContext context) throws DfException,
	CMSMFException {
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
	protected void finalizeConstruction(T object, boolean newObject, CmsTransferContext context) throws DfException,
	CMSMFException {
	}

	protected boolean postConstruction(T object, boolean newObject, CmsTransferContext context) throws DfException,
	CMSMFException {
		return false;
	}

	protected boolean cleanupAfterSave(T object, boolean newObject, CmsTransferContext context) throws DfException,
	CMSMFException {
		return false;
	}

	protected void updateReferenced(T object, CmsTransferContext context) throws DfException, CMSMFException {
	}

	protected final boolean copyAttributeToObject(String attrName, T object) throws DfException {
		return copyAttributeToObject(getAttribute(attrName), object);
	}

	protected final boolean copyAttributeToObject(CmsAttribute attribute, T object) throws DfException {
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
		CmsAttribute dataAttr = getAttribute(attrName);
		if (dataAttr != null) { return setAttributeOnObject(dataAttr, values, object); }

		int idx = object.findAttrIndex(attrName);
		IDfAttr attribute = object.getAttr(idx);
		CmsDataType dataType = CmsDataType.fromAttribute(attribute);
		return setAttributeOnObject(attrName, dataType, attribute.isRepeating(), values, object);
	}

	protected final boolean setAttributeOnObject(CmsAttribute attribute, IDfValue value, T object) throws DfException {
		return setAttributeOnObject(attribute, Collections.singleton(value), object);
	}

	protected final boolean setAttributeOnObject(CmsAttribute attribute, Collection<IDfValue> values, T object)
		throws DfException {
		if (object == null) { throw new IllegalArgumentException("Must provide an object to set the attributes to"); }
		if (attribute == null) { throw new IllegalArgumentException("Must provide an attribute to set on the object"); }
		return setAttributeOnObject(attribute.getName(), attribute.getType(), attribute.isRepeating(), values, object);
	}

	private final boolean setAttributeOnObject(String attrName, CmsDataType dataType, boolean repeating,
		Collection<IDfValue> values, T object) throws DfException {
		if (object == null) { throw new IllegalArgumentException("Must provide an object to set the attributes to"); }
		if (attrName == null) { throw new IllegalArgumentException(
			"Must provide an attribute name to set on the object"); }
		if (values == null) {
			values = Collections.emptyList();
		}
		// If an existing object is being updated, first clear repeating values if the attribute
		// being set is repeating type.
		clearAttributeFromObject(attrName, object);
		for (IDfValue value : values) {
			if (value == null) {
				value = dataType.getNullValue();
			}
			if (repeating) {
				object.appendValue(attrName, value);
			} else {
				// I wonder if appendValue() can also be used for non-repeating attributes...
				object.setValue(attrName, value);
			}
		}
		return true;
	}

	protected final void clearAttributeFromObject(String attr, T object) throws DfException {
		if (object == null) { throw new IllegalArgumentException("Must provide an object to clear the attribute from"); }
		CmsAttribute dataAttr = getAttribute(attr);
		if (dataAttr != null) {
			clearAttributeFromObject(dataAttr, object);
		} else {
			clearAttributeFromObject(object.getAttr(object.findAttrIndex(attr)), object);
		}
	}

	protected final void clearAttributeFromObject(CmsAttribute attribute, T object) throws DfException {
		if (attribute == null) { throw new IllegalArgumentException(
			"Must provide an attribute to clear from the object"); }
		clearAttributeFromObject(attribute.getName(), attribute.getType(), attribute.isRepeating(), object);
	}

	protected final void clearAttributeFromObject(IDfAttr attribute, T object) throws DfException {
		if (attribute == null) { throw new IllegalArgumentException(
			"Must provide an attribute to clear from the object"); }
		clearAttributeFromObject(attribute.getName(), CmsDataType.fromAttribute(attribute), attribute.isRepeating(),
			object);
	}

	protected final void clearAttributeFromObject(String attrName, CmsDataType dataType, boolean repeating, T object)
		throws DfException {
		if (object == null) { throw new IllegalArgumentException("Must provide an object to clear the attribute from"); }
		if (attrName == null) { throw new IllegalArgumentException(
			"Must provide an attribute name to clear from object"); }
		if (repeating) {
			object.removeAll(attrName);
		} else {
			if (dataType == null) { throw new IllegalArgumentException(
				"Must provide the data type for the attribute being cleared"); }
			object.setValue(attrName, dataType.getNullValue());
		}
	}

	protected final boolean updateSystemAttributes(final IDfPersistentObject targetObject, CmsTransferContext context)
		throws DfException, CMSMFException {
		if (targetObject == null) { throw new IllegalArgumentException("Must provide a target object to update"); }
		if (context == null) { throw new IllegalArgumentException("Must provide a context to update with"); }

		// Update the system attributes, if we can
		final String objectId = targetObject.getObjectId().getId();
		final CmsObjectType targetType;
		try {
			targetType = CmsObjectType.decodeType(targetObject);
		} catch (UnsupportedObjectTypeException e) {
			throw new CMSMFException(String.format("Failed to decode the object type for [%s]", targetObject.getType()
				.getName()), e);
		}
		Mapping m = context.getAttributeMapper().getSourceMapping(targetType, CmsAttributes.R_OBJECT_ID, objectId);
		if (m == null) { return false; }

		Set<String> s = Collections.singleton(m.getSourceValue());
		final AtomicBoolean success = new AtomicBoolean(false);
		context.deserializeObjects(targetType.getCmsObjectClass(), s, new ObjectHandler() {

			@Override
			public boolean newBatch(String batchId) throws CMSMFException {
				return true;
			}

			@Override
			public void handle(CmsObject<?> dataObject) throws CMSMFException {
				try {
					success.set(dataObject.updateSystemAttributes(targetObject));
				} catch (DfException e) {
					throw new CMSMFException(String.format(
						"Failed to update the system attributes for %s object [%s](%s)", dataObject.getType().name(),
						dataObject.getLabel(), objectId), e);
				}
			}

			@Override
			public boolean closeBatch(boolean ok) throws CMSMFException {
				return true;
			}
		});
		return success.get();
	}

	protected String generateSystemAttributesSQL(IDfPersistentObject object) throws DfException {

		final String objType = object.getType().getName();
		CmsAttribute attribute = getAttribute(CmsAttributes.R_MODIFY_DATE);
		if (attribute == null) { return null; }

		final IDfValue modifyDate = attribute.getValue();
		String sql = "" //
			+ "UPDATE %s_s SET " //
			+ "       r_modify_date = %s " //
			+ "       %s " //
			+ " WHERE r_object_id = ''%s''";
		String vstampFlag = "";
		// TODO: For now we don't touch the i_vstamp b/c we don't think it necessary
		// (Setting.SKIP_VSTAMP.getBoolean() ? "" : String.format(", i_vstamp = %d",
		// dctmObj.getIntSingleAttrValue(CmsAttributes.I_VSTAMP)));

		return String.format(sql, objType, DfUtils.generateSqlDateClause(modifyDate.asTime(), object.getSession()),
			vstampFlag, object.getObjectId().getId());
	}

	/**
	 * Updates modify date attribute of an persistent object using execsql.
	 *
	 * @param object
	 *            the DFC persistentObject representing an object in repository
	 * @throws DfException
	 *             Signals that Dctm Server error has occurred.
	 */
	private boolean updateSystemAttributes(IDfPersistentObject object) throws DfException {
		final String sqlStr = generateSystemAttributesSQL(object);
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
		return String.format("CmsObject [type=%s, subtype=%s, dfClass=%s, id=%s, label=%s]", this.type, this.subtype,
			this.dfClass.getSimpleName(), this.id, this.label);
	}
}