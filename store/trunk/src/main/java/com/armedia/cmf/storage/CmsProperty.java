package com.armedia.cmf.storage;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import com.armedia.commons.utilities.Tools;

public class CmsProperty implements Iterable<String> {

	protected static final String[] NO_VALUES = new String[0];

	private final String name;
	private final String type;
	private final boolean repeating;

	private String singleValue = null;
	private final List<String> values;

	public CmsProperty(String name, String type, String... values) {
		this(name, type, true, Arrays.asList(values));
	}

	public CmsProperty(String name, String type, boolean repeating, String... values) {
		this(name, type, repeating, Arrays.asList(values));
	}

	public CmsProperty(String name, String type, Collection<String> values) {
		this(name, type, true, values);
	}

	public CmsProperty(String name, String type, boolean repeating, Collection<String> values) {
		if (name == null) { throw new IllegalArgumentException("Must provide a name"); }
		if (type == null) { throw new IllegalArgumentException("Must provide a data type"); }
		if (values == null) {
			values = Collections.emptyList();
		}
		this.name = name;
		this.type = type;
		final int valueCount = values.size();
		this.repeating = repeating;
		this.values = new ArrayList<String>(valueCount);
		setValues(values);
	}

	/**
	 * <p>
	 * Returns the name given to this instance.
	 * </p>
	 *
	 * @return the name given to this instance.
	 */
	public final String getName() {
		return this.name;
	}

	/**
	 * <p>
	 * Returns the type associated to this instance.
	 * </p>
	 *
	 * @return the type associated to this instance.
	 */
	public final String getType() {
		return this.type;
	}

	/**
	 * <p>
	 * Indicates whether or not this instance can store multiple values ({@code true}), or only a
	 * single one ({@code false}).
	 * </p>
	 *
	 * @return whether or not this instance can store multiple values ({@code true}), or only a
	 *         single one ({@code false}).
	 */
	public final boolean isRepeating() {
		return this.repeating;
	}

	/**
	 * <p>
	 * Returns the number of values stored in this instance. For single-valued instances, it always
	 * returns 1.
	 * </p>
	 *
	 * @return the number of values stored in this instance. For single-valued instances, it always
	 *         returns 1.
	 */
	public final int getValueCount() {
		if (!this.repeating) { return 1; }
		return this.values.size();
	}

	private int sanitizeIndex(int idx) {
		if (idx < 0) {
			idx = 0;
		}
		if ((!this.repeating && (idx > 0)) || (this.repeating && (idx >= this.values.size()))) { throw new ArrayIndexOutOfBoundsException(
			idx); }
		return idx;
	}

	/**
	 * <p>
	 * Returns {@code true} if there are values stored in this instance, {@code false} otherwise. In
	 * particular, for single-valued instances, this method always returns {@code true}.
	 * </p>
	 *
	 * @return {@code true} if there are values stored in this instance, {@code false} otherwise.
	 */
	public final boolean hasValues() {
		return !this.repeating || !this.values.isEmpty();
	}

	/**
	 * <p>
	 * Sets the values for this instance to match the given parameter array. If this instance is a
	 * single-valued instance, then only the first element of the array will be stored as the single
	 * value. If there were no elements in the array, then an empty array (of length 0) will be used
	 * in its stead. In the case of single-valued instances, if the array is empty, then only the
	 * {@code null}-value will be used.
	 * </p>
	 *
	 * @param values
	 *            the values to set.
	 */
	public final void setValues(String... values) {
		setValues(Arrays.asList(values));
	}

	/**
	 * <p>
	 * Sets the values for this instance to match the given {@link Collection}. If this instance is
	 * a single-valued instance, then only the first element of the collection will be stored as the
	 * single value. If the collection was {@code null}, then an empty collection will be used in
	 * its stead. In the case of single-valued instances, if the collection is empty, then only the
	 * {@code null}-value will be used.
	 * </p>
	 *
	 * @param values
	 *            the values to set.
	 */
	public final void setValues(Collection<String> values) {
		this.values.clear();
		if (values == null) {
			values = Collections.emptyList();
		}
		if (this.repeating) {
			for (String value : values) {
				this.values.add(value);
			}
		} else {
			String value = null;
			if (!values.isEmpty()) {
				value = values.iterator().next();
			}
			this.singleValue = value;
		}
	}

	/**
	 * <p>
	 * Returns a list containing all the values in this instance. If this instance is single-valued,
	 * a singleton list (as calculated by {@link Collections#singletonList(Object)}) will be
	 * returned containing only the singular value.
	 * </p>
	 *
	 * @return a list containing all the values in this instance
	 */
	public final List<String> getValues() {
		if (this.repeating) { return this.values; }
		List<String> l = new ArrayList<String>(1);
		l.add(this.singleValue);
		return l;
	}

	/**
	 * <p>
	 * Adds a value to this repeating instance. This operation is not allowed for single-valued
	 * instances.If the given {@code value} parameter is {@code null}, that value is used.
	 * </p>
	 *
	 * @param value
	 */
	public final void addValue(String value) {
		if (this.repeating) {
			this.values.add(value);
		} else {
			throw new UnsupportedOperationException("This is a single-valued property, cannot add another value");
		}
	}

	/**
	 * <p>
	 * Adds all the given values to this repeating instance. This operation is not allowed for
	 * single-valued instances.
	 * </p>
	 *
	 * @param values
	 */
	public final void addValues(Collection<String> values) {
		for (String v : values) {
			addValue(v);
		}
	}

