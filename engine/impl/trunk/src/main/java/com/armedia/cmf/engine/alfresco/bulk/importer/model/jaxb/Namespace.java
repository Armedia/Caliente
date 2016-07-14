package com.armedia.cmf.engine.alfresco.bulk.importer.model.jaxb;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
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
 *       &lt;attGroup ref="{http://www.alfresco.org/model/dictionary/1.0}namespaceDefinition"/>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 *
 *
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "")
public class Namespace {

	@XmlAttribute(required = true)
	protected String uri;

	@XmlAttribute(required = true)
	protected String prefix;

	/**
	 * Gets the value of the uri property.
	 *
	 * @return possible object is {@link String }
	 *
	 */
	public String getUri() {
		return this.uri;
	}

	/**
	 * Sets the value of the uri property.
	 *
	 * @param value
	 *            allowed object is {@link String }
	 *
	 */
	public void setUri(String value) {
		this.uri = value;
	}

	/**
	 * Gets the value of the prefix property.
	 *
	 * @return possible object is {@link String }
	 *
	 */
	public String getPrefix() {
		return this.prefix;
	}

	/**
	 * Sets the value of the prefix property.
	 *
	 * @param value
	 *            allowed object is {@link String }
	 *
	 */
	public void setPrefix(String value) {
		this.prefix = value;
	}

}