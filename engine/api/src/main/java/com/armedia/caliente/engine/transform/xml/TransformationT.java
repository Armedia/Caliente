
package com.armedia.caliente.engine.transform.xml;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlType;

import com.armedia.caliente.engine.transform.TransformationContext;
import com.armedia.caliente.engine.transform.xml.actions.ActionAddDecoratorT;
import com.armedia.caliente.engine.transform.xml.actions.ActionApplyValueMappingT;
import com.armedia.caliente.engine.transform.xml.actions.ActionClearValueMappingT;
import com.armedia.caliente.engine.transform.xml.actions.ActionClearVariableT;
import com.armedia.caliente.engine.transform.xml.actions.ActionCustomActionT;
import com.armedia.caliente.engine.transform.xml.actions.ActionMapAttributeValueT;
import com.armedia.caliente.engine.transform.xml.actions.ActionRemoveAttributeT;
import com.armedia.caliente.engine.transform.xml.actions.ActionRemoveDecoratorT;
import com.armedia.caliente.engine.transform.xml.actions.ActionReplaceAttributeT;
import com.armedia.caliente.engine.transform.xml.actions.ActionReplaceDecoratorT;
import com.armedia.caliente.engine.transform.xml.actions.ActionReplaceSubtypeT;
import com.armedia.caliente.engine.transform.xml.actions.ActionSetAttributeT;
import com.armedia.caliente.engine.transform.xml.actions.ActionSetSubtypeT;
import com.armedia.caliente.engine.transform.xml.actions.ActionSetValueMappingT;
import com.armedia.caliente.engine.transform.xml.actions.ActionSetVariableT;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "transformation.t", propOrder = {
	"transformations"
})
public class TransformationT extends ConditionalActionT {

	@XmlElements({
		@XmlElement(name = "set-subtype", type = ActionSetSubtypeT.class),
		@XmlElement(name = "replace-subtype", type = ActionReplaceSubtypeT.class),
		@XmlElement(name = "add-decorator", type = ActionAddDecoratorT.class),
		@XmlElement(name = "remove-decorator", type = ActionRemoveDecoratorT.class),
		@XmlElement(name = "replace-decorator", type = ActionReplaceDecoratorT.class),
		@XmlElement(name = "set-attribute", type = ActionSetAttributeT.class),
		@XmlElement(name = "remove-attribute", type = ActionRemoveAttributeT.class),
		@XmlElement(name = "replace-attribute", type = ActionReplaceAttributeT.class),
		@XmlElement(name = "map-attribute-value", type = ActionMapAttributeValueT.class),
		@XmlElement(name = "set-variable", type = ActionSetVariableT.class),
		@XmlElement(name = "clear-variable", type = ActionClearVariableT.class),
		@XmlElement(name = "set-value-mapping", type = ActionSetValueMappingT.class),
		@XmlElement(name = "clear-value-mapping", type = ActionClearValueMappingT.class),
		@XmlElement(name = "apply-value-mapping", type = ActionApplyValueMappingT.class),
		@XmlElement(name = "custom-action", type = ActionCustomActionT.class)
	})
	protected List<ConditionalActionT> transformations;

	public List<ConditionalActionT> getTransformations() {
		if (this.transformations == null) {
			this.transformations = new ArrayList<>();
		}
		return this.transformations;
	}

	@Override
	protected final void applyTransformation(TransformationContext ctx) {
		for (ConditionalActionT action : getTransformations()) {
			if (action != null) {
				action.applyTransformation(ctx);
			}
		}
	}
}