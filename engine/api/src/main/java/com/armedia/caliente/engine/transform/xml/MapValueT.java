
package com.armedia.caliente.engine.transform.xml;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

import com.armedia.caliente.store.CmfAttribute;
import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.store.CmfValue;
import com.armedia.caliente.store.CmfValueCodec;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "mapValue.t", propOrder = {
	"target", "selector"
})
public class MapValueT implements Transformation {

	@XmlElement(required = true)
	protected ExpressionT target;

	@XmlElement(name = "switch", required = true)
	protected SwitchValueT selector;

	public ExpressionT getTarget() {
		return this.target;
	}

	public void setTarget(ExpressionT target) {
		this.target = target;
	}

	public SwitchValueT getSwitch() {
		return this.selector;
	}

	public void setSwitch(SwitchValueT value) {
		this.selector = value;
	}

	@Override
	public <V> void apply(TransformationContext<V> ctx) {
		String attName = getTarget().evaluate(ctx);
		CmfAttribute<V> att = ctx.getObject().getAttribute(attName);
		// No transformation to apply
		if (att == null) { return; }

		CmfAttribute<V> newAtt = new CmfAttribute<>(att);
		CmfObject<V> object = ctx.getObject();
		CmfValueCodec<V> codec = object.getTranslator().getCodec(newAtt.getType());
		for (V v : att) {
			CmfValue cv = codec.encodeValue(v);
			String newVal = this.selector.selectValue(ctx, cv.asString());
			v = codec.decodeValue(new CmfValue(newVal));
			if (newAtt.isRepeating()) {
				newAtt.addValue(v);
			} else {
				newAtt.setValue(v);
			}
		}
		object.setAttribute(newAtt);
	}

}