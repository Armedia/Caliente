package com.armedia.caliente.store.local;

import java.util.List;

public interface XmlStoreProperties<P extends XmlProperty> {

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
	public List<P> getProperty();
}