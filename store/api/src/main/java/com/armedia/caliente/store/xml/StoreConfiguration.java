package com.armedia.caliente.store.xml;

import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "store.t", propOrder = {
	"parent", "type", "prep", "setting"
})
public class StoreConfiguration extends SettingContainer {

	@XmlElement(name = "parent", required = false)
	protected String parent;

	@XmlElement(required = true)
	protected String type;

	@XmlElement(required = false)
	protected String prep;

	@XmlAttribute(name = "id", required = true)
	protected String id;

	@Override
	protected void afterUnmarshal(Unmarshaller unmarshaller, Object parent) {
		super.afterUnmarshal(unmarshaller, parent);
	}

	@Override
	protected void beforeMarshal(Marshaller marshaller) {
		super.beforeMarshal(marshaller);
	}

	public String getParentName() {
		return this.parent;
	}

	public void setParentName(String parent) {
		this.parent = parent;
	}

	/**
	 * Gets the value of the className property.
	 *
	 * @return possible object is {@link String }
	 *
	 */
	public String getType() {
		return this.type;
	}

	/**
	 * Sets the value of the className property.
	 *
	 * @param value
	 *            allowed object is {@link String }
	 *
	 */
	public void setType(String value) {
		this.type = value;
	}

	public String getPrep() {
		return this.prep;
	}

	public void setPrep(String prep) {
		this.prep = prep;
	}

	/**
	 * Gets the value of the id property.
	 *
	 * @return possible object is {@link String }
	 *
	 */
	public String getId() {
		return this.id;
	}

	/**
	 * Sets the value of the id property.
	 *
	 * @param value
	 *            allowed object is {@link String }
	 *
	 */
	public void setId(String value) {
		this.id = value;
	}

	@Override
	public StoreConfiguration clone() {
		return StoreConfiguration.class.cast(super.clone());
	}
}