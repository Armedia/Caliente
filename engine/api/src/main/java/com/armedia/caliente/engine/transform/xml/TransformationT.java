
package com.armedia.caliente.engine.transform.xml;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlType;

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
	protected final <V> void applyTransformation(TransformationContext<V> ctx) {
		for (ConditionalActionT action : getTransformations()) {
			if (action != null) {
				action.applyTransformation(ctx);
			}
		}
	}
}