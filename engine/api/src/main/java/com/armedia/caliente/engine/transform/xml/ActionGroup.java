package com.armedia.caliente.engine.transform.xml;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlType;

import com.armedia.caliente.engine.transform.TransformationContext;
import com.armedia.caliente.engine.transform.TransformationException;
import com.armedia.caliente.engine.transform.xml.actions.AbortTransformation;
import com.armedia.caliente.engine.transform.xml.actions.AttributeRemove;
import com.armedia.caliente.engine.transform.xml.actions.AttributeReplace;
import com.armedia.caliente.engine.transform.xml.actions.AttributeSet;
import com.armedia.caliente.engine.transform.xml.actions.CustomAction;
import com.armedia.caliente.engine.transform.xml.actions.DecoratorAdd;
import com.armedia.caliente.engine.transform.xml.actions.DecoratorRemove;
import com.armedia.caliente.engine.transform.xml.actions.DecoratorReplace;
import com.armedia.caliente.engine.transform.xml.actions.EndTransformation;
import com.armedia.caliente.engine.transform.xml.actions.MapAttributeValue;
import com.armedia.caliente.engine.transform.xml.actions.MapVariableValue;
import com.armedia.caliente.engine.transform.xml.actions.NameReplace;
import com.armedia.caliente.engine.transform.xml.actions.NameSet;
import com.armedia.caliente.engine.transform.xml.actions.SubtypeReplace;
import com.armedia.caliente.engine.transform.xml.actions.SubtypeSet;
import com.armedia.caliente.engine.transform.xml.actions.ValueMappingApply;
import com.armedia.caliente.engine.transform.xml.actions.ValueMappingClear;
import com.armedia.caliente.engine.transform.xml.actions.ValueMappingSet;
import com.armedia.caliente.engine.transform.xml.actions.VariableRemove;
import com.armedia.caliente.engine.transform.xml.actions.VariableReplace;
import com.armedia.caliente.engine.transform.xml.actions.VariableSet;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "actionGroup.t", propOrder = {
	"actions"
})
public class ActionGroup extends ConditionalAction {

	@XmlElements({
		@XmlElement(name = "group", type = ActionGroup.class), //
		@XmlElement(name = "set-name", type = NameSet.class), //
		@XmlElement(name = "replace-name", type = NameReplace.class), //
		@XmlElement(name = "set-subtype", type = SubtypeSet.class), //
		@XmlElement(name = "replace-subtype", type = SubtypeReplace.class), //
		@XmlElement(name = "add-decorator", type = DecoratorAdd.class), //
		@XmlElement(name = "remove-decorator", type = DecoratorRemove.class), //
		@XmlElement(name = "replace-decorator", type = DecoratorReplace.class), //
		@XmlElement(name = "set-attribute", type = AttributeSet.class), //
		@XmlElement(name = "remove-attribute", type = AttributeRemove.class), //
		@XmlElement(name = "replace-attribute", type = AttributeReplace.class), //
		@XmlElement(name = "map-attribute-value", type = MapAttributeValue.class), //
		@XmlElement(name = "map-variable-value", type = MapVariableValue.class), //
		@XmlElement(name = "set-variable", type = VariableSet.class), //
		@XmlElement(name = "remove-variable", type = VariableRemove.class), //
		@XmlElement(name = "replace-variable", type = VariableReplace.class), //
		@XmlElement(name = "set-value-mapping", type = ValueMappingSet.class), //
		@XmlElement(name = "clear-value-mapping", type = ValueMappingClear.class), //
		@XmlElement(name = "apply-value-mapping", type = ValueMappingApply.class), //
		@XmlElement(name = "custom-action", type = CustomAction.class), //
		@XmlElement(name = "end-transformation", type = EndTransformation.class), //
		@XmlElement(name = "abort-transformation", type = AbortTransformation.class), //
	})
	protected List<Action> actions;

	public List<Action> getActions() {
		if (this.actions == null) {
			this.actions = new ArrayList<>();
		}
		return this.actions;
	}

	@Override
	protected final void applyTransformation(TransformationContext ctx) throws TransformationException {
		for (Action action : getActions()) {
			if (action != null) {
				action.apply(ctx);
			}
		}
	}

}