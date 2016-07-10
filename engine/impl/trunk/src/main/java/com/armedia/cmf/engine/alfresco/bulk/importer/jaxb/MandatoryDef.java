
package com.armedia.cmf.engine.alfresco.bulk.importer.jaxb;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;

/**
 * <p>
 * Java class for mandatoryDef complex type.
 *
 * <p>
 * The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType name="mandatoryDef">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;attribute name="enforced" type="{http://www.w3.org/2001/XMLSchema}boolean" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 *
 *
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "mandatoryDef", propOrder = {})
public class MandatoryDef {

	@XmlAttribute
	protected Boolean enforced;

	/**
	 * Gets the value of the enforced property.
	 *
	 * @return possible object is {@link Boolean }
	 *
	 */
	public Boolean getEnforced() {
		return this.enforced;
	}

	/**
	 * Sets the value of the enforced property.
	 *
	 * @param value
	 *            allowed object is {@link Boolean }
	 *
	 */
	public void setEnforced(Boolean value) {
		this.enforced = value;
	}
}