
package com.armedia.caliente.engine.alfresco.bi.importer.jaxb.model;

import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "propertyOverride", propOrder = {
	"mandatory", "_default", "constraints"
})
public class PropertyOverride {

	@XmlElement
	protected MandatoryDef mandatory;

	@XmlElement(name = "default")
	protected String _default;

	@XmlElementWrapper(name = "constraints")
	@XmlElement(name = "constraint")
	protected List<Constraint> constraints;

	@XmlAttribute(required = true)
	protected String name;

	/**
	 * Gets the value of the mandatory property.
	 *
	 * @return possible object is {@link MandatoryDef }
	 *
	 */
	public MandatoryDef getMandatory() {
		return this.mandatory;
	}

	/**
	 * Sets the value of the mandatory property.
	 *
	 * @param value
	 *            allowed object is {@link MandatoryDef }
	 *
	 */
	public void setMandatory(MandatoryDef value) {
		this.mandatory = value;
	}

	/**
	 * Gets the value of the default property.
	 *
	 * @return possible object is {@link String }
	 *
	 */
	public String getDefault() {
		return this._default;
	}

	/**
	 * Sets the value of the default property.
	 *
	 * @param value
	 *            allowed object is {@link String }
	 *
	 */
	public void setDefault(String value) {
		this._default = value;
	}

	public List<Constraint> getConstraints() {
		return this.constraints = ObjectFactory.getList(this.constraints);
	}

	/**
	 * Gets the value of the name property.
	 *
	 * @return possible object is {@link String }
	 *
	 */
	public String getName() {
		return this.name;
	}

	/**
	 * Sets the value of the name property.
	 *
	 * @param value
	 *            allowed object is {@link String }
	 *
	 */
	public void setName(String value) {
		this.name = value;
	}
}