
package com.armedia.caliente.engine.transform.xml;

import java.util.List;
import java.util.Objects;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;

import com.armedia.caliente.engine.transform.Condition;
import com.armedia.caliente.engine.transform.TransformationContext;

/**
 * <p>
 * Java class for conditionGroupXnor.t complex type.
 *
 * <p>
 * The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType name="conditionGroupXnor.t">
 *   &lt;complexContent>
 *     &lt;extension base="{http://www.armedia.com/ns/caliente/engine/transformations}conditionGroup.t">
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 *
 *
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "conditionGroupXnor.t")
public class ConditionGroupXnorT extends ConditionGroupT {

	@Override
	protected <V> boolean doEvaluate(List<Condition> elements, TransformationContext<V> ctx) {
		int trueCount = 0;
		for (Condition c : elements) {
			Objects.requireNonNull(c, "Null conditional elements are not allowed");
			if (c.check(ctx)) {
				trueCount++;
			}
		}
		return ((trueCount % 2) == 0);
	}

}