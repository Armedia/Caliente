/**
 *
 */

package com.armedia.cmf.engine.sharepoint;

import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.beanutils.BeanUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.cmf.storage.StoredDataType;
import com.armedia.cmf.storage.StoredObjectType;
import com.armedia.commons.utilities.Tools;

/**
 * @author diego
 *
 */
public abstract class ShptObject<T> {

	protected static class AttributeDescriptor {
		private final String name;
		private final StoredDataType type;
		private final boolean repeating;

		/**
		 * @param type
		 * @param repeating
		 */
		protected AttributeDescriptor(String name, StoredDataType type, boolean repeating) {
			this.name = name;
			this.type = type;
			this.repeating = repeating;
		}

		public final String getName() {
			return this.name;
		}

		public final StoredDataType getType() {
			return this.type;
		}

		public final boolean isRepeating() {
			return this.repeating;
		}

		@Override
		public int hashCode() {
			return Tools.hashTool(this, null, this.name, this.type, this.repeating);
		}

		@Override
		public boolean equals(Object obj) {
			if (!Tools.baseEquals(this, obj)) { return false; }
			AttributeDescriptor other = AttributeDescriptor.class.cast(obj);
			if (!Tools.equals(this.name, other.name)) { return false; }
			if (this.type != other.type) { return false; }
			if (this.repeating != other.repeating) { return false; }
			return true;
		}
	}

	private static final Map<String, Map<String, AttributeDescriptor>> DESCRIPTORS = new ConcurrentHashMap<String, Map<String, AttributeDescriptor>>();

	protected static void registerDescriptors(Class<? extends ShptObject<?>> k,
		Collection<AttributeDescriptor> descriptors) {
		Map<String, AttributeDescriptor> m = new HashMap<String, AttributeDescriptor>();
		for (AttributeDescriptor d : descriptors) {
			m.put(d.name, d);
		}
		ShptObject.DESCRIPTORS.put(k.getCanonicalName(), Tools.freezeMap(m));
	}

	private static Map<String, AttributeDescriptor> getDescriptors(Class<?> k) {
		return ShptObject.DESCRIPTORS.get(k.getCanonicalName());
	}

	protected final Logger log = LoggerFactory.getLogger(getClass());
	protected final StoredObjectType type;
	protected final T wrapped;
	private final Map<String, AttributeDescriptor> attributes;
	private final Map<String, Object> values = new HashMap<String, Object>();

	protected ShptObject(T wrapped, StoredObjectType type) {
		if (wrapped == null) { throw new IllegalArgumentException("Must provide an object to wrap around"); }
		if (type == null) { throw new IllegalArgumentException("Must provide an object type"); }
		this.wrapped = wrapped;
		this.type = type;
		this.attributes = ShptObject.getDescriptors(wrapped.getClass());
		for (AttributeDescriptor d : this.attributes.values()) {
			final String name = d.getName();
			try {
				final Object v = BeanUtils.getProperty(this.wrapped, name);
				if (v != null) {
					this.values.put(name, v);
				}
			} catch (IllegalAccessException e) {
				if (this.log.isTraceEnabled()) {
					this.log.error(String.format("Failed to read the property [%s] from an object of class [%s]", name,
						wrapped.getClass().getCanonicalName()), e);
				}
			} catch (InvocationTargetException e) {
				if (this.log.isTraceEnabled()) {
					this.log.error(String.format("Failed to read the property [%s] from an object of class [%s]", name,
						wrapped.getClass().getCanonicalName()), e);
				}
			} catch (NoSuchMethodException e) {
				if (this.log.isTraceEnabled()) {
					this.log.error(String.format("Failed to read the property [%s] from an object of class [%s]", name,
						wrapped.getClass().getCanonicalName()), e);
				}
			}
		}
	}

	public Set<String> getAttributeNames() {
		return this.values.keySet();
	}

	public ShptValue getAttribute(String name) throws NoSuchElementException {
		AttributeDescriptor d = this.attributes.get(name);
		if (d == null) { throw new NoSuchElementException(String.format("No property named [%s] in class [%s]", name,
			this.wrapped.getClass().getCanonicalName())); }

		Object v = this.values.get(name);
		if (v == null) { return null; }

		Class<?> k = v.getClass();
		if (k.isArray()) {
		}
		if (k.isPrimitive()) {
			if (k == Boolean.TYPE) {
				StoredDataType.BOOLEAN.hashCode();
			} else if ((k == Byte.TYPE) || (k == Short.TYPE) || (k == Integer.TYPE) || (k == Long.TYPE)) {
				StoredDataType.INTEGER.hashCode();
			} else if ((k == Float.TYPE) || (k == Double.TYPE)) {
				StoredDataType.DOUBLE.hashCode();
			}
		}
		return null;
	}

	public void setAttribute(String name, ShptValue value) {

	}

	public void clearAttribute(String name) {

	}

	public final T getObject() {
		return this.wrapped;
	}

	public abstract String getId();

	public abstract String getName();

	public abstract String getServerRelativeUrl();

	public abstract Date getCreatedTime();

	public abstract Date getLastModifiedTime();

	public final StoredObjectType getStoredType() {
		return this.type;
	}
}