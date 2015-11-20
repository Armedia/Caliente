package com.armedia.cmf.engine.xml.importer.jaxb;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlTransient;

@XmlTransient
public class AggregatorBase<T> {

	private final String label;

	protected List<T> items;

	protected AggregatorBase(String label) {
		this.label = label;
	}

	protected final synchronized List<T> getItems() {
		if (this.items == null) {
			this.items = new ArrayList<T>();
		}
		return this.items;
	}

	public final synchronized boolean add(T item) {
		return getItems().add(item);
	}

	public final synchronized boolean remove(T item) {
		return getItems().remove(item);
	}

	public final synchronized boolean contains(T item) {
		return getItems().contains(item);
	}

	public final synchronized void clear() {
		getItems().clear();
	}

	public final synchronized int getCount() {
		return getItems().size();
	}

	@Override
	public final synchronized String toString() {
		return String.format("%s [%s=%s]", getClass().getSimpleName(), this.label, getItems());
	}
}