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
import java.util.TreeMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.caliente.cli.CommandLineException;
import com.armedia.commons.utilities.Tools;

public class CommandLine implements CommandLineValues {

	private static final Parameter HELP;

	static {
		MutableParameter help = new MutableParameter();
		help.setLongOpt("help");
		help.setShortOpt('?');
		help.setDescription("Show this help message");
		help.setRequired(false);
		HELP = help;
	}

	protected static final String[] NO_ARGS = {};

	protected final Logger log = LoggerFactory.getLogger(getClass());

	private final ReadWriteLock rwLock = new ReentrantReadWriteLock();
	private final Map<Character, CommandLineParameter> shortOptions = new TreeMap<>();
	private final Map<String, CommandLineParameter> longOptions = new TreeMap<>();
	private final Map<String, CommandLineParameter> commandLineParameters = new TreeMap<>();

	private final boolean helpSupported;

	private final Map<String, List<String>> values = new HashMap<>();
	private final List<String> positionalValues = new ArrayList<>();

	private String helpMessage = null;

	public CommandLine() {
		this(true);
	}

	public CommandLine(boolean defaultHelp) {
		if (defaultHelp) {
			try {
				define(CommandLine.HELP, true);
				this.helpSupported = true;
			} catch (CommandLineException e) {
				// Not gonna happen...but still barf if it does
				throw new RuntimeException("Unexpected Command Line exception", e);
			}
		} else {
			this.helpSupported = false;
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
		this.values.put(p.getKey(), Tools.freezeList(l));
	}

	final void addRemainingParameters(Collection<String> remaining) {
		if (remaining == null) {
			remaining = Collections.emptyList();
		}
		this.positionalValues.addAll(remaining);
	}

	public final void parse(String executableName, Collection<String> args) throws CommandLineParseException {
		parse(new CommonsCliParser(), executableName, args);
	}

	public final <C extends CommandLineParserContext> void parse(CommandLineParser<C> parser, String executableName,
		Collection<String> args) throws CommandLineParseException {
		if (args == null) {
			args = Collections.emptyList();
		}
		parse(parser, executableName, (args.isEmpty() ? CommandLine.NO_ARGS : args.toArray(CommandLine.NO_ARGS)));
	}

	public final void parse(String executableName, String... args) throws CommandLineParseException {
		parse(new CommonsCliParser(), executableName, args);
	}

	public final <C extends CommandLineParserContext> void parse(CommandLineParser<C> parser, String executableName,
		String... args) throws CommandLineParseException {
		if (parser == null) { throw new IllegalArgumentException("Must provide a parser implementation"); }
		if (executableName == null) {
			executableName = "Command Line";
		}
		final Lock l = this.rwLock.writeLock();
		l.lock();
		try {
			if (args == null) {
				args = CommandLine.NO_ARGS;
			}
			// Clear the current state
			this.values.clear();
			this.positionalValues.clear();
			// Parse!
			C ctx = null;
			try {
				try {
					ctx = parser.createContext(this, executableName,
						Collections.unmodifiableCollection(this.commandLineParameters.values()));
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
						parser.getHelpMessage(ctx, e));
				}
				if (this.helpSupported && isPresent(CommandLine.HELP)) {
					this.helpMessage = parser.getHelpMessage(ctx, null);
				} else {
					this.helpMessage = null;
				}
			} finally {
				if (ctx != null) {
					parser.cleanup(ctx);
				}
			}
		} finally {
			l.unlock();
		}
	}

	/* (non-Javadoc)
	 * @see com.armedia.caliente.cli.parser.CommandLineValues#isHelpRequested()
	 */
	@Override
	public boolean isHelpRequested() {
		return (this.helpSupported && isPresent(CommandLine.HELP));
	}

	/* (non-Javadoc)
	 * @see com.armedia.caliente.cli.parser.CommandLineValues#getHelpMessage()
	 */
	@Override
	public String getHelpMessage() {
		if (!isHelpRequested()) { return null; }
		return this.helpMessage;
	}

	protected boolean isShortOptionValid(Character shortOpt) {
		return (shortOpt != null);
	}

	protected boolean isLongOptionValid(String longOpt) {
		return (longOpt != null);
	}

	private <P extends Parameter> void assertValid(P param) {
		Objects.requireNonNull(param, "Must provide a parameter whose presence to check for");
		String key = param.getKey();
		if (key == null) { throw new IllegalArgumentException(
			"The given parameter definition does not define a valid key"); }
		if (CommandLineParameter.class.isInstance(param)) {
			CommandLineParameter p = CommandLineParameter.class.cast(param);
			if (p.getCommandLineValues() != this) { throw new IllegalArgumentException(
				"The given parameter is not associated to this command-line interface"); }
		}
	}

