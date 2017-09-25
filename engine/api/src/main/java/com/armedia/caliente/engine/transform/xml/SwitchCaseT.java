
package com.armedia.caliente.engine.transform.xml;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for switchCase.t complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="switchCase.t">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="match" type="{http://www.armedia.com/ns/caliente/engine/transform}switchCaseMatch.t" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element name="value" type="{http://www.armedia.com/ns/caliente/engine/transform}expression.t" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "switchCase.t", propOrder = {
    "match",
    "value"
})
public class SwitchCaseT {

    protected List<SwitchCaseMatchT> match;
    protected ExpressionT value;

    /**
     * Gets the value of the match property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the match property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getMatch().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link SwitchCaseMatchT }
     * 
     * 
     */
    public List<SwitchCaseMatchT> getMatch() {
        if (match == null) {
            match = new ArrayList<SwitchCaseMatchT>();
        }
        return this.match;
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
