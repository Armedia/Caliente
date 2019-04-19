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