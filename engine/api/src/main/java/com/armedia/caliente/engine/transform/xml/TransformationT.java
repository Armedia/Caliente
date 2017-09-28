
package com.armedia.caliente.engine.transform.xml;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlType;

import com.armedia.caliente.engine.transform.TransformationContext;
import com.armedia.caliente.engine.transform.xml.actions.AddDecorator;
import com.armedia.caliente.engine.transform.xml.actions.ApplyValueMapping;
import com.armedia.caliente.engine.transform.xml.actions.ClearValueMapping;
import com.armedia.caliente.engine.transform.xml.actions.ClearVariable;
import com.armedia.caliente.engine.transform.xml.actions.CustomAction;
import com.armedia.caliente.engine.transform.xml.actions.MapAttributeValue;
import com.armedia.caliente.engine.transform.xml.actions.RemoveAttribute;
import com.armedia.caliente.engine.transform.xml.actions.RemoveDecorator;
import com.armedia.caliente.engine.transform.xml.actions.ReplaceAttribute;
import com.armedia.caliente.engine.transform.xml.actions.ReplaceDecorator;
import com.armedia.caliente.engine.transform.xml.actions.ReplaceSubtype;
import com.armedia.caliente.engine.transform.xml.actions.SetAttribute;
import com.armedia.caliente.engine.transform.xml.actions.SetSubtype;
import com.armedia.caliente.engine.transform.xml.actions.SetValueMapping;
import com.armedia.caliente.engine.transform.xml.actions.SetVariable;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "transformation.t", propOrder = {
	"transformations"
})
public class TransformationT extends ConditionalActionT {

	@XmlElements({
		@XmlElement(name = "set-subtype", type = SetSubtype.class),
		@XmlElement(name = "replace-subtype", type = ReplaceSubtype.class),
		@XmlElement(name = "add-decorator", type = AddDecorator.class),
		@XmlElement(name = "remove-decorator", type = RemoveDecorator.class),
		@XmlElement(name = "replace-decorator", type = ReplaceDecorator.class),
		@XmlElement(name = "set-attribute", type = SetAttribute.class),
		@XmlElement(name = "remove-attribute", type = RemoveAttribute.class),
		@XmlElement(name = "replace-attribute", type = ReplaceAttribute.class),
		@XmlElement(name = "map-attribute-value", type = MapAttributeValue.class),
		@XmlElement(name = "set-variable", type = SetVariable.class),
		@XmlElement(name = "clear-variable", type = ClearVariable.class),
		@XmlElement(name = "set-value-mapping", type = SetValueMapping.class),
		@XmlElement(name = "clear-value-mapping", type = ClearValueMapping.class),
		@XmlElement(name = "apply-value-mapping", type = ApplyValueMapping.class),
		@XmlElement(name = "custom-action", type = CustomAction.class)
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