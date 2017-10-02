
package com.armedia.caliente.engine.transform.xml;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "condition.t", propOrder = {
	"condition"
})
public class ActionCondition extends ConditionWrapper {

	public ActionCondition() {
		super();
	}

	public ActionCondition(Condition condition) {
		super(condition);
	}
}