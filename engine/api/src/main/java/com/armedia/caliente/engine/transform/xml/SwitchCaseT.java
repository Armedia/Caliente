
package com.armedia.caliente.engine.transform.xml;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "switchCase.t", propOrder = {
	"match", "value"
})
public class SwitchCaseT {

	protected SwitchCaseMatchT match;

	protected ExpressionT value;

	public SwitchCaseMatchT getMatch() {
		return this.match;
	}

	public void setMatch(SwitchCaseMatchT match) {
		this.match = match;
	}

	public ExpressionT getValue() {
		return this.value;
	}

	public void setValue(ExpressionT value) {
		this.value = value;
	}

}