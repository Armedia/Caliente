
package com.armedia.caliente.engine.transform.xml.actions;

import java.text.ParseException;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import com.armedia.caliente.engine.transform.RuntimeTransformationException;
import com.armedia.caliente.engine.transform.TransformationContext;
import com.armedia.caliente.engine.transform.xml.ConditionalAction;
import com.armedia.caliente.engine.transform.xml.Expression;
import com.armedia.caliente.store.CmfDataType;
import com.armedia.caliente.store.CmfProperty;
import com.armedia.caliente.store.CmfValue;
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

	protected abstract CmfProperty<CmfValue> createValue(TransformationContext ctx, String name, CmfDataType type,
		boolean multivalue);

	@Override
	protected final void applyTransformation(TransformationContext ctx) {
		Object name = Tools.toString(Expression.eval(getName(), ctx));
		if (name == null) { throw new RuntimeTransformationException(
			"No name expression given for variable definition"); }

		final CmfDataType type = getType();
		final Object value = Expression.eval(getValue(), ctx);
		final boolean repeating = (Iterable.class.isInstance(value) || ((value != null) && value.getClass().isArray()));
		final CmfProperty<CmfValue> variable = createValue(ctx, String.valueOf(name), type, repeating);
		Object currentValue = value;
		try {
			if (repeating) {
				if (Iterable.class.isInstance(value)) {
					for (Object o : Iterable.class.cast(value)) {
						variable.addValue(new CmfValue(type, currentValue = o));
					}
				} else {
					for (Object o : (Object[]) value) {
						variable.addValue(new CmfValue(type, currentValue = o));
					}
				}
			} else {
				variable.setValue(new CmfValue(type, currentValue));
			}
		} catch (ParseException e) {
			throw new RuntimeTransformationException(String.format("Failed to convert value [%s] as a %s", value, type),
				e);
		}
	}

}