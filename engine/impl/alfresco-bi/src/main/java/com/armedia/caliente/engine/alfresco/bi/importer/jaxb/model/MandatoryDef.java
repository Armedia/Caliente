
package com.armedia.caliente.engine.alfresco.bi.importer.jaxb.model;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlValue;

import com.armedia.commons.utilities.Tools;

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