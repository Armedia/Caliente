
package com.armedia.caliente.engine.transform.xml.actions;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;

import com.armedia.caliente.engine.transform.ObjectDataMember;
import com.armedia.caliente.engine.transform.TransformationContext;
import com.armedia.caliente.store.CmfDataType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "actionSetAttribute.t", propOrder = {
	"name", "type", "value"
})
public class AttributeSet extends AbstractSetValue {

	@Override
	protected ObjectDataMember createValue(TransformationContext ctx, String name, CmfDataType type,
		boolean multivalue) {
		ObjectDataMember member = new ObjectDataMember(name, type, multivalue);
		ctx.getObject().getAtt().put(name, member);
		return member;
	}

}