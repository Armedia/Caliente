
package com.armedia.caliente.engine.transform.xml;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

import com.armedia.caliente.store.CmfAttribute;
import com.armedia.caliente.store.CmfDataType;

/**
 * <p>
 * Java class for setValue.t complex type.
 *
 * <p>
 * The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType name="setValue.t">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="name" type="{http://www.armedia.com/ns/caliente/engine/transform}expression.t"/>
 *         &lt;element name="value" type="{http://www.armedia.com/ns/caliente/engine/transform}expression.t"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 *
 *
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "setValue.t", propOrder = {
	"name", "value"
})
public class SetValueT implements Transformation {

	@XmlElement(required = true)
	protected ExpressionT name;

	@XmlElement(required = true)
	protected ExpressionT value;

	/**
	 * Gets the value of the name property.
	 *
	 * @return possible object is {@link ExpressionT }
	 *
	 */
	public ExpressionT getName() {
		return this.name;
	}

	/**
	 * Sets the value of the name property.
	 *
	 * @param value
	 *            allowed object is {@link ExpressionT }
	 *
	 */
	public void setName(ExpressionT value) {
		this.name = value;
	}

	/**
	 * Gets the value of the value property.
	 *
	 * @return possible object is {@link ExpressionT }
	 *
	 */
	public ExpressionT getValue() {
		return this.value;
	}

	/**
	 * Sets the value of the value property.
	 *
	 * @param value
	 *            allowed object is {@link ExpressionT }
	 *
	 */
	public void setValue(ExpressionT value) {
		this.value = value;
	}

	@Override
	public <V> void apply(TransformationContext<V> ctx) {
		ExpressionT name = getName();
		if (name == null) { return; }
		ExpressionT value = getValue();
		if (value == null) { return; }
		CmfDataType type = null;
		boolean repeating = false;
		CmfAttribute<V> att = ctx.getObject().getOrCreateAttribute(name.evaluate(ctx), type, repeating);
		att.hashCode();
	}

}