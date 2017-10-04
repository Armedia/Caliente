
package com.armedia.caliente.engine.transform.xml.conditions;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import com.armedia.caliente.engine.transform.TransformationContext;
import com.armedia.caliente.engine.transform.TransformationException;
import com.armedia.caliente.engine.transform.xml.Condition;
import com.armedia.caliente.engine.transform.xml.Expression;
import com.armedia.caliente.store.CmfValueMapper;
import com.armedia.caliente.store.CmfType;
import com.armedia.caliente.store.xml.CmfTypeAdapter;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "conditionHasValueMapping.t", propOrder = {
	"type", "name", "from", "to"
})
public class HasValueMapping implements Condition {

	@XmlElement(name = "type", required = false)
	@XmlJavaTypeAdapter(CmfTypeAdapter.class)
	protected CmfType type;

	@XmlElement(name = "name", required = true)
	protected Expression name;

	@XmlElement(name = "name", required = false)
	protected Expression from;

	@XmlElement(name = "name", required = false)
	protected Expression to;

	public void setType(CmfType type) {
		this.type = type;
	}

	public CmfType getType() {
		return this.type;
	}

	public Expression getName() {
		return this.name;
	}

	public void setName(Expression name) {
		this.name = name;
	}

	public Expression getFrom() {
		return this.from;
	}

	public void setFrom(Expression from) {
		this.from = from;
	}

	public Expression getTo() {
		return this.to;
	}

	public void setTo(Expression to) {
		this.to = to;
	}

	@Override
	public boolean check(TransformationContext ctx) throws TransformationException {
		CmfType type = getType();
		if (type == null) { throw new TransformationException("No type given to find the mappings with"); }

		Object name = Expression.eval(getName(), ctx);
		if (name == null) { throw new TransformationException("No name given to check for"); }

		CmfValueMapper mapper = ctx.getAttributeMapper();
		Expression key = null;

		key = getFrom();
		if (key != null) {
			Object sourceValue = Expression.eval(key, ctx);
			if (sourceValue == null) { throw new TransformationException("No source value given to search with"); }
			return (mapper.getTargetMapping(getType(), name.toString(), sourceValue.toString()) != null);
		}

		key = getTo();
		if (key != null) {
			Object targetValue = Expression.eval(key, ctx);
			if (targetValue == null) { throw new TransformationException("No target value given to search with"); }
			return (mapper.getSourceMapping(getType(), name.toString(), targetValue.toString()) != null);
		}

		throw new TransformationException("No source or target value given to search with");
	}
}