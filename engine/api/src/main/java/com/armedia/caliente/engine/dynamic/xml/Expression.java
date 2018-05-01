
package com.armedia.caliente.engine.dynamic.xml;

import java.io.PrintWriter;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.script.Bindings;
import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.script.SimpleScriptContext;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlValue;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.concurrent.ConcurrentException;
import org.apache.commons.lang3.concurrent.ConcurrentInitializer;
import org.apache.commons.lang3.concurrent.ConcurrentUtils;
import org.apache.commons.lang3.concurrent.LazyInitializer;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.caliente.engine.dynamic.RuntimeDynamicElementException;
import com.armedia.commons.utilities.Tools;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "expression.t", propOrder = {
	"script"
})
public class Expression {
	private static final ScriptEngineManager ENGINE_FACTORY;
	private static final Map<String, String> ENGINE_ALIASES;

	static {
		ENGINE_FACTORY = new ScriptEngineManager();
		Map<String, String> m = new TreeMap<>();
		for (ScriptEngineFactory f : Expression.ENGINE_FACTORY.getEngineFactories()) {
			for (String s : f.getNames()) {
				m.put(StringUtils.lowerCase(s), s);
			}
		}
		ENGINE_ALIASES = Tools.freezeMap(new LinkedHashMap<>(m));
	}

	private static final LazyInitializer<Object> TOOLS = new LazyInitializer<Object>() {
		@Override
		protected Object initialize() throws ConcurrentException {
			Map<String, Object> tools = new HashMap<>();

			// Add some date formatting stuff
			Map<String, Object> dateMap = new HashMap<>();
			dateMap.put("ISO_DATE", DateFormatUtils.ISO_DATE_FORMAT);
			dateMap.put("ISO_DATE_TZ", DateFormatUtils.ISO_DATE_TIME_ZONE_FORMAT);
			dateMap.put("ISO_DATETIME", DateFormatUtils.ISO_DATETIME_FORMAT);
			dateMap.put("ISO_DATETIME_TZ", DateFormatUtils.ISO_DATETIME_TIME_ZONE_FORMAT);
			dateMap.put("ISO_TIME", DateFormatUtils.ISO_TIME_FORMAT);
			dateMap.put("ISO_TIME_TZ", DateFormatUtils.ISO_TIME_TIME_ZONE_FORMAT);
			dateMap.put("ISO_TIME_NO_T", DateFormatUtils.ISO_TIME_NO_T_FORMAT);
			dateMap.put("ISO_TIME_NO_T_TZ", DateFormatUtils.ISO_TIME_NO_T_TIME_ZONE_FORMAT);

			tools.put("dateFormat", Tools.freezeMap(dateMap));

			return Tools.freezeMap(tools);
		}
	};

	private static final ConcurrentMap<String, ConcurrentMap<String, CompiledScript>> COMPILER_CACHE = new ConcurrentHashMap<>();

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

	public interface ScriptContextConfig {
		public void configure(ScriptContext ctx);
	}

	private static final ConcurrentMap<String, CompiledScript> getCompilerCache(String lang) {
		Objects.requireNonNull(lang, "Must provide a language to scan the cache for");
		return ConcurrentUtils.createIfAbsentUnchecked(Expression.COMPILER_CACHE, lang,
			new ConcurrentInitializer<ConcurrentMap<String, CompiledScript>>() {
				@Override
				public ConcurrentMap<String, CompiledScript> get() throws ConcurrentException {
					return new ConcurrentHashMap<>();
				}
			});
	}

	private static final ScriptEngine getEngine(String language) {
		if (language == null) { return null; }
		language = StringUtils.strip(language);
		ScriptEngine engine = null;
		if (language != null) {
			String actualLanguage = Expression.ENGINE_ALIASES.get(language);
			if (!StringUtils.isEmpty(actualLanguage)) {
				engine = Expression.ENGINE_FACTORY.getEngineByName(actualLanguage);
			}
			if (engine == null) { throw new IllegalArgumentException(
				String.format("Unknown script language [%s] - supported languages are (case-insensitive): %s", language,
					Expression.ENGINE_ALIASES.keySet())); }
		}
		return engine;
	}

