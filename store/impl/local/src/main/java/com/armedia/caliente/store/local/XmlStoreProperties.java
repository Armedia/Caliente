package com.armedia.caliente.store.local;

import java.util.ArrayList;
import java.util.List;

public abstract class XmlStoreProperties<P extends XmlProperty> {

	protected List<P> property;

	/**
	 * Gets the value of the property property.
	 *
	 * <p>
	 * This accessor method returns a reference to the live list, not a snapshot. Therefore any
	 * modification you make to the returned list will be present inside the JAXB object. This is
	 * why there is not a <CODE>set</CODE> method for the property property.
	 *
	 * <p>
	 * For example, to add a new item, do as follows:
	 *
	 * <pre>
	 * getProperty().add(newItem);
	 * </pre>
	 *
	 */
	public List<P> getProperty() {
		if (this.property == null) {
			this.property = new ArrayList<>();
		}
		return this.property;
	}
}