
package com.armedia.cmf.engine.alfresco.bulk.importer.jaxb;

import java.util.ArrayList;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;

/**
 * <p>
 * Java class for namedValue complex type.
 *
 * <p>
 * The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType name="namedValue">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;choice>
 *         &lt;element name="value" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="list" minOccurs="0">
 *           &lt;complexType>
 *             &lt;complexContent>
 *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *                 &lt;sequence>
 *                   &lt;element name="value" type="{http://www.w3.org/2001/XMLSchema}string" maxOccurs="unbounded" minOccurs="0"/>
 *                 &lt;/sequence>
 *               &lt;/restriction>
 *             &lt;/complexContent>
 *           &lt;/complexType>
 *         &lt;/element>
 *       &lt;/choice>
 *       &lt;attGroup ref="{http://www.alfresco.org/model/dictionary/1.0}name"/>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 *
 *
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "namedValue", propOrder = {
	"value", "list"
})
public class NamedValue {

	protected String value;
	protected NamedValue.List list;
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

	/**
	 * Gets the value of the list property.
	 *
	 * @return possible object is {@link NamedValue.List }
	 *
	 */
	public NamedValue.List getList() {
		return this.list;
	}

	/**
	 * Sets the value of the list property.
	 *
	 * @param value
	 *            allowed object is {@link NamedValue.List }
	 *
	 */
	public void setList(NamedValue.List value) {
		this.list = value;
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
	 *         &lt;element name="value" type="{http://www.w3.org/2001/XMLSchema}string" maxOccurs="unbounded" minOccurs="0"/>
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
		"value"
	})
	public static class List {

		protected java.util.List<String> value;

		/**
		 * Gets the value of the value property.
		 *
		 * <p>
		 * This accessor method returns a reference to the live list, not a snapshot. Therefore any
		 * modification you make to the returned list will be present inside the JAXB object. This
		 * is why there is not a <CODE>set</CODE> method for the value property.
		 *
		 * <p>
		 * For example, to add a new item, do as follows:
		 *
		 * <pre>
		 * getValue().add(newItem);
		 * </pre>
		 *
		 *
		 * <p>
		 * Objects of the following type(s) are allowed in the list {@link String }
		 *
		 *
		 */
		public java.util.List<String> getValue() {
			if (this.value == null) {
				this.value = new ArrayList<String>();
			}
			return this.value;
		}

	}

}