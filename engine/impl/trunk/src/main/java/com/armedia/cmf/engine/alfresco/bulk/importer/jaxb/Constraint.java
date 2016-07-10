
package com.armedia.cmf.engine.alfresco.bulk.importer.jaxb;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;

/**
 * Constraint for Alfresco M2Model
 *
 *
 * <p>
 * Java class for constraint complex type.
 *
 * <p>
 * The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType name="constraint">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="parameter" type="{http://www.alfresco.org/model/dictionary/1.0}namedValue" maxOccurs="unbounded" minOccurs="0"/>
 *       &lt;/sequence>
 *       &lt;attribute name="type">
 *         &lt;simpleType>
 *           &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *           &lt;/restriction>
 *         &lt;/simpleType>
 *       &lt;/attribute>
 *       &lt;attribute name="ref" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="name" type="{http://www.w3.org/2001/XMLSchema}string" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 *
 *
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "constraint", propOrder = {
	"parameter"
})
public class Constraint {

	protected List<NamedValue> parameter;
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
	 * Objects of the following type(s) are allowed in the list {@link NamedValue }
	 *
	 *
	 */
	public List<NamedValue> getParameter() {
		if (this.parameter == null) {
			this.parameter = new ArrayList<NamedValue>();
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

}