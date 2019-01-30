package com.armedia.caliente.store.local;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlValue;

import com.armedia.caliente.store.CmfValue;
import com.armedia.caliente.store.CmfValue.Type;

@XmlTransient
public abstract class XmlProperty {

	@XmlAttribute(name = "name", required = true)
	protected String name;

	@XmlAttribute(name = "type", required = true)
	protected String type;

	@XmlTransient
	protected CmfValue.Type dataType;

	@XmlValue
	protected String value;

	/**
	 * Gets the value of the value property.
	 *
	 * @return possible object is {@link String }
	 *
	 */
	public String getValue() {
		return this.value;
	}

	/**
	 * Sets the value of the value property.
	 *
	 * @param value
	 *            allowed object is {@link String }
	 *
	 */
	public void setValue(String value) {
		this.value = value;
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

	/**
	 * Gets the value of the type property.
	 *
	 * @return possible object is {@link Type }
	 *
	 */
	public CmfValue.Type getType() {
		return this.dataType;
	}

	/**
	 * Sets the value of the type property.
	 *
	 * @param value
	 *            allowed object is {@link Type }
	 *
	 */
	public void setType(CmfValue.Type value) {
		this.dataType = value;
	}
}