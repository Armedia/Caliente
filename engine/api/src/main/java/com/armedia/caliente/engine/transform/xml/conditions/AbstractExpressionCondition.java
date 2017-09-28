
package com.armedia.caliente.engine.transform.xml.conditions;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;

import com.armedia.caliente.engine.transform.xml.Condition;
import com.armedia.caliente.engine.transform.xml.ExpressionT;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "conditionExpression.t")
public abstract class AbstractExpressionCondition extends ExpressionT implements Condition {
}