package com.armedia.caliente.engine.dynamic.xml.actions;

import java.util.Map;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;

import com.armedia.caliente.engine.dynamic.DynamicElementContext;
import com.armedia.caliente.engine.dynamic.DynamicValue;
import com.armedia.caliente.store.CmfValue;

public class InternalPropertyActions {

	@XmlAccessorType(XmlAccessType.FIELD)
	@XmlType(name = "actionCopyInternalProperty.t", propOrder = {
		"from", "to"
	})
	public static class Copy extends AbstractCopyRenameValue {
		@Override
		protected Map<String, DynamicValue> getCandidateValues(DynamicElementContext<?> ctx) {
			return ctx.getDynamicObject().getPriv();
		}

		@Override
		protected void storeValue(DynamicElementContext<?> ctx, DynamicValue src, DynamicValue copy) {
			ctx.getDynamicObject().getPriv().put(copy.getName(), copy);
		}
	}

	@XmlAccessorType(XmlAccessType.FIELD)
	@XmlType(name = "actionJoinInternalProperty.t", propOrder = {
		"comparison", "name", "separator", "keepEmpty"
	})
	public static class Join extends AbstractJoinValueAttribute {
		@Override
		protected Map<String, DynamicValue> getCandidateValues(DynamicElementContext<?> ctx) {
			return ctx.getDynamicObject().getPriv();
		}
	}

	@XmlAccessorType(XmlAccessType.FIELD)
	@XmlType(name = "actionRemoveInternalProperty.t", propOrder = {
		"comparison", "name"
	})
	public static class Remove extends AbstractTransformValueAttribute {
		@Override
		protected DynamicValue executeAction(DynamicElementContext<?> ctx, DynamicValue candidate) {
			ctx.getDynamicObject().getPriv().remove(candidate.getName());
			return null;
		}
	}

	@XmlAccessorType(XmlAccessType.FIELD)
	@XmlType(name = "actionRenameInternalProperty.t")
	public static class Rename extends Copy {
		@Override
		protected void storeValue(DynamicElementContext<?> ctx, DynamicValue src, DynamicValue copy) {
			ctx.getDynamicObject().getPriv().remove(src.getName());
			super.storeValue(ctx, src, copy);
		}
	}

	@XmlAccessorType(XmlAccessType.FIELD)
	@XmlType(name = "actionReplaceInternalProperty.t", propOrder = {
		"comparison", "name", "cardinality", "regex", "replacement"
	})
	public static class Replace extends AbstractReplaceValue {
		@Override
		protected final Map<String, DynamicValue> getCandidateValues(DynamicElementContext<?> ctx) {
			return ctx.getDynamicObject().getPriv();
		}
	}

	@XmlAccessorType(XmlAccessType.FIELD)
	@XmlType(name = "actionSetInternalProperty.t", propOrder = {
		"name", "type", "value"
	})
	public static class Set extends AbstractSetValue {
		@Override
		protected DynamicValue createValue(DynamicElementContext<?> ctx, String name, CmfValue.Type type,
			boolean multivalue) {
			DynamicValue member = new DynamicValue(name, type, multivalue);
			ctx.getDynamicObject().getPriv().put(name, member);
			return member;
		}
	}

	@XmlAccessorType(XmlAccessType.FIELD)
	@XmlType(name = "actionSplitInternalProperty.t", propOrder = {
		"comparison", "name", "separator", "keepEmpty"
	})
	public static class Split extends AbstractSplitValueAttribute {
		@Override
		protected Map<String, DynamicValue> getCandidateValues(DynamicElementContext<?> ctx) {
			return ctx.getDynamicObject().getPriv();
		}
	}

	@XmlAccessorType(XmlAccessType.FIELD)
	@XmlType(name = "actionMapInternalPropertyValue.t", propOrder = {
		"comparison", "name", "cardinality", "cases", "defVal"
	})
	public static class MapValue extends AbstractMapValue {
		@Override
		protected final Map<String, DynamicValue> getCandidateValues(DynamicElementContext<?> ctx) {
			return ctx.getDynamicObject().getPriv();
		}
	}
}