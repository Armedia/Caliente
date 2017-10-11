
package com.armedia.caliente.engine.dynamic.xml.actions;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import com.armedia.caliente.engine.dynamic.xml.Comparison;
import com.armedia.caliente.engine.dynamic.xml.ComparisonAdapter;
import com.armedia.caliente.engine.dynamic.xml.Expression;
import com.armedia.commons.utilities.Tools;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "mapValueCase.t", propOrder = {
	"value", "replacement"
})
public class MapValueCase {

	@XmlElement(name = "value", required = true)
	protected Expression value;

	@XmlElement(name = "replacement", required = true)
	protected Expression replacement;

	@XmlAttribute(name = "comparison", required = true)
	@XmlJavaTypeAdapter(ComparisonAdapter.class)
	protected Comparison comparison;

	public Expression getValue() {
		return this.value;
	}

	public void setValue(Expression value) {
		this.value = value;
	}

	public Expression getReplacement() {
		return this.replacement;
	}

	public void setReplacement(Expression value) {
		this.replacement = value;
	}

	public Comparison getComparison() {
		return Tools.coalesce(this.comparison, Comparison.DEFAULT);
	}

	public void setComparison(Comparison value) {
		this.comparison = value;
	}

}