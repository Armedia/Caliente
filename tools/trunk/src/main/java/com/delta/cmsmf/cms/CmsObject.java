/**
 *
 */

package com.delta.cmsmf.cms;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.text.StrTokenizer;
import org.apache.log4j.Logger;

import com.armedia.commons.utilities.Tools;
import com.delta.cmsmf.cms.CmsAttributeHandlers.AttributeHandler;
import com.delta.cmsmf.cms.CmsCounter.Result;
import com.delta.cmsmf.exception.CMSMFException;
import com.delta.cmsmf.utils.DfUtils;
import com.documentum.fc.client.DfPermit;
import com.documentum.fc.client.IDfCollection;
import com.documentum.fc.client.IDfLocalTransaction;
import com.documentum.fc.client.IDfPermit;
import com.documentum.fc.client.IDfPermitType;
import com.documentum.fc.client.IDfPersistentObject;
import com.documentum.fc.client.IDfQuery;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.client.IDfSysObject;
import com.documentum.fc.common.DfException;
import com.documentum.fc.common.IDfAttr;
import com.documentum.fc.common.IDfTime;
import com.documentum.fc.common.IDfValue;

/**
 * @author Diego Rivera <diego.rivera@armedia.com>
 *
 */
/**
 * @author Diego Rivera <diego.rivera@armedia.com>
 *
 * @param <T>
 */
public abstract class CmsObject<T extends IDfPersistentObject> {

	private static final Collection<String> NO_PERMITS = Collections.emptySet();

	protected final class PermitDelta {
		private final String objectId;
		private final IDfPermit oldPermit;
		private final IDfPermit newPermit;
		private final Collection<IDfPermit> newXPermit;

		public PermitDelta(IDfSysObject object, int newPermission, String... newXPermits) throws DfException {
			this(object, newPermission, (newXPermits == null ? CmsObject.NO_PERMITS : Arrays.asList(newXPermits)));
		}

		public PermitDelta(IDfSysObject object, int newPermission, Collection<String> newXPermits) throws DfException {
			this.objectId = object.getObjectId().getId();
			final String userName = object.getSession().getLoginUserName();

			// Does it have the required access permission?
			int oldPermission = object.getPermit();
			if (oldPermission < newPermission) {
				this.oldPermit = new DfPermit();
				this.oldPermit.setAccessorName(userName);
				this.oldPermit.setPermitType(IDfPermitType.ACCESS_PERMIT);
				this.oldPermit.setPermitValue(DfUtils.decodeAccessPermission(oldPermission));
				this.newPermit = new DfPermit();
				this.newPermit.setAccessorName(userName);
				this.newPermit.setPermitType(IDfPermitType.ACCESS_PERMIT);
				this.newPermit.setPermitValue(DfUtils.decodeAccessPermission(newPermission));
			} else {
				this.oldPermit = null;
				this.newPermit = null;
			}

			Set<String> s = new HashSet<String>(StrTokenizer.getCSVInstance(object.getXPermitList()).getTokenList());
			List<IDfPermit> l = new ArrayList<IDfPermit>();
			for (String x : newXPermits) {
				if (x == null) {
					continue;
				}
				if (!s.contains(x)) {
					IDfPermit xpermit = new DfPermit();
					xpermit.setAccessorName(userName);
					xpermit.setPermitType(IDfPermitType.EXTENDED_PERMIT);
					xpermit.setPermitValue(x);
					l.add(xpermit);
				}
			}
			this.newXPermit = Collections.unmodifiableCollection(l);
		}

		private boolean apply(IDfSysObject object, boolean grant) throws DfException {
			if (!Tools.equals(this.objectId, object.getObjectId().getId())) { throw new DfException(
				String.format("ERROR: Expected object with ID [%s] but got [%s] instead", this.objectId, object
					.getObjectId().getId())); }
			boolean ret = false;
			IDfPermit access = (grant ? this.newPermit : this.oldPermit);
			if (access != null) {
				object.grantPermit(access);
				ret = true;
			}
			for (IDfPermit p : this.newXPermit) {
				if (grant) {
					object.grantPermit(p);
				} else {
					object.revokePermit(p);
				}
				ret = true;
			}
			return ret;
		}

		public boolean grant(IDfSysObject object) throws DfException {
			return apply(object, true);
		}

		public boolean revoke(IDfSysObject object) throws DfException {
			return apply(object, false);
		}
	}

	public static final class SaveResult {
		private final Result result;
		private final String objectId;

		private SaveResult(Result result, String objectId) {
			this.result = result;
			this.objectId = objectId;
		}

		public Result getResult() {
			return this.result;
		}

