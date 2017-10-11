
package com.armedia.caliente.engine.dynamic.xml.conditions;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;

import com.armedia.caliente.engine.dynamic.Condition;
import com.armedia.caliente.engine.dynamic.xml.Expression;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "conditionExpression.t")
public abstract class AbstractExpressionCondition extends Expression implements Condition {
}