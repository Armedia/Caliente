package com.armedia.caliente.engine.dynamic.xml;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlType;

import com.armedia.caliente.engine.dynamic.Action;
import com.armedia.caliente.engine.dynamic.ActionException;
import com.armedia.caliente.engine.dynamic.DynamicElementContext;
import com.armedia.caliente.engine.dynamic.xml.actions.AbortTransformation;
import com.armedia.caliente.engine.dynamic.xml.actions.AttributeCopy;
import com.armedia.caliente.engine.dynamic.xml.actions.AttributeRemove;
import com.armedia.caliente.engine.dynamic.xml.actions.AttributeRename;
import com.armedia.caliente.engine.dynamic.xml.actions.AttributeReplace;
import com.armedia.caliente.engine.dynamic.xml.actions.AttributeSet;
import com.armedia.caliente.engine.dynamic.xml.actions.CustomAction;
import com.armedia.caliente.engine.dynamic.xml.actions.EndTransformation;
import com.armedia.caliente.engine.dynamic.xml.actions.LoadExternalMetadata;
import com.armedia.caliente.engine.dynamic.xml.actions.MapAttributeValue;
import com.armedia.caliente.engine.dynamic.xml.actions.MapOriginalSubtype;
import com.armedia.caliente.engine.dynamic.xml.actions.MapSubtype;
import com.armedia.caliente.engine.dynamic.xml.actions.MapVariableValue;
import com.armedia.caliente.engine.dynamic.xml.actions.OriginalSecondarySubtypeRemove;
import com.armedia.caliente.engine.dynamic.xml.actions.OriginalSecondarySubtypeReset;
import com.armedia.caliente.engine.dynamic.xml.actions.PrincipalMappingApply;
import com.armedia.caliente.engine.dynamic.xml.actions.SecondarySubtypeAdd;
import com.armedia.caliente.engine.dynamic.xml.actions.SecondarySubtypeRemove;
import com.armedia.caliente.engine.dynamic.xml.actions.SecondarySubtypeReplace;
import com.armedia.caliente.engine.dynamic.xml.actions.SubtypeReplace;
import com.armedia.caliente.engine.dynamic.xml.actions.SubtypeSet;
import com.armedia.caliente.engine.dynamic.xml.actions.ValueMappingApply;
import com.armedia.caliente.engine.dynamic.xml.actions.ValueMappingClear;
import com.armedia.caliente.engine.dynamic.xml.actions.ValueMappingSet;
import com.armedia.caliente.engine.dynamic.xml.actions.VariableCopy;
import com.armedia.caliente.engine.dynamic.xml.actions.VariableRemove;
import com.armedia.caliente.engine.dynamic.xml.actions.VariableRename;
import com.armedia.caliente.engine.dynamic.xml.actions.VariableReplace;
import com.armedia.caliente.engine.dynamic.xml.actions.VariableSet;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "actionGroup.t", propOrder = {
	"actions"
})
public class ActionGroup extends ConditionalAction {

	@XmlElements({
		@XmlElement(name = "group", type = ActionGroup.class), //
		@XmlElement(name = "map-subtype", type = MapSubtype.class), //
		@XmlElement(name = "map-original-subtype", type = MapOriginalSubtype.class), //
		@XmlElement(name = "set-subtype", type = SubtypeSet.class), //
		@XmlElement(name = "replace-subtype", type = SubtypeReplace.class), //
		@XmlElement(name = "add-secondary-subtype", type = SecondarySubtypeAdd.class), //
		@XmlElement(name = "remove-secondary-subtype", type = SecondarySubtypeRemove.class), //
		@XmlElement(name = "replace-secondary-subtype", type = SecondarySubtypeReplace.class), //
		@XmlElement(name = "remove-original-secondary-subtypes", type = OriginalSecondarySubtypeRemove.class), //
		@XmlElement(name = "reset-original-secondary-subtypes", type = OriginalSecondarySubtypeReset.class), //
		@XmlElement(name = "set-attribute", type = AttributeSet.class), //
		@XmlElement(name = "copy-attribute", type = AttributeCopy.class), //
		@XmlElement(name = "rename-attribute", type = AttributeRename.class), //
		@XmlElement(name = "remove-attribute", type = AttributeRemove.class), //
		@XmlElement(name = "replace-attribute", type = AttributeReplace.class), //
		@XmlElement(name = "map-attribute-value", type = MapAttributeValue.class), //
		@XmlElement(name = "map-variable-value", type = MapVariableValue.class), //
		@XmlElement(name = "set-variable", type = VariableSet.class), //
		@XmlElement(name = "copy-variable", type = VariableCopy.class), //
		@XmlElement(name = "rename-variable", type = VariableRename.class), //
		@XmlElement(name = "remove-variable", type = VariableRemove.class), //
		@XmlElement(name = "replace-variable", type = VariableReplace.class), //
		@XmlElement(name = "set-value-mapping", type = ValueMappingSet.class), //
		@XmlElement(name = "clear-value-mapping", type = ValueMappingClear.class), //
		@XmlElement(name = "apply-value-mapping", type = ValueMappingApply.class), //
		@XmlElement(name = "map-principal", type = PrincipalMappingApply.class), //
		@XmlElement(name = "custom-action", type = CustomAction.class), //
		@XmlElement(name = "end-transformation", type = EndTransformation.class), //
		@XmlElement(name = "abort-transformation", type = AbortTransformation.class), //
		@XmlElement(name = "load-external-metadata", type = LoadExternalMetadata.class), //
	})
	protected List<Action> actions;

	public List<Action> getActions() {
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
	protected final void executeAction(DynamicElementContext ctx) throws ActionException {
		for (Action action : getActions()) {
			if (action != null) {
				action.apply(ctx);
			}
		}
	}

}