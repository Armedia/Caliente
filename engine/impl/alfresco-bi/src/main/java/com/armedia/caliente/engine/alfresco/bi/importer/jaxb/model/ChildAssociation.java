
package com.armedia.caliente.engine.alfresco.bi.importer.jaxb.model;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "childAssociation", propOrder = {
	"childName", "duplicate", "propagateTimestamps"
})
public class ChildAssociation extends Association {

	@XmlElement(name = "child-name")
	protected String childName;

	@XmlElement
	protected Boolean duplicate;

	@XmlElement
	protected Boolean propagateTimestamps;

	/**
	 * Gets the value of the childName property.
	 *
	 * @return possible object is {@link String }
	 *
	 */
	public String getChildName() {
		return this.childName;
	}

	/**
	 * Sets the value of the childName property.
	 *
	 * @param value
	 *            allowed object is {@link String }
	 *
	 */
	public void setChildName(String value) {
		this.childName = value;
	}

	/**
	 * Gets the value of the duplicate property.
	 *
	 * @return possible object is {@link Boolean }
	 *
	 */
	public Boolean getDuplicate() {
		return this.duplicate;
	}

	/**
	 * Sets the value of the duplicate property.
	 *
	 * @param value
	 *            allowed object is {@link Boolean }
	 *
	 */
	public void setDuplicate(Boolean value) {
		this.duplicate = value;
	}

	/**
	 * Gets the value of the propagateTimestamps property.
	 *
	 * @return possible object is {@link Boolean }
	 *
	 */
	public Boolean getPropagateTimestamps() {
		return this.propagateTimestamps;
	}

	/**
	 * Sets the value of the propagateTimestamps property.
	 *
	 * @param value
	 *            allowed object is {@link Boolean }
	 *
	 */
	public void setPropagateTimestamps(Boolean value) {
		this.propagateTimestamps = value;
	}

}