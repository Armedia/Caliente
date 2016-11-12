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

public class CommandLine implements CommandLineValues, Iterable<CommandLineParameter> {

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

	protected static final String[] NO_ARGS = {};

	protected final Logger log = LoggerFactory.getLogger(getClass());

	private final ReadWriteLock rwLock = new ReentrantReadWriteLock();
	private final Map<Character, CommandLineParameter> shortOptions = new TreeMap<>();
	private final Map<String, CommandLineParameter> longOptions = new TreeMap<>();
	private final Map<String, CommandLineParameter> commandLineParameters = new TreeMap<>();

	private final CommandLineParameter help;

	private final Map<String, List<String>> values = new HashMap<>();
	private final List<String> remainingParameters = new ArrayList<>();

	private String helpMessage = null;

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

	final void setParameterValues(CommandLineParameter p, Collection<String> values) {
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
		this.remainingParameters.addAll(remaining);
	}

	public final void parse(String executableName, Collection<String> args) throws CommandLineParseException {
		parse(new CommonsCliParser(), executableName, args);
	}

	public final void parse(CommandLineParser parser, String executableName, Collection<String> args)
		throws CommandLineParseException {
		if (args == null) {
			args = Collections.emptyList();
		}
		parse(parser, executableName, (args.isEmpty() ? CommandLine.NO_ARGS : args.toArray(CommandLine.NO_ARGS)));
	}

	public final void parse(String executableName, String... args) throws CommandLineParseException {
		parse(new CommonsCliParser(), executableName, args);
	}

