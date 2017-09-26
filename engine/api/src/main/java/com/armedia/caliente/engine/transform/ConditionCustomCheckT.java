
package com.armedia.caliente.engine.transform;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "conditionCustomCheck.t")
public class ConditionCustomCheckT extends ConditionExpressionT {

	@Override
	public <V> boolean check(TransformationContext<V> ctx) {
		// TODO: The expression is the name of a class we need to instantiate "somehow", that
		// implements the Condition interface, and invoke its evaluate() method.
		return false;
	}

}