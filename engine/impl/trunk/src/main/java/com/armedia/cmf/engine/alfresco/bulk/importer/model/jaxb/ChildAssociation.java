
package com.armedia.cmf.engine.alfresco.bulk.importer.model.jaxb;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

/**
 * <p>
 * Java class for childAssociation complex type.
 *
 * <p>
 * The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType name="childAssociation">
 *   &lt;complexContent>
 *     &lt;extension base="{http://www.alfresco.org/model/dictionary/1.0}association">
 *       &lt;sequence>
 *         &lt;element name="child-name" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="duplicate" type="{http://www.w3.org/2001/XMLSchema}boolean" minOccurs="0"/>
 *         &lt;element name="propagateTimestamps" type="{http://www.w3.org/2001/XMLSchema}boolean" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 *
 *
 */
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