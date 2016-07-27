
package com.armedia.cmf.engine.alfresco.bulk.importer.model.jaxb;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlValue;

import com.armedia.commons.utilities.Tools;

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
@XmlType(name = "mandatoryDef", propOrder = {
	"value"
})
public class MandatoryDef {

	@XmlAttribute
	protected Boolean enforced = Boolean.FALSE;

	@XmlValue
	protected String value = Boolean.FALSE.toString();

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
		this.enforced = Tools.coalesce(value, Boolean.FALSE);
	}

	public Boolean getValue() {
		return Boolean.valueOf(this.value);
	}

	public void setValue(Boolean value) {
		this.value = Tools.coalesce(value, Boolean.FALSE).toString();
	}
}