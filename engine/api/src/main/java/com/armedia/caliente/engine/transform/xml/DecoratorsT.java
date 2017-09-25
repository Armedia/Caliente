
package com.armedia.caliente.engine.transform.xml;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for decorators.t complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="decorators.t">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="remove-decorator" type="{http://www.armedia.com/ns/caliente/engine/transform}expression.t" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element name="add-decorator" type="{http://www.armedia.com/ns/caliente/engine/transform}expression.t" maxOccurs="unbounded" minOccurs="0"/>
 *       &lt;/sequence>
 *       &lt;attribute name="clear-existing" type="{http://www.w3.org/2001/XMLSchema}boolean" default="false" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "decorators.t", propOrder = {
    "removeDecorator",
    "addDecorator"
})
public class DecoratorsT {

    @XmlElement(name = "remove-decorator")
    protected List<ExpressionT> removeDecorator;
    @XmlElement(name = "add-decorator")
    protected List<ExpressionT> addDecorator;
    @XmlAttribute(name = "clear-existing")
    protected Boolean clearExisting;

    /**
     * Gets the value of the removeDecorator property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the removeDecorator property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getRemoveDecorator().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link ExpressionT }
     * 
     * 
     */
    public List<ExpressionT> getRemoveDecorator() {
        if (removeDecorator == null) {
            removeDecorator = new ArrayList<ExpressionT>();
        }
        return this.removeDecorator;
    }

    /**
     * Gets the value of the addDecorator property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the addDecorator property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getAddDecorator().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link ExpressionT }
     * 
     * 
     */
    public List<ExpressionT> getAddDecorator() {
        if (addDecorator == null) {
            addDecorator = new ArrayList<ExpressionT>();
        }
        return this.addDecorator;
    }

    /**
     * Gets the value of the clearExisting property.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public boolean isClearExisting() {
        if (clearExisting == null) {
            return false;
        } else {
            return clearExisting;
        }
    }

    /**
     * Sets the value of the clearExisting property.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setClearExisting(Boolean value) {
        this.clearExisting = value;
    }

}
