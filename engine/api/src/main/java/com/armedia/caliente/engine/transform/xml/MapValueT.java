
package com.armedia.caliente.engine.transform.xml;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for mapValue.t complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="mapValue.t">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="target" type="{http://www.armedia.com/ns/caliente/engine/transform}expression.t"/>
 *         &lt;element name="switch" type="{http://www.armedia.com/ns/caliente/engine/transform}switchValue.t"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "mapValue.t", propOrder = {
    "target",
    "_switch"
})
public class MapValueT {

    @XmlElement(required = true)
    protected ExpressionT target;
    @XmlElement(name = "switch", required = true)
    protected SwitchValueT _switch;

    /**
     * Gets the value of the target property.
     * 
     * @return
     *     possible object is
     *     {@link ExpressionT }
     *     
     */
    public ExpressionT getTarget() {
        return target;
    }

    /**
     * Sets the value of the target property.
     * 
     * @param value
     *     allowed object is
     *     {@link ExpressionT }
     *     
     */
    public void setTarget(ExpressionT value) {
        this.target = value;
    }

    /**
     * Gets the value of the switch property.
     * 
     * @return
     *     possible object is
     *     {@link SwitchValueT }
     *     
     */
    public SwitchValueT getSwitch() {
        return _switch;
    }

    /**
     * Sets the value of the switch property.
     * 
     * @param value
     *     allowed object is
     *     {@link SwitchValueT }
     *     
     */
    public void setSwitch(SwitchValueT value) {
        this._switch = value;
    }

}
