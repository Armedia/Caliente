
package com.armedia.caliente.engine.transform.xml;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for attributeMapping.t complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="attributeMapping.t">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;choice maxOccurs="unbounded" minOccurs="0">
 *         &lt;element name="set-value" type="{http://www.armedia.com/ns/caliente/engine/transform}setValue.t"/>
 *         &lt;element name="map-value" type="{http://www.armedia.com/ns/caliente/engine/transform}mapValue.t"/>
 *       &lt;/choice>
 *       &lt;attribute name="includes" type="{http://www.w3.org/2001/XMLSchema}string" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "attributeMapping.t", propOrder = {
    "setValueOrMapValue"
})
@XmlSeeAlso({
    NamedAttributeMappingT.class
})
public class AttributeMappingT {

    @XmlElements({
        @XmlElement(name = "set-value", type = SetValueT.class),
        @XmlElement(name = "map-value", type = MapValueT.class)
    })
    protected List<Object> setValueOrMapValue;
    @XmlAttribute(name = "includes")
    protected String includes;

    /**
     * Gets the value of the setValueOrMapValue property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the setValueOrMapValue property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getSetValueOrMapValue().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link SetValueT }
     * {@link MapValueT }
     * 
     * 
     */
    public List<Object> getSetValueOrMapValue() {
        if (setValueOrMapValue == null) {
            setValueOrMapValue = new ArrayList<Object>();
        }
        return this.setValueOrMapValue;
    }

    /**
     * Gets the value of the includes property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getIncludes() {
        return includes;
    }

    /**
     * Sets the value of the includes property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setIncludes(String value) {
        this.includes = value;
    }

}
