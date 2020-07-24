package com.armedia.caliente.engine.dynamic.xml.actions;

import java.util.Map;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;

import com.armedia.caliente.engine.dynamic.DynamicElementContext;
import com.armedia.caliente.engine.dynamic.DynamicValue;

public class VariableActions {

	@XmlAccessorType(XmlAccessType.FIELD)
	@XmlType(name = "actionCopyVariable.t", propOrder = {
		"from", "to"
	})
	public static class Copy extends AbstractCopyRenameValue {
		@Override
		protected Map<String, DynamicValue> getCandidateValues(DynamicElementContext<?> ctx) {
			return ctx.getVariables();
		}

		@Override
		protected void storeValue(DynamicElementContext<?> ctx, DynamicValue src, DynamicValue copy) {
			ctx.getVariables().put(copy.getName(), copy);
		}
	}

	@XmlAccessorType(XmlAccessType.FIELD)
	@XmlType(name = "actionJoinVariable.t", propOrder = {
		"comparison", "name", "separator", "keepEmpty"
	})
	public static class Join extends AbstractJoinValueAttribute {
		@Override
		protected Map<String, DynamicValue> getCandidateValues(DynamicElementContext<?> ctx) {
			return ctx.getVariables();
		}
	}

	@XmlAccessorType(XmlAccessType.FIELD)
	@XmlType(name = "actionRemoveVariable.t", propOrder = {
		"comparison", "name"
	})
	public static class Remove extends AbstractTransformValueAttribute {
		@Override
		protected DynamicValue executeAction(DynamicElementContext<?> ctx, DynamicValue candidate) {
			ctx.getVariables().remove(candidate.getName());
			return null;
		}
	}

	@XmlAccessorType(XmlAccessType.FIELD)
	@XmlType(name = "actionRenameVariable.t")
	public static class Rename extends Copy {
		@Override
		protected void storeValue(DynamicElementContext<?> ctx, DynamicValue src, DynamicValue copy) {
			ctx.getVariables().remove(src.getName());
			super.storeValue(ctx, src, copy);
		}
	}

	@XmlAccessorType(XmlAccessType.FIELD)
	@XmlType(name = "actionReplaceVariable.t", propOrder = {
		"comparison", "name", "cardinality", "regex", "replacement"
	})
	public static class Replace extends AbstractReplaceValue {
		@Override
		protected final Map<String, DynamicValue> getCandidateValues(DynamicElementContext<?> ctx) {
			return ctx.getVariables();
		}
	}

	@XmlAccessorType(XmlAccessType.FIELD)
	@XmlType(name = "actionSetVariable.t", propOrder = {
		"name", "type", "value"
	})
	public static class Set extends AbstractSetValue {
		@Override
		protected void storeValue(DynamicElementContext<?> ctx, DynamicValue value) {
			ctx.getVariables().put(value.getName(), value);
		}
	}

	@XmlAccessorType(XmlAccessType.FIELD)
	@XmlType(name = "actionSplitVariable.t", propOrder = {
		"comparison", "name", "separator", "keepEmpty"
	})
	public static class Split extends AbstractSplitValueAttribute {
		@Override
		protected Map<String, DynamicValue> getCandidateValues(DynamicElementContext<?> ctx) {
			return ctx.getVariables();
		}
	}

	@XmlAccessorType(XmlAccessType.FIELD)
	@XmlType(name = "actionMapVariableValue.t", propOrder = {
		"comparison", "name", "cardinality", "cases", "defVal"
	})
	public static class MapValue extends AbstractMapValue {

		@Override
		protected final Map<String, DynamicValue> getCandidateValues(DynamicElementContext<?> ctx) {
			return ctx.getVariables();
		}

	}
}