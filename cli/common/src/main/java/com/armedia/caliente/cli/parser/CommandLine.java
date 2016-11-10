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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.caliente.cli.CommandLineException;
import com.armedia.commons.utilities.Tools;

public class CommandLine implements Iterable<Parameter> {

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

	protected final Logger log = LoggerFactory.getLogger(getClass());

	private final ReadWriteLock rwLock = new ReentrantReadWriteLock();
	private final Map<Character, Parameter> shortOptions = new TreeMap<>();
	private final Map<String, Parameter> longOptions = new TreeMap<>();
	private final Set<Parameter> parameters = new TreeSet<>();

	private final Parameter help;

	private final Map<Parameter, List<String>> values = new HashMap<>();
	private final List<String> remainingParameters = new ArrayList<>();

	public CommandLine() {
		this(true);
	}

	public CommandLine(boolean defaultHelp) {
		if (defaultHelp) {
			try {
				this.help = define(CommandLine.HELP, true);
			} catch (CommandLineException e) {
				// Not gonna happen...but still barf if it does
				throw new RuntimeException("Unexpected Command Line exception", e);
			}
		} else {
			this.help = null;
		}
	}

	final void setParameterValues(Parameter p, Collection<String> values) {
		if ((values == null) || values.isEmpty()) {
			values = Collections.emptyList();
		}
		List<String> l = new ArrayList<>(values.size());
		for (String s : values) {
			l.add(s);
		}
		this.values.put(p, Tools.freezeList(l));
	}

	final void addRemainingParameters(Collection<String> remaining) {
		if (remaining == null) {
			remaining = Collections.emptyList();
		}
		this.remainingParameters.addAll(remaining);
	}

	public final void parse(String executableName, String... args)
		throws CommandLineParseException, HelpRequestedException {
		parse(new CommonsCliParser(), executableName, args);
	}

	public final void parse(CommandLineParser parser, String executableName, String... args)
		throws CommandLineParseException, HelpRequestedException {
		if (parser == null) { throw new IllegalArgumentException("Must provide a parser implementation"); }
		if (executableName == null) {
			executableName = "Command Line";
		}
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
			CommandLineParser.Context ctx = null;
			try {
				try {
					ctx = parser.initContext(this, Tools.freezeSet(this.parameters));
				} catch (Exception e) {
					throw new CommandLineParseException(
						String.format("A initialization error ocurred while initializing the command-line parser",
							Arrays.toString(args)),
						e, null);
				}
				try {
					parser.parse(ctx, args);
				} catch (Exception e) {
					throw new CommandLineParseException(String
						.format("A parsing error ocurred while parsing the command line %s", Arrays.toString(args)), e,
						parser.getHelpMessage(ctx, executableName, e));
				}
				if ((this.help != null) && this.help.isPresent()) { throw new HelpRequestedException(
					parser.getHelpMessage(ctx, executableName, null)); }
			} finally {
				if (ctx != null) {
					try {
						parser.cleanup(ctx);
					} finally {
						ctx.clearState();
					}
				}
			}
		} finally {
			l.unlock();
		}
	}

	protected boolean isShortOptionValid(Character shortOpt) {
		return (shortOpt != null);
	}

	protected boolean isLongOptionValid(String longOpt) {
		return (longOpt != null);
	}

	private void assertValid(Parameter param) {
		Objects.requireNonNull(param, "Must provide a parameter whose presence to check for");
		if (param.getCLI() != this) { throw new IllegalArgumentException(
			"The given parameter is not associated to this command-line interface"); }
	}

	private void assertValid(ParameterDefinition def) throws InvalidParameterDefinitionException {
		if (def == null) { throw new InvalidParameterDefinitionException("Parameter definition may not be null"); }

		final Character shortOpt = def.getShortOpt();
		final boolean hasShortOpt = (shortOpt != null);

		final String longOpt = def.getLongOpt();
		final boolean hasLongOpt = (longOpt != null);

		if (!hasShortOpt && !hasLongOpt) { throw new InvalidParameterDefinitionException(
			"The given parameter definition has neither a short or a long option"); }
		if (hasShortOpt) {
			boolean valid = Character.isLetterOrDigit(shortOpt.charValue())
				|| CommandLine.HELP.getShortOpt().equals(shortOpt);
			if (valid) {
				// Custom validation
				valid &= isShortOptionValid(shortOpt);
			}
			if (!valid) { throw new InvalidParameterDefinitionException(
				String.format("The short option value [%s] is not valid", shortOpt)); }
		}
		if (hasLongOpt) {
			boolean valid = !StringUtils.containsWhitespace(longOpt);
			if (valid) {
				valid &= (longOpt.length() > 1);
			}
			if (valid) {
				valid &= isLongOptionValid(longOpt);
			}
			if (!valid) { throw new InvalidParameterDefinitionException(
				String.format("The long option value [%s] is not valid", longOpt)); }
		}
	}

	private final Parameter define(ParameterDefinition def, boolean unchecked)
		throws DuplicateParameterDefinitionException, InvalidParameterDefinitionException {
		if (!unchecked) {
			assertValid(def);
		}
		final Lock l = this.rwLock.writeLock();
		l.lock();

		try {
			final Character shortOpt = def.getShortOpt();
			Parameter shortParam = null;
			if (!unchecked && (shortOpt != null)) {
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
			if (!unchecked && (longOpt != null)) {
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

	public final Parameter define(ParameterDefinition def)
		throws DuplicateParameterDefinitionException, InvalidParameterDefinitionException {
		return define(def, false);
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

	public final Iterable<Parameter> shortOptions() {
		final Lock l = this.rwLock.readLock();
		l.lock();
		try {
			return Tools.freezeList(new ArrayList<>(this.shortOptions.values()));
		} finally {
			l.unlock();
		}
	}

	public final Parameter getParameter(char shortOpt) {
		final Lock l = this.rwLock.readLock();
		l.lock();
		try {
			return this.shortOptions.get(shortOpt);
		} finally {
			l.unlock();
		}
	}

	public final boolean hasParameter(char shortOpt) {
		final Lock l = this.rwLock.readLock();
		l.lock();
		try {
			return this.shortOptions.containsKey(shortOpt);
		} finally {
			l.unlock();
		}
	}

	public final Iterable<Parameter> longOptions() {
		final Lock l = this.rwLock.readLock();
		l.lock();
		try {
			return Tools.freezeList(new ArrayList<>(this.longOptions.values()));
		} finally {
			l.unlock();
		}
	}

	public final Parameter getParameter(String longOpt) {
		final Lock l = this.rwLock.readLock();
		l.lock();
		try {
			return this.longOptions.get(longOpt);
		} finally {
			l.unlock();
		}
	}

	public final boolean hasParameter(String longOpt) {
		final Lock l = this.rwLock.readLock();
		l.lock();
		try {
			return this.longOptions.containsKey(longOpt);
		} finally {
			l.unlock();
		}
	}

	public final boolean hasHelpParameter() {
		return (this.help != null);
	}

	public final Parameter getHelpParameter() {
		return this.help;
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
		if (l == null) { return null; }
		if (l.isEmpty()) { return Collections.emptyList(); }
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
		if (l == null) { return null; }
		if (l.isEmpty()) { return Collections.emptyList(); }
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
		if (l == null) { return null; }
		if (l.isEmpty()) { return Collections.emptyList(); }
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
		if (l == null) { return null; }
		if (l.isEmpty()) { return Collections.emptyList(); }
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
		if (l == null) { return null; }
		if (l.isEmpty()) { return Collections.emptyList(); }
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