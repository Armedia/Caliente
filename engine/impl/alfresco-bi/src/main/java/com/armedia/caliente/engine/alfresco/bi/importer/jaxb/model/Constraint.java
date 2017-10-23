
package com.armedia.caliente.engine.alfresco.bi.importer.jaxb.model;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "constraint", propOrder = {
	"parameter"
})
public class Constraint {

	@XmlElement
	protected List<Constraint.Parameter> parameter;

	@XmlAttribute
	protected String type;

	@XmlAttribute
	protected String ref;

	@XmlAttribute
	protected String name;

	/**
	 * Gets the value of the parameter property.
	 *
	 * <p>
	 * This accessor method returns a reference to the live list, not a snapshot. Therefore any
	 * modification you make to the returned list will be present inside the JAXB object. This is
	 * why there is not a <CODE>set</CODE> method for the parameter property.
	 *
	 * <p>
	 * For example, to add a new item, do as follows:
	 *
	 * <pre>
	 * getParameter().add(newItem);
	 * </pre>
	 *
	 *
	 * <p>
	 * Objects of the following type(s) are allowed in the list {@link Constraint.Parameter }
	 *
	 *
	 */
	public List<Constraint.Parameter> getParameter() {
		if (this.parameter == null) {
			this.parameter = new ArrayList<>();
		}
		return this.parameter;
	}

	/**
	 * Gets the value of the type property.
	 *
	 * @return possible object is {@link String }
	 *
	 */
	public String getType() {
		return this.type;
	}

	/**
	 * Sets the value of the type property.
	 *
	 * @param value
	 *            allowed object is {@link String }
	 *
	 */
	public void setType(String value) {
		this.type = value;
	}

	/**
	 * Gets the value of the ref property.
	 *
	 * @return possible object is {@link String }
	 *
	 */
	public String getRef() {
		return this.ref;
	}

	/**
	 * Sets the value of the ref property.
	 *
	 * @param value
	 *            allowed object is {@link String }
	 *
	 */
	public void setRef(String value) {
		this.ref = value;
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
	@XmlType(name = "namedValue", propOrder = {
		"value", "list"
	})
	public static class Parameter {

		@XmlElement
		protected String value;

		@XmlElementWrapper(name = "list")
		@XmlElement(name = "value")
		protected List<String> list;

		@XmlAttribute(required = true)
		protected String name;

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

		public List<String> getValueList() {
			return this.list = ObjectFactory.getList(this.list);
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
}