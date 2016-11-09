package com.armedia.caliente.cli.parser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.commons.lang3.StringUtils;

import com.armedia.commons.utilities.Tools;

public abstract class CommandLine implements Iterable<Parameter> {

	private static final ParameterDefinition HELP;

	static {
		MutableParameterDefinition help = new MutableParameterDefinition();
		help.setLongOpt("help");
		help.setShortOpt('?');
		help.setDescription("Show this help message");
		help.setRequired(false);
		help.setValueCount(0);
		HELP = help;
	}

	protected static final String[] NO_PARAM = {};

	private final ReadWriteLock rwLock = new ReentrantReadWriteLock();
	private final Map<Character, Parameter> shortOptions = new TreeMap<>();
	private final Map<String, Parameter> longOptions = new TreeMap<>();
	private final Set<Parameter> parameters = new TreeSet<>();

	private Parameter help = null;

	private final Map<Parameter, List<String>> values = new HashMap<>();
	private final List<String> remainingParameters = new ArrayList<>();

	private final CommandLineParserListener listener = new CommandLineParserListener() {

		@Override
		public void setParameter(Parameter p, Collection<String> values) {
			if ((values == null) || values.isEmpty()) {
				values = Collections.emptyList();
			}
			List<String> l = new ArrayList<>(values.size());
			for (String s : values) {
				l.add(s);
			}
			CommandLine.this.values.put(p, Tools.freezeList(l));
		}

		@Override
		public void setParameter(Parameter p) {
			setParameter(p, null);
		}

		@Override
		public void addRemainingParameters(Collection<String> remaining) {
			if (remaining == null) {
				remaining = Collections.emptyList();
			}
			CommandLine.this.remainingParameters.addAll(remaining);
		}
	};

	public final void parse(CommandLineParser parser, String executableName, String... args)
		throws CommandLineParseException, HelpRequestedException {
		final Lock l = this.rwLock.writeLock();
		l.lock();
		try {
			if (args == null) {
				args = CommandLine.NO_PARAM;
			}
			// Clear the current state
			this.values.clear();
			this.remainingParameters.clear();
			// Parse!
			try {
				try {
					parser.init(Tools.freezeSet(this.parameters));
				} catch (Exception e) {
					throw new CommandLineParseException(
						String.format("A initialization error ocurred while initializing the command-line parser",
							Arrays.toString(args)),
						e, null);
				}
				try {
					parser.parse(this.listener, args);
					if (this.help
						.isPresent()) { throw new HelpRequestedException(parser.getHelpMessage(executableName, null)); }
				} catch (Exception e) {
					throw new CommandLineParseException(String
						.format("A parsing error ocurred while parsing the command line %s", Arrays.toString(args)), e,
						parser.getHelpMessage(executableName, e));
				}
			} finally {
				parser.cleanup();
			}
		} finally {
			l.unlock();
		}
	}

	private boolean isShortOptionValid(Character shortOpt) {
		if (shortOpt == null) { return false; }
		final char c = shortOpt.charValue();
		if (!Character.isLetterOrDigit(c)) { return false; }
		return true;
	}

	private boolean isLongOptionValid(String longOpt) {
		if (longOpt == null) { return false; }
		if (StringUtils.containsWhitespace(longOpt)) { return false; }
		return (longOpt.length() > 1);
	}

	private void assertValid(Parameter param) {
		Objects.requireNonNull(param, "Must provide a parameter whose presence to check for");
		if (param.getCLI() != this) { throw new IllegalArgumentException(
			"The given parameter is not associated to this command-line interface"); }
	}

	private void assertValid(ParameterDefinition def) {
		Objects.requireNonNull(def, "Must provide a parameter definition");

		final Character shortOpt = def.getShortOpt();
		final boolean hasShortOpt = (shortOpt != null);

		final String longOpt = def.getLongOpt();
		final boolean hasLongOpt = (longOpt != null);

		if (!hasShortOpt && !hasLongOpt) { throw new IllegalArgumentException(
			"The given parameter definition has neither a short or a long option"); }
		if (hasShortOpt && !isShortOptionValid(shortOpt)) { throw new IllegalArgumentException(
			String.format("The short option value [%s] is not valid", shortOpt)); }
		if (hasLongOpt && !isLongOptionValid(longOpt)) { throw new IllegalArgumentException(
			String.format("The long option value [%s] is not valid", longOpt)); }
	}

	public final Parameter define(ParameterDefinition def) throws DuplicateParameterDefinitionException {
		assertValid(def);
		final Lock l = this.rwLock.writeLock();
		l.lock();

		if (this.help == null) {
			this.help = define(CommandLine.HELP);
		}

		try {
			final Character shortOpt = def.getShortOpt();
			Parameter shortParam = null;
			if (shortOpt != null) {
				shortParam = this.shortOptions.get(shortOpt);
				if (shortParam != null) {
					if (shortParam.getDefinition().equals(def)) {
						// It's the same parameter, so we can safely return the existing one
						return shortParam;
					}
					// The parameters aren't equal...so...this is an error
					throw new DuplicateParameterDefinitionException(String.format(
						"The new parameter definition for short option [%s] collides with an existing one", shortOpt),
						shortParam.getDefinition(), def);
				}
			}

			final String longOpt = def.getLongOpt();
			Parameter longParam = null;
			if (longOpt != null) {
				longParam = this.longOptions.get(longOpt);
				if (longParam != null) {
					if (longParam.getDefinition().equals(def)) {
						// It's the same parameter, so we can safely return the existing one
						return longParam;
					}
					// The parameters aren't equal...so...this is an error
					throw new DuplicateParameterDefinitionException(
						String.format("The new parameter definition for long option [%s] collides with an existing one",
							longOpt),
						longParam.getDefinition(), def);
				}
			}

			Parameter ret = new Parameter(this, def);
			if (shortOpt != null) {
				this.shortOptions.put(shortOpt, ret);
			}
			if (longOpt != null) {
				this.longOptions.put(longOpt, ret);
			}
			this.parameters.add(ret);
			return ret;
		} finally {
			l.unlock();
		}
	}

