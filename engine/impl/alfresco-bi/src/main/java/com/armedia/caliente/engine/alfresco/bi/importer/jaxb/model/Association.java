
package com.armedia.caliente.engine.alfresco.bi.importer.jaxb.model;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "association", propOrder = {
	"title", "description", "source", "target"
})
@XmlSeeAlso({
	ChildAssociation.class
})
public class Association {

	@XmlAttribute(required = true)
	protected String name;

	@XmlElement
	protected String title;

	@XmlElement
	protected String description;

	@XmlElement
	protected Association.Source source;

	@XmlElement(required = true)
	protected Association.Target target;

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
	 * Gets the value of the source property.
	 *
	 * @return possible object is {@link Association.Source }
	 *
	 */
	public Association.Source getSource() {
		return this.source;
	}

	/**
	 * Sets the value of the source property.
	 *
	 * @param value
	 *            allowed object is {@link Association.Source }
	 *
	 */
	public void setSource(Association.Source value) {
		this.source = value;
	}

	/**
	 * Gets the value of the target property.
	 *
	 * @return possible object is {@link Association.Target }
	 *
	 */
	public Association.Target getTarget() {
		return this.target;
	}

	/**
	 * Sets the value of the target property.
	 *
	 * @param value
	 *            allowed object is {@link Association.Target }
	 *
	 */
	public void setTarget(Association.Target value) {
		this.target = value;
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

	@XmlTransient
	protected static class Endpoint {

		@XmlElement
		protected String role;

		@XmlElement
		protected Boolean mandatory;

		@XmlElement
		protected Boolean many;

		/**
		 * Gets the value of the role property.
		 *
		 * @return possible object is {@link String }
		 *
		 */
		public String getRole() {
			return this.role;
		}

		/**
		 * Sets the value of the role property.
		 *
		 * @param value
		 *            allowed object is {@link String }
		 *
		 */
		public void setRole(String value) {
			this.role = value;
		}

		/**
		 * Gets the value of the mandatory property.
		 *
		 * @return possible object is {@link Boolean }
		 *
		 */
		public Boolean getMandatory() {
			return this.mandatory;
		}

		/**
		 * Sets the value of the mandatory property.
		 *
		 * @param value
		 *            allowed object is {@link Boolean }
		 *
		 */
		public void setMandatory(Boolean value) {
			this.mandatory = value;
		}

		/**
		 * Gets the value of the many property.
		 *
		 * @return possible object is {@link Boolean }
		 *
		 */
		public Boolean getMany() {
			return this.many;
		}

		/**
		 * Sets the value of the many property.
		 *
		 * @param value
		 *            allowed object is {@link Boolean }
		 *
		 */
		public void setMany(Boolean value) {
			this.many = value;
		}
	}

	@XmlAccessorType(XmlAccessType.FIELD)
	@XmlType(name = "", propOrder = {
		"role", "mandatory", "many"
	})
	public static class Source extends Endpoint {
	}

	@XmlAccessorType(XmlAccessType.FIELD)
	@XmlType(name = "", propOrder = {
		"clazz", "role", "mandatory", "many"
	})
	public static class Target extends Endpoint {

		@XmlElement(name = "class", required = true)
		protected String clazz;

		/**
		 * Gets the value of the clazz property.
		 *
		 * @return possible object is {@link String }
		 *
		 */
		public String getClazz() {
			return this.clazz;
		}

		/**
		 * Sets the value of the clazz property.
		 *
		 * @param value
		 *            allowed object is {@link String }
		 *
		 */
		public void setClazz(String value) {
			this.clazz = value;
		}
	}
}