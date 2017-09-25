
package com.armedia.caliente.engine.transform.xml;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for anonymous complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType>
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;choice maxOccurs="unbounded" minOccurs="0">
 *         &lt;element name="include" type="{http://www.w3.org/2001/XMLSchema}string" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element name="attribute-mapping" type="{http://www.armedia.com/ns/caliente/engine/transform}namedAttributeMapping.t" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element name="type-mapping" type="{http://www.armedia.com/ns/caliente/engine/transform}namedTypeMapping.t" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element name="transformation" type="{http://www.armedia.com/ns/caliente/engine/transform}transformation.t" maxOccurs="unbounded" minOccurs="0"/>
 *       &lt;/choice>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "includeOrAttributeMappingOrTypeMapping"
})
@XmlRootElement(name = "transformations")
public class Transformations {

    @XmlElements({
        @XmlElement(name = "include", type = String.class),
        @XmlElement(name = "attribute-mapping", type = NamedAttributeMappingT.class),
        @XmlElement(name = "type-mapping", type = NamedTypeMappingT.class),
        @XmlElement(name = "transformation", type = TransformationT.class)
    })
    protected List<Object> includeOrAttributeMappingOrTypeMapping;

    /**
     * Gets the value of the includeOrAttributeMappingOrTypeMapping property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the includeOrAttributeMappingOrTypeMapping property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getIncludeOrAttributeMappingOrTypeMapping().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link String }
     * {@link NamedAttributeMappingT }
     * {@link NamedTypeMappingT }
     * {@link TransformationT }
     * 
     * 
     */
    public List<Object> getIncludeOrAttributeMappingOrTypeMapping() {
        if (includeOrAttributeMappingOrTypeMapping == null) {
            includeOrAttributeMappingOrTypeMapping = new ArrayList<Object>();
        }
        return this.includeOrAttributeMappingOrTypeMapping;
    }

}
