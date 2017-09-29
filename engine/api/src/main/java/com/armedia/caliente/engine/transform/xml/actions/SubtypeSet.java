
package com.armedia.caliente.engine.transform.xml.actions;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

import com.armedia.caliente.engine.transform.TransformationContext;
import com.armedia.caliente.engine.transform.TransformationException;
import com.armedia.caliente.engine.transform.xml.ConditionalAction;
import com.armedia.caliente.engine.transform.xml.Expression;
import com.armedia.commons.utilities.Tools;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "actionSetSubtype.t", propOrder = {
	"subtype"
})
public class SubtypeSet extends ConditionalAction {

	@XmlElement(name = "subtype", required = true)
	protected Expression subtype;

	public Expression getSubtype() {
		return this.subtype;
	}

	public void setSubtype(Expression value) {
		this.subtype = value;
	}

	@Override
	protected void applyTransformation(TransformationContext ctx) throws TransformationException {
		String subtype = Tools.toString(Expression.eval(getSubtype(), ctx));
		if (subtype == null) { throw new TransformationException("No subtype given to set to"); }
		ctx.setSubtype(subtype);
	}

}