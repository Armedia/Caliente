
package com.armedia.caliente.engine.alfresco.bi.importer.jaxb.model;

import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "class", propOrder = {
	"title", "description", "parent", "archive", "includedInSuperTypeQuery", "properties", "associations", "overrides",
	"mandatoryAspects"
})
public class ClassElement {

	@XmlAttribute(required = true)
	protected String name;

	@XmlElement
	protected String title;

	@XmlElement
	protected String description;

	@XmlElement
	protected String parent;

	@XmlElement
	protected Boolean archive;

	@XmlElement
	protected Boolean includedInSuperTypeQuery;

	@XmlElementWrapper(name = "properties")
	@XmlElement(name = "property", required = true)
	protected List<Property> properties;

	@XmlElement
	protected ClassElement.Associations associations;

	@XmlElementWrapper(name = "overrides")
	@XmlElement(name = "property", required = true)
	protected List<PropertyOverride> overrides;

	@XmlElementWrapper(name = "mandatory-aspects")
	@XmlElement(name = "aspect", required = true)
	protected List<String> mandatoryAspects;

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

	public List<Property> getProperties() {
		return this.properties = ObjectFactory.getList(this.properties);
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

	public List<PropertyOverride> getOverrides() {
		return this.overrides = ObjectFactory.getList(this.overrides);
	}

	public List<String> getMandatoryAspects() {
		return this.mandatoryAspects = ObjectFactory.getList(this.mandatoryAspects);
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

	@XmlAccessorType(XmlAccessType.FIELD)
	@XmlType(name = "", propOrder = {
		"association", "childAssociation"
	})
	public static class Associations {

		@XmlElement
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
			return this.association = ObjectFactory.getList(this.association);
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
			return this.childAssociation = ObjectFactory.getList(this.childAssociation);
		}
	}
}