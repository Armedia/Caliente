
package com.armedia.caliente.engine.transform.xml;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for switchValue.t complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="switchValue.t">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="case" type="{http://www.armedia.com/ns/caliente/engine/transform}switchCase.t"/>
 *         &lt;element name="default" type="{http://www.armedia.com/ns/caliente/engine/transform}expression.t" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "switchValue.t", propOrder = {
    "_case",
    "_default"
})
public class SwitchValueT {

    @XmlElement(name = "case", required = true)
    protected SwitchCaseT _case;
    @XmlElement(name = "default")
    protected ExpressionT _default;

    /**
     * Gets the value of the case property.
     * 
     * @return
     *     possible object is
     *     {@link SwitchCaseT }
     *     
     */
    public SwitchCaseT getCase() {
        return _case;
    }

    /**
     * Sets the value of the case property.
     * 
     * @param value
     *     allowed object is
     *     {@link SwitchCaseT }
     *     
     */
    public void setCase(SwitchCaseT value) {
        this._case = value;
    }

    /**
     * Gets the value of the default property.
     * 
     * @return
     *     possible object is
     *     {@link ExpressionT }
     *     
     */
    public ExpressionT getDefault() {
        return _default;
    }

    /**
     * Sets the value of the default property.
     * 
     * @param value
     *     allowed object is
     *     {@link ExpressionT }
     *     
     */
    public void setDefault(ExpressionT value) {
        this._default = value;
    }

}
