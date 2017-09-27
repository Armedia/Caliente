
package com.armedia.caliente.engine.transform.xml;

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
public class ExpressionT {
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
		return Tools.coalesce(this.lang, ExpressionT.CONSTANT);
	}

	public void setLang(String value) {
		this.lang = value;
	}

	public Object evaluate(TransformationContext ctx) {
		// First: if the language is "constant" or null, we return the literal string value
		String language = Tools.coalesce(getLang(), ExpressionT.CONSTANT);
		if (ExpressionT.CONSTANT.equalsIgnoreCase(language)) { return this.value; }

		ScriptEngine engine = ExpressionT.ENGINE_FACTORY.getEngineByName(this.lang);
		if (engine == null) { throw new RuntimeTransformationException(
			String.format("No script engine [%s] is available", language)); }
		try {
			return engine.eval(StringUtils.strip(this.value));
		} catch (ScriptException e) {
			throw new RuntimeTransformationException(e);
		}
	}
}