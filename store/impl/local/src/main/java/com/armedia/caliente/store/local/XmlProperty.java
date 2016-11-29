package com.armedia.caliente.store.local;

import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlValue;

import com.armedia.caliente.store.CmfDataType;

@XmlTransient
public abstract class XmlProperty {

	@XmlValue
	protected String value;

	@XmlAttribute(name = "name", required = true)
	protected String name;

	@XmlAttribute(name = "type", required = true)
	protected String type;

	@XmlTransient
	private CmfDataType dataType;

	protected void beforeMarshal(Marshaller m) {
		this.type = this.dataType.name();
	}

	protected void afterUnmarshal(Unmarshaller u, Object parent) {
		this.dataType = CmfDataType.valueOf(this.type);
	}

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
	 * @return possible object is {@link CmfDataType }
	 *
	 */
	public CmfDataType getType() {
		return this.dataType;
	}

	/**
	 * Sets the value of the type property.
	 *
	 * @param value
	 *            allowed object is {@link CmfDataType }
	 *
	 */
	public void setType(CmfDataType value) {
		this.dataType = value;
	}
}