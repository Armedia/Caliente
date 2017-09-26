
package com.armedia.caliente.engine.transform;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import com.armedia.commons.utilities.Tools;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "mapValueCase.t", propOrder = {
	"value", "replacement"
})
public class MapValueCaseT {

	@XmlElement(name = "value", required = true)
	protected ExpressionT value;

	@XmlElement(name = "replacement", required = true)
	protected ExpressionT replacement;

	@XmlAttribute(name = "comparison", required = true)
	@XmlJavaTypeAdapter(ComparisonAdapter.class)
	protected Comparison comparison;

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

	public Comparison getComparison() {
		return Tools.coalesce(this.comparison, Comparison.EQ);
	}

	public void setComparison(Comparison value) {
		this.comparison = value;
	}

}