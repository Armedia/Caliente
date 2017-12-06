
package com.armedia.caliente.engine.dynamic.xml.actions;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;

import com.armedia.caliente.engine.dynamic.DynamicElementContext;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "actionMapSubtype.t", propOrder = {
	"cases", "defVal"
})
public class MapSubtype extends AbstractMapSubtype {

	@Override
	protected Object getCandidate(DynamicElementContext ctx) {
		return ctx.getDynamicObject().getSubtype();
	}

}