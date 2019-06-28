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
package com.armedia.caliente.engine.xml.importer.jaxb;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.xml.bind.annotation.XmlTransient;

import com.armedia.commons.utilities.concurrent.ShareableList;

@XmlTransient
public class AggregatorBase<T> {

	private final String label;

	protected final List<T> items = new ShareableList<>(new ArrayList<T>());

	protected AggregatorBase() {
		throw new UnsupportedOperationException("This constructor should NEVER be called");
	}

	protected AggregatorBase(String label) {
		this.label = label;
	}

	protected final List<T> getItems() {
		return this.items;
	}

	public final boolean add(Collection<T> item) {
		return this.items.addAll(item);
	}

	public final boolean add(T item) {
		return this.items.add(item);
	}

	public final boolean remove(Collection<T> item) {
		return this.items.removeAll(item);
	}

	public final boolean remove(T item) {
		return this.items.remove(item);
	}

	public final boolean containsAll(Collection<T> item) {
		return this.items.containsAll(item);
	}

	public final boolean contains(T item) {
		return this.items.contains(item);
	}

	public final void clear() {
		this.items.clear();
	}

	public final int getCount() {
		return this.items.size();
	}

	@Override
	public final String toString() {
		return String.format("%s [%s=%s]", getClass().getSimpleName(), this.label, this.items);
	}
}