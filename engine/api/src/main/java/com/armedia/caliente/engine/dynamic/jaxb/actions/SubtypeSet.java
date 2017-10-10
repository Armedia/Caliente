
package com.armedia.caliente.engine.dynamic.jaxb.actions;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;

import org.apache.commons.lang3.StringUtils;

import com.armedia.caliente.engine.dynamic.ActionException;
import com.armedia.caliente.engine.dynamic.ObjectContext;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "actionSetSubtype.t", propOrder = {
	"value"
})
public class SubtypeSet extends AbstractSingleValueSet {

	@Override
	protected void setNewValue(ObjectContext ctx, String newValue) throws ActionException {
		newValue = StringUtils.strip(newValue);
		if (StringUtils.isEmpty(newValue)) { throw new ActionException("Empty value given to set"); }
		ctx.getTransformableObject().setSubtype(newValue);
	}

}