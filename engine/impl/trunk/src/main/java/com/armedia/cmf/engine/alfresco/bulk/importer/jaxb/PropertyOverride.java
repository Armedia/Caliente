
package com.armedia.cmf.engine.alfresco.bulk.importer.jaxb;

import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlType;

/**
 * <p>
 * Java class for propertyOverride complex type.
 *
 * <p>
 * The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType name="propertyOverride">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="mandatory" type="{http://www.alfresco.org/model/dictionary/1.0}mandatoryDef" minOccurs="0"/>
 *         &lt;element name="default" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
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
@XmlType(name = "propertyOverride", propOrder = {
	"mandatory", "_default", "constraints"
})
public class PropertyOverride {

	@XmlElement
	protected MandatoryDef mandatory;

	@XmlElement(name = "default")
	protected String _default;

	@XmlElementWrapper(name = "constraints")
	@XmlElement(name = "constraint")
	protected List<Constraint> constraints;

	@XmlAttribute(required = true)
	protected String name;

	/**
	 * Gets the value of the mandatory property.
	 *
	 * @return possible object is {@link MandatoryDef }
	 *
	 */
	public MandatoryDef getMandatory() {
		return this.mandatory;
	}

	/**
	 * Sets the value of the mandatory property.
	 *
	 * @param value
	 *            allowed object is {@link MandatoryDef }
	 *
	 */
	public void setMandatory(MandatoryDef value) {
		this.mandatory = value;
	}

	/**
	 * Gets the value of the default property.
	 *
	 * @return possible object is {@link String }
	 *
	 */
	public String getDefault() {
		return this._default;
	}

	/**
	 * Sets the value of the default property.
	 *
	 * @param value
	 *            allowed object is {@link String }
	 *
	 */
	public void setDefault(String value) {
		this._default = value;
	}

	public List<Constraint> getConstraints() {
		return this.constraints = ObjectFactory.getList(this.constraints);
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