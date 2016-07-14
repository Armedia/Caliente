
package com.armedia.cmf.engine.alfresco.bulk.importer.model.jaxb;

import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;
import javax.xml.datatype.XMLGregorianCalendar;

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
 *         &lt;element name="description" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="author" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="published" type="{http://www.w3.org/2001/XMLSchema}date" minOccurs="0"/>
 *         &lt;element name="version" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="imports" minOccurs="0">
 *           &lt;complexType>
 *             &lt;complexContent>
 *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *                 &lt;sequence>
 *                   &lt;element name="import" maxOccurs="unbounded">
 *                     &lt;complexType>
 *                       &lt;complexContent>
 *                         &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *                           &lt;attGroup ref="{http://www.alfresco.org/model/dictionary/1.0}namespaceDefinition"/>
 *                         &lt;/restriction>
 *                       &lt;/complexContent>
 *                     &lt;/complexType>
 *                   &lt;/element>
 *                 &lt;/sequence>
 *               &lt;/restriction>
 *             &lt;/complexContent>
 *           &lt;/complexType>
 *         &lt;/element>
 *         &lt;element name="namespaces">
 *           &lt;complexType>
 *             &lt;complexContent>
 *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *                 &lt;sequence>
 *                   &lt;element name="namespace" maxOccurs="unbounded">
 *                     &lt;complexType>
 *                       &lt;complexContent>
 *                         &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *                           &lt;attGroup ref="{http://www.alfresco.org/model/dictionary/1.0}namespaceDefinition"/>
 *                         &lt;/restriction>
 *                       &lt;/complexContent>
 *                     &lt;/complexType>
 *                   &lt;/element>
 *                 &lt;/sequence>
 *               &lt;/restriction>
 *             &lt;/complexContent>
 *           &lt;/complexType>
 *         &lt;/element>
 *         &lt;element name="data-types" minOccurs="0">
 *           &lt;complexType>
 *             &lt;complexContent>
 *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *                 &lt;sequence>
 *                   &lt;element name="data-type" maxOccurs="unbounded">
 *                     &lt;complexType>
 *                       &lt;complexContent>
 *                         &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *                           &lt;sequence>
 *                             &lt;group ref="{http://www.alfresco.org/model/dictionary/1.0}TextualDescription"/>
 *                             &lt;element name="analyser-class" type="{http://www.w3.org/2001/XMLSchema}anyType" minOccurs="0"/>
 *                             &lt;element name="java-class" type="{http://www.w3.org/2001/XMLSchema}anyType" minOccurs="0"/>
 *                           &lt;/sequence>
 *                           &lt;attGroup ref="{http://www.alfresco.org/model/dictionary/1.0}name"/>
 *                         &lt;/restriction>
 *                       &lt;/complexContent>
 *                     &lt;/complexType>
 *                   &lt;/element>
 *                 &lt;/sequence>
 *               &lt;/restriction>
 *             &lt;/complexContent>
 *           &lt;/complexType>
 *         &lt;/element>
 *         &lt;element name="constraints" minOccurs="0">
 *           &lt;complexType>
 *             &lt;complexContent>
 *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *                 &lt;sequence>
 *                   &lt;element name="constraint" type="{http://www.alfresco.org/model/dictionary/1.0}constraint" maxOccurs="unbounded"/>
 *                 &lt;/sequence>
 *               &lt;/restriction>
 *             &lt;/complexContent>
 *           &lt;/complexType>
 *         &lt;/element>
 *         &lt;element name="types" minOccurs="0">
 *           &lt;complexType>
 *             &lt;complexContent>
 *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *                 &lt;sequence>
 *                   &lt;element name="type" type="{http://www.alfresco.org/model/dictionary/1.0}type" maxOccurs="unbounded"/>
 *                 &lt;/sequence>
 *               &lt;/restriction>
 *             &lt;/complexContent>
 *           &lt;/complexType>
 *         &lt;/element>
 *         &lt;element name="aspects" minOccurs="0">
 *           &lt;complexType>
 *             &lt;complexContent>
 *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *                 &lt;sequence>
 *                   &lt;element name="aspect" type="{http://www.alfresco.org/model/dictionary/1.0}aspect" maxOccurs="unbounded"/>
 *                 &lt;/sequence>
 *               &lt;/restriction>
 *             &lt;/complexContent>
 *           &lt;/complexType>
 *         &lt;/element>
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
	"description", "author", "published", "version", "imports", "namespaces", "dataTypes", "constraints", "types",
	"aspects"
})
@XmlRootElement(name = "model")
public class Model {

	@XmlAttribute(required = true)
	protected String name;

	@XmlElement
	protected String description;

	@XmlElement
	protected String author;

	@XmlSchemaType(name = "date")
	protected XMLGregorianCalendar published;

	@XmlElement
	protected String version;

	@XmlElementWrapper(name = "imports")
	@XmlElement(name = "import", required = true)
	protected List<Namespace> imports;

	@XmlElementWrapper(name = "namespaces")
	@XmlElement(name = "namespace", required = true)
	protected List<Namespace> namespaces;

	@XmlElementWrapper(name = "data-types")
	@XmlElement(name = "data-type", required = true)
	protected List<DataType> dataTypes;

	@XmlElementWrapper(name = "constraints")
	@XmlElement(name = "constraint", required = true)
	protected List<Constraint> constraints;

	@XmlElementWrapper(name = "types")
	@XmlElement(name = "type", required = true)
	protected List<ClassElement> types;

	@XmlElementWrapper(name = "aspects")
	@XmlElement(name = "aspect")
	protected List<ClassElement> aspects;

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
	 * Gets the value of the author property.
	 *
	 * @return possible object is {@link String }
	 *
	 */
	public String getAuthor() {
		return this.author;
	}

	/**
	 * Sets the value of the author property.
	 *
	 * @param value
	 *            allowed object is {@link String }
	 *
	 */
	public void setAuthor(String value) {
		this.author = value;
	}

	/**
	 * Gets the value of the published property.
	 *
	 * @return possible object is {@link XMLGregorianCalendar }
	 *
	 */
	public XMLGregorianCalendar getPublished() {
		return this.published;
	}

	/**
	 * Sets the value of the published property.
	 *
	 * @param value
	 *            allowed object is {@link XMLGregorianCalendar }
	 *
	 */
	public void setPublished(XMLGregorianCalendar value) {
		this.published = value;
	}

	/**
	 * Gets the value of the version property.
	 *
	 * @return possible object is {@link String }
	 *
	 */
	public String getVersion() {
		return this.version;
	}

	/**
	 * Sets the value of the version property.
	 *
	 * @param value
	 *            allowed object is {@link String }
	 *
	 */
	public void setVersion(String value) {
		this.version = value;
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

	public List<Namespace> getImports() {
		return this.imports = ObjectFactory.getList(this.imports);
	}

	public List<Namespace> getNamespaces() {
		return this.namespaces = ObjectFactory.getList(this.namespaces);
	}

	public List<DataType> getDataTypes() {
		return this.dataTypes = ObjectFactory.getList(this.dataTypes);
	}

	public List<Constraint> getConstraints() {
		return this.constraints = ObjectFactory.getList(this.constraints);
	}

	public List<ClassElement> getTypes() {
		return this.types = ObjectFactory.getList(this.types);
	}

	public List<ClassElement> getAspects() {
		return this.aspects = ObjectFactory.getList(this.aspects);
	}
}