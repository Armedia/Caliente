
package com.armedia.caliente.engine.dynamic.xml.actions;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import com.armedia.caliente.engine.dynamic.ActionException;
import com.armedia.caliente.engine.dynamic.DynamicElementContext;
import com.armedia.caliente.engine.dynamic.DynamicValue;
import com.armedia.caliente.engine.dynamic.xml.ConditionalAction;
import com.armedia.caliente.engine.dynamic.xml.Expression;
import com.armedia.caliente.store.CmfValue;
import com.armedia.caliente.store.xml.CmfValueTypeAdapter;
import com.armedia.commons.utilities.Tools;

@XmlTransient
public abstract class AbstractSetValue extends ConditionalAction {

	@XmlElement(name = "name", required = true)
	protected Expression name;

	@XmlElement(name = "type", required = false)
	@XmlJavaTypeAdapter(CmfValueTypeAdapter.class)
	protected CmfValue.Type type;

	@XmlElement(name = "value", required = true)
	protected Expression value;

	public Expression getName() {
		return this.name;
	}

	public void setName(Expression value) {
		this.name = value;
	}

	public CmfValue.Type getType() {
		return Tools.coalesce(this.type, CmfValue.Type.STRING);
	}

	public void setDataType(CmfValue.Type value) {
		this.type = value;
	}

	public Expression getValue() {
		return this.value;
	}

	public void setValue(Expression value) {
		this.value = value;
	}

	protected abstract DynamicValue createValue(DynamicElementContext ctx, String name, CmfValue.Type type,
		boolean multivalue);

	private Iterable<?> toIterable(Object o) {
		if (o == null) { return null; }
		if (Iterable.class.isInstance(o)) { return Iterable.class.cast(o); }
		if (o.getClass().isArray()) { return Arrays.asList((Object[]) o); }
		return Collections.singletonList(o);
	}

	private Object fromIterable(Object o) {
		if (o == null) { return null; }
		if (Iterable.class.isInstance(o) || o.getClass().isArray()) {
			Iterable<?> iterable = (Iterable.class.isInstance(o) ? Iterable.class.cast(o)
				: Arrays.asList((Object[]) o));
			Iterator<?> iterator = iterable.iterator();
			if (iterator.hasNext()) {
				o = iterator.next();
			}
		}
		return o;
	}

	@Override
	protected final void executeAction(DynamicElementContext ctx) throws ActionException {
		Object name = Tools.toString(ActionTools.eval(getName(), ctx));
		if (name == null) { throw new ActionException("No name expression given for variable definition"); }

		final CmfValue.Type type = getType();
		final Object value = ActionTools.eval(getValue(), ctx);
		final boolean repeating = (Iterable.class.isInstance(value) || ((value != null) && value.getClass().isArray()));
		final DynamicValue variable = createValue(ctx, String.valueOf(name), type, repeating);
		if (value != null) {
			if (repeating) {
				// Make sure we take all available values
				variable.setValues(toIterable(value));
			} else {
				// Make sure we take only the first value
				variable.setValue(fromIterable(value));
			}
		}
		ctx.getDynamicObject().getAtt().put(variable.getName(), variable);
	}

}