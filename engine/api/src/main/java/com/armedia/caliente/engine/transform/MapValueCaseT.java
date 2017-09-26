
package com.armedia.caliente.engine.transform;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.CollapsedStringAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

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