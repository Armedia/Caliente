
package com.armedia.caliente.engine.dynamic.jaxb.actions;

import java.util.Arrays;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import com.armedia.caliente.engine.dynamic.ActionException;
import com.armedia.caliente.engine.dynamic.ObjectContext;
import com.armedia.caliente.engine.dynamic.TypedValue;
import com.armedia.caliente.engine.dynamic.jaxb.ConditionalAction;
import com.armedia.caliente.engine.dynamic.jaxb.Expression;
import com.armedia.caliente.store.CmfDataType;
import com.armedia.caliente.store.xml.CmfDataTypeAdapter;
import com.armedia.commons.utilities.Tools;

@XmlTransient
public abstract class AbstractSetValue extends ConditionalAction {

	@XmlElement(name = "name", required = true)
	protected Expression name;

	@XmlElement(name = "type", required = false)
	@XmlJavaTypeAdapter(CmfDataTypeAdapter.class)
	protected CmfDataType type;

	@XmlElement(name = "value", required = true)
	protected Expression value;

	public Expression getName() {
		return this.name;
	}

	public void setName(Expression value) {
		this.name = value;
	}

	public CmfDataType getType() {
		return Tools.coalesce(this.type, CmfDataType.STRING);
	}

	public void setDataType(CmfDataType value) {
		this.type = value;
	}

	public Expression getValue() {
		return this.value;
	}

	public void setValue(Expression value) {
		this.value = value;
	}

	protected abstract TypedValue createValue(ObjectContext ctx, String name, CmfDataType type,
		boolean multivalue);

	@Override
	protected final void applyTransformation(ObjectContext ctx) throws ActionException {
		Object name = Tools.toString(ActionTools.eval(getName(), ctx));
		if (name == null) { throw new ActionException("No name expression given for variable definition"); }

		final CmfDataType type = getType();
		final Object value = ActionTools.eval(getValue(), ctx);
		final boolean repeating = (Iterable.class.isInstance(value) || ((value != null) && value.getClass().isArray()));
		final TypedValue variable = createValue(ctx, String.valueOf(name), type, repeating);
		if (repeating) {
			if (Iterable.class.isInstance(value)) {
				variable.setValues(Iterable.class.cast(value));
			} else {
				variable.setValues(Arrays.asList((Object[]) value));
			}
		} else {
			variable.setValue(value);
		}
		ctx.getTransformableObject().getAtt().put(variable.getName(), variable);
	}

}