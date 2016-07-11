package com.armedia.cmf.engine.alfresco.bulk.importer.jaxb;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

/**
 * <p>
 * Java class for anonymous complex type.
 *
 * <p>
 * The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType>
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;group ref="{http://www.alfresco.org/model/dictionary/1.0}TextualDescription"/>
 *         &lt;element name="analyser-class" type="{http://www.w3.org/2001/XMLSchema}anyType" minOccurs="0"/>
 *         &lt;element name="java-class" type="{http://www.w3.org/2001/XMLSchema}anyType" minOccurs="0"/>
 *       &lt;/sequence>
 *       &lt;attGroup ref="{http://www.alfresco.org/model/dictionary/1.0}name"/>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 *
 *
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
	"title", "description", "analyserClass", "javaClass"
})
public class DataType {

	@XmlElement
	protected String title;

	@XmlElement
	protected String description;

	@XmlElement(name = "analyser-class")
	protected Object analyserClass;

	@XmlElement(name = "java-class")
	protected Object javaClass;

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
	public Object getAnalyserClass() {
		return this.analyserClass;
	}

	/**
	 * Sets the value of the analyserClass property.
	 *
	 * @param value
	 *            allowed object is {@link Object }
	 *
	 */
	public void setAnalyserClass(Object value) {
		this.analyserClass = value;
	}

	/**
	 * Gets the value of the javaClass property.
	 *
	 * @return possible object is {@link Object }
	 *
	 */
	public Object getJavaClass() {
		return this.javaClass;
	}

	/**
	 * Sets the value of the javaClass property.
	 *
	 * @param value
	 *            allowed object is {@link Object }
	 *
	 */
	public void setJavaClass(Object value) {
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