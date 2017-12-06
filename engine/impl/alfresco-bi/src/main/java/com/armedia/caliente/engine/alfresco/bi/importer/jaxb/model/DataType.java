package com.armedia.caliente.engine.alfresco.bi.importer.jaxb.model;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
	"title", "description", "defaultAnalyserClass", "javaClass"
})
public class DataType {

	@XmlElement
	protected String title;

	@XmlElement
	protected String description;

	@XmlElement(name = "default-analyser-class")
	protected String defaultAnalyserClass;

	@XmlElement(name = "java-class")
	protected String javaClass;

	@XmlAttribute(required = true)
	protected String name;

	/**
	 * Gets the value of the title property.
	 *
	 * @return possible object is {@link String }
	 *
	 */
	public String getTitle() {
		return this.title;
	}

	/**
	 * Sets the value of the title property.
	 *
	 * @param value
	 *            allowed object is {@link String }
	 *
	 */
	public void setTitle(String value) {
		this.title = value;
	}

	/**
	 * Gets the value of the description property.
	 *
	 * @return possible object is {@link String }
	 *
	 */
	public String getDescription() {
		return this.description;
	}

	/**
	 * Sets the value of the description property.
	 *
	 * @param value
	 *            allowed object is {@link String }
	 *
	 */
	public void setDescription(String value) {
		this.description = value;
	}

	/**
	 * Gets the value of the analyserClass property.
	 *
	 * @return possible object is {@link Object }
	 *
	 */
	public String getAnalyserClass() {
		return this.defaultAnalyserClass;
	}

	/**
	 * Sets the value of the analyserClass property.
	 *
	 * @param value
	 *            allowed object is {@link Object }
	 *
	 */
	public void setAnalyserClass(String value) {
		this.defaultAnalyserClass = value;
	}

	/**
	 * Gets the value of the javaClass property.
	 *
	 * @return possible object is {@link Object }
	 *
	 */
	public String getJavaClass() {
		return this.javaClass;
	}

	/**
	 * Sets the value of the javaClass property.
	 *
	 * @param value
	 *            allowed object is {@link Object }
	 *
	 */
	public void setJavaClass(String value) {
		this.javaClass = value;
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