	public final void parse(CommandLineParser parser, String executableName, String... args)
		throws CommandLineParseException {
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
			this.remainingParameters.clear();
			// Parse!
			CommandLineParser.Context ctx = null;
			try {
				try {
					ctx = parser.initContext(this, executableName,
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
				if ((this.help != null) && this.help.isPresent()) {
					this.helpMessage = parser.getHelpMessage(ctx, null);
				} else {
					this.helpMessage = null;
				}
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

	/* (non-Javadoc)
	 * @see com.armedia.caliente.cli.parser.CommandLineValues#isHelpRequested()
	 */
	@Override
	public boolean isHelpRequested() {
		return ((this.help != null) && this.help.isPresent());
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

	private <P extends ParameterDefinition> void assertValid(P param) {
		Objects.requireNonNull(param, "Must provide a parameter whose presence to check for");
		String key = param.getKey();
		if (key == null) { throw new IllegalArgumentException(
			"The given parameter definition does not define a valid key"); }
		if (CommandLineParameter.class.isInstance(param)) {
			CommandLineParameter p = CommandLineParameter.class.cast(param);
			if (p.getCLI() != this) { throw new IllegalArgumentException(
				"The given parameter is not associated to this command-line interface"); }
		}
	}

	private void assertValidDefinition(ParameterDefinition def) throws InvalidParameterDefinitionException {
		if (def == null) { throw new InvalidParameterDefinitionException(
			"CommandLineParameter definition may not be null"); }

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

	private final CommandLineParameter define(ParameterDefinition def, boolean unchecked)
		throws DuplicateParameterDefinitionException, InvalidParameterDefinitionException {
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
					if (shortParam.getDefinition().equals(def)) {
						// It's the same parameter, so we can safely return the existing one
						return shortParam;
					}
					// The commandLineParameters aren't equal...so...this is an error
					throw new DuplicateParameterDefinitionException(String.format(
						"The new parameter definition for short option [%s] collides with an existing one", shortOpt),
						shortParam.getDefinition(), def);
				}
			}

			final String longOpt = def.getLongOpt();
			CommandLineParameter longParam = null;
			if (!unchecked && (longOpt != null)) {
				longParam = this.longOptions.get(longOpt);
				if (longParam != null) {
					if (longParam.getDefinition().equals(def)) {
						// It's the same parameter, so we can safely return the existing one
						return longParam;
					}
					// The commandLineParameters aren't equal...so...this is an error
					throw new DuplicateParameterDefinitionException(
						String.format("The new parameter definition for long option [%s] collides with an existing one",
							longOpt),
						longParam.getDefinition(), def);
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

	public final CommandLineParameter define(ParameterDefinition def)
		throws DuplicateParameterDefinitionException, InvalidParameterDefinitionException {
		return define(def, false);
	}

	/* (non-Javadoc)
	 * @see com.armedia.caliente.cli.parser.CommandLineValues#iterator()
	 */
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

	/* (non-Javadoc)
	 * @see com.armedia.caliente.cli.parser.CommandLineValues#shortOptions()
	 */
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

	/* (non-Javadoc)
	 * @see com.armedia.caliente.cli.parser.CommandLineValues#getParameter(char)
	 */
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

	/* (non-Javadoc)
	 * @see com.armedia.caliente.cli.parser.CommandLineValues#hasParameter(char)
	 */
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

	/* (non-Javadoc)
	 * @see com.armedia.caliente.cli.parser.CommandLineValues#longOptions()
	 */
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

	/* (non-Javadoc)
	 * @see com.armedia.caliente.cli.parser.CommandLineValues#getParameter(java.lang.String)
	 */
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

	/* (non-Javadoc)
	 * @see com.armedia.caliente.cli.parser.CommandLineValues#hasParameter(java.lang.String)
	 */
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

	/* (non-Javadoc)
	 * @see com.armedia.caliente.cli.parser.CommandLineValues#isParameterDefined(com.armedia.caliente.cli.parser.ParameterDefinition)
	 */
	@Override
	public final boolean isParameterDefined(ParameterDefinition parameter) {
		return (getParameterFromDefinition(parameter) != null);
	}

	/* (non-Javadoc)
	 * @see com.armedia.caliente.cli.parser.CommandLineValues#getParameterFromDefinition(com.armedia.caliente.cli.parser.ParameterDefinition)
	 */
	@Override
	public final CommandLineParameter getParameterFromDefinition(ParameterDefinition parameter) {
		if (parameter == null) { throw new IllegalArgumentException(
			"Must provide a parameter definition to retrieve"); }
		final String key = parameter.getKey();
		if (key == null) { throw new IllegalArgumentException(
			"The parameter definition given doesn't generate a valid key"); }
		final Lock l = this.rwLock.readLock();
		l.lock();
		try {
			return this.commandLineParameters.get(key);
		} finally {
			l.unlock();
		}
	}

	/* (non-Javadoc)
	 * @see com.armedia.caliente.cli.parser.CommandLineValues#hasHelpParameter()
	 */
	@Override
	public final boolean hasHelpParameter() {
		return (this.help != null);
	}

	/* (non-Javadoc)
	 * @see com.armedia.caliente.cli.parser.CommandLineValues#getHelpParameter()
	 */
	@Override
	public final CommandLineParameter getHelpParameter() {
		return this.help;
	}

	/* (non-Javadoc)
	 * @see com.armedia.caliente.cli.parser.CommandLineValues#getBoolean(com.armedia.caliente.cli.parser.ParameterDefinition)
	 */
	@Override
	public final Boolean getBoolean(ParameterDefinition param) {
		String s = getString(param);
		return (s != null ? Tools.toBoolean(s) : null);
	}

	/* (non-Javadoc)
	 * @see com.armedia.caliente.cli.parser.CommandLineValues#getBoolean(com.armedia.caliente.cli.parser.ParameterDefinition, boolean)
	 */
	@Override
	public final boolean getBoolean(ParameterDefinition param, boolean def) {
		Boolean v = getBoolean(param);
		return (v != null ? v.booleanValue() : def);
	}

	/* (non-Javadoc)
	 * @see com.armedia.caliente.cli.parser.CommandLineValues#getAllBooleans(com.armedia.caliente.cli.parser.ParameterDefinition)
	 */
	@Override
	public final List<Boolean> getAllBooleans(ParameterDefinition param) {
		List<String> l = getAllStrings(param);
		if (l == null) { return null; }
		if (l.isEmpty()) { return Collections.emptyList(); }
		List<Boolean> r = new ArrayList<>(l.size());
		for (String s : l) {
			r.add(Tools.toBoolean(s));
		}
		return Tools.freezeList(r);
	}

	/* (non-Javadoc)
	 * @see com.armedia.caliente.cli.parser.CommandLineValues#getInteger(com.armedia.caliente.cli.parser.ParameterDefinition)
	 */
	@Override
	public final Integer getInteger(ParameterDefinition param) {
		String s = getString(param);
		return (s != null ? Integer.valueOf(s) : null);
	}

	/* (non-Javadoc)
	 * @see com.armedia.caliente.cli.parser.CommandLineValues#getInteger(com.armedia.caliente.cli.parser.ParameterDefinition, int)
	 */
	@Override
	public final int getInteger(ParameterDefinition param, int def) {
		Integer v = getInteger(param);
		return (v != null ? v.intValue() : def);
	}

	/* (non-Javadoc)
	 * @see com.armedia.caliente.cli.parser.CommandLineValues#getAllIntegers(com.armedia.caliente.cli.parser.ParameterDefinition)
	 */
	@Override
	public final List<Integer> getAllIntegers(ParameterDefinition param) {
		List<String> l = getAllStrings(param);
		if (l == null) { return null; }
		if (l.isEmpty()) { return Collections.emptyList(); }
		List<Integer> r = new ArrayList<>(l.size());
		for (String s : l) {
			r.add(Integer.valueOf(s));
		}
		return Tools.freezeList(r);
	}

	/* (non-Javadoc)
	 * @see com.armedia.caliente.cli.parser.CommandLineValues#getLong(com.armedia.caliente.cli.parser.ParameterDefinition)
	 */
	@Override
	public final Long getLong(ParameterDefinition param) {
		String s = getString(param);
		return (s != null ? Long.valueOf(s) : null);
	}

	/* (non-Javadoc)
	 * @see com.armedia.caliente.cli.parser.CommandLineValues#getLong(com.armedia.caliente.cli.parser.ParameterDefinition, long)
	 */
	@Override
	public final long getLong(ParameterDefinition param, long def) {
		Long v = getLong(param);
		return (v != null ? v.longValue() : def);
	}

	/* (non-Javadoc)
	 * @see com.armedia.caliente.cli.parser.CommandLineValues#getAllLongs(com.armedia.caliente.cli.parser.ParameterDefinition)
	 */
	@Override
	public final List<Long> getAllLongs(ParameterDefinition param) {
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
	public final Float getFloat(ParameterDefinition param) {
		String s = getString(param);
		return (s != null ? Float.valueOf(s) : null);
	}

	@Override
	public final float getFloat(ParameterDefinition param, float def) {
		Float v = getFloat(param);
		return (v != null ? v.floatValue() : def);
	}

	@Override
	public final List<Float> getAllFloats(ParameterDefinition param) {
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
	public final Double getDouble(ParameterDefinition param) {
		String s = getString(param);
		return (s != null ? Double.valueOf(s) : null);
	}

	@Override
	public final double getDouble(ParameterDefinition param, double def) {
		assertValid(param);
		Double v = getDouble(param);
		return (v != null ? v.doubleValue() : def);
	}

	@Override
	public final List<Double> getAllDoubles(ParameterDefinition param) {
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
	public final String getString(ParameterDefinition param) {
		List<String> l = getAllStrings(param);
		if ((l == null) || l.isEmpty()) { return null; }
		return l.get(0);
	}

	@Override
	public final String getString(ParameterDefinition param, String def) {
		assertValid(param);
		final String v = getString(param);
		return (v != null ? v : def);
	}

	@Override
	public final List<String> getAllStrings(ParameterDefinition param) {
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
	public final List<String> getAllStrings(ParameterDefinition param, List<String> def) {
		assertValid(param);
		List<String> ret = getAllStrings(param);
		if (ret == null) { return def; }
		return ret;
	}

	@Override
	public final boolean isPresent(ParameterDefinition param) {
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