	private void assertValidDefinition(Parameter def) throws InvalidParameterException {
		if (def == null) { throw new InvalidParameterException("CommandLineParameter definition may not be null"); }

		final Character shortOpt = def.getShortOpt();
		final boolean hasShortOpt = (shortOpt != null);

		final String longOpt = def.getLongOpt();
		final boolean hasLongOpt = (longOpt != null);

		if (!hasShortOpt && !hasLongOpt) { throw new InvalidParameterException(
			"The given parameter definition has neither a short or a long option"); }
		if (hasShortOpt) {
			boolean valid = Character.isLetterOrDigit(shortOpt.charValue())
				|| CommandLine.HELP.getShortOpt().equals(shortOpt);
			if (valid) {
				// Custom validation
				valid &= isShortOptionValid(shortOpt);
			}
			if (!valid) { throw new InvalidParameterException(
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
			if (!valid) { throw new InvalidParameterException(
				String.format("The long option value [%s] is not valid", longOpt)); }
		}
	}

	public final CommandLineParameter define(Parameter def)
		throws DuplicateParameterException, InvalidParameterException {
		return define(def, false);
	}

	private final CommandLineParameter define(Parameter def, boolean unchecked)
		throws DuplicateParameterException, InvalidParameterException {
		if (!unchecked) {
			assertValidDefinition(def);
		}
		final Lock l = this.rwLock.writeLock();
		l.lock();

		try {
			final Character shortOpt = def.getShortOpt();
			CommandLineParameter shortParam = null;
			if (!unchecked && (shortOpt != null)) {
				shortParam = this.shortOptions.get(shortOpt);
				if (shortParam != null) {
					if (BaseParameter.isEquivalent(shortParam, def)) {
						// It's the same parameter, so we can safely return the existing one
						return shortParam;
					}
					// The commandLineParameters aren't equal...so...this is an error
					throw new DuplicateParameterException(String.format(
						"The new parameter definition for short option [%s] collides with an existing one", shortOpt),
						shortParam, def);
				}
			}

			final String longOpt = def.getLongOpt();
			CommandLineParameter longParam = null;
			if (!unchecked && (longOpt != null)) {
				longParam = this.longOptions.get(longOpt);
				if (longParam != null) {
					if (BaseParameter.isEquivalent(longParam, def)) {
						// It's the same parameter, so we can safely return the existing one
						return longParam;
					}
					// The commandLineParameters aren't equal...so...this is an error
					throw new DuplicateParameterException(
						String.format("The new parameter definition for long option [%s] collides with an existing one",
							longOpt),
						longParam, def);
				}
			}

			CommandLineParameter ret = new CommandLineParameter(this, def);
			if (shortOpt != null) {
				this.shortOptions.put(shortOpt, ret);
			}
			if (longOpt != null) {
				this.longOptions.put(longOpt, ret);
			}
			this.commandLineParameters.put(ret.getKey(), ret);
			return ret;
		} finally {
			l.unlock();
		}
	}

	@Override
	public final Iterator<CommandLineParameter> iterator() {
		final Lock l = this.rwLock.readLock();
		l.lock();
		try {
			return Tools.freezeList(new ArrayList<>(this.commandLineParameters.values())).iterator();
		} finally {
			l.unlock();
		}
	}

	@Override
	public final Iterable<CommandLineParameter> shortOptions() {
		final Lock l = this.rwLock.readLock();
		l.lock();
		try {
			return Tools.freezeList(new ArrayList<>(this.shortOptions.values()));
		} finally {
			l.unlock();
		}
	}

	@Override
	public final CommandLineParameter getParameter(char shortOpt) {
		final Lock l = this.rwLock.readLock();
		l.lock();
		try {
			return this.shortOptions.get(shortOpt);
		} finally {
			l.unlock();
		}
	}

	@Override
	public final boolean hasParameter(char shortOpt) {
		final Lock l = this.rwLock.readLock();
		l.lock();
		try {
			return this.shortOptions.containsKey(shortOpt);
		} finally {
			l.unlock();
		}
	}

	@Override
	public final Iterable<CommandLineParameter> longOptions() {
		final Lock l = this.rwLock.readLock();
		l.lock();
		try {
			return Tools.freezeList(new ArrayList<>(this.longOptions.values()));
		} finally {
			l.unlock();
		}
	}

	@Override
	public final CommandLineParameter getParameter(String longOpt) {
		final Lock l = this.rwLock.readLock();
		l.lock();
		try {
			return this.longOptions.get(longOpt);
		} finally {
			l.unlock();
		}
	}

	@Override
	public final boolean hasParameter(String longOpt) {
		final Lock l = this.rwLock.readLock();
		l.lock();
		try {
			return this.longOptions.containsKey(longOpt);
		} finally {
			l.unlock();
		}
	}

	@Override
	public final boolean isDefined(Parameter parameter) {
		return (getParameter(parameter) != null);
	}

	@Override
	public final CommandLineParameter getParameter(Parameter parameter) {
		if (parameter == null) { throw new IllegalArgumentException(
			"Must provide a parameter definition to retrieve"); }
		return getParameterByKey(parameter.getKey());
	}

	protected final CommandLineParameter getParameterByKey(String key) {
		if (key == null) { throw new IllegalArgumentException("Must provide a key to search for"); }
		final Lock l = this.rwLock.readLock();
		l.lock();
		try {
			return this.commandLineParameters.get(key);
		} finally {
			l.unlock();
		}
	}

	@Override
	public final boolean hasHelpParameter() {
		return this.helpSupported;
	}

	@Override
	public final CommandLineParameter getHelpParameter() {
		return getParameter(CommandLine.HELP);
	}

	@Override
	public final Boolean getBoolean(Parameter param) {
		String s = getString(param);
		return (s != null ? Tools.toBoolean(s) : null);
	}

	@Override
	public final boolean getBoolean(Parameter param, boolean def) {
		Boolean v = getBoolean(param);
		return (v != null ? v.booleanValue() : def);
	}

	@Override
	public final List<Boolean> getAllBooleans(Parameter param) {
		List<String> l = getAllStrings(param);
		if (l == null) { return null; }
		if (l.isEmpty()) { return Collections.emptyList(); }
		List<Boolean> r = new ArrayList<>(l.size());
		for (String s : l) {
			r.add(Tools.toBoolean(s));
		}
		return Tools.freezeList(r);
	}

	@Override
	public final Integer getInteger(Parameter param) {
		String s = getString(param);
		return (s != null ? Integer.valueOf(s) : null);
	}

	@Override
	public final int getInteger(Parameter param, int def) {
		Integer v = getInteger(param);
		return (v != null ? v.intValue() : def);
	}

	@Override
	public final List<Integer> getAllIntegers(Parameter param) {
		List<String> l = getAllStrings(param);
		if (l == null) { return null; }
		if (l.isEmpty()) { return Collections.emptyList(); }
		List<Integer> r = new ArrayList<>(l.size());
		for (String s : l) {
			r.add(Integer.valueOf(s));
		}
		return Tools.freezeList(r);
	}

	@Override
	public final Long getLong(Parameter param) {
		String s = getString(param);
		return (s != null ? Long.valueOf(s) : null);
	}

	@Override
	public final long getLong(Parameter param, long def) {
		Long v = getLong(param);
		return (v != null ? v.longValue() : def);
	}

	@Override
	public final List<Long> getAllLongs(Parameter param) {
		List<String> l = getAllStrings(param);
		if (l == null) { return null; }
		if (l.isEmpty()) { return Collections.emptyList(); }
		List<Long> r = new ArrayList<>(l.size());
		for (String s : l) {
			r.add(Long.valueOf(s));
		}
		return Tools.freezeList(r);
	}

	@Override
	public final Float getFloat(Parameter param) {
		String s = getString(param);
		return (s != null ? Float.valueOf(s) : null);
	}

	@Override
	public final float getFloat(Parameter param, float def) {
		Float v = getFloat(param);
		return (v != null ? v.floatValue() : def);
	}

	@Override
	public final List<Float> getAllFloats(Parameter param) {
		List<String> l = getAllStrings(param);
		if (l == null) { return null; }
		if (l.isEmpty()) { return Collections.emptyList(); }
		List<Float> r = new ArrayList<>(l.size());
		for (String s : l) {
			r.add(Float.valueOf(s));
		}
		return Tools.freezeList(r);
	}

	@Override
	public final Double getDouble(Parameter param) {
		String s = getString(param);
		return (s != null ? Double.valueOf(s) : null);
	}

	@Override
	public final double getDouble(Parameter param, double def) {
		assertValid(param);
		Double v = getDouble(param);
		return (v != null ? v.doubleValue() : def);
	}

	@Override
	public final List<Double> getAllDoubles(Parameter param) {
		List<String> l = getAllStrings(param);
		if (l == null) { return null; }
		if (l.isEmpty()) { return Collections.emptyList(); }
		List<Double> r = new ArrayList<>(l.size());
		for (String s : l) {
			r.add(Double.valueOf(s));
		}
		return Tools.freezeList(r);
	}

	@Override
	public final String getString(Parameter param) {
		List<String> l = getAllStrings(param);
		if ((l == null) || l.isEmpty()) { return null; }
		return l.get(0);
	}

	@Override
	public final String getString(Parameter param, String def) {
		assertValid(param);
		final String v = getString(param);
		return (v != null ? v : def);
	}

	@Override
	public final List<String> getAllStrings(Parameter param) {
		assertValid(param);
		final Lock l = this.rwLock.readLock();
		l.lock();
		try {
			return this.values.get(param.getKey());
		} finally {
			l.unlock();
		}
	}

	@Override
	public final List<String> getAllStrings(Parameter param, List<String> def) {
		assertValid(param);
		List<String> ret = getAllStrings(param);
		if (ret == null) { return def; }
		return ret;
	}

	@Override
	public final boolean isPresent(Parameter param) {
		assertValid(param);
		final Lock l = this.rwLock.readLock();
		l.lock();
		try {
			return this.values.containsKey(param.getKey());
		} finally {
			l.unlock();
		}
	}

	@Override
	public final List<String> getPositionalValues() {
		final Lock l = this.rwLock.readLock();
		l.lock();
		try {
			return Tools.freezeCopy(this.positionalValues);
		} finally {
			l.unlock();
		}
	}
}