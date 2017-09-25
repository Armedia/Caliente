
package com.armedia.caliente.engine.transform.xml;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for typeMapping.t complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="typeMapping.t">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="type" type="{http://www.armedia.com/ns/caliente/engine/transform}typeName.t" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element name="subtype" type="{http://www.armedia.com/ns/caliente/engine/transform}typeName.t" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element name="decorators" type="{http://www.armedia.com/ns/caliente/engine/transform}decorators.t" maxOccurs="unbounded" minOccurs="0"/>
 *       &lt;/sequence>
 *       &lt;attribute name="extends" type="{http://www.armedia.com/ns/caliente/engine/transform}typeName.t" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "typeMapping.t", propOrder = {
    "type",
    "subtype",
    "decorators"
})
@XmlSeeAlso({
    NamedTypeMappingT.class
})
public class TypeMappingT {

    protected List<String> type;
    protected List<String> subtype;
    protected List<DecoratorsT> decorators;
    @XmlAttribute(name = "extends")
    protected String _extends;

    /**
     * Gets the value of the type property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the type property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getType().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link String }
     * 
     * 
     */
    public List<String> getType() {
        if (type == null) {
            type = new ArrayList<String>();
        }
        return this.type;
    }

    /**
     * Gets the value of the subtype property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the subtype property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getSubtype().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link String }
     * 
     * 
     */
    public List<String> getSubtype() {
        if (subtype == null) {
            subtype = new ArrayList<String>();
        }
        return this.subtype;
    }

    /**
     * Gets the value of the decorators property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the decorators property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getDecorators().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link DecoratorsT }
     * 
     * 
     */
    public List<DecoratorsT> getDecorators() {
        if (decorators == null) {
            decorators = new ArrayList<DecoratorsT>();
        }
        return this.decorators;
    }

    /**
     * Gets the value of the extends property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getExtends() {
        return _extends;
    }

    /**
     * Sets the value of the extends property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setExtends(String value) {
        this._extends = value;
    }

}
