package com.armedia.caliente.engine.dynamic.xml.actions;

import java.util.Map;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;

import com.armedia.caliente.engine.dynamic.DynamicElementContext;
import com.armedia.caliente.engine.dynamic.DynamicValue;

public class AttributeActions {

	@XmlAccessorType(XmlAccessType.FIELD)
	@XmlType(name = "actionCopyAttribute.t", propOrder = {
		"from", "to"
	})
	public static class Copy extends AbstractCopyRenameValue {
		@Override
		protected Map<String, DynamicValue> getCandidateValues(DynamicElementContext<?> ctx) {
			return ctx.getDynamicObject().getAtt();
		}

		@Override
		protected void storeValue(DynamicElementContext<?> ctx, DynamicValue src, DynamicValue copy) {
			ctx.getDynamicObject().getAtt().put(copy.getName(), copy);
		}
	}

	@XmlAccessorType(XmlAccessType.FIELD)
	@XmlType(name = "actionJoinAttribute.t", propOrder = {
		"comparison", "name", "separator", "keepEmpty"
	})
	public static class Join extends AbstractJoinValueAttribute {
		@Override
		protected Map<String, DynamicValue> getCandidateValues(DynamicElementContext<?> ctx) {
			return ctx.getDynamicObject().getAtt();
		}
	}

	@XmlAccessorType(XmlAccessType.FIELD)
	@XmlType(name = "actionRemoveAttribute.t", propOrder = {
		"comparison", "name"
	})
	public static class Remove extends AbstractTransformValueAttribute {
		@Override
		protected DynamicValue executeAction(DynamicElementContext<?> ctx, DynamicValue candidate) {
			ctx.getDynamicObject().getAtt().remove(candidate.getName());
			return null;
		}
	}

	@XmlAccessorType(XmlAccessType.FIELD)
	@XmlType(name = "actionRenameAttribute.t")
	public static class Rename extends Copy {
		@Override
		protected void storeValue(DynamicElementContext<?> ctx, DynamicValue src, DynamicValue copy) {
			ctx.getDynamicObject().getAtt().remove(src.getName());
			super.storeValue(ctx, src, copy);
		}
	}

	@XmlAccessorType(XmlAccessType.FIELD)
	@XmlType(name = "actionReplaceAttribute.t", propOrder = {
		"comparison", "name", "cardinality", "regex", "replacement"
	})
	public static class Replace extends AbstractReplaceValue {
		@Override
		protected final Map<String, DynamicValue> getCandidateValues(DynamicElementContext<?> ctx) {
			return ctx.getDynamicObject().getAtt();
		}
	}

	@XmlAccessorType(XmlAccessType.FIELD)
	@XmlType(name = "actionSetAttribute.t", propOrder = {
		"name", "type", "value"
	})
	public static class Set extends AbstractSetValue {
		@Override
		protected void storeValue(DynamicElementContext<?> ctx, DynamicValue value) {
			ctx.getDynamicObject().getAtt().put(value.getName(), value);
		}
	}

	@XmlAccessorType(XmlAccessType.FIELD)
	@XmlType(name = "actionSplitAttribute.t", propOrder = {
		"comparison", "name", "separator", "keepEmpty"
	})
	public static class Split extends AbstractSplitValueAttribute {
		@Override
		protected Map<String, DynamicValue> getCandidateValues(DynamicElementContext<?> ctx) {
			return ctx.getDynamicObject().getAtt();
		}
	}

	@XmlAccessorType(XmlAccessType.FIELD)
	@XmlType(name = "actionMapAttributeValue.t", propOrder = {
		"comparison", "name", "cardinality", "cases", "defVal"
	})
	public static class MapValue extends AbstractMapValue {

		@Override
		protected final Map<String, DynamicValue> getCandidateValues(DynamicElementContext<?> ctx) {
			return ctx.getDynamicObject().getAtt();
		}

	}
}