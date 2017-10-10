package com.armedia.caliente.engine.dynamic.jaxb;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlType;

import com.armedia.caliente.engine.dynamic.Action;
import com.armedia.caliente.engine.dynamic.ActionException;
import com.armedia.caliente.engine.dynamic.ObjectContext;
import com.armedia.caliente.engine.dynamic.jaxb.actions.AbortTransformation;
import com.armedia.caliente.engine.dynamic.jaxb.actions.AttributeRemove;
import com.armedia.caliente.engine.dynamic.jaxb.actions.AttributeReplace;
import com.armedia.caliente.engine.dynamic.jaxb.actions.AttributeSet;
import com.armedia.caliente.engine.dynamic.jaxb.actions.CustomAction;
import com.armedia.caliente.engine.dynamic.jaxb.actions.EndTransformation;
import com.armedia.caliente.engine.dynamic.jaxb.actions.MapAttributeValue;
import com.armedia.caliente.engine.dynamic.jaxb.actions.MapVariableValue;
import com.armedia.caliente.engine.dynamic.jaxb.actions.OriginalSecondarySubtypeRemove;
import com.armedia.caliente.engine.dynamic.jaxb.actions.OriginalSecondarySubtypeReset;
import com.armedia.caliente.engine.dynamic.jaxb.actions.SecondarySubtypeAdd;
import com.armedia.caliente.engine.dynamic.jaxb.actions.SecondarySubtypeRemove;
import com.armedia.caliente.engine.dynamic.jaxb.actions.SecondarySubtypeReplace;
import com.armedia.caliente.engine.dynamic.jaxb.actions.SubtypeReplace;
import com.armedia.caliente.engine.dynamic.jaxb.actions.SubtypeSet;
import com.armedia.caliente.engine.dynamic.jaxb.actions.ValueMappingApply;
import com.armedia.caliente.engine.dynamic.jaxb.actions.ValueMappingClear;
import com.armedia.caliente.engine.dynamic.jaxb.actions.ValueMappingSet;
import com.armedia.caliente.engine.dynamic.jaxb.actions.VariableRemove;
import com.armedia.caliente.engine.dynamic.jaxb.actions.VariableReplace;
import com.armedia.caliente.engine.dynamic.jaxb.actions.VariableSet;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "actionGroup.t", propOrder = {
	"actions"
})
public class ActionGroup extends ConditionalAction {

	@XmlElements({
		@XmlElement(name = "group", type = ActionGroup.class), //
		@XmlElement(name = "set-subtype", type = SubtypeSet.class), //
		@XmlElement(name = "replace-subtype", type = SubtypeReplace.class), //
		@XmlElement(name = "add-secondary-subtype", type = SecondarySubtypeAdd.class), //
		@XmlElement(name = "remove-secondary-subtype", type = SecondarySubtypeRemove.class), //
		@XmlElement(name = "replace-secondary-subtype", type = SecondarySubtypeReplace.class), //
		@XmlElement(name = "remove-original-secondary-subtypes", type = OriginalSecondarySubtypeRemove.class), //
		@XmlElement(name = "reset-original-secondary-subtypes", type = OriginalSecondarySubtypeReset.class), //
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
	protected List<? extends Action> actions;

	public List<? extends Action> getActions() {
		if (this.actions == null) {
			this.actions = new ArrayList<>();
		}
		return this.actions;
	}

	@Override
	protected boolean isSkippable() {
		// Allow skipping if there are no actions contained in this group
		return super.isSkippable() || (this.actions == null) || this.actions.isEmpty();
	}

	@Override
	protected final void applyTransformation(ObjectContext ctx) throws ActionException {
		for (Action action : getActions()) {
			if (action != null) {
				action.apply(ctx);
			}
		}
	}

}