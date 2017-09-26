
package com.armedia.caliente.engine.transform.xml;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;

import com.armedia.caliente.engine.transform.Condition;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "conditionExpression.t")
public abstract class ConditionExpressionT extends ExpressionT implements Condition {
}