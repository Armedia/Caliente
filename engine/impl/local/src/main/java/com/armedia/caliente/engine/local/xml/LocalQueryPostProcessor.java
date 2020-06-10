package com.armedia.caliente.engine.local.xml;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.Method;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.script.ScriptException;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlValue;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.commons.utilities.Tools;
import com.armedia.commons.utilities.concurrent.BaseShareableLockable;
import com.armedia.commons.utilities.concurrent.MutexAutoLock;
import com.armedia.commons.utilities.concurrent.SharedAutoLock;
import com.armedia.commons.utilities.function.CheckedFunction;
import com.armedia.commons.utilities.script.JSR223Script;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "localQueryPostProcessor.t", propOrder = {
	"value"
})
public class LocalQueryPostProcessor extends BaseShareableLockable {

	private static final String DEFAULT_LANGUAGE = "jexl3";

	private static final Class<?>[] METHOD_ARGS = {
		String.class
	};
	private static final Pattern CLASS_PARSER = Pattern
		.compile("^((?:[\\w$&&[^\\d]][\\w$]*)(?:\\.[\\w$&&[^\\d]][\\w$]*)*)(?:::([\\w$&&[^\\d]][\\w$]*))?$");

	@FunctionalInterface
	public static interface Processor {
		public String process(String value) throws Exception;
	}

	private static class ScriptProcessor implements Processor {
		private final JSR223Script script;

		private ScriptProcessor(String language, String script) throws ScriptException {
			if (StringUtils.isBlank(script)) {
				throw new IllegalArgumentException("The post-processor script may not be blank");
			}
			language = (StringUtils.isBlank(language) ? LocalQueryPostProcessor.DEFAULT_LANGUAGE : language);
			try {
				this.script = new JSR223Script.Builder() //
					.allowCompilation(true) //
					.precompile(true) //
					.language(language) //
					.source(script) //
					.build();
			} catch (IOException e) {
				throw new UncheckedIOException("Unexpected IOException when working in memory", e);
			}
		}

		@Override
		public String process(String value) throws ScriptException {
			try {
				return Tools.toString(this.script.eval((b) -> b.put("path", value)));
			} catch (IOException e) {
				throw new UncheckedIOException("Unexpected IOException when working in memory", e);
			}
		}
	}

	@XmlTransient
	private final Logger log = LoggerFactory.getLogger(getClass());

	@XmlValue
	protected String value;

	@XmlAttribute(name = "type", required = true)
	protected String type;

	@XmlTransient
	private CheckedFunction<String, String, ? extends Exception> postProcessor = null;

	@XmlTransient
	private volatile boolean initialized = false;

	public String getValue() {
		return this.value;
	}

	public void setValue(String value) {
		Objects.requireNonNull(value, "Must provide a non-null value");
		try (MutexAutoLock lock = autoMutexLock()) {
			this.value = value;
			reset();
		}
	}

	public String getType() {
		return shareLocked(() -> this.type);
	}

	public void setType(String type) {
		Objects.requireNonNull(type, "Must provide a non-null type");
		try (MutexAutoLock lock = autoMutexLock()) {
			this.type = type;
			reset();
		}
	}

	private void reset() {
		this.postProcessor = null;
		this.initialized = false;
	}

	private CheckedFunction<String, String, ? extends Exception> initProcessor() throws Exception {
		if (!StringUtils.equalsIgnoreCase("CLASS", this.type)) {
			// If the type is not "class", then it's definitely a script
			return new ScriptProcessor(this.type, this.value)::process;
		}

		Processor processor = null;
		// This is a classname
		Matcher m = LocalQueryPostProcessor.CLASS_PARSER.matcher(this.value);
		if (!m.matches()) {
			throw new IllegalArgumentException("The class specification [" + this.value
				+ "] is not valid, must match this pattern: fully.qualified.className(::methodName)?");
		}

		Class<?> klass = Class.forName(m.group(1));
		String methodName = m.group(2);
		if (StringUtils.isBlank(methodName)) {
			if (!Processor.class.isAssignableFrom(klass)) {
				throw new ClassCastException("The class [" + klass.getCanonicalName() + "] does not implement "
					+ Processor.class.getCanonicalName() + " and no method name was given, can't proceed");
			}
			processor = Processor.class.cast(klass.getConstructor().newInstance());
		} else {
			final Method method = klass.getMethod(m.group(2), LocalQueryPostProcessor.METHOD_ARGS);
			final Object o = klass.getConstructor().newInstance();
			processor = (str) -> Tools.toString(method.invoke(o, str));
		}
		return processor::process;
	}

	private CheckedFunction<String, String, ? extends Exception> getPostProcessor() throws Exception {
		try (SharedAutoLock shared = autoSharedLock()) {
			boolean initialized = this.initialized;
			if (!initialized) {
				try (MutexAutoLock mutex = shared.upgrade()) {
					initialized = this.initialized;
					if (!initialized) {
						try {
							this.postProcessor = initProcessor();
						} catch (Exception e) {
							this.log.error(
								"Exception caught when initializing the {} LocalQueryPostProcessor with [{}]",
								this.type, this.value, e);
							// Rethrow the exception each time...
							this.postProcessor = (str) -> {
								throw e;
							};
							throw e;
						} finally {
							this.initialized = true;
						}
					}
				}
			}
			return this.postProcessor;
		}
	}

	public String postProcess(String path) throws Exception {
		return getPostProcessor().applyChecked(path);
	}

	@Override
	public String toString() {
		String type = Tools.coalesce(this.type, LocalQueryPostProcessor.DEFAULT_LANGUAGE);
		String value = (StringUtils.equalsIgnoreCase("CLASS", type) ? this.value : "<script>");
		return String.format("LocalQueryPostProcessor [type=%s, value=%s]", type, value);
	}
}