	private static final CompiledScript compileScript(String language, final String source) throws ScriptException {
		final ScriptEngine engine = Expression.getEngine(language);
		if (!Compilable.class.isInstance(engine)) { return null; }

		final ConcurrentMap<String, CompiledScript> cache = Expression.getCompilerCache(language);
		final Compilable compiler = Compilable.class.cast(engine);
		// The key will be the source's SHA256 checksum
		final String key = DigestUtils.sha256Hex(source);
		try {
			return ConcurrentUtils.createIfAbsent(cache, key, new ConcurrentInitializer<CompiledScript>() {
				@Override
				public CompiledScript get() throws ConcurrentException {
					try {
						return compiler.compile(source);
					} catch (ScriptException e) {
						throw new ConcurrentException(e);
					}
				}
			});
		} catch (ConcurrentException e) {
			final Throwable cause = e.getCause();
			if (ScriptException.class.isInstance(cause)) { throw ScriptException.class.cast(cause); }
			throw new RuntimeDynamicElementException(
				String.format("Failed to pre-compile the %s script:%n%s%n", language, source), cause);
		}
	}

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
				throw new RuntimeDynamicElementException(
					String.format("Unknown script language [%s] - supported languages: %s", this.lang, m));
			}
		}
	}

	public Expression() {
		this(null, null);
	}

	public Expression(String script) {
		this(null, script);
	}

	public Expression(String lang, String script) {
		lang = StringUtils.strip(lang);
		this.engine = Expression.getEngine(lang);
		this.lang = lang;
		this.script = script;
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
		this.engine = Expression.getEngine(lang);
		this.lang = lang;
	}

	private ScriptEngine getEngine() {
		return this.engine = Expression.getEngine(getLang());
	}

	public Object evaluate() throws ScriptException {
		return evaluate(null);
	}

	public Object evaluate(ScriptContextConfig cfg) throws ScriptException {
		// If there is no engine needed, then we simply return the contents of the script as a
		// literal string script
		final ScriptEngine engine = getEngine();
		if (engine == null) { return getScript(); }

		// We have an engine, so strip out the script for (slightly) faster parsing
		final String script = StringUtils.strip(getScript());
		final String lang = getLang();
		final ScriptContext scriptCtx = new SimpleScriptContext();
		engine.setContext(scriptCtx);

		scriptCtx.setWriter(new PrintWriter(System.out));
		scriptCtx.setErrorWriter(new PrintWriter(System.err));

		if (cfg != null) {
			cfg.configure(scriptCtx);
		}
		Bindings b = scriptCtx.getBindings(ScriptContext.GLOBAL_SCOPE);
		if (b == null) {
			b = scriptCtx.getBindings(ScriptContext.ENGINE_SCOPE);
		}
		if (!b.containsKey("tools")) {
			try {
				b.put("tools", Expression.TOOLS.get());
			} catch (ConcurrentException e) {
				throw new ScriptException(e);
			}
		}

		this.log.trace("Compiling {} expression script:{}{}{}", lang, Expression.NL, script, Expression.NL);
		final CompiledScript compiled = Expression.compileScript(lang, script);
		if (compiled != null) {
			this.log.trace("The {} script was compiled - will use the precompiled version", lang);
			return compiled.eval(scriptCtx);
		}

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
	}

	@Override
	public String toString() {
		String lang = getLang();
		String script = getScript();
		if (lang == null) { return script; }
		return String.format("/* %s */%n%s", lang, script);
	}

	public static Expression constant(String value) {
		Expression e = new Expression();
		e.setScript(value);
		return e;
	}

	public static Object eval(Expression e, ScriptContextConfig cfg) throws ScriptException {
		if (e == null) { return null; }
		return e.evaluate(cfg);
	}

}