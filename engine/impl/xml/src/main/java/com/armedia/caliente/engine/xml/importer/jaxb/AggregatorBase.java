package com.armedia.caliente.engine.xml.importer.jaxb;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.xml.bind.annotation.XmlTransient;

import com.armedia.commons.utilities.concurrent.ShareableList;

@XmlTransient
public class AggregatorBase<T> {

	private final String label;

	protected List<T> items;

	protected AggregatorBase(String label) {
		this.label = label;
	}

	protected final synchronized List<T> getItems() {
		if (this.items == null) {
			this.items = new ShareableList<>(new ArrayList<T>());
		}
		return this.items;
	}

	public final boolean add(Collection<T> item) {
		return getItems().addAll(item);
	}

	public final boolean add(T item) {
		return getItems().add(item);
	}

	public final boolean remove(Collection<T> item) {
		return getItems().removeAll(item);
	}

	public final boolean remove(T item) {
		return getItems().remove(item);
	}

	public final boolean containsAll(Collection<T> item) {
		return getItems().containsAll(item);
	}

	public final boolean contains(T item) {
		return getItems().contains(item);
	}

	public final void clear() {
		getItems().clear();
	}

	public final int getCount() {
		return getItems().size();
	}

	@Override
	public final synchronized String toString() {
		return String.format("%s [%s=%s]", getClass().getSimpleName(), this.label, getItems());
	}
}