		public String getObjectId() {
			return this.objectId;
		}

		@Override
		public int hashCode() {
			return Tools.hashTool(this, null, this.result, this.objectId);
		}

		@Override
		public boolean equals(Object obj) {
			if (!Tools.baseEquals(this, obj)) { return false; }
			SaveResult other = SaveResult.class.cast(obj);
			if (!Tools.equals(this.result, other.result)) { return false; }
			if (!Tools.equals(this.objectId, other.objectId)) { return false; }
			return true;
		}

		@Override
		public String toString() {
			return String.format("SaveResult [result=%s, objectId=%s]", this.result, this.objectId);
		}
	}

	private static final String DEBUG_DUMP = "$debug-dump$";

	private final CmsAttributeMapper NULL_MAPPER = new CmsAttributeMapper() {
		@Override
		protected Mapping createMapping(CmsObjectType objectType, String mappingName, String sourceValue,
			String targetValue) {
			return null;
		}

		@Override
		public Mapping getTargetMapping(CmsObjectType objectType, String mappingName, String sourceValue) {
			return null;
		}

		@Override
		public Mapping getSourceMapping(CmsObjectType objectType, String mappingName, String targetValue) {
			return null;
		}
	};

	protected final Logger log = Logger.getLogger(getClass());

	private final CmsObjectType type;
	private final Class<T> dfClass;

	private String id = null;
	private String label = null;
	private String subtype = null;
	private final Map<String, CmsAttribute> attributes = new HashMap<String, CmsAttribute>();
	private final Map<String, CmsProperty> properties = new HashMap<String, CmsProperty>();

	protected CmsObject(CmsObjectType type, Class<T> dfClass) {
		if (type == null) { throw new IllegalArgumentException("Must provide an object type"); }
		if (dfClass == null) { throw new IllegalArgumentException("Must provde a DF class"); }
		if (type.getDfClass() != dfClass) { throw new IllegalArgumentException(String.format(
			"Class mismatch: type is tied to class [%s], but was given class [%s]", type.getDfClass()
			.getCanonicalName(), dfClass.getCanonicalName())); }
		this.type = type;
		this.dfClass = dfClass;
	}

	public void load(ResultSet rs) throws SQLException {
		this.id = rs.getString("object_id");
		this.label = rs.getString("object_label");
		this.subtype = rs.getString("object_subtype");
		this.attributes.clear();
		this.properties.clear();
	}

