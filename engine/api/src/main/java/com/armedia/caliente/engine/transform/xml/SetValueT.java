
package com.armedia.caliente.engine.transform.xml;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for setValue.t complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="setValue.t">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="name" type="{http://www.armedia.com/ns/caliente/engine/transform}expression.t"/>
 *         &lt;element name="value" type="{http://www.armedia.com/ns/caliente/engine/transform}expression.t"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "setValue.t", propOrder = {
    "name",
    "value"
})
public class SetValueT {

    @XmlElement(required = true)
    protected ExpressionT name;
    @XmlElement(required = true)
    protected ExpressionT value;

    /**
     * Gets the value of the name property.
     * 
     * @return
     *     possible object is
     *     {@link ExpressionT }
     *     
     */
    public ExpressionT getName() {
        return name;
    }

    /**
     * Sets the value of the name property.
     * 
     * @param value
     *     allowed object is
     *     {@link ExpressionT }
     *     
     */
    public void setName(ExpressionT value) {
        this.name = value;
    }

    /**
     * Gets the value of the value property.
     * 
     * @return
     *     possible object is
     *     {@link ExpressionT }
     *     
     */
    public ExpressionT getValue() {
        return value;
    }

    /**
     * Sets the value of the value property.
     * 
     * @param value
     *     allowed object is
     *     {@link ExpressionT }
     *     
     */
    public void setValue(ExpressionT value) {
        this.value = value;
    }

}
