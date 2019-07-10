/*******************************************************************************
 * #%L
 * Armedia Caliente
 * %%
 * Copyright (c) 2010 - 2019 Armedia LLC
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
package com.armedia.caliente.store;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import com.armedia.commons.utilities.Tools;

public class CmfProperty<VALUE> extends CmfBaseSetting implements Iterable<VALUE> {

	private VALUE singleValue = null;
	private final List<VALUE> values;

	public CmfProperty(CmfProperty<VALUE> pattern) {
		super(pattern);
		if (pattern == null) { throw new IllegalArgumentException("Must provide a pattern to construct from"); }
		this.singleValue = pattern.singleValue;
		final int valueCount = pattern.values.size();
		this.values = new ArrayList<>(valueCount);
		setValues(pattern.values);
	}

	public CmfProperty(CmfEncodeableName name, CmfValue.Type type) {
		this(name, type, true, (VALUE) null);
	}

	public CmfProperty(CmfEncodeableName name, CmfValue.Type type, boolean multivalue) {
		this(name, type, multivalue, (VALUE) null);
	}

	public CmfProperty(CmfEncodeableName name, CmfValue.Type type, VALUE value) {
		this(name, type, false, value);
	}

	public CmfProperty(CmfEncodeableName name, CmfValue.Type type, boolean multivalue, VALUE value) {
		this(name, type, multivalue, (value != null ? Collections.singleton(value) : null));
	}

	public CmfProperty(CmfEncodeableName name, CmfValue.Type type, Collection<VALUE> values) {
		this(name, type, true, values);
	}

	public CmfProperty(CmfEncodeableName name, CmfValue.Type type, boolean multivalue, Collection<VALUE> values) {
		this(name.encode(), type, multivalue, values);
	}

	public CmfProperty(String name, CmfValue.Type type) {
		this(name, type, true, (VALUE) null);
	}

	public CmfProperty(String name, CmfValue.Type type, boolean multivalue) {
		this(name, type, multivalue, (VALUE) null);
	}

	public CmfProperty(String name, CmfValue.Type type, VALUE value) {
		this(name, type, false, value);
	}

	public CmfProperty(String name, CmfValue.Type type, boolean multivalue, VALUE value) {
		this(name, type, multivalue, (value != null ? Collections.singleton(value) : null));
	}

	public CmfProperty(String name, CmfValue.Type type, Collection<VALUE> values) {
		this(name, type, true, values);
	}

	public CmfProperty(String name, CmfValue.Type type, boolean multivalue, Collection<VALUE> values) {
		super(name, type, multivalue);
		if (values == null) {
			values = Collections.emptyList();
		}
		final int valueCount = values.size();
		this.values = new ArrayList<>(valueCount);
		setValues(values);
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
		if (!isMultivalued()) { return 1; }
		return this.values.size();
	}

	private int sanitizeIndex(int idx) {
		if (idx < 0) {
			idx = 0;
		}
		if ((!isMultivalued() && (idx > 0)) || (isMultivalued() && (idx >= this.values.size()))) {
			throw new ArrayIndexOutOfBoundsException(idx);
		}
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
		return !isMultivalued() || !this.values.isEmpty();
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
	public CmfProperty<VALUE> setValues(Collection<VALUE> values) {
		this.values.clear();
		if (values == null) {
			values = Collections.emptyList();
		}
		if (isMultivalued()) {
			for (VALUE value : values) {
				this.values.add(value);
			}
		} else {
			VALUE value = null;
			if (!values.isEmpty()) {
				value = values.iterator().next();
			}
			this.singleValue = value;
		}
		return this;
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
	public final List<VALUE> getValues() {
		if (isMultivalued()) { return this.values; }
		List<VALUE> l = new ArrayList<>(1);
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
	public CmfProperty<VALUE> addValue(VALUE value) {
		if (isMultivalued()) {
			this.values.add(value);
			return this;
		}
		throw new UnsupportedOperationException("This is a single-valued property, cannot add another value");
	}

	/**
	 * <p>
	 * Adds all the given values to this repeating instance. This operation is not allowed for
	 * single-valued instances.
	 * </p>
	 *
	 * @param values
	 */
	public CmfProperty<VALUE> addValues(Collection<VALUE> values) {
		for (VALUE v : values) {
			addValue(v);
		}
		return this;
	}

	/**
	 * <p>
	 * Sets value for this instance. If this is a repeating value instance, it clears all stored
	 * values and leaves only the submitted value. If the given {@code value} parameter is
	 * {@code null}, then that value is used. If the desire is to clear all repeating values, use
	 * {@link #clearValue()} instead.
	 * </p>
	 */
	public CmfProperty<VALUE> setValue(VALUE value) {
		if (isMultivalued()) {
			this.values.clear();
			this.values.add(value);
		} else {
			this.singleValue = value;
		}
		return this;
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
		if (isMultivalued()) {
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
	public final VALUE removeValue(int idx) {
		idx = sanitizeIndex(idx);
		if (isMultivalued()) { return this.values.remove(idx); }
		VALUE old = this.singleValue;
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
	public final VALUE getValue(int idx) {
		idx = sanitizeIndex(idx);
		if (isMultivalued()) { return this.values.get(idx); }
		return this.singleValue;
	}

	/**
	 * <p>
	 * Identical to {@link #getValue(int)} invoked with {@code idx = 0}.
	 * </p>
	 *
	 * @return the 0-th value in this element.
	 */
	public final VALUE getValue() {
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
	public boolean isSame(CmfProperty<?> other) {
		if (other == null) { return false; }
		if (other == this) { return true; }
		if (!super.equals(other)) { return false; }
		return true;
	}

	public boolean isSameValues(CmfProperty<?> other) {
		if (!isSame(other)) { return false; }
		if (!isMultivalued()) { return Tools.equals(this.singleValue, other.singleValue); }
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
	public final Iterator<VALUE> iterator() {
		if (isMultivalued()) { return this.values.iterator(); }
		return new Iterator<VALUE>() {
			boolean retrieved = false;
			boolean removed = false;

			@Override
			public boolean hasNext() {
				return !this.retrieved;
			}

			@Override
			public VALUE next() {
				if (this.retrieved) { throw new NoSuchElementException(); }
				this.retrieved = true;
				return CmfProperty.this.singleValue;
			}

			@Override
			public void remove() {
				if (!this.retrieved) { throw new IllegalStateException("No element to remove()"); }
				if (this.removed) { throw new IllegalStateException("Element already removed"); }
				CmfProperty.this.singleValue = null;
				this.removed = true;
			}
		};
	}

	@Override
	public String toString() {
		return String.format("CmsProperty [name=%s, type=%s, repeating=%s, %s=%s]", getName(), getType(),
			isMultivalued(), (isMultivalued() ? "values" : "singleValue"),
			(isMultivalued() ? this.values : this.singleValue));
	}
}