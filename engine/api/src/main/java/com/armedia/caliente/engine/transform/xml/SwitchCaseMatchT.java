
package com.armedia.caliente.engine.transform.xml;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.CollapsedStringAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

/**
 * <p>
 * Java class for switchCaseMatch.t complex type.
 *
 * <p>
 * The following schema fragment specifies the expected content contained within this class.
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
public class SwitchCaseMatchT extends ExpressionT {

	@XmlAttribute(name = "comparison", required = true)
	@XmlJavaTypeAdapter(CollapsedStringAdapter.class)
	protected String comparison;

	public Comparison getComparison() {
		return Comparison.get(this.comparison, Comparison.EQ);
	}

	public void setComparison(Comparison comparison) {
		this.comparison = (comparison != null ? comparison.name() : null);
	}

}