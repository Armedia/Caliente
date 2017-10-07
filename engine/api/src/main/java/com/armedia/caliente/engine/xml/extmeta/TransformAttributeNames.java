package com.armedia.caliente.engine.xml.extmeta;

import java.util.ArrayList;
import java.util.List;

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

import com.armedia.caliente.engine.xml.Expression.ScriptContextConfig;
import com.armedia.caliente.engine.xml.ExpressionException;
import com.armedia.commons.utilities.Tools;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "externalMetadataTransformNames.t", propOrder = {
	"map", "defaultTransform"
})
public class TransformAttributeNames {

	@XmlElement(name = "map", required = false)
	protected List<MetadataNameMapping> map;

	@XmlElement(name = "default", required = false)
	protected String defaultTransform;

	public List<MetadataNameMapping> getMap() {
		if (this.map == null) {
			this.map = new ArrayList<>();
		}
		return this.map;
	}

	public String getDefaultTransform() {
		return this.defaultTransform;
	}

	public void setDefaultTransform(String value) {
		this.defaultTransform = value;
	}

	public String transformName(final String sqlName) throws ExpressionException {
		for (MetadataNameMapping mapping : getMap()) {
			if (Tools.equals(mapping.from, sqlName)) {
				if (mapping.to == null) { return sqlName; }
				Object value = mapping.to.evaluate(new ScriptContextConfig() {
					@Override
					public void configure(ScriptContext ctx) {
						final Bindings bindings = ctx.getBindings(ScriptContext.ENGINE_SCOPE);
						bindings.put("sqlName", sqlName);
					}
				});
				return Tools.toString(Tools.coalesce(value, sqlName));
			}
		}
		return sqlName;
	}
}