	@Override
	public final Iterator<Parameter> iterator() {
		final Lock l = this.rwLock.readLock();
		l.lock();
		try {
			return Tools.freezeSet(this.parameters).iterator();
		} finally {
			l.unlock();
		}
	}

	final Boolean getBoolean(Parameter param) {
		String s = getString(param);
		return (s != null ? Tools.toBoolean(s) : null);
	}

	final boolean getBoolean(Parameter param, boolean def) {
		Boolean v = getBoolean(param);
		return (v != null ? v.booleanValue() : def);
	}

	final List<Boolean> getAllBooleans(Parameter param) {
		List<String> l = getAllStrings(param);
		if ((l == null) || l.isEmpty()) { return Collections.emptyList(); }
		List<Boolean> r = new ArrayList<>(l.size());
		for (String s : l) {
			r.add(Tools.toBoolean(s));
		}
		return Tools.freezeList(r);
	}

	final Integer getInteger(Parameter param) {
		String s = getString(param);
		return (s != null ? Integer.valueOf(s) : null);
	}

	final int getInteger(Parameter param, int def) {
		Integer v = getInteger(param);
		return (v != null ? v.intValue() : def);
	}

	final List<Integer> getAllIntegers(Parameter param) {
		List<String> l = getAllStrings(param);
		if ((l == null) || l.isEmpty()) { return Collections.emptyList(); }
		List<Integer> r = new ArrayList<>(l.size());
		for (String s : l) {
			r.add(Integer.valueOf(s));
		}
		return Tools.freezeList(r);
	}

	final Long getLong(Parameter param) {
		String s = getString(param);
		return (s != null ? Long.valueOf(s) : null);
	}

	final long getLong(Parameter param, long def) {
		Long v = getLong(param);
		return (v != null ? v.longValue() : def);
	}

	final List<Long> getAllLongs(Parameter param) {
		List<String> l = getAllStrings(param);
		if ((l == null) || l.isEmpty()) { return Collections.emptyList(); }
		List<Long> r = new ArrayList<>(l.size());
		for (String s : l) {
			r.add(Long.valueOf(s));
		}
		return Tools.freezeList(r);
	}

	final Float getFloat(Parameter param) {
		String s = getString(param);
		return (s != null ? Float.valueOf(s) : null);
	}

	final float getFloat(Parameter param, float def) {
		Float v = getFloat(param);
		return (v != null ? v.floatValue() : def);
	}

	final List<Float> getAllFloats(Parameter param) {
		List<String> l = getAllStrings(param);
		if ((l == null) || l.isEmpty()) { return Collections.emptyList(); }
		List<Float> r = new ArrayList<>(l.size());
		for (String s : l) {
			r.add(Float.valueOf(s));
		}
		return Tools.freezeList(r);
	}

	final Double getDouble(Parameter param) {
		String s = getString(param);
		return (s != null ? Double.valueOf(s) : null);
	}

	final double getDouble(Parameter param, double def) {
		assertValid(param);
		Double v = getDouble(param);
		return (v != null ? v.doubleValue() : def);
	}

	final List<Double> getAllDoubles(Parameter param) {
		List<String> l = getAllStrings(param);
		if ((l == null) || l.isEmpty()) { return Collections.emptyList(); }
		List<Double> r = new ArrayList<>(l.size());
		for (String s : l) {
			r.add(Double.valueOf(s));
		}
		return Tools.freezeList(r);
	}

	final String getString(Parameter param) {
		List<String> l = getAllStrings(param);
		if ((l == null) || l.isEmpty()) { return null; }
		return l.get(0);
	}

	final String getString(Parameter param, String def) {
		assertValid(param);
		final String v = getString(param);
		return (v != null ? v : def);
	}

	final List<String> getAllStrings(Parameter param) {
		assertValid(param);
		final Lock l = this.rwLock.readLock();
		l.lock();
		try {
			return this.values.get(param);
		} finally {
			l.unlock();
		}
	}

	final boolean isPresent(Parameter param) {
		assertValid(param);
		final Lock l = this.rwLock.readLock();
		l.lock();
		try {
			return this.values.containsKey(param);
		} finally {
			l.unlock();
		}
	}

	public final List<String> getRemainingParameters() {
		final Lock l = this.rwLock.readLock();
		l.lock();
		try {
			return Tools.freezeCopy(this.remainingParameters);
		} finally {
			l.unlock();
		}
	}
}