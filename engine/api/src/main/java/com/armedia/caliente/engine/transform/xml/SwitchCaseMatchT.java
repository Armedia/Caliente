
package com.armedia.caliente.engine.transform.xml;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.CollapsedStringAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;


/**
 * <p>Java class for switchCaseMatch.t complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="switchCaseMatch.t">
 *   &lt;simpleContent>
 *     &lt;extension base="&lt;http://www.armedia.com/ns/caliente/engine/transform>expression.t">
 *       &lt;attribute name="comparison" use="required" type="{http://www.armedia.com/ns/caliente/engine/transform}comparison.t" />
 *     &lt;/extension>
 *   &lt;/simpleContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "switchCaseMatch.t")
public class SwitchCaseMatchT
    extends ExpressionT
{

    @XmlAttribute(name = "comparison", required = true)
    @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
    protected String comparison;

    /**
     * Gets the value of the comparison property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getComparison() {
        return comparison;
    }

    /**
     * Sets the value of the comparison property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setComparison(String value) {
        this.comparison = value;
    }

}
