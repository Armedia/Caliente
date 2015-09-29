/**
 *
 */

package com.armedia.cmf.engine.sharepoint.types;

import java.util.ArrayList;
import java.util.Collection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.cmf.engine.exporter.ExportException;
import com.armedia.cmf.engine.sharepoint.ShptSession;
import com.armedia.cmf.engine.sharepoint.exporter.ShptExportContext;
import com.armedia.cmf.storage.ContentStore;
import com.armedia.cmf.storage.ContentStore.Handle;
import com.armedia.cmf.storage.StoredObject;
import com.armedia.cmf.storage.StoredObjectType;
import com.armedia.cmf.storage.StoredValue;

/**
 * @author diego
 *
 */
public abstract class ShptObject<T> {

	public static final String TARGET_NAME = "shpt";

	protected final Logger log = LoggerFactory.getLogger(getClass());
	protected final StoredObjectType type;
	private final Class<T> wrappedClass;
	protected final T wrapped;
	protected final ShptSession service;

	protected ShptObject(ShptSession service, T wrapped, StoredObjectType type) {
		if (service == null) { throw new IllegalArgumentException(
			"Must provide the service this item was retrieved with"); }
		if (wrapped == null) { throw new IllegalArgumentException("Must provide an object to wrap around"); }
		if (type == null) { throw new IllegalArgumentException("Must provide an object type"); }
		@SuppressWarnings("unchecked")
		Class<T> c = (Class<T>) wrapped.getClass();
		this.wrappedClass = c;
		this.service = service;
		this.wrapped = wrapped;
		this.type = type;
	}

	public final ShptSession getService() {
		return this.service;
	}

	protected final T castObject(Object object) throws Exception {
		if (object == null) { return null; }
		if (!this.wrappedClass.isInstance(object)) { throw new Exception(String.format(
			"Expected an object of class %s, but got one of class %s", this.wrappedClass.getCanonicalName(), object
				.getClass().getCanonicalName())); }
		return this.wrappedClass.cast(object);
	}

	public final T getObject() {
		return this.wrapped;
	}

	public abstract String getId();

	public String getSearchKey() {
		return getId();
	}

	public abstract String getBatchId();

	public abstract String getLabel();

	public abstract String getName();

	public final StoredObjectType getStoredType() {
		return this.type;
	}

	public final T getWrapped() {
		return this.wrapped;
	}

	public final StoredObject<StoredValue> marshal() throws ExportException {
		StoredObject<StoredValue> object = new StoredObject<StoredValue>(this.type, getId(), getSearchKey(),
			getBatchId(), getLabel(), this.type.name());
		marshal(object);
		return object;
	}

	protected abstract void marshal(StoredObject<StoredValue> object) throws ExportException;

	public final Collection<ShptObject<?>> identifyDependents(ShptSession service, StoredObject<StoredValue> marshaled,
		ShptExportContext ctx) throws Exception {
		return findDependents(service, marshaled, ctx);
	}

	protected Collection<ShptObject<?>> findDependents(ShptSession service, StoredObject<StoredValue> marshaled,
		ShptExportContext ctx) throws Exception {
		return new ArrayList<ShptObject<?>>();
	}

	public final Collection<ShptObject<?>> identifyRequirements(ShptSession session,
		StoredObject<StoredValue> marshaled, ShptExportContext ctx) throws Exception {
		return findRequirements(session, marshaled, ctx);
	}

	protected Collection<ShptObject<?>> findRequirements(ShptSession session, StoredObject<StoredValue> marshaled,
		ShptExportContext ctx) throws Exception {
		return new ArrayList<ShptObject<?>>();
	}

	public Handle storeContent(ShptSession session, StoredObject<StoredValue> marshaled, ContentStore streamStore)
		throws Exception {
		return null;
	}
}