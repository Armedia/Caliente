
package com.armedia.caliente.engine.transform.xml;

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlValue;

import org.apache.commons.lang3.StringUtils;

import com.armedia.caliente.engine.transform.RuntimeTransformationException;
import com.armedia.caliente.engine.transform.TransformationContext;
import com.armedia.commons.utilities.Tools;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "expression.t", propOrder = {
	"value"
})
public class Expression {
	private static final ScriptEngineManager ENGINE_FACTORY = new ScriptEngineManager();

	protected static final String CONSTANT = "const";

	@XmlValue
	protected String value;

	@XmlAttribute(name = "lang")
	protected String lang;

	public String getValue() {
		return this.value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public String getLang() {
		return StringUtils.strip(Tools.coalesce(this.lang, Expression.CONSTANT));
	}

	public void setLang(String value) {
		this.lang = value;
	}

	public Object evaluate(TransformationContext ctx) {
		// First: if the language is "constant" or null, we return the literal string value
		String language = getLang();
		if (Expression.CONSTANT.equalsIgnoreCase(language)) { return this.value; }

		ScriptEngine engine = Expression.ENGINE_FACTORY.getEngineByName(language);
		if (engine == null) { throw new RuntimeTransformationException(
			String.format("No script engine [%s] is available", language)); }

		Bindings bindings = engine.getBindings(ScriptContext.ENGINE_SCOPE);
		// TODO: Set the bindings and context for this engine instance based on the transformation
		// context...need to define "how" things will be accessible within the script
		engine.setBindings(bindings, ScriptContext.ENGINE_SCOPE);

		try {
			return engine.eval(StringUtils.strip(this.value));
		} catch (ScriptException e) {
			throw new RuntimeTransformationException(e);
		}
	}
}