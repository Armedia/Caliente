
package com.armedia.caliente.engine.transform.xml.actions;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;

import org.apache.commons.lang3.StringUtils;

import com.armedia.caliente.engine.transform.TransformationContext;
import com.armedia.caliente.engine.transform.TransformationException;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "actionSetSubtype.t", propOrder = {
	"value"
})
public class SubtypeSet extends AbstractSingleValueSet {

	@Override
	protected void setNewValue(TransformationContext ctx, String newValue) throws TransformationException {
		newValue = StringUtils.strip(newValue);
		if (StringUtils.isEmpty(newValue)) { throw new TransformationException("Empty value given to set"); }
		ctx.getObject().setSubtype(newValue);
	}

}