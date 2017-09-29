
package com.armedia.caliente.engine.transform.xml;

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlValue;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.caliente.engine.transform.RuntimeTransformationException;
import com.armedia.caliente.engine.transform.TransformationContext;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "expression.t", propOrder = {
	"value"
})
public class Expression {
	private static final ScriptEngineManager ENGINE_FACTORY = new ScriptEngineManager();

	private static final String NL = String.format("%n");

	@XmlTransient
	protected final Logger log = LoggerFactory.getLogger(getClass());

	@XmlValue
	protected String value;

	@XmlAttribute(name = "lang")
	protected String lang;

	@XmlTransient
	private ScriptEngine engine = null;

	protected void beforeMarshal(Marshaller m) {
		this.lang = StringUtils.strip(this.lang);
	}

	protected void afterUnmarshal(Unmarshaller u, Object parent) {
		this.lang = StringUtils.strip(this.lang);
		if (this.lang != null) {
			this.engine = Expression.ENGINE_FACTORY.getEngineByName(this.lang);
			if (this.engine == null) { throw new RuntimeTransformationException(
				String.format("Unknown script language [%s]", this.lang)); }
		}
	}

	public String getValue() {
		return this.value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public String getLang() {
		return StringUtils.strip(this.lang);
	}

	public void setLang(String lang) {
		lang = StringUtils.strip(lang);
		ScriptEngine engine = null;
		if (lang != null) {
			engine = Expression.ENGINE_FACTORY.getEngineByName(lang);
			if (engine == null) { throw new IllegalArgumentException(
				String.format("Unknown script language [%s]", lang)); }
		}
		this.lang = lang;
		this.engine = engine;
	}

	private ScriptEngine getEngine() {
		if (this.engine == null) {
			final String language = getLang();
			if (language != null) {
				this.engine = Expression.ENGINE_FACTORY.getEngineByName(language);
				if (this.engine == null) { throw new RuntimeTransformationException(
					String.format("Unknown script language [%s]", language)); }
			}
		}
		return this.engine;
	}

	public Object evaluate(TransformationContext ctx) {
		// First: if the language is "constant" or null, we return the literal string value
		final String script = StringUtils.strip(this.value);
		final ScriptEngine engine = getEngine();

		// If there is no engine needed, then we simply return the contents of the script as a
		// literal string value
		if (engine == null) { return script; }

		// We have an engine...so use it!!
		final String lang = getLang();
		final Bindings bindings = engine.getBindings(ScriptContext.ENGINE_SCOPE);
		// TODO: Set the bindings and context for this engine instance based on the transformation
		// context...need to define "how" things will be accessible within the script
		// * An object called "object" which contains:
		// ** a map called "attributes"
		// *** each element will either be a single value or a list
		// ** a map called "calienteProperties"
		// *** each element will either be a single value or a list
		// * A map called "variables"
		// ** each element will either be a single value or a list
		// * A string called "subtype"
		// * A list (set?) called "decorators"
		engine.setBindings(bindings, ScriptContext.ENGINE_SCOPE);

		try {
			this.log.trace("Evaluating {} expression script:{}{}{}", lang, Expression.NL, script, Expression.NL);
			Object ret = engine.eval(script);
			if (ret != null) {
				this.log.trace("Returned [{}] from {} expression script:{}{}{}", ret, lang, Expression.NL, script,
					Expression.NL);
			} else {
				this.log.trace("Returned <null> from {} expression script:{}{}{}", lang, Expression.NL, script,
					Expression.NL);
			}
			return ret;
		} catch (ScriptException e) {
			this.log.debug("Exception caught while evaluating {} expression script:{}{}{}", lang, Expression.NL, script,
				Expression.NL, e);
			throw new RuntimeTransformationException(e);
		}
	}
}