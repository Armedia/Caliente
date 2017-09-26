
package com.armedia.caliente.engine.transform.xml;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.CollapsedStringAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

/**
 * <p>
 * Java class for mapValueCase.t complex type.
 *
 * <p>
 * The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType name="mapValueCase.t">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="value" type="{http://www.armedia.com/ns/caliente/engine/transformations}expression.t"/>
 *         &lt;element name="replacement" type="{http://www.armedia.com/ns/caliente/engine/transformations}expression.t"/>
 *       &lt;/sequence>
 *       &lt;attribute name="comparison" use="required" type="{http://www.armedia.com/ns/caliente/engine/transformations}comparison.t" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 *
 *
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "mapValueCase.t", propOrder = {
	"value", "replacement"
})
public class MapValueCaseT {

	@XmlElement(required = true)
	protected ExpressionT value;

	@XmlElement(required = true)
	protected ExpressionT replacement;

	@XmlAttribute(name = "comparison", required = true)
	@XmlJavaTypeAdapter(CollapsedStringAdapter.class)
	protected String comparison;

	public ExpressionT getValue() {
		return this.value;
	}

	public void setValue(ExpressionT value) {
		this.value = value;
	}

	public ExpressionT getReplacement() {
		return this.replacement;
	}

	public void setReplacement(ExpressionT value) {
		this.replacement = value;
	}

	public String getComparison() {
		return this.comparison;
	}

	public void setComparison(String value) {
		this.comparison = value;
	}

}