
package com.armedia.caliente.engine.transform;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

/**
 * <p>
 * Java class for mapValue.t complex type.
 *
 * <p>
 * The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType name="mapValue.t">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence maxOccurs="unbounded">
 *         &lt;element name="case" type="{http://www.armedia.com/ns/caliente/engine/transformations}mapValueCase.t" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element name="default" type="{http://www.armedia.com/ns/caliente/engine/transformations}expression.t" minOccurs="0"/>
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
	"cases", "defaultValue"
})
public class MapValueT {

	@XmlElement(name = "case", required = false)
	protected List<MapValueCaseT> cases;

	@XmlElement(name = "default", required = false)
	protected ExpressionT defaultValue;

	public List<MapValueCaseT> getCases() {
		if (this.cases == null) {
			this.cases = new ArrayList<>();
		}
		return this.cases;
	}

	public ExpressionT getDefaultValue() {
		return this.defaultValue;
	}

	public void setDefaultValue(ExpressionT defaultValue) {
		this.defaultValue = defaultValue;
	}

}