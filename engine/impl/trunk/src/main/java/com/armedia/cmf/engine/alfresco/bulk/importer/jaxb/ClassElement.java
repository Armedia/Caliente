
package com.armedia.cmf.engine.alfresco.bulk.importer.jaxb;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlType;

/**
 * <p>
 * Java class for class complex type.
 *
 * <p>
 * The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType name="class">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;group ref="{http://www.alfresco.org/model/dictionary/1.0}TextualDescription"/>
 *         &lt;element name="parent" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="archive" type="{http://www.w3.org/2001/XMLSchema}boolean" minOccurs="0"/>
 *         &lt;element name="includedInSuperTypeQuery" type="{http://www.w3.org/2001/XMLSchema}boolean" minOccurs="0"/>
 *         &lt;element name="properties" minOccurs="0">
 *           &lt;complexType>
 *             &lt;complexContent>
 *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *                 &lt;sequence>
 *                   &lt;element name="property" type="{http://www.alfresco.org/model/dictionary/1.0}property" maxOccurs="unbounded" minOccurs="0"/>
 *                 &lt;/sequence>
 *               &lt;/restriction>
 *             &lt;/complexContent>
 *           &lt;/complexType>
 *         &lt;/element>
 *         &lt;element name="associations" minOccurs="0">
 *           &lt;complexType>
 *             &lt;complexContent>
 *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *                 &lt;sequence>
 *                   &lt;element name="association" type="{http://www.alfresco.org/model/dictionary/1.0}association" maxOccurs="unbounded" minOccurs="0"/>
 *                   &lt;element name="child-association" type="{http://www.alfresco.org/model/dictionary/1.0}childAssociation" maxOccurs="unbounded" minOccurs="0"/>
 *                 &lt;/sequence>
 *               &lt;/restriction>
 *             &lt;/complexContent>
 *           &lt;/complexType>
 *         &lt;/element>
 *         &lt;element name="overrides" minOccurs="0">
 *           &lt;complexType>
 *             &lt;complexContent>
 *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *                 &lt;sequence>
 *                   &lt;element name="property" type="{http://www.alfresco.org/model/dictionary/1.0}propertyOverride" maxOccurs="unbounded"/>
 *                 &lt;/sequence>
 *               &lt;/restriction>
 *             &lt;/complexContent>
 *           &lt;/complexType>
 *         &lt;/element>
 *         &lt;element name="mandatory-aspects" minOccurs="0">
 *           &lt;complexType>
 *             &lt;complexContent>
 *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *                 &lt;sequence>
 *                   &lt;element name="aspect" type="{http://www.w3.org/2001/XMLSchema}string" maxOccurs="unbounded"/>
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
@XmlType(name = "class", propOrder = {
	"title", "description", "parent", "archive", "includedInSuperTypeQuery", "properties", "associations", "overrides",
	"mandatoryAspects"
})
@XmlSeeAlso({
	Aspect.class, Type.class
})
public class ClassElement {

	protected String title;
	protected String description;
	protected String parent;
	protected Boolean archive;
	protected Boolean includedInSuperTypeQuery;
	protected ClassElement.Properties properties;
	protected ClassElement.Associations associations;
	protected ClassElement.Overrides overrides;
	@XmlElement(name = "mandatory-aspects")
	protected ClassElement.MandatoryAspects mandatoryAspects;
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
	 * Gets the value of the parent property.
	 *
	 * @return possible object is {@link String }
	 *
	 */
	public String getParent() {
		return this.parent;
	}

	/**
	 * Sets the value of the parent property.
	 *
	 * @param value
	 *            allowed object is {@link String }
	 *
	 */
	public void setParent(String value) {
		this.parent = value;
	}

	/**
	 * Gets the value of the archive property.
	 *
	 * @return possible object is {@link Boolean }
	 *
	 */
	public Boolean getArchive() {
		return this.archive;
	}

	/**
	 * Sets the value of the archive property.
	 *
	 * @param value
	 *            allowed object is {@link Boolean }
	 *
	 */
	public void setArchive(Boolean value) {
		this.archive = value;
	}

	/**
	 * Gets the value of the includedInSuperTypeQuery property.
	 *
	 * @return possible object is {@link Boolean }
	 *
	 */
	public Boolean getIncludedInSuperTypeQuery() {
		return this.includedInSuperTypeQuery;
	}

	/**
	 * Sets the value of the includedInSuperTypeQuery property.
	 *
	 * @param value
	 *            allowed object is {@link Boolean }
	 *
	 */
	public void setIncludedInSuperTypeQuery(Boolean value) {
		this.includedInSuperTypeQuery = value;
	}

	/**
	 * Gets the value of the properties property.
	 *
	 * @return possible object is {@link ClassElement.Properties }
	 *
	 */
	public ClassElement.Properties getProperties() {
		return this.properties;
	}

	/**
	 * Sets the value of the properties property.
	 *
	 * @param value
	 *            allowed object is {@link ClassElement.Properties }
	 *
	 */
	public void setProperties(ClassElement.Properties value) {
		this.properties = value;
	}

	/**
	 * Gets the value of the associations property.
	 *
	 * @return possible object is {@link ClassElement.Associations }
	 *
	 */
	public ClassElement.Associations getAssociations() {
		return this.associations;
	}

	/**
	 * Sets the value of the associations property.
	 *
	 * @param value
	 *            allowed object is {@link ClassElement.Associations }
	 *
	 */
	public void setAssociations(ClassElement.Associations value) {
		this.associations = value;
	}

	/**
	 * Gets the value of the overrides property.
	 *
	 * @return possible object is {@link ClassElement.Overrides }
	 *
	 */
	public ClassElement.Overrides getOverrides() {
		return this.overrides;
	}

	/**
	 * Sets the value of the overrides property.
	 *
	 * @param value
	 *            allowed object is {@link ClassElement.Overrides }
	 *
	 */
	public void setOverrides(ClassElement.Overrides value) {
		this.overrides = value;
	}

	/**
	 * Gets the value of the mandatoryAspects property.
	 *
	 * @return possible object is {@link ClassElement.MandatoryAspects }
	 *
	 */
	public ClassElement.MandatoryAspects getMandatoryAspects() {
		return this.mandatoryAspects;
	}

	/**
	 * Sets the value of the mandatoryAspects property.
	 *
	 * @param value
	 *            allowed object is {@link ClassElement.MandatoryAspects }
	 *
	 */
	public void setMandatoryAspects(ClassElement.MandatoryAspects value) {
		this.mandatoryAspects = value;
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
	 *         &lt;element name="association" type="{http://www.alfresco.org/model/dictionary/1.0}association" maxOccurs="unbounded" minOccurs="0"/>
	 *         &lt;element name="child-association" type="{http://www.alfresco.org/model/dictionary/1.0}childAssociation" maxOccurs="unbounded" minOccurs="0"/>
	 *       &lt;/sequence>
	 *     &lt;/restriction>
	 *   &lt;/complexContent>
	 * &lt;/complexType>
	 * </pre>
	 *
	 *
	 */
	@XmlAccessorType(XmlAccessType.FIELD)
	@XmlType(name = "", propOrder = {
		"association", "childAssociation"
	})
	public static class Associations {

		protected List<Association> association;
		@XmlElement(name = "child-association")
		protected List<ChildAssociation> childAssociation;

		/**
		 * Gets the value of the association property.
		 *
		 * <p>
		 * This accessor method returns a reference to the live list, not a snapshot. Therefore any
		 * modification you make to the returned list will be present inside the JAXB object. This
		 * is why there is not a <CODE>set</CODE> method for the association property.
		 *
		 * <p>
		 * For example, to add a new item, do as follows:
		 *
		 * <pre>
		 * getAssociation().add(newItem);
		 * </pre>
		 *
		 *
		 * <p>
		 * Objects of the following type(s) are allowed in the list {@link Association }
		 *
		 *
		 */
		public List<Association> getAssociation() {
			if (this.association == null) {
				this.association = new ArrayList<Association>();
			}
			return this.association;
		}

		/**
		 * Gets the value of the childAssociation property.
		 *
		 * <p>
		 * This accessor method returns a reference to the live list, not a snapshot. Therefore any
		 * modification you make to the returned list will be present inside the JAXB object. This
		 * is why there is not a <CODE>set</CODE> method for the childAssociation property.
		 *
		 * <p>
		 * For example, to add a new item, do as follows:
		 *
		 * <pre>
		 * getChildAssociation().add(newItem);
		 * </pre>
		 *
		 *
		 * <p>
		 * Objects of the following type(s) are allowed in the list {@link ChildAssociation }
		 *
		 *
		 */
		public List<ChildAssociation> getChildAssociation() {
			if (this.childAssociation == null) {
				this.childAssociation = new ArrayList<ChildAssociation>();
			}
			return this.childAssociation;
		}

	}

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
	 *         &lt;element name="aspect" type="{http://www.w3.org/2001/XMLSchema}string" maxOccurs="unbounded"/>
	 *       &lt;/sequence>
	 *     &lt;/restriction>
	 *   &lt;/complexContent>
	 * &lt;/complexType>
	 * </pre>
	 *
	 *
	 */
	@XmlAccessorType(XmlAccessType.FIELD)
	@XmlType(name = "", propOrder = {
		"aspect"
	})
	public static class MandatoryAspects {

		@XmlElement(required = true)
		protected List<String> aspect;

		/**
		 * Gets the value of the aspect property.
		 *
		 * <p>
		 * This accessor method returns a reference to the live list, not a snapshot. Therefore any
		 * modification you make to the returned list will be present inside the JAXB object. This
		 * is why there is not a <CODE>set</CODE> method for the aspect property.
		 *
		 * <p>
		 * For example, to add a new item, do as follows:
		 *
		 * <pre>
		 * getAspect().add(newItem);
		 * </pre>
		 *
		 *
		 * <p>
		 * Objects of the following type(s) are allowed in the list {@link String }
		 *
		 *
		 */
		public List<String> getAspect() {
			if (this.aspect == null) {
				this.aspect = new ArrayList<String>();
			}
			return this.aspect;
		}

	}

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
	 *         &lt;element name="property" type="{http://www.alfresco.org/model/dictionary/1.0}propertyOverride" maxOccurs="unbounded"/>
	 *       &lt;/sequence>
	 *     &lt;/restriction>
	 *   &lt;/complexContent>
	 * &lt;/complexType>
	 * </pre>
	 *
	 *
	 */
	@XmlAccessorType(XmlAccessType.FIELD)
	@XmlType(name = "", propOrder = {
		"property"
	})
	public static class Overrides {

		@XmlElement(required = true)
		protected List<PropertyOverride> property;

		/**
		 * Gets the value of the property property.
		 *
		 * <p>
		 * This accessor method returns a reference to the live list, not a snapshot. Therefore any
		 * modification you make to the returned list will be present inside the JAXB object. This
		 * is why there is not a <CODE>set</CODE> method for the property property.
		 *
		 * <p>
		 * For example, to add a new item, do as follows:
		 *
		 * <pre>
		 * getProperty().add(newItem);
		 * </pre>
		 *
		 *
		 * <p>
		 * Objects of the following type(s) are allowed in the list {@link PropertyOverride }
		 *
		 *
		 */
		public List<PropertyOverride> getProperty() {
			if (this.property == null) {
				this.property = new ArrayList<PropertyOverride>();
			}
			return this.property;
		}

	}

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
	 *         &lt;element name="property" type="{http://www.alfresco.org/model/dictionary/1.0}property" maxOccurs="unbounded" minOccurs="0"/>
	 *       &lt;/sequence>
	 *     &lt;/restriction>
	 *   &lt;/complexContent>
	 * &lt;/complexType>
	 * </pre>
	 *
	 *
	 */
	@XmlAccessorType(XmlAccessType.FIELD)
	@XmlType(name = "", propOrder = {
		"property"
	})
	public static class Properties {

		protected List<Property> property;

		/**
		 * Gets the value of the property property.
		 *
		 * <p>
		 * This accessor method returns a reference to the live list, not a snapshot. Therefore any
		 * modification you make to the returned list will be present inside the JAXB object. This
		 * is why there is not a <CODE>set</CODE> method for the property property.
		 *
		 * <p>
		 * For example, to add a new item, do as follows:
		 *
		 * <pre>
		 * getProperty().add(newItem);
		 * </pre>
		 *
		 *
		 * <p>
		 * Objects of the following type(s) are allowed in the list {@link Property }
		 *
		 *
		 */
		public List<Property> getProperty() {
			if (this.property == null) {
				this.property = new ArrayList<Property>();
			}
			return this.property;
		}

	}

}