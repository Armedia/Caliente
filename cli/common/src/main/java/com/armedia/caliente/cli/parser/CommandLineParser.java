package com.armedia.caliente.cli.parser;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class CommandLineParser {

	protected final Logger log = LoggerFactory.getLogger(getClass());

	protected static final class Context {
		private final CommandLine cl;

		private final Map<String, Object> state = new HashMap<>();

		private Context(CommandLine cl) {
			if (cl == null) { throw new IllegalArgumentException("Must provide a CommandLine instance"); }
			this.cl = cl;
		}

		public void setParameter(Parameter p) {
			setParameter(p, null);
		}

		public void setParameter(Parameter p, Collection<String> values) {
			if (p == null) { throw new IllegalArgumentException("Must provide a parameter to set"); }
			if (values == null) {
				values = Collections.emptyList();
			}
			this.cl.setParameterValues(p, values);
		}

		public void addRemainingParameters(Collection<String> remaining) {
			if (remaining == null) {
				remaining = Collections.emptyList();
			}
			this.cl.addRemainingParameters(remaining);
		}

		public boolean hasState(String name) {
			return this.state.containsKey(name);
		}

		public Set<String> getStateNames() {
			return this.state.keySet();
		}

		public Object setState(String name, Object value) {
			if (name == null) { throw new IllegalArgumentException("Must provide a non-null state name"); }
			return this.state.put(name, value);
		}

		public Object clearState(String name) {
			if (name == null) { throw new IllegalArgumentException("Must provide a non-null state name"); }
			return this.state.remove(name);
		}

		public <T> T getState(String name) {
			if (name == null) { throw new IllegalArgumentException("Must provide a non-null state name"); }
			Object o = this.state.get(name);
			if (o == null) { return null; }
			// Dangerous...unchecked cast
			@SuppressWarnings("unchecked")
			T t = (T) o;
			return t;
		}

		public <T> T getState(String name, Class<T> k) {
			if (name == null) { throw new IllegalArgumentException("Must provide a non-null state name"); }
			if (k == null) { throw new IllegalArgumentException("Must provide a class to cast into"); }
			Object o = this.state.get(name);
			if (o == null) { return null; }
			return (k.isInstance(o) ? k.cast(o) : null);
		}

		public void clearState() {
			this.state.clear();
		}
	}

	final Context initContext(CommandLine cl, String executableName, Set<Parameter> def) throws Exception {
		Context ctx = new Context(cl);
		init(ctx, executableName, def);
		return ctx;
	}

	protected abstract void init(Context ctx, String executableName, Set<Parameter> def) throws Exception;

	protected abstract void parse(final Context ctx, String... parameters) throws Exception;

	protected abstract String getHelpMessage(final Context ctx, Throwable thrown);

	protected void cleanup(Context ctx) {
	}
}