
package com.armedia.caliente.engine.transform.xml;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlType;

import com.armedia.caliente.engine.transform.TransformationContext;
import com.armedia.caliente.engine.transform.xml.actions.DecoratorAdd;
import com.armedia.caliente.engine.transform.xml.actions.ValueMappingApply;
import com.armedia.caliente.engine.transform.xml.actions.ValueMappingClear;
import com.armedia.caliente.engine.transform.xml.actions.VariableClear;
import com.armedia.caliente.engine.transform.xml.actions.CustomAction;
import com.armedia.caliente.engine.transform.xml.actions.MapAttributeValue;
import com.armedia.caliente.engine.transform.xml.actions.AttributeRemove;
import com.armedia.caliente.engine.transform.xml.actions.DecoratorRemove;
import com.armedia.caliente.engine.transform.xml.actions.AttributeReplace;
import com.armedia.caliente.engine.transform.xml.actions.DecoratorReplace;
import com.armedia.caliente.engine.transform.xml.actions.SubtypeReplace;
import com.armedia.caliente.engine.transform.xml.actions.AttributeSet;
import com.armedia.caliente.engine.transform.xml.actions.SubtypeSet;
import com.armedia.caliente.engine.transform.xml.actions.ValueMappingSet;
import com.armedia.caliente.engine.transform.xml.actions.VariableSet;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "transformation.t", propOrder = {
	"transformations"
})
public class Transformation extends ConditionalAction {

	@XmlElements({
		@XmlElement(name = "set-subtype", type = SubtypeSet.class),
		@XmlElement(name = "replace-subtype", type = SubtypeReplace.class),
		@XmlElement(name = "add-decorator", type = DecoratorAdd.class),
		@XmlElement(name = "remove-decorator", type = DecoratorRemove.class),
		@XmlElement(name = "replace-decorator", type = DecoratorReplace.class),
		@XmlElement(name = "set-attribute", type = AttributeSet.class),
		@XmlElement(name = "remove-attribute", type = AttributeRemove.class),
		@XmlElement(name = "replace-attribute", type = AttributeReplace.class),
		@XmlElement(name = "map-attribute-value", type = MapAttributeValue.class),
		@XmlElement(name = "set-variable", type = VariableSet.class),
		@XmlElement(name = "clear-variable", type = VariableClear.class),
		@XmlElement(name = "set-value-mapping", type = ValueMappingSet.class),
		@XmlElement(name = "clear-value-mapping", type = ValueMappingClear.class),
		@XmlElement(name = "apply-value-mapping", type = ValueMappingApply.class),
		@XmlElement(name = "custom-action", type = CustomAction.class)
	})
	protected List<ConditionalAction> transformations;

	public List<ConditionalAction> getTransformations() {
		if (this.transformations == null) {
			this.transformations = new ArrayList<>();
		}
		return this.transformations;
	}

	@Override
	protected final void applyTransformation(TransformationContext ctx) {
		for (ConditionalAction action : getTransformations()) {
			if (action != null) {
				action.applyTransformation(ctx);
			}
		}
	}
}