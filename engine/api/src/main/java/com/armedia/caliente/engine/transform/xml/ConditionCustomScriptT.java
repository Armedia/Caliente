
package com.armedia.caliente.engine.transform.xml;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;

import com.armedia.caliente.engine.transform.Condition;
import com.armedia.caliente.engine.transform.TransformationContext;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "conditionCustomScript.t")
public class ConditionCustomScriptT extends ExpressionT implements Condition {

	@Override
	public <V> boolean check(TransformationContext<V> ctx) {
		// TODO: The expression is a script that needs to be evaluated using
		// JSR223, and whose return value must be either true (!= 0), or false (== 0)
		return false;
	}

}