	/**
	 * <p>
	 * Sets value for this instance. If this is a repeating value instance, it clears all stored
	 * values and leaves only the submitted value. If the given {@code value} parameter is
	 * {@code null}, then that value is used. If the desire is to clear all repeating values, use
	 * {@link #clearValue()} instead.
	 * </p>
	 */
	public final void setValue(String value) {
		if (this.repeating) {
			this.values.clear();
			this.values.add(value);
		} else {
			this.singleValue = value;
		}
	}

	/**
	 * <p>
	 * Clears the set value for this instance. If this is a repeating value instance, it clears all
	 * stored values. If the value was cleared, then {@code true} is returned, {@code false}
	 * otherwise. Invoking this method for single-valued instances will always return {@code true}.
	 * </p>
	 *
	 * @return {@code true} if the value was cleared, {@code false} otherwise.
	 */
	public final boolean clearValue() {
		if (this.repeating) {
			boolean empty = this.values.isEmpty();
			this.values.clear();
			return !empty;
		} else {
			this.singleValue = null;
			return true;
		}
	}

	/**
	 * <p>
	 * Removes and returns the indexed value from this instance. If this is a single-valued
	 * instance, then only index 0 is allowed to be used, and that will result in the equivalent
	 * outcome as an invocation to {@link #clearValue()}, except the old single value will be
	 * returned. If this is a repeat-valued instance, then the {@code idx}-th element in the value
	 * list will be removed and returned, equivalently to the behavior of {@link List#remove(int)}.
	 * Negative indexes will be folded to 0.
	 * </p>
	 * <p>
	 * If the given index is out of bounds for the number of elements in a repeat-valued instance,
	 * an {@link ArrayIndexOutOfBoundsException} will be raised.
	 * </p>
	 *
	 * @param idx
	 * @return the removed value
	 */
	public final String removeValue(int idx) {
		idx = sanitizeIndex(idx);
		if (this.repeating) { return this.values.remove(idx); }
		String old = this.singleValue;
		this.singleValue = null;
		return old;
	}

	/**
	 * <p>
	 * Returns the indexed value from this instance. If this is a single-valued instance, then only
	 * index 0 is allowed to be used, and that will result in the equivalent outcome as an
	 * invocation to {@link #getValue()}. If this is a repeat-valued instance, then the {@code idx}
	 * -th element in the value list will be returned, equivalently to the behavior of
	 * {@link List#get(int)}. Negative indexes will be folded to 0.
	 * </p>
	 * <p>
	 * If the given index is out of bounds for the number of elements in a repeat-valued instance,
	 * an {@link ArrayIndexOutOfBoundsException} will be raised.
	 * </p>
	 *
	 * @param idx
	 * @return the removed value
	 * @throws ArrayIndexOutOfBoundsException
	 */
	public final String getValue(int idx) {
		idx = sanitizeIndex(idx);
		if (this.repeating) { return this.values.get(idx); }
		return this.singleValue;
	}

	/**
	 * <p>
	 * Identical to {@link #getValue(int)} invoked with {@code idx = 0}.
	 * </p>
	 *
	 * @return the 0-th value in this element.
	 */
	public final String getValue() {
		return getValue(0);
	}

	/**
	 * <p>
	 * Compares the basic structure of this instance to the other instance. In particular, it
	 * doesn't compare the stored values - only the name, type and whether it's a repeat-valued
	 * instance.
	 * </p>
	 *
	 * @param other
	 * @return {@code true} if the basic typing structure matches, {@code false} otherwise
	 */
	public boolean isSame(CmsProperty other) {
		if (other == null) { return false; }
		if (other == this) { return true; }
		if (!this.name.equals(other.name)) { return false; }
		if (this.type != other.type) { return false; }
		if (this.repeating != other.repeating) { return false; }
		return true;
	}

	public boolean isSameValues(CmsProperty other) {
		if (!isSame(other)) { return false; }
		if (!this.repeating) { return Tools.equals(this.singleValue, other.singleValue); }
		final int valueCount = this.values.size();
		if (valueCount != other.getValueCount()) { return false; }
		for (int i = 0; i < valueCount; i++) {
			if (!Tools.equals(this.values.get(i), other.values.get(i))) { return false; }
		}
		return true;
	}

	/**
	 * <p>
	 * Returns an {@link Iterator} to walk over the values for this instance. The iterator is backed
	 * by the object at all times. That means that invoking {@link Iterator#remove()} will have the
	 * effect of removing values from the instance. For single-valued instances, this means that the
	 * single value will be set to the {@code null}-value.
	 * </p>
	 */
	@Override
	public final Iterator<String> iterator() {
		if (this.repeating) { return this.values.iterator(); }
		return new Iterator<String>() {
			boolean retrieved = false;
			boolean removed = false;

			@Override
			public boolean hasNext() {
				return !this.retrieved;
			}

			@Override
			public String next() {
				if (this.retrieved) { throw new NoSuchElementException(); }
				this.retrieved = true;
				return CmsProperty.this.singleValue;
			}

			@Override
			public void remove() {
				if (!this.retrieved) { throw new IllegalStateException("No element to remove()"); }
				if (this.removed) { throw new IllegalStateException("Element already removed"); }
				CmsProperty.this.singleValue = null;
				this.removed = true;
			}
		};
	}

	public final String getConcatenatedString(String sep) {
		if (sep == null) {
			sep = "";
		}
		if (!this.repeating) { return this.singleValue; }
		StringBuilder b = new StringBuilder();
		for (String v : this.values) {
			if ((b.length() > 0) && (sep.length() > 0)) {
				b.append(sep);
			}
			b.append(v);
		}
		return b.toString();
	}

	@Override
	public String toString() {
		return String.format("CmsProperty [name=%s, type=%s, repeating=%s, %s=%s]", this.name, this.type,
			this.repeating, (this.repeating ? "values" : "singleValue"), (this.repeating ? this.values
				: this.singleValue));
	}
}