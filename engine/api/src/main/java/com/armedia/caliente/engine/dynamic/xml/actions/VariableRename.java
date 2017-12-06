package com.armedia.caliente.engine.dynamic.xml.actions;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;

import com.armedia.caliente.engine.dynamic.DynamicElementContext;
import com.armedia.caliente.engine.dynamic.DynamicValue;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "actionRenameVariable.t")
public class VariableRename extends VariableCopy {

	@Override
	protected void storeValue(DynamicElementContext ctx, DynamicValue src, DynamicValue copy) {
		ctx.getVariables().remove(src.getName());
		super.storeValue(ctx, src, copy);
	}

}