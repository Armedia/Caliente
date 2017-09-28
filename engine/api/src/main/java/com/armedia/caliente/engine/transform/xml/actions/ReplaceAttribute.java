
package com.armedia.caliente.engine.transform.xml.actions;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import com.armedia.caliente.engine.transform.TransformationContext;
import com.armedia.caliente.engine.transform.xml.Cardinality;
import com.armedia.caliente.engine.transform.xml.CardinalityAdapter;
import com.armedia.caliente.engine.transform.xml.ConditionalActionT;
import com.armedia.caliente.engine.transform.xml.ExpressionT;
import com.armedia.commons.utilities.Tools;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "actionReplaceAttribute.t", propOrder = {
	"attributeName", "cardinality", "regex", "replacement"
})
public class ReplaceAttribute extends ConditionalActionT {

	@XmlElement(name = "attribute-name", required = true)
	protected ExpressionT attributeName;

	@XmlElement(name = "cardinality", required = false)
	@XmlJavaTypeAdapter(CardinalityAdapter.class)
	protected Cardinality cardinality;

	@XmlElement(name = "regex", required = true)
	protected ExpressionT regex;

	@XmlElement(name = "replacement", required = true)
	protected ExpressionT replacement;

	public ExpressionT getAttributeName() {
		return this.attributeName;
	}

	public void setAttributeName(ExpressionT value) {
		this.attributeName = value;
	}

	public Cardinality getCardinality() {
		return Tools.coalesce(this.cardinality, Cardinality.ALL);
	}

	public void setCardinality(Cardinality value) {
		this.cardinality = value;
	}

	public ExpressionT getRegex() {
		return this.regex;
	}

	public void setRegex(ExpressionT value) {
		this.regex = value;
	}

	public ExpressionT getReplacement() {
		return this.replacement;
	}

	public void setReplacement(ExpressionT value) {
		this.replacement = value;
	}

	@Override
	protected void applyTransformation(TransformationContext ctx) {
		// TODO implement this transformation

	}

}