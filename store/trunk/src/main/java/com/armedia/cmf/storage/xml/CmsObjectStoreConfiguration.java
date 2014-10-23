package com.armedia.cmf.storage.xml;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "objectstore.t", propOrder = {
	"className", "setting"
})
public class CmsObjectStoreConfiguration extends SettingContainer {

	@XmlElement(required = true)
	protected String className;

	@XmlAttribute(name = "id", required = true)
	protected String id;

	/**
	 * Gets the value of the className property.
	 *
	 * @return possible object is {@link String }
	 *
	 */
	public String getClassName() {
		return this.className;
	}

	/**
	 * Sets the value of the className property.
	 *
	 * @param value
	 *            allowed object is {@link String }
	 *
	 */
	public void setClassName(String value) {
		this.className = value;
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
}