	public void loadAttributes(ResultSet rs) throws SQLException {
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

	public void loadProperties(ResultSet rs) throws SQLException {
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

	public final CmsObjectType getType() {
		return this.type;
	}

	public final String getSubtype() {
		return this.subtype;
	}

	public final String getId() {
		return this.id;
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
	 * {@link #loadFromCMS(IDfPersistentObject)}, or not.
	 * </p>
	 *
	 * @param object
	 * @return {@code true} if the object should continue to be loaded, {@code false} otherwise.
	 * @throws DfException
	 */
	protected boolean isValidForLoad(T object) throws DfException {
		return true;
	}

	protected abstract String calculateLabel(T object) throws DfException;

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
	public final boolean loadFromCMS(IDfPersistentObject object) throws DfException, CMSMFException {
		if (object == null) { throw new IllegalArgumentException("Must provide an object to populate from"); }
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
		// call, etc., because setting it directly as an attribute would result in an error from
		// DFC, and therefore specialized code is required to handle it
		this.properties.clear();
		List<CmsProperty> properties = new ArrayList<CmsProperty>();
		getDataProperties(properties, typedObject);
		// TODO: Only do this when in debug mode, to avoid slowdowns.
		properties.add(new CmsProperty(CmsObject.DEBUG_DUMP, CmsDataType.DF_STRING, false, DfValueFactory
			.newStringValue(object.dump())));
		for (CmsProperty property : properties) {
			// This mechanism overwrites properties, and intentionally so
			this.properties.put(property.getName(), property);
		}
		return true;
	}

	public final void persistDependencies(IDfPersistentObject object, CmsDependencyManager manager) throws DfException,
	CMSMFException {
		if (object == null) { throw new IllegalArgumentException(
			"Must provide the Documentum object from which to identify the dependencies"); }
		doPersistDependencies(castObject(object), manager);
	}

	protected void doPersistDependencies(T object, CmsDependencyManager manager) throws DfException, CMSMFException {
	}

	public final SaveResult saveToCMS(IDfSession session, CmsAttributeMapper mapper) throws DfException,
	CMSMFException, SQLException {
		if (session == null) { throw new IllegalArgumentException("Must provide a session to save the object"); }
		if (mapper == null) {
			mapper = this.NULL_MAPPER;
		}
		boolean transOpen = false;
		boolean ok = false;

		// We assume the worst, out of the gate
		IDfLocalTransaction localTx = null;
		boolean mustFreeze = false;
		boolean mustImmute = false;
		IDfSysObject sysObject = null;
		try {
			if (session.isTransactionActive()) {
				localTx = session.beginTransEx();
			} else {
				session.beginTrans();
			}
			transOpen = true;
			if (skipImport(session)) { return new SaveResult(Result.SKIPPED, null); }

			T object = locateInCms(session);
			final boolean isNew = (object == null);
			final boolean updateVersionLabels = isVersionable(object);
			final Result result;
			if (isNew) {
				// Create a new object
				object = newObject(session);
				result = Result.CREATED;
				if (object instanceof IDfSysObject) {
					sysObject = IDfSysObject.class.cast(object);
				}
			} else {
				if (isSameObject(object)) { return new SaveResult(Result.DUPLICATE, object.getObjectId().getId()); }
				result = Result.UPDATED;
				if (object instanceof IDfSysObject) {
					sysObject = IDfSysObject.class.cast(object);
					if (sysObject.isFrozen()) {
						mustFreeze = true;
						if (this.log.isDebugEnabled()) {
							this.log.debug(String.format("Clearing frozen status from [%s](%s)", this.label, this.id));
						}
						sysObject.setBoolean(CmsAttributes.R_FROZEN_FLAG, false);
						sysObject.save();
					}
					if (sysObject.isImmutable()) {
						mustImmute = true;
						if (this.log.isDebugEnabled()) {
							this.log.debug(String
								.format("Clearing immutable status from [%s](%s)", this.label, this.id));
						}
						sysObject.setBoolean(CmsAttributes.R_IMMUTABLE_FLAG, false);
						sysObject.save();
					}
				}
			}

			CmsAttribute frozen = getAttribute(CmsAttributes.R_FROZEN_FLAG);
			if (frozen != null) {
				// We only copy over the "true" values - we don't override local frozen status
				// if it's set to true, and the incoming value is false
				mustFreeze |= frozen.getValue().asBoolean();
			}
			CmsAttribute immutable = getAttribute(CmsAttributes.R_IMMUTABLE_FLAG);
			if (immutable != null) {
				// We only copy over the "true" values - we don't override local immutable status
				// if it's set to true, and the incoming value is false
				mustImmute |= immutable.getValue().asBoolean();
			}

			// Mapping idMapping =
			mapper.setMapping(this.type, CmsAttributes.R_OBJECT_ID, this.id, object.getObjectId().getId());

			prepareForConstruction(object, isNew);

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

			finalizeConstruction(object, isNew);
			object.save();
			if (postConstruction(object, isNew)) {
				object.save();
			}

			updateSystemAttributes(object);
			object.save();

			if (cleanupAfterSave(object, isNew)) {
				object.save();
			}
			ok = true;
			return new SaveResult(result, object.getObjectId().getId());
		} finally {
			if (ok && (sysObject != null)) {
				if (mustImmute) {
					if (this.log.isDebugEnabled()) {
						this.log.debug(String.format("Setting immutability status to [%s](%s)", this.label, this.id));
					}
					sysObject.setBoolean(CmsAttributes.R_IMMUTABLE_FLAG, true);
					sysObject.save();
				}
				if (mustFreeze) {
					if (this.log.isDebugEnabled()) {
						this.log.debug(String.format("Setting frozen status to [%s](%s)", this.label, this.id));
					}
					sysObject.setBoolean(CmsAttributes.R_FROZEN_FLAG, true);
					sysObject.save();
				}
			}
			if (transOpen) {
				if (ok) {
					if (localTx != null) {
						session.commitTransEx(localTx);
					} else {
						session.commitTrans();
					}
				} else {
					// Clear the mapping
					mapper.clearSourceMapping(this.type, CmsAttributes.R_OBJECT_ID, this.id);
					if (localTx != null) {
						session.abortTransEx(localTx);
					} else {
						session.abortTrans();
					}
				}
			}
		}
	}

	public void resolveDependencies(T object, CmsAttributeMapper mapper) throws DfException, CMSMFException {

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

	protected boolean skipImport(IDfSession session) throws DfException {
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

	protected T newObject(IDfSession session) throws DfException {
		return castObject(session.newObject(this.subtype));
	}

	protected abstract T locateInCms(IDfSession session) throws CMSMFException, DfException;

	protected void getDataProperties(Collection<CmsProperty> properties, T object) throws DfException {
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
	protected void prepareForConstruction(T object, boolean newObject) throws DfException {
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
	protected void finalizeConstruction(T object, boolean newObject) throws DfException {
	}

	protected boolean postConstruction(T object, boolean newObject) throws DfException {
		return false;
	}

	protected boolean cleanupAfterSave(T object, boolean newObject) throws DfException {
		return false;
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

	/**
	 * Updates modify date attribute of an persistent object using execsql.
	 *
	 * @param object
	 *            the DFC persistentObject representing an object in repository
	 * @throws DfException
	 *             Signals that Dctm Server error has occurred.
	 */
	protected final void updateSystemAttributes(T object) throws DfException {
		final String sqlStr;
		if (object instanceof IDfSysObject) {
			// IDfSysObject sysObject = IDfSysObject.class.cast(object);
			final IDfSession session = object.getSession();

			// prepare sql to be executed
			CmsAttribute modifyDateAtt = getAttribute(CmsAttributes.R_MODIFY_DATE);
			CmsAttribute modifierNameAtt = getAttribute(CmsAttributes.R_MODIFIER);
			CmsAttribute creationDateAtt = getAttribute(CmsAttributes.R_CREATION_DATE);
			CmsAttribute creatorNameAtt = getAttribute(CmsAttributes.R_CREATOR_NAME);

			CmsAttribute aclDomainAtt = getAttribute(CmsAttributes.ACL_DOMAIN);

			// Important: REPLACE QUOTES!!

			// Make sure we ALWAYS store this, even if it's "null"...default to the connected user
			// if not set
			String creatorName = creatorNameAtt.getValue().asString();
			if (creatorName.length() == 0) {
				creatorName = "${owner_name}";
			}
			creatorName = CmsMappingUtils.resolveSpecialUser(session, creatorName);
			creatorName = creatorName.replaceAll("'", "''''");

			String modifierName = modifierNameAtt.getValue().asString();
			if (modifierName.length() == 0) {
				modifierName = "${owner_name}";
			}
			modifierName = CmsMappingUtils.resolveSpecialUser(session, modifierName);
			modifierName = modifierName.replaceAll("'", "''''");

			String aclDomain = aclDomainAtt.getValue().asString();
			if (aclDomain.length() == 0) {
				aclDomain = "${owner_name}";
			}
			aclDomain = CmsMappingUtils.resolveSpecialUser(session, aclDomain);
			aclDomain = aclDomain.replaceAll("'", "''''");

			String aclName = getAttribute(CmsAttributes.ACL_NAME).getValue().asString();

			IDfTime modifyDate = modifyDateAtt.getValue().asTime();
			IDfTime creationDate = creationDateAtt.getValue().asTime();

			String sql = "" //
				+ "UPDATE dm_sysobject_s SET " //
				+ "       r_modify_date = %s, " //
				+ "       r_creation_date = %s, " //
				+ "       r_creator_name = ''%s'', " //
				+ "       r_modifier = ''%s'', " //
				+ "       acl_name = ''%s'', " //
				+ "       acl_domain = ''%s'' " //
				+ "       %s " //
				+ " WHERE r_object_id = ''%s''";

			String vstampFlag = "";
			// TODO: For now we don't touch the i_vstamp b/c we don't think it necessary
			// (Setting.SKIP_VSTAMP.getBoolean() ? "" : String.format(", i_vstamp = %d",
			// dctmObj.getIntSingleAttrValue(CmsAttributes.I_VSTAMP)));
			sqlStr = String.format(sql, DfUtils.generateSqlDateClause(modifyDate, session),
				DfUtils.generateSqlDateClause(creationDate, session), creatorName, modifierName, aclName, aclDomain,
				vstampFlag, object.getObjectId().getId());

		} else {

			final String objType = object.getType().getName();
			CmsAttribute attribute = getAttribute(CmsAttributes.R_MODIFY_DATE);
			if (attribute == null) { return; }

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

			sqlStr = String.format(sql, objType,
				DfUtils.generateSqlDateClause(modifyDate.asTime(), object.getSession()), vstampFlag, object
				.getObjectId().getId());

		}
		runExecSQL(object.getSession(), sqlStr);
	}

	/*
	@formatter:off
	protected final void updateContentAttributes(T object) throws DfException {
		final IDfSession session = object.getSession();
		String parentID = object.getObjectId().getId();

		List<CmsContent> contentList = ((DctmDocument) dctmObj).getContentList();
		for (CmsContent content : contentList) {
			String setFile = content.getStrSingleAttrValue(DctmAttrNameConstants.SET_FILE);
			if (StringUtils.isBlank(setFile)) {
				setFile = " ";
			}
			// If setFile contains single quote in its contents, to escape it, replace it with 4
			// single quotes.
			setFile = setFile.replaceAll("'", "''''");
			String setClient = content.getStrSingleAttrValue(DctmAttrNameConstants.SET_CLIENT);
			if (StringUtils.isBlank(setClient)) {
				setClient = " ";
			}
			IDfTime setTime = new DfTime(content.getDateSingleAttrValue(DctmAttrNameConstants.SET_TIME));

			String pageModifierStr = "";
			if (!StringUtils.isBlank(content.getPageModifier())) {
				pageModifierStr = String.format("and dcr.page_modifier = ''%s''", content.getPageModifier());
			}

			// Prepare the sql to be executed
			String sql = "" //
				+ "UPDATE dmr_content_s SET " //
				+ "       set_file = ''%s'', " //
				+ "       set_client = ''%s'', " //
				+ "       set_time = %s " //
				+ " WHERE r_object_id = (" //
				+ "           select dcs.r_object_id " //
				+ "             from dmr_content_s dcs, dmr_content_r dcr " //
				+ "            where dcr.parent_id = ''%s'' " //
				+ "              and dcs.r_object_id = dcr.r_object_id " //
				+ "              and dcs.rendition = %d " //
				+ "              %s " //
				+ "              and dcr.page = %d " //
				+ "              and dcs.full_format = ''%s''" //
				+ "       )";

			String sqlStr = String.format(sql, setFile, setClient, DfUtils.generateSqlDateClause(setTime, session),
				parentID, dctmContent.getIntSingleAttrValue(CmsAttributes.RENDITION), pageModifierStr,
				dctmContent.getPageNbr(), dctmContent.getStrSingleAttrValue(CmsAttributes.FULL_FORMAT));
			// Run the exec sql
			runExecSQL(session, sqlStr);
		}
	}
	 */
	// 	@formatter:on

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

	protected void updateIsDeletedAttribute(IDfSession session, List<String> objectIds) throws DfException {
		// Prepare list of object ids for IN clause
		StringBuilder b = new StringBuilder();
		for (String id : objectIds) {
			if (b.length() > 0) {
				b.append(", ");
			}
			b.append("''").append(id).append("''");
		}

		// prepare sql to be executed
		String sql = "UPDATE dm_sysobject_s SET i_is_deleted = 1 WHERE r_object_id IN ( %s )";
		runExecSQL(session, String.format(sql, b));
	}

	/**
	 * Runs execsql query that can be used to update various system/internal attributes.
	 *
	 * @param session
	 *            the repository session
	 * @param sqlQueryString
	 *            the sql query string
	 * @throws DfException
	 *             Signals that Dctm Server error has occurred.
	 */
	private final void runExecSQL(IDfSession session, String sql) throws DfException {
		IDfCollection resultCol = DfUtils.executeQuery(session, String.format("EXECUTE exec_sql WITH query='%s'", sql),
			IDfQuery.DF_QUERY);
		try {
			if (resultCol.next()) {
				final IDfValue ret = resultCol.getValueAt(0);
				DfUtils.closeQuietly(resultCol);
				final String outcome;
				if (ret.toString().equalsIgnoreCase("F")) {
					outcome = "rollback";
				} else {
					outcome = "commit";
				}
				resultCol = DfUtils.executeQuery(session, String.format("EXECUTE exec_sql with query='%s';", outcome),
					IDfQuery.DF_QUERY);
			}
		} finally {
			DfUtils.closeQuietly(resultCol);
		}
	}

	/**
	 * Removes all links of an object in CMS.
	 *
	 * @param object
	 *            the DFC sysObject who is being unlinked
	 * @throws DfException
	 *             Signals that Dctm Server error has occurred.
	 */
	protected final Set<String> removeAllLinks(IDfSysObject object) throws DfException {
		Set<String> ret = new HashSet<String>();
		int folderIdCount = object.getFolderIdCount();
		for (int i = 0; i < folderIdCount; i++) {
			// We have to do it backwards, instead of forwards, because it's
			// faster. Otherwise, when we remove an "earlier" element, the ones
			// down the list have to be shifted "forward" by DCTM, and that takes
			// time, which we try to avoid by being smart...
			String id = object.getFolderId(folderIdCount - i - 1).getId();
			if (!ret.contains(id)) {
				ret.add(id);
			}
			object.unlink(id);
		}
		return ret;
	}

	@Override
	public String toString() {
		return String.format("CmsObject [type=%s, subtype=%s, dfClass=%s, id=%s, label=%s]", this.type, this.subtype,
			this.dfClass.getSimpleName(), this.id, this.label);
	}
}