
package com.armedia.caliente.engine.transform.xml;

import java.io.PrintWriter;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
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
import com.armedia.caliente.engine.transform.TransformationException;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "expression.t", propOrder = {
	"script"
})
public class Expression {
	private static final ScriptEngineManager ENGINE_FACTORY = new ScriptEngineManager();

	private static final String NL = String.format("%n");

	public static final Expression NULL = new Expression() {

		{
			this.lang = null;
			this.script = null;
		}

		@Override
		public void setScript(String script) {
			// Do nothing...
		}

		@Override
		public void setLang(String lang) {
			// Do nothing...
		}
	};

	@XmlTransient
	protected final Logger log = LoggerFactory.getLogger(getClass());

	@XmlValue
	protected String script;

	@XmlAttribute(name = "lang")
	protected String lang;

	@XmlTransient
	private volatile ScriptEngine engine = null;

	protected void beforeMarshal(Marshaller m) {
		this.lang = StringUtils.strip(this.lang);
	}

	protected void afterUnmarshal(Unmarshaller u, Object parent) {
		this.lang = StringUtils.strip(this.lang);
		if (this.lang != null) {
			this.engine = Expression.ENGINE_FACTORY.getEngineByName(this.lang);
			if (this.engine == null) {
				Map<String, List<String>> m = new TreeMap<>();
				for (ScriptEngineFactory f : Expression.ENGINE_FACTORY.getEngineFactories()) {
					m.put(String.format("%s %s", f.getLanguageName(), f.getLanguageVersion()), f.getNames());
				}
				throw new RuntimeTransformationException(
					String.format("Unknown script language [%s] - supported languages: %s", this.lang, m));
			}
		}
	}

	public String getScript() {
		return this.script;
	}

	public void setScript(String script) {
		this.script = script;
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
		final String language = getLang();
		if (language == null) { return null; }
		if (this.engine == null) {
			synchronized (this) {
				if (this.engine == null) {
					this.engine = Expression.ENGINE_FACTORY.getEngineByName(language);
					if (this.engine == null) { throw new RuntimeTransformationException(
						String.format("Unknown script language [%s]", language)); }
				}
			}
		}
		return this.engine;
	}

	private Object evaluate(TransformationContext ctx) throws TransformationException {
		// First: if the language is "constant" or null, we return the literal string script
		final String script = StringUtils.strip(getScript());
		final ScriptEngine engine = getEngine();

		// If there is no engine needed, then we simply return the contents of the script as a
		// literal string script
		if (engine == null) { return script; }

		// We have an engine...so use it!!
		final String lang = getLang();

		final ScriptContext scriptCtx = engine.getContext();
		scriptCtx.setWriter(new PrintWriter(System.out));
		scriptCtx.setErrorWriter(new PrintWriter(System.err));

		final Bindings bindings = engine.createBindings();
		bindings.put("obj", ctx.getObject());
		bindings.put("var", ctx.getVariables());
		bindings.put("mapper", ctx.getAttributeMapper());
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
			String msg = String.format("Exception caught while evaluating %s expression script:%n%s%n", lang, script);
			this.log.debug(msg, e);
			throw new TransformationException(msg, e);
		}
	}

	public static Object eval(Expression e, TransformationContext ctx) throws TransformationException {
		Objects.requireNonNull(ctx, "No transformation context given for expression evaluation");
		if (e == null) { return null; }
		return e.evaluate(ctx);
	}
}