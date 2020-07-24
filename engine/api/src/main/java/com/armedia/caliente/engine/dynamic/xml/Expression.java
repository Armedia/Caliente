/*******************************************************************************
 * #%L
 * Armedia Caliente
 * %%
 * Copyright (C) 2013 - 2019 Armedia, LLC
 * %%
 * This file is part of the Caliente software.
 *
 * If the software was purchased under a paid Caliente license, the terms of
 * the paid license agreement will prevail.  Otherwise, the software is
 * provided under the following open source license terms:
 *
 * Caliente is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Caliente is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Caliente. If not, see <http://www.gnu.org/licenses/>.
 * #L%
 *******************************************************************************/

package com.armedia.caliente.engine.dynamic.xml;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import javax.script.Bindings;
import javax.script.ScriptContext;
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

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.concurrent.ConcurrentException;
import org.apache.commons.lang3.concurrent.LazyInitializer;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.commons.utilities.Tools;
import com.armedia.commons.utilities.script.JSR223Script;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "expression.t", propOrder = {
	"script"
})
public class Expression {
	private static final LazyInitializer<Object> TOOLS = new LazyInitializer<Object>() {
		@Override
		protected Object initialize() throws ConcurrentException {
			Map<String, Object> tools = new HashMap<>();

			// Add some date formatting stuff
			Map<String, Object> dateMap = new HashMap<>();
			dateMap.put("ISO_DATE", DateFormatUtils.ISO_8601_EXTENDED_DATE_FORMAT);
			dateMap.put("ISO_DATETIME", DateFormatUtils.ISO_8601_EXTENDED_DATETIME_FORMAT);
			dateMap.put("ISO_DATETIME_TZ", DateFormatUtils.ISO_8601_EXTENDED_DATETIME_TIME_ZONE_FORMAT);
			dateMap.put("ISO_TIME", DateFormatUtils.ISO_8601_EXTENDED_TIME_FORMAT);
			dateMap.put("ISO_TIME_TZ", DateFormatUtils.ISO_8601_EXTENDED_TIME_TIME_ZONE_FORMAT);

			tools.put("dateFormat", Tools.freezeMap(dateMap));

			return Tools.freezeMap(tools);
		}
	};

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
	private final JSR223Script.Builder scriptBuilder = new JSR223Script.Builder();

	protected void beforeMarshal(Marshaller m) {
		this.lang = StringUtils.strip(this.lang);
	}

	protected void afterUnmarshal(Unmarshaller u, Object parent) {
		this.lang = StringUtils.strip(this.lang);
	}

	public Expression() {
		this(null, null);
	}

	public Expression(String script) {
		this(null, script);
	}

	public Expression(String lang, String script) {
		lang = StringUtils.strip(lang);
		this.lang = lang;
		this.script = script;
	}

	private JSR223Script getExecutable() throws ScriptException, IOException {
		if (StringUtils.isBlank(this.lang)) { return null; }
		return this.scriptBuilder //
			.language(this.lang) //
			.allowCompilation(true) //
			.source(StringUtils.strip(this.script)) //
			.build() //
		;
	}

	public String getScript() {
		return this.script;
	}

	public void setScript(String script) {
		this.script = script;
	}

	public String getLang() {
		return this.lang;
	}

	public void setLang(String lang) {
		this.lang = StringUtils.strip(lang);
	}

	public Object evaluate() throws ScriptException {
		return evaluate(null);
	}

	public Object evaluate(Consumer<ScriptContext> cfg) throws ScriptException {
		try {
			// If there is no engine needed, then we simply return the contents of the script as a
			// literal string script
			final JSR223Script executable = getExecutable();
			if (executable == null) { return getScript(); }

			// We have an engine, so strip out the script for (slightly) faster parsing
			final ScriptContext scriptCtx = new SimpleScriptContext();

			scriptCtx.setWriter(new PrintWriter(System.out));
			scriptCtx.setErrorWriter(new PrintWriter(System.err));

			if (cfg != null) {
				cfg.accept(scriptCtx);
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
			if (!b.containsKey("log")) {
				b.put("log", this.log);
			}

			this.log.trace("Evaluating {} expression script:{}{}{}", this.lang, Tools.NL, this.script, Tools.NL);
			Object ret = executable.eval(scriptCtx);
			if (ret != null) {
				this.log.trace("Returned [{}] from {} expression script:{}{}{}", ret, this.lang, Tools.NL, this.script,
					Tools.NL);
			} else {
				this.log.trace("Returned <null> from {} expression script:{}{}{}", this.lang, Tools.NL, this.script,
					Tools.NL);
			}
			return ret;
		} catch (IOException e) {
			throw new RuntimeException("Unexpected IOException while operating from memory", e);
		}
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

	public static Object eval(Expression e, Consumer<ScriptContext> cfg) throws ScriptException {
		if (e == null) { return null; }
		return e.evaluate(cfg);
	}

}